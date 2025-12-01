# Metronome

## Overview

A metronome with tap tempo and time signature support. Generates audio clicks with accented downbeats for rhythm tracking.

**Package:** `it.denzosoft.jfx2.tools`

## Features

- **BPM Range**: 40-240 beats per minute
- **Time Signatures**: 4/4, 3/4, 2/4, 6/8, 5/4, 7/8
- **Tap Tempo**: Automatic BPM detection from user taps
- **Audio Clicks**: Synthesized sine wave clicks with exponential decay
- **Accented Downbeat**: Higher pitch (1500Hz) for beat 1, lower pitch (1000Hz) for other beats
- **Visual Indicators**: Beat tracking for UI integration
- **Adjustable Volume**: 0.0 to 1.0

## Usage Example

```java
// Create and prepare metronome
Metronome metronome = new Metronome();
metronome.prepare(44100);  // Sample rate in Hz

// Configure settings
metronome.setBpm(120.0f);
metronome.setTimeSignature(Metronome.TimeSignature.FOUR_FOUR);
metronome.setVolume(0.7f);

// Start the metronome
metronome.start();

// Process audio (mix clicks into output buffer)
float[] audioBuffer = new float[256];
metronome.process(audioBuffer, 256);

// Check for beat events
if (metronome.wasBeatTriggered()) {
    if (metronome.isDownbeat()) {
        System.out.println("DOWNBEAT - Beat 1");
    } else {
        System.out.println("Beat " + metronome.getCurrentBeat());
    }
}

// Tap tempo
metronome.tap();  // Call on each user tap (button press)
// BPM automatically updates after 2+ taps

// Stop
metronome.stop();
```

## Public Methods

### Initialization

#### `Metronome()`
Constructor. Creates a metronome with 120 BPM in 4/4 time.

#### `void prepare(int sampleRate)`
Prepare the metronome for audio generation.
- **sampleRate**: Sample rate in Hz

Calculates internal timing based on sample rate and current BPM.

#### `void reset()`
Reset the metronome state to beat 1 and zero sample counter.

### Control

#### `void start()`
Start the metronome. Resets to beat 1 and begins click generation.

#### `void stop()`
Stop the metronome. Stops all click generation.

#### `boolean isRunning()`
Returns true if the metronome is currently running.

### Audio Processing

#### `void process(float[] output, int frameCount)`
Generate metronome audio clicks.
- **output**: Output buffer to **mix clicks into** (adds to existing audio)
- **frameCount**: Number of frames to process

Mixes click audio into the output buffer when running. The buffer is not cleared, so you can mix metronome with other audio sources.

### Configuration

#### `void setBpm(float bpm)`
Set the BPM (clamped to 40-240).
- **bpm**: Beats per minute

Updates internal sample timing automatically.

#### `float getBpm()`
Returns the current BPM.

#### `void setTimeSignature(TimeSignature timeSignature)`
Set the time signature.
- **timeSignature**: One of the `TimeSignature` enum values

Resets current beat to 1 if it exceeds the new time signature's beat count.

#### `TimeSignature getTimeSignature()`
Returns the current time signature.

#### `void setVolume(float volume)`
Set the volume (clamped to 0.0-1.0).
- **volume**: Volume level (0.0 = silent, 1.0 = full volume)

#### `float getVolume()`
Returns the current volume.

### Tap Tempo

#### `void tap()`
Process a tap for tap tempo. Call this when the user taps the tempo button.

Automatically calculates BPM from the interval between taps:
- Requires 2+ taps to calculate BPM
- Maintains history of last 4 taps
- Resets if more than 2 seconds between taps
- Averages intervals for smoother BPM detection

#### `void resetTap()`
Reset tap tempo history. Clears all recorded taps.

### Status

#### `int getCurrentBeat()`
Get the current beat (1-based). Range: 1 to `timeSignature.beats`.

#### `boolean wasBeatTriggered()`
Check if a beat was triggered in the last `process()` call.

Useful for syncing visual indicators or other beat-synchronized effects.

#### `boolean isDownbeat()`
Check if the current beat is the downbeat (beat 1).

#### `int getSamplesPerBeat()`
Get samples per beat based on current BPM and sample rate.

