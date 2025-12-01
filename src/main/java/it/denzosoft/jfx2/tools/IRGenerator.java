package it.denzosoft.jfx2.tools;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * High-quality Impulse Response generator tool.
 *
 * <p>Generates an IR by deconvolving a "wet" signal from a "dry" signal
 * using advanced DSP techniques for professional-quality results.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Wiener deconvolution with regularization for noise robustness</li>
 *   <li>Automatic time alignment via cross-correlation</li>
 *   <li>Logarithmic sweep generation for optimal SNR</li>
 *   <li>Inverse sweep filtering for linear deconvolution</li>
 *   <li>Spectral smoothing to reduce noise</li>
 *   <li>Minimum phase conversion option</li>
 *   <li>Quality analysis and reporting</li>
 * </ul>
 * </p>
 */
public class IRGenerator {

    // Wiener filter regularization parameter (higher = more noise reduction, less detail)
    private static final float DEFAULT_REGULARIZATION = 0.001f;

    // Noise floor threshold in dB
    private static final float NOISE_FLOOR_DB = -60f;

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "generate", "gen" -> {
                    if (args.length < 4) {
                        System.err.println("Error: generate requires dry.wav, wet.wav, and output.wav");
                        printUsage();
                        return;
                    }
                    String dryPath = args[1];
                    String wetPath = args[2];
                    String outputPath = args[3];
                    int irLength = args.length > 4 ? Integer.parseInt(args[4]) : 2048;
                    boolean minimumPhase = hasFlag(args, "--minimum-phase", "-m");
                    boolean autoAlign = !hasFlag(args, "--no-align");
                    float regularization = getFloatArg(args, "--reg", DEFAULT_REGULARIZATION);

                    generateIR(dryPath, wetPath, outputPath, irLength, minimumPhase, autoAlign, regularization);
                }

                case "sweep" -> {
                    String outputPath = args.length > 1 ? args[1] : "sweep.wav";
                    float duration = args.length > 2 ? Float.parseFloat(args[2]) : 3.0f;
                    int sampleRate = args.length > 3 ? Integer.parseInt(args[3]) : 44100;

                    generateSweepFile(outputPath, duration, sampleRate);
                }

                case "inverse" -> {
                    if (args.length < 2) {
                        System.err.println("Error: inverse requires sweep.wav");
                        return;
                    }
                    String sweepPath = args[1];
                    String outputPath = args.length > 2 ? args[2] : "inverse_sweep.wav";

                    generateInverseSweep(sweepPath, outputPath);
                }

                case "analyze" -> {
                    if (args.length < 2) {
                        System.err.println("Error: analyze requires ir.wav");
                        return;
                    }
                    analyzeIR(args[1]);
                }

                case "help", "-h", "--help" -> printUsage();

