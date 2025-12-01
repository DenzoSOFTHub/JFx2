# NAMEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `nam`
**Display Name**: NAM

Neural Amp Modeler (NAM) effect for loading and processing audio using trained neural network models of real amplifiers and effects.

## Description

The NAMEffect loads and processes audio using .nam model files. These models are trained using machine learning to replicate the sound of real amplifiers, pedals, and other audio equipment with high accuracy.

NAM models capture:
- Complete amp tonal characteristics
- Nonlinear behavior and dynamics
- Frequency response and harmonics
- Compression and saturation

Supports WaveNet and LSTM neural network architectures.

## Parameters

### Input/Output

#### inputGain (Input Gain)
- **Range**: -12.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Adjust input level before NAM processing
- **Usage**: Match input level to what the model was trained with. Too hot = distorted, too low = weak

#### outputGain (Output Gain)
- **Range**: -12.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Adjust output level after NAM processing
- **Usage**: Compensate for level changes or match other effects in chain

#### mix (Mix)
- **Range**: 0.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Blend between dry and processed signal
- **Usage**: 100% for full amp simulation. Lower values for parallel processing or subtle coloration.

## Model Loading

### Loading a Model

Use the `loadModel()` method:

```java
NAMEffect namEffect = new NAMEffect();
boolean success = namEffect.loadModel("/path/to/model.nam");
if (success) {
    System.out.println("Model loaded: " + namEffect.getModelPath());
}
```

### Model Information

Query loaded model information:

```java
boolean loaded = namEffect.isModelLoaded();
String path = namEffect.getModelPath();
NAMLoader.NAMMetadata metadata = namEffect.getNAMMetadata();
NAMModel model = namEffect.getModel();

System.out.println("Architecture: " + metadata.architecture());
System.out.println("Sample Rate: " + model.getSampleRate());
```

## NAM Model Files

### File Format

NAM models are stored in .nam files containing:
- Model architecture (WaveNet or LSTM)
- Trained neural network weights
- Sample rate the model was trained at
- Metadata (name, description, date, etc.)

### Acquiring Models

NAM models can be:
- Downloaded from online repositories
- Captured using NAM training software
- Shared by the community

Common sources:
- ToneHunt.org (community models)
- Neural Amp Modeler GitHub (examples)
- Commercial model packs

### Creating Your Own Models

Use the official NAM training software:
1. Record dry guitar signal
2. Re-amp through target equipment
3. Record wet signal
4. Train neural network to match dry → wet
5. Export .nam file

Training typically requires:
- Clean DI guitar signal (5-10 minutes)
- Reference recording through target gear
- Python environment with NAM software
- GPU for faster training (optional)

## Signal Flow

1. **Input Stage**
   - Receive input signal
   - Apply input gain

2. **Model Loading Check**
   - If no model loaded: pass through with output gain
   - If model loaded: continue processing

3. **Sample Rate Handling**
   - Check if effect sample rate matches model sample rate
   - If mismatch: apply resampling (linear interpolation)

4. **NAM Processing**
   - Process each sample through neural network
   - Model applies learned transformation

5. **Output Stage**
   - Apply output gain
   - Mix with dry signal
   - Output result

## Implementation Details

### Automatic Resampling

If the effect's sample rate doesn't match the model's sample rate, the effect automatically resamples:

```
Model @ 48kHz, Effect @ 44.1kHz:
  resampleRatio = 48000 / 44100 = 1.088

For each effect sample:
  - Accumulate phase
  - When phase >= 1.0, process model
  - Interpolate output back to effect rate
```

Uses simple linear interpolation. For best quality, use models trained at your target sample rate.

### Model State Management

The NAM model maintains internal state for recurrent networks (LSTM):
- State is preserved between process calls
- Call `reset()` to clear state
- Important for accurate LSTM behavior

### Stereo Processing

Processes stereo as mono:
1. Mix L+R to mono
2. Process through model
3. Apply processed signal to both channels

