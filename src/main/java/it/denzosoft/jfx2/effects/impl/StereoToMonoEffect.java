package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Intelligent Stereo to Mono Converter.
 *
 * <p>Provides multiple modes for converting stereo to mono with
 * intelligent handling of phase issues and frequency-dependent processing:</p>
 *
 * <ul>
 *   <li><b>Sum (L+R)</b> - Classic mono sum with level compensation</li>
 *   <li><b>Left Only</b> - Use left channel only</li>
 *   <li><b>Right Only</b> - Use right channel only</li>
 *   <li><b>Mid</b> - Extract center/mono content (L+R)/2</li>
 *   <li><b>Side</b> - Extract stereo difference (L-R)/2</li>
 *   <li><b>Smart</b> - Phase-aware summing with correlation detection</li>
 *   <li><b>Bass Mono</b> - Mono below crossover, stereo above</li>
 * </ul>
 *
 * <p>Features phase correlation metering and automatic phase correction
 * to prevent cancellation artifacts.</p>
 */
public class StereoToMonoEffect extends AbstractEffect {

    // Parameters
    private Parameter mode;
    private Parameter phaseCorrect;   // Auto-correct phase issues
    private Parameter crossover;      // For Bass Mono mode
    private Parameter width;          // Pre-conversion width adjustment
    private Parameter balance;        // L/R balance before conversion
    private Parameter outputGain;     // Output level compensation

    // DSP state
    private int currentSampleRate = 44100;

    // Low-pass filter for bass mono (Linkwitz-Riley 2nd order)
    private double lpStateL1 = 0, lpStateL2 = 0;
    private double lpStateR1 = 0, lpStateR2 = 0;
    private double hpStateL1 = 0, hpStateL2 = 0;
    private double hpStateR1 = 0, hpStateR2 = 0;

    // Phase correlation detection
    private float correlationSum = 0;
    private float energyL = 0;
    private float energyR = 0;
    private int correlationSamples = 0;
    private static final int CORRELATION_WINDOW = 1024;
    private float currentCorrelation = 1.0f;

    // Phase detection for auto-correction
    private float phaseAccumulator = 0;

    public StereoToMonoEffect() {
        super(EffectMetadata.of("stereo2mono", "Stereo→Mono",
                "Intelligent stereo to mono conversion with phase handling", EffectCategory.UTILITY));
        initParameters();
    }

