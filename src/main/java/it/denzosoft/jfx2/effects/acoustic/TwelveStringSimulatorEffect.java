package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * 12-String Guitar Simulator
 *
 * <p>Simulates the rich, shimmering sound of a 12-string guitar.
 * Adds octave-up doubling for high strings and unison doubling
 * for low strings with subtle detuning for the characteristic
 * chorus effect.</p>
 */
public class TwelveStringSimulatorEffect extends AbstractEffect {

    private Parameter octaveParam;  // Octave string level
    private Parameter detuneParam;  // Detune amount
    private Parameter shimmerParam; // High frequency shimmer
    private Parameter lowStringsParam; // Low string doubling
    private Parameter mixParam;

    // Delay lines for doubling
    private float[] delayLine1;
    private float[] delayLine2;
    private int delayPos = 0;

    // Pitch shifting states
    private float phase1 = 0, phase2 = 0;
    private float lpState = 0, hpState = 0;

    private static final int DELAY_SIZE = 2048;

    public TwelveStringSimulatorEffect() {
        super(EffectMetadata.of("12string", "12-String Simulator",
                "Simulates 12-string guitar sound", EffectCategory.ACOUSTIC));

        octaveParam = addFloatParameter("octave", "Octave", "Octave string level", 0f, 1f, 0.6f, "");
        detuneParam = addFloatParameter("detune", "Detune", "String pair detuning", 0f, 1f, 0.3f, "");
        shimmerParam = addFloatParameter("shimmer", "Shimmer", "High frequency shimmer", 0f, 1f, 0.5f, "");
        lowStringsParam = addFloatParameter("lowstrings", "Low Strings", "Low string unison doubling", 0f, 1f, 0.4f, "");
        mixParam = addFloatParameter("mix", "Mix", "Dry/Wet mix", 0f, 1f, 0.7f, "");

        delayLine1 = new float[DELAY_SIZE];
        delayLine2 = new float[DELAY_SIZE];
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        java.util.Arrays.fill(delayLine1, 0);
        java.util.Arrays.fill(delayLine2, 0);
        delayPos = 0;
        phase1 = phase2 = 0;
        lpState = hpState = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float octave = octaveParam.getValue();
        float detune = detuneParam.getValue();
        float shimmer = shimmerParam.getValue();
        float lowStrings = lowStringsParam.getValue();
        float mix = mixParam.getValue();

        // Detune in cents (0-15 cents typical for 12-string)
        float detuneCents = detune * 15f;
        float detuneRatio = (float) Math.pow(2.0, detuneCents / 1200.0);

        // Octave up ratio
        float octaveRatio = 2.0f;

        // Crossover frequency to separate low/high strings
        float crossoverCoef = (float) Math.exp(-2.0 * Math.PI * 300f / sampleRate);

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];

            // Store in delay lines
            delayLine1[delayPos] = s;
            delayLine2[delayPos] = s;

            // Crossover filter: separate low and high frequencies
            lpState = lpState * crossoverCoef + s * (1f - crossoverCoef);
            float lowFreq = lpState;
            float highFreq = s - lpState;

            // === HIGH STRINGS: Octave up simulation ===
            // Simple pitch shift using variable delay
            phase1 += octaveRatio;
            if (phase1 >= DELAY_SIZE / 2) phase1 -= DELAY_SIZE / 2;

            int readPos1 = (int)((delayPos - phase1 + DELAY_SIZE) % DELAY_SIZE);
            float octaveUp = delayLine1[readPos1];

            // Window for smooth looping
            float window1 = (float) Math.sin((phase1 / (DELAY_SIZE / 2)) * Math.PI);
            octaveUp *= window1;

            // Add slight detune to octave string
            phase2 += octaveRatio * detuneRatio;
            if (phase2 >= DELAY_SIZE / 2) phase2 -= DELAY_SIZE / 2;

            int readPos2 = (int)((delayPos - phase2 + DELAY_SIZE) % DELAY_SIZE);
            float octaveUp2 = delayLine2[readPos2];
            float window2 = (float) Math.cos((phase2 / (DELAY_SIZE / 2)) * Math.PI);
            octaveUp2 *= Math.abs(window2);

            // Combine octave strings (like a real 12-string high pair)
            float octaveString = (octaveUp + octaveUp2) * 0.5f * octave;

            // Apply shimmer (high-shelf boost to octave)
            hpState = hpState * 0.95f + octaveString * 0.05f;
            octaveString = octaveString + (octaveString - hpState) * shimmer * 0.5f;

            // === LOW STRINGS: Unison doubling with detune ===
            // Simple chorus-like doubling for low strings
            int delayTime = (int)(sampleRate * 0.008f);  // 8ms
            float modulation = (float) Math.sin(phase1 * 0.01f) * detune * 5f;
            int lowDelayPos = (int)((delayPos - delayTime - modulation + DELAY_SIZE) % DELAY_SIZE);
            float lowDouble = delayLine1[lowDelayPos];

            float lowStringDouble = lowFreq * 0.5f + lowDouble * 0.5f * lowStrings;

            // === Combine all ===
            // High frequencies get octave up, low frequencies get unison double
            float twelveString = lowStringDouble + highFreq + octaveString * (highFreq != 0 ? 1f : 0.3f);

            // Normalize
            twelveString *= 0.7f;

            delayPos = (delayPos + 1) % DELAY_SIZE;

            // Mix
            output[i] = s * (1f - mix) + twelveString * mix;
        }
    }
}
