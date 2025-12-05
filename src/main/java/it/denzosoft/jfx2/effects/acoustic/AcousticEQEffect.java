package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Acoustic EQ
 *
 * <p>5-band EQ tailored for acoustic guitar frequencies.
 * Bands are centered at key acoustic guitar frequencies:
 * Body (100Hz), Warmth (250Hz), Presence (1kHz),
 * Clarity (3kHz), Air (8kHz).</p>
 */
public class AcousticEQEffect extends AbstractEffect {

    private Parameter bodyParam;      // 100Hz - body resonance
    private Parameter warmthParam;    // 250Hz - warmth/fullness
    private Parameter presenceParam;  // 1kHz - presence/attack
    private Parameter clarityParam;   // 3kHz - clarity/definition
    private Parameter airParam;       // 8kHz - air/sparkle

    // Filter states
    private float body1 = 0, body2 = 0;
    private float warmth1 = 0, warmth2 = 0;
    private float presence1 = 0, presence2 = 0;
    private float clarity1 = 0, clarity2 = 0;
    private float air1 = 0, air2 = 0;

    public AcousticEQEffect() {
        super(EffectMetadata.of("acousticeq", "Acoustic EQ",
                "5-band EQ for acoustic guitar", EffectCategory.ACOUSTIC));

        bodyParam = addFloatParameter("body", "Body (100Hz)", "Low frequency body", 0f, 1f, 0.5f, "");
        warmthParam = addFloatParameter("warmth", "Warmth (250Hz)", "Low-mid warmth", 0f, 1f, 0.5f, "");
        presenceParam = addFloatParameter("presence", "Presence (1kHz)", "Midrange presence", 0f, 1f, 0.5f, "");
        clarityParam = addFloatParameter("clarity", "Clarity (3kHz)", "Upper-mid clarity", 0f, 1f, 0.5f, "");
        airParam = addFloatParameter("air", "Air (8kHz)", "High frequency air", 0f, 1f, 0.5f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        body1 = body2 = warmth1 = warmth2 = 0;
        presence1 = presence2 = clarity1 = clarity2 = 0;
        air1 = air2 = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Convert 0-1 to dB boost/cut (-12dB to +12dB)
        float bodyGain = (bodyParam.getValue() - 0.5f) * 24f;
        float warmthGain = (warmthParam.getValue() - 0.5f) * 24f;
        float presenceGain = (presenceParam.getValue() - 0.5f) * 24f;
        float clarityGain = (clarityParam.getValue() - 0.5f) * 24f;
        float airGain = (airParam.getValue() - 0.5f) * 24f;

        // Convert dB to linear
        float bodyLin = (float) Math.pow(10, bodyGain / 20f) - 1f;
        float warmthLin = (float) Math.pow(10, warmthGain / 20f) - 1f;
        float presenceLin = (float) Math.pow(10, presenceGain / 20f) - 1f;
        float clarityLin = (float) Math.pow(10, clarityGain / 20f) - 1f;
        float airLin = (float) Math.pow(10, airGain / 20f) - 1f;

        // Filter coefficients
        float bodyCoef = (float) Math.exp(-2.0 * Math.PI * 100f / sampleRate);
        float warmthCoef = (float) Math.exp(-2.0 * Math.PI * 250f / sampleRate);
        float presenceCoef = (float) Math.exp(-2.0 * Math.PI * 1000f / sampleRate);
        float clarityCoef = (float) Math.exp(-2.0 * Math.PI * 3000f / sampleRate);
        float airCoef = (float) Math.exp(-2.0 * Math.PI * 8000f / sampleRate);

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];

            // Body band (low shelf)
            body1 = body1 * bodyCoef + s * (1f - bodyCoef);
            body2 = body2 * bodyCoef + body1 * (1f - bodyCoef);
            float bodyBand = body2;

            // Warmth band (bandpass around 250Hz)
            warmth1 = warmth1 * warmthCoef + s * (1f - warmthCoef);
            warmth2 = warmth2 * warmthCoef + warmth1 * (1f - warmthCoef);
            float warmthBand = warmth1 - warmth2;

            // Presence band (bandpass around 1kHz)
            presence1 = presence1 * presenceCoef + s * (1f - presenceCoef);
            presence2 = presence2 * presenceCoef + presence1 * (1f - presenceCoef);
            float presenceBand = presence1 - presence2;

            // Clarity band (bandpass around 3kHz)
            clarity1 = clarity1 * clarityCoef + s * (1f - clarityCoef);
            clarity2 = clarity2 * clarityCoef + clarity1 * (1f - clarityCoef);
            float clarityBand = clarity1 - clarity2;

            // Air band (high shelf)
            air1 = air1 * airCoef + s * (1f - airCoef);
            float airBand = s - air1;

            // Apply EQ
            float eq = s;
            eq += bodyBand * bodyLin;
            eq += warmthBand * warmthLin;
            eq += presenceBand * presenceLin;
            eq += clarityBand * clarityLin;
            eq += airBand * airLin;

            // Soft limit
            if (Math.abs(eq) > 0.95f) {
                eq = (float) Math.tanh(eq);
            }

            output[i] = eq;
        }
    }
}
