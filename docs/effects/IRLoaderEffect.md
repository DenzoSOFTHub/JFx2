# IRLoaderEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `irloader`
**Display Name**: IR Loader

Load and apply impulse responses (IRs) from WAV files using FFT convolution for authentic speaker cabinet simulation and acoustic spaces.

## Description

The IRLoaderEffect loads WAV files as impulse responses and applies them to the audio signal using efficient FFT-based convolution. This is the standard method for cabinet simulation, room reverb, and other convolution-based effects.

Features:
- Load mono or stereo WAV files
- Support for 8/16/24/32-bit PCM and 32-bit float
- Automatic sample rate conversion
- Adjustable mix (dry/wet balance)
- Pre-delay for timing adjustment
- Output gain control
- Low/High cut filters for tone shaping
- IR length trimming for CPU optimization
- True stereo convolution

Maximum IR length: 96,000 samples (~2 seconds at 48kHz)

## Parameters

### Mix and Level

#### mix (Mix)
- **Range**: 0.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Balance between dry signal and IR-processed signal
- **Usage**:
  - 100%: Fully processed (typical for cabinet IRs)
  - 50%: Equal blend (parallel processing)
  - 20-30%: Subtle ambience (room IRs)

#### gain (Gain)
- **Range**: -24.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Output level adjustment for the IR signal
- **Usage**: Compensate for level differences between IRs or boost/cut as needed

### Timing

#### predelay (Pre-Delay)
- **Range**: 0.0 to 50.0 ms
- **Default**: 0.0 ms
- **Description**: Delay before the IR starts
- **Usage**:
  - 0ms: Direct, immediate response
  - 5-15ms: Slight space/distance
  - 20-50ms: Room reflections, slapback effect

### Tone Shaping

#### lowcut (Low Cut)
- **Range**: 20.0 to 500.0 Hz
- **Default**: 20.0 Hz
- **Description**: High-pass filter, removes low frequencies
- **Usage**:
  - 20-40 Hz: Remove subsonic rumble only
  - 60-80 Hz: Tighten bass (typical for modern metal)
  - 100+ Hz: Thin sound, remove bass (special effects)

#### highcut (High Cut)
- **Range**: 2000.0 to 20000.0 Hz
- **Default**: 20000.0 Hz (no filtering)
- **Description**: Low-pass filter, removes high frequencies
- **Usage**:
  - 12-20 kHz: Natural (no filtering)
  - 8-10 kHz: Smooth, darker tone
  - 4-6 kHz: Very dark, muffled (phone/radio effect)

### Optimization

#### trim (IR Length)
- **Range**: 10.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Use only a portion of the IR
- **Usage**:
  - 100%: Full IR (best quality)
  - 50-75%: Reduced CPU, shorter reverb tail
  - 25-50%: Minimal CPU, direct sound only
  - Lower values reduce latency and CPU usage

## Loading IR Files

### File Format Support

**Supported Formats**:
- WAV files (.wav)
- Mono or Stereo
- 8-bit, 16-bit, 24-bit, 32-bit PCM
- 32-bit float
- Any sample rate (automatically converted)

**Not Supported**:
- Compressed formats (MP3, AAC, OGG)
- Multi-channel (3+ channels)

### Using the API

```java
IRLoaderEffect ir = new IRLoaderEffect();
boolean success = ir.loadIR("/path/to/impulse.wav");

if (success) {
    System.out.println("Loaded: " + ir.getFilePath());
    System.out.println("Length: " + ir.getIRDuration() + "s");
    System.out.println("Stereo: " + ir.isIRStereo());
} else {
    System.err.println("Failed to load IR");
}
```

### IR File Information

Query loaded IR properties:

```java
boolean loaded = ir.isIRLoaded();
String path = ir.getFilePath();
int samples = ir.getIRLength();
float duration = ir.getIRDuration();  // in seconds
boolean stereo = ir.isIRStereo();
int latency = ir.getLatency();  // in samples
```

## Signal Flow

1. **IR Loading**
   - Read WAV file
   - Convert to PCM if necessary
   - Parse samples (handle different bit depths)
   - Resample to effect sample rate (if needed)
   - Normalize to prevent clipping

2. **IR Preparation**
   - Apply trim percentage
   - Add pre-delay samples (zeros before IR)
   - Prepare FFT convolvers (L and R)

