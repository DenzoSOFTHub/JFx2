# SynthDroneEffect

Generates synthetic drone oscillators from detected pitch.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Filter |
| **ID** | `synthdrone` |
| **Display Name** | Synth Drone |

## Description

The Synth Drone effect detects the pitch of incoming audio and generates up to 4 synthetic oscillators that follow the detected pitch. Each oscillator can be tuned to different intervals, creating rich drone textures, sub-octaves, or harmonic layers.

Key features:
- **YIN Pitch Detection**: Accurate monophonic tracking
- **4 Oscillators**: Each with independent waveform, interval, detune, volume, pan
- **Intervals**: -24 to +24 semitones for sub-bass to harmonics
- **Waveform Selection**: Sine, Saw, Square, Triangle
- **Envelope Following**: Responds to input dynamics

## Parameters

### Row 1: Global Mix

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `dry` | Dry | 0-100 | 50 | % | Original signal level |
| `wet` | Wet | 0-100 | 80 | % | Synth signal level |
| `attack` | Attack | 1-500 | 50 | ms | Envelope attack time |
| `release` | Release | 10-2000 | 300 | ms | Envelope release time |
| `glide` | Glide | 0-500 | 50 | ms | Pitch glide between notes |

### Rows 2-5: Per-Oscillator Parameters

Each of the 4 oscillators has these parameters:

| ID | Name | Range | Default | Description |
|----|------|-------|---------|-------------|
| `oscNOn` | OscN | on/off | varies | Enable oscillator |
| `oscNWave` | Wave | (choice) | Sine | Waveform type |
| `oscNInt` | Interval | -24 to +24 | varies | Pitch interval (semitones) |
| `oscNDet` | Detune | -50 to +50 | 0 | Fine detune (cents) |
| `oscNVol` | Volume | 0-100 | varies | Oscillator volume |
| `oscNPan` | Pan | -100 to +100 | varies | Stereo position |

### Default Oscillator Settings

| Osc | Enabled | Interval | Volume | Pan | Purpose |
|-----|---------|----------|--------|-----|---------|
| 1 | Yes | 0 (unison) | 100% | Center | Main pitch |
| 2 | Yes | -12 (octave down) | 70% | Center | Sub-octave |
| 3 | No | +7 (fifth up) | 50% | Left | Harmonic layer |
| 4 | No | -5 (fourth down) | 40% | Right | Bass layer |

## Waveforms

| # | Waveform | Character |
|---|----------|-----------|
| 0 | Sine | Pure, clean, fundamental |
| 1 | Saw | Bright, buzzy, rich harmonics |
| 2 | Square | Hollow, clarinet-like, odd harmonics |
| 3 | Triangle | Soft, mellow, few harmonics |

## Common Intervals

| Semitones | Interval | Ratio | Use |
|-----------|----------|-------|-----|
| -24 | 2 octaves down | 1:4 | Deep sub-bass |
| -12 | Octave down | 1:2 | Sub-octave, bass thickening |
| -7 | Fifth down | 2:3 | Power chord low |
| -5 | Fourth down | 3:4 | Bass layer |
| 0 | Unison | 1:1 | Main pitch |
| +5 | Fourth up | 4:3 | Harmonic layer |
| +7 | Fifth up | 3:2 | Power chord high |
| +12 | Octave up | 2:1 | Brightness, shimmer |
| +19 | Octave + fifth | 3:1 | Rich harmonic |

## Signal Flow

```
Audio Input ──► Pitch Detection (YIN) ──► Envelope Follower
                       │                        │
                       ▼                        │
                 Detected Freq                  │
                       │                        │
         ┌─────────────┼─────────────┐          │
         ▼             ▼             ▼          │
      Osc 1         Osc 2         Osc 3/4       │
    (Interval)    (Interval)    (Interval)      │
         │             │             │          │
         ▼             ▼             ▼          │
      Waveform     Waveform     Waveform        │
         │             │             │          │
         ▼             ▼             ▼          │
      Volume       Volume        Volume         │
         │             │             │          │
         ▼             ▼             ▼          │
       Pan L/R      Pan L/R      Pan L/R        │
         │             │             │          │
         └─────────────┼─────────────┘          │
                       ▼                        │
                   Mix Sum ◄────────────────────┘
                       │              (envelope applied)
                       ▼
                  Dry/Wet Mix
                       │
                       ▼
                 Stereo Output
```

