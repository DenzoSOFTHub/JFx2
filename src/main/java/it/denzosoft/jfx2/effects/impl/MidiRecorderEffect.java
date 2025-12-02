package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MIDI Recorder effect that detects notes from audio and saves them as MIDI.
 *
 * <p>Features:
 * <ul>
 *   <li>Monophonic pitch detection using YIN algorithm</li>
 *   <li>Polyphonic pitch detection using FFT peak analysis</li>
 *   <li>Note onset/offset detection</li>
 *   <li>MIDI file export on stop</li>
 *   <li>Tempo sync with Settings block</li>
 * </ul>
 * </p>
 */
public class MidiRecorderEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "midirecorder",
            "MIDI Recorder",
            "Records audio as MIDI notes with pitch detection",
            EffectCategory.OUTPUT_SINK
    );

    // Detection modes
    private static final String[] DETECTION_MODES = {"Monophonic (YIN)", "Polyphonic (FFT)"};

    // Parameters
    private final Parameter modeParam;
    private final Parameter thresholdParam;
    private final Parameter minNoteParam;
    private final Parameter maxNoteParam;
    private final Parameter velocitySensParam;
    private final Parameter quantizeParam;
    private final Parameter fileParam;

    // Pitch detection state
    private static final int YIN_BUFFER_SIZE = 2048;
    private static final int FFT_SIZE = 4096;
    private float[] yinBuffer;
    private float[] fftBuffer;
    private float[] fftWindow;
    private float[] magnitudes;

    // Note tracking
    private static final int MAX_POLYPHONY = 6;
    private int[] activeNotes;
    private long[] noteOnTimes;
    private int[] noteVelocities;
    private int activeNoteCount;

    // MIDI recording
    private Sequence midiSequence;
    private Track midiTrack;
    private long tickPosition;
    private long startTimeMs;
    private boolean recording;
    private String outputFilePath;

    // Sample accumulation for pitch detection
    private float[] sampleAccumulator;
    private int accumulatorPos;

    // Smoothing for pitch detection
    private float lastDetectedPitch;
    private int pitchConfirmCount;
    private static final int PITCH_CONFIRM_FRAMES = 3;

    public MidiRecorderEffect() {
        super(METADATA);

        modeParam = addChoiceParameter("mode", "Mode",
                "Pitch detection algorithm",
                DETECTION_MODES, 0);

        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Minimum signal level to detect notes",
                -60.0f, 0.0f, -40.0f, "dB");

        minNoteParam = addFloatParameter("minNote", "Min Note",
                "Lowest MIDI note to detect (e.g., 40 = E2)",
                24.0f, 96.0f, 40.0f, "");

        maxNoteParam = addFloatParameter("maxNote", "Max Note",
                "Highest MIDI note to detect (e.g., 84 = C6)",
                48.0f, 120.0f, 84.0f, "");

        velocitySensParam = addFloatParameter("velSens", "Vel Sens",
                "Velocity sensitivity",
                0.0f, 100.0f, 70.0f, "%");

        quantizeParam = addChoiceParameter("quantize", "Quantize",
                "Note quantization",
                new String[]{"Off", "1/4", "1/8", "1/16", "1/32"}, 0);

        fileParam = addChoiceParameter("file", "File",
                "Output file (auto-named)",
                new String[]{"midi_recording.mid"}, 0);

        activeNotes = new int[MAX_POLYPHONY];
        noteOnTimes = new long[MAX_POLYPHONY];
        noteVelocities = new int[MAX_POLYPHONY];
        Arrays.fill(activeNotes, -1);
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // YIN buffer
        yinBuffer = new float[YIN_BUFFER_SIZE / 2];

        // FFT buffers
        fftBuffer = new float[FFT_SIZE];
        fftWindow = new float[FFT_SIZE];
        magnitudes = new float[FFT_SIZE / 2];

        // Hann window for FFT
        for (int i = 0; i < FFT_SIZE; i++) {
            fftWindow[i] = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
        }

        // Sample accumulator
        sampleAccumulator = new float[Math.max(YIN_BUFFER_SIZE, FFT_SIZE)];
        accumulatorPos = 0;

        // Initialize MIDI sequence
        try {
            midiSequence = new Sequence(Sequence.PPQ, 480); // 480 ticks per quarter note
            midiTrack = midiSequence.createTrack();
            tickPosition = 0;
            recording = false;
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }

        lastDetectedPitch = 0;
        pitchConfirmCount = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        if (!recording) {
            startRecording();
        }

        // Pass through audio
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));

        // Accumulate samples for pitch detection
        int samplesToProcess = Math.min(frameCount, input.length);
        for (int i = 0; i < samplesToProcess; i++) {
            sampleAccumulator[accumulatorPos++] = input[i];

            int bufferSize = (modeParam.getChoiceIndex() == 0) ? YIN_BUFFER_SIZE : FFT_SIZE;

            if (accumulatorPos >= bufferSize) {
                // Process accumulated samples
                processPitchDetection();
                accumulatorPos = 0;
            }
        }

        // Update tick position based on samples processed
        updateTickPosition(frameCount);
    }

    private static final String MIDI_OUTPUT_DIR = "./midi-output/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private void startRecording() {
        recording = true;
        startTimeMs = System.currentTimeMillis();
        tickPosition = 0;
        activeNoteCount = 0;
        Arrays.fill(activeNotes, -1);

        // Create output directory if it doesn't exist
        File outputDir = new File(MIDI_OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Generate output file name with timestamp yyyyMMddHHmmss.mid
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        outputFilePath = MIDI_OUTPUT_DIR + timestamp + ".mid";
    }

    private void updateTickPosition(int frameCount) {
        // Get tempo from Settings block or use default
        float bpm = 120.0f;
        SettingsEffect settings = SettingsEffect.getGlobalInstance();
        if (settings != null) {
            bpm = settings.getBpm();
        }

        // Calculate ticks: (samples / sampleRate) * (bpm / 60) * ticksPerBeat
        double seconds = (double) frameCount / sampleRate;
        double beats = seconds * bpm / 60.0;
        double ticks = beats * 480; // 480 ticks per quarter note
        tickPosition += (long) ticks;
    }

    private void processPitchDetection() {
        float threshold = (float) Math.pow(10, thresholdParam.getTargetValue() / 20.0);

        // Calculate RMS level
        float rms = 0;
        int bufferSize = (modeParam.getChoiceIndex() == 0) ? YIN_BUFFER_SIZE : FFT_SIZE;
        for (int i = 0; i < bufferSize; i++) {
            rms += sampleAccumulator[i] * sampleAccumulator[i];
        }
        rms = (float) Math.sqrt(rms / bufferSize);

        if (rms < threshold) {
            // Signal below threshold - turn off all notes
            handleSilence();
            lastDetectedPitch = 0;
            pitchConfirmCount = 0;
            return;
        }

        // Detect pitch based on mode
        float[] detectedPitches;
        float[] detectedAmplitudes;

        if (modeParam.getChoiceIndex() == 0) {
            // Monophonic - YIN algorithm
            float pitch = detectPitchYIN(sampleAccumulator, sampleRate);
            if (pitch > 0) {
                detectedPitches = new float[]{pitch};
                detectedAmplitudes = new float[]{rms};
            } else {
                detectedPitches = new float[0];
                detectedAmplitudes = new float[0];
            }
        } else {
            // Polyphonic - FFT peaks
            float[][] result = detectPitchesFFT(sampleAccumulator, sampleRate);
            detectedPitches = result[0];
            detectedAmplitudes = result[1];
        }

        // Convert pitches to MIDI notes and handle note on/off
        handleDetectedPitches(detectedPitches, detectedAmplitudes, rms);
    }

    /**
     * YIN pitch detection algorithm for monophonic signals.
     */
    private float detectPitchYIN(float[] buffer, int sampleRate) {
        int halfBufferSize = YIN_BUFFER_SIZE / 2;
        float threshold = 0.15f;

        // Step 1: Calculate difference function
        for (int tau = 0; tau < halfBufferSize; tau++) {
            yinBuffer[tau] = 0;
            for (int i = 0; i < halfBufferSize; i++) {
                float delta = buffer[i] - buffer[i + tau];
                yinBuffer[tau] += delta * delta;
            }
        }

        // Step 2: Cumulative mean normalized difference
        yinBuffer[0] = 1;
        float runningSum = 0;
        for (int tau = 1; tau < halfBufferSize; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] *= tau / runningSum;
        }

        // Step 3: Absolute threshold
        int tauEstimate = -1;
        for (int tau = 2; tau < halfBufferSize; tau++) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfBufferSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++;
                }
                tauEstimate = tau;
                break;
            }
        }

        if (tauEstimate == -1) {
            return -1; // No pitch detected
        }

        // Step 4: Parabolic interpolation
        float betterTau;
        if (tauEstimate > 0 && tauEstimate < halfBufferSize - 1) {
            float s0 = yinBuffer[tauEstimate - 1];
            float s1 = yinBuffer[tauEstimate];
            float s2 = yinBuffer[tauEstimate + 1];
            betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        } else {
            betterTau = tauEstimate;
        }

        return sampleRate / betterTau;
    }

    /**
     * FFT-based polyphonic pitch detection.
     */
    private float[][] detectPitchesFFT(float[] buffer, int sampleRate) {
        // Apply window
        for (int i = 0; i < FFT_SIZE; i++) {
            fftBuffer[i] = buffer[i] * fftWindow[i];
        }

        // Simple DFT (for real implementation, use FFT library)
        computeMagnitudes(fftBuffer, magnitudes);

        // Find peaks
        List<float[]> peaks = new ArrayList<>();
        float minFreq = midiToFreq((int) minNoteParam.getTargetValue());
        float maxFreq = midiToFreq((int) maxNoteParam.getTargetValue());

        int minBin = (int) (minFreq * FFT_SIZE / sampleRate);
        int maxBin = (int) (maxFreq * FFT_SIZE / sampleRate);
        maxBin = Math.min(maxBin, magnitudes.length - 1);

        float maxMag = 0;
        for (int i = minBin; i <= maxBin; i++) {
            if (magnitudes[i] > maxMag) maxMag = magnitudes[i];
        }

        float peakThreshold = maxMag * 0.3f;

        for (int i = minBin + 1; i < maxBin; i++) {
            if (magnitudes[i] > magnitudes[i - 1] &&
                magnitudes[i] > magnitudes[i + 1] &&
                magnitudes[i] > peakThreshold) {

                // Parabolic interpolation for better frequency estimate
                float alpha = magnitudes[i - 1];
                float beta = magnitudes[i];
                float gamma = magnitudes[i + 1];
                float p = 0.5f * (alpha - gamma) / (alpha - 2 * beta + gamma);

                float freq = (i + p) * sampleRate / FFT_SIZE;
                peaks.add(new float[]{freq, magnitudes[i]});
            }
        }

        // Sort by amplitude and take top MAX_POLYPHONY
        peaks.sort((a, b) -> Float.compare(b[1], a[1]));

        int numPeaks = Math.min(peaks.size(), MAX_POLYPHONY);
        float[] pitches = new float[numPeaks];
        float[] amplitudes = new float[numPeaks];

        for (int i = 0; i < numPeaks; i++) {
            pitches[i] = peaks.get(i)[0];
            amplitudes[i] = peaks.get(i)[1];
        }

        return new float[][]{pitches, amplitudes};
    }

    /**
     * Simple DFT magnitude computation.
     */
    private void computeMagnitudes(float[] input, float[] output) {
        int n = input.length;
        int numBins = output.length;

        for (int k = 0; k < numBins; k++) {
            float real = 0;
            float imag = 0;
            for (int t = 0; t < n; t++) {
                float angle = (float) (2 * Math.PI * k * t / n);
                real += input[t] * Math.cos(angle);
                imag -= input[t] * Math.sin(angle);
            }
            output[k] = (float) Math.sqrt(real * real + imag * imag);
        }
    }

    private void handleDetectedPitches(float[] pitches, float[] amplitudes, float rms) {
        int minNote = (int) minNoteParam.getTargetValue();
        int maxNote = (int) maxNoteParam.getTargetValue();
        float velSens = velocitySensParam.getTargetValue() / 100.0f;

        // Convert frequencies to MIDI notes
        Set<Integer> detectedNotes = new HashSet<>();
        Map<Integer, Float> noteAmplitudes = new HashMap<>();

        for (int i = 0; i < pitches.length; i++) {
            int midiNote = freqToMidi(pitches[i]);
            if (midiNote >= minNote && midiNote <= maxNote) {
                detectedNotes.add(midiNote);
                noteAmplitudes.put(midiNote, amplitudes[i]);
            }
        }

        // Check for note offs (active notes no longer detected)
        for (int i = 0; i < MAX_POLYPHONY; i++) {
            if (activeNotes[i] >= 0 && !detectedNotes.contains(activeNotes[i])) {
                // Note off
                addNoteOffEvent(activeNotes[i], tickPosition);
                activeNotes[i] = -1;
                activeNoteCount--;
            }
        }

        // Check for note ons (new notes detected)
        for (int note : detectedNotes) {
            boolean alreadyActive = false;
            for (int i = 0; i < MAX_POLYPHONY; i++) {
                if (activeNotes[i] == note) {
                    alreadyActive = true;
                    break;
                }
            }

            if (!alreadyActive && activeNoteCount < MAX_POLYPHONY) {
                // Find empty slot
                for (int i = 0; i < MAX_POLYPHONY; i++) {
                    if (activeNotes[i] < 0) {
                        activeNotes[i] = note;
                        noteOnTimes[i] = tickPosition;

                        // Calculate velocity based on amplitude
                        float amp = noteAmplitudes.getOrDefault(note, rms);
                        int velocity = (int) (64 + 63 * velSens * Math.min(1.0f, amp * 10));
                        velocity = Math.max(1, Math.min(127, velocity));
                        noteVelocities[i] = velocity;

                        addNoteOnEvent(note, velocity, tickPosition);
                        activeNoteCount++;
                        break;
                    }
                }
            }
        }
    }

    private void handleSilence() {
        // Turn off all active notes
        for (int i = 0; i < MAX_POLYPHONY; i++) {
            if (activeNotes[i] >= 0) {
                addNoteOffEvent(activeNotes[i], tickPosition);
                activeNotes[i] = -1;
            }
        }
        activeNoteCount = 0;
    }

    private void addNoteOnEvent(int note, int velocity, long tick) {
        if (midiTrack == null) return;

        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON, 0, note, velocity);
            midiTrack.add(new MidiEvent(msg, tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void addNoteOffEvent(int note, long tick) {
        if (midiTrack == null) return;

        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF, 0, note, 0);
            midiTrack.add(new MidiEvent(msg, tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private float midiToFreq(int midi) {
        return (float) (440.0 * Math.pow(2, (midi - 69) / 12.0));
    }

    private int freqToMidi(float freq) {
        return (int) Math.round(69 + 12 * Math.log(freq / 440.0) / Math.log(2));
    }

    @Override
    protected void onReset() {
        accumulatorPos = 0;
        lastDetectedPitch = 0;
        pitchConfirmCount = 0;

        // Turn off all notes and save MIDI file
        if (recording) {
            handleSilence();
            saveMidiFile();
            recording = false;
        }

        // Reset MIDI sequence for next recording
        try {
            midiSequence = new Sequence(Sequence.PPQ, 480);
            midiTrack = midiSequence.createTrack();
            tickPosition = 0;
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }

        Arrays.fill(activeNotes, -1);
        activeNoteCount = 0;
    }

    @Override
    public void release() {
        // Save MIDI file if recording
        if (recording) {
            handleSilence();
            saveMidiFile();
            recording = false;
        }
        super.release();
    }

    private void saveMidiFile() {
        if (midiSequence == null || outputFilePath == null) return;

        // Add tempo meta event
        float bpm = 120.0f;
        SettingsEffect settings = SettingsEffect.getGlobalInstance();
        if (settings != null) {
            bpm = settings.getBpm();
        }

        try {
            // Tempo meta event (microseconds per beat)
            int mpqn = (int) (60000000.0 / bpm);
            byte[] tempoData = new byte[]{
                    (byte) ((mpqn >> 16) & 0xFF),
                    (byte) ((mpqn >> 8) & 0xFF),
                    (byte) (mpqn & 0xFF)
            };
            MetaMessage tempoMsg = new MetaMessage();
            tempoMsg.setMessage(0x51, tempoData, 3);
            midiTrack.add(new MidiEvent(tempoMsg, 0));

            // Save to file
            File outputFile = new File(outputFilePath);
            MidiSystem.write(midiSequence, 1, outputFile);

            System.out.println("MIDI file saved: " + outputFile.getAbsolutePath());
        } catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Mode, Threshold
        // Row 2: Min/Max Note, Vel Sens
        // Row 3: Quantize
        return new int[]{2, 3, 1};
    }
}
