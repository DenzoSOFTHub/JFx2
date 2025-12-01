# Limiter Effect

## Description

The Limiter is a brickwall peak limiter that prevents audio from exceeding a defined ceiling level. It provides transparent peak control and protection from digital clipping. Unlike a compressor, the limiter uses an extremely high ratio (effectively infinity:1) to create a hard ceiling that no signal can exceed. The implementation includes look-ahead processing for more transparent operation and reduced distortion on fast transients.

**Category:** Dynamics
**ID:** `limiter`
**Display Name:** Limiter

## How It Works

The limiter monitors the incoming signal level using a fast peak envelope follower. When the signal approaches the threshold, gain reduction is applied to keep the output below the ceiling. A 5ms look-ahead buffer allows the limiter to "see" peaks before they arrive, enabling smoother gain reduction that starts before the peak occurs. This reduces the harsh distortion that can result from instantaneous limiting.

## Parameters

### Threshold
- **ID:** `threshold`
- **Range:** -24 dB to 0 dB
- **Default:** -6 dB
- **Unit:** dB
- **Description:** Level above which limiting begins. Lower threshold values result in more limiting and louder perceived output. Set this to control how much limiting occurs. The limiter will reduce gain to prevent any signal from exceeding this level (plus the ceiling offset).

### Release
- **ID:** `release`
- **Range:** 10 ms to 1000 ms
- **Default:** 100 ms
- **Unit:** ms
- **Description:** How quickly the limiter recovers after peaks. Faster release times (10-50 ms) can cause audible distortion or pumping on sustained material. Slower release times (200-500 ms) sound more transparent but may cause the limiter to "hang" after loud passages.

### Ceiling
- **ID:** `ceiling`
- **Range:** -6 dB to 0 dB
- **Default:** -0.3 dB
- **Unit:** dB
- **Description:** Maximum output level that the signal will never exceed. Setting this below 0 dB provides headroom for inter-sample peaks and prevents clipping in downstream processing or conversion. A setting of -0.3 dB is recommended for most applications.

### Knee
- **ID:** `knee`
- **Range:** 0 dB to 6 dB
- **Default:** 0 dB (hard knee)
- **Unit:** dB
- **Description:** Softness of the limiting transition. A hard knee (0 dB) provides the most aggressive peak control. A soft knee (3-6 dB) creates a more gradual transition that can sound more natural on some material but may allow very brief overshoots.

## Implementation Details

### Signal Flow

```
Input --> Look-Ahead Buffer --> Gain Stage --> Hard Clip --> Output
              |                      ^
              v                      |
       Envelope Follower --> Gain Calculator
              |
              v
         Attack/Release
```

### Look-Ahead Processing

The limiter uses a 5ms look-ahead delay to improve transient handling:

```java
lookAheadSamples = (int)(0.005 * sampleRate)  // 5ms at sample rate
lookAheadL = new float[lookAheadSamples]
lookAheadR = new float[lookAheadSamples]
```

**How Look-Ahead Works:**
1. Input samples are written to a circular buffer
2. The envelope follower analyzes the current (incoming) signal
3. Gain reduction is calculated based on current peaks
4. The delayed signal from the buffer is multiplied by the gain reduction
5. Result: gain reduction begins before the actual peak arrives

This creates smoother, less distorted limiting because the gain starts decreasing before the transient rather than reacting after it has already clipped.

### Envelope Detection

The envelope follower uses peak detection with asymmetric attack/release:

```java
// Very fast attack (1ms) to catch transients
attackCoeff = exp(-1.0 / (0.001 * sampleRate))

// User-controlled release
releaseCoeff = exp(-1.0 / (releaseMs * sampleRate / 1000.0))

// Envelope following
if (absInput > envelope) {
    envelope = attackCoeff * envelope + (1 - attackCoeff) * absInput
} else {
    envelope = releaseCoeff * envelope
}
```

The fixed 1ms attack ensures that no peak is missed, while the adjustable release controls the recovery behavior.

### Gain Reduction Algorithm

The gain reduction uses a simple peak reduction algorithm with optional soft knee:

**Hard Knee (knee = 0):**
```java
if (envelope > threshold) {
    gainReduction = threshold / envelope
}
```

**Soft Knee (knee > 0):**
```java
overDb = 20 * log10(envelope / threshold)
if (overDb < knee) {
    // Soft knee region - quadratic curve
    kneeGain = overDb * overDb / (2 * knee)
    gainReduction = 10^(-kneeGain/20)
} else {
    // Full limiting
    gainReduction = threshold / envelope
}
```

### Safety Clipping

A final hard clip at the ceiling level provides a safety net in case any peaks slip through:

```java
limited = Math.max(-ceilingLin, Math.min(ceilingLin, limited))
```

This ensures the output absolutely never exceeds the ceiling, even if the envelope follower fails to catch an extremely fast transient.

