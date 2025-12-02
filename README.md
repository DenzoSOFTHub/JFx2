# JFx2 - Guitar Multi-Effects Processor

A professional-grade guitar multi-effects processor with node-based signal routing, written in pure Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

## Overview

JFx2 is a real-time audio processing application designed for guitarists and audio enthusiasts. It provides a flexible, node-based signal routing system with a comprehensive suite of effects, amp simulations, and audio tools.

### Key Features

- **Node-Based Signal Routing**: Visual drag-and-drop interface for creating complex signal chains with parallel and serial routing
- **70+ Audio Effects**: Comprehensive collection including distortion, delay, reverb, modulation, pitch, dynamics, and more
- **Amp & Cabinet Simulation**: Realistic tube amp modeling with preamp, power amp, and cabinet simulation
- **Neural Amp Modeling (NAM)**: Load and use neural network amp/effect models (.nam files)
- **Impulse Response Support**: Load custom IR files for cabinet simulation
- **Real-Time Processing**: Low-latency audio processing with automatic latency compensation
- **Preset Management**: Save, load, and organize your effect chains as rigs
- **Built-in Tools**: Chromatic tuner, metronome, drum machine with pattern sequencer, audio recorder
- **MIDI Recording**: Convert your playing to MIDI with pitch detection
- **3D Spatial Audio**: HRTF-based 3D panning for immersive sound design

## Screenshots

```
┌─────────────────────────────────────────────────────────────────┐
│  JFx2 - Guitar Multi-Effects Processor                    [_][X]│
├─────────────────────────────────────────────────────────────────┤
│ [Effects Palette]  │        Signal Flow Canvas                  │
│                    │                                            │
│ ▼ Distortion       │   ┌──────┐    ┌──────┐    ┌──────┐        │
│   • Overdrive      │   │Audio │───▶│ Dist │───▶│Delay │───┐    │
│   • Distortion     │   │Input │    │      │    │      │   │    │
│   • Fuzz           │   └──────┘    └──────┘    └──────┘   │    │
│                    │                                       │    │
│ ▼ Modulation       │   ┌──────┐    ┌──────┐               │    │
│   • Chorus         │   │Reverb│◀───│ Amp  │◀──────────────┘    │
│   • Flanger        │   │      │    │      │                     │
│   • Phaser         │   └──┬───┘    └──────┘                     │
│                    │      │                                     │
│ ▼ Delay & Reverb   │      ▼                                     │
│   • Delay          │   ┌──────┐                                 │
│   • Reverb         │   │Audio │                                 │
│   • Spring Reverb  │   │Output│                                 │
│                    │   └──────┘                                 │
├────────────────────┴────────────────────────────────────────────┤
│ [Parameter Panel]  Distortion - Tube Screamer Style             │
│  Drive: ████████░░ 75%   Tone: ██████░░░░ 60%   Level: ████████ │
└─────────────────────────────────────────────────────────────────┘
```

## System Requirements

- **Java**: JDK 21 or higher
- **OS**: Windows 10/11, Linux, macOS
- **RAM**: 4 GB minimum, 8 GB recommended
- **Audio**: Any audio interface supported by Java Sound API

## Installation

### From Source

```bash
# Clone the repository
git clone https://github.com/DenzoSOFTHub/JFx2.git
cd JFx2

# Build with Maven
mvn clean package

# Run the application
mvn exec:java
```

### Pre-built JAR

