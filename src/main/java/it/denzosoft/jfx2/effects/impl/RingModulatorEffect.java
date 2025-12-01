package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Ring modulator effect.
 *
 * <p>Multiplies the input signal by a carrier oscillator to create
 * metallic, bell-like tones. The output contains sum and difference
 * frequencies of the input and carrier.</p>
 *
 * <p>Features:
 * - Variable carrier frequency (20 Hz to 4000 Hz)
 * - Multiple carrier waveforms (sine, square, triangle)
 * - Optional LFO modulation of carrier frequency
 * - Mix control</p>
 */
public class RingModulatorEffect extends AbstractEffect {

    // Parameters
    private Parameter carrierFreq;
    private Parameter carrierWaveform;
    private Parameter lfoDepth;
    private Parameter lfoRate;
    private Parameter mix;

    // Carrier oscillator state - Left and Right
    private double carrierPhaseL;
    private double carrierPhaseR;

    // LFOs for frequency modulation - Left and Right
    private LFO lfoL;
    private LFO lfoR;

    public RingModulatorEffect() {
        super(EffectMetadata.of("ringmod", "Ring Modulator", "Ring modulation for metallic tones", EffectCategory.MODULATION));
        initParameters();
    }

    private void initParameters() {
        carrierFreq = addFloatParameter("freq", "Frequency",
                "Carrier oscillator frequency. Creates bell-like tones when offset from note frequencies.",
                20.0f, 4000.0f, 440.0f, "Hz");

        carrierWaveform = addChoiceParameter("waveform", "Waveform",
                "Carrier wave shape: Sine (pure), Square (harsh), Triangle (mellow), Sawtooth (bright).",
                new String[]{"Sine", "Square", "Triangle", "Sawtooth"}, 0);

        lfoDepth = addFloatParameter("lfoDepth", "LFO Depth",
                "Amount of frequency modulation from LFO. Creates warbling, sci-fi effects.",
                0.0f, 100.0f, 0.0f, "%");
        lfoRate = addFloatParameter("lfoRate", "LFO Rate",
                "Speed of the frequency modulation. Slow for subtle movement, fast for extreme effects.",
                0.1f, 10.0f, 1.0f, "Hz");

        mix = addFloatParameter("mix", "Mix",
                "Balance between dry signal and ring modulated signal.",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        carrierPhaseL = 0;
        carrierPhaseR = 0;

        lfoL = new LFO(lfoRate.getValue(), sampleRate);
        lfoL.setWaveform(LFO.Waveform.SINE);
        lfoR = new LFO(lfoRate.getValue(), sampleRate);
        lfoR.setWaveform(LFO.Waveform.SINE);
        lfoR.setPhase(0.25f);  // Slight stereo offset
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float baseFreq = carrierFreq.getValue();
        int waveform = carrierWaveform.getChoiceIndex();
        float lfoDepthAmt = lfoDepth.getValue() / 100.0f;
        float mixAmt = mix.getValue() / 100.0f;

        lfoL.setFrequency(lfoRate.getValue());

        double phaseInc = 2.0 * Math.PI / sampleRate;

        for (int i = 0; i < frameCount; i++) {
            float sample = input[i];

            // LFO modulates carrier frequency
            float lfoMod = lfoL.tick();
            float freq = baseFreq * (1.0f + lfoMod * lfoDepthAmt);
            freq = Math.max(20.0f, Math.min(freq, sampleRate / 2.0f - 1));

            // Generate carrier
            float carrier = generateCarrier(carrierPhaseL, waveform);

            // Ring modulation = input * carrier
            float modulated = sample * carrier;

            // Advance carrier phase
            carrierPhaseL += phaseInc * freq;
            if (carrierPhaseL >= 2.0 * Math.PI) {
                carrierPhaseL -= 2.0 * Math.PI;
            }

            // Mix
            output[i] = sample * (1.0f - mixAmt) + modulated * mixAmt;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float baseFreq = carrierFreq.getValue();
        int waveform = carrierWaveform.getChoiceIndex();
        float lfoDepthAmt = lfoDepth.getValue() / 100.0f;
        float mixAmt = mix.getValue() / 100.0f;

        lfoL.setFrequency(lfoRate.getValue());
        lfoR.setFrequency(lfoRate.getValue());

        double phaseInc = 2.0 * Math.PI / sampleRate;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            float sampleL = inputL[i];
            float lfoModL = lfoL.tick();
            float freqL = baseFreq * (1.0f + lfoModL * lfoDepthAmt);
            freqL = Math.max(20.0f, Math.min(freqL, sampleRate / 2.0f - 1));
            float carrierL = generateCarrier(carrierPhaseL, waveform);
            float modulatedL = sampleL * carrierL;
            carrierPhaseL += phaseInc * freqL;
            if (carrierPhaseL >= 2.0 * Math.PI) carrierPhaseL -= 2.0 * Math.PI;
            outputL[i] = sampleL * (1.0f - mixAmt) + modulatedL * mixAmt;

            // Right channel
            float sampleR = inputR[i];
            float lfoModR = lfoR.tick();
            float freqR = baseFreq * (1.0f + lfoModR * lfoDepthAmt);
            freqR = Math.max(20.0f, Math.min(freqR, sampleRate / 2.0f - 1));
            float carrierR = generateCarrier(carrierPhaseR, waveform);
            float modulatedR = sampleR * carrierR;
            carrierPhaseR += phaseInc * freqR;
            if (carrierPhaseR >= 2.0 * Math.PI) carrierPhaseR -= 2.0 * Math.PI;
            outputR[i] = sampleR * (1.0f - mixAmt) + modulatedR * mixAmt;
        }
    }

    private float generateCarrier(double phase, int waveform) {
        return switch (waveform) {
            case 0 -> (float) Math.sin(phase);  // Sine
            case 1 -> phase < Math.PI ? 1.0f : -1.0f;  // Square
            case 2 -> (float) (2.0 * Math.abs(2.0 * (phase / (2.0 * Math.PI)) - 1.0) - 1.0);  // Triangle
            case 3 -> (float) (phase / Math.PI - 1.0);  // Sawtooth
            default -> (float) Math.sin(phase);
        };
    }

    @Override
    protected void onReset() {
        carrierPhaseL = 0;
        carrierPhaseR = 0;
        if (lfoL != null) lfoL.reset();
        if (lfoR != null) {
            lfoR.reset();
            lfoR.setPhase(0.25f);
        }
    }
}
