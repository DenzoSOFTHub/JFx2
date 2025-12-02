# StereoImageReverbEffect

Reverb with advanced stereo imaging and spatial positioning.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Reverb |
| **ID** | `stereoimagereverb` |
| **Display Name** | Stereo Image Reverb |

## Description

The Stereo Image Reverb creates spacious, immersive stereo field through reverb processing. Unlike standard reverbs that produce mono-ish wet signals, this effect focuses on creating wide, enveloping spaces with precise stereo positioning.

Key features:
- **Haas Effect Delays**: Stereo positioning through inter-channel delay
- **Decorrelated Early Reflections**: 8 taps per channel panned across stereo field
- **Independent L/R Late Reverb**: Separate FDN tanks with cross-feed
- **Mid/Side Processing**: Precise width control
- **Modulated Delays**: Movement and depth through pitch modulation

## Parameters

### Row 1: Reverb

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mix` | Mix | 0-100 | 35 | % | Dry/wet balance |
| `decay` | Decay | 0.2-15 | 3 | s | Reverb tail length |
| `predelay` | Pre-Delay | 0-100 | 15 | ms | Initial delay before reverb |
| `damping` | Damping | 0-100 | 40 | % | High frequency absorption |
| `diffusion` | Diffusion | 0-100 | 70 | % | Reverb density and smoothness |
| `mod` | Modulation | 0-100 | 25 | % | Pitch modulation for movement |

### Row 2: Stereo Imaging

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `width` | Width | 0-200 | 120 | % | Overall stereo width (0=mono, 100=normal, 200=wide) |
| `depth` | Depth | 0-100 | 60 | % | Front-to-back depth perception |
| `spread` | Spread | 0-100 | 80 | % | Early reflection spread across stereo field |
| `haas` | Haas | 0-35 | 0 | ms | Stereo positioning delay (Haas effect) |
| `crossfeed` | X-Feed | 0-50 | 15 | % | Channel cross-mixing for cohesion |
| `balance` | Balance | -100 to +100 | 0 | - | Left/right balance |

## DSP Architecture

### Signal Flow

```
Input ──► Haas Delay ──► Pre-Delay ──► Early Reflections (8 per channel)
                                              │
                                              ▼
                                       All-Pass Diffusors (2 per channel)
                                              │
                                              ▼
                                       FDN Late Reverb (4 delays per channel)
                                              │
                                     ┌────────┴────────┐
                                     ▼                 ▼
                                  Damping         Modulation
                                     │                 │
                                     └────────┬────────┘
                                              ▼
                                         Cross-Feed
                                              │
                                              ▼
                                    Width (Mid/Side)
                                              │
                                              ▼
                                        Balance
                                              │
                                              ▼
                                       Dry/Wet Mix
                                              │
                                              ▼
                                      Stereo Output
