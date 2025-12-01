package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Talk Box / Formant Filter effect.
 *
 * <p>Simulates vocal formants to create "talking" sounds.
 * Uses multiple bandpass filters tuned to vowel frequencies.</p>
 *
 * <p>Features:
 * - Multiple vowel presets (A, E, I, O, U)
 * - Smooth morphing between vowels
 * - LFO modulation for automatic vowel sweeping
 * - Adjustable formant frequencies</p>
 */
public class TalkBoxEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "talkbox",
            "Talk Box",
            "Formant filter for vocal-like sounds",
            EffectCategory.FILTER
    );

    private static final String[] VOWEL_NAMES = {
            "A", "E", "I", "O", "U"
    };

    // Formant frequencies for each vowel (F1, F2, F3)
    // Based on average male voice formants
    private static final float[][] FORMANTS = {
            {730, 1090, 2440},   // A
            {660, 1720, 2410},   // E
            {270, 2290, 3010},   // I
            {570, 840, 2410},    // O
            {440, 1020, 2240}    // U
    };

    // Number of formant filters
    private static final int NUM_FORMANTS = 3;

    // Parameters
    private final Parameter vowelParam;
    private final Parameter morphParam;
    private final Parameter resonanceParam;
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter mixParam;

    // Formant filters - Left
    private BiquadFilter[] formantFiltersL;
    // Formant filters - Right
    private BiquadFilter[] formantFiltersR;

    // LFO for automatic vowel morphing
    private LFO lfo;

    public TalkBoxEffect() {
        super(METADATA);

        // Vowel: 0-4 (A, E, I, O, U)
        vowelParam = addChoiceParameter("vowel", "Vowel",
                "Target vowel sound. Use morph to blend between vowels.",
                VOWEL_NAMES, 0);

        // Morph: 0% to 100%, default 0%
        morphParam = addFloatParameter("morph", "Morph",
                "Blend to next vowel (cyclically). 0% = selected vowel, 100% = next vowel.",
                0.0f, 100.0f, 0.0f, "%");

        // Resonance: 2 to 15, default 8
        resonanceParam = addFloatParameter("resonance", "Q",
                "Formant resonance. Higher = more pronounced vowel character.",
                2.0f, 15.0f, 8.0f, "");

        // LFO Rate: 0 to 5 Hz, default 0 (off)
        rateParam = addFloatParameter("rate", "Rate",
                "LFO speed for automatic vowel morphing. 0 = manual control.",
                0.0f, 5.0f, 0.0f, "Hz");

        // LFO Depth: 0% to 100%, default 100%
        depthParam = addFloatParameter("depth", "Depth",
                "LFO modulation depth for vowel morphing.",
                0.0f, 100.0f, 100.0f, "%");

        // Mix: 0% to 100%, default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and formant-filtered signal.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        formantFiltersL = new BiquadFilter[NUM_FORMANTS];
        formantFiltersR = new BiquadFilter[NUM_FORMANTS];

        for (int i = 0; i < NUM_FORMANTS; i++) {
            formantFiltersL[i] = new BiquadFilter();
            formantFiltersL[i].setSampleRate(sampleRate);

            formantFiltersR[i] = new BiquadFilter();
            formantFiltersR[i].setSampleRate(sampleRate);
        }

        lfo = new LFO(LFO.Waveform.TRIANGLE, 1.0f, sampleRate);
    }

    /**
     * Interpolate between two vowel formant sets.
     */
    private float[] interpolateFormants(int vowel1, int vowel2, float blend) {
        float[] result = new float[NUM_FORMANTS];
        for (int i = 0; i < NUM_FORMANTS; i++) {
            result[i] = FORMANTS[vowel1][i] * (1 - blend) + FORMANTS[vowel2][i] * blend;
        }
        return result;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int baseVowel = (int) vowelParam.getValue();
        float morph = morphParam.getValue() / 100.0f;
        float resonance = resonanceParam.getValue();
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        lfo.setFrequency(rate);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Get LFO modulation
            float lfoValue = 0;
            if (rate > 0) {
                lfoValue = (lfo.tick() + 1.0f) * 0.5f * depth; // 0 to depth
            }

            // Calculate effective morph position
            float totalMorph = morph + lfoValue;
            totalMorph = totalMorph % 1.0f; // Wrap around

            // Determine which two vowels to interpolate
            float vowelPos = baseVowel + totalMorph * (VOWEL_NAMES.length);
            int vowel1 = ((int) vowelPos) % VOWEL_NAMES.length;
            int vowel2 = (vowel1 + 1) % VOWEL_NAMES.length;
            float blend = vowelPos - (int) vowelPos;

            // Get interpolated formant frequencies
            float[] formants = interpolateFormants(vowel1, vowel2, blend);

            // Process through formant filters and sum
            float wet = 0;
            for (int f = 0; f < NUM_FORMANTS; f++) {
                // Lower gain for higher formants
                float gain = 1.0f / (f + 1);
                formantFiltersL[f].configure(FilterType.BANDPASS, formants[f], resonance, 0.0f);
                wet += formantFiltersL[f].process(dry) * gain;
            }

            // Normalize
            wet *= 0.7f;

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        int baseVowel = (int) vowelParam.getValue();
        float morph = morphParam.getValue() / 100.0f;
        float resonance = resonanceParam.getValue();
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        lfo.setFrequency(rate);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            float lfoValue = 0;
            if (rate > 0) {
                lfoValue = (lfo.tick() + 1.0f) * 0.5f * depth;
            }

            float totalMorph = morph + lfoValue;
            totalMorph = totalMorph % 1.0f;

            float vowelPos = baseVowel + totalMorph * (VOWEL_NAMES.length);
            int vowel1 = ((int) vowelPos) % VOWEL_NAMES.length;
            int vowel2 = (vowel1 + 1) % VOWEL_NAMES.length;
            float blend = vowelPos - (int) vowelPos;

            float[] formants = interpolateFormants(vowel1, vowel2, blend);

            float wetL = 0, wetR = 0;
            for (int f = 0; f < NUM_FORMANTS; f++) {
                float gain = 1.0f / (f + 1);
                formantFiltersL[f].configure(FilterType.BANDPASS, formants[f], resonance, 0.0f);
                formantFiltersR[f].configure(FilterType.BANDPASS, formants[f], resonance, 0.0f);
                wetL += formantFiltersL[f].process(dryL) * gain;
                wetR += formantFiltersR[f].process(dryR) * gain;
            }

            wetL *= 0.7f;
            wetR *= 0.7f;

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (formantFiltersL != null) {
            for (BiquadFilter f : formantFiltersL) if (f != null) f.reset();
        }
        if (formantFiltersR != null) {
            for (BiquadFilter f : formantFiltersR) if (f != null) f.reset();
        }
        if (lfo != null) lfo.reset();
    }

    // Convenience setters
    public void setVowel(int vowel) { vowelParam.setValue(vowel); }
    public void setMorph(float percent) { morphParam.setValue(percent); }
    public void setResonance(float q) { resonanceParam.setValue(q); }
    public void setRate(float hz) { rateParam.setValue(hz); }
    public void setDepth(float percent) { depthParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
}
