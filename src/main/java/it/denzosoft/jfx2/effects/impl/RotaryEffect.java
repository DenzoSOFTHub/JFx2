package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Rotary Speaker (Leslie) simulation.
 *
 * <p>Emulates the sound of a Leslie rotating speaker cabinet,
 * with separate horn (treble) and drum (bass) rotors spinning
 * at different speeds.</p>
 *
 * <p>Features:
 * - Dual rotor simulation (horn and drum)
 * - Slow/Fast speed switching with acceleration
 * - Doppler pitch shift effect
 * - Amplitude modulation from rotating speakers
 * - Crossover for bass/treble split</p>
 */
public class RotaryEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "rotary",
            "Rotary",
            "Leslie rotating speaker simulation",
            EffectCategory.MODULATION
    );

    private static final String[] SPEED_NAMES = {
            "Slow",
            "Fast",
            "Brake"
    };

    // Rotor speeds (Hz)
    private static final float HORN_SLOW = 0.8f;
    private static final float HORN_FAST = 6.7f;
    private static final float DRUM_SLOW = 0.7f;
    private static final float DRUM_FAST = 5.8f;

    // Parameters
    private final Parameter speedParam;
    private final Parameter hornLevelParam;
    private final Parameter drumLevelParam;
    private final Parameter dopplerParam;
    private final Parameter mixParam;

    // DSP components
    private BiquadFilter crossoverLPL, crossoverLPR;
    private BiquadFilter crossoverHPL, crossoverHPR;
    private DelayLine hornDelayL, hornDelayR;
    private DelayLine drumDelayL, drumDelayR;

    // Rotor state
    private float hornPhaseL = 0, hornPhaseR = 0;
    private float drumPhaseL = 0, drumPhaseR = 0;
    private float currentHornSpeed = HORN_SLOW;
    private float currentDrumSpeed = DRUM_SLOW;

    public RotaryEffect() {
        super(METADATA);

        // Speed: Slow, Fast, Brake
        speedParam = addChoiceParameter("speed", "Speed",
                "Rotor speed: Slow = gentle, Fast = intense, Brake = stopping.",
                SPEED_NAMES, 0);

        // Horn level: 0% to 100%, default 100%
        hornLevelParam = addFloatParameter("horn", "Horn",
                "Level of the treble (horn) rotor. This is the characteristic Leslie sound.",
                0.0f, 100.0f, 100.0f, "%");

        // Drum level: 0% to 100%, default 70%
        drumLevelParam = addFloatParameter("drum", "Drum",
                "Level of the bass (drum) rotor. Adds warmth and movement to low frequencies.",
                0.0f, 100.0f, 70.0f, "%");

        // Doppler depth: 0% to 100%, default 50%
        dopplerParam = addFloatParameter("doppler", "Doppler",
                "Amount of pitch shift from Doppler effect. Higher = more pitch wobble.",
                0.0f, 100.0f, 50.0f, "%");

        // Mix: 0% to 100%, default 100%
        mixParam = addFloatParameter("mix", "Mix",
                "Blend between dry and rotary sound.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Crossover at 800 Hz
        float crossoverFreq = 800.0f;

        crossoverLPL = new BiquadFilter();
        crossoverLPL.setSampleRate(sampleRate);
        crossoverLPL.configure(FilterType.LOWPASS, crossoverFreq, 0.707f, 0.0f);

        crossoverLPR = new BiquadFilter();
        crossoverLPR.setSampleRate(sampleRate);
        crossoverLPR.configure(FilterType.LOWPASS, crossoverFreq, 0.707f, 0.0f);

        crossoverHPL = new BiquadFilter();
        crossoverHPL.setSampleRate(sampleRate);
        crossoverHPL.configure(FilterType.HIGHPASS, crossoverFreq, 0.707f, 0.0f);

        crossoverHPR = new BiquadFilter();
        crossoverHPR.setSampleRate(sampleRate);
        crossoverHPR.configure(FilterType.HIGHPASS, crossoverFreq, 0.707f, 0.0f);

        // Delay lines for Doppler effect (max 5ms)
        hornDelayL = new DelayLine(5.0f, sampleRate);
        hornDelayR = new DelayLine(5.0f, sampleRate);
        drumDelayL = new DelayLine(5.0f, sampleRate);
        drumDelayR = new DelayLine(5.0f, sampleRate);

        // Initialize phases with stereo offset
        hornPhaseL = 0;
        hornPhaseR = (float) Math.PI; // 180 degrees offset
        drumPhaseL = 0;
        drumPhaseR = (float) Math.PI;

        currentHornSpeed = HORN_SLOW;
        currentDrumSpeed = DRUM_SLOW;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // For mono, just process left
        float[] tempR = new float[frameCount];
        onProcessStereo(input, input, output, tempR, frameCount);
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        int speedMode = (int) speedParam.getValue();
        float hornLevel = hornLevelParam.getValue() / 100.0f;
        float drumLevel = drumLevelParam.getValue() / 100.0f;
        float doppler = dopplerParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Target speeds based on mode
        float targetHornSpeed, targetDrumSpeed;
        switch (speedMode) {
            case 1: // Fast
                targetHornSpeed = HORN_FAST;
                targetDrumSpeed = DRUM_FAST;
                break;
            case 2: // Brake
                targetHornSpeed = 0.0f;
                targetDrumSpeed = 0.0f;
                break;
            default: // Slow
                targetHornSpeed = HORN_SLOW;
                targetDrumSpeed = DRUM_SLOW;
                break;
        }

        // Acceleration/deceleration
        float accelCoeff = 0.9995f; // Slow acceleration
        float phaseInc = 1.0f / sampleRate;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Update rotor speeds with acceleration
            currentHornSpeed += (targetHornSpeed - currentHornSpeed) * (1 - accelCoeff);
            currentDrumSpeed += (targetDrumSpeed - currentDrumSpeed) * (1 - accelCoeff);

            // Update phases
            hornPhaseL += currentHornSpeed * phaseInc * 2 * (float) Math.PI;
            hornPhaseR += currentHornSpeed * phaseInc * 2 * (float) Math.PI;
            drumPhaseL += currentDrumSpeed * phaseInc * 2 * (float) Math.PI;
            drumPhaseR += currentDrumSpeed * phaseInc * 2 * (float) Math.PI;

            // Wrap phases
            if (hornPhaseL > 2 * Math.PI) hornPhaseL -= 2 * (float) Math.PI;
            if (hornPhaseR > 2 * Math.PI) hornPhaseR -= 2 * (float) Math.PI;
            if (drumPhaseL > 2 * Math.PI) drumPhaseL -= 2 * (float) Math.PI;
            if (drumPhaseR > 2 * Math.PI) drumPhaseR -= 2 * (float) Math.PI;

            // Split input with crossover
            float bassL = crossoverLPL.process(inputL[i]);
            float bassR = crossoverLPR.process(inputR[i]);
            float trebleL = crossoverHPL.process(inputL[i]);
            float trebleR = crossoverHPR.process(inputR[i]);

            // Horn (treble) processing
            float hornAmpL = 0.5f + 0.5f * (float) Math.sin(hornPhaseL);
            float hornAmpR = 0.5f + 0.5f * (float) Math.sin(hornPhaseR);

            // Doppler delay modulation for horn
            float hornDelayMsL = 1.0f + doppler * 2.0f * (float) Math.sin(hornPhaseL);
            float hornDelayMsR = 1.0f + doppler * 2.0f * (float) Math.sin(hornPhaseR);

            hornDelayL.write(trebleL);
            hornDelayR.write(trebleR);

            float hornL = hornDelayL.readCubic(hornDelayL.msToSamples(hornDelayMsL)) * hornAmpL;
            float hornR = hornDelayR.readCubic(hornDelayR.msToSamples(hornDelayMsR)) * hornAmpR;

            // Drum (bass) processing - slower, less Doppler
            float drumAmpL = 0.6f + 0.4f * (float) Math.sin(drumPhaseL);
            float drumAmpR = 0.6f + 0.4f * (float) Math.sin(drumPhaseR);

            float drumDelayMsL = 0.5f + doppler * 0.5f * (float) Math.sin(drumPhaseL);
            float drumDelayMsR = 0.5f + doppler * 0.5f * (float) Math.sin(drumPhaseR);

            drumDelayL.write(bassL);
            drumDelayR.write(bassR);

            float drumL = drumDelayL.readCubic(drumDelayL.msToSamples(drumDelayMsL)) * drumAmpL;
            float drumR = drumDelayR.readCubic(drumDelayR.msToSamples(drumDelayMsR)) * drumAmpR;

            // Combine with levels
            float wetL = hornL * hornLevel + drumL * drumLevel;
            float wetR = hornR * hornLevel + drumR * drumLevel;

            // Mix with dry
            outputL[i] = inputL[i] * (1 - mix) + wetL * mix;
            outputR[i] = inputR[i] * (1 - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (crossoverLPL != null) crossoverLPL.reset();
        if (crossoverLPR != null) crossoverLPR.reset();
        if (crossoverHPL != null) crossoverHPL.reset();
        if (crossoverHPR != null) crossoverHPR.reset();
        if (hornDelayL != null) hornDelayL.clear();
        if (hornDelayR != null) hornDelayR.clear();
        if (drumDelayL != null) drumDelayL.clear();
        if (drumDelayR != null) drumDelayR.clear();

        hornPhaseL = 0;
        hornPhaseR = (float) Math.PI;
        drumPhaseL = 0;
        drumPhaseR = (float) Math.PI;
        currentHornSpeed = HORN_SLOW;
        currentDrumSpeed = DRUM_SLOW;
    }

    // Convenience setters
    public void setSpeed(int mode) {
        speedParam.setValue(mode);
    }

    public void setHornLevel(float percent) {
        hornLevelParam.setValue(percent);
    }

    public void setDrumLevel(float percent) {
        drumLevelParam.setValue(percent);
    }

    public void setDoppler(float percent) {
        dopplerParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }
}
