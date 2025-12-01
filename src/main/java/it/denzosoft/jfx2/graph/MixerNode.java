package it.denzosoft.jfx2.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixer node - mixes multiple inputs to a single output.
 *
 * <p>Combines 2-8 inputs with individual level and pan controls.
 * Supports mono and stereo modes.</p>
 */
public class MixerNode extends AbstractNode {

    /**
     * Stereo mode for the mixer.
     */
    public enum StereoMode {
        MONO,    // Mono output (pan affects level)
        STEREO   // Stereo processing with L/R panning, output is L+R sum
    }

    private final List<Port> inputPorts;
    private final Port outputPort;
    private final int numInputs;

    // Per-channel controls
    private final float[] levels;
    private final float[] pans;  // -1 = left, 0 = center, +1 = right
    private final boolean[] mutes;

    // Master level
    private float masterLevel = 1.0f;

    // Stereo mode
    private StereoMode stereoMode = StereoMode.STEREO;

    // Work buffers for stereo processing
    private float[] workBufferL;
    private float[] workBufferR;

    /**
     * Create a mixer node with specified number of inputs.
     *
     * @param id        Unique identifier
     * @param numInputs Number of inputs (2-8)
     */
    public MixerNode(String id, int numInputs) {
        super(id, "Mixer", NodeType.MIXER);

        // Clamp to valid range
        this.numInputs = Math.max(2, Math.min(8, numInputs));

        // Create input ports (all mono - they receive mono signals)
        this.inputPorts = new ArrayList<>();
        for (int i = 0; i < this.numInputs; i++) {
            Port in = addInputPort("in" + (i + 1), "Input " + (i + 1), PortType.AUDIO_MONO);
            inputPorts.add(in);
        }

        // Create stereo output port (interleaved L/R samples)
        this.outputPort = addOutputPort("out", "Output", PortType.AUDIO_STEREO);

        // Initialize controls
        this.levels = new float[this.numInputs];
        this.pans = new float[this.numInputs];
        this.mutes = new boolean[this.numInputs];

        for (int i = 0; i < this.numInputs; i++) {
            levels[i] = 1.0f;  // Unity gain
            pans[i] = 0.0f;    // Center
            mutes[i] = false;
        }
    }

    /**
     * Create a mixer with 2 inputs (default).
     */
    public MixerNode(String id) {
        this(id, 2);
    }

    @Override
    public void prepare(int sampleRate, int maxFrameCount) {
        super.prepare(sampleRate, maxFrameCount);
        workBufferL = new float[maxFrameCount];
        workBufferR = new float[maxFrameCount];
    }

    @Override
    public void process(int frameCount) {
        if (stereoMode == StereoMode.STEREO) {
            processStereo(frameCount);
        } else {
            processMono(frameCount);
        }
    }

    /**
     * Process in mono mode - pan affects level, output duplicated to L and R.
     * Output is interleaved stereo (L, R, L, R, ...).
     */
    private void processMono(int frameCount) {
        float[] output = outputPort.getBuffer();

        if (output == null) {
            return;
        }

        // Use work buffer for mono accumulation
        int len = Math.min(frameCount, workBufferL.length);

        // Clear work buffer
        for (int i = 0; i < len; i++) {
            workBufferL[i] = 0.0f;
        }

        // Sum all inputs with level and pan
        for (int ch = 0; ch < numInputs; ch++) {
            if (mutes[ch]) {
                continue;
            }

            float[] input = inputPorts.get(ch).getBuffer();
            if (input == null) {
                continue;
            }

            float level = levels[ch];
            // For mono output, pan affects level (center = full, sides = reduced)
            // Using constant power pan law approximation
            float panLevel = (float) Math.cos(pans[ch] * Math.PI / 4.0);

            float gain = level * panLevel;

            int inputLen = Math.min(len, input.length);
            for (int i = 0; i < inputLen; i++) {
                workBufferL[i] += input[i] * gain;
            }
        }

        // Apply master level and output as interleaved stereo (L=R)
        int outLen = Math.min(len * 2, output.length);
        for (int i = 0; i < len && i * 2 + 1 < outLen; i++) {
            float sample = workBufferL[i] * masterLevel;
            output[i * 2] = sample;      // Left
            output[i * 2 + 1] = sample;  // Right (same as left)
        }
    }

