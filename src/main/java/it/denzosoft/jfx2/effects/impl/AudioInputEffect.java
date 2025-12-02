package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Audio Input effect - reads audio from an audio input device.
 *
 * <p>This is a generator effect that captures audio from a hardware input device
 * (microphone, audio interface, etc.) and outputs it to the signal chain.</p>
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
public class AudioInputEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "audioinput",
            "Audio Input",
            "Capture audio from a hardware input device (mic, audio interface)",
            EffectCategory.INPUT_SOURCE
    );

    // Global registry of devices in use
    private static final Map<String, AudioInputEffect> devicesInUse = new ConcurrentHashMap<>();

    // Parameters
    private Parameter deviceParam;
    private Parameter gainParam;
    private Parameter muteParam;

    // Device state
    private String selectedDeviceName;
    private Mixer.Info selectedDevice;
    private TargetDataLine inputLine;
    private AudioFormat audioFormat;

    // Audio buffers
    private byte[] byteBuffer;
    private float[] floatBuffer;

    // State
    private volatile boolean deviceOpen = false;
    private List<String> deviceNames;
    private String lastError = null;

    public AudioInputEffect() {
        super(METADATA);
        initParameters();
    }

    private void initParameters() {
        // Build device list
        deviceNames = getAllDeviceNames();
        String[] deviceChoices = deviceNames.stream()
                .map(name -> name == null ? "Default Input Device" : name)
                .toArray(String[]::new);

        deviceParam = addChoiceParameter("device", "Device",
                "Select the audio input device to capture from.",
                deviceChoices, 0);

        gainParam = addFloatParameter("gain", "Gain",
                "Input level adjustment. 0 dB = unity gain.",
                -60.0f, 24.0f, 0.0f, "dB");

        muteParam = addBooleanParameter("mute", "Mute",
                "Mute this input.",
                false);
    }

    /**
     * Get all input device names.
     */
    private static List<String> getAllDeviceNames() {
        List<String> names = new ArrayList<>();
        names.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetInfo)) {
                    names.add(mixerInfo.getName());
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }

        return names;
    }

    /**
     * Get available input devices (not in use by other instances).
     */
    public static List<String> getAvailableDevices() {
        List<String> available = new ArrayList<>();
        available.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetInfo)) {
                    String name = mixerInfo.getName();
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
     * Check if a device is in use.
     */
    public static boolean isDeviceInUse(String deviceName) {
        return deviceName != null && devicesInUse.containsKey(deviceName);
    }

    /**
     * Reset all audio input devices.
     * Closes all open devices and clears the registry, allowing them to be reopened.
     * This is useful when devices become unresponsive or when the system audio
     * configuration changes.
     */
    public static void resetAllDevices() {
        System.out.println("[AudioInputEffect] Resetting all input devices...");

        // Close all devices
        for (AudioInputEffect effect : new ArrayList<>(devicesInUse.values())) {
            effect.closeDevice();
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

        System.out.println("[AudioInputEffect] All input devices reset.");
    }

    /**
     * Reopen all devices after a reset.
     * This should be called after resetAllDevices() to re-establish connections.
     */
    public static void reopenAllDevices() {
        System.out.println("[AudioInputEffect] Reopening all input devices...");

        for (AudioInputEffect effect : new ArrayList<>(devicesInUse.values())) {
            effect.updateDevice();
        }
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create audio format
        audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,  // 16-bit
                1,   // mono
                2,   // frame size in bytes
                sampleRate,
                false  // little-endian
        );

        // Allocate buffers
        int bufferBytes = maxFrameCount * 2;  // 16-bit = 2 bytes per sample
        byteBuffer = new byte[bufferBytes];
        floatBuffer = new float[maxFrameCount];

        // Update device from parameter
        updateDevice();
    }

    private void updateDevice() {
        int deviceIndex = deviceParam.getChoiceIndex();
        String newDeviceName = (deviceIndex >= 0 && deviceIndex < deviceNames.size())
                ? deviceNames.get(deviceIndex) : null;

        // Check if device changed
        if (Objects.equals(newDeviceName, selectedDeviceName) && deviceOpen) {
            return;
        }

        // Check if new device is available
        if (newDeviceName != null && devicesInUse.containsKey(newDeviceName)) {
            AudioInputEffect other = devicesInUse.get(newDeviceName);
            if (other != this) {
                System.err.println("Audio input device '" + newDeviceName + "' is already in use");
                return;
            }
        }

        // Release current device
        closeDevice();
        if (selectedDeviceName != null) {
            devicesInUse.remove(selectedDeviceName);
        }

        // Set new device
        selectedDeviceName = newDeviceName;
        if (selectedDeviceName != null) {
            devicesInUse.put(selectedDeviceName, this);
        }
        selectedDevice = findDevice(selectedDeviceName);

        // Open new device
        openDevice();
    }

    private Mixer.Info findDevice(String deviceName) {
        if (deviceName == null) {
            return null;
        }

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(deviceName)) {
                return info;
            }
        }
        return null;
    }

    private void openDevice() {
        openDeviceWithRetry(true);
    }

    private void openDeviceWithRetry(boolean allowRetry) {
        if (deviceOpen || audioFormat == null) {
            return;
        }

        lastError = null;

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

            System.out.println("AudioInputEffect opened device: " +
                    (selectedDeviceName != null ? selectedDeviceName : "Default"));

        } catch (Exception e) {
            lastError = e.getMessage();
            if (inputLine != null) {
                try { inputLine.close(); } catch (Exception ignored) {}
                inputLine = null;
            }

            // Try reset and retry once
            if (allowRetry) {
                System.out.println("AudioInputEffect: Resetting device and retrying...");
                closeDevice();
                System.gc();
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                openDeviceWithRetry(false);
            } else {
                System.err.println("AudioInputEffect failed to open device: " + lastError);
                deviceOpen = false;
            }
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

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Check if device parameter changed
        int deviceIndex = deviceParam.getChoiceIndex();
        String paramDeviceName = (deviceIndex >= 0 && deviceIndex < deviceNames.size())
                ? deviceNames.get(deviceIndex) : null;
        if (!Objects.equals(paramDeviceName, selectedDeviceName)) {
            updateDevice();
        }

        // Read from device or output silence
        if (!deviceOpen || inputLine == null) {
            Arrays.fill(output, 0, Math.min(frameCount, output.length), 0.0f);
            return;
        }

        // Read from input line
        int bytesToRead = frameCount * 2;
        int bytesRead = inputLine.read(byteBuffer, 0, Math.min(bytesToRead, byteBuffer.length));

        if (bytesRead > 0) {
            // Convert bytes to floats
            int sampleCount = bytesRead / 2;
            bytesToFloats(byteBuffer, floatBuffer, bytesRead);

            // Apply gain and mute
            applyGain(floatBuffer, sampleCount);

            // Copy to output
            System.arraycopy(floatBuffer, 0, output, 0, Math.min(sampleCount, output.length));
        } else {
            Arrays.fill(output, 0, Math.min(frameCount, output.length), 0.0f);
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Process mono, then copy to both channels
        onProcess(inputL, outputL, frameCount);

        // Copy L to R for stereo output
        int len = Math.min(frameCount, Math.min(outputL.length, outputR.length));
        System.arraycopy(outputL, 0, outputR, 0, len);
    }

    private void applyGain(float[] buffer, int sampleCount) {
        boolean muted = muteParam.getValue() > 0.5f;
        float gainDb = gainParam.getValue();
        float gainLinear = muted ? 0.0f : (float) Math.pow(10.0, gainDb / 20.0);

        for (int i = 0; i < sampleCount && i < buffer.length; i++) {
            buffer[i] *= gainLinear;
        }
    }

    private void bytesToFloats(byte[] bytes, float[] floats, int byteCount) {
        int sampleCount = byteCount / 2;
        for (int i = 0; i < sampleCount && i < floats.length; i++) {
            int lo = bytes[2 * i] & 0xFF;
            int hi = bytes[2 * i + 1];
            int sample = (hi << 8) | lo;
            floats[i] = sample / 32768.0f;
        }
    }

    @Override
    protected void onReset() {
        // Nothing to reset
    }

    @Override
    public void release() {
        closeDevice();
        if (selectedDeviceName != null) {
            devicesInUse.remove(selectedDeviceName);
        }
        super.release();
    }

    @Override
    public int getLatency() {
        return 0;
    }

    /**
     * Get the current device name.
     */
    public String getDeviceName() {
        return selectedDeviceName;
    }

    /**
     * Check if device is open.
     */
    public boolean isDeviceOpen() {
        return deviceOpen;
    }

    /**
     * Get device info for display.
     */
    public String getDeviceInfo() {
        if (selectedDeviceName == null) {
            return "Default Input Device";
        }
        return selectedDeviceName + (deviceOpen ? " (Active)" : " (Inactive)");
    }

    // Convenience setters
    public void setGain(float dB) {
        gainParam.setValue(dB);
    }

    public float getGain() {
        return gainParam.getValue();
    }

    public void setMuted(boolean muted) {
        muteParam.setValue(muted ? 1.0f : 0.0f);
    }

    public boolean isMuted() {
        return muteParam.getValue() > 0.5f;
    }

    /**
     * Get the last error message if device failed to open.
     * @return Error message or null if no error
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Check if device has an error (failed to open).
     */
    public boolean hasError() {
        return lastError != null && !deviceOpen;
    }
}
