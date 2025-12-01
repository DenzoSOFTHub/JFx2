# OscillatorEffect

## Overview

OscillatorEffect is a generator effect that produces various mathematical waveforms for use as test signals, drone sounds, or synthesis building blocks. It ignores any input signal and outputs pure waveform audio.

**Category:** Input Source
**ID:** `oscillator`
**Display Name:** Oscillator

## Description

This effect generates four classic waveform types (sine, triangle, sawtooth, square) at configurable frequency and volume. It's useful for testing signal chains, creating drone sounds, or as a basic synthesizer voice. The oscillator also supports MIDI note input for musical applications.

## Parameters

### Waveform
- **ID:** `waveform`
- **Type:** Choice
- **Options:**
  - `Sine` - Pure tone, no harmonics (Default)
  - `Triangle` - Mellow, odd harmonics only
  - `Sawtooth` - Bright, all harmonics
  - `Square` - Hollow, odd harmonics
- **Default:** Sine
- **Description:** Wave shape selection. Each waveform has a distinct harmonic character suitable for different applications.

### Frequency
- **ID:** `frequency`
- **Range:** 20 Hz to 2000 Hz
- **Default:** 440 Hz (A4)
- **Unit:** Hz
- **Description:** Oscillator pitch in Hz. A4 = 440 Hz. The range covers the guitar's fundamental frequencies plus harmonics.

### Volume
- **ID:** `volume`
- **Range:** -60 dB to 0 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Output level of the oscillator. At 0 dB, outputs full scale (-1.0 to +1.0). Reduce to blend with other signals.

### Playing
- **ID:** `playing`
- **Type:** Boolean
- **Default:** On (true)
- **Description:** Enable or disable the oscillator output. When off, the effect outputs silence.

## Implementation Details

### Phase Accumulator

The oscillator uses a phase accumulator approach for efficient waveform generation:

```
Phase Increment = (2 * PI * frequency) / sampleRate

Per Sample:
1. Generate sample from current phase
2. Advance phase by increment
3. Wrap phase to [0, 2*PI)
```

This method ensures continuous, click-free waveforms even when frequency changes.

### Waveform Generation

**Sine Wave:**
```java
sample = sin(phase)
```
Pure tone with no harmonics. Ideal for test signals and tuning references.

**Triangle Wave:**
```java
normalizedPhase = phase / (2 * PI)
if (normalizedPhase < 0.25):
    sample = normalizedPhase * 4
else if (normalizedPhase < 0.75):
    sample = 2 - normalizedPhase * 4
else:
    sample = normalizedPhase * 4 - 4
```
Smooth transitions between peaks. Contains only odd harmonics that roll off at -12 dB/octave.

**Sawtooth Wave:**
```java
normalizedPhase = phase / (2 * PI)
sample = 2 * normalizedPhase - 1
```
Ramps from -1 to +1 with instant reset. Contains all harmonics rolling off at -6 dB/octave.

**Square Wave:**
```java
sample = (phase < PI) ? 1.0 : -1.0
```
Binary output (+1 or -1) at 50% duty cycle. Contains only odd harmonics with -6 dB/octave rolloff.

### Signal Flow

```
Phase Accumulator
      |
      v
Waveform Generator (selected type)
      |
      v
Volume (dB to linear)
      |
      v
Playing Gate (on/off)
      |
      v
Output Buffer
```

### Stereo Processing

For stereo output, the same mono signal is sent to both channels:
```java
outputL[i] = sample * volumeLinear
outputR[i] = sample * volumeLinear
```

### MIDI Note Conversion

The effect provides MIDI note-to-frequency conversion:
```java
frequency = 440 * 2^((midiNote - 69) / 12)
```

Where MIDI note 69 = A4 = 440 Hz.

### Parameter Smoothing

Frequency changes are applied immediately. For the phase accumulator approach, this works well for small changes but may cause slight pitch discontinuities for large jumps. For smoother frequency transitions, consider ramping the frequency over multiple samples.

## Usage Tips

### Test Signal Generation
- **Sine at 440 Hz:** Standard tuning reference (A4)
- **Sine at 1000 Hz:** Standard audio test frequency
- **Sine sweep:** Manually vary frequency to test frequency response

### Drone/Pad Sounds
- Use low frequencies (50-200 Hz) for bass drones
- Layer multiple oscillators at harmonic intervals
- Mix with effects (reverb, delay) for ambient textures

### Signal Chain Testing
- Place at the start of chain to test effects in isolation
- Compare processed vs unprocessed for effect verification
- Use specific frequencies to test filter cutoffs

### Waveform Selection Guide

| Waveform | Character | Best For |
|----------|-----------|----------|
| Sine | Pure, clean | Test signals, tuning, clean tones |
| Triangle | Soft, mellow | Pads, flute-like sounds |
| Sawtooth | Bright, buzzy | Lead sounds, rich textures |
| Square | Hollow, reedy | Clarinet-like, chip tunes |

### Volume Levels
- **0 dB:** Full scale, use alone or as primary source
- **-6 to -12 dB:** For mixing with other signals
- **-20 dB and below:** Subtle background tone

### Musical Notes Reference

| Note | Frequency (Hz) |
|------|---------------|
| E2 (Low E) | 82.4 |
| A2 | 110.0 |
| D3 | 146.8 |
| G3 | 196.0 |
| B3 | 246.9 |
| E4 (High E) | 329.6 |
| A4 (Standard) | 440.0 |

## Technical Specifications

- **Frequency Range:** 20 Hz to 2000 Hz
- **Volume Range:** -60 dB to 0 dB
- **Phase Resolution:** Double precision (64-bit)
- **Output Range:** -1.0 to +1.0 (at 0 dB)
- **Waveforms:** 4 (Sine, Triangle, Sawtooth, Square)
- **Processing:** 32-bit float
- **Aliasing:** Not band-limited (may alias at high frequencies)

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/OscillatorEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes phase increment
- `onProcess(...)` - Mono waveform generation
- `onProcessStereo(...)` - Stereo waveform generation
- `generateSample(int waveform)` - Waveform calculation
- `updatePhaseIncrement()` - Recalculates phase increment from frequency

**Waveform Constants:**
- `WAVEFORM_SINE = 0`
- `WAVEFORM_TRIANGLE = 1`
- `WAVEFORM_SAWTOOTH = 2`
- `WAVEFORM_SQUARE = 3`

**Public API:**
- `setWaveform(int waveform)` - Select waveform type
- `getWaveform()` - Get current waveform
- `setFrequency(float hz)` - Set frequency in Hz
- `getFrequency()` - Get current frequency
- `setMidiNote(int midiNote)` - Set frequency from MIDI note
- `play()` - Enable oscillator
- `stop()` - Disable oscillator
- `isPlaying()` - Check if active

## See Also

- [SynthEffect](SynthEffect.md) - Guitar-to-synth with pitch tracking
- [AudioInputEffect](AudioInputEffect.md) - For live audio input
- [WavFileInputEffect](WavFileInputEffect.md) - For file-based audio
