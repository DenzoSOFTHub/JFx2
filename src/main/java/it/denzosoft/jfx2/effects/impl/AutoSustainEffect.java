package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Auto-Sustain effect with threshold-based level control.
 *
 * <p>Maintains constant output volume while the input signal is above a threshold,
 * then applies a configurable decay time when the signal falls below.</p>
 *
 * <p>Features:
 * - Threshold detection to trigger sustain/decay modes
 * - Configurable sustain level (target output)
 * - Hold time before decay starts
 * - Adjustable decay time
 * - Tone shaping</p>
 *
 * <p>Use cases:
 * - Create pad-like sustain from guitar
 * - Smooth volume swells
 * - Consistent note levels for recording
 * - Ambient/drone textures</p>
 */
public class AutoSustainEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "autosustain",
            "Auto Sustain",
            "Threshold-based sustain with configurable decay",
            EffectCategory.DYNAMICS
    );

    // === ROW 1: Detection Parameters ===
    private final Parameter thresholdParam;
    private final Parameter sensitivityParam;

    // === ROW 2: Sustain Parameters ===
    private final Parameter levelParam;
    private final Parameter attackParam;

    // === ROW 3: Decay Parameters ===
    private final Parameter holdParam;
    private final Parameter decayParam;

    // === ROW 4: Output Parameters ===
    private final Parameter toneParam;
    private final Parameter mixParam;

    // DSP state
    private float envelopeL = 0;
    private float envelopeR = 0;
    private float currentGainL = 1.0f;
    private float currentGainR = 1.0f;
    private float decayGainL = 1.0f;
    private float decayGainR = 1.0f;

    // Gate state
    private boolean gateOpenL = false;
    private boolean gateOpenR = false;
    private int holdCounterL = 0;
    private int holdCounterR = 0;

    // Filters
    private BiquadFilter toneFilterL, toneFilterR;
    private BiquadFilter lowCutL, lowCutR;

    public AutoSustainEffect() {
        super(METADATA);

        // === ROW 1: Detection ===
        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Signal level above which sustain is active. Below this, decay begins.",
                -60.0f, 0.0f, -40.0f, "dB");

        sensitivityParam = addFloatParameter("sens", "Sensitivity",
                "How fast the level detector responds to changes.",
                1.0f, 100.0f, 10.0f, "ms");

        // === ROW 2: Sustain ===
        levelParam = addFloatParameter("level", "Level",
                "Target output level to maintain while signal is above threshold.",
                0.0f, 100.0f, 70.0f, "%");

        attackParam = addFloatParameter("attack", "Attack",
                "How fast the gain increases to reach sustain level.",
                1.0f, 500.0f, 50.0f, "ms");

        // === ROW 3: Decay ===
        holdParam = addFloatParameter("hold", "Hold",
                "Time to wait before starting decay after signal drops below threshold.",
                0.0f, 2000.0f, 100.0f, "ms");

        decayParam = addFloatParameter("decay", "Decay",
                "Time for the sound to fade to silence after hold period.",
                100.0f, 10000.0f, 2000.0f, "ms");

        // === ROW 4: Output ===
        toneParam = addFloatParameter("tone", "Tone",
                "Brightness of the sustained signal.",
                200.0f, 10000.0f, 5000.0f, "Hz");

        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry (original) and wet (sustained) signal.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        envelopeL = 0;
        envelopeR = 0;
        currentGainL = 1.0f;
        currentGainR = 1.0f;
        decayGainL = 1.0f;
        decayGainR = 1.0f;
        gateOpenL = false;
        gateOpenR = false;
        holdCounterL = 0;
        holdCounterR = 0;

        // Tone filter (lowpass)
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Low cut to prevent rumble
        lowCutL = new BiquadFilter();
        lowCutL.setSampleRate(sampleRate);
        lowCutL.configure(FilterType.HIGHPASS, 40.0f, 0.707f, 0.0f);

        lowCutR = new BiquadFilter();
        lowCutR.setSampleRate(sampleRate);
        lowCutR.configure(FilterType.HIGHPASS, 40.0f, 0.707f, 0.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float thresholdLin = dbToLinear(thresholdParam.getValue());
        float sensMs = sensitivityParam.getValue();
        float targetLevel = levelParam.getValue() / 100.0f * 0.7f; // Scale to reasonable output
        float attackMs = attackParam.getValue();
        float holdMs = holdParam.getValue();
        float decayMs = decayParam.getValue();
        float toneFreq = toneParam.getValue();
        float mix = mixParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);

        // Time constants
        float envCoeff = (float) Math.exp(-1.0 / (sensMs * sampleRate / 1000.0));
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float decayCoeff = (float) Math.exp(-1.0 / (decayMs * sampleRate / 1000.0));
        int holdSamples = (int) (holdMs * sampleRate / 1000.0f);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];
            float absInput = Math.abs(dry);

            // Envelope follower
            if (absInput > envelopeL) {
                envelopeL = envCoeff * envelopeL + (1 - envCoeff) * absInput;
            } else {
                envelopeL = envCoeff * envelopeL + (1 - envCoeff) * absInput;
            }

            // Gate logic
            if (envelopeL > thresholdLin) {
                // Signal above threshold - sustain mode
                gateOpenL = true;
                holdCounterL = holdSamples;
                decayGainL = 1.0f; // Reset decay

                // Calculate gain to reach target level
                float desiredGain = targetLevel / Math.max(envelopeL, 0.0001f);
                desiredGain = Math.min(desiredGain, 20.0f); // Limit max gain to +26dB
                desiredGain = Math.max(desiredGain, 0.1f);

                // Smooth attack
                currentGainL = attackCoeff * currentGainL + (1 - attackCoeff) * desiredGain;
            } else {
                // Signal below threshold
                if (holdCounterL > 0) {
                    // Still in hold period - maintain current gain
                    holdCounterL--;
                } else {
                    // Hold expired - start decay
                    gateOpenL = false;
                    decayGainL = decayCoeff * decayGainL;
                }
            }

            // Apply gain
            float effectiveGain = gateOpenL ? currentGainL : currentGainL * decayGainL;
            float wet = dry * effectiveGain;

            // Apply tone filter
            wet = toneFilterL.process(wet);

            // Low cut
            wet = lowCutL.process(wet);

            // Soft clip
            wet = softClip(wet);

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float thresholdLin = dbToLinear(thresholdParam.getValue());
        float sensMs = sensitivityParam.getValue();
        float targetLevel = levelParam.getValue() / 100.0f * 0.7f;
        float attackMs = attackParam.getValue();
        float holdMs = holdParam.getValue();
        float decayMs = decayParam.getValue();
        float toneFreq = toneParam.getValue();
        float mix = mixParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        float envCoeff = (float) Math.exp(-1.0 / (sensMs * sampleRate / 1000.0));
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float decayCoeff = (float) Math.exp(-1.0 / (decayMs * sampleRate / 1000.0));
        int holdSamples = (int) (holdMs * sampleRate / 1000.0f);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Linked stereo envelope (use max of both channels)
            float absInput = Math.max(Math.abs(dryL), Math.abs(dryR));

            // Envelope follower
            if (absInput > envelopeL) {
                envelopeL = envCoeff * envelopeL + (1 - envCoeff) * absInput;
            } else {
                envelopeL = envCoeff * envelopeL + (1 - envCoeff) * absInput;
            }

            // Gate logic (linked stereo)
            if (envelopeL > thresholdLin) {
                gateOpenL = true;
                holdCounterL = holdSamples;
                decayGainL = 1.0f;

                float desiredGain = targetLevel / Math.max(envelopeL, 0.0001f);
                desiredGain = Math.min(desiredGain, 20.0f);
                desiredGain = Math.max(desiredGain, 0.1f);

                currentGainL = attackCoeff * currentGainL + (1 - attackCoeff) * desiredGain;
            } else {
                if (holdCounterL > 0) {
                    holdCounterL--;
                } else {
                    gateOpenL = false;
                    decayGainL = decayCoeff * decayGainL;
                }
            }

            // Apply gain (same for both channels - linked)
            float effectiveGain = gateOpenL ? currentGainL : currentGainL * decayGainL;
            float wetL = dryL * effectiveGain;
            float wetR = dryR * effectiveGain;

            // Apply tone filter
            wetL = toneFilterL.process(wetL);
            wetR = toneFilterR.process(wetR);

            // Low cut
            wetL = lowCutL.process(wetL);
            wetR = lowCutR.process(wetR);

            // Soft clip
            wetL = softClip(wetL);
            wetR = softClip(wetR);

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    /**
     * Soft clip to prevent harsh distortion.
     */
    private float softClip(float x) {
        if (x > 0.9f) {
            return 0.9f + 0.1f * (float) Math.tanh((x - 0.9f) * 10);
        } else if (x < -0.9f) {
            return -0.9f - 0.1f * (float) Math.tanh((-x - 0.9f) * 10);
        }
        return x;
    }

    @Override
    protected void onReset() {
        envelopeL = 0;
        envelopeR = 0;
        currentGainL = 1.0f;
        currentGainR = 1.0f;
        decayGainL = 1.0f;
        decayGainR = 1.0f;
        gateOpenL = false;
        gateOpenR = false;
        holdCounterL = 0;
        holdCounterR = 0;

        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (lowCutL != null) lowCutL.reset();
        if (lowCutR != null) lowCutR.reset();
    }

    // Convenience setters
    public void setThreshold(float dB) { thresholdParam.setValue(dB); }
    public void setSensitivity(float ms) { sensitivityParam.setValue(ms); }
    public void setLevel(float percent) { levelParam.setValue(percent); }
    public void setAttack(float ms) { attackParam.setValue(ms); }
    public void setHold(float ms) { holdParam.setValue(ms); }
    public void setDecay(float ms) { decayParam.setValue(ms); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setMix(float percent) { mixParam.setValue(percent); }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Detection (2): threshold, sens
        // Row 2: Sustain (2): level, attack
        // Row 3: Decay (2): hold, decay
        // Row 4: Output (2): tone, mix
        return new int[] {2, 2, 2, 2};
    }
}
