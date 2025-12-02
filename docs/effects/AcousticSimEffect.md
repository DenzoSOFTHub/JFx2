# AcousticSimEffect

Acoustic guitar body simulator with multiple instrument models.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Filter |
| **ID** | `acousticsim` |
| **Display Name** | Acoustic Sim |

## Description

The Acoustic Simulator transforms an electric guitar signal into various acoustic instrument tones by applying body resonance simulation, EQ shaping, and transient enhancement. Each model uses carefully tuned resonant filters to emulate the acoustic properties of different instrument bodies.

The effect works by:
1. Applying multiple resonant bandpass filters at body-specific frequencies
2. Shaping the EQ with low shelf, mid peak, and high shelf filters
3. Enhancing pick attack transients
4. Adding model-specific effects (chorus for 12-string, comb filters for resonator/banjo)

## Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `model` | Model | (see below) | Dreadnought | - | Acoustic instrument model |
| `body` | Body | 0-100 | 60 | % | Body resonance amount |
| `top` | Top | 0-100 | 50 | % | Top brightness/high frequency character |
| `attack` | Attack | 0-100 | 50 | % | Transient/pick attack enhancement |
| `warmth` | Warmth | 0-100 | 50 | % | Low-end warmth/bass body |
| `air` | Air | 0-100 | 30 | % | High frequency air/presence |
| `blend` | Blend | 0-100 | 100 | % | Wet/dry mix |

## Available Models (10)

| # | Model | Body Size | Character | Best For |
|---|-------|-----------|-----------|----------|
| 0 | **Dreadnought** | Large | Balanced, full tone | Strumming, all-around |
| 1 | **Jumbo** | Extra Large | Enhanced bass | Big chords, rhythm |
| 2 | **Parlor** | Small | Midrange focus | Fingerpicking, blues |
| 3 | **Concert** | Medium | Articulate, balanced | Fingerpicking |
| 4 | **Nylon** | Classical | Warm, mellow | Classical, flamenco |
| 5 | **12-String** | Large | Shimmering, doubled | Folk, jangle |
| 6 | **Mandolin** | Small | Bright, cutting | Folk, bluegrass |
| 7 | **Resonator** | Metal cone | Metallic, nasal | Blues, slide |
| 8 | **Banjo** | Drum head | Plucky, bright | Bluegrass, country |
| 9 | **Ukulele** | Tiny | High, bright | Hawaiian, novelty |

## Model Resonance Frequencies

Each model uses three resonant bandpass filters tuned to characteristic body frequencies:

| Model | Resonance 1 | Resonance 2 | Resonance 3 |
|-------|-------------|-------------|-------------|
| Dreadnought | 100 Hz (Q=2.0) | 200 Hz (Q=1.5) | 400 Hz (Q=1.2) |
| Jumbo | 80 Hz (Q=2.5) | 160 Hz (Q=2.0) | 350 Hz (Q=1.5) |
| Parlor | 150 Hz (Q=1.8) | 300 Hz (Q=2.0) | 600 Hz (Q=1.5) |
| Concert | 110 Hz (Q=1.8) | 220 Hz (Q=1.6) | 450 Hz (Q=1.4) |
| Nylon | 90 Hz (Q=2.2) | 180 Hz (Q=1.8) | 350 Hz (Q=1.3) |
| 12-String | 100 Hz (Q=2.0) | 200 Hz (Q=1.5) | 400 Hz (Q=1.2) |
| Mandolin | 200 Hz (Q=2.0) | 400 Hz (Q=2.5) | 800 Hz (Q=2.0) |
| Resonator | 150 Hz (Q=3.0) | 450 Hz (Q=4.0) | 900 Hz (Q=3.5) |
| Banjo | 250 Hz (Q=2.5) | 500 Hz (Q=3.0) | 1000 Hz (Q=2.5) |
| Ukulele | 300 Hz (Q=2.0) | 600 Hz (Q=2.5) | 1200 Hz (Q=2.0) |

## DSP Components

### Body Resonance Filters

Three biquad bandpass filters simulate the resonant frequencies of the acoustic body:

```
       ┌─────────────────────┐
       │ Resonance 1 (Low)   │──┐
Input ─┼─────────────────────┤  │
       │ Resonance 2 (Mid)   │──┼──► Sum ──► Output
       ├─────────────────────┤  │
       │ Resonance 3 (High)  │──┘
       └─────────────────────┘
```

