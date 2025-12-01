# Splitter Effect

## Overview

SplitterEffect is a utility effect that takes a single input signal and copies it to multiple outputs for parallel processing chains. It enables complex signal routing topologies such as parallel effects processing, wet/dry blending, and multi-amp configurations.

**Category:** Utility
**ID:** `splitter`
**Display Name:** Splitter

## Description

The Splitter is a routing utility rather than an audio processor. It passes the input signal through unchanged to its output while enabling the signal graph to route copies of the signal to multiple destinations. The actual signal duplication is handled by the node connection system - this effect provides the logical representation and output count configuration.

This effect is essential for building parallel processing chains where you want to process the same signal through different effect chains and then combine them (typically with a Mixer effect).

## Parameters

### Outputs
- **ID:** `outputs`
- **Range:** 2 to 4
- **Default:** 2
- **Unit:** (none)
- **Description:** Number of parallel output paths. Each output receives an identical copy of the input signal. The signal graph uses this value to determine how many connections can be made from this node.

## DSP Components

This effect has no DSP components - it performs a simple pass-through operation.

## Implementation Details

### Signal Flow

```
Input -> [Pass-Through] -> Output
            |
            +-> Node System handles duplication to connected outputs
```

### Processing Algorithm

The processing is trivially simple:

**Mono:**
```java
System.arraycopy(input, 0, output, 0, frameCount);
```

**Stereo:**
```java
System.arraycopy(inputL, 0, outputL, 0, frameCount);
System.arraycopy(inputR, 0, outputR, 0, frameCount);
```

The effect itself does not create multiple copies of the signal. Instead, it serves as a logical node in the signal graph that:
1. Indicates to the routing system that multiple connections are allowed from this point
2. Provides a visual representation in the UI for parallel signal paths
3. Stores the configured number of outputs for preset save/load

### Node System Integration

The `getOutputCount()` method returns the configured number of outputs, which the signal graph uses to:
- Validate connection attempts
- Render the appropriate number of output ports in the UI
- Serialize the routing configuration in presets

## Usage Tips

### Parallel Effects Processing
Create parallel effect chains where the same signal goes through different processors:
```
Guitar -> Splitter -> [Chain A: Distortion -> EQ] -> Mixer -> Output
                  +-> [Chain B: Clean Delay] -------^
```

### Wet/Dry Blend
Send one path through effects and one clean:
```
Guitar -> Splitter -> [Effects Chain] -> Mixer -> Output
                  +-> [Bypass/Clean] ----^
```

### Bi-Amp Configuration
Send to two different amp simulations:
```
Guitar -> Splitter -> [Amp A: Marshall] -> Mixer -> Output
                  +-> [Amp B: Fender] -----^
```

### Frequency Band Splitting (with filters)
Combined with filter effects for multi-band processing:
```
Guitar -> Splitter -> [Lowpass -> Bass Processing] -> Mixer -> Output
                  +-> [Highpass -> Treble Processing] --^
```

### 3-Way Split Example
For more complex routing:
```
Guitar -> Splitter (3 outputs) -> [Dry Path] -------------> Mixer -> Output
                              +-> [Distortion Path] --------^
                              +-> [Modulation Path] --------^
```

## Best Practices

### Gain Staging
- When mixing multiple parallel paths, reduce individual levels to avoid clipping
- Use the Mixer's gain compensation feature (-6 dB for 2 inputs, -9 dB for 3, etc.)
- Consider that parallel distortion can stack harmonics

### Phase Coherence
- Be aware that some effects introduce phase shifts
- Linear-phase EQ or matched processing helps maintain phase coherence
- Latency differences between chains may cause comb filtering

### CPU Efficiency
- More splits = more processing chains = more CPU usage
- Use the minimum number of outputs needed
- Consider freezing/bouncing complex parallel setups when recording

### Routing Clarity
- Keep signal flow left-to-right in the UI when possible
- Use descriptive names for effect chains
- Match every Splitter with a corresponding Mixer

## Common Parallel Configurations

### Classic Parallel Compression
```
Splitter -> [Dry] ----------------> Mixer
        +-> [Heavy Compression] ----^
```
Blend a compressed signal with the dry for punch without losing dynamics.

### Parallel Distortion
```
Splitter -> [Clean with slight EQ] -> Mixer
        +-> [High Gain Distortion] ---^
```
Maintains clarity and note definition while adding distortion thickness.

### Reverb/Delay Send
```
Splitter -> [Dry Signal] -----------------> Mixer
        +-> [Reverb at 100% wet] -----------^
        +-> [Delay at 100% wet] ------------^
```
Classic send/return style mixing with full control over effect levels.

## Technical Specifications

- **Latency:** 0 samples (pass-through)
- **CPU Usage:** Negligible (memory copy only)
- **Output Range:** 2-4 parallel outputs
- **Processing:** 32-bit float pass-through
- **Stereo Support:** Full stereo pass-through

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/SplitterEffect.java`

**Key Methods:**
- `onProcess(float[] input, float[] output, int frameCount)` - Mono pass-through
- `onProcessStereo(...)` - Stereo pass-through
- `getOutputCount()` - Returns configured number of outputs
- `setOutputCount(int count)` - Sets number of outputs (clamped to 2-4)

## See Also

- [MixerEffect](MixerEffect.md) - Combines multiple signals back together (complement to Splitter)
- [GainEffect](GainEffect.md) - Adjust levels in parallel paths
