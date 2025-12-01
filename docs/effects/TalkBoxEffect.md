# TalkBoxEffect

## Overview

TalkBoxEffect is a formant filter effect that simulates vocal sounds by applying resonant bandpass filters tuned to vowel frequencies. It creates "talking" guitar sounds similar to a hardware talk box, but without requiring a tube and microphone.

**Category:** Filter
**ID:** `talkbox`
**Display Name:** Talk Box

## Description

This effect uses multiple bandpass filters positioned at formant frequencies to shape your guitar signal into vowel-like sounds. By morphing between vowels and using LFO modulation, you can create animated, speech-like tones. Unlike a real talk box which requires you to physically shape the sound with your mouth, this effect provides automatic vowel selection and morphing.

## Parameters

### Vowel
- **ID:** `vowel`
- **Type:** Choice
- **Options:** A, E, I, O, U
- **Default:** A
- **Description:** Target vowel sound. Use Morph to blend between vowels. Each vowel has distinct formant frequencies that create its characteristic sound.

### Morph
- **ID:** `morph`
- **Range:** 0% to 100%
- **Default:** 0%
- **Unit:** %
- **Description:** Blend to next vowel (cyclically). 0% = selected vowel, 100% = next vowel in sequence. Allows smooth transitions between vowel sounds.

### Q (Resonance)
- **ID:** `resonance`
- **Range:** 2 to 15
- **Default:** 8
- **Description:** Formant resonance. Higher values create more pronounced, clearer vowel character. Lower values are more subtle.

### Rate
- **ID:** `rate`
- **Range:** 0 Hz to 5 Hz
- **Default:** 0 Hz
- **Unit:** Hz
- **Description:** LFO speed for automatic vowel morphing. 0 = manual control only. Higher values create faster vowel sweeps.

### Depth
- **ID:** `depth`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** LFO modulation depth for vowel morphing. Controls how far the LFO sweeps through the vowel cycle.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Balance between dry and formant-filtered signal. 100% = pure talk box effect, lower values blend in the original guitar.

## Implementation Details

### Formant Frequencies

The effect models human vowel formants (resonant frequencies of the vocal tract). Each vowel is defined by three formant frequencies (F1, F2, F3):

| Vowel | F1 (Hz) | F2 (Hz) | F3 (Hz) |
|-------|---------|---------|---------|
| A | 730 | 1090 | 2440 |
| E | 660 | 1720 | 2410 |
| I | 270 | 2290 | 3010 |
| O | 570 | 840 | 2410 |
| U | 440 | 1020 | 2240 |

These values are based on average male voice formants and provide recognizable vowel sounds when applied to guitar signals.

### Filter Bank

Three parallel bandpass filters (biquad) are used:

```
Input Signal
    |
    +---> Formant Filter 1 (F1) ---> * gain1 --+
    |                                          |
    +---> Formant Filter 2 (F2) ---> * gain2 --+--> Sum --> Mix --> Output
    |                                          |
    +---> Formant Filter 3 (F3) ---> * gain3 --+
```

Gain scaling by formant number:
- F1 (lowest): gain = 1.0
- F2 (middle): gain = 0.5
- F3 (highest): gain = 0.33

This weighting emphasizes lower formants as in natural speech.

### Vowel Interpolation

When morphing between vowels, formant frequencies are linearly interpolated:

```java
for each formant i:
    interpolated[i] = formants[vowel1][i] * (1 - blend)
                    + formants[vowel2][i] * blend
```

The vowel sequence wraps cyclically: A -> E -> I -> O -> U -> A

### LFO Modulation

When Rate > 0, an LFO modulates the morph position:

```java
lfoValue = (lfo.tick() + 1.0) * 0.5 * depth  // 0 to depth
totalMorph = morph + lfoValue
totalMorph = totalMorph % 1.0  // Wrap around

vowelPos = baseVowel + totalMorph * 5  // 5 vowels
vowel1 = floor(vowelPos) % 5
vowel2 = (vowel1 + 1) % 5
blend = vowelPos - floor(vowelPos)
```

This creates animated, sweeping vowel sounds.

