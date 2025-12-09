# JFx2 - Guitar Multi-Effects Processor

A professional-grade guitar multi-effects processor with node-based signal routing, written in pure Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

## Overview

JFx2 is a real-time audio processing application designed for guitarists and audio enthusiasts. It provides a flexible, node-based signal routing system with a comprehensive suite of effects, amp simulations, and audio tools.

### Key Features

- **Node-Based Signal Routing**: Visual drag-and-drop interface for creating complex signal chains with parallel and serial routing
- **75+ Audio Effects**: Comprehensive collection including distortion, delay, reverb, modulation, pitch, dynamics, acoustic, and more
- **Plugin System**: Extend JFx2 with custom effect plugins (bold highlighting in effect tree)
- **Amp & Cabinet Simulation**: Realistic tube amp modeling with preamp, power amp, and cabinet simulation
- **Neural Amp Modeling (NAM)**: Load and use neural network amp/effect models (.nam files)
- **Impulse Response Support**: Load custom IR files for cabinet simulation
- **Real-Time Processing**: Low-latency audio processing with automatic latency compensation
- **Preset Management**: Save, load, and organize your effect chains as rigs
- **Built-in Tools**: Chromatic tuner, metronome, drum machine with pattern sequencer, audio recorder
- **MIDI Recording**: Convert your playing to MIDI with pitch detection
- **3D Spatial Audio**: HRTF-based 3D panning for immersive sound design

## Documentation

| Document | Description |
|----------|-------------|
| **[User Manual](docs/USER_MANUAL.md)** | Complete guide for using JFx2 |
| **[Technical Documentation](docs/TECHNICAL.md)** | Architecture, DSP, and development guide |
| **[Effects Reference](docs/effects/)** | Detailed documentation for each effect |

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

For detailed instructions, see the **[User Manual](docs/USER_MANUAL.md)**.

---

## Effects Library (75+ Effects)

### Input Sources

| Effect | Description |
|--------|-------------|
| [**Audio Input**](docs/effects/AudioInputEffect.md) | Capture audio from hardware input device (mic, audio interface) |
| [**WAV File Input**](docs/effects/WavFileInputEffect.md) | Play audio files (.wav) as input source |
| [**Oscillator**](docs/effects/OscillatorEffect.md) | Test tone generator with sine, saw, square, triangle waveforms |
| [**Drum Machine**](docs/effects/DrumMachineEffect.md) | Pattern-based drum sequencer with multiple kits and time signatures |

### Output & Recording

| Effect | Description |
|--------|-------------|
| [**Audio Output**](docs/effects/AudioOutputEffect.md) | Send processed audio to hardware output device |
| [**WAV File Output**](docs/effects/WavFileOutputEffect.md) | Record audio to .wav file |
| [**MIDI Recorder**](docs/effects/MidiRecorderEffect.md) | Convert audio to MIDI using pitch detection (monophonic/polyphonic) |

### Gain & Dynamics

| Effect | Description |
|--------|-------------|
| [**Gain**](docs/effects/GainEffect.md) | Volume control with multiple saturation curves |
| [**Noise Gate**](docs/effects/NoiseGateEffect.md) | Remove unwanted noise with threshold and range control |
| [**Noise Suppressor**](docs/effects/NoiseSuppressorEffect.md) | Advanced noise reduction with frequency-selective gating |
| [**Compressor**](docs/effects/CompressorEffect.md) | Dynamic range compression with attack, release, ratio, threshold |
| [**Multiband Compressor**](docs/effects/MultibandCompressorEffect.md) | 4-band compressor for precise dynamic control |
| [**Limiter**](docs/effects/LimiterEffect.md) | Brick-wall peak limiting to prevent clipping |
| [**Volume Swell**](docs/effects/VolumeSwellEffect.md) | Automatic volume swells with attack time control |
| [**Sustainer**](docs/effects/SustainerEffect.md) | Increase sustain by boosting quiet signals |
| [**Auto Sustain**](docs/effects/AutoSustainEffect.md) | Envelope-based infinite sustain effect |

### Distortion & Overdrive

| Effect | Description |
|--------|-------------|
| [**Overdrive**](docs/effects/OverdriveEffect.md) | Tube-style soft clipping with tone control |
| [**Drive**](docs/effects/DriveEffect.md) | Transparent clean boost and light overdrive |
| [**Distortion**](docs/effects/DistortionEffect.md) | Hard clipping distortion with EQ shaping |
| [**Fuzz**](docs/effects/FuzzEffect.md) | Classic 60s/70s style fuzz with bias control |
| [**Tube Distortion**](docs/effects/TubeDistortionEffect.md) | Tube saturation modeling with multiple tube types |

