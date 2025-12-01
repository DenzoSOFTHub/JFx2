package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Octaver effect - classic analog-style octave generation.
 *
 * <p>Generates:
 * - Octave down (-1): Frequency division using flip-flop detection
 * - Octave up (+1): Full-wave rectification (doubles frequency)
 * </p>
 *
 * <p>Each octave has independent level control plus dry mix.</p>
 */
public class OctaverEffect extends AbstractEffect {

    // Parameters
    private Parameter octaveDown;
    private Parameter octaveUp;
    private Parameter dryLevel;
    private Parameter lowPassFreq;
    private Parameter tracking;

    // Octave down state - Left channel
    private boolean flipFlopStateL;
    private float lastSampleL;
    private float octDownOutputL;
    private float octDownSmoothL;
    private float octUpSmoothL;

    // Octave down state - Right channel
    private boolean flipFlopStateR;
    private float lastSampleR;
    private float octDownOutputR;
    private float octDownSmoothR;
    private float octUpSmoothR;

    // Filters - Left channel
    private BiquadFilter inputFilterL;
    private BiquadFilter octDownFilterL;
    private BiquadFilter octUpFilterL;

    // Filters - Right channel
    private BiquadFilter inputFilterR;
    private BiquadFilter octDownFilterR;
    private BiquadFilter octUpFilterR;

    // Zero crossing detection
    private float zeroCrossThreshold;

    public OctaverEffect() {
        super(EffectMetadata.of("octaver", "Octaver", "Classic octave up and down effect", EffectCategory.PITCH));
        initParameters();
    }

