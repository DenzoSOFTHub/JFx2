# NoiseSuppressorEffect

Spectral noise suppressor with automatic noise profile learning.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Dynamics |
| **ID** | `noisesuppressor` |
| **Display Name** | Noise Suppressor |

## Description

The Noise Suppressor automatically learns the frequency profile of background noise during silent periods (when signal is below threshold), then uses spectral subtraction to remove that noise from the signal.

Unlike a simple noise gate which just cuts the signal, the Noise Suppressor:
- **Analyzes** the spectral content of the noise
- **Subtracts** the noise profile from the signal
- **Preserves** the musical content while reducing noise

This is ideal for:
- **Hum removal**: 50/60 Hz electrical hum and harmonics
- **Hiss reduction**: High-frequency noise from preamps
- **Environmental noise**: Air conditioning, fans, computer noise
- **Recording cleanup**: Remove consistent background noise

## Algorithm

### Spectral Subtraction

1. **FFT Analysis**: Signal is analyzed in 2048-sample frames with 75% overlap
2. **Noise Learning**: When signal < threshold, magnitude spectrum is accumulated
3. **Profile Building**: After 10+ frames, average noise spectrum becomes the profile
4. **Subtraction**: For each frame, noise magnitude is subtracted from signal magnitude
5. **Reconstruction**: Clean signal is reconstructed with original phase via IFFT

```
Input ──► FFT ──► |Magnitude| ──► Subtract Noise ──► IFFT ──► Output
                      │                  ▲
                      ▼                  │
              [Below Threshold?]   Noise Profile
                      │                  ▲
                      └──► Accumulate ───┘
```

## Parameters

### Row 1: Detection

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `threshold` | Threshold | -80 to -20 | -50 | dB | Level below which noise learning occurs |
| `sens` | Sensitivity | 1-100 | 20 | ms | Envelope follower speed |

### Row 2: Reduction

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `reduction` | Reduction | 0-100 | 80 | % | Amount of noise reduction |
| `floor` | Floor | -60 to -6 | -30 | dB | Minimum gain to prevent artifacts |

### Row 3: Learning

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `learn` | Learn | On/Off | On | - | Enable automatic noise learning |
| `smooth` | Smoothing | 0-100 | 50 | % | Gain smoothing to reduce musical noise |

### Row 4: Output

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mix` | Mix | 0-100 | 100 | % | Dry/wet balance |

## Usage Tips

### Initial Setup

1. **Set threshold** just above your noise floor
   - Play nothing, watch the signal level
   - Set threshold slightly above the noise peaks

2. **Enable Learn** and wait for profile to build
   - Stay silent for 1-2 seconds
   - Profile builds automatically

3. **Adjust Reduction** to taste
   - Start at 80%, increase if noise persists
   - Decrease if artifacts appear

### Optimal Settings by Noise Type

#### High-Frequency Hiss
```
Threshold: -50 dB
Reduction: 90%
Floor: -40 dB
Smoothing: 60%
```

#### 50/60 Hz Hum
```
Threshold: -45 dB
Reduction: 85%
Floor: -30 dB
Smoothing: 40%
```

#### Environmental Noise
```
Threshold: -55 dB
Reduction: 70%
Floor: -24 dB
Smoothing: 70%
```

### Avoiding Artifacts

**Musical Noise** (warbling artifacts):
- Increase **Smoothing** (50-80%)
- Increase **Floor** (less aggressive)
- Decrease **Reduction**

**Over-suppression** (thin sound):
- Decrease **Reduction**
- Increase **Floor**
- Lower **Mix** for parallel processing

## Technical Specifications

| Specification | Value |
|---------------|-------|
| FFT Size | 2048 samples |
| Hop Size | 512 samples (75% overlap) |
| Window | Hann |
| Latency | 2048 samples (~46ms @ 44.1kHz) |
| Min Learn Frames | 10 |

## Signal Flow

```
                    ┌────────────────────────────────────────────────┐
                    │              NOISE SUPPRESSOR                  │
                    │                                                │
Input ─────────────►│  ┌─────────────┐                              │
        │           │  │  Envelope   │                              │
        │           │  │  Follower   │                              │
        │           │  └──────┬──────┘                              │
        │           │         │                                      │
        │           │    < Threshold?                                │
        │           │         │                                      │
        │           │    ┌────┴────┐                                │
        │           │    │  LEARN  │──► Noise Profile               │
        │           │    └─────────┘         │                      │
        │           │                        ▼                      │
        │           │  ┌─────────┐    ┌────────────┐               │
        └───────────┼─►│   FFT   │───►│  Spectral  │               │
                    │  │ 2048pt  │    │ Subtraction│               │
                    │  └─────────┘    └─────┬──────┘               │
                    │                       │                       │
                    │                 ┌─────▼──────┐               │
                    │                 │   IFFT     │               │
                    │                 │ + Overlap  │               │
                    │                 └─────┬──────┘               │
                    │                       │                       │
                    │                       ▼                       │
                    │              ┌────────────────┐               │
                    │              │  Dry/Wet Mix   │──────────────►│ Output
                    │              └────────────────┘               │
                    │                                                │
                    └────────────────────────────────────────────────┘
```

## Noise Profile Reset

The noise profile is automatically reset when:
- The signal path is restarted
- The effect is reset
- `resetNoiseProfile()` is called programmatically

This ensures the suppressor adapts to the current noise environment each session.

## Comparison with Noise Gate

| Feature | Noise Suppressor | Noise Gate |
|---------|------------------|------------|
| Method | Spectral subtraction | Level-based cut |
| Noise during playing | Reduced | Present |
| Musical content | Preserved | Preserved |
| Latency | ~46ms | Minimal |
| CPU Usage | Higher | Lower |
| Learning | Automatic | None |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/NoiseSuppressorEffect.java`
- **Factory ID**: `noisesuppressor`
- **Key Methods**:
  - `processFFTFrame()`: FFT analysis and spectral subtraction
  - `resetNoiseProfile()`: Clear learned noise profile

## See Also

- [NoiseGateEffect.md](NoiseGateEffect.md) - Simple level-based noise gate
- [CompressorEffect.md](CompressorEffect.md) - Dynamic range compression
- [FilterEffect.md](FilterEffect.md) - Frequency filtering