## Usage Tips

### Sub-Octave Bass

```
Osc 1: On, Interval 0, Sine, Vol 100%, Center
Osc 2: On, Interval -12, Sine, Vol 80%, Center
Osc 3: Off
Osc 4: Off
Glide: 30ms
```
Adds solid sub-octave for bass thickening.

### Power Chord Drone

```
Osc 1: On, Interval 0, Saw, Vol 100%, Center
Osc 2: On, Interval -12, Saw, Vol 70%, Center
Osc 3: On, Interval +7, Saw, Vol 50%, Left 30
Osc 4: Off
Attack: 100ms
Release: 500ms
```
Creates sustained power chord texture.

### Ambient Pad

```
Osc 1: On, Interval 0, Sine, Vol 80%, Left 20
Osc 2: On, Interval +12, Triangle, Vol 40%, Right 40
Osc 3: On, Interval +7, Sine, Vol 30%, Right 60
Osc 4: On, Interval -12, Triangle, Vol 50%, Left 60
Attack: 200ms
Release: 1000ms
Glide: 150ms
```
Spacious, evolving drone texture.

### Organ-Style Layers

```
Osc 1: On, Interval 0, Sine, Vol 100%, Center
Osc 2: On, Interval +12, Sine, Vol 50%, Center
Osc 3: On, Interval +19, Sine, Vol 30%, Center
Osc 4: On, Interval -12, Sine, Vol 40%, Center
Attack: 50ms
Glide: 0ms
```
Creates organ-like harmonic structure.

### Wide Stereo Drone

```
Osc 1: On, Interval 0, Saw, Vol 100%, Left 80
Osc 2: On, Interval 0, Saw, Detune +10ct, Vol 100%, Right 80
Osc 3: On, Interval -12, Square, Vol 60%, Center
Osc 4: Off
```
Detuned stereo spread with sub-octave foundation.

## Best Practices

### Pitch Detection

- Works best with clean, monophonic signals
- Detection range: 50 Hz - 2000 Hz (approximately guitar range)
- RMS threshold: Signal needs to be above minimum level for tracking

### Glide Usage

| Glide Time | Effect |
|------------|--------|
| 0 ms | Instant pitch change |
| 20-50 ms | Quick transitions |
| 50-150 ms | Smooth legato |
| 150-300 ms | Noticeable portamento |
| 300-500 ms | Slow, ambient glide |

### Interval Combinations

For **consonant** sounds:
- Unison (0), Octaves (-12, +12), Fifths (+7, -5)

For **rich/complex** sounds:
- Add thirds (+4 major, +3 minor), sevenths (+10, +11)

For **dissonant/tension**:
- Tritone (+6), minor seconds (+1, -1)

### Volume Balance

- Main oscillator (unison): 80-100%
- Sub-octave: 60-80% (strong foundation)
- Upper harmonics: 30-50% (color, not dominant)

### Pan Settings

- Sub-bass (-12 and below): Center (prevents stereo imbalance)
- Main pitch: Center or slight offset
- Upper harmonics: Spread for width

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | ~46 ms (YIN buffer / 44.1kHz) |
| YIN Buffer | 2048 samples |
| Oscillators | 4 |
| Detection Range | 50 Hz - 2000 Hz |
| Waveforms | 4 (Sine, Saw, Square, Triangle) |
| Interval Range | -24 to +24 semitones |
| Detune Range | -50 to +50 cents |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/SynthDroneEffect.java`
- **Factory ID**: `synthdrone`
- **Key Methods**:
  - `detectPitch()`: YIN pitch detection algorithm
  - `generateWaveform()`: Waveform synthesis
  - `processInternal()`: Main processing with envelope and glide

## See Also

- [PitchSynthEffect.md](PitchSynthEffect.md) - Full-featured pitch synth
- [OctaverEffect.md](OctaverEffect.md) - Octave generation
- [HarmonizerEffect.md](HarmonizerEffect.md) - Pitch-shifted harmonies
