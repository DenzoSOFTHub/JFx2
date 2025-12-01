# Vibrato Effect

## Description

The Vibrato effect modulates the pitch of the signal using an LFO (Low Frequency Oscillator). Unlike tremolo which modulates volume, vibrato creates a pitch wobble effect by varying the delay time of the signal. This simulates the natural vibrato technique used by vocalists and instrumentalists.

**Category**: Modulation
**ID**: `vibrato`
**Display Name**: Vibrato

## How It Works

The vibrato operates by:
1. Writing the input signal to a delay line
2. Using an LFO to modulate the read position in the delay line
3. When the delay is shorter, the pitch goes up (reading faster through the buffer)
4. When the delay is longer, the pitch goes down (reading slower)
5. This creates continuous pitch variation without changing playback speed

The optional rise time parameter allows the vibrato to gradually increase from zero to full depth, simulating the natural onset of finger vibrato.

## Parameters

### Rate
- **ID**: `rate`
- **Range**: 0.5 Hz to 10.0 Hz
- **Default**: 5.0 Hz
- **Unit**: Hz
- **Description**: Speed of the vibrato. For guitar, typical rates are 4-7 Hz, matching natural finger vibrato. Slower rates (1-3 Hz) create exaggerated, Leslie-like wobble. Faster rates (8-10 Hz) approach trill-like effects.

### Depth
- **ID**: `depth`
- **Range**: 0 to 100 cents
- **Default**: 30 cents
- **Unit**: cents (1/100 of a semitone)
- **Description**: Amount of pitch variation. Subtle vibrato uses 10-20 cents. Moderate vibrato uses 20-40 cents. Wide vibrato uses 50-100 cents. Note: 100 cents = 1 semitone.

### Waveform
- **ID**: `waveform`
- **Type**: Choice
- **Options**:
  - 0: Sine
  - 1: Triangle
- **Default**: 0 (Sine)
- **Description**: Modulation shape:
  - **Sine**: Smooth, natural vibrato - most musical
  - **Triangle**: More pronounced, linear pitch changes

### Rise
- **ID**: `rise`
- **Range**: 0 ms to 1000 ms
- **Default**: 0 ms (instant)
- **Unit**: ms
- **Description**: Time for vibrato to reach full depth. Simulates natural finger vibrato where the player gradually applies the effect. At 0ms, the effect is immediate. At 200-500ms, it gradually fades in like natural vibrato.

## Implementation Details

### Signal Flow

```
Input → Delay Line → LFO Modulated Read Position → Cubic Interpolation → Output
                              ↑
                         Rise Envelope
```

### LFO Configuration

- **Waveforms**: Sine (smooth) or Triangle (pronounced)
- **Update**: Per-sample modulation for smooth pitch changes
- **Stereo**: Mono LFO (both channels use same modulation for pitch coherence)

### Delay Time Calculation

The depth in cents is converted to delay time modulation:

```java
maxDelayMs = depthCents / 17.31  // Approximation: cents ≈ delay_ratio * 1731
baseDelayMs = maxDelayMs + 1.0   // Center point
modulatedDelayMs = baseDelayMs + lfoValue * maxDelayMs * riseGain
```

This approximation works well for small pitch shifts:
- 17 cents ≈ 1ms delay variation
- 34 cents ≈ 2ms
- 50 cents ≈ 3ms

### Rise Time Envelope

The rise parameter creates a linear envelope:

```java
if (riseCounter < riseSamples) {
    riseGain = riseCounter / riseSamples  // 0 to 1
    riseCounter++
} else {
    riseGain = 1.0
}
```

This multiplies the depth, starting at 0 and ramping to full depth over the rise time.

### Interpolation

Cubic (4-point) interpolation is used when reading from the delay line:
- Provides smooth, artifact-free pitch shifting
- Eliminates aliasing and stepping artifacts
- Essential for musical vibrato quality

### Buffer Size

- **Maximum Delay**: 20 ms
- **Typical Range**: 1-6 ms (for 0-100 cents)
- **Base Delay**: Automatically calculated from depth setting

## Vibrato vs. Other Modulation Effects

| Feature | Vibrato | Tremolo | Chorus |
|---------|---------|---------|--------|
| Modulates | Pitch | Volume | Pitch (multi-voice) |
| Perception | Pitch wobble | Volume pulse | Ensemble sound |
| Delay Used | Yes | No | Yes (multiple) |
| Voices | 1 | N/A | 4 |
| Mix Control | No (100% wet) | No | Yes |

## Usage Tips

### Natural Guitar Vibrato
- **Rate**: 5-6 Hz
- **Depth**: 20-30 cents
- **Waveform**: Sine
- **Rise**: 200-400 ms
- Simulates realistic finger vibrato

### Subtle Vibrato
- **Rate**: 4-5 Hz
- **Depth**: 10-20 cents
- **Waveform**: Sine
- **Rise**: 0-100 ms
- Gentle pitch movement for enhancement

### Wide Vibrato
- **Rate**: 5-7 Hz
- **Depth**: 40-60 cents
- **Waveform**: Sine
- **Rise**: 0 ms
- Pronounced, expressive pitch variation

### Leslie Simulation
- **Rate**: 1-3 Hz
- **Depth**: 30-50 cents
- **Waveform**: Sine
- **Rise**: 0 ms
- Slow, deep pitch wobble

### Trill Effect
- **Rate**: 8-10 Hz
- **Depth**: 50-100 cents
- **Waveform**: Triangle
- **Rise**: 0 ms
- Fast alternation approaching a trill

### Expressive Lead
- **Rate**: 5-6 Hz
- **Depth**: 25-40 cents
- **Waveform**: Sine
- **Rise**: 300-600 ms
- Long rise time for dynamic expression

### Vocal Simulation
- **Rate**: 5-7 Hz
- **Depth**: 15-25 cents
- **Waveform**: Sine
- **Rise**: 100-300 ms
- Mimics vocal vibrato characteristics

## Technical Details

### Pitch Shift Accuracy

The relationship between delay and pitch shift:
```
pitch_ratio = 1 / (1 + delay_ratio)
cents = 1200 * log2(pitch_ratio)
```

For small variations, the linear approximation used (cents ≈ delay * 17.31) is accurate within 1-2 cents.

### Latency

- **Base Latency**: 1-6 ms (depends on depth setting)
- **Total Latency**: Basedelay + modulation range
- **Maximum**: ~7 ms at 100 cents depth

### CPU Usage

- **Delay Line**: Single delay buffer per channel
- **Interpolation**: Cubic (4-point Lagrange)
- **LFO**: Efficient sine/triangle generation
- **Overall**: Moderate CPU usage

## Technical Specifications

- **Latency**: Variable (1-7 ms based on depth)
- **Voices**: 1 per channel
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Interpolation**: Cubic (4-point) for smooth pitch shifting
- **Stereo**: True stereo (same pitch modulation for coherence)
- **Rise Time**: Linear envelope, 0-1000 ms
- **Pitch Range**: 0-100 cents (1 semitone)
