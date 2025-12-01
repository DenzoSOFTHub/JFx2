# Offline Processor

## Overview

Offline audio processor for rendering a rig (signal chain) to a buffer. Creates a non-real-time version of a rig by replacing audio I/O nodes with buffer-based nodes, enabling batch processing of audio files.

**Package:** `it.denzosoft.jfx2.tools`

## Features

- **Batch Processing**: Process entire audio files through any rig configuration
- **Buffer-Based I/O**: Replaces real-time audio I/O with memory buffers
- **Full Rig Support**: Works with all effects, splitters, mixers
- **Progress Reporting**: Shows processing progress in console
- **Preset Compatibility**: Loads standard .jfxrig preset files
- **Configurable**: Custom sample rate and block size

## Usage Example

### Basic Processing

```java
// Load a rig from preset file
Rig rig = RigLoader.load("my_preset.jfxrig");

// Create offline processor
OfflineProcessor processor = new OfflineProcessor(44100, 256);

// Load input audio
float[] inputAudio = loadAudioFromFile("input.wav");

// Process through the rig
float[] outputAudio = processor.process(rig, inputAudio);

// Save result
saveAudioToFile("output.wav", outputAudio);
```

### Custom Configuration

```java
// Create with custom settings
OfflineProcessor processor = new OfflineProcessor(48000, 512);

// Process
float[] output = processor.process(rig, input);
```

### Default Configuration

```java
// Uses 44100 Hz sample rate, 256 sample block size
OfflineProcessor processor = new OfflineProcessor();
```

## Public Methods

### Constructors

#### `OfflineProcessor(int sampleRate, int blockSize)`

Create offline processor with custom settings.

**Parameters:**
- `sampleRate`: Sample rate in Hz (e.g., 44100, 48000)
- `blockSize`: Processing block size in samples (e.g., 128, 256, 512)

**Typical block sizes:**
- 128: Fast processing, more overhead
- 256: Good balance (recommended)
- 512: Efficient for large files
- 1024: Maximum efficiency

#### `OfflineProcessor()`

Create with default settings (44100 Hz, 256 samples).

### Processing

#### `float[] process(Rig rig, float[] input)`

Process audio through a rig offline.

**Parameters:**
- `rig`: The rig configuration to use
- `input`: Input audio samples

**Returns:** Output samples (same length as input)

**Process:**
1. Creates modified rig with buffer I/O nodes
2. Replaces `AudioInput` → `BufferInputNode`
3. Replaces `AudioOutput` → `BufferOutputNode`
4. Processes audio in blocks
5. Returns complete output buffer

**Console output:**
```
Processing 176400 samples in blocks of 256
  Processed 1 seconds...
  Processed 2 seconds...
  Processed 3 seconds...
Offline processing complete: 176400 samples
```

## Internal Implementation

### Signal Graph Creation

The processor creates an offline version of the rig:

1. **Identify I/O nodes**: Finds `audioinput` and `audiooutput` nodes
2. **Create buffer nodes**: Instantiates `BufferInputNode` and `BufferOutputNode`
3. **Clone effect chain**: Creates all effect/splitter/mixer nodes
4. **Remap connections**: Redirects I/O connections to buffer nodes
5. **Preserve parameters**: Applies all effect parameters from rig

### Node Remapping

**Original rig:**
```
AudioInput → Distortion → Delay → AudioOutput
```

**Offline rig:**
```
BufferInputNode → Distortion → Delay → BufferOutputNode
```

### Block Processing

Processing occurs in blocks for efficiency:

```java
while (samplesRemaining > 0) {
    int blockSize = min(this.blockSize, samplesRemaining);

    // Update buffer positions
    bufferInput.setPosition(currentPosition);
    bufferOutput.setPosition(currentPosition);

    // Process block through graph
    graph.process(null, null, blockSize);

    currentPosition += blockSize;
}
```

## Buffer Nodes

### BufferInputNode

Replaces `AudioInputNode` for offline processing.

**Features:**
- Reads from memory buffer instead of audio hardware
- Supports position tracking
- Zero-fills beyond buffer end

**Key Methods:**
- `setBuffer(float[] buffer)`: Set input audio buffer
- `setPosition(int position)`: Set read position
- `process(int frameCount)`: Read samples to output port

**Implementation:**
```java
public void process(int frameCount) {
    float[] outBuffer = outputPort.getBuffer();
    for (int i = 0; i < frameCount; i++) {
        int srcIdx = position + i;
        outBuffer[i] = (srcIdx < buffer.length) ? buffer[srcIdx] : 0;
    }
}
```

### BufferOutputNode

Replaces `AudioOutputNode` for offline processing.

**Features:**
- Writes to memory buffer instead of audio hardware
- Supports position tracking
- Bounds checking

**Key Methods:**
- `setBuffer(float[] buffer)`: Set output audio buffer
- `setPosition(int position)`: Set write position
- `process(int frameCount)`: Write samples from input port

**Implementation:**
```java
public void process(int frameCount) {
    float[] inBuffer = inputPort.getBuffer();
    for (int i = 0; i < frameCount; i++) {
        int dstIdx = position + i;
        if (dstIdx < buffer.length) {
            buffer[dstIdx] = inBuffer[i];
        }
    }
}
```

## Supported Node Types

The offline processor supports all standard node types:

### Effects
All effect types from `EffectFactory`:
- Distortion, Overdrive, Fuzz
- Delay, Reverb
- Chorus, Flanger, Phaser
- EQ, Compressor, Gate
- Etc.

### Routing
- **Splitter**: Signal splitting with multiple outputs
- **Mixer**: Multi-input mixing with level/pan controls

### I/O (Remapped)
- **AudioInput** → `BufferInputNode`
- **AudioOutput** → `BufferOutputNode`

