# Volume Swell Effect

## Description

The Volume Swell (also known as Auto-Swell or Slow Gear) creates violin-like or bowed string attacks by automatically fading in the volume at the start of each note. It detects note onsets and applies a volume envelope that gradually increases from silence to full volume over a user-defined attack time. This eliminates the percussive pick attack characteristic of guitar, creating smooth, pad-like textures ideal for ambient music, orchestral emulation, and atmospheric soundscapes.

**Category:** Dynamics
**ID:** `volumeswell`
**Display Name:** Volume Swell

## How It Works

The effect uses an envelope follower to track the input signal level and detect note onsets. When a new note is detected (signal rises sharply above the threshold), the output volume is reset to zero and gradually increases over the attack time. A hold mechanism prevents re-triggering during sustained notes, and the curve parameter allows shaping the swell for different musical effects.

## Parameters

### Attack
- **ID:** `attack`
- **Range:** 10 ms to 2000 ms
- **Default:** 300 ms
- **Unit:** ms
- **Description:** Time for the volume to rise from silence to full. Shorter times (10-100 ms) create subtle attack softening. Longer times (500-2000 ms) create dramatic, orchestral swells. The sweet spot for violin-like sound is typically 200-400 ms.

### Sensitivity
- **ID:** `sensitivity`
- **Range:** -60 dB to -20 dB
- **Default:** -40 dB
- **Unit:** dB
- **Description:** Threshold for detecting note onsets. Lower values (toward -60 dB) detect softer playing and are more sensitive. Higher values (toward -20 dB) require louder playing to trigger a new swell. Set this just above your noise floor for optimal detection.

### Hold
- **ID:** `hold`
- **Range:** 50 ms to 500 ms
- **Default:** 100 ms
- **Unit:** ms
- **Description:** Minimum time between swell triggers. Prevents re-triggering during sustained notes or when playing legato. Longer hold times (200-500 ms) are more forgiving for uneven playing. Shorter hold times (50-100 ms) allow faster note repetition.

### Curve
- **ID:** `curve`
- **Range:** 0.5 to 2.0
- **Default:** 1.0
- **Unit:** (ratio)
- **Description:** Shape of the volume swell:
  - **< 1.0 (Logarithmic):** Slow start, fast finish - volume rises slowly at first, then accelerates
  - **1.0 (Linear):** Constant rate of volume increase
  - **> 1.0 (Exponential):** Fast start, slow finish - volume rises quickly at first, then slows down

## Implementation Details

### Signal Flow

```
Input --> Envelope Follower --> Onset Detector
                                     |
                                     v
                              Trigger Decision
                                     |
                                     v
Input --> Gain Stage ------------> Curve Shaping --> Output
              ^                        ^
              |                        |
         Swell Gain <-- Swell State --+
```

### Envelope Detection

The effect uses a fast peak envelope follower (5ms time constant) for accurate onset detection:

```java
envCoeff = exp(-1.0 / (0.005 * sampleRate))  // 5ms time constant

if (absInput > envelope) {
    envelope = absInput  // Instant attack
} else {
    envelope = envCoeff * envelope + (1 - envCoeff) * absInput
}
```

The instant attack ensures transients are detected immediately, while the 5ms decay provides a stable level reading.

### Onset Detection Algorithm

A new note is detected when both conditions are met:
1. Current envelope exceeds the sensitivity threshold
2. Current envelope is more than 2x the previous level (significant jump)

```java
if (envelope > sensitivityLin && envelope > lastLevel * 2.0) {
    // New note detected
    swellGain = 0         // Reset gain to silence
    swelling = true       // Start swell
    holdCounter = holdSamples  // Reset hold timer
}
lastLevel = envelope
```

The 2x ratio ensures that only significant level jumps trigger new swells, preventing re-triggering on level fluctuations within a sustained note.

### Swell State Machine

The effect operates in three states:

**1. Swelling (new note detected):**
```java
if (swelling) {
    swellGain += attackIncrement  // Linear ramp
    if (swellGain >= 1.0) {
        swellGain = 1.0
        swelling = false  // Swell complete
    }
}
```

**2. Hold (preventing re-triggers):**
```java
if (holdCounter > 0) {
    holdCounter--
    // New triggers blocked during hold
}
```

**3. Decay (signal dropped, allow new trigger):**
```java
if (envelope < sensitivityLin * 0.5) {
    // Signal dropped significantly
    swellGain = max(0, swellGain - attackIncrement * 0.5)
}
```

### Attack Increment Calculation

The attack increment is calculated for a linear ramp over the attack time:

```java
attackSamples = attackMs * sampleRate / 1000.0
attackIncrement = 1.0 / attackSamples
```

Each sample, the swell gain increases by this amount until it reaches 1.0.

### Curve Shaping

The linear swell gain is shaped by the curve parameter using a power function:

```java
shapedGain = pow(swellGain, curve)
```

**Curve < 1.0 (Logarithmic):**
- `pow(0.5, 0.5) = 0.707` - At halfway through swell, gain is at 71%
- Creates slow start, fast finish
- Sounds more gradual, natural for very long swells

**Curve = 1.0 (Linear):**
- `pow(0.5, 1.0) = 0.5` - At halfway, gain is at 50%
- Constant rate of change
- Most predictable, transparent sound

**Curve > 1.0 (Exponential):**
- `pow(0.5, 2.0) = 0.25` - At halfway through swell, gain is at 25%
- Creates fast start, slow finish
- Sounds more dramatic, good for shorter attack times

### Stereo Processing

In stereo mode, both channels share:
- A single envelope follower (uses max of both channels)
- A single swell state machine
- Identical gain applied to both channels

This ensures the stereo image remains stable during swells.

## Usage Tips

### Classic Violin Swell
- **Attack:** 250-400 ms
- **Sensitivity:** -45 to -35 dB
- **Hold:** 100-150 ms
- **Curve:** 1.0
- Creates authentic bowed string attack

### Pad/Ambient Texture
- **Attack:** 800-1500 ms
- **Sensitivity:** -50 to -40 dB
- **Hold:** 200-300 ms
- **Curve:** 0.7-0.9
- Very slow swells for atmospheric soundscapes

### Subtle Attack Softening
- **Attack:** 50-100 ms
- **Sensitivity:** -40 to -30 dB
- **Hold:** 75-100 ms
- **Curve:** 1.0
- Just removes the pick attack without dramatic effect

### Orchestral Swells
- **Attack:** 500-1000 ms
- **Sensitivity:** -45 to -35 dB
- **Hold:** 150-250 ms
- **Curve:** 0.8-1.0
- Dramatic, cinematic volume rises

### Fast Legato
- **Attack:** 100-200 ms
- **Sensitivity:** -35 to -25 dB
- **Hold:** 50-75 ms
- **Curve:** 1.2-1.5
- Allows faster playing while still softening attacks

### Reverse Effect Simulation
- **Attack:** 300-600 ms
- **Sensitivity:** -40 dB
- **Hold:** 100 ms
- **Curve:** 1.5-2.0
- Fast initial rise creates pseudo-reverse effect

### Clean Sustained Chords
- **Attack:** 400-600 ms
- **Sensitivity:** -50 to -40 dB
- **Hold:** 200-300 ms
- **Curve:** 0.9-1.0
- Removes harsh chord attacks for smooth progressions

## Technical Specifications

- **Algorithm:** Onset-triggered volume envelope
- **Envelope Detection:** Peak follower with 5ms time constant
- **Onset Detection:** Level ratio threshold (2x jump)
- **Processing:** 32-bit float internal
- **Latency:** Zero (no look-ahead)
- **Stereo Mode:** Linked (identical gain both channels)
- **CPU Usage:** Very low

## Common Issues and Solutions

### Effect Does Not Trigger

**Problem:** Playing notes but no swell occurs.

