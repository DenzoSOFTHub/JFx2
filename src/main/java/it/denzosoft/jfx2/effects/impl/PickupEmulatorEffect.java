package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Pickup Emulator
 *
 * <p>Transforms the sound of one pickup type into another using
 * sophisticated EQ curves. Allows a humbucker to sound like a
 * single coil, a piezo to sound like a mic'd acoustic, etc.</p>
 *
 * <p>Pickup types and their characteristics:</p>
 * <ul>
 *   <li>Humbucker: Warm, thick, less highs, prominent low-mids</li>
 *   <li>Single Coil: Bright, articulate, "quack", tight bass</li>
 *   <li>P90: Between HB and SC, gritty mids, fat tone</li>
 *   <li>Piezo: Very bright, quick transients, "quacky"</li>
 *   <li>Acoustic Mic: Natural, warm body, smooth highs, air</li>
 * </ul>
 */
public class PickupEmulatorEffect extends AbstractEffect {

    /**
     * Pickup type enumeration with EQ profiles.
     * Values represent relative dB adjustments at key frequencies:
     * [60Hz, 150Hz, 400Hz, 800Hz, 1.5kHz, 3kHz, 6kHz, 10kHz]
     */
    private enum PickupType {
        HUMBUCKER("Humbucker", new float[]{3, 4, 3, 1, 0, -2, -4, -6}),
        SINGLE_COIL("Single Coil", new float[]{-2, -1, 0, 2, 3, 4, 3, 2}),
        P90("P90", new float[]{1, 2, 2, 3, 2, 1, 0, -1}),
        PIEZO("Piezo", new float[]{-4, -2, 2, 4, 5, 6, 5, 4}),
        ACOUSTIC_MIC("Acoustic Mic", new float[]{2, 3, 1, 0, -1, 0, 2, 3}),
        TELE_BRIDGE("Tele Bridge", new float[]{-1, 0, 1, 3, 4, 5, 3, 2}),
        TELE_NECK("Tele Neck", new float[]{2, 3, 2, 0, -1, 0, 1, 0}),
        JAZZ_BOX("Jazz Box", new float[]{4, 5, 3, 1, -2, -4, -5, -6}),
        ACTIVE_HB("Active HB", new float[]{1, 2, 1, 2, 3, 3, 2, 1}),
        LIPSTICK("Lipstick", new float[]{-1, 0, 1, 2, 3, 4, 4, 3});

        final String displayName;
        final float[] eqProfile;

        PickupType(String displayName, float[] eqProfile) {
            this.displayName = displayName;
            this.eqProfile = eqProfile;
        }
    }

    private static final float[] BAND_FREQUENCIES = {60, 150, 400, 800, 1500, 3000, 6000, 10000};
    private static final int NUM_BANDS = 8;

    private Parameter sourceParam;
    private Parameter targetParam;
    private Parameter intensityParam;
    private Parameter mixParam;

    // Filter states for each band (2-pole for each)
    private float[][] bandStates;  // [band][state0, state1]
    private float[] bandCoefs;     // Pre-calculated coefficients
    private float[] currentEqGains; // Current EQ adjustment per band

    public PickupEmulatorEffect() {
        super(EffectMetadata.of("pickupemu", "Pickup Emulator",
                "Transform pickup type via EQ", EffectCategory.EQ));

        sourceParam = addFloatParameter("source", "Source",
                "HB/SC/P90/Piezo/Mic/TeleBr/TeleNk/Jazz/Active/Lip", 0f, 1f, 0f, "");
        targetParam = addFloatParameter("target", "Target",
                "HB/SC/P90/Piezo/Mic/TeleBr/TeleNk/Jazz/Active/Lip", 0f, 1f, 0.1f, "");
        intensityParam = addFloatParameter("intensity", "Intensity",
                "Effect intensity", 0f, 1f, 0.7f, "");
        mixParam = addFloatParameter("mix", "Mix",
                "Dry/Wet mix", 0f, 1f, 1f, "");

        bandStates = new float[NUM_BANDS][2];
        bandCoefs = new float[NUM_BANDS];
        currentEqGains = new float[NUM_BANDS];
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Pre-calculate band filter coefficients
        for (int i = 0; i < NUM_BANDS; i++) {
            bandCoefs[i] = (float) Math.exp(-2.0 * Math.PI * BAND_FREQUENCIES[i] / sampleRate);
        }
    }

