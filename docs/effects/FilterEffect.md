# FilterEffect

## Overview

FilterEffect is a versatile multi-band filter with up to 5 independently configurable filter bands. Each band can be set to various filter types including lowpass, highpass, bandpass, notch, parametric peak, shelving, and all-pass filters.

**Category:** EQ
**ID:** `filter`
**Display Name:** Filter

## Description

This is a powerful Swiss Army knife filter effect that provides maximum flexibility for sculpting your tone. Unlike a traditional parametric EQ with fixed band roles, each of the 5 bands can be configured as any filter type, allowing you to create complex filtering setups such as multiple notch filters for feedback elimination, cascaded lowpass filters for steep roll-off, or creative combinations for unique tonal shaping.

## Parameters

### Band Parameters (Bands 1-5)

Each of the 5 bands has 4 parameters:

#### Band Type
- **ID:** `type1` through `type5`
- **Type:** Choice
- **Options:**
  - `None` - Band disabled (no processing)
  - `Low Pass` - Cuts frequencies above cutoff
  - `High Pass` - Cuts frequencies below cutoff
  - `Band Pass` - Passes frequencies around center, cuts others
  - `Notch` - Cuts frequencies around center (opposite of bandpass)
  - `Peak` - Boost/cut at center frequency (parametric EQ style)
  - `Low Shelf` - Boost/cut all frequencies below corner
  - `High Shelf` - Boost/cut all frequencies above corner
  - `All Pass` - Changes phase without affecting amplitude
- **Default:** None

#### Band Frequency
- **ID:** `freq1` through `freq5`
- **Range:** 20 Hz to 20000 Hz
- **Defaults:** 100 Hz, 300 Hz, 1000 Hz, 3000 Hz, 8000 Hz (spread across spectrum)
- **Unit:** Hz
- **Description:** Center/cutoff/corner frequency for the band. The exact meaning depends on filter type.

#### Band Q
- **ID:** `q1` through `q5`
- **Range:** 0.1 to 20.0
- **Default:** 0.707 (Butterworth response)
- **Description:** Q factor (resonance/bandwidth). Higher values create narrower, more resonant filters. Q of 0.707 provides maximally flat passband for lowpass/highpass.

#### Band Gain
- **ID:** `gain1` through `gain5`
- **Range:** -24 dB to +24 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Gain for Peak and Shelf filter types. Has no effect on other filter types.

### Output Level
- **ID:** `output`
- **Range:** -12 dB to +12 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Master output level after all filtering. Use to compensate for overall level changes.

## DSP Components

### Biquad Filters
- **Per Band:** 2 `BiquadFilter` instances (left and right channel)
- **Total:** 10 biquad filters for full stereo processing
- Filters are cascaded in series (Band 1 -> Band 2 -> Band 3 -> Band 4 -> Band 5)

### Change Detection
- Parameters are monitored for changes
- Filter coefficients only recalculated when parameters actually change
- Reduces CPU usage during static operation

## Implementation Details

### Signal Flow

```
Input -> Band 1 -> Band 2 -> Band 3 -> Band 4 -> Band 5 -> Output Gain -> Output
         (if not None)   ...cascaded...                (if not None)
```

### Filter Coefficient Calculation

All filter types use standard biquad (2nd-order IIR) implementations based on the Audio EQ Cookbook formulas. The transfer function is:

```
        b0 + b1*z^-1 + b2*z^-2
H(z) = ------------------------
        a0 + a1*z^-1 + a2*z^-2
```

#### Lowpass Filter
```java
w0 = 2*PI*freq/sampleRate
alpha = sin(w0)/(2*Q)

b0 = (1 - cos(w0))/2
b1 = 1 - cos(w0)
b2 = (1 - cos(w0))/2
a0 = 1 + alpha
a1 = -2*cos(w0)
a2 = 1 - alpha
```

#### Highpass Filter
```java
b0 = (1 + cos(w0))/2
b1 = -(1 + cos(w0))
b2 = (1 + cos(w0))/2
a0 = 1 + alpha
a1 = -2*cos(w0)
a2 = 1 - alpha
```

#### Bandpass Filter
```java
b0 = alpha
b1 = 0
b2 = -alpha
a0 = 1 + alpha
a1 = -2*cos(w0)
a2 = 1 - alpha
```

#### Notch Filter
```java
b0 = 1
b1 = -2*cos(w0)
b2 = 1
a0 = 1 + alpha
a1 = -2*cos(w0)
a2 = 1 - alpha
```

