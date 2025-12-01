package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import java.util.Arrays;

/**
 * Spectral noise suppressor effect.
 *
 * <p>Learns the noise profile when the signal is below a threshold (silent periods
 * with background noise), then uses spectral subtraction to remove that noise
 * from the signal.</p>
 *
 * <p>Algorithm:
 * <ol>
 *   <li>When signal &lt; threshold: accumulate FFT frames to build noise profile</li>
 *   <li>For each frame: FFT → subtract noise spectrum → IFFT</li>
 *   <li>Overlap-add for smooth output</li>
 * </ol>
 * </p>
 *
 * <p>The noise profile is reset when the effect is reset (signal path restart).</p>
 */
public class NoiseSuppressorEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "noisesuppressor",
            "Noise Suppressor",
            "Learns and removes background noise using spectral subtraction",
            EffectCategory.DYNAMICS
    );

    // FFT parameters
    private static final int FFT_SIZE = 2048;
    private static final int HOP_SIZE = FFT_SIZE / 4;  // 75% overlap
    private static final int MIN_LEARN_FRAMES = 10;    // Minimum frames to learn

    // === ROW 1: Detection Parameters ===
    private final Parameter thresholdParam;
    private final Parameter sensitivityParam;

    // === ROW 2: Reduction Parameters ===
    private final Parameter reductionParam;
    private final Parameter floorParam;

    // === ROW 3: Learning Control ===
    private final Parameter learnParam;
    private final Parameter smoothingParam;

    // === ROW 4: Output ===
    private final Parameter mixParam;

    // FFT buffers
    private float[] realBuffer;
    private float[] imagBuffer;
    private float[] windowedInput;
    private float[] outputAccum;

    // Noise profile (magnitude spectrum)
    private float[] noiseProfile;
    private float[] noiseProfileAccum;
    private int noiseFrameCount;
    private boolean profileReady;

    // Input/output ring buffers
    private float[] inputRing;
    private float[] outputRing;
    private int inputWritePos;
    private int outputReadPos;
    private int samplesUntilNextFFT;

    // Envelope follower for threshold detection
    private float envelope;

    // Hann window
    private float[] window;

    // Twiddle factors
    private float[] twiddleReal;
    private float[] twiddleImag;

    // Gain smoothing per bin
    private float[] smoothedGain;

    public NoiseSuppressorEffect() {
        super(METADATA);

        // === ROW 1: Detection ===
        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Signal level below which noise learning occurs. Set just above your noise floor.",
                -80.0f, -20.0f, -50.0f, "dB");

        sensitivityParam = addFloatParameter("sens", "Sensitivity",
                "Envelope follower speed for threshold detection.",
                1.0f, 100.0f, 20.0f, "ms");

        // === ROW 2: Reduction ===
        reductionParam = addFloatParameter("reduction", "Reduction",
                "Amount of noise reduction. Higher = more aggressive.",
                0.0f, 100.0f, 80.0f, "%");

        floorParam = addFloatParameter("floor", "Floor",
                "Minimum gain floor to prevent artifacts. Higher = safer but less reduction.",
                -60.0f, -6.0f, -30.0f, "dB");

        // === ROW 3: Learning ===
        learnParam = addBooleanParameter("learn", "Learn",
                "Enable automatic noise learning when signal is below threshold.",
                true);

        smoothingParam = addFloatParameter("smooth", "Smoothing",
                "Gain smoothing to reduce musical noise artifacts.",
                0.0f, 100.0f, 50.0f, "%");

        // === ROW 4: Output ===
        mixParam = addFloatParameter("mix", "Mix",
                "Dry/wet balance. 100% = full noise suppression.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Allocate FFT buffers
        realBuffer = new float[FFT_SIZE];
        imagBuffer = new float[FFT_SIZE];
        windowedInput = new float[FFT_SIZE];
        outputAccum = new float[FFT_SIZE];

        // Noise profile
        noiseProfile = new float[FFT_SIZE / 2 + 1];
        noiseProfileAccum = new float[FFT_SIZE / 2 + 1];
        noiseFrameCount = 0;
        profileReady = false;

        // Ring buffers
        inputRing = new float[FFT_SIZE * 2];
        outputRing = new float[FFT_SIZE * 2];
        inputWritePos = 0;
        outputReadPos = 0;
        samplesUntilNextFFT = HOP_SIZE;

        // Envelope
        envelope = 0;

        // Gain smoothing
        smoothedGain = new float[FFT_SIZE / 2 + 1];
        Arrays.fill(smoothedGain, 1.0f);

        // Create Hann window
        window = new float[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * i / (FFT_SIZE - 1)));
        }

        // Compute twiddle factors
        computeTwiddleFactors();

        // Initialize output accumulator
        Arrays.fill(outputAccum, 0);
    }

    private void computeTwiddleFactors() {
        twiddleReal = new float[FFT_SIZE / 2];
        twiddleImag = new float[FFT_SIZE / 2];

        for (int i = 0; i < FFT_SIZE / 2; i++) {
            double angle = -2.0 * Math.PI * i / FFT_SIZE;
            twiddleReal[i] = (float) Math.cos(angle);
            twiddleImag[i] = (float) Math.sin(angle);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float thresholdLin = dbToLinear(thresholdParam.getValue());
        float sensMs = sensitivityParam.getValue();
        float reduction = reductionParam.getValue() / 100.0f;
        float floorLin = dbToLinear(floorParam.getValue());
        boolean learning = learnParam.getBooleanValue();
        float smoothing = smoothingParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        float envCoeff = (float) Math.exp(-1.0 / (sensMs * sampleRate / 1000.0));

        for (int i = 0; i < frameCount; i++) {
            float dry = input[i];

            // Update envelope
            float absIn = Math.abs(dry);
            if (absIn > envelope) {
                envelope = envCoeff * envelope + (1 - envCoeff) * absIn;
            } else {
                envelope = envCoeff * envelope;
            }

            // Store in input ring buffer
            inputRing[inputWritePos] = dry;
            inputWritePos = (inputWritePos + 1) % inputRing.length;

            // Check if we should learn noise
            if (learning && envelope < thresholdLin) {
                // Will be handled in FFT processing
            }

            // Decrement counter
            samplesUntilNextFFT--;

            if (samplesUntilNextFFT <= 0) {
                // Process FFT frame
                processFFTFrame(thresholdLin, reduction, floorLin, smoothing, learning);
                samplesUntilNextFFT = HOP_SIZE;
            }

            // Read from output ring buffer
            float wet = outputRing[outputReadPos];
            outputRing[outputReadPos] = 0;  // Clear for next accumulation
            outputReadPos = (outputReadPos + 1) % outputRing.length;

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    private void processFFTFrame(float thresholdLin, float reduction, float floorLin,
                                  float smoothing, boolean learning) {
        // Extract frame from input ring buffer with window
        int readStart = (inputWritePos - FFT_SIZE + inputRing.length) % inputRing.length;
        for (int i = 0; i < FFT_SIZE; i++) {
            int idx = (readStart + i) % inputRing.length;
            windowedInput[i] = inputRing[idx] * window[i];
        }

        // Copy to FFT buffers
        System.arraycopy(windowedInput, 0, realBuffer, 0, FFT_SIZE);
        Arrays.fill(imagBuffer, 0);

        // Forward FFT
        fft(realBuffer, imagBuffer, false);

        // Calculate magnitude spectrum
        float[] magnitude = new float[FFT_SIZE / 2 + 1];
        float[] phase = new float[FFT_SIZE / 2 + 1];

        for (int i = 0; i <= FFT_SIZE / 2; i++) {
            float re = realBuffer[i];
            float im = imagBuffer[i];
            magnitude[i] = (float) Math.sqrt(re * re + im * im);
            phase[i] = (float) Math.atan2(im, re);
        }

        // Check if we should learn noise (use RMS of frame)
        float frameRMS = 0;
        for (int i = 0; i < FFT_SIZE; i++) {
            frameRMS += windowedInput[i] * windowedInput[i];
        }
        frameRMS = (float) Math.sqrt(frameRMS / FFT_SIZE);

        if (learning && frameRMS < thresholdLin) {
            // Accumulate noise profile
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                noiseProfileAccum[i] += magnitude[i];
            }
            noiseFrameCount++;

            // Update profile if we have enough frames
            if (noiseFrameCount >= MIN_LEARN_FRAMES) {
                for (int i = 0; i <= FFT_SIZE / 2; i++) {
                    noiseProfile[i] = noiseProfileAccum[i] / noiseFrameCount;
                }
                profileReady = true;
            }
        }

        // Apply noise suppression if profile is ready
        if (profileReady) {
            float oversubtract = 1.0f + reduction;  // Oversubtraction factor

            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                // Spectral subtraction
                float noiseMag = noiseProfile[i] * oversubtract * reduction;
                float cleanMag = magnitude[i] - noiseMag;

                // Apply floor
                float minMag = magnitude[i] * floorLin;
                if (cleanMag < minMag) {
                    cleanMag = minMag;
                }

                // Calculate gain
                float gain = (magnitude[i] > 0.0001f) ? cleanMag / magnitude[i] : floorLin;
                gain = Math.max(gain, floorLin);
                gain = Math.min(gain, 1.0f);

                // Smooth gain
                float smoothCoeff = smoothing * 0.95f + 0.01f;
                smoothedGain[i] = smoothCoeff * smoothedGain[i] + (1 - smoothCoeff) * gain;

                // Apply smoothed gain
                magnitude[i] *= smoothedGain[i];
            }
        }

        // Reconstruct complex spectrum
        for (int i = 0; i <= FFT_SIZE / 2; i++) {
            realBuffer[i] = magnitude[i] * (float) Math.cos(phase[i]);
            imagBuffer[i] = magnitude[i] * (float) Math.sin(phase[i]);
        }

        // Mirror for negative frequencies
        for (int i = 1; i < FFT_SIZE / 2; i++) {
            realBuffer[FFT_SIZE - i] = realBuffer[i];
            imagBuffer[FFT_SIZE - i] = -imagBuffer[i];
        }

        // Inverse FFT
        fft(realBuffer, imagBuffer, true);

        // Overlap-add to output ring buffer
        int writeStart = (outputReadPos + FFT_SIZE - HOP_SIZE + outputRing.length) % outputRing.length;
        for (int i = 0; i < FFT_SIZE; i++) {
            int idx = (writeStart + i) % outputRing.length;
            outputRing[idx] += realBuffer[i] * window[i];
        }
    }

    /**
     * In-place radix-2 FFT/IFFT.
     */
    private void fft(float[] real, float[] imag, boolean inverse) {
        int n = FFT_SIZE;
        int halfN = n / 2;

        // Bit-reversal permutation
        for (int i = 0, j = 0; i < n; i++) {
            if (j > i) {
                float tempR = real[i];
                float tempI = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempR;
                imag[j] = tempI;
            }
            int m = halfN;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        // Cooley-Tukey FFT
        for (int mmax = 1; mmax < n; mmax <<= 1) {
            int step = mmax << 1;
            int twiddleStep = n / step;

            for (int m = 0; m < mmax; m++) {
                int twiddleIdx = m * twiddleStep;
                float wr = twiddleReal[twiddleIdx];
                float wi = inverse ? -twiddleImag[twiddleIdx] : twiddleImag[twiddleIdx];

                for (int i = m; i < n; i += step) {
                    int j = i + mmax;
                    float tr = wr * real[j] - wi * imag[j];
                    float ti = wr * imag[j] + wi * real[j];
                    real[j] = real[i] - tr;
                    imag[j] = imag[i] - ti;
                    real[i] += tr;
                    imag[i] += ti;
                }
            }
        }

        // Scale for inverse transform
        if (inverse) {
            float scale = 1.0f / n;
            for (int i = 0; i < n; i++) {
                real[i] *= scale;
                imag[i] *= scale;
            }
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        // Process left channel
        onProcess(inputL, outputL, frameCount);

        // For stereo, copy left to right (linked processing)
        // A full stereo implementation would need separate buffers
        System.arraycopy(outputL, 0, outputR, 0, frameCount);
    }

    @Override
    protected void onReset() {
        // Reset noise profile - this happens when signal path restarts
        if (noiseProfile != null) {
            Arrays.fill(noiseProfile, 0);
            Arrays.fill(noiseProfileAccum, 0);
            noiseFrameCount = 0;
            profileReady = false;
        }

        // Reset buffers
        if (inputRing != null) Arrays.fill(inputRing, 0);
        if (outputRing != null) Arrays.fill(outputRing, 0);
        if (outputAccum != null) Arrays.fill(outputAccum, 0);
        if (smoothedGain != null) Arrays.fill(smoothedGain, 1.0f);

        inputWritePos = 0;
        outputReadPos = 0;
        samplesUntilNextFFT = HOP_SIZE;
        envelope = 0;
    }

    @Override
    public int getLatency() {
        return FFT_SIZE;
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Detection (2): threshold, sens
        // Row 2: Reduction (2): reduction, floor
        // Row 3: Learning (2): learn, smooth
        // Row 4: Output (1): mix
        return new int[] {2, 2, 2, 1};
    }

    /**
     * Check if noise profile has been learned.
     */
    public boolean isProfileReady() {
        return profileReady;
    }

    /**
     * Get the number of frames used for noise learning.
     */
    public int getLearnedFrameCount() {
        return noiseFrameCount;
    }

    /**
     * Manually reset the noise profile to start fresh learning.
     */
    public void resetNoiseProfile() {
        if (noiseProfile != null) {
            Arrays.fill(noiseProfile, 0);
            Arrays.fill(noiseProfileAccum, 0);
            noiseFrameCount = 0;
            profileReady = false;
        }
    }
}