#### `float getMsPerBeat()`
Get milliseconds per beat (60000 / BPM).

#### `String getDisplayString()`
Get a display string showing current state.

Format: `"120.0 BPM  4/4  [1] 2  3  4 "`
- Current beat is shown in brackets

## Time Signatures

### TimeSignature Enum

| Enum Value | Beats | Note Value | Display |
|------------|-------|------------|---------|
| FOUR_FOUR | 4 | 4 | "4/4" |
| THREE_FOUR | 3 | 4 | "3/4" |
| TWO_FOUR | 2 | 4 | "2/4" |
| SIX_EIGHT | 6 | 8 | "6/8" |
| FIVE_FOUR | 5 | 4 | "5/4" |
| SEVEN_EIGHT | 7 | 8 | "7/8" |

#### Accessing Time Signature Properties

```java
TimeSignature ts = metronome.getTimeSignature();
int beats = ts.beats;           // Number of beats per measure
int noteValue = ts.noteValue;   // Note value (4 = quarter note, 8 = eighth)
String display = ts.display;    // Display string (e.g., "4/4")
```

## Implementation Details

### Click Sound Generation

The metronome generates clicks using synthesized sine waves:

1. **Frequency Selection**:
   - Downbeat (beat 1): 1500 Hz (high)
   - Other beats: 1000 Hz (low)

2. **Envelope**: Exponential decay
   - Duration: 10ms
   - Envelope formula: `(remaining / total)²`

3. **Synthesis**: `sample = sin(phase) × envelope × 0.5 × volume`

### Beat Timing

Timing is sample-accurate:
- `samplesPerBeat = sampleRate × 60 / BPM`
- Beat triggers when `sampleCounter % samplesPerBeat == 0`
- No drift or timing errors accumulate

### Tap Tempo Algorithm

1. Record timestamp on each tap
2. If > 2 seconds since last tap, reset history
3. Calculate average interval between all taps in history (up to 4 taps)
4. Convert interval to BPM: `BPM = 60000 / avgIntervalMs`
5. Clamp to valid range (40-240)

Example:
- Tap 1: 0ms
- Tap 2: 500ms → BPM = 120
- Tap 3: 1000ms → BPM = 120 (average of 500ms intervals)
- Tap 4: 1500ms → BPM = 120 (stable)

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| BPM_MIN | 40 | Minimum BPM |
| BPM_MAX | 240 | Maximum BPM |
| CLICK_FREQUENCY_HIGH | 1500 Hz | Accented beat frequency |
| CLICK_FREQUENCY_LOW | 1000 Hz | Normal beat frequency |
| CLICK_DURATION_MS | 10 ms | Click sound duration |
| CLICK_VOLUME | 0.5 | Internal click volume multiplier |
| TAP_HISTORY_SIZE | 4 | Number of taps to remember |
| TAP_TIMEOUT_MS | 2000 | Timeout between taps (resets history) |
| DEFAULT_BPM | 120 | Default tempo |
| DEFAULT_VOLUME | 0.7 | Default volume |

## Common Use Cases

### Practice Tool

```java
metronome.setBpm(80.0f);  // Slow practice tempo
metronome.setTimeSignature(TimeSignature.FOUR_FOUR);
metronome.start();
```

### Tap Tempo from User Input

```java
// In UI button handler
button.setOnAction(e -> {
    metronome.tap();
    System.out.println("New BPM: " + metronome.getBpm());
});
```

### Visual Beat Indicator

```java
if (metronome.wasBeatTriggered()) {
    int beat = metronome.getCurrentBeat();
    highlightBeat(beat);

    if (metronome.isDownbeat()) {
        flashDownbeatIndicator();
    }
}
```

### Recording with Click Track

```java
// Mix metronome into recording
float[] recordBuffer = new float[256];
recordGuitar(recordBuffer, 256);
metronome.process(recordBuffer, 256);  // Adds clicks to recording
writeToFile(recordBuffer);
```

## Performance Considerations

- **CPU Usage**: Minimal (simple sine wave generation only when clicks are active)
- **Memory**: < 1KB per instance
- **Latency**: Sample-accurate (no latency)
- **Timing Accuracy**: Perfect (sample counter-based, no drift)
