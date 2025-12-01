# JFx2 Effect Preset Format (.jfxpreset)

An effect preset file stores the parameter configuration for a single effect type.

## Overview

| Property | Value |
|----------|-------|
| **Extension** | `.jfxpreset` |
| **Format** | Key-Value Properties |
| **Encoding** | UTF-8 |

## Purpose

Effect presets allow users to save and recall parameter settings for individual effects. Unlike rig files (`.jfxrig`) which store complete signal chains, preset files store settings for a single effect type that can be applied to any instance of that effect.

## File Structure

```properties
# JFx2 Effect Preset
# Effect: tubepreamp

name=Warm Vintage
effectId=tubepreamp
created=1701234567890
modified=1701234567890
description=Classic warm tube tone
author=User

# Parameters
param.gain=65.0
param.bass=55.0
param.mid=60.0
param.treble=50.0
param.presence=45.0
param.bias=50.0
```

## Sections

### Header Comments

Optional comment lines starting with `#`:

```properties
# JFx2 Effect Preset
# Effect: tubepreamp
```

### Metadata

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `name` | string | Yes | Preset display name |
| `effectId` | string | Yes | Effect type identifier |
| `created` | long | No | Creation timestamp (Unix millis) |
| `modified` | long | No | Last modification timestamp (Unix millis) |
| `description` | string | No | Preset description |
| `author` | string | No | Creator name |

### Parameters

Parameter values use the `param.` prefix:

```properties
param.{parameterId}={value}
```

Where:
- `{parameterId}` is the effect's parameter ID (e.g., `gain`, `mix`, `threshold`)
- `{value}` is a floating-point number

## Value Escaping

Special characters in string values are escaped:

| Sequence | Character |
|----------|-----------|
| `\\` | Backslash |
| `\n` | Newline |
| `\r` | Carriage return |

## Examples

### Compressor Preset

```properties
# JFx2 Effect Preset
# Effect: compressor

name=Gentle Squeeze
effectId=compressor
created=1701234567890
modified=1701234567890
description=Subtle compression for clean tones
author=Factory

# Parameters
param.threshold=-15.0
param.ratio=3.0
param.attack=20.0
param.release=150.0
param.makeup=2.0
param.knee=6.0
```

### Delay Preset

```properties
# JFx2 Effect Preset
# Effect: delay

name=Slapback
effectId=delay
created=1701234567890
modified=1701234567890
description=Classic rockabilly slapback delay
author=Factory

# Parameters
param.time=120.0
param.feedback=15.0
param.mix=35.0
param.lowCut=200.0
param.highCut=5000.0
param.sync=0.0
```

### Reverb Preset

```properties
# JFx2 Effect Preset
# Effect: reverb

name=Large Hall
effectId=reverb
created=1701234567890
modified=1701234567890

# Parameters
param.roomSize=80.0
param.damp=40.0
param.mix=30.0
param.predelay=25.0
param.width=100.0
```

## File Locations

Effect presets are stored in the user's home directory:

| Platform | Path |
|----------|------|
| Windows | `%USERPROFILE%\.jfx2\presets\{effectId}\` |
| Linux | `~/.jfx2/presets/{effectId}/` |
| macOS | `~/.jfx2/presets/{effectId}/` |

### Directory Structure

```
~/.jfx2/presets/
├── tubepreamp/
│   ├── Warm_Vintage.jfxpreset
│   ├── British_Clean.jfxpreset
│   └── High_Gain.jfxpreset
├── delay/
│   ├── Slapback.jfxpreset
│   └── Long_Ambient.jfxpreset
├── reverb/
│   ├── Room.jfxpreset
│   ├── Hall.jfxpreset
│   └── Cathedral.jfxpreset
└── compressor/
    └── Gentle_Squeeze.jfxpreset
```

## Filename Conventions

- Preset filename is derived from the preset name
- Invalid characters are replaced with underscores
- Spaces are replaced with underscores
- Characters replaced: `\ / : * ? " < > |`

Examples:
- "Warm Vintage" → `Warm_Vintage.jfxpreset`
- "80's Chorus" → `80_s_Chorus.jfxpreset`

## API Usage

### Saving a Preset

```java
PresetManager manager = PresetManager.getInstance();

// Save current effect state
Preset preset = manager.savePreset(effect, "My Preset");

// Save with metadata
Preset preset = manager.savePreset(effect, "My Preset",
    "A great preset for blues", "John Doe");
```

### Loading Presets

```java
PresetManager manager = PresetManager.getInstance();

// Get all presets for an effect type
List<Preset> presets = manager.getPresetsForEffect("tubepreamp");

// Apply preset to effect
manager.applyPreset(preset, effect);
```

### Deleting a Preset

```java
PresetManager manager = PresetManager.getInstance();
boolean deleted = manager.deletePreset(preset);
```

## Implementation

- **Model**: `it.denzosoft.jfx2.preset.Preset`
- **Manager**: `it.denzosoft.jfx2.preset.PresetManager`

### Preset Class

```java
public class Preset {
    String name;           // Preset name
    String effectId;       // Effect type ID
    Map<String, Float> parameterValues;  // Parameter settings
    String description;    // Optional description
    String author;         // Optional author
    long createdTime;      // Creation timestamp
    long modifiedTime;     // Last modified timestamp
}
```

## Differences from Rig Format

| Aspect | Preset (.jfxpreset) | Rig (.jfxrig) |
|--------|---------------------|---------------|
| Scope | Single effect | Complete signal chain |
| Format | Key-value properties | JSON |
| Contains | Parameters only | Nodes + connections |
| Location | `~/.jfx2/presets/{effectId}/` | `presets/` |
| Use Case | Recall effect settings | Recall entire setup |

## See Also

- [RigFormat.md](RigFormat.md) - Complete rig file format
- [FavoritesFormat.md](FavoritesFormat.md) - Favorites and usage tracking
