package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Wah pedal effect with multiple modes.
 *
 * <p>Modes:
 * - Auto-wah: Envelope follower controls filter sweep
 * - LFO: Automatic oscillation
 * - Manual: Fixed position (for external control)</p>
 *
 * <p>Uses a resonant bandpass filter (state variable filter)
 * to emulate classic wah pedal characteristics.</p>
 */
public class WahEffect extends AbstractEffect {

    // Parameters
    private Parameter mode;
    private Parameter position;
    private Parameter minFreq;
    private Parameter maxFreq;
    private Parameter resonance;
    private Parameter lfoRate;
    private Parameter sensitivity;
    private Parameter attackTime;
    private Parameter releaseTime;
    private Parameter mix;

    // State variable filter state - Left
    private float lpStateL;
    private float bpStateL;
    private float envelopeL;

    // State variable filter state - Right
    private float lpStateR;
    private float bpStateR;
    private float envelopeR;

    // Envelope coefficients
    private float envAttackCoeff;
    private float envReleaseCoeff;

    // LFOs
    private LFO lfoL;
    private LFO lfoR;

    public WahEffect() {
        super(EffectMetadata.of("wah", "Wah", "Wah pedal with auto-wah and LFO modes", EffectCategory.FILTER));
        initParameters();
    }