Download the latest release from the [Releases](https://github.com/DenzoSOFTHub/JFx2/releases) page and run:

```bash
java -jar JFx2-2.0.jar
```

## Quick Start

1. **Launch JFx2** - The application opens with a default signal chain
2. **Add Effects** - Drag effects from the left palette onto the canvas
3. **Connect Nodes** - Click and drag from output ports to input ports
4. **Adjust Parameters** - Click on any effect block to show its parameters
5. **Save Your Rig** - Use File > Save Rig to save your configuration

---

## Effects Library (70+ Effects)

### Input Sources

| Effect | Description |
|--------|-------------|
| **Audio Input** | Capture audio from hardware input device (mic, audio interface) |
| **WAV File Input** | Play audio files (.wav) as input source |
| **Oscillator** | Test tone generator with sine, saw, square, triangle waveforms |
| **Drum Machine** | Pattern-based drum sequencer with multiple kits and time signatures |

### Output & Recording

| Effect | Description |
|--------|-------------|
| **Audio Output** | Send processed audio to hardware output device |
| **WAV File Output** | Record audio to .wav file |
| **MIDI Recorder** | Convert audio to MIDI using pitch detection (monophonic/polyphonic) |

### Gain & Dynamics

| Effect | Description |
|--------|-------------|
| **Gain** | Volume control with multiple saturation curves |
| **Noise Gate** | Remove unwanted noise with threshold and range control |
| **Noise Suppressor** | Advanced noise reduction with frequency-selective gating |
| **Compressor** | Dynamic range compression with attack, release, ratio, threshold |
| **Multiband Compressor** | 4-band compressor for precise dynamic control |
| **Limiter** | Brick-wall peak limiting to prevent clipping |
| **Volume Swell** | Automatic volume swells with attack time control |
| **Sustainer** | Increase sustain by boosting quiet signals |
| **Auto Sustain** | Envelope-based infinite sustain effect |

### Distortion & Overdrive

| Effect | Description |
|--------|-------------|
| **Overdrive** | Tube-style soft clipping with tone control |
| **Drive** | Transparent clean boost and light overdrive |
| **Distortion** | Hard clipping distortion with EQ shaping |
| **Fuzz** | Classic 60s/70s style fuzz with bias control |
| **Tube Distortion** | Tube saturation modeling with multiple tube types |

### Amp Simulation

| Effect | Description |
|--------|-------------|
| **Amp** | Full amplifier simulation with gain stages, tonestack, and presence |
| **Tube Preamp** | 12AX7 preamp stage modeling with multiple voicings |
| **Tube Power Amp** | Power amp saturation with sag and compression |
| **Cabinet Sim** | Speaker cabinet modeling with multiple cab types and mic positions |
| **Cabinet Simulator** | Extended cabinet simulation with room ambience |
| **IR Loader** | Load custom impulse response files (.wav) |
| **Neural Amp (NAM)** | Neural Amp Modeler - load .nam model files |
| **NAM** | Alternate NAM loader with additional options |

### Delay Effects

| Effect | Description |
|--------|-------------|
| **Delay** | Digital delay with sync, feedback, and modulation |
| **Tape Echo** | Vintage tape delay with wow, flutter, and tape saturation |
| **Multi-Tap Delay** | Up to 8 delay taps with individual timing and level |
| **Reverse Delay** | Backwards delay for atmospheric effects |
| **Ping Pong Delay** | Stereo bouncing delay between left and right |
| **Quad Delay** | 4 independent delay lines with cross-feedback |

### Reverb Effects

| Effect | Description |
|--------|-------------|
| **Reverb** | Algorithmic reverb with room size and damping |
| **Spring Reverb** | Vintage spring tank simulation with drip and splash |
| **Shimmer Reverb** | Pitch-shifted reverb tails for ambient textures |
| **Plate Reverb** | Classic studio plate reverb with bright, dense decay |
| **Room Reverb** | Physically-modeled room with configurable dimensions and materials |
| **Stereo Image Reverb** | Wide stereo reverb with Haas effect and spatial imaging |

### Modulation Effects

| Effect | Description |
|--------|-------------|
| **Chorus** | Classic chorus with rate, depth, and mix control |
| **Flanger** | Jet-like flanging with resonance and manual control |
| **Phaser** | Multi-stage phase shifting with sweep and resonance |
| **Tremolo** | Amplitude modulation with multiple waveforms |
| **Vibrato** | Pitch modulation with rate and depth |
| **Panner** | Auto-panning between left and right channels |
| **Pan 3D** | HRTF-based 3D spatial positioning (360° + elevation) |
| **Ring Modulator** | Ring modulation with carrier frequency control |
| **UniVibe** | Classic photocell vibe/chorus effect |
| **Rotary** | Leslie speaker cabinet simulation with horn and drum |

### EQ & Filter

| Effect | Description |
|--------|-------------|
| **Filter** | Multimode filter (LP, HP, BP, Notch) with resonance |
| **Parametric EQ** | 4-band fully parametric equalizer |
| **Graphic EQ** | 10-band graphic equalizer |
| **Wah** | Manual and auto-wah with multiple voicings |
| **Envelope Filter** | Funky auto-wah triggered by playing dynamics |
| **Talk Box** | Vowel formant filter for voice-like tones |

### Pitch & Harmony

| Effect | Description |
|--------|-------------|
| **Pitch Shifter** | Transpose pitch up or down with formant control |
| **Octaver** | Analog-style octave up and down generator |
| **Harmonizer** | Intelligent harmonies based on scale and key |
| **Auto Tuner** | Gentle pitch correction with expression preservation |

### Synth & Special

| Effect | Description |
|--------|-------------|
| **Synth** | Guitar-triggered synthesizer with filter and envelope |
| **Synth Drone** | Pitch-tracking drone oscillators with intervals |
| **Pitch Synth** | Full polyphonic synth with wavetables and ADSR |
| **Acoustic Sim** | Electric to acoustic guitar body simulation (10 models) |

### Utility

| Effect | Description |
|--------|-------------|
| **Splitter** | Split signal to multiple parallel paths |
| **Mixer** | Mix multiple signals with level control |
| **Mono to Stereo** | Convert mono to stereo with width control |
| **Stereo to Mono** | Intelligent stereo to mono with phase correction |
| **Looper** | Multi-layer audio looper with overdub |
| **Settings** | Global settings block (tempo, key, tuning reference) |

---

## Audio Features

### Latency Management
- Automatic calculation of total signal chain latency
- Per-effect latency reporting
- Real-time latency display in status bar (I/O + effects breakdown)

### Audio Device Management
- Multiple input/output device support
- Automatic device reset on stop for reliable reconnection
- Manual device reset button in Settings > Audio

### Sample Rates & Buffer Sizes
- Sample rates: 44.1kHz, 48kHz, 88.2kHz, 96kHz
- Buffer sizes: 64 to 16384 samples
- Configurable input mode (Mono/Stereo)

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+N` | New Rig |
| `Ctrl+O` | Open Rig |
| `Ctrl+S` | Save Rig |
| `Ctrl+Shift+S` | Save Rig As |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Delete` | Delete selected block |
| `Space` | Toggle bypass on selected |
| `T` | Open Tuner |
| `M` | Toggle Metronome |
| `+/-` | Zoom in/out |
| `0` | Reset zoom |
| `F` | Fit all to view |

## Configuration

Settings are stored in the user's home directory:
- Windows: `%USERPROFILE%\.jfx2\`
- Linux/macOS: `~/.jfx2/`

### Audio Settings

Access via **Settings > Audio**:
- Sample Rate: 44100 Hz (default)
- Buffer Size: 256-1024 samples
- Input/Output Device selection
- Reset Input Devices button

### Tuner Settings

Access via **Settings > Tuner**:
- Reference frequency (A4): 400-480 Hz (default 440 Hz)
- Standard guitar tuning frequencies displayed

### Soundfont Settings

Access via **Settings > Soundfont**:
- Use default Java wavetable synthesizer
- Load external SoundFont files (.sf2, .dls) for MIDI playback

## Building from Source

### Prerequisites

- JDK 21+
- Maven 3.8+

### Build Commands

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Run application
mvn exec:java

# Clean build
mvn clean
```

## Project Structure

```
JFx2/
├── src/main/java/it/denzosoft/jfx2/
│   ├── audio/          # Audio I/O backend
│   ├── dsp/            # DSP utilities (filters, delay lines)
│   ├── effects/        # Effect implementations
│   │   └── impl/       # Individual effect classes
│   ├── graph/          # Signal flow graph
│   ├── nam/            # Neural Amp Modeler support
│   ├── preset/         # Rig/preset management
│   ├── tools/          # Tuner, metronome, etc.
│   └── ui/             # User interface
│       ├── canvas/     # Signal flow canvas
│       ├── controls/   # Custom UI controls
│       ├── dialogs/    # Dialog windows
│       └── panels/     # UI panels
├── presets/            # Factory presets
├── docs/               # Documentation
│   └── effects/        # Per-effect documentation
└── scripts/            # Build scripts
```

## Documentation

Detailed documentation is available in the `docs/` directory:
- `docs/ROADMAP.md` - Development roadmap
- `docs/use-cases/` - Functional specifications
- `docs/effects/` - Individual effect documentation with parameters, DSP details, and usage tips

## Contributing

This is a proprietary project. For bug reports or feature requests, please contact the development team.

## Credits

Developed by **DenzoSOFT**

### Technologies Used
- Java Sound API for audio I/O
- Swing for user interface
- Custom DSP algorithms

### Acknowledgments
- Neural Amp Modeler project for NAM file format
- Various open-source DSP algorithms and research papers

## License

Copyright (c) 2024 DenzoSOFT. All rights reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or modification is strictly prohibited.

---

**JFx2** - *Your tone, your way.*
