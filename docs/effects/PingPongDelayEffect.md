# PingPongDelayEffect

## Overview

PingPongDelayEffect creates a stereo delay that bounces between left and right channels, producing a wide, immersive stereo field. Each repeat alternates between channels, creating the characteristic "ping-pong" effect.

**Category:** Delay
**ID:** `pingpong`
**Display Name:** Ping-Pong Delay

## Description

This effect uses cross-feedback between left and right delay lines to create alternating L/R repeats. The result is a spacious, wide stereo image that moves the echoes across the sound field. Perfect for creating movement and width in mixes.

## Parameters

### Time
- **ID:** `time`
- **Range:** 50 ms to 1000 ms
- **Default:** 375 ms
- **Unit:** ms (milliseconds)
- **Description:** Delay time for each bounce. This is the time between each ping-pong repeat.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 90%
- **Default:** 50%
- **Unit:** %
- **Description:** Amount fed back into the delay. Higher values create more bounces before the signal fades out.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 40%
- **Unit:** %
- **Description:** Balance between dry and delayed signals. 0% is dry only, 100% is wet only.

### Width
- **ID:** `width`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Stereo width control. 100% creates full ping-pong effect, 0% collapses to mono (centered delay).

### Tone
- **ID:** `tone`
- **Range:** 1000 Hz to 12000 Hz
- **Default:** 5000 Hz
- **Unit:** Hz
- **Description:** High frequency cutoff for the delays. Lower values create darker, warmer repeats similar to analog delays.

### Offset
- **ID:** `offset`
- **Range:** -50% to +50%
- **Default:** 0%
- **Unit:** %
- **Description:** Time offset between left and right channels. 0% creates symmetric ping-pong. Positive values delay the right channel more, negative values delay the left channel more.

## DSP Components

### Delay Lines
- **Left Channel:** `DelayLine delayLineL`
- **Right Channel:** `DelayLine delayLineR`
- Maximum capacity: 2 seconds each
- Uses cubic interpolation for smooth, artifact-free delays

### Tone Filters
- **Left Channel:** `BiquadFilter toneFilterL` - Lowpass, Q=0.707
- **Right Channel:** `BiquadFilter toneFilterR` - Lowpass, Q=0.707
- Applied to feedback path for progressive high-frequency rolloff

### Cross-Feedback State
- `float feedbackL` - Stores left channel feedback sample
- `float feedbackR` - Stores right channel feedback sample
- Cross-fed: left delay gets right's feedback, right delay gets left's feedback

## Implementation Details

### Signal Flow

#### Cross-Feedback Mechanism:
```
Input (Mono Sum) → Left Delay Line ← Right Feedback
                ↓
         Left Output → Tone Filter → Next Left Feedback
                                  ↓
                          Right Delay Line ← Left Feedback
                                  ↓
                          Right Output → Tone Filter → Next Right Feedback
```

The key to the ping-pong effect is the cross-feeding:
- Left delay writes: `monoInput + feedbackR * feedback`
- Right delay writes: `feedbackL * feedback` (note: only feedback, no new input)

This creates alternating bounces where each repeat switches sides.

#### Stereo Processing:
1. Sum input channels to mono: `monoIn = (dryL + dryR) * 0.5`
2. Write mono input + opposite channel's feedback to each delay line
3. Read delayed signals using cubic interpolation
4. Apply tone filters to both channels
5. Store filtered outputs for next iteration's cross-feedback
6. Apply width control using mid-side processing
7. Mix with dry signals

### Width Control

Uses mid-side stereo processing:
```
mid = (wetL + wetR) * 0.5
side = (wetL - wetR) * 0.5

wetL_final = mid + side * width
wetR_final = mid - side * width
```

When width = 0%, side component is eliminated, resulting in mono.
When width = 100%, full stereo separation is maintained.

### Offset Control

Delay times are adjusted per channel:
```
timeMsL = timeMs * (1.0 - offset * 0.5)
timeMsR = timeMs * (1.0 + offset * 0.5)
```

This creates asymmetric ping-pong patterns and can add rhythmic interest.

### Mono Input Handling

When processing mono input:
1. Expand internally to stereo (duplicate input to both channels)
2. Process full stereo ping-pong algorithm
3. Sum back to mono for output: `(outL + outR) * 0.5`

## Usage Tips

### Classic Ping-Pong
- **Time:** 250-500 ms
- **Feedback:** 50-70%
- **Mix:** 40-60%
- **Width:** 100%
- **Tone:** 4000-6000 Hz
- **Offset:** 0%

### Tight Bouncing Delays
- **Time:** 100-200 ms
- **Feedback:** 40-60%
- **Mix:** 30-50%
- **Width:** 80-100%
- **Tone:** 6000-8000 Hz

### Ambient Stereo Wash
- **Time:** 400-800 ms
- **Feedback:** 60-80%
- **Mix:** 50-80%
- **Width:** 100%
- **Tone:** 3000-4000 Hz (darker)
- **Offset:** +10% to +30% (asymmetric)

### Pseudo-Doubling
- **Time:** 50-80 ms
- **Feedback:** 20-30%
- **Mix:** 25-40%
- **Width:** 60-80%
- **Tone:** 8000+ Hz

### Narrow Ping-Pong
- **Time:** 300-400 ms
- **Feedback:** 50-60%
- **Mix:** 40-50%
- **Width:** 40-60% (less extreme)
- **Offset:** 0%

## Technical Specifications

- **Maximum Delay Time:** 2000 ms per channel
- **Interpolation:** Cubic (4-point for smooth delays)
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **Stereo Processing:** True stereo with cross-feedback
- **CPU Usage:** Low to moderate (2 delay lines + 2 filters)

## Advanced Techniques

### Rhythmic Patterns
Combine with different offset values to create polyrhythmic patterns:
- **Offset +25%:** Creates 4:3 ratio between channels
- **Offset -15%:** Subtle shuffle feel

### Stereo Widening
Use very short times (50-100 ms) with low feedback and high width for stereo enhancement without obvious delay.

### Dual Delay Feel
Set offset to +30-50% to create two distinct delay times bouncing between channels.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/PingPongDelayEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize delay lines and filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono input processing
- `onProcessStereo(...)` - Stereo cross-feedback processing
- `onReset()` - Clear all buffers and state

**Key Features:**
- Cross-feedback between channels creates ping-pong effect
- Cubic interpolation for smooth delays
- Mid-side processing for width control
- Separate tone filtering per channel

## See Also

- [DelayEffect](DelayEffect.md) - Standard digital delay
- [MultiTapDelayEffect](MultiTapDelayEffect.md) - Rhythmic multi-tap patterns
- [TapeEchoEffect](TapeEchoEffect.md) - Vintage tape echo with modulation
