# Drive Effect

**Category:** Distortion
**Effect ID:** `drive`
**Class:** `it.denzosoft.jfx2.effects.impl.DriveEffect`

## Overview

The Drive effect emulates the warm, dynamic response of a tube amplifier being pushed into saturation. It features asymmetric clipping for even harmonic content, mid-range boost for thickness, and a tone control for shaping the output. This effect is ideal for blues, rock, and classic overdrive tones.

## Sonic Description

Warm tube-style drive with a focus on musical dynamics and touch response. The Drive effect captures the characteristic "sag" and compression of a tube amp while maintaining pick attack and note definition. The asymmetric clipping generates even harmonics that add warmth and body without harshness. The mid-boost creates thickness and sustain, making it perfect for lead tones and rhythm work.

Compared to Overdrive (which uses symmetric tanh clipping), Drive has a more complex, tube-like response that responds well to playing dynamics and guitar volume changes.

## Parameters

### 1. Gain
- **Range:** 1.0 to 30.0
- **Default:** 8.0
- **Unit:** (linear multiplier)
- **Description:** Amount of drive/saturation. Lower values (1-5) produce clean to slightly crunchy tones. Mid values (6-15) provide classic overdrive. Higher values (16-30) deliver full saturation and sustain.

### 2. Tone
- **Range:** 500.0 Hz to 6000.0 Hz
- **Default:** 2500.0 Hz
- **Unit:** Hz
- **Description:** Output tone control (lowpass filter). Lower frequencies (500-1500 Hz) sound warmer and darker, perfect for jazz or vintage tones. Higher frequencies (3000-6000 Hz) sound brighter and more aggressive, ideal for cutting through a mix.

### 3. Body
- **Range:** 0.0% to 100.0%
- **Default:** 50.0%
- **Unit:** %
- **Description:** Mid-range boost at 800 Hz for thickness and sustain. At 0%, no boost is applied. At 100%, a 9 dB boost adds significant body and "meat" to the tone. This parameter emulates the characteristic mid-forward sound of classic tube amps.

### 4. Level
- **Range:** -20.0 dB to +6.0 dB
- **Default:** 0.0 dB
- **Unit:** dB
- **Description:** Output volume control. Use to match bypassed volume or compensate for gain changes.

## Implementation Details

### Signal Flow

```
Input → HPF (60Hz) → Mid Boost (800Hz) → Asymmetric Clip → Tone (LPF) → Output LPF (8kHz) → Level → Output
```

### Filters

