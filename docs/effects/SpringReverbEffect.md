# SpringReverbEffect

## Overview

SpringReverbEffect emulates the distinctive sound of spring reverb tanks found in guitar amplifiers like Fender, Ampeg, and vintage organs. It captures the characteristic "boing," "drip," and metallic quality of physical spring transducers.

**Category:** Reverb
**ID:** `springreverb`
**Display Name:** Spring Reverb

## Description

This effect simulates multiple physical springs in parallel, each with different resonant characteristics. Spring reverb has a unique sound due to the mechanical properties of springs: mid-focused frequency response, metallic overtones, and characteristic transient response. Perfect for surf, rockabilly, and vintage tones.

## Parameters

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Balance between dry and reverb signal. Spring reverbs in amps typically operated at 20-40% mix.

### Decay
- **ID:** `decay`
- **Range:** 0.5 s to 4.0 s
- **Default:** 2.0 s
- **Unit:** seconds
- **Description:** Length of the reverb tail. Real spring tanks typically had 1.5-3 second decay times.

### Tone
- **ID:** `tone`
- **Range:** 500 Hz to 5000 Hz
- **Default:** 2000 Hz
- **Unit:** Hz
- **Description:** High frequency content. Lower values create darker, more vintage sounds. Real springs had limited high-frequency response, typically rolling off above 3-4 kHz.

### Drip
- **ID:** `drip`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Amount of the characteristic spring "drip" sound on attacks. This is the metallic, bouncing quality that occurs on transients. Higher values create more pronounced drip.

### Tension
- **ID:** `tension`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Spring tension. Higher values create tighter, faster response with shorter effective delay times. Lower values create looser, slower springs.

## DSP Components

### Spring Simulation (3 Springs)

Each spring is modeled as:
- Delay line (different length per spring)
- All-pass filter for metallic character
- Feedback path for resonance

**Spring Delay Times:**
- Spring 1: 35.0 ms (base)
- Spring 2: 41.0 ms (longer)
- Spring 3: 47.0 ms (longest)

The different lengths create natural comb filtering and complexity.

### Filters

#### Input Filters (Per Channel)
- **Type:** Biquad Peak filter
- **Frequency:** 1500 Hz
- **Q:** 0.7
- **Gain:** +6 dB
- **Purpose:** Emphasizes mid-range frequencies, simulating spring transducer response

#### Output Filters (Per Channel)
- **Type:** Biquad Lowpass
- **Frequency:** Controlled by Tone parameter
- **Q:** 0.707 (Butterworth)
- **Purpose:** Simulates limited high-frequency response of springs

#### Low Cut Filters (Per Channel)
- **Type:** Biquad Highpass
- **Frequency:** 120 Hz
- **Q:** 0.707
- **Purpose:** Prevents low-frequency rumble and mud buildup

### All-Pass Filters

Each spring has a simple all-pass filter for metallic quality:
```
output = -coeff * input + state[0] + coeff * state[1]
state[0] = input
state[1] = output
```

Coefficient: 0.6 (creates characteristic metallic "boing")

### Delay Lines
- **Per Spring per Channel:** 3 springs × 2 channels = 6 delay lines
- **Maximum Capacity:** 100 ms each
- Uses cubic interpolation for smooth modulation

## Implementation Details

### Signal Flow

```
Input → Input Peak Filter (1500 Hz boost)
        ↓
        Split to 3 Springs in Parallel
        ↓
        [For each spring:]
        Write: Input * (0.5 + drip*0.5) + Feedback
        ↓
        Read: Delay Line (at spring-specific time)
        ↓
        All-Pass Filter (metallic character)
        ↓
        Store as Feedback
        ↓
        Sum All 3 Springs → Average
        ↓
        Output Lowpass Filter (tone control)
        ↓
        Low Cut Filter (rumble removal)
        ↓
        Mix with Dry → Output
```

### Spring Delay Time Calculation

Base spring times are modulated by tension:
```java
float tensionMod = 0.7 + tension * 0.6;
// Range: 0.7 to 1.3

float springTime = SPRING_TIMES[s] * tensionMod;
```

**At tension = 0%:** Times are 70% of base (24.5, 28.7, 32.9 ms) - Loose springs
**At tension = 100%:** Times are 130% of base (45.5, 53.3, 61.1 ms) - Tight springs

### Feedback Calculation

Feedback amount is calculated to achieve target decay time:
```java
float feedbackAmount = pow(0.001, 1.0 / (decayTime * sampleRate / springTime));
feedbackAmount = min(0.95, feedbackAmount);
```

This formula ensures the reverb decays to -60 dB in the specified decay time.

### Drip Control

Input gain to springs varies with drip parameter:
```java
float springInput = filtered * (0.5 + drip * 0.5);
```

**At drip = 0%:** Input gain = 0.5 (subtle)
**At drip = 100%:** Input gain = 1.0 (pronounced)

This controls how strongly transients excite the springs, affecting the "drip" characteristic.

### Stereo Processing

In stereo mode:
- Each channel has independent springs
- Right channel springs are slightly detuned (±2% per spring)
- Creates stereo width and natural variation
- Simulates two-spring stereo tanks (common in Fender amps)

### All-Pass Metallic Character

The all-pass filters create the characteristic metallic "boing" by adding:
- Phase shifts at different frequencies
- Resonant peaks
- Frequency-dependent group delay

Coefficient of 0.6 is tuned to match real spring tank character.

## Usage Tips

### Classic Surf Reverb
- **Mix:** 40-60%
- **Decay:** 2.0-2.5 s
- **Tone:** 2500-3500 Hz
- **Drip:** 60-80%
- **Tension:** 40-60%

