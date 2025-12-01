# Overdrive Effect

**Category:** Distortion
**Effect ID:** `overdrive`
**Class:** `it.denzosoft.jfx2.effects.impl.OverdriveEffect`

## Overview

Classic warm overdrive sound inspired by tube amp breakup. The Overdrive effect features soft tanh clipping for smooth, musical saturation with pre-filtering to remove rumble and post-filtering tone control. This effect is designed for transparent-to-moderate overdrive tones that preserve playing dynamics.

## Sonic Description

Warm tube-style overdrive with smooth, symmetric saturation. The tanh (hyperbolic tangent) clipping provides a natural compression and harmonic enhancement that responds well to guitar volume and picking dynamics. Unlike asymmetric clipping (used in Drive), the symmetric tanh curve generates primarily odd harmonics, resulting in a more transparent, "glassy" character.

Perfect for clean boost, light crunch, classic rock rhythm, and smooth lead tones. The effect maintains clarity even at higher drive settings, making it suitable for both single notes and complex chords.

## Parameters

### 1. Drive
- **Range:** 1.0x to 50.0x
- **Default:** 5.0x
- **Unit:** x (linear multiplier)
- **Description:** Amount of saturation and harmonic content. Lower values (1-3x) provide subtle warmth and compression. Mid values (4-10x) deliver classic overdrive tones. Higher values (15-50x) add more grit and sustain with increased compression.

### 2. Tone
- **Range:** 200.0 Hz to 8000.0 Hz
- **Default:** 3000.0 Hz
- **Unit:** Hz
- **Description:** Lowpass filter cutoff frequency controlling brightness. Lower values (200-1000 Hz) are darker and warmer, suitable for jazz or vintage tones. Mid values (2000-4000 Hz) provide balanced clarity. Higher values (5000-8000 Hz) are brighter and edgier, ideal for cutting through dense mixes.

### 3. Level
- **Range:** -20.0 dB to +6.0 dB
- **Default:** 0.0 dB
- **Unit:** dB
- **Description:** Output volume after distortion. Use to match bypassed volume or boost signal for downstream effects. Typically reduced when using high drive settings to prevent clipping.

## Implementation Details

### Signal Flow

```
Input → HPF (80Hz) → Tanh Clip → Tone (LPF) → Output LPF (10kHz) → Level → Output
```

### Filters

#### Input High-Pass Filter (80 Hz)
- **Type:** Butterworth highpass
- **Frequency:** 80 Hz (fixed)
- **Q:** 0.707
- **Purpose:** Removes DC offset and rumble, preventing low-frequency mud from saturating the clipping stage

#### Tone Filter
- **Type:** Butterworth lowpass
- **Frequency:** 200 Hz to 8000 Hz (user adjustable)
- **Q:** 0.707
- **Purpose:** Post-clipping tone shaping to control brightness and harshness

#### Output Low-Pass Filter (10 kHz)
- **Type:** Butterworth lowpass
- **Frequency:** 10000 Hz (fixed)
- **Q:** 0.707
- **Purpose:** Anti-aliasing and smoothing to reduce harsh high-frequency artifacts from clipping

### Clipping Algorithm

The Overdrive effect uses **symmetric soft clipping** via `WaveShaper.tanhClip()`, which applies the hyperbolic tangent function:

```
output = tanh(input * drive)
```

The tanh function provides:
- Smooth, continuous saturation curve
- Symmetric clipping (same behavior for positive and negative peaks)
- Gradual transition from linear to saturated regions
- Natural-sounding compression
- Primarily odd-order harmonics (1st, 3rd, 5th)

Characteristics:
- **Linear region** (small signals): Nearly unity gain with minimal distortion
- **Transition region** (medium signals): Progressive saturation and compression
- **Saturated region** (large signals): Approaches ±1.0 asymptotically, never exceeding

The tanh curve is softer than hard clipping but harder than some tube models, providing a good balance between transparency and saturation.

## Signal Flow Diagram

