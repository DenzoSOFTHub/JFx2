package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.AllPassFilter;
import it.denzosoft.jfx2.dsp.CombFilter;
import it.denzosoft.jfx2.effects.*;

/**
 * Reverb effect based on Freeverb algorithm.
 *
 * <p>Uses 8 parallel comb filters feeding into 4 series all-pass filters.
 * Produces a rich, natural-sounding reverb.</p>
 */
public class ReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "reverb",
            "Reverb",
            "Room reverb based on Freeverb algorithm",
            EffectCategory.REVERB
    );

    // Parameters
    private final Parameter roomSizeParam;
    private final Parameter dampParam;
    private final Parameter widthParam;
    private final Parameter mixParam;
    private final Parameter predelayParam;

    // Freeverb constants (tuned for 44100 Hz)
    private static final int[] COMB_TUNING_L = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    private static final int[] COMB_TUNING_R = {1116 + 23, 1188 + 23, 1277 + 23, 1356 + 23,
            1422 + 23, 1491 + 23, 1557 + 23, 1617 + 23};
    private static final int[] ALLPASS_TUNING = {556, 441, 341, 225};
    private static final int STEREO_SPREAD = 23;

    private static final float FIXED_GAIN = 0.015f;
    private static final float SCALE_ROOM = 0.28f;
    private static final float OFFSET_ROOM = 0.7f;
    private static final float SCALE_DAMP = 0.4f;

    // DSP components
    private CombFilter[] combL;
    private CombFilter[] combR;
    private AllPassFilter[] allpassL;
    private AllPassFilter[] allpassR;

    // Predelay - Left channel
    private float[] predelayBufferL;
    private int predelayWritePosL;
    private int predelaySamples;

    // Predelay - Right channel
    private float[] predelayBufferR;
    private int predelayWritePosR;

    public ReverbEffect() {
        super(METADATA);

        // Room size: 0% (small) to 100% (large), default 50%
        roomSizeParam = addFloatParameter("roomSize", "Room Size",
                "Simulated room size. Larger values create longer, more spacious reverb tails.",
                0.0f, 100.0f, 50.0f, "%");

        // Damping: 0% (bright) to 100% (dark), default 50%
        dampParam = addFloatParameter("damp", "Damping",
                "High frequency absorption. Higher values make the reverb darker and more natural.",
                0.0f, 100.0f, 50.0f, "%");

        // Width: 0% (mono) to 100% (full stereo), default 100%
        widthParam = addFloatParameter("width", "Width",
                "Stereo spread of the reverb. Lower values create a more focused, centered sound.",
                0.0f, 100.0f, 100.0f, "%");

        // Mix: 0% (dry) to 100% (wet), default 30%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and reverb signal. Higher values create a more distant sound.",
                0.0f, 100.0f, 30.0f, "%");

        // Predelay: 0 to 100 ms, default 10 ms
        predelayParam = addFloatParameter("predelay", "Pre-Delay",
                "Time before reverb starts. Adds clarity by separating direct sound from reflections.",
                0.0f, 100.0f, 10.0f, "ms");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Scale tuning values for different sample rates
        float sampleRateScale = sampleRate / 44100.0f;

        // Create comb filters
        combL = new CombFilter[8];
        combR = new CombFilter[8];
        for (int i = 0; i < 8; i++) {
            int delayL = (int) (COMB_TUNING_L[i] * sampleRateScale);
            int delayR = (int) (COMB_TUNING_R[i] * sampleRateScale);
            combL[i] = new CombFilter(delayL, 0.5f, 0.5f);
            combR[i] = new CombFilter(delayR, 0.5f, 0.5f);
        }

        // Create all-pass filters
        allpassL = new AllPassFilter[4];
        allpassR = new AllPassFilter[4];
        for (int i = 0; i < 4; i++) {
            int delay = (int) (ALLPASS_TUNING[i] * sampleRateScale);
            allpassL[i] = new AllPassFilter(delay, 0.5f);
            allpassR[i] = new AllPassFilter(delay + (int) (STEREO_SPREAD * sampleRateScale), 0.5f);
        }

        // Predelay buffers (max 100ms) - Left and Right
        int maxPredelaySamples = (int) (100.0f * sampleRate / 1000.0f);
        predelayBufferL = new float[maxPredelaySamples + 1];
        predelayBufferR = new float[maxPredelaySamples + 1];
        predelayWritePosL = 0;
        predelayWritePosR = 0;
        predelaySamples = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float roomSize = roomSizeParam.getValue() / 100.0f;
        float damp = dampParam.getValue() / 100.0f;
        float width = widthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float predelayMs = predelayParam.getValue();

        // Calculate feedback and damping for comb filters
        float feedback = roomSize * SCALE_ROOM + OFFSET_ROOM;
        float dampValue = damp * SCALE_DAMP;

        // Update comb filter parameters
        for (int i = 0; i < 8; i++) {
            combL[i].setFeedback(feedback);
            combL[i].setDamp(dampValue);
            combR[i].setFeedback(feedback);
            combR[i].setDamp(dampValue);
        }

        // Calculate predelay samples
        predelaySamples = (int) (predelayMs * sampleRate / 1000.0f);
        if (predelaySamples >= predelayBufferL.length) {
            predelaySamples = predelayBufferL.length - 1;
        }

        // Width coefficients
        float wet1 = mix * (width / 2.0f + 0.5f);
        float wet2 = mix * ((1.0f - width) / 2.0f);

        for (int i = 0; i < frameCount && i < input.length; i++) {
            float dry = input[i];

            // Apply predelay (using L buffer for mono)
            predelayBufferL[predelayWritePosL] = dry;
            int predelayReadPos = (predelayWritePosL - predelaySamples + predelayBufferL.length) % predelayBufferL.length;
            float predelayed = predelayBufferL[predelayReadPos];
            predelayWritePosL = (predelayWritePosL + 1) % predelayBufferL.length;

            // Scale input
            float inputScaled = predelayed * FIXED_GAIN;

            // Process through parallel comb filters
            float outL = 0.0f;
            float outR = 0.0f;
            for (int c = 0; c < 8; c++) {
                outL += combL[c].process(inputScaled);
                outR += combR[c].process(inputScaled);
            }

            // Process through series all-pass filters
            for (int a = 0; a < 4; a++) {
                outL = allpassL[a].process(outL);
                outR = allpassR[a].process(outR);
            }

            // Output (mono input, process stereo internally, then mono output)
            // For stereo output we'd do: outL * wet1 + outR * wet2
            // For mono: average the channels
            float wet = (outL + outR) * 0.5f * mix;

            // Write output with index check for stereo buffer
            if (i < output.length) {
                output[i] = dry * (1.0f - mix) + wet;
            }
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float roomSize = roomSizeParam.getValue() / 100.0f;
        float damp = dampParam.getValue() / 100.0f;
        float width = widthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float predelayMs = predelayParam.getValue();

        // Calculate feedback and damping for comb filters
        float feedback = roomSize * SCALE_ROOM + OFFSET_ROOM;
        float dampValue = damp * SCALE_DAMP;

        // Update comb filter parameters
        for (int i = 0; i < 8; i++) {
            combL[i].setFeedback(feedback);
            combL[i].setDamp(dampValue);
            combR[i].setFeedback(feedback);
            combR[i].setDamp(dampValue);
        }

        // Calculate predelay samples
        predelaySamples = (int) (predelayMs * sampleRate / 1000.0f);
        if (predelaySamples >= predelayBufferL.length) {
            predelaySamples = predelayBufferL.length - 1;
        }

        // Width coefficients for stereo output
        float wet1 = mix * (width / 2.0f + 0.5f);
        float wet2 = mix * ((1.0f - width) / 2.0f);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Apply predelay - Left
            predelayBufferL[predelayWritePosL] = dryL;
            int predelayReadPosL = (predelayWritePosL - predelaySamples + predelayBufferL.length) % predelayBufferL.length;
            float predelayedL = predelayBufferL[predelayReadPosL];
            predelayWritePosL = (predelayWritePosL + 1) % predelayBufferL.length;

            // Apply predelay - Right
            predelayBufferR[predelayWritePosR] = dryR;
            int predelayReadPosR = (predelayWritePosR - predelaySamples + predelayBufferR.length) % predelayBufferR.length;
            float predelayedR = predelayBufferR[predelayReadPosR];
            predelayWritePosR = (predelayWritePosR + 1) % predelayBufferR.length;

            // Scale inputs
            float inputScaledL = predelayedL * FIXED_GAIN;
            float inputScaledR = predelayedR * FIXED_GAIN;

            // Process through parallel comb filters
            float outL = 0.0f;
            float outR = 0.0f;
            for (int c = 0; c < 8; c++) {
                outL += combL[c].process(inputScaledL);
                outR += combR[c].process(inputScaledR);
            }

            // Process through series all-pass filters
            for (int a = 0; a < 4; a++) {
                outL = allpassL[a].process(outL);
                outR = allpassR[a].process(outR);
            }

            // Stereo width mixing and output
            outputL[i] = dryL * (1.0f - mix) + outL * wet1 + outR * wet2;
            outputR[i] = dryR * (1.0f - mix) + outR * wet1 + outL * wet2;
        }
    }

    @Override
    protected void onReset() {
        if (combL != null) {
            for (CombFilter c : combL) c.clear();
        }
        if (combR != null) {
            for (CombFilter c : combR) c.clear();
        }
        if (allpassL != null) {
            for (AllPassFilter a : allpassL) a.clear();
        }
        if (allpassR != null) {
            for (AllPassFilter a : allpassR) a.clear();
        }
        if (predelayBufferL != null) {
            java.util.Arrays.fill(predelayBufferL, 0.0f);
        }
        if (predelayBufferR != null) {
            java.util.Arrays.fill(predelayBufferR, 0.0f);
        }
        predelayWritePosL = 0;
        predelayWritePosR = 0;
    }

    // Convenience setters
    public void setRoomSize(float percent) {
        roomSizeParam.setValue(percent);
    }

    public void setDamping(float percent) {
        dampParam.setValue(percent);
    }

    public void setWidth(float percent) {
        widthParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }

    public void setPredelay(float ms) {
        predelayParam.setValue(ms);
    }
}
