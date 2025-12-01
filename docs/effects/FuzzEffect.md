# Fuzz Effect

**Category:** Distortion
**Effect ID:** `fuzz`
**Class:** `it.denzosoft.jfx2.effects.impl.FuzzEffect`

## Overview

Classic Fuzz effect emulating the aggressive, square-wave-like distortion of vintage 1960s/70s fuzz pedals like the Fuzz Face, Big Muff, and Octavia. Characterized by massive sustain, thick harmonics, buzzy texture, and three distinct voicing modes for different classic fuzz characters.

## Sonic Description

The Fuzz effect creates extreme saturation with near-square-wave distortion, massive compression, and distinctive "buzzy" or "woolly" texture. Unlike overdrive or distortion which aim for smooth saturation, fuzz is intentionally aggressive and raw. At high settings, the waveform approaches a square wave, creating infinite sustain and thick, complex harmonics.

The three fuzz types offer different tonal flavors:
- **Classic:** Warm, germanium-style fuzz with mid boost (Fuzz Face character)
- **Muff:** Scooped mids, thick bass and treble (Big Muff character)
- **Octave:** Upper octave generation for psychedelic tones (Octavia character)

Perfect for psychedelic rock, stoner metal, garage rock, and any situation requiring extreme, vintage-flavored distortion.

## Parameters

### 1. Fuzz
- **Range:** 1.0 to 100.0
- **Default:** 70.0
- **Unit:** (gain amount)
- **Description:** Amount of fuzz/gain. Lower values (1-30) provide thick overdrive-like tones. Mid values (40-70) deliver classic fuzz character. Higher values (75-100) approach square wave with maximum compression and sustain.

### 2. Tone
- **Range:** 200.0 Hz to 5000.0 Hz
- **Default:** 1500.0 Hz
- **Unit:** Hz (lowpass cutoff)
- **Description:** Output tone control. Lower frequencies (200-800 Hz) create thick, dark, doom-like tones. Mid frequencies (1000-2500 Hz) provide classic fuzz warmth. Higher frequencies (3000-5000 Hz) add cutting brightness and definition.

### 3. Type
- **Type:** Choice (0-2)
- **Default:** 0 (Classic)
- **Options:**
  - **0 - Classic:** Fuzz Face style - warm germanium character with mid boost at 500 Hz (+3 dB)
  - **1 - Muff:** Big Muff style - scooped mids at 600 Hz (-6 dB), thick bass and treble
  - **2 - Octave:** Octavia style - upper octave generation with high-mid boost at 1200 Hz (+4 dB)
- **Description:** Fuzz character and voicing. Each type has distinct frequency response and harmonic content.

### 4. Sustain
- **Range:** 0.0% to 100.0%
- **Default:** 60.0%
- **Unit:** %
- **Description:** Controls compression and note sustain. Lower values (0-30%) allow more dynamics and note decay. Mid values (40-70%) provide balanced sustain. Higher values (75-100%) create extreme compression with near-infinite sustain and reduced dynamic range.

### 5. Level
- **Range:** -20.0 dB to +6.0 dB
- **Default:** -3.0 dB
- **Unit:** dB
- **Description:** Output volume. Default is -3 dB to compensate for gain. May need further reduction at extreme fuzz settings.

## Implementation Details

### Signal Flow

```
Input → HPF (40Hz) → Input LPF (4kHz) → Fuzz Clipping → Octave (Type 2) → Mid Filter (Type-dependent) → Tone (LPF) → Output LPF (6kHz) → Level → Output
```

### Filters

#### Input High-Pass Filter (40 Hz)
- **Type:** Butterworth highpass
- **Frequency:** 40 Hz (lower than other effects - lets more bass through)
- **Q:** 0.5
- **Purpose:** Removes DC offset while preserving low frequencies for thick fuzz tone

#### Input Low-Pass Filter (4 kHz)
- **Type:** Butterworth lowpass
- **Frequency:** 4000 Hz (fixed)
- **Q:** 0.707
- **Purpose:** Tames harsh highs before clipping, emulates input capacitance of vintage circuits

#### Mid Filter (Type-dependent)
- **Type:** Peaking filter
- **Frequency/Gain:**
  - Classic: 500 Hz, Q=1.0, +3 dB (warm mid boost)
  - Muff: 600 Hz, Q=0.8, -6 dB (mid scoop)
  - Octave: 1200 Hz, Q=1.5, +4 dB (upper-mid boost for octave clarity)
