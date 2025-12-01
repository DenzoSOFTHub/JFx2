# Chorus Effect

## Description

The Chorus effect creates a rich, shimmering sound by mixing the dry signal with multiple delayed copies modulated by LFOs (Low Frequency Oscillators) at different phases. It simulates the sound of multiple instruments or voices playing together, creating a thicker, more lush tone.

**Category**: Modulation
**ID**: `chorus`
**Display Name**: Chorus

## How It Works

The effect uses a 4-voice architecture where each voice is created by:
1. Delaying the input signal by a base delay time (7ms)
2. Modulating this delay time using independent LFOs with phase offsets
3. Mixing all voices together and blending with the dry signal

The LFOs operate at different phases to prevent the voices from modulating in sync, which would sound like a single vibrato. Instead, they create a complex, evolving texture.

## Parameters

### Rate
- **ID**: `rate`
- **Range**: 0.1 Hz to 5.0 Hz
- **Default**: 0.8 Hz
- **Unit**: Hz
- **Description**: Speed of the modulation. Slower rates (0.1-0.5 Hz) create subtle and lush chorus effects suitable for clean tones. Faster rates (2-5 Hz) produce more pronounced movement and can approach vibrato-like effects.

### Depth
- **ID**: `depth`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Amount of pitch modulation applied to each voice. Higher values create more pronounced swirling effects with greater pitch variation. At 0%, the effect is bypassed. At 100%, the maximum delay modulation of 5ms is applied.

### Mix
- **ID**: `mix`
- **Range**: 0% to 100%
- **Default**: 50%
- **Unit**: %
- **Description**: Balance between dry (unprocessed) and wet (chorused) signal. 0% = completely dry, 100% = only chorused signal. The classic ensemble sound is achieved around 50%.

### Spread
- **ID**: `spread`
- **Range**: 0% to 100%
- **Default**: 80%
- **Unit**: %
- **Description**: Stereo width of the chorus voices. Higher values create wider, more immersive stereo image by cross-mixing the wet signals between channels. At 0%, the effect is mono. At 100%, maximum stereo separation is achieved.

## Implementation Details

### Signal Flow

**Mono Processing:**
```
Input → [4 Delay Lines with LFO Modulation] → Mix → Sum → Mix with Dry → Output
```

**Stereo Processing:**
```
Input L/R → [4 Delay Lines per channel] → Mix → Stereo Spread → Mix with Dry → Output L/R
```

### LFO Configuration

- **Waveform**: Sine wave (smooth modulation)
- **Number of Voices**: 4
- **Phase Distribution**: Evenly spread across 360 degrees (0°, 90°, 180°, 270°)
- **Stereo Offset**: Right channel LFOs are offset by 0.5 phase (180 degrees) for stereo width
- **Update**: LFO frequency is updated per buffer based on Rate parameter

### Delay Modulation

- **Base Delay**: 7.0 ms (fixed center point)
- **Maximum Depth**: 5.0 ms (added/subtracted from base)
- **Total Range**: 2.0 ms to 12.0 ms (7 ± 5 ms)
- **Interpolation**: Cubic interpolation for smooth, artifact-free modulation
- **Buffer Size**: 20 ms (provides headroom for modulation)

### Stereo Spread Algorithm

The spread parameter controls cross-mixing of wet signals:
```java
wetLFinal = wetL * (1.0 - spread * 0.3) + wetR * spread * 0.3
wetRFinal = wetR * (1.0 - spread * 0.3) + wetL * spread * 0.3
```

This creates stereo width by:
- Keeping 70-100% of each channel's original wet signal
- Mixing in 0-30% of the opposite channel's wet signal

### Voice Architecture

Each voice processes independently:
1. Write input to delay line
2. Generate LFO value for this voice (phase-offset sine wave)
3. Calculate modulated delay time: `baseSamples + lfoValue * depthSamples`
4. Read delayed sample with cubic interpolation
5. Sum all voices and divide by 4 (average)

## Usage Tips

### Classic Chorus
- **Rate**: 0.5-0.8 Hz
- **Depth**: 40-60%
- **Mix**: 40-60%
- **Spread**: 70-90%
- Creates the traditional lush, shimmering chorus sound

### Subtle Enhancement
- **Rate**: 0.2-0.4 Hz
- **Depth**: 20-30%
- **Mix**: 20-40%
- **Spread**: 50-70%
- Adds slight thickness without obvious modulation

### Vibrato Effect
- **Rate**: 4-5 Hz
- **Depth**: 70-100%
- **Mix**: 80-100%
- **Spread**: 30-50%
- Fast, pronounced pitch wobble

### Wide Stereo
- **Rate**: 0.6-1.0 Hz
- **Depth**: 50-70%
- **Mix**: 50-70%
- **Spread**: 90-100%
- Maximum stereo width and spaciousness

### 12-String Guitar Simulation
- **Rate**: 0.3-0.6 Hz
- **Depth**: 30-40%
- **Mix**: 30-50%
- **Spread**: 60-80%
- Simulates the natural detuning of double courses

## Technical Specifications

- **Latency**: Base delay of 7ms plus modulation (up to 12ms maximum)
- **Voices**: 4 independent modulation paths
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Interpolation**: Cubic (4-point) for alias-free modulation
- **Stereo**: True stereo with independent LFOs per channel
