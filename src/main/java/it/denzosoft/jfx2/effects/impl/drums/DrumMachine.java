package it.denzosoft.jfx2.effects.impl.drums;

import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.impl.drums.DrumPattern.DrumHit;
import it.denzosoft.jfx2.effects.impl.drums.DrumSounds.DrumSound;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Drum Machine input effect.
 *
 * <p>Generates stereo drum patterns at configurable tempo and style.
 * Includes a 2-bar count-in with stick clicks before the main pattern starts.</p>
 *
 * <p>Supports loading external pattern libraries in JSON format.</p>
 *
 * <p>This is an input effect (source) - it generates audio signal rather than
 * processing input.</p>
 */
public class DrumMachine extends AbstractEffect {

    private static final EffectMetadata METADATA = new EffectMetadata(
            "drummachine",
            "Drum Machine",
            "Programmable drum machine with multiple rhythm styles",
            EffectCategory.INPUT_SOURCE,
            "JFx2",
            "1.0"
    );

    private static final String[] COUNT_IN_CHOICES = {"Off", "1 Bar", "2 Bars"};

    // Pattern library
    private DrumPatternLibrary patternLibrary;
    private String[] styleNames;

    // Parameters
    private Parameter bpmParam;
    private Parameter styleParam;
    private Parameter volumeParam;
    private Parameter countInParam;
    private Parameter playParam;
    private Parameter accentParam;
    private Parameter swingParam;
    private Parameter soundSourceParam;

    // Audio state
    private DrumSoundProvider sounds;
    private DrumSoundsFactory.ProviderType currentSoundSource = DrumSoundsFactory.ProviderType.SYNTHESIZED;
    private DrumPattern currentPattern;
    private DrumPattern countInPattern;

    // Sequencer state
    private long samplePosition;          // Current sample position
    private double samplesPerStep;        // Samples per 16th note
    private int currentStep;              // Current step in pattern
    private boolean isCountingIn;         // True during count-in
    private int countInStep;              // Current step in count-in
    private boolean isPlaying;
    private int lastStyleIndex = -1;

    // Active voices (playing drum sounds)
    private final List<DrumVoice> activeVoices = new ArrayList<>();

    public DrumMachine() {
        super(METADATA);

        // Load default pattern library
        loadDefaultLibrary();

        // Play/Stop - default to playing
        playParam = addBooleanParameter("play", "Play",
                "Start or stop playback", true);

        // BPM: 40-240
        bpmParam = addFloatParameter("bpm", "BPM",
                "Tempo in beats per minute", 40f, 240f, 120f, "bpm");

        // Style selector - populated from library
        styleParam = addChoiceParameter("style", "Style",
                "Drum pattern style", styleNames, 0);

        // Volume: -40 to +6 dB
        volumeParam = addFloatParameter("volume", "Volume",
                "Output volume", -40f, 6f, 0f, "dB");

        // Count-in: 0=off, 1=1 bar, 2=2 bars
        countInParam = addChoiceParameter("countin", "Count-In",
                "Number of count-in bars", COUNT_IN_CHOICES, 2);

        // Accent strength: 0-100%
        accentParam = addFloatParameter("accent", "Accent",
                "Accent strength on downbeats", 0f, 100f, 50f, "%");

        // Swing amount: 0-100%
        swingParam = addFloatParameter("swing", "Swing",
                "Swing/shuffle amount", 0f, 100f, 0f, "%");

        // Sound source selector
        soundSourceParam = addChoiceParameter("soundsource", "Sound Source",
                "Drum sound generation method", DrumSoundsFactory.getProviderNames(), 0);

        // Initialize with default pattern
        updatePattern();
    }

    /**
     * Load the default built-in pattern library.
     */
    private void loadDefaultLibrary() {
        patternLibrary = DrumPatternLibrary.createDefaultLibrary();
        updateStyleNames();
    }

