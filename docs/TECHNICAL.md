# JFx2 Technical Documentation

This document provides technical details about JFx2's architecture, DSP implementation, and development guidelines.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Audio Engine](#audio-engine)
3. [Signal Graph System](#signal-graph-system)
4. [Effect System](#effect-system)
5. [DSP Fundamentals](#dsp-fundamentals)
6. [Preset System](#preset-system)
7. [UI Architecture](#ui-architecture)
8. [Development Guide](#development-guide)

---

## Architecture Overview

JFx2 follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interface                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │EffectPalette │  │SignalFlowPanel│  │   ParameterPanel    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                      Signal Graph                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │InputNode │───▶│EffectNode│───▶│OutputNode│                  │
│  └──────────┘    └──────────┘    └──────────┘                  │
├─────────────────────────────────────────────────────────────────┤
│                      Audio Engine                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Processing Thread (real-time)                │  │
│  └──────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                      Java Sound API                              │
│  ┌─────────────────┐              ┌─────────────────┐          │
│  │  TargetDataLine │              │  SourceDataLine │          │
│  │     (Input)     │              │    (Output)     │          │
│  └─────────────────┘              └─────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Core Packages

| Package | Description |
|---------|-------------|
| `it.denzosoft.jfx2.audio` | Audio I/O, engine, configuration |
| `it.denzosoft.jfx2.graph` | Signal flow graph, nodes, connections |
| `it.denzosoft.jfx2.effects` | Effect base classes and implementations |
| `it.denzosoft.jfx2.dsp` | DSP utilities (filters, delay lines, etc.) |
| `it.denzosoft.jfx2.preset` | Rig/preset serialization |
| `it.denzosoft.jfx2.ui` | Swing-based user interface |
| `it.denzosoft.jfx2.nam` | Neural Amp Modeler support |

---

## Audio Engine

### AudioEngine Class

The `AudioEngine` manages the audio processing lifecycle:

```java
public class AudioEngine {
    private AudioConfig config;
    private AudioCallback callback;
    private Thread processingThread;
    private volatile boolean running;

    public void initialize(AudioConfig config);
    public void start(AudioCallback callback);
    public void stop();
    public void shutdown();
}
```

### AudioConfig

Configuration record for audio parameters:

```java
public record AudioConfig(
    int sampleRate,      // 44100, 48000, 88200, 96000
    int bufferSize,      // 64 to 16384 samples
    int inputChannels,   // 1 (mono) or 2 (stereo)
    int outputChannels   // Always 2 (stereo)
) {
    public static final AudioConfig DEFAULT =
        new AudioConfig(44100, 512, 1, 2);
}
```

### Processing Loop

The audio processing runs in a dedicated high-priority thread:

```java
private void processingLoop() {
    while (running) {
        metrics.beginProcessing();

        // 1. Read from input devices (InputNode/AudioInputEffect)
        // 2. Process signal graph in topological order
        // 3. Write to output devices (OutputNode/AudioOutputEffect)
        // Note: SourceDataLine.write() blocks, controlling timing

        callback.process(inputBuffer, outputBuffer, frameCount);

        metrics.endProcessing(frameCount);
        metrics.updateCpuLoad(bufferTimeNanos);
    }
}
```

### Latency Calculation

Total latency = I/O buffer latency + Effect chain latency

```java
// I/O latency (round-trip)
double ioLatencyMs = (bufferSize * 2.0 * 1000.0) / sampleRate + 2.0;

// Effect latency
int effectLatencySamples = signalGraph.calculateTotalLatency();
float effectLatencyMs = effectLatencySamples * 1000.0f / sampleRate;
```

---

## Signal Graph System

### Node Types

```java
public enum NodeType {
    INPUT,      // Audio sources (AudioInputEffect, Oscillator, etc.)
    EFFECT,     // Processing effects
    OUTPUT,     // Audio sinks (AudioOutputEffect, WavFileOutput, etc.)
    UTILITY     // Splitter, Mixer, etc.
}
```

### Port System

Nodes communicate through typed ports:

```java
public enum PortType {
    AUDIO_MONO(1),    // Single channel
    AUDIO_STEREO(2),  // Left + Right
    CONTROL(1),       // Control signals
    TRIGGER(1);       // Gate/trigger signals

    public int getChannelCount();
}
```

### Connection Model

```java
public class Connection {
    private final Port sourcePort;
    private final Port targetPort;

    public boolean isValid();  // Type compatibility check
    public void transfer();    // Copy buffer data
}
```

### Processing Order

The graph uses topological sorting for correct processing order:

```java
public class SignalGraph {
    private List<ProcessingNode> processingOrder;
    private boolean orderDirty = true;

    private void rebuildProcessingOrder() {
        // Kahn's algorithm for topological sort
        // Ensures nodes process in dependency order
    }

    public void process(int frameCount) {
        if (orderDirty) {
            rebuildProcessingOrder();
        }
        for (ProcessingNode node : processingOrder) {
            node.process(frameCount);
        }
    }
}
```

### EffectNode Wrapper

Effects are wrapped in EffectNode for graph integration:

```java
public class EffectNode extends AbstractNode {
    private final AudioEffect effect;

    @Override
    public void process(int frameCount) {
        // Read from input ports
        float[] inputL = getInputBuffer("inL");
        float[] inputR = getInputBuffer("inR");

        // Process effect
        effect.processStereo(inputL, inputR, outputL, outputR, frameCount);

        // Output buffers available on output ports
    }

    @Override
    public int getLatency() {
        return effect.getLatency();
    }
}
```

---

## Effect System

### Effect Hierarchy

```
AudioEffect (interface)
    │
    ├── AbstractEffect (base class)
    │       │
    │       ├── GainEffect
    │       ├── DelayEffect
    │       ├── ReverbEffect
    │       └── ... (70+ effects)
    │
    └── AbstractStereoEffect (stereo-specific base)
```

### Effect Lifecycle

```java
public interface AudioEffect {
    // Metadata
    EffectMetadata getMetadata();
    List<Parameter> getParameters();

    // Lifecycle
    void prepare(int sampleRate, int maxFrameCount);
    void process(float[] input, float[] output, int frameCount);
    void processStereo(float[] inL, float[] inR,
                       float[] outL, float[] outR, int frameCount);
    void reset();
    void release();

    // State
    void setBypass(boolean bypass);
    boolean isBypass();
    int getLatency();
}
```

### Parameter System

```java
public class Parameter {
    private final String id;
    private final String name;
    private final float min, max, defaultValue;
    private float value;
    private float smoothedValue;
    private float smoothingCoeff;

    // Smoothing for click-free parameter changes
    public void smooth() {
        smoothedValue += (value - smoothedValue) * smoothingCoeff;
    }

    // Parameter types
    public static Parameter createFloat(String id, String name,
                                        float min, float max, float def);
    public static Parameter createChoice(String id, String name,
                                         String[] choices, int def);
    public static Parameter createBoolean(String id, String name, boolean def);
}
```

### Creating a New Effect

1. Extend `AbstractEffect`:

```java
public class MyEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
        "myeffect",           // Unique ID
        "My Effect",          // Display name
        "Description here",   // Description
        EffectCategory.MODULATION  // Category
    );

    private Parameter mixParam;
    private Parameter rateParam;

    // DSP state
    private float phase;

    public MyEffect() {
        super(METADATA);
        initParameters();
    }

    private void initParameters() {
        mixParam = addFloatParameter("mix", "Mix",
            0, 100, 50, "%");
        rateParam = addFloatParameter("rate", "Rate",
            0.1f, 10.0f, 1.0f, "Hz");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Allocate buffers, initialize DSP state
        this.phase = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float mix = mixParam.getValue() / 100.0f;
        float rate = rateParam.getValue();
        float phaseInc = rate / sampleRate;

        for (int i = 0; i < frameCount; i++) {
            float mod = (float) Math.sin(phase * 2 * Math.PI);
            output[i] = input[i] * (1 - mix) + input[i] * mod * mix;
            phase += phaseInc;
            if (phase >= 1.0f) phase -= 1.0f;
        }
    }

    @Override
    protected void onReset() {
        phase = 0;
    }
}
```

2. Register in `EffectFactory`:

```java
// In EffectFactory.registerBuiltInEffects():
register("myeffect", MyEffect::new);
```

### Effect Categories

```java
public enum EffectCategory {
    INPUT_SOURCE("Input Sources"),
    OUTPUT_SINK("Output"),
    GAIN_DYNAMICS("Gain & Dynamics"),
    DISTORTION("Distortion"),
    AMP_SIM("Amp Simulation"),
    DELAY("Delay"),
    REVERB("Reverb"),
    MODULATION("Modulation"),
    FILTER("Filter & EQ"),
    PITCH("Pitch"),
    UTILITY("Utility")
}
```

---

## DSP Fundamentals

### Sample Format

- Internal processing: 32-bit float (-1.0 to +1.0)
- Audio I/O: 16-bit signed integer (converted at boundaries)

### Delay Lines

```java
public class DelayLine {
    private float[] buffer;
    private int writeIndex;
    private int maxDelay;

    public void write(float sample) {
        buffer[writeIndex] = sample;
        writeIndex = (writeIndex + 1) % buffer.length;
    }

    public float read(int delaySamples) {
        int readIndex = (writeIndex - delaySamples + buffer.length) % buffer.length;
        return buffer[readIndex];
    }

    // Fractional delay with linear interpolation
    public float readInterpolated(float delaySamples) {
        int intPart = (int) delaySamples;
        float fracPart = delaySamples - intPart;
        float s0 = read(intPart);
        float s1 = read(intPart + 1);
        return s0 + fracPart * (s1 - s0);
    }
}
```

### Biquad Filters

Standard biquad implementation for EQ, filters:

```java
public class BiquadFilter {
    // Coefficients
    private float b0, b1, b2;  // Feedforward
    private float a1, a2;      // Feedback (a0 normalized to 1)

    // State
    private float z1, z2;      // Delay elements

    public float process(float input) {
        float output = b0 * input + z1;
        z1 = b1 * input - a1 * output + z2;
        z2 = b2 * input - a2 * output;
        return output;
    }

    // Coefficient calculators
    public void setLowpass(float freq, float q, float sampleRate);
    public void setHighpass(float freq, float q, float sampleRate);
    public void setBandpass(float freq, float q, float sampleRate);
    public void setNotch(float freq, float q, float sampleRate);
    public void setPeaking(float freq, float q, float gainDb, float sampleRate);
}
```

### All-Pass Filters

Used in phasers and reverb diffusion:

```java
public class AllPassFilter {
    private float[] buffer;
    private int index;
    private float gain;

    public float process(float input) {
        float delayed = buffer[index];
        float output = -input + delayed;
        buffer[index] = input + delayed * gain;
        index = (index + 1) % buffer.length;
        return output;
    }
}
```

### LFO (Low Frequency Oscillator)

```java
public class LFO {
    public enum Waveform { SINE, TRIANGLE, SQUARE, SAW }

    private float phase;
    private float frequency;
    private Waveform waveform;

    public float next(float sampleRate) {
        float value = switch (waveform) {
            case SINE -> (float) Math.sin(phase * 2 * Math.PI);
            case TRIANGLE -> 1 - 4 * Math.abs(phase - 0.5f);
            case SQUARE -> phase < 0.5f ? 1 : -1;
            case SAW -> 2 * phase - 1;
        };

        phase += frequency / sampleRate;
        if (phase >= 1.0f) phase -= 1.0f;

        return value;
    }
}
```

### Pitch Detection (YIN Algorithm)

```java
public class PitchDetector {
    private float[] buffer;
    private float[] difference;
    private float[] cumulativeMean;

    public float detectPitch(float[] input, int sampleRate) {
        // Step 1: Compute difference function
        for (int tau = 0; tau < buffer.length / 2; tau++) {
            float sum = 0;
            for (int i = 0; i < buffer.length / 2; i++) {
                float delta = buffer[i] - buffer[i + tau];
                sum += delta * delta;
            }
            difference[tau] = sum;
        }

        // Step 2: Cumulative mean normalized difference
        cumulativeMean[0] = 1;
        float runningSum = 0;
        for (int tau = 1; tau < difference.length; tau++) {
            runningSum += difference[tau];
            cumulativeMean[tau] = difference[tau] / (runningSum / tau);
        }

        // Step 3: Find first minimum below threshold
        for (int tau = 2; tau < cumulativeMean.length; tau++) {
            if (cumulativeMean[tau] < 0.1f) {
                // Parabolic interpolation for sub-sample accuracy
                float period = parabolicInterpolation(tau);
                return sampleRate / period;
            }
        }

        return 0; // No pitch detected
    }
}
```

### Granular Pitch Shifting

```java
public class GranularPitchShifter {
    private float[] buffer;
    private int grainSize = 512;
    private float[] window;  // Hann window

    public float process(float input, float shiftRatio) {
        // Write to circular buffer
        buffer[writeIndex++] = input;

        // Read with shifted speed
        readPosition += shiftRatio;

        // Crossfade between two grains for smooth output
        float grain1 = readGrain(grainPosition1);
        float grain2 = readGrain(grainPosition2);
        float crossfade = calculateCrossfade();

        return grain1 * (1 - crossfade) + grain2 * crossfade;
    }
}
```

---

## Preset System

### Rig Format

Rigs are saved as JSON files (.jfxrig):

```json
{
  "version": "2.0",
  "name": "My Clean Rig",
  "author": "User",
  "description": "Clean tone with chorus and reverb",
  "nodes": [
    {
      "id": "input_1",
      "type": "audioinput",
      "x": 100,
      "y": 200,
      "parameters": {
        "device": 0,
        "gain": 0.0
      }
    },
    {
      "id": "chorus_1",
      "type": "chorus",
      "x": 300,
      "y": 200,
      "parameters": {
        "rate": 1.5,
        "depth": 50,
        "mix": 40
      }
    }
  ],
  "connections": [
    {
      "source": "input_1.out",
      "target": "chorus_1.inL"
    }
  ]
}
```

### Serialization

```java
public class RigSerializer {
    public void save(Rig rig, File file) throws IOException;
    public Rig load(File file) throws IOException;
}
```

### Effect Presets

Individual effect presets are also JSON:

```json
{
  "effectType": "delay",
  "name": "Slapback",
  "parameters": {
    "time": 120,
    "feedback": 20,
    "mix": 35
  }
}
```

---

## UI Architecture

### Main Components

| Component | Class | Description |
|-----------|-------|-------------|
| Main Window | `MainFrame` | Application frame, menu bar |
| Effect Palette | `EffectPalettePanel` | Draggable effect list by category |
| Signal Canvas | `SignalFlowPanel` | Node graph visualization |
| Parameter Panel | `ParameterPanel` | Selected effect parameters |
| Status Bar | `StatusBarPanel` | Tuner, latency, CPU meter |

### Theme System

```java
public class DarkTheme {
    // Colors
    public static final Color BG_DARK = new Color(30, 30, 35);
    public static final Color BG_MEDIUM = new Color(45, 45, 50);
    public static final Color BG_LIGHT = new Color(60, 60, 65);
    public static final Color ACCENT_PRIMARY = new Color(0, 150, 255);

    // Fonts
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    // Styling methods
    public static void styleButton(JButton button);
    public static void styleComboBox(JComboBox<?> combo);
}
```

### Canvas Rendering

```java
public class SignalFlowPanel extends JPanel {
    private List<NodeBlock> blocks;
    private List<ConnectionWire> wires;

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw connections first (behind nodes)
        for (ConnectionWire wire : wires) {
            wire.draw(g2);
        }

        // Draw nodes
        for (NodeBlock block : blocks) {
            block.draw(g2);
        }
    }
}
```

---

## Development Guide

### Building

```bash
# Full build
mvn clean package

# Quick compile
mvn compile

# Run
mvn exec:java
```

### Code Style

- Package-private by default, public only when needed
- Immutable records for data classes
- Builder pattern for complex objects
- Null-safety with Optional where appropriate

### Adding a New Effect

1. Create effect class in `effects/impl/`
2. Register in `EffectFactory`
3. Create documentation in `docs/effects/`
4. Test with different sample rates and buffer sizes

### Debugging Audio

Enable verbose logging:

```java
System.setProperty("jfx2.audio.debug", "true");
```

Check for common issues:
- Buffer underruns: Increase buffer size
- Latency: Reduce buffer size
- Clicks/pops: Check for denormals, parameter smoothing

### Performance Optimization

- Avoid allocations in process() methods
- Use pre-allocated buffers
- Denormal prevention: `value = (Math.abs(value) < 1e-15f) ? 0 : value;`
- SIMD-friendly loops (JIT will vectorize)

### Thread Safety

- Audio thread is high-priority, avoid blocking
- Parameter changes are thread-safe via smoothing
- UI updates via SwingUtilities.invokeLater()

---

## API Reference

See Javadoc in source code for detailed API documentation.

Key classes:
- `AudioEngine` - Audio lifecycle management
- `SignalGraph` - Node graph management
- `AudioEffect` - Effect interface
- `AbstractEffect` - Effect base class
- `Parameter` - Effect parameter
- `EffectFactory` - Effect registry
- `RigSerializer` - Preset I/O

---

*Document version: 2.0*
*Last updated: December 2024*