    @Override
    protected void onReset() {
        for (int i = 0; i < NUM_BANDS; i++) {
            bandStates[i][0] = 0;
            bandStates[i][1] = 0;
            currentEqGains[i] = 0;
        }
    }

    /**
     * Get pickup type from parameter value (0-1 range).
     */
    private PickupType getPickupType(float value) {
        int index = (int)(value * (PickupType.values().length - 0.001f));
        index = Math.max(0, Math.min(index, PickupType.values().length - 1));
        return PickupType.values()[index];
    }

    /**
     * Calculate EQ gains needed to transform from source to target pickup.
     */
    private void calculateTransformEQ(PickupType source, PickupType target, float intensity) {
        for (int i = 0; i < NUM_BANDS; i++) {
            // Difference: what we need to add/remove
            float diff = target.eqProfile[i] - source.eqProfile[i];
            // Apply intensity
            currentEqGains[i] = diff * intensity;
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float source = sourceParam.getValue();
        float target = targetParam.getValue();
        float intensity = intensityParam.getValue();
        float mix = mixParam.getValue();

        PickupType sourceType = getPickupType(source);
        PickupType targetType = getPickupType(target);

        // Calculate the EQ transform
        calculateTransformEQ(sourceType, targetType, intensity);

        // Convert dB gains to linear multipliers
        float[] linearGains = new float[NUM_BANDS];
        for (int i = 0; i < NUM_BANDS; i++) {
            linearGains[i] = (float) Math.pow(10, currentEqGains[i] / 20.0);
        }

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];
            float dry = s;

            // Extract and process each frequency band
            float[] bandSignals = new float[NUM_BANDS];
            float processedSum = 0;

            // Band 0: Low shelf (everything below 60Hz)
            bandStates[0][0] = bandStates[0][0] * bandCoefs[0] + s * (1f - bandCoefs[0]);
            bandSignals[0] = bandStates[0][0];

            // Bands 1-6: Bandpass (difference between adjacent low-passes)
            for (int b = 1; b < NUM_BANDS - 1; b++) {
                bandStates[b][0] = bandStates[b][0] * bandCoefs[b] + s * (1f - bandCoefs[b]);
                bandStates[b][1] = bandStates[b][1] * bandCoefs[b] + bandStates[b][0] * (1f - bandCoefs[b]);

                // Bandpass = current LP - previous LP
                float prevLp = bandStates[b-1][0];
                bandSignals[b] = bandStates[b][0] - prevLp;
            }

            // Band 7: High shelf (everything above 10kHz)
            bandStates[NUM_BANDS-1][0] = bandStates[NUM_BANDS-1][0] * bandCoefs[NUM_BANDS-1]
                                        + s * (1f - bandCoefs[NUM_BANDS-1]);
            bandSignals[NUM_BANDS-1] = s - bandStates[NUM_BANDS-1][0];

            // Apply gains and sum
            for (int b = 0; b < NUM_BANDS; b++) {
                processedSum += bandSignals[b] * linearGains[b];
            }

            // Add back the mid content that might be missing
            // (compensate for imperfect band separation)
            float midCompensation = s * 0.3f;
            processedSum = processedSum * 0.7f + midCompensation;

            // Normalize to prevent level changes
            float normFactor = 0.9f;
            processedSum *= normFactor;

            // Soft limit
            if (Math.abs(processedSum) > 0.95f) {
                processedSum = (float) Math.tanh(processedSum);
            }

            // Mix
            output[i] = dry * (1f - mix) + processedSum * mix;
        }
    }

    /**
     * Get description of current transformation.
     */
    public String getTransformDescription() {
        PickupType source = getPickupType(sourceParam.getValue());
        PickupType target = getPickupType(targetParam.getValue());
        return source.displayName + " → " + target.displayName;
    }
}
