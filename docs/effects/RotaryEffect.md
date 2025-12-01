# Rotary Speaker Effect

## Description

The Rotary Speaker effect emulates the sound of a Leslie rotating speaker cabinet, most famously used with Hammond organs. It simulates separate horn (treble) and drum (bass) rotors spinning at different speeds, creating complex amplitude and pitch modulation through the Doppler effect. The result is a rich, three-dimensional swirling sound.

**Category**: Modulation
**ID**: `rotary`
**Display Name**: Rotary

## How It Works

The rotary speaker operates by:
1. Splitting the input signal into bass and treble bands using a crossover filter (800 Hz)
2. Processing the treble through a "horn" rotor simulation
3. Processing the bass through a "drum" rotor simulation
4. Each rotor creates:
   - **Amplitude modulation**: Volume varies as the speaker rotates toward/away from listener
   - **Doppler shift**: Pitch varies due to speaker motion (approaching = higher pitch, receding = lower pitch)
5. Simulating acceleration/deceleration when changing speeds
6. Combining the processed signals and mixing with dry signal

The horn rotor spins faster and has more pronounced Doppler effect, while the drum rotor is slower with gentler modulation, mimicking the real Leslie cabinet mechanics.

## Parameters

### Speed
- **ID**: `speed`
- **Type**: Choice
- **Options**:
  - 0: Slow
  - 1: Fast
  - 2: Brake
- **Default**: 0 (Slow)
- **Description**: Rotor speed mode:
  - **Slow**: Gentle rotation - Horn: 0.8 Hz, Drum: 0.7 Hz
  - **Fast**: Intense rotation - Horn: 6.7 Hz, Drum: 5.8 Hz
  - **Brake**: Rotors gradually stop (with realistic deceleration)

### Horn Level
- **ID**: `horn`
- **Range**: 0% to 100%
- **Default**: 100%
- **Unit**: %
- **Description**: Level of the treble (horn) rotor. This rotor handles frequencies above 800 Hz and provides the characteristic Leslie high-frequency swirl. At 100%, full horn intensity. At 0%, horn is bypassed.

### Drum Level
- **ID**: `drum`
- **Range**: 0% to 100%
- **Default**: 70%
- **Unit**: %
- **Description**: Level of the bass (drum) rotor. This rotor handles frequencies below 800 Hz and adds warmth and movement to low frequencies. Typically set slightly lower than horn for balanced sound.

### Doppler
- **ID**: `doppler`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Amount of pitch shift from Doppler effect. Higher values create more pronounced pitch wobble as rotors spin. At 0%, only amplitude modulation occurs. At 100%, maximum pitch variation (±2ms for horn, ±0.5ms for drum).

### Mix
- **ID**: `mix`
- **Range**: 0% to 100%
- **Default**: 100%
- **Unit**: %
- **Description**: Blend between dry (unprocessed) and rotary (processed) sound. At 100%, pure rotary cabinet sound. Lower values allow blending with dry signal.

## Implementation Details

### Signal Flow

```
Input → Crossover Filter (800 Hz) → Bass/Treble Split
                                          ↓
                  ┌───────────────────────┴──────────────────────┐
                  ↓                                               ↓
            Drum Rotor (Bass)                              Horn Rotor (Treble)
        Phase → Amplitude Mod                         Phase → Amplitude Mod
             → Doppler Delay                               → Doppler Delay
             → Level Control                               → Level Control
                  ↓                                               ↓
                  └───────────────────┬───────────────────────────┘
                                      ↓
                              Mix with Dry → Output
```

### Crossover Filter

- **Type**: Butterworth 2nd order (Biquad)
- **Frequency**: 800 Hz
- **Lowpass**: Feeds drum rotor
- **Highpass**: Feeds horn rotor
- **Q**: 0.707 (maximally flat response)

### Rotor Speeds

**Horn (Treble) Rotor:**
- Slow: 0.8 Hz (48 RPM)
- Fast: 6.7 Hz (402 RPM)

**Drum (Bass) Rotor:**
- Slow: 0.7 Hz (42 RPM)
- Fast: 5.8 Hz (348 RPM)

These values approximate real Leslie cabinet speeds.

### Acceleration/Deceleration

Speed changes use exponential smoothing:
```java
accelCoeff = 0.9995  // Very slow acceleration
currentSpeed += (targetSpeed - currentSpeed) * (1 - accelCoeff)
```

This creates realistic motor-like acceleration, taking several seconds to change between slow and fast speeds.

### Rotor Phase

Each rotor maintains a phase value (0 to 2π):
```java
phaseInc = currentSpeed * 2π / sampleRate
phase += phaseInc
if (phase > 2π) phase -= 2π
```