```
                      ┌─────────────┐
Input ──────────────>│   HPF 80Hz  │
                      └──────┬──────┘
                             │
                      ┌──────▼──────────┐
                      │   Tanh Clip     │ (symmetric)
                      │  (Drive param)  │
                      └──────┬──────────┘
                             │
                      ┌──────▼──────┐
                      │  Tone LPF   │ (200-8000 Hz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │ Output LPF  │ (10 kHz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │    Level    │
                      └──────┬──────┘
                             │
                           Output
```

## Usage Tips

### Transparent Clean Boost
- **Drive:** 1.0-2.0x
- **Tone:** 5000-8000 Hz
- **Level:** +3 to +6 dB
- **Use:** Minimal saturation, pushes next stage without coloration

### Light Crunch
- **Drive:** 3.0-6.0x
- **Tone:** 3000-4000 Hz
- **Level:** 0 dB
- **Use:** Subtle breakup, responsive to pick attack

### Classic Overdrive
- **Drive:** 7.0-12.0x
- **Tone:** 2500-3500 Hz
- **Level:** -3 to 0 dB
- **Use:** Warm, sustaining rhythm and lead tones

### Heavy Saturation
- **Drive:** 20.0-40.0x
- **Tone:** 2000-3000 Hz
- **Level:** -6 to -3 dB
- **Use:** Full compression and sustain for soaring leads

### Warm Jazz Tone
- **Drive:** 2.0-4.0x
- **Tone:** 800-1500 Hz
- **Level:** 0 dB
- **Use:** Subtle warmth without losing clarity

### Bright Modern Edge
- **Drive:** 8.0-15.0x
- **Tone:** 4000-6000 Hz
- **Level:** -3 dB
- **Use:** Clear, articulate with presence

## Comparison with Other Distortion Effects

| Feature | Overdrive | Drive | Distortion | Fuzz |
|---------|-----------|-------|------------|------|
| Gain range | 1-50x | 1-30 | 1-100x | 1-100 |
| Clipping type | Symmetric tanh | Asymmetric | Selectable | Multi-stage |
| Harmonics | Odd (transparent) | Even (warm) | Mixed | Complex |
| Character | Smooth, glassy | Warm, tube-like | Aggressive | Fuzzy, buzzy |
| Input HPF | 80 Hz | 60 Hz | 100 Hz | 40 Hz |
| Output LPF | 10 kHz | 8 kHz | 8 kHz | 6 kHz |
| Tone controls | 1 (post) | 1 (post) | 2 (pre/post) | 1 (post) |
| Best for | Classic rock, blues | Blues, rock | Metal, high gain | Psych, stoner |

## Technical Notes

### Processing Characteristics
- Symmetric saturation: Equal treatment of positive and negative peaks
- Odd-harmonic content: 3rd, 5th, 7th harmonics predominate
- Smooth compression: Progressive gain reduction, no hard knee
- Transparent at low settings: Minimal coloration below drive = 3x
- Stable at extreme settings: No oscillation or instability

### Implementation Details
- Stereo processing: Independent left/right channels with matched filter states
- No latency introduced
- All filters use cascaded biquad topology
- Filter states are reset on bypass to prevent zipper noise
- Dynamic tone changes are smoothed automatically
- CPU usage: Low (4 biquad filters + tanh waveshaping per channel)

### Mathematical Background

The tanh function maps input to output:
```
y = tanh(x * drive)
  = (e^(x*drive) - e^(-x*drive)) / (e^(x*drive) + e^(-x*drive))
```

Properties:
- Domain: (-∞, +∞)
- Range: (-1, +1)
- Symmetry: tanh(-x) = -tanh(x)
- Derivative at origin: tanh'(0) = 1 (unity gain for small signals)

As drive increases:
- The function becomes steeper near zero
- Saturation occurs earlier in the signal chain
- Harmonic content increases
- Compression becomes more aggressive

### Optimization Notes
- Java's `Math.tanh()` is used for platform-optimized implementation
- No lookup tables needed - tanh is sufficiently fast on modern CPUs
- Filter coefficient updates only when parameters change
- Minimal branching in processing loop for cache efficiency
