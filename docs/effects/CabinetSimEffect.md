# CabinetSimEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `cabsim`
**Display Name**: Cabinet Sim

Speaker cabinet simulation using FFT convolution with built-in synthetic impulse responses.

## Description

The CabinetSimEffect simulates different speaker cabinet types using FFT-based convolution. Unlike IRLoaderEffect which loads external WAV files, this effect generates synthetic impulse responses that approximate real cabinet characteristics.

Features:
- 5 cabinet types (1x12, 2x12, 4x12 British, 4x12 Modern, Direct)
- 4 microphone positions (Center, Edge, Back, Room)
- Low and high cut filters for tone shaping
- Adjustable dry/wet mix
- Fast FFT convolution (~46ms IR at 44.1kHz)

## Parameters

### Cabinet Configuration

#### cabinet (Cabinet)
- **Type**: Choice (5 options)
- **Default**: 0 (1x12 Vintage)
- **Description**: Speaker cabinet type
- **Options**:
  - **1x12 Vintage**: Small, warm, focused. Good for blues and light rock.
  - **2x12 Open**: Balanced, open back. Good for classic rock and funk.
  - **4x12 British**: Mid-focused, classic rock tone. British voicing.
  - **4x12 Modern**: Tight, aggressive. Modern metal and high-gain.
  - **Direct**: Bypass (no cabinet simulation).

#### mic (Mic Position)
- **Type**: Choice (4 options)
- **Default**: 0 (Center)
- **Description**: Microphone placement on speaker
- **Options**:
  - **Center**: Bright, direct, aggressive. Most high-frequency content.
  - **Edge**: Warm, balanced. Less harsh than center.
  - **Back**: Muffled, dark. Phase issues, distant sound.
  - **Room**: Ambient, distant. Reverberant character.

### Tone Shaping

#### lowCut (Low Cut)
- **Range**: 20.0 to 500.0 Hz
- **Default**: 80.0 Hz
- **Description**: Removes low frequencies below this point
- **Usage**: Higher values tighten the bass response. Use 80-100 Hz for modern tones, 60-80 Hz for vintage.

#### highCut (High Cut)
- **Range**: 2000.0 to 12000.0 Hz
- **Default**: 8000.0 Hz
- **Description**: Removes high frequencies above this point
- **Usage**: Lower values create darker, smoother tone. Use 6-8 kHz for smooth tones, 10-12 kHz for bright.

### Output

#### mix (Mix)
- **Range**: 0.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Balance between dry signal and cabinet simulation
- **Usage**: 100% for realistic amp tones. Lower values for subtle cabinet coloring.

## Cabinet Characteristics

### 1x12 Vintage
- **Resonance**: 100 Hz
- **Room Size**: 0.3 (small)
- **Brightness**: 0.4 (warm)
- **Low End**: 0.7 (moderate bass)
- **Character**: Warm, focused, good for blues and classic rock

### 2x12 Open
- **Resonance**: 90 Hz
- **Room Size**: 0.5 (medium)
- **Brightness**: 0.6 (balanced)
- **Low End**: 0.8 (good bass)
- **Character**: Open, airy, balanced frequency response

### 4x12 British
- **Resonance**: 80 Hz
- **Room Size**: 0.7 (large)
- **Brightness**: 0.5 (mid-focused)
- **Low End**: 0.9 (full bass)
- **Character**: Classic Marshall-style, mid-focused, tight

### 4x12 Modern
- **Resonance**: 70 Hz
- **Room Size**: 0.6 (large)
- **Brightness**: 0.7 (bright)
- **Low End**: 1.0 (maximum bass)
- **Character**: Tight, aggressive, modern high-gain

### Direct
- **Effect**: Bypassed (delta impulse)
- **Character**: No cabinet coloration, direct sound

## Microphone Position Effects

### Center
- **Brightness**: 1.2x (brightest)
- **Delay**: 0 samples (immediate)
- **Room Mix**: 0.1 (minimal ambience)
- **Character**: Bright, aggressive, most high-frequency content

### Edge
- **Brightness**: 0.8x (warmer)
- **Delay**: 2 samples
- **Room Mix**: 0.15 (slight ambience)
- **Character**: Balanced, less harsh, warmer than center

### Back
- **Brightness**: 0.5x (dark)
- **Delay**: 10 samples
- **Room Mix**: 0.25 (noticeable ambience)
- **Character**: Muffled, distant, phase-shifted

