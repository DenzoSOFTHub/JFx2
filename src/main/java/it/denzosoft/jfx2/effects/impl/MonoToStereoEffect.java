package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Mono to Stereo converter effect.
 *
 * <p>Transforms a mono signal into a rich stereo image using multiple techniques:
 * <ul>
 *   <li><b>Panning</b>: Position the sound in the stereo field</li>
 *   <li><b>Width</b>: Stereo spread amount</li>
 *   <li><b>Haas Effect</b>: Small delay on one channel for spatial perception</li>
 *   <li><b>Stereo Enhancement</b>: Frequency-based stereo separation</li>
 *   <li><b>LFO Animation</b>: Subtle movement in the stereo field</li>
 * </ul>
 * </p>
 *
 * <p>This effect should be used with STEREO mode to produce true stereo output.</p>
 */
public class MonoToStereoEffect extends AbstractEffect {

    private static final int MAX_HAAS_DELAY_MS = 40;  // Max Haas delay in ms
    private static final int MAX_HAAS_SAMPLES = 2048; // Buffer for Haas delay

    // Parameters
    private Parameter panParam;
    private Parameter widthParam;
    private Parameter haasDelayParam;
    private Parameter haasBalanceParam;
    private Parameter enhanceParam;
    private Parameter enhanceFreqParam;
    private Parameter lfoRateParam;
    private Parameter lfoDepthParam;
    private Parameter mixParam;

    // Haas delay lines
    private DelayLine haasDelayL;
    private DelayLine haasDelayR;

    // Stereo enhancement filters (different EQ on L and R)
    private BiquadFilter enhanceFilterL;
    private BiquadFilter enhanceFilterR;

    // LFO for stereo animation
    private LFO panLfo;

    // All-pass filters for phase-based widening
    private BiquadFilter allpassL;
    private BiquadFilter allpassR;

    public MonoToStereoEffect() {
        super(EffectMetadata.of("mono2stereo", "Mono to Stereo",
                "Convert mono signal to stereo with panning, width and spatial effects",
                EffectCategory.UTILITY));
        initParameters();
    }