This matches typical amp behavior (mono input → mono output).

## Usage Tips

### Matching Input Levels

NAM models are sensitive to input level:

**Too Low**:
- Weak, thin tone
- Loss of dynamics
- Doesn't engage model properly

**Too High**:
- Distorted beyond what model expects
- Unnatural artifacts
- Loss of clarity

**Just Right**:
- Match the input level used during training
- Typically -12 to -18 dBFS peak for guitar
- Use input gain to adjust

### Clean vs Overdriven Models

**Clean Models**:
- Input Gain: 0 dB (nominal)
- Use high-output pickups or boost before for drive
- Stack multiple NAM models

**Overdriven/High-Gain Models**:
- Input Gain: -3 to -6 dB (tame hot pickups)
- May need output gain boost
- Often best with no additional drive

### Chaining NAM Models

You can stack multiple NAM effects:
1. Preamp model → Power amp model → Cabinet model
2. Pedal model → Amp model → Cabinet model

Tips:
- Watch gain staging between models
- Use output gain of first to set input level of second
- Cabinet model usually goes last

### Performance Considerations

**WaveNet Models**:
- Generally lighter CPU usage
- Faster processing
- Good for real-time

**LSTM Models**:
- Higher CPU usage
- More computationally intensive
- May add latency

Monitor CPU usage and adjust buffer size if needed.

### Sample Rate Recommendations

For best results:
- Use models trained at your target sample rate
- 44.1 kHz or 48 kHz are most common
- Resampling adds slight quality loss
- No resampling = zero added latency

## Troubleshooting

### Model Won't Load

**Check**:
- File path is correct
- File is a valid .nam file
- File isn't corrupted
- File permissions are readable

### Sounds Wrong or Distorted

**Try**:
- Adjust input gain (±6 dB)
- Check if output is clipping
- Verify sample rate matches
- Reset effect state
- Reload model

### Excessive CPU Usage

**Solutions**:
- Use WaveNet instead of LSTM models
- Increase audio buffer size
- Use lower sample rate (44.1 vs 96 kHz)
- Reduce polyphony if using multiple instances

### Latency Issues

**Check**:
- Model sample rate (resampling adds latency)
- Audio buffer size
- Use WaveNet for lower latency

## Technical Specifications

- **Processing**: 32-bit float
- **Neural Networks**: WaveNet, LSTM architectures
- **Latency**: Minimal (depends on model and resampling)
- **CPU Usage**: Variable (WaveNet < LSTM)
- **Stereo**: Mono processing (summed to mono, duplicated to stereo)
- **Max Sample Rate**: Unlimited (resampling applied)
- **Supported Formats**: .nam files only

## Comparison with Other Amp Sims

| Feature | NAMEffect | AmpEffect | TubePreamp |
|---------|-----------|-----------|------------|
| Accuracy | Highest (real gear) | Good (modeled) | Good (modeled) |
| Flexibility | Limited (per model) | High (many params) | High (many params) |
| CPU | Medium-High | Low | Low-Medium |
| Customization | None (fixed model) | Full control | Full control |
| Setup | Load models | Adjust params | Adjust params |

## Best Practices

1. **Organize Your Models**: Keep models in organized folders by type (amps, pedals, cabs)
2. **Label Clearly**: Name files with amp name, gain setting, etc.
3. **Test Input Levels**: Find the sweet spot for each model
4. **Save Presets**: Save your favorite model + gain combinations
5. **Use Appropriate Chains**: Preamp → Power Amp → Cabinet order
6. **Monitor CPU**: Don't overload with too many NAM instances
7. **Match Sample Rates**: Avoid resampling when possible

## See Also

- NeuralAmpEffect - Custom trained neural networks (.jfxnn format)
- AmpEffect - Parametric amp simulation
- IRLoaderEffect - Cabinet impulse responses
- docs/NEURAL_AMP_MODELER.md - Detailed NAM documentation
- NAMLoader class - Model loading implementation
- NAMModel class - Model processing implementation
