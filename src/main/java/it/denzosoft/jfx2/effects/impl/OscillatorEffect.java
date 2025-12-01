package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Oscillator effect - generates various waveforms.
 *
 * <p>This is a generator effect that ignores input and produces audio
 * from mathematical waveform generation. Supports sine, triangle,
 * sawtooth, and square waves.</p>
 */
public class OscillatorEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "oscillator",
            "Oscillator",
            "Generates sine, triangle, sawtooth, or square waveforms",
            EffectCategory.INPUT_SOURCE
    );

    // Waveform types
    public static final int WAVEFORM_SINE = 0;
    public static final int WAVEFORM_TRIANGLE = 1;
    public static final int WAVEFORM_SAWTOOTH = 2;
    public static final int WAVEFORM_SQUARE = 3;

    private static final String[] WAVEFORM_NAMES = {"Sine", "Triangle", "Sawtooth", "Square"};

    // Parameters
    private final Parameter waveformParam;
    private final Parameter frequencyParam;
    private final Parameter volumeParam;
    private final Parameter playingParam;

    // Oscillator state
    private double phase;
    private double phaseIncrement;

    public OscillatorEffect() {
        super(METADATA);

        // Waveform selector
        waveformParam = addChoiceParameter("waveform", "Waveform",
                "Wave shape: Sine (pure tone), Triangle (mellow), Sawtooth (bright), Square (hollow).",
                WAVEFORM_NAMES, WAVEFORM_SINE);

        // Frequency: 20 Hz to 2000 Hz (covers guitar range A0=27.5 to B5=987 plus harmonics)
        frequencyParam = addFloatParameter("frequency", "Frequency",
                "Oscillator pitch in Hz. A4 = 440 Hz. Use for test tones or drone sounds.",
                20.0f, 2000.0f, 440.0f, "Hz");

        // Volume control (default 0dB = full scale -1 to +1)
        volumeParam = addFloatParameter("volume", "Volume",
                "Output level of the oscillator. At 0dB outputs full scale (-1 to +1).",
                -60.0f, 0.0f, 0.0f, "dB");

        // Playing state
        playingParam = addBooleanParameter("playing", "Playing",
                "Enable or disable the oscillator output.",
                true);

        this.phase = 0;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        updatePhaseIncrement();
    }

    private void updatePhaseIncrement() {
        float freq = frequencyParam.getValue();
        phaseIncrement = (2.0 * Math.PI * freq) / sampleRate;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int waveform = waveformParam.getChoiceIndex();
        float freq = frequencyParam.getValue();
        float volumeDb = volumeParam.getValue();
        float volumeLinear = dbToLinear(volumeDb);
        boolean playing = playingParam.getValue() > 0.5f;

        // Update phase increment if frequency changed
        phaseIncrement = (2.0 * Math.PI * freq) / sampleRate;

        for (int i = 0; i < frameCount && i < output.length; i++) {
            if (!playing) {
                output[i] = 0.0f;
                continue;
            }

            float sample = generateSample(waveform);
            output[i] = sample * volumeLinear;

            // Advance phase
            phase += phaseIncrement;
            if (phase >= 2.0 * Math.PI) {
                phase -= 2.0 * Math.PI;
            }
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        int waveform = waveformParam.getChoiceIndex();
        float freq = frequencyParam.getValue();
        float volumeDb = volumeParam.getValue();
        float volumeLinear = dbToLinear(volumeDb);
        boolean playing = playingParam.getValue() > 0.5f;

        // Update phase increment if frequency changed
        phaseIncrement = (2.0 * Math.PI * freq) / sampleRate;

        int len = Math.min(frameCount, Math.min(outputL.length, outputR.length));

        for (int i = 0; i < len; i++) {
            if (!playing) {
                outputL[i] = 0.0f;
                outputR[i] = 0.0f;
                continue;
            }

            float sample = generateSample(waveform);
            float out = sample * volumeLinear;
            outputL[i] = out;
            outputR[i] = out;

            // Advance phase
            phase += phaseIncrement;
            if (phase >= 2.0 * Math.PI) {
                phase -= 2.0 * Math.PI;
            }
        }
    }

    private float generateSample(int waveform) {
        return switch (waveform) {
            case WAVEFORM_SINE -> (float) Math.sin(phase);
            case WAVEFORM_TRIANGLE -> {
                double normalizedPhase = phase / (2.0 * Math.PI);
                if (normalizedPhase < 0.25) {
                    yield (float) (normalizedPhase * 4.0);
                } else if (normalizedPhase < 0.75) {
                    yield (float) (2.0 - normalizedPhase * 4.0);
                } else {
                    yield (float) (normalizedPhase * 4.0 - 4.0);
                }
            }
            case WAVEFORM_SAWTOOTH -> {
                double normalizedPhase = phase / (2.0 * Math.PI);
                yield (float) (2.0 * normalizedPhase - 1.0);
            }
            case WAVEFORM_SQUARE -> phase < Math.PI ? 1.0f : -1.0f;
            default -> (float) Math.sin(phase);
        };
    }

    @Override
    protected void onReset() {
        phase = 0;
    }

    /**
     * Set the waveform type.
     */
    public void setWaveform(int waveform) {
        waveformParam.setValue(waveform);
    }

    /**
     * Get the current waveform type.
     */
    public int getWaveform() {
        return waveformParam.getChoiceIndex();
    }

    /**
     * Set frequency in Hz.
     */
    public void setFrequency(float hz) {
        frequencyParam.setValue(hz);
        updatePhaseIncrement();
    }

    /**
     * Get current frequency in Hz.
     */
    public float getFrequency() {
        return frequencyParam.getValue();
    }

    /**
     * Set frequency from MIDI note number.
     * A4 (MIDI note 69) = 440 Hz
     */
    public void setMidiNote(int midiNote) {
        float freq = (float) (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0));
        setFrequency(freq);
    }

    /**
     * Start oscillator.
     */
    public void play() {
        playingParam.setValue(1.0f);
    }

    /**
     * Stop oscillator.
     */
    public void stop() {
        playingParam.setValue(0.0f);
    }

    /**
     * Check if oscillator is active.
     */
    public boolean isPlaying() {
        return playingParam.getValue() > 0.5f;
    }
}
