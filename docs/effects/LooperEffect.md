# LooperEffect

## Overview

LooperEffect is a multi-layer loop recording and playback effect with overdubbing capabilities. It allows recording loops up to 5 minutes in length, layering additional parts via overdub, and exporting the result to WAV files.

**Category:** Utility
**ID:** `looper`
**Display Name:** Looper

## Description

This effect provides classic looper pedal functionality with extended features. Record a base loop, overdub additional layers, undo/delete layers, and export your creation. The looper can also load WAV files as the initial loop for backing track integration or sample-based workflows.

## Parameters

### Control Parameters (Triggers)

| ID | Name | Description |
|----|------|-------------|
| `rec` | Rec | Start recording a new loop. Press Stop when done. |
| `stop` | Stop | Stop recording and start playback. |
| `overdub` | Overdub | Record a new layer while playing. Press again to finish. |
| `pause` | Pause | Pause/resume loop playback. |
| `delete` | Delete | Delete the last recorded layer (undo). |
| `mute` | Mute | Mute loop output (loop continues running). |
| `save` | Save | Export all layers mixed to a WAV file. |

### Continuous Parameters

### Level
- **ID:** `level`
- **Range:** 0% to 100%
- **Default:** 80%
- **Unit:** %
- **Description:** Output level of the loop playback. Does not affect the dry signal pass-through.

### Feedback
- **ID:** `feedback`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** How much of existing layers to keep when overdubbing. At 100%, all previous audio is preserved. Lower values cause previous layers to fade.

## Looper States

The looper operates in five distinct states:

| State | Description | Input | Loop Output |
|-------|-------------|-------|-------------|
| `IDLE` | Nothing recorded | Pass-through | None |
| `RECORDING` | Recording initial loop | Pass-through | None |
| `PLAYING` | Playing back loop | Pass-through | Loop + layers |
| `OVERDUBBING` | Playing + recording new layer | Pass-through | Loop + layers |
| `PAUSED` | Playback paused | Pass-through | None |

## Implementation Details

### State Machine

```
    +-------+
    | IDLE  |<----- Delete (all layers)
    +---+---+
        |
        | Rec
        v
+-------+--------+
| RECORDING      |
+-------+--------+
        |
        | Stop
        v
+-------+--------+      Overdub      +-------------+
| PLAYING        |<----------------->| OVERDUBBING |
+-------+--------+                   +-------------+
    |       ^
    |       |
    | Pause | Pause
    v       |
+-------+---+----+
| PAUSED         |
+----------------+
```

### Layer Management

Layers are stored as separate float arrays:
```
layers: List<float[]>
  - Layer 0: Original recording
  - Layer 1: First overdub
  - Layer 2: Second overdub
  - ... (unlimited layers)
```

Playback mixes all layers together:
```java
output = sum(layers[i][position]) * level
```

### Recording Process

**Initial Recording:**
1. Press Rec -> Clear layers, start recording
2. Input samples written to recordBuffer
3. Press Stop -> Copy recordBuffer to new layer
4. Start playback from position 0

**Overdubbing:**
1. Press Overdub while playing
2. Input samples written to recordBuffer (at playback position)
3. Press Stop/Overdub again -> Add recordBuffer as new layer
4. Continue playback

### WAV File Loading

The looper can load WAV files as the initial layer:

```java
looper.setWavFile("/path/to/file.wav", true);  // autoPlay = true
```

Loading process:
1. Read WAV file with AudioSystem
2. Convert to mono if stereo (average L+R)
3. Resample if sample rates differ (linear interpolation)
4. Set as first layer
5. Auto-start playback if requested

### Resampling

Linear interpolation for sample rate conversion:
```java
ratio = targetRate / sourceRate
newLength = (int)(sourceLength * ratio)

for each output sample:
    srcIndex = outputIndex / ratio
    idx0 = floor(srcIndex)
    idx1 = idx0 + 1
    frac = srcIndex - idx0
    output[i] = input[idx0] * (1 - frac) + input[idx1] * frac
```

