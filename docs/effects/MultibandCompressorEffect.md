# MultibandCompressorEffect

3-band multiband compressor for mastering and dynamic EQ.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Dynamics |
| **ID** | `multibandcomp` |
| **Display Name** | Multiband Comp |

## Description

The Multiband Compressor splits the audio signal into three frequency bands (Low, Mid, High) using phase-coherent Linkwitz-Riley crossover filters, applies independent compression to each band, then sums them back together.

This allows for:
- **Frequency-selective dynamics control**: Compress bass without affecting highs
- **Mastering**: Balance frequency bands for a polished mix
- **Dynamic EQ**: Reduce harsh frequencies only when they exceed a threshold
- **De-essing**: Target high frequencies to reduce sibilance
- **Bass control**: Tighten low end without affecting the rest

## Parameters

### Row 1: Global Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `xLowMid` | Low/Mid | 20-1000 | 200 | Hz | Crossover frequency between Low and Mid bands |
| `xMidHigh` | Mid/High | 1000-16000 | 3000 | Hz | Crossover frequency between Mid and High bands |
| `output` | Output | -12 to +12 | 0 | dB | Master output level |

### Row 2: Low Band Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `lOn` | Low | On/Off | On | - | Enable/disable low band compression |
| `lSolo` | Solo | On/Off | Off | - | Solo low band (mute others) |
| `lThr` | Threshold | -60 to 0 | -20 | dB | Compression threshold |
| `lRatio` | Ratio | 1:1 to 20:1 | 4:1 | :1 | Compression ratio |
| `lAtk` | Attack | 0.1-100 | 20 | ms | Attack time |
| `lRel` | Release | 10-1000 | 200 | ms | Release time |
| `lGain` | Gain | -12 to +12 | 0 | dB | Band makeup/cut gain |

### Row 3: Mid Band Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mOn` | Mid | On/Off | On | - | Enable/disable mid band compression |
| `mSolo` | Solo | On/Off | Off | - | Solo mid band (mute others) |
| `mThr` | Threshold | -60 to 0 | -18 | dB | Compression threshold |
| `mRatio` | Ratio | 1:1 to 20:1 | 3:1 | :1 | Compression ratio |
| `mAtk` | Attack | 0.1-100 | 10 | ms | Attack time |
| `mRel` | Release | 10-1000 | 150 | ms | Release time |
| `mGain` | Gain | -12 to +12 | 0 | dB | Band makeup/cut gain |

### Row 4: High Band Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `hOn` | High | On/Off | On | - | Enable/disable high band compression |
| `hSolo` | Solo | On/Off | Off | - | Solo high band (mute others) |
| `hThr` | Threshold | -60 to 0 | -16 | dB | Compression threshold |
| `hRatio` | Ratio | 1:1 to 20:1 | 2.5:1 | :1 | Compression ratio |
| `hAtk` | Attack | 0.1-100 | 5 | ms | Attack time |
| `hRel` | Release | 10-1000 | 100 | ms | Release time |
| `hGain` | Gain | -12 to +12 | 0 | dB | Band makeup/cut gain |

## DSP Components

### Linkwitz-Riley Crossover Filters

The effect uses 4th-order Linkwitz-Riley (LR4) crossovers, which are standard for professional multiband processing:

- **Phase coherent**: Bands sum back to flat frequency response
- **24 dB/octave slope**: Clean separation between bands
- **Implementation**: Two cascaded 2nd-order Butterworth filters

```
                    ┌──────────────┐
                    │   Lowpass    │───► Low Band
Input ──┬──────────►│   (LR4)      │
        │           └──────────────┘
        │           ┌──────────────┐     ┌──────────────┐
        ├──────────►│  Highpass    │────►│   Lowpass    │───► Mid Band
        │           │   (LR4)      │     │   (LR4)      │
        │           └──────────────┘     └──────────────┘
        │           ┌──────────────┐
        └──────────►│  Highpass    │───► High Band
                    │   (LR4)      │
                    └──────────────┘
```

### Per-Band Compression

Each band has an independent compressor with:

1. **RMS Level Detection**: 50ms window for smooth response
2. **Linked Stereo**: L+R levels averaged for coherent stereo image
3. **Soft Knee**: 6dB fixed knee for transparent compression
4. **Attack/Release Smoothing**: Exponential envelope follower

### Gain Reduction Formula

```
if (inputDb <= threshold - knee/2):
    gainReduction = 0 dB
elif (inputDb >= threshold + knee/2):
    gainReduction = -(overshoot * (1 - 1/ratio))
else:
    // Soft knee interpolation
    x = overshoot + knee/2
    gainReduction = -(x² * (1 - 1/ratio)) / (2 * knee)
```

## Signal Flow