                default -> {
                    // Legacy mode: assume it's dry.wav wet.wav output.wav
                    if (args.length >= 3) {
                        int irLength = args.length > 3 ? Integer.parseInt(args[3]) : 2048;
                        generateIR(args[0], args[1], args[2], irLength, false, true, DEFAULT_REGULARIZATION);
                    } else {
                        printUsage();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("IR Generator - High-quality impulse response creation tool");
        System.out.println();
        System.out.println("Commands:");
        System.out.println();
        System.out.println("  generate <dry.wav> <wet.wav> <output.wav> [irLength] [options]");
        System.out.println("      Generate IR from dry/wet audio pair");
        System.out.println("      irLength: 1024, 2048, or 4096 (default: 2048)");
        System.out.println("      Options:");
        System.out.println("        --minimum-phase, -m    Convert to minimum phase IR");
        System.out.println("        --no-align             Disable automatic time alignment");
        System.out.println("        --reg <value>          Regularization factor (default: 0.001)");
        System.out.println();
        System.out.println("  sweep <output.wav> [duration] [sampleRate]");
        System.out.println("      Generate logarithmic sine sweep for IR capture");
        System.out.println("      duration: in seconds (default: 3.0)");
        System.out.println("      sampleRate: in Hz (default: 44100)");
        System.out.println();
        System.out.println("  inverse <sweep.wav> [output.wav]");
        System.out.println("      Generate inverse filter for a sweep (for linear deconvolution)");
        System.out.println();
        System.out.println("  analyze <ir.wav>");
        System.out.println("      Analyze an IR file and show quality metrics");
        System.out.println();
        System.out.println("Workflow for best results:");
        System.out.println("  1. Generate sweep:    IRGenerator sweep test_sweep.wav 3");
        System.out.println("  2. Play sweep through your amp/cab and record the output");
        System.out.println("  3. Generate IR:       IRGenerator generate test_sweep.wav recorded.wav my_ir.wav 2048");
        System.out.println("  4. Analyze quality:   IRGenerator analyze my_ir.wav");
    }

    private static boolean hasFlag(String[] args, String... flags) {
        for (String arg : args) {
            for (String flag : flags) {
                if (arg.equalsIgnoreCase(flag)) return true;
            }
        }
        return false;
    }

    private static float getFloatArg(String[] args, String flag, float defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) {
                return Float.parseFloat(args[i + 1]);
            }
        }
        return defaultValue;
    }

    // ==================== MAIN IR GENERATION ====================

    private static void generateIR(String dryPath, String wetPath, String outputPath,
                                   int irLength, boolean minimumPhase, boolean autoAlign,
                                   float regularization) throws Exception {
        System.out.println("IR Generator - High Quality Mode");
        System.out.println("=================================");
        System.out.println("Dry file:       " + dryPath);
        System.out.println("Wet file:       " + wetPath);
        System.out.println("Output:         " + outputPath);
        System.out.println("IR length:      " + irLength + " samples");
        System.out.println("Minimum phase:  " + minimumPhase);
        System.out.println("Auto-align:     " + autoAlign);
        System.out.println("Regularization: " + regularization);
        System.out.println();

        // Validate IR length
        if (irLength != 512 && irLength != 1024 && irLength != 2048 && irLength != 4096) {
            throw new IllegalArgumentException("IR length must be 512, 1024, 2048, or 4096");
        }

        IRGenerator generator = new IRGenerator();

        // Load files
        System.out.println("Loading audio files...");
        AudioData dry = generator.loadWav(dryPath);
        AudioData wet = generator.loadWav(wetPath);

        System.out.println("  Dry: " + dry.samples.length + " samples @ " + dry.sampleRate + " Hz");
        System.out.println("  Wet: " + wet.samples.length + " samples @ " + wet.sampleRate + " Hz");

        if (dry.sampleRate != wet.sampleRate) {
            throw new IllegalArgumentException("Sample rates must match! Dry: " + dry.sampleRate + ", Wet: " + wet.sampleRate);
        }

        float[] drySamples = dry.samples;
        float[] wetSamples = wet.samples;

        // Auto-align if enabled
        int alignOffset = 0;
        if (autoAlign) {
            System.out.println("\nPerforming automatic time alignment...");
            alignOffset = generator.findAlignment(drySamples, wetSamples);
            System.out.println("  Alignment offset: " + alignOffset + " samples (" +
                             String.format("%.2f", alignOffset * 1000.0 / dry.sampleRate) + " ms)");

            if (alignOffset > 0) {
                wetSamples = generator.applyOffset(wetSamples, alignOffset);
            } else if (alignOffset < 0) {
                drySamples = generator.applyOffset(drySamples, -alignOffset);
            }
        }

        // Use shorter length
        int length = Math.min(drySamples.length, wetSamples.length);

        // Check if input is a sweep (for optimized deconvolution)
        boolean isSweep = generator.detectSweep(drySamples, dry.sampleRate);
        System.out.println("\nSignal type: " + (isSweep ? "Logarithmic sweep detected" : "Generic signal"));

        // Generate IR
        System.out.println("\nGenerating IR...");
        float[] ir;

        if (isSweep) {
            // Use optimized sweep deconvolution
            ir = generator.sweepDeconvolution(drySamples, wetSamples, length, irLength, dry.sampleRate);
        } else {
            // Use Wiener deconvolution
            ir = generator.wienerDeconvolution(drySamples, wetSamples, length, irLength, regularization);
        }

        // Apply noise gate to remove pre-response noise
        System.out.println("Applying noise gate...");
        generator.applyNoiseGate(ir);

        // Convert to minimum phase if requested
        if (minimumPhase) {
            System.out.println("Converting to minimum phase...");
            ir = generator.toMinimumPhase(ir);
        }

        // Normalize
        generator.normalize(ir);

        // Apply smooth fade-out
        generator.applyFadeOut(ir, irLength / 4);

        // Save
        generator.saveWav(ir, outputPath, dry.sampleRate);

        // Quality analysis
        System.out.println("\n" + "=".repeat(40));
        System.out.println("IR Quality Analysis");
        System.out.println("=".repeat(40));
        generator.printQualityMetrics(ir, dry.sampleRate);

        System.out.println("\nIR saved to: " + outputPath);
    }

    // ==================== SWEEP GENERATION ====================

    private static void generateSweepFile(String outputPath, float duration, int sampleRate) throws Exception {
        System.out.println("Generating Logarithmic Sine Sweep");
        System.out.println("=================================");
        System.out.println("Output:      " + outputPath);
        System.out.println("Duration:    " + duration + " seconds");
        System.out.println("Sample rate: " + sampleRate + " Hz");
        System.out.println("Frequency:   20 Hz - 20000 Hz");
        System.out.println();

        IRGenerator generator = new IRGenerator();
        float[] sweep = generator.generateLogSweep(20f, 20000f, duration, sampleRate);

        // Add silence before and after
        int silenceSamples = sampleRate / 2;  // 0.5 second silence
        float[] withSilence = new float[silenceSamples + sweep.length + silenceSamples];
        System.arraycopy(sweep, 0, withSilence, silenceSamples, sweep.length);

        generator.saveWav(withSilence, outputPath, sampleRate);

        System.out.println("Sweep generated: " + outputPath);
        System.out.println("Total duration:  " + String.format("%.2f", withSilence.length / (float)sampleRate) + " seconds");
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Play this sweep through your amp/cabinet/effect");
        System.out.println("  2. Record the output");
        System.out.println("  3. Run: IRGenerator generate " + outputPath + " <recorded.wav> <output_ir.wav>");
    }

    private static void generateInverseSweep(String sweepPath, String outputPath) throws Exception {
        System.out.println("Generating Inverse Sweep Filter");
        System.out.println("===============================");

        IRGenerator generator = new IRGenerator();
        AudioData sweep = generator.loadWav(sweepPath);

        float[] inverse = generator.createInverseSweepFilter(sweep.samples, sweep.sampleRate);

        generator.saveWav(inverse, outputPath, sweep.sampleRate);

        System.out.println("Inverse filter saved to: " + outputPath);
    }

    // ==================== IR ANALYSIS ====================

    private static void analyzeIR(String irPath) throws Exception {
        System.out.println("IR Analysis");
        System.out.println("===========");

        IRGenerator generator = new IRGenerator();
        AudioData ir = generator.loadWav(irPath);

        System.out.println("File:        " + irPath);
        System.out.println("Length:      " + ir.samples.length + " samples");
        System.out.println("Duration:    " + String.format("%.2f", ir.samples.length * 1000.0 / ir.sampleRate) + " ms");
        System.out.println("Sample rate: " + ir.sampleRate + " Hz");
        System.out.println();

        generator.printQualityMetrics(ir.samples, ir.sampleRate);
    }

    // ==================== DECONVOLUTION METHODS ====================

    /**
     * Wiener deconvolution - robust method for noisy signals.
     * H(f) = conj(X(f)) * Y(f) / (|X(f)|² + λ)
     */
    private float[] wienerDeconvolution(float[] dry, float[] wet, int length, int irLength, float regularization) {
        int fftSize = nextPowerOf2(length + irLength);

        float[] dryReal = new float[fftSize];
        float[] dryImag = new float[fftSize];
        float[] wetReal = new float[fftSize];
        float[] wetImag = new float[fftSize];

        // Copy with zero-padding
        System.arraycopy(dry, 0, dryReal, 0, Math.min(length, fftSize));
        System.arraycopy(wet, 0, wetReal, 0, Math.min(length, fftSize));

        // Apply window to reduce spectral leakage
        applyBlackmanWindow(dryReal, length);
        applyBlackmanWindow(wetReal, length);

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);

        // Forward FFT
        fft(dryReal, dryImag, fftSize, twiddleReal, twiddleImag, false);
        fft(wetReal, wetImag, fftSize, twiddleReal, twiddleImag, false);

        // Estimate noise floor for adaptive regularization
        float noiseFloor = estimateNoiseFloor(dryReal, dryImag, fftSize);
        float adaptiveReg = Math.max(regularization, noiseFloor * noiseFloor);

        // Wiener deconvolution: H = conj(X) * Y / (|X|² + λ)
        float[] irReal = new float[fftSize];
        float[] irImag = new float[fftSize];

        for (int i = 0; i < fftSize; i++) {
            float xr = dryReal[i];
            float xi = dryImag[i];
            float yr = wetReal[i];
            float yi = wetImag[i];

            float powerX = xr * xr + xi * xi;
            float denom = powerX + adaptiveReg;

            // conj(X) * Y = (xr - j*xi) * (yr + j*yi) = (xr*yr + xi*yi) + j(xr*yi - xi*yr)
            irReal[i] = (xr * yr + xi * yi) / denom;
            irImag[i] = (xr * yi - xi * yr) / denom;
        }

        // Inverse FFT
        fft(irReal, irImag, fftSize, twiddleReal, twiddleImag, true);

        // Extract IR
        float[] ir = new float[irLength];
        System.arraycopy(irReal, 0, ir, 0, irLength);

        return ir;
    }

    /**
     * Optimized deconvolution for logarithmic sweeps.
     * Uses the inverse sweep filter for cleaner results.
     */
    private float[] sweepDeconvolution(float[] sweep, float[] response, int length, int irLength, int sampleRate) {
        int fftSize = nextPowerOf2(length * 2);

        // Create inverse sweep filter
        float[] inverseFilter = createInverseSweepFilter(sweep, sampleRate);

        float[] respReal = new float[fftSize];
        float[] respImag = new float[fftSize];
        float[] invReal = new float[fftSize];
        float[] invImag = new float[fftSize];

        System.arraycopy(response, 0, respReal, 0, Math.min(length, fftSize));
        System.arraycopy(inverseFilter, 0, invReal, 0, Math.min(inverseFilter.length, fftSize));

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);

        // Forward FFT
        fft(respReal, respImag, fftSize, twiddleReal, twiddleImag, false);
        fft(invReal, invImag, fftSize, twiddleReal, twiddleImag, false);

        // Complex multiplication
        float[] irReal = new float[fftSize];
        float[] irImag = new float[fftSize];

        for (int i = 0; i < fftSize; i++) {
            irReal[i] = respReal[i] * invReal[i] - respImag[i] * invImag[i];
            irImag[i] = respReal[i] * invImag[i] + respImag[i] * invReal[i];
        }

        // Inverse FFT
        fft(irReal, irImag, fftSize, twiddleReal, twiddleImag, true);

        // Find the peak (should be near the center due to the sweep properties)
        int peakIdx = findPeak(irReal);

        // Extract IR starting from peak
        float[] ir = new float[irLength];
        for (int i = 0; i < irLength; i++) {
            int idx = peakIdx + i;
            if (idx >= 0 && idx < fftSize) {
                ir[i] = irReal[idx];
            }
        }

        return ir;
    }

    /**
     * Create inverse filter for logarithmic sweep.
     * The inverse is the time-reversed sweep with amplitude envelope correction.
     */
    private float[] createInverseSweepFilter(float[] sweep, int sampleRate) {
        int length = sweep.length;
        float[] inverse = new float[length];

        // Time-reverse
        for (int i = 0; i < length; i++) {
            inverse[i] = sweep[length - 1 - i];
        }

        // Apply amplitude correction (6dB/octave for log sweep)
        // The envelope should decrease exponentially for the inverse
        float duration = (float) length / sampleRate;
        float startFreq = 20f;
        float endFreq = 20000f;
        float k = (float) Math.log(endFreq / startFreq) / duration;

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;
            float envelope = (float) Math.exp(-k * t);
            inverse[i] *= envelope;
        }

        // Normalize
        float max = 0;
        for (float v : inverse) max = Math.max(max, Math.abs(v));
        if (max > 0) {
            for (int i = 0; i < length; i++) inverse[i] /= max;
        }

        return inverse;
    }

    // ==================== SIGNAL PROCESSING UTILITIES ====================

    /**
     * Generate logarithmic sine sweep.
     */
    private float[] generateLogSweep(float startFreq, float endFreq, float durationSeconds, int sampleRate) {
        int numSamples = (int)(durationSeconds * sampleRate);
        float[] sweep = new float[numSamples];

        double k = Math.log(endFreq / startFreq) / durationSeconds;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double phase = 2.0 * Math.PI * startFreq * (Math.exp(k * t) - 1) / k;
            sweep[i] = (float) Math.sin(phase);
        }

        // Apply smooth fade in/out (raised cosine)
        int fadeLen = sampleRate / 10;  // 100ms fade
        for (int i = 0; i < fadeLen; i++) {
            float fade = 0.5f * (1 - (float)Math.cos(Math.PI * i / fadeLen));
            sweep[i] *= fade;
            sweep[numSamples - 1 - i] *= fade;
        }

        return sweep;
    }

    /**
     * Detect if signal is a logarithmic sweep.
     */
    private boolean detectSweep(float[] signal, int sampleRate) {
        // Simple heuristic: check if signal has characteristics of a sweep
        // - Consistent amplitude
        // - Increasing frequency over time

        if (signal.length < sampleRate) return false;

        // Check amplitude consistency
        int blockSize = sampleRate / 10;
        float[] blockRms = new float[signal.length / blockSize];

        for (int b = 0; b < blockRms.length; b++) {
            float sum = 0;
            for (int i = 0; i < blockSize; i++) {
                float s = signal[b * blockSize + i];
                sum += s * s;
            }
            blockRms[b] = (float) Math.sqrt(sum / blockSize);
        }

        // Check if RMS is relatively consistent (within 6dB)
        float minRms = Float.MAX_VALUE, maxRms = 0;
        for (float rms : blockRms) {
            if (rms > 0.01f) {  // Ignore silence
                minRms = Math.min(minRms, rms);
                maxRms = Math.max(maxRms, rms);
            }
        }

        return maxRms / minRms < 2.0f;  // Less than 6dB variation
    }

    /**
     * Find alignment between two signals using cross-correlation.
     */
    private int findAlignment(float[] a, float[] b) {
        int maxLag = Math.min(a.length, b.length) / 4;  // Search range
        int bestLag = 0;
        float bestCorr = Float.NEGATIVE_INFINITY;

        // Use a subset for faster computation
        int searchLen = Math.min(a.length, 44100);  // Max 1 second

        for (int lag = -maxLag; lag <= maxLag; lag++) {
            float corr = 0;
            int count = 0;

            for (int i = 0; i < searchLen; i++) {
                int ia = i;
                int ib = i + lag;
                if (ia >= 0 && ia < a.length && ib >= 0 && ib < b.length) {
                    corr += a[ia] * b[ib];
                    count++;
                }
            }

            if (count > 0) {
                corr /= count;
                if (corr > bestCorr) {
                    bestCorr = corr;
                    bestLag = lag;
                }
            }
        }

        return bestLag;
    }

    private float[] applyOffset(float[] signal, int offset) {
        if (offset <= 0) return signal;

        float[] result = new float[signal.length];
        System.arraycopy(signal, offset, result, 0, signal.length - offset);
        return result;
    }

    /**
     * Convert to minimum phase IR using Hilbert transform.
     */
    private float[] toMinimumPhase(float[] ir) {
        int fftSize = nextPowerOf2(ir.length * 2);

        float[] real = new float[fftSize];
        float[] imag = new float[fftSize];
        System.arraycopy(ir, 0, real, 0, ir.length);

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);

        // FFT
        fft(real, imag, fftSize, twiddleReal, twiddleImag, false);

        // Get magnitude and compute log magnitude
        float[] logMag = new float[fftSize];
        for (int i = 0; i < fftSize; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            logMag[i] = (float) Math.log(Math.max(mag, 1e-10));
        }

        // Hilbert transform of log magnitude to get minimum phase
        float[] hilbertReal = new float[fftSize];
        float[] hilbertImag = new float[fftSize];
        System.arraycopy(logMag, 0, hilbertReal, 0, fftSize);

        fft(hilbertReal, hilbertImag, fftSize, twiddleReal, twiddleImag, false);

        // Apply Hilbert transform in frequency domain
        for (int i = 1; i < fftSize / 2; i++) {
            hilbertImag[i] = -hilbertReal[i];
            hilbertReal[i] = hilbertImag[i];
        }
        hilbertReal[0] = hilbertImag[0] = 0;
        hilbertReal[fftSize/2] = hilbertImag[fftSize/2] = 0;

        fft(hilbertReal, hilbertImag, fftSize, twiddleReal, twiddleImag, true);

        // Reconstruct minimum phase signal
        float[] minPhaseReal = new float[fftSize];
        float[] minPhaseImag = new float[fftSize];

        for (int i = 0; i < fftSize; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            float phase = -hilbertImag[i];  // Minimum phase
            minPhaseReal[i] = mag * (float) Math.cos(phase);
            minPhaseImag[i] = mag * (float) Math.sin(phase);
        }

        // IFFT
        fft(minPhaseReal, minPhaseImag, fftSize, twiddleReal, twiddleImag, true);

        float[] result = new float[ir.length];
        System.arraycopy(minPhaseReal, 0, result, 0, ir.length);
        return result;
    }

    /**
     * Apply noise gate to remove pre-response noise.
     */
    private void applyNoiseGate(float[] ir) {
        // Find the main peak
        int peakIdx = 0;
        float peakVal = 0;
        for (int i = 0; i < ir.length; i++) {
            if (Math.abs(ir[i]) > peakVal) {
                peakVal = Math.abs(ir[i]);
                peakIdx = i;
            }
        }

        // Calculate threshold
        float threshold = peakVal * (float) Math.pow(10, NOISE_FLOOR_DB / 20);

        // Zero out samples before the main response starts
        int responseStart = peakIdx;
        for (int i = peakIdx - 1; i >= 0; i--) {
            if (Math.abs(ir[i]) > threshold * 10) {
                responseStart = i;
            } else {
                break;
            }
        }

        // Apply soft gate before response
        for (int i = 0; i < responseStart; i++) {
            ir[i] *= 0.001f;  // Reduce by 60dB
        }
    }

    private void applyBlackmanWindow(float[] signal, int length) {
        for (int i = 0; i < length; i++) {
            double window = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (length - 1))
                          + 0.08 * Math.cos(4 * Math.PI * i / (length - 1));
            signal[i] *= (float) window;
        }
    }

    private float estimateNoiseFloor(float[] real, float[] imag, int fftSize) {
        // Estimate noise from high frequency content
        float sum = 0;
        int count = 0;
        for (int i = fftSize * 3 / 4; i < fftSize; i++) {
            sum += (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            count++;
        }
        return sum / count;
    }

    private int findPeak(float[] signal) {
        int peakIdx = 0;
        float peakVal = 0;
        for (int i = 0; i < signal.length; i++) {
            if (Math.abs(signal[i]) > peakVal) {
                peakVal = Math.abs(signal[i]);
                peakIdx = i;
            }
        }
        return peakIdx;
    }

    private void normalize(float[] ir) {
        float maxAbs = 0;
        for (float sample : ir) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }
        if (maxAbs > 0) {
            float scale = 0.99f / maxAbs;
            for (int i = 0; i < ir.length; i++) {
                ir[i] *= scale;
            }
        }
    }

    private void applyFadeOut(float[] ir, int fadeLength) {
        fadeLength = Math.min(fadeLength, ir.length / 2);
        int startFade = ir.length - fadeLength;
        for (int i = startFade; i < ir.length; i++) {
            float t = (float)(i - startFade) / fadeLength;
            float fade = 0.5f * (1 + (float)Math.cos(Math.PI * t));  // Raised cosine
            ir[i] *= fade;
        }
    }

    // ==================== QUALITY METRICS ====================

    private void printQualityMetrics(float[] ir, int sampleRate) {
        // Peak level
        float peak = 0;
        int peakIdx = 0;
        for (int i = 0; i < ir.length; i++) {
            if (Math.abs(ir[i]) > peak) {
                peak = Math.abs(ir[i]);
                peakIdx = i;
            }
        }
        System.out.println("Peak level:     " + String.format("%.2f", 20 * Math.log10(peak)) + " dB");
        System.out.println("Peak position:  " + peakIdx + " samples (" +
                          String.format("%.2f", peakIdx * 1000.0 / sampleRate) + " ms)");

        // RT60 estimate (time for 60dB decay)
        float rt60Threshold = peak * 0.001f;  // -60dB
        int rt60Sample = ir.length - 1;
        for (int i = peakIdx; i < ir.length; i++) {
            if (Math.abs(ir[i]) < rt60Threshold) {
                rt60Sample = i;
                break;
            }
        }
        float rt60 = (rt60Sample - peakIdx) * 1000.0f / sampleRate;
        System.out.println("RT60 estimate:  " + String.format("%.1f", rt60) + " ms");

        // Energy distribution
        float totalEnergy = 0;
        float earlyEnergy = 0;  // First 10ms
        int earlyWindow = sampleRate / 100;

        for (int i = 0; i < ir.length; i++) {
            float e = ir[i] * ir[i];
            totalEnergy += e;
            if (i < peakIdx + earlyWindow) {
                earlyEnergy += e;
            }
        }

        float earlyRatio = earlyEnergy / totalEnergy * 100;
        System.out.println("Early energy:   " + String.format("%.1f", earlyRatio) + "% (first 10ms)");

        // Frequency response estimate
        int fftSize = nextPowerOf2(ir.length);
        float[] real = new float[fftSize];
        float[] imag = new float[fftSize];
        System.arraycopy(ir, 0, real, 0, ir.length);

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);
        fft(real, imag, fftSize, twiddleReal, twiddleImag, false);

        // Calculate average magnitude in bass, mid, treble
        float bassSum = 0, midSum = 0, trebleSum = 0;
        int bassCount = 0, midCount = 0, trebleCount = 0;

        for (int i = 1; i < fftSize / 2; i++) {
            float freq = (float) i * sampleRate / fftSize;
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            float db = 20 * (float) Math.log10(Math.max(mag, 1e-10f));

            if (freq < 250) {
                bassSum += db;
                bassCount++;
            } else if (freq < 4000) {
                midSum += db;
                midCount++;
            } else if (freq < 16000) {
                trebleSum += db;
                trebleCount++;
            }
        }

        float bassAvg = bassSum / bassCount;
        float midAvg = midSum / midCount;
        float trebleAvg = trebleSum / trebleCount;
        float reference = midAvg;

        System.out.println("\nFrequency balance (relative to mids):");
        System.out.println("  Bass (<250Hz):    " + String.format("%+.1f", bassAvg - reference) + " dB");
        System.out.println("  Mids (250-4kHz):  " + String.format("%+.1f", midAvg - reference) + " dB (reference)");
        System.out.println("  Treble (4-16kHz): " + String.format("%+.1f", trebleAvg - reference) + " dB");
    }

    // ==================== FFT IMPLEMENTATION ====================

    private void fft(float[] real, float[] imag, int n, float[] twiddleReal, float[] twiddleImag, boolean inverse) {
        int halfN = n / 2;

        // Bit-reversal permutation
        for (int i = 0, j = 0; i < n; i++) {
            if (j > i) {
                float tempR = real[i], tempI = imag[i];
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

        if (inverse) {
            float scale = 1.0f / n;
            for (int i = 0; i < n; i++) {
                real[i] *= scale;
                imag[i] *= scale;
            }
        }
    }

    private void computeTwiddleFactors(float[] twiddleReal, float[] twiddleImag, int fftSize) {
        for (int i = 0; i < fftSize / 2; i++) {
            double angle = -2.0 * Math.PI * i / fftSize;
            twiddleReal[i] = (float) Math.cos(angle);
            twiddleImag[i] = (float) Math.sin(angle);
        }
    }

    private int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) power <<= 1;
        return power;
    }

    // ==================== FILE I/O ====================

    private record AudioData(float[] samples, int sampleRate) {}

    private AudioData loadWav(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioStream.getFormat();
        int sampleRate = (int) format.getSampleRate();

        // Convert to mono 16-bit if needed
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                false
        );

        if (!format.matches(targetFormat)) {
            if (AudioSystem.isConversionSupported(targetFormat, format)) {
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            }
        }

        byte[] audioBytes = audioStream.readAllBytes();
        audioStream.close();

        int numSamples = audioBytes.length / 2;
        float[] samples = new float[numSamples];
        ByteBuffer bb = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            samples[i] = bb.getShort() / 32768.0f;
        }

        return new AudioData(samples, sampleRate);
    }

    private void saveWav(float[] samples, String path, int sampleRate) throws Exception {
        byte[] audioBytes = new byte[samples.length * 2];
        ByteBuffer bb = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);

        for (float sample : samples) {
            float clipped = Math.max(-1.0f, Math.min(1.0f, sample));
            bb.putShort((short)(clipped * 32767));
        }

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                false
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream ais = new AudioInputStream(bais, format, samples.length);

        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(path));
        ais.close();
    }
}
