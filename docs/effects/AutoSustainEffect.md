# AutoSustainEffect

Threshold-based sustainer with configurable decay time.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Dynamics |
| **ID** | `autosustain` |
| **Display Name** | Auto Sustain |

## Description

The Auto Sustain effect maintains a constant output volume while the input signal is above a configurable threshold. When the signal falls below the threshold, a hold period delays the onset of decay, followed by a smooth exponential fade-out.

This creates:
- **Pad-like sustain**: Transform guitar into ambient textures
- **Consistent levels**: Even note volumes for recording
- **Smooth swells**: Natural volume envelope shaping
- **Drone sounds**: Long, sustained tones

## Parameters

### Row 1: Detection

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `threshold` | Threshold | -60 to 0 | -40 | dB | Level below which decay begins |
| `sens` | Sensitivity | 1-100 | 10 | ms | Envelope detector response time |

### Row 2: Sustain

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `level` | Level | 0-100 | 70 | % | Target output level during sustain |
| `attack` | Attack | 1-500 | 50 | ms | Time to reach sustain level |

### Row 3: Decay

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `hold` | Hold | 0-2000 | 100 | ms | Wait time before decay starts |
| `decay` | Decay | 100-10000 | 2000 | ms | Time for sound to fade to silence |

### Row 4: Output

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `tone` | Tone | 200-10000 | 5000 | Hz | Lowpass filter cutoff frequency |
| `mix` | Mix | 0-100 | 100 | % | Dry/wet balance |

## Signal Flow

```
                    ┌─────────────────────────────────────────────────┐
                    │               AUTO SUSTAIN                       │
                    │                                                  │
Input ─────────────►│  ┌──────────┐    ┌──────────────┐               │
        │           │  │ Envelope │───►│ Gate Logic   │               │
        │           │  │ Detector │    │ (threshold)  │               │
        │           │  └──────────┘    └──────┬───────┘               │
        │           │                         │                        │
        │           │                    ┌────▼────┐                   │
        │           │                    │ Above?  │                   │
        │           │                    └────┬────┘                   │
        │           │              ┌──────────┴──────────┐             │
        │           │              │                     │             │
        │           │         ┌────▼────┐          ┌────▼────┐        │
        │           │         │ SUSTAIN │          │  DECAY  │        │
        │           │         │ Mode    │          │  Mode   │        │
        │           │         │ (AGC)   │          │ (fade)  │        │
        │           │         └────┬────┘          └────┬────┘        │
        │           │              │     ┌─────┐       │              │
        │           │              └────►│Gain │◄──────┘              │
        │           │                    └──┬──┘                       │
        │           │                       │                          │
        └───────────┼────►[×]◄──────────────┘                         │
                    │      │                                           │
                    │  ┌───▼───┐    ┌───────┐    ┌──────┐             │
                    │  │ Tone  │───►│LowCut │───►│ Clip │────►Output  │
                    │  │Filter │    │ 40Hz  │    │(soft)│             │
                    │  └───────┘    └───────┘    └──────┘             │
                    │                                                  │
                    └──────────────────────────────────────────────────┘
```

## Operating Modes

### Sustain Mode (Signal > Threshold)
- Automatic gain control maintains constant output level
- Attack time controls how quickly gain adjusts
- Hold counter is reset

### Decay Mode (Signal < Threshold)
1. **Hold Phase**: Wait for hold time (prevents false triggers)
2. **Decay Phase**: Exponential fade-out over decay time

## Usage Tips

### Ambient Pads
```
Threshold: -35 dB
Level: 60%
Attack: 100 ms
Hold: 500 ms
Decay: 5000 ms (5 seconds)
Tone: 3000 Hz
Mix: 100%
```

### Recording Sustain
```
Threshold: -30 dB
Level: 80%
Attack: 20 ms
Hold: 50 ms
Decay: 1000 ms
Tone: 8000 Hz
Mix: 100%
```

### Drone/Infinite Sustain
```
Threshold: -50 dB (very sensitive)
Level: 70%
Attack: 200 ms
Hold: 2000 ms
Decay: 10000 ms (10 seconds)
Tone: 4000 Hz
Mix: 100%
```

### Parallel Blend
```
Threshold: -40 dB
Level: 70%
Attack: 50 ms
Hold: 100 ms
Decay: 3000 ms
Mix: 50% (blend with dry for natural attack)
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Detection | Envelope follower (peak) |
| Stereo Mode | Linked (L+R max) |
| Max Gain | +26 dB |
| Low Cut | 40 Hz highpass |
| Clipping | Soft clip at ±0.9 |

## Comparison with Sustainer Effect

| Feature | Auto Sustain | Sustainer |
|---------|--------------|-----------|
| Style | Threshold-gated AGC | E-Bow simulation |
| Decay | User-controlled | Fixed release |
| Hold | Yes (configurable) | No |
| Harmonics | None | Harmonic/Fundamental modes |
| Best For | Ambient, pads | Lead, melody |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/AutoSustainEffect.java`
- **Factory ID**: `autosustain`

## See Also

- [SustainerEffect.md](SustainerEffect.md) - E-Bow style sustainer
- [CompressorEffect.md](CompressorEffect.md) - Dynamic range compression
- [NoiseGateEffect.md](NoiseGateEffect.md) - Threshold-based gating
- [VolumeSwellEffect.md](VolumeSwellEffect.md) - Attack volume shaping
