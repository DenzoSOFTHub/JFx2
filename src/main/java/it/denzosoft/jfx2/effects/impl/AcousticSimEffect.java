package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Acoustic Guitar Simulator Effect.
 *
 * <p>Transforms electric guitar signal into various acoustic guitar tones
 * by applying body resonance simulation, EQ shaping, and transient enhancement.</p>
 *
 * <p>Available models:</p>
 * <ul>
 *   <li>Dreadnought - Classic full-size acoustic, balanced tone</li>
 *   <li>Jumbo - Large body, enhanced bass response</li>
 *   <li>Parlor - Small vintage body, midrange focus</li>
 *   <li>Concert - Balanced, articulate fingerpicking tone</li>
 *   <li>Nylon - Classical guitar, warm and mellow</li>
 *   <li>12-String - Shimmering doubled strings effect</li>
 *   <li>Mandolin - Bright, octave-shifted character</li>
 *   <li>Resonator - Metallic dobro-style resonance</li>
 *   <li>Banjo - Bright, plucky attack</li>
 *   <li>Ukulele - Small body, high and bright</li>
 * </ul>
 */
public class AcousticSimEffect extends AbstractEffect {

    // Parameters
    private Parameter model;
    private Parameter body;
    private Parameter top;
    private Parameter attack;
    private Parameter warmth;
    private Parameter air;
    private Parameter blend;

    // Internal DSP state
    // Body resonance filters (biquad state: x[n-1], x[n-2], y[n-1], y[n-2])
    private double[] bodyFilter1State = new double[4];
    private double[] bodyFilter2State = new double[4];
    private double[] bodyFilter3State = new double[4];

    // EQ filters
    private double[] lowShelfState = new double[4];
    private double[] midPeakState = new double[4];
    private double[] highShelfState = new double[4];

    // Transient detector
    private float envelope = 0;
    private float prevSample = 0;

    // 12-string chorus
    private float[] chorusBuffer;
    private int chorusWritePos = 0;
    private float chorusPhase = 0;

    // Resonator comb filter
    private float[] combBuffer;
    private int combWritePos = 0;

    // Sample rate for filter calculations
    private int currentSampleRate = 44100;

    public AcousticSimEffect() {
        super(EffectMetadata.of("acousticsim", "Acoustic Sim",
                "Acoustic guitar body simulation with multiple models", EffectCategory.FILTER));
        initParameters();
    }

