package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import java.util.Arrays;

/**
 * 4-voice intelligent harmonizer with scale-aware pitch shifting.
 *
 * <p>Creates harmonies based on musical scales. Each voice can be shifted
 * by a configurable number of scale degrees (intervals) from the input pitch.
 * Supports various scale types with selectable root note.</p>
 *
 * <p>Each voice has independent:
 * <ul>
 *   <li>Interval: Scale degrees to shift (-7 to +7)</li>
 *   <li>Delay: Time offset for rhythmic effects (0-500ms)</li>
 *   <li>Pan: Stereo position (-100 to +100)</li>
 *   <li>Level: Volume mix (0-100%)</li>
 * </ul>
 * </p>
 *
 * <p>Pitch shifting uses granular overlap-add processing for high quality.</p>
 */
public class HarmonizerEffect extends AbstractEffect {

    // Number of harmony voices
    private static final int NUM_VOICES = 4;

    // Granular processing constants
    private static final int MAX_DELAY = 8192;
    private static final int NUM_GRAINS = 4;

    // Maximum voice delay in samples (500ms @ 48kHz)
    private static final int MAX_VOICE_DELAY = 24000;

    // Scale definitions (intervals in semitones from root)
    // Organized by: Major modes, Melodic Minor modes, Harmonic Minor modes, Pentatonics, Blues
    private static final int[][] SCALES = {
            // === Major Scale Modes (7) ===
            {0, 2, 4, 5, 7, 9, 11, 12},   // 0: Ionian (Major)
            {0, 2, 3, 5, 7, 9, 10, 12},   // 1: Dorian
            {0, 1, 3, 5, 7, 8, 10, 12},   // 2: Phrygian
            {0, 2, 4, 6, 7, 9, 11, 12},   // 3: Lydian
            {0, 2, 4, 5, 7, 9, 10, 12},   // 4: Mixolydian
            {0, 2, 3, 5, 7, 8, 10, 12},   // 5: Aeolian (Natural Minor)
            {0, 1, 3, 5, 6, 8, 10, 12},   // 6: Locrian

            // === Melodic Minor Modes (7) ===
            {0, 2, 3, 5, 7, 9, 11, 12},   // 7: Melodic Minor
            {0, 1, 3, 5, 7, 9, 10, 12},   // 8: Dorian b2 (Phrygian #6)
            {0, 2, 4, 6, 8, 9, 11, 12},   // 9: Lydian Augmented
            {0, 2, 4, 6, 7, 9, 10, 12},   // 10: Lydian Dominant
            {0, 2, 4, 5, 7, 8, 10, 12},   // 11: Mixolydian b6 (Hindu)
            {0, 2, 3, 5, 6, 8, 10, 12},   // 12: Locrian #2 (Half-Diminished)
            {0, 1, 3, 4, 6, 8, 10, 12},   // 13: Super Locrian (Altered)

            // === Harmonic Minor Modes (7) ===
            {0, 2, 3, 5, 7, 8, 11, 12},   // 14: Harmonic Minor
            {0, 1, 3, 5, 6, 9, 10, 12},   // 15: Locrian #6
            {0, 2, 4, 5, 8, 9, 11, 12},   // 16: Ionian #5 (Augmented Major)
            {0, 2, 3, 6, 7, 9, 10, 12},   // 17: Dorian #4 (Romanian)
            {0, 1, 4, 5, 7, 8, 10, 12},   // 18: Phrygian Dominant
            {0, 3, 4, 6, 7, 9, 11, 12},   // 19: Lydian #2
            {0, 1, 3, 4, 6, 8, 9, 12},    // 20: Ultra Locrian (Super Locrian bb7)

            // === Pentatonic Scales (3) ===
            {0, 2, 4, 7, 9, 12},          // 21: Pentatonic Major
            {0, 3, 5, 7, 10, 12},         // 22: Pentatonic Minor
            {0, 2, 4, 7, 10, 12},         // 23: Pentatonic Dominant

            // === Blues Scale ===
            {0, 3, 5, 6, 7, 10, 12}       // 24: Blues
    };

