# EnvelopeFilterEffect

## Overview

EnvelopeFilterEffect is a dedicated dynamics-controlled filter (auto-wah) that responds to your playing intensity. Unlike the WahEffect which offers multiple modes, this effect focuses exclusively on envelope-following with additional controls for filter type selection and sweep direction.

**Category:** Filter
**ID:** `envelopefilter`
**Display Name:** Envelope Filter

## Description

The envelope filter is an essential effect for funk, disco, and expressive playing styles. It creates automatic wah-like sweeps triggered by your picking dynamics - play harder and the filter opens up; play softly and it closes down. This implementation offers choice of filter types (lowpass, bandpass, highpass) and bidirectional sweep for maximum versatility.

## Parameters

### Sensitivity
- **ID:** `sensitivity`
- **Range:** -40 dB to 0 dB
- **Default:** -20 dB
- **Unit:** dB
- **Description:** Input sensitivity threshold. Lower (more negative) values make the filter more responsive to soft playing. Higher values require harder attack to trigger the sweep. Think of it as "how hard do I need to play to open the filter?"

### Attack
- **ID:** `attack`
- **Range:** 1 ms to 50 ms
- **Default:** 10 ms
- **Unit:** ms
- **Description:** How fast the filter responds to picking. Fast attack (1-10 ms) creates immediate, percussive response. Slower attack (20-50 ms) creates more gradual sweeps.

### Decay
- **ID:** `decay`
- **Range:** 50 ms to 1000 ms
- **Default:** 200 ms
- **Unit:** ms
- **Description:** How fast the filter closes after the note. Short decay (50-150 ms) for tight, funky response. Long decay (400-1000 ms) for sustained, singing filter sweeps.

### Range
- **ID:** `range`
- **Range:** 100 Hz to 4000 Hz
- **Default:** 2000 Hz
- **Unit:** Hz
- **Description:** Frequency sweep range from the base frequency (200 Hz). Higher values create wider, more dramatic sweeps; lower values are more subtle.

### Resonance (Q)
- **ID:** `resonance`
- **Range:** 0.5 to 10.0
- **Default:** 4.0
- **Description:** Filter resonance (Q factor). Higher values create more pronounced, vocal wah-like peaks. Lower values are smoother and subtler.

### Type
- **ID:** `type`
- **Type:** Choice
- **Options:**
  - `Lowpass` - Smooth, warm filter sweep
  - `Bandpass` - Classic wah sound (default)
  - `Highpass` - Bright, cutting sweep
- **Default:** Bandpass

### Direction
- **ID:** `direction`
- **Type:** Choice
- **Options:**
  - `Up` - Playing harder sweeps to higher frequencies (brighter)
  - `Down` - Playing harder sweeps to lower frequencies (darker)
- **Default:** Up

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between dry signal and filtered signal. 100% is full effect; lower values blend in clean signal for parallel filtering.

## DSP Components

### Biquad Filter
- **Left Channel:** `BiquadFilter filterL`
- **Right Channel:** `BiquadFilter filterR`
- Filter type is dynamically configurable (lowpass, bandpass, or highpass)

### Envelope Follower
- `envelopeL`, `envelopeR` - Per-channel envelope states
- Attack/decay smoothing with configurable time constants

## Implementation Details

### Signal Flow

```
Input Signal
     |
     v
+-------------------+
| Level Detection   |
| abs(input) / sens |
+-------------------+
     |
     v
+-------------------+
| Envelope Follower |
| (Attack/Decay)    |
+-------------------+
     |
     v
+-------------------+
| Direction Select  |
| Up: base + env*rng|
| Down: max - env*rng|
+-------------------+
     |
     v
  Filter Frequency
     |
     v
+-------------------+
| Biquad Filter     |
| (LP/BP/HP)        |
+-------------------+
     |
     v
+-------------------+
| Dry/Wet Mix       |
+-------------------+
     |
     v
  Output
```

### Envelope Follower Algorithm

```java
float absInput = abs(input) / sensitivity;

if (absInput > envelope) {
    // Attack phase - filter opening
    envelope = attackCoeff * envelope + (1 - attackCoeff) * absInput;
} else {
    // Decay phase - filter closing
    envelope = decayCoeff * envelope;
}

// Clamp to valid range
float envValue = min(1.0f, envelope);
```

### Envelope Coefficient Calculation

```java
attackCoeff = exp(-1.0 / (attackMs * sampleRate / 1000.0));
decayCoeff = exp(-1.0 / (decayMs * sampleRate / 1000.0));
```

These coefficients determine how quickly the envelope responds:
- Higher coefficient = slower response (more smoothing)
- Lower coefficient = faster response (more immediate)

### Frequency Calculation

```java
float baseFreq = 200.0f;  // Fixed starting frequency
float maxFreq = baseFreq + range;

if (directionUp) {
    filterFreq = baseFreq + envValue * range;
} else {
    filterFreq = maxFreq - envValue * range;
}

filterFreq = clamp(filterFreq, baseFreq, maxFreq);
```

### Filter Configuration

The filter is reconfigured every sample to track the envelope:

```java
filter.configure(filterType, filterFreq, resonance, 0.0f);
float wet = filter.process(input);
output = dry * (1.0f - mix) + wet * mix;
```

### Stereo Behavior

In stereo mode, both channels share a linked envelope (using the maximum of both channel levels):

```java
float absInput = max(abs(inputL), abs(inputR)) / sensitivity;
```

This ensures consistent filter movement across the stereo field.

