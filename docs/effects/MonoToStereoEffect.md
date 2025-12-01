# Mono to Stereo Effect

## Overview

MonoToStereoEffect transforms a mono input signal into a rich stereo image using multiple psychoacoustic techniques. It combines panning, stereo width control, Haas effect delays, frequency-based stereo enhancement, and LFO-modulated animation to create immersive stereo soundscapes from mono sources.

**Category:** Utility
**ID:** `mono2stereo`
**Display Name:** Mono to Stereo

## Description

This effect is designed to take a mono signal (such as a guitar or bass) and create a convincing stereo image. Unlike simple panning which just moves a mono signal between speakers, this effect uses multiple psychoacoustic phenomena to create true stereo width:

- **Haas Effect**: Small timing differences between channels create spatial perception
- **Frequency Separation**: Different EQ on each channel adds width
- **Phase Difference**: All-pass filtering creates subtle decorrelation
- **Width Control**: Mid-Side processing for adjustable stereo spread
- **Animation**: LFO modulation adds movement and life

## Parameters

### Pan
- **ID:** `pan`
- **Range:** -100% to +100%
- **Default:** 0%
- **Unit:** %
- **Description:** Position in the stereo field. -100% = full left, 0% = center, +100% = full right. Uses equal-power panning to maintain consistent perceived loudness across the stereo field.

### Width
- **ID:** `width`
- **Range:** 0% to 200%
- **Default:** 100%
- **Unit:** %
- **Description:** Stereo width using Mid-Side processing. 0% = mono (no stereo), 100% = normal stereo, 200% = extra wide. Values above 100% emphasize the side (difference) signal for a wider image.

### Haas Delay
- **ID:** `haasDelay`
- **Range:** 0 ms to 40 ms
- **Default:** 15 ms
- **Unit:** ms
- **Description:** Delay time for the Haas effect. The brain perceives sounds arriving slightly later as coming from the same direction as the earlier sound. 10-30 ms creates natural spatial width without audible echo.

### Haas Balance
- **ID:** `haasBalance`
- **Range:** -100% to +100%
- **Default:** +100%
- **Unit:** %
- **Description:** Which channel receives the delay. -100% = delay left channel (sound perceived as coming from right), +100% = delay right channel (sound perceived as coming from left). 0% = no Haas effect.

### Enhance
- **ID:** `enhance`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Amount of frequency-based stereo enhancement. Applies complementary EQ and phase differences between channels to increase perceived width without level changes.

### Enhance Freq
- **ID:** `enhanceFreq`
- **Range:** 200 Hz to 4000 Hz
- **Default:** 1000 Hz
- **Unit:** Hz
- **Description:** Center frequency for the stereo enhancement. Frequencies around this point will be separated between left and right channels.

### LFO Rate
- **ID:** `lfoRate`
- **Range:** 0 Hz to 5 Hz
- **Default:** 0 Hz
- **Unit:** Hz
- **Description:** Speed of automatic stereo movement. Creates subtle animation in the stereo field. Set to 0 for static positioning.

### LFO Depth
- **ID:** `lfoDepth`
- **Range:** 0% to 100%
- **Default:** 20%
- **Unit:** %
- **Description:** Amount of automatic stereo movement. Higher values create more pronounced panning animation when LFO Rate is active.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between original mono signal and stereo-processed signal. At 0%, the output remains mono. At 100%, only the stereo-processed signal is heard.

## DSP Components

### Haas Delay Lines
- **Type:** DelayLine (per channel)
- **Maximum Delay:** 40 ms (2048 samples at 48 kHz)
- **Purpose:** Creates timing differences between channels for spatial perception

### Enhancement Filters
- **Left Channel:** Low-shelf filter (BiquadFilter)
- **Right Channel:** High-shelf filter (BiquadFilter)
- **Gain:** +3 dB boost on each
- **Purpose:** Creates complementary EQ for frequency-based width

### All-Pass Filters
- **Left Channel:** All-pass at 0.8x enhance frequency
- **Right Channel:** All-pass at 1.2x enhance frequency
- **Purpose:** Creates subtle phase differences for decorrelation

### Pan LFO
- **Type:** LFO with sine waveform
- **Range:** 0-5 Hz
- **Purpose:** Animates the pan position over time

## Implementation Details

### Signal Flow

```
Mono Input -> LFO Pan Modulation -> Equal Power Panning
                                          |
                                          v
                               Haas Delay Processing
                                          |
                                          v
                               Stereo Enhancement (EQ + Phase)
                                          |
                                          v
                               Mid-Side Width Processing
                                          |
                                          v
                               Apply Final Panning
                                          |
                                          v
                               Mix with Dry Signal
                                          |
                                          v
                               Stereo Output (L/R)
```

### Equal-Power Panning