    private static final String[] SCALE_NAMES = {
            // Major modes
            "Ionian (Major)", "Dorian", "Phrygian", "Lydian",
            "Mixolydian", "Aeolian (Minor)", "Locrian",
            // Melodic Minor modes
            "Melodic Minor", "Dorian b2", "Lydian Aug", "Lydian Dom",
            "Mixolydian b6", "Locrian #2", "Super Locrian",
            // Harmonic Minor modes
            "Harmonic Minor", "Locrian #6", "Ionian #5", "Dorian #4",
            "Phrygian Dom", "Lydian #2", "Ultra Locrian",
            // Pentatonics
            "Penta Major", "Penta Minor", "Penta Dom",
            // Blues
            "Blues"
    };

    private static final String[] ROOT_NOTES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    // Global parameters (Row 1)
    private Parameter rootNote;
    private Parameter scaleType;
    private Parameter dryLevel;
    private Parameter grainSizeParam;
    private Parameter masterLevel;

    // Per-voice parameters (Row 2-5: one row per voice)
    private Parameter[] voiceEnabled;  // Toggle on/off
    private Parameter[] voiceInterval;
    private Parameter[] voiceDelay;
    private Parameter[] voicePan;
    private Parameter[] voiceLevel;

    // Processing state per voice (stereo: L and R)
    private float[][] voiceBufferL;
    private float[][] voiceBufferR;
    private int[] voiceWritePos;
    private int[] voiceSamplesFilled;
    private float[][] voiceGrainPhases;

    // Voice delay lines for per-voice delay
    private float[][] delayLineL;
    private float[][] delayLineR;
    private int[] delayWritePos;

    public HarmonizerEffect() {
        super(EffectMetadata.of("harmonizer", "Harmonizer",
                "4-voice scale-aware pitch harmonizer", EffectCategory.PITCH));
        // Default to Stereo mode for harmonizer
        setStereoMode(StereoMode.STEREO);
        initParameters();
    }

