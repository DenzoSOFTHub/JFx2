# AutoTunerEffect

Gentle pitch correction with expression preservation.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Pitch |
| **ID** | `autotuner` |
| **Display Name** | Auto Tuner |

## Description

The Auto Tuner provides musical pitch correction that respects expression. Unlike aggressive auto-tune effects that snap notes instantly, this effect applies smooth, gradual correction that preserves vibrato, bends, and natural playing dynamics.

Key features:
- **Gentle Correction**: Slow, musical pitch centering
- **Scale-Aware**: Corrects only to notes in the selected scale
- **Expression Preservation**: Maintains vibrato, tremolo, and bends
- **Humanize**: Adds subtle natural variation to avoid robotic sound

## Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `key` | Key | C to B | C | - | Root note of the scale |
| `scale` | Scale | (see below) | Major | - | Scale type for correction |
| `speed` | Speed | 0-100 | 30 | % | Correction speed (lower = more natural) |
| `sensitivity` | Correction | 0-100 | 80 | % | Amount of pitch correction |
| `humanize` | Humanize | 0-100 | 10 | % | Subtle natural pitch variation |
| `blend` | Blend | 0-100 | 100 | % | Wet/dry mix |

## Available Scales (10)

| # | Scale | Intervals (semitones) | Character |
|---|-------|----------------------|-----------|
| 0 | Chromatic | 0,1,2,3,4,5,6,7,8,9,10,11 | All notes allowed |
| 1 | Major | 0,2,4,5,7,9,11 | Bright, happy |
| 2 | Minor | 0,2,3,5,7,8,10 | Sad, emotional |
| 3 | Harmonic Minor | 0,2,3,5,7,8,11 | Classical, exotic |
| 4 | Melodic Minor | 0,2,3,5,7,9,11 | Jazz minor |
| 5 | Pentatonic Major | 0,2,4,7,9 | Folk, country |
| 6 | Pentatonic Minor | 0,3,5,7,10 | Blues, rock |
| 7 | Blues | 0,3,5,6,7,10 | Blues with blue note |
| 8 | Dorian | 0,2,3,5,7,9,10 | Minor with major 6th |
| 9 | Mixolydian | 0,2,4,5,7,9,10 | Dominant 7th feel |

## DSP Components

### Pitch Detection

Uses autocorrelation for fundamental frequency detection:

1. **Buffer Analysis**: 2048-sample circular buffer
2. **Energy Check**: Skip detection if signal too quiet
3. **Autocorrelation**: Find peak correlation in valid pitch range
4. **Period Range**: 20-800 samples (~55 Hz to 2200 Hz)
5. **Parabolic Interpolation**: Refine period for sub-sample accuracy
6. **Detection Rate**: Every 64 samples to save CPU

### Pitch Correction Algorithm

```
Detected Pitch ──► Find Nearest Scale Note ──► Calculate Difference
                                                        │
                                                        ▼
                              Smoothed Correction ◄── Smooth Filter
                                      │
                                      ▼
                              Apply Humanize
                                      │
                                      ▼
                              Calculate Shift Ratio
                                      │
                                      ▼
                              Granular Pitch Shift
```

### Correction Rate

The Speed parameter controls how quickly pitch is corrected:

| Speed | Correction Rate | Behavior |
|-------|-----------------|----------|
| 0% | 0.001 | Very slow, full expression preserved |
| 50% | 0.05 | Moderate, good balance |
| 100% | 0.1 | Fast, more noticeable correction |

Formula: `rate = 0.001 + speed × 0.099`

### Scale Quantization

For each detected pitch:
1. Convert frequency to MIDI note number
2. Find nearest note in selected scale + key
3. Search across nearby octaves for best match
4. Return target frequency

```java
// Frequency to MIDI note
midiNote = 69 + 12 × log2(freq / 440)

// MIDI note to frequency
freq = 440 × 2^((midiNote - 69) / 12)
```

