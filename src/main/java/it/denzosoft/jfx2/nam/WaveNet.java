package it.denzosoft.jfx2.nam;

import it.denzosoft.jfx2.nam.json.JsonValue;
import java.util.List;

/**
 * WaveNet model for Neural Amp Modeler.
 *
 * <p>Architecture:
 * <pre>
 *      input (1 sample)
 *            │
 *        ┌───┴───┐
 *        │ head  │  1x1 conv: 1 -> channels
 *        └───┬───┘
 *            │
 *     ┌──────┴──────┐
 *     │  LayerArray │  Multiple dilated conv layers
 *     └──────┬──────┘
 *            │
 *     ┌──────┴──────┐  (repeat for each layer array)
 *     │  LayerArray │
 *     └──────┬──────┘
 *            │
 *        skip accumulator
 *            │
 *     ┌──────┴──────┐
 *     │    head     │  2x 1x1 conv with activation
 *     └──────┬──────┘
 *            │
 *         output (1 sample) * head_scale
 * </pre></p>
 */
public class WaveNet implements NAMModel {

    private final int sampleRate;

    // Input head: 1 -> channels
    private final Conv1x1 inputHead;

    // Layer arrays
    private final WaveNetLayerArray[] layerArrays;

    // Output head
    private final Conv1x1 headLayer1;  // headChannels -> headChannels
    private final Conv1x1 headLayer2;  // headChannels -> 1
    private final String headActivation;
    private final float headScale;

    // Configuration
    private final int channels;
    private final int headChannels;
    private final int receptiveField;

    // Buffers
    private final float[] inputExpanded;
    private final float[] condition;
    private final float[] skipAccum;
    private final float[] layerOutput;
    private final float[] headTemp;

    // Prewarm counter
    private int samplesProcessed = 0;

    public WaveNet(JsonValue config, float[] weights, int sampleRate) {
        this.sampleRate = sampleRate;

        // Parse config
        this.channels = config.getInt("channels", 16);
        this.headChannels = config.getInt("head_size", 8);
        this.headScale = config.getFloat("head_scale", 0.02f);
        this.headActivation = config.getString("head_activation", "Tanh");

        int kernelSize = config.getInt("kernel_size", 3);
        String activation = config.getString("activation", "Tanh");
        boolean gated = config.getBoolean("gated", true);

        // Parse dilations - can be nested array for multiple layer arrays
        List<JsonValue> dilationsConfig = config.getArray("dilations");
        int numLayerArrays;
        int[][] dilationsPerArray;

        if (dilationsConfig != null && !dilationsConfig.isEmpty()) {
            if (dilationsConfig.get(0).isArray()) {
                // Nested array: [[1,2,4,8,16,32], [1,2,4,8,16,32]]
                numLayerArrays = dilationsConfig.size();
                dilationsPerArray = new int[numLayerArrays][];
                for (int i = 0; i < numLayerArrays; i++) {
                    dilationsPerArray[i] = dilationsConfig.get(i).asIntArray();
                }
            } else {
                // Single array: [1,2,4,8,16,32] - use twice as default
                numLayerArrays = 2;
                int[] dilations = config.get("dilations").asIntArray();
                dilationsPerArray = new int[][] { dilations, dilations };
            }
        } else {
            // Default dilations
            numLayerArrays = 2;
            dilationsPerArray = new int[][] {
                    {1, 2, 4, 8, 16, 32},
                    {1, 2, 4, 8, 16, 32}
            };
        }

        // Create layers
        this.inputHead = new Conv1x1(1, channels, false);

        this.layerArrays = new WaveNetLayerArray[numLayerArrays];
        int totalReceptiveField = 1;

        for (int i = 0; i < numLayerArrays; i++) {
            layerArrays[i] = new WaveNetLayerArray(
                    channels,    // inputSize
                    1,           // conditionSize (raw input sample)
                    headChannels,
                    channels,
                    kernelSize,
                    dilationsPerArray[i],
                    activation,
                    gated
            );
            totalReceptiveField += layerArrays[i].getReceptiveField() - 1;
        }

        this.receptiveField = totalReceptiveField;

        // Output head
        this.headLayer1 = new Conv1x1(headChannels, headChannels, true);
        this.headLayer2 = new Conv1x1(headChannels, 1, true);

        // Allocate buffers
        this.inputExpanded = new float[channels];
        this.condition = new float[1];
        this.skipAccum = new float[headChannels];
        this.layerOutput = new float[channels];
        this.headTemp = new float[headChannels];

        // Load weights
        loadWeights(weights);
    }

    private void loadWeights(float[] weights) {
        int pos = 0;

        // Input head weights
        inputHead.setWeights(weights, pos);
        pos += inputHead.getWeightCount();

        // Layer array weights
        for (WaveNetLayerArray layerArray : layerArrays) {
            pos += layerArray.loadWeights(weights, pos);
        }

        // Output head weights
        headLayer1.setWeights(weights, pos);
        pos += headLayer1.getWeightCount();
        headLayer1.setBiases(weights, pos);
        pos += headLayer1.getBiasCount();

        headLayer2.setWeights(weights, pos);
        pos += headLayer2.getWeightCount();
        headLayer2.setBiases(weights, pos);
        pos += headLayer2.getBiasCount();

        // Verify all weights consumed (optional check)
        if (pos != weights.length) {
            System.err.println("Warning: WaveNet weight count mismatch. Expected " +
                    weights.length + ", used " + pos);
        }
    }

    @Override
    public float process(float sample) {
        // Condition is the raw input
        condition[0] = sample;

        // Expand input through head
        float[] inputSample = {sample};
        inputHead.process(inputSample, inputExpanded);

        // Clear skip accumulator
        java.util.Arrays.fill(skipAccum, 0);

        // Process through layer arrays
        System.arraycopy(inputExpanded, 0, layerOutput, 0, channels);
        for (WaveNetLayerArray layerArray : layerArrays) {
            layerArray.process(layerOutput, condition, skipAccum, layerOutput);
        }

        // Output head
        // First layer with activation
        headLayer1.process(skipAccum, headTemp);
        Activations.applyInPlace(headActivation, headTemp);

        // Second layer (linear)
        float[] output = new float[1];
        headLayer2.process(headTemp, output);

        // Scale
        float result = output[0] * headScale;

        samplesProcessed++;
        return result;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        for (int i = 0; i < numSamples; i++) {
            output[i] = process(input[i]);
        }
    }

    @Override
    public void reset() {
        for (WaveNetLayerArray layerArray : layerArrays) {
            layerArray.reset();
        }
        samplesProcessed = 0;
    }

    @Override
    public int getReceptiveField() {
        return receptiveField;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public String getArchitecture() {
        return "WaveNet";
    }

    @Override
    public boolean isPrewarmed() {
        return samplesProcessed >= receptiveField;
    }

    /**
     * Get a summary of the model.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("WaveNet Model:\n");
        sb.append("  Channels: ").append(channels).append("\n");
        sb.append("  Head channels: ").append(headChannels).append("\n");
        sb.append("  Layer arrays: ").append(layerArrays.length).append("\n");
        for (int i = 0; i < layerArrays.length; i++) {
            sb.append("    [").append(i).append("] ")
                    .append(layerArrays[i].getLayerCount()).append(" layers\n");
        }
        sb.append("  Receptive field: ").append(receptiveField).append(" samples\n");
        sb.append("  Head scale: ").append(headScale).append("\n");
        sb.append("  Sample rate: ").append(sampleRate).append(" Hz\n");
        return sb.toString();
    }
}
