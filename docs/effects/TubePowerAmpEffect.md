# TubePowerAmpEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `tubepoweramp`
**Display Name**: Tube Power Amp

Vacuum tube power amplifier simulation with Class A/AB operation, push-pull and single-ended configurations, and output transformer modeling.

## Description

The TubePowerAmpEffect simulates the power amplifier section of a tube guitar amplifier, including:
- 8 power tube types (EL34, 6L6, EL84, 6V6, KT88, KT66, 6550, 5881)
- Class A and Class AB operation modes
- Push-pull and single-ended topologies
- Adjustable bias (cold to hot)
- Power supply sag simulation
- Output transformer saturation
- Negative feedback control
- Presence and Resonance controls

## Parameters

### Tube Configuration

#### tubeType (Tube Type)
- **Range**: 0.0 to 7.0
- **Default**: 0.0 (EL34)
- **Description**: Select power tube type
- **Values**:
  - 0 = EL34 (British, mid-focused)
  - 1 = 6L6 (American, balanced)
  - 2 = EL84 (British, punchy)
  - 3 = 6V6 (American, warm)
  - 4 = KT88 (High power, tight)
  - 5 = KT66 (British, warm)
  - 6 = 6550 (High power, American)
  - 7 = 5881 (Rugged 6L6)

#### classType (Class)
- **Range**: 0.0 to 1.0
- **Default**: 1.0 (Class AB)
- **Description**: Operating class
- **Values**:
  - 0 = Class A (warm, compressed, always conducting)
  - 1 = Class AB (tight, powerful, crossover distortion)

#### topology (Topology)
- **Range**: 0.0 to 1.0
- **Default**: 0.0 (Push-Pull)
- **Description**: Tube configuration
- **Values**:
  - 0 = Push-Pull (tight, powerful, cancels even harmonics)
  - 1 = Single-Ended (warm, more 2nd harmonics, asymmetric)

#### bias (Bias)
- **Range**: -100.0 to +100.0 %
- **Default**: 0.0 %
- **Description**: Tube bias point
- **Usage**:
  - Cold (negative): Less current, more crossover distortion (Class AB), tighter
  - Normal (0): Balanced operation
  - Hot (positive): More current, warmer, earlier saturation

### Drive and Power

#### drive (Drive)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Signal level driving the power tubes
- **Implementation**: Scales input by (value/100 * 3.0 + 0.5)

#### sag (Sag)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Power supply sag - creates compression and 'feel'
- **Usage**: Higher values create more compression under load
- **Time Constants**:
  - Attack: 5 ms
  - Release: 50 ms

#### transformer (Transformer)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Output transformer saturation
- **Usage**: Adds warmth and compression, limits bass response

### Negative Feedback

#### feedback (Neg Feedback)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Negative feedback amount
- **Usage**:
  - 0%: No feedback - loose, raw, higher gain
  - 100%: Full feedback - tight, clean, lower gain

#### presence (Presence)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: High frequency emphasis in feedback loop
- **Implementation**: High shelf at 4 kHz, 0 to +12 dB
- **Usage**: Works best with moderate feedback settings

#### resonance (Resonance)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Low frequency emphasis (depth control)
- **Implementation**: Low shelf at 90 Hz, 0 to +10 dB
- **Usage**: Adds low-end thump and body

### Output

#### master (Master)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Output level
- **Implementation**: Linear gain 0.0 to 2.0

## Power Tube Type Characteristics

Each tube type has unique characteristics stored in the PowerTubeType enum:

- **Headroom**: Available voltage swing
- **Even Harmonics**: 2nd, 4th harmonic content
- **Odd Harmonics**: 3rd, 5th harmonic content
- **Compression**: Dynamic compression amount
- **Bass Tightness**: Low frequency response
- **Mid Emphasis**: Midrange character
- **Sag Amount**: Power supply compression sensitivity

## Signal Flow

1. **Input Stage**
   - Apply drive gain
   - Scale by tube characteristics

2. **Power Supply Sag**
   - Calculate load (signal level)
   - Discharge power supply capacitor (slow)
   - Reduce available voltage based on load

3. **Negative Feedback**
   - Subtract feedback signal from input
   - Reduces gain and tightens response

4. **Power Tube Stage**
   - Choose topology (Push-Pull or Single-Ended)
   - Apply bias offset
   - Scale by plate voltage with sag
   - Process through tube transfer function
   - Apply Class A/AB compression

5. **Output Transformer**
   - Core saturation (tanh)
   - Bass high-pass filtering (40-100 Hz)
   - Adds warmth and limits bass

6. **Presence/Resonance**
   - Apply frequency shaping in feedback loop
   - Mix back into signal

7. **Output Stage**
   - DC blocking
   - Apply master volume
   - Apply tube mid emphasis
   - Soft output limiting

## Implementation Details

### Single-Ended Topology

Asymmetric clipping with one tube:

**Positive Swing** (soft saturation):
```
if x < 1.5:
  output = x - evenHarm * 0.4 * x^2
else:
  output = 1.5 - evenHarm * 0.4 * 1.5^2 + (1.0 - 1.0/(x - 1.5 + 1.0)) * 0.3
```

**Negative Swing** (earlier cutoff):
```
if x < 1.0:
  output = -x + evenHarm * 0.2 * x^2
else:
  output = -1.0 + 0.2 / (x + 0.5)
```

### Push-Pull Topology

Two tubes in complementary configuration:

1. Split signal into positive and negative halves
2. Process each half through separate tube
3. Combine: `output = tubeA - tubeB`
4. Cancels even harmonics (more linear)

**Crossover Distortion** (Class AB only):
```
crossoverRegion = 0.1 + bias * 0.05
if |x| < crossoverRegion:
  crossoverAmount = 1.0 - |x| / crossoverRegion
  output *= (1.0 - crossoverAmount * 0.2 * (1.0 - bias * 0.5))
```

### Power Supply Sag

Simulates capacitor discharge under load:

```
load = |sample|
sagCapacitor = sagCapacitor * 0.9999 + load * 0.0001

sagVoltage = 1.0 - sagAmount * min(sagCapacitor, 1.0) * 0.4
```

### Output Transformer

**Core Saturation**:
```
transformerCore = transformerCore * 0.9 + input * 0.1
coreSaturation = tanh(transformerCore * (0.5 + saturation))
output = input * (1.0 - saturation * 0.5) + coreSaturation * saturation * 0.5
```

**Bass High-Pass** (transformer can't pass DC):
```
cutoffHz = 40 + (1.0 - bassTightness) * 60
Apply first-order high-pass filter
```

### Negative Feedback

```
feedbackSignal = lastOutput * feedback * 0.5
sample -= feedbackSignal
```

Feedback reduces gain, tightens bass, and linearizes the response.

## Stereo Processing

Processes stereo signals as mono (typical for power amps). Input is mixed to mono, processed, then copied to both output channels.

## Usage Tips

### Clean Jazz (Class A, Single-Ended)
- Tube Type: 6V6 or EL84
- Class: A
- Topology: Single-Ended
- Bias: +30% (hot, warm)
- Drive: 30%
- Sag: 60% (more compression)
- Transformer: 40%
- Feedback: 70% (tight, clean)
- Master: 50%

### British Rock (Class AB, Push-Pull)
- Tube Type: EL34
- Class: AB
- Topology: Push-Pull
- Bias: 0% (normal)
- Drive: 60%
- Sag: 50%
- Transformer: 60%
- Feedback: 40%
- Presence: 60%
- Master: 50%

### American Clean (6L6, Class AB)
- Tube Type: 6L6
- Class: AB
- Topology: Push-Pull
- Bias: +10% (slightly hot)
- Drive: 40%
- Sag: 40%
- Transformer: 40%
- Feedback: 80% (very tight)
- Resonance: 60%
- Master: 60%

### Modern High-Gain (KT88, Class AB)
- Tube Type: KT88
- Class: AB
- Topology: Push-Pull
- Bias: -20% (cold, tight)
- Drive: 80%
- Sag: 20% (minimal sag)
- Transformer: 30%
- Feedback: 30%
- Presence: 70%
- Master: 40%

### Vintage Blues (Class A, Single-Ended)
- Tube Type: 6V6
- Class: A
- Topology: Single-Ended
- Bias: +40% (hot)
- Drive: 70%
- Sag: 70%
- Transformer: 70%
- Feedback: 20%
- Master: 50%

### Metal Tightness (6550, Class AB)
- Tube Type: 6550
- Class: AB
- Topology: Push-Pull
- Bias: -30% (very cold)
- Drive: 70%
- Sag: 10%
- Transformer: 20%
- Feedback: 50%
- Presence: 80%
- Resonance: 30%
- Master: 50%

## Technical Specifications

- **Processing**: 32-bit float
- **Filter Types**: Biquad IIR (2nd order)
- **Latency**: 0 samples (real-time)
- **CPU Usage**: Low-Medium
- **Stereo**: Mono processing (summed to mono, duplicated to stereo)

## Class A vs Class AB

**Class A**:
- Tubes always conducting
- No crossover distortion
- Warmer, more compressed
- Lower efficiency
- More even harmonics

**Class AB**:
- Tubes conduct for > 180° but < 360°
- Crossover distortion when both tubes near cutoff
- Tighter, more powerful
- Higher efficiency
- More odd harmonics

## Push-Pull vs Single-Ended

**Push-Pull**:
- Two tubes in complementary configuration
- Cancels even harmonics
- More power, tighter bass
- More symmetric clipping
- Typical for most guitar amps

**Single-Ended**:
- One tube (or parallel tubes acting as one)
- Retains even harmonics (2nd, 4th)
- Less power, looser bass
- Asymmetric clipping
- Typical for small vintage amps

## See Also

- TubePreampEffect - Preamp section
- AmpEffect - Complete amp simulation
- PowerTubeType enum - Power tube characteristic definitions
