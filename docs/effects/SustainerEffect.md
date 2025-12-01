# Sustainer Effect

## Description

The Sustainer creates infinite sustain by using aggressive dynamic compression and automatic gain control to maintain a constant note volume. It emulates hardware devices like the E-Bow or Fernandes Sustainer system, which use electromagnetic feedback to keep guitar strings vibrating indefinitely. The effect includes harmonic enhancement options and tone shaping to create different sustain characters, from natural violin-like sustain to harmonically rich textures.

**Category:** Dynamics
**ID:** `sustainer`
**Display Name:** Sustainer

## How It Works

The sustainer uses an envelope follower to track the input signal level, then applies dynamic gain to bring quiet signals up to a target level. As a note naturally decays, the sustainer increases gain to compensate, maintaining constant volume. This creates the illusion of infinite sustain. The effect includes tone filtering to shape the sustained sound and soft clipping to prevent harsh distortion from the high gain stages.

## Parameters

### Sustain
- **ID:** `sustain`
- **Range:** 0% to 100%
- **Default:** 80%
- **Unit:** %
- **Description:** Amount of sustain applied. Higher values provide more gain compensation and longer sustain. At 0%, the effect is bypassed. At 100%, maximum gain is applied to maintain constant level.

### Attack
- **ID:** `attack`
- **Range:** 1 ms to 100 ms
- **Default:** 20 ms
- **Unit:** ms
- **Description:** How fast the sustain effect kicks in after a note is played. Slower attack (50-100 ms) preserves natural pick attack and playing dynamics. Faster attack (1-10 ms) creates a more immediate, organ-like sustain that may soften transients.

### Tone
- **ID:** `tone`
- **Range:** 500 Hz to 8000 Hz
- **Default:** 3000 Hz
- **Unit:** Hz
- **Description:** Brightness of the sustained signal. Controls a low-pass filter cutoff frequency. Lower values (500-2000 Hz) create a warmer, darker sustain. Higher values (4000-8000 Hz) preserve brightness and articulation.

### Mode
- **ID:** `mode`
- **Range:** Natural, Harmonic, Fundamental
- **Default:** Natural
- **Unit:** (choice)
- **Description:** Sustain character mode:
  - **Natural:** Gentle compression with tone control, most transparent sound
  - **Harmonic:** Boosts upper harmonics at 2500 Hz for a richer, more complex sustain
  - **Fundamental:** Emphasizes fundamental frequency by applying steeper low-pass filtering, for a purer, more violin-like tone

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between dry (original) and sustained signal. 0% is completely dry (bypassed). 100% is fully sustained. Lower mix values (30-50%) can create a subtle sustain enhancement while preserving dynamics.

### Sensitivity
- **ID:** `sensitivity`
- **Range:** -40 dB to 0 dB
- **Default:** -20 dB
- **Unit:** dB
- **Description:** Input sensitivity threshold. Signals below this level will not trigger the sustainer, preventing noise from being amplified. Lower values (toward -40 dB) respond to softer playing. Higher values (toward 0 dB) require louder playing to engage.

## Implementation Details

### Signal Flow

```
Input --> Envelope Follower --> Gain Calculator --> Gain Smoother
                                      |                   |
                                      v                   v
                                 Target Level      Smoothed Gain
                                                         |
                                                         v
Input --> Gain Stage --> Mode Processing --> Low Cut --> Soft Clip --> Mix --> Output
              ^                |
              |                v
         Smoothed Gain    Tone/Harmonic
                          Filter
```

### Envelope Following

The sustainer uses an asymmetric envelope follower:

```java
// Attack: user-controlled
attackCoeff = exp(-1.0 / (attackMs * sampleRate / 1000.0))
// Release: fixed at 200ms for smooth decay tracking
releaseCoeff = exp(-1.0 / (200.0 * sampleRate / 1000.0))

if (absInput > envelope) {
    envelope = attackCoeff * envelope + (1 - attackCoeff) * absInput
} else {
    envelope = releaseCoeff * envelope + (1 - releaseCoeff) * absInput
}
```

The attack coefficient determines how quickly the sustainer responds to new notes, while the 200ms fixed release provides smooth tracking of natural decay.

### Gain Calculation

The desired gain is calculated to bring the current level up to a target level:

```java
// Target level scales with sustain amount
target = targetLevel * (0.5 + sustain * 0.5)  // 0.15 to 0.30

// Calculate required gain
if (envelope > sensitivity * 0.1) {
    desiredGain = target / max(envelope, 0.001)
    desiredGain = clamp(desiredGain, 0.1, 10.0 + sustain * 40.0)
} else {
    desiredGain = 1.0  // Signal too weak, don't amplify
}
```

The maximum gain is limited based on the sustain parameter to prevent excessive noise amplification at high settings.

### Gain Smoothing

Gain changes are smoothed to prevent audible steps:

```java
gainSmoothCoeff = 0.999
currentGain = gainSmoothCoeff * currentGain + (1 - gainSmoothCoeff) * desiredGain
```

This creates a very smooth gain transition with a time constant of approximately 100ms.

### Mode Processing

**Natural Mode (mode = 0):**
- Single low-pass filter at Tone frequency
- Q = 0.707 (Butterworth)
- Most transparent sustain sound

**Harmonic Mode (mode = 1):**
- Peak filter at 2500 Hz
- Q = 1.5, Gain = +6 dB
- Adds harmonic richness and presence
- Creates a more complex, "singing" sustain

**Fundamental Mode (mode = 2):**
- Double low-pass filtering at Tone frequency
- Creates a steeper rolloff (-24 dB/octave effective)
- Emphasizes the fundamental frequency
- Produces a purer, more violin-like tone

### Filter Configuration

```java
// Tone filter (low-pass)
toneFilter.configure(FilterType.LOWPASS, toneFreq, 0.707, 0.0)

// Harmonic boost filter (peak EQ)
harmonicFilter.configure(FilterType.PEAK, 2500.0, 1.5, 6.0)

// Low cut filter (high-pass to remove rumble)
lowCut.configure(FilterType.HIGHPASS, 80.0, 0.707, 0.0)
```

### Soft Clipping

To prevent harsh digital clipping at high gain settings, a soft clipper is applied:

```java
if (wet > 0.9) {
    wet = 0.9 + 0.1 * tanh((wet - 0.9) * 10)
}
if (wet < -0.9) {
    wet = -0.9 - 0.1 * tanh((-wet - 0.9) * 10)
}
```

This creates a smooth saturation above 0.9 (approximately -0.9 dB) rather than hard clipping at 1.0.

### Stereo Processing

In stereo mode, the sustainer uses linked envelope detection:

```java
absInput = max(abs(dryL), abs(dryR))
```

Both channels share the same gain value to preserve stereo imaging. The filters are processed independently per channel to maintain stereo characteristics in the tone shaping.

## Usage Tips

### E-Bow Emulation
- **Sustain:** 90-100%
- **Attack:** 30-50 ms
- **Tone:** 2000-3000 Hz
- **Mode:** Natural
- **Mix:** 100%
- **Sensitivity:** -25 to -20 dB
- Creates the classic electromagnetic feedback sustain sound

### Violin Bow Effect
- **Sustain:** 85-95%
- **Attack:** 50-80 ms
- **Tone:** 1500-2500 Hz
- **Mode:** Fundamental
- **Mix:** 100%
- **Sensitivity:** -30 to -20 dB
- Pure, singing sustain with slow attack for bowed string feel

### Harmonic Pad
- **Sustain:** 70-85%
- **Attack:** 20-40 ms
- **Tone:** 4000-6000 Hz
- **Mode:** Harmonic
- **Mix:** 60-80%
- **Sensitivity:** -25 dB
- Rich, complex sustain with enhanced harmonics for ambient textures

### Subtle Sustain Enhancement
- **Sustain:** 40-60%
- **Attack:** 30-50 ms
- **Tone:** 3000-4000 Hz
- **Mode:** Natural
- **Mix:** 30-50%
- **Sensitivity:** -30 dB
- Adds sustain without completely eliminating dynamics

### Lead Guitar Boost
- **Sustain:** 70-85%
- **Attack:** 15-30 ms
- **Tone:** 3500-5000 Hz
- **Mode:** Natural
- **Mix:** 80-100%
- **Sensitivity:** -25 dB
- Extended sustain while maintaining articulation

### Feedback Simulation
- **Sustain:** 95-100%
- **Attack:** 10-20 ms
- **Tone:** 1000-2000 Hz
- **Mode:** Fundamental
- **Mix:** 100%
- **Sensitivity:** -20 dB
- Creates controlled feedback-like sustain