### Amp Simulation

| Effect | Description |
|--------|-------------|
| [**Amp**](docs/effects/AmpEffect.md) | Full amplifier simulation with gain stages, tonestack, and presence |
| [**Tube Preamp**](docs/effects/TubePreampEffect.md) | 12AX7 preamp stage modeling with multiple voicings |
| [**Tube Power Amp**](docs/effects/TubePowerAmpEffect.md) | Power amp saturation with sag and compression |
| [**Cabinet Sim**](docs/effects/CabinetSimEffect.md) | Speaker cabinet modeling with multiple cab types and mic positions |
| [**Cabinet Simulator**](docs/effects/CabinetSimulatorEffect.md) | Extended cabinet simulation with room ambience |
| [**IR Loader**](docs/effects/IRLoaderEffect.md) | Load custom impulse response files (.wav) |
| [**Neural Amp**](docs/effects/NeuralAmpEffect.md) | Neural Amp Modeler - load .nam model files |
| [**NAM**](docs/effects/NAMEffect.md) | Alternate NAM loader with additional options |

### Delay Effects

| Effect | Description |
|--------|-------------|
| [**Delay**](docs/effects/DelayEffect.md) | Digital delay with sync, feedback, and modulation |
| [**Tape Echo**](docs/effects/TapeEchoEffect.md) | Vintage tape delay with wow, flutter, and tape saturation |
| [**Multi-Tap Delay**](docs/effects/MultiTapDelayEffect.md) | Up to 8 delay taps with individual timing and level |
| [**Reverse Delay**](docs/effects/ReverseDelayEffect.md) | Backwards delay for atmospheric effects |
| [**Ping Pong Delay**](docs/effects/PingPongDelayEffect.md) | Stereo bouncing delay between left and right |
| [**Quad Delay**](docs/effects/QuadDelayEffect.md) | 4 independent delay lines with cross-feedback |

### Reverb Effects

| Effect | Description |
|--------|-------------|
| [**Reverb**](docs/effects/ReverbEffect.md) | Algorithmic reverb with room size and damping |
| [**Spring Reverb**](docs/effects/SpringReverbEffect.md) | Vintage spring tank simulation with drip and splash |
| [**Shimmer Reverb**](docs/effects/ShimmerReverbEffect.md) | Pitch-shifted reverb tails for ambient textures |
| [**Plate Reverb**](docs/effects/PlateReverbEffect.md) | Classic studio plate reverb with bright, dense decay |
| [**Room Reverb**](docs/effects/RoomReverbEffect.md) | Physically-modeled room with configurable dimensions and materials |
| [**Stereo Image Reverb**](docs/effects/StereoImageReverbEffect.md) | Wide stereo reverb with Haas effect and spatial imaging |

### Modulation Effects

| Effect | Description |
|--------|-------------|
| [**Chorus**](docs/effects/ChorusEffect.md) | Classic chorus with rate, depth, and mix control |
| [**Flanger**](docs/effects/FlangerEffect.md) | Jet-like flanging with resonance and manual control |
| [**Phaser**](docs/effects/PhaserEffect.md) | Multi-stage phase shifting with sweep and resonance |
| [**Tremolo**](docs/effects/TremoloEffect.md) | Amplitude modulation with multiple waveforms |
| [**Vibrato**](docs/effects/VibratoEffect.md) | Pitch modulation with rate and depth |
| [**Panner**](docs/effects/PannerEffect.md) | Auto-panning between left and right channels |
| [**Pan 3D**](docs/effects/Pan3DEffect.md) | HRTF-based 3D spatial positioning (360° + elevation) |
| [**Ring Modulator**](docs/effects/RingModulatorEffect.md) | Ring modulation with carrier frequency control |
| [**UniVibe**](docs/effects/UniVibeEffect.md) | Classic photocell vibe/chorus effect |
| [**Rotary**](docs/effects/RotaryEffect.md) | Leslie speaker cabinet simulation with horn and drum |

### EQ & Filter

