# CabinetSimulatorEffect

## Overview

**Category**: Amp Simulation
**Effect ID**: `cabinetsim`
**Display Name**: Cabinet Simulator

Advanced guitar cabinet simulation with detailed speaker and microphone modeling, supporting dual microphone setups.

## Description

The CabinetSimulatorEffect provides comprehensive speaker cabinet simulation with:
- 12 speaker types (Celestion, Jensen, Eminence models)
- 6 cabinet configurations (1x10, 1x12, 2x10, 2x12, 4x10, 4x12)
- 11 microphone types (SM57, MD421, R121, e906, etc.)
- Dual microphone support with independent positioning
- Detailed mic placement controls (position, distance, angle)
- Speaker resonance and compression modeling
- Cabinet body resonance

Unlike CabinetSimEffect which uses synthetic IRs, this effect models the physics of speakers, cabinets, and microphones algorithmically.

## Parameters

### Cabinet Configuration

#### speakerType (Speaker Type)
- **Range**: 0.0 to 11.0
- **Default**: 1.0 (Greenback)
- **Description**: Speaker model selection
- **Values**:
  - 0 = Celestion Vintage 30 (bright, tight, modern)
  - 1 = Celestion Greenback (warm, mid-focused, classic)
  - 2 = Celestion G12H (balanced, versatile)
  - 3 = Celestion Blue (bright, chimey, vintage)
  - 4 = Jensen P12R (clean, American)
  - 5 = Jensen C12N (smooth, vintage American)
  - 6 = Eminence Cannabis Rex (hemp cone, balanced)
  - 7 = Eminence Governor (tight, modern)
  - 8 = Celestion G12T-75 (bright, aggressive)
  - 9 = Weber Blue Dog (vintage American tone)
  - 10 = 10" speaker (smaller, tighter)
  - 11 = 15" speaker (larger, fuller bass)

#### cabinetConfig (Cabinet)
- **Range**: 0.0 to 5.0
- **Default**: 3.0 (2x12)
- **Description**: Cabinet configuration
- **Values**:
  - 0 = 1x10 (small, tight, focused)
  - 1 = 1x12 (versatile, balanced)
  - 2 = 2x10 (punchy, tight)
  - 3 = 2x12 (common, balanced)
  - 4 = 4x10 (powerful, bass-light)
  - 5 = 4x12 (full, powerful, maximum bass)

#### cabinetResonance (Cab Resonance)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Cabinet body resonance amount
- **Usage**: Adds low-frequency body and depth. Higher values increase "thump" and cabinet character.

### Microphone 1

#### mic1Type (Mic 1 Type)
- **Range**: 0.0 to 10.0
- **Default**: 0.0 (SM57)
- **Description**: Microphone model
- **Values**:
  - 0 = Shure SM57 (industry standard, bright)
  - 1 = Sennheiser MD421 (versatile dynamic)
  - 2 = Royer R121 (ribbon, smooth, dark)
  - 3 = Sennheiser e906 (guitar-specific dynamic)
  - 4 = Sennheiser e609 (side-address dynamic)
  - 5 = AKG C414 (large diaphragm condenser)
  - 6 = Neumann U87 (studio standard condenser)
  - 7 = Beyerdynamic M160 (ribbon, vintage)
  - 8 = Electro-Voice RE20 (large dynamic)
  - 9 = Audio-Technica AT4050 (multi-pattern condenser)
  - 10 = AEA Fathead (ribbon, warm)

#### mic1Position (Mic 1 Position)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Position from speaker edge to center
- **Usage**:
  - 0% (Edge): Dark, more bass, less harsh
  - 50% (Halfway): Balanced
  - 100% (Center): Bright, aggressive, most highs

