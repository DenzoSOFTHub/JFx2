package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Sustainer / E-Bow simulation effect.
 *
 * <p>Creates infinite sustain by detecting the input signal and
 * using aggressive compression and feedback to maintain note volume.
 * Emulates the effect of an E-Bow or Fernandes Sustainer system.</p>
 *
 * <p>Features:
 * - Envelope follower for dynamic compression
 * - Harmonic enhancement for richness
 * - Attack control for natural playing feel
 * - Tone shaping for different sustain characters</p>
 */
public class SustainerEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "sustainer",
            "Sustainer",
            "Infinite sustain simulation like E-Bow",
            EffectCategory.DYNAMICS
    );

    private static final String[] MODE_NAMES = {
            "Natural",      // Gentle compression, natural decay override
            "Harmonic",     // Adds upper harmonics
            "Fundamental"   // Emphasizes fundamental, reduces harmonics
    };

    // Parameters
    private final Parameter sustainParam;
    private final Parameter attackParam;
    private final Parameter toneParam;
    private final Parameter modeParam;
    private final Parameter mixParam;
    private final Parameter sensitivityParam;

    // Envelope follower
    private float envelopeL = 0;
    private float envelopeR = 0;

    // Target level for sustain
    private float targetLevel = 0.3f;

    // Smoothed gain
    private float currentGainL = 1.0f;
    private float currentGainR = 1.0f;

    // Filters
    private BiquadFilter toneFilterL, toneFilterR;
    private BiquadFilter harmonicFilterL, harmonicFilterR;
    private BiquadFilter lowCutL, lowCutR;

    public SustainerEffect() {
        super(METADATA);

        // Sustain: 0% to 100%, default 80%
        sustainParam = addFloatParameter("sustain", "Sustain",
                "Amount of sustain. Higher = longer notes, more compression.",
                0.0f, 100.0f, 80.0f, "%");

        // Attack: 1 ms to 100 ms, default 20 ms
        attackParam = addFloatParameter("attack", "Attack",
                "How fast sustain kicks in. Slower = more natural picking feel.",
                1.0f, 100.0f, 20.0f, "ms");

        // Tone: 500 Hz to 8000 Hz, default 3000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "Brightness of the sustained signal.",
                500.0f, 8000.0f, 3000.0f, "Hz");

        // Mode
        modeParam = addChoiceParameter("mode", "Mode",
                "Sustain character: Natural, Harmonic (adds overtones), Fundamental (pure).",
                MODE_NAMES, 0);

        // Mix: 0% to 100%, default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and sustained signal.",
                0.0f, 100.0f, 100.0f, "%");

        // Sensitivity: -40 dB to 0 dB, default -20 dB
        sensitivityParam = addFloatParameter("sensitivity", "Sens",
                "Input sensitivity. Lower = responds to softer playing.",
                -40.0f, 0.0f, -20.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        envelopeL = 0;
        envelopeR = 0;
        currentGainL = 1.0f;
        currentGainR = 1.0f;

        // Tone filter
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Harmonic boost filter (upper mids)
        harmonicFilterL = new BiquadFilter();
        harmonicFilterL.setSampleRate(sampleRate);
        harmonicFilterL.configure(FilterType.PEAK, 2500.0f, 1.5f, 6.0f);

        harmonicFilterR = new BiquadFilter();
        harmonicFilterR.setSampleRate(sampleRate);
        harmonicFilterR.configure(FilterType.PEAK, 2500.0f, 1.5f, 6.0f);

        // Low cut
        lowCutL = new BiquadFilter();
        lowCutL.setSampleRate(sampleRate);
        lowCutL.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        lowCutR = new BiquadFilter();
        lowCutR.setSampleRate(sampleRate);
        lowCutR.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float sustain = sustainParam.getValue() / 100.0f;
        float attackMs = attackParam.getValue();
        float toneFreq = toneParam.getValue();
        int mode = (int) modeParam.getValue();
        float mix = mixParam.getValue() / 100.0f;
        float sensitivity = dbToLinear(sensitivityParam.getValue());

        toneFilterL.setFrequency(toneFreq);

        // Envelope coefficients
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (200.0 * sampleRate / 1000.0)); // 200ms release

        // Target level scales with sustain
        float target = targetLevel * (0.5f + sustain * 0.5f);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];
            float absInput = Math.abs(dry);

            // Envelope follower
            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = releaseCoeff * envelopeL + (1 - releaseCoeff) * absInput;
            }

            // Calculate required gain to reach target
            float desiredGain = 1.0f;
            if (envelopeL > sensitivity * 0.1f) {
                // Signal is present - apply sustain
                desiredGain = target / Math.max(envelopeL, 0.001f);
                desiredGain = Math.min(desiredGain, 10.0f + sustain * 40.0f); // Limit max gain based on sustain
                desiredGain = Math.max(desiredGain, 0.1f);
            } else {
                // Signal too weak - let it decay naturally
                desiredGain = 1.0f;
            }

            // Smooth gain changes
            float gainSmoothCoeff = 0.999f;
            currentGainL = gainSmoothCoeff * currentGainL + (1 - gainSmoothCoeff) * desiredGain;

            // Apply gain
            float wet = dry * currentGainL * sustain + dry * (1 - sustain);

            // Apply mode processing
            switch (mode) {
                case 1: // Harmonic - boost upper harmonics
                    wet = harmonicFilterL.process(wet);
                    break;
                case 2: // Fundamental - lowpass to reduce harmonics
                    wet = toneFilterL.process(wet);
                    wet = toneFilterL.process(wet); // Double filter for steeper rolloff
                    break;
                default: // Natural - just tone control
                    wet = toneFilterL.process(wet);
                    break;
            }

            // Low cut to prevent rumble
            wet = lowCutL.process(wet);

            // Soft clip to prevent distortion
            if (wet > 0.9f) wet = 0.9f + 0.1f * (float) Math.tanh((wet - 0.9f) * 10);
            if (wet < -0.9f) wet = -0.9f - 0.1f * (float) Math.tanh((-wet - 0.9f) * 10);

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float sustain = sustainParam.getValue() / 100.0f;
        float attackMs = attackParam.getValue();
        float toneFreq = toneParam.getValue();
        int mode = (int) modeParam.getValue();
        float mix = mixParam.getValue() / 100.0f;
        float sensitivity = dbToLinear(sensitivityParam.getValue());

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (200.0 * sampleRate / 1000.0));

        float target = targetLevel * (0.5f + sustain * 0.5f);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Linked envelope (max of both channels)
            float absInput = Math.max(Math.abs(dryL), Math.abs(dryR));

            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = releaseCoeff * envelopeL + (1 - releaseCoeff) * absInput;
            }

            float desiredGain = 1.0f;
            if (envelopeL > sensitivity * 0.1f) {
                desiredGain = target / Math.max(envelopeL, 0.001f);
                desiredGain = Math.min(desiredGain, 10.0f + sustain * 40.0f);
                desiredGain = Math.max(desiredGain, 0.1f);
            }

            float gainSmoothCoeff = 0.999f;
            currentGainL = gainSmoothCoeff * currentGainL + (1 - gainSmoothCoeff) * desiredGain;

            float wetL = dryL * currentGainL * sustain + dryL * (1 - sustain);
            float wetR = dryR * currentGainL * sustain + dryR * (1 - sustain);

            switch (mode) {
                case 1:
                    wetL = harmonicFilterL.process(wetL);
                    wetR = harmonicFilterR.process(wetR);
                    break;
                case 2:
                    wetL = toneFilterL.process(wetL);
                    wetL = toneFilterL.process(wetL);
                    wetR = toneFilterR.process(wetR);
                    wetR = toneFilterR.process(wetR);
                    break;
                default:
                    wetL = toneFilterL.process(wetL);
                    wetR = toneFilterR.process(wetR);
                    break;
            }

            wetL = lowCutL.process(wetL);
            wetR = lowCutR.process(wetR);

            // Soft clip
            if (wetL > 0.9f) wetL = 0.9f + 0.1f * (float) Math.tanh((wetL - 0.9f) * 10);
            if (wetL < -0.9f) wetL = -0.9f - 0.1f * (float) Math.tanh((-wetL - 0.9f) * 10);
            if (wetR > 0.9f) wetR = 0.9f + 0.1f * (float) Math.tanh((wetR - 0.9f) * 10);
            if (wetR < -0.9f) wetR = -0.9f - 0.1f * (float) Math.tanh((-wetR - 0.9f) * 10);

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        envelopeL = 0;
        envelopeR = 0;
        currentGainL = 1.0f;
        currentGainR = 1.0f;
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (harmonicFilterL != null) harmonicFilterL.reset();
        if (harmonicFilterR != null) harmonicFilterR.reset();
        if (lowCutL != null) lowCutL.reset();
        if (lowCutR != null) lowCutR.reset();
    }

    // Convenience setters
    public void setSustain(float percent) { sustainParam.setValue(percent); }
    public void setAttack(float ms) { attackParam.setValue(ms); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setMode(int mode) { modeParam.setValue(mode); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setSensitivity(float dB) { sensitivityParam.setValue(dB); }
}
