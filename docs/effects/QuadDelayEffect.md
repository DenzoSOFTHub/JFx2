# QuadDelayEffect

4-channel delay with independent time, feedback, level and pan per channel.

## Overview

| Property | Value |
|----------|-------|
| **Category** | Delay |
| **ID** | `quaddelay` |
| **Display Name** | Quad Delay |

## Description

The Quad Delay provides 4 independent delay channels, each with its own time, feedback, level, and stereo pan position. This enables complex rhythmic patterns, wide stereo imaging, and creative sound design.

Use cases:
- **Rhythmic delays**: Create polyrhythmic patterns with different time divisions
- **Stereo spreading**: Pan delays across the stereo field
- **Ambient textures**: Layer long delays with short slapbacks
- **Call and response**: Distinct delay voices in different positions

## Parameters

### Row 1: Global

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `dry` | Dry | 0-100 | 100 | % | Original signal level |
| `filter` | Filter | 1000-15000 | 8000 | Hz | Lowpass filter on all delay taps |
| `masterFb` | Master FB | 0-50 | 0 | % | Additional feedback to all channels |

### Row 2: Channel 1

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `ch1On` | Ch1 | On/Off | On | - | Enable channel 1 |
| `ch1Time` | Time | 0-2000 | 250 | ms | Delay time |
| `ch1Fb` | Feedback | 0-95 | 30 | % | Feedback amount |
| `ch1Lvl` | Level | 0-100 | 80 | % | Output level |
| `ch1Pan` | Pan | -100 to +100 | -60 | | Stereo position |

### Row 3: Channel 2

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `ch2On` | Ch2 | On/Off | On | - | Enable channel 2 |
| `ch2Time` | Time | 0-2000 | 375 | ms | Delay time |
| `ch2Fb` | Feedback | 0-95 | 25 | % | Feedback amount |
| `ch2Lvl` | Level | 0-100 | 70 | % | Output level |
| `ch2Pan` | Pan | -100 to +100 | +40 | | Stereo position |

### Row 4: Channel 3

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `ch3On` | Ch3 | On/Off | Off | - | Enable channel 3 |
| `ch3Time` | Time | 0-2000 | 500 | ms | Delay time |
| `ch3Fb` | Feedback | 0-95 | 20 | % | Feedback amount |
| `ch3Lvl` | Level | 0-100 | 60 | % | Output level |
| `ch3Pan` | Pan | -100 to +100 | -30 | | Stereo position |

### Row 5: Channel 4

| ID | Name | Range | Default | Unit | Description |
|----|------|-------|---------|------|-------------|
| `ch4On` | Ch4 | On/Off | Off | - | Enable channel 4 |
| `ch4Time` | Time | 0-2000 | 750 | ms | Delay time |
| `ch4Fb` | Feedback | 0-95 | 15 | % | Feedback amount |
| `ch4Lvl` | Level | 0-100 | 50 | % | Output level |
| `ch4Pan` | Pan | -100 to +100 | +70 | | Stereo position |

## Signal Flow

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    QUAD DELAY                           │
                    │                                                         │