### Vintage Amp Reverb (Fender)
- **Mix:** 25-40%
- **Decay:** 1.5-2.5 s
- **Tone:** 2000-3000 Hz
- **Drip:** 40-60%
- **Tension:** 45-65%

### Dark, Murky Spring
- **Mix:** 30-50%
- **Decay:** 2.5-3.5 s
- **Tone:** 1000-1500 Hz (dark)
- **Drip:** 30-50%
- **Tension:** 30-50%

### Tight, Articulate Spring
- **Mix:** 20-35%
- **Decay:** 1.5-2.0 s
- **Tone:** 3000-4000 Hz (bright)
- **Drip:** 40-60%
- **Tension:** 60-80%

### Extreme Boing (Special Effect)
- **Mix:** 50-80%
- **Decay:** 3.0-4.0 s
- **Tone:** 2000-3000 Hz
- **Drip:** 80-100%
- **Tension:** 20-40% (loose)

### Organ-Style Spring
- **Mix:** 30-45%
- **Decay:** 2.0-3.0 s
- **Tone:** 1500-2500 Hz
- **Drip:** 50-70%
- **Tension:** 50-60%

## Musical Applications

### Surf Rock
Cranked spring reverb is essential for surf guitar. Use high mix (50-70%), moderate decay, and high drip for that classic Dick Dale sound.

### Rockabilly
Moderate spring reverb (30-40% mix) adds space without washing out the percussive playing style. Keep drip moderate (40-60%).

### Blues
Subtle spring reverb (20-30% mix) adds depth while maintaining clarity. Use darker tone settings (1500-2000 Hz).

### Psychedelic
Extreme spring settings with high drip create otherworldly textures. Automate tension for "spring dive bomb" effects.

### Country
Light spring reverb (20-30% mix) is traditional for clean country tones. Keep it subtle to maintain twang and articulation.

## Physical Spring Reverb Background

Spring reverb was invented in the 1930s and became popular in guitar amplifiers in the 1960s:

### How Physical Springs Work
1. Input transducer converts audio signal to mechanical motion
2. Spring(s) transmit vibrations from one end to other
3. Output transducer converts mechanical motion back to audio
4. Reflections within spring create reverb effect

### Common Spring Configurations
- **Single Spring:** Mono, found in small amps
- **Two Springs:** Stereo, found in Fender amps (Twin Reverb, etc.)
- **Three Springs:** Hammond organs, higher density
- **Four Springs:** Some high-end units, Accutronics tanks

### Characteristic Artifacts
- **Drip:** Transient response causing metallic "boing"
- **Boing:** Low-frequency resonance when struck
- **Crash:** Spring noise when amp is physically disturbed
- **Limited Bandwidth:** Typically 150 Hz to 4 kHz
- **Mid-Focused:** Peak around 1-2 kHz
- **Mechanical Noise:** Inherent spring ringing

## Technical Specifications

- **Number of Springs:** 3 (parallel)
- **Spring Delay Range:** 24.5 ms to 61.1 ms (tension-dependent)
- **Feedback Range:** Calculated for 0.5-4.0 s decay
- **All-Pass Coefficient:** 0.6 (metallic character)
- **Input Boost:** +6 dB peak at 1500 Hz
- **Low Cut:** 120 Hz highpass
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **CPU Usage:** Moderate (6 delay lines + 6 filters + 3 all-pass per channel)
- **Memory Usage:** ~45 KB for all delay lines

## Advanced Techniques

### Spring Crash Effect
Automate drip from 0% to 100% rapidly to simulate hitting the reverb tank. Create dramatic impact sounds.

### Spring Dive Bomb
Automate tension from high to low (80% → 20%) during sustained notes to simulate pulling on springs. Creates downward pitch bend effect.

### Stereo Width Enhancement
Process guitar in mono but use spring reverb in stereo. The natural variation between channels creates width.

### Parallel Processing
Run spring reverb on a parallel track at 100% mix, then blend. Allows for very wet reverb without losing direct signal clarity.

## Differences from Algorithmic Reverb

**Spring Reverb:**
- Metallic, colored sound
- Strong transient response (drip)
- Limited bandwidth
- Comb filtering artifacts
- Characteristic mid-range boost
- Physical "boing" quality

**Algorithmic Reverb (Freeverb):**
- Neutral, smooth sound
- Even frequency response
- Wide bandwidth
- Minimal coloration
- Natural space simulation

Choose spring reverb when you want vintage character and coloration, not neutral room simulation.

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/SpringReverbEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize springs, filters, and all-pass states
- `onProcess(float[] input, float[] output, int frameCount)` - Mono spring processing
- `onProcessStereo(...)` - Stereo processing with detuned springs
- `allPass(float input, float[] state, float coeff)` - Simple all-pass for metallic character
- `onReset()` - Clear all spring states and filters

**Key Features:**
- 3 parallel springs with different delay times
- Input peak filter for mid-range emphasis
- All-pass filters for metallic quality
- Tension control modulates spring delay times
- Drip control for transient response

**Constants:**
- `NUM_SPRINGS = 3`
- `SPRING_TIMES = {35.0, 41.0, 47.0}` ms

## See Also

- [ReverbEffect](ReverbEffect.md) - Algorithmic room reverb (Freeverb)
- [ShimmerReverbEffect](ShimmerReverbEffect.md) - Ambient pitch-shifted reverb
- [TapeEchoEffect](TapeEchoEffect.md) - Vintage echo with character