    private void initParameters() {
        // === ROW 1: Global parameters ===
        rootNote = addChoiceParameter("root", "Root",
                "Root note of the scale. Harmonies will follow intervals from this key.",
                ROOT_NOTES, 0);  // Default: C

        scaleType = addChoiceParameter("scale", "Scale",
                "Scale type for harmony intervals. Determines which notes are consonant.",
                SCALE_NAMES, 0);  // Default: Major

        dryLevel = addFloatParameter("dry", "Dry",
                "Level of the original unprocessed signal.",
                0.0f, 100.0f, 100.0f, "%");

        grainSizeParam = addFloatParameter("grain", "Grain",
                "Processing window size. Larger = smoother, smaller = more responsive.",
                20.0f, 100.0f, 50.0f, "ms");

        masterLevel = addFloatParameter("level", "Level",
                "Master output level for all harmony voices.",
                0.0f, 100.0f, 100.0f, "%");

        // === ROWS 2-5: Per-voice parameters (one row per voice) ===
        voiceEnabled = new Parameter[NUM_VOICES];
        voiceInterval = new Parameter[NUM_VOICES];
        voiceDelay = new Parameter[NUM_VOICES];
        voicePan = new Parameter[NUM_VOICES];
        voiceLevel = new Parameter[NUM_VOICES];

        // Default values
        int[] defaultIntervals = {2, 4, -3, -5};      // 3rd up, 5th up, 3rd down, 5th down
        float[] defaultPans = {-50, 50, -30, 30};     // Spread across stereo field
        float[] defaultLevels = {80, 70, 60, 50};     // Decreasing levels
        boolean[] defaultEnabled = {true, true, false, false};  // First two voices on by default

        for (int v = 0; v < NUM_VOICES; v++) {
            int voiceNum = v + 1;

            // Toggle at the beginning of each voice row
            voiceEnabled[v] = addBooleanParameter("v" + voiceNum + "On", "V" + voiceNum,
                    "Enable/disable voice " + voiceNum + ". When off, no processing is done.",
                    defaultEnabled[v]);

            voiceInterval[v] = addFloatParameter("v" + voiceNum + "Int", "Interval",
                    "Voice " + voiceNum + " interval in scale degrees. 0=unison, 2=third, 4=fifth, etc.",
                    -7.0f, 7.0f, defaultIntervals[v], "deg");

            voiceDelay[v] = addFloatParameter("v" + voiceNum + "Dly", "Delay",
                    "Voice " + voiceNum + " delay time for rhythmic offset effects.",
                    0.0f, 500.0f, v * 10.0f, "ms");

            voicePan[v] = addFloatParameter("v" + voiceNum + "Pan", "Pan",
                    "Voice " + voiceNum + " stereo position. -100=left, 0=center, +100=right.",
                    -100.0f, 100.0f, defaultPans[v], "");

            voiceLevel[v] = addFloatParameter("v" + voiceNum + "Lvl", "Level",
                    "Voice " + voiceNum + " volume level.",
                    0.0f, 100.0f, defaultLevels[v], "%");
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Allocate buffers for each voice
        voiceBufferL = new float[NUM_VOICES][MAX_DELAY];
        voiceBufferR = new float[NUM_VOICES][MAX_DELAY];
        voiceWritePos = new int[NUM_VOICES];
        voiceSamplesFilled = new int[NUM_VOICES];
        voiceGrainPhases = new float[NUM_VOICES][NUM_GRAINS];

        // Initialize grain phases for each voice (staggered)
        for (int v = 0; v < NUM_VOICES; v++) {
            for (int g = 0; g < NUM_GRAINS; g++) {
                voiceGrainPhases[v][g] = (float) g / NUM_GRAINS;
            }
        }

        // Allocate voice delay lines
        delayLineL = new float[NUM_VOICES][MAX_VOICE_DELAY];
        delayLineR = new float[NUM_VOICES][MAX_VOICE_DELAY];
        delayWritePos = new int[NUM_VOICES];
    }

    /**
     * Calculate the pitch shift in semitones for a given scale degree interval.
     */
    private float intervalToSemitones(int interval, int scaleIndex) {
        int[] scale = SCALES[scaleIndex];
        int scaleSize = scale.length - 1;  // Exclude octave repeat

        if (interval == 0) return 0;

        int octaves = 0;
        int degree = interval;

        // Handle negative intervals
        if (degree < 0) {
            while (degree < 0) {
                degree += scaleSize;
                octaves--;
            }
        } else {
            while (degree >= scaleSize) {
                degree -= scaleSize;
                octaves++;
            }
        }

        return scale[degree] + (octaves * 12);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono processing - create temporary stereo buffers
        float[] tempL = new float[frameCount];
        float[] tempR = new float[frameCount];
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];

        // Copy mono input to both channels
        System.arraycopy(input, 0, tempL, 0, frameCount);
        System.arraycopy(input, 0, tempR, 0, frameCount);

        // Process in stereo
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
        float dry = dryLevel.getValue() / 100.0f;
        float master = masterLevel.getValue() / 100.0f;
        int scale = scaleType.getChoiceIndex();

        float grainMs = grainSizeParam.getValue();
        int grainSamples = (int) (grainMs * sampleRate / 1000.0f);
        grainSamples = Math.max(256, Math.min(grainSamples, MAX_DELAY / 2));
        float phaseInc = 1.0f / grainSamples;

        // Initialize output with dry signal
        for (int i = 0; i < frameCount; i++) {
            outputL[i] = inputL[i] * dry;
            outputR[i] = inputR[i] * dry;
        }

        // Process each voice
        for (int v = 0; v < NUM_VOICES; v++) {
            // Skip disabled voices entirely (no processing, save CPU)
            if (!voiceEnabled[v].getBooleanValue()) continue;

            float level = voiceLevel[v].getValue() / 100.0f * master;
            if (level < 0.001f) continue;  // Skip silent voices

            int interval = Math.round(voiceInterval[v].getValue());
            float semitones = intervalToSemitones(interval, scale);
            float pitchRatio = (float) Math.pow(2.0, semitones / 12.0);

            int delaySamples = (int) (voiceDelay[v].getValue() * sampleRate / 1000.0f);
            delaySamples = Math.min(delaySamples, MAX_VOICE_DELAY - 1);

            float pan = voicePan[v].getValue() / 100.0f;  // -1 to +1
            float panL = (float) Math.cos((pan + 1) * Math.PI / 4);  // Constant power pan
            float panR = (float) Math.sin((pan + 1) * Math.PI / 4);

            // Process this voice
            for (int i = 0; i < frameCount; i++) {
                // Mix input to mono for pitch processing
                float monoIn = (inputL[i] + inputR[i]) * 0.5f;

                // Write to pitch shift buffer
                voiceBufferL[v][voiceWritePos[v]] = monoIn;
                voiceSamplesFilled[v] = Math.min(voiceSamplesFilled[v] + 1, MAX_DELAY);

                // Granular pitch shift
                float shifted = 0;
                float windowSum = 0;

                if (voiceSamplesFilled[v] >= grainSamples) {
                    for (int g = 0; g < NUM_GRAINS; g++) {
                        float phase = voiceGrainPhases[v][g];
                        float readOffset = grainSamples * (1.0f - phase) * pitchRatio;
                        float readPosF = voiceWritePos[v] - readOffset;
                        if (readPosF < 0) readPosF += MAX_DELAY;

                        int readPos = ((int) readPosF % MAX_DELAY + MAX_DELAY) % MAX_DELAY;
                        float frac = readPosF - (int) readPosF;
                        int nextPos = (readPos + 1) % MAX_DELAY;

                        float sample = voiceBufferL[v][readPos] * (1.0f - frac)
                                + voiceBufferL[v][nextPos] * frac;

                        // Hann window
                        float window = 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * phase));
                        shifted += sample * window;
                        windowSum += window;

                        voiceGrainPhases[v][g] += phaseInc;
                        if (voiceGrainPhases[v][g] >= 1.0f) {
                            voiceGrainPhases[v][g] -= 1.0f;
                        }
                    }
                    if (windowSum > 0.001f) {
                        shifted /= windowSum;
                    }
                }

                voiceWritePos[v] = (voiceWritePos[v] + 1) % MAX_DELAY;

                // Apply voice delay
                delayLineL[v][delayWritePos[v]] = shifted;
                delayLineR[v][delayWritePos[v]] = shifted;

                int readIdx = (delayWritePos[v] - delaySamples + MAX_VOICE_DELAY) % MAX_VOICE_DELAY;
                float delayedL = delayLineL[v][readIdx];
                float delayedR = delayLineR[v][readIdx];

                delayWritePos[v] = (delayWritePos[v] + 1) % MAX_VOICE_DELAY;

                // Apply pan and level, add to output
                outputL[i] += delayedL * panL * level;
                outputR[i] += delayedR * panR * level;
            }
        }

        // Soft clip output to prevent clipping
        for (int i = 0; i < frameCount; i++) {
            outputL[i] = softClip(outputL[i]);
            outputR[i] = softClip(outputR[i]);
        }
    }

    /**
     * Soft clipping to prevent harsh distortion.
     */
    private float softClip(float x) {
        if (x > 1.0f) {
            return 1.0f - (float) Math.exp(-x + 1.0f);
        } else if (x < -1.0f) {
            return -1.0f + (float) Math.exp(x + 1.0f);
        }
        return x;
    }

    @Override
    protected void onReset() {
        if (voiceBufferL != null) {
            for (int v = 0; v < NUM_VOICES; v++) {
                Arrays.fill(voiceBufferL[v], 0);
                Arrays.fill(voiceBufferR[v], 0);
                Arrays.fill(delayLineL[v], 0);
                Arrays.fill(delayLineR[v], 0);
                voiceWritePos[v] = 0;
                voiceSamplesFilled[v] = 0;
                delayWritePos[v] = 0;
                for (int g = 0; g < NUM_GRAINS; g++) {
                    voiceGrainPhases[v][g] = (float) g / NUM_GRAINS;
                }
            }
        }
    }

    @Override
    public int getLatency() {
        // Latency is grain size plus maximum voice delay
        int grainLatency = (int) (grainSizeParam.getValue() * sampleRate / 1000.0f);
        return grainLatency;
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Global parameters (5): root, scale, dry, grain, level
        // Rows 2-5: Per-voice parameters (5 each): vNOn, vNInt, vNDly, vNPan, vNLvl
        return new int[] {5, 5, 5, 5, 5};
    }
}