## Usage Tips

### Filter Type Selection

| Type | Character | Best For |
|------|-----------|----------|
| **Lowpass** | Warm, smooth | Subtle sweeps, clean tones |
| **Bandpass** | Vocal, quacky | Classic funk, wah sounds |
| **Highpass** | Bright, cutting | Percussive, aggressive tones |

### Direction Selection

| Direction | Character | Best For |
|-----------|-----------|----------|
| **Up** | Brighter when playing hard | Traditional wah feel, emphasis |
| **Down** | Darker when playing hard | Inverted response, unique textures |

### Classic Funk Settings
- **Sensitivity:** -15 dB
- **Attack:** 5 ms
- **Decay:** 150 ms
- **Range:** 2000 Hz
- **Resonance:** 6.0
- **Type:** Bandpass
- **Direction:** Up
- **Mix:** 100%

### Subtle Touch Sensitivity
- **Sensitivity:** -25 dB
- **Attack:** 15 ms
- **Decay:** 300 ms
- **Range:** 1200 Hz
- **Resonance:** 3.0
- **Type:** Lowpass
- **Direction:** Up
- **Mix:** 80%

### Aggressive Quack
- **Sensitivity:** -10 dB
- **Attack:** 3 ms
- **Decay:** 100 ms
- **Range:** 3000 Hz
- **Resonance:** 8.0
- **Type:** Bandpass
- **Direction:** Up
- **Mix:** 100%

### Inverted (Backward) Filter
- **Sensitivity:** -18 dB
- **Attack:** 8 ms
- **Decay:** 200 ms
- **Range:** 2500 Hz
- **Resonance:** 5.0
- **Type:** Bandpass
- **Direction:** Down
- **Mix:** 100%

Creates an unusual "closing" effect when you pick hard.

### Smooth Pad Enhancement
- **Sensitivity:** -30 dB
- **Attack:** 40 ms
- **Decay:** 800 ms
- **Range:** 1500 Hz
- **Resonance:** 2.0
- **Type:** Lowpass
- **Direction:** Up
- **Mix:** 60%

### Sensitivity Guidelines

| Sensitivity | Response |
|-------------|----------|
| -40 dB | Very sensitive, opens on lightest touch |
| -30 dB | Sensitive, good for clean playing |
| -20 dB (default) | Moderate, balanced response |
| -10 dB | Requires hard picking to open fully |
| 0 dB | Maximum input required |

### Attack/Decay Combinations

| Attack | Decay | Character |
|--------|-------|-----------|
| Fast (5ms) | Short (100ms) | Tight, percussive |
| Fast (5ms) | Long (500ms) | Punchy attack, sustained sweep |
| Slow (30ms) | Short (150ms) | Smooth attack, snappy close |
| Slow (30ms) | Long (800ms) | Gradual, ambient |

### Resonance Guidelines

| Q Value | Character |
|---------|-----------|
| 0.5-2 | Subtle, smooth |
| 2-4 | Musical, moderate emphasis |
| 4-6 | Classic envelope filter |
| 6-8 | Pronounced, vocal |
| 8-10 | Aggressive, near self-oscillation |

## Technical Specifications

- **Filter Type:** Biquad (2nd-order IIR)
- **Available Modes:** Lowpass, Bandpass, Highpass
- **Filter Slope:** 12 dB/octave
- **Base Frequency:** Fixed at 200 Hz
- **Maximum Frequency:** 200 Hz + Range parameter (up to 4200 Hz)
- **Envelope Detection:** Peak detection with separate attack/decay
- **Stereo Behavior:** Linked envelope from max(L, R) input
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **CPU Usage:** Low (single biquad per channel)
- **Latency:** Negligible

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/EnvelopeFilterEffect.java`

**Constants:**
- `FILTER_TYPES[] = {"Lowpass", "Bandpass", "Highpass"}`
- `DIRECTION_NAMES[] = {"Up", "Down"}`
- Base frequency: 200 Hz (hardcoded)

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes filters and envelope states
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with linked envelope
- `getFilterType()` - Converts parameter to FilterType enum
- `onReset()` - Clears filter and envelope states

**Convenience Setters:**
- `setSensitivity(float dB)`
- `setAttack(float ms)`
- `setDecay(float ms)`
- `setRange(float hz)`
- `setResonance(float q)`
- `setType(int type)` - 0=Lowpass, 1=Bandpass, 2=Highpass
- `setDirection(int dir)` - 0=Up, 1=Down
- `setMix(float percent)`

## Comparison with WahEffect

| Feature | EnvelopeFilterEffect | WahEffect |
|---------|---------------------|-----------|
| **Modes** | Envelope only | Auto, LFO, Manual |
| **Filter Types** | LP, BP, HP selectable | BP only (SVF) |
| **Direction** | Up or Down | Up only |
| **Filter Topology** | Biquad | State Variable |
| **Min/Max Freq** | Fixed base + range | Adjustable min/max |
| **Best For** | Dedicated auto-wah | Versatile wah pedal |

Choose EnvelopeFilterEffect for dedicated envelope-following with filter type flexibility. Choose WahEffect for a more traditional wah pedal experience with LFO and manual modes.

## See Also

- [WahEffect](WahEffect.md) - Multi-mode wah pedal
- [FilterEffect](FilterEffect.md) - Multi-band configurable filter
- [CompressorEffect](CompressorEffect.md) - Related dynamics processor
- [PhaserEffect](PhaserEffect.md) - Another modulated filter effect
