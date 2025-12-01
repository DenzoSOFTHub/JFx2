# Neural Network Trainer

## Overview

Neural network trainer for audio effect modeling. Trains a feedforward neural network to learn the transformation from a dry (input) audio signal to a wet (processed) signal. The trained network can then be used as a real-time audio effect.

**Package:** `it.denzosoft.jfx2.tools`

## Features

- **Audio-to-Audio Learning**: Trains from dry/wet WAV file pairs
- **Windowed Input**: Uses sliding window of past samples for context
- **Adam Optimizer**: Adaptive learning rate optimization
- **Early Stopping**: Automatic convergence detection
- **Progress Reporting**: Real-time training metrics
- **Batch Processing**: Efficient mini-batch gradient descent
- **Model Serialization**: Save/load trained networks (.jfxnn format)
- **Pure Java**: No external ML libraries required

## Command-Line Usage

### Basic Training

```bash
java NeuralNetworkTrainer dry.wav wet.wav output.jfxnn
```

### With Options

```bash
java NeuralNetworkTrainer guitar_dry.wav guitar_amped.wav amp_model.jfxnn \
    --window 64 \
    --hidden 32,16,8 \
    --lr 0.001 \
    --batch 64 \
    --epochs 100 \
    --threshold 0.0001
```

## Programmatic Usage

### Simple Training

```java
NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();
float loss = trainer.train("dry.wav", "wet.wav", "model.jfxnn");
System.out.println("Final loss: " + loss);
```

### Custom Configuration

```java
NeuralNetworkTrainer trainer = new NeuralNetworkTrainer()
    .setInputWindowSize(64)
    .setHiddenSizes(32, 16, 8)
    .setLearningRate(0.001f)
    .setBatchSize(64)
    .setMaxEpochs(100)
    .setConvergenceThreshold(0.0001f)
    .setVerbose(true);

float finalLoss = trainer.train(
    Path.of("dry.wav"),
    Path.of("wet.wav"),
    Path.of("model.jfxnn")
);

// Access trained network
NeuralNetwork network = trainer.getNetwork();
```

## Public Methods

### Constructors

#### `NeuralNetworkTrainer()`

Create trainer with default configuration:
- Input window: 64 samples
- Hidden layers: [32, 16, 8]
- Activation: Tanh
- Learning rate: 0.001
- Batch size: 64
- Max epochs: 100

### Training

#### `float train(String dryWavPath, String wetWavPath, String outputModelPath) throws IOException`

Train network from WAV files (string paths).

**Returns:** Final training loss (MSE)

#### `float train(Path dryWavPath, Path wetWavPath, Path outputModelPath) throws IOException`

Train network from WAV files (Path objects).

**Parameters:**
- `dryWavPath`: Input (dry) audio file
- `wetWavPath`: Target (wet/processed) audio file
- `outputModelPath`: Where to save trained model

**Returns:** Final training loss (MSE)

**Process:**
1. Load and validate audio files
2. Create neural network architecture
3. Train using Adam optimizer
4. Apply early stopping if converged
5. Save best model to disk

### Configuration (Fluent API)

All configuration methods return `this` for chaining.

#### `NeuralNetworkTrainer setInputWindowSize(int size)`

Set the input window size (number of past samples used as context).

**Default:** 64

**Recommended:**
- 32: Fast, less context (simple effects)
- 64: Good balance (distortion, compression)
- 128: More context (time-based effects)
- 256: Maximum context (complex effects)

#### `NeuralNetworkTrainer setHiddenSizes(int... sizes)`

Set hidden layer sizes.

**Default:** [32, 16, 8]

**Examples:**
- `[64, 32]`: Larger network, more capacity
- `[16, 8]`: Smaller, faster
- `[64, 64, 32]`: Deep network

**Rules:**
- More neurons = more capacity, slower training
- Deeper = can learn more complex patterns
- Typically decrease size in deeper layers

