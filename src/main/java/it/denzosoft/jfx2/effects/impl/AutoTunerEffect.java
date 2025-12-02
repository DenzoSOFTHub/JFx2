package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Auto-Tuner Effect with gentle pitch correction.
 *
 * <p>Provides pitch correction that respects musical expression:
 * <ul>
 *   <li>Smooth correction with adjustable speed</li>
 *   <li>Preserves vibrato and tremolo</li>
 *   <li>Allows bending without cutting off</li>
 *   <li>Multiple scale/key options</li>
 * </ul>
 * </p>
 *
 * <p>The correction is applied gently over time, allowing expressive
 * playing while still centering sustained notes.</p>
 */
public class AutoTunerEffect extends AbstractEffect {

    // Parameters
    private Parameter key;           // Root note (C, C#, D, etc.)
    private Parameter scale;         // Scale type (Chromatic, Major, Minor, etc.)
    private Parameter speed;         // Correction speed (slow = more natural)
    private Parameter sensitivity;   // How much correction to apply
    private Parameter humanize;      // Adds slight natural variation
    private Parameter blend;         // Wet/dry mix

    // Pitch detection
    private float[] inputBuffer;
    private int bufferWritePos = 0;
    private static final int BUFFER_SIZE = 2048;
    private static final int MIN_PERIOD = 20;   // ~2200 Hz max
    private static final int MAX_PERIOD = 800;  // ~55 Hz min

    // Pitch shifting
    private float[] shiftBuffer;
    private static final int SHIFT_BUFFER_SIZE = 4096;
    private int shiftWritePos = 0;
    private float shiftReadPos = 0;

    // State
    private float currentPitch = 0;
    private float targetPitch = 0;
    private float correctedPitch = 0;
    private float smoothedCorrection = 0;
    private int currentSampleRate = 44100;