    private void initParameters() {
        octaveDown = addFloatParameter("octDown", "Octave Down",
                "Level of the one-octave-down signal. Creates thick, bass-heavy tones.",
                0.0f, 100.0f, 50.0f, "%");
        octaveUp = addFloatParameter("octUp", "Octave Up",
                "Level of the one-octave-up signal. Adds shimmer and 12-string-like quality.",
                0.0f, 100.0f, 0.0f, "%");
        dryLevel = addFloatParameter("dry", "Dry",
                "Level of the original unprocessed signal. Blend to taste with octave signals.",
                0.0f, 100.0f, 50.0f, "%");
        lowPassFreq = addFloatParameter("lpf", "Tone",
                "Filters the octave signals. Lower values smooth out the synthetic character.",
                200.0f, 2000.0f, 800.0f, "Hz");
        tracking = addFloatParameter("tracking", "Tracking",
                "How accurately the effect follows your playing. Higher = better tracking but more glitches.",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Left channel state
        flipFlopStateL = false;
        lastSampleL = 0;
        octDownOutputL = 0;
        octDownSmoothL = 0;
        octUpSmoothL = 0;

        // Right channel state
        flipFlopStateR = false;
        lastSampleR = 0;
        octDownOutputR = 0;
        octDownSmoothR = 0;
        octUpSmoothR = 0;

        // Input bandpass filter (helps with tracking) - Left
        inputFilterL = new BiquadFilter();
        inputFilterL.setSampleRate(sampleRate);
        inputFilterL.configure(FilterType.BANDPASS, 200.0f, 1.0f, 0);

        // Input bandpass filter - Right
        inputFilterR = new BiquadFilter();
        inputFilterR.setSampleRate(sampleRate);
        inputFilterR.configure(FilterType.BANDPASS, 200.0f, 1.0f, 0);

        // Low-pass filter for octave down - Left
        octDownFilterL = new BiquadFilter();
        octDownFilterL.setSampleRate(sampleRate);
        octDownFilterL.configure(FilterType.LOWPASS, 400.0f, 0.707f, 0);

        // Low-pass filter for octave down - Right
        octDownFilterR = new BiquadFilter();
        octDownFilterR.setSampleRate(sampleRate);
        octDownFilterR.configure(FilterType.LOWPASS, 400.0f, 0.707f, 0);

        // Low-pass for octave up - Left
        octUpFilterL = new BiquadFilter();
        octUpFilterL.setSampleRate(sampleRate);
        octUpFilterL.configure(FilterType.LOWPASS, 2000.0f, 0.707f, 0);

        // Low-pass for octave up - Right
        octUpFilterR = new BiquadFilter();
        octUpFilterR.setSampleRate(sampleRate);
        octUpFilterR.configure(FilterType.LOWPASS, 2000.0f, 0.707f, 0);

        zeroCrossThreshold = 0.01f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float downLevel = octaveDown.getValue() / 100.0f;
        float upLevel = octaveUp.getValue() / 100.0f;
        float dry = dryLevel.getValue() / 100.0f;
        float lpfFreq = lowPassFreq.getValue();
        float trackAmt = tracking.getValue() / 100.0f;

        // Update filter frequencies
        octDownFilterL.configure(FilterType.LOWPASS, lpfFreq, 0.707f, 0);
        octUpFilterL.configure(FilterType.LOWPASS, lpfFreq * 2.0f, 0.707f, 0);

        // Adaptive threshold based on tracking setting
        float adaptiveThreshold = zeroCrossThreshold * (1.0f - trackAmt * 0.9f);

        for (int i = 0; i < frameCount; i++) {
            float sample = input[i];

            // Filter input for better tracking
            float filtered = inputFilterL.process(sample);

            // === OCTAVE DOWN (Frequency Division) ===
            // Detect zero crossings and toggle flip-flop
            if ((lastSampleL < -adaptiveThreshold && filtered >= adaptiveThreshold) ||
                (lastSampleL > adaptiveThreshold && filtered <= -adaptiveThreshold)) {
                flipFlopStateL = !flipFlopStateL;
            }

            // Generate square wave at half frequency
            float octDownSquare = flipFlopStateL ? 1.0f : -1.0f;

            // Modulate with envelope of input for more natural sound
            float envelope = Math.abs(sample);
            octDownSmoothL = 0.999f * octDownSmoothL + 0.001f * envelope;
            octDownOutputL = octDownSquare * octDownSmoothL * 2.0f;

            // Filter to smooth the square wave into something more sine-like
            float octDown = octDownFilterL.process(octDownOutputL);

            // === OCTAVE UP (Full-Wave Rectification) ===
            // Full-wave rectification doubles the frequency
            float rectified = Math.abs(sample) * 2.0f - sample;

            // Remove DC and smooth
            octUpSmoothL = 0.99f * octUpSmoothL + 0.01f * rectified;
            float octUp = rectified - octUpSmoothL;

            // Filter to clean up harmonics
            octUp = octUpFilterL.process(octUp);

            // === MIX ===
            output[i] = sample * dry + octDown * downLevel + octUp * upLevel;

            lastSampleL = filtered;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float downLevel = octaveDown.getValue() / 100.0f;
        float upLevel = octaveUp.getValue() / 100.0f;
        float dry = dryLevel.getValue() / 100.0f;
        float lpfFreq = lowPassFreq.getValue();
        float trackAmt = tracking.getValue() / 100.0f;

        // Update filter frequencies for both channels
        octDownFilterL.configure(FilterType.LOWPASS, lpfFreq, 0.707f, 0);
        octDownFilterR.configure(FilterType.LOWPASS, lpfFreq, 0.707f, 0);
        octUpFilterL.configure(FilterType.LOWPASS, lpfFreq * 2.0f, 0.707f, 0);
        octUpFilterR.configure(FilterType.LOWPASS, lpfFreq * 2.0f, 0.707f, 0);

        // Adaptive threshold based on tracking setting
        float adaptiveThreshold = zeroCrossThreshold * (1.0f - trackAmt * 0.9f);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // === LEFT CHANNEL ===
            float sampleL = inputL[i];
            float filteredL = inputFilterL.process(sampleL);

            // Octave down - zero crossing detection
            if ((lastSampleL < -adaptiveThreshold && filteredL >= adaptiveThreshold) ||
                (lastSampleL > adaptiveThreshold && filteredL <= -adaptiveThreshold)) {
                flipFlopStateL = !flipFlopStateL;
            }
            float octDownSquareL = flipFlopStateL ? 1.0f : -1.0f;
            float envelopeL = Math.abs(sampleL);
            octDownSmoothL = 0.999f * octDownSmoothL + 0.001f * envelopeL;
            octDownOutputL = octDownSquareL * octDownSmoothL * 2.0f;
            float octDownL = octDownFilterL.process(octDownOutputL);

            // Octave up - full-wave rectification
            float rectifiedL = Math.abs(sampleL) * 2.0f - sampleL;
            octUpSmoothL = 0.99f * octUpSmoothL + 0.01f * rectifiedL;
            float octUpL = rectifiedL - octUpSmoothL;
            octUpL = octUpFilterL.process(octUpL);

            outputL[i] = sampleL * dry + octDownL * downLevel + octUpL * upLevel;
            lastSampleL = filteredL;

            // === RIGHT CHANNEL ===
            float sampleR = inputR[i];
            float filteredR = inputFilterR.process(sampleR);

            // Octave down - zero crossing detection
            if ((lastSampleR < -adaptiveThreshold && filteredR >= adaptiveThreshold) ||
                (lastSampleR > adaptiveThreshold && filteredR <= -adaptiveThreshold)) {
                flipFlopStateR = !flipFlopStateR;
            }
            float octDownSquareR = flipFlopStateR ? 1.0f : -1.0f;
            float envelopeR = Math.abs(sampleR);
            octDownSmoothR = 0.999f * octDownSmoothR + 0.001f * envelopeR;
            octDownOutputR = octDownSquareR * octDownSmoothR * 2.0f;
            float octDownR = octDownFilterR.process(octDownOutputR);

            // Octave up - full-wave rectification
            float rectifiedR = Math.abs(sampleR) * 2.0f - sampleR;
            octUpSmoothR = 0.99f * octUpSmoothR + 0.01f * rectifiedR;
            float octUpR = rectifiedR - octUpSmoothR;
            octUpR = octUpFilterR.process(octUpR);

            outputR[i] = sampleR * dry + octDownR * downLevel + octUpR * upLevel;
            lastSampleR = filteredR;
        }
    }

    @Override
    protected void onReset() {
        // Left channel state
        flipFlopStateL = false;
        lastSampleL = 0;
        octDownOutputL = 0;
        octDownSmoothL = 0;
        octUpSmoothL = 0;

        // Right channel state
        flipFlopStateR = false;
        lastSampleR = 0;
        octDownOutputR = 0;
        octDownSmoothR = 0;
        octUpSmoothR = 0;

        // Reset filters - Left
        if (inputFilterL != null) inputFilterL.reset();
        if (octDownFilterL != null) octDownFilterL.reset();
        if (octUpFilterL != null) octUpFilterL.reset();

        // Reset filters - Right
        if (inputFilterR != null) inputFilterR.reset();
        if (octDownFilterR != null) octDownFilterR.reset();
        if (octUpFilterR != null) octUpFilterR.reset();
    }
}
