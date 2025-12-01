package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Guitar Synth effect.
 *
 * <p>Transforms guitar signal into synthesizer-like tones by
 * tracking the input pitch and generating oscillator waveforms.
 * Simple monophonic synth with filter and modulation.</p>
 *
 * <p>Features:
 * - Pitch tracking from guitar input
 * - Multiple oscillator waveforms
 * - Resonant lowpass filter
 * - LFO modulation for filter/pitch
 * - Envelope follower for dynamics</p>
 */
public class SynthEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "synth",
            "Synth",
            "Guitar-to-synth with pitch tracking",
            EffectCategory.FILTER
    );

    private static final String[] WAVEFORM_NAMES = {
            "Square",
            "Saw",
            "Triangle",
            "Sine"
    };

    private static final String[] OCTAVE_NAMES = {
            "-1 Oct",
            "Normal",
            "+1 Oct",
            "+2 Oct"
    };

    // Parameters
    private final Parameter waveformParam;
    private final Parameter octaveParam;
    private final Parameter filterParam;
    private final Parameter resonanceParam;
    private final Parameter attackParam;
    private final Parameter releaseParam;
    private final Parameter lfoRateParam;
    private final Parameter lfoDepthParam;
    private final Parameter mixParam;
    private final Parameter glideParam;

    // Pitch detection
    private float[] pitchBuffer;
    private int pitchBufferPos;
    private int pitchBufferSize;
    private float detectedFreq = 0;
    private float lastZeroCrossing = 0;
    private int zeroCrossingCount = 0;
    private float prevSample = 0;

    // Oscillator
    private float oscPhase = 0;
    private float currentFreq = 0;

    // Envelope
    private float envelope = 0;

    // Filter
    private BiquadFilter filterL, filterR;

    // LFO
    private LFO lfo;

    public SynthEffect() {
        super(METADATA);

        // Waveform
        waveformParam = addChoiceParameter("waveform", "Wave",
                "Oscillator waveform. Square = classic synth, Saw = bright, buzzy.",
                WAVEFORM_NAMES, 0);

        // Octave
        octaveParam = addChoiceParameter("octave", "Octave",
                "Pitch shift in octaves.",
                OCTAVE_NAMES, 1);

        // Filter cutoff: 100 Hz to 8000 Hz, default 2000 Hz
        filterParam = addFloatParameter("filter", "Filter",
                "Lowpass filter cutoff frequency.",
                100.0f, 8000.0f, 2000.0f, "Hz");

        // Resonance: 0.5 to 10, default 2
        resonanceParam = addFloatParameter("resonance", "Q",
                "Filter resonance. Higher = more pronounced filter sweep.",
                0.5f, 10.0f, 2.0f, "");

        // Attack: 1 ms to 500 ms, default 10 ms
        attackParam = addFloatParameter("attack", "Attack",
                "Envelope attack time.",
                1.0f, 500.0f, 10.0f, "ms");

        // Release: 10 ms to 2000 ms, default 200 ms
        releaseParam = addFloatParameter("release", "Release",
                "Envelope release time.",
                10.0f, 2000.0f, 200.0f, "ms");

        // LFO Rate: 0 to 10 Hz, default 2 Hz
        lfoRateParam = addFloatParameter("lfoRate", "LFO Rate",
                "Speed of filter/pitch modulation.",
                0.0f, 10.0f, 2.0f, "Hz");

        // LFO Depth: 0% to 100%, default 30%
        lfoDepthParam = addFloatParameter("lfoDepth", "LFO Depth",
                "Amount of LFO modulation on filter.",
                0.0f, 100.0f, 30.0f, "%");

        // Mix: 0% to 100%, default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry guitar and synth.",
                0.0f, 100.0f, 100.0f, "%");

        // Glide: 0 ms to 500 ms, default 50 ms
        glideParam = addFloatParameter("glide", "Glide",
                "Portamento time between notes.",
                0.0f, 500.0f, 50.0f, "ms");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Pitch detection buffer (enough for ~50Hz lowest freq)
        pitchBufferSize = sampleRate / 40;
        pitchBuffer = new float[pitchBufferSize];
        pitchBufferPos = 0;
        detectedFreq = 0;
        zeroCrossingCount = 0;
        lastZeroCrossing = 0;
        prevSample = 0;

        oscPhase = 0;
        currentFreq = 0;
        envelope = 0;

        filterL = new BiquadFilter();
        filterL.setSampleRate(sampleRate);
        filterL.configure(FilterType.LOWPASS, filterParam.getValue(), resonanceParam.getValue(), 0.0f);

        filterR = new BiquadFilter();
        filterR.setSampleRate(sampleRate);
        filterR.configure(FilterType.LOWPASS, filterParam.getValue(), resonanceParam.getValue(), 0.0f);

        lfo = new LFO(LFO.Waveform.TRIANGLE, lfoRateParam.getValue(), sampleRate);
    }

    /**
     * Simple zero-crossing pitch detection.
     */
    private float detectPitch(float sample) {
        // Store in buffer
        pitchBuffer[pitchBufferPos] = sample;
        pitchBufferPos = (pitchBufferPos + 1) % pitchBufferSize;

        // Zero crossing detection (positive to negative)
        if (prevSample >= 0 && sample < 0) {
            float period = pitchBufferPos - lastZeroCrossing;
            if (period < 0) period += pitchBufferSize;

            if (period > 10 && period < pitchBufferSize - 10) {
                float freq = sampleRate / period;
                // Only accept reasonable guitar frequencies (80 Hz to 1200 Hz)
                if (freq > 80 && freq < 1200) {
                    // Smooth the frequency
                    if (detectedFreq == 0) {
                        detectedFreq = freq;
                    } else {
                        detectedFreq = detectedFreq * 0.9f + freq * 0.1f;
                    }
                }
            }
            lastZeroCrossing = pitchBufferPos;
        }
        prevSample = sample;

        return detectedFreq;
    }

    /**
     * Generate oscillator sample.
     */
    private float oscillator(int waveform, float phase) {
        switch (waveform) {
            case 0: // Square
                return phase < 0.5f ? 1.0f : -1.0f;
            case 1: // Saw
                return 2.0f * phase - 1.0f;
            case 2: // Triangle
                return phase < 0.5f ? 4.0f * phase - 1.0f : 3.0f - 4.0f * phase;
            case 3: // Sine
            default:
                return (float) Math.sin(phase * 2.0 * Math.PI);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int waveform = (int) waveformParam.getValue();
        int octaveShift = (int) octaveParam.getValue() - 1; // -1, 0, +1, +2
        float filterFreq = filterParam.getValue();
        float resonance = resonanceParam.getValue();
        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float lfoRate = lfoRateParam.getValue();
        float lfoDepth = lfoDepthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float glideMs = glideParam.getValue();

        lfo.setFrequency(lfoRate);

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        float glideCoeff = glideMs > 0 ? (float) Math.exp(-1.0 / (glideMs * sampleRate / 1000.0)) : 0;

        float octaveMult = (float) Math.pow(2.0, octaveShift);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];
            float absInput = Math.abs(dry);

            // Envelope follower
            if (absInput > envelope) {
                envelope = attackCoeff * envelope + (1 - attackCoeff) * absInput;
            } else {
                envelope = releaseCoeff * envelope;
            }

            // Pitch detection
            float detectedPitch = detectPitch(dry);

            // Apply octave shift
            float targetFreq = detectedPitch * octaveMult;

            // Glide to target frequency
            if (glideMs > 0 && currentFreq > 0) {
                currentFreq = glideCoeff * currentFreq + (1 - glideCoeff) * targetFreq;
            } else {
                currentFreq = targetFreq;
            }

            // Generate oscillator
            float osc = 0;
            if (currentFreq > 20 && envelope > 0.01f) {
                osc = oscillator(waveform, oscPhase);

                // Advance phase
                oscPhase += currentFreq / sampleRate;
                if (oscPhase >= 1.0f) oscPhase -= 1.0f;

                // Apply envelope
                osc *= Math.min(envelope * 3.0f, 1.0f);
            }

            // LFO modulation on filter
            float lfoValue = lfo.tick();
            float modFilterFreq = filterFreq * (1.0f + lfoValue * lfoDepth);
            modFilterFreq = Math.max(100, Math.min(8000, modFilterFreq));

            // Apply filter
            filterL.configure(FilterType.LOWPASS, modFilterFreq, resonance, 0.0f);
            float wet = filterL.process(osc);

            // Scale output
            wet *= 0.7f;

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // For synth, we typically use mono detection and stereo output
        int waveform = (int) waveformParam.getValue();
        int octaveShift = (int) octaveParam.getValue() - 1;
        float filterFreq = filterParam.getValue();
        float resonance = resonanceParam.getValue();
        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float lfoRate = lfoRateParam.getValue();
        float lfoDepth = lfoDepthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float glideMs = glideParam.getValue();

        lfo.setFrequency(lfoRate);

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        float glideCoeff = glideMs > 0 ? (float) Math.exp(-1.0 / (glideMs * sampleRate / 1000.0)) : 0;

        float octaveMult = (float) Math.pow(2.0, octaveShift);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];
            float mono = (dryL + dryR) * 0.5f;
            float absInput = Math.abs(mono);

            if (absInput > envelope) {
                envelope = attackCoeff * envelope + (1 - attackCoeff) * absInput;
            } else {
                envelope = releaseCoeff * envelope;
            }

            float detectedPitch = detectPitch(mono);
            float targetFreq = detectedPitch * octaveMult;

            if (glideMs > 0 && currentFreq > 0) {
                currentFreq = glideCoeff * currentFreq + (1 - glideCoeff) * targetFreq;
            } else {
                currentFreq = targetFreq;
            }

            float osc = 0;
            if (currentFreq > 20 && envelope > 0.01f) {
                osc = oscillator(waveform, oscPhase);
                oscPhase += currentFreq / sampleRate;
                if (oscPhase >= 1.0f) oscPhase -= 1.0f;
                osc *= Math.min(envelope * 3.0f, 1.0f);
            }

            float lfoValue = lfo.tick();
            float modFilterFreq = filterFreq * (1.0f + lfoValue * lfoDepth);
            modFilterFreq = Math.max(100, Math.min(8000, modFilterFreq));

            filterL.configure(FilterType.LOWPASS, modFilterFreq, resonance, 0.0f);
            filterR.configure(FilterType.LOWPASS, modFilterFreq, resonance, 0.0f);

            float wetL = filterL.process(osc);
            float wetR = filterR.process(osc);

            wetL *= 0.7f;
            wetR *= 0.7f;

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (pitchBuffer != null) java.util.Arrays.fill(pitchBuffer, 0);
        pitchBufferPos = 0;
        detectedFreq = 0;
        zeroCrossingCount = 0;
        lastZeroCrossing = 0;
        prevSample = 0;
        oscPhase = 0;
        currentFreq = 0;
        envelope = 0;
        if (filterL != null) filterL.reset();
        if (filterR != null) filterR.reset();
        if (lfo != null) lfo.reset();
    }

    // Convenience setters
    public void setWaveform(int waveform) { waveformParam.setValue(waveform); }
    public void setOctave(int octave) { octaveParam.setValue(octave); }
    public void setFilter(float hz) { filterParam.setValue(hz); }
    public void setResonance(float q) { resonanceParam.setValue(q); }
    public void setAttack(float ms) { attackParam.setValue(ms); }
    public void setRelease(float ms) { releaseParam.setValue(ms); }
    public void setLfoRate(float hz) { lfoRateParam.setValue(hz); }
    public void setLfoDepth(float percent) { lfoDepthParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setGlide(float ms) { glideParam.setValue(ms); }
}
