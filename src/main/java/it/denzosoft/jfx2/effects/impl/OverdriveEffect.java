package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.WaveShaper;
import it.denzosoft.jfx2.effects.*;

/**
 * Overdrive effect with soft tanh clipping.
 *
 * <p>Classic warm overdrive sound inspired by tube amp breakup.
 * Features pre-filtering and tone control.</p>
 */
public class OverdriveEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "overdrive",
            "Overdrive",
            "Warm tube-style overdrive with soft clipping",
            EffectCategory.DISTORTION
    );

    // Parameters
    private final Parameter driveParam;
    private final Parameter toneParam;
    private final Parameter levelParam;

    // Filters - Left channel
    private BiquadFilter inputHpfL;   // Remove DC and rumble
    private BiquadFilter toneFilterL; // Post-clipping tone control
    private BiquadFilter outputLpfL;  // Anti-aliasing / smoothing

    // Filters - Right channel
    private BiquadFilter inputHpfR;
    private BiquadFilter toneFilterR;
    private BiquadFilter outputLpfR;

    public OverdriveEffect() {
        super(METADATA);

        // Drive: 1x to 50x, default 5x
        driveParam = addFloatParameter("drive", "Drive",
                "Amount of saturation and harmonic content. Higher values add more grit and sustain.",
                1.0f, 50.0f, 5.0f, "x");

        // Tone: 200 Hz to 8000 Hz (lowpass cutoff), default 3000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "Controls brightness. Lower values are darker/warmer, higher values are brighter/edgier.",
                200.0f, 8000.0f, 3000.0f, "Hz");

        // Output level: -20 dB to +6 dB, default 0 dB
        levelParam = addFloatParameter("level", "Level",
                "Output volume after distortion. Use to match bypassed volume or boost signal.",
                -20.0f, 6.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Input high-pass at 80 Hz - Left
        inputHpfL = new BiquadFilter();
        inputHpfL.setSampleRate(sampleRate);
        inputHpfL.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        // Input high-pass - Right
        inputHpfR = new BiquadFilter();
        inputHpfR.setSampleRate(sampleRate);
        inputHpfR.configure(FilterType.HIGHPASS, 80.0f, 0.707f, 0.0f);

        // Tone control (lowpass) - Left
        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Tone control - Right
        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Output lowpass at 10kHz - Left
        outputLpfL = new BiquadFilter();
        outputLpfL.setSampleRate(sampleRate);
        outputLpfL.configure(FilterType.LOWPASS, 10000.0f, 0.707f, 0.0f);

        // Output lowpass - Right
        outputLpfR = new BiquadFilter();
        outputLpfR.setSampleRate(sampleRate);
        outputLpfR.configure(FilterType.LOWPASS, 10000.0f, 0.707f, 0.0f);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float drive = driveParam.getValue();
        float toneFreq = toneParam.getValue();
        float levelLinear = dbToLinear(levelParam.getValue());

        // Update tone filter if changed
        toneFilterL.setFrequency(toneFreq);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Input filtering (remove DC)
            sample = inputHpfL.process(sample);

            // Apply drive and soft clipping
            sample = WaveShaper.tanhClip(sample, drive);

            // Tone control
            sample = toneFilterL.process(sample);

            // Output smoothing
            sample = outputLpfL.process(sample);

            // Apply output level
            output[i] = sample * levelLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float drive = driveParam.getValue();
        float toneFreq = toneParam.getValue();
        float levelLinear = dbToLinear(levelParam.getValue());

        // Update tone filters
        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Input filtering
            sampleL = inputHpfL.process(sampleL);
            sampleR = inputHpfR.process(sampleR);

            // Apply drive and soft clipping
            sampleL = WaveShaper.tanhClip(sampleL, drive);
            sampleR = WaveShaper.tanhClip(sampleR, drive);

            // Tone control
            sampleL = toneFilterL.process(sampleL);
            sampleR = toneFilterR.process(sampleR);

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
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        if (outputLpfL != null) outputLpfL.reset();
        if (outputLpfR != null) outputLpfR.reset();
    }

    // Convenience setters
    public void setDrive(float drive) {
        driveParam.setValue(drive);
    }

    public void setToneHz(float hz) {
        toneParam.setValue(hz);
    }

    public void setLevelDb(float dB) {
        levelParam.setValue(dB);
    }
}
