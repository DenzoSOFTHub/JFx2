package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Tube distortion effect simulating various vacuum tube types.
 *
 * <p>Models the nonlinear behavior of different tube types including:
 * <ul>
 *   <li>Preamp tubes: 12AX7, 12AT7, 12AU7</li>
 *   <li>Power tubes: EL34, 6L6, EL84, 6V6</li>
 * </ul>
 * </p>
 *
 * <p>Each tube type has unique characteristics:
 * <ul>
 *   <li>Gain factor (amplification)</li>
 *   <li>Harmonic content (even vs odd)</li>
 *   <li>Clipping asymmetry</li>
 *   <li>Compression behavior</li>
 * </ul>
 * </p>
 *
 * <p>The bias parameter controls the operating point on the tube's
 * transfer curve, affecting crossover distortion and compression.</p>
 */
public class TubeDistortionEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "tubedist",
            "Tube Distortion",
            "Vacuum tube distortion with selectable tube types",
            EffectCategory.DISTORTION
    );

    // Tube type definitions
    private static final String[] TUBE_NAMES = {
            "12AX7",    // 0: High gain preamp, classic
            "12AT7",    // 1: Medium gain preamp, cleaner
            "12AU7",    // 2: Low gain preamp, very clean
            "EL34",     // 3: Power tube, British (Marshall)
            "6L6",      // 4: Power tube, American (Fender)
            "EL84",     // 5: Power tube, chimey (Vox)
            "6V6"       // 6: Power tube, sweet breakup
    };

    // Tube characteristics: [gain, asymmetry, evenHarmonics, compression]
    private static final float[][] TUBE_CHARS = {
            // 12AX7: High gain, moderate asymmetry, rich harmonics
            {1.0f, 0.15f, 0.6f, 0.3f},
            // 12AT7: Medium gain, less asymmetry, cleaner
            {0.6f, 0.10f, 0.5f, 0.2f},
            // 12AU7: Low gain, minimal asymmetry, very clean
            {0.3f, 0.05f, 0.4f, 0.1f},
            // EL34: High gain, aggressive, lots of mids
            {1.2f, 0.20f, 0.5f, 0.5f},
            // 6L6: Medium-high gain, clean headroom, scooped
            {0.9f, 0.12f, 0.7f, 0.4f},
            // EL84: Medium gain, compressed, chimey
            {0.8f, 0.18f, 0.55f, 0.6f},
            // 6V6: Medium gain, early breakup, sweet
            {0.7f, 0.22f, 0.65f, 0.45f}
    };

    // === ROW 1: Tube Selection ===
    private final Parameter tubeTypeParam;
    private final Parameter stagesParam;

    // === ROW 2: Drive & Bias ===
    private final Parameter driveParam;
    private final Parameter biasParam;

    // === ROW 3: Character ===
    private final Parameter sagParam;
    private final Parameter toneParam;

    // === ROW 4: Output ===
    private final Parameter outputParam;
    private final Parameter mixParam;

    // DSP state
    private float dcBlockerStateL = 0;
    private float dcBlockerStateR = 0;
    private float sagEnvelopeL = 0;
    private float sagEnvelopeR = 0;

    // Filters
    private BiquadFilter inputFilterL, inputFilterR;
    private BiquadFilter toneFilterL, toneFilterR;
    private BiquadFilter presenceFilterL, presenceFilterR;

    public TubeDistortionEffect() {
        super(METADATA);

        // === ROW 1: Tube Selection ===
        tubeTypeParam = addChoiceParameter("tube", "Tube",
                "Select the vacuum tube type to emulate. Preamp tubes (12Axx) for early gain stages, power tubes for output stage character.",
                TUBE_NAMES, 0);

        stagesParam = addFloatParameter("stages", "Stages",
                "Number of gain stages (cascaded tubes). More stages = more gain and saturation.",
                1.0f, 4.0f, 2.0f, "");

        // === ROW 2: Drive & Bias ===
        driveParam = addFloatParameter("drive", "Drive",
                "Input gain before the tube stage. Controls how hard the tubes are driven.",
                0.0f, 100.0f, 50.0f, "%");

        biasParam = addFloatParameter("bias", "Bias",
                "Tube bias point. Cold (left) = more crossover distortion, Hot (right) = more compression and sag.",
                -100.0f, 100.0f, 0.0f, "%");

        // === ROW 3: Character ===
        sagParam = addFloatParameter("sag", "Sag",
                "Power supply sag simulation. Higher = more dynamic compression on loud notes.",
                0.0f, 100.0f, 30.0f, "%");

        toneParam = addFloatParameter("tone", "Tone",
                "High frequency content. Lower = darker, warmer tone.",
                0.0f, 100.0f, 50.0f, "%");

        // === ROW 4: Output ===
        outputParam = addFloatParameter("output", "Output",
                "Output level after tube processing.",
                0.0f, 100.0f, 50.0f, "%");

        mixParam = addFloatParameter("mix", "Mix",
                "Dry/wet balance. 100% = full tube distortion.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        dcBlockerStateL = 0;
        dcBlockerStateR = 0;
        sagEnvelopeL = 0;
        sagEnvelopeR = 0;

        // Input highpass to remove DC and subsonic content
        inputFilterL = new BiquadFilter();
        inputFilterL.setSampleRate(sampleRate);
        inputFilterL.configure(FilterType.HIGHPASS, 30.0f, 0.707f, 0.0f);

        inputFilterR = new BiquadFilter();
        inputFilterR.setSampleRate(sampleRate);
        inputFilterR.configure(FilterType.HIGHPASS, 30.0f, 0.707f, 0.0f);

        // Tone control (lowpass)
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, 5000.0f, 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, 5000.0f, 0.707f, 0.0f);

        // Presence boost
        presenceFilterL = new BiquadFilter();
        presenceFilterL.setSampleRate(sampleRate);
        presenceFilterL.configure(FilterType.PEAK, 3000.0f, 1.0f, 3.0f);

        presenceFilterR = new BiquadFilter();
        presenceFilterR.setSampleRate(sampleRate);
        presenceFilterR.configure(FilterType.PEAK, 3000.0f, 1.0f, 3.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int tubeType = tubeTypeParam.getChoiceIndex();
        int stages = Math.round(stagesParam.getValue());
        float drive = driveParam.getValue() / 100.0f;
        float bias = biasParam.getValue() / 100.0f;
        float sag = sagParam.getValue() / 100.0f;
        float tone = toneParam.getValue() / 100.0f;
        float outputLevel = outputParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Get tube characteristics
        float[] tubeChar = TUBE_CHARS[tubeType];
        float tubeGain = tubeChar[0];
        float asymmetry = tubeChar[1];
        float evenHarmonics = tubeChar[2];
        float compression = tubeChar[3];

        // Update tone filter
        float toneFreq = 1000.0f + tone * 9000.0f;  // 1kHz to 10kHz
        toneFilterL.setFrequency(toneFreq);

        // Drive scaling (exponential for more natural feel)
        float driveGain = (float) Math.pow(10.0, drive * 2.0) * 0.5f;

        // Sag time constant
        float sagAttack = (float) Math.exp(-1.0 / (5.0 * sampleRate / 1000.0));
        float sagRelease = (float) Math.exp(-1.0 / (100.0 * sampleRate / 1000.0));

        for (int i = 0; i < frameCount; i++) {
            float dry = input[i];

            // Input filtering
            float x = inputFilterL.process(dry);

            // Apply drive
            x *= driveGain;

            // Sag envelope (reduces gain on loud signals)
            float absX = Math.abs(x);
            if (absX > sagEnvelopeL) {
                sagEnvelopeL = sagAttack * sagEnvelopeL + (1 - sagAttack) * absX;
            } else {
                sagEnvelopeL = sagRelease * sagEnvelopeL + (1 - sagRelease) * absX;
            }
            float sagGain = 1.0f - sag * sagEnvelopeL * compression;
            sagGain = Math.max(sagGain, 0.3f);
            x *= sagGain;

            // Apply tube stages
            for (int stage = 0; stage < stages; stage++) {
                x = processTubeStage(x, tubeGain, asymmetry, evenHarmonics, bias, compression);
                // Inter-stage gain adjustment
                x *= 0.7f;
            }

            // DC blocker
            float dcBlocked = x - dcBlockerStateL + 0.995f * dcBlockerStateL;
            dcBlockerStateL = x - dcBlocked;
            x = dcBlocked;

            // Tone filtering
            x = toneFilterL.process(x);

            // Add presence for power tubes
            if (tubeType >= 3) {
                x = presenceFilterL.process(x);
            }

            // Output level
            x *= outputLevel * 2.0f;

            // Soft limit output
            x = softClip(x);

            // Mix
            output[i] = dry * (1.0f - mix) + x * mix;
        }
    }

    /**
     * Process a single tube stage.
     */
    private float processTubeStage(float x, float gain, float asymmetry,
                                    float evenHarmonics, float bias, float compression) {
        // Apply gain
        x *= gain * 2.0f;

        // Apply bias offset (shifts operating point)
        float biasOffset = bias * 0.3f;
        x += biasOffset;

        // Asymmetric tube transfer function
        // Positive half: grid conduction (soft clip)
        // Negative half: plate saturation (harder clip)
        float y;
        if (x >= 0) {
            // Grid conduction - softer clipping, more even harmonics
            float k = 1.0f + evenHarmonics;
            y = (float) (Math.tanh(x * k) / k);
            // Add second harmonic (even)
            y += evenHarmonics * 0.1f * (float) Math.sin(2.0 * Math.PI * x * 0.5);
        } else {
            // Plate saturation - harder clipping
            float k = 1.0f + (1.0f - evenHarmonics) * 0.5f;
            y = (float) (-Math.tanh(-x * k * (1.0f + asymmetry)) / k);
        }

        // Apply compression characteristic
        float compressedY = y * (1.0f - compression * 0.3f * Math.abs(y));

        // Remove bias offset from output
        compressedY -= biasOffset * 0.5f;

        return compressedY;
    }

    /**
     * Soft clipping for output limiting.
     */
    private float softClip(float x) {
        if (x > 1.0f) {
            return 1.0f - (float) Math.exp(1.0f - x) * 0.36788f;
        } else if (x < -1.0f) {
            return -1.0f + (float) Math.exp(1.0f + x) * 0.36788f;
        }
        return x;
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        int tubeType = tubeTypeParam.getChoiceIndex();
        int stages = Math.round(stagesParam.getValue());
        float drive = driveParam.getValue() / 100.0f;
        float bias = biasParam.getValue() / 100.0f;
        float sag = sagParam.getValue() / 100.0f;
        float tone = toneParam.getValue() / 100.0f;
        float outputLevel = outputParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        float[] tubeChar = TUBE_CHARS[tubeType];
        float tubeGain = tubeChar[0];
        float asymmetry = tubeChar[1];
        float evenHarmonics = tubeChar[2];
        float compression = tubeChar[3];

        float toneFreq = 1000.0f + tone * 9000.0f;
        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        float driveGain = (float) Math.pow(10.0, drive * 2.0) * 0.5f;

        float sagAttack = (float) Math.exp(-1.0 / (5.0 * sampleRate / 1000.0));
        float sagRelease = (float) Math.exp(-1.0 / (100.0 * sampleRate / 1000.0));

        int len = Math.min(frameCount, Math.min(inputL.length,
                  Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Left channel
            float xL = inputFilterL.process(dryL);
            xL *= driveGain;

            float absL = Math.abs(xL);
            if (absL > sagEnvelopeL) {
                sagEnvelopeL = sagAttack * sagEnvelopeL + (1 - sagAttack) * absL;
            } else {
                sagEnvelopeL = sagRelease * sagEnvelopeL + (1 - sagRelease) * absL;
            }
            float sagGainL = 1.0f - sag * sagEnvelopeL * compression;
            sagGainL = Math.max(sagGainL, 0.3f);
            xL *= sagGainL;

            for (int stage = 0; stage < stages; stage++) {
                xL = processTubeStage(xL, tubeGain, asymmetry, evenHarmonics, bias, compression);
                xL *= 0.7f;
            }

            float dcBlockedL = xL - dcBlockerStateL + 0.995f * dcBlockerStateL;
            dcBlockerStateL = xL - dcBlockedL;
            xL = dcBlockedL;

            xL = toneFilterL.process(xL);
            if (tubeType >= 3) {
                xL = presenceFilterL.process(xL);
            }
            xL *= outputLevel * 2.0f;
            xL = softClip(xL);

            // Right channel
            float xR = inputFilterR.process(dryR);
            xR *= driveGain;

            float absR = Math.abs(xR);
            if (absR > sagEnvelopeR) {
                sagEnvelopeR = sagAttack * sagEnvelopeR + (1 - sagAttack) * absR;
            } else {
                sagEnvelopeR = sagRelease * sagEnvelopeR + (1 - sagRelease) * absR;
            }
            float sagGainR = 1.0f - sag * sagEnvelopeR * compression;
            sagGainR = Math.max(sagGainR, 0.3f);
            xR *= sagGainR;

            for (int stage = 0; stage < stages; stage++) {
                xR = processTubeStage(xR, tubeGain, asymmetry, evenHarmonics, bias, compression);
                xR *= 0.7f;
            }

            float dcBlockedR = xR - dcBlockerStateR + 0.995f * dcBlockerStateR;
            dcBlockerStateR = xR - dcBlockedR;
            xR = dcBlockedR;

            xR = toneFilterR.process(xR);
            if (tubeType >= 3) {
                xR = presenceFilterR.process(xR);
            }
            xR *= outputLevel * 2.0f;
            xR = softClip(xR);

            outputL[i] = dryL * (1.0f - mix) + xL * mix;
            outputR[i] = dryR * (1.0f - mix) + xR * mix;
        }
    }

    @Override
    protected void onReset() {
        dcBlockerStateL = 0;
        dcBlockerStateR = 0;
        sagEnvelopeL = 0;
        sagEnvelopeR = 0;

        if (inputFilterL != null) inputFilterL.reset();
        if (inputFilterR != null) inputFilterR.reset();
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (presenceFilterL != null) presenceFilterL.reset();
        if (presenceFilterR != null) presenceFilterR.reset();
    }

    @Override
    public int[] getParameterRowSizes() {
        // All parameters on one row: tube, stages, drive, bias, sag, tone, output, mix
        return new int[] {8};
    }
}
