# RoomReverbEffect

Physically-based room simulation with realistic acoustics.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Reverb |
| **ID** | `roomreverb` |
| **Display Name** | Room Reverb |

## Description

The Room Reverb provides physically-based room simulation using acoustic modeling principles. It calculates early reflections based on room geometry using the image-source method, and generates late reverb using a Feedback Delay Network (FDN) with parameters derived from room acoustics.

Features:
- **Room Dimensions**: Configurable length, width, and height
- **Wall Materials**: 7 material presets with frequency-dependent absorption
- **Early Reflections**: 6 primary + 8 secondary reflections from walls/floor/ceiling
- **Late Reverb**: 8-delay FDN with Sabine RT60 calculation
- **Air Absorption**: Distance-dependent high-frequency roll-off
- **Source/Listener Position**: Affects reflection timing and levels

## Parameters

### Row 1: Room Geometry & Materials

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `roomType` | Room | (preset) | Medium Room | - | Room type preset |
| `length` | Length | 2-50 | 8 | m | Room length |
| `width` | Width | 2-40 | 6 | m | Room width |
| `height` | Height | 2-20 | 3 | m | Room height (ceiling) |
| `wallMat` | Walls | (material) | Drywall | - | Wall surface material |
| `floor` | Floor | 0-100 | 30 | % | Floor absorption |
| `ceiling` | Ceiling | 0-100 | 20 | % | Ceiling absorption |

### Row 2: Acoustics

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mix` | Mix | 0-100 | 40 | % | Dry/wet balance |
| `erLevel` | Early | 0-100 | 80 | % | Early reflections level |
| `tailLevel` | Tail | 0-100 | 70 | % | Late reverb level |
| `diffusion` | Diffusion | 0-100 | 70 | % | Reverb density |
| `airAbs` | Air Abs | 0-100 | 30 | % | Air absorption (HF loss) |
| `srcDist` | Source | 0.5-20 | 3 | m | Source distance |
| `listPos` | Position | 0-100 | 30 | % | Listener position (center to corner) |

## Wall Materials

| # | Material | Low Abs | Mid Abs | High Abs | Character |
|---|----------|---------|---------|----------|-----------|
| 0 | Concrete | 0.02 | 0.02 | 0.03 | Very reflective, hard |
| 1 | Brick | 0.03 | 0.04 | 0.05 | Slightly warmer |
| 2 | Wood Panel | 0.10 | 0.07 | 0.06 | Warm, vintage studio |
| 3 | Drywall | 0.08 | 0.05 | 0.04 | Neutral, common |
| 4 | Glass | 0.03 | 0.02 | 0.02 | Bright, reflective |
| 5 | Carpet/Drapes | 0.15 | 0.35 | 0.50 | Dead, absorptive |
| 6 | Acoustic Tile | 0.25 | 0.60 | 0.80 | Very dead, studio |

## Room Type Presets

| Preset | Dimensions | Character |
|--------|------------|-----------|
| Custom | User-defined | - |
| Small Room | ~4×3×2.5m | Tight, intimate |
| Medium Room | ~8×6×3m | Natural, balanced |
| Large Hall | ~20×15×10m | Spacious, grand |
| Cathedral | ~40×25×20m | Massive, ethereal |
| Bathroom | ~3×2×2.5m | Bright, reflective |
| Studio Booth | ~2×2×2.5m | Very tight, dry |

## Acoustics Modeling

### Early Reflections (Image-Source Method)

6 primary reflections calculated from room geometry:

| Reflection | Surface | Calculation |
|------------|---------|-------------|
| ER 0 | Left Wall | Distance = 2 × source Y position |
| ER 1 | Right Wall | Distance = 2 × (width - source Y) |
| ER 2 | Front Wall | Distance = 2 × source X position |
| ER 3 | Back Wall | Distance = 2 × (length - source X) |
| ER 4 | Floor | Distance = 2 × source Z (height) |
| ER 5 | Ceiling | Distance = 2 × (height - source Z) |

8 secondary reflections from corner combinations (weaker, two-bounce paths).

### Reflection Gain

Combines inverse distance law and material absorption:
```
gain = (1 / (1 + distance × 0.5)) × (1 - absorption)
```

### RT60 (Sabine Equation)

Reverberation time calculated from room properties:
```
RT60 = 0.161 × Volume / Total_Absorption

where:
  Volume = length × width × height
  Total_Absorption = Σ(surface_area × absorption_coefficient)
```

### FDN Delay Times

8 delays based on room modes:

| Delay | Based On |
|-------|----------|
| 1 | Length / speed_of_sound |
| 2 | Width / speed_of_sound |
| 3 | Height / speed_of_sound |
| 4 | √(length² + width²) / c |
| 5 | √(length² + height²) / c |
| 6 | √(width² + height²) / c |
| 7 | √(length² + width² + height²) / c |
| 8 | (length + width + height) / 3 / c |

### 3-Band Damping

Each FDN delay has frequency-dependent damping:
- **Low**: 0.1 coefficient, multiplied by (1 - low_absorption)
- **Mid**: 0.3 coefficient, multiplied by (1 - mid_absorption)
- **High**: 0.7 coefficient, multiplied by (1 - high_absorption)

## Signal Flow

```
Input ──► Early Reflections ────────┬──► Diffusors ──► FDN Tank
              │                     │                      │
              │                     │              ┌───────┴───────┐
              ▼                     │              ▼               ▼
         ER Output                  │         3-Band Damp    Modulation
          × Level                   │              │               │
              │                     │              └───────┬───────┘
              │                     │                      ▼
              │                     │              Air Absorption
              │                     │                      │
              │                     │                      ▼
              │                     │              Tail × Level
              │                     │                      │
              └─────────────────────┼──────────────────────┘
                                    │
                                    ▼
                              Dry/Wet Mix
                                    │
                                    ▼
                             Stereo Output
```

## Usage Tips

### Natural Room Sound

```
Room: Medium Room
Walls: Drywall
Mix: 30-40%
Early: 80%
Tail: 60%
```

### Bright Live Room

```
Walls: Glass or Concrete
Floor: 10%
Ceiling: 10%
Early: 90%
Air Abs: 10%
```

### Dead Studio

```
Walls: Acoustic Tile
Floor: 80%
Ceiling: 60%
Early: 50%
Tail: 40%
```

### Large Concert Hall

```
Length: 30-40m
Width: 20-25m
Height: 15-20m
Walls: Wood Panel
Early: 70%
Tail: 80%
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 0 samples |
| Primary Reflections | 6 |
| Secondary Reflections | 8 |
| FDN Delays | 8 |
| Diffusors | 4 × All-pass |
| Speed of Sound | 343 m/s |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/RoomReverbEffect.java`
- **Factory ID**: `roomreverb`
- **Key Methods**:
  - `updateRoomParameters()`: Calculate reflection times
  - `calculateRT60()`: Sabine reverberation time
  - `metersToSamples()`: Distance to delay conversion
  - `calculateReflectionGain()`: Attenuation calculation

## See Also

- [PlateReverbEffect.md](PlateReverbEffect.md) - Plate reverb simulation
- [ReverbEffect.md](ReverbEffect.md) - Basic algorithmic reverb
- [SpringReverbEffect.md](SpringReverbEffect.md) - Spring tank simulation
