# Ring Modulator Effect

## Description

The Ring Modulator effect multiplies the input signal by a carrier oscillator to create metallic, bell-like, and dissonant tones. The output contains sum and difference frequencies of the input and carrier, producing inharmonic overtones. This creates sounds ranging from subtle shimmer to extreme robotic and alien effects.

**Category**: Modulation
**ID**: `ringmod`
**Display Name**: Ring Modulator

## How It Works

Ring modulation is based on amplitude modulation where the carrier oscillator varies between -1 and +1 (rather than 0 and 1 in standard AM):

1. Generate a carrier oscillator at a specified frequency
2. Multiply the input signal by the carrier: `output = input × carrier`
3. This multiplication creates sum and difference frequencies:
   - For input frequency `f_in` and carrier frequency `f_c`:
   - Output contains: `f_in + f_c` and `|f_in - f_c|`
4. Optional LFO modulates the carrier frequency for evolving timbres
5. Mix the modulated signal with the dry signal

The mathematical relationship creates non-harmonic overtones, which is why ring modulation sounds metallic and bell-like rather than musical in the traditional sense.

## Parameters

### Frequency
- **ID**: `freq`
- **Range**: 20 Hz to 4000 Hz
- **Default**: 440 Hz
- **Unit**: Hz
- **Description**: Carrier oscillator frequency. Lower frequencies (20-100 Hz) create tremolo-like effects with metallic overtones. Mid frequencies (200-800 Hz) produce classic bell tones. High frequencies (1000-4000 Hz) create bright, shimmering, or harsh timbres. Musical intervals (octaves, fifths) relative to the input create more tonal results.

### Waveform
- **ID**: `waveform`
- **Type**: Choice
- **Options**:
  - 0: Sine
  - 1: Square
  - 2: Triangle
  - 3: Sawtooth
- **Default**: 0 (Sine)
- **Description**: Carrier wave shape:
  - **Sine**: Pure, simple ring modulation - creates the cleanest bell-like tones
  - **Square**: Harsh, aggressive - adds odd harmonics for robotic, buzzy sounds
  - **Triangle**: Mellow, softer than square - creates warmer metallic tones
  - **Sawtooth**: Bright, complex - rich in harmonics for sci-fi effects

### LFO Depth
- **ID**: `lfoDepth`
- **Range**: 0% to 100%
- **Default**: 0%
- **Unit**: %
- **Description**: Amount of frequency modulation from the LFO. At 0%, the carrier frequency is static. Higher values create warbling, sci-fi effects as the carrier frequency sweeps up and down. Creates evolving, non-static timbres.

### LFO Rate
- **ID**: `lfoRate`
- **Range**: 0.1 Hz to 10.0 Hz
- **Default**: 1.0 Hz
- **Unit**: Hz
- **Description**: Speed of the frequency modulation. Slow rates (0.1-1 Hz) create subtle, slowly evolving timbres. Medium rates (1-4 Hz) add pronounced movement. Fast rates (5-10 Hz) create extreme, chaotic effects. Only active when LFO Depth > 0%.

### Mix
- **ID**: `mix`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Balance between dry signal and ring modulated signal. At 0%, the effect is bypassed. At 50%, equal mix creates usable metallic tones. At 100%, only the ring modulated signal is heard for extreme effects.

## Implementation Details

### Signal Flow

```
Input ──────────────────────────┐
                                ↓
Carrier Oscillator ──→ Multiply ──→ Mix ──→ Output
      ↑                                ↑
      LFO (optional)              Dry Signal
```

### Carrier Oscillator

The carrier is generated using phase accumulation:

```java
phaseInc = 2π / sampleRate
carrierPhase += phaseInc * frequency
if (carrierPhase >= 2π) carrierPhase -= 2π
carrier = waveformFunction(carrierPhase)
```

**Waveform Generation:**
- **Sine**: `sin(phase)`
- **Square**: `phase < π ? 1.0 : -1.0`
- **Triangle**: `2 * abs(2 * (phase / 2π) - 1) - 1`
- **Sawtooth**: `phase / π - 1.0`

### LFO Modulation

When LFO Depth > 0%:

```java
lfoMod = lfo.tick()  // -1 to +1
modulatedFreq = baseFreq * (1.0 + lfoMod * lfoDepth)
modulatedFreq = clamp(modulatedFreq, 20 Hz, sampleRate/2)
```

The LFO uses a sine wave for smooth frequency sweeps.

### Stereo Processing

- **Left/Right Phases**: 180° apart (opposite phase)
- **Independent LFOs**: Slight phase offset (90°) for stereo width
- **Independent Carrier Phases**: Creates wider, more immersive stereo field

### Frequency Relationships

**Musical Intervals** (input frequency × carrier frequency):
- **Octave** (2:1): Creates octave doubling with metallic timbre
- **Fifth** (3:2): Produces fifths with bell-like quality
- **Fourth** (4:3): Musical but dissonant
- **Tritone** (√2:1): Maximum dissonance
- **Random/Non-musical**: Inharmonic, alien tones

