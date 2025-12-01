# TubePreampEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `tubepreamp`
**Display Name**: Tube Preamp

Vacuum tube preamplifier simulation with realistic tube characteristics, multiple tube types, and adjustable bias settings.

## Description

The TubePreampEffect simulates a vacuum tube preamplifier with two gain stages, offering detailed control over tube characteristics and circuit parameters. The simulation includes:
- 8 different tube types (12AX7, 12AT7, 12AU7, 12AY7, 5751, EF86, 6SL7, 6SN7)
- Adjustable bias (cold to hot)
- Cathode vs Fixed bias modes
- Plate voltage simulation
- Cathode bypass capacitor
- Voltage sag under load
- 3-band EQ (Bass, Mid, Treble)

## Parameters

### Tube Configuration

#### tubeType (Tube Type)
- **Range**: 0.0 to 7.0
- **Default**: 0.0 (12AX7/ECC83)
- **Description**: Select tube type
- **Values**:
  - 0 = 12AX7 / ECC83 (high gain, balanced harmonics)
  - 1 = 12AT7 (medium gain, cleaner)
  - 2 = 12AU7 (low gain, very clean)
  - 3 = 12AY7 (medium-low gain)
  - 4 = 5751 (lower gain 12AX7)
  - 5 = EF86 (pentode, high gain)
  - 6 = 6SL7 (octal, high gain)
  - 7 = 6SN7 (octal, medium gain)

#### bias (Bias)
- **Range**: -100.0 to +100.0 %
- **Default**: 0.0 %
- **Description**: Tube bias point
- **Usage**:
  - Negative (cold bias): Less current, harsher clipping, more odd harmonics
  - 0 (normal bias): Balanced operation
  - Positive (hot bias): More current, warmer, earlier saturation, more even harmonics

#### biasType (Bias Type)
- **Range**: 0.0 to 1.0
- **Default**: 0.0 (Cathode)
- **Description**: Bias configuration
- **Values**:
  - 0 = Cathode (self-biasing, more compression and sag)
  - 1 = Fixed (tighter, crisper response)

#### plateVoltage (Plate Voltage)
- **Range**: 50.0 to 150.0 %
- **Default**: 100.0 %
- **Description**: B+ plate voltage
- **Usage**:
  - Lower voltage: Earlier breakup, less headroom, warmer
  - Higher voltage: More headroom, cleaner at high levels

#### cathodeBypass (Cathode Bypass)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Cathode bypass capacitor amount
- **Usage**:
  - 0%: No bypass - less gain, tighter bass, more articulate
  - 100%: Full bypass - higher gain, fuller bass
- **Implementation**: High-pass filter from 80-380 Hz based on bypass amount

### Gain Stages

#### stage1Gain (Stage 1 Gain)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: First gain stage drive
- **Usage**: Primary gain control, affects overall saturation character

#### stage2Gain (Stage 2 Gain)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Second gain stage drive
- **Usage**: Adds additional saturation and harmonic complexity

### Power Supply

#### sag (Sag)
- **Range**: 0.0 to 100.0 %
- **Default**: 30.0 %
- **Description**: Power supply sag - creates compression and feel
- **Usage**: Higher values create more dynamic compression and "give"
- **Time Constants**:
  - Attack: 10 ms
  - Release: 100 ms

### Tone Stack

#### bass (Bass)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Low frequency response (100 Hz shelf)
- **Implementation**: Low shelf filter, -12 dB to +12 dB

#### mid (Mid)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Mid frequency response (500 Hz peak)
- **Implementation**: Peaking filter, Q=1.0, -12 dB to +12 dB

#### treble (Treble)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: High frequency response (2 kHz shelf)
- **Implementation**: High shelf filter, -12 dB to +12 dB

### Output

#### output (Output)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Output level
- **Implementation**: Linear gain 0.0 to 2.0

## Tube Type Characteristics

Each tube type has unique characteristics stored in the TubeType enum:

- **Normalized Gain**: Relative gain factor
- **Even Harmonics**: Amount of 2nd, 4th harmonics
- **Odd Harmonics**: Amount of 3rd, 5th harmonics
- **Compression**: Dynamic compression amount
- **Bass Response**: Low frequency character
- **Treble Response**: High frequency character

## Signal Flow

1. **Input Stage**
   - Apply stage 1 gain (0.5 + drive * 4.0)
   - Apply tube gain factor
   - Cathode bypass filtering (if < 100%)

