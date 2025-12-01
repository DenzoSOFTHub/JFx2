# JFx2 Favorites Format (favorites.json)

The favorites file tracks user preferences and usage statistics for rig presets.

## Overview

| Property | Value |
|----------|-------|
| **Filename** | `favorites.json` |
| **Format** | JSON |
| **Encoding** | UTF-8 |
| **Location** | `presets/favorites.json` |

## Purpose

The favorites system allows users to:
- Mark presets as favorites for quick access
- Rate presets (0-5 stars)
- Track usage statistics (use count, last used)
- Access recently used presets

## File Structure

```json
{
  "presets": {
    "preset_id": {
      "favorite": true,
      "rating": 5,
      "useCount": 10,
      "lastUsed": "2025-11-29T21:34:40.322133800Z"
    }
  }
}
```

## Schema

### Root Object

| Field | Type | Description |
|-------|------|-------------|
| `presets` | object | Map of preset ID to preset data |

### Preset Data

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `favorite` | boolean | false | Whether the preset is marked as favorite |
| `rating` | integer | 0 | User rating (0-5 stars) |
| `useCount` | integer | 0 | Number of times the preset has been loaded |
| `lastUsed` | string | null | ISO 8601 timestamp of last use |

## Preset ID Format

The preset ID is the relative path from the presets directory:

| Preset Location | Preset ID |
|-----------------|-----------|
| `presets/basic.jfxrig` | `basic` |
| `presets/factory/Clean.jfxrig` | `factory/Clean.jfxrig` |
| `presets/user/MyRig.jfxrig` | `user/MyRig.jfxrig` |

## Example

```json
{
  "presets": {
    "factory/Clean.jfxrig": {
      "favorite": true,
      "rating": 5,
      "useCount": 15,
      "lastUsed": "2025-11-30T14:22:10.123456Z"
    },
    "factory/Crunch.jfxrig": {
      "favorite": true,
      "rating": 4,
      "useCount": 8,
      "lastUsed": "2025-11-29T21:49:05.180279800Z"
    },
    "factory/Lead.jfxrig": {
      "favorite": false,
      "rating": 3,
      "useCount": 3,
      "lastUsed": "2025-11-28T10:15:00Z"
    },
    "basic": {
      "favorite": false,
      "rating": 0,
      "useCount": 1,
      "lastUsed": "2025-11-29T21:49:13.329465800Z"
    }
  }
}
```

## API Usage

### Check Favorite Status

```java
FavoritesManager favorites = new FavoritesManager();

// Check if favorite
boolean isFav = favorites.isFavorite("factory/Clean.jfxrig");

// Toggle favorite
boolean newStatus = favorites.toggleFavorite("factory/Clean.jfxrig");

// Set favorite
favorites.setFavorite("factory/Clean.jfxrig", true);
```

### Ratings

```java
FavoritesManager favorites = new FavoritesManager();

// Get rating (0-5)
int rating = favorites.getRating("factory/Clean.jfxrig");

// Set rating
favorites.setRating("factory/Clean.jfxrig", 5);
```

### Usage Tracking

```java
FavoritesManager favorites = new FavoritesManager();

// Mark as used (increments count, updates timestamp)
favorites.markUsed("factory/Clean.jfxrig");

// Get use count
int count = favorites.getUseCount("factory/Clean.jfxrig");

// Get recently used presets
List<String> recent = favorites.getRecentlyUsed(10);

// Get most used presets
List<String> popular = favorites.getMostUsed(10);
```

### Get All Favorites

```java
FavoritesManager favorites = new FavoritesManager();
List<String> allFavorites = favorites.getFavorites();
```

## Implementation

- **Manager**: `it.denzosoft.jfx2.preset.FavoritesManager`

### PresetData Record

```java
public record PresetData(
    boolean favorite,   // Is marked as favorite
    int rating,         // 0-5 star rating
    int useCount,       // Number of times loaded
    String lastUsed     // ISO timestamp
) {
    public static PresetData DEFAULT =
        new PresetData(false, 0, 0, null);
}
```

## Use Cases

### Preset Browser Filtering

The favorites data enables filtering in the preset browser:
- **Favorites view**: Show only presets where `favorite == true`
- **Recent view**: Sort by `lastUsed` timestamp
- **Popular view**: Sort by `useCount`
- **Rating filter**: Show presets with `rating >= N`

### Preset Sorting

Available sort options using favorites data:
1. **Name** (alphabetical)
2. **Rating** (highest first)
3. **Most Used** (by `useCount`)
4. **Recently Used** (by `lastUsed`)

## Data Persistence

- Data is saved automatically after each modification
- File is created if it doesn't exist
- Invalid entries are preserved but ignored
- Orphan entries (deleted presets) remain until cleanup

## See Also

- [RigFormat.md](RigFormat.md) - Rig file format
- [PresetFormat.md](PresetFormat.md) - Effect preset format
