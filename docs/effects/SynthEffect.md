# SynthEffect

## Overview

SynthEffect is a guitar-to-synth effect that transforms guitar signals into synthesizer-like tones by tracking the input pitch and generating oscillator waveforms. It provides a monophonic synth voice with filter, LFO modulation, and envelope following.

**Category:** Filter
**ID:** `synth`
**Display Name:** Synth

## Description

This effect analyzes your guitar signal to detect the pitch being played, then generates a synthesizer waveform at that frequency. The result is a synth sound that follows your playing dynamics and pitch. Combined with a resonant filter and LFO modulation, it can create everything from classic synth leads to sci-fi effects.

## Parameters

### Waveform
- **ID:** `waveform`
- **Type:** Choice
- **Options:**
  - `Square` - Classic synth sound (Default)
  - `Saw` - Bright, buzzy
  - `Triangle` - Mellow, soft
  - `Sine` - Pure, clean
- **Default:** Square
- **Description:** Oscillator waveform. Square is the classic synthesizer sound, Saw is brighter, Triangle and Sine are smoother.

### Octave
- **ID:** `octave`
- **Type:** Choice
- **Options:**
  - `-1 Oct` - One octave down
  - `Normal` - Same pitch as input (Default)
  - `+1 Oct` - One octave up
  - `+2 Oct` - Two octaves up
- **Default:** Normal
- **Description:** Pitch shift in octaves relative to the detected pitch.

### Filter
- **ID:** `filter`
- **Range:** 100 Hz to 8000 Hz
- **Default:** 2000 Hz
- **Unit:** Hz
- **Description:** Lowpass filter cutoff frequency. Lower values create darker, more muffled tones. Higher values are brighter.

### Q (Resonance)
- **ID:** `resonance`
- **Range:** 0.5 to 10
- **Default:** 2
- **Description:** Filter resonance. Higher values create more pronounced filter sweep and can approach self-oscillation for dramatic effects.

### Attack
- **ID:** `attack`
- **Range:** 1 ms to 500 ms
- **Default:** 10 ms
- **Unit:** ms
- **Description:** Envelope attack time. How quickly the synth responds to picking dynamics. Shorter = snappier, longer = softer.

### Release
- **ID:** `release`
- **Range:** 10 ms to 2000 ms
- **Default:** 200 ms
- **Unit:** ms
- **Description:** Envelope release time. How long the synth sustains after the note fades. Longer = more sustain.

### LFO Rate
- **ID:** `lfoRate`
- **Range:** 0 Hz to 10 Hz
- **Default:** 2 Hz
- **Unit:** Hz
- **Description:** Speed of filter modulation. 0 = no modulation. Higher values create faster wobble effects.

### LFO Depth
- **ID:** `lfoDepth`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Amount of LFO modulation applied to the filter cutoff. Higher values create more dramatic filter sweeps.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between dry guitar and synth signal. 100% = pure synth, 50% = equal blend, 0% = dry only.

### Glide
- **ID:** `glide`
- **Range:** 0 ms to 500 ms
- **Default:** 50 ms
- **Unit:** ms
- **Description:** Portamento time between notes. Creates smooth pitch slides between notes instead of instant jumps.

## Implementation Details

### Pitch Detection

The effect uses zero-crossing pitch detection:

```
Zero-Crossing Algorithm:
1. Store samples in circular buffer
2. Detect negative-going zero crossings
3. Measure period between crossings
4. Convert period to frequency: freq = sampleRate / period
5. Apply smoothing: detectedFreq = 0.9 * previous + 0.1 * new
6. Accept only frequencies in guitar range (80-1200 Hz)
```

This simple method works well for monophonic guitar signals but may struggle with:
- Very low notes (less accurate)
- Chords (tracks strongest fundamental)
- Noisy or distorted signals

### Oscillator Generation

```
Per Sample:
1. Calculate target frequency from detected pitch + octave shift
2. Apply glide smoothing to current frequency
3. Generate waveform sample from oscillator phase
4. Advance phase: phase += currentFreq / sampleRate
5. Wrap phase at 1.0
6. Apply envelope to oscillator output
```

Waveform formulas match OscillatorEffect:
- Square: `phase < 0.5 ? 1 : -1`
- Saw: `2 * phase - 1`
- Triangle: Piecewise linear
- Sine: `sin(phase * 2 * PI)`

### Envelope Follower

```
if (inputLevel > envelope):
    envelope = attackCoeff * envelope + (1 - attackCoeff) * inputLevel
else:
    envelope = releaseCoeff * envelope

Where:
attackCoeff = exp(-1 / (attackMs * sampleRate / 1000))
releaseCoeff = exp(-1 / (releaseMs * sampleRate / 1000))
```