    private void initParameters() {
        mode = addChoiceParameter("mode", "Mode",
                "Control mode: Auto (dynamics-controlled), LFO (automatic sweep), Manual (fixed position).",
                new String[]{"Auto", "LFO", "Manual"}, 0);

        position = addFloatParameter("position", "Position",
                "Filter position in Manual mode. 0% = bass (toe up), 100% = treble (toe down).",
                0.0f, 100.0f, 50.0f, "%");

        minFreq = addFloatParameter("minFreq", "Min Freq",
                "Lowest frequency of the sweep range. Lower values extend the wah's bass response.",
                200.0f, 800.0f, 400.0f, "Hz");
        maxFreq = addFloatParameter("maxFreq", "Max Freq",
                "Highest frequency of the sweep range. Higher values add more treble bite.",
                1000.0f, 5000.0f, 2500.0f, "Hz");
        resonance = addFloatParameter("resonance", "Resonance",
                "Filter peak intensity. Higher values create more vocal, quacky wah tone.",
                1.0f, 20.0f, 8.0f, "");

        lfoRate = addFloatParameter("lfoRate", "LFO Rate",
                "Speed of automatic sweep in LFO mode. Slow for ambient, fast for funky rhythms.",
                0.1f, 10.0f, 1.0f, "Hz");

        sensitivity = addFloatParameter("sensitivity", "Sensitivity",
                "Response to playing dynamics in Auto mode. Higher = more reactive to attack.",
                0.0f, 100.0f, 50.0f, "%");
        attackTime = addFloatParameter("attack", "Attack",
                "How quickly the wah opens in Auto mode. Fast for percussive, slow for smooth.",
                1.0f, 100.0f, 10.0f, "ms");
        releaseTime = addFloatParameter("release", "Release",
                "How quickly the wah closes in Auto mode. Affects the decay of the effect.",
                10.0f, 500.0f, 100.0f, "ms");

        mix = addFloatParameter("mix", "Mix",
                "Balance between dry and wah-filtered signal.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        lpStateL = 0;
        bpStateL = 0;
        envelopeL = 0;
        lpStateR = 0;
        bpStateR = 0;
        envelopeR = 0;

        updateEnvelopeCoeffs();

        lfoL = new LFO(lfoRate.getValue(), sampleRate);
        lfoL.setWaveform(LFO.Waveform.SINE);
        lfoR = new LFO(lfoRate.getValue(), sampleRate);
        lfoR.setWaveform(LFO.Waveform.SINE);
        lfoR.setPhase(0.25f);  // Slight stereo offset
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int modeIdx = mode.getChoiceIndex();
        float pos = position.getValue() / 100.0f;
        float minF = minFreq.getValue();
        float maxF = maxFreq.getValue();
        float q = resonance.getValue();
        float sens = sensitivity.getValue() / 100.0f;
        float mixAmt = mix.getValue() / 100.0f;

        updateEnvelopeCoeffs();
        lfoL.setFrequency(lfoRate.getValue());

        for (int i = 0; i < frameCount; i++) {
            float sample = input[i];

            // Determine filter position based on mode
            float wahPos;
            switch (modeIdx) {
                case 0:  // Auto-wah
                    float level = Math.abs(sample);
                    if (level > envelopeL) {
                        envelopeL = envAttackCoeff * envelopeL + (1.0f - envAttackCoeff) * level;
                    } else {
                        envelopeL = envReleaseCoeff * envelopeL + (1.0f - envReleaseCoeff) * level;
                    }
                    wahPos = Math.min(envelopeL * sens * 10.0f, 1.0f);
                    break;
                case 1:  // LFO
                    wahPos = (lfoL.tick() + 1.0f) * 0.5f;
                    break;
                case 2:  // Manual
                default:
                    wahPos = pos;
                    break;
            }

            float freq = minF * (float) Math.pow(maxF / minF, wahPos);
            float f = 2.0f * (float) Math.sin(Math.PI * freq / sampleRate);
            f = Math.min(f, 0.99f);
            float fb = q + q / (1.0f - f);

            float hp = sample - lpStateL - fb * bpStateL;
            bpStateL += f * hp;
            bpStateL = Math.max(-1.5f, Math.min(1.5f, bpStateL));
            lpStateL += f * bpStateL;
            lpStateL = Math.max(-1.5f, Math.min(1.5f, lpStateL));

            float filtered = (float) Math.tanh(bpStateL * 1.5f);
            output[i] = sample * (1.0f - mixAmt) + filtered * mixAmt;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        int modeIdx = mode.getChoiceIndex();
        float pos = position.getValue() / 100.0f;
        float minF = minFreq.getValue();
        float maxF = maxFreq.getValue();
        float q = resonance.getValue();
        float sens = sensitivity.getValue() / 100.0f;
        float mixAmt = mix.getValue() / 100.0f;

        updateEnvelopeCoeffs();
        lfoL.setFrequency(lfoRate.getValue());
        lfoR.setFrequency(lfoRate.getValue());

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            float wahPosL, wahPosR;
            switch (modeIdx) {
                case 0:  // Auto-wah
                    float levelL = Math.abs(sampleL);
                    if (levelL > envelopeL) {
                        envelopeL = envAttackCoeff * envelopeL + (1.0f - envAttackCoeff) * levelL;
                    } else {
                        envelopeL = envReleaseCoeff * envelopeL + (1.0f - envReleaseCoeff) * levelL;
                    }
                    wahPosL = Math.min(envelopeL * sens * 10.0f, 1.0f);

                    float levelR = Math.abs(sampleR);
                    if (levelR > envelopeR) {
                        envelopeR = envAttackCoeff * envelopeR + (1.0f - envAttackCoeff) * levelR;
                    } else {
                        envelopeR = envReleaseCoeff * envelopeR + (1.0f - envReleaseCoeff) * levelR;
                    }
                    wahPosR = Math.min(envelopeR * sens * 10.0f, 1.0f);
                    break;
                case 1:  // LFO
                    wahPosL = (lfoL.tick() + 1.0f) * 0.5f;
                    wahPosR = (lfoR.tick() + 1.0f) * 0.5f;
                    break;
                case 2:  // Manual
                default:
                    wahPosL = pos;
                    wahPosR = pos;
                    break;
            }

            // Left channel
            float freqL = minF * (float) Math.pow(maxF / minF, wahPosL);
            float fL = 2.0f * (float) Math.sin(Math.PI * freqL / sampleRate);
            fL = Math.min(fL, 0.99f);
            float fbL = q + q / (1.0f - fL);
            float hpL = sampleL - lpStateL - fbL * bpStateL;
            bpStateL += fL * hpL;
            bpStateL = Math.max(-1.5f, Math.min(1.5f, bpStateL));
            lpStateL += fL * bpStateL;
            lpStateL = Math.max(-1.5f, Math.min(1.5f, lpStateL));
            float filteredL = (float) Math.tanh(bpStateL * 1.5f);
            outputL[i] = sampleL * (1.0f - mixAmt) + filteredL * mixAmt;

            // Right channel
            float freqR = minF * (float) Math.pow(maxF / minF, wahPosR);
            float fR = 2.0f * (float) Math.sin(Math.PI * freqR / sampleRate);
            fR = Math.min(fR, 0.99f);
            float fbR = q + q / (1.0f - fR);
            float hpR = sampleR - lpStateR - fbR * bpStateR;
            bpStateR += fR * hpR;
            bpStateR = Math.max(-1.5f, Math.min(1.5f, bpStateR));
            lpStateR += fR * bpStateR;
            lpStateR = Math.max(-1.5f, Math.min(1.5f, lpStateR));
            float filteredR = (float) Math.tanh(bpStateR * 1.5f);
            outputR[i] = sampleR * (1.0f - mixAmt) + filteredR * mixAmt;
        }
    }

    @Override
    protected void onReset() {
        lpStateL = 0;
        bpStateL = 0;
        envelopeL = 0;
        lpStateR = 0;
        bpStateR = 0;
        envelopeR = 0;
        if (lfoL != null) lfoL.reset();
        if (lfoR != null) {
            lfoR.reset();
            lfoR.setPhase(0.25f);
        }
    }

    private void updateEnvelopeCoeffs() {
        float attackMs = attackTime.getValue();
        float releaseMs = releaseTime.getValue();

        envAttackCoeff = (float) Math.exp(-1.0 / (sampleRate * attackMs / 1000.0));
        envReleaseCoeff = (float) Math.exp(-1.0 / (sampleRate * releaseMs / 1000.0));
    }
}
