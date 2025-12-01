# ShimmerReverbEffect

## Overview

ShimmerReverbEffect creates ethereal, ambient textures by combining reverb with pitch shifting. The pitch-shifted signal is fed back into the reverb, creating evolving, crystalline harmonics that shimmer above the original sound.

**Category:** Reverb
**ID:** `shimmerreverb`
**Display Name:** Shimmer Reverb

## Description

Shimmer reverb was pioneered by Brian Eno and Daniel Lanois in the 1980s and later popularized by pedals like the Eventide Space and Strymon BigSky. It combines long reverb decay with pitch shifting (typically octave-up) in the feedback path, creating lush, evolving ambient textures. Perfect for ambient, post-rock, and cinematic soundscapes.

## Parameters

### Mix
- **ID:** `mix`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Balance between dry and reverb signal. Shimmer effects typically use higher mix settings (50-80%) to emphasize the texture.

### Decay
- **ID:** `decay`
- **Range:** 1 s to 30 s
- **Default:** 8 s
- **Unit:** seconds
- **Description:** Length of the reverb tail. Shimmer reverbs use very long decay times (5-20+ seconds) for ambient textures. Longer decays allow harmonics to evolve and build.

### Shimmer
- **ID:** `shimmer`
- **Range:** 0% to 100%
- **Default:** 50%
- **Unit:** %
- **Description:** Amount of pitch-shifted signal in the feedback path. 0% is regular reverb with no shimmer. 100% is fully pitch-shifted feedback. Higher values create more crystalline, bell-like tones.

### Pitch
- **ID:** `pitch`
- **Range:** 0 st to 24 st
- **Default:** 12 st
- **Unit:** semitones
- **Description:** Pitch shift amount. 12 semitones = 1 octave up. Common settings: 12 (octave up), 24 (two octaves up), 7 (perfect fifth), 5 (perfect fourth).

### Dampening
- **ID:** `dampening`
- **Range:** 1000 Hz to 16000 Hz
- **Default:** 6000 Hz
- **Unit:** Hz
- **Description:** High frequency absorption in the reverb tank. Lower values create darker, more natural decay. Higher values keep the shimmer bright and crystalline.

### Modulation
- **ID:** `modulation`
- **Range:** 0% to 100%
- **Default:** 30%
- **Unit:** %
- **Description:** Amount of chorus-like modulation in the reverb. Adds movement and prevents static, metallic character. Creates a more organic, living sound.

## DSP Components

### Diffuser Network (4 All-Pass Filters per Channel)

Early diffusion stage breaks up the input signal:

**Diffuser Delay Times (Prime Numbers):**
- Diffuser 1: 7.1 ms
- Diffuser 2: 11.3 ms
- Diffuser 3: 13.7 ms
- Diffuser 4: 17.9 ms

**Characteristics:**
- All-pass topology (preserves amplitude, shifts phase)
- Coefficient: 0.6 (creates diffusion without excessive coloration)
- Modulated by modulation parameter
- Cascaded (series) configuration

### Main Reverb Tank

Long delay lines for reverb body:
- **Left Tank:** 500 ms maximum capacity
- **Right Tank:** 500 ms maximum capacity (10% longer for stereo)
- Reading position modulated for movement

### Pitch Shifter (Granular)

Simple granular pitch shifter for each channel:
- **Buffer Size:** 100 ms (0.1 seconds)
- **Algorithm:** Time-domain pitch shifting with windowing
- **Window:** Triangle window for crossfading
- **Interpolation:** Linear

### Filters

#### Dampening Filters (Per Channel)
- **Type:** Biquad Lowpass
- **Q:** 0.707 (Butterworth)
- **Frequency:** Controlled by Dampening parameter
- **Purpose:** High-frequency absorption for natural decay

#### Low Cut Filters (Per Channel)
- **Type:** Biquad Highpass
- **Frequency:** 80 Hz
- **Q:** 0.707
- **Purpose:** Prevents low-frequency buildup and mud

### Modulation

Single LFO at 0.5 Hz (sine wave) modulates:
- Diffuser delay times (±10%)
- Tank delay times (±5%)
- Creates chorus-like movement

## Implementation Details

### Signal Flow

```
Input → Diffuser Network (4 all-pass, modulated)
        ↓
        Write to Reverb Tank (with feedback)
        ↓
        Read from Tank (modulated read position)
        ↓
        Dampening Filter (high-frequency absorption)
        ↓
        Split into Two Paths:
        ├─→ Regular Feedback (1 - shimmer)
        └─→ Pitch Shifter → Shimmer Feedback (shimmer)
        ↓
        Mix Both Feedback Paths
        ↓
        × Feedback Amount (based on decay)
        ↓
        Back to Tank Input
        ↓
        Low Cut Filter
        ↓
        Mix with Dry → Output
```

### Diffuser Network

Each diffuser implements all-pass topology:
```java
float delayed = diffuser.readCubic(delaySamples);
float apCoeff = 0.6;
float output = -apCoeff * input + delayed;
diffuser.write(input + apCoeff * output);
```

