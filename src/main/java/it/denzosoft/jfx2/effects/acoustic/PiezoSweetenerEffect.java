package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Piezo Sweetener
 *
 * <p>Reduces the harsh "quack" and brittleness common with
 * undersaddle piezo pickups. Softens transients, tames harsh
 * frequencies, and adds warmth for a more natural acoustic sound.</p>
 */
public class PiezoSweetenerEffect extends AbstractEffect {

    private Parameter quackReduceParam;  // Reduces piezo quack
    private Parameter warmthParam;       // Adds low-mid warmth
    private Parameter transientParam;    // Softens attack
    private Parameter presenceParam;     // High frequency presence
    private Parameter mixParam;

    // Filters
    private float lp1State = 0, lp2State = 0;  // Low-pass for warmth
    private float hp1State = 0;  // High-pass for quack reduction
    private float notchState1 = 0, notchState2 = 0;  // Notch for quack freq
    private float envState = 0;  // Envelope for transient control

    public PiezoSweetenerEffect() {
        super(EffectMetadata.of("piezosweetener", "Piezo Sweetener",
                "Reduces piezo harshness and quack", EffectCategory.ACOUSTIC));

        quackReduceParam = addFloatParameter("quack", "Quack Reduce", "Reduces piezo quack", 0f, 1f, 0.5f, "");
        warmthParam = addFloatParameter("warmth", "Warmth", "Adds low-mid warmth", 0f, 1f, 0.4f, "");
        transientParam = addFloatParameter("transient", "Transient", "Softens attack transients", 0f, 1f, 0.3f, "");
        presenceParam = addFloatParameter("presence", "Presence", "High frequency presence", 0f, 1f, 0.5f, "");
        mixParam = addFloatParameter("mix", "Mix", "Dry/Wet mix", 0f, 1f, 1f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        lp1State = lp2State = hp1State = 0;
        notchState1 = notchState2 = 0;
        envState = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float quackReduce = quackReduceParam.getValue();
        float warmth = warmthParam.getValue();
        float transient_ = transientParam.getValue();
        float presence = presenceParam.getValue();
        float mix = mixParam.getValue();

        // Quack frequency is typically 2-4kHz
        float quackFreq = 2500f;
        float quackCoef = (float) Math.exp(-2.0 * Math.PI * quackFreq / sampleRate);

        // Warmth adds around 200-400Hz
        float warmthFreq = 300f;
        float warmthCoef = (float) Math.exp(-2.0 * Math.PI * warmthFreq / sampleRate);

        // Transient envelope
        float attackTime = 0.001f + transient_ * 0.02f;  // 1-20ms
        float releaseTime = 0.05f;
        float attackCoef = (float) Math.exp(-1.0 / (sampleRate * attackTime));
        float releaseCoef = (float) Math.exp(-1.0 / (sampleRate * releaseTime));

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];
            float dry = s;

            // === Transient softening ===
            float envelope = Math.abs(s);
            if (envelope > envState) {
                envState = envState * attackCoef + envelope * (1f - attackCoef);
            } else {
                envState = envState * releaseCoef + envelope * (1f - releaseCoef);
            }

            // Soft clip transients
            if (transient_ > 0.01f && envelope > envState * 1.5f) {
                float excess = envelope - envState * 1.5f;
                float reduction = 1f - (excess * transient_ * 2f);
                reduction = Math.max(0.5f, reduction);
                s *= reduction;
            }

            // === Quack reduction (notch around 2.5kHz) ===
            // Two-pole notch filter
            float notchIn = s;
            notchState1 = notchState1 * quackCoef + notchIn * (1f - quackCoef);
            notchState2 = notchState2 * quackCoef + notchState1 * (1f - quackCoef);

            // Extract quack frequency
            float quackContent = notchState1 - notchState2;

            // Reduce quack
            s = s - quackContent * quackReduce * 0.7f;

            // === Warmth (low-mid boost) ===
            lp1State = lp1State * warmthCoef + s * (1f - warmthCoef);
            lp2State = lp2State * warmthCoef + lp1State * (1f - warmthCoef);

            // Add warmth
            float warmthBoost = lp2State * warmth * 0.5f;
            s = s + warmthBoost;

            // === Presence (high shelf) ===
            hp1State = hp1State * 0.9f + s * 0.1f;
            float highContent = s - hp1State;

            // Adjust presence
            s = s + highContent * (presence - 0.5f) * 0.5f;

            // === High frequency smoothing ===
            // Gentle roll-off above 8kHz to reduce piezo harshness
            float hfSmooth = s * 0.85f + lp1State * 0.15f;
            s = s * (1f - quackReduce * 0.3f) + hfSmooth * quackReduce * 0.3f;

            // Soft limit
            if (Math.abs(s) > 0.95f) {
                s = (float) Math.tanh(s);
            }

            // Mix
            output[i] = dry * (1f - mix) + s * mix;
        }
    }
}
