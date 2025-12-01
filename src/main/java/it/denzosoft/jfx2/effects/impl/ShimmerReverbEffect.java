package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Shimmer Reverb effect.
 *
 * <p>Creates ethereal, ambient textures by combining reverb with
 * pitch shifting. The pitch-shifted signal is fed back into the
 * reverb, creating evolving, crystalline harmonics.</p>
 *
 * <p>Features:
 * - Long, ambient decay
 * - Octave-up pitch shifting in feedback
 * - Modulated diffusion
 * - High-frequency shimmer control</p>
 */
public class ShimmerReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "shimmerreverb",
            "Shimmer Reverb",
            "Ethereal reverb with pitch-shifted harmonics",
            EffectCategory.REVERB
    );

    // Diffuser delay times (prime numbers for less coloration)
    private static final float[] DIFFUSER_TIMES = {7.1f, 11.3f, 13.7f, 17.9f};
    private static final int NUM_DIFFUSERS = 4;

    // Parameters
    private final Parameter mixParam;
    private final Parameter decayParam;
    private final Parameter shimmerParam;
    private final Parameter pitchParam;
    private final Parameter dampeningParam;
    private final Parameter modulationParam;

    // Diffuser network
    private DelayLine[] diffusersL;
    private DelayLine[] diffusersR;

    // Main reverb tank
    private DelayLine tankL, tankR;

    // Pitch shifter buffers (simple granular pitch shift)
    private float[] pitchBufferL, pitchBufferR;
    private int pitchWritePosL, pitchWritePosR;
    private float pitchReadPosL, pitchReadPosR;
    private int pitchBufferSize;

    // Filters
    private BiquadFilter damperL, damperR;
    private BiquadFilter lowCutL, lowCutR;

    // Modulation phase
    private float modPhase = 0;

    public ShimmerReverbEffect() {
        super(METADATA);

        // Mix: 0% to 100%, default 50%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and reverb signal.",
                0.0f, 100.0f, 50.0f, "%");

        // Decay: 1s to 30s, default 8s
        decayParam = addFloatParameter("decay", "Decay",
                "Length of the reverb tail. Long decays for ambient textures.",
                1.0f, 30.0f, 8.0f, "s");

        // Shimmer: 0% to 100%, default 50%
        shimmerParam = addFloatParameter("shimmer", "Shimmer",
                "Amount of pitch-shifted signal in the feedback. Higher = more crystalline.",
                0.0f, 100.0f, 50.0f, "%");

        // Pitch: 0 (unison) to 12 (octave), default 12
        pitchParam = addFloatParameter("pitch", "Pitch",
                "Pitch shift amount in semitones. 12 = octave up.",
                0.0f, 24.0f, 12.0f, "st");

        // Dampening: 1000 Hz to 16000 Hz, default 6000 Hz
        dampeningParam = addFloatParameter("dampening", "Dampening",
                "High frequency absorption. Lower = darker, more natural decay.",
                1000.0f, 16000.0f, 6000.0f, "Hz");

        // Modulation: 0% to 100%, default 30%
        modulationParam = addFloatParameter("modulation", "Mod",
                "Amount of chorus-like modulation in the reverb.",
                0.0f, 100.0f, 30.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Diffuser network
        diffusersL = new DelayLine[NUM_DIFFUSERS];
        diffusersR = new DelayLine[NUM_DIFFUSERS];
        for (int i = 0; i < NUM_DIFFUSERS; i++) {
            diffusersL[i] = new DelayLine(50.0f, sampleRate);
            diffusersR[i] = new DelayLine(50.0f, sampleRate);
        }

        // Main tank (long delay for reverb)
        tankL = new DelayLine(500.0f, sampleRate);
        tankR = new DelayLine(500.0f, sampleRate);

        // Pitch shifter buffer (100ms window)
        pitchBufferSize = (int) (0.1f * sampleRate);
        pitchBufferL = new float[pitchBufferSize];
        pitchBufferR = new float[pitchBufferSize];
        pitchWritePosL = 0;
        pitchWritePosR = 0;
        pitchReadPosL = 0;
        pitchReadPosR = 0;

        // Damping filter
        damperL = new BiquadFilter();
        damperL.setSampleRate(sampleRate);
        damperL.configure(FilterType.LOWPASS, dampeningParam.getValue(), 0.707f, 0.0f);

        damperR = new BiquadFilter();
        damperR.setSampleRate(sampleRate);
        damperR.configure(FilterType.LOWPASS, dampeningParam.getValue(), 0.707f, 0.0f);

        // Low cut
        lowCutL = new BiquadFilter();
        lowCutL.setSampleRate(sampleRate);
        lowCutL.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        lowCutR = new BiquadFilter();
        lowCutR.setSampleRate(sampleRate);
        lowCutR.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        modPhase = 0;
    }

    /**
     * Simple granular pitch shifter for left channel.
     */
    private float pitchShiftL(float input, float readPos) {
        // Write to buffer
        pitchBufferL[pitchWritePosL] = input;

        // Read at different rate for pitch shift
        int readIdx = (int) readPos;
        float frac = readPos - readIdx;

        // Linear interpolation
        int idx0 = readIdx % pitchBufferSize;
        int idx1 = (readIdx + 1) % pitchBufferSize;
        float sample = pitchBufferL[idx0] * (1 - frac) + pitchBufferL[idx1] * frac;

        // Window for crossfade (simple triangle)
        float windowPos = (readPos % (pitchBufferSize / 2.0f)) / (pitchBufferSize / 2.0f);
        float window = windowPos < 0.5f ? windowPos * 2 : (1 - windowPos) * 2;

        return sample * window;
    }

    /**
     * Simple granular pitch shifter for right channel.
     */
    private float pitchShiftR(float input, float readPos) {
        // Write to buffer
        pitchBufferR[pitchWritePosR] = input;

        // Read at different rate for pitch shift
        int readIdx = (int) readPos;
        float frac = readPos - readIdx;

        // Linear interpolation
        int idx0 = readIdx % pitchBufferSize;
        int idx1 = (readIdx + 1) % pitchBufferSize;
        float sample = pitchBufferR[idx0] * (1 - frac) + pitchBufferR[idx1] * frac;

        // Window for crossfade (simple triangle)
        float windowPos = (readPos % (pitchBufferSize / 2.0f)) / (pitchBufferSize / 2.0f);
        float window = windowPos < 0.5f ? windowPos * 2 : (1 - windowPos) * 2;

        return sample * window;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float[] tempR = new float[frameCount];
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];
        onProcessStereo(input, input, outL, outR, frameCount);

        // Mix to mono
        for (int i = 0; i < frameCount && i < output.length; i++) {
            output[i] = (outL[i] + outR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float mix = mixParam.getValue() / 100.0f;
        float decayTime = decayParam.getValue();
        float shimmer = shimmerParam.getValue() / 100.0f;
        float pitchSemitones = pitchParam.getValue();
        float dampFreq = dampeningParam.getValue();
        float modulation = modulationParam.getValue() / 100.0f;

        damperL.setFrequency(dampFreq);
        damperR.setFrequency(dampFreq);

        // Calculate feedback for decay
        float tankTimeMs = 150.0f;
        float feedbackAmount = (float) Math.pow(0.001, tankTimeMs / 1000.0 / decayTime);
        feedbackAmount = Math.min(0.98f, feedbackAmount);

        // Pitch ratio
        float pitchRatio = (float) Math.pow(2.0, pitchSemitones / 12.0);

        float modRate = 0.5f; // Hz

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Update modulation
            modPhase += modRate / sampleRate;
            if (modPhase > 1.0f) modPhase -= 1.0f;
            float modValue = (float) Math.sin(modPhase * 2 * Math.PI) * modulation;

            // Input through diffusers
            float diffusedL = dryL;
            float diffusedR = dryR;

            for (int d = 0; d < NUM_DIFFUSERS; d++) {
                float delayMs = DIFFUSER_TIMES[d] * (1.0f + modValue * 0.1f);
                float delaySamples = diffusersL[d].msToSamples(delayMs);

                // All-pass diffuser
                float delayedL = diffusersL[d].readCubic(delaySamples);
                float delayedR = diffusersR[d].readCubic(delaySamples);

                float apCoeff = 0.6f;
                float outL = -apCoeff * diffusedL + delayedL;
                float outR = -apCoeff * diffusedR + delayedR;

                diffusersL[d].write(diffusedL + apCoeff * outL);
                diffusersR[d].write(diffusedR + apCoeff * outR);

                diffusedL = outL;
                diffusedR = outR;
            }

            // Read from tank
            float tankOutL = tankL.readCubic(tankL.msToSamples(tankTimeMs * (1 + modValue * 0.05f)));
            float tankOutR = tankR.readCubic(tankR.msToSamples(tankTimeMs * 1.1f * (1 - modValue * 0.05f)));

            // Apply damping
            tankOutL = damperL.process(tankOutL);
            tankOutR = damperR.process(tankOutR);

            // Pitch shift for shimmer
            float shiftedL = pitchShiftL(tankOutL, pitchReadPosL);
            float shiftedR = pitchShiftR(tankOutR, pitchReadPosR);

            // Update pitch buffer positions (separate for L/R)
            pitchWritePosL = (pitchWritePosL + 1) % pitchBufferSize;
            pitchWritePosR = (pitchWritePosR + 1) % pitchBufferSize;
            pitchReadPosL += pitchRatio;
            pitchReadPosR += pitchRatio;
            if (pitchReadPosL >= pitchBufferSize) pitchReadPosL -= pitchBufferSize;
            if (pitchReadPosR >= pitchBufferSize) pitchReadPosR -= pitchBufferSize;

            // Mix shimmer with regular feedback
            float feedbackL = tankOutL * (1 - shimmer) + shiftedL * shimmer;
            float feedbackR = tankOutR * (1 - shimmer) + shiftedR * shimmer;

            // Write to tank
            tankL.write(diffusedL + feedbackL * feedbackAmount);
            tankR.write(diffusedR + feedbackR * feedbackAmount);

            // Get wet signal
            float wetL = lowCutL.process(tankOutL);
            float wetR = lowCutR.process(tankOutR);

            // Output
            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (diffusersL != null) for (DelayLine d : diffusersL) if (d != null) d.clear();
        if (diffusersR != null) for (DelayLine d : diffusersR) if (d != null) d.clear();
        if (tankL != null) tankL.clear();
        if (tankR != null) tankR.clear();
        if (pitchBufferL != null) java.util.Arrays.fill(pitchBufferL, 0);
        if (pitchBufferR != null) java.util.Arrays.fill(pitchBufferR, 0);
        pitchWritePosL = 0;
        pitchWritePosR = 0;
        pitchReadPosL = 0;
        pitchReadPosR = 0;
        if (damperL != null) damperL.reset();
        if (damperR != null) damperR.reset();
        if (lowCutL != null) lowCutL.reset();
        if (lowCutR != null) lowCutR.reset();
        modPhase = 0;
    }

    // Convenience setters
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setDecay(float seconds) { decayParam.setValue(seconds); }
    public void setShimmer(float percent) { shimmerParam.setValue(percent); }
    public void setPitch(float semitones) { pitchParam.setValue(semitones); }
    public void setDampening(float hz) { dampeningParam.setValue(hz); }
    public void setModulation(float percent) { modulationParam.setValue(percent); }
}
