package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * 10-band Graphic Equalizer effect.
 *
 * <p>Features:
 * - 10 bands with fixed ISO standard frequencies: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
 * - Individual gain control for each band (-12 to +12 dB)
 * - Single Q parameter shared by all bands
 * - All bands use Peak filter type
 *
 * <p>This is a classic graphic EQ design where frequency bands are fixed
 * and only the gain (boost/cut) is adjustable per band.</p>
 */
public class GraphicEQEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "graphiceq",
            "Graphic EQ",
            "10-band graphic equalizer with fixed frequencies",
            EffectCategory.EQ
    );

    // Number of EQ bands
    public static final int NUM_BANDS = 10;

    // ISO standard center frequencies for 10-band graphic EQ
    public static final float[] BAND_FREQUENCIES = {
            31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
    };

    // Labels for display
    public static final String[] BAND_LABELS = {
            "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"
    };

    // Parameters
    private final Parameter[] gainParams = new Parameter[NUM_BANDS];
    private final Parameter qParam;
    private final Parameter outputParam;

    // Filters for stereo processing
    private final BiquadFilter[] filtersL = new BiquadFilter[NUM_BANDS];
    private final BiquadFilter[] filtersR = new BiquadFilter[NUM_BANDS];

    // Change detection
    private final float[] lastGain = new float[NUM_BANDS];
    private float lastQ = -1;

    public GraphicEQEffect() {
        super(METADATA);

        // Create gain parameters for each band
        for (int i = 0; i < NUM_BANDS; i++) {
            String label = BAND_LABELS[i];
            gainParams[i] = addFloatParameter("gain" + (i + 1), label + " Hz",
                    "Gain for " + label + " Hz band.",
                    -12.0f, 12.0f, 0.0f, "dB");
            lastGain[i] = Float.NaN;
        }

        // Single Q parameter for all bands
        qParam = addFloatParameter("q", "Q",
                "Bandwidth (Q factor) for all bands. Lower = wider, higher = narrower.",
                0.5f, 4.0f, 1.5f, "");

        // Output level
        outputParam = addFloatParameter("output", "Output",
                "Output level after EQ.",
                -12.0f, 12.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        for (int i = 0; i < NUM_BANDS; i++) {
            filtersL[i] = new BiquadFilter();
            filtersL[i].setSampleRate(sampleRate);
            filtersR[i] = new BiquadFilter();
            filtersR[i].setSampleRate(sampleRate);
            lastGain[i] = Float.NaN; // Force update
        }
        lastQ = -1;
        updateFilters();
    }

    private void updateFilters() {
        float q = qParam.getValue();
        boolean qChanged = q != lastQ;
        lastQ = q;

        for (int i = 0; i < NUM_BANDS; i++) {
            float gain = gainParams[i].getValue();

            // Only update if parameters changed
            if (!qChanged && gain == lastGain[i]) {
                continue;
            }

            lastGain[i] = gain;

            // Band 0 (31Hz): High-pass filter
            // Band 9 (16kHz): Low-pass filter
            // Other bands: Peak filter
            if (i == 0) {
                // High-pass filter - gain controls resonance Q for this band
                float hpQ = 0.707f + (gain / 12.0f) * 2.0f;  // Q varies from ~0.5 to ~2.5
                hpQ = Math.max(0.5f, Math.min(3.0f, hpQ));
                filtersL[i].configure(FilterType.HIGHPASS, BAND_FREQUENCIES[i], hpQ, 0);
                filtersR[i].configure(FilterType.HIGHPASS, BAND_FREQUENCIES[i], hpQ, 0);
            } else if (i == NUM_BANDS - 1) {
                // Low-pass filter - gain controls resonance Q for this band
                float lpQ = 0.707f + (gain / 12.0f) * 2.0f;  // Q varies from ~0.5 to ~2.5
                lpQ = Math.max(0.5f, Math.min(3.0f, lpQ));
                filtersL[i].configure(FilterType.LOWPASS, BAND_FREQUENCIES[i], lpQ, 0);
                filtersR[i].configure(FilterType.LOWPASS, BAND_FREQUENCIES[i], lpQ, 0);
            } else {
                // Peak filter with gain
                filtersL[i].configure(FilterType.PEAK, BAND_FREQUENCIES[i], q, gain);
                filtersR[i].configure(FilterType.PEAK, BAND_FREQUENCIES[i], q, gain);
            }
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        updateFilters();
        float outputGain = dbToLinear(outputParam.getValue());

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Process through all bands (even 0 dB bands for phase consistency)
            for (int b = 0; b < NUM_BANDS; b++) {
                sample = filtersL[b].process(sample);
            }

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

            // Process through all bands
            for (int b = 0; b < NUM_BANDS; b++) {
                sampleL = filtersL[b].process(sampleL);
                sampleR = filtersR[b].process(sampleR);
            }

            outputL[i] = sampleL * outputGain;
            outputR[i] = sampleR * outputGain;
        }
    }

    @Override
    protected void onReset() {
        for (int i = 0; i < NUM_BANDS; i++) {
            if (filtersL[i] != null) filtersL[i].reset();
            if (filtersR[i] != null) filtersR[i].reset();
        }
    }

    // ==================== Frequency Response Calculation ====================

    /**
     * Calculate the combined frequency response magnitude at a given frequency.
     *
     * @param frequency The frequency in Hz
     * @return The magnitude response in dB
     */
    public double getFrequencyResponseDb(double frequency) {
        double totalMagnitudeDb = 0.0;
        // Use getTargetValue() for UI display (immediate response to user changes)
        float q = qParam.getTargetValue();
        // Use default sample rate if not yet prepared
        int sr = sampleRate > 0 ? sampleRate : 44100;

        for (int i = 0; i < NUM_BANDS; i++) {
            float gain = gainParams[i].getTargetValue();

            // Skip bands with zero gain (flat response contributes 0 dB)
            if (Math.abs(gain) < 0.01) {
                continue;
            }

            double magnitude;
            if (i == 0) {
                // High-pass filter - gain controls Q, but only show effect when gain != 0
                float hpQ = 0.707f + (gain / 12.0f) * 2.0f;
                hpQ = Math.max(0.5f, Math.min(3.0f, hpQ));
                magnitude = calculateHighpassMagnitude(frequency, BAND_FREQUENCIES[i], hpQ, sr);
            } else if (i == NUM_BANDS - 1) {
                // Low-pass filter - gain controls Q, but only show effect when gain != 0
                float lpQ = 0.707f + (gain / 12.0f) * 2.0f;
                lpQ = Math.max(0.5f, Math.min(3.0f, lpQ));
                magnitude = calculateLowpassMagnitude(frequency, BAND_FREQUENCIES[i], lpQ, sr);
            } else {
                // Peak filter with gain
                magnitude = calculatePeakMagnitude(frequency, BAND_FREQUENCIES[i], q, gain, sr);
            }

            double magnitudeDb = 20.0 * Math.log10(Math.max(magnitude, 1e-10));
            totalMagnitudeDb += magnitudeDb;
        }

        return totalMagnitudeDb;
    }

    /**
     * Calculate peak filter magnitude response at a given frequency.
     */
    private double calculatePeakMagnitude(double freq, float centerFreq, float q, float gainDb, int sr) {
        double w0 = 2.0 * Math.PI * centerFreq / sr;
        double w = 2.0 * Math.PI * freq / sr;

        double cosW0 = Math.cos(w0);
        double sinW0 = Math.sin(w0);
        double alpha = sinW0 / (2.0 * q);
        double A = Math.pow(10.0, gainDb / 40.0);

        // Peak filter coefficients
        double b0 = 1 + alpha * A;
        double b1 = -2 * cosW0;
        double b2 = 1 - alpha * A;
        double a0 = 1 + alpha / A;
        double a1 = -2 * cosW0;
        double a2 = 1 - alpha / A;

        // Normalize coefficients
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;

        // Calculate magnitude at frequency w
        double cosW = Math.cos(w);
        double cos2W = Math.cos(2 * w);
        double sinW = Math.sin(w);
        double sin2W = Math.sin(2 * w);

        double numReal = b0 + b1 * cosW + b2 * cos2W;
        double numImag = -(b1 * sinW + b2 * sin2W);
        double denReal = 1 + a1 * cosW + a2 * cos2W;
        double denImag = -(a1 * sinW + a2 * sin2W);

        double numMag = Math.sqrt(numReal * numReal + numImag * numImag);
        double denMag = Math.sqrt(denReal * denReal + denImag * denImag);

        return numMag / denMag;
    }

    /**
     * Calculate high-pass filter magnitude response at a given frequency.
     */
    private double calculateHighpassMagnitude(double freq, float cutoffFreq, float q, int sr) {
        double w0 = 2.0 * Math.PI * cutoffFreq / sr;
        double w = 2.0 * Math.PI * freq / sr;

        double cosW0 = Math.cos(w0);
        double sinW0 = Math.sin(w0);
        double alpha = sinW0 / (2.0 * q);

        // High-pass filter coefficients
        double b0 = (1 + cosW0) / 2;
        double b1 = -(1 + cosW0);
        double b2 = (1 + cosW0) / 2;
        double a0 = 1 + alpha;
        double a1 = -2 * cosW0;
        double a2 = 1 - alpha;

        // Normalize coefficients
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;

        // Calculate magnitude at frequency w
        double cosW = Math.cos(w);
        double cos2W = Math.cos(2 * w);
        double sinW = Math.sin(w);
        double sin2W = Math.sin(2 * w);

        double numReal = b0 + b1 * cosW + b2 * cos2W;
        double numImag = -(b1 * sinW + b2 * sin2W);
        double denReal = 1 + a1 * cosW + a2 * cos2W;
        double denImag = -(a1 * sinW + a2 * sin2W);

        double numMag = Math.sqrt(numReal * numReal + numImag * numImag);
        double denMag = Math.sqrt(denReal * denReal + denImag * denImag);

        return numMag / denMag;
    }

    /**
     * Calculate low-pass filter magnitude response at a given frequency.
     */
    private double calculateLowpassMagnitude(double freq, float cutoffFreq, float q, int sr) {
        double w0 = 2.0 * Math.PI * cutoffFreq / sr;
        double w = 2.0 * Math.PI * freq / sr;

        double cosW0 = Math.cos(w0);
        double sinW0 = Math.sin(w0);
        double alpha = sinW0 / (2.0 * q);

        // Low-pass filter coefficients
        double b0 = (1 - cosW0) / 2;
        double b1 = 1 - cosW0;
        double b2 = (1 - cosW0) / 2;
        double a0 = 1 + alpha;
        double a1 = -2 * cosW0;
        double a2 = 1 - alpha;

        // Normalize coefficients
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;

        // Calculate magnitude at frequency w
        double cosW = Math.cos(w);
        double cos2W = Math.cos(2 * w);
        double sinW = Math.sin(w);
        double sin2W = Math.sin(2 * w);

        double numReal = b0 + b1 * cosW + b2 * cos2W;
        double numImag = -(b1 * sinW + b2 * sin2W);
        double denReal = 1 + a1 * cosW + a2 * cos2W;
        double denImag = -(a1 * sinW + a2 * sin2W);

        double numMag = Math.sqrt(numReal * numReal + numImag * numImag);
        double denMag = Math.sqrt(denReal * denReal + denImag * denImag);

        return numMag / denMag;
    }

    /**
     * Get the frequency response curve as an array of dB values.
     *
     * @param numPoints Number of points (frequencies are logarithmically spaced from 20Hz to 20kHz)
     * @return Array of magnitude values in dB
     */
    public double[] getFrequencyResponseCurve(int numPoints) {
        double[] response = new double[numPoints];
        double minFreq = 20.0;
        double maxFreq = 20000.0;
        double logMin = Math.log10(minFreq);
        double logMax = Math.log10(maxFreq);

        for (int i = 0; i < numPoints; i++) {
            double logFreq = logMin + (double) i / (numPoints - 1) * (logMax - logMin);
            double freq = Math.pow(10.0, logFreq);
            response[i] = getFrequencyResponseDb(freq);
        }

        return response;
    }

    // ==================== Convenience Accessors ====================

    /**
     * Get gain for a band (returns target value for UI display).
     */
    public float getBandGain(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return gainParams[band].getTargetValue();
        }
        return 0f;
    }

    /**
     * Set gain for a band.
     */
    public void setBandGain(int band, float dB) {
        if (band >= 0 && band < NUM_BANDS) {
            gainParams[band].setValue(dB);
        }
    }

    /**
     * Get the Q value (returns target value for UI display).
     */
    public float getQ() {
        return qParam.getTargetValue();
    }

    /**
     * Set the Q value for all bands.
     */
    public void setQ(float q) {
        qParam.setValue(q);
    }

    /**
     * Get frequency for a band.
     */
    public float getBandFrequency(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return BAND_FREQUENCIES[band];
        }
        return 1000f;
    }

    /**
     * Set output level.
     */
    public void setOutput(float dB) {
        outputParam.setValue(dB);
    }

    /**
     * Reset all bands to 0 dB (flat response).
     */
    public void flatten() {
        for (int i = 0; i < NUM_BANDS; i++) {
            gainParams[i].setValue(0f);
        }
    }
}
