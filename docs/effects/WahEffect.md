# WahEffect

## Overview

WahEffect is a versatile wah pedal emulation with three operating modes: Auto-wah (envelope-controlled), LFO (automatic sweep), and Manual (fixed position). It uses a resonant state variable filter to recreate the characteristic vocal, "quacky" sound of classic wah pedals.

**Category:** Filter
**ID:** `wah`
**Display Name:** Wah

## Description

The wah pedal is one of the most expressive guitar effects, made famous by players like Jimi Hendrix, Kirk Hammett, and countless funk guitarists. This implementation provides authentic wah tones through a swept resonant bandpass filter, with the added convenience of auto-wah and LFO modes when a physical expression pedal is not available.

## Parameters

### Mode
- **ID:** `mode`
- **Type:** Choice
- **Options:**
  - `Auto` - Envelope follower controls filter sweep (dynamics-controlled)
  - `LFO` - Automatic oscillation sweeps the filter
  - `Manual` - Fixed position (use Position parameter or external control)
- **Default:** Auto

### Position
- **ID:** `position`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Filter position in Manual mode. 0% = bass (toe up), 100% = treble (toe down). In Auto/LFO modes, this parameter is ignored.

### Min Frequency
- **ID:** `minFreq`
- **Range:** 200 Hz to 800 Hz
- **Default:** 400 Hz
- **Unit:** Hz
- **Description:** Lowest frequency of the sweep range (toe-up position). Lower values extend the wah's bass response for deeper, throatier sounds.

### Max Frequency
- **ID:** `maxFreq`
- **Range:** 1000 Hz to 5000 Hz
- **Default:** 2500 Hz
- **Unit:** Hz
- **Description:** Highest frequency of the sweep range (toe-down position). Higher values add more treble bite and "quack" to the effect.

### Resonance
- **ID:** `resonance`
- **Range:** 1.0 to 20.0
- **Default:** 8.0
- **Description:** Filter peak intensity (Q factor). Higher values create more vocal, quacky wah tones. Lower values are smoother and subtler.

### LFO Rate
- **ID:** `lfoRate`
- **Range:** 0.1 Hz to 10.0 Hz
- **Default:** 1.0 Hz
- **Unit:** Hz
- **Description:** Speed of automatic sweep in LFO mode. Slow rates (0.1-0.5 Hz) for ambient sweeps, faster rates (2-6 Hz) for rhythmic, funky patterns.

### Sensitivity
- **ID:** `sensitivity`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Response to playing dynamics in Auto mode. Higher values make the filter more reactive to picking attack; lower values require harder playing to open the filter.

### Attack
- **ID:** `attack`
- **Range:** 1 ms to 100 ms
- **Default:** 10 ms
- **Unit:** ms
- **Description:** How quickly the wah opens (filter sweeps up) in Auto mode. Fast attack for percussive, immediate response; slower for more gradual sweeps.

### Release
- **ID:** `release`
- **Range:** 10 ms to 500 ms
- **Default:** 100 ms
- **Unit:** ms
- **Description:** How quickly the wah closes (filter sweeps down) in Auto mode. Short release for tight, funky response; longer release for sustained, singing tones.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between dry signal and wah-filtered signal. 100% is full wah effect; lower values blend in clean signal.

## DSP Components

### State Variable Filter
The wah uses a state variable filter (SVF) topology that provides simultaneous lowpass, bandpass, and highpass outputs. Only the bandpass output is used for the characteristic wah sound.

**Per Channel:**
- `lpState` - Lowpass state variable
- `bpState` - Bandpass state variable (this is the output)
- `envelope` - Envelope follower state (Auto mode)

### LFO
- **Left Channel:** `LFO lfoL` - Sine wave oscillator
- **Right Channel:** `LFO lfoR` - Sine wave with 25% phase offset (stereo width)

### Envelope Follower
- Attack/release envelope detector for Auto mode
- Separate coefficients for attack (fast) and release (slower)

## Implementation Details

### Signal Flow

```
Input Signal
     |
     v
+-----------------+
| Mode Selection  |
|-----------------|
| Auto: Envelope  |
| LFO: Oscillator |
| Manual: Fixed   |
+-----------------+
     |
     v
  Wah Position (0-1)
     |
     v
+------------------------+
| Frequency Calculation  |
| freq = minF * (maxF/   |
|        minF)^position  |
+------------------------+
     |
     v
+------------------------+
| State Variable Filter  |
| (Resonant Bandpass)    |
+------------------------+
     |
     v
  Soft Saturation (tanh)
     |
     v
+------------------------+
| Dry/Wet Mix            |
+------------------------+
     |
     v
  Output
```

### State Variable Filter Algorithm

The SVF is computed per-sample:

```java
// Calculate filter coefficient from frequency
float f = 2.0f * sin(PI * freq / sampleRate);
f = min(f, 0.99f);  // Stability limit

// Feedback amount from resonance
float fb = q + q / (1.0f - f);

// SVF computation
float hp = input - lpState - fb * bpState;
bpState += f * hp;
bpState = clamp(bpState, -1.5f, 1.5f);  // Prevent runaway
lpState += f * bpState;
lpState = clamp(lpState, -1.5f, 1.5f);

// Output is the bandpass state
output = tanh(bpState * 1.5f);  // Soft saturation
```

