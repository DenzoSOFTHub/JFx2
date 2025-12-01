# JFx2 Rig File Format (.jfxrig)

A rig file stores the complete signal chain configuration including all effects, their parameters, and connections.

## Overview

| Property | Value |
|----------|-------|
| **Extension** | `.jfxrig` |
| **Format** | JSON |
| **Encoding** | UTF-8 |
| **Version** | 1.0 |

## File Structure

```json
{
  "metadata": { ... },
  "nodes": [ ... ],
  "connections": [ ... ]
}
```

## Sections

### Metadata

Contains information about the rig preset.

```json
"metadata": {
  "name": "Clean",
  "author": "JFx2 Factory",
  "description": "Crystal clear clean tone with subtle enhancement",
  "category": "Clean",
  "tags": "clean, jazz, country",
  "version": "1.0",
  "createdAt": "2024-01-01T00:00:00Z",
  "modifiedAt": "2024-01-01T00:00:00Z"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Display name of the rig |
| `author` | string | No | Creator name (default: "User") |
| `description` | string | No | Brief description of the rig |
| `category` | string | No | Category for organization (Clean, Crunch, Lead, Ambient, Custom) |
| `tags` | string | No | Comma-separated search tags |
| `version` | string | No | Format version (default: "1.0") |
| `createdAt` | string | No | ISO 8601 creation timestamp |
| `modifiedAt` | string | No | ISO 8601 last modification timestamp |

### Nodes

Array of effect nodes in the signal chain.

```json
"nodes": [
  {
    "id": "gate",
    "type": "noisegate",
    "name": "Noise Gate",
    "bypassed": false,
    "x": 230,
    "y": 200,
    "parameters": {
      "threshold": -55,
      "attack": 0.5,
      "release": 50
    }
  }
]
```

#### Node Definition

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique identifier for the node |
| `type` | string | Yes | Effect type ID (e.g., "noisegate", "delay", "reverb") |
| `name` | string | No | Display name (defaults to type) |
| `bypassed` | boolean | No | Whether the effect is bypassed (default: false) |
| `x` | integer | No | Canvas X position in pixels |
| `y` | integer | No | Canvas Y position in pixels |
| `parameters` | object | No | Parameter ID to value mapping |
| `config` | object | No | Extra configuration (for special nodes) |

#### Effect Types

Standard effects use their type ID:
- `audioinput` - Audio input device
- `audiooutput` - Audio output device
- `gain` - Gain/volume control
- `noisegate` - Noise gate
- `compressor` - Dynamic compressor
- `overdrive` - Overdrive distortion
- `distortion` - Hard clipping distortion
- `fuzz` - Fuzz distortion
- `amp` - Amp simulation
- `tubepreamp` - Tube preamp
- `cabinetsim` - Cabinet simulator
- `chorus` - Chorus modulation
- `flanger` - Flanger modulation
- `phaser` - Phaser modulation
- `tremolo` - Tremolo effect
- `delay` - Digital delay
- `pingpongdelay` - Stereo ping pong delay
- `tapeecho` - Tape echo
- `reverb` - Algorithmic reverb
- `springreverb` - Spring reverb
- `shimmerreverb` - Shimmer reverb
- `parametriceq` - Parametric EQ
- `graphiceq` - Graphic EQ
- `filter` - Multi-mode filter
- `wah` - Wah pedal
- `pitchshifter` - Pitch shifter
- `octaver` - Octave generator
- `oscillator` - Test tone generator
- `drummachine` - Drum machine
- `looper` - Audio looper
- `wavinput` - WAV file player
- `wavoutput` - WAV file recorder

#### Special Nodes

**Splitter** - Splits signal to multiple outputs:
```json
{
  "id": "split1",
  "type": "splitter",
  "name": "Splitter",
  "x": 300,
  "y": 200,
  "config": {
    "numOutputs": 2
  }
}
```

**Mixer** - Combines multiple inputs:
```json
{
  "id": "mix1",
  "type": "mixer",
  "name": "Mixer",
  "x": 600,
  "y": 200,
  "config": {
    "numInputs": 4,
    "stereoMode": "STEREO",
    "levels": [1.0, 1.0, 1.0, 1.0],
    "pans": [-1.0, 0.0, 0.0, 1.0],
    "masterLevel": 1.0
  }
}
```

### Connections

Array of connections between nodes.

```json
"connections": [
  {
    "sourceNode": "input",
    "sourcePort": "out",
    "targetNode": "gate",
    "targetPort": "in"
  }
]
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sourceNode` | string | Yes | ID of the source node |
| `sourcePort` | string | Yes | Output port ID (usually "out") |
| `targetNode` | string | Yes | ID of the target node |
| `targetPort` | string | Yes | Input port ID (usually "in") |

#### Port Naming

**Standard Ports:**
- `in` - Default input port
- `out` - Default output port

**Splitter Outputs:**
- `out1`, `out2`, `out3`, `out4` - Individual output ports

**Mixer Inputs:**
- `in1`, `in2`, `in3`, `in4` - Individual input ports

## Complete Example

```json
{
  "metadata": {
    "name": "Clean",
    "author": "JFx2 Factory",
    "description": "Crystal clear clean tone with subtle enhancement",
    "category": "Clean",
    "tags": "clean, jazz, country",
    "version": "1.0",
    "createdAt": "2024-01-01T00:00:00Z",
    "modifiedAt": "2024-01-01T00:00:00Z"
  },
  "nodes": [
    {
      "id": "input",
      "type": "audioinput",
      "name": "Audio In",
      "bypassed": false,
      "x": 50,
      "y": 200
    },
    {
      "id": "gate",
      "type": "noisegate",
      "name": "Noise Gate",
      "bypassed": false,
      "x": 230,
      "y": 200,
      "parameters": {
        "threshold": -55,
        "attack": 0.5,
        "release": 50
      }
    },
    {
      "id": "reverb",
      "type": "reverb",
      "name": "Reverb",
      "bypassed": false,
      "x": 410,
      "y": 200,
      "parameters": {
        "roomSize": 30,
        "damp": 60,
        "mix": 15,
        "predelay": 10
      }
    },
    {
      "id": "output",
      "type": "audiooutput",
      "name": "Audio Out",
      "bypassed": false,
      "x": 590,
      "y": 200
    }
  ],
  "connections": [
    {
      "sourceNode": "input",
      "sourcePort": "out",
      "targetNode": "gate",
      "targetPort": "in"
    },
    {
      "sourceNode": "gate",
      "sourcePort": "out",
      "targetNode": "reverb",
      "targetPort": "in"
    },
    {
      "sourceNode": "reverb",
      "sourcePort": "out",
      "targetNode": "output",
      "targetPort": "in"
    }
  ]
}
```

## Parallel Routing Example

```json
{
  "metadata": {
    "name": "Parallel Delay/Reverb",
    "category": "Custom"
  },
  "nodes": [
    {"id": "input", "type": "audioinput", "x": 50, "y": 200},
    {"id": "split", "type": "splitter", "x": 200, "y": 200, "config": {"numOutputs": 2}},
    {"id": "delay", "type": "delay", "x": 400, "y": 100, "parameters": {"time": 350, "feedback": 40, "mix": 100}},
    {"id": "reverb", "type": "reverb", "x": 400, "y": 300, "parameters": {"roomSize": 70, "mix": 100}},
    {"id": "mixer", "type": "mixer", "x": 600, "y": 200, "config": {"numInputs": 2, "levels": [0.5, 0.5]}},
    {"id": "output", "type": "audiooutput", "x": 800, "y": 200}
  ],
  "connections": [
    {"sourceNode": "input", "sourcePort": "out", "targetNode": "split", "targetPort": "in"},
    {"sourceNode": "split", "sourcePort": "out1", "targetNode": "delay", "targetPort": "in"},
    {"sourceNode": "split", "sourcePort": "out2", "targetNode": "reverb", "targetPort": "in"},
    {"sourceNode": "delay", "sourcePort": "out", "targetNode": "mixer", "targetPort": "in1"},
    {"sourceNode": "reverb", "sourcePort": "out", "targetNode": "mixer", "targetPort": "in2"},
    {"sourceNode": "mixer", "sourcePort": "out", "targetNode": "output", "targetPort": "in"}
  ]
}
```

## File Locations

| Location | Path | Description |
|----------|------|-------------|
| Factory Presets | `presets/factory/*.jfxrig` | Built-in presets |
| User Presets | `presets/*.jfxrig` | User-saved rigs |

## Implementation

- **Serializer**: `it.denzosoft.jfx2.preset.TemplateSerializer`
- **Rig Model**: `it.denzosoft.jfx2.preset.Rig`
- **Metadata**: `it.denzosoft.jfx2.preset.RigMetadata`
- **Manager**: `it.denzosoft.jfx2.preset.TemplateManager`

## See Also

- [PresetFormat.md](PresetFormat.md) - Individual effect preset format
- [FavoritesFormat.md](FavoritesFormat.md) - Favorites and usage tracking