```

### Haas Effect

Creates stereo positioning by delaying one channel:
- 0 ms: Centered image
- 1-5 ms: Subtle widening
- 5-20 ms: Strong left/right positioning
- 20-35 ms: Echo-like separation

### Early Reflections

8 taps per channel with decorrelated timing:

| Tap | Left Time (ms) | Right Time (ms) | Left Pan | Right Pan | Gain |
|-----|----------------|-----------------|----------|-----------|------|
| 1 | 7.3 | 9.1 | -0.9 | 0.9 | 0.85 |
| 2 | 13.7 | 15.9 | 0.3 | -0.3 | 0.75 |
| 3 | 21.2 | 24.6 | -0.6 | 0.6 | 0.65 |
| 4 | 29.8 | 33.2 | 0.7 | -0.7 | 0.55 |
| 5 | 41.5 | 44.7 | -0.4 | 0.4 | 0.45 |
| 6 | 53.1 | 58.3 | 0.8 | -0.8 | 0.38 |
| 7 | 67.9 | 71.2 | -0.2 | 0.2 | 0.30 |
| 8 | 79.4 | 83.6 | 0.5 | -0.5 | 0.22 |

Pan is scaled by the Spread parameter.

### All-Pass Diffusors

2 cascaded all-pass filters per channel:

| Stage | Left Time | Right Time | Gain |
|-------|-----------|------------|------|
| 1 | 113 samples | 127 samples | 0.6 |
| 2 | 199 samples | 211 samples | 0.6 |

Effective gain scaled by Diffusion parameter.

### Late Reverb FDN

4 parallel delays per channel with different times:

| Delay | Left (samples) | Right (samples) | LFO Rate (Hz) | Mod Depth |
|-------|----------------|-----------------|---------------|-----------|
| 1 | 1427 | 1531 | 0.37 | 8 |
| 2 | 1637 | 1709 | 0.53 | 10 |
| 3 | 1823 | 1907 | 0.71 | 12 |
| 4 | 2011 | 2099 | 0.89 | 14 |

### Feedback Calculation

```
feedback = exp(-3 × avgDelayTime / (decayTime × sampleRate))
feedback = min(0.97, feedback)  // Stability limit
```

### Width Processing (Mid/Side)

```
mid = (L + R) × 0.5
side = (L - R) × 0.5 × width
output_L = mid + side
output_R = mid - side
```

## Usage Tips

### Wide Ambient Space

```
Decay: 3-5s
Width: 150%
Spread: 90%
Depth: 70%
Modulation: 30%
X-Feed: 20%
```

### Tight Room with Stereo Interest

```
Decay: 1-1.5s
Pre-Delay: 10ms
Width: 110%
Spread: 60%
Haas: 5ms
```

### Immersive Pad Wash

```
Decay: 8-12s
Mix: 50-60%
Width: 180%
Spread: 100%
Modulation: 40%
Damping: 50%
```

### Vocal with Positioning

```
Decay: 2s
Pre-Delay: 30ms
Width: 100%
Haas: 10ms (positions left)
X-Feed: 25%
```

### Mono-Compatible Wide

```
Width: 130%
Spread: 70%
X-Feed: 30%
```
Higher cross-feed maintains mono compatibility.

## Best Practices

### Stereo Width

- **0-50%**: Narrower than input, mono-ish reverb
- **100%**: Natural stereo width
- **120-150%**: Enhanced stereo, good for pads and ambient
- **150-200%**: Extreme width, may cause phase issues

### Haas Effect Usage

| Haas Time | Effect |
|-----------|--------|
| 0 ms | Centered |
| 1-10 ms | Widening without obvious delay |
| 10-20 ms | Distinct positioning |
| 20-35 ms | Echo-like, less natural |

### Cross-Feed

- Lower values (0-15%): Maximum stereo separation
- Medium values (15-30%): Balanced, natural cohesion
- Higher values (30-50%): More centered, better mono compatibility

### Modulation

- Low (0-20%): Subtle movement, natural
- Medium (20-40%): Noticeable shimmer
- High (40-70%): Obvious pitch wobble, special effect

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 0 samples |
| Max Haas Delay | 50 ms (2400 samples @ 48kHz) |
| Max Pre-Delay | 100 ms |
| Early Reflections | 8 per channel |
| All-Pass Diffusors | 2 per channel |
| FDN Delays | 4 per channel |
| Modulation LFOs | 4 per channel (0.37-0.89 Hz) |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/StereoImageReverbEffect.java`
- **Factory ID**: `stereoimagereverb`
- **Key Methods**:
  - `processInternal()`: Main stereo processing
  - Haas effect, early reflections, FDN, width processing

## See Also

- [PlateReverbEffect.md](PlateReverbEffect.md) - Plate reverb simulation
- [RoomReverbEffect.md](RoomReverbEffect.md) - Physical room modeling
- [ReverbEffect.md](ReverbEffect.md) - Basic algorithmic reverb
