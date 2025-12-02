# PitchSynthEffect

Converts pitch to high-quality synthesizer sounds.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Filter |
| **ID** | `pitchsynth` |
| **Display Name** | Pitch Synth |

## Description

The Pitch Synth effect detects the pitch of incoming audio and generates synthesizer sounds that follow the detected notes. This creates a "guitar synth" or "vocal synth" effect, allowing any instrument to trigger synthesizer sounds.

Key features:
- **Dual Detection Modes**: Monophonic (YIN) or Polyphonic (FFT)
- **Anti-Aliased Wavetables**: High-quality oscillators without aliasing
- **10 Instrument Presets**: From strings to bells
- **Full ADSR Envelope**: Shape the synth response
- **Resonant Filter**: Lowpass, highpass, bandpass with envelope modulation
- **Unison Voices**: Up to 7 detuned voices for richness
- **LFO Modulation**: Vibrato and tremolo

## Parameters

### Row 1: Detection & Mix

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mode` | Mode | (choice) | Monophonic | - | Pitch detection algorithm |
| `sens` | Sensitivity | 0-100 | 50 | % | Detection sensitivity threshold |
| `glide` | Glide | 0-500 | 30 | ms | Portamento between notes |
| `dry` | Dry | 0-100 | 0 | % | Original signal level |
| `wet` | Wet | 0-100 | 100 | % | Synth signal level |
| `inst` | Instrument | (choice) | Strings | - | Instrument preset |

### Row 2: Oscillator

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `wave` | Waveform | (choice) | Saw | - | Oscillator waveform |
| `octave` | Octave | -2 to +2 | 0 | - | Octave shift |
| `unison` | Unison | 1-7 | 3 | - | Number of unison voices |
| `detune` | Detune | 0-50 | 15 | ct | Unison detune amount |
| `attack` | Attack | 1-2000 | 50 | ms | Envelope attack time |
| `decay` | Decay | 1-2000 | 200 | ms | Envelope decay time |
| `sustain` | Sustain | 0-100 | 70 | % | Envelope sustain level |
| `release` | Release | 1-3000 | 300 | ms | Envelope release time |

### Row 3: Filter & Modulation

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `ftype` | Filter | (choice) | Lowpass | - | Filter type |
| `cutoff` | Cutoff | 20-20000 | 5000 | Hz | Filter cutoff frequency |
| `reso` | Resonance | 0-100 | 20 | % | Filter resonance |
| `fenv` | Flt Env | -100 to +100 | 30 | % | Filter envelope amount |
| `vibRate` | Vib Rate | 0.1-15 | 5 | Hz | Vibrato speed |
| `vibDepth` | Vib Depth | 0-100 | 20 | % | Vibrato intensity |
| `tremRate` | Trem Rate | 0.1-15 | 4 | Hz | Tremolo speed |
| `tremDepth` | Trem Depth | 0-100 | 0 | % | Tremolo intensity |

## Detection Modes

| Mode | Algorithm | Use Case |
|------|-----------|----------|
| **Monophonic** | YIN autocorrelation | Single-note lines, leads, solos |
| **Polyphonic** | FFT peak analysis | Chords, multiple notes (up to 6) |

## Waveforms

| # | Waveform | Character |
|---|----------|-----------|
| 0 | Sine | Pure, flute-like |
| 1 | Saw | Bright, buzzy, classic synth |
| 2 | Square | Hollow, clarinet-like |
| 3 | Triangle | Soft, mellow |
| 4 | Pulse (25%) | Nasal, reedy |
| 5 | SuperSaw | Fat, detuned saw stack |

## Instrument Presets

| # | Preset | Character |
|---|--------|-----------|
| 0 | Strings | Warm pad-like strings |
| 1 | Brass | Bright brass section |
| 2 | Pad | Soft ambient pad |
| 3 | Lead | Sharp synth lead |
| 4 | Bass | Deep synth bass |
| 5 | Organ | Hammond-style organ |
| 6 | Choir | Vocal-like choir |
| 7 | Bell | Metallic bell tones |
| 8 | Pluck | Plucked string sound |
| 9 | Custom | User-defined (no preset override) |

## Filter Types

| Type | Description |
|------|-------------|
| Lowpass | Removes high frequencies (most common) |
| Highpass | Removes low frequencies |
| Bandpass | Keeps only frequencies around cutoff |

## DSP Architecture

### Signal Flow

```
Audio Input ──► Pitch Detection ──► Note Tracking
                     │
                     ▼
              Frequency + Amplitude
                     │
                     ▼
              ┌──────┴──────┐
              │   Voices    │
              │  (1-8)      │
              └──────┬──────┘
                     │
                     ▼
              ┌──────┴──────┐
              │   Unison    │
              │  (1-7)      │
              └──────┬──────┘
                     │
                     ▼
             Wavetable Lookup
                     │
                     ▼
                SVF Filter
                     │
                     ▼
              ADSR Envelope
                     │
                     ▼
              Vibrato/Tremolo
                     │
                     ▼
               Soft Clip
                     │
                     ▼
              Dry/Wet Mix
                     │
                     ▼
             Stereo Output
