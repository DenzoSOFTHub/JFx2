package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Tube Preamp Emulation Effect.
 *
 * <p>Simulates a vacuum tube preamplifier with configurable tube types,
 * bias settings, and circuit parameters that affect the tone.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Multiple tube types (12AX7, 12AT7, 12AU7, EF86, etc.)</li>
 *   <li>Adjustable bias (cold to hot)</li>
 *   <li>Cathode vs Fixed bias modes</li>
 *   <li>Plate voltage simulation</li>
 *   <li>Cathode bypass capacitor</li>
 *   <li>Voltage sag under load</li>
 *   <li>Multi-stage gain structure</li>
 *   <li>3-band EQ (Bass, Mid, Treble)</li>
 * </ul></p>
 */
public class TubePreampEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "tubepreamp",
            "Tube Preamp",
            "Vacuum tube preamplifier simulation with realistic tube characteristics",
            EffectCategory.AMP_SIM
    );

    // Parameters
    private final Parameter tubeTypeParam;
    private final Parameter biasParam;
    private final Parameter biasTypeParam;
    private final Parameter plateVoltageParam;
    private final Parameter cathodeBypassParam;
    private final Parameter stage1GainParam;
    private final Parameter stage2GainParam;
    private final Parameter sagParam;
    private final Parameter bassParam;
    private final Parameter midParam;
    private final Parameter trebleParam;
    private final Parameter outputParam;

    // Current tube type
    private TubeType currentTubeType = TubeType.ECC83_12AX7;

    // DSP state
    private int sampleRate;

    // Filters for tone stack
    private float[] bassState = new float[2];
    private float[] midState = new float[2];
    private float[] trebleState = new float[2];

    // Sag state
    private float sagLevel = 1.0f;
    private float sagAttack;
    private float sagRelease;

    // Cathode bypass filter state
    private float cathodeBypassState = 0;

    // DC blocking
    private float dcBlockState = 0;
    private static final float DC_BLOCK_COEFF = 0.995f;

    // Stage state for inter-stage coupling
    private float stage1Output = 0;
    private float stage2Output = 0;

    public TubePreampEffect() {
        super(METADATA);

        // Tube type selection (0-7 mapped to TubeType enum)
        tubeTypeParam = addFloatParameter("tubeType", "Tube Type",
                "Select tube type: 0=12AX7, 1=12AT7, 2=12AU7, 3=12AY7, 4=5751, 5=EF86, 6=6SL7, 7=6SN7",
                0.0f, 7.0f, 0.0f, "");

        // Bias: -100 (very cold) to +100 (very hot), 0 = normal
        biasParam = addFloatParameter("bias", "Bias",
                "Tube bias point: negative=cold (less current, harsh), positive=hot (more current, warm)",
                -100.0f, 100.0f, 0.0f, "%");

        // Bias type: 0 = Cathode (self-biasing, more sag), 1 = Fixed (tighter response)
        biasTypeParam = addFloatParameter("biasType", "Bias Type",
                "0=Cathode (self-biasing, more compression), 1=Fixed (tighter, crisper)",
                0.0f, 1.0f, 0.0f, "");

        // Plate voltage: affects headroom and saturation character
        plateVoltageParam = addFloatParameter("plateVoltage", "Plate Voltage",
                "Higher voltage = more headroom, lower = earlier breakup",
                50.0f, 150.0f, 100.0f, "%");

        // Cathode bypass: 0 = no bypass (less gain, tighter bass), 100 = full bypass (more gain, fuller bass)
        cathodeBypassParam = addFloatParameter("cathodeBypass", "Cathode Bypass",
                "Cathode bypass capacitor amount - more = fuller bass and higher gain",
                0.0f, 100.0f, 50.0f, "%");

        // Stage 1 gain
        stage1GainParam = addFloatParameter("stage1Gain", "Stage 1 Gain",
                "First gain stage drive",
                0.0f, 100.0f, 50.0f, "%");

        // Stage 2 gain
        stage2GainParam = addFloatParameter("stage2Gain", "Stage 2 Gain",
                "Second gain stage drive",
                0.0f, 100.0f, 50.0f, "%");

        // Sag: power supply voltage drop under load
        sagParam = addFloatParameter("sag", "Sag",
                "Power supply sag - creates compression and feel",
                0.0f, 100.0f, 30.0f, "%");

        // Tone stack
        bassParam = addFloatParameter("bass", "Bass",
                "Low frequency response",
                0.0f, 100.0f, 50.0f, "%");

        midParam = addFloatParameter("mid", "Mid",
                "Mid frequency response",
                0.0f, 100.0f, 50.0f, "%");

        trebleParam = addFloatParameter("treble", "Treble",
                "High frequency response",
                0.0f, 100.0f, 50.0f, "%");

        // Output level
        outputParam = addFloatParameter("output", "Output",
                "Output level",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;

        // Sag time constants
        sagAttack = (float) Math.exp(-1.0 / (sampleRate * 0.01));   // 10ms attack
        sagRelease = (float) Math.exp(-1.0 / (sampleRate * 0.1));   // 100ms release

        // Reset state
        onReset();
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Get current tube type
        int tubeIndex = Math.min((int) tubeTypeParam.getValue(), TubeType.values().length - 1);
        currentTubeType = TubeType.values()[tubeIndex];

        // Get parameters
        float bias = biasParam.getValue() / 100.0f;
        boolean cathodeBias = biasTypeParam.getValue() < 0.5f;
        float plateVoltage = plateVoltageParam.getValue() / 100.0f;
        float cathodeBypass = cathodeBypassParam.getValue() / 100.0f;
        float stage1Drive = stage1GainParam.getValue() / 100.0f;
        float stage2Drive = stage2GainParam.getValue() / 100.0f;
        float sagAmount = sagParam.getValue() / 100.0f;
        float bass = bassParam.getValue() / 100.0f;
        float mid = midParam.getValue() / 100.0f;
        float treble = trebleParam.getValue() / 100.0f;
        float outputLevel = outputParam.getValue() / 100.0f * 2.0f;

        // Tube characteristics
        float tubeGain = currentTubeType.getNormalizedGain();
        float evenHarm = currentTubeType.getEvenHarmonics();
        float oddHarm = currentTubeType.getOddHarmonics();
        float compression = currentTubeType.getCompression();

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // === STAGE 1 ===
            // Apply stage 1 drive
            float stage1In = sample * (0.5f + stage1Drive * 4.0f) * tubeGain;

            // Cathode bypass effect (affects low frequencies and gain)
            if (cathodeBypass < 1.0f) {
                // High-pass filter to simulate unbypassed cathode
                float cutoff = 80.0f + (1.0f - cathodeBypass) * 300.0f;
                float rc = 1.0f / (2.0f * (float) Math.PI * cutoff);
                float alpha = rc / (rc + 1.0f / sampleRate);
                float filtered = alpha * (cathodeBypassState + stage1In - stage1Output);
                cathodeBypassState = stage1In;
                stage1In = stage1In * cathodeBypass + filtered * (1.0f - cathodeBypass);
            }

            // Tube saturation stage 1
            float stage1Out = processTubeStage(stage1In, bias, plateVoltage,
                    evenHarm, oddHarm, compression, cathodeBias);

            // Update sag based on signal level
            float signalLevel = Math.abs(stage1Out);
            if (signalLevel > sagLevel) {
                sagLevel = sagLevel * sagAttack + signalLevel * (1 - sagAttack);
            } else {
                sagLevel = sagLevel * sagRelease + signalLevel * (1 - sagRelease);
            }

            // Apply sag (reduces voltage under load)
            float sagFactor = 1.0f - sagAmount * 0.3f * Math.min(sagLevel, 1.0f);
            stage1Out *= sagFactor;

            stage1Output = stage1Out;

            // === STAGE 2 ===
            // Inter-stage coupling (AC coupling with some low-end rolloff)
            float stage2In = stage1Out * (0.5f + stage2Drive * 4.0f) * tubeGain * 0.7f;

            // Tube saturation stage 2
            float stage2Out = processTubeStage(stage2In, bias, plateVoltage,
                    evenHarm, oddHarm, compression, cathodeBias);

            stage2Output = stage2Out;

            // === TONE STACK ===
            float toned = processToneStack(stage2Out, bass, mid, treble);

            // === OUTPUT ===
            // DC blocking
            float dcBlocked = toned - dcBlockState;
            dcBlockState = toned - dcBlocked * DC_BLOCK_COEFF;

            // Apply output level and tube frequency response
            float finalOut = dcBlocked * outputLevel;
            finalOut *= currentTubeType.getBassResponse() * 0.2f + 0.8f;

            // Soft clip output to prevent harsh digital clipping
            if (finalOut > 1.0f) finalOut = 1.0f - 1.0f / (finalOut + 1.0f);
            else if (finalOut < -1.0f) finalOut = -1.0f + 1.0f / (-finalOut + 1.0f);

            output[i] = finalOut;
        }
    }

    /**
     * Process a single tube gain stage.
     */
    private float processTubeStage(float input, float bias, float plateVoltage,
                                   float evenHarm, float oddHarm, float compression,
                                   boolean cathodeBias) {
        // Bias shifts the operating point
        // Positive bias = hotter = more even harmonics, earlier saturation
        // Negative bias = colder = more odd harmonics, harsher clipping
        float biasedInput = input + bias * 0.2f;

        // Plate voltage affects headroom
        // Higher voltage = more headroom before clipping
        float headroom = 0.5f + plateVoltage * 0.5f;
        float normalizedInput = biasedInput / headroom;

        // Tube transfer function (asymmetric soft clipping)
        // This creates the characteristic tube sound with even harmonics
        float output;

        if (normalizedInput >= 0) {
            // Positive half: softer clipping (grid conduction)
            float x = normalizedInput;
            if (x < 1.0f) {
                // Polynomial approximation of tube curve
                output = x - evenHarm * 0.25f * x * x + oddHarm * 0.1f * x * x * x;
            } else {
                // Soft saturation
                output = 1.0f - (1.0f - evenHarm * 0.5f) / (x + 1.0f);
            }
        } else {
            // Negative half: harder clipping (cutoff)
            float x = -normalizedInput;
            if (x < 1.0f) {
                output = -x - evenHarm * 0.15f * x * x - oddHarm * 0.15f * x * x * x;
            } else {
                // Harder cutoff
                output = -1.0f + (1.0f - oddHarm * 0.3f) / (x + 0.5f);
            }
        }

        // Cathode bias adds compression (self-biasing effect)
        if (cathodeBias) {
            float comp = 1.0f / (1.0f + compression * Math.abs(output) * 0.5f);
            output *= comp;
        }

        return output * headroom;
    }

    /**
     * Process the tone stack (3-band EQ).
     */
    private float processToneStack(float input, float bass, float mid, float treble) {
        // Bass filter (low shelf around 100Hz)
        float bassFreq = 100.0f;
        float bassGain = -12.0f + bass * 24.0f; // -12dB to +12dB
        float bassOut = processShelf(input, bassFreq, bassGain, bassState, true);

        // Mid filter (peaking around 500Hz)
        float midFreq = 500.0f;
        float midGain = -12.0f + mid * 24.0f;
        float midOut = processPeaking(bassOut, midFreq, midGain, 1.0f, midState);

        // Treble filter (high shelf around 2kHz)
        float trebleFreq = 2000.0f;
        float trebleGain = -12.0f + treble * 24.0f;
        float trebleOut = processShelf(midOut, trebleFreq, trebleGain, trebleState, false);

        // Apply tube's inherent frequency response
        trebleOut *= currentTubeType.getTrebleResponse();

        return trebleOut;
    }

    /**
     * Simple shelf filter.
     */
    private float processShelf(float input, float freq, float gainDb, float[] state, boolean lowShelf) {
        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / 2.0f * 0.707f;

        float a0, a1, a2, b0, b1, b2;

        if (lowShelf) {
            float sqrtGain = (float) Math.sqrt(gain);
            a0 = (gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
            a1 = -2 * ((gain - 1) + (gain + 1) * cosW0);
            a2 = (gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
            b0 = gain * ((gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
            b1 = 2 * gain * ((gain - 1) - (gain + 1) * cosW0);
            b2 = gain * ((gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha);
        } else {
            float sqrtGain = (float) Math.sqrt(gain);
            a0 = (gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
            a1 = 2 * ((gain - 1) - (gain + 1) * cosW0);
            a2 = (gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
            b0 = gain * ((gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
            b1 = -2 * gain * ((gain - 1) + (gain + 1) * cosW0);
            b2 = gain * ((gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha);
        }

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        // Direct Form II
        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    /**
     * Simple peaking filter.
     */
    private float processPeaking(float input, float freq, float gainDb, float q, float[] state) {
        float gain = (float) Math.pow(10.0, gainDb / 40.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / (2.0f * q);

        float a0 = 1.0f + alpha / gain;
        float a1 = -2.0f * cosW0;
        float a2 = 1.0f - alpha / gain;
        float b0 = 1.0f + alpha * gain;
        float b1 = -2.0f * cosW0;
        float b2 = 1.0f - alpha * gain;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        // Direct Form II
        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Process mono (preamps are typically mono)
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        float[] monoIn = new float[len];
        float[] monoOut = new float[len];

        // Mix to mono
        for (int i = 0; i < len; i++) {
            monoIn[i] = (inputL[i] + inputR[i]) * 0.5f;
        }

        // Process
        onProcess(monoIn, monoOut, len);

        // Copy to both channels
        System.arraycopy(monoOut, 0, outputL, 0, len);
        System.arraycopy(monoOut, 0, outputR, 0, len);
    }

    @Override
    protected void onReset() {
        java.util.Arrays.fill(bassState, 0);
        java.util.Arrays.fill(midState, 0);
        java.util.Arrays.fill(trebleState, 0);
        sagLevel = 1.0f;
        cathodeBypassState = 0;
        dcBlockState = 0;
        stage1Output = 0;
        stage2Output = 0;
    }

    // Convenience methods
    public void setTubeType(TubeType type) {
        for (int i = 0; i < TubeType.values().length; i++) {
            if (TubeType.values()[i] == type) {
                tubeTypeParam.setValue(i);
                break;
            }
        }
    }

    public TubeType getTubeType() {
        int index = Math.min((int) tubeTypeParam.getValue(), TubeType.values().length - 1);
        return TubeType.values()[index];
    }

    public void setBias(float percent) { biasParam.setValue(percent); }
    public void setCathodeBias(boolean cathode) { biasTypeParam.setValue(cathode ? 0 : 1); }
    public void setPlateVoltage(float percent) { plateVoltageParam.setValue(percent); }
    public void setCathodeBypass(float percent) { cathodeBypassParam.setValue(percent); }
    public void setStage1Gain(float percent) { stage1GainParam.setValue(percent); }
    public void setStage2Gain(float percent) { stage2GainParam.setValue(percent); }
    public void setSag(float percent) { sagParam.setValue(percent); }
    public void setBass(float percent) { bassParam.setValue(percent); }
    public void setMid(float percent) { midParam.setValue(percent); }
    public void setTreble(float percent) { trebleParam.setValue(percent); }
    public void setOutput(float percent) { outputParam.setValue(percent); }
}