### Stereo Linking

In stereo mode, the limiter uses linked detection:

```java
// Use maximum of both channels for detection
absInput = Math.max(Math.abs(sampleL), Math.abs(sampleR))
```

This ensures both channels receive identical gain reduction, preserving the stereo image. Without linking, a loud sound on one channel would be reduced more than the other, causing the image to shift.

## Usage Tips

### Transparent Peak Control
- **Threshold:** -3 to -1 dB
- **Release:** 150-250 ms
- **Ceiling:** -0.3 dB
- **Knee:** 0 dB
- Catches only the highest peaks with minimal audible effect

### Loudness Maximizing
- **Threshold:** -8 to -6 dB
- **Release:** 50-100 ms
- **Ceiling:** -0.3 dB
- **Knee:** 0 dB
- Increases average loudness while preventing clipping

### Mastering Limiter
- **Threshold:** -3 to -1 dB
- **Release:** 200-400 ms
- **Ceiling:** -0.5 to -1.0 dB
- **Knee:** 0 dB
- Conservative limiting for final output

### Aggressive Limiting
- **Threshold:** -12 to -8 dB
- **Release:** 30-50 ms
- **Ceiling:** -0.3 dB
- **Knee:** 0 dB
- Significant gain reduction for very loud output (may cause audible artifacts)

### Smooth Limiting
- **Threshold:** -6 to -3 dB
- **Release:** 100-200 ms
- **Ceiling:** -0.3 dB
- **Knee:** 3-6 dB
- Softer transitions for less aggressive sound

### Pre-Recording Safety
- **Threshold:** -1 to 0 dB
- **Release:** 100 ms
- **Ceiling:** -0.1 dB
- **Knee:** 0 dB
- Prevents digital clipping during recording without affecting dynamics

## Technical Specifications

- **Algorithm:** Look-ahead brickwall limiter
- **Look-Ahead:** 5 ms
- **Attack Time:** Fixed at 1 ms (internal)
- **Release Time:** User adjustable (10-1000 ms)
- **Stereo Mode:** Linked (identical gain reduction both channels)
- **Processing:** 32-bit float internal
- **Latency:** 5 ms (equal to look-ahead time)
- **Safety Clip:** Hard clip at ceiling level
- **CPU Usage:** Low

## Latency Compensation

The limiter introduces 5ms of latency due to the look-ahead buffer. This latency is reported by the `getLatency()` method:

```java
@Override
public int getLatency() {
    return lookAheadSamples;  // 5ms worth of samples
}
```

When using the limiter in a signal chain, the host or signal graph should compensate for this delay to maintain proper timing with other effects.

## DSP Theory

### Why Look-Ahead?

Without look-ahead, a limiter must react after the peak has already occurred:

1. Peak arrives
2. Envelope follower detects it
3. Gain reduction is calculated
4. Next sample is reduced

This means the first part of the transient passes through before limiting can take effect. If the transient is fast enough, it may clip before the limiter can respond.

With look-ahead:

1. Peak enters the buffer
2. Envelope follower detects it immediately
3. Gain reduction is calculated
4. 5ms later, when the peak exits the buffer, gain is already reduced

The result is smooth, predictable limiting without the harsh distortion of instantaneous peak clipping.

### Inter-Sample Peaks

Digital audio can contain "inter-sample peaks" - theoretical analog values between samples that exceed 0 dBFS. When converted to analog or resampled, these can cause clipping even if no digital sample exceeds 0 dBFS.

Setting the ceiling to -0.3 dB or lower provides headroom for these inter-sample peaks:
- -0.1 dB: Minimal headroom (may still have inter-sample clips)
- -0.3 dB: Recommended for most applications
- -0.5 dB: Conservative, safe for all downstream processing
- -1.0 dB: Very conservative, useful before lossy encoding

### Linked vs. Unlinked Stereo

**Linked (used in this implementation):**
- Same gain reduction applied to both channels
- Preserves stereo image
- A loud peak on one side reduces both sides equally

**Unlinked:**
- Independent gain reduction per channel
- Maximizes each channel independently
- Can cause stereo image to "wander" or "pump"

For most guitar and music applications, linked operation is preferred.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/LimiterEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize look-ahead buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Mono limiting
- `onProcessStereo(...)` - Stereo limiting with linked detection
- `onReset()` - Clear buffers and reset envelope
- `getLatency()` - Returns look-ahead delay in samples

**Key Constants:**
- Look-ahead time: 5 ms
- Attack time: 1 ms (fixed internal)

## See Also

- [CompressorEffect](CompressorEffect.md) - Dynamic range compression with adjustable ratio
- [NoiseGateEffect](NoiseGateEffect.md) - Noise reduction via gating
- [GainEffect](GainEffect.md) - Simple gain/volume control
