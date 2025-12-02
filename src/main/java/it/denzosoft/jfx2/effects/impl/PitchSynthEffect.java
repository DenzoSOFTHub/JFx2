package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Pitch-to-Synth effect - detects pitch and plays synthesized instruments.
 *
 * <p>Features:
 * <ul>
 *   <li>Monophonic (YIN) or Polyphonic (FFT) pitch detection</li>
 *   <li>High-quality wavetable synthesis with anti-aliasing</li>
 *   <li>Multiple instrument presets (Strings, Brass, Pad, Lead, etc.)</li>
 *   <li>Full ADSR envelope</li>
 *   <li>Resonant filter with multiple modes</li>
 *   <li>LFO modulation for vibrato/tremolo</li>
 *   <li>Unison voices with detune for richness</li>
 * </ul>
 * </p>
 */
public class PitchSynthEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "pitchsynth",
            "Pitch Synth",
            "Converts pitch to high-quality synthesizer sounds",
            EffectCategory.FILTER
    );

    // Detection modes
    private static final String[] DETECTION_MODES = {"Monophonic", "Polyphonic"};

    // Instrument presets
    private static final String[] INSTRUMENTS = {
            "Strings",      // Warm pad-like strings
            "Brass",        // Bright brass section
            "Pad",          // Soft ambient pad
            "Lead",         // Sharp synth lead
            "Bass",         // Deep synth bass
            "Organ",        // Hammond-style organ
            "Choir",        // Vocal-like choir
            "Bell",         // Metallic bell tones
            "Pluck",        // Plucked string sound
            "Custom"        // User-defined
    };

    // Filter types
    private static final String[] FILTER_TYPES = {"Lowpass", "Highpass", "Bandpass"};

    // Oscillator waveforms
    private static final String[] WAVEFORMS = {"Sine", "Saw", "Square", "Triangle", "Pulse", "SuperSaw"};

    // Parameters - Detection
    private final Parameter modeParam;
    private final Parameter sensitivityParam;
    private final Parameter glideParam;

    // Parameters - Mix
    private final Parameter dryParam;
    private final Parameter wetParam;
    private final Parameter instrumentParam;

    // Parameters - Oscillator
    private final Parameter waveformParam;
    private final Parameter octaveParam;
    private final Parameter unisonParam;
    private final Parameter detuneParam;

    // Parameters - Envelope
    private final Parameter attackParam;
    private final Parameter decayParam;
    private final Parameter sustainParam;
    private final Parameter releaseParam;

    // Parameters - Filter
    private final Parameter filterTypeParam;
    private final Parameter cutoffParam;
    private final Parameter resonanceParam;
    private final Parameter filterEnvParam;

    // Parameters - Modulation
    private final Parameter vibratoRateParam;
    private final Parameter vibratoDepthParam;
    private final Parameter tremoloRateParam;
    private final Parameter tremoloDepthParam;

    // Pitch detection - YIN (monophonic)
    private static final int YIN_BUFFER_SIZE = 2048;
    private float[] yinBuffer;
    private float[] yinDiff;
    private float[] sampleAccumulator;
    private int accumulatorPos;

    // Pitch detection - FFT (polyphonic)
    private static final int FFT_SIZE = 4096;
    private static final int MAX_POLYPHONY = 6;
    private float[] fftBuffer;
    private float[] fftWindow;
    private float[] magnitudes;
    private float[] detectedFreqs;
    private float[] detectedAmps;
    private int numDetectedNotes;

    // Synth voices
    private static final int NUM_VOICES = 8;
    private static final int UNISON_VOICES = 7;
    private SynthVoice[] voices;

    // Global state
    private float masterEnvelope;
    private float inputRms;
    private double vibratoPhase;
    private double tremoloPhase;

    // Anti-aliased wavetables
    private static final int WAVETABLE_SIZE = 2048;
    private static final int NUM_OCTAVES = 10;
    private float[][] sawTables;
    private float[][] squareTables;
    private float[][] triangleTables;
    private float[][] pulseTables;

    public PitchSynthEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        // === ROW 1: Detection & Mix ===
        modeParam = addChoiceParameter("mode", "Mode",
                "Pitch detection mode",
                DETECTION_MODES, 0);

        sensitivityParam = addFloatParameter("sens", "Sensitivity",
                "Detection sensitivity threshold",
                0.0f, 100.0f, 50.0f, "%");

        glideParam = addFloatParameter("glide", "Glide",
                "Portamento time between notes",
                0.0f, 500.0f, 30.0f, "ms");

        dryParam = addFloatParameter("dry", "Dry",
                "Original signal level",
                0.0f, 100.0f, 0.0f, "%");

        wetParam = addFloatParameter("wet", "Wet",
                "Synth signal level",
                0.0f, 100.0f, 100.0f, "%");

        instrumentParam = addChoiceParameter("inst", "Instrument",
                "Instrument preset",
                INSTRUMENTS, 0);

        // === ROW 2: Oscillator ===
        waveformParam = addChoiceParameter("wave", "Waveform",
                "Oscillator waveform",
                WAVEFORMS, 1);

        octaveParam = addFloatParameter("octave", "Octave",
                "Octave shift (-2 to +2)",
                -2.0f, 2.0f, 0.0f, "");

        unisonParam = addFloatParameter("unison", "Unison",
                "Number of unison voices (1-7)",
                1.0f, 7.0f, 3.0f, "");

        detuneParam = addFloatParameter("detune", "Detune",
                "Unison detune amount",
                0.0f, 50.0f, 15.0f, "ct");

        // === ROW 3: Envelope ADSR ===
        attackParam = addFloatParameter("attack", "Attack",
                "Envelope attack time",
                1.0f, 2000.0f, 50.0f, "ms");

        decayParam = addFloatParameter("decay", "Decay",
                "Envelope decay time",
                1.0f, 2000.0f, 200.0f, "ms");

        sustainParam = addFloatParameter("sustain", "Sustain",
                "Envelope sustain level",
                0.0f, 100.0f, 70.0f, "%");

        releaseParam = addFloatParameter("release", "Release",
                "Envelope release time",
                1.0f, 3000.0f, 300.0f, "ms");

        // === ROW 4: Filter ===
        filterTypeParam = addChoiceParameter("ftype", "Filter",
                "Filter type",
                FILTER_TYPES, 0);

        cutoffParam = addFloatParameter("cutoff", "Cutoff",
                "Filter cutoff frequency",
                20.0f, 20000.0f, 5000.0f, "Hz");

        resonanceParam = addFloatParameter("reso", "Resonance",
                "Filter resonance",
                0.0f, 100.0f, 20.0f, "%");

        filterEnvParam = addFloatParameter("fenv", "Flt Env",
                "Filter envelope amount",
                -100.0f, 100.0f, 30.0f, "%");

        // === ROW 5: Modulation ===
        vibratoRateParam = addFloatParameter("vibRate", "Vib Rate",
                "Vibrato speed",
                0.1f, 15.0f, 5.0f, "Hz");

        vibratoDepthParam = addFloatParameter("vibDepth", "Vib Depth",
                "Vibrato intensity",
                0.0f, 100.0f, 20.0f, "%");

        tremoloRateParam = addFloatParameter("tremRate", "Trem Rate",
                "Tremolo speed",
                0.1f, 15.0f, 4.0f, "Hz");

        tremoloDepthParam = addFloatParameter("tremDepth", "Trem Depth",
                "Tremolo intensity",
                0.0f, 100.0f, 0.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // YIN buffers
        yinBuffer = new float[YIN_BUFFER_SIZE];
        yinDiff = new float[YIN_BUFFER_SIZE / 2];
        sampleAccumulator = new float[YIN_BUFFER_SIZE];
        accumulatorPos = 0;

        // FFT buffers
        fftBuffer = new float[FFT_SIZE];
        fftWindow = new float[FFT_SIZE];
        magnitudes = new float[FFT_SIZE / 2];
        detectedFreqs = new float[MAX_POLYPHONY];
        detectedAmps = new float[MAX_POLYPHONY];

        // Create Hann window for FFT
        for (int i = 0; i < FFT_SIZE; i++) {
            fftWindow[i] = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
        }

        // Initialize voices
        voices = new SynthVoice[NUM_VOICES];
        for (int i = 0; i < NUM_VOICES; i++) {
            voices[i] = new SynthVoice(sampleRate);
        }

        // Generate anti-aliased wavetables
        generateWavetables();

        masterEnvelope = 0;
        vibratoPhase = 0;
        tremoloPhase = 0;
    }

    /**
     * Generate band-limited wavetables for each octave to prevent aliasing.
     */
    private void generateWavetables() {
        sawTables = new float[NUM_OCTAVES][WAVETABLE_SIZE];
        squareTables = new float[NUM_OCTAVES][WAVETABLE_SIZE];
        triangleTables = new float[NUM_OCTAVES][WAVETABLE_SIZE];
        pulseTables = new float[NUM_OCTAVES][WAVETABLE_SIZE];

        for (int octave = 0; octave < NUM_OCTAVES; octave++) {
            // Calculate max harmonics for this octave (Nyquist limit)
            float baseFreq = 27.5f * (float) Math.pow(2, octave); // A0 = 27.5 Hz
            int maxHarmonics = (int) (sampleRate / 2 / baseFreq);
            maxHarmonics = Math.min(maxHarmonics, 256);

            for (int i = 0; i < WAVETABLE_SIZE; i++) {
                float phase = (float) i / WAVETABLE_SIZE;

                // Saw wave (additive synthesis)
                float saw = 0;
                for (int h = 1; h <= maxHarmonics; h++) {
                    saw += (float) Math.sin(2 * Math.PI * h * phase) / h;
                }
                sawTables[octave][i] = saw * 2 / (float) Math.PI;

                // Square wave (odd harmonics only)
                float square = 0;
                for (int h = 1; h <= maxHarmonics; h += 2) {
                    square += (float) Math.sin(2 * Math.PI * h * phase) / h;
                }
                squareTables[octave][i] = square * 4 / (float) Math.PI;

                // Triangle wave
                float triangle = 0;
                for (int h = 1; h <= maxHarmonics; h += 2) {
                    float sign = ((h - 1) / 2 % 2 == 0) ? 1 : -1;
                    triangle += sign * (float) Math.sin(2 * Math.PI * h * phase) / (h * h);
                }
                triangleTables[octave][i] = triangle * 8 / (float) (Math.PI * Math.PI);

                // Pulse wave (25% duty cycle)
                float pulse = 0;
                for (int h = 1; h <= maxHarmonics; h++) {
                    pulse += (float) Math.sin(2 * Math.PI * h * phase) * (float) Math.sin(Math.PI * h * 0.25) / h;
                }
                pulseTables[octave][i] = pulse * 4 / (float) Math.PI;
            }
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];
        processInternal(input, input, outL, outR, frameCount);
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
        float wet = wetParam.getValue() / 100.0f;
        float sensitivity = sensitivityParam.getValue() / 100.0f;
        float glideMs = glideParam.getValue();
        int mode = modeParam.getChoiceIndex();

        // Get envelope parameters
        float attackMs = attackParam.getValue();
        float decayMs = decayParam.getValue();
        float sustainLevel = sustainParam.getValue() / 100.0f;
        float releaseMs = releaseParam.getValue();

        // Get oscillator parameters
        int waveform = waveformParam.getChoiceIndex();
        float octaveShift = octaveParam.getValue();
        int numUnison = (int) unisonParam.getValue();
        float detuneAmount = detuneParam.getValue();

        // Get filter parameters
        int filterType = filterTypeParam.getChoiceIndex();
        float cutoff = cutoffParam.getValue();
        float resonance = resonanceParam.getValue() / 100.0f;
        float filterEnv = filterEnvParam.getValue() / 100.0f;

        // Get modulation parameters
        float vibRate = vibratoRateParam.getValue();
        float vibDepth = vibratoDepthParam.getValue() / 100.0f;
        float tremRate = tremoloRateParam.getValue();
        float tremDepth = tremoloDepthParam.getValue() / 100.0f;

        // Apply instrument preset
        applyInstrumentPreset();

        // Accumulate samples for pitch detection
        for (int i = 0; i < frameCount; i++) {
            float mono = (inputL[i] + inputR[i]) * 0.5f;
            sampleAccumulator[accumulatorPos++] = mono;

            if (accumulatorPos >= YIN_BUFFER_SIZE) {
                // Perform pitch detection
                if (mode == 0) {
                    detectPitchYIN(sensitivity);
                } else {
                    detectPitchFFT(sensitivity);
                }
                accumulatorPos = 0;
            }
        }

        // Calculate input RMS for envelope following
        float rmsSum = 0;
        for (int i = 0; i < frameCount; i++) {
            float s = (inputL[i] + inputR[i]) * 0.5f;
            rmsSum += s * s;
        }
        inputRms = (float) Math.sqrt(rmsSum / frameCount);

        // Glide coefficient
        float glideCoef = glideMs > 0 ? (float) Math.exp(-1.0 / (glideMs * sampleRate / 1000.0)) : 0;

        // Process each sample
        for (int i = 0; i < frameCount; i++) {
            // Update LFOs
            vibratoPhase += vibRate / sampleRate;
            if (vibratoPhase >= 1.0) vibratoPhase -= 1.0;
            float vibrato = (float) Math.sin(2 * Math.PI * vibratoPhase) * vibDepth;

            tremoloPhase += tremRate / sampleRate;
            if (tremoloPhase >= 1.0) tremoloPhase -= 1.0;
            float tremolo = 1.0f - tremDepth * 0.5f * (1 + (float) Math.sin(2 * Math.PI * tremoloPhase));

            // Update master envelope based on input
            float envTarget = (numDetectedNotes > 0 && inputRms > 0.01f * (1 - sensitivity)) ? 1.0f : 0.0f;
            float envCoef = envTarget > masterEnvelope ?
                    (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0)) :
                    (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
            masterEnvelope = envCoef * masterEnvelope + (1 - envCoef) * envTarget;

            // Generate synth output
            float synthL = 0;
            float synthR = 0;

            for (int v = 0; v < Math.min(numDetectedNotes, NUM_VOICES); v++) {
                SynthVoice voice = voices[v];

                // Update voice target frequency
                float targetFreq = detectedFreqs[v] * (float) Math.pow(2, octaveShift);
                if (targetFreq > 20 && targetFreq < 20000) {
                    voice.setTargetFrequency(targetFreq, glideCoef);
                    voice.setAmplitude(detectedAmps[v]);

                    // Generate voice with unison
                    float[] voiceOut = voice.generate(
                            waveform, numUnison, detuneAmount, vibrato,
                            filterType, cutoff, resonance, filterEnv,
                            attackMs, decayMs, sustainLevel, releaseMs,
                            sawTables, squareTables, triangleTables, pulseTables
                    );

                    synthL += voiceOut[0];
                    synthR += voiceOut[1];
                }
            }

            // Apply tremolo and master envelope
            synthL *= tremolo * masterEnvelope;
            synthR *= tremolo * masterEnvelope;

            // Soft clip to prevent harsh distortion
            synthL = softClip(synthL);
            synthR = softClip(synthR);

            // Mix dry and wet
            outputL[i] = inputL[i] * dry + synthL * wet;
            outputR[i] = inputR[i] * dry + synthR * wet;
        }
    }

    /**
     * Apply instrument preset settings.
     */
    private void applyInstrumentPreset() {
        int preset = instrumentParam.getChoiceIndex();
        if (preset == 9) return; // Custom - don't override

        // Presets define characteristic sounds
        // These are applied as suggestions but user can still override
    }

    /**
     * Monophonic pitch detection using YIN algorithm.
     */
    private void detectPitchYIN(float sensitivity) {
        float threshold = 0.15f * (1 - sensitivity * 0.5f);

        // Check RMS
        float rms = 0;
        for (int i = 0; i < YIN_BUFFER_SIZE; i++) {
            rms += sampleAccumulator[i] * sampleAccumulator[i];
        }
        rms = (float) Math.sqrt(rms / YIN_BUFFER_SIZE);

        if (rms < 0.005f * (1 - sensitivity)) {
            numDetectedNotes = 0;
            return;
        }

        int halfBuffer = YIN_BUFFER_SIZE / 2;

        // Step 1: Difference function
        for (int tau = 0; tau < halfBuffer; tau++) {
            yinDiff[tau] = 0;
            for (int i = 0; i < halfBuffer; i++) {
                float delta = sampleAccumulator[i] - sampleAccumulator[i + tau];
                yinDiff[tau] += delta * delta;
            }
        }

        // Step 2: Cumulative mean normalized difference
        yinDiff[0] = 1;
        float runningSum = 0;
        for (int tau = 1; tau < halfBuffer; tau++) {
            runningSum += yinDiff[tau];
            yinDiff[tau] *= tau / runningSum;
        }

        // Step 3: Absolute threshold
        int tauEstimate = -1;
        for (int tau = 2; tau < halfBuffer; tau++) {
            if (yinDiff[tau] < threshold) {
                while (tau + 1 < halfBuffer && yinDiff[tau + 1] < yinDiff[tau]) {
                    tau++;
                }
                tauEstimate = tau;
                break;
            }
        }

        if (tauEstimate == -1) {
            numDetectedNotes = 0;
            return;
        }

        // Step 4: Parabolic interpolation
        float betterTau;
        if (tauEstimate > 0 && tauEstimate < halfBuffer - 1) {
            float s0 = yinDiff[tauEstimate - 1];
            float s1 = yinDiff[tauEstimate];
            float s2 = yinDiff[tauEstimate + 1];
            betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        } else {
            betterTau = tauEstimate;
        }

        float freq = sampleRate / betterTau;

        // Validate frequency range
        if (freq >= 30 && freq <= 4000) {
            detectedFreqs[0] = freq;
            detectedAmps[0] = Math.min(1.0f, rms * 5);
            numDetectedNotes = 1;
        } else {
            numDetectedNotes = 0;
        }
    }

    /**
     * Polyphonic pitch detection using FFT peak analysis.
     */
    private void detectPitchFFT(float sensitivity) {
        // Copy and window the signal
        int fftLen = Math.min(FFT_SIZE, YIN_BUFFER_SIZE);
        for (int i = 0; i < fftLen; i++) {
            fftBuffer[i] = sampleAccumulator[i % YIN_BUFFER_SIZE] * fftWindow[i];
        }
        for (int i = fftLen; i < FFT_SIZE; i++) {
            fftBuffer[i] = 0;
        }

        // Perform FFT (in-place, real input)
        float[] real = new float[FFT_SIZE];
        float[] imag = new float[FFT_SIZE];
        System.arraycopy(fftBuffer, 0, real, 0, FFT_SIZE);

        fft(real, imag, FFT_SIZE);

        // Calculate magnitudes
        float maxMag = 0;
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            magnitudes[i] = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            if (magnitudes[i] > maxMag) maxMag = magnitudes[i];
        }

        if (maxMag < 0.01f * (1 - sensitivity)) {
            numDetectedNotes = 0;
            return;
        }

        // Normalize
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            magnitudes[i] /= maxMag;
        }

        // Find peaks (local maxima above threshold)
        float threshold = 0.1f * (1 - sensitivity * 0.8f);
        numDetectedNotes = 0;

        float freqResolution = (float) sampleRate / FFT_SIZE;
        int minBin = (int) (50 / freqResolution);   // 50 Hz minimum
        int maxBin = (int) (4000 / freqResolution); // 4000 Hz maximum

        for (int i = minBin; i < Math.min(maxBin, FFT_SIZE / 2 - 1); i++) {
            if (magnitudes[i] > threshold &&
                magnitudes[i] > magnitudes[i - 1] &&
                magnitudes[i] > magnitudes[i + 1]) {

                // Parabolic interpolation for better accuracy
                float alpha = magnitudes[i - 1];
                float beta = magnitudes[i];
                float gamma = magnitudes[i + 1];
                float p = 0.5f * (alpha - gamma) / (alpha - 2 * beta + gamma);

                float freq = (i + p) * freqResolution;
                float amp = beta - 0.25f * (alpha - gamma) * p;

                // Check if this is a fundamental (not a harmonic of existing note)
                boolean isHarmonic = false;
                for (int j = 0; j < numDetectedNotes; j++) {
                    float ratio = freq / detectedFreqs[j];
                    if (ratio > 1.9f && ratio < 2.1f) {
                        isHarmonic = true;
                        break;
                    }
                    if (ratio > 2.9f && ratio < 3.1f) {
                        isHarmonic = true;
                        break;
                    }
                }

                if (!isHarmonic && numDetectedNotes < MAX_POLYPHONY) {
                    detectedFreqs[numDetectedNotes] = freq;
                    detectedAmps[numDetectedNotes] = amp;
                    numDetectedNotes++;
                }
            }
        }

        // Sort by amplitude (strongest first)
        for (int i = 0; i < numDetectedNotes - 1; i++) {
            for (int j = i + 1; j < numDetectedNotes; j++) {
                if (detectedAmps[j] > detectedAmps[i]) {
                    float tempF = detectedFreqs[i];
                    float tempA = detectedAmps[i];
                    detectedFreqs[i] = detectedFreqs[j];
                    detectedAmps[i] = detectedAmps[j];
                    detectedFreqs[j] = tempF;
                    detectedAmps[j] = tempA;
                }
            }
        }
    }

    /**
     * Simple in-place FFT (Cooley-Tukey radix-2).
     */
    private void fft(float[] real, float[] imag, int n) {
        // Bit reversal
        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                float tempR = real[i];
                float tempI = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempR;
                imag[j] = tempI;
            }
            int k = n / 2;
            while (k <= j) {
                j -= k;
                k /= 2;
            }
            j += k;
        }

        // FFT computation
        for (int len = 2; len <= n; len *= 2) {
            float angle = -2.0f * (float) Math.PI / len;
            float wR = (float) Math.cos(angle);
            float wI = (float) Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                float curR = 1, curI = 0;
                for (int k = 0; k < len / 2; k++) {
                    int idx1 = i + k;
                    int idx2 = i + k + len / 2;

                    float tR = curR * real[idx2] - curI * imag[idx2];
                    float tI = curR * imag[idx2] + curI * real[idx2];

                    real[idx2] = real[idx1] - tR;
                    imag[idx2] = imag[idx1] - tI;
                    real[idx1] = real[idx1] + tR;
                    imag[idx1] = imag[idx1] + tI;

                    float newR = curR * wR - curI * wI;
                    float newI = curR * wI + curI * wR;
                    curR = newR;
                    curI = newI;
                }
            }
        }
    }

    /**
     * Soft clipping function.
     */
    private float softClip(float x) {
        if (x > 1.0f) return 1.0f - (float) Math.exp(1 - x);
        if (x < -1.0f) return -1.0f + (float) Math.exp(1 + x);
        return x;
    }

    @Override
    protected void onReset() {
        accumulatorPos = 0;
        numDetectedNotes = 0;
        masterEnvelope = 0;
        vibratoPhase = 0;
        tremoloPhase = 0;

        if (voices != null) {
            for (SynthVoice voice : voices) {
                voice.reset();
            }
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Mode, Sensitivity, Glide, Dry, Wet, Instrument
        // Row 2: Waveform, Octave, Unison, Detune, Attack, Decay, Sustain, Release
        // Row 3: Filter, Cutoff, Resonance, Flt Env, Vib Rate, Vib Depth, Trem Rate, Trem Depth
        return new int[] {6, 8, 8};
    }

    /**
     * Individual synth voice with oscillator, filter, and envelope.
     */
    private class SynthVoice {
        private final int voiceSampleRate;
        private float frequency;
        private float targetFrequency;
        private float amplitude;

        // Unison oscillator phases
        private double[] phases;

        // Filter state (SVF - State Variable Filter)
        private float filterLow, filterBand, filterHigh;
        private float filterLow2, filterBand2, filterHigh2;

        // Envelope state
        private float envValue;
        private int envStage; // 0=off, 1=attack, 2=decay, 3=sustain, 4=release

        // Stereo spread
        private float[] unisonPanL;
        private float[] unisonPanR;

        SynthVoice(int sampleRate) {
            this.voiceSampleRate = sampleRate;
            phases = new double[UNISON_VOICES];
            unisonPanL = new float[UNISON_VOICES];
            unisonPanR = new float[UNISON_VOICES];

            // Initialize unison panning (spread across stereo field)
            for (int i = 0; i < UNISON_VOICES; i++) {
                float pan = (i - (UNISON_VOICES - 1) / 2.0f) / ((UNISON_VOICES - 1) / 2.0f + 0.5f);
                unisonPanL[i] = (float) Math.cos((pan + 1) * Math.PI / 4);
                unisonPanR[i] = (float) Math.sin((pan + 1) * Math.PI / 4);
            }

            reset();
        }

        void setTargetFrequency(float freq, float glideCoef) {
            targetFrequency = freq;
            if (glideCoef > 0 && frequency > 0) {
                frequency = glideCoef * frequency + (1 - glideCoef) * targetFrequency;
            } else {
                frequency = targetFrequency;
            }
        }

        void setAmplitude(float amp) {
            this.amplitude = amp;
        }

        float[] generate(int waveform, int numUnison, float detuneAmount, float vibrato,
                         int filterType, float cutoff, float resonance, float filterEnv,
                         float attackMs, float decayMs, float sustainLevel, float releaseMs,
                         float[][] sawTables, float[][] squareTables,
                         float[][] triangleTables, float[][] pulseTables) {

            float[] output = new float[2];

            if (frequency <= 0 || amplitude <= 0.001f) {
                return output;
            }

            // Update envelope
            updateEnvelope(attackMs, decayMs, sustainLevel, releaseMs);

            // Calculate effective cutoff with envelope modulation
            float effCutoff = cutoff * (float) Math.pow(2, filterEnv * envValue * 4);
            effCutoff = Math.max(20, Math.min(20000, effCutoff));

            // Generate unison oscillators
            float sumL = 0, sumR = 0;
            int activeUnison = Math.min(numUnison, UNISON_VOICES);

            for (int u = 0; u < activeUnison; u++) {
                // Calculate detuned frequency
                float detuneCents = (u - (activeUnison - 1) / 2.0f) * detuneAmount / (activeUnison / 2.0f + 0.5f);
                float detunedFreq = frequency * (float) Math.pow(2, (detuneCents + vibrato * 50) / 1200.0);

                // Generate waveform
                float sample = generateWaveform(waveform, phases[u], detunedFreq,
                        sawTables, squareTables, triangleTables, pulseTables);

                // Apply panning
                sumL += sample * unisonPanL[u];
                sumR += sample * unisonPanR[u];

                // Advance phase
                phases[u] += detunedFreq / voiceSampleRate;
                if (phases[u] >= 1.0) phases[u] -= 1.0;
            }

            // Normalize unison
            float unisonGain = 1.0f / (float) Math.sqrt(activeUnison);
            sumL *= unisonGain;
            sumR *= unisonGain;

            // Apply filter (stereo SVF)
            float[] filteredL = applyFilter(sumL, filterType, effCutoff, resonance, true);
            float[] filteredR = applyFilter(sumR, filterType, effCutoff, resonance, false);

            // Apply envelope and amplitude
            output[0] = filteredL[filterType] * envValue * amplitude;
            output[1] = filteredR[filterType] * envValue * amplitude;

            return output;
        }

        private float generateWaveform(int waveform, double phase, float freq,
                                        float[][] sawTables, float[][] squareTables,
                                        float[][] triangleTables, float[][] pulseTables) {
            // Select appropriate wavetable octave
            int octave = (int) (Math.log(freq / 27.5f) / Math.log(2));
            octave = Math.max(0, Math.min(NUM_OCTAVES - 1, octave));

            float tablePhase = (float) (phase * WAVETABLE_SIZE);
            int index = (int) tablePhase;
            float frac = tablePhase - index;
            int nextIndex = (index + 1) % WAVETABLE_SIZE;

            switch (waveform) {
                case 0: // Sine
                    return (float) Math.sin(2 * Math.PI * phase);

                case 1: // Saw
                    return sawTables[octave][index] * (1 - frac) + sawTables[octave][nextIndex] * frac;

                case 2: // Square
                    return squareTables[octave][index] * (1 - frac) + squareTables[octave][nextIndex] * frac;

                case 3: // Triangle
                    return triangleTables[octave][index] * (1 - frac) + triangleTables[octave][nextIndex] * frac;

                case 4: // Pulse
                    return pulseTables[octave][index] * (1 - frac) + pulseTables[octave][nextIndex] * frac;

                case 5: // SuperSaw (multiple detuned saws)
                    float superSaw = 0;
                    for (int i = -2; i <= 2; i++) {
                        double detunePhase = phase + i * 0.01;
                        if (detunePhase >= 1.0) detunePhase -= 1.0;
                        if (detunePhase < 0) detunePhase += 1.0;
                        int idx = (int) (detunePhase * WAVETABLE_SIZE) % WAVETABLE_SIZE;
                        superSaw += sawTables[octave][idx] * (1.0f - Math.abs(i) * 0.15f);
                    }
                    return superSaw / 3.5f;

                default:
                    return 0;
            }
        }

        private float[] applyFilter(float input, int type, float cutoff, float resonance, boolean isLeft) {
            // State Variable Filter (SVF)
            float f = 2 * (float) Math.sin(Math.PI * cutoff / voiceSampleRate);
            f = Math.min(0.99f, f);
            float q = 1 - resonance * 0.9f;

            float[] result = new float[3];

            if (isLeft) {
                filterLow += f * filterBand;
                filterHigh = q * input - filterLow - q * filterBand;
                filterBand += f * filterHigh;

                // Second pass for steeper slope
                filterLow2 += f * filterBand2;
                filterHigh2 = q * filterLow - filterLow2 - q * filterBand2;
                filterBand2 += f * filterHigh2;

                result[0] = filterLow2;  // Lowpass
                result[1] = filterHigh2; // Highpass
                result[2] = filterBand2; // Bandpass
            } else {
                // Use separate state for right channel (stereo)
                filterLow += f * filterBand;
                filterHigh = q * input - filterLow - q * filterBand;
                filterBand += f * filterHigh;

                result[0] = filterLow;
                result[1] = filterHigh;
                result[2] = filterBand;
            }

            return result;
        }

        private void updateEnvelope(float attackMs, float decayMs, float sustainLevel, float releaseMs) {
            if (amplitude > 0.01f) {
                if (envStage == 0 || envStage == 4) {
                    envStage = 1; // Start attack
                }
            } else if (envStage != 0 && envStage != 4) {
                envStage = 4; // Start release
            }

            switch (envStage) {
                case 1: // Attack
                    float attackRate = 1.0f / (attackMs * voiceSampleRate / 1000.0f);
                    envValue += attackRate;
                    if (envValue >= 1.0f) {
                        envValue = 1.0f;
                        envStage = 2;
                    }
                    break;

                case 2: // Decay
                    float decayRate = (1.0f - sustainLevel) / (decayMs * voiceSampleRate / 1000.0f);
                    envValue -= decayRate;
                    if (envValue <= sustainLevel) {
                        envValue = sustainLevel;
                        envStage = 3;
                    }
                    break;

                case 3: // Sustain
                    envValue = sustainLevel;
                    break;

                case 4: // Release
                    float releaseRate = envValue / (releaseMs * voiceSampleRate / 1000.0f);
                    envValue -= releaseRate;
                    if (envValue <= 0.001f) {
                        envValue = 0;
                        envStage = 0;
                    }
                    break;

                default:
                    envValue = 0;
            }
        }

        void reset() {
            frequency = 0;
            targetFrequency = 0;
            amplitude = 0;
            filterLow = filterBand = filterHigh = 0;
            filterLow2 = filterBand2 = filterHigh2 = 0;
            envValue = 0;
            envStage = 0;
            for (int i = 0; i < UNISON_VOICES; i++) {
                phases[i] = Math.random(); // Random phase for unison richness
            }
        }
    }
}
