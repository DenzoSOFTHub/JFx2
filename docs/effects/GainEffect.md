# Gain Effect

**Category:** Gain
**Effect ID:** `gain`
**Class:** `it.denzosoft.jfx2.effects.impl.GainEffect`

## Overview

The Gain effect provides volume control with multiple saturation curves to handle signal overflow gracefully. It offers clean gain adjustment in dB with smooth transitions and optional soft clipping modes that add harmonic warmth.

## Sonic Description

A versatile gain stage that ranges from transparent unity gain to warm saturation. The effect can operate as a simple volume control, a transparent limiter, or a subtle harmonic enhancer depending on the saturation mode selected. When driven hard with soft saturation modes, it adds musical harmonics similar to analog tape or tube circuits.

## Parameters

### 1. Gain
- **Range:** -60.0 dB to +24.0 dB
- **Default:** 0.0 dB (unity)
- **Unit:** dB
- **Description:** Input gain control. Boost signal into saturation for more harmonics. At 0 dB, signal passes at unity gain. Negative values attenuate, positive values boost.

### 2. Saturation
- **Type:** Choice (0-6)
- **Default:** 0 (None)
- **Options:**
  - **0 - None:** No clipping, signal can exceed -1/+1 range
  - **1 - Hard Clip:** Simple hard clipping at -1/+1 boundaries
  - **2 - Soft (Tanh):** Hyperbolic tangent - very smooth, symmetric saturation
  - **3 - Warm (Arctan):** Arctangent - warm, musical character
  - **4 - Cubic:** Polynomial soft clip - transparent with minimal harmonic addition
  - **5 - Tube:** Asymmetric tube-style saturation with even harmonics
  - **6 - Tape:** Tape-style saturation with compression and subtle odd harmonics
- **Description:** Clipping behavior when signal exceeds -1/+1 range. Soft modes add harmonic warmth.

### 3. Knee
- **Range:** 0.0% to 100.0%
- **Default:** 50.0%
- **Unit:** %
- **Description:** Controls saturation curve sharpness. Low values (0-30%) provide gradual transition and softer saturation. High values (70-100%) provide abrupt transition and harder saturation.

### 4. Level
- **Range:** -60.0 dB to 0.0 dB
- **Default:** 0.0 dB (unity)
- **Unit:** dB
- **Description:** Output level control. Reduce after saturation to avoid downstream clipping. Use to compensate for gain added by saturation.

## Implementation Details

### Algorithm Overview

The gain effect applies a simple signal flow:
1. Multiply input by linear gain (converted from dB)
2. Apply selected saturation curve
3. Multiply output by linear level (converted from dB)

### Saturation Curves

#### None (Mode 0)
No processing - signal passes through unmodified. Can exceed ±1.0 range.

#### Hard Clip (Mode 1)
```
output = clamp(input, -1.0, +1.0)
```
Simple hard limiting at ±1.0. Creates harsh digital clipping with odd harmonics.

#### Soft (Tanh) (Mode 2)
```
drive = 0.5 + knee * 2.5  (range: 0.5 to 3.0)
output = tanh(input * drive)
```
Smooth symmetric saturation using hyperbolic tangent. Very musical and transparent. Adds primarily odd harmonics.

#### Warm (Arctan) (Mode 3)
```
drive = 0.5 + knee * 3.5  (range: 0.5 to 4.0)
output = atan(input * drive) * (2.0 / π)
```
Warm character with gentler saturation than tanh. Popular for mastering applications.

#### Cubic (Mode 4)
```
threshold = 0.3 + knee * 0.6  (range: 0.3 to 0.9)
- Linear region: |input| < threshold → output = input
- Soft region: |input| > threshold → quadratic blend to ±1.0
```
Transparent soft clipping with minimal harmonic content. Three regions: linear, soft knee, and hard limit.

#### Tube (Mode 5)
```
factor = 0.8 + knee * 2.2  (range: 0.8 to 3.0)
- Positive half: output = (1.0 - exp(-input * factor)) * 0.95
- Negative half: output = -1.0 + exp(input * factor * 0.8)
```
Asymmetric saturation simulating tube grid limiting. More compression on positive peaks, less on negative peaks. Generates even harmonics for warm, vintage character.

#### Tape (Mode 6)
```
linearThreshold = 0.7 - knee * 0.5  (range: 0.2 to 0.7)
drive = 1.0 + knee * 3.0  (range: 1.0 to 4.0)
- Linear region: |input| < threshold → output = input * (1.0 + knee * 0.15)
- Saturation region: |input| > threshold → tanh compression to 0.98
```
Soft compression with subtle odd harmonics, emulating analog tape saturation. Features a boosted linear region and gentle saturation.

### Knee Parameter Behavior

The knee parameter affects each saturation mode differently:
- **Tanh/Arctan:** Controls drive factor (steepness of curve)
- **Cubic:** Controls threshold where soft clipping begins
- **Tube:** Controls aggressiveness of exponential shaping
- **Tape:** Controls both linear threshold and saturation drive

## Signal Flow Diagram

```
Input → Gain Stage → Saturation → Level Stage → Output
         (dB to linear)  (various curves)  (dB to linear)
```

## Usage Tips

### As a Transparent Volume Control
- Set **Gain** to desired level
- Set **Saturation** to "None" (0)
- Set **Level** to 0 dB
- **Knee** is ignored

### As a Soft Limiter
- Set **Gain** to 0-6 dB
- Set **Saturation** to "Cubic" (4) for transparency
- Set **Knee** to 70-100% for harder limiting
- Reduce **Level** to taste (typically -3 to -6 dB)

### For Harmonic Enhancement
- Set **Gain** to +12 to +18 dB
- Set **Saturation** to "Tube" (5) or "Tape" (6)
- Set **Knee** to 30-60% for musical saturation
- Reduce **Level** to -12 to -18 dB to compensate

### As a Mastering Tool
- Set **Gain** to +3 to +6 dB
- Set **Saturation** to "Warm (Arctan)" (3)
- Set **Knee** to 40-50%
- Set **Level** to -3 to -6 dB

### For Aggressive Saturation
- Set **Gain** to +18 to +24 dB
- Set **Saturation** to "Soft (Tanh)" (2) or "Hard Clip" (1)
- Set **Knee** to 80-100%
- Reduce **Level** significantly (-12 to -20 dB)

## Technical Notes

- All saturation curves are symmetric except Tube mode
- Processing is done in 32-bit float for maximum headroom
- The effect is mono/stereo agnostic - each channel processed independently
- No latency introduced
- CPU usage is minimal - suitable for real-time processing
- dB to linear conversion uses standard formula: `linear = 10^(dB/20)`