    private void initParameters() {
        mode = addChoiceParameter("mode", "Mode",
                "Conversion mode for stereo to mono",
                new String[]{"Sum (L+R)", "Left Only", "Right Only", "Mid", "Side", "Smart", "Bass Mono"}, 0);

        phaseCorrect = addChoiceParameter("phaseCorrect", "Phase",
                "Phase correction mode",
                new String[]{"Off", "Auto", "Invert L", "Invert R"}, 1);

        crossover = addFloatParameter("crossover", "Crossover",
                "Frequency below which signal is summed to mono (Bass Mono mode)",
                20.0f, 500.0f, 120.0f, "Hz");

        width = addFloatParameter("width", "Width",
                "Stereo width adjustment before conversion (100% = original)",
                0.0f, 200.0f, 100.0f, "%");

        balance = addFloatParameter("balance", "Balance",
                "L/R balance before conversion (50% = center)",
                0.0f, 100.0f, 50.0f, "%");

        outputGain = addFloatParameter("output", "Output",
                "Output gain compensation",
                -12.0f, 12.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.currentSampleRate = sampleRate;
        resetFilters();
        correlationSum = 0;
        energyL = 0;
        energyR = 0;
        correlationSamples = 0;
        currentCorrelation = 1.0f;
        phaseAccumulator = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono input - just pass through
        System.arraycopy(input, 0, output, 0, frameCount);
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                    float[] outputL, float[] outputR, int frameCount) {
        int modeIdx = mode.getChoiceIndex();
        int phaseMode = phaseCorrect.getChoiceIndex();
        float crossoverFreq = crossover.getValue();
        float widthAmt = width.getValue() / 100.0f;
        float balanceAmt = balance.getValue() / 100.0f;
        float gainDb = outputGain.getValue();
        float gain = (float) Math.pow(10, gainDb / 20.0);

        for (int i = 0; i < frameCount; i++) {
            float left = inputL[i];
            float right = inputR[i];

            // Apply phase correction if needed
            if (phaseMode == 2) {
                left = -left;  // Invert L
            } else if (phaseMode == 3) {
                right = -right;  // Invert R
            } else if (phaseMode == 1) {
                // Auto phase correction based on correlation
                updateCorrelation(left, right);
                if (currentCorrelation < -0.5f) {
                    // Severe phase cancellation detected - invert one channel
                    right = -right;
                }
            }

            // Apply balance
            float balL = balanceAmt <= 0.5f ? 1.0f : 2.0f * (1.0f - balanceAmt);
            float balR = balanceAmt >= 0.5f ? 1.0f : 2.0f * balanceAmt;
            left *= balL;
            right *= balR;

            // Apply width adjustment (Mid/Side processing)
            float mid = (left + right) * 0.5f;
            float side = (left - right) * 0.5f;
            side *= widthAmt;
            left = mid + side;
            right = mid - side;

            // Convert to mono based on mode
            float mono = 0;
            switch (modeIdx) {
                case 0 -> {
                    // Sum (L+R) with -3dB compensation to prevent clipping
                    mono = (left + right) * 0.707f;
                }
                case 1 -> {
                    // Left Only
                    mono = left;
                }
                case 2 -> {
                    // Right Only
                    mono = right;
                }
                case 3 -> {
                    // Mid (center content)
                    mono = (left + right) * 0.5f;
                }
                case 4 -> {
                    // Side (difference content)
                    mono = (left - right) * 0.5f;
                }
                case 5 -> {
                    // Smart - correlation-weighted sum
                    mono = smartSum(left, right);
                }
                case 6 -> {
                    // Bass Mono - mono below crossover, preserve stereo above
                    mono = bassMonoProcess(left, right, crossoverFreq);
                }
                default -> mono = (left + right) * 0.707f;
            }

            // Apply output gain
            mono *= gain;

            // Output mono to both channels
            outputL[i] = mono;
            outputR[i] = mono;
        }
    }

    /**
     * Update phase correlation measurement.
     */
    private void updateCorrelation(float left, float right) {
        correlationSum += left * right;
        energyL += left * left;
        energyR += right * right;
        correlationSamples++;

        if (correlationSamples >= CORRELATION_WINDOW) {
            // Calculate normalized correlation coefficient
            float denominator = (float) Math.sqrt(energyL * energyR);
            if (denominator > 0.0001f) {
                currentCorrelation = correlationSum / denominator;
            } else {
                currentCorrelation = 1.0f;
            }

            // Reset accumulators
            correlationSum = 0;
            energyL = 0;
            energyR = 0;
            correlationSamples = 0;
        }
    }

    /**
     * Smart summing based on correlation.
     * Adjusts the mix to minimize phase cancellation.
     */
    private float smartSum(float left, float right) {
        // Calculate instantaneous correlation
        float instantCorr = left * right;
        float instantEnergy = (left * left + right * right) * 0.5f;

        if (instantEnergy < 0.0001f) {
            return (left + right) * 0.5f;
        }

        // Smooth the phase accumulator
        float alpha = 0.01f;
        phaseAccumulator = phaseAccumulator * (1 - alpha) + (instantCorr / (instantEnergy + 0.0001f)) * alpha;

        // If correlation is negative (out of phase), reduce the problematic channel
        if (phaseAccumulator < 0) {
            // Signals are out of phase - use the louder one more
            float absL = Math.abs(left);
            float absR = Math.abs(right);
            if (absL > absR) {
                return left * 0.8f + right * 0.2f;
            } else {
                return left * 0.2f + right * 0.8f;
            }
        } else {
            // In phase - normal sum
            return (left + right) * 0.707f;
        }
    }

    /**
     * Bass mono processing - mono below crossover frequency.
     */
    private float bassMonoProcess(float left, float right, float crossoverFreq) {
        // Calculate filter coefficients (Butterworth low-pass)
        double w0 = 2.0 * Math.PI * crossoverFreq / currentSampleRate;
        double cosW0 = Math.cos(w0);
        double alpha = Math.sin(w0) / Math.sqrt(2.0);  // Q = 0.707 for Butterworth

        // Low-pass coefficients
        double b0_lp = (1 - cosW0) / 2;
        double b1_lp = 1 - cosW0;
        double b2_lp = (1 - cosW0) / 2;
        double a0 = 1 + alpha;
        double a1 = -2 * cosW0;
        double a2 = 1 - alpha;

        // Normalize
        b0_lp /= a0; b1_lp /= a0; b2_lp /= a0;
        a1 /= a0; a2 /= a0;

        // High-pass coefficients
        double b0_hp = (1 + cosW0) / 2 / a0;
        double b1_hp = -(1 + cosW0) / a0;
        double b2_hp = (1 + cosW0) / 2 / a0;

        // Apply low-pass to get bass
        double bassL = b0_lp * left + lpStateL1;
        lpStateL1 = b1_lp * left - a1 * bassL + lpStateL2;
        lpStateL2 = b2_lp * left - a2 * bassL;

        double bassR = b0_lp * right + lpStateR1;
        lpStateR1 = b1_lp * right - a1 * bassR + lpStateR2;
        lpStateR2 = b2_lp * right - a2 * bassR;

        // Apply high-pass to get highs
        double highL = b0_hp * left + hpStateL1;
        hpStateL1 = b1_hp * left - a1 * highL + hpStateL2;
        hpStateL2 = b2_hp * left - a2 * highL;

        double highR = b0_hp * right + hpStateR1;
        hpStateR1 = b1_hp * right - a1 * highR + hpStateR2;
        hpStateR2 = b2_hp * right - a2 * highR;

        // Sum bass to mono, keep highs stereo average
        float bassMono = (float) ((bassL + bassR) * 0.5);
        float highsMono = (float) ((highL + highR) * 0.5);

        return bassMono + highsMono;
    }

    /**
     * Reset all filter states.
     */
    private void resetFilters() {
        lpStateL1 = lpStateL2 = 0;
        lpStateR1 = lpStateR2 = 0;
        hpStateL1 = hpStateL2 = 0;
        hpStateR1 = hpStateR2 = 0;
    }

    @Override
    public void reset() {
        resetFilters();
        correlationSum = 0;
        energyL = 0;
        energyR = 0;
        correlationSamples = 0;
        currentCorrelation = 1.0f;
        phaseAccumulator = 0;
    }
}
