# ReverseDelayEffect

## Overview

ReverseDelayEffect creates backwards-sounding echoes by capturing audio segments and playing them in reverse. This produces ethereal, psychedelic textures that are impossible to achieve with forward delays.

**Category:** Delay
**ID:** `reversedelay`
**Display Name:** Reverse Delay

## Description

This effect captures incoming audio into buffers, then reads those buffers in reverse to create backwards echoes. The reversed signal can be fed back to create building, evolving textures. Crossfading between buffer segments ensures smooth transitions without clicks or pops.

## Parameters

### Time
- **ID:** `time`
- **Range:** 100 ms to 2000 ms
- **Default:** 500 ms
- **Unit:** ms (milliseconds)
- **Description:** Length of the reversed segment. Longer times create more dramatic, swooping reverse effects.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 80%
- **Default:** 30%
- **Unit:** %
- **Description:** Amount of reversed signal fed back. Creates building layers and evolving textures. Higher values create dense, complex soundscapes.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Balance between dry and reversed signals. 50% provides equal blend for typical reverse delay effect.

### Crossfade
- **ID:** `crossfade`
- **Range:** 5% to 50%
- **Default:** 20%
- **Unit:** %
- **Description:** Overlap between segments as percentage of buffer length. Higher values create smoother transitions but less defined reverse effect. Lower values create more articulated but potentially clickier transitions.

## DSP Components

### Double Buffers
The effect uses double buffering to allow simultaneous writing and reading:

- **Left Channel:**
  - `bufferL1` - First buffer (maximum 2 seconds)
  - `bufferL2` - Second buffer (maximum 2 seconds)
- **Right Channel:**
  - `bufferR1` - First buffer (maximum 2 seconds)
  - `bufferR2` - Second buffer (maximum 2 seconds)

### Buffer State
- `writePos` - Current write position (forward)
- `readPos` - Current read position (forward, but reads buffer backwards)
- `useBuffer1` - Toggle between buffer 1 and buffer 2
- `bufferSize` - Active buffer length in samples (based on Time parameter)

### Feedback State
- `feedbackL` - Stores left channel feedback sample
- `feedbackR` - Stores right channel feedback sample

## Implementation Details

### Double Buffer Strategy

The effect uses ping-pong buffering:
```
Cycle 1: Write to Buffer 1 while reading Buffer 2 in reverse
Cycle 2: Write to Buffer 2 while reading Buffer 1 in reverse
```

This allows continuous operation without interruption or buffer underruns.

### Signal Flow

```
Input + Feedback → Write to Current Buffer (forward)
                 ↓
        Read from Opposite Buffer (reverse)
                 ↓
        Apply Crossfade Window
                 ↓
        Store as Feedback
                 ↓
        Mix with Dry → Output

When Buffer Full: Swap Buffers, Reset Positions
```

### Reverse Reading

Reading is performed backwards through the buffer:
```java
int reversePos = bufferSize - 1 - readPos;
float reversed = buffer[reversePos];
```

As `readPos` increments from 0 to bufferSize:
- Sample is read from end to beginning
- Creates backwards playback effect

### Crossfade Algorithm

Applied at buffer boundaries to prevent clicks:
```java
float fadeGain = 1.0f;

if (readPos < crossfadeSamples) {
    // Fade in at start
    fadeGain = (float) readPos / crossfadeSamples;
} else if (readPos > bufferSize - crossfadeSamples) {
    // Fade out at end
    fadeGain = (float) (bufferSize - readPos) / crossfadeSamples;
}

reversed *= fadeGain;
```

This creates smooth envelope at segment boundaries:
- Start of segment: Fades from 0% to 100%
- Middle of segment: Full volume (100%)
- End of segment: Fades from 100% to 0%

### Buffer Swap

When write position reaches buffer end:
```java
if (writePos >= bufferSize) {
    writePos = 0;
    readPos = 0;
    useBuffer1 = !useBuffer1;  // Toggle buffers
}
```

Ensures continuous operation without gaps or discontinuities.

### Feedback Path

Feedback is taken from the reversed output:
```java
inputWithFeedback = dry + feedbackL * feedback;
writeBuffer[writePos] = inputWithFeedback;
```

This creates layered reverse textures that build over time with higher feedback settings.

## Usage Tips

### Classic Reverse Echo
- **Time:** 500-800 ms
- **Feedback:** 25-40%
- **Mix:** 40-60%
- **Crossfade:** 15-25%

