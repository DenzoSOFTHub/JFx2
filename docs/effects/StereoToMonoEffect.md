# StereoToMonoEffect

Intelligent stereo to mono conversion with phase handling.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Utility |
| **ID** | `stereo2mono` |
| **Display Name** | Stereo→Mono |

## Description

The Stereo to Mono effect provides intelligent conversion from stereo to mono with multiple modes and phase-aware processing. Unlike simple summing which can cause phase cancellation, this effect includes correlation detection and correction to preserve signal integrity.

Key features:
- **7 Conversion Modes**: From simple sum to intelligent phase-aware summing
- **Phase Correlation Detection**: Monitors L/R correlation in real-time
- **Auto Phase Correction**: Prevents cancellation artifacts
- **Bass Mono Mode**: Mono bass with stereo highs (common in mastering)
- **Width & Balance**: Pre-conversion adjustments

## Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mode` | Mode | (see below) | Sum (L+R) | - | Conversion mode |
| `phaseCorrect` | Phase | (see below) | Auto | - | Phase correction mode |
| `crossover` | Crossover | 20-500 | 120 | Hz | Bass Mono crossover frequency |
| `width` | Width | 0-200 | 100 | % | Pre-conversion stereo width |
| `balance` | Balance | 0-100 | 50 | % | L/R balance (50% = center) |
| `output` | Output | -12 to +12 | 0 | dB | Output gain compensation |

## Conversion Modes

| # | Mode | Formula | Use Case |
|---|------|---------|----------|
| 0 | **Sum (L+R)** | (L + R) × 0.707 | Standard mono conversion |
| 1 | **Left Only** | L | Use left channel only |
| 2 | **Right Only** | R | Use right channel only |
| 3 | **Mid** | (L + R) × 0.5 | Extract center content |
| 4 | **Side** | (L - R) × 0.5 | Extract stereo difference |
| 5 | **Smart** | Phase-weighted sum | Intelligent summing |
| 6 | **Bass Mono** | Mono below crossover | Tighten low end |

### Sum (L+R) Mode

Classic mono summing with -3 dB compensation to prevent clipping:
```
mono = (left + right) × 0.707
```

### Left/Right Only

Simple channel selection, no processing.

### Mid Mode

Extracts the center (mono-compatible) content:
```
mid = (left + right) × 0.5
```

### Side Mode

Extracts the stereo difference (what's different between L and R):
```
side = (left - right) × 0.5
```

### Smart Mode

Correlation-weighted summing that detects phase issues:

1. Calculate instantaneous correlation
2. If signals are out of phase (correlation < 0):
   - Use the louder channel more (80/20 split)
3. If signals are in phase:
   - Normal sum with -3 dB compensation

```java
if (phaseAccumulator < 0) {
    // Out of phase - favor louder channel
    if (absL > absR) {
        mono = left × 0.8 + right × 0.2;
    } else {
        mono = left × 0.2 + right × 0.8;
    }
} else {
    // In phase - normal sum
    mono = (left + right) × 0.707;
}
```

### Bass Mono Mode

Sums bass frequencies to mono while keeping highs in stereo:

1. Split signal at crossover frequency
2. Sum bass to mono
3. Sum highs to mono (or could keep stereo)

Uses Butterworth low-pass/high-pass filters.

## Phase Correction Modes

| # | Mode | Description |
|---|------|-------------|
| 0 | **Off** | No phase correction |
| 1 | **Auto** | Detect and correct phase issues |
| 2 | **Invert L** | Invert left channel polarity |
| 3 | **Invert R** | Invert right channel polarity |

### Auto Phase Detection

Monitors correlation coefficient in 1024-sample windows:

```
correlation = Σ(L × R) / √(Σ(L²) × Σ(R²))
```

| Correlation | Meaning | Action |
|-------------|---------|--------|
| +1.0 | Identical (mono) | Normal sum |
| 0.0 | Uncorrelated | Normal sum |
| -0.5 to -1.0 | Out of phase | Invert one channel |

When correlation < -0.5, automatically inverts right channel.

## DSP Components

### Correlation Detector

Rolling window correlation coefficient:
- Window size: 1024 samples
- Updates every 1024 samples
- Normalized to -1 to +1 range

### Crossover Filters (Bass Mono)

Linkwitz-Riley 2nd order filters:
- Low-pass for bass extraction
- High-pass for treble extraction
- Flat summed response

### Mid/Side Processing

Pre-conversion width adjustment:
```
mid = (L + R) × 0.5
side = (L - R) × 0.5 × width
L' = mid + side
R' = mid - side
```

## Signal Flow

```
Stereo Input ──► Phase Correction ──► Balance Adjust ──► Width Adjust
                                                              │
                                                              ▼
                                                        Mode Processing
                                                              │
                                ┌─────────────────────────────┴─────────────────────────────┐
                                │           │           │           │           │           │
                                ▼           ▼           ▼           ▼           ▼           ▼
                             Sum(L+R)   Left Only  Right Only    Mid        Side        Smart
                                │           │           │           │           │           │
                                └───────────┴───────────┴───────────┴───────────┴───────────┘
                                                              │
                                                              ▼
                                                        Output Gain
                                                              │
                                                              ▼
                                                   Mono Output (L = R)
```

## Usage Tips

### Standard Mono Conversion

```
Mode: Sum (L+R)
Phase: Auto
Width: 100%
Balance: 50%
Output: 0 dB
```

### Extracting Center Vocals

```
Mode: Mid
Width: 100%
```
Isolates content panned to center (vocals, bass, kick).

### Extracting Sides (Remove Vocals)

```
Mode: Side
Width: 100%
```
Removes center content, keeps stereo elements.

### Bass Tightening (Mastering)

```
Mode: Bass Mono
Crossover: 80-150 Hz
```
Common mastering technique for vinyl cutting and club systems.

### Mono Compatibility Check

Use to preview how a stereo mix translates to mono:
```
Mode: Sum (L+R)
Phase: Auto
```

### Fixing Phase Issues

If stereo track sounds thin/hollow:
```
Mode: Sum (L+R)
Phase: Auto (or try Invert L/R)
```

## Best Practices

### Avoid Phase Cancellation

- Use Auto phase correction by default
- Listen for "hollow" or "thin" sound indicating cancellation
- Smart mode handles most problematic material

### Bass Mono Crossover

| Application | Crossover |
|-------------|-----------|
| Vinyl mastering | 80-100 Hz |
| Club/PA systems | 100-120 Hz |
| General mixing | 120-150 Hz |
| Subwoofer alignment | 80 Hz |

### Preserving Stereo Character

- Use Width > 100% before conversion to exaggerate stereo
- Use Balance to favor one side if centered too much

### Level Compensation

Mono summing may change perceived level:
- In-phase signals: +3 dB louder
- Out-of-phase signals: Can cancel to silence
- Use Output gain to compensate

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 0 samples |
| Correlation Window | 1024 samples |
| Crossover Filter | Butterworth 2nd order |
| Processing | 32-bit float |
| Output | Mono (identical L and R) |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/StereoToMonoEffect.java`
- **Factory ID**: `stereo2mono`
- **Key Methods**:
  - `onProcessStereo()`: Main processing
  - `updateCorrelation()`: Phase correlation detector
  - `smartSum()`: Correlation-weighted summing
  - `bassMonoProcess()`: Crossover processing

## See Also

- [MonoToStereoEffect.md](MonoToStereoEffect.md) - Mono to stereo conversion
- [Pan3DEffect.md](Pan3DEffect.md) - 3D spatial positioning
- [PannerEffect.md](PannerEffect.md) - Stereo auto-panning
