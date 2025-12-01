# Noise Gate Effect

## Description

The Noise Gate reduces unwanted noise by attenuating the signal when it falls below a threshold level. Unlike a compressor which reduces loud signals, a gate reduces quiet signals - typically hum, hiss, or amp noise that is audible between notes. The implementation includes a 5ms look-ahead buffer to preserve the natural attack of notes, and a hold time to prevent the gate from chattering on sustained notes with varying dynamics.

**Category:** Dynamics
**ID:** `noisegate`
**Display Name:** Noise Gate

## How It Works

The noise gate monitors the input signal level using a fast peak envelope follower. When the level drops below the threshold, the gate begins to close, attenuating the signal by the amount specified by the range parameter. When the level rises above the threshold, the gate opens, allowing the full signal through. The look-ahead buffer ensures that the beginning of notes is not cut off, while the hold time prevents rapid opening and closing on sustained notes.

## Parameters

### Threshold
- **ID:** `threshold`
- **Range:** -80 dB to 0 dB
- **Default:** -40 dB
- **Unit:** dB
- **Description:** Level below which the gate closes. Set this just above your noise floor for optimal noise reduction without affecting your playing. Too low and noise will pass through; too high and quiet notes or note tails will be cut off.

### Attack
- **ID:** `attack`
- **Range:** 0.1 ms to 50 ms
- **Default:** 1 ms
- **Unit:** ms
- **Description:** How quickly the gate opens when signal exceeds threshold. Very fast attack (0.1-2 ms) preserves transients but may cause clicks. Slower attack (10-50 ms) sounds smoother but may soften the initial pick attack.

### Hold
- **ID:** `hold`
- **Range:** 0 ms to 500 ms
- **Default:** 50 ms
- **Unit:** ms
- **Description:** Time the gate stays open after signal drops below threshold. Prevents the gate from chattering on and off during sustained notes or notes with varying dynamics. Longer hold times are more forgiving but allow more noise through during pauses.

### Release
- **ID:** `release`
- **Range:** 5 ms to 500 ms
- **Default:** 100 ms
- **Unit:** ms
- **Description:** How quickly the gate closes after the hold time expires. Faster release (5-30 ms) cuts off noise more abruptly but may sound unnatural. Slower release (100-500 ms) provides a smooth fade-out that sounds more natural but allows more noise tail.

### Range
- **ID:** `range`
- **Range:** -80 dB to 0 dB
- **Default:** -80 dB
- **Unit:** dB
- **Description:** Maximum attenuation when the gate is fully closed. -80 dB is effectively silence. Higher values (e.g., -20 dB) allow some signal through even when closed, which can sound more natural and less obvious. A range of 0 dB bypasses the gate entirely.

## Implementation Details

### Signal Flow

```
Input --> Look-Ahead Buffer --> Gain Stage --> Output
              |                      ^
              v                      |
        Peak Detector                |
              |                      |
              v                      |
        Gate Logic (Open/Hold/Close) |
              |                      |
              v                      |
        Attack/Release Envelope -----+
```

### Look-Ahead Processing

The noise gate uses a 5ms look-ahead delay to preserve transient attacks:

```java
LOOKAHEAD_MS = 5.0f
lookaheadSamples = (int)(LOOKAHEAD_MS * sampleRate / 1000.0f)
lookaheadBuffer = new float[lookaheadSamples + maxFrameCount]
```

**How Look-Ahead Works:**
1. The current sample is written to the look-ahead buffer
2. The envelope follower analyzes the current (incoming) signal
3. If the signal exceeds threshold, the gate begins opening immediately
4. The delayed sample from 5ms ago is multiplied by the current gate gain
5. Result: the gate opens before the transient arrives, preserving the attack

Without look-ahead, fast transients would be partially clipped before the gate could open.

### Envelope Detection

The gate uses a fast peak detector (0.5ms time constant) to catch transients:

```java
envCoeff = exp(-1.0 / (0.5 * sampleRate / 1000.0))

if (absInput > envelope) {
    envelope = absInput  // Instant attack for detection
} else {
    envelope = envCoeff * envelope + (1 - envCoeff) * absInput
}
```

The instant attack ensures no transient is missed, while the 0.5ms decay provides a stable level reading.

### Gate State Machine

The gate operates in three states:

**1. Open (signal above threshold):**
```java
if (envelope > thresholdLinear) {
    holdCounter = holdSamples  // Reset hold timer
    gateGain -> 1.0  // Open gate (attack smoothing applied)
}
```

**2. Hold (signal below threshold, within hold time):**
```java
if (holdCounter > 0) {
    holdCounter--
    gateGain -> 1.0  // Keep gate open
}
```

**3. Close (signal below threshold, hold expired):**
```java
if (holdCounter == 0) {
    gateGain -> rangeLinear  // Close gate (release smoothing applied)
}
```

### Attack/Release Smoothing

Gain changes are smoothed to prevent clicks:

```java
attackCoeff = exp(-1.0 / (attackMs * sampleRate / 1000.0))
releaseCoeff = exp(-1.0 / (releaseMs * sampleRate / 1000.0))

// Opening gate (attack)
gateGain = attackCoeff * gateGain + (1 - attackCoeff) * 1.0

// Closing gate (release)
gateGain = releaseCoeff * gateGain + (1 - releaseCoeff) * rangeLinear
```

The attack coefficient controls how fast the gate opens; the release coefficient controls how fast it closes.

### Stereo Processing

In stereo mode, each channel has independent:
- Envelope follower
- Gate state (open/hold/close)
- Attack/release envelope

This allows the gate to respond independently to each channel, which is useful when the channels have different content. For linked stereo operation (both channels gated together), use the maximum of both channels as the detection signal.

### Metering

The implementation provides metering values for UI visualization:

```java
getInputLevelDb()      // Current input level (-80 to 0 dB)
getOutputLevelDb()     // Current output level (-80 to 0 dB)
getGateReductionDb()   // Current gain reduction (-80 to 0 dB)
getThresholdDb()       // Current threshold setting
```

These can be used to display input/output meters and gate state indicators.

## Usage Tips

### High-Gain Guitar Setup
- **Threshold:** -50 to -40 dB
- **Attack:** 0.5-2 ms
- **Hold:** 50-100 ms
- **Release:** 50-100 ms
- **Range:** -80 dB
- Eliminates amp hiss and hum between notes with tight response

### Natural Sustain Preservation
- **Threshold:** -60 to -50 dB
- **Attack:** 5-10 ms
- **Hold:** 100-200 ms
- **Release:** 200-300 ms
- **Range:** -40 to -30 dB
- Allows note tails to decay naturally while still reducing noise

### Palm Muted Riffs
- **Threshold:** -45 to -35 dB
- **Attack:** 0.5-1 ms
- **Hold:** 30-50 ms
- **Release:** 30-50 ms
- **Range:** -80 dB
- Very tight gating for staccato playing

### Clean Channel Noise Reduction
- **Threshold:** -55 to -45 dB
- **Attack:** 2-5 ms
- **Hold:** 100-150 ms
- **Release:** 150-250 ms
- **Range:** -60 to -40 dB
- Gentle gating that does not affect clean dynamics

### Live Performance
- **Threshold:** -45 to -35 dB
- **Attack:** 1-3 ms
- **Hold:** 75-125 ms
- **Release:** 100-150 ms
- **Range:** -60 dB
- Balanced settings for varied playing styles

### Recording Cleanup
- **Threshold:** -50 to -40 dB
- **Attack:** 2-5 ms
- **Hold:** 50-100 ms
- **Release:** 100-200 ms
- **Range:** -80 dB
- Clean separation between takes

## Technical Specifications

