# DrumMachine

## Overview

DrumMachine is an input source effect that generates programmable drum patterns at configurable tempo. It provides multiple rhythm styles with features like count-in, accent control, swing, and the ability to load custom pattern libraries.

**Category:** Input Source
**ID:** `drummachine`
**Display Name:** Drum Machine

## Description

This effect generates stereo drum patterns for practice, songwriting, or live performance. It includes a comprehensive pattern library covering various musical styles, synthesized drum sounds with multiple sound sources, and professional features like count-in and swing timing. The drum machine can be synchronized with other time-based effects through BPM parameter matching.

## Parameters

### Play
- **ID:** `play`
- **Type:** Boolean
- **Default:** On (true)
- **Description:** Start or stop playback. When enabled, the drum pattern plays continuously.

### BPM
- **ID:** `bpm`
- **Range:** 40 to 240
- **Default:** 120
- **Unit:** bpm
- **Description:** Tempo in beats per minute. Controls the speed of the drum pattern.

### Style
- **ID:** `style`
- **Type:** Choice
- **Options:** Dynamically populated from pattern library (e.g., Rock Basic, Funk Groove, Jazz Swing, etc.)
- **Default:** First pattern in library
- **Description:** Drum pattern style. Select from various pre-programmed rhythms or patterns loaded from external libraries.

### Volume
- **ID:** `volume`
- **Range:** -40 dB to +6 dB
- **Default:** 0 dB
- **Unit:** dB
- **Description:** Output volume of the drum machine. Adjust to balance with other sources.

### Count-In
- **ID:** `countin`
- **Type:** Choice
- **Options:**
  - `Off` - No count-in
  - `1 Bar` - One bar count-in
  - `2 Bars` - Two bars count-in (Default)
- **Default:** 2 Bars
- **Description:** Number of count-in bars before the main pattern starts. Uses stick click sounds with accented downbeats.

### Accent
- **ID:** `accent`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Accent strength on downbeats. Higher values make the first beat of each bar more pronounced.

### Swing
- **ID:** `swing`
- **Range:** 0% to 100%
- **Default:** 0%
- **Unit:** %
- **Description:** Swing/shuffle amount. Shifts off-beat notes later to create a more human, groovy feel. 0% = straight timing.

### Sound Source
- **ID:** `soundsource`
- **Type:** Choice
- **Options:** Synthesized, MIDI, SF2 (SoundFont)
- **Default:** Synthesized
- **Description:** Drum sound generation method. Synthesized uses built-in synthesis, MIDI uses system MIDI sounds, SF2 uses SoundFont files.

## Implementation Details

### Pattern Structure

Patterns are organized hierarchically:

```
DrumPattern:
  - name: String
  - category: String (Rock, Jazz, Latin, etc.)
  - beatsPerBar: int (typically 4)
  - stepsPerBeat: int (typically 4 = 16th notes)
  - numBars: int
  - hits: List<DrumHit>

DrumHit:
  - bar: int
  - beat: int
  - step: int (0-3 for 16th notes within beat)
  - sound: DrumSound (KICK, SNARE, HIHAT, etc.)
  - velocity: float (0.0-1.0)
  - pan: float (-1 to +1)
```

### Sequencer Engine

The sequencer uses sample-accurate timing:

```
Samples per beat = sampleRate * 60 / BPM
Samples per step = samples per beat / 4 (for 16th notes)

Per audio frame:
1. Calculate current step from sample position
2. If new step reached:
   - Trigger all hits for this step
   - Apply accent to downbeats
3. Mix active voices with panning
4. Advance sample position
```

### Count-In Implementation

```
Count-In Pattern:
- Duration: 1 or 2 bars (based on setting)
- Sound: Stick clicks only
- Timing: Quarter notes (beats 1, 2, 3, 4)
- Velocity: Beat 1 = 1.0 (accented), beats 2-4 = 0.7

Playback Flow:
1. Start playback
2. If count-in enabled:
   a. Play count-in pattern
   b. When complete, switch to main pattern
3. Play main pattern (looping)
```

### Voice Management

Polyphonic playback using voice pool:

```java
class DrumVoice:
  - samples: float[]  // Drum sound data
  - velocity: float   // Hit velocity
  - pan: float        // Stereo position
  - position: int     // Playback position

Per frame:
1. For each active voice:
   a. Get next sample
   b. Apply pan (constant-power)
   c. Mix to output
   d. Remove if finished
```

### Stereo Panning

Constant-power panning for natural stereo imaging:

```java
// pan: -1 (left) to +1 (right)
leftGain = cos((pan + 1) * PI / 4)
rightGain = sin((pan + 1) * PI / 4)
```

### Accent Processing

Accented steps receive velocity boost:

```java
if (pattern.isAccent(step)):
    velocity *= (1.0 + accentAmount * 0.3)
    velocity = min(1.0, velocity)
```