- **Purpose:** Shapes frequency response to match classic fuzz voicings

#### Tone Filter
- **Type:** Butterworth lowpass
- **Frequency:** 200 Hz to 5000 Hz (user adjustable)
- **Q:** 0.707
- **Purpose:** User-adjustable tone control for brightness

#### Output Low-Pass Filter (6 kHz)
- **Type:** Butterworth lowpass
- **Frequency:** 6000 Hz (lower than other effects - fuzz is naturally dark)
- **Q:** 0.707
- **Purpose:** Final smoothing and anti-aliasing

### Fuzz Clipping Algorithm

The fuzz effect uses a unique two-stage clipping algorithm:

```java
private float fuzzClip(float input, float fuzz, float sustain) {
    // Pre-gain with sustain compression
    float sustainFactor = 1.0 + sustain * 2.0;  // 1.0 to 3.0
    float gained = input * fuzz * sustainFactor;

    // Stage 1: Soft clip (tanh)
    float stage1 = tanh(gained * 2.0);

    // Stage 2: Hard clip approaching square wave
    float threshold = 0.8 - (fuzz/100.0) * 0.5;  // 0.8 to 0.3
    float stage2 = hardClip(stage1 * 1.5, threshold);

    // Mix stages based on fuzz amount
    float fuzzMix = fuzz / 100.0;
    return stage1 * (1.0 - fuzzMix * 0.7) + stage2 * fuzzMix * 0.7;
}
```

**Stage 1 (Soft Clip):**
- Tanh saturation with drive of 2.0
- Provides smooth compression
- Generates odd harmonics

**Stage 2 (Hard Clip):**
- Variable threshold: 0.8 (low fuzz) to 0.3 (high fuzz)
- Approaches square wave at high fuzz settings
- Generates all harmonic orders

**Mixing:**
- At low fuzz: More stage 1 (smoother)
- At high fuzz: More stage 2 (harder, more square-like)
- Progressive transition for musical control

**Sustain Control:**
- Multiplies pre-gain by 1.0 to 3.0
- Higher sustain = more compression
- Interacts with fuzz amount for character control

### Octave Effect (Type 2)

The Octave mode uses full-wave rectification to generate upper octave:

```java
private float octaveUp(float input, float lastSample) {
    // Full-wave rectification creates octave up
    return Math.abs(input) * 0.7 + input * 0.3;
}
```

In processing:
```java
if (type == 2) {
    sample = sample * 0.6 + octaveUp(sample, lastSample) * 0.4;
}
```

- 60% original signal
- 40% octave-up signal
- Full-wave rectification doubles frequency
- Creates psychedelic, Hendrix-style tones

## Signal Flow Diagram

```
                      ┌─────────────┐
Input ──────────────>│  HPF 40Hz   │
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │  LPF 4kHz   │ (pre-filter)
                      └──────┬──────┘
                             │
                      ┌──────▼──────────────┐
                      │   Fuzz Clipping     │
                      │  Stage 1: Tanh      │
                      │  Stage 2: Hard Clip │
                      │  (Fuzz + Sustain)   │
                      └──────┬──────────────┘
                             │
                      ┌──────▼──────────┐
                      │  Octave Effect  │ (Type 2 only)
                      │ (Full-wave rect)│
                      └──────┬──────────┘
                             │
                      ┌──────▼──────────┐
                      │   Mid Filter    │
                      │ • Classic: +3dB │
                      │ • Muff: -6dB    │
                      │ • Octave: +4dB  │
                      └──────┬──────────┘
                             │
                      ┌──────▼──────┐
                      │  Tone LPF   │ (200-5000 Hz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │ Output LPF  │ (6 kHz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │    Level    │
                      └──────┬──────┘
                             │
                           Output
```

## Usage Tips

### Classic Germanium Fuzz (Fuzz Face style)
- **Fuzz:** 60-80
- **Tone:** 1200-1800 Hz
- **Type:** Classic (0)
- **Sustain:** 50-70%
- **Level:** -3 dB
- **Use:** Warm, vintage fuzz with mid presence. Works great with guitar volume cleanup.

### Big Muff Style
- **Fuzz:** 75-90
- **Tone:** 1500-2500 Hz
- **Type:** Muff (1)
- **Sustain:** 70-90%
- **Level:** -6 dB
- **Use:** Thick, scooped sustain for heavy riffs and doomy tones.

