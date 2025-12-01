# Flanger Effect

## Description

The Flanger effect creates a characteristic "jet plane" or "whooshing" sound by mixing the dry signal with a short, modulated delay and feeding the output back into the input. The feedback creates comb filtering that sweeps through the frequency spectrum, producing metallic, swirling tones.

**Category**: Modulation
**ID**: `flanger`
**Display Name**: Flanger

## How It Works

The flanger operates by:
1. Taking the input signal
2. Adding feedback from the previous output
3. Passing it through a very short delay line (0.5-10ms)
4. Modulating the delay time with an LFO
5. Feeding the output back to create resonant peaks/notches
6. Mixing the result with the dry signal

The short delay times cause comb filtering - evenly spaced notches in the frequency spectrum. As the delay time sweeps, these notches move through the spectrum, creating the characteristic "whoosh" sound.

## Parameters

### Rate
- **ID**: `rate`
- **Range**: 0.05 Hz to 5.0 Hz
- **Default**: 0.5 Hz
- **Unit**: Hz
- **Description**: Speed of the sweep. Slow rates (0.1-0.5 Hz) create the classic jet plane sound. Faster rates (1-5 Hz) add vibrato-like movement.

### Depth
- **ID**: `depth`
- **Range**: 0% to 100%
- **Default**: 70%
- **Unit**: %
- **Description**: Intensity of the sweep. Higher values create more dramatic frequency changes and a wider sweeping range. Controls how much the delay time varies from the base delay.

### Delay
- **ID**: `delay`
- **Range**: 0.5 ms to 10.0 ms
- **Default**: 2.0 ms
- **Unit**: ms
- **Description**: Base delay time around which modulation occurs. Shorter delays (1-3ms) produce metallic, through-zero-like sounds. Longer delays (5-10ms) approach chorus-like character.

### Feedback
- **ID**: `feedback`
- **Range**: -90% to +90%
- **Default**: 50%
- **Unit**: %
- **Description**: Amount of output fed back to the input. Positive values create resonant peaks in the spectrum. Negative values invert the phase, creating resonant notches. Higher absolute values increase effect intensity. Zero produces a simple modulated delay without the characteristic flanger resonance.

### Mix
- **ID**: `mix`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Balance between dry (unprocessed) and wet (flanged) signal.

## Implementation Details

### Signal Flow

**Mono Processing:**
```
Input → (+Feedback) → Soft Clip → Delay Line → LFO Modulation → Wet Signal
                                                                      ↓
                                                         Mix with Dry → Output
                                                                      ↓
                                                            Store Feedback
```

**Stereo Processing:**
```
Similar to mono but with independent delay lines and phase-offset LFOs for L/R channels
```

### LFO Configuration

- **Waveform**: Triangle wave (creates linear sweep characteristic of analog flangers)
- **Update**: Frequency updated per buffer based on Rate parameter
- **Stereo**: Right channel LFO is 180° out of phase (0.5 phase offset) for stereo width

### Delay Modulation

- **Minimum Delay**: 0.5 ms (fixed)
- **Base Delay**: User-adjustable (0.5-10 ms)
- **Modulation Range**: From minimum to base delay, scaled by depth
- **Interpolation**: Cubic interpolation for smooth, artifact-free modulation
- **Buffer Size**: 20 ms (2x maximum delay for safety)

### Delay Time Calculation

```java
lfoValue = (lfo.tick() + 1.0) * 0.5  // Convert -1..1 to 0..1
modulationRange = baseDelay - minDelay
delaySamples = minDelay + lfoValue * modulationRange * depth + baseDelay * (1 - depth)
```

This ensures the delay sweeps between minimum and base delay based on depth.

### Feedback Loop with Soft Clipping

To prevent feedback runaway, a soft clipping function is applied:

```java
if (x > 1.0) return 1.0 - 1.0 / (x + 1.0)
if (x < -1.0) return -1.0 + 1.0 / (-x + 1.0)
return x
```

This provides smooth saturation at ±1.0, maintaining stability even with extreme feedback settings.

### Comb Filtering

The feedback creates a comb filter with notches at:
```
f_notch = (2n + 1) / (2 * delayTime)
```

As the delay time sweeps, these notches move through the spectrum, creating the flanger effect.

## Key Differences from Chorus

| Feature | Flanger | Chorus |
|---------|---------|--------|
| Delay Time | 0.5-10ms | 7-20ms |
| Voices | Single | Multiple (4) |
| Feedback | Yes (creates resonance) | No |
| LFO Wave | Triangle | Sine |
| Character | Metallic, jet-plane | Lush, ensemble |
| Frequency Effect | Pronounced sweeping | Subtle detuning |

## Usage Tips

### Classic Jet Flanger
- **Rate**: 0.2-0.4 Hz
- **Depth**: 80-100%
- **Delay**: 2-3 ms
- **Feedback**: 50-70%
- **Mix**: 50-70%
- Slow, deep sweep with strong resonance

### Subtle Flange
- **Rate**: 0.3-0.6 Hz
- **Depth**: 40-60%
- **Delay**: 1-2 ms
- **Feedback**: 20-40%
- **Mix**: 30-50%
- Adds movement without overwhelming the signal

### Through-Zero Flanger
- **Rate**: 0.1-0.3 Hz
- **Depth**: 90-100%
- **Delay**: 0.5-1 ms
- **Feedback**: -70 to -90%
- **Mix**: 60-80%
- Extreme, hollow sound with negative feedback

### Fast Warbly
- **Rate**: 2-4 Hz
- **Depth**: 60-80%
- **Delay**: 3-5 ms
- **Feedback**: 30-50%
- **Mix**: 50-60%
- Vibrato-like modulation with flanger character

### Resonant Sweep
- **Rate**: 0.15-0.25 Hz
- **Depth**: 70-90%
- **Delay**: 2-4 ms
- **Feedback**: 70-90%
- **Mix**: 60-80%
- Very slow, highly resonant sweep for dramatic effect

### Chorus-Flanger Hybrid
- **Rate**: 0.5-0.8 Hz
- **Depth**: 50-70%
- **Delay**: 6-8 ms
- **Feedback**: 20-30%
- **Mix**: 40-60%
- Combines flanger resonance with chorus-like depth

## Technical Specifications

- **Latency**: Minimum 0.5ms, maximum 10ms based on delay setting
- **Voices**: 1 per channel with feedback
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Interpolation**: Cubic (4-point) for alias-free modulation
- **Stereo**: True stereo with independent delay lines and phase-offset LFOs
- **Stability**: Soft clipping prevents feedback runaway
