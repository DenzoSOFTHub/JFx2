# PlateReverbEffect

Classic plate reverb with bright, shimmery character.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Reverb |
| **ID** | `platereverb` |
| **Display Name** | Plate Reverb |

## Description

The Plate Reverb simulates vintage plate reverb units which use a large suspended metal plate to create reverberation. Known for their bright, dense, and shimmery character, plate reverbs are a staple of studio recording.

This implementation uses:
- **All-pass diffusor network** for initial diffusion
- **Feedback delay network (FDN)** for reverb tail
- **High-frequency damping** for natural decay
- **Modulation** for shimmer and depth

## Parameters

### Row 1

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mix` | Mix | 0-100 | 35 | % | Dry/wet balance |
| `decay` | Decay | 0.1-10 | 2.5 | s | Reverb tail length |
| `predelay` | Pre-Delay | 0-100 | 10 | ms | Initial delay before reverb |
| `size` | Size | 20-150 | 100 | % | Simulated plate size |

### Row 2

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `damping` | Damping | 0-100 | 30 | % | High frequency absorption |
| `bright` | Brightness | 0-100 | 70 | % | High frequency content |
| `mod` | Modulation | 0-100 | 20 | % | Pitch modulation for shimmer |
| `width` | Width | 0-150 | 100 | % | Stereo width |

## DSP Architecture

### Signal Flow

```
Input ──► Pre-Delay ──► All-Pass Diffusors (4) ──► FDN Tank (4 delays)
                                                         │
                                                   ┌─────┴─────┐
                                                   ▼           ▼
                                              Damping    Modulation
                                                   │           │
                                                   └─────┬─────┘
                                                         ▼
                                                   Width Control
                                                         │
                                                         ▼
                                                   Dry/Wet Mix
                                                         │
                                                         ▼
                                                  Stereo Output
```

### All-Pass Diffusors

4 cascaded all-pass filters provide initial diffusion:

| Stage | Delay (samples) | Gain |
|-------|-----------------|------|
| 1 | 142 | 0.5 |
| 2 | 107 | 0.5 |
| 3 | 379 | 0.5 |
| 4 | 277 | 0.5 |

### Feedback Delay Network

4 parallel delays with cross-coupled feedback:

| Delay | Base Time (samples) | LFO Rate (Hz) | Mod Depth |
|-------|---------------------|---------------|-----------|
| 1 | 1557 | 0.5 | 12 |
| 2 | 1617 | 0.7 | 14 |
| 3 | 1491 | 0.6 | 11 |
| 4 | 1422 | 0.8 | 13 |

### Feedback Calculation

Based on decay time using the formula:
```
feedback = exp(-3 × avgDelayTime / (decayTime × sampleRate))
```
Limited to maximum 0.98 for stability.

## Usage Tips

### Classic Plate Sound

```
Mix: 30-40%
Decay: 2-3s
Pre-Delay: 10-20ms
Size: 100%
Damping: 30%
Brightness: 70%
Modulation: 20%
Width: 100%
```

### Short Snare Plate

```
Mix: 40%
Decay: 0.8-1.2s
Pre-Delay: 0ms
Damping: 40%
Brightness: 60%
```

### Lush Vocal Plate

```
Mix: 25-35%
Decay: 2.5-3.5s
Pre-Delay: 30-50ms
Modulation: 30%
Width: 120%
```

### Shimmering Pad

```
Decay: 5-8s
Modulation: 50-70%
Brightness: 80%
Width: 150%
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 0 samples |
| Max Pre-Delay | 100 ms |
| Diffusors | 4 × All-pass |
| FDN Delays | 4 |
| Modulation | Per-delay LFO |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/PlateReverbEffect.java`
- **Factory ID**: `platereverb`

## See Also

- [ReverbEffect.md](ReverbEffect.md) - Basic algorithmic reverb
- [ShimmerReverbEffect.md](ShimmerReverbEffect.md) - Pitch-shifted reverb
- [SpringReverbEffect.md](SpringReverbEffect.md) - Spring tank simulation
- [RoomReverbEffect.md](RoomReverbEffect.md) - Physical room modeling
