# NeuralAmpEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `neuralamp`
**Display Name**: Neural Amp

AI-powered amp/effect modeling using custom-trained neural networks in JFx2's native .jfxnn format.

## Description

The NeuralAmpEffect uses trained neural networks to process audio, emulating the characteristics of amplifiers, pedals, or other audio transformations learned from dry/wet audio pairs.

Unlike NAMEffect which loads external .nam files, this effect uses JFx2's internal neural network format (.jfxnn) and can be trained using the included NeuralNetworkTrainer tool.

Features:
- Custom neural network architecture
- Sliding window input for temporal context
- Trained on dry/wet audio pairs
- Learns nonlinear behavior and dynamics
- Supports arbitrary audio transformations

## Parameters

### Input/Output

#### inputGain (Input Gain)
- **Range**: -12.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Adjust input level before neural network processing
- **Usage**: Match the input level used during training for best results

#### outputGain (Output Gain)
- **Range**: -12.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Adjust output level after neural network processing
- **Usage**: Compensate for level changes or normalize output

#### mix (Mix)
- **Range**: 0.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Blend between dry and processed signal
- **Usage**: 100% for full effect. Lower values for parallel processing or subtle coloration.

## Model Loading

### Loading a Model

Use the `loadModel()` method:

```java
NeuralAmpEffect neuralAmp = new NeuralAmpEffect();
boolean success = neuralAmp.loadModel("/path/to/model.jfxnn");
if (success) {
    System.out.println("Model loaded: " + neuralAmp.getModelPath());
}
```

### Model Information

Query loaded model:

```java
boolean loaded = neuralAmp.isModelLoaded();
String path = neuralAmp.getModelPath();
NeuralNetwork network = neuralAmp.getNetwork();

System.out.println(network.getSummary());
System.out.println("Window Size: " + network.getInputWindowSize());
```

## Neural Network Architecture

### Sliding Window Input

The network processes audio using a sliding window approach:

**Window Size**: Typically 32-128 samples
- Larger window: More context, better for long-term dynamics
- Smaller window: Less latency, faster processing

**Input**: Last N samples (where N = window size)
**Output**: Single sample prediction

### Network Structure

Typical architecture:
```
Input Layer: [window_size] samples
Hidden Layer 1: 32-64 neurons, tanh activation
Hidden Layer 2: 32-64 neurons, tanh activation
Hidden Layer 3: 16-32 neurons, tanh activation
Output Layer: 1 neuron, linear activation
```

### Training Data

Networks are trained on audio pairs:
- **Dry Signal**: Clean input (DI guitar)
- **Wet Signal**: Processed through target equipment
- **Training**: Network learns to transform dry → wet

## Signal Flow

1. **Input Stage**
   - Receive input signal
   - Apply input gain

2. **Model Check**
   - If no model loaded: pass through with output gain
   - If model loaded: continue processing

3. **Sliding Window Buffer**
   - Maintain circular buffer of recent samples
   - Buffer size = network input window size

4. **Neural Network Processing**
   - Extract window from circular buffer
   - Feed window to neural network
   - Get single sample prediction

5. **Output Stage**
   - Apply output gain
   - Mix with dry signal
   - Output result

## Implementation Details

### Circular Buffer Management

```
Buffer size = window size (e.g., 64 samples)

For each sample:
  1. Add sample to buffer at current position
  2. Build window: samples from (pos+1) to pos (oldest to newest)
  3. Advance buffer position (wrap around)
  4. Process window through network
```

### Window Construction

The window is built oldest-to-newest:
```
If buffer position = 10, window size = 4:
  Window = [buffer[11], buffer[12], buffer[13], buffer[10]]
           (oldest)                            (newest)
```

This provides the network with recent sample history.

### Stereo Processing

Processes stereo as mono (typical for guitar amps):
1. Mix L+R channels to mono
2. Process through neural network
3. Apply processed signal to both output channels

## Training Your Own Models

### Using the NeuralNetworkTrainer Tool

1. **Prepare Training Data**
   - Record clean DI guitar signal (~5-10 minutes)
   - Re-amp through target equipment
   - Record wet signal
   - Export both as mono WAV files (44.1/48kHz)

2. **Train the Network**
   ```bash
   java -cp JFx2.jar it.denzosoft.jfx2.nn.NeuralNetworkTrainer \
     --dry dry_signal.wav \
     --wet wet_signal.wav \
     --output model.jfxnn \
     --window 64 \
     --epochs 100
   ```

3. **Load and Test**
   ```java
   NeuralAmpEffect effect = new NeuralAmpEffect();
   effect.loadModel("model.jfxnn");
   ```

### Training Parameters

**Window Size** (--window):
- 32: Fast, less context
- 64: Balanced (recommended)
- 128: More context, slower

**Epochs** (--epochs):
- 50-100: Quick training, may underfit
- 100-200: Balanced (recommended)
- 200+: Risk of overfitting

