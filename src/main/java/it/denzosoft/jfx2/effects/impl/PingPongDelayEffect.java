package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Ping-Pong Delay effect.
 *
 * <p>Creates a stereo delay that bounces between left and right channels,
 * creating a wide, immersive stereo field.</p>
 *
 * <p>Features:
 * - Alternating L/R repeats
 * - Adjustable stereo width
 * - High-cut filter for analog feel
 * - Cross-feedback between channels</p>
 */
public class PingPongDelayEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "pingpong",
            "Ping-Pong Delay",
            "Stereo bouncing delay effect",
            EffectCategory.DELAY
    );

    // Parameters
    private final Parameter timeParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;
    private final Parameter widthParam;
    private final Parameter toneParam;
    private final Parameter offsetParam;

    // DSP components
    private DelayLine delayLineL;
    private DelayLine delayLineR;
    private BiquadFilter toneFilterL;
    private BiquadFilter toneFilterR;

    // Feedback state
    private float feedbackL = 0;
    private float feedbackR = 0;

    public PingPongDelayEffect() {
        super(METADATA);

        // Time: 50 ms to 1000 ms, default 375 ms
        timeParam = addFloatParameter("time", "Time",
                "Delay time for each bounce.",
                50.0f, 1000.0f, 375.0f, "ms");

        // Feedback: 0% to 90%, default 50%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount fed back. Higher = more bounces.",
                0.0f, 90.0f, 50.0f, "%");

        // Mix: 0% to 100%, default 40%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and delayed signals.",
                0.0f, 100.0f, 40.0f, "%");

        // Width: 0% to 100%, default 100%
        widthParam = addFloatParameter("width", "Width",
                "Stereo width. 100% = full ping-pong, 0% = mono.",
                0.0f, 100.0f, 100.0f, "%");

        // Tone: 1000 Hz to 12000 Hz, default 5000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "High frequency cutoff for the delays.",
                1000.0f, 12000.0f, 5000.0f, "Hz");

        // Offset: -50% to +50%, default 0%
        offsetParam = addFloatParameter("offset", "Offset",
                "Time offset between channels. 0 = symmetric.",
                -50.0f, 50.0f, 0.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Max 2 seconds delay
        delayLineL = new DelayLine(2000.0f, sampleRate);
        delayLineR = new DelayLine(2000.0f, sampleRate);

        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        feedbackL = 0;
        feedbackR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono input - expand to stereo internally
        float[] tempR = new float[frameCount];
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];
        onProcessStereo(input, input, outL, outR, frameCount);

        // Mix to mono output
        for (int i = 0; i < frameCount && i < output.length; i++) {
            output[i] = (outL[i] + outR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float timeMs = timeParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float width = widthParam.getValue() / 100.0f;
        float toneFreq = toneParam.getValue();
        float offset = offsetParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        // Calculate delay times with offset
        float timeMsL = timeMs * (1.0f - offset * 0.5f);
        float timeMsR = timeMs * (1.0f + offset * 0.5f);
        float delaySamplesL = delayLineL.msToSamples(timeMsL);
        float delaySamplesR = delayLineR.msToSamples(timeMsR);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Mono sum for input to create ping-pong
            float monoIn = (dryL + dryR) * 0.5f;

            // Cross-feed: left gets right's feedback, right gets left's
            // This creates the ping-pong bouncing effect
            delayLineL.write(monoIn + feedbackR * feedback);
            delayLineR.write(feedbackL * feedback);

            // Read delayed signals
            float wetL = delayLineL.readCubic(delaySamplesL);
            float wetR = delayLineR.readCubic(delaySamplesR);

            // Apply tone filters
            wetL = toneFilterL.process(wetL);
            wetR = toneFilterR.process(wetR);

            // Store for cross-feedback
            feedbackL = wetL;
            feedbackR = wetR;

            // Apply width (narrow to mono if width < 100%)
            float midWet = (wetL + wetR) * 0.5f;
            float sideWet = (wetL - wetR) * 0.5f;
            wetL = midWet + sideWet * width;
            wetR = midWet - sideWet * width;

            // Mix output
            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (delayLineL != null) delayLineL.clear();
        if (delayLineR != null) delayLineR.clear();
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        feedbackL = 0;
        feedbackR = 0;
    }

    // Convenience setters
    public void setTime(float ms) { timeParam.setValue(ms); }
    public void setFeedback(float percent) { feedbackParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setWidth(float percent) { widthParam.setValue(percent); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setOffset(float percent) { offsetParam.setValue(percent); }
}
