package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.WaveShaper;
import it.denzosoft.jfx2.effects.*;

/**
 * Classic Fuzz effect.
 *
 * <p>Emulates the aggressive, square-wave-like distortion of classic
 * 1960s/70s fuzz pedals like the Fuzz Face or Big Muff. Characterized
 * by massive sustain, thick harmonics, and a "buzzy" texture.</p>
 *
 * <p>Characteristics:
 * - Hard clipping transitioning to near-square wave
 * - Massive sustain and compression
 * - Thick, woolly low-mids
 * - Scooped or boosted mids option
 * - Classic rock/psych/stoner tone</p>
 */
public class FuzzEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "fuzz",
            "Fuzz",
            "Classic 60s/70s fuzz with aggressive clipping",
            EffectCategory.DISTORTION
    );

    // Fuzz type names
    private static final String[] FUZZ_TYPES = {
            "Classic",   // Fuzz Face style - germanium-like
            "Muff",      // Big Muff style - scooped mids
            "Octave"     // Octavia style - with upper octave
    };

    // Parameters
    private final Parameter fuzzParam;
    private final Parameter toneParam;
    private final Parameter typeParam;
    private final Parameter sustainParam;
    private final Parameter levelParam;

    // Filters - Left channel
    private BiquadFilter inputHpfL;
    private BiquadFilter inputLpfL;
    private BiquadFilter midFilterL;
    private BiquadFilter toneFilterL;
    private BiquadFilter outputLpfL;

    // Filters - Right channel
    private BiquadFilter inputHpfR;
    private BiquadFilter inputLpfR;
    private BiquadFilter midFilterR;
    private BiquadFilter toneFilterR;
    private BiquadFilter outputLpfR;

    // Octave effect state
    private float lastSampleL = 0;
    private float lastSampleR = 0;

    public FuzzEffect() {
        super(METADATA);

        // Fuzz amount: 1 to 100, default 70
        fuzzParam = addFloatParameter("fuzz", "Fuzz",
                "Amount of fuzz/gain. Higher values approach square wave.",
                1.0f, 100.0f, 70.0f, "");

        // Tone: 200 Hz to 5000 Hz, default 1500 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "Output tone. Lower = thick and dark, higher = cutting and bright.",
                200.0f, 5000.0f, 1500.0f, "Hz");

        // Fuzz type
        typeParam = addChoiceParameter("type", "Type",
                "Fuzz character: Classic (warm), Muff (scooped), Octave (with upper octave).",
                FUZZ_TYPES, 0);

        // Sustain/compression: 0% to 100%, default 60%
        sustainParam = addFloatParameter("sustain", "Sustain",
                "Controls compression and note sustain.",
                0.0f, 100.0f, 60.0f, "%");

        // Output level: -20 dB to +6 dB, default -3 dB
        levelParam = addFloatParameter("level", "Level",
                "Output volume.",
                -20.0f, 6.0f, -3.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Input high-pass at 40 Hz (let some bass through for fuzz)
        inputHpfL = new BiquadFilter();
        inputHpfL.setSampleRate(sampleRate);
        inputHpfL.configure(FilterType.HIGHPASS, 40.0f, 0.5f, 0.0f);

        inputHpfR = new BiquadFilter();
        inputHpfR.setSampleRate(sampleRate);
        inputHpfR.configure(FilterType.HIGHPASS, 40.0f, 0.5f, 0.0f);

        // Input lowpass to tame harsh highs before clipping
        inputLpfL = new BiquadFilter();
        inputLpfL.setSampleRate(sampleRate);
        inputLpfL.configure(FilterType.LOWPASS, 4000.0f, 0.707f, 0.0f);

        inputLpfR = new BiquadFilter();
        inputLpfR.setSampleRate(sampleRate);
        inputLpfR.configure(FilterType.LOWPASS, 4000.0f, 0.707f, 0.0f);

        // Mid scoop/boost filter
        midFilterL = new BiquadFilter();
        midFilterL.setSampleRate(sampleRate);

        midFilterR = new BiquadFilter();
        midFilterR.setSampleRate(sampleRate);

        // Tone control
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Output lowpass at 6kHz (fuzz is naturally dark)
        outputLpfL = new BiquadFilter();
        outputLpfL.setSampleRate(sampleRate);
        outputLpfL.configure(FilterType.LOWPASS, 6000.0f, 0.707f, 0.0f);

        outputLpfR = new BiquadFilter();
        outputLpfR.setSampleRate(sampleRate);
        outputLpfR.configure(FilterType.LOWPASS, 6000.0f, 0.707f, 0.0f);

        updateMidFilter();
    }

    private void updateMidFilter() {
        int type = (int) typeParam.getValue();

        switch (type) {
            case 0 -> {
                // Classic - slight mid boost for warmth
                midFilterL.configure(FilterType.PEAK, 500.0f, 1.0f, 3.0f);
                midFilterR.configure(FilterType.PEAK, 500.0f, 1.0f, 3.0f);
            }
            case 1 -> {
                // Muff - mid scoop
                midFilterL.configure(FilterType.PEAK, 600.0f, 0.8f, -6.0f);
                midFilterR.configure(FilterType.PEAK, 600.0f, 0.8f, -6.0f);
            }
            case 2 -> {
                // Octave - boost upper harmonics
                midFilterL.configure(FilterType.PEAK, 1200.0f, 1.5f, 4.0f);
                midFilterR.configure(FilterType.PEAK, 1200.0f, 1.5f, 4.0f);
            }
        }
    }

    /**
     * Fuzz clipping algorithm - combination of hard clip and tanh.
     */
    private float fuzzClip(float input, float fuzz, float sustain) {
        // Pre-gain with sustain compression
        float sustainFactor = 1.0f + sustain * 2.0f;
        float gained = input * fuzz * sustainFactor;

        // Two-stage clipping for fuzz character
        // First stage: soft clip
        float stage1 = WaveShaper.tanhClip(gained, 2.0f);

        // Second stage: harder clip approaching square wave
        float threshold = 0.8f - (fuzz / 100.0f) * 0.5f;  // Lower threshold at higher fuzz
        float stage2 = WaveShaper.hardClip(stage1 * 1.5f, threshold);

        // Mix stages based on fuzz amount
        float fuzzMix = fuzz / 100.0f;
        return stage1 * (1.0f - fuzzMix * 0.7f) + stage2 * fuzzMix * 0.7f;
    }

    /**
     * Simple octave up effect using full-wave rectification.
     */
    private float octaveUp(float input, float lastSample) {
        // Full-wave rectification creates octave up
        return Math.abs(input) * 0.7f + input * 0.3f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float fuzz = fuzzParam.getValue();
        float toneFreq = toneParam.getValue();
        int type = (int) typeParam.getValue();
        float sustain = sustainParam.getValue() / 100.0f;
        float levelLinear = dbToLinear(levelParam.getValue());

        toneFilterL.setFrequency(toneFreq);
        updateMidFilter();

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Input filtering
            sample = inputHpfL.process(sample);
            sample = inputLpfL.process(sample);

            // Fuzz clipping
            sample = fuzzClip(sample, fuzz, sustain);

            // Octave effect for type 2
            if (type == 2) {
                sample = sample * 0.6f + octaveUp(sample, lastSampleL) * 0.4f;
            }
            lastSampleL = input[i];

            // Mid filter (scoop or boost)
            sample = midFilterL.process(sample);

            // Tone control
            sample = toneFilterL.process(sample);

            // Output smoothing
            sample = outputLpfL.process(sample);

            output[i] = sample * levelLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float fuzz = fuzzParam.getValue();
        float toneFreq = toneParam.getValue();
        int type = (int) typeParam.getValue();
        float sustain = sustainParam.getValue() / 100.0f;
        float levelLinear = dbToLinear(levelParam.getValue());

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);
        updateMidFilter();

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Input filtering
            sampleL = inputHpfL.process(sampleL);
            sampleR = inputHpfR.process(sampleR);
            sampleL = inputLpfL.process(sampleL);
            sampleR = inputLpfR.process(sampleR);

            // Fuzz clipping
            sampleL = fuzzClip(sampleL, fuzz, sustain);
            sampleR = fuzzClip(sampleR, fuzz, sustain);

            // Octave effect for type 2
            if (type == 2) {
                sampleL = sampleL * 0.6f + octaveUp(sampleL, lastSampleL) * 0.4f;
                sampleR = sampleR * 0.6f + octaveUp(sampleR, lastSampleR) * 0.4f;
            }
            lastSampleL = inputL[i];
            lastSampleR = inputR[i];

            // Mid filter
            sampleL = midFilterL.process(sampleL);
            sampleR = midFilterR.process(sampleR);

            // Tone control
            sampleL = toneFilterL.process(sampleL);
            sampleR = toneFilterR.process(sampleR);

            // Output smoothing
            sampleL = outputLpfL.process(sampleL);
            sampleR = outputLpfR.process(sampleR);

            outputL[i] = sampleL * levelLinear;
            outputR[i] = sampleR * levelLinear;
        }
    }

    @Override
    protected void onReset() {
        if (inputHpfL != null) inputHpfL.reset();
        if (inputHpfR != null) inputHpfR.reset();
        if (inputLpfL != null) inputLpfL.reset();
        if (inputLpfR != null) inputLpfR.reset();
        if (midFilterL != null) midFilterL.reset();
        if (midFilterR != null) midFilterR.reset();
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (outputLpfL != null) outputLpfL.reset();
        if (outputLpfR != null) outputLpfR.reset();
        lastSampleL = 0;
        lastSampleR = 0;
    }

    // Convenience setters
    public void setFuzz(float amount) {
        fuzzParam.setValue(amount);
    }

    public void setTone(float hz) {
        toneParam.setValue(hz);
    }

    public void setType(int type) {
        typeParam.setValue(type);
    }

    public void setSustain(float percent) {
        sustainParam.setValue(percent);
    }

    public void setLevel(float dB) {
        levelParam.setValue(dB);
    }
}