The envelope shapes the synth output to match playing dynamics.

### Filter

A biquad lowpass filter processes the oscillator output:
- Type: Lowpass
- Frequency: Filter parameter + LFO modulation
- Q: Resonance parameter
- Updated per-sample for smooth LFO effect

### LFO Modulation

```
modFilterFreq = filterFreq * (1 + lfoValue * lfoDepth)
modFilterFreq = clamp(modFilterFreq, 100, 8000)
```

The LFO uses a triangle waveform for smooth, symmetrical modulation.

### Glide (Portamento)

```
if (glideMs > 0 && currentFreq > 0):
    currentFreq = glideCoeff * currentFreq + (1 - glideCoeff) * targetFreq
else:
    currentFreq = targetFreq
```

This creates smooth frequency transitions between notes.

### Signal Flow

```
Guitar Input
    |
    +----> Pitch Detection ----+
    |                          |
    +----> Envelope Follower   |
    |            |             v
    |            |      Octave Shift
    |            |             |
    |            |             v
    |            |      Glide Smoothing
    |            |             |
    |            |             v
    |            +----> Oscillator
    |                         |
    |                         v
    |         LFO ------> Lowpass Filter
    |                         |
    |                         v
    +----(dry)----> Mix <----(wet)
                      |
                      v
                   Output
```

## Usage Tips

### Classic Synth Lead
- **Waveform:** Square
- **Octave:** Normal or +1
- **Filter:** 1500-2500 Hz
- **Resonance:** 3-5
- **Attack:** 5-20 ms
- **Release:** 150-300 ms
- **LFO Rate:** 0 Hz (off)
- **Mix:** 100%

### Warm Pad Sound
- **Waveform:** Triangle or Sine
- **Octave:** Normal or -1
- **Filter:** 800-1500 Hz
- **Resonance:** 1-2
- **Attack:** 100-300 ms
- **Release:** 500-1000 ms
- **LFO Rate:** 1-2 Hz
- **LFO Depth:** 20-40%
- **Mix:** 80-100%

### Aggressive Synth
- **Waveform:** Saw
- **Octave:** Normal
- **Filter:** 3000-5000 Hz
- **Resonance:** 6-8
- **Attack:** 1-5 ms
- **Release:** 100-200 ms
- **LFO Rate:** 4-6 Hz
- **LFO Depth:** 50-70%
- **Mix:** 100%

### Tips for Best Results
1. **Clean playing:** Synth works best with clean, clear notes
2. **Single notes:** Monophonic - one note at a time tracks best
3. **Palm muting:** Helps with cleaner pitch tracking
4. **Neck pickup:** Often provides cleaner fundamental for tracking
5. **Play deliberately:** Let notes ring for better tracking

### Blending with Guitar
- Use Mix at 50-80% for synth+guitar layering
- Lower Filter with high Mix for contrast
- Add a bit of dry signal for attack definition

## Technical Specifications

- **Pitch Detection:** Zero-crossing method
- **Detection Range:** 80 Hz to 1200 Hz
- **Pitch Buffer:** sampleRate / 40 samples
- **Waveforms:** 4 (Square, Saw, Triangle, Sine)
- **Filter Type:** Biquad lowpass
- **LFO Waveform:** Triangle
- **Processing:** 32-bit float
- **Output Scaling:** 0.7 (headroom protection)

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/SynthEffect.java`

**Key Methods:**
- `onPrepare(...)` - Initialize pitch buffer, filters, LFO
- `onProcess(...)` - Mono processing
- `onProcessStereo(...)` - Stereo processing
- `detectPitch(float sample)` - Zero-crossing pitch detection
- `oscillator(int waveform, float phase)` - Waveform generation

**DSP Components:**
- `BiquadFilter filterL, filterR` - Resonant lowpass filter
- `LFO lfo` - Modulation oscillator
- `float[] pitchBuffer` - Circular buffer for pitch detection

**Convenience Setters:**
- `setWaveform(int)`, `setOctave(int)`
- `setFilter(float hz)`, `setResonance(float q)`
- `setAttack(float ms)`, `setRelease(float ms)`
- `setLfoRate(float hz)`, `setLfoDepth(float percent)`
- `setMix(float percent)`, `setGlide(float ms)`

## See Also

- [OscillatorEffect](OscillatorEffect.md) - Standalone oscillator
- [TalkBoxEffect](TalkBoxEffect.md) - Vocal formant filtering
- [FilterEffect](FilterEffect.md) - Basic filter effect
- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Auto-wah/envelope filter