3. **Real-Time Processing**
   - FFT convolution (overlap-add method)
   - Apply low cut filter (high-pass)
   - Apply high cut filter (low-pass)
   - Apply gain
   - Mix with dry signal

4. **Output**
   - Output mixed result

## Implementation Details

### FFT Convolution

Uses overlap-add FFT convolution for efficiency:

**Advantages**:
- Very efficient for long IRs
- O(N log N) instead of O(N²)
- Constant CPU regardless of IR length

**Process**:
1. Split IR into blocks
2. FFT each block
3. Overlap-add convolution
4. IFFT back to time domain

### Automatic Resampling

If IR sample rate ≠ effect sample rate:

```
Resampling uses linear interpolation:
  ratio = irSampleRate / effectSampleRate
  newLength = irLength / ratio

For each output sample:
  srcPos = outputIndex * ratio
  srcIdx = floor(srcPos)
  frac = srcPos - srcIdx
  output = input[srcIdx] * (1-frac) + input[srcIdx+1] * frac
```

**Quality**: Good for most purposes. For best quality, use IRs at your target sample rate.

### IR Normalization

IRs are normalized to prevent clipping:

```
Find peak sample: peak = max(|all samples|)
If peak > 0.001:
  scale = 0.5 / peak  (normalize to 0.5, leave headroom)
  Multiply all samples by scale
```

### Sample Format Conversion

**16-bit PCM**:
```
sample = int16 / 32768.0
```

**24-bit PCM**:
```
sample = int24 / 8388608.0
```

**32-bit PCM**:
```
sample = int32 / 2147483648.0
```

**32-bit Float**:
```
sample = float (direct)
```

### Stereo IR Handling

**Mono IR**: Applied to both channels identically
**Stereo IR**: Left channel → Left output, Right channel → Right output

If input is mono and IR is stereo:
- Input applied to both IR channels
- Creates stereo width from mono input

## Latency

FFT convolution introduces latency:

**Typical Latency**: 128-256 samples (3-6ms at 44.1kHz)
- Depends on FFT block size
- Larger blocks = more latency but more efficient
- Query with `getLatency()` method

**Latency Compensation**:
- Report latency to host DAW
- DAW can compensate automatically
- Important for multi-track recording

## Stereo Processing

**True Stereo**:
- Separate convolvers for L and R channels
- Maintains stereo imaging
- Stereo IRs create stereo width

**Mono-to-Stereo**:
- Mono input + Stereo IR = Stereo output
- Useful for adding width

## IR Types and Usage

### Cabinet IRs

**Typical Length**: 100-500ms (4,410-22,050 samples @ 44.1kHz)

**Settings**:
- Mix: 100%
- Gain: 0 dB (adjust as needed)
- Low Cut: 80-100 Hz
- High Cut: 8000-12000 Hz
- Trim: 50-100%

**Tips**:
- Try different mic positions
- Blend multiple cabinet IRs (use multiple IR Loader instances)
- Low cut tightens for metal
- High cut smooths harsh IRs

### Room/Reverb IRs

**Typical Length**: 1-2 seconds (44,100-88,200 samples @ 44.1kHz)

**Settings**:
- Mix: 20-40% (parallel blend)
- Gain: -6 to 0 dB
- Low Cut: 100-200 Hz (remove mud)
- High Cut: 8000-12000 Hz
- Pre-Delay: 10-30ms (separation)
- Trim: 100% (use full reverb tail)

**Tips**:
- Lower mix for subtle ambience
- Pre-delay separates direct from reverb
- Low cut prevents muddy reverb

### Acoustic Space IRs

**Typical Length**: 500ms-2s

**Settings**:
- Mix: 30-60%
- Gain: -3 to 0 dB
- Pre-Delay: 5-20ms
- Trim: 75-100%

**Tips**:
- Captures real rooms, halls, churches
- Great for adding realistic space
- Combine with cabinet IRs

### Special Effect IRs

**Examples**:
- Telephone/radio simulation
- Amp spring reverb
- Vintage equipment
- Creative processing

**Settings**: Varies by effect
- Experiment with mix, gain, filters
- Trim may help reduce unwanted tail

## Usage Tips

### Getting Good Results

1. **Choose Quality IRs**: High-quality captures make a big difference
2. **Proper Gain Staging**: Adjust gain to prevent clipping
3. **Tone Shaping**: Use filters to tailor the sound
4. **Pre-Delay**: Add space if needed
5. **Trim for Performance**: Reduce CPU with shorter IRs

