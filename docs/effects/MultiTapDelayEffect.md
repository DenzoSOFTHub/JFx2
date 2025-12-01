# MultiTapDelayEffect

## Overview

MultiTapDelayEffect creates rhythmic delay patterns using four independent delay taps with preset timing relationships. Perfect for creating complex, musical rhythmic echoes with a single effect.

**Category:** Delay
**ID:** `multitap`
**Display Name:** Multi-Tap Delay

## Description

This effect implements four parallel delay taps, each reading from the same delay line at different positions with different levels. The tap timing and levels are organized into preset rhythmic patterns that provide instant musical results.

## Parameters

### Time
- **ID:** `time`
- **Range:** 100 ms to 1500 ms
- **Default:** 500 ms
- **Unit:** ms (milliseconds)
- **Description:** Base delay time. All taps are calculated as ratios of this value. Higher values create slower, more spacious patterns.

### Pattern
- **ID:** `pattern`
- **Type:** Choice
- **Options:**
  - `Quarter` (0) - Quarter note subdivisions: 1/4, 2/4, 3/4, 4/4
  - `Eighth` (1) - Eighth note subdivisions: 1/8, 2/8, 3/8, 4/8
  - `Dotted` (2) - Dotted rhythm: 3/8, 3/4, 9/8, 3/2
  - `Triplet` (3) - Triplet feel: 1/3, 2/3, 1, 4/3
  - `Random` (4) - Random-ish pattern: 1/5, 9/20, 7/10, 1
- **Default:** Quarter
- **Description:** Rhythmic pattern defining tap timing and level relationships.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 80%
- **Default:** 30%
- **Unit:** %
- **Description:** Amount fed back from the last tap. Creates repeating patterns.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 40%
- **Unit:** %
- **Description:** Balance between dry and delayed signals. 50% is equal blend.

### Tone
- **ID:** `tone`
- **Range:** 1000 Hz to 12000 Hz
- **Default:** 6000 Hz
- **Unit:** Hz
- **Description:** High frequency cutoff for the delays. Lower values create warmer, darker repeats.

### Spread
- **ID:** `spread`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Stereo spread of the taps. Higher values create wider stereo field by panning taps alternately left and right.

## DSP Components

### Delay Lines
- **Left Channel:** `DelayLine delayLineL` - Maximum 3 seconds
- **Right Channel:** `DelayLine delayLineR` - Maximum 3 seconds
- Uses cubic interpolation for smooth, musical delays

### Tone Filters
- **Left Channel:** `BiquadFilter toneFilterL` - Lowpass, Q=0.707
- **Right Channel:** `BiquadFilter toneFilterR` - Lowpass, Q=0.707
- Applied to summed tap output for high-frequency control

### Tap Configuration
- **Number of Taps:** 4 per channel
- Each tap has independent timing ratio and level
- All taps share same base delay time (scaled by pattern)

## Tap Patterns

### Pattern Timing Ratios

| Pattern  | Tap 1 | Tap 2 | Tap 3 | Tap 4 |
|----------|-------|-------|-------|-------|
| Quarter  | 0.25  | 0.50  | 0.75  | 1.00  |
| Eighth   | 0.125 | 0.25  | 0.375 | 0.50  |
| Dotted   | 0.375 | 0.75  | 1.125 | 1.50  |
| Triplet  | 0.333 | 0.667 | 1.00  | 1.333 |
| Random   | 0.20  | 0.45  | 0.70  | 1.00  |

### Pattern Level Attenuation

| Pattern  | Tap 1 | Tap 2 | Tap 3 | Tap 4 |
|----------|-------|-------|-------|-------|
| Quarter  | 0.8   | 0.6   | 0.4   | 0.3   |
| Eighth   | 0.9   | 0.7   | 0.5   | 0.3   |
| Dotted   | 0.7   | 0.5   | 0.4   | 0.3   |
| Triplet  | 0.8   | 0.6   | 0.5   | 0.4   |
| Random   | 0.6   | 0.8   | 0.5   | 0.7   |

## Implementation Details

### Signal Flow

```
Input → Write to Delay Line (with feedback from last tap)
        ↓
        Read Tap 1 at ratio[0] * baseTime → × level[0] ─┐
        Read Tap 2 at ratio[1] * baseTime → × level[1] ─┤
        Read Tap 3 at ratio[2] * baseTime → × level[2] ─┼→ Sum
        Read Tap 4 at ratio[3] * baseTime → × level[3] ─┘
        ↓
        Tone Filter
        ↓
        Mix with Dry → Output

        Tap 4 Output → Feedback to Input
```

### Stereo Processing

In stereo mode, spread parameter controls spatial distribution:

```java
// Alternate taps between channels
float panL = 0.5 + (t % 2 == 0 ? spread * 0.5 : -spread * 0.5);
float panR = 1.0 - panL;

// Cross-mix taps
wetL += tapL * panL + tapR * (1 - panL);
wetR += tapR * panR + tapL * (1 - panR);
```

