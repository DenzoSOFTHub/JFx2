package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.WaveShaper;
import it.denzosoft.jfx2.effects.*;

/**
 * High-gain distortion effect with pre and post tone controls.
 *
 * <p>More aggressive than overdrive, with hard clipping option.
 * Features both pre-clipping (brightness) and post-clipping (warmth) tone controls.</p>
 */
public class DistortionEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "distortion",
            "Distortion",
            "High-gain distortion with pre/post tone shaping",
            EffectCategory.DISTORTION
    );

    // Parameters
    private final Parameter driveParam;
    private final Parameter preToneParam;   // Pre-clipping brightness
    private final Parameter postToneParam;  // Post-clipping warmth
    private final Parameter clipTypeParam;  // Hard vs soft clipping
    private final Parameter levelParam;

    // Filters - Left channel
    private BiquadFilter inputHpfL;
    private BiquadFilter preToneFilterL;   // Pre-clipping high shelf
    private BiquadFilter postToneFilterL;  // Post-clipping lowpass
    private BiquadFilter outputLpfL;

    // Filters - Right channel
    private BiquadFilter inputHpfR;
    private BiquadFilter preToneFilterR;
    private BiquadFilter postToneFilterR;
    private BiquadFilter outputLpfR;

    public DistortionEffect() {
        super(METADATA);

        // Drive: 1x to 100x, default 20x
        driveParam = addFloatParameter("drive", "Drive",
                "Amount of gain and distortion. Higher values create heavier, more saturated tones.",
                1.0f, 100.0f, 20.0f, "x");

        // Pre-tone (brightness): -12 dB to +12 dB high shelf at 2kHz, default 0 dB
        preToneParam = addFloatParameter("preTone", "Pre Tone",
                "Adjusts high frequency content before clipping. Boost for cutting tone, cut for thicker sound.",
                -12.0f, 12.0f, 0.0f, "dB");

        // Post-tone (warmth): 500 Hz to 8000 Hz lowpass, default 4000 Hz
        postToneParam = addFloatParameter("postTone", "Post Tone",
                "Filters the distorted signal. Lower values remove harshness, higher values add presence.",
                500.0f, 8000.0f, 4000.0f, "Hz");

        // Clip type: 0 = soft (tanh), 1 = hard, 2 = asymmetric
        clipTypeParam = addChoiceParameter("clipType", "Clip Type",
                "Clipping style: Soft (warm tube-like), Hard (aggressive transistor), Asymmetric (complex harmonics).",
                new String[]{"Soft", "Hard", "Asymmetric"}, 0);

        // Output level: -20 dB to +6 dB, default -6 dB (compensate for high gain)
        levelParam = addFloatParameter("level", "Level",
                "Output volume. Reduce to prevent clipping the next stage.",
                -20.0f, 6.0f, -6.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Input high-pass at 100 Hz - Left
        inputHpfL = new BiquadFilter();
        inputHpfL.setSampleRate(sampleRate);
        inputHpfL.configure(FilterType.HIGHPASS, 100.0f, 0.707f, 0.0f);

        // Input high-pass - Right
        inputHpfR = new BiquadFilter();
        inputHpfR.setSampleRate(sampleRate);
        inputHpfR.configure(FilterType.HIGHPASS, 100.0f, 0.707f, 0.0f);

        // Pre-tone: high shelf at 2kHz - Left
        preToneFilterL = new BiquadFilter();
        preToneFilterL.setSampleRate(sampleRate);
        preToneFilterL.configure(FilterType.HIGHSHELF, 2000.0f, 0.707f, preToneParam.getValue());

        // Pre-tone - Right
        preToneFilterR = new BiquadFilter();
        preToneFilterR.setSampleRate(sampleRate);
        preToneFilterR.configure(FilterType.HIGHSHELF, 2000.0f, 0.707f, preToneParam.getValue());

        // Post-tone: lowpass for warmth - Left
        postToneFilterL = new BiquadFilter();
        postToneFilterL.setSampleRate(sampleRate);
        postToneFilterL.configure(FilterType.LOWPASS, postToneParam.getValue(), 0.707f, 0.0f);

        // Post-tone - Right
        postToneFilterR = new BiquadFilter();
        postToneFilterR.setSampleRate(sampleRate);
        postToneFilterR.configure(FilterType.LOWPASS, postToneParam.getValue(), 0.707f, 0.0f);

        // Output lowpass at 8kHz - Left
        outputLpfL = new BiquadFilter();
        outputLpfL.setSampleRate(sampleRate);
        outputLpfL.configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);

        // Output lowpass - Right
        outputLpfR = new BiquadFilter();
        outputLpfR.setSampleRate(sampleRate);
        outputLpfR.configure(FilterType.LOWPASS, 8000.0f, 0.707f, 0.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float drive = driveParam.getValue();
        float preToneDb = preToneParam.getValue();
        float postToneFreq = postToneParam.getValue();
        int clipType = clipTypeParam.getChoiceIndex();
        float levelLinear = dbToLinear(levelParam.getValue());

        // Update filters if parameters changed
        preToneFilterL.setGainDb(preToneDb);
        postToneFilterL.setFrequency(postToneFreq);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Input filtering
            sample = inputHpfL.process(sample);

            // Pre-tone (brightness control before clipping)
            sample = preToneFilterL.process(sample);

            // Apply drive and clipping
            switch (clipType) {
                case 0:  // Soft (tanh)
                    sample = WaveShaper.tanhClip(sample, drive);
                    break;
                case 1:  // Hard
                    sample = sample * drive;
                    sample = WaveShaper.hardClip(sample, 1.0f);
                    break;
                case 2:  // Asymmetric
                    sample = WaveShaper.asymmetricClip(sample, drive);
                    break;
            }

            // Post-tone (warmth control after clipping)
            sample = postToneFilterL.process(sample);

            // Output smoothing
            sample = outputLpfL.process(sample);

            // Apply output level
            output[i] = sample * levelLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float drive = driveParam.getValue();
        float preToneDb = preToneParam.getValue();
        float postToneFreq = postToneParam.getValue();
        int clipType = clipTypeParam.getChoiceIndex();
        float levelLinear = dbToLinear(levelParam.getValue());

        // Update filters if parameters changed
        preToneFilterL.setGainDb(preToneDb);
        preToneFilterR.setGainDb(preToneDb);
        postToneFilterL.setFrequency(postToneFreq);
        postToneFilterR.setFrequency(postToneFreq);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Input filtering
            sampleL = inputHpfL.process(sampleL);
            sampleR = inputHpfR.process(sampleR);

            // Pre-tone (brightness control before clipping)
            sampleL = preToneFilterL.process(sampleL);
            sampleR = preToneFilterR.process(sampleR);

            // Apply drive and clipping
            switch (clipType) {
                case 0:  // Soft (tanh)
                    sampleL = WaveShaper.tanhClip(sampleL, drive);
                    sampleR = WaveShaper.tanhClip(sampleR, drive);
                    break;
                case 1:  // Hard
                    sampleL = WaveShaper.hardClip(sampleL * drive, 1.0f);
                    sampleR = WaveShaper.hardClip(sampleR * drive, 1.0f);
                    break;
                case 2:  // Asymmetric
                    sampleL = WaveShaper.asymmetricClip(sampleL, drive);
                    sampleR = WaveShaper.asymmetricClip(sampleR, drive);
                    break;
            }

            // Post-tone (warmth control after clipping)
            sampleL = postToneFilterL.process(sampleL);
            sampleR = postToneFilterR.process(sampleR);

            // Output smoothing
            sampleL = outputLpfL.process(sampleL);
            sampleR = outputLpfR.process(sampleR);

            // Apply output level
            outputL[i] = sampleL * levelLinear;
            outputR[i] = sampleR * levelLinear;
        }
    }

    @Override
    protected void onReset() {
        if (inputHpfL != null) inputHpfL.reset();
        if (inputHpfR != null) inputHpfR.reset();
        if (preToneFilterL != null) preToneFilterL.reset();
        if (preToneFilterR != null) preToneFilterR.reset();
        if (postToneFilterL != null) postToneFilterL.reset();
        if (postToneFilterR != null) postToneFilterR.reset();
        if (outputLpfL != null) outputLpfL.reset();
        if (outputLpfR != null) outputLpfR.reset();
    }

    // Convenience setters
    public void setDrive(float drive) {
        driveParam.setValue(drive);
    }

    public void setPreToneDb(float dB) {
        preToneParam.setValue(dB);
    }

    public void setPostToneHz(float hz) {
        postToneParam.setValue(hz);
    }

    public void setClipType(int type) {
        clipTypeParam.setChoice(type);
    }

    public void setLevelDb(float dB) {
        levelParam.setValue(dB);
    }
}