**Solutions:**
- Lower Sensitivity (try -50 to -45 dB)
- Play with more consistent dynamics
- Check input level is adequate
- Ensure effect is enabled and not bypassed

### Effect Triggers Too Often

**Problem:** Swells re-trigger during sustained notes or legato passages.

**Solutions:**
- Increase Hold time (try 200-300 ms)
- Increase Sensitivity (try -30 to -25 dB)
- Play with more legato technique
- Reduce dynamic variation in playing

### Swells Sound Choppy

**Problem:** The swell sounds stuttery or discontinuous.

**Solutions:**
- Increase Attack time for smoother transition
- Increase Hold time to prevent re-triggering
- Adjust Curve for more gradual rise

### Attack Sounds Too Soft/Slow

**Problem:** The beginning of notes is too quiet for too long.

**Solutions:**
- Decrease Attack time
- Increase Curve (try 1.5-2.0) for faster initial rise
- Decrease Sensitivity to trigger earlier

### Noise Triggers Swells

**Problem:** Background noise or hum triggers unwanted swells.

**Solutions:**
- Increase Sensitivity (toward -20 dB)
- Use a noise gate before the volume swell
- Check for ground loops or interference
- Reduce input gain

## Playing Techniques

### Best Practices

1. **Play Sustained Notes:** The effect works best with notes that ring out. Staccato playing may trigger multiple swells.

2. **Consistent Dynamics:** Try to maintain consistent picking strength. Wild dynamic variations may cause erratic triggering.

3. **Allow Notes to Fade:** Let notes decay naturally before playing new ones to allow the swell to complete.

4. **Use Legato:** Hammer-ons and pull-offs may not trigger new swells, allowing for smooth legato passages.

5. **Avoid Palm Muting:** Muted notes have different dynamics and may not trigger reliably.

### Musical Applications

- **Ambient Guitar:** Long swells with reverb and delay
- **Film Scoring:** Orchestral pad textures
- **Worship Music:** Sustained, non-percussive guitar parts
- **Jazz Ballads:** Soft, breathy chord swells
- **Classical Crossover:** Violin/cello emulation
- **Sound Design:** Textural elements and atmospheres

## DSP Theory

### Why Onset Detection Instead of Continuous Envelope?

A continuous envelope follower that simply tracks and inverts the input level would create constant pumping and breathing artifacts. Instead, the onset detection approach:

1. Only triggers on significant level jumps (new notes)
2. Applies a predictable envelope shape
3. Holds the envelope during sustain
4. Produces more musical, predictable results

### Curve Mathematics

The power function `pow(x, curve)` where x is 0 to 1 provides intuitive control:

- For curve < 1: Output is always greater than input (except at 0 and 1)
- For curve > 1: Output is always less than input (except at 0 and 1)
- For curve = 1: Output equals input (linear)

This creates smooth, continuous curves without discontinuities, which is essential for artifact-free volume automation.

### Level Ratio Threshold

Using a ratio (2x) rather than an absolute difference for onset detection provides:

1. **Dynamic Adaptation:** Works at any playing level
2. **Musical Relevance:** A 2x (6 dB) jump is perceptually significant
3. **Noise Immunity:** Noise fluctuations rarely exceed 2x ratio
4. **Consistent Behavior:** Same relative change required regardless of absolute level

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/VolumeSwellEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize state
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with linked detection
- `onReset()` - Reset envelope and swell state

**Key State Variables:**
- `envelope` - Current signal level for onset detection
- `swellGain` - Current volume ramp position (0 to 1)
- `lastLevel` - Previous envelope for ratio comparison
- `holdCounter` - Samples remaining in hold period
- `swelling` - Whether currently in a swell

## See Also

- [SustainerEffect](SustainerEffect.md) - Infinite sustain simulation
- [TremoloEffect](TremoloEffect.md) - Rhythmic volume modulation
- [CompressorEffect](CompressorEffect.md) - Dynamic range control
- [NoiseGateEffect](NoiseGateEffect.md) - Noise reduction (use before swell)
