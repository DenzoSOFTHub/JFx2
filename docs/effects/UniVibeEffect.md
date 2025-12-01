# Uni-Vibe Effect

## Description

The Uni-Vibe effect emulates the classic Shin-ei Uni-Vibe, a photocell-based modulation effect made famous by Jimi Hendrix and Robin Trower. It creates a swirling, psychedelic sound that sits between a phaser and a chorus. The effect uses 4-stage variable phase shifting with an asymmetric LFO sweep that mimics the non-linear response of photocells in the original hardware.

**Category**: Modulation
**ID**: `univibe`
**Display Name**: Uni-Vibe

## How It Works

The Uni-Vibe operates by:
1. Passing the input signal through 4 all-pass filter stages
2. Modulating the cutoff frequencies of these filters using an LFO
3. Using asymmetric LFO shaping to mimic photocell response
4. The filters are tuned to specific frequencies (200, 400, 900, 2000 Hz)
5. In Chorus mode: mixing the phased signal with dry
6. In Vibrato mode: outputting only the phased signal
7. Applying volume compensation

The asymmetric sweep and carefully chosen filter frequencies create the Uni-Vibe's unique character - warmer and more organic than a typical phaser, with a hint of pitch shifting like a chorus.

## Parameters

### Speed
- **ID**: `speed`
- **Range**: 0.5 Hz to 10.0 Hz
- **Default**: 3.0 Hz
- **Unit**: Hz
- **Description**: Rate of the modulation sweep. Slow speeds (0.5-2 Hz) create slow, hypnotic swirls. Medium speeds (2-5 Hz) are classic Uni-Vibe territory. Fast speeds (6-10 Hz) add intense movement.

### Intensity
- **ID**: `intensity`
- **Range**: 0% to 100%
- **Default**: 70%
- **Unit**: %
- **Description**: Depth of the effect. Higher values create more pronounced sweeping and swirling. Controls how far the filter frequencies deviate from their center points. At 0%, minimal effect. At 100%, maximum sweep range.

### Mode
- **ID**: `mode`
- **Type**: Choice
- **Options**:
  - 0: Chorus
  - 1: Vibrato
- **Default**: 0 (Chorus)
- **Description**: Operating mode:
  - **Chorus**: Mixes phased signal with dry signal (50/50). Creates swirling, lush effect while maintaining note clarity.
  - **Vibrato**: Outputs only phased signal. Creates pitch wobble effect for more extreme, psychedelic sound.

### Volume
- **ID**: `volume`
- **Range**: -12 dB to +6 dB
- **Default**: 0 dB
- **Unit**: dB
- **Description**: Output level adjustment. Compensates for perceived volume changes when using different intensity or mode settings. Negative values reduce output, positive values boost.

## Implementation Details

### Signal Flow

```
Input → Stage 1 (200 Hz) → Stage 2 (400 Hz) → Stage 3 (900 Hz) → Stage 4 (2000 Hz) → Mode Mix → Volume → Output
                ↑              ↑                 ↑                  ↑
                └──────────────┴─────────────────┴──────────────────┘
                              LFO with Photocell Shaping
```

### LFO Configuration

- **Waveform**: Sine
- **Shaping**: Asymmetric photocell response curve
- **Update**: Per-sample modulation
- **Stereo**: Mono LFO (same modulation for both channels for pitch coherence)

### Photocell Response Simulation

The original Uni-Vibe used photocells and light bulbs for modulation, which have a non-linear response:

```java
lfoRaw = sine(-1 to +1)
lfoShaped = (lfoRaw + 1.0) * 0.5  // Convert to 0..1
lfoShaped = pow(lfoShaped, 1.5)    // Asymmetric curve (photocell response)
lfoShaped = lfoShaped * 2.0 - 1.0  // Back to -1..1
```

The power function (1.5 exponent) creates:
- Slower attack (rising)
- Faster decay (falling)
- More time spent at higher values
- Asymmetric, organic sweep

This is what distinguishes the Uni-Vibe from standard phasers.

### All-Pass Filter Stages

**Stage Center Frequencies:**
1. 200 Hz (bass)
2. 400 Hz (low-mid)
3. 900 Hz (mid)
4. 2000 Hz (high-mid)

Note: Asymmetric spacing creates more musical character than evenly-spaced stages.

**Frequency Modulation:**
```java
modFreq = baseFreq * (1.0 + lfoShaped * intensity)
```

At 100% intensity, frequencies can roughly double.

**All-Pass Coefficient:**
```java
w0 = 2π * modFreq / sampleRate
tanW0 = tan(w0 / 2)
coefficient = (tanW0 - 1) / (tanW0 + 1)
```

