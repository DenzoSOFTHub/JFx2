# Tremolo Effect

## Description

The Tremolo effect modulates the volume (amplitude) of the signal using an LFO (Low Frequency Oscillator). This creates a rhythmic pulsing or wavering effect, commonly found in vintage guitar amplifiers. Unlike vibrato which modulates pitch, tremolo creates volume changes.

**Category**: Modulation
**ID**: `tremolo`
**Display Name**: Tremolo

## How It Works

The tremolo operates by:
1. Generating an LFO waveform at the specified rate
2. Converting the LFO output to a gain multiplier
3. Multiplying the input signal by this time-varying gain
4. The result is amplitude modulation - the volume goes up and down rhythmically

The shape of the LFO waveform determines the character of the tremolo:
- **Sine**: Smooth, natural pulsing
- **Triangle**: Linear volume changes
- **Square**: Choppy, on/off effect
- **Random**: Unpredictable, experimental modulation

## Parameters

### Rate
- **ID**: `rate`
- **Range**: 0.5 Hz to 15.0 Hz
- **Default**: 5.0 Hz
- **Unit**: Hz
- **Description**: Speed of volume modulation. Slow rates (0.5-2 Hz) create subtle pulsing. Medium rates (3-7 Hz) are classic tremolo. Fast rates (8-15 Hz) create helicopter-like effects.

### Depth
- **ID**: `depth`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Intensity of volume changes. At 0%, no effect is applied. At 100%, the volume varies maximally (creates more dramatic pulsing). Higher values create more pronounced volume swells.

### Waveform
- **ID**: `waveform`
- **Type**: Choice
- **Options**:
  - 0: Sine
  - 1: Triangle
  - 2: Square
  - 3: Random
- **Default**: 0 (Sine)
- **Description**: Shape of modulation:
  - **Sine**: Smooth, rounded pulsing - most natural and musical
  - **Triangle**: Linear volume ramps - symmetric rise and fall
  - **Square**: Choppy, abrupt changes - rhythmic on/off
  - **Random**: Experimental, unpredictable volume changes

## Implementation Details

### Signal Flow

**Mono Processing:**
```
Input → LFO → Gain Calculation → Multiply → Output
```

**Stereo Processing:**
```
Input L → LFO L (phase 0°) → Gain L → Multiply → Output L
Input R → LFO R (phase 90°) → Gain R → Multiply → Output R
```

### LFO Configuration

- **Waveforms**: Sine, Triangle, Square, Random (user-selectable)
- **Update**: LFO frequency and waveform updated per buffer
- **Stereo Offset**: Right channel LFO has 0.25 phase offset (90 degrees) for stereo width

### Gain Calculation

The LFO output (-1 to +1) is converted to a gain multiplier:

```java
lfoValue = -1 to +1
gain = 1.0 - depth * 0.5 * (1.0 - lfoValue)
```

This formula ensures:
- When `lfoValue = -1`: `gain = 1 - depth` (minimum gain)
- When `lfoValue = 0`: `gain = 1 - depth * 0.5` (middle)
- When `lfoValue = +1`: `gain = 1` (maximum gain)

The gain modulates below unity (1.0), creating volume reduction rather than boosting. This is typical of traditional tremolo circuits.

### Depth Scaling

- At 0% depth: gain is always 1.0 (no effect)
- At 50% depth: gain varies from 0.75 to 1.0
- At 100% depth: gain varies from 0.5 to 1.0

This prevents complete silence even at maximum depth, maintaining signal presence.

### Stereo Processing

In stereo mode, the left and right channels have independent LFOs with a 90-degree phase offset. This creates:
- Enhanced stereo width
- Alternating pulsing between left and right
- More immersive tremolo effect
- Classic "ping-pong" feeling at moderate rates

## Waveform Characteristics

### Sine Wave
- **Character**: Smooth, natural
- **Best For**: Subtle ambiance, surf music, clean tones
- **Description**: The most common tremolo waveform. Creates gentle, rounded volume swells.

### Triangle Wave
- **Character**: Linear, symmetric
- **Best For**: Rhythmic parts, experimental sounds
- **Description**: Volume ramps up and down at constant rate. More pronounced than sine but smoother than square.

### Square Wave
- **Character**: Choppy, staccato
- **Best For**: Rhythmic effects, synth-like sounds
- **Description**: Abrupt on/off switching. Creates a stuttering, gating-like effect.

### Random
- **Character**: Unpredictable, chaotic
- **Best For**: Experimental, ambient textures
- **Description**: Random volume fluctuations. Creates organic, non-repetitive modulation.

## Usage Tips

### Classic Surf Tremolo
- **Rate**: 4-6 Hz
- **Depth**: 60-80%
- **Waveform**: Sine
- Smooth, pronounced pulsing typical of vintage amps

### Subtle Enhancement
- **Rate**: 3-5 Hz
- **Depth**: 20-40%
- **Waveform**: Sine
- Adds gentle movement without being obvious

### Helicopter Effect
- **Rate**: 10-15 Hz
- **Depth**: 70-100%
- **Waveform**: Sine or Triangle
- Fast, intense pulsing

### Rhythmic Tremolo
- **Rate**: 2-4 Hz (sync to song tempo)
- **Depth**: 50-70%
- **Waveform**: Triangle or Square
- Creates rhythmic pulsing that follows the music

### Stutter/Gate
- **Rate**: 6-10 Hz
- **Depth**: 80-100%
- **Waveform**: Square
- Choppy, on/off effect

### Ambient Swell
- **Rate**: 0.5-2 Hz
- **Depth**: 40-60%
- **Waveform**: Sine
- Very slow, gentle volume swells

### Experimental
- **Rate**: 1-5 Hz (variable)
- **Depth**: 50-80%
- **Waveform**: Random
- Unpredictable, organic modulation

## Technical Specifications

- **Latency**: None (real-time amplitude modulation)
- **Processing**: Gain multiplication per sample
- **Sample Rate**: Adapts to system sample rate
- **Internal Precision**: 32-bit float
- **Stereo**: True stereo with phase-offset LFOs (90° separation)
- **Waveforms**: 4 types (Sine, Triangle, Square, Random)
- **CPU Usage**: Very low (simple multiplication)

## Tremolo vs. Vibrato

| Feature | Tremolo | Vibrato |
|---------|---------|---------|
| Modulates | Amplitude (volume) | Pitch (frequency) |
| Perception | Volume pulsing | Pitch wobble |
| Implementation | Gain multiplication | Delay modulation |
| CPU Usage | Very low | Moderate |
| Common Use | Rhythm guitar, surf | Lead guitar, expression |