#### `NeuralNetworkTrainer setHiddenActivation(ActivationFunction activation)`

Set activation function for hidden layers.

**Default:** `ActivationFunction.TANH`

**Options:**
- `TANH`: Good for audio (range -1 to 1)
- `RELU`: Fast, but may clip
- `SIGMOID`: Legacy, slower convergence

#### `NeuralNetworkTrainer setLearningRate(float rate)`

Set learning rate for Adam optimizer.

**Default:** 0.001

**Recommended:**
- 0.0001: Conservative, slower but stable
- 0.001: Good default
- 0.01: Aggressive, may diverge

#### `NeuralNetworkTrainer setBatchSize(int size)`

Set mini-batch size for gradient descent.

**Default:** 64

**Recommendations:**
- 32: Smaller, more updates, noisier gradients
- 64: Good balance
- 128: Larger, smoother gradients, fewer updates

#### `NeuralNetworkTrainer setMaxEpochs(int epochs)`

Set maximum number of training epochs.

**Default:** 100

Training may stop earlier if convergence threshold is reached.

#### `NeuralNetworkTrainer setConvergenceThreshold(float threshold)`

Set MSE threshold for early stopping.

**Default:** 0.0001

**Stopping criteria:**
- Loss < threshold: Converged, stop immediately
- No improvement for 5 epochs: Stop training

#### `NeuralNetworkTrainer setChunkSize(int size)`

Set chunk size for processing long audio files.

**Default:** 44100 (1 second)

Processes audio in chunks to avoid memory issues. Smaller chunks = more frequent progress updates.

#### `NeuralNetworkTrainer setVerbose(boolean verbose)`

Enable/disable console output.

**Default:** true

### Access

#### `NeuralNetwork getNetwork()`

Get the trained network (null if not yet trained).

Use this to access the network after training for manual testing or integration.

## Training Process

### Input Preparation

For each output sample, the network receives a window of past input samples:

```
Input window [t-63, t-62, ..., t-1, t] → Network → Output [t]
```

**Example with window size 64:**
```
dry[0..63]   → network → wet[63]
dry[1..64]   → network → wet[64]
dry[2..65]   → network → wet[65]
...
```

### Training Algorithm

1. **Initialization**:
   - Create network with specified architecture
   - Enable Adam optimizer
   - Reset best loss tracker

2. **Epoch Loop** (up to maxEpochs):
   - For each chunk of audio:
     - For each sample in chunk:
       - Build input window
       - Forward pass (compute output)
       - Backward pass (compute gradients)
       - Accumulate gradients
       - Update weights every batchSize samples
     - Check convergence within chunk
   - Calculate epoch loss (MSE)
   - Check for improvement
   - Save model if improved
   - Early stopping if no improvement for 5 epochs

3. **Convergence Checks**:
   - **Early (within chunk)**: Loss < threshold → stop immediately
   - **Per epoch**: No improvement for 5 epochs → stop
   - **Maximum epochs**: Reached maxEpochs → stop

### Progress Output

```
=== Neural Network Trainer ===

Loading dry audio: guitar_dry.wav
  Samples: 220500
Loading wet audio: guitar_amped.wav
  Samples: 220500

Network: [64 → 32 → 16 → 8 → 1]
  Hidden activation: Tanh
  Output activation: Linear
  Total parameters: 2977
  Optimizer: Adam

Training configuration:
  Learning rate: 0.001
  Batch size: 64
  Max epochs: 100
  Convergence threshold: 0.0001
  Chunk size: 44100 samples

Trainable samples: 220436
Number of chunks: 5

Epoch   1/100 - Loss: 0.025431 - Time: 2.3s
Epoch   2/100 - Loss: 0.012567 - Time: 4.6s
Epoch   3/100 - Loss: 0.008234 - Time: 6.9s
...
Epoch  28/100 - Loss: 0.000089 - Time: 64.4s
Converged at epoch 28 (loss: 0.000089)

Training complete. Final loss: 0.000089, Time: 64.4s
Model saved to: amp_model.jfxnn
```