    private void initParameters() {
        model = addChoiceParameter("model", "Model",
                "Select acoustic guitar model type",
                new String[]{"Dreadnought", "Jumbo", "Parlor", "Concert", "Nylon",
                        "12-String", "Mandolin", "Resonator", "Banjo", "Ukulele"}, 0);

        body = addFloatParameter("body", "Body",
                "Body resonance amount - simulates the acoustic chamber",
                0.0f, 100.0f, 60.0f, "%");

        top = addFloatParameter("top", "Top",
                "Top brightness - controls the high frequency character",
                0.0f, 100.0f, 50.0f, "%");

        attack = addFloatParameter("attack", "Attack",
                "Transient enhancement - emphasizes pick attack",
                0.0f, 100.0f, 50.0f, "%");

        warmth = addFloatParameter("warmth", "Warmth",
                "Low-end warmth - adds bass body",
                0.0f, 100.0f, 50.0f, "%");

        air = addFloatParameter("air", "Air",
                "High frequency air/presence",
                0.0f, 100.0f, 30.0f, "%");

        blend = addFloatParameter("blend", "Blend",
                "Wet/dry mix",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.currentSampleRate = sampleRate;

        // Reset filter states
        bodyFilter1State = new double[4];
        bodyFilter2State = new double[4];
        bodyFilter3State = new double[4];
        lowShelfState = new double[4];
        midPeakState = new double[4];
        highShelfState = new double[4];

        // Initialize buffers
        chorusBuffer = new float[sampleRate / 10];  // ~100ms buffer
        combBuffer = new float[sampleRate / 50];    // ~20ms buffer
        chorusWritePos = 0;
        combWritePos = 0;
        chorusPhase = 0;
        envelope = 0;
        prevSample = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int currentModel = model.getChoiceIndex();
        float bodyAmt = body.getValue() / 100.0f;
        float topAmt = top.getValue() / 100.0f;
        float attackAmt = attack.getValue() / 100.0f;
        float warmthAmt = warmth.getValue() / 100.0f;
        float airAmt = air.getValue() / 100.0f;
        float blendAmt = blend.getValue() / 100.0f;

        for (int i = 0; i < frameCount; i++) {
            float in = input[i];
            float processed = 0;

            // Apply model-specific processing
            switch (currentModel) {
                case 0 -> processed = processDreadnought(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 1 -> processed = processJumbo(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 2 -> processed = processParlor(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 3 -> processed = processConcert(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 4 -> processed = processNylon(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 5 -> processed = process12String(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 6 -> processed = processMandolin(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 7 -> processed = processResonator(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 8 -> processed = processBanjo(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                case 9 -> processed = processUkulele(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
                default -> processed = processDreadnought(in, bodyAmt, topAmt, attackAmt, warmthAmt, airAmt);
            }

            // Blend with dry signal
            output[i] = in * (1 - blendAmt) + processed * blendAmt;
        }
    }

    // ==================== MODEL PROCESSORS ====================

    /**
     * Dreadnought - Classic balanced acoustic tone.
     */
    private float processDreadnought(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        // Body resonances at ~100Hz, 200Hz, 400Hz
        float body1 = (float) applyResonantFilter(in, 100, 2.0, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 200, 1.5, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 400, 1.2, bodyFilter3State);

        float bodyMix = in + (body1 * 0.3f + body2 * 0.25f + body3 * 0.15f) * bodyAmt;

        // EQ shaping
        float eq = applyEQ(bodyMix, 120, warmthAmt, 800, 0.3f * topAmt, 4000, airAmt * 0.5f);

        // Transient enhancement
        return applyTransientEnhance(eq, attackAmt * 0.4f) * 0.85f;
    }

    /**
     * Jumbo - Large body with enhanced bass.
     */
    private float processJumbo(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 80, 2.5, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 160, 2.0, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 350, 1.5, bodyFilter3State);

        float bodyMix = in + (body1 * 0.4f + body2 * 0.3f + body3 * 0.2f) * bodyAmt;

        float eq = applyEQ(bodyMix, 100, warmthAmt * 1.3f, 600, 0.2f * topAmt, 3500, airAmt * 0.4f);

        return applyTransientEnhance(eq, attackAmt * 0.3f) * 0.8f;
    }

    /**
     * Parlor - Small vintage body, midrange focus.
     */
    private float processParlor(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 150, 1.8, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 300, 2.0, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 600, 1.5, bodyFilter3State);

        float bodyMix = in + (body1 * 0.2f + body2 * 0.35f + body3 * 0.25f) * bodyAmt;

        float eq = applyEQ(bodyMix, 150, warmthAmt * 0.6f, 1000, 0.5f * topAmt, 4500, airAmt * 0.6f);

        return applyTransientEnhance(eq, attackAmt * 0.5f) * 0.9f;
    }

    /**
     * Concert - Balanced, articulate fingerpicking tone.
     */
    private float processConcert(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 110, 1.8, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 220, 1.6, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 450, 1.4, bodyFilter3State);

        float bodyMix = in + (body1 * 0.25f + body2 * 0.25f + body3 * 0.2f) * bodyAmt;

        float eq = applyEQ(bodyMix, 130, warmthAmt * 0.9f, 900, 0.4f * topAmt, 5000, airAmt * 0.7f);

        return applyTransientEnhance(eq, attackAmt * 0.6f) * 0.85f;
    }

    /**
     * Nylon - Classical guitar, warm and mellow.
     */
    private float processNylon(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 90, 2.2, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 180, 1.8, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 350, 1.3, bodyFilter3State);

        float bodyMix = in + (body1 * 0.35f + body2 * 0.3f + body3 * 0.15f) * bodyAmt;

        // Warm, rolled-off highs
        float eq = applyEQ(bodyMix, 100, warmthAmt * 1.2f, 500, 0.3f * topAmt, 2500, airAmt * 0.2f);

        // Softer attack, apply gentle low-pass
        float enhanced = applyTransientEnhance(eq, attackAmt * 0.2f);
        return applyLowPass(enhanced, 6000) * 0.85f;
    }

    /**
     * 12-String - Shimmering doubled strings effect.
     */
    private float process12String(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 100, 2.0, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 200, 1.5, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 400, 1.2, bodyFilter3State);

        float bodyMix = in + (body1 * 0.3f + body2 * 0.25f + body3 * 0.15f) * bodyAmt;

        // Add chorus effect for shimmer
        float chorusOut = applyChorus(bodyMix, 0.015f, 0.3f);
        float mixed = bodyMix * 0.6f + chorusOut * 0.4f * topAmt;

        // Brighter EQ
        float eq = applyEQ(mixed, 110, warmthAmt * 0.8f, 1200, 0.4f * topAmt, 6000, airAmt * 0.8f);

        return applyTransientEnhance(eq, attackAmt * 0.5f) * 0.8f;
    }

    /**
     * Mandolin - Bright, octave-shifted character.
     */
    private float processMandolin(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 200, 2.0, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 400, 2.5, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 800, 2.0, bodyFilter3State);

        float bodyMix = in + (body1 * 0.15f + body2 * 0.3f + body3 * 0.35f) * bodyAmt;

        // Very bright
        float eq = applyEQ(bodyMix, 300, warmthAmt * 0.3f, 1500, 0.6f * topAmt, 7000, airAmt);

        // Sharp attack, subtle doubling
        float enhanced = applyTransientEnhance(eq, attackAmt * 0.8f);
        float doubled = applyChorus(enhanced, 0.008f, 0.15f);

        return (enhanced * 0.7f + doubled * 0.3f) * 0.9f;
    }

    /**
     * Resonator - Metallic dobro-style resonance.
     */
    private float processResonator(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 150, 3.0, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 450, 4.0, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 900, 3.5, bodyFilter3State);

        float bodyMix = in + (body1 * 0.2f + body2 * 0.4f + body3 * 0.3f) * bodyAmt;

        // Add comb filter for metallic cone sound
        float comb = applyCombFilter(bodyMix, 150, 0.4f * bodyAmt);
        bodyMix = bodyMix * 0.6f + comb * 0.4f;

        // Bright, nasal EQ
        float eq = applyEQ(bodyMix, 200, warmthAmt * 0.5f, 1200, 0.7f * topAmt, 5000, airAmt * 0.9f);

        return applyTransientEnhance(eq, attackAmt * 0.6f) * 0.85f;
    }

    /**
     * Banjo - Bright, plucky attack.
     */
    private float processBanjo(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 250, 2.5, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 500, 3.0, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 1000, 2.5, bodyFilter3State);

        float bodyMix = in + (body1 * 0.15f + body2 * 0.35f + body3 * 0.35f) * bodyAmt;

        // Banjo head resonance
        float head = applyCombFilter(bodyMix, 200, 0.3f * bodyAmt);
        bodyMix = bodyMix * 0.7f + head * 0.3f;

        // Very bright, almost no bass
        float eq = applyEQ(bodyMix, 400, warmthAmt * 0.2f, 2000, 0.6f * topAmt, 8000, airAmt);

        return applyTransientEnhance(eq, attackAmt) * 0.9f;
    }

    /**
     * Ukulele - Small body, high and bright.
     */
    private float processUkulele(float in, float bodyAmt, float topAmt, float attackAmt, float warmthAmt, float airAmt) {
        float body1 = (float) applyResonantFilter(in, 300, 2.0, bodyFilter1State);
        float body2 = (float) applyResonantFilter(in, 600, 2.5, bodyFilter2State);
        float body3 = (float) applyResonantFilter(in, 1200, 2.0, bodyFilter3State);

        float bodyMix = in + (body1 * 0.1f + body2 * 0.3f + body3 * 0.35f) * bodyAmt;

        float eq = applyEQ(bodyMix, 400, warmthAmt * 0.2f, 1800, 0.5f * topAmt, 6000, airAmt * 0.8f);

        // Moderate attack, cut low frequencies
        float enhanced = applyTransientEnhance(eq, attackAmt * 0.4f);

        return applyHighPass(enhanced, 200) * 0.9f;
    }

    // ==================== DSP UTILITIES ====================

    /**
     * Apply a resonant bandpass filter.
     */
    private double applyResonantFilter(float in, double freq, double q, double[] state) {
        double w0 = 2.0 * Math.PI * freq / currentSampleRate;
        double alpha = Math.sin(w0) / (2.0 * q);

        double b0 = alpha;
        double b1 = 0;
        double b2 = -alpha;
        double a0 = 1 + alpha;
        double a1 = -2 * Math.cos(w0);
        double a2 = 1 - alpha;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        double out = b0 * in + b1 * state[0] + b2 * state[1] - a1 * state[2] - a2 * state[3];

        state[1] = state[0];
        state[0] = in;
        state[3] = state[2];
        state[2] = out;

        return out;
    }

    /**
     * Apply EQ with low shelf, mid peak, and high shelf.
     */
    private float applyEQ(float in, float lowFreq, float lowGain,
                          float midFreq, float midGain,
                          float highFreq, float highGain) {
        float out = in;
        out = (float) applyShelf(out, lowFreq, lowGain - 0.5f, true, lowShelfState);
        out = (float) applyPeak(out, midFreq, midGain - 0.5f, 1.5, midPeakState);
        out = (float) applyShelf(out, highFreq, highGain - 0.5f, false, highShelfState);
        return out;
    }

    /**
     * Apply shelf filter (low or high).
     */
    private double applyShelf(float in, float freq, float gainDb, boolean lowShelf, double[] state) {
        double A = Math.pow(10, gainDb * 6 / 40.0);
        double w0 = 2.0 * Math.PI * freq / currentSampleRate;
        double S = 1.0;

        double alpha = Math.sin(w0) / 2.0 * Math.sqrt((A + 1.0/A) * (1.0/S - 1) + 2);
        double cosw0 = Math.cos(w0);
        double sqrtA = Math.sqrt(A);

        double b0, b1, b2, a0, a1, a2;

        if (lowShelf) {
            b0 = A * ((A + 1) - (A - 1) * cosw0 + 2 * sqrtA * alpha);
            b1 = 2 * A * ((A - 1) - (A + 1) * cosw0);
            b2 = A * ((A + 1) - (A - 1) * cosw0 - 2 * sqrtA * alpha);
            a0 = (A + 1) + (A - 1) * cosw0 + 2 * sqrtA * alpha;
            a1 = -2 * ((A - 1) + (A + 1) * cosw0);
            a2 = (A + 1) + (A - 1) * cosw0 - 2 * sqrtA * alpha;
        } else {
            b0 = A * ((A + 1) + (A - 1) * cosw0 + 2 * sqrtA * alpha);
            b1 = -2 * A * ((A - 1) + (A + 1) * cosw0);
            b2 = A * ((A + 1) + (A - 1) * cosw0 - 2 * sqrtA * alpha);
            a0 = (A + 1) - (A - 1) * cosw0 + 2 * sqrtA * alpha;
            a1 = 2 * ((A - 1) - (A + 1) * cosw0);
            a2 = (A + 1) - (A - 1) * cosw0 - 2 * sqrtA * alpha;
        }

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        double out = b0 * in + b1 * state[0] + b2 * state[1] - a1 * state[2] - a2 * state[3];

        state[1] = state[0];
        state[0] = in;
        state[3] = state[2];
        state[2] = out;

        return out;
    }

    /**
     * Apply peak EQ filter.
     */
    private double applyPeak(float in, float freq, float gainDb, double q, double[] state) {
        double A = Math.pow(10, gainDb * 6 / 40.0);
        double w0 = 2.0 * Math.PI * freq / currentSampleRate;
        double alpha = Math.sin(w0) / (2.0 * q);

        double b0 = 1 + alpha * A;
        double b1 = -2 * Math.cos(w0);
        double b2 = 1 - alpha * A;
        double a0 = 1 + alpha / A;
        double a1 = -2 * Math.cos(w0);
        double a2 = 1 - alpha / A;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        double out = b0 * in + b1 * state[0] + b2 * state[1] - a1 * state[2] - a2 * state[3];

        state[1] = state[0];
        state[0] = in;
        state[3] = state[2];
        state[2] = out;

        return out;
    }

    /**
     * Apply transient enhancement.
     */
    private float applyTransientEnhance(float in, float amount) {
        float absIn = Math.abs(in);
        float attackCoef = 0.01f;
        float releaseCoef = 0.0005f;

        if (absIn > envelope) {
            envelope = envelope + attackCoef * (absIn - envelope);
        } else {
            envelope = envelope + releaseCoef * (absIn - envelope);
        }

        float transientLevel = Math.max(0, absIn - envelope * 0.8f);
        return in + transientLevel * amount * 3.0f * Math.signum(in);
    }

    /**
     * Apply chorus effect for 12-string shimmer.
     */
    private float applyChorus(float in, float depth, float mix) {
        if (chorusBuffer == null || chorusBuffer.length == 0) return in;

        chorusBuffer[chorusWritePos] = in;

        float lfoRate = 1.5f;
        chorusPhase += lfoRate / currentSampleRate;
        if (chorusPhase >= 1.0f) chorusPhase -= 1.0f;

        float modulation = (float) Math.sin(2 * Math.PI * chorusPhase);
        float delayMs = 15 + modulation * depth * 1000;
        int delaySamples = (int) (delayMs * currentSampleRate / 1000.0f);
        delaySamples = Math.min(delaySamples, chorusBuffer.length - 1);

        int readPos = chorusWritePos - delaySamples;
        if (readPos < 0) readPos += chorusBuffer.length;

        float delayed = chorusBuffer[readPos];

        chorusWritePos = (chorusWritePos + 1) % chorusBuffer.length;

        return in * (1 - mix) + delayed * mix;
    }

    /**
     * Apply comb filter for metallic resonance.
     */
    private float applyCombFilter(float in, float freqHz, float feedback) {
        if (combBuffer == null || combBuffer.length == 0) return in;

        int delaySamples = (int) (currentSampleRate / freqHz);
        delaySamples = Math.min(delaySamples, combBuffer.length - 1);
        delaySamples = Math.max(delaySamples, 1);

        int readPos = combWritePos - delaySamples;
        if (readPos < 0) readPos += combBuffer.length;

        float delayed = combBuffer[readPos];
        float out = in + delayed * feedback;

        combBuffer[combWritePos] = out;
        combWritePos = (combWritePos + 1) % combBuffer.length;

        return out;
    }

    /**
     * Simple low-pass filter.
     */
    private float applyLowPass(float in, float cutoff) {
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoff);
        float dt = 1.0f / currentSampleRate;
        float alpha = dt / (rc + dt);

        prevSample = prevSample + alpha * (in - prevSample);
        return prevSample;
    }

    /**
     * Simple high-pass filter.
     */
    private float applyHighPass(float in, float cutoff) {
        return in - applyLowPass(in, cutoff);
    }

    @Override
    public void reset() {
        bodyFilter1State = new double[4];
        bodyFilter2State = new double[4];
        bodyFilter3State = new double[4];
        lowShelfState = new double[4];
        midPeakState = new double[4];
        highShelfState = new double[4];
        envelope = 0;
        prevSample = 0;
        if (chorusBuffer != null) {
            java.util.Arrays.fill(chorusBuffer, 0);
        }
        chorusWritePos = 0;
        chorusPhase = 0;
        if (combBuffer != null) {
            java.util.Arrays.fill(combBuffer, 0);
        }
        combWritePos = 0;
    }
}
