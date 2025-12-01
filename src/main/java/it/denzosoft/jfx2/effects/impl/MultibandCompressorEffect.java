package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * 3-band multiband compressor for mastering and dynamic EQ.
 *
 * <p>Splits the audio into three frequency bands (Low, Mid, High) using
 * Linkwitz-Riley crossover filters, applies independent compression to
 * each band, then sums them back together.</p>
 *
 * <p>Each band has:
 * <ul>
 *   <li>Enable toggle - bypass band processing</li>
 *   <li>Solo - listen to band in isolation</li>
 *   <li>Threshold, Ratio, Attack, Release - compression controls</li>
 *   <li>Gain - makeup/cut for band level</li>
 * </ul>
 * </p>
 *
 * <p>Uses 4th-order Linkwitz-Riley crossovers for phase-coherent band splitting.</p>
 */
public class MultibandCompressorEffect extends AbstractEffect {

    private static final int NUM_BANDS = 3;
    private static final String[] BAND_NAMES = {"Low", "Mid", "High"};

    // RMS window for level detection
    private static final float RMS_WINDOW_MS = 50.0f;

    // Global parameters
    private Parameter crossoverLowMid;
    private Parameter crossoverMidHigh;
    private Parameter outputLevel;

    // Per-band parameters
    private Parameter[] bandEnabled;
    private Parameter[] bandSolo;
    private Parameter[] bandThreshold;
    private Parameter[] bandRatio;
    private Parameter[] bandAttack;
    private Parameter[] bandRelease;
    private Parameter[] bandGain;

    // Crossover filter state (Linkwitz-Riley 4th order = 2x 2nd order Butterworth)
    // Low/Mid crossover
    private BiquadFilter[] lowpassLM_L;   // 2 cascaded for LR4
    private BiquadFilter[] lowpassLM_R;
    private BiquadFilter[] highpassLM_L;
    private BiquadFilter[] highpassLM_R;

    // Mid/High crossover
    private BiquadFilter[] lowpassMH_L;
    private BiquadFilter[] lowpassMH_R;
    private BiquadFilter[] highpassMH_L;
    private BiquadFilter[] highpassMH_R;

    // Compressor state per band (stereo)
    private float[] rmsLevelL;
    private float[] rmsLevelR;
    private float[] gainReductionL;
    private float[] gainReductionR;
    private float rmsCoeff;

    // Temporary buffers for band processing
    private float[] bandL;
    private float[] bandR;
    private float[] tempL;
    private float[] tempR;

    public MultibandCompressorEffect() {
        super(EffectMetadata.of("multibandcomp", "Multiband Comp",
                "3-band multiband compressor for mastering", EffectCategory.DYNAMICS));
        setStereoMode(StereoMode.STEREO);
        initParameters();
    }