#### Peak Filter (Parametric EQ)
```java
A = 10^(gain/40)  // gain in dB

b0 = 1 + alpha*A
b1 = -2*cos(w0)
b2 = 1 - alpha*A
a0 = 1 + alpha/A
a1 = -2*cos(w0)
a2 = 1 - alpha/A
```

#### Low Shelf Filter
```java
A = 10^(gain/40)
sqrtA = sqrt(A)
sqrtA2Alpha = 2*sqrtA*alpha

b0 = A*((A+1) - (A-1)*cos(w0) + sqrtA2Alpha)
b1 = 2*A*((A-1) - (A+1)*cos(w0))
b2 = A*((A+1) - (A-1)*cos(w0) - sqrtA2Alpha)
a0 = (A+1) + (A-1)*cos(w0) + sqrtA2Alpha
a1 = -2*((A-1) + (A+1)*cos(w0))
a2 = (A+1) + (A-1)*cos(w0) - sqrtA2Alpha
```

#### High Shelf Filter
```java
b0 = A*((A+1) + (A-1)*cos(w0) + sqrtA2Alpha)
b1 = -2*A*((A-1) + (A+1)*cos(w0))
b2 = A*((A+1) + (A-1)*cos(w0) - sqrtA2Alpha)
a0 = (A+1) - (A-1)*cos(w0) + sqrtA2Alpha
a1 = 2*((A-1) - (A+1)*cos(w0))
a2 = (A+1) - (A-1)*cos(w0) - sqrtA2Alpha
```

#### All Pass Filter
```java
b0 = 1 - alpha
b1 = -2*cos(w0)
b2 = 1 + alpha
a0 = 1 + alpha
a1 = -2*cos(w0)
a2 = 1 - alpha
```

### Frequency Response Calculation

The effect includes methods to calculate the combined frequency response:

- `getFrequencyResponseDb(double frequency)` - Returns magnitude in dB at a specific frequency
- `getFrequencyResponseCurve(int numPoints)` - Returns array of dB values across 20Hz-20kHz (log-spaced)

These methods use `getTargetValue()` for immediate UI feedback during parameter adjustment.

## Usage Tips

### Simple Tone Shaping
- **Band 1:** Low Shelf at 100 Hz for bass control
- **Band 2:** Peak at 400 Hz to reduce muddiness
- **Band 3:** Peak at 2.5 kHz for presence
- **Band 4:** High Shelf at 8 kHz for air/sparkle
- **Band 5:** None (disabled)

### Guitar Cabinet Simulation Enhancement
- **Band 1:** High Pass at 80 Hz, Q=0.707 (remove sub-bass rumble)
- **Band 2:** Low Shelf at 200 Hz, -3 dB (tighten low end)
- **Band 3:** Notch at 3.5 kHz, Q=5 (reduce harshness)
- **Band 4:** Low Pass at 6 kHz, Q=0.707 (roll off fizz)
- **Band 5:** None

### Feedback Elimination (Live Performance)
- Use multiple narrow Notch filters (Q=10-20) at problem frequencies
- Identify feedback frequencies and notch them out surgically
- Minimal impact on overall tone compared to wide cuts

### Creative Phase Effects
- Use multiple All Pass filters at different frequencies
- Creates phaser-like effects when frequencies are swept
- Combine with other effects for unique textures

### Steep Filter Slopes
- Cascade multiple Lowpass or Highpass filters at the same frequency
- Each 2nd-order filter adds 12 dB/octave slope
- 5 cascaded lowpass = 60 dB/octave slope (very steep!)

## Technical Specifications

- **Number of Bands:** 5 (all independently configurable)
- **Filter Order:** 2nd-order (12 dB/octave per band)
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **CPU Usage:** Low to moderate (depends on number of active bands)
- **Latency:** Negligible (IIR filters have no lookahead)

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/FilterEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes all biquad filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing (independent L/R filters)
- `updateFilters()` - Recalculates coefficients when parameters change
- `getFrequencyResponseDb(double frequency)` - For UI frequency response display
- `onReset()` - Clears all filter states

## See Also

- [ParametricEQEffect](ParametricEQEffect.md) - Traditional 4-band parametric EQ
- [GraphicEQEffect](GraphicEQEffect.md) - 10-band graphic equalizer
- [WahEffect](WahEffect.md) - Wah pedal with swept bandpass filter
- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Dynamics-controlled filter
