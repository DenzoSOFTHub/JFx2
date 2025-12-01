# IR Generator

## Overview

High-quality Impulse Response (IR) generator tool for creating cabinet and effect IRs. Uses advanced DSP techniques including Wiener deconvolution, logarithmic sweep generation, and spectral analysis.

**Package:** `it.denzosoft.jfx2.tools`

## Features

- **Wiener Deconvolution**: Robust deconvolution with regularization for noise reduction
- **Sweep-Based Capture**: Logarithmic sweep generation for optimal SNR
- **Automatic Time Alignment**: Cross-correlation-based alignment of dry/wet signals
- **Spectral Smoothing**: Noise reduction in frequency domain
- **Minimum Phase Conversion**: Optional minimum-phase IR generation
- **Quality Analysis**: Comprehensive metrics (RT60, frequency balance, peak analysis)
- **Multiple IR Lengths**: 512, 1024, 2048, or 4096 samples
- **Pure Java**: No external dependencies (uses javax.sound.sampled only)

## Command-Line Usage

### Generate IR from Dry/Wet Pair

```bash
# Basic usage
java IRGenerator generate dry.wav wet.wav output.wav

# With IR length
java IRGenerator generate dry.wav wet.wav output.wav 2048

# With options
java IRGenerator generate dry.wav wet.wav output.wav 2048 \
    --minimum-phase \
    --reg 0.001 \
    --no-align
```

### Generate Sweep for IR Capture

```bash
# Basic (3 seconds @ 44.1kHz)
java IRGenerator sweep test_sweep.wav

# Custom duration and sample rate
java IRGenerator sweep test_sweep.wav 5.0 48000
```

### Generate Inverse Sweep Filter

```bash
java IRGenerator inverse sweep.wav inverse_sweep.wav
```

### Analyze IR Quality

```bash
java IRGenerator analyze my_ir.wav
```

## Programmatic Usage

```java
IRGenerator generator = new IRGenerator();

// Load audio files
AudioData dry = generator.loadWav("dry.wav");
AudioData wet = generator.loadWav("wet.wav");

// Generate IR (2048 samples, minimum phase, auto-align enabled)
float[] ir = generator.wienerDeconvolution(
    dry.samples, wet.samples,
    Math.min(dry.samples.length, wet.samples.length),
    2048,
    0.001f  // regularization
);

// Normalize and apply fade-out
generator.normalize(ir);
generator.applyFadeOut(ir, 512);

// Save IR
generator.saveWav(ir, "output_ir.wav", dry.sampleRate);
```

## Commands

### generate

Generate IR from dry/wet audio pair.

**Syntax:**
```bash
IRGenerator generate <dry.wav> <wet.wav> <output.wav> [irLength] [options]
```

**Parameters:**
- `dry.wav`: Input (dry) signal
- `wet.wav`: Output (wet/processed) signal
- `output.wav`: Output IR file
- `irLength`: IR length in samples (512, 1024, 2048, 4096) - default: 2048

**Options:**
- `--minimum-phase`, `-m`: Convert to minimum phase IR
- `--no-align`: Disable automatic time alignment
- `--reg <value>`: Regularization factor for Wiener filter (default: 0.001)

**Example:**
```bash
IRGenerator generate guitar_dry.wav guitar_amped.wav my_amp.wav 2048 -m --reg 0.002
```

### sweep

Generate logarithmic sine sweep for IR capture.

**Syntax:**
```bash
IRGenerator sweep <output.wav> [duration] [sampleRate]
```

**Parameters:**
- `output.wav`: Output sweep file
- `duration`: Duration in seconds (default: 3.0)
- `sampleRate`: Sample rate in Hz (default: 44100)

**Output:**
- Sweep from 20 Hz to 20,000 Hz
- 0.5 second silence before and after sweep
- Smooth fade in/out (100ms raised cosine)

**Example:**
```bash
IRGenerator sweep test_sweep.wav 3 48000
```

### inverse

Generate inverse filter for a sweep (for manual linear deconvolution).

**Syntax:**
```bash
IRGenerator inverse <sweep.wav> [output.wav]
```

