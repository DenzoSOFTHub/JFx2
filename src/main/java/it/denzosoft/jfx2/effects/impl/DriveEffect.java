package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.WaveShaper;
import it.denzosoft.jfx2.effects.*;

/**
 * Tube-style Drive effect with asymmetric clipping.
 *
 * <p>Emulates the warm, dynamic response of a tube amplifier being pushed
 * into saturation. Features asymmetric clipping for even harmonic content
 * and a mid-boost for classic blues/rock tones.</p>
 *
 * <p>Characteristics:
 * - Asymmetric soft clipping (tube-like)
 * - Pre-gain mid boost for thickness
 * - Dynamic response preserving pick attack
 * - Warm, musical harmonic content</p>
 */
public class DriveEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "drive",
            "Drive",
            "Warm tube-style drive with asymmetric clipping",
            EffectCategory.DISTORTION
    );

    // Parameters
    private final Parameter gainParam;
    private final Parameter toneParam;
    private final Parameter bodyParam;
    private final Parameter levelParam;

    // Filters - Left channel
    private BiquadFilter inputHpfL;
    private BiquadFilter midBoostL;
    private BiquadFilter toneFilterL;
    private BiquadFilter outputLpfL;

    // Filters - Right channel
    private BiquadFilter inputHpfR;
    private BiquadFilter midBoostR;
    private BiquadFilter toneFilterR;
    private BiquadFilter outputLpfR;

    public DriveEffect() {
        super(METADATA);

        // Gain: 1 to 30, default 8
        gainParam = addFloatParameter("gain", "Gain",
                "Amount of drive/saturation. Lower values clean, higher values crunch.",
                1.0f, 30.0f, 8.0f, "");

        // Tone: 500 Hz to 6000 Hz, default 2500 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "Output tone control. Lower = warmer, higher = brighter.",
                500.0f, 6000.0f, 2500.0f, "Hz");

        // Body (mid boost): 0% to 100%, default 50%
        bodyParam = addFloatParameter("body", "Body",
                "Mid-range boost for thickness and sustain.",
                0.0f, 100.0f, 50.0f, "%");

        // Output level: -20 dB to +6 dB, default 0 dB
        levelParam = addFloatParameter("level", "Level",
                "Output volume.",
                -20.0f, 6.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Input high-pass at 60 Hz (tighter than overdrive)
        inputHpfL = new BiquadFilter();
        inputHpfL.setSampleRate(sampleRate);
        inputHpfL.configure(FilterType.HIGHPASS, 60.0f, 0.707f, 0.0f);

        inputHpfR = new BiquadFilter();
        inputHpfR.setSampleRate(sampleRate);
        inputHpfR.configure(FilterType.HIGHPASS, 60.0f, 0.707f, 0.0f);

        // Mid boost around 800 Hz
        midBoostL = new BiquadFilter();
        midBoostL.setSampleRate(sampleRate);
        updateMidBoost(midBoostL);

        midBoostR = new BiquadFilter();
        midBoostR.setSampleRate(sampleRate);
        updateMidBoost(midBoostR);

        // Tone control (lowpass)
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Output lowpass at 8kHz
        outputLpfL = new BiquadFilter();
        outputLpfL.setSampleRate(sampleRate);
        outputLpfL.configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);

        outputLpfR = new BiquadFilter();
        outputLpfR.setSampleRate(sampleRate);
        outputLpfR.configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);
    }

    private void updateMidBoost(BiquadFilter filter) {
        float bodyAmount = bodyParam.getValue() / 100.0f;
        float boostDb = bodyAmount * 9.0f;  // 0 to 9 dB boost
        filter.configure(FilterType.PEAK, 800.0f, 1.2f, boostDb);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float gain = gainParam.getValue();
        float toneFreq = toneParam.getValue();
        float levelLinear = dbToLinear(levelParam.getValue());

        toneFilterL.setFrequency(toneFreq);
        updateMidBoost(midBoostL);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Input filtering
            sample = inputHpfL.process(sample);

            // Pre-gain mid boost
            sample = midBoostL.process(sample);

            // Asymmetric soft clipping (tube-like with even harmonics)
            sample = WaveShaper.asymmetricClip(sample, gain);

            // Tone control
            sample = toneFilterL.process(sample);

            // Output smoothing
            sample = outputLpfL.process(sample);

            output[i] = sample * levelLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float gain = gainParam.getValue();
        float toneFreq = toneParam.getValue();
        float levelLinear = dbToLinear(levelParam.getValue());

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);
        updateMidBoost(midBoostL);
        updateMidBoost(midBoostR);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Input filtering
            sampleL = inputHpfL.process(sampleL);
            sampleR = inputHpfR.process(sampleR);

            // Pre-gain mid boost
            sampleL = midBoostL.process(sampleL);
            sampleR = midBoostR.process(sampleR);

            // Asymmetric soft clipping
            sampleL = WaveShaper.asymmetricClip(sampleL, gain);
            sampleR = WaveShaper.asymmetricClip(sampleR, gain);

            // Tone control
            sampleL = toneFilterL.process(sampleL);
            sampleR = toneFilterR.process(sampleR);

            // Output smoothing
            sampleL = outputLpfL.process(sampleL);
            sampleR = outputLpfR.process(sampleR);

            outputL[i] = sampleL * levelLinear;
            outputR[i] = sampleR * levelLinear;
        }
    }

    @Override
    protected void onReset() {
        if (inputHpfL != null) inputHpfL.reset();
        if (inputHpfR != null) inputHpfR.reset();
        if (midBoostL != null) midBoostL.reset();
        if (midBoostR != null) midBoostR.reset();
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (outputLpfL != null) outputLpfL.reset();
        if (outputLpfR != null) outputLpfR.reset();
    }

    // Convenience setters
    public void setGain(float gain) {
        gainParam.setValue(gain);
    }

    public void setTone(float hz) {
        toneParam.setValue(hz);
    }

    public void setBody(float percent) {
        bodyParam.setValue(percent);
    }

    public void setLevel(float dB) {
        levelParam.setValue(dB);
    }
}
