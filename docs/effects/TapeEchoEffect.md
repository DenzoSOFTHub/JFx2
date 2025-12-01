# TapeEchoEffect

## Overview

TapeEchoEffect emulates vintage tape echo machines like the Echoplex, Roland Space Echo, and Maestro Echoplex. It captures the characteristic warmth, degradation, and modulation artifacts of magnetic tape-based delay units.

**Category:** Delay
**ID:** `tapeecho`
**Display Name:** Tape Echo

## Description

This effect simulates the physical characteristics of tape echo machines, including tape head high-frequency loss, tape transport speed variations (wow and flutter), tape saturation, and multiple playback head configurations. The result is a warm, organic echo with vintage character.

## Parameters

### Time
- **ID:** `time`
- **Range:** 50 ms to 1000 ms
- **Default:** 350 ms
- **Unit:** ms (milliseconds)
- **Description:** Delay time representing the distance between record and playback heads. Classic tape echoes typically operated at 300-400 ms.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 95%
- **Default:** 50%
- **Unit:** %
- **Description:** Amount of signal fed back, simulating how much of the playback signal is routed back to the record head.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Balance between dry and echo signal. Tape echoes were typically used with modest mix settings.

### Tone
- **ID:** `tone`
- **Range:** 1000 Hz to 8000 Hz
- **Default:** 3000 Hz
- **Unit:** Hz
- **Description:** High frequency cutoff simulating tape head loss. Lower values create darker, more vintage sounds. Real tape echoes had significant high-frequency rolloff.

### Wow
- **ID:** `wow`
- **Range:** 0% to 100%
- **Default:** 20%
- **Unit:** %
- **Description:** Slow pitch variation (0.5-2 Hz) caused by tape speed fluctuations from motor irregularities and capstan slippage.

### Flutter
- **ID:** `flutter`
- **Range:** 0% to 100%
- **Default:** 15%
- **Unit:** %
- **Description:** Fast pitch variation (5-7 Hz) from capstan and pinch roller irregularities and tape tension variations.

### Mode
- **ID:** `mode`
- **Type:** Choice
- **Options:**
  - `Single` (0) - Single playback head
  - `Multi` (1) - Multiple playback heads at different positions
  - `Slapback` (2) - Short, single repeat
- **Default:** Single
- **Description:** Echo configuration simulating different playback head arrangements on tape machines.

### Saturation
- **ID:** `saturation`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Tape saturation on feedback path. Adds warmth, compression, and harmonic distortion characteristic of magnetic tape.

## DSP Components

### Delay Lines
- **Left Channel:** `DelayLine delayLineL` - Maximum 2 seconds
- **Right Channel:** `DelayLine delayLineR` - Maximum 2 seconds
- Uses cubic interpolation for smooth pitch modulation

### Tone Filters (Tape Head Loss)
- **Left:** `BiquadFilter toneFilterL` - Lowpass, Q=0.707
- **Right:** `BiquadFilter toneFilterR` - Lowpass, Q=0.707
- Simulates high-frequency loss in magnetic tape recording/playback

### Low Cut Filters
- **Left:** `BiquadFilter lowCutL` - Highpass at 80 Hz, Q=0.707
- **Right:** `BiquadFilter lowCutR` - Highpass at 80 Hz, Q=0.707
- Prevents bass buildup and mud in feedback path

### Modulation LFOs
- **Wow LFO:** Sine wave at 0.8 Hz (slow speed variation)
- **Flutter LFO:** Sine wave at 6.0 Hz (fast irregular motion)
- Both contribute to pitch variation for authentic tape character

### Feedback State
- Stores previous output for feedback calculation
- Passes through saturation and filtering before feedback

## Implementation Details

### Signal Flow

```
Input → Write to Delay Line (with filtered feedback)
        ↓
        Read with Modulated Delay Time
        ↓
        Tone Filter (tape head loss)
        ↓
        Low Cut Filter
        ↓
        Tape Saturation
        ↓
        Store as Feedback → Back to Write
        ↓
        (Multi-Head Mode: Add Secondary Taps)
        ↓
        Mix with Dry → Output
```

### Tape Modulation

Wow and flutter combine to create time-varying pitch shifts:
```
wowMod = wowLFO.tick() * wow * 0.02     // ±2% pitch variation
flutterMod = flutterLFO.tick() * flutter * 0.005  // ±0.5% pitch variation

modulation = 1.0 + wowMod + flutterMod
delaySamples = baseDelaySamples * modulation
```

This modulates the read position in the delay line, creating pitch shifting that simulates tape speed variations.

### Tape Saturation