**All-Pass Processing (per stage):**
```java
output = a * (x - y_prev) + x_prev
state = x + a * output
```

This first-order all-pass structure provides frequency-dependent phase shift.

### Mode Implementation

**Chorus Mode:**
```java
output = (dry + phased) * 0.5 * volumeLinear
```
Equal mix of dry and phased signals.

**Vibrato Mode:**
```java
output = phased * volumeLinear
```
Only the phased signal, creating pitch modulation effect.

### Filter Interactions

With 4 stages modulated together:
- Phase shifts accumulate through the stages
- Creates 4 notches in the frequency spectrum
- Asymmetric spacing prevents "hollow" sound
- Lower frequencies shift more dramatically (200-400 Hz range)
- Higher frequencies provide sparkle (900-2000 Hz range)

## Key Characteristics

### Differences from Standard Phaser

| Feature | Uni-Vibe | Standard Phaser |
|---------|----------|-----------------|
| Stages | 4 (asymmetric spacing) | 4-12 (even spacing) |
| LFO | Asymmetric (photocell) | Symmetric |
| Frequencies | 200, 400, 900, 2000 Hz | Swept range |
| Character | Warm, organic | Clean, mechanical |
| Vibrato Mode | Yes | Rare |
| Feedback | No | Usually yes |

### Differences from Chorus

| Feature | Uni-Vibe | Chorus |
|---------|----------|--------|
| Mechanism | Phase shifting | Delay modulation |
| Voices | 1 (4 filter stages) | Multiple (typically 4) |
| Pitch Shift | Subtle | More pronounced |
| Stereo | Mono-ish | Wide stereo |
| Character | Swirly, vocal | Thick, ensemble |

## Usage Tips

### Classic Hendrix Vibe
- **Speed**: 2-4 Hz
- **Intensity**: 70-90%
- **Mode**: Chorus
- **Volume**: 0 dB
- Slow, deep swirl on lead guitar

### Subtle Enhancement
- **Speed**: 1-2 Hz
- **Intensity**: 40-60%
- **Mode**: Chorus
- **Volume**: -2 dB
- Adds dimension without being obvious

### Extreme Vibrato
- **Speed**: 4-6 Hz
- **Intensity**: 80-100%
- **Mode**: Vibrato
- **Volume**: 0 dB
- Psychedelic pitch wobble

### Fast Warble
- **Speed**: 6-10 Hz
- **Intensity**: 60-80%
- **Mode**: Chorus
- **Volume**: 0 dB
- Intense, tremolo-like movement

### Slow Throb
- **Speed**: 0.5-1.5 Hz
- **Intensity**: 60-80%
- **Mode**: Chorus
- **Volume**: 0 dB
- Very slow, hypnotic sweep

### Robin Trower Tone
- **Speed**: 3-5 Hz
- **Intensity**: 75-95%
- **Mode**: Chorus
- **Volume**: +2 dB
- Classic rock lead sound

### Rhythm Texture
- **Speed**: 2-3 Hz
- **Intensity**: 50-70%
- **Mode**: Chorus
- **Volume**: -1 dB
- Adds movement to rhythm parts

### Ambient Swirl
- **Speed**: 0.7-1.5 Hz
- **Intensity**: 70-90%
- **Mode**: Vibrato
- **Volume**: -2 dB
- Spacey, floating effect

## Recommended Signal Chain Position

The Uni-Vibe traditionally works best:
1. **After overdrive/distortion**: Modulates the distorted signal for classic tones
2. **Before delay/reverb**: Keeps the modulation focused, prevents muddiness
3. **With moderate gain**: Too much distortion can mask the effect

## Historical Context

The Shin-ei Uni-Vibe was introduced in the late 1960s and gained fame through Jimi Hendrix's use on tracks like "Machine Gun" and "The Wind Cries Mary." It used a light bulb and photocells to control the phase shift, creating an organic, pulsing quality that couldn't be achieved with purely electronic modulation. The effect has been used by David Gilmour, Robin Trower, Ernie Isley, and many others seeking that warm, swirling psychedelic sound.

## Technical Specifications

- **Latency**: Minimal (single-sample processing)
- **Stages**: 4 first-order all-pass filters
- **Stage Frequencies**: 200, 400, 900, 2000 Hz (asymmetric)
- **LFO Shaping**: Power curve (1.5 exponent) for photocell simulation
- **Sample Rate**: Adapts to system sample rate
- **Processing**: 32-bit float internal processing
- **Stereo**: True stereo with same modulation (mono LFO for pitch coherence)
- **Modes**: 2 (Chorus and Vibrato)
- **CPU Usage**: Low (4 filter stages + simple LFO)