## Use Cases

### Amp Modeling

Train a network to emulate an amplifier:

```bash
# 1. Record dry guitar
record_audio guitar_dry.wav

# 2. Play through amp and record
play_through_amp guitar_dry.wav
record_audio guitar_amped.wav

# 3. Train model
java NeuralNetworkTrainer guitar_dry.wav guitar_amped.wav my_amp.jfxnn
```

### Effect Learning

Learn any audio effect:

```bash
# Distortion pedal
java NeuralNetworkTrainer clean.wav distorted.wav distortion_model.jfxnn

# Compressor
java NeuralNetworkTrainer uncompressed.wav compressed.wav compressor_model.jfxnn

# EQ
java NeuralNetworkTrainer flat.wav eq_curve.wav eq_model.jfxnn
```

### Speaker Simulation

Model speaker/cabinet response:

```bash
# Compare DI signal to mic'd cabinet
java NeuralNetworkTrainer guitar_di.wav guitar_mic.wav cabinet_model.jfxnn --window 128
```

## Command-Line Options

### --window <size>

Input window size (default: 64)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --window 128
```

### --hidden <sizes>

Hidden layer sizes, comma-separated (default: 32,16,8)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --hidden 64,32,16
```

### --lr <rate>

Learning rate (default: 0.001)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --lr 0.0005
```

### --batch <size>

Batch size (default: 64)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --batch 128
```

### --epochs <num>

Maximum epochs (default: 100)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --epochs 200
```

### --threshold <val>

Convergence threshold (default: 0.0001)

```bash
java NeuralNetworkTrainer dry.wav wet.wav model.jfxnn --threshold 0.00001
```

## Model File Format

Trained models are saved in `.jfxnn` format (JFx2 Neural Network).

**Contains:**
- Network architecture (layer sizes)
- All weights and biases
- Activation functions
- Training metadata

**Usage:**
```java
// Load trained model
NeuralNetwork network = NeuralNetwork.load(Path.of("model.jfxnn"));

