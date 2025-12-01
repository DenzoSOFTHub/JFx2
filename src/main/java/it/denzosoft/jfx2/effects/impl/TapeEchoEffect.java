package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Tape Echo effect.
 *
 * <p>Emulates vintage tape echo machines like the Echoplex or Roland Space Echo.
 * Features characteristic tape degradation, wow/flutter modulation, and
 * warm filtered repeats.</p>
 *
 * <p>Characteristics:
 * - High frequency rolloff on repeats (tape head loss)
 * - Wow and flutter from tape transport variations
 * - Saturation on feedback path
 * - Multiple playback heads (tape modes)</p>
 */
public class TapeEchoEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "tapeecho",
            "Tape Echo",
            "Vintage tape echo with wow/flutter and degradation",
            EffectCategory.DELAY
    );

    private static final String[] MODE_NAMES = {
            "Single",      // Single head
            "Multi",       // Multiple heads
            "Slapback"     // Short slapback
    };

    // Parameters
    private final Parameter timeParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;
    private final Parameter toneParam;
    private final Parameter wowParam;
    private final Parameter flutterParam;
    private final Parameter modeParam;
    private final Parameter saturationParam;

    // DSP components - Left
    private DelayLine delayLineL;
    private BiquadFilter toneFilterL;
    private BiquadFilter lowCutL;

    // DSP components - Right
    private DelayLine delayLineR;
    private BiquadFilter toneFilterR;
    private BiquadFilter lowCutR;

    // Modulation
    private LFO wowLFO;
    private LFO flutterLFO;

    // Feedback state
    private float feedbackL = 0;
    private float feedbackR = 0;

    public TapeEchoEffect() {
        super(METADATA);

        // Time: 50 ms to 1000 ms, default 350 ms
        timeParam = addFloatParameter("time", "Time",
                "Delay time. Classic tape echoes typically 300-400ms.",
                50.0f, 1000.0f, 350.0f, "ms");

        // Feedback: 0% to 95%, default 50%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount of signal fed back. Higher = more repeats.",
                0.0f, 95.0f, 50.0f, "%");

        // Mix: 0% to 100%, default 30%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and echo signal.",
                0.0f, 100.0f, 30.0f, "%");

        // Tone: 1000 Hz to 8000 Hz, default 3000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "High frequency cutoff. Lower = darker, more vintage.",
                1000.0f, 8000.0f, 3000.0f, "Hz");

        // Wow: 0% to 100%, default 20%
        wowParam = addFloatParameter("wow", "Wow",
                "Slow pitch variation from tape speed changes.",
                0.0f, 100.0f, 20.0f, "%");

        // Flutter: 0% to 100%, default 15%
        flutterParam = addFloatParameter("flutter", "Flutter",
                "Fast pitch variation from capstan irregularities.",
                0.0f, 100.0f, 15.0f, "%");

        // Mode
        modeParam = addChoiceParameter("mode", "Mode",
                "Echo configuration: Single, Multi-head, or Slapback.",
                MODE_NAMES, 0);

        // Saturation: 0% to 100%, default 30%
        saturationParam = addFloatParameter("saturation", "Saturation",
                "Tape saturation on feedback. Adds warmth and compression.",
                0.0f, 100.0f, 30.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Max 2 seconds delay
        delayLineL = new DelayLine(2000.0f, sampleRate);
        delayLineR = new DelayLine(2000.0f, sampleRate);

        // Tone filter (lowpass for tape rolloff)
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Low cut to prevent mud buildup
        lowCutL = new BiquadFilter();
        lowCutL.setSampleRate(sampleRate);
        lowCutL.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        lowCutR = new BiquadFilter();
        lowCutR.setSampleRate(sampleRate);
        lowCutR.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        // Wow LFO (slow, ~0.5-2 Hz)
        wowLFO = new LFO(LFO.Waveform.SINE, 0.8f, sampleRate);

        // Flutter LFO (faster, ~5-7 Hz)
        flutterLFO = new LFO(LFO.Waveform.SINE, 6.0f, sampleRate);

        feedbackL = 0;
        feedbackR = 0;
    }

    /**
     * Tape saturation using tanh-like soft clipping.
     */
    private float saturate(float input, float drive) {
        if (drive <= 0) return input;
        float gained = input * (1.0f + drive * 2.0f);
        return (float) Math.tanh(gained) / (1.0f + drive * 0.5f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float timeMs = timeParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float toneFreq = toneParam.getValue();
        float wow = wowParam.getValue() / 100.0f;
        float flutter = flutterParam.getValue() / 100.0f;
        int mode = (int) modeParam.getValue();
        float saturation = saturationParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);

        // Calculate base delay
        float baseDelaySamples = delayLineL.msToSamples(timeMs);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Get modulation
            float wowMod = wowLFO.tick() * wow * 0.02f;     // ±2% pitch variation
            float flutterMod = flutterLFO.tick() * flutter * 0.005f; // ±0.5%

            float modulation = 1.0f + wowMod + flutterMod;
            float delaySamples = baseDelaySamples * modulation;

            // Write input + feedback to delay
            float inputWithFeedback = dry + feedbackL * feedback;
            delayLineL.write(inputWithFeedback);

            // Read delayed signal
            float wet = delayLineL.readCubic(delaySamples);

            // Apply tone filtering (tape head loss)
            wet = toneFilterL.process(wet);
            wet = lowCutL.process(wet);

            // Apply saturation
            wet = saturate(wet, saturation);

            // Store feedback
            feedbackL = wet;

            // Multi-head mode - add secondary tap
            if (mode == 1) {
                float tap2 = delayLineL.readCubic(delaySamples * 0.666f) * 0.5f;
                float tap3 = delayLineL.readCubic(delaySamples * 0.333f) * 0.3f;
                wet = wet + tap2 + tap3;
            } else if (mode == 2) {
                // Slapback - shorter time, less feedback
                delaySamples = baseDelaySamples * 0.3f * modulation;
                wet = delayLineL.readCubic(delaySamples);
            }

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float timeMs = timeParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float toneFreq = toneParam.getValue();
        float wow = wowParam.getValue() / 100.0f;
        float flutter = flutterParam.getValue() / 100.0f;
        int mode = (int) modeParam.getValue();
        float saturation = saturationParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        float baseDelaySamples = delayLineL.msToSamples(timeMs);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            float wowMod = wowLFO.tick() * wow * 0.02f;
            float flutterMod = flutterLFO.tick() * flutter * 0.005f;
            float modulation = 1.0f + wowMod + flutterMod;
            float delaySamples = baseDelaySamples * modulation;

            // Left channel
            delayLineL.write(dryL + feedbackL * feedback);
            float wetL = delayLineL.readCubic(delaySamples);
            wetL = toneFilterL.process(wetL);
            wetL = lowCutL.process(wetL);
            wetL = saturate(wetL, saturation);
            feedbackL = wetL;

            // Right channel (slight offset for stereo)
            float delaySamplesR = delaySamples * 1.02f; // 2% longer for stereo spread
            delayLineR.write(dryR + feedbackR * feedback);
            float wetR = delayLineR.readCubic(delaySamplesR);
            wetR = toneFilterR.process(wetR);
            wetR = lowCutR.process(wetR);
            wetR = saturate(wetR, saturation);
            feedbackR = wetR;

            // Multi-head mode
            if (mode == 1) {
                wetL += delayLineL.readCubic(delaySamples * 0.666f) * 0.5f;
                wetL += delayLineL.readCubic(delaySamples * 0.333f) * 0.3f;
                wetR += delayLineR.readCubic(delaySamplesR * 0.666f) * 0.5f;
                wetR += delayLineR.readCubic(delaySamplesR * 0.333f) * 0.3f;
            }

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
        if (lowCutL != null) lowCutL.reset();
        if (lowCutR != null) lowCutR.reset();
        if (wowLFO != null) wowLFO.reset();
        if (flutterLFO != null) flutterLFO.reset();
        feedbackL = 0;
        feedbackR = 0;
    }

    // Convenience setters
    public void setTime(float ms) { timeParam.setValue(ms); }
    public void setFeedback(float percent) { feedbackParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setWow(float percent) { wowParam.setValue(percent); }
    public void setFlutter(float percent) { flutterParam.setValue(percent); }
    public void setMode(int mode) { modeParam.setValue(mode); }
    public void setSaturation(float percent) { saturationParam.setValue(percent); }
}
