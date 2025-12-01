package it.denzosoft.jfx2.graph;

import it.denzosoft.jfx2.effects.Parameter;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Output node - sends audio to an audio output device.
 *
 * <p>Each OutputNode can be configured to send audio to a specific output device.
 * Multiple OutputNodes can exist, each sending to a different device.
 * This allows routing the same signal to multiple outputs simultaneously.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Device selection from available output devices</li>
 *   <li>Gain control (-60dB to +24dB)</li>
 *   <li>Mute function</li>
 *   <li>Automatic device conflict detection</li>
 * </ul>
 * </p>
 */
public class OutputNode extends AbstractNode {

    // Global registry of devices in use by OutputNodes
    private static final Map<String, OutputNode> devicesInUse = new ConcurrentHashMap<>();

    private final Port inputPort;

    // Parameters
    private final Parameter gainParam;
    private final Parameter muteParam;

    // Device state
    private String selectedDeviceName;  // null = default device
    private Mixer.Info selectedDevice;
    private SourceDataLine outputLine;
    private AudioFormat audioFormat;

    // Audio buffers
    private byte[] byteBuffer;
    private float[] floatBuffer;
    private int outputChannels = 2;

    // State
    private volatile boolean deviceOpen = false;
    private volatile boolean writing = false;

    /**
     * Create an output node with default device.
     *
     * @param id Unique identifier
     */
    public OutputNode(String id) {
        this(id, PortType.AUDIO_STEREO, null);
    }

    /**
     * Create an output node with specified port type and default device.
     *
     * @param id   Unique identifier
     * @param type Input signal type
     */
    public OutputNode(String id, PortType type) {
        this(id, type, null);
    }

    /**
     * Create an output node.
     *
     * @param id         Unique identifier
     * @param type       Input signal type (AUDIO_MONO or AUDIO_STEREO)
     * @param deviceName Name of the output device, or null for default
     */
    public OutputNode(String id, PortType type, String deviceName) {
        super(id, "Output", NodeType.OUTPUT);
        this.inputPort = addInputPort("in", "Input", type);
        this.selectedDeviceName = deviceName;
        this.outputChannels = type.getChannelCount();

        // Create parameters
        this.gainParam = new Parameter("gain", "Gain",
                "Output level adjustment. 0 dB = unity gain.",
                -60.0f, 24.0f, 0.0f, "dB");

        this.muteParam = new Parameter("mute", "Mute",
                "Mute this output.",
                false);
    }

    /**
     * Get available output devices that are not already in use by other OutputNodes.
     *
     * @return List of available device names (null entry for "Default")
     */
    public static List<String> getAvailableDevices() {
        List<String> available = new ArrayList<>();
        available.add(null);  // Default device

        // Get all output devices
        AudioFormat testFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(sourceInfo)) {
                    String name = mixerInfo.getName();
                    // Only add if not already in use
                    if (!devicesInUse.containsKey(name)) {
                        available.add(name);
                    }
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }

