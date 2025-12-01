# JFx2 Effects Reference

This document describes all audio effects available in JFx2, their parameters, and stereo handling implementation.

## Table of Contents

1. [Dynamics Effects](#dynamics-effects)
2. [Distortion Effects](#distortion-effects)
3. [Delay Effects](#delay-effects)
4. [Reverb Effects](#reverb-effects)
5. [Modulation Effects](#modulation-effects)
6. [EQ Effects](#eq-effects)
7. [Filter Effects](#filter-effects)
8. [Amp Simulation Effects](#amp-simulation-effects)
9. [Pitch Effects](#pitch-effects)
10. [Utility Effects](#utility-effects)
11. [I/O Effects](#io-effects)

---

## Dynamics Effects

### GainEffect
**ID:** `gain` | **Category:** DYNAMICS

Simple gain/volume control that adjusts the signal level in decibels.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| gain | -60 to +24 | 0 | dB | Volume adjustment level |

**Stereo:** Stateless - applies identical gain to both channels.

---

### NoiseGateEffect
**ID:** `noisegate` | **Category:** DYNAMICS

Attenuates signal when it falls below the threshold. Uses 5ms lookahead to preserve attack transients.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| threshold | -80 to 0 | -40 | dB | Gate open threshold |
| attack | 0.1 to 50 | 1 | ms | Gate opening speed |
| hold | 0 to 500 | 50 | ms | Hold time after signal drops |
| release | 5 to 500 | 100 | ms | Gate closing speed |
| range | -80 to 0 | -80 | dB | Maximum attenuation |

**Stereo:** Full dual-mono with independent envelope followers, gate gain, hold counters, and lookahead buffers per channel.

---

### CompressorEffect
**ID:** `compressor` | **Category:** DYNAMICS

Dynamic range compressor with RMS detection and soft knee support.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| threshold | -60 to 0 | -20 | dB | Compression threshold |
| ratio | 1 to 20 | 4 | :1 | Compression ratio |
| attack | 0.1 to 100 | 10 | ms | Attack time |
| release | 10 to 1000 | 100 | ms | Release time |
| knee | 0 to 12 | 3 | dB | Soft knee width |
| makeup | 0 to 24 | 0 | dB | Makeup gain |

**Stereo:** Dual-mono compression with independent RMS detection per channel.

---

### LimiterEffect
**ID:** `limiter` | **Category:** DYNAMICS

Brickwall limiter with 5ms lookahead for transparent peak control.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| threshold | -24 to 0 | -6 | dB | Limiting threshold |
| release | 10 to 1000 | 100 | ms | Release time |
| ceiling | -6 to 0 | -0.3 | dB | Maximum output level |
| knee | 0 to 6 | 0 | dB | Knee softness |

**Stereo:** Stereo-linked envelope (uses max of L/R) to prevent stereo image shift.

---

### VolumeSwellEffect
**ID:** `volumeswell` | **Category:** DYNAMICS

Creates violin-like swells by automatically fading in the attack of each note.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| attack | 10 to 2000 | 300 | ms | Swell rise time |
| sensitivity | -60 to -20 | -40 | dB | Onset detection threshold |
| hold | 50 to 500 | 100 | ms | Minimum time between triggers |
| curve | 0.5 to 2.0 | 1.0 | - | Swell shape curve |

**Stereo:** Stereo-linked envelope detection with same swell gain applied to both channels.

---

### SustainerEffect
**ID:** `sustainer` | **Category:** DYNAMICS

E-Bow/Fernandes Sustainer simulation with infinite sustain using aggressive compression.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| sustain | 0 to 100 | 80 | % | Sustain amount |
| attack | 1 to 100 | 20 | ms | Sustain kick-in time |
| tone | 500 to 8000 | 3000 | Hz | Brightness control |
| mode | Natural/Harmonic/Fundamental | 0 | - | Sustain character |
| mix | 0 to 100 | 100 | % | Dry/wet mix |
| sensitivity | -40 to 0 | -20 | dB | Input sensitivity |

**Stereo:** Stereo-linked envelope/gain with separate L/R tone filters.

---

## Distortion Effects

### OverdriveEffect
**ID:** `overdrive` | **Category:** DISTORTION

Classic warm overdrive with soft tanh clipping. Emulates tube amp breakup.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| drive | 1 to 50 | 5 | x | Saturation amount |
| tone | 200 to 8000 | 3000 | Hz | Brightness control |
| level | -20 to +6 | 0 | dB | Output volume |

**Stereo:** Full dual-mono with separate L/R filters (input HPF, tone, output LPF).

---

### DriveEffect
**ID:** `drive` | **Category:** DISTORTION

Tube-style drive with asymmetric clipping for even harmonic content and mid-boost.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| gain | 1 to 30 | 8 | - | Drive/saturation amount |
| tone | 500 to 6000 | 2500 | Hz | Output tone |
| body | 0 to 100 | 50 | % | Mid-range boost amount |
| level | -20 to +6 | 0 | dB | Output volume |

**Stereo:** Full dual-mono with separate L/R filters (input HPF, mid boost, tone, output LPF).

---

### DistortionEffect
**ID:** `distortion` | **Category:** DISTORTION

High-gain distortion with pre/post tone controls and selectable clipping types.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| drive | 1 to 100 | 20 | x | Distortion amount |
| preTone | -12 to +12 | 0 | dB | Pre-clipping brightness |
| postTone | 500 to 8000 | 4000 | Hz | Post-clipping warmth |
| clipType | Soft/Hard/Asymmetric | 0 | - | Clipping algorithm |
| level | -20 to +6 | -6 | dB | Output volume |

**Stereo:** Full dual-mono with separate L/R filters.

---

### FuzzEffect
**ID:** `fuzz` | **Category:** DISTORTION

Classic 1960s/70s fuzz with aggressive square-wave-like distortion.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| fuzz | 1 to 100 | 70 | - | Fuzz/gain amount |
| tone | 200 to 5000 | 1500 | Hz | Output tone |
| type | Classic/Muff/Octave | 0 | - | Fuzz character |
| sustain | 0 to 100 | 60 | % | Compression/sustain |
| level | -20 to +6 | -3 | dB | Output volume |

**Types:**
- **Classic:** Fuzz Face style
- **Muff:** Big Muff style with scooped mids
- **Octave:** Octavia style with upper octave

**Stereo:** Full dual-mono with separate L/R filters and octave state.

---

## Delay Effects

### DelayEffect
**ID:** `delay` | **Category:** DELAY

Digital delay with feedback and optional BPM sync.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| time | 10 to 2000 | 400 | ms | Delay time |
| feedback | 0 to 95 | 40 | % | Feedback amount |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| filter | 500 to 12000 | 6000 | Hz | Feedback lowpass cutoff |
| sync | true/false | false | - | Enable BPM sync |
| bpm | 40 to 240 | 120 | - | Tempo for sync |
| division | 1/1 to 1/16T | 1/4 | - | Note division |

**Stereo:** Full dual-mono with separate L/R delay lines and filters.

---

### TapeEchoEffect
**ID:** `tapeecho` | **Category:** DELAY

Vintage tape echo emulation with wow, flutter, and saturation.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| time | 50 to 1000 | 300 | ms | Delay time |
| feedback | 0 to 95 | 50 | % | Feedback amount |
| mix | 0 to 100 | 40 | % | Dry/wet balance |
| tone | 1000 to 8000 | 3000 | Hz | High frequency cutoff |
| wow | 0 to 100 | 20 | % | Slow pitch variation |
| flutter | 0 to 100 | 15 | % | Fast pitch variation |
| mode | Single/Multi/Slapback | 0 | - | Playback head mode |
| saturation | 0 to 100 | 30 | % | Tape saturation |

**Stereo:** Full dual-mono with separate L/R delay lines and filters. LFOs shared (simulates single transport).

---

### MultiTapDelayEffect
**ID:** `multitap` | **Category:** DELAY

Rhythmic delay with 4 taps and preset patterns.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| time | 100 to 1500 | 500 | ms | Base delay time |
| pattern | Quarter/Eighth/Dotted/Triplet/Random | 0 | - | Tap pattern |
| feedback | 0 to 80 | 30 | % | Feedback from last tap |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| tone | 1000 to 12000 | 6000 | Hz | High frequency cutoff |
| spread | 0 to 100 | 50 | % | Stereo spread of taps |

**Stereo:** Full dual-mono with taps panned alternately based on spread parameter.

---

### ReverseDelayEffect
**ID:** `reversedelay` | **Category:** DELAY

Creates backwards-sounding echoes using double-buffering with crossfade.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| time | 100 to 2000 | 500 | ms | Reversed segment length |
| feedback | 0 to 80 | 30 | % | Feedback amount |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| crossfade | 5 to 50 | 20 | % | Overlap between segments |

**Stereo:** Full dual-mono with separate L/R double buffers.

---

### PingPongDelayEffect
**ID:** `pingpong` | **Category:** DELAY

Stereo bouncing delay with echoes alternating between L/R channels.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| time | 50 to 1000 | 300 | ms | Delay time per bounce |
| feedback | 0 to 90 | 50 | % | Number of bounces |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| width | 0 to 100 | 100 | % | Stereo width |
| tone | 1000 to 12000 | 6000 | Hz | High frequency cutoff |
| offset | -50 to +50 | 0 | % | Time offset between channels |

**Stereo:** Cross-feedback routing (L receives R's feedback and vice versa).

---

## Reverb Effects

### ReverbEffect
**ID:** `reverb` | **Category:** REVERB

Room reverb based on Freeverb algorithm with 8 comb + 4 allpass filters.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| roomSize | 0 to 100 | 50 | % | Simulated room size |
| damp | 0 to 100 | 50 | % | High frequency absorption |
| width | 0 to 100 | 100 | % | Stereo spread |
| mix | 0 to 100 | 30 | % | Dry/wet balance |
| predelay | 0 to 100 | 10 | ms | Time before reverb starts |

**Stereo:** Full stereo with different delay tunings for L/R channels.

---

### SpringReverbEffect
**ID:** `springreverb` | **Category:** REVERB

Amp-style spring reverb with characteristic metallic "drip" sound.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| mix | 0 to 100 | 40 | % | Dry/wet balance |
| decay | 0.5 to 4 | 2 | s | Reverb tail length |
| tone | 500 to 5000 | 2000 | Hz | High frequency content |
| drip | 0 to 100 | 50 | % | Spring drip character |
| tension | 0 to 100 | 50 | % | Spring tension |

**Stereo:** Full dual-mono with 3 parallel springs per channel and timing offsets.

---

### ShimmerReverbEffect
**ID:** `shimmerreverb` | **Category:** REVERB

Ethereal ambient reverb with pitch shifting for crystalline harmonics.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| decay | 1 to 30 | 10 | s | Reverb tail length |
| shimmer | 0 to 100 | 50 | % | Pitch-shifted feedback |
| pitch | 0 to 24 | 12 | semitones | Pitch shift (12 = octave) |
| dampening | 1000 to 16000 | 8000 | Hz | High frequency absorption |
| modulation | 0 to 100 | 30 | % | Chorus-like modulation |

**Stereo:** Full dual-mono with separate diffusers, tanks, and pitch buffers.

---

## Modulation Effects

### ChorusEffect
**ID:** `chorus` | **Category:** MODULATION

4-voice chorus for rich, shimmering ensemble sound.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.1 to 5.0 | 0.8 | Hz | Modulation speed |
| depth | 0 to 100 | 50 | % | Pitch modulation amount |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| spread | 0 to 100 | 80 | % | Stereo width |

**Stereo:** Full dual-mono with 4 delay lines and LFOs per channel (R offset by 0.5 phase).

---

### FlangerEffect
**ID:** `flanger` | **Category:** MODULATION

Classic flanger with "jet plane" sweeping comb filter effect.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.05 to 5.0 | 0.5 | Hz | Sweep speed |
| depth | 0 to 100 | 70 | % | Sweep intensity |
| delay | 0.5 to 10 | 2 | ms | Base delay time |
| feedback | -90 to +90 | 50 | % | Resonance (negative = inverted) |
| mix | 0 to 100 | 50 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R delay lines, LFOs (0.5 phase offset), and feedback.

---

### PhaserEffect
**ID:** `phaser` | **Category:** MODULATION

6-stage phaser with sweeping notch/peak movement.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.05 to 5.0 | 0.5 | Hz | Sweep speed |
| depth | 0 to 100 | 70 | % | Frequency sweep range |
| feedback | -90 to +90 | 40 | % | Effect intensity |
| mix | 0 to 100 | 50 | % | Dry/wet balance |
| center | 200 to 2000 | 800 | Hz | Center frequency |

**Stereo:** Full dual-mono with separate allpass states and LFOs (180 degrees offset).

---

### TremoloEffect
**ID:** `tremolo` | **Category:** MODULATION

Classic amplitude modulation effect.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.5 to 15 | 5 | Hz | Volume modulation speed |
| depth | 0 to 100 | 50 | % | Intensity of volume changes |
| waveform | Sine/Triangle/Square/Random | 0 | - | Modulation shape |

**Stereo:** Separate L/R LFOs with 0.25 phase offset for stereo movement.

---

### VibratoEffect
**ID:** `vibrato` | **Category:** MODULATION

Pitch modulation effect with optional rise time for natural feel.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.5 to 10 | 5 | Hz | Vibrato speed |
| depth | 0 to 100 | 30 | cents | Pitch variation amount |
| waveform | Sine/Triangle | 0 | - | Modulation shape |
| rise | 0 to 1000 | 0 | ms | Time for vibrato to reach full depth |

**Stereo:** Separate L/R delay lines, shared LFO (correct for coherent pitch bend).

---

### PannerEffect
**ID:** `panner` | **Category:** MODULATION

Auto-panner with constant-power panning between L/R channels.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| rate | 0.1 to 10 | 1 | Hz | Panning speed |
| depth | 0 to 100 | 100 | % | Pan sweep width |
| waveform | Sine/Triangle/Square | 0 | - | Panning shape |
| smooth | 0 to 100 | 20 | % | Transition smoothing |

**Stereo:** Single LFO controlling pan position (correct design for panning).

---

### RingModulatorEffect
**ID:** `ringmod` | **Category:** MODULATION

Multiplies signal by carrier oscillator for metallic, bell-like tones.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| freq | 20 to 4000 | 440 | Hz | Carrier frequency |
| waveform | Sine/Square/Triangle/Saw | 0 | - | Carrier wave shape |
| lfoDepth | 0 to 100 | 0 | % | Frequency modulation depth |
| lfoRate | 0.1 to 10 | 1 | Hz | Frequency modulation speed |
| mix | 0 to 100 | 50 | % | Dry/wet balance |

**Stereo:** Separate L/R carrier phases and LFOs (0.25 phase offset).

---

### UniVibeEffect
**ID:** `univibe` | **Category:** MODULATION

Shin-ei Uni-Vibe photocell phaser/chorus emulation.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| speed | 0.5 to 10 | 3 | Hz | Modulation rate |
| intensity | 0 to 100 | 70 | % | Effect depth |
| mode | Chorus/Vibrato | 0 | - | Mix mode |
| volume | -12 to +6 | 0 | dB | Output level |

**Stereo:** Separate L/R allpass filters, shared LFO (matches original mono pedal).

---

### RotaryEffect
**ID:** `rotary` | **Category:** MODULATION

Leslie rotating speaker simulation with horn and drum rotors.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| speed | Slow/Fast/Brake | 0 | - | Rotor speed mode |
| horn | 0 to 100 | 100 | % | Treble rotor level |
| drum | 0 to 100 | 70 | % | Bass rotor level |
| doppler | 0 to 100 | 50 | % | Doppler pitch shift |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with 180-degree phase offset simulating opposite-side mic placement.

---

## EQ Effects

### FilterEffect
**ID:** `filter` | **Category:** EQ

Multi-band filter with up to 5 configurable biquad bands.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| type1-5 | None/LP/HP/BP/Notch/Peak/LowShelf/HighShelf/AP | 0 | - | Filter type per band |
| freq1-5 | 20 to 20000 | varies | Hz | Frequency per band |
| q1-5 | 0.1 to 20 | 0.707 | - | Q/resonance per band |
| gain1-5 | -24 to +24 | 0 | dB | Gain per band (Peak/Shelf only) |
| output | -12 to +12 | 0 | dB | Output level |

**Stereo:** Full dual-mono with separate L/R filter instances.

---

### ParametricEQEffect
**ID:** `parametriceq` | **Category:** EQ

4-band parametric EQ with Low Shelf, 2 Mid Peak, and High Shelf bands.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| lowFreq | 20 to 500 | 100 | Hz | Low shelf corner |
| lowGain | -15 to +15 | 0 | dB | Low shelf gain |
| lowMidFreq | 100 to 2000 | 400 | Hz | Low-mid center |
| lowMidGain | -15 to +15 | 0 | dB | Low-mid gain |
| lowMidQ | 0.1 to 10 | 1.0 | - | Low-mid bandwidth |
| highMidFreq | 500 to 8000 | 2000 | Hz | High-mid center |
| highMidGain | -15 to +15 | 0 | dB | High-mid gain |
| highMidQ | 0.1 to 10 | 1.0 | - | High-mid bandwidth |
| highFreq | 2000 to 16000 | 8000 | Hz | High shelf corner |
| highGain | -15 to +15 | 0 | dB | High shelf gain |
| output | -12 to +12 | 0 | dB | Output level |

**Stereo:** Full dual-mono with separate L/R filter instances.

---

### GraphicEQEffect
**ID:** `graphiceq` | **Category:** EQ

10-band graphic EQ with ISO standard frequencies.

**Bands:** 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| gain1-10 | -12 to +12 | 0 | dB | Gain per band |
| q | 0.5 to 4.0 | 1.5 | - | Shared Q for all bands |
| output | -12 to +12 | 0 | dB | Output level |

**Stereo:** Full dual-mono with separate L/R filter instances.

---

## Filter Effects

### WahEffect
**ID:** `wah` | **Category:** FILTER

Classic wah pedal with Auto, LFO, and Manual modes.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| mode | Auto/LFO/Manual | 0 | - | Control mode |
| position | 0 to 100 | 50 | % | Manual position |
| minFreq | 200 to 800 | 400 | Hz | Minimum sweep frequency |
| maxFreq | 1000 to 5000 | 2500 | Hz | Maximum sweep frequency |
| resonance | 1 to 20 | 8 | - | Filter resonance |
| lfoRate | 0.1 to 10 | 1 | Hz | LFO speed |
| sensitivity | 0 to 100 | 50 | % | Auto-wah sensitivity |
| attack | 1 to 100 | 10 | ms | Envelope attack |
| release | 10 to 500 | 100 | ms | Envelope release |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R state variable filters and envelopes.

---

### EnvelopeFilterEffect
**ID:** `envelopefilter` | **Category:** FILTER

Dynamic auto-wah that responds to input level.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| sensitivity | -40 to 0 | -20 | dB | Input sensitivity |
| attack | 1 to 50 | 10 | ms | Envelope attack |
| decay | 50 to 1000 | 200 | ms | Envelope decay |
| range | 100 to 4000 | 2000 | Hz | Frequency sweep range |
| resonance | 0.5 to 10 | 4 | - | Filter Q |
| type | LP/BP/HP | 1 | - | Filter type |
| direction | Up/Down | 0 | - | Sweep direction |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R filters, stereo-linked envelope detection.

---

### TalkBoxEffect
**ID:** `talkbox` | **Category:** FILTER

Formant filter simulating vowel sounds (A, E, I, O, U).

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| vowel | A/E/I/O/U | 0 | - | Base vowel |
| morph | 0 to 100 | 0 | % | Blend to next vowel |
| resonance | 2 to 15 | 8 | - | Formant Q |
| rate | 0 to 5 | 0 | Hz | LFO rate (0 = manual) |
| depth | 0 to 100 | 100 | % | LFO depth |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with 3 formant filters per channel.

---

### SynthEffect
**ID:** `synth` | **Category:** FILTER

Guitar-to-synth with pitch tracking and oscillator generation.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| waveform | Square/Saw/Triangle/Sine | 0 | - | Oscillator waveform |
| octave | -1/Normal/+1/+2 | 1 | Oct | Pitch shift |
| filter | 100 to 8000 | 2000 | Hz | Lowpass cutoff |
| resonance | 0.5 to 10 | 2 | - | Filter Q |
| attack | 1 to 500 | 10 | ms | Envelope attack |
| release | 10 to 2000 | 200 | ms | Envelope release |
| lfoRate | 0 to 10 | 2 | Hz | Filter LFO rate |
| lfoDepth | 0 to 100 | 30 | % | Filter LFO depth |
| mix | 0 to 100 | 100 | % | Dry/wet balance |
| glide | 0 to 500 | 50 | ms | Portamento time |

**Stereo:** Mono pitch detection and oscillator, separate L/R output filters.

---

## Amp Simulation Effects

### AmpEffect
**ID:** `amp` | **Category:** AMP_SIM

Parametric guitar amplifier simulation with preamp, tone stack, and power amp.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| inputGain | -12 to +12 | 0 | dB | Input level |
| bright | true/false | false | - | High frequency sparkle |
| preampGain | 0 to 100 | 50 | % | Preamp drive |
| bass | 0 to 10 | 5 | - | Low frequency tone |
| mid | 0 to 10 | 5 | - | Midrange tone |
| treble | 0 to 10 | 5 | - | High frequency tone |
| presence | 0 to 10 | 5 | - | Upper harmonics |
| sag | 0 to 100 | 30 | % | Power supply compression |
| master | -60 to +6 | -6 | dB | Output volume |

**Stereo:** Full dual-mono with 6 filters + power amp state per channel.

---

### CabinetSimEffect
**ID:** `cabsim` | **Category:** AMP_SIM

Speaker cabinet simulation using synthesized impulse responses.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| cabinet | 1x12 Vintage/2x12 Open/4x12 British/4x12 Modern/Direct | 0 | - | Cabinet type |
| mic | Center/Edge/Back/Room | 0 | - | Mic position |
| lowCut | 20 to 500 | 80 | Hz | High-pass filter |
| highCut | 2000 to 12000 | 8000 | Hz | Low-pass filter |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R FFT convolvers.

---

### IRLoaderEffect
**ID:** `irloader` | **Category:** AMP_SIM

Load and apply external impulse response WAV files.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| mix | 0 to 100 | 100 | % | Dry/wet balance |
| gain | -24 to +12 | 0 | dB | Output gain |
| predelay | 0 to 50 | 0 | ms | Pre-delay |
| lowcut | 20 to 500 | 20 | Hz | High-pass filter |
| highcut | 2000 to 20000 | 20000 | Hz | Low-pass filter |
| trim | 10 to 100 | 100 | % | IR length to use |

**Supported formats:** 8/16/24/32-bit WAV (PCM and float), mono or stereo.

**Stereo:** Full dual-mono with separate L/R convolvers. Stereo IR files maintain true stereo.

---

## Pitch Effects

### PitchShifterEffect
**ID:** `pitchshift` | **Category:** PITCH

Granular pitch shifter with +/- 12 semitones range.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| shift | -12 to +12 | 0 | semitones | Main pitch shift |
| fine | -100 to +100 | 0 | cents | Fine adjustment |
| grain | 20 to 100 | 50 | ms | Processing window size |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R buffers and grain phase tracking.

---

### OctaverEffect
**ID:** `octaver` | **Category:** PITCH

Classic analog-style octave up and down effect.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| octDown | 0 to 100 | 50 | % | Octave-down level |
| octUp | 0 to 100 | 0 | % | Octave-up level |
| dry | 0 to 100 | 50 | % | Original signal level |
| lpf | 200 to 2000 | 800 | Hz | Tone filter |
| tracking | 0 to 100 | 50 | % | Zero-crossing sensitivity |

**Techniques:**
- **Octave Down:** Flip-flop frequency division
- **Octave Up:** Full-wave rectification

**Stereo:** Full dual-mono with separate L/R filters and tracking state.

---

## Utility Effects

### SplitterEffect
**ID:** `splitter` | **Category:** UTILITY

Splits signal into multiple outputs for parallel processing.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| outputs | 2 to 4 | 2 | - | Number of output paths |

**Stereo:** Stateless pass-through.

---

### MixerEffect
**ID:** `mixer` | **Category:** UTILITY

Mixes multiple inputs with gain compensation.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| inputs | 2 to 4 | 2 | - | Number of input paths |
| gain | -12 to +6 | 0 | dB | Output gain |

**Stereo:** Stateless gain application.

---

### MonoToStereoEffect
**ID:** `mono2stereo` | **Category:** UTILITY

Transforms mono signal into stereo using psychoacoustic techniques.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| pan | -100 to +100 | 0 | % | Stereo position |
| width | 0 to 200 | 100 | % | Stereo spread |
| haasDelay | 0 to 40 | 0 | ms | Haas effect delay |
| haasBalance | -100 to +100 | 0 | % | Delayed channel |
| enhance | 0 to 100 | 0 | % | Frequency-based enhancement |
| enhanceFreq | 200 to 4000 | 1000 | Hz | Enhancement frequency |
| lfoRate | 0 to 5 | 0 | Hz | Auto-pan rate |
| lfoDepth | 0 to 100 | 0 | % | Auto-pan depth |
| mix | 0 to 100 | 100 | % | Dry/wet balance |

**Stereo:** Full dual-mono with separate L/R delay lines, filters, and allpass filters.

---

## I/O Effects

### AudioInputEffect
**ID:** `audioinput` | **Category:** INPUT_SOURCE

Captures audio from hardware input device.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| device | (available devices) | 0 | - | Input device selection |
| gain | -60 to +24 | 0 | dB | Input level |
| mute | true/false | false | - | Mute input |

**Stereo:** Mono input device, duplicates to both channels.

---

### AudioOutputEffect
**ID:** `audiooutput` | **Category:** OUTPUT_SINK

Sends audio to hardware output device.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| device | (available devices) | 0 | - | Output device selection |
| gain | -60 to +24 | 0 | dB | Output level |
| mute | true/false | false | - | Mute output |

**Stereo:** Stereo output device.

---

### WavFileInputEffect
**ID:** `wavfileinput` | **Category:** INPUT_SOURCE

Plays WAV files with looping support.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| volume | -60 to +12 | 0 | dB | Playback volume |
| loopCount | -1 to 100 | 0 | - | Loops (-1 = infinite) |
| playing | true/false | false | - | Start/stop playback |

**Supported formats:** 8/16/24/32-bit WAV.

**Note:** Stereo WAV files are converted to mono on load.

---

### WavFileOutputEffect
**ID:** `wavfileoutput` | **Category:** OUTPUT_SINK

Records audio to WAV files.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| recording | true/false | false | - | Start/stop recording |

**Output format:** 16-bit PCM WAV.

**Note:** Records mono (L+R summed).

---

### OscillatorEffect
**ID:** `oscillator` | **Category:** INPUT_SOURCE

Generates test tones and waveforms.

| Parameter | Range | Default | Unit | Description |
|-----------|-------|---------|------|-------------|
| waveform | Sine/Triangle/Sawtooth/Square | 0 | - | Wave shape |
| frequency | 20 to 2000 | 440 | Hz | Oscillator pitch |
| volume | -60 to 0 | -12 | dB | Output level |
| playing | true/false | false | - | Enable output |

**Stereo:** Mono oscillator, identical output on both channels.

---

## Stereo Handling Summary

All 47 effects properly implement `onProcessStereo()` with appropriate stereo handling:

| Category | Total | Full Dual-Mono | Stereo-Linked | Stateless |
|----------|-------|----------------|---------------|-----------|
| Dynamics | 6 | 2 | 3 | 1 |
| Distortion | 4 | 4 | 0 | 0 |
| Delay | 5 | 5 | 0 | 0 |
| Reverb | 3 | 3 | 0 | 0 |
| Modulation | 9 | 7 | 2 | 0 |
| EQ | 3 | 3 | 0 | 0 |
| Filter | 4 | 3 | 1 | 0 |
| Amp Sim | 3 | 3 | 0 | 0 |
| Pitch | 2 | 2 | 0 | 0 |
| Utility | 3 | 1 | 0 | 2 |
| I/O | 5 | 0 | 0 | 5 |
| **Total** | **47** | **33** | **6** | **8** |

### Design Guidelines

- **Dual-Mono:** Effects with filters, delay lines, or envelope followers should maintain separate L/R state
- **Stereo-Linked:** Dynamics effects may use linked envelope (max of L/R) to prevent stereo image shift
- **LFO Offset:** Modulation effects typically use 0.25-0.5 phase offset between L/R for stereo width
- **Stateless:** Simple gain/routing effects can process channels identically