    /**
     * Process in stereo mode - true L/R panning with constant power pan law.
     * Output is interleaved stereo (L, R, L, R, ...).
     */
    private void processStereo(int frameCount) {
        float[] output = outputPort.getBuffer();

        if (output == null) {
            return;
        }

        // Use work buffers for accumulation
        int len = Math.min(frameCount, Math.min(workBufferL.length, workBufferR.length));

        // Clear work buffers
        for (int i = 0; i < len; i++) {
            workBufferL[i] = 0.0f;
            workBufferR[i] = 0.0f;
        }

        // Sum all inputs with level and pan
        for (int ch = 0; ch < numInputs; ch++) {
            if (mutes[ch]) {
                continue;
            }

            float[] input = inputPorts.get(ch).getBuffer();
            if (input == null) {
                continue;
            }

            float level = levels[ch];
            float pan = pans[ch];

            // Constant power pan law
            // pan = -1: L=1, R=0
            // pan =  0: L=0.707, R=0.707
            // pan = +1: L=0, R=1
            float angle = (pan + 1.0f) * (float) Math.PI / 4.0f;  // 0 to PI/2
            float gainL = level * (float) Math.cos(angle);
            float gainR = level * (float) Math.sin(angle);

            int inputLen = Math.min(len, input.length);
            for (int i = 0; i < inputLen; i++) {
                workBufferL[i] += input[i] * gainL;
                workBufferR[i] += input[i] * gainR;
            }
        }

        // Apply master level and output as interleaved stereo
        int outLen = Math.min(len * 2, output.length);
        for (int i = 0; i < len && i * 2 + 1 < outLen; i++) {
            output[i * 2] = workBufferL[i] * masterLevel;      // Left
            output[i * 2 + 1] = workBufferR[i] * masterLevel;  // Right
        }
    }

    /**
     * Get input port by index (0-based).
     */
    public Port getInput(int index) {
        if (index < 0 || index >= inputPorts.size()) {
            throw new IndexOutOfBoundsException("Input index " + index + " out of range (0-" + (inputPorts.size() - 1) + ")");
        }
        return inputPorts.get(index);
    }

    /**
     * Get all input ports.
     */
    public List<Port> getInputs() {
        return new ArrayList<>(inputPorts);
    }

    /**
     * Get the output port.
     */
    public Port getOutput() {
        return outputPort;
    }

    /**
     * Get number of inputs.
     */
    public int getNumInputs() {
        return numInputs;
    }

    // Stereo mode

    /**
     * Set stereo mode.
     */
    public void setStereoMode(StereoMode mode) {
        this.stereoMode = mode;
    }

    /**
     * Get stereo mode.
     */
    public StereoMode getStereoMode() {
        return stereoMode;
    }

    // Channel controls

    /**
     * Set level for a channel.
     *
     * @param channel Channel index (0-based)
     * @param level   Level (0.0 to 2.0, 1.0 = unity)
     */
    public void setLevel(int channel, float level) {
        if (channel >= 0 && channel < numInputs) {
            levels[channel] = Math.max(0.0f, Math.min(2.0f, level));
        }
    }

    /**
     * Get level for a channel.
     */
    public float getLevel(int channel) {
        if (channel >= 0 && channel < numInputs) {
            return levels[channel];
        }
        return 0.0f;
    }

    /**
     * Set level in dB for a channel.
     */
    public void setLevelDb(int channel, float dB) {
        setLevel(channel, (float) Math.pow(10.0, dB / 20.0));
    }

    /**
     * Set pan for a channel.
     *
     * @param channel Channel index (0-based)
     * @param pan     Pan (-1.0 = left, 0.0 = center, +1.0 = right)
     */
    public void setPan(int channel, float pan) {
        if (channel >= 0 && channel < numInputs) {
            pans[channel] = Math.max(-1.0f, Math.min(1.0f, pan));
        }
    }

    /**
     * Get pan for a channel.
     */
    public float getPan(int channel) {
        if (channel >= 0 && channel < numInputs) {
            return pans[channel];
        }
        return 0.0f;
    }

    /**
     * Set mute for a channel.
     */
    public void setMute(int channel, boolean mute) {
        if (channel >= 0 && channel < numInputs) {
            mutes[channel] = mute;
        }
    }

    /**
     * Get mute for a channel.
     */
    public boolean isMuted(int channel) {
        if (channel >= 0 && channel < numInputs) {
            return mutes[channel];
        }
        return false;
    }

    /**
     * Set master level.
     */
    public void setMasterLevel(float level) {
        this.masterLevel = Math.max(0.0f, Math.min(2.0f, level));
    }

    /**
     * Get master level.
     */
    public float getMasterLevel() {
        return masterLevel;
    }

    /**
     * Set master level in dB.
     */
    public void setMasterLevelDb(float dB) {
        setMasterLevel((float) Math.pow(10.0, dB / 20.0));
    }
}
