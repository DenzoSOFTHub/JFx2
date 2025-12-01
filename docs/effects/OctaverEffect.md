# Octaver Effect

## Overview

OctaverEffect is a classic analog-style octave generator that creates one octave below and one octave above the input signal. It uses frequency division for the octave-down and full-wave rectification for the octave-up, mimicking the behavior of vintage analog octave pedals.

**Category:** Pitch
**ID:** `octaver`
**Display Name:** Octaver

## Description

This effect emulates the classic analog octave pedal sound. Unlike pitch shifters that use time-domain granular processing, the octaver uses simple analog-style techniques: flip-flop frequency division for the sub-octave and full-wave rectification for the octave-up. This produces a characteristically synthetic, organ-like tone that has been a staple of bass and guitar sounds since the 1970s.

## Parameters

### Octave Down
- **ID:** `octDown`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Level of the one-octave-down signal. Creates thick, bass-heavy tones. The sub-octave has a synthetic, square-wave quality that blends well with the dry signal.

### Octave Up
- **ID:** `octUp`
- **Range:** 0% to 100%
- **Default:** 0%
- **Unit:** %
- **Description:** Level of the one-octave-up signal. Adds shimmer and a 12-string guitar-like quality. Creates a bright, cutting tone through frequency doubling.

### Dry
- **ID:** `dry`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Level of the original unprocessed signal. Blend to taste with octave signals for the desired mix of natural and synthetic tones.

### Tone
- **ID:** `lpf`
- **Range:** 200 Hz to 2000 Hz
- **Default:** 800 Hz
- **Unit:** Hz
- **Description:** Low-pass filter cutoff for the octave signals. Lower values smooth out the synthetic character and remove harsh harmonics. Higher values retain more brightness and edge.

### Tracking
- **ID:** `tracking`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** How accurately the effect follows your playing. Higher values provide better tracking of pitch but may introduce more glitches on complex signals. Lower values are more forgiving but may miss fast playing.

## DSP Components

### Input Filter
- **Type:** Bandpass (BiquadFilter)
- **Center Frequency:** 200 Hz
- **Q Factor:** 1.0
- **Purpose:** Conditions the input for better zero-crossing detection by removing high-frequency content and DC offset

### Octave Down Filter
- **Type:** Lowpass (BiquadFilter)
- **Cutoff:** Set by Tone parameter
- **Q Factor:** 0.707 (Butterworth)
- **Purpose:** Smooths the square wave output into a more sine-like waveform

### Octave Up Filter
- **Type:** Lowpass (BiquadFilter)
- **Cutoff:** 2x Tone parameter
- **Q Factor:** 0.707 (Butterworth)
- **Purpose:** Removes harsh harmonics from the rectified signal

### State Variables (per channel)
- `flipFlopState` - Boolean toggle for frequency division
- `lastSample` - Previous filtered sample for zero-crossing detection
- `octDownOutput` - Current octave-down square wave value
- `octDownSmooth` - Envelope follower for octave-down amplitude
- `octUpSmooth` - DC blocker state for octave-up

## Implementation Details

### Signal Flow

```
Input -> Bandpass Filter -> Zero-Crossing Detection -> Flip-Flop -> Square Wave
                                                            |
                                                            v
Input Envelope -> Amplitude Modulation -> Lowpass Filter -> Octave Down Output
                                                                    |
                                                                    v
Input -> Full-Wave Rectification -> DC Removal -> Lowpass Filter -> Octave Up Output
                                                                    |
                                                                    v
                            [Dry + Octave Down + Octave Up] -> Output
```

### Octave Down Algorithm (Frequency Division)

1. **Input Conditioning**: The input is passed through a bandpass filter centered at 200 Hz to create a cleaner signal for zero-crossing detection.

2. **Zero-Crossing Detection**: The algorithm detects when the filtered signal crosses zero (changes from positive to negative or vice versa), using an adaptive threshold based on the Tracking parameter.

3. **Flip-Flop Toggle**: Each zero crossing toggles a boolean state, effectively dividing the frequency by 2.

4. **Square Wave Generation**: The flip-flop state generates a square wave at half the input frequency:
   ```java
   octDownSquare = flipFlopState ? 1.0f : -1.0f;
   ```