    // Scale definitions (semitones from root)
    private static final int[] CHROMATIC = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] MINOR = {0, 2, 3, 5, 7, 8, 10};
    private static final int[] HARMONIC_MINOR = {0, 2, 3, 5, 7, 8, 11};
    private static final int[] MELODIC_MINOR = {0, 2, 3, 5, 7, 9, 11};
    private static final int[] PENTATONIC_MAJOR = {0, 2, 4, 7, 9};
    private static final int[] PENTATONIC_MINOR = {0, 3, 5, 7, 10};
    private static final int[] BLUES = {0, 3, 5, 6, 7, 10};
    private static final int[] DORIAN = {0, 2, 3, 5, 7, 9, 10};
    private static final int[] MIXOLYDIAN = {0, 2, 4, 5, 7, 9, 10};

    private static final int[][] SCALES = {
            CHROMATIC, MAJOR, MINOR, HARMONIC_MINOR, MELODIC_MINOR,
            PENTATONIC_MAJOR, PENTATONIC_MINOR, BLUES, DORIAN, MIXOLYDIAN
    };

    // Note names for display
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public AutoTunerEffect() {
        super(EffectMetadata.of("autotuner", "Auto Tuner",
                "Gentle pitch correction preserving expression", EffectCategory.PITCH));
        initParameters();
    }

    private void initParameters() {
        key = addChoiceParameter("key", "Key",
                "Root note of the scale",
                NOTE_NAMES, 0);  // Default: C

        scale = addChoiceParameter("scale", "Scale",
                "Scale type for pitch correction",
                new String[]{"Chromatic", "Major", "Minor", "Harm Minor", "Mel Minor",
                        "Pent Major", "Pent Minor", "Blues", "Dorian", "Mixolydian"}, 1);  // Default: Major

        speed = addFloatParameter("speed", "Speed",
                "Correction speed - lower values preserve more expression (bends, vibrato)",
                0.0f, 100.0f, 30.0f, "%");

        sensitivity = addFloatParameter("sensitivity", "Correction",
                "Amount of pitch correction applied",
                0.0f, 100.0f, 80.0f, "%");

        humanize = addFloatParameter("humanize", "Humanize",
                "Adds subtle natural pitch variation",
                0.0f, 100.0f, 10.0f, "%");

        blend = addFloatParameter("blend", "Blend",
                "Mix between dry and pitch-corrected signal",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.currentSampleRate = sampleRate;
        inputBuffer = new float[BUFFER_SIZE];
        shiftBuffer = new float[SHIFT_BUFFER_SIZE];
        bufferWritePos = 0;
        shiftWritePos = 0;
        shiftReadPos = 0;
        currentPitch = 0;
        targetPitch = 0;
        correctedPitch = 0;
        smoothedCorrection = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int keyIndex = key.getChoiceIndex();
        int scaleIndex = scale.getChoiceIndex();
        float speedVal = speed.getValue() / 100.0f;
        float sensitivityVal = sensitivity.getValue() / 100.0f;
        float humanizeVal = humanize.getValue() / 100.0f;
        float blendVal = blend.getValue() / 100.0f;

        int[] currentScale = SCALES[scaleIndex];

        // Correction rate: slower speed = more natural
        // Range from 0.001 (very slow) to 0.1 (fast)
        float correctionRate = 0.001f + speedVal * 0.099f;

        for (int i = 0; i < frameCount; i++) {
            float in = input[i];

            // Store in circular buffer for pitch detection
            inputBuffer[bufferWritePos] = in;
            bufferWritePos = (bufferWritePos + 1) % BUFFER_SIZE;

            // Also store in shift buffer
            shiftBuffer[shiftWritePos] = in;
            shiftWritePos = (shiftWritePos + 1) % SHIFT_BUFFER_SIZE;

            // Detect pitch periodically (every 64 samples to save CPU)
            if (bufferWritePos % 64 == 0) {
                float detectedPitch = detectPitch();
                if (detectedPitch > 0) {
                    currentPitch = detectedPitch;

                    // Find target note in scale
                    targetPitch = findTargetPitch(currentPitch, keyIndex, currentScale);
                }
            }

            // Calculate pitch shift ratio
            float shiftRatio = 1.0f;
            if (currentPitch > 0 && targetPitch > 0) {
                float pitchDiff = targetPitch - currentPitch;

                // Apply humanize - slight random variation
                if (humanizeVal > 0) {
                    pitchDiff += (float) (Math.random() - 0.5) * humanizeVal * 1.0f;
                }

                // Smooth the correction over time
                smoothedCorrection += (pitchDiff * sensitivityVal - smoothedCorrection) * correctionRate;

                // Calculate shift ratio from frequency difference
                if (currentPitch > 0) {
                    float targetFreq = currentPitch + smoothedCorrection;
                    shiftRatio = targetFreq / currentPitch;

                    // Limit ratio to reasonable range (prevent extreme shifts)
                    shiftRatio = Math.max(0.5f, Math.min(2.0f, shiftRatio));
                }
            }

            // Apply pitch shift using variable-rate playback with crossfade
            float shifted = applyPitchShift(in, shiftRatio);

            // Blend with dry signal
            output[i] = in * (1 - blendVal) + shifted * blendVal;
        }
    }

    /**
     * Detect pitch using autocorrelation.
     */
    private float detectPitch() {
        // Calculate autocorrelation
        float maxCorr = 0;
        int bestPeriod = 0;

        // Calculate signal energy
        float energy = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            energy += inputBuffer[i] * inputBuffer[i];
        }

        // Skip if signal is too quiet
        if (energy < 0.001f * BUFFER_SIZE) {
            return 0;
        }

        // Find peak in autocorrelation
        for (int period = MIN_PERIOD; period < MAX_PERIOD && period < BUFFER_SIZE / 2; period++) {
            float corr = 0;
            for (int i = 0; i < BUFFER_SIZE - period; i++) {
                int idx1 = (bufferWritePos + i) % BUFFER_SIZE;
                int idx2 = (bufferWritePos + i + period) % BUFFER_SIZE;
                corr += inputBuffer[idx1] * inputBuffer[idx2];
            }

            // Normalize
            corr /= (BUFFER_SIZE - period);

            if (corr > maxCorr) {
                maxCorr = corr;
                bestPeriod = period;
            }
        }

        // Require minimum correlation strength
        float normalizedCorr = maxCorr / (energy / BUFFER_SIZE + 0.0001f);
        if (normalizedCorr < 0.3f || bestPeriod == 0) {
            return 0;
        }

        // Refine period using parabolic interpolation
        if (bestPeriod > MIN_PERIOD && bestPeriod < MAX_PERIOD - 1) {
            float corrMinus = autocorr(bestPeriod - 1);
            float corrPlus = autocorr(bestPeriod + 1);
            float corrCenter = autocorr(bestPeriod);

            float delta = 0.5f * (corrMinus - corrPlus) / (corrMinus - 2 * corrCenter + corrPlus + 0.0001f);
            bestPeriod = (int) (bestPeriod + delta);
        }

        // Convert period to frequency
        return (float) currentSampleRate / bestPeriod;
    }

    /**
     * Helper for autocorrelation calculation.
     */
    private float autocorr(int period) {
        float corr = 0;
        for (int i = 0; i < BUFFER_SIZE - period; i++) {
            int idx1 = (bufferWritePos + i) % BUFFER_SIZE;
            int idx2 = (bufferWritePos + i + period) % BUFFER_SIZE;
            corr += inputBuffer[idx1] * inputBuffer[idx2];
        }
        return corr / (BUFFER_SIZE - period);
    }

    /**
     * Find the target pitch (nearest note in scale).
     */
    private float findTargetPitch(float freq, int keyIndex, int[] scaleNotes) {
        if (freq <= 0) return 0;

        // Convert frequency to MIDI note number
        float midiNote = 69 + 12 * (float) (Math.log(freq / 440.0) / Math.log(2));

        // Find nearest note in scale
        float nearestNote = findNearestScaleNote(midiNote, keyIndex, scaleNotes);

        // Convert back to frequency
        return (float) (440.0 * Math.pow(2, (nearestNote - 69) / 12.0));
    }

    /**
     * Find the nearest note in the given scale.
     */
    private float findNearestScaleNote(float midiNote, int keyIndex, int[] scaleNotes) {
        float bestNote = midiNote;
        float minDist = Float.MAX_VALUE;

        // Check notes in nearby octaves
        for (int octave = -1; octave <= 10; octave++) {
            for (int interval : scaleNotes) {
                float scaleNote = keyIndex + interval + (octave * 12);
                float dist = Math.abs(midiNote - scaleNote);
                if (dist < minDist) {
                    minDist = dist;
                    bestNote = scaleNote;
                }
            }
        }

        return bestNote;
    }

    /**
     * Apply pitch shift using granular synthesis with crossfade.
     */
    private float applyPitchShift(float input, float ratio) {
        if (Math.abs(ratio - 1.0f) < 0.001f) {
            return input;  // No shift needed
        }

        // Grain parameters
        int grainSize = 512;
        int overlap = grainSize / 2;

        // Advance read position by ratio
        shiftReadPos += ratio;

        // Wrap read position
        while (shiftReadPos >= SHIFT_BUFFER_SIZE) {
            shiftReadPos -= SHIFT_BUFFER_SIZE;
        }
        while (shiftReadPos < 0) {
            shiftReadPos += SHIFT_BUFFER_SIZE;
        }

        // Read with linear interpolation
        int readIdx1 = (int) shiftReadPos;
        int readIdx2 = (readIdx1 + 1) % SHIFT_BUFFER_SIZE;
        float frac = shiftReadPos - readIdx1;

        float sample1 = shiftBuffer[readIdx1];
        float sample2 = shiftBuffer[readIdx2];

        float shifted = sample1 + frac * (sample2 - sample1);

        // Crossfade with original to reduce artifacts
        // Calculate position within grain for windowing
        int grainPos = shiftWritePos % grainSize;
        float window = 0.5f * (1.0f - (float) Math.cos(2 * Math.PI * grainPos / grainSize));

        // Apply Hann window and mix with overlap-add
        return shifted * window + input * (1.0f - window) * 0.3f;
    }

    @Override
    public void reset() {
        if (inputBuffer != null) {
            java.util.Arrays.fill(inputBuffer, 0);
        }
        if (shiftBuffer != null) {
            java.util.Arrays.fill(shiftBuffer, 0);
        }
        bufferWritePos = 0;
        shiftWritePos = 0;
        shiftReadPos = 0;
        currentPitch = 0;
        targetPitch = 0;
        correctedPitch = 0;
        smoothedCorrection = 0;
    }

    @Override
    public int getLatency() {
        // Pitch detection requires filling the buffer before accurate detection
        // Latency is approximately half the buffer size
        return BUFFER_SIZE / 2;
    }
}
