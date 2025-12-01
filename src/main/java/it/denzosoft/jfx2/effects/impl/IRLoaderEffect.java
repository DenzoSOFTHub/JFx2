package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.FFTConvolver;
import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Impulse Response (IR) Loader effect using FFT convolution.
 *
 * <p>Loads a WAV file as an impulse response and applies it to the audio signal
 * using FFT-based convolution. This is commonly used for cabinet simulation,
 * room reverb, or other convolution-based effects.</p>
 *
 * <p>Features:
 * - Loads mono or stereo WAV files (16/24/32 bit)
 * - Automatic sample rate conversion
 * - Adjustable mix (dry/wet balance)
 * - Pre-delay for timing adjustment
 * - Output gain control
 * - Low/High cut filters for tone shaping</p>
 */
public class IRLoaderEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "irloader",
            "IR Loader",
            "Load and apply impulse responses from WAV files",
            EffectCategory.AMP_SIM
    );

    // Maximum IR length in samples (~2 seconds at 48kHz)
    private static final int MAX_IR_SAMPLES = 96000;

    // Parameters
    private final Parameter mixParam;
    private final Parameter gainParam;
    private final Parameter preDelayParam;
    private final Parameter lowCutParam;
    private final Parameter highCutParam;
    private final Parameter trimParam;

    // FFT Convolvers - Left and Right for true stereo
    private FFTConvolver convolverL;
    private FFTConvolver convolverR;

    // IR data
    private float[] irDataL;
    private float[] irDataR;
    private int irLength;
    private int irSampleRate;
    private boolean irStereo;
    private String currentFilePath;
    private boolean irLoaded;

    // Simple one-pole filters for tone shaping
    private float lowCutStateL;
    private float lowCutStateR;
    private float highCutStateL;
    private float highCutStateR;
    private float lowCutCoeff;
    private float highCutCoeff;

    public IRLoaderEffect() {
        super(METADATA);

        // Mix: 0% (dry) to 100% (wet), default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry signal and IR-processed signal. 100% = fully processed.",
                0.0f, 100.0f, 100.0f, "%");

        // Gain: -24 dB to +12 dB, default 0 dB
        gainParam = addFloatParameter("gain", "Gain",
                "Output level adjustment for the IR signal.",
                -24.0f, 12.0f, 0.0f, "dB");

        // Pre-delay: 0 to 50 ms, default 0
        preDelayParam = addFloatParameter("predelay", "Pre-Delay",
                "Delay before the IR starts. Useful for timing adjustment.",
                0.0f, 50.0f, 0.0f, "ms");

        // Low cut: 20 Hz to 500 Hz, default 20 Hz
        lowCutParam = addFloatParameter("lowcut", "Low Cut",
                "Removes low frequencies. Higher values tighten the bass.",
                20.0f, 500.0f, 20.0f, "Hz");

        // High cut: 2 kHz to 20 kHz, default 20 kHz
        highCutParam = addFloatParameter("highcut", "High Cut",
                "Removes high frequencies. Lower values darken the tone.",
                2000.0f, 20000.0f, 20000.0f, "Hz");

        // IR trim: 0% to 100%, default 100%
        trimParam = addFloatParameter("trim", "IR Length",
                "Use only a portion of the IR. Lower values reduce latency and CPU.",
                10.0f, 100.0f, 100.0f, "%");

        // Initialize state
        irLoaded = false;
        currentFilePath = null;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create convolvers
        convolverL = new FFTConvolver();
        convolverR = new FFTConvolver();

        // Reset filter states
        lowCutStateL = 0;
        lowCutStateR = 0;
        highCutStateL = 0;
        highCutStateR = 0;

        updateFilters();

        // If we have IR data, prepare the convolvers
        if (irLoaded && irDataL != null) {
            prepareConvolvers(maxFrameCount);
        }
    }

    /**
     * Prepare convolvers with current IR data.
     */
    private void prepareConvolvers(int blockSize) {
        if (irDataL == null || irLength == 0) {
            return;
        }

        // Apply trim parameter
        float trimPercent = trimParam.getTargetValue() / 100.0f;
        int effectiveLength = Math.max(64, (int) (irLength * trimPercent));

        // Add pre-delay samples
        float preDelayMs = preDelayParam.getTargetValue();
        int preDelaySamples = (int) (preDelayMs * sampleRate / 1000.0f);

        int totalLength = Math.min(effectiveLength + preDelaySamples, MAX_IR_SAMPLES);

        // Create IR with pre-delay
        float[] irWithDelayL = new float[totalLength];
        float[] irWithDelayR = new float[totalLength];

        // Copy IR data after pre-delay
        int copyLen = Math.min(effectiveLength, totalLength - preDelaySamples);
        if (copyLen > 0) {
            System.arraycopy(irDataL, 0, irWithDelayL, preDelaySamples, copyLen);
            if (irStereo && irDataR != null) {
                System.arraycopy(irDataR, 0, irWithDelayR, preDelaySamples, copyLen);
            } else {
                System.arraycopy(irDataL, 0, irWithDelayR, preDelaySamples, copyLen);
            }
        }

        // Prepare convolvers
        convolverL.prepare(irWithDelayL, totalLength, blockSize);
        convolverR.prepare(irWithDelayR, totalLength, blockSize);
    }

    private void updateFilters() {
        float lcFreq = lowCutParam.getValue();
        float hcFreq = highCutParam.getValue();
        int sr = sampleRate > 0 ? sampleRate : 44100;

        lowCutCoeff = (float) (2.0 * Math.PI * lcFreq / sr);
        lowCutCoeff = Math.min(lowCutCoeff, 1.0f);

        highCutCoeff = (float) (2.0 * Math.PI * hcFreq / sr);
        highCutCoeff = Math.min(highCutCoeff, 1.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        if (!irLoaded || convolverL == null) {
            // No IR loaded - pass through
            System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
            return;
        }

        float mix = mixParam.getValue() / 100.0f;
        float gainLinear = dbToLinear(gainParam.getValue());
        updateFilters();

        float[] convolved = new float[frameCount];
        convolverL.process(input, convolved, frameCount);

        float lc = lowCutCoeff;
        float hc = highCutCoeff;

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float wet = convolved[i] * gainLinear;

            // Apply low cut (high-pass)
            lowCutStateL += lc * (wet - lowCutStateL);
            wet = wet - lowCutStateL;

            // Apply high cut (low-pass)
            highCutStateL += hc * (wet - highCutStateL);
            wet = highCutStateL;

            // Mix dry and wet
            output[i] = input[i] * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        if (!irLoaded || convolverL == null || convolverR == null) {
            // No IR loaded - pass through
            int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));
            System.arraycopy(inputL, 0, outputL, 0, len);
            System.arraycopy(inputR, 0, outputR, 0, len);
            return;
        }

        float mix = mixParam.getValue() / 100.0f;
        float gainLinear = dbToLinear(gainParam.getValue());
        updateFilters();

        float[] convolvedL = new float[frameCount];
        float[] convolvedR = new float[frameCount];
        convolverL.process(inputL, convolvedL, frameCount);
        convolverR.process(inputR, convolvedR, frameCount);

        float lc = lowCutCoeff;
        float hc = highCutCoeff;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            float wetL = convolvedL[i] * gainLinear;
            lowCutStateL += lc * (wetL - lowCutStateL);
            wetL = wetL - lowCutStateL;
            highCutStateL += hc * (wetL - highCutStateL);
            wetL = highCutStateL;
            outputL[i] = inputL[i] * (1.0f - mix) + wetL * mix;

            // Right channel
            float wetR = convolvedR[i] * gainLinear;
            lowCutStateR += lc * (wetR - lowCutStateR);
            wetR = wetR - lowCutStateR;
            highCutStateR += hc * (wetR - highCutStateR);
            wetR = highCutStateR;
            outputR[i] = inputR[i] * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (convolverL != null) convolverL.reset();
        if (convolverR != null) convolverR.reset();
        lowCutStateL = 0;
        lowCutStateR = 0;
        highCutStateL = 0;
        highCutStateR = 0;
    }

    /**
     * Load an impulse response from a WAV file.
     *
     * @param filePath Path to the WAV file
     * @return true if loaded successfully
     */
    public boolean loadIR(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            clearIR();
            return false;
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("IR file not found: " + filePath);
                return false;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioStream.getFormat();

            // Convert to PCM if necessary
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED &&
                format.getEncoding() != AudioFormat.Encoding.PCM_FLOAT) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false
                );
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
                format = targetFormat;
            }

            irSampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            irStereo = channels >= 2;

            // Read all audio data
            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            // Convert to float samples
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int totalFrames = audioBytes.length / (bytesPerSample * channels);

            // Limit IR length
            totalFrames = Math.min(totalFrames, MAX_IR_SAMPLES);

            irDataL = new float[totalFrames];
            if (irStereo) {
                irDataR = new float[totalFrames];
            } else {
                irDataR = null;
            }
            irLength = totalFrames;

            // Parse audio data
            for (int i = 0; i < totalFrames; i++) {
                int bytePos = i * bytesPerSample * channels;

                // Read left sample
                float sampleL = readSample(audioBytes, bytePos, bytesPerSample, format);
                irDataL[i] = sampleL;

                // Read right sample if stereo
                if (irStereo) {
                    int rightPos = bytePos + bytesPerSample;
                    float sampleR = readSample(audioBytes, rightPos, bytesPerSample, format);
                    irDataR[i] = sampleR;
                }
            }

            // Resample if necessary
            if (irSampleRate != sampleRate && sampleRate > 0) {
                resampleIR();
            }

            // Normalize IR
            normalizeIR();

            currentFilePath = filePath;
            irLoaded = true;

            // Prepare convolvers if already prepared
            if (convolverL != null && maxFrameCount > 0) {
                prepareConvolvers(maxFrameCount);
            }

            System.out.println("Loaded IR: " + filePath +
                    " (" + irSampleRate + "Hz, " + (irStereo ? "stereo" : "mono") +
                    ", " + String.format("%.2f", irLength / (float) irSampleRate) + "s)");

            return true;

        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error loading IR file: " + e.getMessage());
            clearIR();
            return false;
        }
    }

    /**
     * Read a single sample from byte array.
     */
    private float readSample(byte[] data, int pos, int bytesPerSample, AudioFormat format) {
        if (pos + bytesPerSample > data.length) {
            return 0;
        }

        if (bytesPerSample == 2) {
            // 16-bit little-endian
            int low = data[pos] & 0xFF;
            int high = data[pos + 1];
            short s = (short) ((high << 8) | low);
            return s / 32768.0f;
        } else if (bytesPerSample == 3) {
            // 24-bit little-endian
            int b0 = data[pos] & 0xFF;
            int b1 = data[pos + 1] & 0xFF;
            int b2 = data[pos + 2];
            int value = (b2 << 16) | (b1 << 8) | b0;
            return value / 8388608.0f;
        } else if (bytesPerSample == 4) {
            if (format.getEncoding() == AudioFormat.Encoding.PCM_FLOAT) {
                int bits = (data[pos] & 0xFF) |
                           ((data[pos + 1] & 0xFF) << 8) |
                           ((data[pos + 2] & 0xFF) << 16) |
                           ((data[pos + 3] & 0xFF) << 24);
                return Float.intBitsToFloat(bits);
            } else {
                // 32-bit int
                int value = (data[pos] & 0xFF) |
                            ((data[pos + 1] & 0xFF) << 8) |
                            ((data[pos + 2] & 0xFF) << 16) |
                            ((data[pos + 3]) << 24);
                return value / 2147483648.0f;
            }
        } else if (bytesPerSample == 1) {
            // 8-bit unsigned
            return ((data[pos] & 0xFF) - 128) / 128.0f;
        }

        return 0;
    }

    /**
     * Simple linear resampling of IR to current sample rate.
     */
    private void resampleIR() {
        if (irSampleRate == sampleRate || sampleRate <= 0) {
            return;
        }

        float ratio = (float) irSampleRate / sampleRate;
        int newLength = (int) (irLength / ratio);
        newLength = Math.min(newLength, MAX_IR_SAMPLES);

        float[] newIrL = new float[newLength];
        float[] newIrR = irStereo ? new float[newLength] : null;

        for (int i = 0; i < newLength; i++) {
            float srcPos = i * ratio;
            int srcIdx = (int) srcPos;
            float frac = srcPos - srcIdx;

            if (srcIdx + 1 < irLength) {
                // Linear interpolation
                newIrL[i] = irDataL[srcIdx] * (1 - frac) + irDataL[srcIdx + 1] * frac;
                if (irStereo && irDataR != null) {
                    newIrR[i] = irDataR[srcIdx] * (1 - frac) + irDataR[srcIdx + 1] * frac;
                }
            } else if (srcIdx < irLength) {
                newIrL[i] = irDataL[srcIdx];
                if (irStereo && irDataR != null) {
                    newIrR[i] = irDataR[srcIdx];
                }
            }
        }

        irDataL = newIrL;
        irDataR = newIrR;
        irLength = newLength;
    }

    /**
     * Normalize IR to prevent clipping.
     */
    private void normalizeIR() {
        // Find peak
        float peak = 0;
        for (int i = 0; i < irLength; i++) {
            peak = Math.max(peak, Math.abs(irDataL[i]));
            if (irStereo && irDataR != null) {
                peak = Math.max(peak, Math.abs(irDataR[i]));
            }
        }

        // Normalize to 0.5 (leave headroom)
        if (peak > 0.001f) {
            float scale = 0.5f / peak;
            for (int i = 0; i < irLength; i++) {
                irDataL[i] *= scale;
                if (irStereo && irDataR != null) {
                    irDataR[i] *= scale;
                }
            }
        }
    }

    /**
     * Clear the loaded IR.
     */
    public void clearIR() {
        irDataL = null;
        irDataR = null;
        irLength = 0;
        irLoaded = false;
        currentFilePath = null;

        if (convolverL != null) convolverL.reset();
        if (convolverR != null) convolverR.reset();
    }

    /**
     * Check if an IR is loaded.
     */
    public boolean isIRLoaded() {
        return irLoaded;
    }

    /**
     * Get the currently loaded file path.
     */
    public String getFilePath() {
        return currentFilePath;
    }

    /**
     * Get the IR length in samples.
     */
    public int getIRLength() {
        return irLength;
    }

    /**
     * Get the IR duration in seconds.
     */
    public float getIRDuration() {
        if (irLength == 0 || irSampleRate == 0) return 0;
        return (float) irLength / irSampleRate;
    }

    /**
     * Check if loaded IR is stereo.
     */
    public boolean isIRStereo() {
        return irStereo;
    }

    @Override
    public int getLatency() {
        if (convolverL != null) {
            return convolverL.getLatency();
        }
        return 0;
    }

    // Convenience setters
    public void setMix(float percent) {
        mixParam.setValue(percent);
    }

    public void setGain(float dB) {
        gainParam.setValue(dB);
    }

    public void setPreDelay(float ms) {
        preDelayParam.setValue(ms);
    }

    public void setLowCut(float hz) {
        lowCutParam.setValue(hz);
    }

    public void setHighCut(float hz) {
        highCutParam.setValue(hz);
    }

    public void setTrim(float percent) {
        trimParam.setValue(percent);
    }
}