Uses soft clipping algorithm based on tanh function:
```java
float saturate(float input, float drive) {
    if (drive <= 0) return input;
    float gained = input * (1.0 + drive * 2.0);
    return tanh(gained) / (1.0 + drive * 0.5);
}
```

Creates:
- Soft clipping at peaks
- Harmonic distortion
- Dynamic compression
- Warmth characteristic of tape

### Multi-Head Mode

When mode is set to "Multi":
- Main tap at full delay time
- Secondary tap at 66.6% of delay time (0.5x level)
- Tertiary tap at 33.3% of delay time (0.3x level)

Simulates tape machines like the Roland Space Echo with multiple playback heads at different positions on the tape loop.

### Slapback Mode

When mode is set to "Slapback":
- Delay time reduced to 30% of set value
- Creates characteristic rockabilly/vintage slapback echo
- Typically results in 100-150 ms delay

### Stereo Processing

In stereo mode:
- Right channel delay is 2% longer than left
- Creates subtle stereo spread and movement
- Each channel has independent processing chain
- Wow and flutter are shared between channels (same tape transport)

## Usage Tips

### Classic Rockabilly Slapback
- **Time:** 100-150 ms
- **Mode:** Slapback
- **Feedback:** 10-20%
- **Mix:** 30-50%
- **Tone:** 3000-4000 Hz
- **Wow:** 5-10%
- **Flutter:** 10-15%
- **Saturation:** 20-30%

### Dub/Reggae Echo
- **Time:** 350-450 ms
- **Mode:** Single
- **Feedback:** 60-80%
- **Mix:** 40-60%
- **Tone:** 2500-3500 Hz
- **Wow:** 15-25%
- **Flutter:** 10-20%
- **Saturation:** 40-60%

### Multi-Head Rhythmic Pattern
- **Time:** 300-400 ms
- **Mode:** Multi
- **Feedback:** 40-60%
- **Mix:** 35-50%
- **Tone:** 3000-4000 Hz
- **Wow:** 20-30%
- **Flutter:** 15-25%
- **Saturation:** 30-40%

### Clean Tape Echo
- **Time:** 250-400 ms
- **Mode:** Single
- **Feedback:** 30-50%
- **Mix:** 25-40%
- **Tone:** 4000-6000 Hz (less filtered)
- **Wow:** 5-15% (subtle)
- **Flutter:** 5-10% (subtle)
- **Saturation:** 15-25% (light)

### Heavily Degraded Vintage
- **Time:** 300-500 ms
- **Mode:** Multi
- **Feedback:** 70-85%
- **Mix:** 50-70%
- **Tone:** 1500-2500 Hz (very dark)
- **Wow:** 40-60% (pronounced)
- **Flutter:** 30-50% (pronounced)
- **Saturation:** 60-80% (heavy)

## Technical Specifications

- **Maximum Delay Time:** 2000 ms per channel
- **Interpolation:** Cubic (for smooth modulation)
- **Wow Frequency:** 0.8 Hz (sine wave)
- **Flutter Frequency:** 6.0 Hz (sine wave)
- **Saturation Algorithm:** Hyperbolic tangent with pre-gain and compensation
- **Filter Types:** 2nd-order Butterworth (tone and low-cut)
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **CPU Usage:** Moderate (2 delay lines + 4 filters + 2 LFOs + saturation)

## Historical Context

Tape echo machines were popular from the 1950s through the 1980s:

- **Echoplex EP-3 (1970s):** Single head, warm sound, used in rock and country
- **Roland Space Echo RE-201 (1974):** Multiple heads, spring reverb, iconic in dub
- **Maestro Echoplex (1959):** First widely used tape echo, rock and roll standard
- **Copicat (1960s):** British tube-based tape echo, used in surf and psychedelic rock

These units added character through their imperfections: wow, flutter, saturation, and high-frequency loss. This effect captures those characteristics.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/TapeEchoEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize delay lines, filters, and LFOs
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing with modulation
- `onProcessStereo(...)` - Stereo processing with 2% right channel offset
- `saturate(float input, float drive)` - Tape saturation algorithm
- `onReset()` - Clear all buffers, filters, and LFO state

**Key Features:**
- Dual LFO modulation (wow and flutter)
- Tape saturation with soft clipping
- Multi-head mode simulation
- Comprehensive filtering (tone + low-cut)

## See Also

- [DelayEffect](DelayEffect.md) - Clean digital delay
- [PingPongDelayEffect](PingPongDelayEffect.md) - Stereo bouncing delay
- [MultiTapDelayEffect](MultiTapDelayEffect.md) - Multi-tap rhythmic patterns