    private void initParameters() {
        // === ROW 1: Global parameters ===
        crossoverLowMid = addFloatParameter("xLowMid", "Low/Mid",
                "Crossover frequency between Low and Mid bands.",
                20.0f, 1000.0f, 200.0f, "Hz");

        crossoverMidHigh = addFloatParameter("xMidHigh", "Mid/High",
                "Crossover frequency between Mid and High bands.",
                1000.0f, 16000.0f, 3000.0f, "Hz");

        outputLevel = addFloatParameter("output", "Output",
                "Master output level.",
                -12.0f, 12.0f, 0.0f, "dB");

        // Per-band parameter arrays
        bandEnabled = new Parameter[NUM_BANDS];
        bandSolo = new Parameter[NUM_BANDS];
        bandThreshold = new Parameter[NUM_BANDS];
        bandRatio = new Parameter[NUM_BANDS];
        bandAttack = new Parameter[NUM_BANDS];
        bandRelease = new Parameter[NUM_BANDS];
        bandGain = new Parameter[NUM_BANDS];

        // Default values per band
        float[] defaultThresholds = {-20.0f, -18.0f, -16.0f};
        float[] defaultRatios = {4.0f, 3.0f, 2.5f};
        float[] defaultAttacks = {20.0f, 10.0f, 5.0f};
        float[] defaultReleases = {200.0f, 150.0f, 100.0f};

        // === ROW 2: Low Band ===
        // === ROW 3: Mid Band ===
        // === ROW 4: High Band ===
        for (int b = 0; b < NUM_BANDS; b++) {
            String prefix = BAND_NAMES[b].toLowerCase().charAt(0) + "";  // l, m, h

            // Toggle at the start of each band row
            bandEnabled[b] = addBooleanParameter(prefix + "On", BAND_NAMES[b],
                    "Enable/disable " + BAND_NAMES[b] + " band compression.",
                    true);

            bandSolo[b] = addBooleanParameter(prefix + "Solo", "Solo",
                    "Solo " + BAND_NAMES[b] + " band (mute others).",
                    false);

            bandThreshold[b] = addFloatParameter(prefix + "Thr", "Thr",
                    BAND_NAMES[b] + " band compression threshold.",
                    -60.0f, 0.0f, defaultThresholds[b], "dB");

            bandRatio[b] = addFloatParameter(prefix + "Ratio", "Ratio",
                    BAND_NAMES[b] + " band compression ratio.",
                    1.0f, 20.0f, defaultRatios[b], ":1");

            bandAttack[b] = addFloatParameter(prefix + "Atk", "Atk",
                    BAND_NAMES[b] + " band attack time.",
                    0.1f, 100.0f, defaultAttacks[b], "ms");

            bandRelease[b] = addFloatParameter(prefix + "Rel", "Rel",
                    BAND_NAMES[b] + " band release time.",
                    10.0f, 1000.0f, defaultReleases[b], "ms");

            bandGain[b] = addFloatParameter(prefix + "Gain", "Gain",
                    BAND_NAMES[b] + " band makeup/cut gain.",
                    -12.0f, 12.0f, 0.0f, "dB");
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // RMS coefficient
        rmsCoeff = (float) Math.exp(-1.0 / (RMS_WINDOW_MS * sampleRate / 1000.0));

        // Initialize compressor state
        rmsLevelL = new float[NUM_BANDS];
        rmsLevelR = new float[NUM_BANDS];
        gainReductionL = new float[NUM_BANDS];
        gainReductionR = new float[NUM_BANDS];
        for (int b = 0; b < NUM_BANDS; b++) {
            gainReductionL[b] = 1.0f;
            gainReductionR[b] = 1.0f;
        }

        // Initialize crossover filters (2 cascaded biquads for LR4)
        lowpassLM_L = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        lowpassLM_R = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        highpassLM_L = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        highpassLM_R = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};

        lowpassMH_L = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        lowpassMH_R = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        highpassMH_L = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};
        highpassMH_R = new BiquadFilter[]{new BiquadFilter(), new BiquadFilter()};

        updateCrossoverFilters();