**Example:**
```bash
IRGenerator inverse test_sweep.wav inverse_sweep.wav
```

### analyze

Analyze an IR file and show quality metrics.

**Syntax:**
```bash
IRGenerator analyze <ir.wav>
```

**Displays:**
- Peak level and position
- RT60 decay time
- Early energy ratio
- Frequency balance (bass/mids/treble)

## Workflow for Best Results

### 1. Generate Sweep

```bash
IRGenerator sweep test_sweep.wav 3
```

### 2. Record Your Amp/Cabinet

- Play the sweep through your amp at moderate volume
- Record the output with a microphone or DI box
- Use a quiet environment to minimize noise
- Save as `recorded.wav`

### 3. Generate IR

```bash
IRGenerator generate test_sweep.wav recorded.wav my_ir.wav 2048
```

### 4. Analyze Quality

```bash
IRGenerator analyze my_ir.wav
```

Look for:
- Peak level near 0 dB
- RT60 appropriate for cabinet type (10-50ms typical)
- Balanced frequency response

## Public Methods

### Main Generation

#### `static void generateIR(String dryPath, String wetPath, String outputPath, int irLength, boolean minimumPhase, boolean autoAlign, float regularization)`

Complete IR generation workflow with all options.

### Deconvolution Methods

#### `float[] wienerDeconvolution(float[] dry, float[] wet, int length, int irLength, float regularization)`

Wiener deconvolution - robust for noisy signals.

**Formula:** `H(f) = conj(X(f)) × Y(f) / (|X(f)|² + λ)`

**Parameters:**
- `dry`: Dry (input) signal
- `wet`: Wet (processed) signal
- `length`: Length to process
- `irLength`: Output IR length
- `regularization`: Regularization factor (0.0001-0.01 typical)

**Returns:** IR samples

#### `float[] sweepDeconvolution(float[] sweep, float[] response, int length, int irLength, int sampleRate)`

Optimized deconvolution for logarithmic sweeps using inverse filter.

**Better for:**
- Clean recordings with log sweeps
- Higher SNR than Wiener method
- Linear phase preservation

### Sweep Generation

#### `float[] generateLogSweep(float startFreq, float endFreq, float durationSeconds, int sampleRate)`

Generate logarithmic sine sweep.

**Parameters:**
- `startFreq`: Start frequency in Hz (typically 20)
- `endFreq`: End frequency in Hz (typically 20000)
- `durationSeconds`: Sweep duration
- `sampleRate`: Sample rate

**Returns:** Sweep samples with fade in/out

#### `float[] createInverseSweepFilter(float[] sweep, int sampleRate)`

Create inverse filter for a logarithmic sweep.

**Used internally by** `sweepDeconvolution()` **for linear deconvolution.**

### Signal Processing

#### `int findAlignment(float[] a, float[] b)`

Find time alignment between two signals using cross-correlation.

**Returns:** Lag offset in samples (positive = b is delayed, negative = a is delayed)

#### `float[] toMinimumPhase(float[] ir)`

Convert IR to minimum phase using Hilbert transform.

**Benefits:**
- Removes pre-ringing
- Reduces latency
- Preserves magnitude response

#### `void applyNoiseGate(float[] ir)`

Apply noise gate to remove pre-response noise.

Zeros out samples before main response starts (below -60dB threshold).

#### `void normalize(float[] ir)`

Normalize IR to 0.99 peak level.

#### `void applyFadeOut(float[] ir, int fadeLength)`

Apply smooth fade-out (raised cosine window).

**Recommended:** `fadeLength = irLength / 4`

### Quality Analysis

#### `void printQualityMetrics(float[] ir, int sampleRate)`

Print comprehensive quality analysis:
- Peak level and position
- RT60 decay time estimate
- Early energy ratio (first 10ms)
- Frequency balance (bass/mids/treble relative levels)

### File I/O

#### `AudioData loadWav(String path)`

Load WAV file and convert to mono float samples.

**Returns:** `AudioData` record with samples and sample rate

#### `void saveWav(float[] samples, String path, int sampleRate)`

