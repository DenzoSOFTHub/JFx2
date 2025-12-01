package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Pitch shifter effect using granular time-domain processing.
 *
 * <p>Uses overlap-add with crossfaded grains to shift pitch
 * by ±12 semitones. Includes detune for chorus-like effects.</p>
 */
public class PitchShifterEffect extends AbstractEffect {

    private static final int MAX_DELAY = 4096;
    private static final int OVERLAP = 4;  // Number of overlapping grains

    // Parameters
    private Parameter shift;
    private Parameter fine;
    private Parameter grainSizeParam;
    private Parameter mix;

    // Delay buffer - Left channel
    private float[] bufferL;
    private int writePosL;
    private int samplesFilledL;
    private float[] grainPhasesL;

    // Delay buffer - Right channel
    private float[] bufferR;
    private int writePosR;
    private int samplesFilledR;
    private float[] grainPhasesR;

    private int numGrains;

    public PitchShifterEffect() {
        super(EffectMetadata.of("pitchshift", "Pitch Shifter", "Shift pitch by ±12 semitones", EffectCategory.PITCH));
        initParameters();
    }

    private void initParameters() {
        shift = addFloatParameter("shift", "Shift",
                "Pitch shift in semitones. +12 = one octave up, -12 = one octave down.",
                -12.0f, 12.0f, 0.0f, "st");
        fine = addFloatParameter("fine", "Fine",
                "Fine pitch adjustment in cents. Use for subtle detuning or precise tuning.",
                -100.0f, 100.0f, 0.0f, "cents");
        grainSizeParam = addFloatParameter("grain", "Grain",
                "Processing window size. Larger = smoother but more latency, smaller = more artifacts.",
                20.0f, 100.0f, 50.0f, "ms");
        mix = addFloatParameter("mix", "Mix",
                "Balance between original and pitch-shifted signal. Use 50% for harmony effects.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        numGrains = OVERLAP;

        // Left channel
        bufferL = new float[MAX_DELAY];
        writePosL = 0;
        samplesFilledL = 0;
        grainPhasesL = new float[numGrains];
        for (int i = 0; i < numGrains; i++) {
            grainPhasesL[i] = (float) i / numGrains;
        }

        // Right channel
        bufferR = new float[MAX_DELAY];
        writePosR = 0;
        samplesFilledR = 0;
        grainPhasesR = new float[numGrains];
        for (int i = 0; i < numGrains; i++) {
            grainPhasesR[i] = (float) i / numGrains;
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float semitones = shift.getValue() + fine.getValue() / 100.0f;
        float pitchRatio = (float) Math.pow(2.0, semitones / 12.0);
        float mixAmt = mix.getValue() / 100.0f;

        float grainMs = grainSizeParam.getValue();
        int grainSamples = (int) (grainMs * sampleRate / 1000.0f);
        grainSamples = Math.max(256, Math.min(grainSamples, MAX_DELAY / 2));
        float phaseInc = 1.0f / grainSamples;

        for (int i = 0; i < frameCount; i++) {
            bufferL[writePosL] = input[i];
            samplesFilledL = Math.min(samplesFilledL + 1, MAX_DELAY);

            float shifted = 0;
            float windowSum = 0;

            if (samplesFilledL >= grainSamples) {
                for (int g = 0; g < numGrains; g++) {
                    float phase = grainPhasesL[g];
                    float readOffset = grainSamples * (1.0f - phase) * pitchRatio;
                    float readPosF = writePosL - readOffset;
                    if (readPosF < 0) readPosF += MAX_DELAY;

                    int readPos = (int) readPosF;
                    float frac = readPosF - readPos;
                    readPos = ((readPos % MAX_DELAY) + MAX_DELAY) % MAX_DELAY;
                    int nextPos = (readPos + 1) % MAX_DELAY;
                    float sample = bufferL[readPos] * (1.0f - frac) + bufferL[nextPos] * frac;

                    float window = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * phase));
                    shifted += sample * window;
                    windowSum += window;

                    grainPhasesL[g] += phaseInc;
                    if (grainPhasesL[g] >= 1.0f) grainPhasesL[g] -= 1.0f;
                }
                if (windowSum > 0.001f) shifted /= windowSum;
            }

            writePosL = (writePosL + 1) % MAX_DELAY;
            output[i] = input[i] * (1.0f - mixAmt) + shifted * mixAmt;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float semitones = shift.getValue() + fine.getValue() / 100.0f;
        float pitchRatio = (float) Math.pow(2.0, semitones / 12.0);
        float mixAmt = mix.getValue() / 100.0f;

        float grainMs = grainSizeParam.getValue();
        int grainSamples = (int) (grainMs * sampleRate / 1000.0f);
        grainSamples = Math.max(256, Math.min(grainSamples, MAX_DELAY / 2));
        float phaseInc = 1.0f / grainSamples;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            bufferL[writePosL] = inputL[i];
            samplesFilledL = Math.min(samplesFilledL + 1, MAX_DELAY);
            float shiftedL = 0;
            float windowSumL = 0;
            if (samplesFilledL >= grainSamples) {
                for (int g = 0; g < numGrains; g++) {
                    float phase = grainPhasesL[g];
                    float readOffset = grainSamples * (1.0f - phase) * pitchRatio;
                    float readPosF = writePosL - readOffset;
                    if (readPosF < 0) readPosF += MAX_DELAY;
                    int readPos = ((int) readPosF % MAX_DELAY + MAX_DELAY) % MAX_DELAY;
                    float frac = readPosF - (int) readPosF;
                    int nextPos = (readPos + 1) % MAX_DELAY;
                    float sample = bufferL[readPos] * (1.0f - frac) + bufferL[nextPos] * frac;
                    float window = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * phase));
                    shiftedL += sample * window;
                    windowSumL += window;
                    grainPhasesL[g] += phaseInc;
                    if (grainPhasesL[g] >= 1.0f) grainPhasesL[g] -= 1.0f;
                }
                if (windowSumL > 0.001f) shiftedL /= windowSumL;
            }
            writePosL = (writePosL + 1) % MAX_DELAY;
            outputL[i] = inputL[i] * (1.0f - mixAmt) + shiftedL * mixAmt;

