# JFx2 - Guitar Multi-Effects Processor

A professional-grade guitar multi-effects processor with node-based signal routing, written in pure Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

## Overview

JFx2 is a real-time audio processing application designed for guitarists and audio enthusiasts. It provides a flexible, node-based signal routing system with a comprehensive suite of effects, amp simulations, and audio tools.

### Key Features

- **Node-Based Signal Routing**: Visual drag-and-drop interface for creating complex signal chains
- **50+ Audio Effects**: Distortion, delay, reverb, modulation, dynamics, and more
- **Amp & Cabinet Simulation**: Realistic tube amp modeling with various cabinet types
- **Neural Amp Modeling (NAM)**: Load and use neural network amp models
- **Real-Time Processing**: Low-latency audio processing with multi-threaded architecture
- **Preset Management**: Save, load, and organize your effect chains
- **Built-in Tools**: Tuner, metronome, drum machine, audio recorder

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

## Effects Library

### Gain & Dynamics
| Effect | Description |
|--------|-------------|
| Gain | Volume control with saturation curves |
| Compressor | Dynamic range compression |
| Limiter | Peak limiting |
| Noise Gate | Remove unwanted noise |
| Volume Swell | Automatic volume swells |

### Distortion
| Effect | Description |
|--------|-------------|
| Overdrive | Tube-style soft clipping |
| Distortion | Hard clipping distortion |
| Fuzz | Classic fuzz tones |
| Drive | Transparent gain boost |

### Amp Simulation
| Effect | Description |
|--------|-------------|
| Amp | Full amp simulation with EQ |
| Tube Preamp | 12AX7 preamp modeling |
| Tube Power Amp | Power amp saturation |
| Cabinet Simulator | Speaker cabinet modeling |
| NAM Loader | Neural Amp Modeler support |

### Modulation
| Effect | Description |
|--------|-------------|
| Chorus | Classic chorus effect |
| Flanger | Jet-like flanging |
| Phaser | Phase shifting |
| Tremolo | Amplitude modulation |
| Vibrato | Pitch modulation |
| Rotary | Leslie speaker simulation |
| UniVibe | Classic vibe effect |

### Delay & Reverb
| Effect | Description |
|--------|-------------|
| Delay | Digital delay with tap tempo |
| Ping Pong Delay | Stereo bouncing delay |
| Tape Echo | Vintage tape delay |
| Multi-Tap Delay | Complex delay patterns |
| Reverb | Algorithmic reverb |
| Spring Reverb | Vintage spring reverb |
| Shimmer Reverb | Pitch-shifted reverb |

### Filter & EQ
| Effect | Description |
|--------|-------------|
| Filter | Multimode filter |
| Parametric EQ | 4-band parametric |
| Graphic EQ | 10-band graphic EQ |
| Wah | Auto and manual wah |
| Envelope Filter | Funky auto-wah |

### Pitch & Harmony
| Effect | Description |
|--------|-------------|
| Pitch Shifter | Pitch transposition |
| Octaver | Octave up/down |
| Ring Modulator | Ring modulation |

### Utility
| Effect | Description |
|--------|-------------|
| Splitter | Split signal to multiple paths |
| Mixer | Mix multiple signals |
| Mono to Stereo | Convert mono to stereo |
| Panner | Stereo panning |
| IR Loader | Load impulse responses |

### Tools
| Effect | Description |
|--------|-------------|
| Oscillator | Test tone generator |
| Drum Machine | Pattern-based drums |
| Looper | Audio looper |
| WAV Input | Play audio files |
| WAV Output | Record to file |

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
└── scripts/            # Build scripts
```

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
