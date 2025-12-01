# WavFileOutputEffect

## Overview

WavFileOutputEffect is a sink effect that records audio from the signal chain to a WAV file. It passes audio through while optionally recording to disk, making it useful for capturing performances or creating audio exports.

**Category:** Output Sink
**ID:** `wavfileoutput`
**Display Name:** WAV Recorder

## Description

This effect provides audio recording capabilities with automatic timestamped file naming. It supports both mono and stereo recording, automatically detecting the processing mode. Audio passes through unaffected, allowing the recorder to be placed anywhere in the signal chain.

## Parameters

### Recording
- **ID:** `recording`
- **Type:** Boolean
- **Default:** Off (false)
- **Description:** Start or stop recording. Each recording creates a new file with a timestamp suffix. When enabled, a new file is created; when disabled, the file is finalized and saved.

## Implementation Details

### File Naming Convention

Files are named automatically with timestamps:
```
{baseFileName}_{timestamp}.wav

Example: recording_20231215143022.wav
Format:  YYYYMMDDHHMMSS
```

### Recording Process

```
Start Recording:
1. Create output directory if needed
2. Generate timestamped filename
3. Initialize recording buffers (separate L/R for stereo)
4. Set recording state to active

During Recording:
1. Pass-through: Copy input to output (unmodified)
2. Copy current buffer to recording buffer lists
3. Increment frame counter

Stop Recording:
1. Concatenate all buffers
2. Convert to 16-bit PCM
3. Write WAV file with proper headers
4. Clean up buffers
```

### Audio Format (Output File)

- **Format:** WAV (RIFF)
- **Encoding:** PCM Signed
- **Bit Depth:** 16-bit
- **Sample Rate:** Inherited from audio engine
- **Channels:** 1 (mono) or 2 (stereo) - auto-detected
- **Byte Order:** Little-endian

### Mono vs Stereo Detection

The recording mode is determined by which processing method is called:
- `onProcess()` called -> Mono recording (1 channel)
- `onProcessStereo()` called -> Stereo recording (2 channels)

### Buffer Management

Recording uses a list of float arrays to avoid large contiguous allocations:

```
Recording Buffers:
- recordedBuffersL: List<float[]> - Left/mono channel
- recordedBuffersR: List<float[]> - Right channel (stereo only)
- totalFramesRecorded: int - Frame count for duration calculation
```

Each buffer block is a copy of the processing buffer, stored sequentially.

### WAV File Structure

The effect writes standard WAV files with the following structure:

```
RIFF Chunk:
  - "RIFF" (4 bytes)
  - Chunk size (4 bytes)
  - "WAVE" (4 bytes)

fmt Sub-chunk:
  - "fmt " (4 bytes)
  - Sub-chunk size: 16 (4 bytes)
  - Audio format: 1 (PCM) (2 bytes)
  - Channels: 1 or 2 (2 bytes)
  - Sample rate (4 bytes)
  - Byte rate (4 bytes)
  - Block align (2 bytes)
  - Bits per sample: 16 (2 bytes)

data Sub-chunk:
  - "data" (4 bytes)
  - Data size (4 bytes)
  - PCM samples (variable)
```

### Float-to-PCM Conversion

For stereo recordings, samples are interleaved:
```
[L0, R0, L1, R1, L2, R2, ...]
```

Each sample is converted with clamping:
```java
sample = clamp(floatSample, -1.0, 1.0)
short = (int)(sample * 32767)
```

### Pass-Through Behavior

The effect is transparent to the signal chain:
- Input is copied directly to output
- Recording happens as a side-effect
- No latency is introduced
- Signal quality is unaffected

## Usage Tips

### Basic Recording
1. Add WavFileOutputEffect to your signal chain (typically near the end)
2. Set output directory using `setOutputDirectory()`
3. Optionally set base filename using `setBaseFileName()`
4. Toggle Recording parameter to start/stop

### Recording Location
```java
recorder.setOutputDirectory("/path/to/recordings");
recorder.setBaseFileName("guitar_take");
// Results in: /path/to/recordings/guitar_take_20231215143022.wav
```

### Quick Toggle
```java
recorder.toggleRecording();  // Start or stop based on current state
```

### Monitoring Recording
```java
if (recorder.isRecording()) {
    float duration = recorder.getRecordingDuration();
    String path = recorder.getCurrentRecordingPath();
    boolean stereo = recorder.isStereoRecording();
}
```

### Placement in Signal Chain
- **Before effects:** Records dry signal
- **After effects:** Records processed signal (wet)
- **Multiple recorders:** Can record at different points simultaneously

### File Management
- Default directory: User's home directory
- Files are never overwritten (timestamps ensure uniqueness)
- Release or reset saves any in-progress recording

## Technical Specifications

- **Output Format:** WAV (PCM)
- **Bit Depth:** 16-bit
- **Channels:** Auto-detected (mono or stereo)
- **Sample Rate:** Matches audio engine
- **Timestamp Format:** yyyyMMddHHmmss
- **Memory Usage:** Proportional to recording length
- **Pass-Through Latency:** 0 samples

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/WavFileOutputEffect.java`

**Key Methods:**
- `onProcess(...)` - Mono pass-through and recording
- `onProcessStereo(...)` - Stereo pass-through and recording
- `startRecording(int channels)` - Begin recording with specified channel count
- `stopRecording()` - End recording and save file
- `saveWavFile()` - Write accumulated buffers to disk
- `writeWavFile(...)` - Low-level WAV file writing

**Configuration:**
- `setOutputDirectory(String directory)` - Set save location
- `getOutputDirectory()` - Get current save location
- `setBaseFileName(String name)` - Set filename prefix
- `getBaseFileName()` - Get filename prefix

**Status Methods:**
- `isRecording()` - Check if currently recording
- `getCurrentRecordingPath()` - Get path of current/last recording
- `getRecordingDuration()` - Get duration in seconds
- `isStereoRecording()` - Check if recording in stereo mode
- `toggleRecording()` - Toggle recording state

## See Also

- [WavFileInputEffect](WavFileInputEffect.md) - For playing WAV files
- [AudioOutputEffect](AudioOutputEffect.md) - For real-time audio output
- [LooperEffect](LooperEffect.md) - For loop-based recording