### Clean Sustain
- **Sustain:** 60-80%
- **Attack:** 40-60 ms
- **Tone:** 2500-3500 Hz
- **Mode:** Natural
- **Mix:** 70-90%
- **Sensitivity:** -30 to -25 dB
- Even sustain for clean guitar tones

## Technical Specifications

- **Algorithm:** Envelope-following AGC with mode filtering
- **Envelope Detection:** Asymmetric attack/release
- **Release Time:** Fixed at 200 ms
- **Target Level:** ~0.3 linear (approximately -10 dB)
- **Maximum Gain:** 10x to 50x depending on Sustain setting
- **Processing:** 32-bit float internal
- **Latency:** Zero (no look-ahead)
- **Stereo Mode:** Linked envelope, independent filtering
- **Filters:** Biquad (tone, harmonic boost, low cut)
- **Clipping:** Soft tanh saturation above 0.9

## Performance Considerations

### Noise Amplification

Because the sustainer applies high gain to quiet signals, it will also amplify any noise present in the signal chain. For best results:

1. Place a noise gate before the sustainer
2. Use quality cables and power supplies
3. Set sensitivity appropriately to avoid amplifying floor noise
4. Consider lower sustain settings in noisy environments

### Feedback Control

At very high sustain settings with high-gain amp settings, acoustic feedback may occur. The sustainer can create a positive feedback loop with:
- Close proximity to amp speakers
- High monitor levels
- Certain room acoustics

Solutions:
- Reduce sustain amount
- Increase attack time
- Use Fundamental mode (less high-frequency content)
- Position guitar away from speakers

### Playing Technique

The sustainer responds best to:
- Clean, sustained notes (not rapid picking)
- Single notes or simple intervals
- Consistent picking dynamics
- Notes that ring out fully

Techniques that work well:
- Legato playing (hammer-ons, pull-offs)
- Bending and vibrato
- Slow melodic passages
- Ambient swells

## DSP Theory

### Automatic Gain Control

The sustainer is essentially an AGC (Automatic Gain Control) system that:
1. Measures the current signal level (envelope)
2. Calculates the gain needed to reach a target level
3. Applies the gain smoothly over time

The key equation is:
```
gain = target_level / current_level
```

By continuously adjusting gain as the note decays, the output remains at a constant level, creating the perception of infinite sustain.

### Why Soft Clipping?

At high sustain settings, the gain may exceed 40x (32 dB). When a new note is played, the high gain briefly causes the signal to exceed 0 dBFS before the envelope follower can respond and reduce gain.

Hard clipping at 0 dBFS creates harsh, digital distortion. The tanh soft clipping provides:
- Gradual saturation instead of abrupt clipping
- Harmonic content similar to analog saturation
- More musical-sounding overload behavior

### Mode Filtering Rationale

**Natural Mode:** The goal is transparency. A single 2-pole low-pass filter provides gentle high-frequency rolloff to prevent harsh artifacts from the high gain without significantly coloring the tone.

**Harmonic Mode:** The 2500 Hz peak boost adds energy in the "presence" region where guitar harmonics are most audible. This creates a richer, more complex sustain that sounds less "dead" or artificial.

**Fundamental Mode:** Double filtering creates a 4-pole (-24 dB/octave) rolloff that strongly attenuates harmonics, leaving primarily the fundamental frequency. This creates a purer, more violin-like tone reminiscent of bowed strings.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/SustainerEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize filters and state
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with linked envelope
- `onReset()` - Clear filter states and reset envelope

**Key Components:**
- `toneFilterL/R` - Biquad low-pass for tone control
- `harmonicFilterL/R` - Biquad peak for Harmonic mode
- `lowCutL/R` - Biquad high-pass (80 Hz) to remove rumble

**Dependencies:**
- `BiquadFilter` from `it.denzosoft.jfx2.dsp`
- `FilterType` enum for filter configuration

## See Also

- [CompressorEffect](CompressorEffect.md) - Standard dynamic range compression
- [LimiterEffect](LimiterEffect.md) - Peak limiting
- [VolumeSwellEffect](VolumeSwellEffect.md) - Auto-swell for violin-like attacks
- [NoiseGateEffect](NoiseGateEffect.md) - Noise reduction (use before sustainer)
