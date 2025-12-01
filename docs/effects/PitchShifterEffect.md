# Pitch Shifter Effect

## Overview

PitchShifterEffect is a granular time-domain pitch shifter capable of shifting pitch by up to one octave up or down (plus or minus 12 semitones). It uses overlap-add with crossfaded grains to achieve smooth pitch shifting with adjustable artifact characteristics.

**Category:** Pitch
**ID:** `pitchshift`
**Display Name:** Pitch Shifter

## Description

This effect shifts the pitch of the input signal while maintaining the original duration (time-stretching is not applied). It uses a granular synthesis approach with multiple overlapping grains that are windowed with a Hann (raised cosine) function to minimize discontinuities. The grain size parameter allows trading off between latency/artifacts and smoothness.

## Parameters

### Shift
- **ID:** `shift`
- **Range:** -12 to +12 semitones
- **Default:** 0 st
- **Unit:** st (semitones)
- **Description:** Pitch shift amount in semitones. +12 = one octave up, -12 = one octave down, 0 = no change. Each semitone is a half-step on the musical scale.

### Fine
- **ID:** `fine`
- **Range:** -100 to +100 cents
- **Default:** 0 cents
- **Unit:** cents
- **Description:** Fine pitch adjustment in cents (1/100th of a semitone). Use for subtle detuning or precise tuning adjustments. Combined with Shift for total pitch offset.

### Grain
- **ID:** `grain`
- **Range:** 20 ms to 100 ms
- **Default:** 50 ms
- **Unit:** ms
- **Description:** Processing window (grain) size. Larger values produce smoother results with fewer artifacts but introduce more latency. Smaller values reduce latency but may cause more audible graininess or artifacts.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between original and pitch-shifted signal. At 50%, creates a harmony effect with both original and shifted signals audible. At 100%, only the pitch-shifted signal is heard.

## DSP Components

### Circular Buffer
- **Size:** 4096 samples per channel
- **Purpose:** Stores input samples for grain extraction and read position manipulation

### Grain System
- **Number of Grains:** 4 (OVERLAP constant)
- **Phase Distribution:** Evenly spaced (0.0, 0.25, 0.5, 0.75)
- **Window Function:** Hann window (raised cosine)

### Per-Channel State
- Separate circular buffers for left and right channels
- Independent write positions and grain phases
- Sample fill tracking for startup handling

## Implementation Details

### Signal Flow

```
Input -> Circular Buffer -> [4 Overlapping Grain Readers] -> Window & Sum -> Mix with Dry -> Output
```

### Pitch Ratio Calculation

The pitch ratio determines how fast grains are read relative to writing:
```java
pitchRatio = 2^((shift + fine/100) / 12)
```

For example:
- +12 semitones: pitchRatio = 2.0 (double frequency)
- -12 semitones: pitchRatio = 0.5 (half frequency)
- +7 semitones (perfect fifth): pitchRatio = 1.498

### Granular Synthesis Algorithm

1. **Write Phase**: Each input sample is written to the circular buffer at the current write position.

2. **Grain Reading**: For each of the 4 grains:
   - Calculate read offset based on grain phase and pitch ratio
   - Use linear interpolation between adjacent samples for fractional positions
   - Apply Hann window based on current grain phase
   - Sum weighted contributions from all grains

3. **Phase Advancement**: Each grain's phase advances by `1/grainSamples` per sample, wrapping from 1.0 back to 0.0.

4. **Normalization**: The sum of windowed samples is divided by the sum of window values to maintain consistent amplitude.

### Hann Window Formula

```java
window = 0.5 * (1.0 - cos(2 * PI * phase))
```

This creates a smooth envelope that is zero at phase 0 and 1, maximum at phase 0.5.

### Read Position Calculation

```java
readOffset = grainSamples * (1.0 - phase) * pitchRatio
readPosition = writePosition - readOffset
```

The offset is modulated by the grain phase, causing the read position to sweep through the buffer at a rate determined by the pitch ratio.

### Linear Interpolation

```java
sample = buffer[readPos] * (1 - frac) + buffer[nextPos] * frac
```

Provides smooth transitions between discrete sample values.

## Usage Tips

### Harmony Effect
- **Shift:** +3 to +7 semitones (minor/major third to perfect fifth)
- **Fine:** 0 cents
- **Grain:** 50 ms
- **Mix:** 50%
- Creates a two-voice harmony with the original pitch

### Octave Up
- **Shift:** +12 semitones
- **Fine:** 0 cents
- **Grain:** 40-60 ms
- **Mix:** 100%
- Classic octave-up effect similar to classic pedals

### Octave Down
- **Shift:** -12 semitones
- **Fine:** 0 cents
- **Grain:** 60-80 ms (larger grains help with bass)
- **Mix:** 100%
- Bass-heavy shifted sound

### Subtle Detuning (Chorus-like)
- **Shift:** 0 semitones
- **Fine:** +5 to +15 cents
- **Grain:** 30-50 ms
- **Mix:** 50%
- Creates subtle thickness similar to a chorus

### Whammy-Style Dive
- **Shift:** Sweep from 0 to -12
- **Fine:** 0 cents
- **Grain:** 30 ms (minimize latency for expression)
- **Mix:** 100%
- Dynamic pitch bending effect

### Low-Latency Mode
- **Grain:** 20-30 ms
- For real-time playing where latency is critical
- Accept slightly more artifacts for faster response

### High-Quality Mode
- **Grain:** 80-100 ms
- For recording or when latency is not critical
- Smoothest results with minimal artifacts

## Technical Specifications

- **Maximum Shift Range:** +/- 12 semitones (1 octave)
- **Fine Tuning Resolution:** 1 cent
- **Buffer Size:** 4096 samples (~93 ms at 44.1 kHz)
- **Grain Count:** 4 overlapping grains
- **Interpolation:** Linear
- **Latency:** Equal to grain size (20-100 ms)
- **Processing:** 32-bit float
- **Stereo Support:** Full stereo with independent channel processing

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/PitchShifterEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes buffers and grain phases
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with independent channels
- `onReset()` - Clears buffers and resets grain phases
- `getLatency()` - Returns current latency based on grain size

## See Also

- [OctaverEffect](OctaverEffect.md) - Analog-style octave generation (simpler, lower latency)
- [ChorusEffect](ChorusEffect.md) - Alternative for detuning effects
- [VibratoEffect](VibratoEffect.md) - Pitch modulation effect
