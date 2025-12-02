# MidiRecorderEffect

Records audio as MIDI notes with pitch detection.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Output Sink |
| **ID** | `midirecorder` |
| **Display Name** | MIDI Recorder |

## Description

The MIDI Recorder effect detects notes from incoming audio and records them as standard MIDI files. This allows converting guitar performances, vocal melodies, or any pitched audio into MIDI for further editing in a DAW.

Key features:
- **Dual Detection Modes**: Monophonic (YIN) or Polyphonic (FFT)
- **Note Onset/Offset Detection**: Accurate note timing
- **Velocity Sensitivity**: Captures dynamics
- **MIDI File Export**: Standard MIDI format on stop
- **Tempo Sync**: Uses tempo from Settings block
- **Auto-naming**: Files named with timestamp

## Parameters

### Row 1: Detection

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `mode` | Mode | (choice) | Monophonic (YIN) | - | Pitch detection algorithm |
| `threshold` | Threshold | -60 to 0 | -40 | dB | Minimum signal level to detect notes |

### Row 2: Note Range & Velocity

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `minNote` | Min Note | 24-96 | 40 | - | Lowest MIDI note to detect (40 = E2) |
| `maxNote` | Max Note | 48-120 | 84 | - | Highest MIDI note to detect (84 = C6) |
| `velSens` | Vel Sens | 0-100 | 70 | % | Velocity sensitivity |

### Row 3: Quantization

| ID | Name | Range | Default | Description |
|----|------|-------|---------|-------------|
| `quantize` | Quantize | (choice) | Off | Note quantization |

## Detection Modes

| Mode | Algorithm | Best For |
|------|-----------|----------|
| **Monophonic (YIN)** | Autocorrelation | Single-note lines, melodies |
| **Polyphonic (FFT)** | FFT peak analysis | Chords (up to 6 notes) |

## Note Range Reference

| MIDI Note | Note Name | Instrument |
|-----------|-----------|------------|
| 24 | C1 | Low bass |
| 40 | E2 | Guitar low E |
| 45 | A2 | Guitar A string |
| 60 | C4 | Middle C |
| 64 | E4 | Guitar high E (12th fret) |
| 84 | C6 | Two octaves above middle C |

## Quantization Options

| Option | Description |
|--------|-------------|
| Off | No quantization, record exact timing |
| 1/4 | Quarter notes |
| 1/8 | Eighth notes |
| 1/16 | Sixteenth notes |
| 1/32 | Thirty-second notes |

## Signal Flow

```
Audio Input ──► Pass Through to Output
     │
     ▼
Sample Accumulator
     │
     ▼
Pitch Detection (YIN/FFT)
     │
     ▼
Note Tracking
     │
     ├──► Note On Events ──► MIDI Track
     │
     └──► Note Off Events ──► MIDI Track

On Stop/Reset:
     │
     ▼
Save MIDI File
```

## MIDI Output

### File Location

Files are saved to `./midi-output/` directory with automatic naming:
```
./midi-output/yyyyMMddHHmmss.mid
```

Example: `./midi-output/20240115143022.mid`

### MIDI Format

| Property | Value |
|----------|-------|
| Format | Standard MIDI File Type 1 |
| Resolution | 480 PPQ (ticks per quarter note) |
| Channel | 1 |
| Tempo | From Settings block or 120 BPM default |

### Events Recorded

- **Note On**: Pitch, Velocity, Time
- **Note Off**: Pitch, Time
- **Tempo**: Meta event at tick 0

## Usage Tips

### Recording Guitar

```
Mode: Monophonic (YIN)
Min Note: 40 (E2)
Max Note: 84 (C6)
Threshold: -40 dB
Velocity Sens: 70%
```

### Recording Bass

```
Mode: Monophonic (YIN)
Min Note: 28 (E1)
Max Note: 60 (C4)
Threshold: -35 dB
Velocity Sens: 80%
```

### Recording Chords

```
Mode: Polyphonic (FFT)
Threshold: -45 dB
Velocity Sens: 60%
```
Note: Polyphonic mode may have more false detections.

### Recording Vocals

```
Mode: Monophonic (YIN)
Min Note: 48 (C3) - male
Min Note: 60 (C4) - female
Max Note: 84 (C6)
Threshold: -50 dB
```

## Best Practices

### Threshold Setting

| Threshold | Use Case |
|-----------|----------|
| -60 to -50 dB | Very quiet signals, sensitive detection |
| -50 to -40 dB | Normal playing, good balance |
| -40 to -30 dB | Loud signals, reduces false positives |
| -30 to -20 dB | Very loud, only strong notes |

### Velocity Sensitivity

- **Low (0-30%)**: More uniform velocities
- **Medium (40-60%)**: Natural dynamics
- **High (70-100%)**: Exaggerated dynamics

### Signal Chain

1. Place early in chain for cleanest pitch detection
2. Use compressor before for consistent levels
3. Avoid heavy effects (distortion, modulation) before

### Improving Detection Accuracy

1. Use clean, dry signal
2. Play notes clearly with distinct attacks
3. Avoid string noise and fret buzz
4. Set threshold just above noise floor
5. Use monophonic mode for single-note lines

## Tempo Synchronization

The MIDI Recorder uses the tempo from the Settings block:

1. Add Settings block to your rig
2. Set desired BPM in Settings
3. MIDI recording will use that tempo

If no Settings block exists, defaults to 120 BPM.

## Recording Workflow

1. **Setup**: Add MIDI Recorder to signal chain
2. **Configure**: Set detection mode and note range
3. **Start**: Begin playing - recording starts automatically
4. **Stop**: Stop playback or reset effect
5. **Retrieve**: Find MIDI file in `./midi-output/`

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Latency | Pass-through (0 samples) |
| YIN Buffer | 2048 samples |
| FFT Size | 4096 samples |
| Max Polyphony | 6 notes |
| MIDI Resolution | 480 PPQ |
| Note Confirm Frames | 3 |

## File Handling

### Output Directory

```
./midi-output/
├── 20240115143022.mid
├── 20240115144510.mid
└── ...
```

Directory is created automatically if it doesn't exist.

### File Naming

Timestamp format: `yyyyMMddHHmmss`
- Year (4 digits)
- Month (2 digits)
- Day (2 digits)
- Hour (2 digits, 24h)
- Minute (2 digits)
- Second (2 digits)

## Limitations

- Detection accuracy depends on signal quality
- Very fast passages may miss notes
- Polyphonic detection limited to 6 simultaneous notes
- No velocity curve adjustment (linear mapping)
- No MIDI CC recording (notes only)

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/MidiRecorderEffect.java`
- **Factory ID**: `midirecorder`
- **Key Methods**:
  - `detectPitchYIN()`: Monophonic pitch detection
  - `detectPitchesFFT()`: Polyphonic pitch detection
  - `handleDetectedPitches()`: Note on/off tracking
  - `saveMidiFile()`: MIDI file export
- **Dependencies**:
  - `javax.sound.midi.*`: Java MIDI API
  - `SettingsEffect`: For tempo sync

## See Also

- [AutoTunerEffect.md](AutoTunerEffect.md) - Pitch correction
- [PitchSynthEffect.md](PitchSynthEffect.md) - Pitch to synth conversion
- [SettingsEffect.md](SettingsEffect.md) - Tempo and key settings