```

### Anti-Aliased Wavetables

10 octave-specific wavetables generated at startup:
- Each octave has progressively fewer harmonics to avoid aliasing
- Interpolation between adjacent samples for smooth playback
- Wavetable size: 2048 samples

### Unison Processing

Multiple detuned oscillators summed together:
- Detune spread evenly across unison count
- Stereo panning: voices spread across stereo field
- Gain normalized: `1 / sqrt(numUnison)`

### State Variable Filter (SVF)

Two-pole filter with simultaneous LP/HP/BP outputs:
- Cutoff range: 20 Hz - 20 kHz
- Resonance up to self-oscillation (90%+)
- Envelope modulation: ±4 octaves

## Usage Tips

### Lead Synth

```
Mode: Monophonic
Waveform: Saw or SuperSaw
Unison: 3
Detune: 15 ct
Attack: 20-50 ms
Sustain: 80%
Cutoff: 3000 Hz
Resonance: 30%
Vibrato: Rate 5Hz, Depth 20%
```

### Pad Sound

```
Mode: Polyphonic
Waveform: Saw
Unison: 5-7
Detune: 20-30 ct
Attack: 200-500 ms
Decay: 300 ms
Sustain: 70%
Release: 500-1000 ms
Cutoff: 2000 Hz
Filter Env: -20%
```

### Bass Synth

```
Mode: Monophonic
Waveform: Square or Saw
Octave: -1
Unison: 1-2
Attack: 10 ms
Cutoff: 1000 Hz
Filter Env: 50%
```

### Bell/Pluck

```
Waveform: Sine or Triangle
Attack: 1 ms
Decay: 500 ms
Sustain: 0%
Release: 300 ms
Cutoff: 8000 Hz
Filter Env: 80%
```

### Smooth Glide Lead

```
Glide: 100-200 ms
Unison: 3
Vibrato Depth: 30%
```

## Best Practices

### Pitch Detection

- **Clean Input**: Works best with clear, isolated signals
- **Sensitivity**: Higher values detect quieter notes, may cause false positives
- **Monophonic vs Polyphonic**: Use monophonic for single lines (more accurate)

### Avoiding Aliasing

- Wavetables are pre-computed with band-limiting
- Higher notes automatically use fewer harmonics
- No additional anti-aliasing needed

### Signal Chain

- Place after compressor for consistent detection
- Place before reverb/delay
- Avoid heavy distortion before this effect

### Glide Settings

| Glide Time | Effect |
|------------|--------|
| 0 ms | No portamento, instant pitch change |
| 10-30 ms | Quick transitions, still articulated |
| 50-100 ms | Noticeable slide, smooth legato |
| 100-300 ms | Long glide, synth lead style |
| 300-500 ms | Very slow, ambient/effect |

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | ~46 ms (YIN buffer / 44.1kHz) |
| YIN Buffer | 2048 samples |
| FFT Size | 4096 samples |
| Max Polyphony | 6 notes |
| Voices | 8 |
| Unison per Voice | 7 |
| Wavetable Size | 2048 samples |
| Wavetable Octaves | 10 |
| Detection Range | 30 Hz - 4000 Hz |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/PitchSynthEffect.java`
- **Factory ID**: `pitchsynth`
- **Key Classes**:
  - `SynthVoice`: Individual voice with oscillator, filter, envelope
- **Key Methods**:
  - `detectPitchYIN()`: Monophonic pitch detection
  - `detectPitchFFT()`: Polyphonic pitch detection
  - `generateWavetables()`: Anti-aliased wavetable generation
  - `SynthVoice.generate()`: Voice synthesis

## See Also

- [SynthDroneEffect.md](SynthDroneEffect.md) - Simpler drone synth
- [HarmonizerEffect.md](HarmonizerEffect.md) - Pitch-shifted harmonies
- [AutoTunerEffect.md](AutoTunerEffect.md) - Pitch correction
- [OctaverEffect.md](OctaverEffect.md) - Octave generation