- **Algorithm:** Look-ahead noise gate with hold time
- **Look-Ahead:** 5 ms
- **Detection:** Peak envelope with 0.5ms time constant
- **Stereo Mode:** Dual-mono (independent channels)
- **Processing:** 32-bit float internal
- **Latency:** 5 ms (equal to look-ahead time)
- **Metering:** Input level, output level, gain reduction
- **CPU Usage:** Low

## Common Issues and Solutions

### Gate Chatters During Sustained Notes

**Problem:** The gate opens and closes rapidly during sustained notes.

**Solutions:**
- Increase Hold time (try 100-200 ms)
- Lower Threshold slightly
- Increase Release time
- Reduce Range (allow some signal through when closed)

### Transient Attacks Are Clipped

**Problem:** The beginning of notes sounds cut off or softened.

**Solutions:**
- Decrease Attack time (try 0.5-2 ms)
- Lower Threshold to open gate earlier
- The 5ms look-ahead should prevent this; if still occurring, check that playing dynamics are consistent

### Note Tails Cut Off Too Abruptly

**Problem:** Sustained notes end abruptly when they decay below threshold.

**Solutions:**
- Increase Release time (try 200-400 ms)
- Increase Hold time
- Lower Threshold
- Increase Range to allow some tail through (try -40 dB)

### Gate Does Not Close Completely

**Problem:** Noise is still audible when not playing.

**Solutions:**
- Lower Range (try -80 dB for complete silence)
- Increase Threshold to ensure signal drops below it
- Check for ground loops or other noise sources

### Sound Is Pumping or Breathing

**Problem:** The gate opening/closing is audibly distracting.

**Solutions:**
- Increase Attack time for smoother opening
- Increase Release time for smoother closing
- Increase Range for partial attenuation rather than complete silence
- Increase Hold time

## DSP Theory

### Why Hold Time?

Musical signals do not maintain a constant level. A sustained note may vary in amplitude due to:
- Natural acoustic variations
- Picking dynamics
- Tremolo or vibrato
- Room acoustics

Without hold time, a gate would rapidly open and close as the signal fluctuates around the threshold, causing an audible "chattering" or "stuttering" effect.

The hold timer keeps the gate open for a minimum duration after the signal first drops below threshold. This allows the signal to recover above threshold without the gate closing, eliminating chatter on sustained notes.

### Range vs. Complete Silence

A gate that closes to complete silence (-80 dB or below) can sound unnatural because:
- It removes the room ambience present in the noise floor
- The transition from "sound" to "complete silence" is jarring
- Natural acoustic environments are never truly silent

Setting Range to a higher value (e.g., -30 to -20 dB) creates a "soft gate" or "downward expander" that:
- Reduces noise without eliminating it completely
- Sounds more natural and transparent
- Is less noticeable during transitions

For high-gain applications where complete noise elimination is required, use full range (-80 dB).

### Look-Ahead Trade-offs

**Advantages:**
- Preserves transient attacks perfectly
- Smoother operation with fewer artifacts
- More transparent sound

**Disadvantages:**
- Introduces 5ms latency
- Requires additional memory for buffer
- Not suitable for live monitoring through speakers (comb filtering with direct sound)

For applications where latency is critical (live monitoring), the look-ahead can be conceptually reduced or bypassed, though this is not currently user-configurable.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/NoiseGateEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize buffers and state
- `onProcess(float[] input, float[] output, int frameCount)` - Mono gating
- `onProcessStereo(...)` - Stereo gating with independent channels
- `onReset()` - Clear buffers and reset state
- `getInputLevelDb()` - Current input level for metering
- `getOutputLevelDb()` - Current output level for metering
- `getGateReductionDb()` - Current gain reduction for metering
- `getThresholdDb()` - Current threshold value

**Key Constants:**
- Look-ahead time: 5 ms
- Envelope detection time constant: 0.5 ms

## See Also

- [CompressorEffect](CompressorEffect.md) - Dynamic range compression
- [LimiterEffect](LimiterEffect.md) - Peak limiting
- [SustainerEffect](SustainerEffect.md) - Infinite sustain simulation