    /**
     * Load a pattern library from a JSON file.
     *
     * @param path Path to the JSON library file
     * @throws IOException if loading fails
     */
    public void loadLibrary(Path path) throws IOException {
        DrumPatternLibrary newLibrary = DrumPatternLibrary.loadFromFile(path);
        if (newLibrary.getPatternCount() > 0) {
            patternLibrary = newLibrary;
            updateStyleNames();
            updateStyleParameter();
            updatePattern();
        }
    }

    /**
     * Load a pattern library from JSON string.
     *
     * @param json JSON content
     */
    public void loadLibraryFromJson(String json) {
        DrumPatternLibrary newLibrary = DrumPatternLibrary.fromJson(json);
        if (newLibrary.getPatternCount() > 0) {
            patternLibrary = newLibrary;
            updateStyleNames();
            updateStyleParameter();
            updatePattern();
        }
    }

    /**
     * Get the current pattern library.
     */
    public DrumPatternLibrary getPatternLibrary() {
        return patternLibrary;
    }

    /**
     * Save the current library to a JSON file.
     */
    public void saveLibrary(Path path) throws IOException {
        patternLibrary.saveToFile(path);
    }

    private void updateStyleNames() {
        List<DrumPattern> patterns = patternLibrary.getPatterns();
        styleNames = new String[patterns.size()];
        for (int i = 0; i < patterns.size(); i++) {
            styleNames[i] = patterns.get(i).getName();
        }
    }