| Effect | Description |
|--------|-------------|
| [**Filter**](docs/effects/FilterEffect.md) | Multimode filter (LP, HP, BP, Notch) with resonance |
| [**Parametric EQ**](docs/effects/ParametricEQEffect.md) | 4-band fully parametric equalizer |
| [**Graphic EQ**](docs/effects/GraphicEQEffect.md) | 10-band graphic equalizer |
| [**Wah**](docs/effects/WahEffect.md) | Manual and auto-wah with multiple voicings |
| [**Envelope Filter**](docs/effects/EnvelopeFilterEffect.md) | Funky auto-wah triggered by playing dynamics |
| [**Talk Box**](docs/effects/TalkBoxEffect.md) | Vowel formant filter for voice-like tones |

### Pitch & Harmony

| Effect | Description |
|--------|-------------|
| [**Pitch Shifter**](docs/effects/PitchShifterEffect.md) | Transpose pitch up or down with formant control |
| [**Octaver**](docs/effects/OctaverEffect.md) | Analog-style octave up and down generator |
| [**Harmonizer**](docs/effects/HarmonizerEffect.md) | Intelligent harmonies based on scale and key |
| [**Auto Tuner**](docs/effects/AutoTunerEffect.md) | Gentle pitch correction with expression preservation |

### Acoustic Processing

| Effect | Description |
|--------|-------------|
| [**Acoustic Sim**](docs/effects/AcousticSimEffect.md) | Electric to acoustic guitar body simulation (10 models) |
| [**Pickup Emulator**](docs/effects/PickupEmulatorEffect.md) | Transform pickup tones (humbucker ↔ single coil ↔ P90 ↔ piezo) |
| [**Body Resonance**](docs/effects/BodyResonanceEffect.md) | Add acoustic body resonance to electric guitars |
| [**Piezo Sweetener**](docs/effects/PiezoSweetenerEffect.md) | Warm up harsh piezo pickup sound |
| [**12-String Simulator**](docs/effects/TwelveStringSimulatorEffect.md) | Simulate 12-string guitar with octave doubling |
| [**Acoustic Compressor**](docs/effects/AcousticCompressorEffect.md) | Gentle compression optimized for acoustic guitar |
| [**Acoustic EQ**](docs/effects/AcousticEQEffect.md) | 5-band EQ with acoustic presets (body, presence, air) |
| [**Anti-Feedback**](docs/effects/AntiFeedbackEffect.md) | Automatic feedback detection and notch filtering |

### Synth & Special

| Effect | Description |
|--------|-------------|
| [**Synth**](docs/effects/SynthEffect.md) | Guitar-triggered synthesizer with filter and envelope |
| [**Synth Drone**](docs/effects/SynthDroneEffect.md) | Pitch-tracking drone oscillators with intervals |
| [**Pitch Synth**](docs/effects/PitchSynthEffect.md) | Full polyphonic synth with wavetables and ADSR |

### Utility

| Effect | Description |
|--------|-------------|
| [**Splitter**](docs/effects/SplitterEffect.md) | Split signal to multiple parallel paths |
| [**Mixer**](docs/effects/MixerEffect.md) | Mix multiple signals with level control |
| [**Mono to Stereo**](docs/effects/MonoToStereoEffect.md) | Convert mono to stereo with width control |
| [**Stereo to Mono**](docs/effects/StereoToMonoEffect.md) | Intelligent stereo to mono with phase correction |
| [**Looper**](docs/effects/LooperEffect.md) | Multi-layer audio looper with overdub |

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
│   │   ├── impl/       # Individual effect classes
│   │   ├── acoustic/   # Acoustic guitar processing effects
│   │   └── plugin/     # Plugin system (ServiceLoader)
│   ├── graph/          # Signal flow graph
│   ├── nam/            # Neural Amp Modeler support
│   ├── preset/         # Rig/preset management
│   ├── tools/          # Tuner, metronome, etc.
│   └── ui/             # User interface
│       ├── canvas/     # Signal flow canvas
│       ├── controls/   # Custom UI controls
│       ├── dialogs/    # Dialog windows
│       └── panels/     # UI panels
├── plugins/            # External effect plugins (JARs)
├── presets/            # Factory presets
├── docs/               # Documentation
│   ├── USER_MANUAL.md  # User guide
│   ├── TECHNICAL.md    # Technical documentation
│   └── effects/        # Per-effect documentation
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

Copyright (c) 2025 DenzoSOFT. All rights reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or modification is strictly prohibited.

---

**JFx2** - *Your tone, your way.*
