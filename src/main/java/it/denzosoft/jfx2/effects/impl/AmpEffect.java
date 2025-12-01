package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Parametric guitar amplifier simulation.
 *
 * <p>Models the complete amp signal chain:
 * - Input stage with bright switch
 * - Preamp gain stages with tube-like saturation
 * - 3-band tone stack (Bass, Mid, Treble)
 * - Presence control
 * - Power amp simulation with sag/compression
 * - Master volume</p>
 */
public class AmpEffect extends AbstractEffect {

    // Parameters
    private Parameter inputGain;
    private Parameter bright;
    private Parameter preampGain;
    private Parameter bass;
    private Parameter mid;
    private Parameter treble;
    private Parameter presence;
    private Parameter sagAmount;
    private Parameter masterVolume;

    // Filters for tone stack - Left channel
    private BiquadFilter bassFilterL;
    private BiquadFilter midFilterL;
    private BiquadFilter trebleFilterL;
    private BiquadFilter presenceFilterL;
    private BiquadFilter brightFilterL;
    private BiquadFilter inputHPFL;

    // Filters for tone stack - Right channel
    private BiquadFilter bassFilterR;
    private BiquadFilter midFilterR;
    private BiquadFilter trebleFilterR;
    private BiquadFilter presenceFilterR;
    private BiquadFilter brightFilterR;
    private BiquadFilter inputHPFR;

    // Power amp state - Left
    private float sagStateL;
    private float dcBlockStateL;

    // Power amp state - Right
    private float sagStateR;
    private float dcBlockStateR;

    // Shared timing
    private float sagAttack;
    private float sagRelease;
    private static final float DC_BLOCK_COEFF = 0.995f;

    public AmpEffect() {
        super(EffectMetadata.of("amp", "Amp", "Parametric guitar amplifier simulation", EffectCategory.AMP_SIM));
        initParameters();
    }

