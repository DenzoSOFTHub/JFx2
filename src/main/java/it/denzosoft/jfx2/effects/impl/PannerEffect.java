package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Auto-Panner effect.
 *
 * <p>Automatically moves the audio signal between left and right channels,
 * creating a stereo movement effect. Uses an LFO to modulate the pan position.</p>
 *
 * <p>Features:
 * - Multiple waveform shapes (sine, triangle, square)
 * - Adjustable rate and depth
 * - Smooth or hard panning transitions
 * - Works on both mono and stereo input</p>
 */
public class PannerEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "panner",
            "Panner",
            "Auto-pan with adjustable rate and waveform",
            EffectCategory.MODULATION
    );

    // Waveform names for UI
    private static final String[] WAVEFORM_NAMES = {
            "Sine",      // Smooth, natural movement
            "Triangle",  // Linear sweep
            "Square"     // Hard left/right switching
    };

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter waveformParam;
    private final Parameter smoothParam;

    // DSP components
    private LFO lfo;
    private float lastPan = 0.0f;  // For smoothing

    public PannerEffect() {
        super(METADATA);

        // Rate: 0.1 Hz to 10 Hz, default 1 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of the panning movement. Slow rates for subtle movement, fast for tremolo-like effect.",
                0.1f, 10.0f, 1.0f, "Hz");

        // Depth: 0% to 100%, default 100%
        depthParam = addFloatParameter("depth", "Depth",
                "Width of the pan sweep. 100% = full left to right, lower values stay closer to center.",
                0.0f, 100.0f, 100.0f, "%");

        // Waveform: Sine, Triangle, Square
        waveformParam = addChoiceParameter("waveform", "Wave",
                "Shape of the panning movement. Sine = smooth, Triangle = linear, Square = hard switch.",
                WAVEFORM_NAMES, 0);

        // Smoothing: 0% to 100%, default 20%
        smoothParam = addFloatParameter("smooth", "Smooth",
                "Smooths transitions, especially useful with square wave to avoid clicks.",
                0.0f, 100.0f, 20.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        lfo = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);
        lastPan = 0.0f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono processing - just copy through (panning needs stereo output)
        // In a real scenario, this would be expanded to stereo by the engine
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        int waveformIndex = (int) waveformParam.getValue();
        float smoothing = smoothParam.getValue() / 100.0f;

        // Update LFO
        lfo.setFrequency(rate);
        lfo.setWaveform(indexToWaveform(waveformIndex));

        // Smoothing coefficient (higher = more smoothing)
        float smoothCoef = 0.001f + smoothing * 0.05f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Get LFO value (-1 to 1)
            float lfoValue = lfo.tick();

            // Apply depth (reduce range if depth < 100%)
            float targetPan = lfoValue * depth;

            // Smooth the pan value to avoid clicks
            lastPan = lastPan + smoothCoef * (targetPan - lastPan);
            float pan = lastPan;

            // Convert pan (-1 to 1) to left/right gains using constant power law
            // pan = -1: full left, pan = 0: center, pan = +1: full right
            float angle = (pan + 1.0f) * 0.25f * (float) Math.PI;  // 0 to PI/2
            float gainL = (float) Math.cos(angle);
            float gainR = (float) Math.sin(angle);

            // Mix input channels to mono, then apply panning
            float monoIn = (inputL[i] + inputR[i]) * 0.5f;

            // Or keep stereo separation with pan modulation
            // This version pans the existing stereo field
            float inL = inputL[i];
            float inR = inputR[i];

            // Cross-fade based on pan position
            if (pan < 0) {
                // Panning left: move right channel content to left
                float blend = -pan;  // 0 to 1
                outputL[i] = inL + inR * blend * 0.5f;
                outputR[i] = inR * (1.0f - blend * 0.5f);
            } else {
                // Panning right: move left channel content to right
                float blend = pan;  // 0 to 1
                outputL[i] = inL * (1.0f - blend * 0.5f);
                outputR[i] = inR + inL * blend * 0.5f;
            }

            // Apply constant power compensation
            outputL[i] *= gainL * 1.4142f;  // sqrt(2) compensation
            outputR[i] *= gainR * 1.4142f;
        }
    }

    /**
     * Convert waveform index to LFO.Waveform enum.
     */
    private LFO.Waveform indexToWaveform(int index) {
        return switch (index) {
            case 1 -> LFO.Waveform.TRIANGLE;
            case 2 -> LFO.Waveform.SQUARE;
            default -> LFO.Waveform.SINE;
        };
    }

    @Override
    protected void onReset() {
        if (lfo != null) lfo.reset();
        lastPan = 0.0f;
    }

    // Convenience setters
    public void setRate(float hz) {
        rateParam.setValue(hz);
    }

    public void setDepth(float percent) {
        depthParam.setValue(percent);
    }

    public void setWaveform(int index) {
        waveformParam.setValue(index);
    }

    public void setSmooth(float percent) {
        smoothParam.setValue(percent);
    }
}
