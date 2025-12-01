# Mixer Effect

## Overview

MixerEffect is a utility effect that combines multiple input signals into a single output. It is the complement to the SplitterEffect and is typically used to merge parallel processing chains back together with gain compensation.

**Category:** Utility
**ID:** `mixer`
**Display Name:** Mixer

## Description

The Mixer is a routing utility that combines multiple audio signals into one. While the actual signal summing is handled by the node connection system, this effect provides gain compensation to prevent clipping when summing multiple signals, and serves as the logical representation in the signal graph for merging parallel paths.

This effect is essential for completing parallel processing chains started with a Splitter, allowing you to blend multiple processed versions of a signal.

## Parameters

### Inputs
- **ID:** `inputs`
- **Range:** 2 to 4
- **Default:** 2
- **Unit:** (none)
- **Description:** Number of input signals to accept. The signal graph uses this value to determine how many connections can be made to this node. Should typically match the output count of the corresponding Splitter.

### Output Gain
- **ID:** `gain`
- **Range:** -12 dB to +6 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Output gain adjustment to compensate for signal summing. When mixing multiple signals, the sum can exceed unity gain. Use negative values to prevent clipping: -6 dB for 2 inputs, -9 dB for 3, -12 dB for 4.

## DSP Components

This effect has minimal DSP - it applies a simple gain multiplication to the input signal.

## Implementation Details

### Signal Flow

```
Node System sums all connected inputs -> [Gain Adjustment] -> Output
```

### Processing Algorithm

**Mono:**
```java
float gainLinear = dbToLinear(gainDb);
for (int i = 0; i < frameCount; i++) {
    output[i] = input[i] * gainLinear;
}
```

**Stereo:**
```java
float gainLinear = dbToLinear(gainDb);
for (int i = 0; i < frameCount; i++) {
    outputL[i] = inputL[i] * gainLinear;
    outputR[i] = inputR[i] * gainLinear;
}
```

### dB to Linear Conversion

The gain parameter is stored in decibels and converted to a linear multiplier:
```java
gainLinear = 10^(dB / 20)
```

For example:
- 0 dB = 1.0 (unity gain)
- -6 dB = 0.5 (half amplitude)
- -12 dB = 0.25 (quarter amplitude)
- +6 dB = 2.0 (double amplitude)

### Node System Integration

The `getInputCount()` method returns the configured number of inputs, which the signal graph uses to:
- Validate connection attempts
- Render the appropriate number of input ports in the UI
- Sum all connected input signals before passing to the effect's process method
- Serialize the routing configuration in presets

## Usage Tips

### Gain Staging Guidelines

When summing multiple signals, use these gain reduction values to maintain approximately unity gain:

| Inputs | Recommended Gain |
|--------|------------------|
| 2      | -6 dB            |
| 3      | -9 dB            |
| 4      | -12 dB           |

These values assume all inputs are at similar levels. Adjust as needed based on your specific signal levels.

### Parallel Compression
```
Splitter -> [Dry] ----------------> Mixer (gain: -6 dB) -> Output
        +-> [Heavy Compression] ----^
```
The mixer combines the punchy compressed signal with the dynamic dry signal.

### Wet/Dry Blend
```
Splitter -> [100% Wet Effects] -> Mixer (gain: -6 dB) -> Output
        +-> [Clean/Dry] -----------^
```
Control the effect intensity by adjusting levels before the mixer.

### Multi-Amp Blend
```
Splitter -> [Marshall Sim] -> Mixer (gain: -6 dB) -> Output
        +-> [Fender Sim] ------^
```
Blend two amp characters for a unique tone.

### 3-Way Mix
```
Splitter -> [Clean] ------> Mixer (gain: -9 dB) -> Output
        +-> [Distortion] ---^
        +-> [Modulation] ---^
```
Three parallel paths require approximately -9 dB compensation.

## Best Practices

### Preventing Clipping
- Always apply appropriate gain reduction when mixing multiple signals
- Use the Output Gain parameter rather than post-mixer gain stages
- Monitor output levels and adjust as needed

### Matching Splitter/Mixer Pairs
- Each Splitter should have a corresponding Mixer downstream
- Match input/output counts for cleaner signal flow diagrams
- Label chains clearly in complex configurations

### Level Balancing
- Adjust individual chain levels before the mixer
- Use the mixer gain for overall compensation, not balance
- Consider using individual Gain effects in each chain for fine control

### Phase Considerations
- Summing out-of-phase signals causes cancellation
- Be aware of phase shifts introduced by effects in parallel chains
- Test for phase issues by soloing individual chains

## Common Mixing Scenarios

### Conservative Mix (Headroom Priority)
- **Output Gain:** -12 dB for 2 inputs
- Plenty of headroom, may need makeup gain later
- Good for mastering chains or when precision matters

### Moderate Mix (Balanced)
- **Output Gain:** -6 dB for 2 inputs
- Standard approach for most parallel processing
- Maintains approximate unity gain

### Hot Mix (Maximum Level)
- **Output Gain:** 0 dB or slight positive
- Assumes input signals are already gain-staged
- Use only when signals are known to be below unity

## Technical Specifications

- **Latency:** 0 samples (gain only)
- **CPU Usage:** Negligible (simple multiplication)
- **Input Range:** 2-4 signals
- **Gain Range:** -12 dB to +6 dB
- **Processing:** 32-bit float
- **Stereo Support:** Full stereo processing

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/MixerEffect.java`

**Key Methods:**
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing with gain
- `onProcessStereo(...)` - Stereo processing with gain
- `getInputCount()` - Returns configured number of inputs
- `setInputCount(int count)` - Sets number of inputs (clamped to 2-4)
- `getGainDb()` - Returns current output gain in dB
- `setGainDb(float dB)` - Sets output gain in dB

## See Also

- [SplitterEffect](SplitterEffect.md) - Splits signal into multiple paths (complement to Mixer)
- [GainEffect](GainEffect.md) - For individual path level adjustment