2. **Tube Stage 1**
   - Bias offset applied
   - Plate voltage headroom scaling
   - Asymmetric tube saturation curve
   - Cathode bias compression (if enabled)
   - Sag calculation and application

3. **Inter-Stage Coupling**
   - Apply stage 2 gain (0.5 + drive * 4.0)
   - Scale by tube gain and 0.7 attenuation
   - AC coupling (simulates capacitor)

4. **Tube Stage 2**
   - Same processing as stage 1
   - Additional saturation and harmonics

5. **Tone Stack**
   - Bass shelf (100 Hz)
   - Mid peak (500 Hz)
   - Treble shelf (2000 Hz)
   - Apply tube frequency response

6. **Output Stage**
   - DC blocking filter
   - Output level scaling
   - Bass response compensation
   - Soft output clipping

## Implementation Details

### Tube Saturation Algorithm

The tube transfer function uses asymmetric soft clipping:

**Positive Half** (grid conduction):
```
if x < 1.0:
  output = x - evenHarm * 0.25 * x^2 + oddHarm * 0.1 * x^3
else:
  output = 1.0 - (1.0 - evenHarm * 0.5) / (x + 1.0)
```

**Negative Half** (cutoff):
```
if x < 1.0:
  output = -x - evenHarm * 0.15 * x^2 - oddHarm * 0.15 * x^3
else:
  output = -1.0 + (1.0 - oddHarm * 0.3) / (x + 0.5)
```

### Bias Effects

**Bias Offset**:
```
biasedInput = input + bias * 0.2
```

**Plate Voltage Headroom**:
```
headroom = 0.5 + plateVoltage * 0.5
normalizedInput = biasedInput / headroom
```

**Cathode Bias Compression**:
```
compressionFactor = 1.0 / (1.0 + compression * |output| * 0.5)
output *= compressionFactor
```

### Sag Simulation

Tracks signal envelope with asymmetric attack/release:
```
if signalLevel > sagLevel:
  sagLevel = sagLevel * attack + signalLevel * (1 - attack)
else:
  sagLevel = sagLevel * release + signalLevel * (1 - release)

sagFactor = 1.0 - sagAmount * 0.3 * min(sagLevel, 1.0)
output *= sagFactor
```

## Stereo Processing

Processes stereo signals as mono (typical for guitar preamps). Input is mixed to mono, processed, then copied to both output channels.

## Usage Tips

### Clean Jazz Tone
- Tube Type: 12AU7 (low gain)
- Bias: +20% (hot, warm)
- Bias Type: Cathode
- Plate Voltage: 120%
- Stage 1/2: 30% / 20%
- Cathode Bypass: 70%
- Tone: 60/50/50

### British Crunch
- Tube Type: 12AX7
- Bias: 0% (normal)
- Bias Type: Fixed
- Plate Voltage: 100%
- Stage 1/2: 60% / 50%
- Cathode Bypass: 80%
- Sag: 40%
- Tone: 60/40/60

### High-Gain Lead
- Tube Type: 12AX7
- Bias: -10% (slightly cold, more edge)
- Bias Type: Fixed
- Plate Voltage: 100%
- Stage 1/2: 80% / 70%
- Cathode Bypass: 100%
- Sag: 20% (tight)
- Tone: 50/60/70

### Vintage Blues
- Tube Type: 5751 (lower gain 12AX7)
- Bias: +30% (hot, compressed)
- Bias Type: Cathode
- Plate Voltage: 90%
- Stage 1/2: 70% / 60%
- Cathode Bypass: 50%
- Sag: 60%
- Tone: 70/50/50

### Pentode Aggression
- Tube Type: EF86
- Bias: -20% (cold, aggressive)
- Bias Type: Fixed
- Plate Voltage: 110%
- Stage 1/2: 70% / 60%
- Cathode Bypass: 100%
- Sag: 30%
- Tone: 50/70/60

## Technical Specifications

- **Processing**: 32-bit float
- **Filter Types**: Biquad IIR (2nd order)
- **Latency**: 0 samples (real-time)
- **CPU Usage**: Low-Medium
- **Stereo**: Mono processing (summed to mono, duplicated to stereo)

## See Also

- TubePowerAmpEffect - Power amplifier section
- AmpEffect - Complete amp simulation
- TubeType enum - Tube characteristic definitions
