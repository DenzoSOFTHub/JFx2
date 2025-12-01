# Phaser Effect

## Description

The Phaser effect creates a sweeping, swirling sound by modulating the cutoff frequencies of a series of all-pass filters. Unlike flangers which use delay lines, phasers use frequency-selective filtering to create notches that sweep through the spectrum. The result is a smooth, vocal-like sweeping effect.

**Category**: Modulation
**ID**: `phaser`
**Display Name**: Phaser

## How It Works

The phaser operates by:
1. Passing the input signal through 6 cascaded all-pass filter stages
2. Each stage shifts the phase of frequencies based on its cutoff frequency
3. An LFO sweeps the cutoff frequency of all stages simultaneously
4. The phase-shifted signal is fed back to the input
5. Phase cancellation creates notches in the frequency spectrum
6. The processed signal is mixed with the dry signal

All-pass filters have a flat magnitude response but frequency-dependent phase shift. When mixed with the dry signal, certain frequencies cancel out (notches), creating the phaser's characteristic sound.

## Parameters

### Rate
- **ID**: `rate`
- **Range**: 0.05 Hz to 5.0 Hz
- **Default**: 0.5 Hz
- **Unit**: Hz
- **Description**: Speed of the filter sweep. Slow rates (0.05-0.3 Hz) create hypnotic, slow sweeps. Moderate rates (0.4-1 Hz) are classic phaser territory. Fast rates (2-5 Hz) add vibrato-like movement.

### Depth
- **ID**: `depth`
- **Range**: 0% to 100%
- **Default**: 70%
- **Unit**: %
- **Description**: Range of the frequency sweep. Higher values create more dramatic swooshing effects with a wider sweep range. Controls how far the filters sweep from the center frequency.

### Feedback
- **ID**: `feedback`
- **Range**: -90% to +90%
- **Default**: 40%
- **Unit**: %
- **Description**: Intensity of the effect. Positive values create emphasized resonant peaks. Negative values invert the phase, creating a different tonal character. Higher absolute values increase the prominence of notches.

### Mix
- **ID**: `mix`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Balance between dry and phased signal. Higher values intensify the effect.

### Center
- **ID**: `center`
- **Range**: 200 Hz to 2000 Hz
- **Default**: 800 Hz
- **Unit**: Hz
- **Description**: Center frequency of the sweep. Lower values (200-500 Hz) affect bass frequencies, creating deeper sweeps. Mid values (600-1200 Hz) are classic phaser range. Higher values (1200-2000 Hz) affect treble, creating brighter, more subtle sweeps.

## Implementation Details

### Signal Flow

```
Input → [+ Feedback] → Stage 1 → Stage 2 → Stage 3 → Stage 4 → Stage 5 → Stage 6 → Mix with Dry → Output
                                                                                            ↓
                                                                                   Store Feedback
```

Each stage is a first-order all-pass filter with modulated cutoff frequency.

### LFO Configuration

- **Waveform**: Sine wave (smooth, natural sweep)
- **Update**: Frequency updated per sample for smooth modulation
- **Stereo**: Right channel LFO is 180° out of phase (0.5 phase offset)

### All-Pass Filter Stages

- **Number of Stages**: 6 (creates 6 notches in spectrum)
- **Type**: First-order all-pass filters
- **Configuration**: All stages share the same modulated frequency
- **State Variables**: 1 state variable per stage per channel

### Frequency Sweep Calculation

```java
freqRatio = MAX_FREQ / MIN_FREQ  // 4000 / 100 = 40
sweepRange = depth * 2.0
freq = centerFreq * pow(freqRatio, lfoValue * sweepRange * 0.5)
freq = clamp(freq, MIN_FREQ, MAX_FREQ)
```

This creates a logarithmic sweep, which sounds more musical than linear sweeping.

### All-Pass Coefficient Calculation

```java
w = tan(π * freq / sampleRate)
coefficient = (w - 1) / (w + 1)
```

This bilinear transform maps the analog filter to the digital domain.

### All-Pass Processing

Each stage implements:
```java
newState = input - coefficient * state
output = coefficient * newState + state
state = newState
```

This is a first-order all-pass structure that provides 90° phase shift at the cutoff frequency.

### Notch Positions

With 6 stages, the phaser creates 6 notches in the spectrum. The notch positions are harmonically related to the sweep frequency, creating a rich, musical effect.

## Key Differences from Flanger

| Feature | Phaser | Flanger |
|---------|--------|---------|
| Mechanism | All-pass filters | Delay line |
| Notches | 6 (number of stages) | Many (comb filter) |
| Sweep | Logarithmic (musical) | Linear |
| Character | Smooth, vocal-like | Metallic, jet-plane |
| Spacing | Harmonic | Evenly spaced |
| CPU Usage | Lower | Higher |

## Usage Tips

### Classic Phaser
- **Rate**: 0.3-0.5 Hz
- **Depth**: 60-80%
- **Feedback**: 40-60%
- **Mix**: 50-70%
- **Center**: 800 Hz
- Traditional phaser sound, smooth and swirly

### Subtle Movement
- **Rate**: 0.1-0.2 Hz
- **Depth**: 30-50%
- **Feedback**: 20-40%
- **Mix**: 30-50%
- **Center**: 1000 Hz
- Adds gentle movement without being obvious

### Deep Sweep
- **Rate**: 0.2-0.4 Hz
- **Depth**: 80-100%
- **Feedback**: 60-80%
- **Mix**: 60-80%
- **Center**: 500 Hz
- Low center frequency with wide sweep

### Fast Warble
- **Rate**: 2-4 Hz
- **Depth**: 50-70%
- **Feedback**: 30-50%
- **Mix**: 50-60%
- **Center**: 1000 Hz
- Vibrato-like modulation with phaser character

### Vocal Phaser
- **Rate**: 0.4-0.7 Hz
- **Depth**: 70-90%
- **Feedback**: 50-70%
- **Mix**: 60-80%
- **Center**: 600-800 Hz
- Sweeps through vocal formant range

### Treble Shimmer
- **Rate**: 0.5-1.0 Hz
- **Depth**: 40-60%
- **Feedback**: 30-50%
- **Mix**: 40-60%
- **Center**: 1500-2000 Hz
- High-frequency sweep for subtle shimmer

### Extreme Resonance
- **Rate**: 0.15-0.25 Hz
- **Depth**: 80-100%
- **Feedback**: 70-90%
- **Mix**: 70-90%
- **Center**: 800 Hz
- Very pronounced, almost synth-like sweep

### Negative Phase
- **Rate**: 0.3-0.5 Hz
- **Depth**: 60-80%
- **Feedback**: -60 to -80%
- **Mix**: 50-70%
- **Center**: 800 Hz
- Inverted phase for alternative character

## Technical Specifications

- **Latency**: Minimal (single-sample processing)
- **Stages**: 6 first-order all-pass filters
- **Frequency Range**: 100 Hz to 4000 Hz
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Stereo**: True stereo with independent filter states and phase-offset LFOs
- **Sweep Type**: Logarithmic (musical)