        // Allocate temp buffers
        bandL = new float[maxFrameCount];
        bandR = new float[maxFrameCount];
        tempL = new float[maxFrameCount];
        tempR = new float[maxFrameCount];
    }

    private void updateCrossoverFilters() {
        float freqLM = crossoverLowMid.getValue();
        float freqMH = crossoverMidHigh.getValue();

        // Butterworth Q for LR4 crossover
        float Q = 0.7071f;  // 1/sqrt(2)

        // Low/Mid crossover
        for (int i = 0; i < 2; i++) {
            lowpassLM_L[i].setLowpass(freqLM, Q, sampleRate);
            lowpassLM_R[i].setLowpass(freqLM, Q, sampleRate);
            highpassLM_L[i].setHighpass(freqLM, Q, sampleRate);
            highpassLM_R[i].setHighpass(freqLM, Q, sampleRate);
        }

        // Mid/High crossover
        for (int i = 0; i < 2; i++) {
            lowpassMH_L[i].setLowpass(freqMH, Q, sampleRate);
            lowpassMH_R[i].setLowpass(freqMH, Q, sampleRate);
            highpassMH_L[i].setHighpass(freqMH, Q, sampleRate);
            highpassMH_R[i].setHighpass(freqMH, Q, sampleRate);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Process mono as stereo
        float[] tempOut = new float[frameCount];
        processInternal(input, input, output, tempOut, frameCount);
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        processInternal(inputL, inputR, outputL, outputR, frameCount);
    }

    private void processInternal(float[] inputL, float[] inputR,
                                 float[] outputL, float[] outputR, int frameCount) {
        // Update crossover filters if frequencies changed
        updateCrossoverFilters();

        float outGain = dbToLinear(outputLevel.getValue());

        // Check for solo
        int soloIndex = -1;
        for (int b = 0; b < NUM_BANDS; b++) {
            if (bandSolo[b].getBooleanValue()) {
                soloIndex = b;
                break;
            }
        }

        // Initialize output to zero
        for (int i = 0; i < frameCount; i++) {
            outputL[i] = 0;
            outputR[i] = 0;
        }

        // Process each band
        for (int b = 0; b < NUM_BANDS; b++) {
            // Skip if solo is active and this isn't the solo band
            if (soloIndex >= 0 && soloIndex != b) continue;

            // Extract band
            extractBand(inputL, inputR, bandL, bandR, frameCount, b);

            // Apply compression if enabled
            if (bandEnabled[b].getBooleanValue()) {
                compressBand(bandL, bandR, frameCount, b);
            }

            // Apply band gain
            float bandGainLinear = dbToLinear(bandGain[b].getValue());

            // Add to output
            for (int i = 0; i < frameCount; i++) {
                outputL[i] += bandL[i] * bandGainLinear;
                outputR[i] += bandR[i] * bandGainLinear;
            }
        }

        // Apply output gain
        for (int i = 0; i < frameCount; i++) {
            outputL[i] *= outGain;
            outputR[i] *= outGain;
        }
    }

    /**
     * Extract a frequency band from the input signal.
     */
    private void extractBand(float[] inputL, float[] inputR,
                             float[] outL, float[] outR,
                             int frameCount, int bandIndex) {
        switch (bandIndex) {
            case 0:  // Low band: lowpass at Low/Mid frequency
                for (int i = 0; i < frameCount; i++) {
                    float sampleL = inputL[i];
                    float sampleR = inputR[i];
                    // Cascade 2 lowpass filters for LR4
                    sampleL = lowpassLM_L[0].process(sampleL);
                    sampleL = lowpassLM_L[1].process(sampleL);
                    sampleR = lowpassLM_R[0].process(sampleR);
                    sampleR = lowpassLM_R[1].process(sampleR);
                    outL[i] = sampleL;
                    outR[i] = sampleR;
                }
                break;

            case 1:  // Mid band: highpass at Low/Mid, lowpass at Mid/High
                for (int i = 0; i < frameCount; i++) {
                    float sampleL = inputL[i];
                    float sampleR = inputR[i];
                    // Highpass at Low/Mid
                    sampleL = highpassLM_L[0].process(sampleL);
                    sampleL = highpassLM_L[1].process(sampleL);
                    sampleR = highpassLM_R[0].process(sampleR);
                    sampleR = highpassLM_R[1].process(sampleR);
                    // Lowpass at Mid/High
                    sampleL = lowpassMH_L[0].process(sampleL);
                    sampleL = lowpassMH_L[1].process(sampleL);
                    sampleR = lowpassMH_R[0].process(sampleR);
                    sampleR = lowpassMH_R[1].process(sampleR);
                    outL[i] = sampleL;
                    outR[i] = sampleR;
                }
                break;

            case 2:  // High band: highpass at Mid/High frequency
                for (int i = 0; i < frameCount; i++) {
                    float sampleL = inputL[i];
                    float sampleR = inputR[i];
                    // Cascade 2 highpass filters for LR4
                    sampleL = highpassMH_L[0].process(sampleL);
                    sampleL = highpassMH_L[1].process(sampleL);
                    sampleR = highpassMH_R[0].process(sampleR);
                    sampleR = highpassMH_R[1].process(sampleR);
                    outL[i] = sampleL;
                    outR[i] = sampleR;
                }
                break;
        }
    }

    /**
     * Apply compression to a band.
     */
    private void compressBand(float[] dataL, float[] dataR, int frameCount, int bandIndex) {
        float threshold = bandThreshold[bandIndex].getValue();
        float ratio = bandRatio[bandIndex].getValue();
        float attackMs = bandAttack[bandIndex].getValue();
        float releaseMs = bandRelease[bandIndex].getValue();

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));

        for (int i = 0; i < frameCount; i++) {
            float sampleL = dataL[i];
            float sampleR = dataR[i];

            // RMS detection (linked stereo)
            float squared = (sampleL * sampleL + sampleR * sampleR) * 0.5f;
            rmsLevelL[bandIndex] = rmsCoeff * rmsLevelL[bandIndex] + (1.0f - rmsCoeff) * squared;
            float rmsDb = linearToDb((float) Math.sqrt(rmsLevelL[bandIndex]));

            // Calculate gain reduction
            float targetGainDb = calculateGainReduction(rmsDb, threshold, ratio);
            float targetGainLinear = dbToLinear(targetGainDb);

            // Smooth gain changes
            if (targetGainLinear < gainReductionL[bandIndex]) {
                gainReductionL[bandIndex] = attackCoeff * gainReductionL[bandIndex]
                        + (1.0f - attackCoeff) * targetGainLinear;
            } else {
                gainReductionL[bandIndex] = releaseCoeff * gainReductionL[bandIndex]
                        + (1.0f - releaseCoeff) * targetGainLinear;
            }

            // Apply gain reduction
            dataL[i] = sampleL * gainReductionL[bandIndex];
            dataR[i] = sampleR * gainReductionL[bandIndex];
        }
    }

    /**
     * Calculate gain reduction in dB (soft knee).
     */
    private float calculateGainReduction(float inputDb, float thresholdDb, float ratio) {
        if (ratio <= 1.0f) return 0.0f;

        float kneeDb = 6.0f;  // Fixed soft knee
        float halfKnee = kneeDb / 2.0f;
        float overshoot = inputDb - thresholdDb;

        if (overshoot <= -halfKnee) {
            return 0.0f;
        } else if (overshoot >= halfKnee) {
            return -(overshoot * (1.0f - 1.0f / ratio));
        } else {
            float x = overshoot + halfKnee;
            return -(x * x * (1.0f - 1.0f / ratio)) / (2.0f * kneeDb);
        }
    }

    @Override
    protected void onReset() {
        // Reset compressor state
        if (rmsLevelL != null) {
            for (int b = 0; b < NUM_BANDS; b++) {
                rmsLevelL[b] = 0;
                rmsLevelR[b] = 0;
                gainReductionL[b] = 1.0f;
                gainReductionR[b] = 1.0f;
            }
        }

        // Reset filters
        if (lowpassLM_L != null) {
            for (int i = 0; i < 2; i++) {
                lowpassLM_L[i].reset();
                lowpassLM_R[i].reset();
                highpassLM_L[i].reset();
                highpassLM_R[i].reset();
                lowpassMH_L[i].reset();
                lowpassMH_R[i].reset();
                highpassMH_L[i].reset();
                highpassMH_R[i].reset();
            }
        }
    }

    /**
     * Get gain reduction in dB for a specific band.
     */
    public float getGainReductionDb(int bandIndex) {
        if (bandIndex < 0 || bandIndex >= NUM_BANDS) return 0;
        return linearToDb(gainReductionL[bandIndex]);
    }

    /**
     * Simple biquad filter for crossover implementation.
     */
    private static class BiquadFilter {
        private float b0, b1, b2, a1, a2;
        private float x1, x2, y1, y2;

        void setLowpass(float freq, float Q, int sampleRate) {
            float w0 = (float) (2.0 * Math.PI * freq / sampleRate);
            float cosw0 = (float) Math.cos(w0);
            float sinw0 = (float) Math.sin(w0);
            float alpha = sinw0 / (2.0f * Q);

            float a0 = 1.0f + alpha;
            b0 = ((1.0f - cosw0) / 2.0f) / a0;
            b1 = (1.0f - cosw0) / a0;
            b2 = ((1.0f - cosw0) / 2.0f) / a0;
            a1 = (-2.0f * cosw0) / a0;
            a2 = (1.0f - alpha) / a0;
        }

        void setHighpass(float freq, float Q, int sampleRate) {
            float w0 = (float) (2.0 * Math.PI * freq / sampleRate);
            float cosw0 = (float) Math.cos(w0);
            float sinw0 = (float) Math.sin(w0);
            float alpha = sinw0 / (2.0f * Q);

            float a0 = 1.0f + alpha;
            b0 = ((1.0f + cosw0) / 2.0f) / a0;
            b1 = (-(1.0f + cosw0)) / a0;
            b2 = ((1.0f + cosw0) / 2.0f) / a0;
            a1 = (-2.0f * cosw0) / a0;
            a2 = (1.0f - alpha) / a0;
        }

        float process(float x0) {
            float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
            return y0;
        }

        void reset() {
            x1 = x2 = y1 = y2 = 0;
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Global parameters (3): xLowMid, xMidHigh, output
        // Row 2: Low band (7): lOn, lSolo, lThr, lRatio, lAtk, lRel, lGain
        // Row 3: Mid band (7): mOn, mSolo, mThr, mRatio, mAtk, mRel, mGain
        // Row 4: High band (7): hOn, hSolo, hThr, hRatio, hAtk, hRel, hGain
        return new int[] {3, 7, 7, 7};
    }
}
