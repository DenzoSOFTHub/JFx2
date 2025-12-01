# ReverbEffect

## Overview

ReverbEffect is a high-quality algorithmic reverb based on the Freeverb algorithm. It uses 8 parallel comb filters feeding into 4 series all-pass filters to create a rich, natural-sounding reverb suitable for simulating rooms and halls.

**Category:** Reverb
**ID:** `reverb`
**Display Name:** Reverb

## Description

This implementation of the classic Freeverb algorithm provides lush, smooth reverb with controllable room size, damping, and stereo width. The architecture uses feedback comb filters for density and all-pass filters for diffusion, resulting in a natural-sounding room simulation without metallic artifacts.

## Parameters

### Room Size
- **ID:** `roomSize`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Simulated room size. Larger values create longer, more spacious reverb tails. 0% is a small room, 100% is a large hall.

### Damping
- **ID:** `damp`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** High frequency absorption simulating air absorption and surface damping. 0% is bright and reflective, 100% is dark and heavily damped. Higher values make the reverb more natural.

### Width
- **ID:** `width`
- **Range:** 0% to 100%
- **Default:** 100%
- **Unit:** %
- **Description:** Stereo spread of the reverb. 100% creates full stereo width, 0% creates a mono reverb. Lower values create a more focused, centered sound.

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Balance between dry and reverb signal. 0% is completely dry, 100% is completely wet. Higher values create a more distant, spacious sound.

### Pre-Delay
- **ID:** `predelay`
- **Range:** 0 ms to 100 ms
- **Default:** 10 ms
- **Unit:** ms (milliseconds)
- **Description:** Time before reverb starts. Adds clarity by separating direct sound from early reflections. Larger spaces typically have longer pre-delays.

## DSP Components

### Freeverb Architecture

The Freeverb algorithm consists of two main stages:

#### Stage 1: Parallel Comb Filters (8 per channel)
**Purpose:** Create density and body of reverb

**Left Channel Delays (samples @ 44100 Hz):**
- Comb 1: 1116 samples (~25.3 ms)
- Comb 2: 1188 samples (~26.9 ms)
- Comb 3: 1277 samples (~29.0 ms)
- Comb 4: 1356 samples (~30.7 ms)
- Comb 5: 1422 samples (~32.2 ms)
- Comb 6: 1491 samples (~33.8 ms)
- Comb 7: 1557 samples (~35.3 ms)
- Comb 8: 1617 samples (~36.6 ms)

**Right Channel Delays:**
Each right channel comb is 23 samples longer than left for stereo decorrelation.

**Characteristics:**
- Feedback controlled by Room Size parameter
- Internal damping filter controlled by Damping parameter
- All combs process input in parallel
- Outputs are summed

#### Stage 2: Series All-Pass Filters (4 per channel)
**Purpose:** Diffusion and smoothing

**Delay Times (samples @ 44100 Hz):**
- All-Pass 1: 556 samples (~12.6 ms)
- All-Pass 2: 441 samples (~10.0 ms)
- All-Pass 3: 341 samples (~7.7 ms)
- All-Pass 4: 225 samples (~5.1 ms)

**Characteristics:**
- Fixed feedback coefficient: 0.5
- Cascaded (series) configuration
- Right channel delays offset by 23 samples for stereo
- Creates smooth, dense reverb without metallic coloration

### Pre-Delay Buffers
- **Left Channel:** `predelayBufferL` - Maximum 100 ms
- **Right Channel:** `predelayBufferR` - Maximum 100 ms
- Circular buffer implementation
- Provides separation between direct sound and reverb

## Implementation Details

### Signal Flow

```
Input → Pre-Delay Buffer
        ↓
        × FIXED_GAIN (0.015)
        ↓
        Parallel Comb Filters (8x) → Sum
        ↓
        All-Pass Filter 1
        ↓
        All-Pass Filter 2
        ↓
        All-Pass Filter 3
        ↓
        All-Pass Filter 4
        ↓
        Width Processing (Stereo)
        ↓
        Mix with Dry → Output
```

### Comb Filter Algorithm

Each comb filter implements:
```
output = delayLine.read()
delayLine.write(input + output * feedback * damping)
```

With internal one-pole lowpass damping filter that progressively darkens reflections.

### Room Size Calculation

Room size maps to comb filter feedback:
```java
float feedback = roomSize * SCALE_ROOM + OFFSET_ROOM;
// SCALE_ROOM = 0.28
// OFFSET_ROOM = 0.7
// Result: feedback ranges from 0.7 to 0.98
```

Lower feedback = shorter reverb tail (small room)
Higher feedback = longer reverb tail (large room)

### Damping Calculation

Damping maps to comb filter internal lowpass:
```java
float dampValue = damp * SCALE_DAMP;
// SCALE_DAMP = 0.4
// Result: dampValue ranges from 0.0 to 0.4
```

Controls the one-pole lowpass filter coefficient in each comb filter.

### Width Processing

Uses mid-side processing for stereo width control:
```java
float wet1 = mix * (width / 2.0 + 0.5);    // Main channel coefficient
float wet2 = mix * ((1.0 - width) / 2.0);  // Cross channel coefficient

outputL = dryL * (1 - mix) + outL * wet1 + outR * wet2;
outputR = dryR * (1 - mix) + outR * wet1 + outL * wet2;
```