### Signal Flow

```
Guitar Input
      |
      v
+-----+-----+
| Formant 1 | (F1, low formant)
+-----+-----+
      |
      | * 1.0
      v
+-----+-----+
| Formant 2 | (F2, mid formant)
+-----+-----+
      |
      | * 0.5
      v
+-----+-----+
| Formant 3 | (F3, high formant)
+-----+-----+
      |
      | * 0.33
      v
    Sum (* 0.7 normalization)
      |
      v
   Mix with Dry
      |
      v
   Output
```

### Filter Configuration

Each formant filter is configured as:
- Type: Bandpass
- Frequency: Interpolated formant frequency
- Q: Resonance parameter value
- Updated per-sample for smooth modulation

## Usage Tips

### Classic Talk Box Sound
- **Vowel:** Manual control (A, E, I, O, U)
- **Morph:** 0% (control vowel selection directly)
- **Q:** 8-10
- **Rate:** 0 Hz
- **Mix:** 100%
- **Tip:** Change vowels while playing to "speak"

### Animated Wah-Like Effect
- **Vowel:** A
- **Morph:** 0%
- **Q:** 6-8
- **Rate:** 1-3 Hz
- **Depth:** 30-50%
- **Mix:** 80-100%
- **Tip:** Creates rhythmic vowel sweeping

### Alien Voice
- **Vowel:** Any
- **Morph:** 50%
- **Q:** 12-15
- **Rate:** 3-5 Hz
- **Depth:** 100%
- **Mix:** 100%
- **Tip:** High resonance + fast rate = otherworldly

### Subtle Vowel Coloring
- **Vowel:** O or U
- **Morph:** 0%
- **Q:** 3-5
- **Rate:** 0 Hz
- **Mix:** 40-60%
- **Tip:** Adds character without overwhelming

### Tips for Best Results
1. **Clean tone first:** Talk box works best on clean or lightly overdriven signals
2. **Sustaining notes:** Let notes ring to hear the vowel character
3. **Dynamic playing:** The effect responds to dynamics
4. **Experiment with Q:** Higher resonance = clearer vowels but can be harsh
5. **Combine with drive:** Light overdrive before the talk box adds harmonics

### Creating "Words"
- Set Rate to 0 for manual control
- Change Vowel parameter while playing to spell out sounds
- Example: "WOW" = U -> A -> U
- Example: "YAH" = I -> A
- Practice timing with your playing

## Technical Specifications

- **Formant Filters:** 3 parallel bandpass filters
- **Filter Type:** Biquad bandpass
- **Vowels:** 5 (A, E, I, O, U)
- **Formant Model:** Male voice averages
- **LFO Waveform:** Triangle
- **Output Scaling:** 0.7 (headroom protection)
- **Processing:** 32-bit float

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/TalkBoxEffect.java`

**Key Methods:**
- `onPrepare(...)` - Initialize formant filters and LFO
- `onProcess(...)` - Mono processing
- `onProcessStereo(...)` - Stereo processing
- `interpolateFormants(...)` - Blend formant frequencies between vowels

**Constants:**
- `NUM_FORMANTS = 3` - Number of formant filters
- `FORMANTS[][]` - Formant frequency table for each vowel
- `VOWEL_NAMES[]` - Vowel labels ("A", "E", "I", "O", "U")

**DSP Components:**
- `BiquadFilter[] formantFiltersL` - Left channel formant bank
- `BiquadFilter[] formantFiltersR` - Right channel formant bank
- `LFO lfo` - Modulation oscillator (triangle)

**Convenience Setters:**
- `setVowel(int vowel)`
- `setMorph(float percent)`
- `setResonance(float q)`
- `setRate(float hz)`
- `setDepth(float percent)`
- `setMix(float percent)`

## See Also

- [SynthEffect](SynthEffect.md) - Guitar-to-synth with pitch tracking
- [EnvelopeFilterEffect](EnvelopeFilterEffect.md) - Dynamic filter effect
- [WahEffect](WahEffect.md) - Traditional wah pedal
- [FilterEffect](FilterEffect.md) - Basic filter effect