#### mic1Distance (Mic 1 Distance)
- **Range**: 0.0 to 12.0 inches
- **Default**: 1.0 inch
- **Description**: Distance from speaker grille
- **Effects**:
  - Close (0-2"): Proximity effect (more bass), bright
  - Medium (2-6"): Balanced, less proximity
  - Far (6-12"): Less bass, darker (air absorption)

#### mic1Angle (Mic 1 Angle)
- **Range**: 0.0 to 45.0 degrees
- **Default**: 0.0 degrees (on-axis)
- **Description**: Off-axis angle
- **Usage**:
  - 0° (On-axis): Brightest, most direct
  - 15-30°: Smoother, less harsh
  - 45°: Darkest, most off-axis coloration

#### mic1Level (Mic 1 Level)
- **Range**: 0.0 to 100.0 %
- **Default**: 100.0 %
- **Description**: Microphone 1 output level

### Microphone 2

#### mic2Enabled (Mic 2 Enabled)
- **Range**: 0.0 to 1.0
- **Default**: 0.0 (disabled)
- **Description**: Enable second microphone
- **Usage**: 0 = off, 1 = on

#### mic2Type (Mic 2 Type)
- **Range**: 0.0 to 10.0
- **Default**: 2.0 (R121 ribbon)
- **Description**: Second microphone model
- **See**: mic1Type for values

#### mic2Position (Mic 2 Position)
- **Range**: 0.0 to 100.0 %
- **Default**: 30.0 %
- **Description**: Second mic position from edge to center

#### mic2Distance (Mic 2 Distance)
- **Range**: 0.0 to 12.0 inches
- **Default**: 2.0 inches
- **Description**: Second mic distance from speaker

#### mic2Angle (Mic 2 Angle)
- **Range**: 0.0 to 45.0 degrees
- **Default**: 15.0 degrees
- **Description**: Second mic off-axis angle

#### mic2Level (Mic 2 Level)
- **Range**: 0.0 to 100.0 %
- **Default**: 70.0 %
- **Description**: Microphone 2 output level

### Output

#### output (Output)
- **Range**: 0.0 to 100.0 %
- **Default**: 50.0 %
- **Description**: Output level
- **Implementation**: Linear gain 0.0 to 2.0

## Speaker Type Characteristics

Each speaker type has unique frequency response stored in the SpeakerType enum:
- **Resonance Hz**: Low frequency resonance peak
- **Rolloff Hz**: High frequency rolloff point
- **Bass**: Low frequency amount
- **Low Mids**: 200-500 Hz character
- **Upper Mids**: 1-3 kHz presence
- **Presence**: 3-5 kHz edge
- **Air**: High frequency sparkle
- **Compression**: Dynamic compression amount

## Microphone Type Characteristics

Each microphone has unique EQ characteristics in the MicrophoneType enum:
- **Bass**: Low frequency response
- **Low Mids**: Body and warmth
- **Mids**: Core tone
- **Presence**: Upper midrange bite
- **Air**: High frequency extension
- **Proximity Effect**: Bass boost when close

## Cabinet Configurations

Low frequency cutoff varies by cabinet size:
- **1x10**: 100 Hz (tightest)
- **1x12**: 80 Hz
- **2x10**: 90 Hz
- **2x12**: 70 Hz
- **4x10**: 80 Hz
- **4x12**: 60 Hz (most bass)

## Signal Flow

1. **Input**
   - Receive input signal

2. **Speaker Simulation**
   - Speaker resonance (low frequency emphasis)
   - Cabinet high-pass filter (based on size)
   - Speaker low-pass filter (high frequency rolloff)
   - Speaker compression (cone breakup)
   - Apply speaker EQ character

3. **Multiple Speaker Interaction**
   - Scale by number of speakers
   - Add slight volume increase

4. **Microphone 1 Processing**
   - Position effect (edge=dark, center=bright)
   - Distance effect (proximity bass boost, air absorption)
   - Angle effect (off-axis darkening)
   - Apply mic EQ (bass shelf, presence peak, air shelf)

5. **Microphone 2 Processing** (if enabled)
   - Same as Mic 1
   - Calculate phase delay based on distance difference
   - Apply delay to maintain phase coherence

6. **Mixing**
   - Blend both microphones
   - Normalize by total level

7. **Output**
   - Apply output gain
   - Soft output clipping

## Implementation Details

### Speaker Resonance

Resonant peak at speaker's natural frequency:
```
Peaking filter at resonanceHz
Q = 0.7 + resonanceAmount * 0.5
Gain = 1.0 + resonanceAmount * 0.5
```

### Speaker Compression

Models cone breakup and compression:
```
Track signal envelope (slow: 0.99)
if level > 0.5:
  compression = 1.0 + (level - 0.5) * compressionAmount * 2.0
  sample /= compression

if |sample| > 0.8:
  sample = tanh(sample * 1.2) * 0.85
```

### Microphone Position Effects

**High Frequency** (center = bright):
```
highFreqMult = 0.6 + brightnessFromPosition * 0.6
```

**Low Frequency** (edge = more bass):
```
lowFreqMult = 1.1 - brightnessFromPosition * 0.2
```

### Proximity Effect

Bass boost for directional mics when close:
```
if distance < 3.0:
  proximityBoost = 1.0 + (3.0 - distance)/3.0 * proximityEffect * 0.3
```

### Distance High-Frequency Loss

Air absorption at distance:
```
distanceHighLoss = 1.0 - min(distance / 12.0, 0.3)
```

### Off-Axis Darkening

```
angleRad = angle * π / 180
offAxisDarkening = cos(angleRad)  // 1.0 at 0°, ~0.7 at 45°
```

### Phase Alignment

When using two mics at different distances:
```
delayMs = |mic2Distance - mic1Distance| * 0.0254 / 343.0 * 1000
delaySamples = delayMs * sampleRate / 1000
```

Speed of sound: 343 m/s, 0.0254 m/inch

## Stereo Processing

Processes mono (typical for guitar cabinets). Input is mixed to mono, processed, then copied to both output channels.

## Usage Tips

### Classic SM57 on Center
- Speaker: Greenback
- Cabinet: 4x12
- Mic 1: SM57, Position 100%, Distance 1", Angle 0°
- Character: Bright, aggressive, rock standard

### Dual Mic (SM57 + Ribbon)
- Speaker: Vintage 30
- Cabinet: 4x12
- Mic 1: SM57, Position 80%, Distance 1", Angle 0°, Level 100%
- Mic 2: R121, Position 30%, Distance 3", Angle 30°, Level 70%
- Character: Bright attack with smooth body

### Vintage Blues
- Speaker: Jensen C12N
- Cabinet: 1x12
- Cab Resonance: 70%
- Mic 1: e609, Position 40%, Distance 2", Angle 15°
- Character: Warm, vintage, smooth

### Modern Metal
- Speaker: V30 or G12T-75
- Cabinet: 4x12
- Cab Resonance: 30%
- Mic 1: SM57, Position 100%, Distance 0.5", Angle 0°
- Character: Tight, aggressive, maximum attack

### Jazz Clean (Condenser)
- Speaker: Blue or Jensen P12R
- Cabinet: 1x12 or 2x12
- Mic 1: C414 or U87, Position 50%, Distance 6", Angle 15°
- Character: Smooth, detailed, natural

### Room Sound
- Speaker: Any
- Cabinet: Any
- Mic 1: R121 or U87, Distance 12", Angle 30°
- Character: Distant, ambient, less direct

## Technical Specifications

- **Processing**: 32-bit float
- **Filter Types**: Biquad IIR (2nd order)
- **Latency**: Variable (0-2048 samples for mic 2 delay)
- **CPU Usage**: Medium
- **Stereo**: Mono processing (summed to mono, duplicated to stereo)
- **Max Delay Buffer**: 2048 samples

## Common Microphone Pairings

### Dynamic + Ribbon (Classic)
- Mic 1: SM57 (bright, present)
- Mic 2: R121 or M160 (smooth, dark)
- Blend for attack + body

### Dual Dynamic (Budget)
- Mic 1: SM57 (on-axis)
- Mic 2: e906 or MD421 (off-axis)
- Balanced, affordable

### Dynamic + Condenser (Studio)
- Mic 1: SM57 (close, bright)
- Mic 2: U87 or C414 (farther, room)
- Professional studio sound

### Dual Ribbon (Smooth)
- Mic 1: R121 (on-axis)
- Mic 2: Fathead (off-axis)
- Very smooth, dark, vintage

## See Also

- CabinetSimEffect - Simpler cabinet sim with synthetic IRs
- IRLoaderEffect - Load external impulse responses
- SpeakerType enum - Speaker characteristic definitions
- MicrophoneType enum - Microphone characteristic definitions
