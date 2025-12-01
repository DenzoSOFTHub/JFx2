# Tuner

## Overview

A chromatic guitar tuner with autocorrelation-based pitch detection. Detects the fundamental frequency of the input signal and converts it to musical note notation with cents deviation.

**Package:** `it.denzosoft.jfx2.tools`

## Features

- **NSDF Algorithm**: Uses Normalized Square Difference Function for robust pitch detection
- **Configurable Reference**: Adjustable A4 reference pitch (default 440Hz, range 430-450Hz)
- **Musical Notation**: Returns note name with octave (e.g., "E2", "A4")
- **Cents Deviation**: Shows tuning accuracy from -50 to +50 cents
- **Noise Gate**: Automatic silence detection with RMS threshold
- **Frequency Smoothing**: Exponential smoothing for stable readings
- **Guitar-Optimized**: Detection range from 60Hz (B1) to 1400Hz (F6)

## Usage Example

```java
// Create and prepare tuner
Tuner tuner = new Tuner();
tuner.prepare(44100);  // Sample rate in Hz

// Optional: Set different reference pitch
tuner.setReferenceA4(442.0f);  // Concert pitch variants

// Process audio in real-time
while (running) {
    float[] audioBuffer = getAudioInput();
    tuner.process(audioBuffer, audioBuffer.length);

    // Get results
    if (tuner.isSignalPresent()) {
        String note = tuner.getNoteString();      // "E2"
        float cents = tuner.getCents();           // -12.5
        float freq = tuner.getFrequency();        // 82.1 Hz
        boolean inTune = tuner.isInTune();        // Within ±5 cents

        System.out.println(tuner.getDisplayString());  // "E2  -12 cents"
    }
}
```

## Public Methods

### Initialization

#### `Tuner()`
Constructor. Creates a tuner with A4 = 440Hz reference.

#### `void prepare(int sampleRate)`
Prepares the tuner for processing.
- **sampleRate**: Sample rate in Hz (e.g., 44100, 48000)

Allocates internal buffers (4 periods of lowest frequency, capped at 8192 samples).

#### `void reset()`
Resets the tuner state, clearing all buffers and detection results.

### Processing

#### `void process(float[] input, int frameCount)`
Process audio samples for pitch detection.
- **input**: Input audio buffer
- **frameCount**: Number of frames to process

Updates detection results if signal is present and buffer is filled.

### Configuration

#### `void setReferenceA4(float frequency)`
Set the reference pitch for A4.
- **frequency**: Reference frequency in Hz (clamped to 430-450Hz range)

Default is 440Hz. Common alternatives: 441Hz, 442Hz (orchestral), 432Hz (alternative).

#### `float getReferenceA4()`
Returns the current A4 reference pitch in Hz.

### Results

#### `float getFrequency()`
Returns the detected frequency in Hz (0 if no signal).

#### `String getNoteName()`
Returns the note name (e.g., "A", "C#", "-" if no signal).

#### `int getOctave()`
Returns the octave number (0 if no signal).

#### `String getNoteString()`
Returns the full note string with octave (e.g., "A4", "E2", "-" if no signal).

#### `float getCents()`
Returns cents deviation from the nearest note (-50 to +50).
- Negative values: pitch is flat
- Positive values: pitch is sharp

#### `boolean isSignalPresent()`
Returns true if signal level is above noise threshold (0.01 RMS).

#### `boolean isInTune()`
Returns true if signal is present and tuning is accurate (within ±5 cents).

#### `String getDisplayString()`
Returns a formatted display string for the tuner.
- Format: `"A4  +12 cents"` or `"---  ---"` if no signal

## Implementation Details

### Pitch Detection Algorithm

The tuner uses the **NSDF (Normalized Square Difference Function)** algorithm:

1. **Signal Buffering**: Maintains a circular buffer (4 periods of lowest frequency)
2. **NSDF Calculation**: For each lag τ:
   ```
   NSDF(τ) = 2 × ACF(τ) / (m(τ))
   where ACF is autocorrelation, m is normalization term
   ```
3. **Peak Detection**: Finds the first peak above 70% correlation threshold after NSDF goes negative
4. **Parabolic Interpolation**: Sub-sample accuracy using neighboring NSDF values
5. **Frequency Calculation**: `f = sampleRate / (lag + delta)`

### Frequency to Note Conversion

Uses equal temperament with 12 semitones per octave:
- Calculate semitones from A4: `semitones = 12 × log₂(freq / A4)`
- Round to nearest semitone for note name
- Cents deviation = (actual - rounded) × 100

### Noise Filtering

- **RMS Threshold**: 0.01 for signal presence detection
- **Minimum Correlation**: 50% NSDF for valid pitch
- **Exponential Smoothing**: 30% smoothing factor for frequency stability

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| MIN_FREQUENCY | 60 Hz | Lowest detectable frequency (~B1) |
| MAX_FREQUENCY | 1400 Hz | Highest detectable frequency (~F6) |
| NOISE_THRESHOLD | 0.01 | RMS threshold for signal detection |
| SMOOTHING | 0.3 | Frequency smoothing factor (0-1) |
| Buffer Size | 4 periods | Autocorrelation buffer size |
| NSDF Threshold | 0.7 | Minimum correlation for peak detection |

## Note Names

The tuner uses chromatic scale notation:
```
C, C#, D, D#, E, F, F#, G, G#, A, A#, B
```

Standard guitar tuning (E standard):
- E2: 82.41 Hz (low E string)
- A2: 110.00 Hz
- D3: 146.83 Hz
- G3: 196.00 Hz
- B3: 246.94 Hz
- E4: 329.63 Hz (high E string)

## Performance Considerations

- **Buffer Size**: ~6ms latency at 44.1kHz for low notes (larger buffer)
- **CPU Usage**: Low (simple autocorrelation, no FFT required)
- **Memory**: ~16-32KB per instance (depends on sample rate)
- **Update Rate**: Real-time (updates every `process()` call once buffer is filled)
