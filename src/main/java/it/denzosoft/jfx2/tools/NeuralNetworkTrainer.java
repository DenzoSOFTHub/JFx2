package it.denzosoft.jfx2.tools;

import it.denzosoft.jfx2.nn.ActivationFunction;
import it.denzosoft.jfx2.nn.NeuralNetwork;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Path;

/**
 * Neural Network Trainer for audio effect modeling.
 *
 * <p>Trains a neural network to learn the transformation from a dry
 * audio signal to a wet (processed) signal. The trained network can
 * then be used as an audio effect.</p>
 *
 * <p>Usage example:
 * <pre>
 * NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();
 * trainer.train("dry.wav", "wet.wav", "model.jfxnn");
 * </pre></p>
 */
public class NeuralNetworkTrainer {

    // Training configuration
    private int inputWindowSize = 64;
    private int[] hiddenSizes = {32, 16, 8};
    private ActivationFunction hiddenActivation = ActivationFunction.TANH;
    private float learningRate = 0.001f;
    private int batchSize = 64;
    private int maxEpochs = 100;
    private float convergenceThreshold = 0.0001f;  // MSE threshold for early stopping
    private float convergencePatience = 5;  // Epochs without improvement before stopping
    private int chunkSize = 44100;  // Process 1 second chunks at a time
    private boolean verbose = true;

    // Training state
    private NeuralNetwork network;
    private float bestLoss = Float.MAX_VALUE;
    private int epochsWithoutImprovement = 0;

    public NeuralNetworkTrainer() {
    }

    /**
     * Train the network from dry and wet WAV files.
     *
     * @param dryWavPath Path to dry (input) WAV file
     * @param wetWavPath Path to wet (target) WAV file
     * @param outputModelPath Path to save the trained model
     * @return Final training loss (MSE)
     */
    public float train(String dryWavPath, String wetWavPath, String outputModelPath) throws IOException {
        return train(Path.of(dryWavPath), Path.of(wetWavPath), Path.of(outputModelPath));
    }

