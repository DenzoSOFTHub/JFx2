# HarmonizerEffect

4-voice intelligent harmonizer with scale-aware pitch shifting.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Pitch |
| **ID** | `harmonizer` |
| **Display Name** | Harmonizer |

## Description

The Harmonizer creates rich vocal-style harmonies by pitch-shifting the input signal according to musical scale intervals. Unlike a simple pitch shifter that moves by fixed semitones, the Harmonizer understands musical scales and moves by scale degrees, always producing musically consonant harmonies.

The effect provides 4 independent harmony voices, each with:
- **Interval**: Scale degrees to shift (-7 to +7)
- **Delay**: Time offset for rhythmic effects (0-500ms)
- **Pan**: Stereo position for spatial separation
- **Level**: Volume mix for balance

## Parameters

### Row 1: Global Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `root` | Root | C-B | C | - | Root note of the scale |
| `scale` | Scale | (see below) | Major | - | Scale type for harmony intervals |
| `dry` | Dry | 0-100 | 100 | % | Level of original signal |
| `grain` | Grain | 20-100 | 50 | ms | Processing window size |
| `level` | Level | 0-100 | 100 | % | Master output level for all voices |

### Available Scales (25 total)

#### Major Scale Modes (7)

| # | Scale | Intervals | Character |
|---|-------|-----------|-----------|
| 0 | Ionian (Major) | 0,2,4,5,7,9,11 | Bright, happy, standard major |
| 1 | Dorian | 0,2,3,5,7,9,10 | Minor with major 6th, jazz/funk |
| 2 | Phrygian | 0,1,3,5,7,8,10 | Spanish/flamenco flavor |
| 3 | Lydian | 0,2,4,6,7,9,11 | Dreamy, floating, #4 |
| 4 | Mixolydian | 0,2,4,5,7,9,10 | Dominant 7th, rock/blues |
| 5 | Aeolian (Minor) | 0,2,3,5,7,8,10 | Natural minor, sad |
| 6 | Locrian | 0,1,3,5,6,8,10 | Diminished, dark, unstable |

#### Melodic Minor Modes (7)

| # | Scale | Intervals | Character |
|---|-------|-----------|-----------|
| 7 | Melodic Minor | 0,2,3,5,7,9,11 | Jazz minor, minor with maj7 |
| 8 | Dorian b2 | 0,1,3,5,7,9,10 | Phrygian #6, exotic |
| 9 | Lydian Augmented | 0,2,4,6,8,9,11 | Lydian with #5, very bright |
| 10 | Lydian Dominant | 0,2,4,6,7,9,10 | Lydian b7, jazz fusion |
| 11 | Mixolydian b6 | 0,2,4,5,7,8,10 | Hindu scale, melodic |
| 12 | Locrian #2 | 0,2,3,5,6,8,10 | Half-diminished, jazz |
| 13 | Super Locrian | 0,1,3,4,6,8,10 | Altered scale, tension |

#### Harmonic Minor Modes (7)

| # | Scale | Intervals | Character |
|---|-------|-----------|-----------|
| 14 | Harmonic Minor | 0,2,3,5,7,8,11 | Classical minor, exotic |
| 15 | Locrian #6 | 0,1,3,5,6,9,10 | Dark with raised 6th |
| 16 | Ionian #5 | 0,2,4,5,8,9,11 | Augmented major |
| 17 | Dorian #4 | 0,2,3,6,7,9,10 | Romanian, gypsy |
| 18 | Phrygian Dominant | 0,1,4,5,7,8,10 | Spanish, Middle Eastern |
| 19 | Lydian #2 | 0,3,4,6,7,9,11 | Very bright, unusual |
| 20 | Ultra Locrian | 0,1,3,4,6,8,9 | Diminished, very dark |

#### Pentatonic Scales (3)

| # | Scale | Intervals | Character |
|---|-------|-----------|-----------|
| 21 | Penta Major | 0,2,4,7,9 | Folk, country, pop |
| 22 | Penta Minor | 0,3,5,7,10 | Blues, rock, universal |
| 23 | Penta Dominant | 0,2,4,7,10 | Mixolydian pentatonic |

