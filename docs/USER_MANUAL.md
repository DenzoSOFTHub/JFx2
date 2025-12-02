# JFx2 User Manual

Welcome to JFx2, a professional guitar multi-effects processor with node-based signal routing.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Interface Overview](#interface-overview)
3. [Working with Effects](#working-with-effects)
4. [Signal Routing](#signal-routing)
5. [Presets and Rigs](#presets-and-rigs)
6. [Built-in Tools](#built-in-tools)
7. [Effects Reference](#effects-reference)
8. [Settings](#settings)
9. [Tips and Tricks](#tips-and-tricks)
10. [Troubleshooting](#troubleshooting)

---

## Getting Started

### System Requirements

- **Java**: JDK 21 or higher
- **Operating System**: Windows 10/11, Linux, or macOS
- **RAM**: 4 GB minimum, 8 GB recommended
- **Audio Interface**: Any device supported by Java Sound API

### Installation

#### From Source

```bash
git clone https://github.com/DenzoSOFTHub/JFx2.git
cd JFx2
mvn clean package
mvn exec:java
```

#### From JAR

```bash
java -jar JFx2-2.0.jar
```

### First Launch

When you first launch JFx2, you'll see:
- A default signal chain (Audio Input → Audio Output)
- The Effects Palette on the left
- The Signal Flow Canvas in the center
- The Parameter Panel below

### Quick Start

1. **Connect your guitar** to your audio interface
2. **Click the Play button** (or press F5) to start audio
3. **Add effects** by dragging from the palette to the canvas
4. **Connect effects** by clicking and dragging between ports
5. **Adjust parameters** by clicking on an effect block

---

## Interface Overview

### Main Window Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ [Menu Bar]  File  Edit  View  Tools  Help                       │
├──────────────┬──────────────────────────────────────────────────┤
│              │                                                  │
│  [Effect     │           [Signal Flow Canvas]                   │
│   Palette]   │                                                  │
│              │    ┌──────┐    ┌──────┐    ┌──────┐             │
│  ▼ Distortion│    │Input │───▶│Effect│───▶│Output│             │
│    Overdrive │    └──────┘    └──────┘    └──────┘             │
│    Distortion│                                                  │
│    Fuzz      │                                                  │
│              │                                                  │
│  ▼ Modulation│                                                  │
│    Chorus    │                                                  │
│    Flanger   │                                                  │
│              │                                                  │
├──────────────┴──────────────────────────────────────────────────┤
│ [Parameter Panel]  Current Effect: Overdrive                    │
│   Drive: ████████░░ 75%    Tone: ██████░░░░ 60%                 │
├─────────────────────────────────────────────────────────────────┤
│ [Status Bar]  ♪ E2  │  Latency: 12.5ms  │  CPU: 15%  │  ▶ Play │
└─────────────────────────────────────────────────────────────────┘
```

### Effect Palette

The left sidebar contains all available effects organized by category:

- **Input Sources** - Audio Input, WAV Player, Oscillator, Drum Machine
- **Output** - Audio Output, WAV Recorder, MIDI Recorder
- **Gain & Dynamics** - Gain, Compressor, Noise Gate, Limiter, etc.
- **Distortion** - Overdrive, Distortion, Fuzz, Tube Distortion
- **Amp Simulation** - Amp, Preamp, Power Amp, Cabinet, NAM
- **Delay** - Delay, Tape Echo, Ping Pong, Multi-Tap
- **Reverb** - Reverb, Spring, Plate, Room, Shimmer
- **Modulation** - Chorus, Flanger, Phaser, Tremolo, Rotary
- **Filter & EQ** - Filter, Wah, Parametric EQ, Graphic EQ
- **Pitch** - Pitch Shifter, Octaver, Harmonizer, Auto Tuner
- **Utility** - Splitter, Mixer, Mono/Stereo converters

### Signal Flow Canvas

The central area where you build your signal chain:

- **Effect Blocks** - Visual representation of effects
- **Ports** - Connection points (inputs on left, outputs on right)
- **Wires** - Signal connections between effects
- **Grid** - Optional snap-to-grid for alignment

### Parameter Panel

When you select an effect, its parameters appear here:

- **Knobs** - Click and drag to adjust values
- **Sliders** - Horizontal controls for ranges
- **Dropdowns** - Select from preset options
- **Toggles** - On/off switches

### Status Bar

The bottom bar shows:

- **Tuner Display** - Current note and cents deviation
- **Latency** - Total signal chain delay (I/O + effects)
- **CPU Meter** - Audio processing load
- **Transport** - Play/Stop/Record controls

---

## Working with Effects

### Adding Effects

**Method 1: Drag and Drop**
1. Click on an effect in the palette
2. Drag it onto the canvas
3. Release to place

**Method 2: Double-Click**
1. Select position on canvas
2. Double-click effect in palette
3. Effect appears at selection

**Method 3: Right-Click Menu**
1. Right-click on canvas
2. Select "Add Effect"
3. Choose from submenu

### Selecting Effects

- **Single Click** - Select one effect
- **Ctrl+Click** - Add to selection
- **Click+Drag** - Box selection
- **Ctrl+A** - Select all

### Moving Effects

- Click and drag to reposition
- Hold Shift for horizontal/vertical constraint
- Enable grid snap in View menu

### Deleting Effects

- Select and press Delete
- Right-click → Delete
- Edit menu → Delete

### Bypassing Effects

Toggle bypass to temporarily disable an effect:

- **Space** - Toggle bypass on selected
- Click bypass button on effect block
- Right-click → Bypass

Bypassed effects show grayed-out appearance.

### Adjusting Parameters

1. **Select an effect** to show its parameters
2. **Adjust values** using:
   - Click and drag on knobs
   - Double-click to enter exact value
   - Mouse wheel for fine adjustment
   - Right-click for reset to default

### Parameter Tips

- **Shift+Drag** - Fine adjustment mode
- **Ctrl+Click** - Reset to default
- **Right-Click** - Context menu with options

---

## Signal Routing

### Understanding Ports

Each effect has input and output ports:

| Port Type | Color | Description |
|-----------|-------|-------------|
| Audio Mono | Blue | Single channel audio |
| Audio Stereo | Green | Left + Right channels |
| Control | Yellow | Modulation signals |

### Making Connections

1. **Click** on an output port (right side of block)
2. **Drag** to an input port (left side of another block)
3. **Release** to complete connection

The wire will curve automatically for clarity.

### Parallel Routing

Create parallel signal paths using Splitter and Mixer:

```
         ┌──▶ [Effect A] ──┐
[Splitter]                  [Mixer] ──▶
         └──▶ [Effect B] ──┘
```

1. Add a Splitter effect
2. Connect to multiple parallel paths
3. Reconnect paths to a Mixer

### Series Routing

Default routing is series (chain):

```
[Input] ──▶ [Effect 1] ──▶ [Effect 2] ──▶ [Output]
```

### Complex Routing

Combine parallel and series:

```
[Input] ──▶ [Compressor] ──▶ [Splitter] ──┬──▶ [Distortion] ──┬──▶ [Mixer] ──▶ [Delay] ──▶ [Output]
                                          └──▶ [Clean Amp] ───┘
```

### Mono/Stereo Conversion

- **Mono to Stereo** - Converts mono signal to stereo with width control
- **Stereo to Mono** - Intelligent stereo sum with phase correction

Place these effects where conversion is needed.

### Valid Connections

- Audio mono → Audio mono ✓
- Audio mono → Audio stereo (auto-converted) ✓
- Audio stereo → Audio stereo ✓
- Audio stereo → Audio mono (use Stereo→Mono) ✓

---

## Presets and Rigs

### Understanding Rigs

A **Rig** is your complete setup:
- All effect blocks and their positions
- All connections between effects
- All parameter values
- Metadata (name, author, description)

### Saving a Rig

1. **File → Save Rig** (Ctrl+S)
2. Choose location and filename
3. Add name and description (optional)
4. Click Save

Rigs are saved as `.jfxrig` files.

### Loading a Rig

1. **File → Open Rig** (Ctrl+O)
2. Navigate to rig file
3. Click Open

### Creating a New Rig

1. **File → New Rig** (Ctrl+N)
2. Confirm to discard current rig
3. Start with empty canvas

### Effect Presets

Individual effects can have their own presets:

**Saving an Effect Preset:**
1. Select the effect
2. Right-click → Save Preset
3. Enter preset name

**Loading an Effect Preset:**
1. Select the effect
2. Right-click → Load Preset
3. Choose from list

### Factory Presets

JFx2 includes factory presets for common setups:
- Clean Tones
- Crunch/Overdrive
- High Gain
- Effects-Heavy
- Acoustic Simulation

Access via **File → Templates** or the Template Browser.

---

## Built-in Tools

### Tuner

The chromatic tuner is always visible in the status bar.

**Features:**
- Note detection with cents display
- Reference pitch adjustment (A4 = 400-480 Hz)
- Visual indicator (green when in tune)

**Opening Full Tuner:**
- Press **T**
- View → Tuner
- Click tuner area in status bar

### Metronome

Built-in click track for practice:

**Controls:**
- **Tempo** - 40-240 BPM
- **Time Signature** - 2/4, 3/4, 4/4, 6/8, etc.
- **Volume** - Click level
- **Accent** - First beat emphasis

**Quick Access:**
- Press **M** to toggle
- Add Settings block to set tempo

### Drum Machine

Pattern-based drum sequencer:

**Features:**
- Multiple drum kits
- Step sequencer (16/32/64 steps)
- Pattern presets (Rock, Jazz, Funk, etc.)
- Variable tempo sync

**Adding to Rig:**
1. Drag Drum Machine from Input Sources
2. Connect to your signal chain
3. Click to edit patterns

### Audio Recorder

Record your playing to WAV file:

1. Add **WAV File Output** effect
2. Set filename and location
3. Arm recording (click record button)
4. Play to record
5. Stop to save file

### MIDI Recorder

Convert your playing to MIDI:

1. Add **MIDI Recorder** effect
2. Set detection mode (Mono/Poly)
3. Play - notes are detected
4. Stop - MIDI file is saved to `./midi-output/`

---

## Effects Reference

### Distortion Effects

#### Overdrive
Tube-style soft clipping for warm breakup.
- **Drive** - Amount of gain/saturation
- **Tone** - High frequency content
- **Level** - Output volume

#### Distortion
Hard clipping for aggressive tones.
- **Gain** - Distortion intensity
- **Bass/Mid/Treble** - EQ shaping
- **Level** - Output volume

#### Fuzz
Classic 60s/70s fuzzy tones.
- **Fuzz** - Intensity
- **Tone** - Brightness
- **Bias** - Transistor bias (affects character)

### Amp Simulation

#### Amp
Full amplifier simulation:
- **Gain** - Preamp gain
- **Bass/Mid/Treble** - Tonestack
- **Presence** - High frequency emphasis
- **Master** - Power amp drive

#### Cabinet Sim
Speaker cabinet modeling:
- **Cabinet Type** - 1x12, 2x12, 4x12, etc.
- **Speaker** - Various speaker models
- **Mic Position** - Center to edge
- **Room** - Ambient microphone blend

#### NAM (Neural Amp Modeler)
Load trained neural network models:
1. Click "Load Model"
2. Select .nam file
3. Adjust Input/Output levels

### Delay Effects

#### Delay
Digital delay with modulation:
- **Time** - Delay time (ms or tempo sync)
- **Feedback** - Repeats amount
- **Mix** - Wet/dry balance
- **Modulation** - Pitch wobble

#### Tape Echo
Vintage tape delay:
- **Time** - Echo time
- **Feedback** - Repeats
- **Wow/Flutter** - Tape irregularities
- **Saturation** - Tape warmth

### Reverb Effects

#### Plate Reverb
Classic studio reverb:
- **Decay** - Tail length
- **Pre-Delay** - Initial gap
- **Damping** - High frequency absorption
- **Mix** - Wet/dry

#### Room Reverb
Physically modeled space:
- **Room Size** - Dimensions
- **Wall Material** - Absorption characteristics
- **Early/Late** - Reflection balance

### Modulation Effects

#### Chorus
Classic stereo widening:
- **Rate** - Modulation speed
- **Depth** - Modulation amount
- **Mix** - Effect level

#### Phaser
Sweeping phase notches:
- **Rate** - Sweep speed
- **Depth** - Sweep range
- **Stages** - Number of phase stages (more = deeper)
- **Resonance** - Feedback emphasis

#### Tremolo
Volume modulation:
- **Rate** - Speed
- **Depth** - Intensity
- **Shape** - Waveform (sine, triangle, square)

### Pitch Effects

#### Harmonizer
Intelligent harmonies:
- **Key** - Musical key
- **Scale** - Major, Minor, etc.
- **Interval** - 3rd, 5th, Octave, etc.
- **Mix** - Harmony level

#### Octaver
Analog-style octaves:
- **Octave Down** - Sub-octave level
- **Octave Up** - Upper octave level
- **Direct** - Dry signal level

---

## Settings

### Audio Settings

**Access:** Settings → Audio

| Setting | Description | Recommended |
|---------|-------------|-------------|
| Sample Rate | Audio quality | 44100 Hz |
| Buffer Size | Latency vs stability | 256-512 |
| Input Mode | Mono or Stereo | Mono (guitar) |

**Reset Input Devices** - Click to reset audio connections if devices become unresponsive.

### Tuner Settings

**Access:** Settings → Tuner

- **Reference Frequency** - A4 pitch (default 440 Hz)
- Guitar frequencies update automatically

### Soundfont Settings

**Access:** Settings → Soundfont

For Drum Machine MIDI sounds:
- Default Java synthesizer
- External .sf2/.dls soundfont file

---

## Tips and Tricks

### Signal Chain Order

Classic guitar signal chain:
```
Guitar → Tuner → Wah → Compressor → Overdrive → Distortion →
Amp → EQ → Modulation → Delay → Reverb → Output
```

**General Guidelines:**
- Dynamics (compressor) early in chain
- Distortion before modulation
- Time effects (delay/reverb) last
- EQ can go anywhere for different results

### Gain Staging

Maintain healthy signal levels:
1. Input gain should show green (not red)
2. Each effect should not add excessive gain
3. Watch output level before speakers

### Reducing Latency

For live playing:
- Use smaller buffer size (256 or less)
- Minimize effects with inherent latency
- Check latency display in status bar

### CPU Optimization

If CPU usage is high:
- Reduce reverb quality/decay
- Use fewer parallel paths
- Bypass unused effects
- Increase buffer size

### Creative Tips

**Parallel Distortion:**
Blend clean signal with distorted for clarity.

**Reverb Before Distortion:**
Creates "shoegaze" washy textures.

**Auto-Wah + Delay:**
Funky rhythmic patterns.

**Shimmer Reverb:**
Pitch-shifted trails for ambient soundscapes.

---

## Troubleshooting

### No Sound

1. **Check audio device** - Settings → Audio
2. **Verify connections** - All blocks connected?
3. **Check bypass** - Any effects bypassed?
4. **Input gain** - Is guitar signal present?
5. **Output volume** - Check master and system volume

### Audio Crackling/Pops

1. **Increase buffer size** - Settings → Audio
2. **Close other applications** - Free up CPU
3. **Check USB connection** - If using USB interface
4. **Update audio drivers**

### High Latency

1. **Reduce buffer size** - 256 or lower
2. **Remove high-latency effects** - Pitch shifters, large reverbs
3. **Check total latency** in status bar

### Effect Not Working

1. **Check bypass** - Toggle bypass off
2. **Verify connections** - Ports connected?
3. **Check parameters** - Mix at 0%? Muted?
4. **Reset effect** - Right-click → Reset

### Device Not Found

1. **Settings → Audio → Reset Input Devices**
2. Disconnect and reconnect device
3. Check device is working in OS settings
4. Restart JFx2

### Rig Won't Load

1. Check file is valid .jfxrig
2. Try loading in new instance
3. Check for missing effects (custom/removed)

### Application Crash

1. Check Java version (21+ required)
2. Increase Java heap: `java -Xmx2g -jar JFx2.jar`
3. Check console for error messages
4. Report bug with crash log

---

## Keyboard Shortcuts

| Category | Shortcut | Action |
|----------|----------|--------|
| **File** | Ctrl+N | New Rig |
| | Ctrl+O | Open Rig |
| | Ctrl+S | Save Rig |
| | Ctrl+Shift+S | Save Rig As |
| **Edit** | Ctrl+Z | Undo |
| | Ctrl+Y | Redo |
| | Ctrl+X | Cut |
| | Ctrl+C | Copy |
| | Ctrl+V | Paste |
| | Delete | Delete Selected |
| | Ctrl+A | Select All |
| **View** | + | Zoom In |
| | - | Zoom Out |
| | 0 | Reset Zoom |
| | F | Fit All to View |
| **Effects** | Space | Toggle Bypass |
| | B | Bypass Selected |
| **Tools** | T | Open Tuner |
| | M | Toggle Metronome |
| **Transport** | F5 | Play/Stop |

---

## Getting Help

- **Documentation:** `docs/` folder
- **Effect Details:** `docs/effects/` folder
- **Issues:** GitHub Issues page
- **Updates:** Check Releases page

---

*JFx2 User Manual v2.0*
*Copyright (c) 2024 DenzoSOFT*