Input ─────────────►│─────────────────────────────────────────┬──────────────│
        │           │                                         │              │
        │           │  ┌───────────────────────────────┐      │   ┌───────┐  │
        │           │  │         CHANNEL 1             │      │   │       │  │
        │           │  │  ┌───────┐  ┌────┐  ┌─────┐  │  Pan1 │   │       │  │
        ├───────────┼─►│  │Delay 1│─►│ FB │─►│Level│──┼───────┼──►│       │  │
        │           │  │  │ 250ms │  │30% │  │ 80% │  │   L   │   │       │  │
        │           │  │  └───────┘  └────┘  └─────┘  │       │   │       │  │
        │           │  └───────────────────────────────┘      │   │       │  │
        │           │                                         │   │       │  │
        │           │  ┌───────────────────────────────┐      │   │       │  │
        │           │  │         CHANNEL 2             │      │   │       │  │
        │           │  │  ┌───────┐  ┌────┐  ┌─────┐  │  Pan2 │   │ MIX   │  │
        ├───────────┼─►│  │Delay 2│─►│ FB │─►│Level│──┼───────┼──►│       │──►│ Output
        │           │  │  │ 375ms │  │25% │  │ 70% │  │   R   │   │       │  │
        │           │  │  └───────┘  └────┘  └─────┘  │       │   │       │  │
        │           │  └───────────────────────────────┘      │   │       │  │
        │           │                                         │   │       │  │
        │           │  ┌───────────────────────────────┐      │   │       │  │
        │           │  │         CHANNEL 3             │      │   │       │  │
        │           │  │  ┌───────┐  ┌────┐  ┌─────┐  │  Pan3 │   │       │  │
        ├───────────┼─►│  │Delay 3│─►│ FB │─►│Level│──┼───────┼──►│       │  │
        │           │  │  │ 500ms │  │20% │  │ 60% │  │   L   │   │       │  │
        │           │  │  └───────┘  └────┘  └─────┘  │       │   │       │  │
        │           │  └───────────────────────────────┘      │   │       │  │
        │           │                                         │   │       │  │
        │           │  ┌───────────────────────────────┐      │   │       │  │
        │           │  │         CHANNEL 4             │      │   │       │  │
        │           │  │  ┌───────┐  ┌────┐  ┌─────┐  │  Pan4 │   │       │  │
        └───────────┼─►│  │Delay 4│─►│ FB │─►│Level│──┼───────┼──►│       │  │
                    │  │  │ 750ms │  │15% │  │ 50% │  │   R   │   │       │  │
                    │  │  └───────┘  └────┘  └─────┘  │       │   └───────┘  │
                    │  └───────────────────────────────┘      │              │
                    │                                         │              │
                    └─────────────────────────────────────────┴──────────────┘
```

## Usage Tips

### Rhythmic Pattern (120 BPM)

For tempo-synced delays at 120 BPM (500ms = quarter note):

```
Ch1: 125ms  (16th note)   Pan: -80   Level: 70%
Ch2: 250ms  (8th note)    Pan: +80   Level: 60%
Ch3: 375ms  (dotted 8th)  Pan: -40   Level: 50%
Ch4: 500ms  (quarter)     Pan: +40   Level: 40%
```

### Wide Stereo Spread

```
Ch1: 80ms   Pan: -100 (hard left)    Level: 60%
Ch2: 100ms  Pan: +100 (hard right)   Level: 60%
Ch3: 160ms  Pan: -50                 Level: 40%
Ch4: 200ms  Pan: +50                 Level: 40%
```

### Ambient Wash

```
Ch1: 300ms  Pan: -70   Feedback: 60%   Level: 50%
Ch2: 450ms  Pan: +70   Feedback: 55%   Level: 45%
Ch3: 700ms  Pan: -30   Feedback: 50%   Level: 40%
Ch4: 1000ms Pan: +30   Feedback: 45%   Level: 35%
Filter: 3000 Hz (dark)
```

### Slapback Doubler

```
Ch1: 30ms   Pan: -100  Feedback: 0%   Level: 80%
Ch2: 45ms   Pan: +100  Feedback: 0%   Level: 80%
Ch3: Off
Ch4: Off
```

## Technical Specifications

| Specification | Value |
|---------------|-------|
| Channels | 4 independent |
| Max Delay Time | 2000 ms per channel |
| Max Total | 8000 ms (4 × 2000) |
| Filter | Per-channel lowpass |
| Panning | Constant power |
| Stereo Mode | True stereo |

## Tempo Reference

| BPM | 1/4 note | 1/8 note | 1/16 note | Dotted 1/8 |
|-----|----------|----------|-----------|------------|
| 60  | 1000ms   | 500ms    | 250ms     | 750ms      |
| 90  | 667ms    | 333ms    | 167ms     | 500ms      |
| 120 | 500ms    | 250ms    | 125ms     | 375ms      |
| 140 | 429ms    | 214ms    | 107ms     | 321ms      |
| 160 | 375ms    | 188ms    | 94ms      | 281ms      |

## Code Reference

- **Source**: `src/main/java/it/denzosoft/jfx2/effects/impl/QuadDelayEffect.java`
- **Factory ID**: `quaddelay`

## See Also

- [DelayEffect.md](DelayEffect.md) - Simple mono/stereo delay
- [MultiTapDelayEffect.md](MultiTapDelayEffect.md) - Multi-tap delay
- [PingPongDelayEffect.md](PingPongDelayEffect.md) - Stereo ping-pong delay
- [TapeEchoEffect.md](TapeEchoEffect.md) - Tape echo simulation
