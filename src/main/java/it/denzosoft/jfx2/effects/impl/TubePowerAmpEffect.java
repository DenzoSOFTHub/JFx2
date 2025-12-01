package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Tube Power Amplifier Emulation Effect.
 *
 * <p>Simulates the power amplifier section of a tube guitar amplifier,
 * including output tubes, output transformer, and power supply characteristics.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Multiple power tube types (EL34, 6L6, EL84, 6V6, KT88, etc.)</li>
 *   <li>Class A and Class AB operation modes</li>
 *   <li>Push-pull and single-ended configurations</li>
 *   <li>Adjustable bias (cold to hot)</li>
 *   <li>Power supply sag simulation</li>
 *   <li>Output transformer saturation</li>
 *   <li>Negative feedback control</li>
 *   <li>Presence and Resonance controls</li>
 * </ul></p>
 */
public class TubePowerAmpEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "tubepoweramp",
            "Tube Power Amp",
            "Vacuum tube power amplifier simulation with Class A/AB operation",
            EffectCategory.AMP_SIM
    );

    // Parameters
    private final Parameter tubeTypeParam;
    private final Parameter classTypeParam;
    private final Parameter topologyParam;
    private final Parameter biasParam;
    private final Parameter driveParam;
    private final Parameter sagParam;
    private final Parameter transformerParam;
    private final Parameter feedbackParam;
    private final Parameter presenceParam;
    private final Parameter resonanceParam;
    private final Parameter masterParam;

    // Current state
    private PowerTubeType currentTubeType = PowerTubeType.EL34;
    private int sampleRate;

    // Sag state
    private float sagLevel = 1.0f;
    private float sagAttack;
    private float sagRelease;
    private float sagCapacitor = 0;

    // Push-pull state
    private float tubeAState = 0;
    private float tubeBState = 0;

    // Transformer state
    private float transformerCore = 0;
    private float transformerSaturation = 0;

    // Feedback filters for presence/resonance
    private float[] presenceState = new float[2];
    private float[] resonanceState = new float[2];

    // Output filtering
    private float[] outputLPState = new float[2];

    // DC blocking
    private float dcBlockState = 0;
    private static final float DC_BLOCK_COEFF = 0.995f;

    // Crossover distortion state (for Class AB)
    private float lastOutput = 0;

    public TubePowerAmpEffect() {
        super(METADATA);

        // Tube type selection (0-7 mapped to PowerTubeType enum)
        tubeTypeParam = addFloatParameter("tubeType", "Tube Type",
                "0=EL34, 1=6L6, 2=EL84, 3=6V6, 4=KT88, 5=KT66, 6=6550, 7=5881",
                0.0f, 7.0f, 0.0f, "");

        // Class type: 0 = Class A, 1 = Class AB
        classTypeParam = addFloatParameter("classType", "Class",
                "0=Class A (warm, compressed), 1=Class AB (tight, powerful)",
                0.0f, 1.0f, 1.0f, "");

        // Topology: 0 = Push-Pull, 1 = Single-Ended
        topologyParam = addFloatParameter("topology", "Topology",
                "0=Push-Pull (tight, powerful), 1=Single-Ended (warm, 2nd harmonics)",
                0.0f, 1.0f, 0.0f, "");

        // Bias: -100 (very cold) to +100 (very hot)
        biasParam = addFloatParameter("bias", "Bias",
                "Cold=less current, more crossover distortion; Hot=more current, warmer",
                -100.0f, 100.0f, 0.0f, "%");

        // Drive into power section
        driveParam = addFloatParameter("drive", "Drive",
                "Amount of signal driving the power tubes",
                0.0f, 100.0f, 50.0f, "%");

        // Sag amount
        sagParam = addFloatParameter("sag", "Sag",
                "Power supply sag - creates compression and 'feel'",
                0.0f, 100.0f, 50.0f, "%");

        // Transformer saturation
        transformerParam = addFloatParameter("transformer", "Transformer",
                "Output transformer saturation - adds warmth and compression",
                0.0f, 100.0f, 50.0f, "%");

        // Negative feedback amount
        feedbackParam = addFloatParameter("feedback", "Neg Feedback",
                "Negative feedback: 0=none (loose, raw), 100=full (tight, clean)",
                0.0f, 100.0f, 50.0f, "%");

        // Presence (high frequency boost in feedback loop)
        presenceParam = addFloatParameter("presence", "Presence",
                "High frequency emphasis (works with negative feedback)",
                0.0f, 100.0f, 50.0f, "%");

        // Resonance (low frequency boost in feedback loop)
        resonanceParam = addFloatParameter("resonance", "Resonance",
                "Low frequency emphasis (depth control)",
                0.0f, 100.0f, 50.0f, "%");

        // Master volume
        masterParam = addFloatParameter("master", "Master",
                "Output level",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;

        // Sag time constants (power supply)
        sagAttack = (float) Math.exp(-1.0 / (sampleRate * 0.005));   // 5ms attack
        sagRelease = (float) Math.exp(-1.0 / (sampleRate * 0.05));   // 50ms release

        onReset();
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Get current tube type
        int tubeIndex = Math.min((int) tubeTypeParam.getValue(), PowerTubeType.values().length - 1);
        currentTubeType = PowerTubeType.values()[tubeIndex];

        // Get parameters
        boolean classA = classTypeParam.getValue() < 0.5f;
        boolean singleEnded = topologyParam.getValue() > 0.5f;
        float bias = biasParam.getValue() / 100.0f;
        float drive = (driveParam.getValue() / 100.0f) * 3.0f + 0.5f;
        float sagAmount = sagParam.getValue() / 100.0f * currentTubeType.getSagAmount();
        float transformerDrive = transformerParam.getValue() / 100.0f;
        float feedback = feedbackParam.getValue() / 100.0f;
        float presence = presenceParam.getValue() / 100.0f;
        float resonance = resonanceParam.getValue() / 100.0f;
        float master = masterParam.getValue() / 100.0f * 2.0f;

        // Tube characteristics
        float headroom = currentTubeType.getHeadroom();
        float evenHarm = currentTubeType.getEvenHarmonics();
        float oddHarm = currentTubeType.getOddHarmonics();
        float compression = currentTubeType.getCompression();
        float bassTightness = currentTubeType.getBassTightness();

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i] * drive;

            // === POWER SUPPLY SAG ===
            // Simulate power supply capacitor discharge under load
            float load = Math.abs(sample);
            sagCapacitor = sagCapacitor * 0.9999f + load * 0.0001f;

            // Sag affects available voltage
            float sagVoltage = 1.0f - sagAmount * Math.min(sagCapacitor, 1.0f) * 0.4f;

            // === NEGATIVE FEEDBACK ===
            // Feedback reduces gain and tightens response
            float feedbackSignal = lastOutput * feedback * 0.5f;
            sample -= feedbackSignal;

            // === POWER TUBE STAGE ===
            float tubeOutput;

            if (singleEnded) {
                // Single-ended: one tube, asymmetric clipping, more even harmonics
                tubeOutput = processPowerTubeSE(sample, bias, headroom * sagVoltage,
                        evenHarm, oddHarm, compression, classA);
            } else {
                // Push-pull: two tubes, symmetric clipping
                tubeOutput = processPowerTubePP(sample, bias, headroom * sagVoltage,
                        evenHarm, oddHarm, compression, classA);
            }

            // === OUTPUT TRANSFORMER ===
            // Transformer saturation adds warmth and limits bass
            tubeOutput = processTransformer(tubeOutput, transformerDrive, bassTightness);

            // === PRESENCE / RESONANCE ===
            // These work in the feedback loop
            float presenceBoost = processPresence(tubeOutput, presence);
            float resonanceBoost = processResonance(tubeOutput, resonance);

            // Mix in presence/resonance
            tubeOutput += presenceBoost * (1.0f - feedback) * 0.3f;
            tubeOutput += resonanceBoost * (1.0f - feedback) * 0.2f;

            // === OUTPUT ===
            // DC blocking
            float dcBlocked = tubeOutput - dcBlockState;
            dcBlockState = tubeOutput - dcBlocked * DC_BLOCK_COEFF;

            // Apply master and tube characteristics
            float finalOut = dcBlocked * master;

            // Apply frequency response
            finalOut *= currentTubeType.getMidEmphasis();

            // Soft limiting
            if (finalOut > 1.0f) finalOut = 1.0f - 0.5f / (finalOut + 0.5f);
            else if (finalOut < -1.0f) finalOut = -1.0f + 0.5f / (-finalOut + 0.5f);

            lastOutput = finalOut;
            output[i] = finalOut;
        }
    }

    /**
     * Single-ended power tube (one tube, asymmetric).
     */
    private float processPowerTubeSE(float input, float bias, float headroom,
                                      float evenHarm, float oddHarm, float compression, boolean classA) {
        // Single-ended has more even harmonics (2nd, 4th)
        float biasedInput = input + bias * 0.3f;

        // Asymmetric clipping characteristic
        float output;
        float x = biasedInput / headroom;

        if (x >= 0) {
            // Positive swing: soft saturation
            if (x < 1.5f) {
                output = x - evenHarm * 0.4f * x * x;
            } else {
                output = 1.5f - evenHarm * 0.4f * 1.5f * 1.5f +
                        (1.0f - 1.0f / (x - 1.5f + 1.0f)) * 0.3f;
            }
        } else {
            // Negative swing: earlier cutoff
            x = -x;
            if (x < 1.0f) {
                output = -x + evenHarm * 0.2f * x * x;
            } else {
                output = -1.0f + 0.2f / (x + 0.5f);
            }
        }

        // Class A has more compression
        if (classA) {
            output *= 1.0f / (1.0f + compression * 0.3f * Math.abs(output));
        }

        return output * headroom * 0.8f;
    }

    /**
     * Push-pull power tubes (two tubes, more symmetric).
     */
    private float processPowerTubePP(float input, float bias, float headroom,
                                      float evenHarm, float oddHarm, float compression, boolean classA) {
        float biasedInput = input + bias * 0.2f;
        float x = biasedInput / headroom;

        // Split signal for push-pull
        float posHalf = Math.max(0, x);
        float negHalf = Math.max(0, -x);

        // Process each "tube"
        float tubeAOut = processSinglePowerTube(posHalf, evenHarm, oddHarm, classA);
        float tubeBOut = processSinglePowerTube(negHalf, evenHarm, oddHarm, classA);

        // Combine (push-pull cancels even harmonics)
        float output = tubeAOut - tubeBOut;

        // Class AB crossover distortion (when both tubes are near cutoff)
        if (!classA) {
            float crossoverRegion = 0.1f + bias * 0.05f;
            if (Math.abs(x) < crossoverRegion) {
                // Add some crossover distortion
                float crossoverAmount = 1.0f - Math.abs(x) / crossoverRegion;
                output *= (1.0f - crossoverAmount * 0.2f * (1.0f - bias * 0.5f));
            }
        }

        // Update tube states for dynamics
        tubeAState = tubeAState * 0.99f + tubeAOut * 0.01f;
        tubeBState = tubeBState * 0.99f + tubeBOut * 0.01f;

        // Compression in Class A
        if (classA) {
            output *= 1.0f / (1.0f + compression * 0.2f * Math.abs(output));
        }

        return output * headroom;
    }

    /**
     * Process a single power tube half.
     */
    private float processSinglePowerTube(float input, float evenHarm, float oddHarm, boolean classA) {
        if (input <= 0) return 0;

        float output;
        if (input < 1.0f) {
            // Linear region with slight compression
            output = input - oddHarm * 0.1f * input * input * input;
        } else {
            // Saturation
            output = 1.0f - (1.0f - oddHarm * 0.3f) / (input + 0.5f);
        }

        return output;
    }

    /**
     * Output transformer simulation.
     */
    private float processTransformer(float input, float saturation, float bassTightness) {
        // Transformer core saturation (soft magnetic saturation)
        transformerCore = transformerCore * 0.9f + input * 0.1f;
        float coreSaturation = (float) Math.tanh(transformerCore * (0.5f + saturation));

        // Mix saturated and clean
        float output = input * (1.0f - saturation * 0.5f) + coreSaturation * saturation * 0.5f;

        // Transformer limits low frequencies (bass tightness)
        // Simple high-pass filter
        float hpFreq = 40.0f + (1.0f - bassTightness) * 60.0f;
        float rc = 1.0f / (2.0f * (float) Math.PI * hpFreq);
        float alpha = rc / (rc + 1.0f / sampleRate);
        float filtered = alpha * (transformerSaturation + output - transformerSaturation);
        transformerSaturation = output;

        return filtered;
    }

    /**
     * Presence control (high frequency emphasis).
     */
    private float processPresence(float input, float presence) {
        // High shelf boost around 3-5kHz
        float freq = 4000.0f;
        float gain = presence * 12.0f; // 0 to +12dB
        return processHighShelf(input, freq, gain, presenceState) - input;
    }

    /**
     * Resonance control (low frequency emphasis).
     */
    private float processResonance(float input, float resonance) {
        // Low shelf boost around 80-100Hz
        float freq = 90.0f;
        float gain = resonance * 10.0f; // 0 to +10dB
        return processLowShelf(input, freq, gain, resonanceState) - input;
    }

    private float processHighShelf(float input, float freq, float gainDb, float[] state) {
        if (gainDb < 0.1f) return input;

        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / 2.0f * 0.707f;
        float sqrtGain = (float) Math.sqrt(gain);

        float a0 = (gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
        float a1 = 2 * ((gain - 1) - (gain + 1) * cosW0);
        float a2 = (gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
        float b0 = gain * ((gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
        float b1 = -2 * gain * ((gain - 1) + (gain + 1) * cosW0);
        float b2 = gain * ((gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha);

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    private float processLowShelf(float input, float freq, float gainDb, float[] state) {
        if (gainDb < 0.1f) return input;

        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / 2.0f * 0.707f;
        float sqrtGain = (float) Math.sqrt(gain);

        float a0 = (gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
        float a1 = -2 * ((gain - 1) + (gain + 1) * cosW0);
        float a2 = (gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
        float b0 = gain * ((gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
        float b1 = 2 * gain * ((gain - 1) - (gain + 1) * cosW0);
        float b2 = gain * ((gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha);

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Power amps are typically mono
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        float[] monoIn = new float[len];
        float[] monoOut = new float[len];

        for (int i = 0; i < len; i++) {
            monoIn[i] = (inputL[i] + inputR[i]) * 0.5f;
        }

        onProcess(monoIn, monoOut, len);

        System.arraycopy(monoOut, 0, outputL, 0, len);
        System.arraycopy(monoOut, 0, outputR, 0, len);
    }

    @Override
    protected void onReset() {
        sagLevel = 1.0f;
        sagCapacitor = 0;
        tubeAState = 0;
        tubeBState = 0;
        transformerCore = 0;
        transformerSaturation = 0;
        java.util.Arrays.fill(presenceState, 0);
        java.util.Arrays.fill(resonanceState, 0);
        java.util.Arrays.fill(outputLPState, 0);
        dcBlockState = 0;
        lastOutput = 0;
    }

    // Convenience methods
    public void setTubeType(PowerTubeType type) {
        for (int i = 0; i < PowerTubeType.values().length; i++) {
            if (PowerTubeType.values()[i] == type) {
                tubeTypeParam.setValue(i);
                break;
            }
        }
    }

    public PowerTubeType getTubeType() {
        int index = Math.min((int) tubeTypeParam.getValue(), PowerTubeType.values().length - 1);
        return PowerTubeType.values()[index];
    }

    public void setClassA(boolean classA) { classTypeParam.setValue(classA ? 0 : 1); }
    public void setSingleEnded(boolean singleEnded) { topologyParam.setValue(singleEnded ? 1 : 0); }
    public void setBias(float percent) { biasParam.setValue(percent); }
    public void setDrive(float percent) { driveParam.setValue(percent); }
    public void setSag(float percent) { sagParam.setValue(percent); }
    public void setTransformer(float percent) { transformerParam.setValue(percent); }
    public void setFeedback(float percent) { feedbackParam.setValue(percent); }
    public void setPresence(float percent) { presenceParam.setValue(percent); }
    public void setResonance(float percent) { resonanceParam.setValue(percent); }
    public void setMaster(float percent) { masterParam.setValue(percent); }
}