    private void updateStyleParameter() {
        // Recreate the style parameter with new choices
        // This is a workaround since Parameter doesn't support dynamic choices
        if (styleParam != null) {
            int currentIndex = Math.min(styleParam.getChoiceIndex(), styleNames.length - 1);
            parameters.remove(styleParam.getId());
            parameterList.remove(styleParam);

            // Re-add at correct position (after BPM)
            int bpmIndex = parameterList.indexOf(bpmParam);
            Parameter newStyleParam = new Parameter("style", "Style",
                    "Drum pattern style", styleNames, Math.max(0, currentIndex));
            parameters.put(newStyleParam.getId(), newStyleParam);
            parameterList.add(bpmIndex + 1, newStyleParam);
            styleParam = newStyleParam;
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int bufferSize) {
        // Create sound provider using factory
        updateSoundSource(sampleRate);

        this.samplePosition = 0;
        this.currentStep = 0;
        this.countInStep = 0;
        this.isCountingIn = false;
        this.isPlaying = false;
        this.activeVoices.clear();

        updatePattern();
        updateTiming();
    }

    /**
     * Update the sound source based on the parameter selection.
     */
    private void updateSoundSource(int sampleRate) {
        DrumSoundsFactory.ProviderType[] types = DrumSoundsFactory.ProviderType.values();
        int selectedIndex = soundSourceParam != null ? soundSourceParam.getChoiceIndex() : 0;

        if (selectedIndex >= 0 && selectedIndex < types.length) {
            DrumSoundsFactory.ProviderType newType = types[selectedIndex];

            // Only recreate if type changed or sounds not initialized
            if (sounds == null || currentSoundSource != newType) {
                currentSoundSource = newType;
                sounds = DrumSoundsFactory.create(newType, sampleRate);
            }
        } else if (sounds == null) {
            sounds = DrumSoundsFactory.create(sampleRate);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // For mono processing, we need temporary buffers for stereo generation
        // then mix down to mono
        float[] tempL = new float[frameCount];
        float[] tempR = new float[frameCount];
        processInternal(frameCount, tempL, tempR);

        // Mix stereo to mono
        int len = Math.min(frameCount, output.length);
        for (int i = 0; i < len; i++) {
            output[i] = (tempL[i] + tempR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Stereo output with proper panning
        processInternal(frameCount, outputL, outputR);
    }

    private void processInternal(int frameCount, float[] outputL, float[] outputR) {
        // Check if style changed
        int currentStyleIndex = styleParam.getChoiceIndex();
        if (currentStyleIndex != lastStyleIndex) {
            lastStyleIndex = currentStyleIndex;
            updatePattern();
        }

        // Clear output
        int len = Math.min(frameCount, Math.min(outputL.length, outputR.length));
        for (int i = 0; i < len; i++) {
            outputL[i] = 0;
            outputR[i] = 0;
        }

        // Check play state
        boolean shouldPlay = playParam.getBooleanValue();

        if (shouldPlay && !isPlaying) {
            // Start playing - begin count-in if enabled
            startPlayback();
        } else if (!shouldPlay && isPlaying) {
            // Stop playing
            stopPlayback();
        }

        if (!isPlaying) {
            return;
        }

        // Volume
        float volume = dbToLinear(volumeParam.getValue());

        // Update timing in case BPM changed
        updateTiming();

        // Process each sample
        for (int frame = 0; frame < len; frame++) {
            // Check if we've reached a new step
            int newStep = (int) (samplePosition / samplesPerStep);

            if (isCountingIn) {
                // Count-in phase
                if (newStep != countInStep) {
                    countInStep = newStep;

                    int countInSteps = countInPattern.getTotalSteps();
                    if (countInStep >= countInSteps) {
                        // Count-in finished, start main pattern
                        isCountingIn = false;
                        samplePosition = 0;
                        currentStep = 0;
                        triggerStep(0, currentPattern);
                    } else {
                        triggerStep(countInStep, countInPattern);
                    }
                }
            } else {
                // Main pattern phase
                int patternSteps = currentPattern.getTotalSteps();
                int patternStep = newStep % patternSteps;

                if (patternStep != currentStep) {
                    currentStep = patternStep;
                    triggerStep(currentStep, currentPattern);
                }
            }

            // Mix active voices
            float left = 0;
            float right = 0;

            for (int v = activeVoices.size() - 1; v >= 0; v--) {
                DrumVoice voice = activeVoices.get(v);
                float sample = voice.nextSample();

                if (voice.isFinished()) {
                    activeVoices.remove(v);
                } else {
                    // Pan: -1 = left, +1 = right
                    float pan = voice.pan;
                    float leftGain = (float) Math.cos((pan + 1) * Math.PI / 4);
                    float rightGain = (float) Math.sin((pan + 1) * Math.PI / 4);

                    left += sample * leftGain * voice.velocity;
                    right += sample * rightGain * voice.velocity;
                }
            }

            // Write output
            outputL[frame] = left * volume;
            outputR[frame] = right * volume;

            samplePosition++;
        }
    }

    @Override
    protected void onReset() {
        samplePosition = 0;
        currentStep = 0;
        countInStep = 0;
        isCountingIn = false;
        isPlaying = false;
        activeVoices.clear();
    }

    @Override
    public void release() {
        onReset();
        sounds = null;
        super.release();
    }

    // ==================== PRIVATE METHODS ====================

    private void startPlayback() {
        isPlaying = true;
        samplePosition = 0;
        activeVoices.clear();

        int countInBars = countInParam.getChoiceIndex();
        if (countInBars > 0) {
            isCountingIn = true;
            countInStep = -1;  // Will trigger step 0 on first sample

            // Create count-in pattern
            int beatsPerBar = currentPattern.getBeatsPerBar();
            countInPattern = new DrumPattern("Count-In", "Metronome", beatsPerBar, 4, countInBars);

            for (int bar = 0; bar < countInBars; bar++) {
                for (int beat = 0; beat < beatsPerBar; beat++) {
                    // Accent on beat 1, regular on others
                    float velocity = (beat == 0) ? 1.0f : 0.7f;
                    countInPattern.addHit(bar, beat, 0, DrumSound.STICKS, velocity, 0);
                }
            }
        } else {
            isCountingIn = false;
            currentStep = -1;  // Will trigger step 0 on first sample
        }

        updateTiming();
    }

    private void stopPlayback() {
        isPlaying = false;
        activeVoices.clear();
    }

    private void updatePattern() {
        int styleIndex = styleParam != null ? styleParam.getChoiceIndex() : 0;
        styleIndex = Math.max(0, Math.min(styleIndex, patternLibrary.getPatternCount() - 1));

        currentPattern = patternLibrary.getPattern(styleIndex);
        if (currentPattern == null && patternLibrary.getPatternCount() > 0) {
            currentPattern = patternLibrary.getPattern(0);
        }
    }

    private void updateTiming() {
        float bpm = bpmParam != null ? bpmParam.getValue() : 120f;
        // Samples per beat = sampleRate * 60 / bpm
        // Samples per 16th note (step) = samples per beat / 4
        double samplesPerBeat = sampleRate * 60.0 / bpm;
        samplesPerStep = samplesPerBeat / 4.0;
    }

    private void triggerStep(int step, DrumPattern pattern) {
        if (sounds == null) {
            return;
        }

        List<DrumHit> hits = pattern.getHitsAt(step);
        float accentAmount = accentParam != null ? accentParam.getValue() / 100.0f : 0.5f;

        for (DrumHit hit : hits) {
            if (hit.sound() == DrumSound.REST) continue;

            float[] samples = sounds.getSound(hit.sound());
            if (samples == null || samples.length == 0) {
                continue;
            }

            // Apply accent boost on accented beats
            float velocity = hit.velocity();
            if (pattern.isAccent(step)) {
                velocity *= (1.0f + accentAmount * 0.3f);
            }
            velocity = Math.min(1.0f, velocity);

            DrumVoice voice = new DrumVoice(samples, velocity, hit.pan());
            activeVoices.add(voice);
        }
    }

    /**
     * Get the current style name for display.
     */
    public String getCurrentStyleName() {
        int styleIndex = styleParam != null ? styleParam.getChoiceIndex() : 0;
        if (styleIndex >= 0 && styleIndex < styleNames.length) {
            return styleNames[styleIndex];
        }
        return "Unknown";
    }

    /**
     * Get all available style names.
     */
    public String[] getStyleNames() {
        return styleNames.clone();
    }

    /**
     * Get the number of available patterns.
     */
    public int getPatternCount() {
        return patternLibrary.getPatternCount();
    }

    /**
     * Get current playback position info.
     */
    public PlaybackInfo getPlaybackInfo() {
        if (!isPlaying) {
            return new PlaybackInfo(false, 0, 0, 0, isCountingIn);
        }

        DrumPattern pattern = isCountingIn ? countInPattern : currentPattern;
        if (pattern == null) {
            return new PlaybackInfo(false, 0, 0, 0, false);
        }

        int stepsPerBar = pattern.getBeatsPerBar() * pattern.getStepsPerBeat();
        int step = isCountingIn ? countInStep : currentStep;
        int bar = step / stepsPerBar;
        int beat = (step % stepsPerBar) / pattern.getStepsPerBeat();

        return new PlaybackInfo(true, bar + 1, beat + 1, step % pattern.getStepsPerBeat(), isCountingIn);
    }

    /**
     * Playback position information.
     */
    public record PlaybackInfo(boolean playing, int bar, int beat, int sixteenth, boolean countingIn) {}

    // ==================== DRUM VOICE ====================

    /**
     * A single playing drum sound instance.
     */
    private static class DrumVoice {
        private final float[] samples;
        private final float velocity;
        private final float pan;
        private int position;

        DrumVoice(float[] samples, float velocity, float pan) {
            this.samples = samples;
            this.velocity = velocity;
            this.pan = pan;
            this.position = 0;
        }

        float nextSample() {
            if (position >= samples.length) {
                return 0;
            }
            return samples[position++];
        }

        boolean isFinished() {
            return position >= samples.length;
        }
    }
}
