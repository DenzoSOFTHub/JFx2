package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Audio Output effect - sends audio to an audio output device.
 *
 * <p>This is a sink effect that sends audio from the signal chain to a hardware
 * output device (speakers, headphones, audio interface, etc.).</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Device selection from available output devices</li>
 *   <li>Gain control (-60dB to +24dB)</li>
 *   <li>Mute function</li>
 *   <li>Automatic device conflict detection</li>
 *   <li>Multiple outputs can send to different devices simultaneously</li>
 * </ul>
 * </p>
 */
public class AudioOutputEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "audiooutput",
            "Audio Output",
            "Send audio to a hardware output device (speakers, headphones)",
            EffectCategory.OUTPUT_SINK
    );

    // Global registry of devices in use
    private static final Map<String, AudioOutputEffect> devicesInUse = new ConcurrentHashMap<>();

    // Parameters
    private Parameter deviceParam;
    private Parameter gainParam;
    private Parameter muteParam;

    // Device state
    private String selectedDeviceName;
    private Mixer.Info selectedDevice;
    private SourceDataLine outputLine;
    private AudioFormat audioFormat;

    // Audio buffers
    private byte[] byteBuffer;
    private float[] floatBuffer;
    private int outputChannels = 2;

    // State
    private volatile boolean deviceOpen = false;
    private List<String> deviceNames;

    // Post-gain output level for metering
    private volatile float outputLevelDb = -60f;

    public AudioOutputEffect() {
        super(METADATA);
        initParameters();
    }

    private void initParameters() {
        // Build device list
        deviceNames = getAllDeviceNames();
        String[] deviceChoices = deviceNames.stream()
                .map(name -> name == null ? "Default Output Device" : name)
                .toArray(String[]::new);

        deviceParam = addChoiceParameter("device", "Device",
                "Select the audio output device to send audio to.",
                deviceChoices, 0);

        gainParam = addFloatParameter("gain", "Gain",
                "Output level adjustment. 0 dB = unity gain.",
                -60.0f, 24.0f, 0.0f, "dB");

        muteParam = addBooleanParameter("mute", "Mute",
                "Mute this output.",
                false);
    }

    /**
     * Get all output device names.
     */
    private static List<String> getAllDeviceNames() {
        List<String> names = new ArrayList<>();
        names.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(sourceInfo)) {
                    names.add(mixerInfo.getName());
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }

        return names;
    }

    /**
     * Get available output devices (not in use by other instances).
     */
    public static List<String> getAvailableDevices() {
        List<String> available = new ArrayList<>();
        available.add(null);  // Default device

        AudioFormat testFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(sourceInfo)) {
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

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create audio format (stereo output)
        outputChannels = 2;
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
            AudioOutputEffect other = devicesInUse.get(newDeviceName);
            if (other != this) {
                System.err.println("Audio output device '" + newDeviceName + "' is already in use");
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
        if (deviceOpen || audioFormat == null) {
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

            System.out.println("AudioOutputEffect opened device: " +
                    (selectedDeviceName != null ? selectedDeviceName : "Default"));

        } catch (LineUnavailableException e) {
            System.err.println("AudioOutputEffect failed to open device: " + e.getMessage());
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
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Check if device parameter changed
        int deviceIndex = deviceParam.getChoiceIndex();
        String paramDeviceName = (deviceIndex >= 0 && deviceIndex < deviceNames.size())
                ? deviceNames.get(deviceIndex) : null;
        if (!Objects.equals(paramDeviceName, selectedDeviceName)) {
            updateDevice();
        }

        // Pass through to output buffer
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));

        // Write to device (mono to stereo)
        if (deviceOpen && outputLine != null) {
            // Prepare stereo buffer from mono input
            int sampleCount = Math.min(frameCount, input.length);
            for (int i = 0; i < sampleCount; i++) {
                floatBuffer[i * 2] = input[i];      // Left
                floatBuffer[i * 2 + 1] = input[i];  // Right
            }

            // Apply gain and mute
            applyGain(floatBuffer, sampleCount * 2);

            // Convert to bytes and write
            floatsToBytes(floatBuffer, byteBuffer, sampleCount * 2);
            int bytesToWrite = sampleCount * outputChannels * 2;
            outputLine.write(byteBuffer, 0, Math.min(bytesToWrite, byteBuffer.length));
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Check if device parameter changed
        int deviceIndex = deviceParam.getChoiceIndex();
        String paramDeviceName = (deviceIndex >= 0 && deviceIndex < deviceNames.size())
                ? deviceNames.get(deviceIndex) : null;
        if (!Objects.equals(paramDeviceName, selectedDeviceName)) {
            updateDevice();
        }

        // Pass through to output buffers
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));
        System.arraycopy(inputL, 0, outputL, 0, len);
        System.arraycopy(inputR, 0, outputR, 0, len);

        // Write to device
        if (deviceOpen && outputLine != null) {
            // Interleave stereo samples
            for (int i = 0; i < len; i++) {
                floatBuffer[i * 2] = inputL[i];
                floatBuffer[i * 2 + 1] = inputR[i];
            }

            // Apply gain and mute
            applyGain(floatBuffer, len * 2);

            // Convert to bytes and write
            floatsToBytes(floatBuffer, byteBuffer, len * 2);
            int bytesToWrite = len * outputChannels * 2;
            outputLine.write(byteBuffer, 0, Math.min(bytesToWrite, byteBuffer.length));
        }
    }

    private void applyGain(float[] buffer, int sampleCount) {
        boolean muted = muteParam.getValue() > 0.5f;
        float gainDb = gainParam.getValue();
        float gainLinear = muted ? 0.0f : (float) Math.pow(10.0, gainDb / 20.0);

        float sumSquares = 0;
        for (int i = 0; i < sampleCount && i < buffer.length; i++) {
            buffer[i] *= gainLinear;
            sumSquares += buffer[i] * buffer[i];
        }

        // Calculate RMS level in dB after gain
        if (sampleCount > 0) {
            float rms = (float) Math.sqrt(sumSquares / sampleCount);
            outputLevelDb = rms > 0 ? (float) (20 * Math.log10(rms)) : -60f;
            outputLevelDb = Math.max(-60f, Math.min(0f, outputLevelDb));
        }
    }

    private void floatsToBytes(float[] floats, byte[] bytes, int sampleCount) {
        for (int i = 0; i < sampleCount && i < floats.length; i++) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, floats[i]));
            int sample = (int) (clamped * 32767.0f);
            bytes[2 * i] = (byte) (sample & 0xFF);
            bytes[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
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
            return "Default Output Device";
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
     * Get the output level in dB after gain/mute is applied.
     * This is the actual level being sent to the audio device.
     */
    public float getOutputLevelDb() {
        return outputLevelDb;
    }
}