### Sound Sources

**Synthesized (Default):**
- Built-in drum synthesis
- Kick: Sine wave pitch envelope
- Snare: Noise + filtered sine
- Hi-hat: Filtered noise with envelope
- Toms: Sine with longer decay

**MIDI:**
- Uses Java MIDI synthesizer
- General MIDI drum mapping
- Requires system MIDI support

**SoundFont (SF2):**
- Loads .sf2 files for high-quality samples
- Professional drum kit recordings
- Configurable sound banks

### Pattern Library

Built-in patterns loaded from `DrumPatternLibrary.createDefaultLibrary()`:

| Category | Styles |
|----------|--------|
| Rock | Basic, Driving, Half-time |
| Pop | Standard, Dance, Ballad |
| Funk | Groove, Syncopated |
| Jazz | Swing, Bossa Nova |
| Latin | Samba, Reggae |
| Metal | Double-bass, Blast beat |

Custom libraries can be loaded from JSON files.

### Signal Flow

```
BPM + Style Parameters
         |
         v
   Step Sequencer
         |
         v
   Hit Triggers -----> Voice Pool
         |                  |
         v                  v
   Count-In Logic     Sound Provider
         |                  |
         +--------+---------+
                  |
                  v
            Voice Mixer (Stereo)
                  |
                  v
            Volume Control
                  |
                  v
            Output (L/R)
```

## Usage Tips

### Practice with Count-In
1. Set BPM to desired practice tempo
2. Enable 2-bar count-in
3. Select appropriate style
4. Press Play and wait for count-in
5. Begin playing after count-in completes

### Creating a Backing Track
- Combine with other effects (looper, rhythm guitar)
- Match BPM with delay effects for synchronized echoes
- Use lower volume (-6 to -12 dB) to sit behind guitar

### Style Selection Guide

| Playing Style | Recommended Patterns |
|--------------|---------------------|
| Rhythm practice | Rock Basic, Pop Standard |
| Soloing | Funk Groove, Jazz Swing |
| Acoustic | Ballad, Bossa Nova |
| High-energy | Rock Driving, Metal |

### Swing Settings
- **0%:** Straight eighth/sixteenth notes
- **30-50%:** Subtle shuffle, good for rock/pop
- **60-75%:** Strong shuffle for blues/jazz
- **100%:** Maximum swing (triplet feel)

### Monitoring Playback State
```java
DrumMachine.PlaybackInfo info = drumMachine.getPlaybackInfo();
if (info.playing()) {
    int bar = info.bar();
    int beat = info.beat();
    boolean counting = info.countingIn();
}
```

### Loading Custom Patterns
```java
// From file
drumMachine.loadLibrary(Path.of("custom_patterns.json"));

// From JSON string
drumMachine.loadLibraryFromJson(jsonContent);

// Save current library
drumMachine.saveLibrary(Path.of("my_patterns.json"));
```

## Technical Specifications

- **Tempo Range:** 40-240 BPM
- **Time Signature:** 4/4 (default, configurable per pattern)
- **Resolution:** 16th notes (4 steps per beat)
- **Output:** Stereo
- **Sound Sources:** Synthesized, MIDI, SoundFont
- **Polyphony:** Unlimited voices
- **Pattern Storage:** JSON format

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/drums/DrumMachine.java`

**Supporting Classes:**
- `DrumPattern` - Pattern data structure
- `DrumPatternLibrary` - Pattern collection and I/O
- `DrumSoundProvider` - Sound generation interface
- `DrumSounds` - Synthesized drum sounds
- `MidiDrumSounds` - MIDI-based sounds
- `SF2DrumSounds` - SoundFont-based sounds
- `DrumSoundsFactory` - Sound provider factory

**Key Methods:**
- `onPrepare(...)` - Initialize sequencer and sounds
- `processInternal(...)` - Main audio generation
- `startPlayback()` - Begin with optional count-in
- `triggerStep(...)` - Fire drum hits for a step
- `loadLibrary(Path)` - Load external pattern library

**Public API:**
- `getCurrentStyleName()` - Get active pattern name
- `getStyleNames()` - List available patterns
- `getPatternCount()` - Number of patterns in library
- `getPlaybackInfo()` - Current position info

**PlaybackInfo Record:**
- `playing` - Whether playback is active
- `bar` - Current bar number (1-based)
- `beat` - Current beat number (1-based)
- `sixteenth` - Current 16th note within beat
- `countingIn` - Whether in count-in phase

## See Also

- [LooperEffect](LooperEffect.md) - For loop-based recording
- [OscillatorEffect](OscillatorEffect.md) - For tone generation
- [DelayEffect](DelayEffect.md) - For tempo-synced delays
- [WavFileInputEffect](WavFileInputEffect.md) - For sample playback
