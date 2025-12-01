package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * WAV file player effect - reads a WAV file and outputs audio.
 *
 * <p>This is a generator effect that ignores input and produces audio
 * from a WAV file. Supports looping and playback control.</p>
 */
public class WavFileInputEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "wavfileinput",
            "WAV File Player",
            "Plays audio from a WAV file with loop control",
            EffectCategory.INPUT_SOURCE
    );

    // Parameters
    private final Parameter volumeParam;
    private final Parameter loopCountParam;
    private final Parameter playingParam;

    // Audio data (separate L/R for stereo files)
    private float[] audioDataL;
    private float[] audioDataR;  // null for mono files
    private int audioDataLength; // samples per channel
    private int audioSampleRate;
    private int audioChannels;   // 1 = mono, 2 = stereo

    // Playback state
    private int playbackPosition;
    private int currentLoop;
    private String currentFilePath;
    private boolean isPlaying;

    public WavFileInputEffect() {
        super(METADATA);

        // Volume control
        volumeParam = addFloatParameter("volume", "Volume",
                "Playback volume level. 0 dB = original level, adjust to match other sources.",
                -60.0f, 12.0f, 0.0f, "dB");

        // Loop count: -1 = infinite, 0 = play once, 1+ = repeat N times
        loopCountParam = addFloatParameter("loopCount", "Loop Count",
                "Number of times to repeat: -1 = loop forever, 0 = play once, 1+ = repeat N times.",
                -1.0f, 100.0f, 0.0f, "");

        // Playing state (can be toggled)
        playingParam = addBooleanParameter("playing", "Playing",
                "Start or stop playback. File plays from current position when enabled.",
                false);
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Reset playback
        playbackPosition = 0;
        currentLoop = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float volumeDb = volumeParam.getValue();
        float volumeLinear = dbToLinear(volumeDb);
        int loopCount = (int) loopCountParam.getValue();
        isPlaying = playingParam.getValue() > 0.5f;

        // Clear output first
        for (int i = 0; i < frameCount && i < output.length; i++) {
            output[i] = 0.0f;
        }

        // If no audio loaded or not playing, output silence
        if (audioDataL == null || audioDataLength == 0 || !isPlaying) {
            return;
        }

        // Check if we've finished all loops
        if (loopCount >= 0 && currentLoop > loopCount) {
            return;
        }

        // Generate output from WAV data
        for (int i = 0; i < frameCount && i < output.length; i++) {
            if (playbackPosition < audioDataLength) {
                // Resample if necessary (simple nearest-neighbor for now)
                int srcPos = playbackPosition;
                if (audioSampleRate != sampleRate) {
                    srcPos = (int) ((long) playbackPosition * audioSampleRate / sampleRate);
                }

                if (srcPos < audioDataLength) {
                    // For mono output, mix L+R if stereo source, or just use L for mono
                    if (audioChannels == 2 && audioDataR != null) {
                        output[i] = (audioDataL[srcPos] + audioDataR[srcPos]) * 0.5f * volumeLinear;
                    } else {
                        output[i] = audioDataL[srcPos] * volumeLinear;
                    }
                }

                playbackPosition++;
            } else {
                // End of file - check for loop
                if (loopCount < 0 || currentLoop < loopCount) {
                    // Loop back to start
                    playbackPosition = 0;
                    currentLoop++;
                } else {
                    // Stop playing
                    isPlaying = false;
                    playingParam.setValue(0.0f);
                    break;
                }
            }
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float volumeDb = volumeParam.getValue();
        float volumeLinear = dbToLinear(volumeDb);
        int loopCount = (int) loopCountParam.getValue();
        isPlaying = playingParam.getValue() > 0.5f;

        // Clear output first
        int len = Math.min(frameCount, Math.min(outputL.length, outputR.length));
        for (int i = 0; i < len; i++) {
            outputL[i] = 0.0f;
            outputR[i] = 0.0f;
        }

        // If no audio loaded or not playing, output silence
        if (audioDataL == null || audioDataLength == 0 || !isPlaying) {
            return;
        }

        // Check if we've finished all loops
        if (loopCount >= 0 && currentLoop > loopCount) {
            return;
        }

        // Check if we have stereo audio data
        boolean isStereoSource = (audioChannels == 2 && audioDataR != null);

        // Generate output from WAV data
        for (int i = 0; i < len; i++) {
            if (playbackPosition < audioDataLength) {
                // Resample if necessary (simple nearest-neighbor for now)
                int srcPos = playbackPosition;
                if (audioSampleRate != sampleRate) {
                    srcPos = (int) ((long) playbackPosition * audioSampleRate / sampleRate);
                }

                if (srcPos < audioDataLength) {
                    if (isStereoSource) {
                        // Stereo source: output L and R separately
                        outputL[i] = audioDataL[srcPos] * volumeLinear;
                        outputR[i] = audioDataR[srcPos] * volumeLinear;
                    } else {
                        // Mono source: same signal to both channels
                        float sample = audioDataL[srcPos] * volumeLinear;
                        outputL[i] = sample;
                        outputR[i] = sample;
                    }
                }

                playbackPosition++;
            } else {
                // End of file - check for loop
                if (loopCount < 0 || currentLoop < loopCount) {
                    // Loop back to start
                    playbackPosition = 0;
                    currentLoop++;
                } else {
                    // Stop playing
                    isPlaying = false;
                    playingParam.setValue(0.0f);
                    break;
                }
            }
        }
    }

    @Override
    protected void onReset() {
        playbackPosition = 0;
        currentLoop = 0;
    }

    /**
     * Load a WAV file for playback.
     *
     * @param filePath Path to the WAV file
     * @return true if loaded successfully
     */
    public boolean loadFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            audioDataL = null;
            audioDataR = null;
            audioDataLength = 0;
            currentFilePath = null;
            return false;
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("WAV file not found: " + filePath);
                return false;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioStream.getFormat();

            // Convert to PCM if necessary
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED &&
                format.getEncoding() != AudioFormat.Encoding.PCM_FLOAT) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false
                );
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
                format = targetFormat;
            }

            audioSampleRate = (int) format.getSampleRate();
            audioChannels = format.getChannels();

            // Read all audio data
            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            // Convert to float samples
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int bytesPerFrame = bytesPerSample * audioChannels;
            int totalFrames = audioBytes.length / bytesPerFrame;

            audioDataL = new float[totalFrames];
            if (audioChannels >= 2) {
                audioDataR = new float[totalFrames];
            } else {
                audioDataR = null;
            }
            audioDataLength = totalFrames;

            for (int i = 0; i < totalFrames; i++) {
                int framePos = i * bytesPerFrame;

                // Read left channel sample
                float sampleL = readSample(audioBytes, framePos, bytesPerSample, format.getEncoding());
                audioDataL[i] = sampleL;

                // Read right channel sample if stereo
                if (audioChannels >= 2) {
                    float sampleR = readSample(audioBytes, framePos + bytesPerSample, bytesPerSample, format.getEncoding());
                    audioDataR[i] = sampleR;
                }
            }

            currentFilePath = filePath;
            playbackPosition = 0;
            currentLoop = 0;

            System.out.println("Loaded WAV: " + filePath +
                    " (" + audioSampleRate + "Hz, " + audioChannels + "ch, " +
                    String.format("%.2f", audioDataLength / (float) audioSampleRate) + "s)");

            return true;

        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error loading WAV file: " + e.getMessage());
            audioDataL = null;
            audioDataR = null;
            audioDataLength = 0;
            currentFilePath = null;
            return false;
        }
    }

    /**
     * Read a single sample from byte array.
     */
    private float readSample(byte[] bytes, int pos, int bytesPerSample, AudioFormat.Encoding encoding) {
        if (bytesPerSample == 2) {
            // 16-bit signed little-endian
            int low = bytes[pos] & 0xFF;
            int high = bytes[pos + 1];
            short s = (short) ((high << 8) | low);
            return s / 32768.0f;
        } else if (bytesPerSample == 1) {
            // 8-bit unsigned
            return ((bytes[pos] & 0xFF) - 128) / 128.0f;
        } else if (bytesPerSample == 3) {
            // 24-bit signed little-endian
            int low = bytes[pos] & 0xFF;
            int mid = bytes[pos + 1] & 0xFF;
            int high = bytes[pos + 2];
            int intSample = (high << 16) | (mid << 8) | low;
            return intSample / 8388608.0f;
        } else if (bytesPerSample == 4) {
            if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
                // 32-bit float
                int bits = (bytes[pos] & 0xFF) |
                           ((bytes[pos + 1] & 0xFF) << 8) |
                           ((bytes[pos + 2] & 0xFF) << 16) |
                           ((bytes[pos + 3] & 0xFF) << 24);
                return Float.intBitsToFloat(bits);
            } else {
                // 32-bit signed int
                int intSample = (bytes[pos] & 0xFF) |
                                ((bytes[pos + 1] & 0xFF) << 8) |
                                ((bytes[pos + 2] & 0xFF) << 16) |
                                ((bytes[pos + 3]) << 24);
                return intSample / 2147483648.0f;
            }
        }
        return 0.0f;
    }

    /**
     * Get the current file path.
     */
    public String getFilePath() {
        return currentFilePath;
    }

    /**
     * Start playback.
     */
    public void play() {
        playingParam.setValue(1.0f);
        isPlaying = true;
    }

    /**
     * Stop playback and reset position.
     */
    public void stop() {
        playingParam.setValue(0.0f);
        isPlaying = false;
        playbackPosition = 0;
        currentLoop = 0;
    }

    /**
     * Pause playback (maintains position).
     */
    public void pause() {
        playingParam.setValue(0.0f);
        isPlaying = false;
    }

    /**
     * Check if currently playing.
     */
    public boolean isPlaying() {
        return isPlaying && audioDataL != null;
    }

    /**
     * Check if the loaded file is stereo.
     */
    public boolean isStereo() {
        return audioChannels == 2 && audioDataR != null;
    }

    /**
     * Get playback progress (0.0 to 1.0).
     */
    public float getProgress() {
        if (audioDataLength == 0) return 0;
        return (float) playbackPosition / audioDataLength;
    }

    /**
     * Get duration in seconds.
     */
    public float getDuration() {
        if (audioDataLength == 0 || audioSampleRate == 0) return 0;
        return (float) audioDataLength / audioSampleRate;
    }

    /**
     * Seek to position (0.0 to 1.0).
     */
    public void seek(float position) {
        playbackPosition = (int) (position * audioDataLength);
        playbackPosition = Math.max(0, Math.min(playbackPosition, audioDataLength - 1));
    }
}