### Room
- **Brightness**: 0.6x (dark)
- **Delay**: 20 samples
- **Room Mix**: 0.5 (very ambient)
- **Character**: Distant, reverberant, room sound

## Signal Flow

1. **Input**
   - Receive input signal

2. **IR Selection**
   - Check if cabinet type or mic position changed
   - If changed, generate new synthetic IR
   - Prepare FFT convolver with new IR

3. **FFT Convolution**
   - Convolve input with cabinet IR
   - Efficient processing using overlap-add FFT

4. **Tone Shaping**
   - Apply low cut (high-pass filter)
   - Apply high cut (low-pass filter)

5. **Output**
   - Mix dry and wet signals
   - Output result

## Implementation Details

### Synthetic IR Generation

The effect generates impulse responses procedurally:

**Initial Transient** (2ms):
- Simulates speaker cone response
- Exponentially decaying envelope
- Frequency sweep from resonance+200Hz to resonance

**Cabinet Resonance** (10ms):
- Low-frequency resonance at cabinet frequency
- Exponential decay
- Scaled by low-end parameter

**Room Reflections** (up to 50ms):
- Diffuse reflections and reverberant tail
- Multiple harmonics of resonance frequency
- Scaled by room size parameter

**High Frequency Rolloff**:
- Low-pass filter simulating speaker bandwidth
- Cutoff frequency: 3000-8000 Hz based on brightness
- Speakers don't reproduce high frequencies well

**Normalization**:
- Normalize by energy (RMS) to 0.5
- Prevents clipping
- Maintains consistent level across cabinet types

### IR Length

- **Fixed Length**: 2048 samples
- **Duration**: ~46ms at 44.1kHz, ~42ms at 48kHz
- **Trade-off**: Short enough for low latency, long enough for cabinet character

### Filtering

**Low Cut** (High-Pass):
```
Simple one-pole filter
coefficient = 2π * frequency / sampleRate
output = input - lowCutState
lowCutState += coefficient * (input - lowCutState)
```

**High Cut** (Low-Pass):
```
Simple one-pole filter
coefficient = 2π * frequency / sampleRate
highCutState += coefficient * (input - highCutState)
output = highCutState
```

## Stereo Processing

The effect maintains true stereo:
- Separate FFT convolvers for left and right channels
- Same IR used for both channels
- Independent filter states for stereo

## Latency

The FFT convolver introduces latency:
- Typical: 128-256 samples (3-6ms at 44.1kHz)
- Depends on FFT block size
- Query with `getLatency()` method

## Usage Tips

### Modern Metal Rhythm
- Cabinet: 4x12 Modern
- Mic: Center
- Low Cut: 100 Hz
- High Cut: 8000 Hz
- Mix: 100%

### Classic Rock Lead
- Cabinet: 4x12 British
- Mic: Edge
- Low Cut: 80 Hz
- High Cut: 10000 Hz
- Mix: 100%

### Blues Clean
- Cabinet: 1x12 Vintage
- Mic: Edge
- Low Cut: 60 Hz
- High Cut: 7000 Hz
- Mix: 100%

### Smooth Jazz
- Cabinet: 2x12 Open
- Mic: Back
- Low Cut: 80 Hz
- High Cut: 6000 Hz
- Mix: 100%

### Room Ambience
- Cabinet: 4x12 British
- Mic: Room
- Low Cut: 60 Hz
- High Cut: 12000 Hz
- Mix: 50-70%

### Direct Recording (no cab)
- Cabinet: Direct
- Mix: 100%
- Use for recording direct to use with external IR plugins

## Technical Specifications

- **Processing**: 32-bit float
- **Convolution**: FFT-based (overlap-add)
- **IR Length**: 2048 samples (~46ms @ 44.1kHz)
- **Latency**: Variable (128-256 samples typical)
- **CPU Usage**: Medium (FFT processing)
- **Stereo**: True stereo (independent processing)

## Comparison with IRLoaderEffect

| Feature | CabinetSimEffect | IRLoaderEffect |
|---------|------------------|----------------|
| IR Source | Built-in synthetic | External WAV files |
| Quality | Good approximation | Authentic captures |
| Flexibility | Limited presets | Unlimited IRs |
| Setup | No files needed | Must load files |
| CPU | Medium | Medium-High (longer IRs) |
| Latency | Low (~46ms IR) | Variable (up to 2s IRs) |

## See Also

- IRLoaderEffect - Load external impulse response files
- CabinetSimulatorEffect - Detailed cabinet simulation with speaker/mic modeling
- AmpEffect - Complete amp simulation