Creates early diffusion without amplitude coloration.

### Pitch Shifting Algorithm

Simple granular pitch shifter:

1. **Write to circular buffer:**
   ```java
   pitchBuffer[writePos] = input;
   writePos = (writePos + 1) % bufferSize;
   ```

2. **Read at different rate:**
   ```java
   readPos += pitchRatio;  // e.g., 2.0 for octave up
   if (readPos >= bufferSize) readPos -= bufferSize;
   ```

3. **Interpolate between samples:**
   ```java
   int idx0 = (int)readPos;
   int idx1 = idx0 + 1;
   float frac = readPos - idx0;
   float sample = buffer[idx0] * (1 - frac) + buffer[idx1] * frac;
   ```

4. **Apply window to reduce artifacts:**
   ```java
   float windowPos = (readPos % windowSize) / windowSize;
   float window = windowPos < 0.5 ? windowPos * 2 : (1 - windowPos) * 2;
   return sample * window;
   ```

### Pitch Ratio Calculation

Converts semitones to frequency ratio:
```java
float pitchRatio = pow(2.0, semitones / 12.0);

Examples:
  0 st → 1.0 (unison)
  7 st → 1.498 (perfect fifth)
 12 st → 2.0 (octave up)
 24 st → 4.0 (two octaves up)
```

### Feedback Calculation

Feedback amount ensures target decay time:
```java
float tankTimeMs = 150.0;
float feedback = pow(0.001, tankTimeMs / 1000.0 / decayTime);
feedback = min(0.98, feedback);  // Safety limit
```

This formula ensures reverb decays to -60 dB in the specified time.

### Shimmer Mixing

Regular and pitch-shifted feedback are mixed:
```java
float regularFeedback = tankOutput * (1 - shimmer);
float shimmeredFeedback = pitchShifted * shimmer;
float totalFeedback = (regularFeedback + shimmeredFeedback) * feedbackAmount;
```

**At shimmer = 0%:** Only regular feedback (normal reverb)
**At shimmer = 50%:** Equal mix of regular and pitch-shifted
**At shimmer = 100%:** Only pitch-shifted feedback (pure shimmer)

### Modulation

Sine LFO at 0.5 Hz modulates delay times:
```java
float modValue = sin(modPhase * 2π) * modulation;
float modulatedTime = baseTime * (1.0 + modValue * 0.1);
```

Creates ±10% variation in diffuser times and ±5% in tank times, adding movement and preventing metallic artifacts.

### Stereo Processing

- Left and right channels process independently
- Right channel tank is 10% longer than left
- Separate pitch shift buffers for each channel
- Creates natural stereo width and decorrelation

## Usage Tips

### Classic Octave Shimmer
- **Mix:** 50-70%
- **Decay:** 8-15 s
- **Shimmer:** 50-70%
- **Pitch:** 12 st (octave up)
- **Dampening:** 6000-8000 Hz
- **Modulation:** 30-50%

### Dual Octave Shimmer
- **Mix:** 60-80%
- **Decay:** 10-20 s
- **Shimmer:** 60-80%
- **Pitch:** 24 st (two octaves up)
- **Dampening:** 8000-12000 Hz (bright)
- **Modulation:** 20-40%

### Perfect Fifth Shimmer
- **Mix:** 50-70%
- **Decay:** 6-12 s
- **Shimmer:** 40-60%
- **Pitch:** 7 st (perfect fifth)
- **Dampening:** 5000-7000 Hz
- **Modulation:** 30-50%

### Dark Ambient Pad
- **Mix:** 70-90%
- **Decay:** 15-25 s
- **Shimmer:** 50-70%
- **Pitch:** 12 st
- **Dampening:** 3000-4000 Hz (dark)
- **Modulation:** 40-60%

### Subtle Enhancement
- **Mix:** 30-50%
- **Decay:** 5-8 s
- **Shimmer:** 30-40%
- **Pitch:** 12 st
- **Dampening:** 6000-8000 Hz
- **Modulation:** 20-30%

### Crystalline Texture
- **Mix:** 60-80%
- **Decay:** 10-18 s
- **Shimmer:** 70-90%
- **Pitch:** 12 or 24 st
- **Dampening:** 10000-16000 Hz (very bright)
- **Modulation:** 30-50%

## Musical Applications

### Ambient Guitar
Shimmer reverb is essential for ambient guitar styles. Use long decay (10-20 s), high shimmer (60-80%), and moderate mix (60-80%).

### Post-Rock Swells
Create massive soundscapes by combining volume swells with shimmer reverb. Start notes quietly and swell into the reverb.

### Pad Sounds
Turn sustained notes or chords into evolving pads. Use very long decay (15-30 s) and high mix (70-100%).

### Cinematic Scoring
Add ethereal quality to soundtracks. Moderate shimmer (40-60%) with long decay creates cinematic depth.

