package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Reverse Delay effect.
 *
 * <p>Creates backwards-sounding echoes by reversing audio segments
 * before playback. Creates ethereal, psychedelic textures.</p>
 *
 * <p>Features:
 * - Adjustable reverse window size
 * - Crossfade for smooth transitions
 * - Feedback for building textures
 * - Mix control for blending</p>
 */
public class ReverseDelayEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "reversedelay",
            "Reverse Delay",
            "Backwards echoes for psychedelic textures",
            EffectCategory.DELAY
    );

    // Parameters
    private final Parameter timeParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;
    private final Parameter crossfadeParam;

    // Buffers
    private float[] bufferL1, bufferL2;
    private float[] bufferR1, bufferR2;
    private int bufferSize;
    private int writePos;
    private int readPos;
    private boolean useBuffer1;

    // Feedback
    private float feedbackL = 0;
    private float feedbackR = 0;

    public ReverseDelayEffect() {
        super(METADATA);

        // Time: 100 ms to 2000 ms, default 500 ms
        timeParam = addFloatParameter("time", "Time",
                "Length of the reversed segment. Longer = more dramatic effect.",
                100.0f, 2000.0f, 500.0f, "ms");

        // Feedback: 0% to 80%, default 30%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount of reversed signal fed back. Creates building layers.",
                0.0f, 80.0f, 30.0f, "%");

        // Mix: 0% to 100%, default 50%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and reversed signals.",
                0.0f, 100.0f, 50.0f, "%");

        // Crossfade: 5% to 50%, default 20%
        crossfadeParam = addFloatParameter("crossfade", "Crossfade",
                "Overlap between segments. Higher = smoother but less defined.",
                5.0f, 50.0f, 20.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Max buffer size for 2 seconds
        int maxSamples = (int) (2.0f * sampleRate);

        // Double buffer for crossfading
        bufferL1 = new float[maxSamples];
        bufferL2 = new float[maxSamples];
        bufferR1 = new float[maxSamples];
        bufferR2 = new float[maxSamples];

        bufferSize = (int) (timeParam.getValue() * sampleRate / 1000.0f);
        writePos = 0;
        readPos = 0;
        useBuffer1 = true;

        feedbackL = 0;
        feedbackR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float timeMs = timeParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float crossfade = crossfadeParam.getValue() / 100.0f;

        int newBufferSize = (int) (timeMs * sampleRate / 1000.0f);
        if (newBufferSize != bufferSize) {
            bufferSize = Math.min(newBufferSize, bufferL1.length);
        }

        int crossfadeSamples = (int) (bufferSize * crossfade);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Write to current buffer with feedback
            float inputWithFeedback = dry + feedbackL * feedback;

            float[] writeBuffer = useBuffer1 ? bufferL1 : bufferL2;
            float[] readBuffer = useBuffer1 ? bufferL2 : bufferL1;

            writeBuffer[writePos] = inputWithFeedback;

            // Read from opposite buffer in reverse
            int reversePos = bufferSize - 1 - readPos;
            float reversed = readBuffer[Math.max(0, Math.min(reversePos, bufferSize - 1))];

            // Apply crossfade at boundaries
            float fadeGain = 1.0f;
            if (readPos < crossfadeSamples) {
                fadeGain = (float) readPos / crossfadeSamples;
            } else if (readPos > bufferSize - crossfadeSamples) {
                fadeGain = (float) (bufferSize - readPos) / crossfadeSamples;
            }
            reversed *= fadeGain;

            // Store for feedback
            feedbackL = reversed;

            // Advance positions
            writePos++;
            readPos++;

            // Swap buffers when filled
            if (writePos >= bufferSize) {
                writePos = 0;
                readPos = 0;
                useBuffer1 = !useBuffer1;
            }

            output[i] = dry * (1.0f - mix) + reversed * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float timeMs = timeParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float crossfade = crossfadeParam.getValue() / 100.0f;

        int newBufferSize = (int) (timeMs * sampleRate / 1000.0f);
        if (newBufferSize != bufferSize) {
            bufferSize = Math.min(newBufferSize, bufferL1.length);
        }

        int crossfadeSamples = (int) (bufferSize * crossfade);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            float[] writeBufferL = useBuffer1 ? bufferL1 : bufferL2;
            float[] readBufferL = useBuffer1 ? bufferL2 : bufferL1;
            float[] writeBufferR = useBuffer1 ? bufferR1 : bufferR2;
            float[] readBufferR = useBuffer1 ? bufferR2 : bufferR1;

            writeBufferL[writePos] = dryL + feedbackL * feedback;
            writeBufferR[writePos] = dryR + feedbackR * feedback;

            int reversePos = bufferSize - 1 - readPos;
            reversePos = Math.max(0, Math.min(reversePos, bufferSize - 1));

            float reversedL = readBufferL[reversePos];
            float reversedR = readBufferR[reversePos];

            float fadeGain = 1.0f;
            if (readPos < crossfadeSamples) {
                fadeGain = (float) readPos / crossfadeSamples;
            } else if (readPos > bufferSize - crossfadeSamples) {
                fadeGain = (float) (bufferSize - readPos) / crossfadeSamples;
            }

            reversedL *= fadeGain;
            reversedR *= fadeGain;

            feedbackL = reversedL;
            feedbackR = reversedR;

            writePos++;
            readPos++;

            if (writePos >= bufferSize) {
                writePos = 0;
                readPos = 0;
                useBuffer1 = !useBuffer1;
            }

            outputL[i] = dryL * (1.0f - mix) + reversedL * mix;
            outputR[i] = dryR * (1.0f - mix) + reversedR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (bufferL1 != null) java.util.Arrays.fill(bufferL1, 0);
        if (bufferL2 != null) java.util.Arrays.fill(bufferL2, 0);
        if (bufferR1 != null) java.util.Arrays.fill(bufferR1, 0);
        if (bufferR2 != null) java.util.Arrays.fill(bufferR2, 0);
        writePos = 0;
        readPos = 0;
        useBuffer1 = true;
        feedbackL = 0;
        feedbackR = 0;
    }

    // Convenience setters
    public void setTime(float ms) { timeParam.setValue(ms); }
    public void setFeedback(float percent) { feedbackParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setCrossfade(float percent) { crossfadeParam.setValue(percent); }
}