### Psychedelic Octave (Hendrix style)
- **Fuzz:** 80-95
- **Tone:** 2000-3000 Hz
- **Type:** Octave (2)
- **Sustain:** 60-80%
- **Level:** -3 to 0 dB
- **Use:** Upper octave doubling for soaring leads and psychedelic textures.

### Stoner/Doom
- **Fuzz:** 85-100
- **Tone:** 500-1000 Hz (dark)
- **Type:** Muff (1)
- **Sustain:** 80-100%
- **Level:** -9 to -6 dB
- **Use:** Maximum sustain and thickness for slow, heavy riffs.

### Garage Rock
- **Fuzz:** 70-85
- **Tone:** 1800-2500 Hz
- **Type:** Classic (0)
- **Sustain:** 50-70%
- **Level:** -3 dB
- **Use:** Raw, aggressive rhythm tones with character.

### Modern Fuzz Lead
- **Fuzz:** 75-85
- **Tone:** 2500-3500 Hz
- **Type:** Classic (0) or Octave (2)
- **Sustain:** 60-75%
- **Level:** 0 to +3 dB
- **Use:** Cutting lead tone with definition and sustain.

### Velcro Fuzz (Extreme)
- **Fuzz:** 95-100
- **Tone:** 800-1500 Hz
- **Type:** Any
- **Sustain:** 90-100%
- **Level:** -12 to -9 dB
- **Use:** Square wave madness, gated fuzz, extreme textures.

## Comparison with Other Distortion Effects

| Feature | Fuzz | Distortion | Drive | Overdrive |
|---------|------|------------|-------|-----------|
| Gain range | 1-100 | 1-100x | 1-30 | 1-50x |
| Clipping | Multi-stage + octave | Selectable (3 types) | Asymmetric | Symmetric tanh |
| Character | Buzzy, square-wave | Aggressive | Warm tube | Smooth |
| Compression | Extreme | High | Moderate | Moderate |
| Sustain control | Yes (dedicated param) | No | No | No |
| Types/modes | 3 (Classic/Muff/Octave) | 3 clip types | None | None |
| Input HPF | 40 Hz (loose) | 100 Hz | 60 Hz | 80 Hz |
| Output LPF | 6 kHz (dark) | 8 kHz | 8 kHz | 10 kHz |
| Mid shaping | Type-dependent | Pre-tone control | 800 Hz boost | None |
| Best for | Psych, stoner, garage | Metal, modern | Blues, rock | Classic rock |

## Technical Notes

### Fuzz Characteristics
- **Compression:** Extreme, especially at high sustain settings
- **Harmonics:** All orders (odd + even), approaching square wave
- **Dynamics:** Reduced but interactive with sustain control
- **Cleanup:** Less responsive to guitar volume than overdrive/drive
- **Stacking:** Works best first in chain or with minimal gain before

### Type-Specific Details

**Classic (Type 0):**
- Mid boost at 500 Hz adds warmth and body
- Germanium-style character
- Most dynamic of the three types
- Best for classic rock, blues rock

**Muff (Type 1):**
- Mid scoop at 600 Hz creates "smile" curve
- Thick bass, present treble
- Less mid presence, more extreme sound
- Best for stoner, doom, psych

**Octave (Type 2):**
- Full-wave rectification adds upper octave
- High-mid boost at 1200 Hz helps octave clarity
- Most aggressive and complex
- Best for leads, psychedelic sounds

### Implementation Details
- Stereo processing: Independent left/right with state tracking for octave effect
- Latency: Zero
- Filters: 10 biquad filters total per channel (5 per side in stereo)
- CPU usage: Moderate (multiple filters + complex waveshaping)
- State variables: Stores last sample for octave effect
- Filter states reset on bypass

### Recommended Settings for Cleanup
To use guitar volume for cleanup (works best with Classic type):
- Keep Fuzz at 50-75
- Set Sustain to 40-60%
- Reduce guitar volume to 3-5 for clean tone
- Full guitar volume for full fuzz

### Signal Chain Recommendations
Place Fuzz:
- **First in chain** for classic behavior and maximum interaction
- After wah for envelope filter effects
- Before modulation/delay/reverb (always)
- Avoid stacking with other gain effects (fuzz is self-contained)
