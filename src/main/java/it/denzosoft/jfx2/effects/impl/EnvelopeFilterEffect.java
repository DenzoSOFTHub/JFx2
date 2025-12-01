package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Envelope Filter (Auto-Wah) effect.
 *
 * <p>A dynamic filter that responds to the input signal level,
 * creating wah-like sweeps automatically. Popular in funk and
 * disco guitar tones.</p>
 *
 * <p>Features:
 * - Envelope follower with adjustable attack/decay
 * - Variable filter type (lowpass, bandpass, highpass)
 * - Adjustable sensitivity and range
 * - Up or down sweep direction</p>
 */
public class EnvelopeFilterEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "envelopefilter",
            "Envelope Filter",
            "Auto-wah that responds to playing dynamics",
            EffectCategory.FILTER
    );

    private static final String[] FILTER_TYPES = {
            "Lowpass",
            "Bandpass",
            "Highpass"
    };

    private static final String[] DIRECTION_NAMES = {
            "Up",
            "Down"
    };

    // Parameters
    private final Parameter sensitivityParam;
    private final Parameter attackParam;
    private final Parameter decayParam;
    private final Parameter rangeParam;
    private final Parameter resonanceParam;
    private final Parameter typeParam;
    private final Parameter directionParam;
    private final Parameter mixParam;

    // DSP components
    private BiquadFilter filterL, filterR;

    // Envelope follower state
    private float envelopeL = 0;
    private float envelopeR = 0;

    public EnvelopeFilterEffect() {
        super(METADATA);

        // Sensitivity: -40 dB to 0 dB, default -20 dB
        sensitivityParam = addFloatParameter("sensitivity", "Sens",
                "Input sensitivity. Lower = more sensitive to soft playing.",
                -40.0f, 0.0f, -20.0f, "dB");

        // Attack: 1 ms to 50 ms, default 10 ms
        attackParam = addFloatParameter("attack", "Attack",
                "How fast the filter responds to picking.",
                1.0f, 50.0f, 10.0f, "ms");

        // Decay: 50 ms to 1000 ms, default 200 ms
        decayParam = addFloatParameter("decay", "Decay",
                "How fast the filter closes after the note.",
                50.0f, 1000.0f, 200.0f, "ms");

        // Range: 100 Hz to 4000 Hz, default 2000 Hz
        rangeParam = addFloatParameter("range", "Range",
                "Frequency sweep range. Higher = wider sweep.",
                100.0f, 4000.0f, 2000.0f, "Hz");

        // Resonance: 0.5 to 10, default 4
        resonanceParam = addFloatParameter("resonance", "Q",
                "Filter resonance. Higher = more pronounced wah sound.",
                0.5f, 10.0f, 4.0f, "");

        // Filter type
        typeParam = addChoiceParameter("type", "Type",
                "Filter type. Bandpass is classic wah, lowpass is smoother.",
                FILTER_TYPES, 1); // Default to bandpass

        // Direction
        directionParam = addChoiceParameter("direction", "Dir",
                "Sweep direction. Up = brighter when picking hard.",
                DIRECTION_NAMES, 0);

        // Mix: 0% to 100%, default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and filtered signal.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        filterL = new BiquadFilter();
        filterL.setSampleRate(sampleRate);

        filterR = new BiquadFilter();
        filterR.setSampleRate(sampleRate);

        envelopeL = 0;
        envelopeR = 0;
    }

    private FilterType getFilterType() {
        int type = (int) typeParam.getValue();
        return switch (type) {
            case 0 -> FilterType.LOWPASS;
            case 2 -> FilterType.HIGHPASS;
            default -> FilterType.BANDPASS;
        };
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float sensitivity = dbToLinear(sensitivityParam.getValue());
        float attackMs = attackParam.getValue();
        float decayMs = decayParam.getValue();
        float range = rangeParam.getValue();
        float resonance = resonanceParam.getValue();
        FilterType filterType = getFilterType();
        boolean directionUp = directionParam.getValue() < 0.5f;
        float mix = mixParam.getValue() / 100.0f;

        // Calculate envelope coefficients
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float decayCoeff = (float) Math.exp(-1.0 / (decayMs * sampleRate / 1000.0));

        // Base frequency (filter starts here)
        float baseFreq = 200.0f;
        float maxFreq = baseFreq + range;

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];
            float absInput = Math.abs(dry) / sensitivity;

            // Envelope follower
            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = decayCoeff * envelopeL;
            }

            // Clamp envelope
            float envValue = Math.min(1.0f, envelopeL);

            // Calculate filter frequency
            float filterFreq;
            if (directionUp) {
                filterFreq = baseFreq + envValue * range;
            } else {
                filterFreq = maxFreq - envValue * range;
            }
            filterFreq = Math.max(baseFreq, Math.min(maxFreq, filterFreq));

            // Update filter
            filterL.configure(filterType, filterFreq, resonance, 0.0f);

            // Process
            float wet = filterL.process(dry);

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float sensitivity = dbToLinear(sensitivityParam.getValue());
        float attackMs = attackParam.getValue();
        float decayMs = decayParam.getValue();
        float range = rangeParam.getValue();
        float resonance = resonanceParam.getValue();
        FilterType filterType = getFilterType();
        boolean directionUp = directionParam.getValue() < 0.5f;
        float mix = mixParam.getValue() / 100.0f;

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float decayCoeff = (float) Math.exp(-1.0 / (decayMs * sampleRate / 1000.0));

        float baseFreq = 200.0f;
        float maxFreq = baseFreq + range;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Use max of both channels for linked envelope
            float absInput = Math.max(Math.abs(dryL), Math.abs(dryR)) / sensitivity;

            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = decayCoeff * envelopeL;
            }

            float envValue = Math.min(1.0f, envelopeL);

            float filterFreq;
            if (directionUp) {
                filterFreq = baseFreq + envValue * range;
            } else {
                filterFreq = maxFreq - envValue * range;
            }
            filterFreq = Math.max(baseFreq, Math.min(maxFreq, filterFreq));

            filterL.configure(filterType, filterFreq, resonance, 0.0f);
            filterR.configure(filterType, filterFreq, resonance, 0.0f);

            float wetL = filterL.process(dryL);
            float wetR = filterR.process(dryR);

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (filterL != null) filterL.reset();
        if (filterR != null) filterR.reset();
        envelopeL = 0;
        envelopeR = 0;
    }

    // Convenience setters
    public void setSensitivity(float dB) { sensitivityParam.setValue(dB); }
    public void setAttack(float ms) { attackParam.setValue(ms); }
    public void setDecay(float ms) { decayParam.setValue(ms); }
    public void setRange(float hz) { rangeParam.setValue(hz); }
    public void setResonance(float q) { resonanceParam.setValue(q); }
    public void setType(int type) { typeParam.setValue(type); }
    public void setDirection(int dir) { directionParam.setValue(dir); }
    public void setMix(float percent) { mixParam.setValue(percent); }
}