This creates:
- Even-numbered taps (0, 2) pan toward one side
- Odd-numbered taps (1, 3) pan toward opposite side
- Spread = 0%: All taps centered (mono)
- Spread = 100%: Maximum separation

Additionally, right channel delay times are 10% longer for stereo width.

### Feedback Mechanism

Only the last tap (Tap 4) is fed back:
```java
float lastTapMs = baseTimeMs * tapRatios[3];
feedback = delayLine.readCubic(msToSamples(lastTapMs));
```

This prevents feedback buildup from all taps, maintaining clarity while allowing pattern repetition.

### Tone Filtering

Applied to the summed output of all taps:
- Prevents harsh high frequencies from accumulating
- Creates natural decay of bright content
- Maintains individual tap clarity

## Usage Tips

### Standard Rhythmic Delay
- **Pattern:** Quarter or Eighth
- **Time:** 400-600 ms
- **Feedback:** 30-50%
- **Mix:** 35-50%
- **Tone:** 5000-7000 Hz
- **Spread:** 50-70%

### Dotted Eighth Feel (à la The Edge)
- **Pattern:** Dotted
- **Time:** 300-450 ms (adjust to tempo)
- **Feedback:** 40-60%
- **Mix:** 40-60%
- **Tone:** 6000-8000 Hz
- **Spread:** 60-80%

### Triplet Groove
- **Pattern:** Triplet
- **Time:** 350-500 ms
- **Feedback:** 30-50%
- **Mix:** 40-55%
- **Tone:** 5000-7000 Hz
- **Spread:** 40-60%

### Ambient Texture
- **Pattern:** Random
- **Time:** 600-1000 ms
- **Feedback:** 50-70%
- **Mix:** 50-80%
- **Tone:** 3000-5000 Hz (darker)
- **Spread:** 70-100%

### Fast Articulation
- **Pattern:** Eighth
- **Time:** 150-300 ms
- **Feedback:** 20-40%
- **Mix:** 30-45%
- **Tone:** 7000-10000 Hz
- **Spread:** 30-50%

### Wide Stereo Wash
- **Pattern:** Quarter or Dotted
- **Time:** 500-800 ms
- **Feedback:** 40-60%
- **Mix:** 45-65%
- **Tone:** 4000-6000 Hz
- **Spread:** 80-100%

## Pattern Usage Guide

### Quarter Notes
- **Best for:** Slow to mid-tempo songs
- **Character:** Even, steady rhythm
- **Musical Context:** Rock, pop, ballads

### Eighth Notes
- **Best for:** Faster tempos, rhythmic density
- **Character:** Tight, rapid-fire repeats
- **Musical Context:** Funk, dance, up-tempo rock

### Dotted
- **Best for:** Compound meter feel
- **Character:** Swinging, syncopated
- **Musical Context:** U2-style ambient rock, modern worship

### Triplet
- **Best for:** Shuffle and swing feels
- **Character:** Bouncy, flowing
- **Musical Context:** Blues, jazz-influenced, Latin

### Random
- **Best for:** Non-metric, ambient textures
- **Character:** Unpredictable, organic
- **Musical Context:** Ambient, experimental, soundscapes

## Technical Specifications

- **Maximum Delay Time:** 3000 ms (accommodates longer patterns)
- **Number of Taps:** 4 per channel (8 total in stereo)
- **Interpolation:** Cubic (4-point) for smooth delays
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **CPU Usage:** Moderate (2 delay lines + 2 filters + 8 tap reads per frame)
- **Latency:** Negligible

## Musical Applications

### Tempo Sync Workflow
Although this effect doesn't have built-in BPM sync, you can calculate base time manually:
```
time_ms = (60000 / BPM) * (4 / note_division)

Example: 120 BPM, quarter note
time_ms = (60000 / 120) * (4 / 4) = 500 ms
```

### Stereo Enhancement
Use moderate spread (40-70%) to add width without exaggerating the effect. This keeps the delay musical while enhancing stereo image.

### Layered Delays
Combine with other delay effects for complex textures:
- Multi-Tap for rhythmic foundation
- Ping-Pong for stereo movement
- Reverb for space and depth

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/MultiTapDelayEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize delay lines and filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono tap processing
- `onProcessStereo(...)` - Stereo processing with spread control
- `onReset()` - Clear all buffers and state

**Key Features:**
- 5 preset rhythmic patterns with predefined timing and levels
- Independent stereo spread control
- Single feedback from last tap
- Cubic interpolation for smooth delays

**Pattern Data:**
- `TAP_RATIOS[5][4]` - Timing ratios for each pattern
- `TAP_LEVELS[5][4]` - Level attenuation for each tap

## See Also

- [DelayEffect](DelayEffect.md) - Standard digital delay with BPM sync
- [PingPongDelayEffect](PingPongDelayEffect.md) - Stereo bouncing delay
- [TapeEchoEffect](TapeEchoEffect.md) - Vintage tape echo with multi-head mode