### Experimental Textures
Extreme settings (decay 20-30 s, shimmer 80-100%, pitch 24 st) create otherworldly soundscapes that barely resemble the source.

## Historical Context

### Origins
Shimmer reverb emerged from:
- Brian Eno's ambient work in the 1970s-80s
- Daniel Lanois's production techniques
- Eventide H3000/H8000 pitch-shifted reverb patches

### Commercial Implementations
- **Eventide H8000 (2000):** First widely available shimmer reverb
- **Strymon BigSky (2013):** Popularized in pedal format
- **Eventide Space (2008):** Made shimmer accessible to guitarists
- **Chase Bliss/Cooper FX Generation Loss (2020):** Modern variations

### Popularization
Artists who made shimmer reverb famous:
- U2 (The Edge)
- Sigur Rós
- Explosions in the Sky
- This Will Destroy You
- Hammock

## Technical Specifications

- **Algorithm Type:** Diffuser network + feedback tank + granular pitch shifter
- **Diffusers:** 4 all-pass per channel (prime number delays)
- **Tank Capacity:** 500 ms per channel
- **Pitch Shifter Buffer:** 100 ms per channel
- **Pitch Shift Algorithm:** Time-domain granular with triangle windowing
- **Modulation LFO:** 0.5 Hz sine wave
- **Maximum Decay:** 30 seconds
- **Sample Rate:** Inherited from audio engine
- **Internal Processing:** 32-bit float
- **CPU Usage:** Moderate to high (diffusers + tank + pitch shifting)
- **Memory Usage:** ~300 KB (buffers + tanks)

## Advanced Techniques

### Shimmer Swells
Automate mix from 0% to 80% during volume swells. Creates dramatic builds where the shimmer emerges from silence.

### Pitch Automation
Automate pitch parameter between 7 st (fifth) and 12 st (octave) for evolving harmonic content.

### Freeze Effect
Set decay to 30 s, shimmer to 80%, and mix to 100%. Creates infinite sustain with evolving harmonics. Play a chord and let it evolve.

### Reverse Shimmer
Chain ReverseDelayEffect before ShimmerReverbEffect. Creates reverse swells that feed into shimmering reverb.

### Layered Shimmer
Use two instances with different pitch settings:
- Instance 1: +12 st (octave up)
- Instance 2: +19 st (octave + fifth)
Creates complex harmonic stacking.

## Limitations and Considerations

### Pitch Shifting Artifacts
The simple granular algorithm produces some artifacts:
- Slight wavering/vibrato (mitigated by modulation)
- Reduced fidelity compared to advanced algorithms
- Works best on sustained notes, less on fast passages

### CPU Usage
Shimmer reverb is computationally intensive. Consider:
- Limiting to 1-2 instances per mix
- Using on dedicated ambient tracks
- Bouncing/rendering if CPU is constrained

### Harmonic Buildup
With long decay and high shimmer, harmonics can build indefinitely. Use dampening < 8000 Hz to control brightness buildup.

### Decay Time Latency
Very long decay settings (20-30 s) mean the reverb takes time to fully develop. Be patient—the texture builds over several seconds.

## Troubleshooting

**Problem:** Metallic, robotic sound
- **Solution:** Increase modulation (40-60%), decrease shimmer slightly

**Problem:** Muddy, unclear low end
- **Solution:** Dampening around 5000-7000 Hz, check that low cut is active

**Problem:** Too bright, harsh highs
- **Solution:** Lower dampening (4000-6000 Hz), reduce shimmer amount

**Problem:** Not enough shimmer effect
- **Solution:** Increase shimmer (60-80%), ensure pitch is 12 or 24 st, increase mix

## Code Reference

**Source File:** `/workspace/JFx2/src/main/java/it/denzosoft/jfx2/effects/impl/ShimmerReverbEffect.java`

**Key Methods:**
- `onPrepare(int sampleRate, int maxFrameCount)` - Initialize diffusers, tank, and pitch buffers
- `onProcess(float[] input, float[] output, int frameCount)` - Mono processing (internally stereo)
- `onProcessStereo(...)` - Full stereo shimmer processing
- `pitchShiftL(float input, float readPos)` - Left channel granular pitch shifter
- `pitchShiftR(float input, float readPos)` - Right channel granular pitch shifter
- `onReset()` - Clear all buffers and state

**Key Features:**
- 4-stage all-pass diffuser network with modulation
- Long feedback tank (500 ms)
- Granular pitch shifting in feedback path
- Triangle windowing for artifact reduction
- Modulated read positions for chorus effect

**Constants:**
- `DIFFUSER_TIMES = {7.1, 11.3, 13.7, 17.9}` ms
- `NUM_DIFFUSERS = 4`
- Modulation rate: 0.5 Hz

## See Also

- [ReverbEffect](ReverbEffect.md) - Standard algorithmic reverb (no pitch shift)
- [ReverseDelayEffect](ReverseDelayEffect.md) - Backwards delay for reverse swells
- [DelayEffect](DelayEffect.md) - Use for pre-delay before shimmer
