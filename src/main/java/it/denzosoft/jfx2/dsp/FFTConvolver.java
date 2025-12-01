package it.denzosoft.jfx2.dsp;

/**
 * FFT-based convolution engine for impulse response processing.
 *
 * <p>Uses overlap-add method with radix-2 FFT for efficient convolution.
 * Suitable for cabinet simulation with IR lengths up to 4096 samples.</p>
 */
public class FFTConvolver {

    private int fftSize;
    private int blockSize;
    private int irLength;

    // FFT buffers (real and imaginary parts)
    private float[] realBuffer;
    private float[] imagBuffer;

    // IR in frequency domain
    private float[] irReal;
    private float[] irImag;

    // Overlap buffer for overlap-add
    private float[] overlapBuffer;

    // Input accumulation buffer
    private float[] inputBuffer;
    private int inputBufferPos;

    // Twiddle factors for FFT
    private float[] twiddleReal;
    private float[] twiddleImag;

    private boolean prepared;

    public FFTConvolver() {
        this.prepared = false;
    }

    /**
     * Prepare the convolver with an impulse response.
     *
     * @param ir         The impulse response samples
     * @param irLength   Length of the IR
     * @param blockSize  Processing block size
     */
    public void prepare(float[] ir, int irLength, int blockSize) {
        this.blockSize = blockSize;
        this.irLength = irLength;

        // FFT size must be at least blockSize + irLength - 1, rounded up to power of 2
        int minFftSize = blockSize + irLength - 1;
        this.fftSize = nextPowerOf2(minFftSize);

        // Allocate buffers
        realBuffer = new float[fftSize];
        imagBuffer = new float[fftSize];
        irReal = new float[fftSize];
        irImag = new float[fftSize];
        overlapBuffer = new float[fftSize];
        inputBuffer = new float[blockSize];
        inputBufferPos = 0;

        // Compute twiddle factors
        computeTwiddleFactors();

        // Transform IR to frequency domain
        System.arraycopy(ir, 0, irReal, 0, Math.min(ir.length, irLength));
        for (int i = irLength; i < fftSize; i++) {
            irReal[i] = 0;
        }
        java.util.Arrays.fill(irImag, 0);

        fft(irReal, irImag, false);

        prepared = true;
    }

    /**
     * Process a block of samples through convolution.
     */
    public void process(float[] input, float[] output, int numSamples) {
        if (!prepared) {
            System.arraycopy(input, 0, output, 0, numSamples);
            return;
        }

        for (int i = 0; i < numSamples; i++) {
            inputBuffer[inputBufferPos++] = input[i];

            if (inputBufferPos >= blockSize) {
                processBlock();
                inputBufferPos = 0;
            }

            // Output from overlap buffer
            output[i] = overlapBuffer[i % blockSize];
        }
    }

    private void processBlock() {
        // Copy input to real buffer, zero-pad
        System.arraycopy(inputBuffer, 0, realBuffer, 0, blockSize);
        for (int i = blockSize; i < fftSize; i++) {
            realBuffer[i] = 0;
        }
        java.util.Arrays.fill(imagBuffer, 0);

        // Forward FFT
        fft(realBuffer, imagBuffer, false);

        // Complex multiplication with IR spectrum
        for (int i = 0; i < fftSize; i++) {
            float re = realBuffer[i] * irReal[i] - imagBuffer[i] * irImag[i];
            float im = realBuffer[i] * irImag[i] + imagBuffer[i] * irReal[i];
            realBuffer[i] = re;
            imagBuffer[i] = im;
        }

        // Inverse FFT
        fft(realBuffer, imagBuffer, true);

        // Overlap-add
        for (int i = 0; i < fftSize; i++) {
            if (i < blockSize) {
                // Output samples + previous overlap
                overlapBuffer[i] = realBuffer[i] + overlapBuffer[blockSize + i];
            }
        }

        // Save new overlap for next block
        for (int i = 0; i < fftSize - blockSize; i++) {
            if (i + blockSize < fftSize) {
                overlapBuffer[blockSize + i] = realBuffer[blockSize + i];
            }
        }
    }

    /**
     * In-place radix-2 FFT/IFFT.
     */
    private void fft(float[] real, float[] imag, boolean inverse) {
        int n = fftSize;
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

    private void computeTwiddleFactors() {
        twiddleReal = new float[fftSize / 2];
        twiddleImag = new float[fftSize / 2];

        for (int i = 0; i < fftSize / 2; i++) {
            double angle = -2.0 * Math.PI * i / fftSize;
            twiddleReal[i] = (float) Math.cos(angle);
            twiddleImag[i] = (float) Math.sin(angle);
        }
    }

    private int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }

    /**
     * Reset the convolver state.
     */
    public void reset() {
        if (overlapBuffer != null) {
            java.util.Arrays.fill(overlapBuffer, 0);
        }
        if (inputBuffer != null) {
            java.util.Arrays.fill(inputBuffer, 0);
        }
        inputBufferPos = 0;
    }

    public int getLatency() {
        return blockSize;
    }

    public int getIRLength() {
        return irLength;
    }
}
