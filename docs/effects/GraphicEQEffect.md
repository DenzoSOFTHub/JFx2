# GraphicEQEffect

## Overview

GraphicEQEffect is a classic 10-band graphic equalizer with fixed ISO standard frequencies. Each band provides simple boost/cut control, making it ideal for quick visual tone shaping where the slider positions directly represent the frequency response curve.

**Category:** EQ
**ID:** `graphiceq`
**Display Name:** Graphic EQ

## Description

The graphic EQ is the most intuitive equalizer type - "what you see is what you get." The 10 bands are set at internationally standardized frequencies spanning the full audio spectrum. Simply raise or lower each slider to boost or cut that frequency region. The shared Q parameter allows you to adjust how much the bands overlap, from wide musical response to tighter surgical control.

## Parameters

### Band Gain Controls

All 10 bands have identical gain controls:

| Band | ID | Label | Center Frequency |
|------|-----|-------|------------------|
| 1 | `gain1` | 31 Hz | 31.25 Hz |
| 2 | `gain2` | 62 Hz | 62.5 Hz |
| 3 | `gain3` | 125 Hz | 125 Hz |
| 4 | `gain4` | 250 Hz | 250 Hz |
| 5 | `gain5` | 500 Hz | 500 Hz |
| 6 | `gain6` | 1k Hz | 1000 Hz |
| 7 | `gain7` | 2k Hz | 2000 Hz |
| 8 | `gain8` | 4k Hz | 4000 Hz |
| 9 | `gain9` | 8k Hz | 8000 Hz |
| 10 | `gain10` | 16k Hz | 16000 Hz |

**For each band:**
- **Range:** -12 dB to +12 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Boost or cut the specified frequency region.

### Q (Bandwidth)
- **ID:** `q`
- **Range:** 0.5 to 4.0
- **Default:** 1.5
- **Description:** Bandwidth (Q factor) shared by all middle bands (bands 2-9). Lower values create wider, more overlapping bands for smoother response. Higher values create narrower, more independent bands for precise control.

### Output Level
- **ID:** `output`
- **Range:** -12 dB to +12 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Master output level after EQ. Compensate for overall level changes from boosts or cuts.

## DSP Components

### Biquad Filters
- **10 bands** x 2 channels = **20 biquad filters** total
- Band 1 (31 Hz): High-pass filter (removes sub-bass)
- Bands 2-9: Peak filters (boost/cut)
- Band 10 (16 kHz): Low-pass filter (rolls off ultrasonic)

### Filter Type Selection

The implementation uses different filter types for the edge bands:

| Band | Frequency | Filter Type | Behavior |
|------|-----------|-------------|----------|
| 1 | 31.25 Hz | High-pass | Gain controls Q resonance (0.5-3.0) |
| 2-9 | 62 Hz - 8 kHz | Peak | Gain controls boost/cut amount |
| 10 | 16 kHz | Low-pass | Gain controls Q resonance (0.5-3.0) |

This design provides natural roll-off at the frequency extremes while allowing full parametric control in the middle bands.

## Implementation Details

### Signal Flow

```
Input -> Band 1 (HP) -> Band 2 (Peak) -> ... -> Band 9 (Peak) -> Band 10 (LP) -> Output Gain -> Output
```

All 10 bands process audio in series for consistent phase behavior across the spectrum.

### ISO Standard Frequencies

The band center frequencies follow the ISO 266:1997 standard for preferred frequencies, using the 1/3 octave series at octave intervals:

```
31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, 16000 Hz
```

Each frequency is exactly double the previous (one octave apart), providing even coverage of the audio spectrum.

### Peak Filter Algorithm

For bands 2-9, the standard peak/bell filter is used:

```java
w0 = 2*PI*freq/sampleRate
alpha = sin(w0)/(2*Q)
A = 10^(gain/40)

b0 = 1 + alpha*A
b1 = -2*cos(w0)
b2 = 1 - alpha*A
a0 = 1 + alpha/A
a1 = -2*cos(w0)
a2 = 1 - alpha/A
```

### Edge Band Behavior

The edge bands (31 Hz and 16 kHz) use a unique approach where the gain slider controls the filter's Q (resonance) rather than amplitude:

```java
// For high-pass (band 1) and low-pass (band 10):
Q = 0.707 + (gain/12.0) * 2.0  // Q varies from ~0.5 to ~2.5
Q = clamp(Q, 0.5, 3.0)
```

- Gain = 0 dB: Q = 0.707 (Butterworth, flat)
- Gain = +12 dB: Q = 2.7 (resonant peak at cutoff)
- Gain = -12 dB: Q = 0.5 (gentle rolloff)

### Frequency Response Calculation

The effect provides methods for UI visualization:

- `getFrequencyResponseDb(double frequency)` - Combined response at a frequency
- `getFrequencyResponseCurve(int numPoints)` - Full curve from 20Hz-20kHz

Bands with 0 dB gain are skipped in the calculation for efficiency.

### Change Detection

Filter coefficients are only recalculated when gain or Q values change, reducing CPU usage during static operation.

## Usage Tips

### Understanding the Bands

| Frequency | Character | Typical Adjustments |
|-----------|-----------|---------------------|
| 31 Hz | Sub-bass rumble | Cut to remove unwanted rumble |
| 62 Hz | Deep bass | Boost for weight, cut for tightness |
| 125 Hz | Bass punch | Key bass guitar/kick drum range |
| 250 Hz | Low-mid warmth | Cut to reduce muddiness |
| 500 Hz | Body/fundamental | Boost for fullness, cut for clarity |
| 1 kHz | Midrange presence | Central tone control |
| 2 kHz | Attack/bite | Boost to cut through mix |
| 4 kHz | Presence/edge | Boost for clarity, cut for smoothness |
| 8 kHz | Brilliance | Boost for "air," cut if harsh |
| 16 kHz | Air/sparkle | Subtle top-end sheen |

### Classic "Smiley Face" Curve
- **31 Hz:** -3 dB
- **62 Hz:** 0 dB
- **125 Hz:** +3 dB
- **250 Hz:** -2 dB
- **500 Hz:** -4 dB
- **1 kHz:** -4 dB
- **2 kHz:** -2 dB
- **4 kHz:** +2 dB
- **8 kHz:** +4 dB
- **16 kHz:** +3 dB

Scooped mids with enhanced bass and treble. Popular for metal and rock rhythm tones.

### Flat/Natural Response
Set all bands to 0 dB for bypass-equivalent response.

### Mid-Forward Tone
- **250-500 Hz:** +3 dB
- **1-2 kHz:** +4 dB
- **4-8 kHz:** -2 dB

Emphasizes midrange for better cut-through in dense mixes.

### Bass Enhancement
- **62 Hz:** +4 dB
- **125 Hz:** +3 dB
- **250 Hz:** +2 dB
- Higher bands: 0 dB or slight cut

Adds low-end weight without affecting midrange clarity.

### De-Harshening
- **2 kHz:** -2 dB
- **4 kHz:** -4 dB
- **8 kHz:** -3 dB

Reduces ear fatigue from overly bright or harsh sources.

### Q Parameter Guidelines

| Q Value | Characteristic | Use Case |
|---------|---------------|----------|
| 0.5 | Very wide, overlapping | Smooth, musical response |
| 1.0 | Moderate overlap | General purpose |
| 1.5 (default) | Balanced | Good visual correlation |
| 2.0-3.0 | Narrow, independent | More precise control |
| 4.0 | Very narrow | Maximum band separation |

### Tips for Effective Use

1. **Start flat** - Set all bands to 0 dB before making adjustments
2. **Cut before boost** - Reducing problem frequencies often sounds more natural
3. **Small moves** - 3-4 dB adjustments are usually sufficient
4. **A/B test** - Regularly bypass the EQ to check your changes
5. **Use output control** - Maintain consistent overall level during adjustments

## Technical Specifications

- **Number of Bands:** 10 (fixed ISO frequencies)
- **Frequency Range:** 31.25 Hz to 16 kHz
- **Frequency Spacing:** 1 octave between bands
- **Filter Order:** 2nd-order (12 dB/octave)
- **Gain Range:** +/- 12 dB per band
- **Q Range:** 0.5 to 4.0 (shared)
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **CPU Usage:** Moderate (10 biquad filters per channel)
- **Latency:** Negligible

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/GraphicEQEffect.java`

**Constants:**
- `NUM_BANDS = 10`
- `BAND_FREQUENCIES[] = {31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f}`
- `BAND_LABELS[] = {"31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"}`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes all filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing
- `updateFilters()` - Reconfigures coefficients when parameters change
- `getFrequencyResponseDb(double frequency)` - For UI display
- `getFrequencyResponseCurve(int numPoints)` - Full response curve
- `flatten()` - Reset all bands to 0 dB
- `onReset()` - Clear filter states

**Convenience Accessors:**
- `getBandGain(int band)` / `setBandGain(int band, float dB)`
- `getQ()` / `setQ(float q)`
- `getBandFrequency(int band)` - Returns fixed frequency for band
- `setOutput(float dB)`

## See Also

- [ParametricEQEffect](ParametricEQEffect.md) - 4-band parametric with adjustable frequencies
- [FilterEffect](FilterEffect.md) - Flexible multi-band multi-type filter
- [WahEffect](WahEffect.md) - Wah pedal effect
- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Dynamics-controlled filter