    private void initParameters() {
        inputGain = addFloatParameter("inputGain", "Input",
                "Input level before preamp. Boost hot pickups, cut for active guitars.",
                -12.0f, 12.0f, 0.0f, "dB");
        bright = addBooleanParameter("bright", "Bright",
                "Adds high frequency sparkle at low gain settings. Classic amp feature.",
                false);
        preampGain = addFloatParameter("preampGain", "Gain",
                "Preamp drive amount. Higher values create more tube-like saturation and harmonics.",
                0.0f, 100.0f, 50.0f, "%");

        bass = addFloatParameter("bass", "Bass",
                "Low frequency tone control. Adds thump and warmth, reduce for tighter response.",
                0.0f, 10.0f, 5.0f, "");
        mid = addFloatParameter("mid", "Mid",
                "Midrange tone control. Cut for scooped metal tone, boost for punchy rock sound.",
                0.0f, 10.0f, 5.0f, "");
        treble = addFloatParameter("treble", "Treble",
                "High frequency tone control. Adds bite and clarity, reduce for warmer tone.",
                0.0f, 10.0f, 5.0f, "");
        presence = addFloatParameter("presence", "Presence",
                "Upper harmonic content from power amp. Adds edge and articulation.",
                0.0f, 10.0f, 5.0f, "");

        sagAmount = addFloatParameter("sag", "Sag",
                "Power supply compression feel. Adds touch-sensitive dynamics like vintage amps.",
                0.0f, 100.0f, 30.0f, "%");
        masterVolume = addFloatParameter("master", "Master",
                "Overall output volume. Set preamp high and master low for classic cranked tone at low volume.",
                -60.0f, 6.0f, -6.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create filters - Left channel
        inputHPFL = new BiquadFilter();
        inputHPFL.setSampleRate(sampleRate);
        inputHPFL.configure(FilterType.HIGHPASS, 20.0f, 0.707f, 0);
        brightFilterL = new BiquadFilter();
        brightFilterL.setSampleRate(sampleRate);
        brightFilterL.configure(FilterType.HIGHSHELF, 2000.0f, 0.707f, 6.0f);
        bassFilterL = new BiquadFilter();
        bassFilterL.setSampleRate(sampleRate);
        midFilterL = new BiquadFilter();
        midFilterL.setSampleRate(sampleRate);
        trebleFilterL = new BiquadFilter();
        trebleFilterL.setSampleRate(sampleRate);
        presenceFilterL = new BiquadFilter();
        presenceFilterL.setSampleRate(sampleRate);

        // Create filters - Right channel
        inputHPFR = new BiquadFilter();
        inputHPFR.setSampleRate(sampleRate);
        inputHPFR.configure(FilterType.HIGHPASS, 20.0f, 0.707f, 0);
        brightFilterR = new BiquadFilter();
        brightFilterR.setSampleRate(sampleRate);
        brightFilterR.configure(FilterType.HIGHSHELF, 2000.0f, 0.707f, 6.0f);
        bassFilterR = new BiquadFilter();
        bassFilterR.setSampleRate(sampleRate);
        midFilterR = new BiquadFilter();
        midFilterR.setSampleRate(sampleRate);
        trebleFilterR = new BiquadFilter();
        trebleFilterR.setSampleRate(sampleRate);
        presenceFilterR = new BiquadFilter();
        presenceFilterR.setSampleRate(sampleRate);

        updateToneStack();

        // Power amp sag timing
        sagAttack = (float) Math.exp(-1.0 / (sampleRate * 0.01));   // 10ms attack
        sagRelease = (float) Math.exp(-1.0 / (sampleRate * 0.1));   // 100ms release

        sagStateL = 1.0f;
        sagStateR = 1.0f;
        dcBlockStateL = 0;
        dcBlockStateR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        updateToneStack();

        float inGainLin = dbToLinear(inputGain.getValue());
        float preGain = preampGain.getValue() / 100.0f;
        float sag = sagAmount.getValue() / 100.0f;
        float masterLin = dbToLinear(masterVolume.getValue());
        boolean brightOn = bright.getBooleanValue();

        float stage1Gain = 1.0f + preGain * 10.0f;
        float stage2Gain = 1.0f + preGain * 5.0f;
        float stage3Gain = 1.0f + preGain * 3.0f;

        for (int i = 0; i < frameCount; i++) {
            float sample = input[i] * inGainLin;
            sample = inputHPFL.process(sample);
            if (brightOn) sample = brightFilterL.process(sample);

            // Preamp stages
            sample *= stage1Gain;
            sample = tubeSaturate(sample, 0.8f);
            sample *= stage2Gain;
            sample = tubeSaturate(sample, 0.7f);
            sample *= stage3Gain;
            sample = tubeSaturate(sample, 0.6f);

            // Tone stack
            sample = bassFilterL.process(sample);
            sample = midFilterL.process(sample);
            sample = trebleFilterL.process(sample);

            // Power amp
            sample = presenceFilterL.process(sample);
            sample = powerAmpSaturate(sample);

            // Sag
            if (sag > 0) {
                float level = Math.abs(sample);
                float targetSag = 1.0f - sag * 0.3f * Math.min(level, 1.0f);
                if (targetSag < sagStateL) {
                    sagStateL = sagAttack * sagStateL + (1.0f - sagAttack) * targetSag;
                } else {
                    sagStateL = sagRelease * sagStateL + (1.0f - sagRelease) * targetSag;
                }
                sample *= sagStateL;
            }

            sample *= masterLin;

            // DC blocking
            float dcBlocked = sample - dcBlockStateL;
            dcBlockStateL = sample - dcBlocked * DC_BLOCK_COEFF;
            output[i] = dcBlocked;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        updateToneStack();

        float inGainLin = dbToLinear(inputGain.getValue());
        float preGain = preampGain.getValue() / 100.0f;
        float sag = sagAmount.getValue() / 100.0f;
        float masterLin = dbToLinear(masterVolume.getValue());
        boolean brightOn = bright.getBooleanValue();

        float stage1Gain = 1.0f + preGain * 10.0f;
        float stage2Gain = 1.0f + preGain * 5.0f;
        float stage3Gain = 1.0f + preGain * 3.0f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i] * inGainLin;
            float sampleR = inputR[i] * inGainLin;

            // Input filtering
            sampleL = inputHPFL.process(sampleL);
            sampleR = inputHPFR.process(sampleR);
            if (brightOn) {
                sampleL = brightFilterL.process(sampleL);
                sampleR = brightFilterR.process(sampleR);
            }

            // Preamp stages - Left
            sampleL *= stage1Gain;
            sampleL = tubeSaturate(sampleL, 0.8f);
            sampleL *= stage2Gain;
            sampleL = tubeSaturate(sampleL, 0.7f);
            sampleL *= stage3Gain;
            sampleL = tubeSaturate(sampleL, 0.6f);

            // Preamp stages - Right
            sampleR *= stage1Gain;
            sampleR = tubeSaturate(sampleR, 0.8f);
            sampleR *= stage2Gain;
            sampleR = tubeSaturate(sampleR, 0.7f);
            sampleR *= stage3Gain;
            sampleR = tubeSaturate(sampleR, 0.6f);

            // Tone stack
            sampleL = bassFilterL.process(sampleL);
            sampleL = midFilterL.process(sampleL);
            sampleL = trebleFilterL.process(sampleL);
            sampleR = bassFilterR.process(sampleR);
            sampleR = midFilterR.process(sampleR);
            sampleR = trebleFilterR.process(sampleR);

            // Power amp
            sampleL = presenceFilterL.process(sampleL);
            sampleL = powerAmpSaturate(sampleL);
            sampleR = presenceFilterR.process(sampleR);
            sampleR = powerAmpSaturate(sampleR);

            // Sag
            if (sag > 0) {
                float levelL = Math.abs(sampleL);
                float targetSagL = 1.0f - sag * 0.3f * Math.min(levelL, 1.0f);
                if (targetSagL < sagStateL) {
                    sagStateL = sagAttack * sagStateL + (1.0f - sagAttack) * targetSagL;
                } else {
                    sagStateL = sagRelease * sagStateL + (1.0f - sagRelease) * targetSagL;
                }
                sampleL *= sagStateL;

                float levelR = Math.abs(sampleR);
                float targetSagR = 1.0f - sag * 0.3f * Math.min(levelR, 1.0f);
                if (targetSagR < sagStateR) {
                    sagStateR = sagAttack * sagStateR + (1.0f - sagAttack) * targetSagR;
                } else {
                    sagStateR = sagRelease * sagStateR + (1.0f - sagRelease) * targetSagR;
                }
                sampleR *= sagStateR;
            }

            sampleL *= masterLin;
            sampleR *= masterLin;

            // DC blocking
            float dcBlockedL = sampleL - dcBlockStateL;
            dcBlockStateL = sampleL - dcBlockedL * DC_BLOCK_COEFF;
            float dcBlockedR = sampleR - dcBlockStateR;
            dcBlockStateR = sampleR - dcBlockedR * DC_BLOCK_COEFF;

            outputL[i] = dcBlockedL;
            outputR[i] = dcBlockedR;
        }
    }