    private void initParameters() {
        panParam = addFloatParameter("pan", "Pan",
                "Position in stereo field. -100 = full left, 0 = center, +100 = full right.",
                -100.0f, 100.0f, 0.0f, "%");

        widthParam = addFloatParameter("width", "Width",
                "Stereo width. 0% = mono, 100% = normal stereo, 200% = extra wide.",
                0.0f, 200.0f, 100.0f, "%");

        haasDelayParam = addFloatParameter("haasDelay", "Haas Delay",
                "Delay time for Haas effect (spatial perception). 0 = off, 10-30ms = natural.",
                0.0f, (float) MAX_HAAS_DELAY_MS, 15.0f, "ms");

        haasBalanceParam = addFloatParameter("haasBalance", "Haas Balance",
                "Which channel gets delayed. -100 = delay left, +100 = delay right.",
                -100.0f, 100.0f, 100.0f, "%");

        enhanceParam = addFloatParameter("enhance", "Enhance",
                "Frequency-based stereo enhancement. Separates frequencies between L and R.",
                0.0f, 100.0f, 30.0f, "%");

        enhanceFreqParam = addFloatParameter("enhanceFreq", "Enhance Freq",
                "Center frequency for stereo enhancement separation.",
                200.0f, 4000.0f, 1000.0f, "Hz");

        lfoRateParam = addFloatParameter("lfoRate", "LFO Rate",
                "Speed of automatic stereo movement. Creates subtle animation.",
                0.0f, 5.0f, 0.0f, "Hz");

        lfoDepthParam = addFloatParameter("lfoDepth", "LFO Depth",
                "Amount of automatic stereo movement.",
                0.0f, 100.0f, 20.0f, "%");

        mixParam = addFloatParameter("mix", "Mix",
                "Balance between original mono and stereo-processed signal.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Haas delay lines
        haasDelayL = new DelayLine(MAX_HAAS_SAMPLES);
        haasDelayR = new DelayLine(MAX_HAAS_SAMPLES);

        // Enhancement filters - complementary EQ on L and R
        enhanceFilterL = new BiquadFilter();
        enhanceFilterL.setSampleRate(sampleRate);
        enhanceFilterR = new BiquadFilter();
        enhanceFilterR.setSampleRate(sampleRate);

        // All-pass filters for phase-based widening
        allpassL = new BiquadFilter();
        allpassL.setSampleRate(sampleRate);
        allpassR = new BiquadFilter();
        allpassR.setSampleRate(sampleRate);

        // LFO for pan animation
        panLfo = new LFO(lfoRateParam.getValue(), sampleRate);
        panLfo.setWaveform(LFO.Waveform.SINE);

        updateFilters();
    }

    private void updateFilters() {
        float enhanceFreq = enhanceFreqParam.getValue();

        // Complementary shelving: L gets low boost/high cut, R gets opposite
        enhanceFilterL.configure(FilterType.LOWSHELF, enhanceFreq, 0.707f, 3.0f);
        enhanceFilterR.configure(FilterType.HIGHSHELF, enhanceFreq, 0.707f, 3.0f);

        // All-pass for phase difference (creates subtle comb filtering)
        allpassL.configure(FilterType.ALLPASS, enhanceFreq * 0.8f, 0.707f, 0);
        allpassR.configure(FilterType.ALLPASS, enhanceFreq * 1.2f, 0.707f, 0);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono processing: just pass through
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float pan = panParam.getValue() / 100.0f;  // -1 to +1
        float width = widthParam.getValue() / 100.0f;  // 0 to 2
        float haasMs = haasDelayParam.getValue();
        float haasBalance = haasBalanceParam.getValue() / 100.0f;  // -1 to +1
        float enhance = enhanceParam.getValue() / 100.0f;
        float lfoRate = lfoRateParam.getValue();
        float lfoDepth = lfoDepthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Calculate Haas delay in samples
        int haasDelaySamples = (int) (haasMs * sampleRate / 1000.0f);
        haasDelaySamples = Math.min(haasDelaySamples, MAX_HAAS_SAMPLES - 1);

        // Update LFO
        panLfo.setFrequency(lfoRate);

        // Update enhancement filters
        updateFilters();

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Input: assume mono (L), or mix L+R if stereo input
            float mono = inputL[i];

            // === LFO MODULATION ===
            float lfoPan = 0;
            if (lfoRate > 0.01f) {
                lfoPan = panLfo.tick() * lfoDepth;
            }
            float effectivePan = Math.max(-1.0f, Math.min(1.0f, pan + lfoPan));

            // === PANNING (equal power) ===
            // Equal power panning: maintains constant perceived loudness
            float panAngle = (effectivePan + 1.0f) * 0.25f * (float) Math.PI;  // 0 to PI/2
            float panL = (float) Math.cos(panAngle);
            float panR = (float) Math.sin(panAngle);

            // === HAAS EFFECT ===
            float haasL, haasR;
            if (haasDelaySamples > 0) {
                // Write to both delay lines
                haasDelayL.write(mono);
                haasDelayR.write(mono);

                if (haasBalance > 0) {
                    // Delay right channel
                    haasL = mono;
                    float delayedR = haasDelayR.read(haasDelaySamples);
                    haasR = mono * (1.0f - haasBalance) + delayedR * haasBalance;
                } else {
                    // Delay left channel
                    float delayedL = haasDelayL.read(haasDelaySamples);
                    haasL = mono * (1.0f + haasBalance) + delayedL * (-haasBalance);
                    haasR = mono;
                }
            } else {
                haasL = mono;
                haasR = mono;
                // Still write to delay line to keep it fresh
                haasDelayL.write(mono);
                haasDelayR.write(mono);
            }

            // === STEREO ENHANCEMENT ===
            float enhL = haasL;
            float enhR = haasR;
            if (enhance > 0.01f) {
                // Apply complementary EQ
                float eqL = enhanceFilterL.process(haasL);
                float eqR = enhanceFilterR.process(haasR);

                // Apply all-pass for phase difference
                float phaseL = allpassL.process(haasL);
                float phaseR = allpassR.process(haasR);

                // Blend: original + EQ difference + phase difference
                enhL = haasL + (eqL - haasL) * enhance + (phaseL - haasL) * enhance * 0.3f;
                enhR = haasR + (eqR - haasR) * enhance + (phaseR - haasR) * enhance * 0.3f;
            }

            // === WIDTH ===
            // Mid-Side processing for width control
            float mid = (enhL + enhR) * 0.5f;
            float side = (enhL - enhR) * 0.5f;

            // Adjust width: width=0 -> mono, width=1 -> normal, width=2 -> extra wide
            side *= width;

            // Convert back to L/R
            float widthL = mid + side;
            float widthR = mid - side;

            // === APPLY PANNING ===
            float stereoL = widthL * panL;
            float stereoR = widthR * panR;

            // === MIX ===
            outputL[i] = mono * (1.0f - mix) + stereoL * mix;
            outputR[i] = mono * (1.0f - mix) + stereoR * mix;
        }

        // Mark output as stereo
        outputChannels = 2;
    }

    @Override
    protected void onReset() {
        if (haasDelayL != null) haasDelayL.clear();
        if (haasDelayR != null) haasDelayR.clear();
        if (enhanceFilterL != null) enhanceFilterL.reset();
        if (enhanceFilterR != null) enhanceFilterR.reset();
        if (allpassL != null) allpassL.reset();
        if (allpassR != null) allpassR.reset();
        if (panLfo != null) panLfo.reset();
    }

    @Override
    public int getLatency() {
        // Haas delay introduces latency only on one channel, not overall
        return 0;
    }

    // === Convenience setters ===

    public void setPan(float percent) {
        panParam.setValue(percent);
    }

    public void setWidth(float percent) {
        widthParam.setValue(percent);
    }

    public void setHaasDelay(float ms) {
        haasDelayParam.setValue(ms);
    }

    public void setEnhance(float percent) {
        enhanceParam.setValue(percent);
    }
}
