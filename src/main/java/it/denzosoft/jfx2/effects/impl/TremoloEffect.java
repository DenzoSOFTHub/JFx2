package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Tremolo effect - amplitude modulation.
 *
 * <p>Modulates the volume of the signal using an LFO.
 * Supports multiple waveform shapes.</p>
 */
public class TremoloEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "tremolo",
            "Tremolo",
            "Classic amplitude modulation effect",
            EffectCategory.MODULATION
    );

    private static final String[] WAVEFORM_NAMES = {"Sine", "Triangle", "Square", "Random"};

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter waveformParam;

    // LFOs - Left and Right
    private LFO lfoL;
    private LFO lfoR;

    public TremoloEffect() {
        super(METADATA);

        // Rate: 0.5 Hz to 15 Hz, default 5 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of volume modulation. Slow for subtle pulse, fast for helicopter-like effect.",
                0.5f, 15.0f, 5.0f, "Hz");

        // Depth: 0% to 100%, default 50%
        depthParam = addFloatParameter("depth", "Depth",
                "Intensity of volume changes. Higher values create more dramatic pulsing.",
                0.0f, 100.0f, 50.0f, "%");

        // Waveform: sine, triangle, square, random
        waveformParam = addChoiceParameter("waveform", "Waveform",
                "Shape of modulation: Sine (smooth), Triangle (linear), Square (choppy), Random (experimental).",
                WAVEFORM_NAMES, 0);
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        lfoL = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);
        lfoR = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);
        lfoR.setPhase(0.25f);  // Offset for stereo effect
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        int waveformIndex = waveformParam.getChoiceIndex();

        // Update LFO
        lfoL.setFrequency(rate);

        // Set waveform
        LFO.Waveform waveform = switch (waveformIndex) {
            case 0 -> LFO.Waveform.SINE;
            case 1 -> LFO.Waveform.TRIANGLE;
            case 2 -> LFO.Waveform.SQUARE;
            case 3 -> LFO.Waveform.RANDOM;
            default -> LFO.Waveform.SINE;
        };
        lfoL.setWaveform(waveform);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            // Get LFO value (-1 to +1)
            float lfoValue = lfoL.tick();

            // Convert to gain modulation (1-depth to 1+depth range, centered at 1)
            // When lfo = -1: gain = 1 - depth
            // When lfo = 0: gain = 1
            // When lfo = +1: gain = 1 (we want modulation below unity)
            // Actually, classic tremolo: gain goes from (1-depth) to 1
            float gain = 1.0f - depth * 0.5f * (1.0f - lfoValue);

            output[i] = input[i] * gain;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        int waveformIndex = waveformParam.getChoiceIndex();

        // Update LFOs
        lfoL.setFrequency(rate);
        lfoR.setFrequency(rate);

        // Set waveform
        LFO.Waveform waveform = switch (waveformIndex) {
            case 0 -> LFO.Waveform.SINE;
            case 1 -> LFO.Waveform.TRIANGLE;
            case 2 -> LFO.Waveform.SQUARE;
            case 3 -> LFO.Waveform.RANDOM;
            default -> LFO.Waveform.SINE;
        };
        lfoL.setWaveform(waveform);
        lfoR.setWaveform(waveform);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            float lfoValueL = lfoL.tick();
            float gainL = 1.0f - depth * 0.5f * (1.0f - lfoValueL);
            outputL[i] = inputL[i] * gainL;

            // Right channel
            float lfoValueR = lfoR.tick();
            float gainR = 1.0f - depth * 0.5f * (1.0f - lfoValueR);
            outputR[i] = inputR[i] * gainR;
        }
    }

    @Override
    protected void onReset() {
        if (lfoL != null) {
            lfoL.reset();
        }
        if (lfoR != null) {
            lfoR.reset();
            lfoR.setPhase(0.25f);
        }
    }

    // Convenience setters
    public void setRate(float hz) {
        rateParam.setValue(hz);
    }

    public void setDepth(float percent) {
        depthParam.setValue(percent);
    }

    public void setWaveform(int index) {
        waveformParam.setChoice(index);
    }
}
