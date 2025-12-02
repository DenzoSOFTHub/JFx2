# Pan3DEffect

Realistic 3D spatial audio positioning with HRTF modeling.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Modulation |
| **ID** | `pan3d` |
| **Display Name** | Pan 3D |

## Description

The Pan 3D effect provides realistic spatial audio positioning using Head-Related Transfer Function (HRTF) approximation. Unlike simple stereo panning which only adjusts left/right balance, this effect simulates how sound actually reaches our ears from different positions in 3D space.

The effect models:
- **ITD (Interaural Time Difference)**: Sound arrives at the closer ear first
- **ILD (Interaural Level Difference)**: Sound is louder in the closer ear
- **Head Shadow**: High frequencies are blocked by the head
- **Pinna Filtering**: Ear shape affects frequency response based on elevation
- **Distance Modeling**: Volume and air absorption change with distance
- **Early Reflections**: Room reflections help localize the source

## Parameters

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `angle` | Angle | 0-360 | 0 | ° | Horizontal position (azimuth) |
| `elevation` | Elevation | -90 to +90 | 0 | ° | Vertical position |
| `distance` | Distance | 0.1-10 | 1 | m | Distance from listener |
| `width` | Width | 0-100 | 20 | % | Source width (point to diffuse) |
| `room` | Room | 0-100 | 30 | % | Early reflections amount |
| `hrtf` | HRTF | 0-100 | 80 | % | HRTF modeling intensity |
| `air` | Air | 0-100 | 50 | % | Air absorption (HF loss) |
| `doppler` | Doppler | 0-100 | 0 | % | Doppler effect intensity |

## Angle Reference

```
                    0° (Front)
                       │
                       │
         315° ─────────┼───────── 45°
              ╲        │        ╱
               ╲       │       ╱
                ╲      │      ╱
                 ╲     │     ╱
    270° (Left) ──────[●]────── 90° (Right)
                 ╱     │     ╲
                ╱      │      ╲
               ╱       │       ╲
              ╱        │        ╲
         225° ─────────┼───────── 135°
                       │
                       │
                   180° (Rear)
```

## HRTF Components

### ITD (Interaural Time Difference)

Sound travels around the head, causing delay to the far ear:

| Angle | ITD | Description |
|-------|-----|-------------|
| 0° (front) | 0 ms | Equal time to both ears |
| 45° | ~0.35 ms | Slight delay to left ear |
| 90° (right) | ~0.7 ms | Maximum delay to left ear |
| 180° (rear) | 0 ms | Equal time (behind head) |
| 270° (left) | ~0.7 ms | Maximum delay to right ear |

Formula: `ITD = head_radius × sin(angle) / speed_of_sound`

### ILD (Interaural Level Difference)

Head blocks sound to the far ear, creating level difference:

- Maximum attenuation: ~6 dB at 90° for mid frequencies
- High frequencies: More attenuation (shorter wavelength)
- Low frequencies: Less attenuation (wrap around head)

### Head Shadow Filter

Low-pass filter on the far ear, simulating high-frequency blocking:

| Angle | Far Ear Cutoff |
|-------|----------------|
| 0° | 20 kHz (no shadow) |
| 45° | ~11 kHz |
| 90° | ~2 kHz (full shadow) |

### Pinna (Ear) Filtering

Ear shape creates notch filters that change with elevation:

- Notch frequency: 6-10 kHz range
- Notch shifts with elevation angle
- Helps brain determine up/down direction

### Front/Back Disambiguation

Rear sources have high-frequency roll-off due to ear shape:

- High shelf cut at 3 kHz
- Maximum -6 dB at 180° (directly behind)
- Helps distinguish front from rear

### Air Absorption

High frequencies absorbed over distance:

| Distance | Effect |
|----------|--------|
| < 0.5m | No absorption |
| 1m | Slight HF roll-off |
| 5m | Noticeable darkening |
| 10m | Significant HF loss |

### Early Reflections

Three reflection delays simulate room acoustics:

| Reflection | Delay | Level |
|------------|-------|-------|
| 1st | 10-20 ms | -10 dB |
| 2nd | 25-50 ms | -14 dB |
| 3rd | 40-80 ms | -16 dB |

Room parameter scales both delay time and level.

## Signal Flow

```
Stereo Input ──► Mix to Mono ──► ITD Delay Lines ──► Head Shadow Filter
                     │                                      │
                     │                                      ▼
                     │                             Pinna Notch Filter
                     │                                      │
                     │                                      ▼
                     │                             Front/Back High Shelf
                     │                                      │
                     │                                      ▼
                     │                             Air Absorption Filter
                     │                                      │
                     │          ┌──────────────────────────┘
                     │          │
                     │          ▼
                     │   Apply ILD Gains ──► Add Early Reflections
                     │                              │
                     │                              ▼
                     └────────► Width Mix ──► Stereo Output (L/R)
```

## Usage Tips

### Realistic Positioning

```
Angle: Position as desired
Elevation: 0° (ear level)
Distance: 1-2m
Width: 20%
Room: 30%
HRTF: 80%
Air: 50%
Doppler: 0%
```

### Circling Effect

Automate Angle from 0° to 360° over time:
- Slow (4-8 seconds): Smooth circling
- Fast (1-2 seconds): Dizzying effect

### Behind the Listener

```
Angle: 180°
Elevation: 0°
HRTF: 100%
```
High HRTF ensures clear rear positioning.

### Elevated Source

```
Angle: 0° (front)
Elevation: 45° (above)
HRTF: 80%
```
Pinna filtering creates perception of height.

### Distant Source

```
Distance: 5-10m
Room: 50%
Air: 80%
```
Dark, reverberant, quiet.

### Close Whisper

```
Distance: 0.1-0.3m
Room: 0%
Air: 0%
Width: 0%
```
Intimate, present, no room.

### Doppler Effect

Enable when automating angle for realistic motion:
```
Doppler: 30-50%
```
Adds pitch shift when source moves toward/away.

## Best Practices

### Headphone Monitoring

- HRTF effects work best on headphones
- Speaker playback loses some 3D effect
- Front/back discrimination reduced on speakers

### Mono Compatibility

- Effect may cancel when summed to mono
- Test mono compatibility if needed
- Reduce HRTF intensity for better mono translation

### Automation

- Smooth angle changes to avoid jumps
- Parameter smoothing built-in (0.001 coefficient)
- Avoid rapid position changes unless desired

### With Other Effects

- Place before reverb for realistic room positioning
- Place after reverb to position the wet sound
- Combine with delay for spatial effects

## Physical Constants

| Constant | Value | Description |
|----------|-------|-------------|
| Speed of Sound | 343 m/s | At room temperature |
| Head Radius | 8.75 cm | Average human head |
| Ear Distance | 17.5 cm | Average ear separation |
| Max ITD | ~0.7 ms | At 90° angle |
| Max ITD Samples | 64 | At 44.1 kHz |

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | 32 samples (~0.7 ms @ 44.1 kHz) |
| ITD Buffer | 64 samples |
| Reflection Buffer | 100 ms |
| Filters | One-pole LP, Biquad notch, High shelf |
| Parameter Smoothing | 0.001 coefficient |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/Pan3DEffect.java`
- **Factory ID**: `pan3d`
- **Key Methods**:
  - `onProcessStereo()`: Main processing
  - `applyLowPass()`: Head shadow filter
  - `applyNotch()`: Pinna elevation filter
  - `applyHighShelf()`: Front/back filter
  - `applyAirAbsorption()`: Distance HF loss
  - `getLatency()`: Returns 32 samples

## See Also

- [PannerEffect.md](PannerEffect.md) - Simple stereo auto-panning
- [MonoToStereoEffect.md](MonoToStereoEffect.md) - Mono to stereo conversion
- [StereoToMonoEffect.md](StereoToMonoEffect.md) - Stereo to mono conversion
