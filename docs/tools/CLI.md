# JFx2 Command Line Interface

The JFx2 CLI provides an interactive command-line interface for testing and controlling the audio processing without launching the GUI.

## Usage

```bash
# Interactive mode
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli

# Direct command execution
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --test-graph
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --test-effects
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --test-full
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --test-presets
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --test-tools
java -cp target/classes it.denzosoft.jfx2.cli.JFx2Cli --generate-factory
```

## Interactive Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `1` | `devices` | List all available audio input and output devices |
| `2` | `start` | Start audio processing with current configuration |
| `3` | `stop` | Stop audio processing |
| `4` | `status` | Show current status and audio metrics |
| `5` | `gain` | Interactively adjust gain level |
| `6` | `test` | Test signal graph routing |
| `7` | `effects` | Test effects chain (distortion, delay, reverb) |
| `8` | `full` | Test full processing chain |
| `9` | `presets` | List all available presets |
| `10` | `load` | Load a preset by name |
| `11` | `save` | Save current rig as a new preset |
| `12` | `factory` | Generate factory preset templates |
| `13` | `parallel` | Test parallel signal routing |
| `h` | `help` | Show command menu |
| `q` | `quit` | Exit the CLI |

## Direct Commands

| Command | Description |
|---------|-------------|
| `--test-graph` | Run signal graph unit tests |
| `--test-effects` | Test individual effects processing |
| `--test-full` | Run complete integration test |
| `--test-presets` | Test preset loading and saving |
| `--test-tools` | Test tools (tuner, metronome, etc.) |
| `--generate-factory` | Generate factory preset files |

## Examples

### List Audio Devices
```
> devices

--- Audio Devices ---

Input Devices:
  [0] Primary Sound Capture Driver
  [1] Microphone (Realtek Audio)

Output Devices:
  [0] Primary Sound Driver
  [1] Speakers (Realtek Audio)
```

### Start Audio Processing
```
> start
Audio engine initialized
Signal graph created with default configuration
Audio processing started
```

### Check Status
```
> status

--- Status ---
Audio Engine: Running
Sample Rate: 44100 Hz
Buffer Size: 512 samples
Latency: 11.6 ms
CPU Load: 12.3%
Input Level: -18.2 dB
Output Level: -12.5 dB
```

### Load a Preset
```
> load
Available presets:
  [1] Clean
  [2] Crunch
  [3] Lead
  [4] Ambient
Enter preset number: 2
Loaded preset: Crunch
```

## Architecture

The CLI is built around three main components:

1. **AudioEngine**: Manages audio I/O and processing thread
2. **SignalGraph**: Handles effect routing and processing order
3. **TemplateManager**: Manages preset loading and saving

```
┌─────────────────────────────────────────┐
│              JFx2Cli                    │
├─────────────────────────────────────────┤
│  ┌───────────┐  ┌───────────────────┐  │
│  │ AudioEngine│  │   SignalGraph    │  │
│  └─────┬─────┘  └────────┬──────────┘  │
│        │                 │              │
│        ▼                 ▼              │
│  ┌───────────┐  ┌───────────────────┐  │
│  │  Audio    │  │  TemplateManager  │  │
│  │   I/O     │  │                   │  │
│  └───────────┘  └───────────────────┘  │
└─────────────────────────────────────────┘
```
