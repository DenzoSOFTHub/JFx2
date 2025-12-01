# Compressor Effect

## Description

The Compressor effect reduces dynamic range by automatically attenuating signals that exceed a user-defined threshold. It uses RMS (Root Mean Square) level detection for a smooth, musical response that responds to the average signal energy rather than instantaneous peaks. This makes it ideal for evening out performance dynamics while maintaining a natural, transparent sound.

**Category:** Dynamics
**ID:** `compressor`
**Display Name:** Compressor

## How It Works

The compressor continuously monitors the input signal level using an RMS detector with a 50ms averaging window. When the RMS level exceeds the threshold, gain reduction is applied according to the compression ratio. The attack and release parameters control how quickly the compressor responds to level changes, while the soft knee provides a gradual transition into compression for more transparent operation.

## Parameters

### Threshold
- **ID:** `threshold`
- **Range:** -60 dB to 0 dB
- **Default:** -20 dB
- **Unit:** dB
- **Description:** Level above which compression begins. Lower values compress more of the signal, affecting quieter passages. Higher values only compress the loudest peaks. Set this just above the average level of material you want to remain uncompressed.

### Ratio
- **ID:** `ratio`
- **Range:** 1:1 to 20:1
- **Default:** 4:1
- **Unit:** :1
- **Description:** Amount of compression applied to signals above the threshold. A ratio of 4:1 means that for every 4 dB the input exceeds the threshold, the output only increases by 1 dB. Higher ratios provide more aggressive compression; ratios above 10:1 approach limiting behavior.

### Attack
- **ID:** `attack`
- **Range:** 0.1 ms to 100 ms
- **Default:** 10 ms
- **Unit:** ms
- **Description:** How quickly compression engages when signal exceeds the threshold. Faster attack times (0.1-5 ms) catch transients but may reduce punch. Slower attack times (20-100 ms) let transients through, preserving the initial impact of notes.

### Release
- **ID:** `release`
- **Range:** 10 ms to 1000 ms
- **Default:** 100 ms
- **Unit:** ms
- **Description:** How quickly compression releases after signal drops below the threshold. Faster release (10-50 ms) can cause audible pumping or breathing. Slower release (200-1000 ms) sounds more natural but may not recover in time for the next transient.

### Knee
- **ID:** `knee`
- **Range:** 0 dB to 12 dB
- **Default:** 3 dB
- **Unit:** dB
- **Description:** Transition smoothness around the threshold. A hard knee (0 dB) applies the full ratio immediately above threshold. A soft knee (6-12 dB) gradually increases compression over a range centered on the threshold, resulting in more transparent, musical compression.

### Makeup
- **ID:** `makeup`
- **Range:** 0 dB to 24 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Compensates for volume lost during compression. Since compression reduces peak levels, makeup gain restores the overall loudness. A good starting point is to add makeup gain equal to the average gain reduction shown on meters.

## Implementation Details

### Signal Flow

```
Input --> RMS Detector --> Gain Calculator --> Gain Smoother --> Output
                |               |                    |
                v               v                    v
          Level (dB)      Gain Reduction      Attack/Release
                               |
                               v
                          Soft Knee
```

### RMS Level Detection

The compressor uses RMS (Root Mean Square) detection with a 50ms window for smooth, musical response:

```java
rmsCoeff = exp(-1.0 / (50.0 * sampleRate / 1000.0))
rmsLevel = rmsCoeff * rmsLevel + (1 - rmsCoeff) * (sample * sample)
rmsDb = 20 * log10(sqrt(rmsLevel))
```

RMS detection responds to the average energy of the signal rather than instantaneous peaks. This produces:
- More natural-sounding compression
- Less sensitivity to brief transients
- Behavior similar to how human hearing perceives loudness

### Gain Reduction Calculation

The gain reduction algorithm implements soft knee compression:

**Below Knee (input < threshold - knee/2):**
- No compression applied
- Gain reduction = 0 dB

**Above Knee (input > threshold + knee/2):**
- Full compression applied
- Gain reduction = -(overshoot * (1 - 1/ratio))

**Within Knee Region:**
- Quadratic interpolation for smooth transition
- Gain reduction = -(x^2 * (1 - 1/ratio)) / (2 * kneeWidth)
- Where x = overshoot + knee/2

### Attack/Release Smoothing

Gain changes are smoothed using one-pole filters:

```java
// Time constant calculation
attackCoeff = exp(-1.0 / (attackMs * sampleRate / 1000.0))
releaseCoeff = exp(-1.0 / (releaseMs * sampleRate / 1000.0))

// Apply appropriate coefficient based on gain direction
if (targetGain < currentGain) {
    // Attack: gain is decreasing (more compression)
    currentGain = attackCoeff * currentGain + (1 - attackCoeff) * targetGain
} else {
    // Release: gain is increasing (less compression)
    currentGain = releaseCoeff * currentGain + (1 - releaseCoeff) * targetGain
}
```

This asymmetric smoothing allows independent control of how fast the compressor responds to increasing vs. decreasing levels.

### Stereo Processing

In stereo mode, each channel is processed independently with its own:
- RMS level detector
- Gain reduction calculation
- Attack/release envelope

This maintains stereo imaging but may cause slight stereo imbalance under heavy compression. For linked stereo operation, use both channels with identical settings.

## Usage Tips

### Transparent Leveling
- **Threshold:** -24 to -18 dB
- **Ratio:** 2:1 to 3:1
- **Attack:** 20-30 ms
- **Release:** 150-250 ms
- **Knee:** 6-9 dB
- **Makeup:** 2-4 dB
- Gentle compression that evens out dynamics without obvious artifacts

### Punchy Rhythm Guitar
- **Threshold:** -20 to -15 dB
- **Ratio:** 4:1 to 6:1
- **Attack:** 15-25 ms (let initial pick attack through)
- **Release:** 100-150 ms
- **Knee:** 3-6 dB
- **Makeup:** 4-8 dB
- Preserves attack while controlling sustain

### Sustain Enhancement
- **Threshold:** -30 to -25 dB
- **Ratio:** 6:1 to 10:1
- **Attack:** 5-15 ms
- **Release:** 300-500 ms
- **Knee:** 3 dB
- **Makeup:** 8-12 dB
- Brings up quiet notes and tails for more even sustain

### Lead Guitar Boost
- **Threshold:** -25 to -20 dB
- **Ratio:** 4:1 to 6:1
- **Attack:** 10-20 ms
- **Release:** 200-300 ms
- **Knee:** 6 dB
- **Makeup:** 6-10 dB
- Controls peaks while maintaining expressiveness

### Clean Jazz Tone
- **Threshold:** -18 to -12 dB
- **Ratio:** 2:1 to 3:1
- **Attack:** 30-50 ms
- **Release:** 200-300 ms
- **Knee:** 9-12 dB
- **Makeup:** 2-4 dB
- Very subtle, transparent compression

### Aggressive Limiting
- **Threshold:** -15 to -10 dB
- **Ratio:** 15:1 to 20:1
- **Attack:** 0.5-2 ms
- **Release:** 50-100 ms
- **Knee:** 0-3 dB
- **Makeup:** 6-10 dB
- Near-brickwall limiting for maximum loudness

## Technical Specifications

- **Algorithm:** Feed-forward RMS compression with soft knee
- **Detection:** RMS with 50ms averaging window
- **Processing:** 32-bit float internal
- **Latency:** Zero latency (no look-ahead)
- **Stereo Mode:** Dual-mono (independent channels)
- **Knee Type:** Quadratic soft knee
- **CPU Usage:** Low (simple per-sample calculations)

## DSP Theory

### Why RMS Detection?

Peak detection responds to instantaneous sample values, which can:
- React to brief transients that may not be perceptually loud
- Cause rapid gain changes that are audible as distortion
- Miss the true "loudness" of sustained sounds

RMS detection averages the signal power over time, which:
- Better correlates with perceived loudness
- Provides smoother, more musical gain reduction
- Responds to sustained energy rather than brief spikes

The 50ms window is a good compromise between:
- Fast enough to respond to musical dynamics
- Slow enough to avoid pumping on individual cycles

### Soft Knee Mathematics

Hard knee compression creates a sharp transition that can be audible as "grabbing." The soft knee uses a parabolic curve to create a gradual increase in ratio:

At threshold - knee/2: ratio = 1:1 (no compression)
At threshold: ratio = sqrt(target_ratio) (partial compression)
At threshold + knee/2: ratio = target_ratio (full compression)

This creates an audibly smoother transition that is particularly noticeable on program material with varying dynamics.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/CompressorEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize RMS coefficient and state
- `onProcess(float[] input, float[] output, int frameCount)` - Mono compression
- `onProcessStereo(...)` - Stereo compression with independent channels
- `calculateGainReduction(...)` - Soft knee gain calculation
- `getGainReductionDb()` - Returns current gain reduction for metering

## See Also

- [LimiterEffect](LimiterEffect.md) - Brickwall limiting for peak control
- [NoiseGateEffect](NoiseGateEffect.md) - Noise reduction via gating
- [SustainerEffect](SustainerEffect.md) - Infinite sustain simulation