        return available;
    }

    /**
     * Get all output devices (including those in use).
     *
     * @return List of all device names
     */
    public static List<String> getAllDevices() {
        List<String> all = new ArrayList<>();
        all.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(sourceInfo)) {
                    all.add(mixerInfo.getName());
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }

        return all;
    }

    /**
     * Check if a device is currently in use by another OutputNode.
     *
     * @param deviceName Device name to check
     * @return true if in use by another node
     */
    public static boolean isDeviceInUse(String deviceName) {
        return deviceName != null && devicesInUse.containsKey(deviceName);
    }

    /**
     * Get the OutputNode that is using a specific device.
     *
     * @param deviceName Device name
     * @return The OutputNode using this device, or null
     */
    public static OutputNode getNodeUsingDevice(String deviceName) {
        return devicesInUse.get(deviceName);
    }

    /**
     * Set the output device.
     *
     * @param deviceName Device name, or null for default
     * @return true if device was set successfully
     */
    public boolean setDevice(String deviceName) {
        // Check if device is available
        if (deviceName != null && devicesInUse.containsKey(deviceName)) {
            OutputNode other = devicesInUse.get(deviceName);
            if (other != this) {
                System.err.println("Device '" + deviceName + "' is already in use by " + other.getId());
                return false;
            }
        }

        // Release current device
        if (selectedDeviceName != null) {
            devicesInUse.remove(selectedDeviceName);
        }
        closeDevice();

        // Set new device
        this.selectedDeviceName = deviceName;
        if (deviceName != null) {
            devicesInUse.put(deviceName, this);
        }

        // Find the Mixer.Info for this device name
        this.selectedDevice = findDevice(deviceName);

        return true;
    }

    /**
     * Get the current device name.
     *
     * @return Device name, or null for default
     */
    public String getDeviceName() {
        return selectedDeviceName;
    }

    /**
     * Get the gain parameter.
     */
    public Parameter getGainParam() {
        return gainParam;
    }

    /**
     * Get the mute parameter.
     */
    public Parameter getMuteParam() {
        return muteParam;
    }

    /**
     * Set gain in dB.
     */
    public void setGain(float dB) {
        gainParam.setValue(dB);
    }

    /**
     * Get gain in dB.
     */
    public float getGain() {
        return gainParam.getValue();
    }

    /**
     * Set mute state.
     */
    public void setMuted(boolean muted) {
        muteParam.setValue(muted ? 1.0f : 0.0f);
    }

    /**
     * Check if muted.
     */
    public boolean isMuted() {
        return muteParam.getValue() > 0.5f;
    }

    private Mixer.Info findDevice(String deviceName) {
        if (deviceName == null) {
            return null;  // Use default
        }

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(deviceName)) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void prepare(int sampleRate, int maxFrameCount) {
        super.prepare(sampleRate, maxFrameCount);

        // Prepare parameters
        gainParam.prepare(sampleRate);
        muteParam.prepare(sampleRate);

        // Determine channels from port type
        outputChannels = inputPort.getType().getChannelCount();

        // Create audio format
        audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,  // 16-bit
                outputChannels,
                outputChannels * 2,  // frame size in bytes
                sampleRate,
                false  // little-endian
        );

        // Allocate buffers
        int bufferBytes = maxFrameCount * outputChannels * 2;  // 16-bit = 2 bytes
        byteBuffer = new byte[bufferBytes];
        floatBuffer = new float[maxFrameCount * outputChannels];

        // Open device
        openDevice();
    }

    private void openDevice() {
        if (deviceOpen) {
            return;
        }

        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            if (selectedDevice != null) {
                Mixer mixer = AudioSystem.getMixer(selectedDevice);
                outputLine = (SourceDataLine) mixer.getLine(lineInfo);
            } else {
                outputLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
            }

            outputLine.open(audioFormat, byteBuffer.length * 2);
            outputLine.start();
            deviceOpen = true;

            System.out.println("OutputNode " + id + " opened device: " +
                    (selectedDeviceName != null ? selectedDeviceName : "Default"));

        } catch (LineUnavailableException e) {
            System.err.println("OutputNode " + id + " failed to open device: " + e.getMessage());
            outputLine = null;
            deviceOpen = false;
        }
    }

    private void closeDevice() {
        if (outputLine != null) {
            outputLine.stop();
            outputLine.flush();
            outputLine.close();
            outputLine = null;
        }
        deviceOpen = false;
    }

    @Override
    public void process(int frameCount) {
        // Output node doesn't process - data is read externally via getOutputData
        // or written to device via writeToDevice
    }

    /**
     * Write audio data to the device.
     * Called by the audio engine after graph processing.
     *
     * @param frameCount Number of frames to write
     * @return Number of frames actually written
     */
    public int writeToDevice(int frameCount) {
        if (!deviceOpen || outputLine == null) {
            return 0;
        }

        float[] buffer = inputPort.getBuffer();
        if (buffer == null) {
            return 0;
        }

        writing = true;
        try {
            int sampleCount = frameCount * outputChannels;
            sampleCount = Math.min(sampleCount, Math.min(buffer.length, floatBuffer.length));

            // Copy input to work buffer
            System.arraycopy(buffer, 0, floatBuffer, 0, sampleCount);

            // Apply gain and mute
            applyGain(floatBuffer, sampleCount);

            // Convert floats to bytes
            floatsToBytes(floatBuffer, byteBuffer, sampleCount);

            // Write to device
            int bytesToWrite = sampleCount * 2;  // 16-bit = 2 bytes per sample
            int bytesWritten = outputLine.write(byteBuffer, 0, Math.min(bytesToWrite, byteBuffer.length));

            return bytesWritten / (outputChannels * 2);
        } finally {
            writing = false;
        }
    }

    /**
     * Get the output data after graph processing.
     * Called by the audio engine after graph processing.
     *
     * @param frameCount Number of frames
     * @return Audio data buffer
     */
    public float[] getOutputData(int frameCount) {
        return inputPort.getBuffer();
    }

    /**
     * Copy output data to the provided buffer.
     *
     * @param destination Destination buffer
     * @param frameCount  Number of frames
     */
    public void copyOutputData(float[] destination, int frameCount) {
        float[] buffer = inputPort.getBuffer();
        if (buffer != null && destination != null) {
            int sampleCount = frameCount * inputPort.getType().getChannelCount();
            System.arraycopy(buffer, 0, destination, 0, Math.min(sampleCount, Math.min(buffer.length, destination.length)));
        }
    }

    private void applyGain(float[] buffer, int sampleCount) {
        // Smooth parameter changes
        gainParam.smooth();
        muteParam.smooth();

        boolean muted = muteParam.getValue() > 0.5f;
        float gainDb = gainParam.getValue();
        float gainLinear = muted ? 0.0f : (float) Math.pow(10.0, gainDb / 20.0);

        for (int i = 0; i < sampleCount && i < buffer.length; i++) {
            buffer[i] *= gainLinear;
        }
    }

    private void floatsToBytes(float[] floats, byte[] bytes, int sampleCount) {
        for (int i = 0; i < sampleCount && i < floats.length; i++) {
            // Clamp to valid range
            float clamped = Math.max(-1.0f, Math.min(1.0f, floats[i]));
            int sample = (int) (clamped * 32767.0f);
            // Little-endian 16-bit signed
            bytes[2 * i] = (byte) (sample & 0xFF);
            bytes[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
    }

    @Override
    public void release() {
        closeDevice();

        // Remove from registry
        if (selectedDeviceName != null) {
            devicesInUse.remove(selectedDeviceName);
        }

        super.release();
    }

    @Override
    public void reset() {
        super.reset();
        gainParam.reset();
        muteParam.reset();
    }

    /**
     * Get the input port.
     */
    public Port getInput() {
        return inputPort;
    }

    /**
     * Check if the device is open and ready.
     */
    public boolean isDeviceOpen() {
        return deviceOpen;
    }

    /**
     * Get device info string for display.
     */
    public String getDeviceInfo() {
        if (selectedDeviceName == null) {
            return "Default Output Device";
        }
        return selectedDeviceName + (deviceOpen ? " (Active)" : " (Inactive)");
    }
}
