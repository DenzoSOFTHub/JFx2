package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

import java.util.Arrays;

/**
 * Quad Delay effect with 4 independent delay channels.
 *
 * <p>Each channel has independent control over:
 * <ul>
 *   <li>Delay time (0-2000ms)</li>
 *   <li>Feedback amount</li>
 *   <li>Level/volume</li>
 *   <li>Stereo pan position</li>
 * </ul>
 * </p>
 *
 * <p>This allows for complex rhythmic delays, stereo spreading,
 * and creative sound design possibilities.</p>
 */
public class QuadDelayEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "quaddelay",
            "Quad Delay",
            "4-channel delay with independent time, feedback, level and pan",
            EffectCategory.DELAY
    );

    private static final int NUM_CHANNELS = 4;
    private static final int MAX_DELAY_SAMPLES = 96000;  // 2 seconds @ 48kHz

    // === ROW 1: Global Parameters ===
    private final Parameter dryParam;
    private final Parameter filterParam;
    private final Parameter masterFbParam;

    // === ROWS 2-5: Per-Channel Parameters ===
    private final Parameter[] channelEnabled;
    private final Parameter[] channelTime;
    private final Parameter[] channelFeedback;
    private final Parameter[] channelLevel;
    private final Parameter[] channelPan;

    // Delay lines (stereo per channel)
    private float[][] delayLineL;
    private float[][] delayLineR;
    private int[] writePos;

    // Filters for each channel
    private BiquadFilter[] channelFilterL;
    private BiquadFilter[] channelFilterR;

    // Feedback state
    private float[] lastFeedbackL;
    private float[] lastFeedbackR;

    public QuadDelayEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        // === ROW 1: Global Parameters ===
        dryParam = addFloatParameter("dry", "Dry",
                "Level of the original dry signal.",
                0.0f, 100.0f, 100.0f, "%");

        filterParam = addFloatParameter("filter", "Filter",
                "Lowpass filter on delay taps. Lower = darker repeats.",
                1000.0f, 15000.0f, 8000.0f, "Hz");

        masterFbParam = addFloatParameter("masterFb", "Master FB",
                "Additional feedback applied to all channels.",
                0.0f, 50.0f, 0.0f, "%");

        // === ROWS 2-5: Per-Channel Parameters ===
        channelEnabled = new Parameter[NUM_CHANNELS];
        channelTime = new Parameter[NUM_CHANNELS];
        channelFeedback = new Parameter[NUM_CHANNELS];
        channelLevel = new Parameter[NUM_CHANNELS];
        channelPan = new Parameter[NUM_CHANNELS];

        // Default values for interesting stereo pattern
        float[] defaultTimes = {250, 375, 500, 750};      // Rhythmic pattern
        float[] defaultFeedbacks = {30, 25, 20, 15};      // Decreasing feedback
        float[] defaultLevels = {80, 70, 60, 50};         // Decreasing levels
        float[] defaultPans = {-60, 40, -30, 70};         // Stereo spread
        boolean[] defaultEnabled = {true, true, false, false};

        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            int chNum = ch + 1;
            String prefix = "ch" + chNum;

            channelEnabled[ch] = addBooleanParameter(prefix + "On", "Ch" + chNum,
                    "Enable/disable delay channel " + chNum + ".",
                    defaultEnabled[ch]);

            channelTime[ch] = addFloatParameter(prefix + "Time", "Time",
                    "Delay time for channel " + chNum + ".",
                    0.0f, 2000.0f, defaultTimes[ch], "ms");

            channelFeedback[ch] = addFloatParameter(prefix + "Fb", "Feedback",
                    "Feedback amount for channel " + chNum + ". Higher = more repeats.",
                    0.0f, 95.0f, defaultFeedbacks[ch], "%");

            channelLevel[ch] = addFloatParameter(prefix + "Lvl", "Level",
                    "Output level for channel " + chNum + ".",
                    0.0f, 100.0f, defaultLevels[ch], "%");

            channelPan[ch] = addFloatParameter(prefix + "Pan", "Pan",
                    "Stereo position for channel " + chNum + ". -100=left, +100=right.",
                    -100.0f, 100.0f, defaultPans[ch], "");
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Allocate delay lines for each channel
        delayLineL = new float[NUM_CHANNELS][MAX_DELAY_SAMPLES];
        delayLineR = new float[NUM_CHANNELS][MAX_DELAY_SAMPLES];
        writePos = new int[NUM_CHANNELS];

        // Feedback state
        lastFeedbackL = new float[NUM_CHANNELS];
        lastFeedbackR = new float[NUM_CHANNELS];

        // Filters for each channel
        channelFilterL = new BiquadFilter[NUM_CHANNELS];
        channelFilterR = new BiquadFilter[NUM_CHANNELS];

        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            channelFilterL[ch] = new BiquadFilter();
            channelFilterL[ch].setSampleRate(sampleRate);
            channelFilterL[ch].configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);

            channelFilterR[ch] = new BiquadFilter();
            channelFilterR[ch].setSampleRate(sampleRate);
            channelFilterR[ch].configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono processing - convert to stereo internally
        float[] tempL = new float[frameCount];
        float[] tempR = new float[frameCount];
        System.arraycopy(input, 0, tempL, 0, frameCount);
        System.arraycopy(input, 0, tempR, 0, frameCount);

        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];

        processInternal(tempL, tempR, outL, outR, frameCount);

        // Mix down to mono
        for (int i = 0; i < frameCount; i++) {
            output[i] = (outL[i] + outR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        processInternal(inputL, inputR, outputL, outputR, frameCount);
    }

    private void processInternal(float[] inputL, float[] inputR,
                                  float[] outputL, float[] outputR, int frameCount) {
        float dry = dryParam.getValue() / 100.0f;
        float filterFreq = filterParam.getValue();
        float masterFb = masterFbParam.getValue() / 100.0f;

        // Update filters
        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            channelFilterL[ch].setFrequency(filterFreq);
            channelFilterR[ch].setFrequency(filterFreq);
        }

        // Pre-calculate channel parameters
        boolean[] enabled = new boolean[NUM_CHANNELS];
        int[] delaySamples = new int[NUM_CHANNELS];
        float[] feedback = new float[NUM_CHANNELS];
        float[] level = new float[NUM_CHANNELS];
        float[] panL = new float[NUM_CHANNELS];
        float[] panR = new float[NUM_CHANNELS];

        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            enabled[ch] = channelEnabled[ch].getBooleanValue();
            delaySamples[ch] = (int) (channelTime[ch].getValue() * sampleRate / 1000.0f);
            delaySamples[ch] = Math.min(delaySamples[ch], MAX_DELAY_SAMPLES - 1);
            feedback[ch] = channelFeedback[ch].getValue() / 100.0f + masterFb;
            feedback[ch] = Math.min(feedback[ch], 0.95f);  // Limit to prevent runaway
            level[ch] = channelLevel[ch].getValue() / 100.0f;

            // Constant power panning
            float pan = channelPan[ch].getValue() / 100.0f;  // -1 to +1
            panL[ch] = (float) Math.cos((pan + 1) * Math.PI / 4);
            panR[ch] = (float) Math.sin((pan + 1) * Math.PI / 4);
        }

        int len = Math.min(frameCount, Math.min(inputL.length,
                  Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float inL = inputL[i];
            float inR = inputR[i];

            // Start with dry signal
            float outL = inL * dry;
            float outR = inR * dry;

            // Process each delay channel
            for (int ch = 0; ch < NUM_CHANNELS; ch++) {
                if (!enabled[ch] || level[ch] < 0.001f) continue;

                // Read from delay line
                int readPos = (writePos[ch] - delaySamples[ch] + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
                float delayedL = delayLineL[ch][readPos];
                float delayedR = delayLineR[ch][readPos];

                // Apply filter
                delayedL = channelFilterL[ch].process(delayedL);
                delayedR = channelFilterR[ch].process(delayedR);

                // Add to output with pan and level
                outL += delayedL * level[ch] * panL[ch];
                outR += delayedR * level[ch] * panR[ch];

                // Write to delay line with feedback
                // Mix mono input to this channel's delay line
                float monoIn = (inL + inR) * 0.5f;
                delayLineL[ch][writePos[ch]] = monoIn + delayedL * feedback[ch];
                delayLineR[ch][writePos[ch]] = monoIn + delayedR * feedback[ch];

                // Update write position for this channel
                writePos[ch] = (writePos[ch] + 1) % MAX_DELAY_SAMPLES;
            }

            // Soft clip output
            outputL[i] = softClip(outL);
            outputR[i] = softClip(outR);
        }
    }

    /**
     * Soft clipping to prevent harsh distortion.
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
    protected void onReset() {
        if (delayLineL != null) {
            for (int ch = 0; ch < NUM_CHANNELS; ch++) {
                Arrays.fill(delayLineL[ch], 0);
                Arrays.fill(delayLineR[ch], 0);
                writePos[ch] = 0;
                lastFeedbackL[ch] = 0;
                lastFeedbackR[ch] = 0;

                if (channelFilterL[ch] != null) channelFilterL[ch].reset();
                if (channelFilterR[ch] != null) channelFilterR[ch].reset();
            }
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Global (3): dry, filter, masterFb
        // Row 2: Channel 1 (5): ch1On, ch1Time, ch1Fb, ch1Lvl, ch1Pan
        // Row 3: Channel 2 (5): ch2On, ch2Time, ch2Fb, ch2Lvl, ch2Pan
        // Row 4: Channel 3 (5): ch3On, ch3Time, ch3Fb, ch3Lvl, ch3Pan
        // Row 5: Channel 4 (5): ch4On, ch4Time, ch4Fb, ch4Lvl, ch4Pan
        return new int[] {3, 5, 5, 5, 5};
    }

    @Override
    public int getLatency() {
        return 0;  // No latency, delay is intentional
    }
}
