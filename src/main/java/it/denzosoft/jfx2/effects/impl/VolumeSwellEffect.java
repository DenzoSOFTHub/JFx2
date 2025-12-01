package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Volume Swell (Auto-Swell) effect.
 *
 * <p>Creates violin-like swells by automatically fading in the attack
 * of each note. Detects note onsets and applies a volume envelope
 * that gradually increases.</p>
 *
 * <p>Features:
 * - Adjustable attack time for swell speed
 * - Sensitivity control for onset detection
 * - Hold time to prevent re-triggering
 * - Smooth, musical volume curves</p>
 */
public class VolumeSwellEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "volumeswell",
            "Volume Swell",
            "Auto-swell for violin-like attack",
            EffectCategory.DYNAMICS
    );

    // Parameters
    private final Parameter attackParam;
    private final Parameter sensitivityParam;
    private final Parameter holdParam;
    private final Parameter curveParam;

    // State
    private float envelope = 0;
    private float swellGain = 0;
    private float lastLevel = 0;
    private int holdCounter = 0;
    private boolean swelling = false;

    public VolumeSwellEffect() {
        super(METADATA);

        // Attack time: 10 ms to 2000 ms, default 300 ms
        attackParam = addFloatParameter("attack", "Attack",
                "Time for the volume to rise. Longer = slower, more dramatic swell.",
                10.0f, 2000.0f, 300.0f, "ms");

        // Sensitivity: -60 dB to -20 dB, default -40 dB
        sensitivityParam = addFloatParameter("sensitivity", "Sensitivity",
                "Threshold for detecting note onset. Lower = more sensitive.",
                -60.0f, -20.0f, -40.0f, "dB");

        // Hold time: 50 ms to 500 ms, default 100 ms
        holdParam = addFloatParameter("hold", "Hold",
                "Minimum time between swell triggers.",
                50.0f, 500.0f, 100.0f, "ms");

        // Curve: 0.5 (log) to 2.0 (exp), default 1.0 (linear)
        curveParam = addFloatParameter("curve", "Curve",
                "Shape of the swell. <1 = slow start, >1 = fast start.",
                0.5f, 2.0f, 1.0f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        envelope = 0;
        swellGain = 0;
        lastLevel = 0;
        holdCounter = 0;
        swelling = false;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float attackMs = attackParam.getValue();
        float sensitivityLin = dbToLinear(sensitivityParam.getValue());
        float holdMs = holdParam.getValue();
        float curve = curveParam.getValue();

        // Calculate coefficients
        float attackSamples = attackMs * sampleRate / 1000.0f;
        float attackIncrement = 1.0f / attackSamples;
        int holdSamples = (int) (holdMs * sampleRate / 1000.0f);
        float envCoeff = (float) Math.exp(-1.0 / (0.005 * sampleRate)); // 5ms envelope

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];
            float absInput = Math.abs(sample);

            // Envelope follower for level detection
            if (absInput > envelope) {
                envelope = absInput;
            } else {
                envelope = envCoeff * envelope + (1 - envCoeff) * absInput;
            }

            // Onset detection
            if (holdCounter > 0) {
                holdCounter--;
            } else if (envelope > sensitivityLin && envelope > lastLevel * 2.0f) {
                // New note detected - start swell
                swellGain = 0;
                swelling = true;
                holdCounter = holdSamples;
            }
            lastLevel = envelope;

            // Update swell gain
            if (swelling) {
                swellGain += attackIncrement;
                if (swellGain >= 1.0f) {
                    swellGain = 1.0f;
                    swelling = false;
                }
            } else if (envelope < sensitivityLin * 0.5f) {
                // Signal dropped - allow new trigger
                swellGain = Math.max(0, swellGain - attackIncrement * 0.5f);
            }

            // Apply curve to swell
            float shapedGain = (float) Math.pow(swellGain, curve);

            output[i] = sample * shapedGain;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float attackMs = attackParam.getValue();
        float sensitivityLin = dbToLinear(sensitivityParam.getValue());
        float holdMs = holdParam.getValue();
        float curve = curveParam.getValue();

        float attackSamples = attackMs * sampleRate / 1000.0f;
        float attackIncrement = 1.0f / attackSamples;
        int holdSamples = (int) (holdMs * sampleRate / 1000.0f);
        float envCoeff = (float) Math.exp(-1.0 / (0.005 * sampleRate));

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];
            float absInput = Math.max(Math.abs(sampleL), Math.abs(sampleR));

            // Envelope follower
            if (absInput > envelope) {
                envelope = absInput;
            } else {
                envelope = envCoeff * envelope + (1 - envCoeff) * absInput;
            }

            // Onset detection
            if (holdCounter > 0) {
                holdCounter--;
            } else if (envelope > sensitivityLin && envelope > lastLevel * 2.0f) {
                swellGain = 0;
                swelling = true;
                holdCounter = holdSamples;
            }
            lastLevel = envelope;

            // Update swell gain
            if (swelling) {
                swellGain += attackIncrement;
                if (swellGain >= 1.0f) {
                    swellGain = 1.0f;
                    swelling = false;
                }
            } else if (envelope < sensitivityLin * 0.5f) {
                swellGain = Math.max(0, swellGain - attackIncrement * 0.5f);
            }

            float shapedGain = (float) Math.pow(swellGain, curve);

            outputL[i] = sampleL * shapedGain;
            outputR[i] = sampleR * shapedGain;
        }
    }

    @Override
    protected void onReset() {
        envelope = 0;
        swellGain = 0;
        lastLevel = 0;
        holdCounter = 0;
        swelling = false;
    }

    // Convenience setters
    public void setAttack(float ms) {
        attackParam.setValue(ms);
    }

    public void setSensitivity(float dB) {
        sensitivityParam.setValue(dB);
    }

    public void setHold(float ms) {
        holdParam.setValue(ms);
    }

    public void setCurve(float curve) {
        curveParam.setValue(curve);
    }
}