Save float samples as 16-bit WAV file.

## Implementation Details

### Wiener Deconvolution Algorithm

1. **Zero-pad** dry and wet signals to FFT size
2. **Apply Blackman window** to reduce spectral leakage
3. **Forward FFT** on both signals
4. **Estimate noise floor** from high-frequency content
5. **Apply Wiener formula**: `H(f) = X*(f) · Y(f) / (|X(f)|² + λ)`
6. **Inverse FFT** to get IR
7. **Extract** first `irLength` samples

**Adaptive regularization:** `λ = max(regularization, noiseFloor²)`

### Sweep Deconvolution Algorithm

1. **Detect** if input is a logarithmic sweep
2. **Generate inverse filter** (time-reversed with amplitude correction)
3. **Convolve** response with inverse filter (via FFT multiplication)
4. **Find peak** in result (main impulse)
5. **Extract IR** starting from peak position

**Advantages:**
- Higher SNR (sweep energy spread across frequency range)
- Optimal for clean recordings
- Linear phase preservation

### Minimum Phase Conversion

1. **FFT** of original IR
2. **Calculate magnitude** spectrum
3. **Log magnitude** → Hilbert transform → minimum phase
4. **Reconstruct** complex spectrum with original magnitude, minimum phase
5. **Inverse FFT** to time domain

**Result:** No pre-ringing, reduced latency, same magnitude response

### NSDF Peak Detection

Uses same algorithm as Tuner for sweep detection:
- Checks amplitude consistency across time
- Variation < 6dB indicates sweep

### FFT Implementation

Custom Cooley-Tukey radix-2 FFT:
- Bit-reversal permutation
- In-place computation
- Twiddle factor pre-computation
- Forward and inverse modes

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| DEFAULT_REGULARIZATION | 0.001 | Wiener filter regularization |
| NOISE_FLOOR_DB | -60 dB | Noise gate threshold |
| Sweep start freq | 20 Hz | Log sweep low frequency |
| Sweep end freq | 20000 Hz | Log sweep high frequency |
| Fade duration | 100 ms | Sweep fade in/out |
| IR lengths | 512, 1024, 2048, 4096 | Valid IR sizes |

## Quality Metrics Explained

### Peak Level
Should be close to 0 dB after normalization. Indicates proper signal level.

### Peak Position
Time of main impulse arrival. Typically 0-5ms for direct sound.

### RT60
Reverberation time (60dB decay).
- **Cabinets**: 10-50ms
- **Rooms**: 200-2000ms
- **Long reverbs**: 2000-10000ms

### Early Energy Ratio
Percentage of energy in first 10ms.
- **Cabinets**: 80-95% (direct sound dominant)
- **Rooms**: 20-60% (more reverb)

### Frequency Balance
Relative levels of bass (<250Hz), mids (250-4kHz), treble (4-16kHz).
- Helps identify tonal character
- Reference point: mids = 0 dB

## Tips for Best Results

### Recording Quality
- Use a quiet environment
- Proper microphone placement (on-axis, 1-2" from speaker)
- Moderate volume (avoid clipping and distortion)
- 24-bit recording if possible
- Use sweeps for best SNR

### Parameter Selection
- **IR Length**:
  - 1024: Quick, suitable for cabinets
  - 2048: Good balance (recommended)
  - 4096: Long decay, rooms/reverbs
- **Regularization**:
  - 0.0001: Clean recordings
  - 0.001: Normal (default)
  - 0.01: Very noisy recordings
- **Minimum Phase**: Use for cabinets, avoid for reverbs/delays

### Common Issues
- **No signal detected**: Increase recording level
- **Noisy IR**: Increase regularization
- **Pre-ringing**: Use minimum phase or noise gate
- **Misaligned**: Ensure --no-align is not used
- **Frequency imbalance**: Check mic placement, try different positions

## Performance

- **FFT size**: Automatically chosen as next power of 2
- **Memory**: ~4× FFT size (for real/imag buffers)
- **Speed**: Sub-second for typical IR generation on modern hardware
- **File I/O**: Java Sound API (supports WAV, AIFF)