### Pitch Shifting

Granular synthesis with crossfade:
- **Grain Size**: 512 samples
- **Buffer Size**: 4096 samples
- **Window**: Hann window for smooth crossfades
- **Shift Limit**: 0.5x to 2.0x ratio (±1 octave)

## Signal Flow

```
Input ──► Circular Buffer ──► Pitch Detection (every 64 samples)
                │                      │
                │                      ▼
                │             Target Pitch (scale quantized)
                │                      │
                │                      ▼
                │             Smooth Correction + Humanize
                │                      │
                │                      ▼
                └─────────► Granular Pitch Shift ──► Blend ──► Output
                                   │                    ▲
                                   │                    │
                                   └────────────────────┘
                                      (Dry Signal)
```

## Usage Tips

### Natural Correction

```
Key: Match song key
Scale: Major or Minor
Speed: 20-30%
Correction: 70-80%
Humanize: 10-20%
Blend: 100%
```
Preserves expression while gently centering notes.

### Subtle Intonation Fix

```
Speed: 10%
Correction: 50%
Humanize: 15%
Blend: 80%
```
Very gentle correction for live performance.

### Stronger Correction

```
Speed: 50-70%
Correction: 90-100%
Humanize: 5%
Blend: 100%
```
More noticeable correction, still musical.

### Chromatic (All Notes)

```
Scale: Chromatic
Speed: 40%
Correction: 80%
```
Corrects to nearest semitone regardless of key.

### Blues Playing

```
Key: Match blues key
Scale: Blues
Speed: 20%
Correction: 60%
Humanize: 20%
```
Allows blue notes while correcting bad intonation.

## Best Practices

### Preserving Expression

- **Lower Speed**: More time for bends and vibrato
- **Lower Correction**: Gentler centering
- **Higher Humanize**: Natural variation

### Bends and Vibrato

The slow correction rate allows:
- Vibrato to pass through unaffected (too fast to track)
- Bends to complete before correction kicks in
- Tremolo dynamics preserved

### Choosing Key and Scale

1. Match the song's key signature
2. For blues, use Blues or Pentatonic Minor
3. For jazz, consider Dorian or Melodic Minor
4. Chromatic allows all notes but may correct intentional "blue" notes

### Signal Chain Position

- Place after compression for consistent detection
- Place before reverb/delay
- Works best with clean, monophonic signal
- Avoid heavy distortion before this effect

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 1024 samples (~23ms @ 44.1kHz) |
| Detection Range | 55 Hz - 2200 Hz |
| Detection Buffer | 2048 samples |
| Shift Buffer | 4096 samples |
| Pitch Shift Range | ±12 semitones (limited to ±1 octave) |
| Detection Rate | Every 64 samples |

## Implementation Notes

### Autocorrelation Formula

```
correlation(period) = Σ buffer[i] × buffer[i + period]
                      ─────────────────────────────────
                           (buffer_size - period)
```

### Normalized Correlation Threshold

Detection requires `normalized_correlation > 0.3` to avoid false positives on noise.

### Shift Ratio Calculation

```java
targetFreq = currentPitch + smoothedCorrection;
shiftRatio = targetFreq / currentPitch;
shiftRatio = clamp(shiftRatio, 0.5, 2.0);
```

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/AutoTunerEffect.java`
- **Factory ID**: `autotuner`
- **Key Methods**:
  - `detectPitch()`: Autocorrelation pitch detection
  - `findTargetPitch()`: Scale quantization
  - `findNearestScaleNote()`: Note matching
  - `applyPitchShift()`: Granular synthesis
  - `getLatency()`: Returns 1024 samples

## See Also

- [PitchShifterEffect.md](PitchShifterEffect.md) - Fixed pitch shifting
- [HarmonizerEffect.md](HarmonizerEffect.md) - Scale-aware harmonies
- [OctaverEffect.md](OctaverEffect.md) - Octave generation