**Output Frequencies:**
```
f_out = f_input + f_carrier  (sum)
f_out = |f_input - f_carrier| (difference)
```

For a 440 Hz input with 440 Hz carrier:
- Sum: 880 Hz (octave up)
- Difference: 0 Hz (DC, effectively removed)

### Harmonic Content by Waveform

- **Sine**: Only fundamental frequency in carrier
- **Square**: Odd harmonics (1, 3, 5, 7...) - creates more complex spectra
- **Triangle**: Odd harmonics with faster decay - warmer than square
- **Sawtooth**: All harmonics (1, 2, 3, 4...) - richest, brightest sound

## Mathematical Background

Ring modulation in the time domain:
```
y(t) = x(t) × c(t)
```

Where:
- `x(t)` = input signal
- `c(t)` = carrier oscillator
- `y(t)` = output signal

In the frequency domain (for sinusoidal input and carrier):
```
X(f) = δ(f - f_in)
C(f) = δ(f - f_c)
Y(f) = 0.5 × [δ(f - (f_in + f_c)) + δ(f - (f_in - f_c))]
```

This shows that ring modulation produces sum and difference frequencies, suppressing the original carrier (hence "ring" modulation vs. amplitude modulation).

## Usage Tips

### Classic Bell Tone
- **Frequency**: 440-880 Hz
- **Waveform**: Sine
- **LFO Depth**: 0%
- **LFO Rate**: N/A
- **Mix**: 60-80%
- Pure, bell-like metallic tones

### Robotic Voice
- **Frequency**: 200-400 Hz
- **Waveform**: Square
- **LFO Depth**: 0-20%
- **LFO Rate**: 1-3 Hz
- **Mix**: 80-100%
- Classic robot/Dalek voice effect

### Subtle Shimmer
- **Frequency**: 1000-2000 Hz
- **Waveform**: Sine or Triangle
- **LFO Depth**: 10-30%
- **LFO Rate**: 0.5-2 Hz
- **Mix**: 20-40%
- Adds metallic sheen without overwhelming

### Tremolo Ring Mod
- **Frequency**: 4-8 Hz
- **Waveform**: Sine
- **LFO Depth**: 0%
- **LFO Rate**: N/A
- **Mix**: 60-80%
- Tremolo-like effect with metallic character

### Sci-Fi Sweep
- **Frequency**: 400-800 Hz
- **Waveform**: Sawtooth
- **LFO Depth**: 60-100%
- **LFO Rate**: 0.5-2 Hz
- **Mix**: 70-90%
- Sweeping, alien sound effects

### Metallic Chord
- **Frequency**: Set to musical interval (e.g., 660 Hz for fifth above 440 Hz)
- **Waveform**: Triangle
- **LFO Depth**: 0-10%
- **LFO Rate**: 0.3-1 Hz
- **Mix**: 40-60%
- Creates musical but metallic harmony

### Extreme Chaos
- **Frequency**: 100-400 Hz
- **Waveform**: Square
- **LFO Depth**: 80-100%
- **LFO Rate**: 5-10 Hz
- **Mix**: 90-100%
- Unpredictable, experimental textures

### Gentle Warble
- **Frequency**: 600-1000 Hz
- **Waveform**: Sine
- **LFO Depth**: 20-40%
- **LFO Rate**: 0.2-0.8 Hz
- **Mix**: 30-50%
- Subtle, evolving metallic color

## Musical Applications

### Lead Guitar
- Use low mix (20-40%) for metallic edge
- Octave or fifth relationships for tonal quality
- Subtle LFO for movement

### Rhythm Guitar
- Very low mix (10-30%)
- High carrier frequency (1500-3000 Hz) for shimmer
- No LFO or very slow

### Bass
- Low carrier frequency (40-100 Hz) for doubling
- Square or sawtooth for aggression
- Moderate mix (40-60%)

### Sound Design
- Experiment with non-musical frequencies
- High LFO depth and rate for evolving textures
- Square/sawtooth waveforms for harshness

## Technical Specifications

- **Latency**: None (real-time multiplication)
- **Carrier Range**: 20 Hz to 4000 Hz
- **Waveforms**: 4 (Sine, Square, Triangle, Sawtooth)
- **LFO**: Sine wave, 0.1-10 Hz
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Stereo**: True stereo with phase-offset carriers
- **Aliasing**: None (all operations sub-Nyquist when properly configured)
- **CPU Usage**: Low (simple oscillator + multiplication)

## Historical Context

Ring modulation was originally developed for telecommunications in the 1930s. It entered music through early electronic music composers like Karlheinz Stockhausen in the 1960s. The Moog Modular synthesizers popularized ring modulation for musicians, and it became a staple effect for creating sci-fi sounds, Dalek voices (Doctor Who), and experimental music textures. Guitar effects pedals implementing ring modulation include the Moog MF-102 and various boutique pedals.
