# TubeDistortionEffect

Vacuum tube distortion with selectable tube types.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Distortion |
| **ID** | `tubedist` |
| **Display Name** | Tube Distortion |

## Description

The Tube Distortion effect simulates the nonlinear behavior of various vacuum tubes used in guitar amplifiers. Each tube type has unique characteristics affecting gain, harmonic content, and compression.

## Tube Types

### Preamp Tubes

| Tube | Gain | Character | Typical Use |
|------|------|-----------|-------------|
| **12AX7** | High (mu~100) | Rich harmonics, classic | Most guitar preamps |
| **12AT7** | Medium (mu~60) | Cleaner, articulate | Hi-fi, reverb drivers |
| **12AU7** | Low (mu~20) | Very clean, transparent | Phase inverters, buffers |

### Power Tubes

| Tube | Character | Sound | Typical Amps |
|------|-----------|-------|--------------|
| **EL34** | Aggressive, midrange | British crunch | Marshall, Orange |
| **6L6** | Clean headroom, scooped | American clean/lead | Fender, Mesa |
| **EL84** | Compressed, chimey | Jangly, compressed | Vox AC30 |
| **6V6** | Early breakup, sweet | Warm, singing | Fender Deluxe |

## Parameters

### Row 1: Tube Selection

| ID | Name | Range | Default | Description |
|----|------|-------|---------|-------------|
| `tube` | Tube | Choice | 12AX7 | Vacuum tube type to emulate |
| `stages` | Stages | 1-4 | 2 | Number of cascaded tube stages |

### Row 2: Drive & Bias

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `drive` | Drive | 0-100 | 50 | % | Input gain, how hard tubes are driven |
| `bias` | Bias | -100 to +100 | 0 | % | Operating point. Cold=crossover dist, Hot=compression |

### Row 3: Character

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `sag` | Sag | 0-100 | 30 | % | Power supply sag on loud notes |
| `tone` | Tone | 0-100 | 50 | % | High frequency content |

### Row 4: Output

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `output` | Output | 0-100 | 50 | % | Output level |
| `mix` | Mix | 0-100 | 100 | % | Dry/wet balance |

## Tube Modeling

### Transfer Function

Each tube stage applies an asymmetric waveshaping function:

```
        Output
          │
     1.0 ─┤        ╭────────  Grid conduction (soft)
          │      ╱
          │    ╱
     0.0 ─┼──╱────────────── Input
          │╱
          │
    -1.0 ─┤────────╯          Plate saturation (harder)
          │
```

### Characteristics Modeled

1. **Asymmetric Clipping**: Positive peaks (grid) clip softer than negative (plate)
2. **Even Harmonics**: 2nd harmonic content for warmth (more in power tubes)
3. **Compression**: Signal compression as tubes saturate
4. **Bias Shift**: Operating point affects crossover and compression

### Bias Parameter

```
Cold (-100%)              Normal (0%)              Hot (+100%)
     │                         │                        │
     ▼                         ▼                        ▼
┌─────────┐             ┌─────────┐              ┌─────────┐
│ More    │             │ Balanced│              │ More    │
│crossover│             │ tone    │              │compress │
│distort. │             │         │              │ + sag   │
└─────────┘             └─────────┘              └─────────┘
```

### Sag Simulation

Power supply sag occurs when loud notes draw more current:
- **Fast attack**: Quick response to transients
- **Slow release**: Gradual recovery
- **Result**: Dynamic, touch-sensitive compression

## Signal Flow

```
                    ┌────────────────────────────────────────────────┐
                    │              TUBE DISTORTION                   │
                    │                                                │
Input ─────────────►│  ┌─────────┐                                  │
                    │  │ Highpass│ (30Hz DC block)                  │
                    │  └────┬────┘                                  │
                    │       │                                        │
                    │       ▼                                        │
                    │  ┌─────────┐                                  │
                    │  │  Drive  │ × gain                           │
                    │  └────┬────┘                                  │
                    │       │                                        │
                    │       ▼                                        │
                    │  ┌─────────┐                                  │
                    │  │   Sag   │ envelope compression             │
                    │  └────┬────┘                                  │
                    │       │                                        │
                    │       ▼                                        │
                    │  ┌─────────┐                                  │
                    │  │  Tube   │──┐                               │
                    │  │ Stage 1 │  │ × N stages                    │
                    │  └────┬────┘  │                               │
                    │       └───────┘                               │
                    │       │                                        │
                    │       ▼                                        │
                    │  ┌─────────┐                                  │
                    │  │DC Block │                                  │
                    │  └────┬────┘                                  │
                    │       │                                        │
                    │       ▼                                        │
                    │  ┌─────────┐  ┌──────────┐                    │
                    │  │  Tone   │─►│ Presence │ (power tubes)      │
                    │  └────┬────┘  └────┬─────┘                    │
                    │       │            │                          │
                    │       ▼            ▼                          │
                    │  ┌─────────────────────┐                      │
                    │  │     Soft Limit      │                      │
                    │  └──────────┬──────────┘                      │
                    │             │                                  │
                    │             ▼                                  │
                    │  ┌─────────────────────┐                      │
                    │  │     Dry/Wet Mix     │─────────────────────►│ Output
                    │  └─────────────────────┘                      │
                    │                                                │
                    └────────────────────────────────────────────────┘
```

## Usage Tips

### Clean to Crunch (12AX7)
```
Tube: 12AX7
Stages: 1
Drive: 30%
Bias: 0%
Sag: 20%
Tone: 60%
```

### Classic Rock (EL34)
```
Tube: EL34
Stages: 2
Drive: 60%
Bias: +10%
Sag: 40%
Tone: 50%
```

### High Gain Lead (12AX7 x4)
```
Tube: 12AX7
Stages: 4
Drive: 70%
Bias: -10%
Sag: 30%
Tone: 55%
```

### Fender Clean (6L6)
```
Tube: 6L6
Stages: 1
Drive: 25%
Bias: +5%
Sag: 25%
Tone: 65%
```

### Vox Chime (EL84)
```
Tube: EL84
Stages: 2
Drive: 45%
Bias: 0%
Sag: 50%
Tone: 70%
```

### Tweed Breakup (6V6)
```
Tube: 6V6
Stages: 2
Drive: 55%
Bias: +15%
Sag: 45%
Tone: 45%
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Tube Types | 7 (3 preamp + 4 power) |
| Max Stages | 4 |
| Input Filter | 30 Hz highpass |
| Tone Range | 1 kHz - 10 kHz |
| Presence | 3 kHz peak (power tubes) |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/TubeDistortionEffect.java`
- **Factory ID**: `tubedist`
- **Key Methods**:
  - `processTubeStage()`: Single tube stage waveshaping
  - `softClip()`: Output limiting

## See Also

- [OverdriveEffect.md](OverdriveEffect.md) - Softer overdrive
- [DistortionEffect.md](DistortionEffect.md) - Hard clipping distortion
- [AmpEffect.md](AmpEffect.md) - Full amp simulation
- [TubePreampEffect.md](TubePreampEffect.md) - Preamp-only simulation