### Psychedelic Texture
- **Time:** 1000-1500 ms
- **Feedback:** 50-70%
- **Mix:** 50-80%
- **Crossfade:** 20-30%

### Subtle Reverse Ambience
- **Time:** 300-500 ms
- **Feedback:** 15-30%
- **Mix:** 25-40%
- **Crossfade:** 25-35%

### Dramatic Reverse Swells
- **Time:** 1200-2000 ms
- **Feedback:** 40-60%
- **Mix:** 60-90%
- **Crossfade:** 10-20% (more defined)

### Articulated Reverse Hits
- **Time:** 200-400 ms
- **Feedback:** 10-25%
- **Mix:** 50-70%
- **Crossfade:** 5-10% (less smooth, more defined)

### Ambient Wash
- **Time:** 800-1500 ms
- **Feedback:** 60-80%
- **Mix:** 60-100%
- **Crossfade:** 30-50% (very smooth)

## Musical Applications

### Lead Guitar
Add reverse delay with moderate mix and feedback for "sucking backwards" lead tones. Popular in psychedelic and progressive rock.

### Ambient Soundscapes
Use long times (1000-2000 ms) with high feedback (60-80%) and mix (70-100%) to create evolving, atmospheric pads.

### Transitions
Automate mix from 0% to 100% during song transitions to create dramatic reverse buildup effects.

### Rhythmic Patterns
Use shorter times (200-500 ms) synchronized to tempo for rhythmic reverse echoes that complement the groove.

### Combining Effects
Chain reverse delay before or after other effects:
- **Before Reverb:** Creates reverse echoes in a space
- **After Distortion:** Adds texture to distorted reverse echoes
- **With Modulation:** Creates complex, evolving textures

## Technical Specifications

- **Maximum Buffer Size:** 2 seconds per buffer (4 buffers total: L1, L2, R1, R2)
- **Buffer Mode:** Double buffering (ping-pong)
- **Read Direction:** Reverse (backwards)
- **Write Direction:** Forward
- **Crossfade Type:** Linear fade in/out at boundaries
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **Memory Usage:** ~4 seconds of audio buffered per channel (8 seconds stereo)
- **CPU Usage:** Low to moderate (buffer operations + feedback)
- **Latency:** One buffer length (Time parameter)

## Advanced Techniques

### Crossfade Settings

**Low Crossfade (5-15%):**
- More defined segment boundaries
- Risk of clicks/pops on transients
- Better for rhythmic, percussive material
- More obvious "reversed" character

**Medium Crossfade (15-30%):**
- Balanced smooth transitions
- Natural sound
- Works for most material
- Default recommendation

**High Crossfade (30-50%):**
- Very smooth, seamless
- Less defined reverse effect
- Better for sustained sounds
- Creates more ambient, less obvious effect

### Feedback Characteristics

As feedback increases:
- 0-20%: Single reverse repeat
- 20-40%: 2-3 reverse repeats, building complexity
- 40-60%: Dense reverse texture, still controlled
- 60-80%: Thick, evolving soundscape, near-infinite repeats

## Limitations and Considerations

### Latency
The effect introduces latency equal to the buffer size (Time parameter). At 500 ms, the first reverse echo appears 500 ms after the input signal.

### Transient Handling
Sharp transients at buffer boundaries may cause clicks if crossfade is too low. Increase crossfade for cleaner results with percussive sources.

### Temporal Coherence
The effect reverses time, which can create disorienting results on complex polyphonic material. Works best on:
- Single note lines
- Sustained sounds
- Simple melodic phrases

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/ReverseDelayEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize double buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Mono reverse processing
- `onProcessStereo(...)` - Stereo reverse processing
- `onReset()` - Clear all buffers and reset state

**Key Features:**
- Double buffering for continuous operation
- Reverse reading with forward writing
- Linear crossfade at segment boundaries
- Feedback for building textures
- Dynamic buffer resizing based on Time parameter

**Buffer Management:**
- Automatic buffer swapping at end of cycle
- Position tracking for synchronized read/write
- Feedback integrated into write path

## See Also

- [DelayEffect](DelayEffect.md) - Standard forward delay
- [TapeEchoEffect](TapeEchoEffect.md) - Vintage tape echo
- [ShimmerReverbEffect](ShimmerReverbEffect.md) - Pitch-shifted reverb for ethereal textures
