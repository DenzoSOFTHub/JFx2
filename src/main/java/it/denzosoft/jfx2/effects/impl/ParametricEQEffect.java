package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * 4-band parametric equalizer.
 *
 * <p>Features:
 * - Low shelf band
 * - Two mid parametric bands
 * - High shelf band
 * Each band has frequency, gain, and Q controls.</p>
 */
public class ParametricEQEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "parametriceq",
            "Parametric EQ",
            "4-band parametric equalizer",
            EffectCategory.EQ
    );

    // Parameters for each band
    // Band 1: Low Shelf
    private final Parameter lowFreqParam;
    private final Parameter lowGainParam;

    // Band 2: Low Mid (parametric)
    private final Parameter lowMidFreqParam;
    private final Parameter lowMidGainParam;
    private final Parameter lowMidQParam;

    // Band 3: High Mid (parametric)
    private final Parameter highMidFreqParam;
    private final Parameter highMidGainParam;
    private final Parameter highMidQParam;

    // Band 4: High Shelf
    private final Parameter highFreqParam;
    private final Parameter highGainParam;

    // Output level
    private final Parameter outputParam;

    // Filters - Left channel
    private BiquadFilter lowShelfL;
    private BiquadFilter lowMidL;
    private BiquadFilter highMidL;
    private BiquadFilter highShelfL;

    // Filters - Right channel
    private BiquadFilter lowShelfR;
    private BiquadFilter lowMidR;
    private BiquadFilter highMidR;
    private BiquadFilter highShelfR;

    public ParametricEQEffect() {
        super(METADATA);

        // Band 1: Low Shelf (80-500 Hz)
        lowFreqParam = addFloatParameter("lowFreq", "Low Freq",
                "Corner frequency for low shelf. Affects all frequencies below this point.",
                20.0f, 500.0f, 100.0f, "Hz");
        lowGainParam = addFloatParameter("lowGain", "Low Gain",
                "Boost or cut the bass frequencies. Positive adds warmth, negative reduces muddiness.",
                -15.0f, 15.0f, 0.0f, "dB");

        // Band 2: Low Mid (200-2000 Hz)
        lowMidFreqParam = addFloatParameter("lowMidFreq", "Lo-Mid Freq",
                "Center frequency for low-mid band. Targets body and fundamental tone.",
                100.0f, 2000.0f, 400.0f, "Hz");
        lowMidGainParam = addFloatParameter("lowMidGain", "Lo-Mid Gain",
                "Boost or cut the low-mid frequencies. Cut to reduce boxiness, boost for fullness.",
                -15.0f, 15.0f, 0.0f, "dB");
        lowMidQParam = addFloatParameter("lowMidQ", "Lo-Mid Q",
                "Bandwidth of the band. Higher Q = narrower, more surgical. Lower Q = broader, more musical.",
                0.1f, 10.0f, 1.0f, "");

        // Band 3: High Mid (500-8000 Hz)
        highMidFreqParam = addFloatParameter("highMidFreq", "Hi-Mid Freq",
                "Center frequency for high-mid band. Targets presence and attack.",
                500.0f, 8000.0f, 2000.0f, "Hz");
        highMidGainParam = addFloatParameter("highMidGain", "Hi-Mid Gain",
                "Boost or cut the high-mid frequencies. Boost for cut-through, cut to reduce harshness.",
                -15.0f, 15.0f, 0.0f, "dB");
        highMidQParam = addFloatParameter("highMidQ", "Hi-Mid Q",
                "Bandwidth of the band. Higher Q targets specific resonances, lower Q for general tone shaping.",
                0.1f, 10.0f, 1.0f, "");

        // Band 4: High Shelf (2000-16000 Hz)
        highFreqParam = addFloatParameter("highFreq", "High Freq",
                "Corner frequency for high shelf. Affects all frequencies above this point.",
                2000.0f, 16000.0f, 8000.0f, "Hz");
        highGainParam = addFloatParameter("highGain", "High Gain",
                "Boost or cut the treble. Positive adds air and sparkle, negative darkens the tone.",
                -15.0f, 15.0f, 0.0f, "dB");

        // Output level
        outputParam = addFloatParameter("output", "Output",
                "Overall output level after EQ. Use to compensate for boosts or cuts.",
                -12.0f, 12.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create filters - Left channel
        lowShelfL = new BiquadFilter();
        lowShelfL.setSampleRate(sampleRate);
        lowMidL = new BiquadFilter();
        lowMidL.setSampleRate(sampleRate);
        highMidL = new BiquadFilter();
        highMidL.setSampleRate(sampleRate);
        highShelfL = new BiquadFilter();
        highShelfL.setSampleRate(sampleRate);

        // Create filters - Right channel
        lowShelfR = new BiquadFilter();
        lowShelfR.setSampleRate(sampleRate);
        lowMidR = new BiquadFilter();
        lowMidR.setSampleRate(sampleRate);
        highMidR = new BiquadFilter();
        highMidR.setSampleRate(sampleRate);
        highShelfR = new BiquadFilter();
        highShelfR.setSampleRate(sampleRate);

        // Initial configuration
        updateFilters();
    }

    private void updateFilters() {
        // Low shelf
        lowShelfL.configure(FilterType.LOWSHELF, lowFreqParam.getValue(), 0.707f, lowGainParam.getValue());
        lowShelfR.configure(FilterType.LOWSHELF, lowFreqParam.getValue(), 0.707f, lowGainParam.getValue());

        // Low mid (parametric/peak)
        lowMidL.configure(FilterType.PEAK, lowMidFreqParam.getValue(), lowMidQParam.getValue(), lowMidGainParam.getValue());
        lowMidR.configure(FilterType.PEAK, lowMidFreqParam.getValue(), lowMidQParam.getValue(), lowMidGainParam.getValue());

        // High mid (parametric/peak)
        highMidL.configure(FilterType.PEAK, highMidFreqParam.getValue(), highMidQParam.getValue(), highMidGainParam.getValue());
        highMidR.configure(FilterType.PEAK, highMidFreqParam.getValue(), highMidQParam.getValue(), highMidGainParam.getValue());

        // High shelf
        highShelfL.configure(FilterType.HIGHSHELF, highFreqParam.getValue(), 0.707f, highGainParam.getValue());
        highShelfR.configure(FilterType.HIGHSHELF, highFreqParam.getValue(), 0.707f, highGainParam.getValue());
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        updateFilters();
        float outputGain = dbToLinear(outputParam.getValue());

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Process through each band in series
            sample = lowShelfL.process(sample);
            sample = lowMidL.process(sample);
            sample = highMidL.process(sample);
            sample = highShelfL.process(sample);

            output[i] = sample * outputGain;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        updateFilters();
        float outputGain = dbToLinear(outputParam.getValue());

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Process Left channel
            sampleL = lowShelfL.process(sampleL);
            sampleL = lowMidL.process(sampleL);
            sampleL = highMidL.process(sampleL);
            sampleL = highShelfL.process(sampleL);

            // Process Right channel
            sampleR = lowShelfR.process(sampleR);
            sampleR = lowMidR.process(sampleR);
            sampleR = highMidR.process(sampleR);
            sampleR = highShelfR.process(sampleR);

            outputL[i] = sampleL * outputGain;
            outputR[i] = sampleR * outputGain;
        }
    }

    @Override
    protected void onReset() {
        if (lowShelfL != null) lowShelfL.reset();
        if (lowShelfR != null) lowShelfR.reset();
        if (lowMidL != null) lowMidL.reset();
        if (lowMidR != null) lowMidR.reset();
        if (highMidL != null) highMidL.reset();
        if (highMidR != null) highMidR.reset();
        if (highShelfL != null) highShelfL.reset();
        if (highShelfR != null) highShelfR.reset();
    }

    // Convenience setters for each band

    // Low Shelf
    public void setLowFreq(float hz) {
        lowFreqParam.setValue(hz);
    }

    public void setLowGain(float dB) {
        lowGainParam.setValue(dB);
    }

    // Low Mid
    public void setLowMidFreq(float hz) {
        lowMidFreqParam.setValue(hz);
    }

    public void setLowMidGain(float dB) {
        lowMidGainParam.setValue(dB);
    }

    public void setLowMidQ(float q) {
        lowMidQParam.setValue(q);
    }

    // High Mid
    public void setHighMidFreq(float hz) {
        highMidFreqParam.setValue(hz);
    }

    public void setHighMidGain(float dB) {
        highMidGainParam.setValue(dB);
    }

    public void setHighMidQ(float q) {
        highMidQParam.setValue(q);
    }

    // High Shelf
    public void setHighFreq(float hz) {
        highFreqParam.setValue(hz);
    }

    public void setHighGain(float dB) {
        highGainParam.setValue(dB);
    }

    // Output
    public void setOutput(float dB) {
        outputParam.setValue(dB);
    }
}