### Cabinet IR Best Practices

**Placement in Chain**:
```
Guitar → Amp Sim → IR Loader (Cabinet) → EQ/Compression → Reverb
```

**Multiple Mics**:
- Load multiple IR Loaders
- Different IRs in parallel
- Pan left/right for width
- Adjust levels to blend

**Matching to Amp**:
- Bright amp + dark IR = balanced
- Dark amp + bright IR = balanced
- Experiment with combinations

### Room IR Best Practices

**Parallel Processing**:
- Lower mix (20-40%)
- Preserve dry signal attack
- Add space without washing out

**Cascading**:
- Cabinet IR → Room IR
- Direct sound + space

**Genre Specific**:
- Metal: Tight, short (trim 50%, low cut 100 Hz)
- Blues: Natural, medium (trim 75%, low cut 60 Hz)
- Jazz: Spacious, long (trim 100%, mix 40%)

## Common IR Sources

### Free IRs

- **GuitarHacks**: Free cabinet IRs (popular)
- **Kalthallen Cabs**: Free Impulse Responses
- **Redwirez** (free samples): Professional IR packs (paid with free samples)
- **God's Cab**: Free multi-mic cabinet IR

### Commercial IRs

- **OwnHammer**: Professional cabinet IRs
- **Celestion**: Official speaker IRs
- **3 Sigma Audio**: Modern, high-quality IRs
- **Two Notes**: Official cabinet captures

### Making Your Own

**Equipment Needed**:
- Audio interface (good preamps)
- Microphone (SM57 or similar)
- Speaker cabinet
- Sine sweep generator or capture software

**Process**:
1. Generate sine sweep or impulse
2. Play through cabinet
3. Record with microphone
4. Deconvolve to extract IR
5. Export as WAV file

**Software**:
- REW (Room EQ Wizard) - Free
- Voxengo Deconvolver - Free
- Altiverb XL - Commercial

## Troubleshooting

### IR Won't Load

**Check**:
- File format (must be WAV)
- File path is correct
- File isn't corrupted
- File permissions

**Try**:
- Open in audio editor (verify it's valid)
- Re-export from audio editor
- Check sample rate and bit depth

### Distortion or Clipping

**Causes**:
- IR too loud
- Gain too high
- Input signal too hot

**Solutions**:
- Reduce gain (-3 to -6 dB)
- Lower input level before IR
- Reload IR (re-normalizes)

### Sounds Muffled or Dark

**Causes**:
- High cut too low
- Dark IR
- Wrong IR type

**Solutions**:
- Increase high cut (10-12 kHz)
- Try different IR
- Boost highs after IR

### Too Much Bass/Muddiness

**Solutions**:
- Increase low cut (80-120 Hz)
- Use different IR (tighter)
- EQ after IR

### High CPU Usage

**Solutions**:
- Reduce trim (use shorter IR)
- Increase audio buffer size
- Use shorter IRs (cabinets don't need long IRs)

## Technical Specifications

- **Processing**: 32-bit float
- **Convolution**: FFT-based (overlap-add)
- **Maximum IR Length**: 96,000 samples (~2s @ 48kHz)
- **Latency**: Variable (typically 128-256 samples)
- **CPU Usage**: Medium-High (depends on IR length and trim)
- **Stereo**: True stereo (independent L/R convolution)
- **Sample Format Support**: 8/16/24/32-bit PCM, 32-bit float

## Performance Optimization

### CPU Usage Factors

1. **IR Length**: Longer IRs = more CPU
2. **Trim Setting**: Lower trim = less CPU
3. **Sample Rate**: Higher rate = more CPU
4. **Stereo vs Mono**: Stereo = 2x CPU

### Optimization Strategies

1. **Trim Cabinets**: 50% is often enough (500ms → 250ms)
2. **Mono When Possible**: Use mono IRs if stereo not needed
3. **Appropriate Length**: Don't use 2s IRs for cabinets
4. **Buffer Size**: Larger buffers = more efficient FFT

## See Also

- CabinetSimEffect - Built-in synthetic cabinet IRs
- CabinetSimulatorEffect - Parametric cabinet modeling
- FFTConvolver class - Convolution implementation
- AmpEffect - Amp simulation to use before IR