#### Input High-Pass Filter (60 Hz)
- **Type:** Butterworth highpass
- **Frequency:** 60 Hz (tighter than Overdrive's 80 Hz)
- **Q:** 0.707
- **Purpose:** Removes DC offset and excessive low-end rumble for tighter bass response

#### Mid Boost (800 Hz)
- **Type:** Peaking filter
- **Frequency:** 800 Hz
- **Q:** 1.2
- **Gain:** 0 to 9 dB (based on Body parameter)
- **Purpose:** Pre-gain mid boost for thickness and sustain, emulating tube amp voicing

#### Tone Filter
- **Type:** Butterworth lowpass
- **Frequency:** 500 Hz to 6000 Hz (user adjustable)
- **Q:** 0.707
- **Purpose:** Post-clipping tone shaping

#### Output Low-Pass Filter (8 kHz)
- **Type:** Butterworth lowpass
- **Frequency:** 8000 Hz (fixed)
- **Q:** 0.707
- **Purpose:** Anti-aliasing and smoothing to reduce harsh high-frequency harmonics

### Clipping Algorithm

The Drive effect uses **asymmetric soft clipping** via `WaveShaper.asymmetricClip()`. This creates a tube-like response with different compression characteristics for positive and negative signal peaks:

- **Positive peaks:** More compressed (simulates tube grid limiting)
- **Negative peaks:** Less compressed (more headroom)

This asymmetry generates even-order harmonics (2nd, 4th, 6th) that create warmth and body without the harshness of odd harmonics.

The asymmetric algorithm uses exponential shaping:
```
factor = 0.8 + knee * 2.2

For positive half-cycle:
  output = (1.0 - exp(-input * factor)) * 0.95

For negative half-cycle:
  output = -1.0 + exp(input * factor * 0.8)
```

### Body Control

The Body parameter dynamically adjusts the mid-boost filter:
```
bodyAmount = bodyParam / 100.0
boostDb = bodyAmount * 9.0  // 0 to 9 dB boost
filter.configure(PEAK, 800Hz, Q=1.2, boostDb)
```

At 50% (default), approximately 4.5 dB of mid boost is applied, providing balanced thickness without becoming muddy.

## Signal Flow Diagram

```
                      ┌─────────────┐
Input ──────────────>│   HPF 60Hz  │
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │  Mid Boost  │ (800 Hz, 0-9 dB)
                      │   (Body)    │
                      └──────┬──────┘
                             │
                      ┌──────▼──────────┐
                      │  Asymmetric Clip│ (even harmonics)
                      │   (Gain param)  │
                      └──────┬──────────┘
                             │
                      ┌──────▼──────┐
                      │  Tone LPF   │ (500-6000 Hz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │ Output LPF  │ (8 kHz)
                      └──────┬──────┘
                             │
                      ┌──────▼──────┐
                      │    Level    │
                      └──────┬──────┘
                             │
                           Output
```

## Usage Tips

### Clean Boost
- **Gain:** 1-3
- **Tone:** 3000-4000 Hz
- **Body:** 20-40%
- **Level:** 0 to +3 dB
- **Use:** Slight thickening and compression without obvious distortion

### Classic Blues Drive
- **Gain:** 6-10
- **Tone:** 2000-2500 Hz
- **Body:** 50-70%
- **Level:** -3 to 0 dB
- **Use:** Warm, singing lead tone with sustain

### Rock Rhythm Crunch
- **Gain:** 12-18
- **Tone:** 2500-3500 Hz
- **Body:** 40-60%
- **Level:** -3 to 0 dB
- **Use:** Chunky rhythm tone with clarity

### Heavy Lead
- **Gain:** 20-30
- **Tone:** 2000-3000 Hz
- **Body:** 60-80%
- **Level:** -6 to -3 dB
- **Use:** Full saturation with thick mids

### Bright Country Twang
- **Gain:** 3-6
- **Tone:** 4000-5000 Hz
- **Body:** 20-30%
- **Level:** 0 to +3 dB
- **Use:** Clean with sparkle and slight compression

### Dark Jazz Tone
- **Gain:** 2-5
- **Tone:** 1000-1500 Hz
- **Body:** 60-80%
- **Level:** 0 dB
- **Use:** Warm, mellow tone with body

## Comparison with Other Distortion Effects

| Feature | Drive | Overdrive | Distortion |
|---------|-------|-----------|------------|
| Gain range | 1-30 | 1-50x | 1-100x |
| Clipping type | Asymmetric | Symmetric tanh | Selectable (3 types) |
| Harmonics | Even (warm) | Odd (transparent) | Mixed |
| Mid character | Boosted (800 Hz) | Natural | Pre/post EQ |
| Input HPF | 60 Hz | 80 Hz | 100 Hz |
| Tone range | 500-6000 Hz | 200-8000 Hz | 500-8000 Hz |
| Best for | Blues, rock | Clean-to-crunch | High gain, metal |

## Technical Notes

- Stereo processing: Independent left/right channels with matched filter states
- No latency introduced
- All filters use cascaded biquad topology for stability
- Asymmetric clipping creates even harmonics (2nd, 4th) for warmth
- CPU usage: Low to moderate (6 biquad filters + waveshaping per channel)
- Filter states are reset on effect bypass to prevent zipper noise
- Dynamic parameter updates are smoothed by filter coefficient interpolation