#### Blues Scale (1)

| # | Scale | Intervals | Character |
|---|-------|-----------|-----------|
| 24 | Blues | 0,3,5,6,7,10 | Blues with blue note (b5) |

### Row 2: Voice 1 Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `v1On` | V1 | On/Off | On | - | Enable/disable voice (skips processing when off) |
| `v1Int` | Interval | -7 to +7 | 2 | deg | Interval in scale degrees |
| `v1Dly` | Delay | 0-500 | 0 | ms | Voice delay time |
| `v1Pan` | Pan | -100 to +100 | -50 | - | Stereo position |
| `v1Lvl` | Level | 0-100 | 80 | % | Voice volume |

### Row 3: Voice 2 Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `v2On` | V2 | On/Off | On | - | Enable/disable voice (skips processing when off) |
| `v2Int` | Interval | -7 to +7 | 4 | deg | Interval in scale degrees |
| `v2Dly` | Delay | 0-500 | 10 | ms | Voice delay time |
| `v2Pan` | Pan | -100 to +100 | 50 | - | Stereo position |
| `v2Lvl` | Level | 0-100 | 70 | % | Voice volume |

### Row 4: Voice 3 Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `v3On` | V3 | On/Off | Off | - | Enable/disable voice (skips processing when off) |
| `v3Int` | Interval | -7 to +7 | -3 | deg | Interval in scale degrees |
| `v3Dly` | Delay | 0-500 | 20 | ms | Voice delay time |
| `v3Pan` | Pan | -100 to +100 | -30 | - | Stereo position |
| `v3Lvl` | Level | 0-100 | 60 | % | Voice volume |

### Row 5: Voice 4 Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `v4On` | V4 | On/Off | Off | - | Enable/disable voice (skips processing when off) |
| `v4Int` | Interval | -7 to +7 | -5 | deg | Interval in scale degrees |
| `v4Dly` | Delay | 0-500 | 30 | ms | Voice delay time |
| `v4Pan` | Pan | -100 to +100 | 30 | - | Stereo position |
| `v4Lvl` | Level | 0-100 | 50 | % | Voice volume |

## Scale Degree Reference

| Degree | Name | Example in C Major |
|--------|------|-------------------|
| 0 | Unison | C → C |
| 1 | Second | C → D |
| 2 | Third | C → E |
| 3 | Fourth | C → F |
| 4 | Fifth | C → G |
| 5 | Sixth | C → A |
| 6 | Seventh | C → B |
| 7 | Octave | C → C (high) |
| -1 | Second below | C → B (low) |
| -2 | Third below | C → A (low) |

## DSP Components

### Granular Pitch Shifting

Each voice uses overlap-add granular processing:

1. **Input Buffer**: Circular buffer stores recent audio
2. **Grain Extraction**: 4 overlapping grains read from buffer
3. **Pitch Ratio**: Scale interval converted to playback speed ratio
4. **Window Function**: Hann window for smooth crossfades
5. **Overlap-Add**: Grains summed with normalized amplitude

### Pitch Ratio Calculation

```
semitones = scale[degree] + (octaves * 12)
pitchRatio = 2^(semitones / 12)
```

For example, in C Major:
- Degree +2 (third) = 4 semitones → ratio 1.26
- Degree +4 (fifth) = 7 semitones → ratio 1.50
- Degree -2 (third below) = -4 semitones → ratio 0.79

### Per-Voice Delay

Each voice has an independent delay line:
- Maximum delay: 500ms
- Used for rhythmic offset effects
- Creates "cascade" or "waterfall" harmonies

### Stereo Panning

Constant-power panning for natural imaging:

```
panL = cos((pan + 1) * π / 4)
panR = sin((pan + 1) * π / 4)
```

## Signal Flow