    /**
     * Train the network from dry and wet WAV files.
     */
    public float train(Path dryWavPath, Path wetWavPath, Path outputModelPath) throws IOException {
        log("=== Neural Network Trainer ===\n");

        // Load audio files
        log("Loading dry audio: " + dryWavPath);
        float[] dryAudio = loadWavFile(dryWavPath);
        log("  Samples: " + dryAudio.length);

        log("Loading wet audio: " + wetWavPath);
        float[] wetAudio = loadWavFile(wetWavPath);
        log("  Samples: " + wetAudio.length);

        // Verify lengths match
        int totalSamples = Math.min(dryAudio.length, wetAudio.length);
        if (dryAudio.length != wetAudio.length) {
            log("WARNING: Audio lengths differ. Using " + totalSamples + " samples.");
        }

        // Create network
        network = new NeuralNetwork(inputWindowSize, hiddenSizes, 1, hiddenActivation);
        network.setLearningRate(learningRate);
        network.setBatchSize(batchSize);
        network.setUseAdam(true);

        log("\n" + network.getSummary());
        log("Training configuration:");
        log("  Learning rate: " + learningRate);
        log("  Batch size: " + batchSize);
        log("  Max epochs: " + maxEpochs);
        log("  Convergence threshold: " + convergenceThreshold);
        log("  Chunk size: " + chunkSize + " samples");
        log("");

        // Calculate number of trainable samples
        int trainableSamples = totalSamples - inputWindowSize;
        int numChunks = (trainableSamples + chunkSize - 1) / chunkSize;

        log("Trainable samples: " + trainableSamples);
        log("Number of chunks: " + numChunks);
        log("");

        // Training loop
        float finalLoss = 0;
        long startTime = System.currentTimeMillis();

        for (int epoch = 0; epoch < maxEpochs; epoch++) {
            float epochLoss = 0;
            int sampleCount = 0;

            // Process chunks
            for (int chunk = 0; chunk < numChunks; chunk++) {
                int chunkStart = chunk * chunkSize;
                int chunkEnd = Math.min(chunkStart + chunkSize, trainableSamples);

                float chunkLoss = trainChunk(dryAudio, wetAudio, chunkStart, chunkEnd);
                epochLoss += chunkLoss * (chunkEnd - chunkStart);
                sampleCount += (chunkEnd - chunkStart);

                // Check for early convergence within epoch
                float currentAvgLoss = epochLoss / sampleCount;
                if (currentAvgLoss < convergenceThreshold) {
                    log(String.format("Early convergence at epoch %d, chunk %d (loss: %.6f)",
                            epoch + 1, chunk + 1, currentAvgLoss));
                    finalLoss = currentAvgLoss;
                    saveModel(outputModelPath, finalLoss);
                    return finalLoss;
                }
            }

            // Average loss for epoch
            epochLoss /= sampleCount;
            finalLoss = epochLoss;

            // Log progress
            if (verbose) {
                long elapsed = System.currentTimeMillis() - startTime;
                log(String.format("Epoch %3d/%d - Loss: %.6f - Time: %.1fs",
                        epoch + 1, maxEpochs, epochLoss, elapsed / 1000.0));
            }

            // Check for improvement
            if (epochLoss < bestLoss - convergenceThreshold * 0.1f) {
                bestLoss = epochLoss;
                epochsWithoutImprovement = 0;

                // Save best model
                saveModel(outputModelPath, finalLoss);
            } else {
                epochsWithoutImprovement++;

                if (epochsWithoutImprovement >= convergencePatience) {
                    log(String.format("Early stopping: no improvement for %d epochs", (int) convergencePatience));
                    break;
                }
            }

            // Check convergence
            if (epochLoss < convergenceThreshold) {
                log(String.format("Converged at epoch %d (loss: %.6f)", epoch + 1, epochLoss));
                break;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log(String.format("\nTraining complete. Final loss: %.6f, Time: %.1fs", finalLoss, totalTime / 1000.0));
        log("Model saved to: " + outputModelPath);

        return finalLoss;
    }

    /**
     * Train on a single chunk of audio.
     */
    private float trainChunk(float[] dryAudio, float[] wetAudio, int startSample, int endSample) {
        float chunkLoss = 0;
        int batchCount = 0;

        float[] inputWindow = new float[inputWindowSize];
        float[] target = new float[1];

        network.clearGradients();

        for (int i = startSample; i < endSample; i++) {
            // Build input window
            int windowStart = i;
            for (int j = 0; j < inputWindowSize; j++) {
                inputWindow[j] = dryAudio[windowStart + j];
            }

            // Target is the corresponding wet sample
            target[0] = wetAudio[windowStart + inputWindowSize - 1];

            // Forward + backward
            float loss = network.trainStep(inputWindow, target);
            chunkLoss += loss;
            batchCount++;

            // Update weights every batch
            if (batchCount >= batchSize) {
                network.updateWeights();
                network.clearGradients();
                batchCount = 0;
            }
        }

        // Final batch update
        if (batchCount > 0) {
            network.updateWeights();
            network.clearGradients();
        }

        return chunkLoss / (endSample - startSample);
    }

    /**
     * Save the trained model.
     */
    private void saveModel(Path outputPath, float finalLoss) throws IOException {
        network.save(outputPath);
    }

    /**
     * Load a WAV file and return samples as float array.
     */
    private float[] loadWavFile(Path wavPath) throws IOException {
        try {
            File file = wavPath.toFile();
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

            // Read all bytes
            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            // Convert to float samples
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int numChannels = format.getChannels();
            int bytesPerFrame = bytesPerSample * numChannels;
            int numFrames = audioBytes.length / bytesPerFrame;

            float[] samples = new float[numFrames];

            for (int i = 0; i < numFrames; i++) {
                int framePos = i * bytesPerFrame;

                // Read first channel only (mono or left channel of stereo)
                float sample = readSample(audioBytes, framePos, bytesPerSample, format.getEncoding());

                // If stereo, average with right channel
                if (numChannels >= 2) {
                    float rightSample = readSample(audioBytes, framePos + bytesPerSample, bytesPerSample, format.getEncoding());
                    sample = (sample + rightSample) * 0.5f;
                }

                samples[i] = sample;
            }

            return samples;

        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Unsupported audio format: " + e.getMessage());
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

    private void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    // Configuration setters
    public NeuralNetworkTrainer setInputWindowSize(int size) {
        this.inputWindowSize = size;
        return this;
    }

    public NeuralNetworkTrainer setHiddenSizes(int... sizes) {
        this.hiddenSizes = sizes;
        return this;
    }

    public NeuralNetworkTrainer setHiddenActivation(ActivationFunction activation) {
        this.hiddenActivation = activation;
        return this;
    }

    public NeuralNetworkTrainer setLearningRate(float rate) {
        this.learningRate = rate;
        return this;
    }

    public NeuralNetworkTrainer setBatchSize(int size) {
        this.batchSize = size;
        return this;
    }

    public NeuralNetworkTrainer setMaxEpochs(int epochs) {
        this.maxEpochs = epochs;
        return this;
    }

    public NeuralNetworkTrainer setConvergenceThreshold(float threshold) {
        this.convergenceThreshold = threshold;
        return this;
    }

    public NeuralNetworkTrainer setChunkSize(int size) {
        this.chunkSize = size;
        return this;
    }

    public NeuralNetworkTrainer setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /**
     * Get the trained network (null if not yet trained).
     */
    public NeuralNetwork getNetwork() {
        return network;
    }

    /**
     * Command-line interface for training.
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: NeuralNetworkTrainer <dry.wav> <wet.wav> <output.jfxnn> [options]");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --window <size>     Input window size (default: 64)");
            System.out.println("  --hidden <sizes>    Hidden layer sizes, comma-separated (default: 32,16,8)");
            System.out.println("  --lr <rate>         Learning rate (default: 0.001)");
            System.out.println("  --batch <size>      Batch size (default: 64)");
            System.out.println("  --epochs <num>      Max epochs (default: 100)");
            System.out.println("  --threshold <val>   Convergence threshold (default: 0.0001)");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java NeuralNetworkTrainer guitar_dry.wav guitar_amped.wav amp_model.jfxnn");
            return;
        }

        String dryPath = args[0];
        String wetPath = args[1];
        String outputPath = args[2];

        NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();

        // Parse options
        for (int i = 3; i < args.length; i++) {
            switch (args[i]) {
                case "--window" -> trainer.setInputWindowSize(Integer.parseInt(args[++i]));
                case "--hidden" -> {
                    String[] parts = args[++i].split(",");
                    int[] sizes = new int[parts.length];
                    for (int j = 0; j < parts.length; j++) {
                        sizes[j] = Integer.parseInt(parts[j].trim());
                    }
                    trainer.setHiddenSizes(sizes);
                }
                case "--lr" -> trainer.setLearningRate(Float.parseFloat(args[++i]));
                case "--batch" -> trainer.setBatchSize(Integer.parseInt(args[++i]));
                case "--epochs" -> trainer.setMaxEpochs(Integer.parseInt(args[++i]));
                case "--threshold" -> trainer.setConvergenceThreshold(Float.parseFloat(args[++i]));
            }
        }

        try {
            trainer.train(dryPath, wetPath, outputPath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