// Use in effect
NeuralNetworkEffect effect = new NeuralNetworkEffect();
effect.loadModel("model.jfxnn");
```

## Audio File Requirements

### Format Support

Supported via javax.sound.sampled:
- **WAV**: Recommended
- **AIFF**: Supported
- **AU**: Supported

### Sample Format

- **Bit depth**: 8, 16, 24, 32-bit (auto-converted)
- **Encoding**: PCM signed/unsigned, PCM float (auto-converted)
- **Channels**: Mono or stereo (stereo averaged to mono)
- **Sample rate**: Any (trainer uses actual sample rate, not resampled)

### Quality Recommendations

- **Sample rate**: 44.1kHz or 48kHz
- **Bit depth**: 24-bit for recording, 16-bit acceptable
- **Length**: 30 seconds to 5 minutes (more data = better model)
- **Content**: Varied input (full frequency range, dynamics)
- **Alignment**: Dry and wet must be sample-aligned (same length)

## Performance Metrics

### Training Speed

Typical on modern CPU (i7/Ryzen):
- **Small network** [32, 16]: ~50,000 samples/sec
- **Medium network** [64, 32, 16]: ~30,000 samples/sec
- **Large network** [128, 64, 32]: ~15,000 samples/sec

**Example:** 3 minutes of audio (7.9M samples) with [32, 16, 8]:
- Training time: ~3-5 minutes for 100 epochs
- Typical convergence: 20-50 epochs (1-3 minutes)

### Model Size

Network parameters = weights + biases

**Examples:**
- `[64 → 32 → 16 → 8 → 1]`: ~2,977 parameters (~12 KB)
- `[128 → 64 → 32 → 16 → 1]`: ~11,729 parameters (~47 KB)
- `[64 → 64 → 64 → 1]`: ~8,513 parameters (~34 KB)

### Runtime Performance

Inference (real-time effect) speed:
- **Small network**: 0.1-0.5% CPU per voice
- **Medium network**: 0.5-2% CPU per voice
- **Large network**: 2-5% CPU per voice

**Real-time capable:** Yes, all network sizes suitable for real-time audio

## Tips for Best Results

### Recording Quality

1. **Matched levels**: Ensure dry and wet have similar signal levels
2. **No clipping**: Avoid digital clipping in recordings
3. **Low noise**: Use quiet environment, good preamp
4. **Sample alignment**: Start recording at same time for both
5. **Consistent settings**: Don't change amp/effect settings during recording

### Training Data

1. **Varied input**: Use full frequency range (chords, single notes, harmonics)
2. **Dynamic range**: Include quiet and loud passages
3. **Length**: 1-3 minutes usually sufficient
4. **Multiple takes**: More varied data → better generalization
5. **Test set**: Save 20% of data for validation

### Network Architecture

1. **Start simple**: Begin with [32, 16, 8], increase if needed
2. **Match complexity**:
   - Simple effects (gain, EQ): Small network
   - Nonlinear (distortion, compression): Medium
   - Complex (amp+cab): Larger network
3. **Window size**: Match to effect time dependence
   - Memoryless: 32 samples
   - Short memory: 64-128 samples
   - Long memory: Not suitable for feedforward NN

### Hyperparameters

1. **Learning rate**: Start at 0.001, reduce if loss oscillates
2. **Batch size**: 64 is good default, increase for smoother training
3. **Patience**: Allow 50-100 epochs before giving up
4. **Early stopping**: Let it work, trust the convergence detection

### Validation

After training, test the model:

```java
// Load both original audio and model
float[] dry = loadAudio("dry.wav");
float[] wet = loadAudio("wet.wav");
NeuralNetwork network = NeuralNetwork.load("model.jfxnn");

// Process dry through network
float[] predicted = new float[wet.length];
for (int i = windowSize; i < dry.length; i++) {
    float[] input = Arrays.copyOfRange(dry, i - windowSize, i);
    float[] output = network.forward(input);
    predicted[i] = output[0];
}

// Compare predicted vs actual wet
float mse = calculateMSE(predicted, wet);
System.out.println("Validation MSE: " + mse);
```

## Troubleshooting

### Loss not decreasing

- Reduce learning rate (try 0.0001)
- Increase network size
- Check data alignment (dry/wet must match)
- Ensure varied training data

### Loss exploding (NaN)

- Reduce learning rate significantly
- Check for clipping in input audio
- Normalize audio levels (-1 to +1 range)

### Poor generalization

- Reduce network size (overfitting)
- Add more varied training data
- Train longer (more epochs)

### Training too slow

- Reduce window size
- Use smaller network
- Increase batch size
- Reduce audio length

## Limitations

### Not Suitable For

- **Time-based effects**: Delays, reverbs, loopers
- **Modulation**: LFO/envelope-based modulation
- **Dynamic effects**: Compressor attack/release (partially works)
- **Multi-band**: Frequency-dependent processing (limited)

### Better Suited For

- **Saturation/distortion**: Nonlinear waveshaping
- **Amp modeling**: Tube amp characteristics
- **Tone shaping**: Fixed EQ curves
- **Subtle coloring**: Tape saturation, analog warmth

### Current Limitations

- **Mono only**: No stereo processing
- **Fixed architecture**: Can't modify network after training
- **No real-time training**: Offline training only
- **Memory**: Window size limits temporal context

## See Also

- **NeuralNetwork**: Core neural network implementation
- **NeuralNetworkEffect**: Audio effect using trained models
- **OfflineProcessor**: Batch processing for testing models
- **ActivationFunction**: Available activation functions
