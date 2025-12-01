# DelayEffect

## Overview

DelayEffect is a versatile digital delay with feedback, filtering, and BPM synchronization capabilities. It provides clean, digital-quality repeats with features for both time-based and tempo-synced delay effects.

**Category:** Delay
**ID:** `delay`
**Display Name:** Delay

## Description

This is the core delay effect offering both manual time control and BPM-synced note divisions. It features a feedback path with lowpass filtering to create analog-style darkening on repeats, making it suitable for both clean digital echoes and warm, vintage-inspired delay sounds.

## Parameters

### Time
- **ID:** `time`
- **Range:** 10 ms to 2000 ms
- **Default:** 375 ms
- **Unit:** ms (milliseconds)
- **Description:** Delay time between repeats. Longer times create spacious echoes, shorter times add thickness. At 120 BPM, 375 ms equals a 1/8 note.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 95%
- **Default:** 40%
- **Unit:** %
- **Description:** Amount of signal fed back into the delay. Higher values create more repeats. Maximum is limited to 95% to prevent runaway feedback.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Balance between dry signal and delayed signal. 0% is completely dry, 100% is completely wet, 50% is equal blend.

### Filter
- **ID:** `filter`
- **Range:** 500 Hz to 12000 Hz
- **Default:** 8000 Hz
- **Unit:** Hz
- **Description:** Lowpass filter cutoff frequency for the feedback path. Darkens each repeat by filtering high frequencies. Lower values create vintage tape-like decay.

### Sync
- **ID:** `sync`
- **Type:** Boolean
- **Default:** Off (false)
- **Description:** When enabled, delay time locks to tempo-based note divisions instead of manual time control.

### BPM
- **ID:** `bpm`
- **Range:** 40 to 240
- **Default:** 120
- **Unit:** BPM
- **Description:** Tempo for synced delay. Match to your song's tempo for rhythmic delays. Only active when Sync is enabled.

### Division
- **ID:** `division`
- **Type:** Choice
- **Options:**
  - `1/1` (Whole note)
  - `1/2` (Half note)
  - `1/4` (Quarter note) - Default
  - `1/8` (Eighth note)
  - `1/16` (Sixteenth note)
  - `1/2D` (Dotted half note)
  - `1/4D` (Dotted quarter note)
  - `1/8D` (Dotted eighth note)
  - `1/16D` (Dotted sixteenth note)
  - `1/2T` (Half note triplet)
  - `1/4T` (Quarter note triplet)
  - `1/8T` (Eighth note triplet)
  - `1/16T` (Sixteenth note triplet)
- **Default:** `1/4`
- **Description:** Note value for synced delay. Dotted variants are 1.5x the base duration. Triplet variants are 2/3 of the base duration.

## DSP Components

### Delay Lines
- **Left Channel:** `DelayLine delayLineL` - Maximum 2 seconds capacity
- **Right Channel:** `DelayLine delayLineR` - Maximum 2 seconds capacity
- Supports both linear and cubic interpolation for fractional delay times

### Feedback Filters
- **Left Channel:** `BiquadFilter feedbackFilterL` - Lowpass, Q=0.707 (Butterworth)
- **Right Channel:** `BiquadFilter feedbackFilterR` - Lowpass, Q=0.707 (Butterworth)
- Configured to dampen high frequencies in the feedback path

### Feedback State
- Stores previous output samples for feedback calculation
- Separate state maintained for left and right channels

## Implementation Details

### Signal Flow

#### Mono Processing:
1. Input signal enters
2. Read delayed signal from delay line
3. Apply lowpass filter to delayed signal
4. Write input + filtered feedback to delay line
5. Mix dry and delayed signals based on Mix parameter
6. Output result

#### Stereo Processing:
1. Process left and right channels independently
2. Each channel has its own delay line and feedback filter
3. Same processing as mono but maintains stereo separation
4. Delay times are identical for both channels (no stereo spread)

### BPM Synchronization

When Sync is enabled:
1. Parse note division (extract denominator: 1/4 → 4, 1/8 → 8)
2. Detect dotted (ends with 'D') or triplet (ends with 'T') modifiers
3. Calculate delay samples using formula:
   ```
   samples = (60 / BPM) * (4 / division) * sampleRate

   If dotted: samples *= 1.5
   If triplet: samples *= (2/3)
   ```
4. Use calculated samples for delay line read position

### Filtering Algorithm

The feedback filter is a 2nd-order Butterworth lowpass:
- Applied only to the feedback signal, not the initial delayed output
- Creates natural high-frequency rolloff on subsequent repeats
- Q factor of 0.707 provides maximally flat passband response
- Frequency is updated in real-time as Filter parameter changes

### Feedback Path

```
Input → Delay Line Write
        ↓
        Delay Line Read → Output (to Mix)
        ↓
        Lowpass Filter
        ↓
        × Feedback Amount
        ↓
        Back to Delay Line Write
```

## Usage Tips

### General Purpose Delay
- **Time:** 250-500 ms
- **Feedback:** 30-50%
- **Mix:** 20-40%
- **Filter:** 6000-10000 Hz

### Slapback Echo
- **Time:** 80-150 ms
- **Feedback:** 0-20%
- **Mix:** 30-50%
- **Filter:** 8000+ Hz

### Ambient Wash
- **Time:** 500-1500 ms
- **Feedback:** 60-80%
- **Mix:** 40-70%
- **Filter:** 3000-5000 Hz (darker)

### Rhythmic Pattern (Sync On)
- **BPM:** Match song tempo
- **Division:** 1/4 or 1/8 for standard rhythms, dotted for swing feel
- **Feedback:** 40-60%
- **Mix:** 30-50%

### Vintage Tape Echo Emulation
- **Time:** 300-450 ms
- **Feedback:** 40-60%
- **Mix:** 30-40%
- **Filter:** 2500-4000 Hz (heavily filtered)

## Technical Specifications

- **Maximum Delay Time:** 2000 ms per channel
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Internal Processing:** 32-bit float
- **Interpolation:** Linear for mono, supports cubic for smoother modulation
- **Latency:** Buffer-dependent, typically negligible
- **CPU Usage:** Low (single delay line + simple filter per channel)

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/DelayEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes delay lines and filters
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing
- `calculateSyncedDelay()` - Converts BPM and division to delay samples
- `onReset()` - Clears all buffers and filter state

## See Also

- [PingPongDelayEffect](PingPongDelayEffect.md) - Stereo bouncing delay
- [TapeEchoEffect](TapeEchoEffect.md) - Vintage tape echo emulation with wow/flutter
- [MultiTapDelayEffect](MultiTapDelayEffect.md) - Rhythmic multi-tap patterns