Stereo: Left and right rotors are 180° out of phase for stereo width.

### Amplitude Modulation

**Horn (more pronounced):**
```java
hornAmp = 0.5 + 0.5 * sin(hornPhase)  // Range: 0 to 1
```

**Drum (gentler):**
```java
drumAmp = 0.6 + 0.4 * sin(drumPhase)  // Range: 0.2 to 1
```

The drum has less amplitude variation for a more subtle effect.

### Doppler Delay Modulation

**Horn (more Doppler):**
```java
hornDelayMs = 1.0 + doppler * 2.0 * sin(hornPhase)  // Range: varies by doppler setting
```

**Drum (less Doppler):**
```java
drumDelayMs = 0.5 + doppler * 0.5 * sin(drumPhase)  // Range: varies by doppler setting
```

The horn has 4x more Doppler effect than the drum, creating the characteristic Leslie pitch wobble.

### Stereo Processing

- **Left/Right Phase Offset**: 180° (opposite sides of rotation)
- **Independent Delay Lines**: Separate for L/R and horn/drum (4 total)
- **Phase Distribution**: Creates wide, immersive stereo field

## Rotary Cabinet Physics

### Doppler Effect

When a sound source moves:
- **Approaching**: Frequency increases (pitch goes up)
- **Receding**: Frequency decreases (pitch goes down)

The delay modulation simulates this by varying the effective playback speed.

### Amplitude Variation

As the rotor rotates:
- **Facing listener**: Maximum volume
- **Facing away**: Minimum volume

The amplitude modulation simulates the directional speaker pattern.

### Two-Rotor System

Real Leslie cabinets have:
- **Horn rotor**: High frequencies, faster speed, more directional
- **Drum rotor**: Low frequencies, slower speed, less directional

This effect accurately models both.

## Usage Tips

### Classic Leslie Organ
- **Speed**: Slow or Fast (switch during performance)
- **Horn**: 100%
- **Drum**: 70-80%
- **Doppler**: 50-70%
- **Mix**: 100%
- Authentic Hammond organ + Leslie sound

### Slow Swirl
- **Speed**: Slow
- **Horn**: 90-100%
- **Drum**: 60-80%
- **Doppler**: 40-60%
- **Mix**: 80-100%
- Gentle, hypnotic rotation for ballads

### Fast Spin
- **Speed**: Fast
- **Horn**: 100%
- **Drum**: 70%
- **Doppler**: 60-80%
- **Mix**: 90-100%
- Intense, swirling effect for dramatic moments

### Guitar Leslie
- **Speed**: Slow to Fast
- **Horn**: 80-100%
- **Drum**: 50-70%
- **Doppler**: 40-60%
- **Mix**: 70-90%
- Blended rotary for guitar (slightly less intense than organ)

### Subtle Enhancement
- **Speed**: Slow
- **Horn**: 60-80%
- **Drum**: 40-60%
- **Doppler**: 30-50%
- **Mix**: 40-60%
- Adds dimension without overwhelming the signal

### Speed Ramps
Start with Slow, switch to Fast during chorus or solo, then Brake at the end. This dynamic control is part of the classic Leslie playing technique.

### Bass Emphasis
- **Speed**: Slow
- **Horn**: 50-70%
- **Drum**: 90-100%
- **Doppler**: 40-60%
- **Mix**: 80-100%
- Emphasizes low-frequency rotation

### Doppler-Only
- **Speed**: Fast
- **Horn**: 100%
- **Drum**: 70%
- **Doppler**: 80-100%
- **Mix**: 100%
- Maximum pitch wobble for extreme effect

### Minimal Doppler
- **Speed**: Slow
- **Horn**: 100%
- **Drum**: 70%
- **Doppler**: 10-30%
- **Mix**: 90-100%
- Emphasizes amplitude modulation over pitch shift

## Technical Specifications

- **Latency**: 0.5-5 ms (depends on Doppler setting)
- **Crossover**: 800 Hz, 2nd order Butterworth
- **Rotors**: 2 (horn and drum) with independent speeds
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Interpolation**: Cubic (4-point) for Doppler delay lines
- **Stereo**: True stereo with phase-offset rotors
- **Acceleration**: Realistic motor-like speed ramping
- **CPU Usage**: Moderate (4 delay lines + 4 filters + modulation)

## Historical Context

The Leslie speaker was invented by Don Leslie in the 1940s for use with Hammond organs. The rotating speaker creates a unique three-dimensional sound that cannot be replicated by simple chorus or vibrato effects. It became iconic in rock, jazz, and gospel music, used by artists like Jon Lord, Gregg Allman, and many others. This digital emulation captures the essential characteristics of the classic Leslie 122 and Leslie 147 cabinets.
