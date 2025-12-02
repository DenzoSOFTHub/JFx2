package it.denzosoft.jfx2.graph;

import it.denzosoft.jfx2.audio.AudioEngine;
import it.denzosoft.jfx2.effects.Parameter;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Input node - receives audio from an audio input device.
 *
 * <p>Each InputNode can be configured to read from a specific audio input device.
 * Multiple InputNodes can exist, but each must use a different device.
 * Each node has its own gain control.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Device selection from available input devices</li>
 *   <li>Gain control (-60dB to +24dB)</li>
 *   <li>Mute function</li>
 *   <li>Automatic device conflict detection</li>
 * </ul>
 * </p>
 */
public class InputNode extends AbstractNode {

    // Global registry of devices in use by InputNodes
    private static final Map<String, InputNode> devicesInUse = new ConcurrentHashMap<>();

    private final Port outputPort;

    // Parameters
    private final Parameter gainParam;
    private final Parameter muteParam;

    // Device state
    private String selectedDeviceName;  // null = default device
    private Mixer.Info selectedDevice;
    private TargetDataLine inputLine;
    private AudioFormat audioFormat;

    // Audio buffers
    private byte[] byteBuffer;
    private float[] floatBuffer;
    private int inputChannels = 1;

    // State
    private volatile boolean deviceOpen = false;
    private volatile boolean reading = false;

    /**
     * Create an input node with default device.
     *
     * @param id Unique identifier
     */
    public InputNode(String id) {
        this(id, PortType.AUDIO_MONO, null);
    }

    /**
     * Create an input node with specified port type and default device.
     *
     * @param id   Unique identifier
     * @param type Output signal type
     */
    public InputNode(String id, PortType type) {
        this(id, type, null);
    }

    /**
     * Create an input node.
     *
     * @param id         Unique identifier
     * @param type       Output signal type (AUDIO_MONO or AUDIO_STEREO)
     * @param deviceName Name of the input device, or null for default
     */
    public InputNode(String id, PortType type, String deviceName) {
        super(id, "Input", NodeType.INPUT);
        this.outputPort = addOutputPort("out", "Output", type);
        this.selectedDeviceName = deviceName;

        // Create parameters
        this.gainParam = new Parameter("gain", "Gain",
                "Input level adjustment. 0 dB = unity gain.",
                -60.0f, 24.0f, 0.0f, "dB");

        this.muteParam = new Parameter("mute", "Mute",
                "Mute this input.",
                false);
    }

