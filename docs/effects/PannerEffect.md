# Panner Effect

## Overview

PannerEffect is an auto-panning effect that automatically moves the audio signal between left and right channels using an LFO (Low Frequency Oscillator). It creates stereo movement effects ranging from subtle animation to dramatic ping-pong panning.

**Category:** Modulation
**ID:** `panner`
**Display Name:** Panner

## Description

The Auto-Panner uses an LFO to continuously modulate the stereo position of the audio signal. Multiple waveform shapes allow for different movement characteristics, from smooth sine sweeps to abrupt square wave switching. A smoothing parameter helps prevent clicks when using harder waveforms.

This effect is particularly useful for adding motion and interest to static sounds, creating psychedelic effects, or producing rhythmic stereo patterns synchronized to tempo.

## Parameters

### Rate
- **ID:** `rate`
- **Range:** 0.1 Hz to 10 Hz
- **Default:** 1 Hz
- **Unit:** Hz
- **Description:** Speed of the panning movement. Slow rates (0.1-0.5 Hz) create subtle, ambient movement. Medium rates (1-3 Hz) produce noticeable panning. Fast rates (5-10 Hz) approach tremolo-like effects with rapid left-right switching.

### Depth
- **ID:** `depth`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Width of the pan sweep. At 100%, the signal moves fully from left to right. Lower values keep the signal closer to center, creating subtler movement. At 0%, no panning occurs.

### Waveform
- **ID:** `waveform`
- **Type:** Choice
- **Options:**
  - `Sine` (0) - Smooth, natural movement with acceleration at center and deceleration at extremes
  - `Triangle` (1) - Linear sweep with constant speed across the stereo field
  - `Square` (2) - Hard switching between left and right with no intermediate positions
- **Default:** Sine
- **Description:** Shape of the panning movement. Sine provides the most natural feel. Triangle is more mechanical but predictable. Square creates dramatic ping-pong effects.

### Smooth
- **ID:** `smooth`
- **Range:** 0% to 100%
- **Default:** 20%
- **Unit:** %
- **Description:** Smooths transitions between pan positions. Essential for square wave to prevent clicks. Also useful for triangle to soften the direction changes. Has minimal effect on sine wave which is already smooth.

## DSP Components

### LFO (Low Frequency Oscillator)
- **Type:** LFO class with selectable waveforms
- **Waveforms:** Sine, Triangle, Square
- **Output Range:** -1 to +1
- **Purpose:** Generates the modulation signal for pan position

### Smoothing Filter
- **Type:** One-pole lowpass (exponential smoothing)
- **Coefficient:** 0.001 to 0.051 (based on Smooth parameter)
- **Purpose:** Prevents clicks and pops by smoothing abrupt pan changes

## Implementation Details

### Signal Flow

```
Stereo Input -> Sum to Mono (for panning reference)
                        |
                        v
                    LFO Tick
                        |
                        v
                 Apply Depth Scaling
                        |
                        v
                 Smoothing Filter
                        |
                        v
              Constant Power Panning
                        |
                        v
              Cross-Fade L/R Channels
                        |
                        v
                 Stereo Output
```

### LFO Waveform Selection

The LFO waveform is selected by index:
```java
Waveform indexToWaveform(int index) {
    return switch (index) {
        case 1 -> LFO.Waveform.TRIANGLE;
        case 2 -> LFO.Waveform.SQUARE;
        default -> LFO.Waveform.SINE;
    };
}
```

### Pan Position Calculation

1. **LFO Output**: Get raw LFO value (-1 to +1)
2. **Depth Scaling**: Multiply by depth parameter
3. **Smoothing**: Apply one-pole lowpass filter
   ```java
   lastPan = lastPan + smoothCoef * (targetPan - lastPan);
   ```

### Constant Power Panning

Converts pan position to left/right gains:
```java
angle = (pan + 1.0) * 0.25 * PI;  // 0 to PI/2
gainL = cos(angle);
gainR = sin(angle);
```

At center (pan=0): gainL = gainR = 0.707 (equal power)
At left (pan=-1): gainL = 1.0, gainR = 0.0
At right (pan=+1): gainL = 0.0, gainR = 1.0

### Stereo Cross-Fade Algorithm

Rather than simply applying pan gains to each channel independently, the effect cross-fades content between channels:

```java
if (pan < 0) {
    // Panning left: move right content to left
    float blend = -pan;
    outputL = inputL + inputR * blend * 0.5;
    outputR = inputR * (1.0 - blend * 0.5);
} else {
    // Panning right: move left content to right
    float blend = pan;
    outputL = inputL * (1.0 - blend * 0.5);
    outputR = inputR + inputL * blend * 0.5;
}

// Apply constant power compensation
outputL *= gainL * sqrt(2);
outputR *= gainR * sqrt(2);
```

This preserves the stereo character of the input while creating the panning effect.

## Usage Tips

### Subtle Animation
- **Rate:** 0.2-0.5 Hz
- **Depth:** 20-40%
- **Waveform:** Sine
- **Smooth:** 20%

Adds gentle movement without being obvious. Good for sustained pads or clean tones.

### Classic Auto-Pan
- **Rate:** 1-2 Hz
- **Depth:** 70-100%
- **Waveform:** Sine
- **Smooth:** 20%

The traditional auto-pan sound heard on countless recordings.

### Tremolo-Like Effect
- **Rate:** 5-10 Hz
- **Depth:** 100%
- **Waveform:** Sine
- **Smooth:** 10%

Fast panning creates a stereo tremolo effect.

### Ping-Pong Panning
- **Rate:** 1-3 Hz
- **Depth:** 100%
- **Waveform:** Square
- **Smooth:** 30-50%

Hard switching between left and right. Smoothing prevents clicks.

### Linear Sweep
- **Rate:** 0.5-2 Hz
- **Depth:** 80-100%
- **Waveform:** Triangle
- **Smooth:** 20%

Constant-speed panning with predictable timing.

### Tempo-Synced Panning

To sync with a tempo, calculate the rate:
- 120 BPM quarter notes: Rate = 2 Hz (120/60)
- 120 BPM eighth notes: Rate = 4 Hz
- 120 BPM half notes: Rate = 1 Hz

Formula: Rate = (BPM / 60) * note_multiplier

### Psychedelic Effects
- **Rate:** 3-5 Hz
- **Depth:** 100%
- **Waveform:** Triangle or Square
- **Smooth:** 10-30%

Fast, dramatic panning for special effects.

## Best Practices

### Avoiding Clicks
- Always use some smoothing with Square waveform
- Increase smoothing if you hear artifacts
- Triangle may also benefit from smoothing at direction changes

### Mono Compatibility
- Be aware that extreme panning may cause level changes in mono
- Test your mix in mono if compatibility matters
- Consider using lower depth values for better mono translation

### Mixing Context
- Use subtle settings when the instrument needs to sit in a mix
- More dramatic settings work for featured instruments or effects
- Consider how the panning interacts with other stereo elements

### With Other Effects
- Place before reverb/delay for panning the dry signal only
- Place after reverb/delay for panning the entire wet signal
- Experiment with both positions for different effects

## Waveform Characteristics

### Sine Wave
```
    /\      /\
   /  \    /  \
  /    \  /    \
 /      \/      \
```
- Smooth acceleration and deceleration
- Natural, organic movement
- Spends more time at center, less at extremes

### Triangle Wave
```
  /\    /\    /\
 /  \  /  \  /  \
/    \/    \/    \
```
- Linear, constant-speed movement
- Equal time spent at all positions
- Mechanical, predictable feel

### Square Wave
```
  ____    ____
 |    |  |    |
 |    |__|    |__
```
- Instant switching between extremes
- No intermediate positions
- Most dramatic effect
- Requires smoothing to prevent clicks

## Technical Specifications

- **LFO Rate Range:** 0.1-10 Hz
- **Panning Method:** Constant power (equal power)
- **Smoothing Type:** One-pole lowpass
- **Processing:** 32-bit float
- **Latency:** 0 samples
- **Stereo Support:** Full stereo input, panned stereo output

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/PannerEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes LFO
- `onProcess(...)` - Mono pass-through (panning requires stereo output)
- `onProcessStereo(...)` - Full stereo panning processing
- `indexToWaveform(int index)` - Converts parameter index to LFO waveform
- `onReset()` - Resets LFO and smoothing state

**Convenience Setters:**
- `setRate(float hz)` - Set LFO rate
- `setDepth(float percent)` - Set pan depth
- `setWaveform(int index)` - Set waveform type
- `setSmooth(float percent)` - Set smoothing amount

## See Also

- [MonoToStereoEffect](MonoToStereoEffect.md) - Static stereo widening with optional LFO animation
- [TremoloEffect](TremoloEffect.md) - Volume-based modulation (mono-compatible alternative)
- [ChorusEffect](ChorusEffect.md) - Pitch-based modulation with stereo spread
