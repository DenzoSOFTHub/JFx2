# ParametricEQEffect

## Overview

ParametricEQEffect is a classic 4-band parametric equalizer with a fixed band structure: low shelf, two fully parametric mid bands, and a high shelf. This design mirrors professional studio and live sound EQs, providing intuitive tone shaping for guitar and audio processing.

**Category:** EQ
**ID:** `parametriceq`
**Display Name:** Parametric EQ

## Description

This is a traditional parametric EQ designed for straightforward tone sculpting. Unlike the more flexible FilterEffect, this EQ has a fixed structure that's immediately familiar to musicians and engineers: control the lows with a shelf, surgically adjust two mid frequencies with parametric bands, and shape the highs with another shelf. The Q control on the mid bands allows everything from broad tonal sweeps to precise notching.

## Parameters

### Band 1: Low Shelf

#### Low Frequency
- **ID:** `lowFreq`
- **Range:** 20 Hz to 500 Hz
- **Default:** 100 Hz
- **Unit:** Hz
- **Description:** Corner frequency for the low shelf. Frequencies below this point are affected by the gain setting. Lower values affect only the deepest bass; higher values extend into the low-midrange.

#### Low Gain
- **ID:** `lowGain`
- **Range:** -15 dB to +15 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Boost or cut the bass frequencies. Positive values add warmth and weight; negative values tighten the low end and reduce muddiness.

### Band 2: Low-Mid (Parametric)

#### Lo-Mid Frequency
- **ID:** `lowMidFreq`
- **Range:** 100 Hz to 2000 Hz
- **Default:** 400 Hz
- **Unit:** Hz
- **Description:** Center frequency for the low-mid parametric band. This range covers the "body" of most instruments, fundamental tones, and potential mud/boxiness.

#### Lo-Mid Gain
- **ID:** `lowMidGain`
- **Range:** -15 dB to +15 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Boost or cut at the center frequency. Cut around 300-500 Hz to reduce boxiness; boost around 800-1200 Hz for warmth and fullness.

#### Lo-Mid Q
- **ID:** `lowMidQ`
- **Range:** 0.1 to 10.0
- **Default:** 1.0
- **Description:** Bandwidth (Q factor) of the band. Higher Q values create narrower, more surgical adjustments; lower Q values create broader, more musical changes.

### Band 3: High-Mid (Parametric)

#### Hi-Mid Frequency
- **ID:** `highMidFreq`
- **Range:** 500 Hz to 8000 Hz
- **Default:** 2000 Hz
- **Unit:** Hz
- **Description:** Center frequency for the high-mid parametric band. This range covers presence, attack, and potential harshness in guitars and vocals.

#### Hi-Mid Gain
- **ID:** `highMidGain`
- **Range:** -15 dB to +15 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Boost or cut at the center frequency. Boost around 2-4 kHz for presence and "cut-through"; cut around 3-5 kHz to reduce harshness and ear fatigue.

#### Hi-Mid Q
- **ID:** `highMidQ`
- **Range:** 0.1 to 10.0
- **Default:** 1.0
- **Description:** Bandwidth of the band. Use higher Q to target specific resonances or problem frequencies; use lower Q for general tone shaping.

### Band 4: High Shelf

#### High Frequency
- **ID:** `highFreq`
- **Range:** 2000 Hz to 16000 Hz
- **Default:** 8000 Hz
- **Unit:** Hz
- **Description:** Corner frequency for the high shelf. Frequencies above this point are affected by the gain setting. Lower values affect upper mids; higher values target only the airiest treble.

#### High Gain
- **ID:** `highGain`
- **Range:** -15 dB to +15 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Boost or cut the treble frequencies. Positive values add air, sparkle, and definition; negative values darken the tone and reduce harshness or hiss.

### Output Level

- **ID:** `output`
- **Range:** -12 dB to +12 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Master output level after EQ. Use to compensate for overall level changes caused by boosts or cuts.

## DSP Components

### Biquad Filters
- **Low Shelf:** `BiquadFilter lowShelfL, lowShelfR` - Low shelf filter with fixed Q=0.707
- **Low Mid:** `BiquadFilter lowMidL, lowMidR` - Peak filter with variable Q
- **High Mid:** `BiquadFilter highMidL, highMidR` - Peak filter with variable Q
- **High Shelf:** `BiquadFilter highShelfL, highShelfR` - High shelf filter with fixed Q=0.707

Total: 8 biquad filters for stereo processing

## Implementation Details

### Signal Flow

```
Input -> Low Shelf -> Low Mid (Peak) -> High Mid (Peak) -> High Shelf -> Output Gain -> Output
```

All four bands are always active and process audio in series, even when gain is 0 dB (unity gain maintains phase consistency).

### Filter Types Used

| Band | Filter Type | Q Behavior |
|------|-------------|------------|
| Low | Low Shelf | Fixed at 0.707 (Butterworth) |
| Lo-Mid | Peak (Parametric) | User adjustable 0.1-10 |
| Hi-Mid | Peak (Parametric) | User adjustable 0.1-10 |
| High | High Shelf | Fixed at 0.707 (Butterworth) |