    /**
     * Get available input devices that are not already in use by other InputNodes.
     *
     * @return List of available device names (null entry for "Default")
     */
    public static List<String> getAvailableDevices() {
        List<String> available = new ArrayList<>();
        available.add(null);  // Default device

        // Get all input devices
        AudioFormat testFormat = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetInfo)) {
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
     * Get all input devices (including those in use).
     *
     * @return List of all device names
     */
    public static List<String> getAllDevices() {
        List<String> all = new ArrayList<>();
        all.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetInfo)) {
                    all.add(mixerInfo.getName());
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }

        return all;
    }

    /**
     * Check if a device is currently in use by another InputNode.
     *
     * @param deviceName Device name to check
     * @return true if in use by another node
     */
    public static boolean isDeviceInUse(String deviceName) {
        return deviceName != null && devicesInUse.containsKey(deviceName);
    }

    /**
     * Reset all audio input devices used by InputNodes.
     * Closes all open devices and clears the registry, allowing them to be reopened.
     */
    public static void resetAllDevices() {
        System.out.println("[InputNode] Resetting all input devices...");

        // Close all devices
        for (InputNode node : new ArrayList<>(devicesInUse.values())) {
            node.closeDevice();
        }

        // Clear the registry
        devicesInUse.clear();

        // Force garbage collection to help release native resources
        System.gc();

        // Small delay to allow system to release resources
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[InputNode] All input devices reset.");
    }

    /**
     * Reopen all devices after a reset.
     */
    public static void reopenAllDevices() {
        System.out.println("[InputNode] Reopening all input devices...");

        for (InputNode node : new ArrayList<>(devicesInUse.values())) {
            node.openDevice();
        }
    }

    /**
     * Get the InputNode that is using a specific device.
     *
     * @param deviceName Device name
     * @return The InputNode using this device, or null
     */
    public static InputNode getNodeUsingDevice(String deviceName) {
        return devicesInUse.get(deviceName);
    }

    /**
     * Set the input device.
     *
     * @param deviceName Device name, or null for default
     * @return true if device was set successfully
     */
    public boolean setDevice(String deviceName) {
        // Check if device is available
        if (deviceName != null && devicesInUse.containsKey(deviceName)) {
            InputNode other = devicesInUse.get(deviceName);
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
        inputChannels = outputPort.getType().getChannelCount();

        // Create audio format
        audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,  // 16-bit
                inputChannels,
                inputChannels * 2,  // frame size in bytes
                sampleRate,
                false  // little-endian
        );

        // Allocate buffers
        int bufferBytes = maxFrameCount * inputChannels * 2;  // 16-bit = 2 bytes
        byteBuffer = new byte[bufferBytes];
        floatBuffer = new float[maxFrameCount * inputChannels];

        // Open device
        openDevice();
    }

    private void openDevice() {
        if (deviceOpen) {
            return;
        }

        try {
            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (selectedDevice != null) {
                Mixer mixer = AudioSystem.getMixer(selectedDevice);
                inputLine = (TargetDataLine) mixer.getLine(lineInfo);
            } else {
                inputLine = (TargetDataLine) AudioSystem.getLine(lineInfo);
            }

            inputLine.open(audioFormat, byteBuffer.length * 2);
            inputLine.start();
            deviceOpen = true;

            System.out.println("InputNode " + id + " opened device: " +
                    (selectedDeviceName != null ? selectedDeviceName : "Default"));

        } catch (LineUnavailableException e) {
            System.err.println("InputNode " + id + " failed to open device: " + e.getMessage());
            inputLine = null;
            deviceOpen = false;
        }
    }

    private void closeDevice() {
        if (inputLine != null) {
            inputLine.stop();
            inputLine.close();
            inputLine = null;
        }
        deviceOpen = false;
    }

    /**
     * Read audio data from the device.
     * Called by the audio engine before graph processing.
     *
     * @param frameCount Number of frames to read
     * @return Number of frames actually read
     */
    public int readFromDevice(int frameCount) {
        if (!deviceOpen || inputLine == null) {
            // Output silence if no device
            float[] buffer = outputPort.getBuffer();
            if (buffer != null) {
                Arrays.fill(buffer, 0, Math.min(frameCount * inputChannels, buffer.length), 0.0f);
            }
            return frameCount;
        }

        reading = true;
        try {
            int bytesToRead = frameCount * inputChannels * 2;
            int bytesRead = inputLine.read(byteBuffer, 0, Math.min(bytesToRead, byteBuffer.length));

            if (bytesRead > 0) {
                // Convert bytes to floats
                int sampleCount = bytesRead / 2;
                bytesToFloats(byteBuffer, floatBuffer, bytesRead);

                // Apply gain and mute
                applyGain(floatBuffer, sampleCount);

                // Copy to output buffer
                float[] outBuffer = outputPort.getBuffer();
                if (outBuffer != null) {
                    System.arraycopy(floatBuffer, 0, outBuffer, 0,
                            Math.min(sampleCount, outBuffer.length));
                }

                return bytesRead / (inputChannels * 2);
            }

            return 0;
        } finally {
            reading = false;
        }
    }

    /**
     * Set the input buffer data directly (for external use).
     * Called by the audio engine before graph processing.
     *
     * @param data       Audio data
     * @param frameCount Number of frames
     */
    public void setInputData(float[] data, int frameCount) {
        float[] buffer = outputPort.getBuffer();
        if (buffer != null && data != null) {
            int sampleCount = frameCount * outputPort.getType().getChannelCount();
            System.arraycopy(data, 0, buffer, 0, Math.min(sampleCount, Math.min(data.length, buffer.length)));

            // Apply gain and mute
            applyGain(buffer, sampleCount);
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

    private void bytesToFloats(byte[] bytes, float[] floats, int byteCount) {
        int sampleCount = byteCount / 2;  // 16-bit = 2 bytes per sample
        for (int i = 0; i < sampleCount && i < floats.length; i++) {
            // Little-endian 16-bit signed
            int lo = bytes[2 * i] & 0xFF;
            int hi = bytes[2 * i + 1];
            int sample = (hi << 8) | lo;
            floats[i] = sample / 32768.0f;
        }
    }

    @Override
    public void process(int frameCount) {
        // Input node can either:
        // 1. Read from device via readFromDevice() called externally
        // 2. Have data set via setInputData() called externally
        // The output buffer already contains the input data
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
     * Get the output port.
     */
    public Port getOutput() {
        return outputPort;
    }

    /**
     * Check if the device is open and reading.
     */
    public boolean isDeviceOpen() {
        return deviceOpen;
    }

    /**
     * Get device info string for display.
     */
    public String getDeviceInfo() {
        if (selectedDeviceName == null) {
            return "Default Input Device";
        }
        return selectedDeviceName + (deviceOpen ? " (Active)" : " (Inactive)");
    }
}
