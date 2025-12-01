package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Multi-band filter effect with up to 5 configurable filter bands.
 *
 * <p>Each band can be set to one of these filter types:
 * - None: Disabled (no processing)
 * - Low Pass: Cuts high frequencies above cutoff
 * - High Pass: Cuts low frequencies below cutoff
 * - Band Pass: Passes frequencies around center, cuts others
 * - Notch: Cuts frequencies around center, passes others
 * - Peak: Boost/cut at center frequency (parametric EQ band)
 * - Low Shelf: Boost/cut below corner frequency
 * - High Shelf: Boost/cut above corner frequency
 * - All Pass: Changes phase without affecting amplitude
 *
 * All filter types use a biquad implementation with configurable
 * frequency, Q (resonance/bandwidth), and gain.</p>
 */
public class FilterEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "filter",
            "Filter",
            "Multi-band filter with up to 5 configurable bands",
            EffectCategory.EQ
    );

    // Number of filter bands
    public static final int NUM_BANDS = 5;

    // Filter type names for UI (None first)
    public static final String[] FILTER_TYPE_NAMES = {
            "None",
            "Low Pass",
            "High Pass",
            "Band Pass",
            "Notch",
            "Peak",
            "Low Shelf",
            "High Shelf",
            "All Pass"
    };

    // Filter type index constants
    public static final int TYPE_NONE = 0;
    public static final int TYPE_LOWPASS = 1;
    public static final int TYPE_HIGHPASS = 2;
    public static final int TYPE_BANDPASS = 3;
    public static final int TYPE_NOTCH = 4;
    public static final int TYPE_PEAK = 5;
    public static final int TYPE_LOWSHELF = 6;
    public static final int TYPE_HIGHSHELF = 7;
    public static final int TYPE_ALLPASS = 8;

    // Parameters for each band
    private final Parameter[] typeParams = new Parameter[NUM_BANDS];
    private final Parameter[] frequencyParams = new Parameter[NUM_BANDS];
    private final Parameter[] qParams = new Parameter[NUM_BANDS];
    private final Parameter[] gainParams = new Parameter[NUM_BANDS];

    // Output level
    private final Parameter outputParam;

    // Filters for stereo processing (left and right for each band)
    private final BiquadFilter[] filtersL = new BiquadFilter[NUM_BANDS];
    private final BiquadFilter[] filtersR = new BiquadFilter[NUM_BANDS];

    // Change detection
    private final int[] lastType = new int[NUM_BANDS];
    private final float[] lastFreq = new float[NUM_BANDS];
    private final float[] lastQ = new float[NUM_BANDS];
    private final float[] lastGain = new float[NUM_BANDS];

    public FilterEffect() {
        super(METADATA);

        // Default frequencies for each band (spread across spectrum)
        float[] defaultFreqs = {100f, 300f, 1000f, 3000f, 8000f};

        // Create parameters for each band
        for (int i = 0; i < NUM_BANDS; i++) {
            int bandNum = i + 1;

            typeParams[i] = addChoiceParameter("type" + bandNum, "Band " + bandNum + " Type",
                    "Filter type for band " + bandNum + ". Set to None to disable.",
                    FILTER_TYPE_NAMES, TYPE_NONE);

            frequencyParams[i] = addFloatParameter("freq" + bandNum, "Band " + bandNum + " Freq",
                    "Frequency for band " + bandNum + ".",
                    20.0f, 20000.0f, defaultFreqs[i], "Hz");

            qParams[i] = addFloatParameter("q" + bandNum, "Band " + bandNum + " Q",
                    "Q (resonance/bandwidth) for band " + bandNum + ".",
                    0.1f, 20.0f, 0.707f, "");

            gainParams[i] = addFloatParameter("gain" + bandNum, "Band " + bandNum + " Gain",
                    "Gain for band " + bandNum + " (Peak/Shelf only).",
                    -24.0f, 24.0f, 0.0f, "dB");

            // Initialize change detection
            lastType[i] = -1;
            lastFreq[i] = -1;
            lastQ[i] = -1;
            lastGain[i] = -1;
        }

        // Output level
        outputParam = addFloatParameter("output", "Output",
                "Output level after filtering.",
                -12.0f, 12.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        for (int i = 0; i < NUM_BANDS; i++) {
            filtersL[i] = new BiquadFilter();
            filtersL[i].setSampleRate(sampleRate);
            filtersR[i] = new BiquadFilter();
            filtersR[i].setSampleRate(sampleRate);
            lastType[i] = -1; // Force update
        }
        updateFilters();
    }

    private void updateFilters() {
        for (int i = 0; i < NUM_BANDS; i++) {
            int typeIndex = (int) typeParams[i].getValue();
            float freq = frequencyParams[i].getValue();
            float q = qParams[i].getValue();
            float gain = gainParams[i].getValue();

            // Only update if parameters changed
            if (typeIndex == lastType[i] && freq == lastFreq[i] && q == lastQ[i] && gain == lastGain[i]) {
                continue;
            }

            lastType[i] = typeIndex;
            lastFreq[i] = freq;
            lastQ[i] = q;
            lastGain[i] = gain;

            if (typeIndex != TYPE_NONE) {
                FilterType filterType = indexToFilterType(typeIndex);
                filtersL[i].configure(filterType, freq, q, gain);
                filtersR[i].configure(filterType, freq, q, gain);
            }
        }
    }

    private FilterType indexToFilterType(int index) {
        return switch (index) {
            case TYPE_LOWPASS -> FilterType.LOWPASS;
            case TYPE_HIGHPASS -> FilterType.HIGHPASS;
            case TYPE_BANDPASS -> FilterType.BANDPASS;
            case TYPE_NOTCH -> FilterType.NOTCH;
            case TYPE_PEAK -> FilterType.PEAK;
            case TYPE_LOWSHELF -> FilterType.LOWSHELF;
            case TYPE_HIGHSHELF -> FilterType.HIGHSHELF;
            case TYPE_ALLPASS -> FilterType.ALLPASS;
            default -> FilterType.LOWPASS;
        };
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        updateFilters();
        float outputGain = dbToLinear(outputParam.getValue());

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Process through each active band
            for (int b = 0; b < NUM_BANDS; b++) {
                if ((int) typeParams[b].getValue() != TYPE_NONE) {
                    sample = filtersL[b].process(sample);
                }
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

            // Process through each active band
            for (int b = 0; b < NUM_BANDS; b++) {
                if ((int) typeParams[b].getValue() != TYPE_NONE) {
                    sampleL = filtersL[b].process(sampleL);
                    sampleR = filtersR[b].process(sampleR);
                }
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

        for (int i = 0; i < NUM_BANDS; i++) {
            // Use getTargetValue() for UI display (immediate response to user changes)
            int typeIndex = (int) typeParams[i].getTargetValue();
            if (typeIndex == TYPE_NONE) {
                continue;
            }

            float freq = frequencyParams[i].getTargetValue();
            float q = qParams[i].getTargetValue();
            float gain = gainParams[i].getTargetValue();

            double magnitude = calculateBiquadMagnitude(typeIndex, frequency, freq, q, gain);
            double magnitudeDb = 20.0 * Math.log10(Math.max(magnitude, 1e-10));
            totalMagnitudeDb += magnitudeDb;
        }

        return totalMagnitudeDb;
    }

    /**
     * Calculate biquad filter magnitude response at a given frequency.
     */
    private double calculateBiquadMagnitude(int typeIndex, double freq, float centerFreq, float q, float gainDb) {
        // Use default sample rate if not yet prepared
        int sr = sampleRate > 0 ? sampleRate : 44100;
        // Normalized frequency (0 to 0.5)
        double w0 = 2.0 * Math.PI * centerFreq / sr;
        double w = 2.0 * Math.PI * freq / sr;

        double cosW0 = Math.cos(w0);
        double sinW0 = Math.sin(w0);
        double alpha = sinW0 / (2.0 * q);
        double A = Math.pow(10.0, gainDb / 40.0);

        double b0, b1, b2, a0, a1, a2;

        switch (typeIndex) {
            case TYPE_LOWPASS -> {
                b0 = (1 - cosW0) / 2;
                b1 = 1 - cosW0;
                b2 = (1 - cosW0) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cosW0;
                a2 = 1 - alpha;
            }
            case TYPE_HIGHPASS -> {
                b0 = (1 + cosW0) / 2;
                b1 = -(1 + cosW0);
                b2 = (1 + cosW0) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cosW0;
                a2 = 1 - alpha;
            }
            case TYPE_BANDPASS -> {
                b0 = alpha;
                b1 = 0;
                b2 = -alpha;
                a0 = 1 + alpha;
                a1 = -2 * cosW0;
                a2 = 1 - alpha;
            }
            case TYPE_NOTCH -> {
                b0 = 1;
                b1 = -2 * cosW0;
                b2 = 1;
                a0 = 1 + alpha;
                a1 = -2 * cosW0;
                a2 = 1 - alpha;
            }
            case TYPE_PEAK -> {
                b0 = 1 + alpha * A;
                b1 = -2 * cosW0;
                b2 = 1 - alpha * A;
                a0 = 1 + alpha / A;
                a1 = -2 * cosW0;
                a2 = 1 - alpha / A;
            }
            case TYPE_LOWSHELF -> {
                double sqrtA = Math.sqrt(A);
                double sqrtA2Alpha = 2 * sqrtA * alpha;
                b0 = A * ((A + 1) - (A - 1) * cosW0 + sqrtA2Alpha);
                b1 = 2 * A * ((A - 1) - (A + 1) * cosW0);
                b2 = A * ((A + 1) - (A - 1) * cosW0 - sqrtA2Alpha);
                a0 = (A + 1) + (A - 1) * cosW0 + sqrtA2Alpha;
                a1 = -2 * ((A - 1) + (A + 1) * cosW0);
                a2 = (A + 1) + (A - 1) * cosW0 - sqrtA2Alpha;
            }
            case TYPE_HIGHSHELF -> {
                double sqrtA = Math.sqrt(A);
                double sqrtA2Alpha = 2 * sqrtA * alpha;
                b0 = A * ((A + 1) + (A - 1) * cosW0 + sqrtA2Alpha);
                b1 = -2 * A * ((A - 1) + (A + 1) * cosW0);
                b2 = A * ((A + 1) + (A - 1) * cosW0 - sqrtA2Alpha);
                a0 = (A + 1) - (A - 1) * cosW0 + sqrtA2Alpha;
                a1 = 2 * ((A - 1) - (A + 1) * cosW0);
                a2 = (A + 1) - (A - 1) * cosW0 - sqrtA2Alpha;
            }
            case TYPE_ALLPASS -> {
                b0 = 1 - alpha;
                b1 = -2 * cosW0;
                b2 = 1 + alpha;
                a0 = 1 + alpha;
                a1 = -2 * cosW0;
                a2 = 1 - alpha;
            }
            default -> {
                return 1.0;
            }
        }

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
     * Get the number of active (non-None) bands.
     */
    public int getActiveBandCount() {
        int count = 0;
        for (int i = 0; i < NUM_BANDS; i++) {
            if ((int) typeParams[i].getValue() != TYPE_NONE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get filter type for a band (returns target value for UI display).
     */
    public int getBandType(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return (int) typeParams[band].getTargetValue();
        }
        return TYPE_NONE;
    }

    /**
     * Get frequency for a band (returns target value for UI display).
     */
    public float getBandFrequency(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return frequencyParams[band].getTargetValue();
        }
        return 1000f;
    }

    /**
     * Get Q for a band (returns target value for UI display).
     */
    public float getBandQ(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return qParams[band].getTargetValue();
        }
        return 0.707f;
    }

    /**
     * Get gain for a band (returns target value for UI display).
     */
    public float getBandGain(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return gainParams[band].getTargetValue();
        }
        return 0f;
    }

    // Convenience setters for programmatic control

    public void setBandType(int band, int typeIndex) {
        if (band >= 0 && band < NUM_BANDS) {
            typeParams[band].setValue(typeIndex);
        }
    }

    public void setBandFrequency(int band, float hz) {
        if (band >= 0 && band < NUM_BANDS) {
            frequencyParams[band].setValue(hz);
        }
    }

    public void setBandQ(int band, float q) {
        if (band >= 0 && band < NUM_BANDS) {
            qParams[band].setValue(q);
        }
    }

    public void setBandGain(int band, float dB) {
        if (band >= 0 && band < NUM_BANDS) {
            gainParams[band].setValue(dB);
        }
    }

    public void setOutput(float dB) {
        outputParam.setValue(dB);
    }
}
