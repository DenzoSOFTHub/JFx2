package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.FFTConvolver;
import it.denzosoft.jfx2.effects.*;

/**
 * Cabinet simulation effect using FFT convolution.
 *
 * <p>Simulates different speaker cabinet types with built-in impulse responses.
 * Includes: 1x12, 2x12, 4x12 configurations with different speaker characters.</p>
 */
public class CabinetSimEffect extends AbstractEffect {

    private static final int IR_LENGTH = 2048;  // ~46ms at 44.1kHz

    // Convolvers - Left and Right
    private final FFTConvolver convolverL;
    private final FFTConvolver convolverR;

    private Parameter cabinetType;
    private Parameter micPosition;
    private Parameter lowCut;
    private Parameter highCut;
    private Parameter mix;

    private float[] currentIR;
    private int lastCabinetType = -1;
    private int lastMicPosition = -1;

    // Simple filters for tone shaping - Left
    private float lowCutStateL;
    private float highCutStateL;

    // Simple filters for tone shaping - Right
    private float lowCutStateR;
    private float highCutStateR;

    private float lowCutCoeff;
    private float highCutCoeff;

    public CabinetSimEffect() {
        super(EffectMetadata.of("cabsim", "Cabinet Sim", "Speaker cabinet simulation with IR convolution", EffectCategory.AMP_SIM));

        convolverL = new FFTConvolver();
        convolverR = new FFTConvolver();
        currentIR = new float[IR_LENGTH];

        initParameters();
    }