### EQ Section

Three-band EQ shapes the overall tone:
- **Low Shelf**: Warmth control at model-specific frequency
- **Mid Peak**: Presence/body control with Q=1.5
- **High Shelf**: Air/brightness control

### Transient Enhancer

Envelope follower detects transients and adds emphasis:
- Attack coefficient: 0.01 (fast)
- Release coefficient: 0.0005 (slow)
- Enhancement adds difference between instantaneous level and envelope

### Special Model Effects

**12-String**: Adds chorus effect for string-doubling shimmer
- LFO rate: 1.5 Hz
- Delay: 15ms ± modulation depth
- Mix: 40% wet

**Resonator/Banjo**: Adds comb filter for metallic resonance
- Resonator: 150 Hz comb, 40% feedback
- Banjo: 200 Hz comb, 30% feedback

**Nylon**: Low-pass filter at 6 kHz for warm rolled-off tone

**Ukulele**: High-pass filter at 200 Hz to remove bass

## Signal Flow

```
Input ──┬──► Body Resonance Filters ──► EQ Shaping ──► Transient Enhance
        │                                                      │
        │                                              Model-Specific FX
        │                                                      │
        └──────────────────────────────────────────────────────┼──► Blend ──► Output
                           (Dry Signal)                        │
                                                    (Wet Signal)
```

## Usage Tips

### Natural Acoustic Tone

```
Model: Dreadnought or Concert
Body: 60%
Top: 50%
Attack: 40%
Warmth: 50%
Air: 30%
Blend: 100%
```

### Full-Body Strumming

```
Model: Jumbo
Body: 70%
Top: 40%
Attack: 30%
Warmth: 70%
Air: 20%
Blend: 100%
```

### Fingerpicking Clarity

```
Model: Concert or Parlor
Body: 50%
Top: 60%
Attack: 60%
Warmth: 40%
Air: 50%
Blend: 100%
```

### Jangly 12-String

```
Model: 12-String
Body: 60%
Top: 70%
Attack: 50%
Warmth: 40%
Air: 60%
Blend: 100%
```

### Classical Nylon

```
Model: Nylon
Body: 70%
Top: 30%
Attack: 20%
Warmth: 60%
Air: 20%
Blend: 100%
```

### Slide Blues (Resonator)

```
Model: Resonator
Body: 80%
Top: 60%
Attack: 50%
Warmth: 40%
Air: 70%
Blend: 100%
```

## Best Practices

### Input Signal

- Works best with a clean electric guitar signal
- Neck pickup provides warmer base tone
- Bridge pickup adds more string definition
- Reduce gain/distortion before this effect

### Signal Chain Position

- Place early in chain, before distortion/overdrive
- Can be placed after compressor for more consistent response
- Place before reverb and delay for natural sound

### Blending

- Use 80-100% blend for realistic acoustic tone
- 50-70% blend creates electric/acoustic hybrid
- Lower blend values useful for subtle acoustic character

### Pickup Selection

| Electric Pickup | Recommended Models |
|-----------------|-------------------|
| Neck humbucker | Dreadnought, Jumbo, Nylon |
| Middle single | Concert, Parlor |
| Bridge single | 12-String, Mandolin |
| Bridge humbucker | Resonator, Banjo |

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 0 samples |
| CPU Usage | Low-Medium |
| Resonance Filters | 3 × Biquad bandpass |
| EQ Filters | 3 × Biquad (shelf + peak) |
| Chorus Buffer | 100ms |
| Comb Buffer | 20ms |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/AcousticSimEffect.java`
- **Factory ID**: `acousticsim`
- **Key Methods**:
  - `processDreadnought()`, `processJumbo()`, etc.: Model-specific processing
  - `applyResonantFilter()`: Body resonance
  - `applyEQ()`: Three-band EQ
  - `applyTransientEnhance()`: Pick attack enhancement
  - `applyChorus()`: 12-string shimmer
  - `applyCombFilter()`: Metallic resonance

## See Also

- [FilterEffect.md](FilterEffect.md) - Basic filtering
- [ParametricEQEffect.md](ParametricEQEffect.md) - Advanced EQ
- [IRLoaderEffect.md](IRLoaderEffect.md) - Convolution-based acoustic simulation