### Shelf Filter Implementation

The shelf filters use a Q of 0.707 (1/sqrt(2)), which provides a Butterworth response - maximally flat passband with smooth transition. This is the standard for studio EQs.

### Peak Filter Implementation

The parametric mid bands use the standard peak/bell filter design:

```java
A = 10^(gain/40)  // gain in dB
w0 = 2*PI*freq/sampleRate
alpha = sin(w0)/(2*Q)

b0 = 1 + alpha*A
b1 = -2*cos(w0)
b2 = 1 - alpha*A
a0 = 1 + alpha/A
a1 = -2*cos(w0)
a2 = 1 - alpha/A
```

The bandwidth in octaves relates to Q by:
```
BW = 2 * asinh(1/(2*Q)) / ln(2)
```

| Q Value | Bandwidth (octaves) |
|---------|---------------------|
| 0.1 | ~6.6 octaves (very wide) |
| 0.5 | ~2.5 octaves |
| 1.0 | ~1.4 octaves |
| 2.0 | ~0.7 octaves |
| 5.0 | ~0.28 octaves |
| 10.0 | ~0.14 octaves (surgical) |

### Parameter Update Strategy

Filters are reconfigured every processing block by calling `updateFilters()`. This ensures smooth parameter changes but adds minimal overhead since filter coefficient calculation is relatively inexpensive.

## Usage Tips

### General Guitar Tone Shaping
- **Low Shelf:** 100 Hz, +2 dB (add weight without mud)
- **Lo-Mid:** 400 Hz, -3 dB, Q=1.5 (reduce boxiness)
- **Hi-Mid:** 3 kHz, +2 dB, Q=1.0 (add presence)
- **High Shelf:** 8 kHz, +1 dB (subtle sparkle)

### Clean Guitar Enhancement
- **Low Shelf:** 80 Hz, -2 dB (tighten bass)
- **Lo-Mid:** 250 Hz, +2 dB, Q=0.8 (warmth)
- **Hi-Mid:** 4 kHz, +3 dB, Q=1.2 (chime and clarity)
- **High Shelf:** 10 kHz, +2 dB (air and definition)

### High-Gain Tightening
- **Low Shelf:** 120 Hz, -4 dB (remove flub)
- **Lo-Mid:** 800 Hz, +2 dB, Q=1.0 (midrange punch)
- **Hi-Mid:** 3.5 kHz, -3 dB, Q=2.0 (reduce ice-pick harshness)
- **High Shelf:** 6 kHz, -2 dB (tame fizz)

### Acoustic Guitar
- **Low Shelf:** 80 Hz, -3 dB (reduce boominess)
- **Lo-Mid:** 200 Hz, -2 dB, Q=1.0 (less mud)
- **Hi-Mid:** 5 kHz, +3 dB, Q=0.8 (string clarity)
- **High Shelf:** 12 kHz, +2 dB (air and shimmer)

### Problem Frequency Hunting
1. Set one mid band to Q=5 or higher, gain=+10 dB
2. Sweep the frequency slowly while playing
3. Problem frequencies will become obvious (harsh, boomy, resonant)
4. Once found, change gain to negative to cut
5. Adjust Q: narrower for specific resonances, wider for general tone issues

### Mix-Ready Tone
Start with all gains at 0 dB, then:
1. Cut first, boost second
2. Use narrow Q (2-4) for cuts, wide Q (0.5-1.5) for boosts
3. Keep total boost/cut under 6 dB for natural sound
4. Use output control to maintain consistent overall level

## Technical Specifications

- **Number of Bands:** 4 (fixed configuration)
- **Filter Order:** 2nd-order (12 dB/octave slope)
- **Shelf Q:** Fixed at 0.707 (Butterworth)
- **Peak Q Range:** 0.1 to 10.0
- **Gain Range:** +/- 15 dB per band
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **CPU Usage:** Low (4 biquad filters per channel)
- **Latency:** Negligible

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/ParametricEQEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes all filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing
- `updateFilters()` - Reconfigures all filter coefficients
- `onReset()` - Clears filter states

**Convenience Setters:**
- `setLowFreq(float hz)`, `setLowGain(float dB)`
- `setLowMidFreq(float hz)`, `setLowMidGain(float dB)`, `setLowMidQ(float q)`
- `setHighMidFreq(float hz)`, `setHighMidGain(float dB)`, `setHighMidQ(float q)`
- `setHighFreq(float hz)`, `setHighGain(float dB)`
- `setOutput(float dB)`

## See Also

- [FilterEffect](FilterEffect.md) - Flexible 5-band multi-type filter
- [GraphicEQEffect](GraphicEQEffect.md) - 10-band graphic equalizer
- [WahEffect](WahEffect.md) - Wah pedal effect
- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Dynamics-controlled filter