## Use Cases

### Batch File Processing

```java
OfflineProcessor processor = new OfflineProcessor();

for (String file : audioFiles) {
    float[] input = loadAudio(file);
    float[] output = processor.process(rig, input);
    saveAudio(file.replace(".wav", "_processed.wav"), output);
}
```

### A/B Testing Presets

```java
Rig rig1 = RigLoader.load("preset_a.jfxrig");
Rig rig2 = RigLoader.load("preset_b.jfxrig");

float[] input = loadAudio("guitar.wav");

float[] outputA = processor.process(rig1, input);
float[] outputB = processor.process(rig2, input);

// Compare results
```

### Render Preset Previews

```java
// Generate preview clips for all presets
for (Rig preset : allPresets) {
    float[] demo = loadAudio("demo_loop.wav");
    float[] preview = processor.process(preset, demo);
    saveAudio("preview_" + preset.getName() + ".wav", preview);
}
```

### Automated Testing

```java
@Test
public void testDistortionGain() {
    Rig rig = createTestRig();
    float[] input = generateSineWave(440, 1.0f, 44100);
    float[] output = processor.process(rig, input);

    // Verify output characteristics
    assertPeakLevel(output, expectedLevel);
    assertNoClipping(output);
}
```

### IR Convolution

```java
// Apply IR to dry guitar signal
Rig rig = new Rig();
rig.addEffect(new ConvolutionReverb("cabinet.wav"));

float[] dryGuitar = loadAudio("guitar_dry.wav");
float[] withCabinet = processor.process(rig, dryGuitar);
```

## Performance Considerations

### Memory Usage

- **Input buffer**: `inputLength × 4 bytes`
- **Output buffer**: `inputLength × 4 bytes`
- **Processing nodes**: Depends on rig complexity
- **Effect memory**: Varies (delays/reverbs use more)

**Example:** 10 seconds @ 44.1kHz = ~3.5 MB total

### Processing Speed

Factors affecting speed:
- **Block size**: Larger = more efficient (less overhead)
- **Effect count**: More effects = slower
- **Effect types**: Convolution/neural nets slower than simple effects
- **CPU**: Single-threaded processing

**Typical performance:**
- Simple rig (3-5 effects): 10-50× real-time
- Complex rig (10+ effects): 2-10× real-time
- Convolution reverb: 1-5× real-time

### Optimization Tips

1. **Use larger block sizes** for batch processing (512-1024)
2. **Bypass unused effects** in the rig
3. **Simplify routing** (fewer splitters/mixers)
4. **Process in chunks** for very long files
5. **Reuse processor instance** for multiple files

## Limitations

### Current Limitations

- **Single-threaded**: No parallel processing
- **Mono only**: Stereo rigs processed as dual-mono
- **No automation**: Parameters are static throughout processing
- **No real-time events**: MIDI/tempo sync features disabled

### Not Supported

- Parameter automation/LFO modulation during processing
- Real-time input (microphone, audio interface)
- Interactive parameter changes
- Plugin UIs (headless processing only)

## Error Handling

The processor handles errors gracefully:

```java
float[] output = processor.process(rig, input);

if (output == input) {
    // Processing failed, returned dry signal
    System.err.println("Offline processing failed");
}
```

**Common failure causes:**
- Invalid rig structure (no I/O nodes)
- Missing effect types
- Null input buffer
- Graph cycle detection

**Recovery:** Returns input buffer unchanged (dry signal passthrough)

## Integration Example

### Complete Workflow

```java
import it.denzosoft.jfx2.tools.OfflineProcessor;
import it.denzosoft.jfx2.preset.*;
import javax.sound.sampled.*;
import java.io.*;

public class BatchProcessor {
    public static void main(String[] args) throws Exception {
        // Load rig
        Rig rig = RigLoader.load(new File("my_preset.jfxrig"));

        // Create processor
        OfflineProcessor processor = new OfflineProcessor(44100, 256);

        // Load input WAV
        File inputFile = new File("guitar.wav");
        AudioInputStream ais = AudioSystem.getAudioInputStream(inputFile);

        // Read samples
        byte[] audioBytes = ais.readAllBytes();
        float[] input = bytesToFloats(audioBytes);

        // Process
        System.out.println("Processing...");
        float[] output = processor.process(rig, input);

        // Save output
        byte[] outputBytes = floatsToBytes(output);
        AudioFormat format = ais.getFormat();
        ByteArrayInputStream bais = new ByteArrayInputStream(outputBytes);
        AudioInputStream outputAis = new AudioInputStream(bais, format, output.length);
        AudioSystem.write(outputAis, AudioFileFormat.Type.WAVE, new File("output.wav"));

        System.out.println("Done!");
    }

    // Helper methods for audio conversion
    private static float[] bytesToFloats(byte[] bytes) { /* ... */ }
    private static byte[] floatsToBytes(float[] floats) { /* ... */ }
}
```

## Configuration

### Recommended Settings

| Use Case | Sample Rate | Block Size |
|----------|-------------|------------|
| Quick preview | 22050 Hz | 512 |
| Standard quality | 44100 Hz | 256 |
| High quality | 48000 Hz | 512 |
| Maximum quality | 96000 Hz | 1024 |
| Long files (>10 min) | 44100 Hz | 1024 |

### Block Size Trade-offs

- **Smaller (128-256)**:
  - Lower latency (not relevant for offline)
  - More overhead
  - Better for short files

- **Larger (512-1024)**:
  - Higher efficiency
  - Less overhead
  - Better for long files
  - May use more memory per effect

## See Also

- **IRGenerator**: Generate impulse responses for convolution
- **NeuralNetworkTrainer**: Train neural network effects
- **Rig**: Preset format documentation
- **SignalGraph**: Signal routing engine
