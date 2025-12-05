package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Body Resonance Simulator
 *
 * <p>Simulates the natural resonance and air movement of an acoustic
 * guitar body. Transforms piezo pickup sound to be more like a
 * microphone recording. Adds warmth, depth, and natural response.</p>
 */
public class BodyResonanceEffect extends AbstractEffect {

    private Parameter bodyTypeParam;  // Small/Medium/Large/Jumbo
    private Parameter resonanceParam;
    private Parameter topParam;  // Soundboard brightness
    private Parameter airParam;  // Air/room simulation
    private Parameter mixParam;

    // Resonant filters for body simulation
    private float lf1State = 0, lf2State = 0;  // Low frequency resonance
    private float mf1State = 0, mf2State = 0;  // Mid frequency body
    private float hfState = 0;  // High frequency air
    private float[] irBuffer;  // Simple impulse response
    private int irPos = 0;

    private static final int IR_SIZE = 512;

    public BodyResonanceEffect() {
        super(EffectMetadata.of("bodyresonance", "Body Resonance",
                "Acoustic body simulation for piezo pickups", EffectCategory.ACOUSTIC));

        bodyTypeParam = addFloatParameter("body", "Body Type", "Small/Medium/Large/Jumbo", 0f, 1f, 0.5f, "");
        resonanceParam = addFloatParameter("resonance", "Resonance", "Body resonance amount", 0f, 1f, 0.5f, "");
        topParam = addFloatParameter("top", "Top", "Soundboard brightness", 0f, 1f, 0.5f, "");
        airParam = addFloatParameter("air", "Air", "Air/room character", 0f, 1f, 0.3f, "");
        mixParam = addFloatParameter("mix", "Mix", "Dry/Wet mix", 0f, 1f, 0.7f, "");

        irBuffer = new float[IR_SIZE];
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        lf1State = lf2State = mf1State = mf2State = hfState = 0;
        irPos = 0;
        java.util.Arrays.fill(irBuffer, 0);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float bodyType = bodyTypeParam.getValue();
        float resonance = resonanceParam.getValue();
        float top = topParam.getValue();
        float air = airParam.getValue();
        float mix = mixParam.getValue();

        // Body type affects resonant frequencies
        // Small: higher freq, Large: lower freq
        float bodyFreq = 120f - bodyType * 40f;  // 120Hz (small) to 80Hz (jumbo)
        float bodyQ = 0.5f + resonance * 1.5f;

        // Soundboard resonance (around 200-400Hz)
        float topFreq = 280f + (1f - top) * 120f;

        // Calculate filter coefficients
        float lfCoef = (float) Math.exp(-2.0 * Math.PI * bodyFreq / sampleRate);
        float mfCoef = (float) Math.exp(-2.0 * Math.PI * topFreq / sampleRate);
        float hfCoef = (float) Math.exp(-2.0 * Math.PI * 4000f / sampleRate);

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];

            // Store in IR buffer for subtle convolution
            irBuffer[irPos] = s;

            // Low frequency body resonance (2-pole for resonance)
            float lfIn = s;
            lf1State = lf1State * lfCoef + lfIn * (1f - lfCoef);
            lf2State = lf2State * lfCoef + lf1State * (1f - lfCoef);
            float bodyLow = lf2State * bodyQ;

            // Mid frequency body/soundboard
            mf1State = mf1State * mfCoef + s * (1f - mfCoef);
            mf2State = mf2State * mfCoef + mf1State * (1f - mfCoef);
            float bodyMid = (mf1State - mf2State) * (1f + top);

            // High frequency "air" and sparkle
            hfState = hfState * hfCoef + s * (1f - hfCoef);
            float bodyHigh = (s - hfState) * top * 0.5f;

            // Simple room/air simulation using delayed samples
            float airSim = 0;
            if (air > 0.01f) {
                int delay1 = (int)(sampleRate * 0.003f);  // 3ms
                int delay2 = (int)(sampleRate * 0.007f);  // 7ms
                int pos1 = (irPos - delay1 + IR_SIZE) % IR_SIZE;
                int pos2 = (irPos - delay2 + IR_SIZE) % IR_SIZE;
                airSim = (irBuffer[pos1] * 0.3f + irBuffer[pos2] * 0.2f) * air;
            }

            irPos = (irPos + 1) % IR_SIZE;

            // Combine body simulation
            float bodySound = bodyLow * 0.4f + bodyMid * 0.4f + bodyHigh * 0.2f + airSim;

            // Add subtle resonance feedback
            bodySound += bodySound * resonance * 0.1f;

            // Soft limit
            if (Math.abs(bodySound) > 0.9f) {
                bodySound = (float) Math.tanh(bodySound);
            }

            // Mix with original
            output[i] = s * (1f - mix) + bodySound * mix;
        }
    }
}
