# AudioInputEffect

## Overview

AudioInputEffect is a generator effect that captures audio from a hardware input device (microphone, audio interface, etc.) and outputs it to the signal chain. This is the primary way to get live audio into the JFx2 processing graph.

**Category:** Input Source
**ID:** `audioinput`
**Display Name:** Audio Input

## Description

This effect provides the entry point for live audio signals into JFx2. It supports device selection from all available input devices on the system, with gain control and muting capabilities. The effect automatically handles device conflicts when multiple instances try to use the same hardware.

## Parameters

### Device
- **ID:** `device`
- **Type:** Choice
- **Options:** Dynamically populated from available system input devices
- **Default:** Default Input Device (system default)
- **Description:** Select the audio input device to capture from. Available options depend on the connected hardware (microphones, audio interfaces, line inputs, etc.).

### Gain
- **ID:** `gain`
- **Range:** -60 dB to +24 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Input level adjustment. 0 dB = unity gain (no amplification or attenuation). Use positive values to boost quiet signals, negative values to reduce hot signals.

### Mute
- **ID:** `mute`
- **Type:** Boolean
- **Default:** Off (false)
- **Description:** Mute this input. When enabled, the effect outputs silence regardless of the input signal.

## Implementation Details

### Audio Format

The effect captures audio in the following format:
- **Encoding:** PCM Signed
- **Sample Rate:** Inherited from audio engine (typically 44100 Hz)
- **Bit Depth:** 16-bit
- **Channels:** 1 (mono input)
- **Byte Order:** Little-endian

### Device Management

AudioInputEffect implements a global device registry to prevent conflicts:

```
Device Selection Flow:
1. Check if new device is already in use by another instance
2. Release current device (if any)
3. Update global registry
4. Open new device with retry logic
5. Start audio capture
```

Key features:
- **Conflict Detection:** Prevents multiple instances from opening the same device simultaneously
- **Automatic Retry:** If device opening fails, the effect attempts a reset and retry once
- **Hot-Swapping:** Device can be changed while the effect is running
- **Graceful Fallback:** If a specific device fails, the system default can be used

### Signal Flow

```
Hardware Input Device
        |
        v
  TargetDataLine (Java Sound API)
        |
        v
  Byte Buffer (16-bit PCM)
        |
        v
  bytesToFloats() conversion
        |
        v
  Gain/Mute Processing
        |
        v
  Output Buffer (32-bit float)
```

### Gain Processing

Gain is applied in the linear domain after dB conversion:

```java
gainLinear = 10^(gainDb / 20)
output[i] = input[i] * gainLinear
```

When muted, gainLinear is set to 0.0.

### Stereo Handling

For stereo processing mode:
- Mono input is captured from the device
- The same signal is copied to both left and right output channels
- This provides consistent behavior regardless of downstream stereo/mono routing

### Buffer Management

- Uses pre-allocated byte and float buffers to avoid garbage collection during processing
- Buffer sizes are determined at prepare time based on `maxFrameCount`
- 16-bit audio requires 2 bytes per sample: `bufferBytes = maxFrameCount * 2`

## Usage Tips

### Basic Live Input Setup
1. Add AudioInputEffect as the first node in your signal chain
2. Select your audio interface or microphone from the Device parameter
3. Adjust Gain to achieve optimal input level without clipping
4. Monitor input levels in the downstream signal chain

### Dealing with Device Issues
- If a device fails to open, check that it's not in use by another application
- The "Default Input Device" option is useful when the specific device is unavailable
- Use `hasError()` and `getLastError()` methods to diagnose issues programmatically

### Multiple Inputs
- Create multiple AudioInputEffect instances for multi-input scenarios
- Each instance should select a different device
- The global registry prevents device conflicts automatically

### Low Latency Tips
- Use smaller buffer sizes in the audio engine configuration
- Select ASIO-compatible interfaces on Windows for lowest latency
- Avoid excessive gain as it can introduce noise

## Technical Specifications

- **Sample Rate:** Matches audio engine (typically 44100 Hz)
- **Bit Depth:** 16-bit capture, 32-bit float internal processing
- **Channels:** Mono capture, stereo output available
- **Latency:** Buffer-dependent, typically one buffer's worth
- **CPU Usage:** Very low (primarily I/O bound)
- **Thread Safety:** Device registry uses ConcurrentHashMap

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/AudioInputEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes audio format and buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Captures and processes mono audio
- `onProcessStereo(...)` - Captures mono and outputs to stereo
- `updateDevice()` - Handles device selection changes
- `openDeviceWithRetry(boolean allowRetry)` - Opens device with fallback logic
- `bytesToFloats(...)` - Converts 16-bit PCM to 32-bit float

**Public API:**
- `getDeviceName()` - Returns current device name
- `isDeviceOpen()` - Checks if device is active
- `getDeviceInfo()` - Returns formatted device info string
- `getLastError()` - Returns error message if device failed
- `hasError()` - Checks for device errors

**Static Methods:**
- `getAvailableDevices()` - Lists devices not currently in use
- `isDeviceInUse(String deviceName)` - Checks if a specific device is in use

## See Also

- [AudioOutputEffect](AudioOutputEffect.md) - For sending audio to output devices
- [WavFileInputEffect](WavFileInputEffect.md) - For playing audio from files