    private void initParameters() {
        cabinetType = addChoiceParameter("cabinet", "Cabinet",
                "Speaker cabinet type: 1x12 (warm), 2x12 (balanced), 4x12 British (classic), 4x12 Modern (tight), Direct (bypass).",
                new String[]{"1x12 Vintage", "2x12 Open", "4x12 British", "4x12 Modern", "Direct"}, 0);

        micPosition = addChoiceParameter("mic", "Mic Position",
                "Microphone placement: Center (bright), Edge (warm), Back (muffled), Room (ambient).",
                new String[]{"Center", "Edge", "Back", "Room"}, 0);

        lowCut = addFloatParameter("lowCut", "Low Cut",
                "Removes low frequencies below this point. Higher values tighten the bass response.",
                20.0f, 500.0f, 80.0f, "Hz");
        highCut = addFloatParameter("highCut", "High Cut",
                "Removes high frequencies above this point. Lower values create darker, smoother tone.",
                2000.0f, 12000.0f, 8000.0f, "Hz");
        mix = addFloatParameter("mix", "Mix",
                "Balance between dry signal and cabinet simulation. 100% for realistic amp tones.",
                0.0f, 100.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Generate IR based on cabinet type
        generateIR();
        convolverL.prepare(currentIR, IR_LENGTH, maxFrameCount);
        convolverR.prepare(currentIR, IR_LENGTH, maxFrameCount);

        // Initialize filters
        updateFilters();
        lowCutStateL = 0;
        highCutStateL = 0;
        lowCutStateR = 0;
        highCutStateR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        int cabType = cabinetType.getChoiceIndex();
        int micPos = micPosition.getChoiceIndex();

        if (cabType != lastCabinetType || micPos != lastMicPosition) {
            generateIR();
            convolverL.prepare(currentIR, IR_LENGTH, maxFrameCount);
            lastCabinetType = cabType;
            lastMicPosition = micPos;
        }

        updateFilters();
        float mixAmt = mix.getValue() / 100.0f;

        float[] convolved = new float[frameCount];
        convolverL.process(input, convolved, frameCount);

        float lc = lowCutCoeff;
        float hc = highCutCoeff;

        for (int i = 0; i < frameCount; i++) {
            float sample = convolved[i];

            lowCutStateL += lc * (sample - lowCutStateL);
            sample = sample - lowCutStateL;

            highCutStateL += hc * (sample - highCutStateL);
            sample = highCutStateL;

            output[i] = input[i] * (1.0f - mixAmt) + sample * mixAmt;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        int cabType = cabinetType.getChoiceIndex();
        int micPos = micPosition.getChoiceIndex();

        if (cabType != lastCabinetType || micPos != lastMicPosition) {
            generateIR();
            convolverL.prepare(currentIR, IR_LENGTH, maxFrameCount);
            convolverR.prepare(currentIR, IR_LENGTH, maxFrameCount);
            lastCabinetType = cabType;
            lastMicPosition = micPos;
        }

        updateFilters();
        float mixAmt = mix.getValue() / 100.0f;

        float[] convolvedL = new float[frameCount];
        float[] convolvedR = new float[frameCount];
        convolverL.process(inputL, convolvedL, frameCount);
        convolverR.process(inputR, convolvedR, frameCount);

        float lc = lowCutCoeff;
        float hc = highCutCoeff;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            // Left channel
            float sampleL = convolvedL[i];
            lowCutStateL += lc * (sampleL - lowCutStateL);
            sampleL = sampleL - lowCutStateL;
            highCutStateL += hc * (sampleL - highCutStateL);
            sampleL = highCutStateL;
            outputL[i] = inputL[i] * (1.0f - mixAmt) + sampleL * mixAmt;

            // Right channel
            float sampleR = convolvedR[i];
            lowCutStateR += lc * (sampleR - lowCutStateR);
            sampleR = sampleR - lowCutStateR;
            highCutStateR += hc * (sampleR - highCutStateR);
            sampleR = highCutStateR;
            outputR[i] = inputR[i] * (1.0f - mixAmt) + sampleR * mixAmt;
        }
    }

    @Override
    protected void onReset() {
        convolverL.reset();
        convolverR.reset();
        lowCutStateL = 0;
        highCutStateL = 0;
        lowCutStateR = 0;
        highCutStateR = 0;
    }

    private void updateFilters() {
        float lcFreq = lowCut.getValue();
        float hcFreq = highCut.getValue();

        lowCutCoeff = (float) (2.0 * Math.PI * lcFreq / sampleRate);
        lowCutCoeff = Math.min(lowCutCoeff, 1.0f);

        highCutCoeff = (float) (2.0 * Math.PI * hcFreq / sampleRate);
        highCutCoeff = Math.min(highCutCoeff, 1.0f);
    }

    /**
     * Generate impulse response based on cabinet type and mic position.
     * These are synthetic IRs approximating real cabinet characteristics.
     */
    private void generateIR() {
        int cabType = cabinetType.getChoiceIndex();
        int micPos = micPosition.getChoiceIndex();

        // Clear IR
        java.util.Arrays.fill(currentIR, 0);

        if (cabType == 4) {
            // Direct - bypass (delta impulse)
            currentIR[0] = 1.0f;
            return;
        }

        // Cabinet characteristics
        float resonanceFreq;  // Speaker resonance
        float roomSize;       // Reverberant tail length
        float brightness;     // High frequency content
        float lowEnd;         // Low frequency emphasis

        switch (cabType) {
            case 0:  // 1x12 Vintage (small, warm)
                resonanceFreq = 100.0f;
                roomSize = 0.3f;
                brightness = 0.4f;
                lowEnd = 0.7f;
                break;
            case 1:  // 2x12 Open (balanced)
                resonanceFreq = 90.0f;
                roomSize = 0.5f;
                brightness = 0.6f;
                lowEnd = 0.8f;
                break;
            case 2:  // 4x12 British (mid-focused, classic)
                resonanceFreq = 80.0f;
                roomSize = 0.7f;
                brightness = 0.5f;
                lowEnd = 0.9f;
                break;
            case 3:  // 4x12 Modern (tight, aggressive)
            default:
                resonanceFreq = 70.0f;
                roomSize = 0.6f;
                brightness = 0.7f;
                lowEnd = 1.0f;
                break;
        }

        // Mic position modifies characteristics
        float micBrightness = 1.0f;
        float micDelay = 0;
        float micRoomMix = 0.0f;

        switch (micPos) {
            case 0:  // Center (bright, direct)
                micBrightness = 1.2f;
                micDelay = 0;
                micRoomMix = 0.1f;
                break;
            case 1:  // Edge (warmer, less harsh)
                micBrightness = 0.8f;
                micDelay = 2;
                micRoomMix = 0.15f;
                break;
            case 2:  // Back (muffled, phase issues)
                micBrightness = 0.5f;
                micDelay = 10;
                micRoomMix = 0.25f;
                break;
            case 3:  // Room (ambient, distant)
                micBrightness = 0.6f;
                micDelay = 20;
                micRoomMix = 0.5f;
                break;
        }

        brightness *= micBrightness;

        // Generate synthetic IR
        float effectiveSampleRate = sampleRate > 0 ? sampleRate : 44100.0f;

        // Initial transient (speaker cone response)
        int transientLen = (int) (0.002 * effectiveSampleRate);  // 2ms
        for (int i = 0; i < transientLen && i + (int)micDelay < IR_LENGTH; i++) {
            float t = (float) i / transientLen;
            float env = (float) Math.exp(-t * 3.0);
            float freq = resonanceFreq + 200.0f * (1.0f - t);
            currentIR[i + (int)micDelay] = env * (float) Math.sin(2.0 * Math.PI * freq * i / effectiveSampleRate);
        }

        // Cabinet resonance
        int resonanceLen = (int) (0.01 * effectiveSampleRate);  // 10ms
        for (int i = 0; i < resonanceLen && i < IR_LENGTH; i++) {
            float t = (float) i / resonanceLen;
            float env = lowEnd * (float) Math.exp(-t * 5.0);
            currentIR[i] += env * 0.5f * (float) Math.sin(2.0 * Math.PI * resonanceFreq * i / effectiveSampleRate);
        }

        // Room reflections / reverberant tail
        int roomLen = (int) (roomSize * 0.05 * effectiveSampleRate);
        for (int i = transientLen; i < roomLen && i < IR_LENGTH; i++) {
            float t = (float) (i - transientLen) / (roomLen - transientLen);
            float env = micRoomMix * (float) Math.exp(-t * 4.0);

            // Random-ish diffuse reflections
            float reflection = 0;
            for (int h = 1; h <= 4; h++) {
                float freq = resonanceFreq * h * 0.7f;
                reflection += (float) Math.sin(2.0 * Math.PI * freq * i / effectiveSampleRate + h * 1.5f) / h;
            }
            currentIR[i] += env * reflection * 0.2f;
        }

        // High frequency rolloff (speaker doesn't reproduce highs well)
        float hfRolloff = brightness;
        float lpState = 0;
        float lpCoeff = (float) (2.0 * Math.PI * (3000.0 + 5000.0 * hfRolloff) / effectiveSampleRate);
        lpCoeff = Math.min(lpCoeff, 1.0f);

        for (int i = 0; i < IR_LENGTH; i++) {
            lpState += lpCoeff * (currentIR[i] - lpState);
            currentIR[i] = currentIR[i] * hfRolloff + lpState * (1.0f - hfRolloff);
        }

        // Normalize IR by energy (sum of squares)
        float energy = 0;
        for (int i = 0; i < IR_LENGTH; i++) {
            energy += currentIR[i] * currentIR[i];
        }
        if (energy > 0.0001f) {
            // Normalize to unity energy, then scale down
            float scale = 0.5f / (float) Math.sqrt(energy);
            for (int i = 0; i < IR_LENGTH; i++) {
                currentIR[i] *= scale;
            }
        }
    }

    @Override
    public int getLatency() {
        return convolverL.getLatency();
    }
}
