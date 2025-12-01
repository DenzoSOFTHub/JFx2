# AudioOutputEffect

## Overview

AudioOutputEffect is a sink effect that sends audio from the signal chain to a hardware output device (speakers, headphones, audio interface, etc.). This is the primary way to hear processed audio from JFx2.

**Category:** Output Sink
**ID:** `audiooutput`
**Display Name:** Audio Output

## Description

This effect provides the exit point for audio signals from JFx2 to physical output devices. It supports device selection, gain control, muting, and includes output level metering for monitoring. Multiple outputs can send to different devices simultaneously, enabling advanced routing scenarios.

## Parameters

### Device
- **ID:** `device`
- **Type:** Choice
- **Options:** Dynamically populated from available system output devices
- **Default:** Default Output Device (system default)
- **Description:** Select the audio output device to send audio to. Available options depend on connected hardware (speakers, headphones, audio interfaces, etc.).

### Gain
- **ID:** `gain`
- **Range:** -60 dB to +24 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Output level adjustment. 0 dB = unity gain. Use to match output level to your monitoring setup or prevent clipping.

### Mute
- **ID:** `mute`
- **Type:** Boolean
- **Default:** Off (false)
- **Description:** Mute this output. When enabled, silence is sent to the device. Useful for quick A/B comparisons or preventing unexpected loud sounds.

## Implementation Details

### Audio Format

The effect outputs audio in the following format:
- **Encoding:** PCM Signed
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Bit Depth:** 16-bit
- **Channels:** 2 (stereo output)
- **Byte Order:** Little-endian

### Device Management

AudioOutputEffect implements a global device registry similar to AudioInputEffect:

```
Device Selection Flow:
1. Check if new device is already in use by another instance
2. Release current device (if any)
3. Update global registry
4. Open new device
5. Start audio output stream
```

Key features:
- **Conflict Prevention:** Global registry tracks devices in use
- **Hot-Swapping:** Device can be changed during operation
- **Pass-Through:** Audio passes through to downstream effects while also being sent to hardware

### Signal Flow

```
Input Buffer (32-bit float)
        |
        v
  Pass-Through to Output Buffer
        |
        v
  Mono-to-Stereo Conversion (if mono input)
        |
        v
  Gain/Mute Processing
        |
        v
  Level Metering (RMS calculation)
        |
        v
  floatsToBytes() conversion
        |
        v
  SourceDataLine (Java Sound API)
        |
        v
  Hardware Output Device
```

### Gain and Level Processing

Gain is applied in the linear domain with soft clipping:

```java
gainLinear = 10^(gainDb / 20)
sample = sample * gainLinear
sample = clamp(sample, -1.0, 1.0)  // Prevent digital clipping
```

RMS level calculation for metering:
```java
rms = sqrt(sumOfSquares / sampleCount)
levelDb = 20 * log10(rms)
levelDb = clamp(levelDb, -60, 0)
```

### Mono-to-Stereo Handling

For mono input (onProcess):
1. Input mono samples are duplicated to both channels
2. Gain is applied to the stereo signal
3. Interleaved stereo is written to the device

For stereo input (onProcessStereo):
1. Left and right channels are interleaved: [L0, R0, L1, R1, ...]
2. Gain is applied
3. Interleaved stereo is written to the device

### Buffer Management

- Pre-allocated byte and float buffers avoid GC during processing
- Stereo output requires: `bufferBytes = maxFrameCount * channels * 2`
- Float buffer holds interleaved stereo: `floatBuffer = maxFrameCount * 2`

### Float-to-Byte Conversion

16-bit PCM conversion with clamping:
```java
clamped = clamp(float, -1.0, 1.0)
short = (int)(clamped * 32767)
bytes[0] = short & 0xFF        // Low byte
bytes[1] = (short >> 8) & 0xFF // High byte
```

## Usage Tips

### Basic Output Setup
1. Add AudioOutputEffect as the last node in your signal chain
2. Select your speakers/headphones from the Device parameter
3. Start with Gain at 0 dB
4. Adjust Gain if output is too loud/quiet

### Multiple Output Routing
- Create multiple AudioOutputEffect instances for different outputs
- Example: Main mix to speakers, separate feed to headphones
- Each output can have independent gain and mute settings

### Level Monitoring
- Use `getOutputLevelDb()` to monitor output level programmatically
- Build level meters in UI by polling this value
- Level is calculated post-gain for accurate monitoring

### Preventing Clipping
- Watch for output levels consistently hitting 0 dB
- Reduce Gain parameter if clipping occurs
- The effect automatically hard-clips at +/- 1.0 to prevent digital distortion

### Quick Muting
- Use Mute parameter for instant silence
- Unlike setting Gain to -60 dB, Mute provides true silence (0.0 samples)
- Useful for quick comparisons or emergency silence

## Technical Specifications

- **Sample Rate:** Matches audio engine (typically 44100 Hz)
- **Bit Depth:** 16-bit output
- **Channels:** Stereo (2 channels)
- **Internal Processing:** 32-bit float
- **Latency:** Buffer-dependent, typically one buffer's worth plus device latency
- **CPU Usage:** Very low (primarily I/O bound)
- **Level Meter Range:** -60 dB to 0 dB
- **Thread Safety:** Device registry uses ConcurrentHashMap

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/AudioOutputEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes stereo audio format and buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Mono input to stereo output
- `onProcessStereo(...)` - Stereo input to stereo output
- `updateDevice()` - Handles device selection changes
- `applyGain(...)` - Applies gain and calculates RMS level
- `floatsToBytes(...)` - Converts 32-bit float to 16-bit PCM

**Public API:**
- `getDeviceName()` - Returns current device name
- `isDeviceOpen()` - Checks if device is active
- `getDeviceInfo()` - Returns formatted device info string
- `getOutputLevelDb()` - Returns current output level in dB (for metering)

**Static Methods:**
- `getAvailableDevices()` - Lists devices not currently in use
- `isDeviceInUse(String deviceName)` - Checks if a specific device is in use

## See Also

- [AudioInputEffect](AudioInputEffect.md) - For capturing audio from input devices
- [WavFileOutputEffect](WavFileOutputEffect.md) - For recording audio to files