            // Right channel
            bufferR[writePosR] = inputR[i];
            samplesFilledR = Math.min(samplesFilledR + 1, MAX_DELAY);
            float shiftedR = 0;
            float windowSumR = 0;
            if (samplesFilledR >= grainSamples) {
                for (int g = 0; g < numGrains; g++) {
                    float phase = grainPhasesR[g];
                    float readOffset = grainSamples * (1.0f - phase) * pitchRatio;
                    float readPosF = writePosR - readOffset;
                    if (readPosF < 0) readPosF += MAX_DELAY;
                    int readPos = ((int) readPosF % MAX_DELAY + MAX_DELAY) % MAX_DELAY;
                    float frac = readPosF - (int) readPosF;
                    int nextPos = (readPos + 1) % MAX_DELAY;
                    float sample = bufferR[readPos] * (1.0f - frac) + bufferR[nextPos] * frac;
                    float window = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * phase));
                    shiftedR += sample * window;
                    windowSumR += window;
                    grainPhasesR[g] += phaseInc;
                    if (grainPhasesR[g] >= 1.0f) grainPhasesR[g] -= 1.0f;
                }
                if (windowSumR > 0.001f) shiftedR /= windowSumR;
            }
            writePosR = (writePosR + 1) % MAX_DELAY;
            outputR[i] = inputR[i] * (1.0f - mixAmt) + shiftedR * mixAmt;
        }
    }

    @Override
    protected void onReset() {
        if (bufferL != null) java.util.Arrays.fill(bufferL, 0);
        if (bufferR != null) java.util.Arrays.fill(bufferR, 0);
        writePosL = 0;
        writePosR = 0;
        samplesFilledL = 0;
        samplesFilledR = 0;
        if (grainPhasesL != null) {
            for (int i = 0; i < numGrains; i++) grainPhasesL[i] = (float) i / numGrains;
        }
        if (grainPhasesR != null) {
            for (int i = 0; i < numGrains; i++) grainPhasesR[i] = (float) i / numGrains;
        }
    }

    @Override
    public int getLatency() {
        // Latency equals grain size (we need full grain before outputting)
        return (int) (grainSizeParam.getValue() * sampleRate / 1000.0f);
    }
}