### Frequency Sweep Calculation

The wah position (0-1) is converted to frequency using an exponential sweep:

```java
float freq = minFreq * pow(maxFreq / minFreq, wahPosition);
```

This provides perceptually linear frequency movement across the sweep range.

### Envelope Follower (Auto Mode)

```java
float level = abs(input);

if (level > envelope) {
    // Attack: fast rise
    envelope = attackCoeff * envelope + (1 - attackCoeff) * level;
} else {
    // Release: slow decay
    envelope = releaseCoeff * envelope + (1 - releaseCoeff) * level;
}

// Convert envelope to wah position
wahPosition = min(envelope * sensitivity * 10.0f, 1.0f);
```

### Envelope Coefficient Calculation

```java
attackCoeff = exp(-1.0 / (sampleRate * attackMs / 1000.0));
releaseCoeff = exp(-1.0 / (sampleRate * releaseMs / 1000.0));
```

### Stereo Processing

In stereo mode:
- **Auto mode:** Each channel has independent envelope follower
- **LFO mode:** Right channel LFO has 25% phase offset for stereo movement
- **Manual mode:** Both channels use identical position

## Usage Tips

### Auto-Wah Settings

#### Funky Envelope Filter
- **Mode:** Auto
- **Sensitivity:** 60-80%
- **Attack:** 5-15 ms
- **Release:** 80-150 ms
- **Resonance:** 10-15
- **Min Freq:** 300 Hz
- **Max Freq:** 2000 Hz

#### Subtle Touch Response
- **Mode:** Auto
- **Sensitivity:** 30-40%
- **Attack:** 20-40 ms
- **Release:** 200-300 ms
- **Resonance:** 5-8
- **Min Freq:** 400 Hz
- **Max Freq:** 1500 Hz

### LFO Mode Settings

#### Slow Ambient Sweep
- **Mode:** LFO
- **LFO Rate:** 0.1-0.3 Hz
- **Resonance:** 6-10
- **Min Freq:** 300 Hz
- **Max Freq:** 2000 Hz

#### Rhythmic Funk
- **Mode:** LFO
- **LFO Rate:** 2-4 Hz (sync to tempo)
- **Resonance:** 12-18
- **Min Freq:** 400 Hz
- **Max Freq:** 2500 Hz

#### Psychedelic Warble
- **Mode:** LFO
- **LFO Rate:** 6-10 Hz
- **Resonance:** 15-20
- **Min Freq:** 500 Hz
- **Max Freq:** 3000 Hz

### Manual Mode Settings

#### Classic Parked Wah (Mid Position)
- **Mode:** Manual
- **Position:** 40-60%
- **Resonance:** 8-12

Creates a distinctive midrange honk, popular in rock solos.

#### Cocked Wah Treble
- **Mode:** Manual
- **Position:** 80-95%
- **Resonance:** 6-10

Bright, cutting tone for leads.

### Resonance Guidelines

| Resonance | Character |
|-----------|-----------|
| 1-4 | Subtle, smooth filter sweep |
| 5-8 | Musical, moderate emphasis |
| 8-12 | Classic wah quack |
| 12-16 | Aggressive, vocal |
| 16-20 | Extreme, self-oscillating territory |

### Frequency Range Tips

- **Narrow range** (e.g., 500-1500 Hz): More subtle, focused sweep
- **Wide range** (e.g., 300-3000 Hz): Dramatic, expressive sweep
- **Higher range** (e.g., 600-4000 Hz): Brighter, more biting tone
- **Lower range** (e.g., 200-1200 Hz): Darker, throatier tone

## Technical Specifications

- **Filter Type:** State Variable (2nd-order)
- **Filter Slope:** 12 dB/octave
- **Frequency Range:** 200 Hz to 5000 Hz
- **LFO Waveform:** Sine wave
- **Stereo Behavior:** Independent L/R processing with LFO phase offset
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **Saturation:** Soft clipping via tanh()
- **CPU Usage:** Low (efficient SVF algorithm)
- **Latency:** Negligible

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/WahEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes filter states and LFOs
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with independent envelopes
- `updateEnvelopeCoeffs()` - Recalculates attack/release coefficients
- `onReset()` - Clears all states, resets LFO phase

**State Variables:**
- `lpStateL`, `lpStateR` - Lowpass filter states
- `bpStateL`, `bpStateR` - Bandpass filter states (wah output)
- `envelopeL`, `envelopeR` - Envelope follower states
- `lfoL`, `lfoR` - LFO oscillators

## See Also

- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Dedicated envelope filter with more control
- [FilterEffect](FilterEffect.md) - Multi-band filter with various types
- [PhaserEffect](PhaserEffect.md) - Related swept filter effect
- [FlangerEffect](FlangerEffect.md) - Another modulation effect
