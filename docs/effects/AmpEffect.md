# AmpEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `amp`
**Display Name**: Amp

Parametric guitar amplifier simulation that models a complete amp signal chain with multiple gain stages, tone controls, and power amp characteristics.

## Description

The AmpEffect provides a comprehensive guitar amplifier simulation featuring:
- Input stage with bright switch
- Three-stage tube preamp with progressive saturation
- 3-band tone stack (Bass, Mid, Treble)
- Presence control
- Power amp simulation with sag and compression
- Master volume control

The effect uses asymmetric soft clipping to emulate triode tube characteristics, with separate modeling for preamp and power amp stages.

## Parameters

### Input Section

#### inputGain (Input)
- **Range**: -12.0 to 12.0 dB
- **Default**: 0.0 dB
- **Description**: Input level before preamp. Boost hot pickups, cut for active guitars.
- **Usage**: Use positive values to drive the preamp harder for more saturation. Negative values clean up the tone.

#### bright (Bright)
- **Type**: Boolean
- **Default**: false
- **Description**: Adds high frequency sparkle at low gain settings. Classic amp feature.
- **Usage**: Enable for cleaner tones to add presence and clarity. Disable for high-gain tones to prevent harshness.

### Gain Section

#### preampGain (Gain)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Preamp drive amount. Higher values create more tube-like saturation and harmonics.
- **Usage**:
  - 0-30%: Clean to edge-of-breakup tones
  - 30-60%: Classic rock/blues overdrive
  - 60-100%: High-gain distortion

### Tone Stack

#### bass (Bass)
- **Range**: 0.0 to 10.0
- **Default**: 5.0
- **Description**: Low frequency tone control (100 Hz low shelf). Adds thump and warmth, reduce for tighter response.
- **Implementation**: Low shelf filter at 100 Hz, gain range -12 dB to +12 dB

#### mid (Mid)
- **Range**: 0.0 to 10.0
- **Default**: 5.0
- **Description**: Midrange tone control (800 Hz peak). Cut for scooped metal tone, boost for punchy rock sound.
- **Implementation**: Peaking filter at 800 Hz, Q=1.0, gain range -12 dB to +12 dB

#### treble (Treble)
- **Range**: 0.0 to 10.0
- **Default**: 5.0
- **Description**: High frequency tone control (3 kHz high shelf). Adds bite and clarity, reduce for warmer tone.
- **Implementation**: High shelf filter at 3000 Hz, gain range -12 dB to +12 dB

#### presence (Presence)
- **Range**: 0.0 to 10.0
- **Default**: 5.0
- **Description**: Upper harmonic content from power amp (4.5 kHz high shelf). Adds edge and articulation.
- **Implementation**: High shelf filter at 4500 Hz, gain range -7.5 dB to +7.5 dB

### Power Amp Section

#### sag (Sag)
- **Range**: 0.0 to 100.0 %
- **Default**: 30.0 %
- **Description**: Power supply compression feel. Adds touch-sensitive dynamics like vintage amps.
- **Usage**: Higher values create more compression and "give" under pick attack. Reduces up to 30% of output level under load.
- **Time Constants**:
  - Attack: 10 ms
  - Release: 100 ms

### Output Section

#### master (Master)
- **Range**: -60.0 to 6.0 dB
- **Default**: -6.0 dB
- **Description**: Overall output volume. Set preamp high and master low for classic cranked tone at low volume.

## Signal Flow

1. **Input Stage**
   - Apply input gain
   - 20 Hz high-pass filter (subsonic removal)
   - Optional bright switch (2 kHz high shelf, +6 dB)

2. **Preamp Stages** (Three cascaded stages)
   - Stage 1: Gain multiplier based on preamp gain (1.0 + gain*10), asymmetry 0.8
   - Stage 2: Gain multiplier (1.0 + gain*5), asymmetry 0.7
   - Stage 3: Gain multiplier (1.0 + gain*3), asymmetry 0.6
   - Each stage uses asymmetric tube saturation

3. **Tone Stack**
   - Bass filter (100 Hz low shelf)
   - Mid filter (800 Hz peak)
   - Treble filter (3 kHz high shelf)

4. **Power Amp Stage**
   - Presence filter (4.5 kHz high shelf)
   - Power amp saturation (softer, more symmetric)
   - Even harmonic generation (5% of x^2)
   - Sag simulation (dynamic gain reduction)

5. **Output Stage**
   - Master volume
   - DC blocking filter (0.995 coefficient)

## Implementation Details

### Tube Saturation Algorithm

**Preamp Stage** (asymmetric):
```
Positive half: output = tanh(x * 1.5) * 0.9
Negative half: output = tanh(x * (1.5 + asymmetry)) * (0.9 - asymmetry * 0.1)
```

**Power Amp Stage** (symmetric):
```
Soft clip: output = x / (1 + |x| * 0.5)
Even harmonics: output += x * x * 0.05 * sign(x)
```

### Sag Simulation

The sag effect models power supply compression:
- Tracks signal level with 10 ms attack, 100 ms release
- Reduces available gain by up to 30% (sag * 0.3) based on signal level
- Creates touch-sensitive dynamics and compression

### DC Blocking

A first-order high-pass filter removes DC offset:
```
coefficient = 0.995
output = input - dcState
dcState = input - output * coefficient
```

## Stereo Processing

The effect processes stereo signals independently through duplicate filter chains for left and right channels, maintaining stereo imaging while applying identical processing.

## Usage Tips

### Clean Tones
- Input Gain: 0 dB
- Preamp Gain: 10-30%
- Bright: On
- Tone Stack: Balanced (5/5/5)
- Sag: 20-30%
- Master: -6 dB

### Classic Rock/Blues
- Input Gain: +3 dB
- Preamp Gain: 40-60%
- Bright: Off
- Bass: 6, Mid: 4, Treble: 6
- Presence: 6
- Sag: 40%
- Master: -10 dB

### High-Gain Metal
- Input Gain: 0 dB (use pedal before amp)
- Preamp Gain: 70-90%
- Bright: Off
- Bass: 6, Mid: 3, Treble: 7
- Presence: 7
- Sag: 20% (tighter response)
- Master: -12 dB

### Cranked Vintage Tone (Low Volume)
- Input Gain: +6 dB
- Preamp Gain: 80%
- Bright: Off
- Tone Stack: 5/5/5
- Sag: 50% (more compression)
- Master: -24 dB (low volume)

## Technical Specifications

- **Processing**: 32-bit float
- **Filter Types**: Biquad IIR (2nd order)
- **Latency**: 0 samples (real-time)
- **CPU Usage**: Low (simple filters and waveshaping)
- **Stereo**: True stereo (independent processing)

## See Also

- TubePreampEffect - Detailed preamp simulation with tube types
- TubePowerAmpEffect - Detailed power amp simulation
- CabinetSimEffect - Speaker cabinet simulation
