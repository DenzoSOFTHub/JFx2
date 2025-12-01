package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Delay effect with feedback, filtering, and BPM sync.
 *
 * <p>Features:
 * - Time in milliseconds or BPM-synced note divisions
 * - Feedback with lowpass filtering for analog-style darkening
 * - Mix control for parallel dry/wet blend
 * </p>
 */
public class DelayEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "delay",
            "Delay",
            "Digital delay with feedback and BPM sync",
            EffectCategory.DELAY
    );

    // Parameters
    private final Parameter timeParam;       // Delay time in ms
    private final Parameter feedbackParam;   // Feedback amount
    private final Parameter mixParam;        // Dry/wet mix
    private final Parameter filterParam;     // Feedback filter cutoff
    private final Parameter syncParam;       // BPM sync on/off
    private final Parameter bpmParam;        // BPM for sync
    private final Parameter divisionParam;   // Note division

    // DSP components - Left channel
    private DelayLine delayLineL;
    private BiquadFilter feedbackFilterL;

    // DSP components - Right channel (for stereo)
    private DelayLine delayLineR;
    private BiquadFilter feedbackFilterR;

    // State
    private float feedbackSampleL;
    private float feedbackSampleR;

    // Note divisions: 1/1, 1/2, 1/4, 1/8, 1/16, dotted variants, triplets
    private static final String[] NOTE_DIVISIONS = {
            "1/1", "1/2", "1/4", "1/8", "1/16",
            "1/2D", "1/4D", "1/8D", "1/16D",  // Dotted
            "1/2T", "1/4T", "1/8T", "1/16T"   // Triplet
    };

    public DelayEffect() {
        super(METADATA);

        // Delay time: 10 ms to 2000 ms, default 375 ms (1/8 note at 120 BPM)
        timeParam = addFloatParameter("time", "Time",
                "Delay time between repeats. Longer times create spacious echoes, shorter times add thickness.",
                10.0f, 2000.0f, 375.0f, "ms");

        // Feedback: 0% to 95%, default 40%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount of signal fed back into the delay. Higher values create more repeats.",
                0.0f, 95.0f, 40.0f, "%");

        // Mix: 0% (dry) to 100% (wet), default 30%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry signal and delayed signal. 50% is equal blend.",
                0.0f, 100.0f, 30.0f, "%");

        // Feedback filter: 500 Hz to 12000 Hz lowpass, default 8000 Hz
        filterParam = addFloatParameter("filter", "Filter",
                "Darkens each repeat by filtering high frequencies. Lower values create vintage tape-like decay.",
                500.0f, 12000.0f, 8000.0f, "Hz");

        // BPM sync on/off, default off
        syncParam = addBooleanParameter("sync", "Sync",
                "When enabled, delay time locks to tempo-based note divisions.",
                false);

        // BPM: 40 to 240, default 120
        bpmParam = addFloatParameter("bpm", "BPM",
                "Tempo for synced delay. Match to your song's tempo for rhythmic delays.",
                40.0f, 240.0f, 120.0f, "BPM");

        // Note division
        divisionParam = addChoiceParameter("division", "Division",
                "Note value for synced delay: quarter, eighth, sixteenth, dotted, or triplet.",
                NOTE_DIVISIONS, 2);  // 1/4 note
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create delay lines with max 2 seconds
        delayLineL = new DelayLine(2000.0f, sampleRate);
        delayLineR = new DelayLine(2000.0f, sampleRate);

        // Create feedback filters (lowpass)
        feedbackFilterL = new BiquadFilter();
        feedbackFilterL.setSampleRate(sampleRate);
        feedbackFilterL.configure(FilterType.LOWPASS, filterParam.getValue(), 0.707f, 0.0f);

        feedbackFilterR = new BiquadFilter();
        feedbackFilterR.setSampleRate(sampleRate);
        feedbackFilterR.configure(FilterType.LOWPASS, filterParam.getValue(), 0.707f, 0.0f);

        feedbackSampleL = 0.0f;
        feedbackSampleR = 0.0f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float filterFreq = filterParam.getValue();

        // Update filter if needed
        feedbackFilterL.setFrequency(filterFreq);

        // Calculate delay time in samples
        float delaySamples;
        if (syncParam.getBooleanValue()) {
            delaySamples = calculateSyncedDelay();
        } else {
            delaySamples = delayLineL.msToSamples(timeParam.getValue());
        }

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Read from delay line
            float delayed = delayLineL.read(delaySamples);

            // Apply feedback filter
            float filtered = feedbackFilterL.process(delayed);

            // Write input + filtered feedback to delay
            delayLineL.write(dry + filtered * feedback);

            // Mix dry and wet
            output[i] = dry * (1.0f - mix) + delayed * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float filterFreq = filterParam.getValue();

        // Update filters
        feedbackFilterL.setFrequency(filterFreq);
        feedbackFilterR.setFrequency(filterFreq);

        // Calculate delay time in samples
        float delaySamples;
        if (syncParam.getBooleanValue()) {
            delaySamples = calculateSyncedDelay();
        } else {
            delaySamples = delayLineL.msToSamples(timeParam.getValue());
        }

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            float dryL = inputL[i];
            float delayedL = delayLineL.read(delaySamples);
            float filteredL = feedbackFilterL.process(delayedL);
            delayLineL.write(dryL + filteredL * feedback);
            outputL[i] = dryL * (1.0f - mix) + delayedL * mix;

            // Right channel
            float dryR = inputR[i];
            float delayedR = delayLineR.read(delaySamples);
            float filteredR = feedbackFilterR.process(delayedR);
            delayLineR.write(dryR + filteredR * feedback);
            outputR[i] = dryR * (1.0f - mix) + delayedR * mix;
        }
    }

    /**
     * Calculate delay time from BPM and note division.
     */
    private float calculateSyncedDelay() {
        float bpm = bpmParam.getValue();
        int divIndex = divisionParam.getChoiceIndex();

        // Parse note division
        int noteDivision;
        boolean dotted = false;
        boolean triplet = false;

        String div = NOTE_DIVISIONS[divIndex];
        if (div.endsWith("D")) {
            dotted = true;
            div = div.substring(0, div.length() - 1);
        } else if (div.endsWith("T")) {
            triplet = true;
            div = div.substring(0, div.length() - 1);
        }

        // Extract denominator (1/4 -> 4, 1/8 -> 8, etc.)
        noteDivision = Integer.parseInt(div.split("/")[1]);

        return delayLineL.bpmToSamples(bpm, noteDivision, dotted, triplet);
    }

    @Override
    protected void onReset() {
        if (delayLineL != null) delayLineL.clear();
        if (delayLineR != null) delayLineR.clear();
        if (feedbackFilterL != null) feedbackFilterL.reset();
        if (feedbackFilterR != null) feedbackFilterR.reset();
        feedbackSampleL = 0.0f;
        feedbackSampleR = 0.0f;
    }

    // Convenience setters
    public void setTimeMs(float ms) {
        timeParam.setValue(ms);
    }

    public void setFeedback(float percent) {
        feedbackParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }

    public void setFilterHz(float hz) {
        filterParam.setValue(hz);
    }

    public void setSync(boolean sync) {
        syncParam.setValue(sync);
    }

    public void setBpm(float bpm) {
        bpmParam.setValue(bpm);
    }

    public void setDivision(int index) {
        divisionParam.setChoice(index);
    }
}
