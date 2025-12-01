# WavFileInputEffect

## Overview

WavFileInputEffect is a generator effect that reads audio from a WAV file and outputs it to the signal chain. This allows playback of pre-recorded audio for testing, backing tracks, or sample-based applications.

**Category:** Input Source
**ID:** `wavfileinput`
**Display Name:** WAV File Player

## Description

This effect provides file-based audio playback with support for looping, volume control, and transport controls (play/stop/pause/seek). It loads entire WAV files into memory for low-latency playback and supports both mono and stereo source files.

## Parameters

### Volume
- **ID:** `volume`
- **Range:** -60 dB to +12 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Playback volume level. 0 dB = original level. Adjust to match other sources in the signal chain.

### Loop Count
- **ID:** `loopCount`
- **Range:** -1 to 100
- **Default:** 0
- **Unit:** (none)
- **Description:** Number of times to repeat the file:
  - `-1` = Loop forever (infinite loop)
  - `0` = Play once (no repeat)
  - `1+` = Repeat N times (total plays = N + 1)

### Playing
- **ID:** `playing`
- **Type:** Boolean
- **Default:** Off (false)
- **Description:** Start or stop playback. File plays from current position when enabled. Automatically turns off when playback reaches the end (if not looping).

## Implementation Details

### File Loading

The effect loads WAV files through the Java Sound API with automatic format conversion:

```
File Loading Process:
1. Open file with AudioSystem.getAudioInputStream()
2. Check encoding (PCM_SIGNED, PCM_FLOAT, or convert)
3. Extract format info (sample rate, channels, bit depth)
4. Read all bytes into memory
5. Convert to 32-bit float arrays (separate L/R for stereo)
```

### Supported Formats

**Encoding Types:**
- PCM Signed (8-bit, 16-bit, 24-bit, 32-bit)
- PCM Float (32-bit)
- Other formats are converted to 16-bit PCM

**Channel Configurations:**
- Mono: Single channel, duplicated to stereo on output if needed
- Stereo: Separate left and right channels preserved

**Sample Rates:**
- Any sample rate supported
- Automatic resampling if file rate differs from engine rate

### Sample Conversion

The `readSample()` method handles multiple bit depths:

| Bit Depth | Format | Conversion |
|-----------|--------|------------|
| 8-bit | Unsigned | `(byte - 128) / 128.0` |
| 16-bit | Signed LE | `short / 32768.0` |
| 24-bit | Signed LE | `int24 / 8388608.0` |
| 32-bit int | Signed LE | `int / 2147483648.0` |
| 32-bit float | IEEE Float | Direct conversion |

### Playback Processing

```
Playback Loop (per sample):
1. Check if playing and audio loaded
2. Check if all loops completed
3. Read sample from current position
4. Apply resampling if needed (nearest-neighbor)
5. Apply volume
6. Advance position
7. Handle end-of-file (loop or stop)
```

### Resampling

Simple nearest-neighbor resampling for sample rate conversion:
```java
srcPos = (playbackPosition * audioSampleRate) / engineSampleRate
```

Note: This is a basic implementation. For higher quality, consider implementing linear or cubic interpolation.

### Stereo Handling

**Mono Output (onProcess):**
- Stereo source: Mix L+R with 0.5 factor
- Mono source: Direct output

**Stereo Output (onProcessStereo):**
- Stereo source: Separate L/R channels
- Mono source: Same sample to both channels

### Loop Control

```
End of File Handling:
if (position >= length):
    if (loopCount < 0 OR currentLoop < loopCount):
        position = 0
        currentLoop++
    else:
        stop playback
        set playing parameter to false
```

## Usage Tips

### Loading and Playing Files
```java
WavFileInputEffect player = new WavFileInputEffect();
player.loadFile("/path/to/audio.wav");
player.play();
```

### Seamless Looping
- Use Loop Count = -1 for continuous background playback
- For perfect loops, ensure the WAV file is edited to loop seamlessly
- The effect does not apply crossfading at loop points

### Backing Tracks
- Load the backing track WAV file
- Set Loop Count = 0 for one-shot playback
- Mix with live input using a Mixer effect downstream

### Sample Playback
- Load short samples for one-shot sounds
- Use Loop Count = 0
- Trigger with `play()` method or by setting the Playing parameter

### Progress Monitoring
- Use `getProgress()` to display playback position (0.0 to 1.0)
- Use `getDuration()` to show total time in seconds
- Build transport UI with these values

### Seeking
- Use `seek(float position)` where position is 0.0 to 1.0
- Example: `seek(0.5f)` jumps to middle of file

## Technical Specifications

- **Supported Formats:** WAV (various PCM encodings)
- **Bit Depths:** 8, 16, 24, 32-bit (int and float)
- **Channels:** Mono or Stereo
- **Sample Rates:** Any (resampled to match engine)
- **Storage:** Entire file loaded to memory
- **Resampling:** Nearest-neighbor interpolation
- **Memory Usage:** Approximately 4 bytes per sample per channel

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/WavFileInputEffect.java`

**Key Methods:**
- `loadFile(String filePath)` - Loads a WAV file into memory
- `onProcess(...)` - Mono output processing
- `onProcessStereo(...)` - Stereo output processing
- `readSample(...)` - Converts bytes to float based on format

**Transport Controls:**
- `play()` - Start playback
- `stop()` - Stop and reset to beginning
- `pause()` - Pause at current position
- `seek(float position)` - Jump to position (0.0-1.0)

**Status Methods:**
- `isPlaying()` - Check playback state
- `isStereo()` - Check if loaded file is stereo
- `getProgress()` - Get playback progress (0.0-1.0)
- `getDuration()` - Get file duration in seconds
- `getFilePath()` - Get path of loaded file

## See Also

- [WavFileOutputEffect](WavFileOutputEffect.md) - For recording audio to WAV files
- [AudioInputEffect](AudioInputEffect.md) - For live audio input
- [LooperEffect](LooperEffect.md) - For loop recording and playback