When width = 100%: wet1 = mix, wet2 = 0 (full stereo)
When width = 0%: wet1 = wet2 = mix/2 (mono reverb)

### Sample Rate Scaling

Delay times are scaled for different sample rates:
```java
float scale = sampleRate / 44100.0;
int scaledDelay = (int)(originalDelay * scale);
```

This maintains equivalent reverb character at 48kHz, 96kHz, etc.

### Fixed Gain

Input is scaled by 0.015 before entering comb filters to prevent internal overflow and maintain headroom.

## Usage Tips

### Small Room/Chamber
- **Room Size:** 20-40%
- **Damping:** 40-60%
- **Width:** 80-100%
- **Mix:** 15-30%
- **Pre-Delay:** 5-15 ms

### Medium Hall
- **Room Size:** 50-70%
- **Damping:** 40-55%
- **Width:** 90-100%
- **Mix:** 25-40%
- **Pre-Delay:** 15-30 ms

### Large Hall/Cathedral
- **Room Size:** 75-95%
- **Damping:** 30-50%
- **Width:** 100%
- **Mix:** 30-50%
- **Pre-Delay:** 30-60 ms

### Plate Reverb Emulation
- **Room Size:** 40-60%
- **Damping:** 20-40% (brighter)
- **Width:** 100%
- **Mix:** 20-35%
- **Pre-Delay:** 0-10 ms

### Ambient Wash
- **Room Size:** 80-100%
- **Damping:** 60-80% (darker)
- **Width:** 100%
- **Mix:** 50-80%
- **Pre-Delay:** 20-50 ms

### Vocal Reverb
- **Room Size:** 45-65%
- **Damping:** 50-65%
- **Width:** 90-100%
- **Mix:** 20-35%
- **Pre-Delay:** 20-40 ms (keeps vocals upfront)

### Drum Reverb
- **Room Size:** 30-50%
- **Damping:** 40-60%
- **Width:** 100%
- **Mix:** 15-30%
- **Pre-Delay:** 5-20 ms

## Technical Specifications

- **Algorithm:** Freeverb (Jezar at Dreampoint)
- **Comb Filters:** 8 per channel (16 total)
- **All-Pass Filters:** 4 per channel (8 total)
- **Total DSP Components:** 24 filters
- **Maximum Pre-Delay:** 100 ms
- **Sample Rate:** Scalable (tuned for 44100 Hz)
- **Internal Processing:** 32-bit float
- **Stereo Decorrelation:** 23-sample offset between channels
- **CPU Usage:** Moderate (24 filter operations per sample)
- **Memory Usage:** ~200 KB for all delay lines

## Freeverb Algorithm History

Freeverb was created by Jezar at Dreampoint in 2000 and released into the public domain. It became one of the most popular reverb algorithms due to:
- Simple, elegant design
- Natural sound without metallic artifacts
- Low CPU usage
- Easy to implement and tune

The algorithm uses:
- Prime number delay lengths to avoid periodic resonances
- Stereo decorrelation via offset delays
- One-pole lowpass damping in feedback paths
- All-pass diffusers for smoothness

## Advanced Techniques

### Creating Space Variation

Automate Room Size subtly (±10-20%) to create living, breathing spaces rather than static rooms.

### Ducking

Lower Mix when signal is present, raise when signal is quiet. Creates clarity while maintaining spaciousness.

### Stereo Width Automation

Start narrow (40-60%) and gradually increase to full width (100%) for dramatic buildups.

### Pre-Delay as Effect

Use longer pre-delays (50-100 ms) as a creative effect to separate direct sound from reverb, creating a slapback-into-reverb sound.

### Parallel Processing

Process reverb at 100% mix on a parallel track, then blend with dry signal. Allows for heavy reverb without washing out the source.

## Limitations and Characteristics

### Metallic Resonance
At very high feedback settings (room size > 90%), slight metallic resonance may appear. Use damping > 40% to mitigate.

### Early Reflections
This algorithm focuses on late reverberation. It doesn't model distinct early reflections. Consider adding a multi-tap delay before the reverb for early reflection simulation.

### Tail Build-Up
The reverb tail takes a moment to build to full density (especially at high room sizes). This is normal for the algorithm.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/ReverbEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize all filters and buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing
- `onProcessStereo(...)` - Full stereo reverb processing
- `onReset()` - Clear all filter states and buffers

**Key Features:**
- 8 parallel comb filters per channel
- 4 series all-pass filters per channel
- Sample-rate scalable delay times
- Pre-delay with circular buffering
- Mid-side stereo width control

**DSP Classes:**
- `CombFilter` - Feedback comb filter with damping
- `AllPassFilter` - All-pass filter for diffusion
- Circular buffer for pre-delay

## See Also

- [SpringReverbEffect](SpringReverbEffect.md) - Classic amp-style spring reverb
- [ShimmerReverbEffect](ShimmerReverbEffect.md) - Pitch-shifted ambient reverb
- [DelayEffect](DelayEffect.md) - Add pre-delay for early reflections