### Export (Save)

Saving creates a mixed WAV file:
1. Sum all layers sample-by-sample
2. Normalize to prevent clipping
3. Convert to 16-bit PCM
4. Write WAV to ~/JFx2_Loops/{name}.wav

### Signal Flow

```
Input Signal
    |
    v
+---+-------------------+
| IDLE/PAUSED: Pass     |
| RECORDING: Record+Pass|
| PLAYING: Pass+Mix     |
| OVERDUB: Record+Mix   |
+---+-------------------+
    |
    v
Output (Input + Loop if playing)
```

### Button Edge Detection

Parameters act as trigger buttons with edge detection:
```java
if (currentValue && !previousValue):
    // Rising edge - button pressed
    handleButtonPress()
previousValue = currentValue
```

This prevents continuous triggering while a button is held.

## Usage Tips

### Basic Loop Recording
1. Press Rec to start recording
2. Play your part
3. Press Stop - loop plays back immediately
4. Add more parts with Overdub

### Building Layers
- First layer sets the loop length
- Subsequent overdubs must fit within that length
- Use Level to adjust loop volume vs dry signal
- Delete removes the most recent layer (undo)

### Using WAV Backing Tracks
```java
looper.setWavFile("backing_track.wav", true);
// Loop auto-starts, ready for overdubbing
```

### Exporting Your Loop
```java
Path savedFile = looper.saveToFile("my_loop");
// Saved to ~/JFx2_Loops/my_loop.wav
```

### Monitoring State
```java
LooperState state = looper.getState();
int layers = looper.getLayerCount();
float position = looper.getPlaybackPosition(); // 0.0 to 1.0
float duration = looper.getLoopLengthSeconds();
```

### Muting
- Mute silences the loop but keeps it running
- Useful for breakdowns or practicing timing
- Unmute to bring the loop back in sync

## Technical Specifications

- **Maximum Loop Duration:** 300 seconds (5 minutes)
- **Sample Rate:** Matches audio engine
- **Internal Processing:** 32-bit float
- **Layer Count:** Unlimited
- **File Format (Save):** WAV, 16-bit PCM, mono
- **Save Location:** ~/JFx2_Loops/
- **WAV Import:** Mono or stereo, any sample rate

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/LooperEffect.java`

**Key Methods:**
- `onProcess(...)` - Main processing loop with state machine
- `detectButtonPresses()` - Handle button edge detection
- `getLayersMixed(int position)` - Mix all layers at position
- `loadWavFile()` - Load external WAV file
- `applyLoadedFile()` - Convert and set as first layer
- `resample(...)` - Linear interpolation resampling
- `saveToFile(String name)` - Export to WAV

**State Handlers:**
- `onRecPressed()` - Start/restart recording
- `onStopPressed()` - Stop recording/overdub, or pause playback
- `onOverdubPressed()` - Toggle overdub mode
- `onPausePressed()` - Pause/resume playback
- `onDeletePressed()` - Remove last layer

**Public Trigger Methods:**
- `triggerRec()`, `triggerStop()`, `triggerOverdub()`
- `triggerPause()`, `triggerDelete()`, `triggerSave()`

**Status Methods:**
- `getState()` - Current looper state
- `getLoopLengthSamples()` / `getLoopLengthSeconds()`
- `getLayerCount()` - Number of recorded layers
- `getPlaybackPosition()` - Position as 0.0-1.0
- `isMuted()` - Mute state
- `isFileLoaded()` - Whether WAV file was loaded

**Static Methods:**
- `getLoopsDirectory()` - Returns ~/JFx2_Loops path

## See Also

- [WavFileInputEffect](WavFileInputEffect.md) - For simple WAV playback
- [WavFileOutputEffect](WavFileOutputEffect.md) - For recording to file
- [DelayEffect](DelayEffect.md) - For delay-based effects