The pan position is converted to left/right gains using constant power law:
```java
panAngle = (pan + 1.0) * 0.25 * PI;  // 0 to PI/2
gainL = cos(panAngle);
gainR = sin(panAngle);
```

This maintains consistent loudness as the signal moves across the stereo field.

### Haas Effect Processing

Based on the Haas Balance setting:
```java
if (haasBalance > 0) {
    // Delay right channel - sound perceived from left
    haasL = mono;
    haasR = blend(mono, delayedMono, haasBalance);
} else {
    // Delay left channel - sound perceived from right
    haasL = blend(mono, delayedMono, -haasBalance);
    haasR = mono;
}
```

### Stereo Enhancement

Complementary filtering creates frequency-based separation:
```java
// Left gets low frequencies boosted
eqL = lowShelfFilter.process(input);
// Right gets high frequencies boosted
eqR = highShelfFilter.process(input);

// Add phase difference
phaseL = allpassL.process(input);
phaseR = allpassR.process(input);

// Blend enhancement with original
enhL = input + (eqL - input) * enhance + (phaseL - input) * enhance * 0.3;
enhR = input + (eqR - input) * enhance + (phaseR - input) * enhance * 0.3;
```

### Mid-Side Width Processing

Width control using Mid-Side encoding:
```java
// Encode to Mid-Side
mid = (L + R) * 0.5;
side = (L - R) * 0.5;

// Adjust width (0 = mono, 1 = normal, 2 = extra wide)
side *= width;

// Decode back to L/R
widthL = mid + side;
widthR = mid - side;
```

## Usage Tips

### Natural Mono-to-Stereo Conversion
- **Pan:** 0%
- **Width:** 100%
- **Haas Delay:** 15-25 ms
- **Haas Balance:** +100%
- **Enhance:** 20-40%
- **Enhance Freq:** 800-1200 Hz
- **LFO Rate:** 0 Hz
- **Mix:** 100%

Creates a natural, wide stereo image from a mono source.

### Subtle Widening
- **Pan:** 0%
- **Width:** 80-100%
- **Haas Delay:** 10-15 ms
- **Haas Balance:** +50%
- **Enhance:** 15-25%
- **LFO Rate:** 0 Hz

Adds subtle stereo width without obvious processing.

### Extra Wide Stereo
- **Pan:** 0%
- **Width:** 150-200%
- **Haas Delay:** 20-30 ms
- **Haas Balance:** +100%
- **Enhance:** 50-70%

Creates an extremely wide stereo image. Use sparingly.

### Animated Stereo
- **Pan:** 0%
- **Width:** 100%
- **Haas Delay:** 15 ms
- **LFO Rate:** 0.2-0.5 Hz
- **LFO Depth:** 30-50%

Adds gentle movement to the stereo field.

### Off-Center Positioning
- **Pan:** -50% to +50%
- **Width:** 80-100%
- **Haas Delay:** 10-20 ms

Places the sound off-center while maintaining some stereo width.

## Best Practices

### Mono Compatibility
- Test your mix in mono to check for phase cancellation
- Lower Width values are more mono-compatible
- Haas delays can cause comb filtering in mono

### Mix Context
- Wide settings work well for solo instruments
- Use subtler settings when fitting into a dense mix
- Consider the overall stereo image of your mix

### Frequency Considerations
- Set Enhance Freq based on the source material
- Lower frequencies (200-500 Hz) for bass-heavy content
- Higher frequencies (1000-3000 Hz) for guitars and keys

### Avoiding Artifacts
- Very short Haas delays (<5 ms) can cause comb filtering
- Extreme width (>150%) may sound unnatural
- High LFO rates can be distracting

## Technical Specifications

- **Maximum Haas Delay:** 40 ms
- **Haas Buffer Size:** 2048 samples
- **Enhancement Filter Type:** 2nd-order Butterworth shelving
- **All-Pass Filter Q:** 0.707
- **LFO Waveform:** Sine
- **Processing:** 32-bit float
- **Latency:** 0 samples (Haas delay is per-channel, not overall)
- **Output Mode:** Forces stereo output

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/MonoToStereoEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initializes delay lines, filters, and LFO
- `onProcess(...)` - Mono pass-through (stereo conversion requires stereo mode)
- `onProcessStereo(...)` - Full stereo conversion processing
- `updateFilters()` - Updates enhancement filters when parameters change
- `onReset()` - Clears all delay lines and filter states

**Convenience Setters:**
- `setPan(float percent)` - Set pan position
- `setWidth(float percent)` - Set stereo width
- `setHaasDelay(float ms)` - Set Haas delay time
- `setEnhance(float percent)` - Set enhancement amount

## See Also

- [PannerEffect](PannerEffect.md) - Automated panning with LFO modulation
- [ChorusEffect](ChorusEffect.md) - Alternative stereo widening through modulation
- [SplitterEffect](SplitterEffect.md) - For creating parallel stereo paths