5. **Envelope Following**: The square wave is modulated by the input envelope to create a more natural amplitude:
   ```java
   envelope = Math.abs(sample);
   octDownSmooth = 0.999 * octDownSmooth + 0.001 * envelope;
   octDownOutput = octDownSquare * octDownSmooth * 2.0;
   ```

6. **Low-Pass Filtering**: The resulting signal is filtered to smooth the harsh square wave edges.

### Octave Up Algorithm (Full-Wave Rectification)

1. **Full-Wave Rectification**: The input is rectified using a modified formula that doubles the frequency:
   ```java
   rectified = Math.abs(sample) * 2.0f - sample;
   ```
   This creates a signal where negative portions are flipped positive, effectively doubling the frequency.

2. **DC Removal**: A simple high-pass filter removes the DC offset introduced by rectification:
   ```java
   octUpSmooth = 0.99 * octUpSmooth + 0.01 * rectified;
   octUp = rectified - octUpSmooth;
   ```

3. **Low-Pass Filtering**: The signal is filtered to remove harsh harmonics while preserving the doubled fundamental.

### Adaptive Tracking

The zero-crossing threshold adapts based on the Tracking parameter:
```java
adaptiveThreshold = zeroCrossThreshold * (1.0f - trackAmt * 0.9f);
```

- Higher tracking = lower threshold = more sensitive detection
- Lower tracking = higher threshold = more tolerant of noise

## Usage Tips

### Classic Octave Fuzz
- **Octave Down:** 0%
- **Octave Up:** 70-100%
- **Dry:** 30-50%
- **Tone:** 1200-1600 Hz
- **Tracking:** 50-70%
- Produces the classic Hendrix "Purple Haze" style octave fuzz sound

### Sub-Octave Bass
- **Octave Down:** 70-100%
- **Octave Up:** 0%
- **Dry:** 40-60%
- **Tone:** 400-800 Hz
- **Tracking:** 40-60%
- Thickens single notes with a deep sub-octave, great for bass guitar

### Organ Simulation
- **Octave Down:** 40-60%
- **Octave Up:** 30-50%
- **Dry:** 50-70%
- **Tone:** 800-1200 Hz
- **Tracking:** 50%
- All three levels combined create an organ-like timbre

### 12-String Guitar
- **Octave Down:** 0%
- **Octave Up:** 30-50%
- **Dry:** 70-90%
- **Tone:** 1500-2000 Hz
- **Tracking:** 60-80%
- Adds shimmer and chime similar to a 12-string guitar

### Synth Bass
- **Octave Down:** 80-100%
- **Octave Up:** 0%
- **Dry:** 0-20%
- **Tone:** 300-500 Hz (dark)
- **Tracking:** 50%
- Creates a synth-like bass tone with minimal original signal

## Best Practices

### For Best Tracking
- Play single notes (chords track poorly with this algorithm)
- Use the neck pickup for cleaner, fundamental-rich signal
- Roll off guitar tone knob slightly
- Play at moderate volume (not too quiet)

### Avoiding Glitches
- Reduce Tracking parameter if experiencing glitchy artifacts
- Use lower Tone settings to smooth out artifacts
- Play cleanly with good note separation

### In a Signal Chain
- Place before distortion for classic octave fuzz
- Place after compression for more consistent tracking
- Avoid placing after heavy modulation effects

## Technical Specifications

- **Octave Range:** -1 octave (down) to +1 octave (up)
- **Tracking Method:** Zero-crossing frequency division
- **Octave Up Method:** Full-wave rectification
- **Filter Type:** 2nd-order Butterworth lowpass
- **Latency:** Near-zero (no buffering required)
- **Processing:** 32-bit float
- **Stereo Support:** Full stereo with independent channel processing

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/OctaverEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes filters and state
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Stereo processing with independent channels
- `onReset()` - Resets all state and filters

## See Also

- [PitchShifterEffect](PitchShifterEffect.md) - Granular pitch shifting with more flexibility
- [DistortionEffect](DistortionEffect.md) - Often paired with octaver for octave fuzz
- [FuzzEffect](FuzzEffect.md) - Classic fuzz that pairs well with octave up