    @Override
    protected void onReset() {
        if (inputHPFL != null) inputHPFL.reset();
        if (inputHPFR != null) inputHPFR.reset();
        if (brightFilterL != null) brightFilterL.reset();
        if (brightFilterR != null) brightFilterR.reset();
        if (bassFilterL != null) bassFilterL.reset();
        if (bassFilterR != null) bassFilterR.reset();
        if (midFilterL != null) midFilterL.reset();
        if (midFilterR != null) midFilterR.reset();
        if (trebleFilterL != null) trebleFilterL.reset();
        if (trebleFilterR != null) trebleFilterR.reset();
        if (presenceFilterL != null) presenceFilterL.reset();
        if (presenceFilterR != null) presenceFilterR.reset();
        sagStateL = 1.0f;
        sagStateR = 1.0f;
        dcBlockStateL = 0;
        dcBlockStateR = 0;
    }

    private void updateToneStack() {
        if (sampleRate <= 0) return;

        float bassVal = bass.getValue();
        float midVal = mid.getValue();
        float trebleVal = treble.getValue();
        float presVal = presence.getValue();

        float bassGain = (bassVal - 5.0f) * 2.4f;
        float midGain = (midVal - 5.0f) * 2.4f;
        float trebleGain = (trebleVal - 5.0f) * 2.4f;
        float presenceGain = (presVal - 5.0f) * 1.5f;

        bassFilterL.configure(FilterType.LOWSHELF, 100.0f, 0.707f, bassGain);
        bassFilterR.configure(FilterType.LOWSHELF, 100.0f, 0.707f, bassGain);
        midFilterL.configure(FilterType.PEAK, 800.0f, 1.0f, midGain);
        midFilterR.configure(FilterType.PEAK, 800.0f, 1.0f, midGain);
        trebleFilterL.configure(FilterType.HIGHSHELF, 3000.0f, 0.707f, trebleGain);
        trebleFilterR.configure(FilterType.HIGHSHELF, 3000.0f, 0.707f, trebleGain);
        presenceFilterL.configure(FilterType.HIGHSHELF, 4500.0f, 0.707f, presenceGain);
        presenceFilterR.configure(FilterType.HIGHSHELF, 4500.0f, 0.707f, presenceGain);
    }

    /**
     * Tube-like saturation using asymmetric soft clipping.
     * Emulates triode tube characteristics.
     */
    private float tubeSaturate(float x, float asymmetry) {
        // Asymmetric waveshaping (tubes clip differently on + and - half-cycles)
        if (x >= 0) {
            // Positive half: softer saturation
            return (float) Math.tanh(x * 1.5) * 0.9f;
        } else {
            // Negative half: harder saturation with asymmetry
            float hardness = 1.5f + asymmetry;
            return (float) Math.tanh(x * hardness) * (0.9f - asymmetry * 0.1f);
        }
    }

    /**
     * Power amp saturation - softer, more symmetrical than preamp.
     */
    private float powerAmpSaturate(float x) {
        // Softer saturation than preamp stages
        float softClip = x / (1.0f + Math.abs(x) * 0.5f);

        // Add slight even harmonics (push-pull asymmetry)
        float evenHarmonic = x * x * 0.05f * Math.signum(x);

        return softClip + evenHarmonic;
    }
}
