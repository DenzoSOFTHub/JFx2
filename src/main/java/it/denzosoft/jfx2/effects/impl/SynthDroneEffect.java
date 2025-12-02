package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Synth Drone effect - generates synthetic oscillators based on detected pitch.
 *
 * <p>Features:
 * <ul>
 *   <li>Pitch detection using YIN algorithm</li>
 *   <li>1-4 synthetic oscillators</li>
 *   <li>Waveform selection: Sine, Saw, Square, Triangle</li>
 *   <li>Interval shifting in semitones</li>
 *   <li>Per-oscillator volume and pan</li>
 *   <li>Dry/wet mix control</li>
 * </ul>
 * </p>
 */
public class SynthDroneEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "synthdrone",
            "Synth Drone",
            "Generates synthetic drone oscillators from detected pitch",
            EffectCategory.FILTER
    );

    private static final int NUM_OSCILLATORS = 4;
    private static final String[] WAVEFORMS = {"Sine", "Saw", "Square", "Triangle"};

    // Global parameters
    private final Parameter dryParam;
    private final Parameter wetParam;
    private final Parameter attackParam;
    private final Parameter releaseParam;
    private final Parameter glideParam;

    // Per-oscillator parameters
    private final Parameter[] oscEnabled;
    private final Parameter[] oscWaveform;
    private final Parameter[] oscInterval;
    private final Parameter[] oscDetune;
    private final Parameter[] oscVolume;
    private final Parameter[] oscPan;

    // Pitch detection (YIN)
    private static final int YIN_BUFFER_SIZE = 2048;
    private float[] yinBuffer;
    private float[] sampleAccumulator;
    private int accumulatorPos;

    // Oscillator state
    private double[] oscPhase;
    private float[] oscCurrentFreq;
    private float[] oscTargetFreq;
    private float[] oscEnvelope;

    // Detected pitch
    private float detectedFreq;
    private float lastValidFreq;
    private boolean pitchValid;

    // Envelope
    private float envelopeLevel;

    public SynthDroneEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        // === ROW 1: Global Mix ===
        dryParam = addFloatParameter("dry", "Dry",
                "Original signal level",
                0.0f, 100.0f, 50.0f, "%");

        wetParam = addFloatParameter("wet", "Wet",
                "Synth signal level",
                0.0f, 100.0f, 80.0f, "%");

        attackParam = addFloatParameter("attack", "Attack",
                "Envelope attack time",
                1.0f, 500.0f, 50.0f, "ms");

        releaseParam = addFloatParameter("release", "Release",
                "Envelope release time",
                10.0f, 2000.0f, 300.0f, "ms");

        glideParam = addFloatParameter("glide", "Glide",
                "Pitch glide time between notes",
                0.0f, 500.0f, 50.0f, "ms");

        // === ROWS 2-5: Per-Oscillator Parameters ===
        oscEnabled = new Parameter[NUM_OSCILLATORS];
        oscWaveform = new Parameter[NUM_OSCILLATORS];
        oscInterval = new Parameter[NUM_OSCILLATORS];
        oscDetune = new Parameter[NUM_OSCILLATORS];
        oscVolume = new Parameter[NUM_OSCILLATORS];
        oscPan = new Parameter[NUM_OSCILLATORS];

        // Default intervals for interesting drone
        int[] defaultIntervals = {0, -12, 7, -5};  // Unison, octave down, fifth up, fourth down
        float[] defaultVolumes = {100, 70, 50, 40};
        float[] defaultPans = {0, 0, -50, 50};
        boolean[] defaultEnabled = {true, true, false, false};

        for (int i = 0; i < NUM_OSCILLATORS; i++) {
            int num = i + 1;
            String prefix = "osc" + num;

            oscEnabled[i] = addBooleanParameter(prefix + "On", "Osc" + num,
                    "Enable oscillator " + num,
                    defaultEnabled[i]);

            oscWaveform[i] = addChoiceParameter(prefix + "Wave", "Wave",
                    "Waveform type",
                    WAVEFORMS, 0);

            oscInterval[i] = addFloatParameter(prefix + "Int", "Interval",
                    "Pitch interval in semitones (-24 to +24)",
                    -24.0f, 24.0f, defaultIntervals[i], "st");

            oscDetune[i] = addFloatParameter(prefix + "Det", "Detune",
                    "Fine detune in cents",
                    -50.0f, 50.0f, 0.0f, "ct");

            oscVolume[i] = addFloatParameter(prefix + "Vol", "Volume",
                    "Oscillator volume",
                    0.0f, 100.0f, defaultVolumes[i], "%");

            oscPan[i] = addFloatParameter(prefix + "Pan", "Pan",
                    "Stereo position (-100=L, +100=R)",
                    -100.0f, 100.0f, defaultPans[i], "");
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // YIN pitch detection buffer
        yinBuffer = new float[YIN_BUFFER_SIZE / 2];
        sampleAccumulator = new float[YIN_BUFFER_SIZE];
        accumulatorPos = 0;

        // Oscillator state
        oscPhase = new double[NUM_OSCILLATORS];
        oscCurrentFreq = new float[NUM_OSCILLATORS];
        oscTargetFreq = new float[NUM_OSCILLATORS];
        oscEnvelope = new float[NUM_OSCILLATORS];

        detectedFreq = 0;
        lastValidFreq = 110.0f; // Default A2
        pitchValid = false;
        envelopeLevel = 0;
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
        float wet = wetParam.getValue() / 100.0f;

        // Envelope parameters
        float attackCoef = (float) Math.exp(-1.0 / (attackParam.getValue() * sampleRate / 1000.0));
        float releaseCoef = (float) Math.exp(-1.0 / (releaseParam.getValue() * sampleRate / 1000.0));

        // Glide coefficient
        float glideTime = glideParam.getValue();
        float glideCoef = glideTime > 0 ?
                (float) Math.exp(-1.0 / (glideTime * sampleRate / 1000.0)) : 0;

        // Accumulate samples for pitch detection
        for (int i = 0; i < frameCount; i++) {
            float monoIn = (inputL[i] + inputR[i]) * 0.5f;
            sampleAccumulator[accumulatorPos++] = monoIn;

            if (accumulatorPos >= YIN_BUFFER_SIZE) {
                // Detect pitch
                detectPitch();
                accumulatorPos = 0;
            }
        }

        // Calculate input RMS for envelope follower
        float rms = 0;
        for (int i = 0; i < frameCount; i++) {
            float s = (inputL[i] + inputR[i]) * 0.5f;
            rms += s * s;
        }
        rms = (float) Math.sqrt(rms / frameCount);

        // Update envelope
        float targetEnv = pitchValid ? Math.min(1.0f, rms * 10) : 0;

        // Pre-calculate oscillator parameters
        boolean[] enabled = new boolean[NUM_OSCILLATORS];
        int[] waveform = new int[NUM_OSCILLATORS];
        float[] volume = new float[NUM_OSCILLATORS];
        float[] panL = new float[NUM_OSCILLATORS];
        float[] panR = new float[NUM_OSCILLATORS];

        for (int osc = 0; osc < NUM_OSCILLATORS; osc++) {
            enabled[osc] = oscEnabled[osc].getBooleanValue();
            waveform[osc] = oscWaveform[osc].getChoiceIndex();
            volume[osc] = oscVolume[osc].getValue() / 100.0f;

            // Calculate target frequency with interval and detune
            float interval = oscInterval[osc].getValue();
            float detune = oscDetune[osc].getValue();
            float freqRatio = (float) Math.pow(2, (interval + detune / 100.0) / 12.0);
            oscTargetFreq[osc] = lastValidFreq * freqRatio;

            // Constant power panning
            float pan = oscPan[osc].getValue() / 100.0f;
            panL[osc] = (float) Math.cos((pan + 1) * Math.PI / 4);
            panR[osc] = (float) Math.sin((pan + 1) * Math.PI / 4);
        }

        // Process samples
        for (int i = 0; i < frameCount; i++) {
            // Update envelope
            if (targetEnv > envelopeLevel) {
                envelopeLevel = attackCoef * envelopeLevel + (1 - attackCoef) * targetEnv;
            } else {
                envelopeLevel = releaseCoef * envelopeLevel + (1 - releaseCoef) * targetEnv;
            }

            float synthL = 0;
            float synthR = 0;

            // Generate oscillators
            for (int osc = 0; osc < NUM_OSCILLATORS; osc++) {
                if (!enabled[osc] || volume[osc] < 0.001f) continue;

                // Glide frequency
                if (glideCoef > 0) {
                    oscCurrentFreq[osc] = glideCoef * oscCurrentFreq[osc] +
                            (1 - glideCoef) * oscTargetFreq[osc];
                } else {
                    oscCurrentFreq[osc] = oscTargetFreq[osc];
                }

                // Update per-oscillator envelope
                oscEnvelope[osc] = 0.99f * oscEnvelope[osc] + 0.01f * envelopeLevel;

                // Generate waveform
                float sample = generateWaveform(waveform[osc], oscPhase[osc]);

                // Apply volume and envelope
                sample *= volume[osc] * oscEnvelope[osc];

                // Apply pan
                synthL += sample * panL[osc];
                synthR += sample * panR[osc];

                // Advance phase
                double phaseInc = oscCurrentFreq[osc] / sampleRate;
                oscPhase[osc] += phaseInc;
                if (oscPhase[osc] >= 1.0) {
                    oscPhase[osc] -= 1.0;
                }
            }

            // Mix dry and wet
            outputL[i] = inputL[i] * dry + synthL * wet;
            outputR[i] = inputR[i] * dry + synthR * wet;
        }
    }

    /**
     * Generate waveform sample.
     * @param type 0=Sine, 1=Saw, 2=Square, 3=Triangle
     * @param phase 0.0 to 1.0
     */
    private float generateWaveform(int type, double phase) {
        switch (type) {
            case 0: // Sine
                return (float) Math.sin(2 * Math.PI * phase);

            case 1: // Saw (with anti-aliasing approximation)
                return (float) (2.0 * phase - 1.0);

            case 2: // Square (with soft edges)
                double sq = phase < 0.5 ? 1.0 : -1.0;
                // Soft edges near transitions
                if (phase < 0.02) sq *= phase / 0.02;
                else if (phase > 0.48 && phase < 0.52) sq *= 1.0 - Math.abs(phase - 0.5) / 0.02;
                else if (phase > 0.98) sq *= (1.0 - phase) / 0.02;
                return (float) sq;

            case 3: // Triangle
                if (phase < 0.25) return (float) (4.0 * phase);
                else if (phase < 0.75) return (float) (2.0 - 4.0 * phase);
                else return (float) (4.0 * phase - 4.0);

            default:
                return 0;
        }
    }

    /**
     * Detect pitch using YIN algorithm.
     */
    private void detectPitch() {
        // Calculate RMS to check if there's signal
        float rms = 0;
        for (int i = 0; i < YIN_BUFFER_SIZE; i++) {
            rms += sampleAccumulator[i] * sampleAccumulator[i];
        }
        rms = (float) Math.sqrt(rms / YIN_BUFFER_SIZE);

        if (rms < 0.01f) {
            // Signal too weak
            pitchValid = false;
            return;
        }

        int halfBufferSize = YIN_BUFFER_SIZE / 2;
        float threshold = 0.15f;

        // Step 1: Calculate difference function
        for (int tau = 0; tau < halfBufferSize; tau++) {
            yinBuffer[tau] = 0;
            for (int i = 0; i < halfBufferSize; i++) {
                float delta = sampleAccumulator[i] - sampleAccumulator[i + tau];
                yinBuffer[tau] += delta * delta;
            }
        }

        // Step 2: Cumulative mean normalized difference
        yinBuffer[0] = 1;
        float runningSum = 0;
        for (int tau = 1; tau < halfBufferSize; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] *= tau / runningSum;
        }

        // Step 3: Absolute threshold
        int tauEstimate = -1;
        for (int tau = 2; tau < halfBufferSize; tau++) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfBufferSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++;
                }
                tauEstimate = tau;
                break;
            }
        }

        if (tauEstimate == -1) {
            pitchValid = false;
            return;
        }

        // Step 4: Parabolic interpolation
        float betterTau;
        if (tauEstimate > 0 && tauEstimate < halfBufferSize - 1) {
            float s0 = yinBuffer[tauEstimate - 1];
            float s1 = yinBuffer[tauEstimate];
            float s2 = yinBuffer[tauEstimate + 1];
            betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        } else {
            betterTau = tauEstimate;
        }

        detectedFreq = sampleRate / betterTau;

        // Validate frequency range (roughly guitar range: 80 Hz to 1200 Hz)
        if (detectedFreq >= 50 && detectedFreq <= 2000) {
            pitchValid = true;
            lastValidFreq = detectedFreq;
        } else {
            pitchValid = false;
        }
    }

    @Override
    protected void onReset() {
        accumulatorPos = 0;
        detectedFreq = 0;
        pitchValid = false;
        envelopeLevel = 0;

        for (int i = 0; i < NUM_OSCILLATORS; i++) {
            oscPhase[i] = 0;
            oscCurrentFreq[i] = lastValidFreq;
            oscEnvelope[i] = 0;
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Global (5): dry, wet, attack, release, glide
        // Row 2: Osc1 (6): on, wave, interval, detune, volume, pan
        // Row 3: Osc2 (6)
        // Row 4: Osc3 (6)
        // Row 5: Osc4 (6)
        return new int[] {5, 6, 6, 6, 6};
    }
}