**Learning Rate** (--lr):
- 0.01: Fast convergence, may be unstable
- 0.001: Balanced (default)
- 0.0001: Slow, very stable

**Hidden Layers** (--layers):
- [32, 16]: Small, fast
- [64, 64, 32]: Balanced (default)
- [128, 128, 64]: Large, more accurate

### Training Data Tips

**Dry Signal Requirements**:
- Clean DI recording
- Wide dynamic range (soft to hard picking)
- All strings, all frets
- Various techniques (palm mutes, chords, leads)
- No effects, no processing

**Wet Signal Requirements**:
- Recorded through target equipment
- Exact same performance as dry
- Same length, perfect sync
- No additional processing (unless desired)

**Recording Quality**:
- 44.1 kHz or 48 kHz sample rate
- 24-bit depth recommended
- Low noise floor
- No clipping

## Usage Tips

### Clean Amp Models

**Input Level**:
- Match training data level
- Usually 0 dB for clean DI signal

**Output Level**:
- May need +3 to +6 dB boost
- Clean amps often have lower output

**Mix**:
- 100% for pure amp tone
- 80-90% to retain pick attack

### High-Gain Models

**Input Level**:
- May need -3 to -6 dB cut
- Prevents over-driving the model

**Output Level**:
- Often need 0 to +3 dB boost
- High-gain compresses signal

**Mix**:
- 100% for full saturation
- 70-90% for transparent OD feel

### Pedal Models

**Chaining**:
- Can chain multiple pedal models
- Watch gain staging between models

**Order**:
- Typical: Drive → Modulation → Delay → Reverb
- Amp models usually before cabinet

### Parallel Processing

**Mix < 100%**:
- Blend dry and processed
- Useful for subtle coloration
- NY-style parallel compression

**Example** (Parallel Saturation):
- Input Gain: +6 dB (drive model hard)
- Output Gain: 0 dB
- Mix: 30-50% (blend with clean)

## Performance Considerations

### CPU Usage

**Factors**:
- Window size (larger = more CPU)
- Number of layers (more = more CPU)
- Neurons per layer (more = more CPU)

**Optimization**:
- Use smaller window sizes (32-64)
- Fewer layers (2-3 hidden)
- Fewer neurons per layer (32-64)

### Latency

**Sources**:
- Window size (e.g., 64 samples = 1.45ms @ 44.1kHz)
- Audio buffer size
- Processing time

**Reduction**:
- Minimize window size
- Optimize network architecture
- Use lower sample rate (44.1 vs 96 kHz)

## Troubleshooting

### Model Sounds Wrong

**Check**:
- Input gain matches training data
- Output isn't clipping
- Model was trained correctly
- Dry/wet audio was perfectly aligned

**Try**:
- Adjust input gain ±6 dB
- Reset effect state
- Reload model
- Retrain with better data

### Training Issues

**Poor Convergence**:
- Increase epochs
- Adjust learning rate
- Add more training data
- Simplify network architecture

**Overfitting**:
- Reduce epochs
- Add regularization
- Use simpler architecture
- More diverse training data

**Noisy Output**:
- Check training data quality
- Ensure perfect dry/wet alignment
- Reduce noise in recordings
- Add more training data

## Technical Specifications

- **Processing**: 32-bit float
- **Neural Network**: Feedforward with sliding window
- **Activation**: tanh (hidden), linear (output)
- **Latency**: Window size (typically 32-128 samples)
- **CPU Usage**: Medium (depends on architecture)
- **Stereo**: Mono processing (summed to mono, duplicated to stereo)
- **File Format**: .jfxnn (JFx2 native)

## Comparison with NAMEffect

| Feature | NeuralAmpEffect | NAMEffect |
|---------|-----------------|-----------|
| Format | .jfxnn (native) | .nam (external) |
| Training | Built-in trainer | External software |
| Architecture | Custom feedforward | WaveNet/LSTM |
| Accuracy | Good | Excellent |
| CPU | Medium | Medium-High |
| Flexibility | Full control | Fixed format |
| Community | Limited | Large (ToneHunt) |

## Best Practices

1. **Quality Training Data**: Spend time getting good dry/wet recordings
2. **Perfect Alignment**: Ensure dry and wet are perfectly synced
3. **Dynamic Range**: Include full playing dynamics in training data
4. **Model Organization**: Keep trained models organized by type
5. **Test Thoroughly**: Validate model with different playing styles
6. **Start Simple**: Begin with simpler networks, increase complexity as needed
7. **Save Checkpoints**: Save training checkpoints for rollback
8. **Document Settings**: Record training parameters for each model

## Future Enhancements

Possible improvements:
- Recurrent architectures (LSTM, GRU)
- Larger context windows
- Multi-output (stereo) models
- Real-time training feedback
- Automatic hyperparameter tuning

## See Also

- NAMEffect - Load external NAM models
- NeuralNetwork class - Network implementation
- NeuralNetworkTrainer tool - Training utility
- AmpEffect - Parametric amp simulation
- docs/tools/ - Training tool documentation