```
            ┌─────────────────────────────────────────────────┐
            │              MULTIBAND COMPRESSOR               │
            │                                                 │
Input ─────►│  ┌─────────┐    ┌────────────┐    ┌─────────┐  │
            │  │ LR4 LP  │───►│ Compressor │───►│  Gain   │──┼──┐
            │  │ @200Hz  │    │   (Low)    │    │  (Low)  │  │  │
            │  └─────────┘    └────────────┘    └─────────┘  │  │
            │                                                 │  │
            │  ┌─────────┐    ┌────────────┐    ┌─────────┐  │  │  ┌───────┐
            │  │ LR4 HP  │───►│ Compressor │───►│  Gain   │──┼──┼─►│  SUM  │───► Output
            │  │ @200Hz  │    │   (Mid)    │    │  (Mid)  │  │  │  └───────┘
            │  │ LR4 LP  │    └────────────┘    └─────────┘  │  │
            │  │ @3kHz   │                                    │  │
            │  └─────────┘                                    │  │
            │                                                 │  │
            │  ┌─────────┐    ┌────────────┐    ┌─────────┐  │  │
            │  │ LR4 HP  │───►│ Compressor │───►│  Gain   │──┼──┘
            │  │ @3kHz   │    │   (High)   │    │ (High)  │  │
            │  └─────────┘    └────────────┘    └─────────┘  │
            │                                                 │
            └─────────────────────────────────────────────────┘
```

## Usage Tips

### Mastering - General Polish

```
Low/Mid: 150 Hz, Mid/High: 4000 Hz
Low:  Threshold=-24, Ratio=3:1, Attack=30ms, Release=250ms
Mid:  Threshold=-20, Ratio=2:1, Attack=15ms, Release=150ms
High: Threshold=-18, Ratio=2:1, Attack=5ms,  Release=100ms
Output: +2 dB
```

### Tighten Bass

```
Low/Mid: 200 Hz
Low:  Threshold=-18, Ratio=4:1, Attack=10ms, Release=150ms, Gain=+2dB
Mid:  Enabled=Off
High: Enabled=Off
```

### De-Essing (Reduce Sibilance)

```
Mid/High: 5000 Hz
Low:  Enabled=Off
Mid:  Enabled=Off
High: Threshold=-25, Ratio=6:1, Attack=1ms, Release=50ms, Gain=-2dB
```

### Dynamic EQ - Tame Harsh Mids

```
Low/Mid: 300 Hz, Mid/High: 2500 Hz
Low:  Enabled=Off
Mid:  Threshold=-22, Ratio=3:1, Attack=5ms, Release=100ms, Gain=-1dB
High: Enabled=Off
```

### Parallel Compression (New York Style)

Use the band gains to mix compressed and uncompressed:
```
All bands: High ratio (8:1+), fast attack
Low Gain: -3dB, Mid Gain: -3dB, High Gain: -3dB
Blend compressed bands back with original signal
```

### Live Guitar - Control Feedback

```
Mid/High: 2000 Hz
Low:  Enabled=Off
Mid:  Threshold=-15, Ratio=4:1, Attack=1ms, Release=80ms
High: Threshold=-12, Ratio=3:1, Attack=1ms, Release=60ms
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Number of Bands | 3 (Low, Mid, High) |
| Crossover Type | Linkwitz-Riley 4th order (LR4) |
| Crossover Slope | 24 dB/octave |
| Detection | RMS (50ms window) |
| Stereo Mode | Linked (L+R averaged) |
| Knee | 6 dB soft knee (fixed) |
| Latency | Minimal (filter delay only) |

## Default Crossover Frequencies

| Band | Frequency Range |
|------|-----------------|
| Low | 20 Hz - 200 Hz |
| Mid | 200 Hz - 3000 Hz |
| High | 3000 Hz - 20000 Hz |

## Implementation Notes

### Biquad Filter Coefficients

Lowpass (Butterworth, Q=0.7071):
```
w0 = 2π * freq / sampleRate
alpha = sin(w0) / (2 * Q)
b0 = (1 - cos(w0)) / 2
b1 = 1 - cos(w0)
b2 = (1 - cos(w0)) / 2
a1 = -2 * cos(w0)
a2 = 1 - alpha
// Normalize by a0 = 1 + alpha
```

Highpass (Butterworth, Q=0.7071):
```
b0 = (1 + cos(w0)) / 2
b1 = -(1 + cos(w0))
b2 = (1 + cos(w0)) / 2
a1 = -2 * cos(w0)
a2 = 1 - alpha
```

### Solo Mode

When a band's Solo is enabled:
- Only that band is output
- Other bands are muted
- Useful for setting per-band compression

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/MultibandCompressorEffect.java`
- **Factory ID**: `multibandcomp`
- **Key Methods**:
  - `extractBand()`: Split signal into frequency bands
  - `compressBand()`: Apply compression to a band
  - `BiquadFilter`: Crossover filter implementation

## See Also

- [CompressorEffect.md](CompressorEffect.md) - Single-band compressor
- [LimiterEffect.md](LimiterEffect.md) - Brickwall limiting
- [GraphicEQEffect.md](GraphicEQEffect.md) - Static EQ
- [FilterEffect.md](FilterEffect.md) - Single filter