```
                    ┌─────────────┐
                    │ Voice 1     │
                    │ Pitch+Delay │──┐
                    │ +Pan+Level  │  │
                    └─────────────┘  │
Input ──┬──────────►┌─────────────┐  │
        │          │ Voice 2     │  │     ┌─────────┐
        │          │ Pitch+Delay │──┼────►│  Mixer  │───► Output
        │          │ +Pan+Level  │  │     │  + Dry  │
        │          └─────────────┘  │     └─────────┘
        │          ┌─────────────┐  │          ▲
        │          │ Voice 3     │  │          │
        │          │ Pitch+Delay │──┤          │
        │          │ +Pan+Level  │  │          │
        │          └─────────────┘  │          │
        │          ┌─────────────┐  │          │
        │          │ Voice 4     │──┘          │
        │          │ Pitch+Delay │             │
        │          │ +Pan+Level  │             │
        │          └─────────────┘             │
        │                                      │
        └──────────────────────────────────────┘
                     (Dry Signal)
```

## Usage Tips

### Classic Thirds Harmony

```
Root: Match your song key
Scale: Major or Minor
V1: Int=2, Level=80%, Pan=-30
V2: Int=0, Level=0% (off)
V3: Int=0, Level=0% (off)
V4: Int=0, Level=0% (off)
Dry: 100%
```

### Thick Power Chord

```
Root: Match song key
Scale: Pentatonic Major
V1: Int=4 (fifth), Level=70%, Pan=-50
V2: Int=-4 (fifth below), Level=70%, Pan=50
V3: Int=0, Level=0%
V4: Int=0, Level=0%
Dry: 100%
```

### Full Choir (4-part harmony)

```
Root: C (or song key)
Scale: Major
V1: Int=2 (3rd up), Level=60%, Pan=-60, Delay=5ms
V2: Int=4 (5th up), Level=50%, Pan=60, Delay=10ms
V3: Int=-2 (3rd down), Level=50%, Pan=-40, Delay=15ms
V4: Int=-4 (5th down), Level=40%, Pan=40, Delay=20ms
Dry: 80%
```

### Cascading Arpeggio

```
Scale: Pentatonic Minor
V1: Int=2, Level=60%, Pan=-80, Delay=50ms
V2: Int=4, Level=50%, Pan=80, Delay=100ms
V3: Int=5, Level=40%, Pan=-60, Delay=150ms
V4: Int=7, Level=30%, Pan=60, Delay=200ms
Dry: 70%
```

### Blues Lead

```
Root: Match blues key
Scale: Blues
V1: Int=2, Level=50%, Pan=-50
V2: Int=-2, Level=40%, Pan=50
V3: Int=0, Level=0%
V4: Int=0, Level=0%
Dry: 100%
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | ~50ms (grain size dependent) |
| CPU Usage | Medium-High |
| Voices | 4 |
| Grains per voice | 4 |
| Maximum delay | 500ms per voice |
| Pitch range | ±7 scale degrees (±1 octave+) |

## Implementation Notes

### Scale-Aware Interval Calculation

The effect maps scale degrees to semitones using lookup tables:

```java
private float intervalToSemitones(int interval, int scaleIndex) {
    int[] scale = SCALES[scaleIndex];
    int scaleSize = scale.length - 1;

    // Handle octave wrapping
    int octaves = 0;
    int degree = interval;

    while (degree < 0) {
        degree += scaleSize;
        octaves--;
    }
    while (degree >= scaleSize) {
        degree -= scaleSize;
        octaves++;
    }

    return scale[degree] + (octaves * 12);
}
```

### Output Soft Clipping

To prevent harsh distortion when multiple voices sum:

```java
private float softClip(float x) {
    if (x > 1.0f) {
        return 1.0f - (float) Math.exp(-x + 1.0f);
    } else if (x < -1.0f) {
        return -1.0f + (float) Math.exp(x + 1.0f);
    }
    return x;
}
```

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/HarmonizerEffect.java`
- **Factory ID**: `harmonizer`
- **Key Methods**:
  - `intervalToSemitones()`: Scale degree to semitone conversion
  - `processInternal()`: Main DSP processing
  - `softClip()`: Output limiting

## See Also

- [PitchShifterEffect.md](PitchShifterEffect.md) - Simple pitch shifting
- [OctaverEffect.md](OctaverEffect.md) - Octave generation
- [ChorusEffect.md](ChorusEffect.md) - For slight detuning effects
