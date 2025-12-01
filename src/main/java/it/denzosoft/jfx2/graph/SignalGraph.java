package it.denzosoft.jfx2.graph;

import java.util.*;
import java.util.concurrent.*;

/**
 * Signal processing graph.
 *
 * <p>Manages nodes and connections, and processes audio through the graph
 * in topologically sorted order. Supports parallel processing of independent
 * branches for multi-core optimization.</p>
 */
public class SignalGraph {

    private final Map<String, ProcessingNode> nodes;
    private final Map<String, Connection> connections;

    private InputNode inputNode;
    private OutputNode outputNode;

    // Processing order (topologically sorted)
    private List<ProcessingNode> processingOrder;
    // Parallel processing levels (nodes at same level are independent)
    private List<List<ProcessingNode>> parallelLevels;
    private boolean orderDirty;

    // Configuration
    private int sampleRate;
    private int maxFrameCount;

    // Level metering (in dB, updated during process)
    private volatile float inputLevelDb = -60f;
    private volatile float outputLevelDb = -60f;
    private static final float MIN_DB = -60f;

    // Input audio listener (for tuner, etc.)
    private InputAudioListener inputAudioListener;
    private String tunerSourceNodeId = null;  // null = auto (AudioInput), or specific node ID

    // Output audio listener (for signal monitor, etc.)
    private OutputAudioListener outputAudioListener;

    // Input FFT listener (for spectrum analyzers, etc.)
    private InputFFTListener inputFFTListener;

    // FFT calculation buffers
    private static final int FFT_SIZE = 2048;
    private final float[] fftInputBuffer = new float[FFT_SIZE];
    private final float[] fftMagnitudes = new float[FFT_SIZE / 2];
    private int fftBufferPos = 0;
    private int currentSampleRate = 44100;

    /**
     * Listener for receiving input audio samples.
     */
    public interface InputAudioListener {
        void onInputAudio(float[] samples, int length);
    }

    /**
     * Listener for receiving output audio samples (processed signal).
     */
    public interface OutputAudioListener {
        void onOutputAudio(float[] samples, int length);
    }

    /**
     * Listener for receiving FFT magnitude data from input audio.
     * FFT is computed on the left channel (or mono) input signal.
     */
    public interface InputFFTListener {
        /**
         * Called when new FFT data is available.
         *
         * @param magnitudes   FFT magnitude values (linear, 0-1 range normalized)
         * @param numBins      Number of valid bins (FFT_SIZE / 2)
         * @param sampleRate   Sample rate for frequency calculation
         * @param binFrequency Frequency resolution per bin (sampleRate / FFT_SIZE)
         */
        void onInputFFT(float[] magnitudes, int numBins, int sampleRate, float binFrequency);
    }

    // Parallel processing
    private boolean parallelProcessingEnabled = true;
    private static final int MIN_NODES_FOR_PARALLEL = 2;  // Minimum nodes at level to use parallel
    private ExecutorService threadPool;
    private int threadCount;

    public SignalGraph() {
        this.nodes = new LinkedHashMap<>();
        this.connections = new LinkedHashMap<>();
        this.processingOrder = new ArrayList<>();
        this.parallelLevels = new ArrayList<>();
        this.orderDirty = true;

        // Create thread pool with available processors (minus 1 for audio thread)
        this.threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.threadPool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "AudioProcessor");
            t.setPriority(Thread.MAX_PRIORITY);  // High priority for audio
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Create a default graph with input and output nodes.
     */
    public void createDefaultGraph() {
        // Create input and output nodes
        inputNode = new InputNode("input", PortType.AUDIO_MONO);
        outputNode = new OutputNode("output", PortType.AUDIO_STEREO);

        addNode(inputNode);
        addNode(outputNode);

        // Connect input directly to output (passthrough)
        connect(inputNode.getOutput(), outputNode.getInput());
    }

    /**
     * Add a node to the graph.
     *
     * @param node The node to add
     */
    public void addNode(ProcessingNode node) {
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Node with ID '" + node.getId() + "' already exists");
        }
        nodes.put(node.getId(), node);
        orderDirty = true;

        // If already prepared, prepare the new node
        if (sampleRate > 0) {
            node.prepare(sampleRate, maxFrameCount);
        }
    }

    /**
     * Remove a node from the graph.
     *
     * @param nodeId ID of the node to remove
     */
    public void removeNode(String nodeId) {
        ProcessingNode node = nodes.get(nodeId);
        if (node == null) {
            return;
        }

        // Clear reference if removing input/output node
        if (node == inputNode) {
            inputNode = null;
        } else if (node == outputNode) {
            outputNode = null;
        }

        // Release node resources
        node.release();

        // Remove all connections to/from this node
        List<String> connectionsToRemove = new ArrayList<>();
        for (Connection conn : connections.values()) {
            if (conn.getSourceNode() == node || conn.getTargetNode() == node) {
                connectionsToRemove.add(conn.getId());
            }
        }
        for (String connId : connectionsToRemove) {
            disconnect(connId);
        }

        nodes.remove(nodeId);
        orderDirty = true;
    }

    /**
     * Get a node by ID.
     *
     * @param nodeId Node identifier
     * @return The node, or null if not found
     */
    public ProcessingNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Connect two ports.
     *
     * @param sourcePort Output port (source)
     * @param targetPort Input port (target)
     * @return The created connection
     */
    public Connection connect(Port sourcePort, Port targetPort) {
        // Validate connection
        if (!sourcePort.canConnectTo(targetPort)) {
            throw new IllegalArgumentException("Cannot connect " + sourcePort + " to " + targetPort);
        }

        // Check for cycles
        if (wouldCreateCycle(sourcePort.getOwner(), targetPort.getOwner())) {
            throw new IllegalArgumentException("Connection would create a cycle");
        }

        // Create connection
        String connId = "conn_" + System.currentTimeMillis() + "_" + connections.size();
        Connection connection = new Connection(connId, sourcePort, targetPort);
        connections.put(connId, connection);
        orderDirty = true;

        return connection;
    }

    /**
     * Connect two nodes using default ports.
     *
     * @param sourceNodeId Source node ID
     * @param targetNodeId Target node ID
     * @return The created connection
     */
    public Connection connect(String sourceNodeId, String targetNodeId) {
        ProcessingNode source = nodes.get(sourceNodeId);
        ProcessingNode target = nodes.get(targetNodeId);

        if (source == null) {
            throw new IllegalArgumentException("Source node not found: " + sourceNodeId);
        }
        if (target == null) {
            throw new IllegalArgumentException("Target node not found: " + targetNodeId);
        }

        // Use first available ports
        List<Port> sourceOutputs = source.getOutputPorts();
        List<Port> targetInputs = target.getInputPorts();

        if (sourceOutputs.isEmpty()) {
            throw new IllegalArgumentException("Source node has no output ports");
        }
        if (targetInputs.isEmpty()) {
            throw new IllegalArgumentException("Target node has no input ports");
        }

        return connect(sourceOutputs.get(0), targetInputs.get(0));
    }

    /**
     * Disconnect a connection by ID.
     *
     * @param connectionId Connection identifier
     */
    public void disconnect(String connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection != null) {
            connection.disconnect();
            orderDirty = true;
        }
    }

    /**
     * Connect two specific ports by node and port names.
     *
     * @param sourceNodeId Source node ID
     * @param sourcePortName Source port name
     * @param targetNodeId Target node ID
     * @param targetPortName Target port name
     * @return The created connection
     */
    public Connection connect(String sourceNodeId, String sourcePortName,
                              String targetNodeId, String targetPortName) {
        ProcessingNode sourceNode = getNode(sourceNodeId);
        ProcessingNode targetNode = getNode(targetNodeId);

        if (sourceNode == null || targetNode == null) {
            throw new IllegalArgumentException("Source or target node not found");
        }

        Port sourcePort = sourceNode.getOutputPort(sourcePortName);
        Port targetPort = targetNode.getInputPort(targetPortName);

        if (sourcePort == null || targetPort == null) {
            throw new IllegalArgumentException("Source or target port not found");
        }

        return connect(sourcePort, targetPort);
    }

    /**
     * Disconnect a connection by source and target port info.
     *
     * @param sourceNodeId Source node ID
     * @param sourcePortName Source port name
     * @param targetNodeId Target node ID
     * @param targetPortName Target port name
     */
    public void disconnect(String sourceNodeId, String sourcePortName,
                          String targetNodeId, String targetPortName) {
        // Find matching connection
        for (Connection conn : connections.values()) {
            if (conn.getSourcePort().getOwner().getId().equals(sourceNodeId) &&
                    conn.getSourcePort().getName().equals(sourcePortName) &&
                    conn.getTargetPort().getOwner().getId().equals(targetNodeId) &&
                    conn.getTargetPort().getName().equals(targetPortName)) {
                disconnect(conn.getId());
                return;
            }
        }
    }

    /**
     * Check if adding a connection would create a cycle.
     */
    private boolean wouldCreateCycle(ProcessingNode source, ProcessingNode target) {
        // DFS from target to see if we can reach source
        Set<ProcessingNode> visited = new HashSet<>();
        return canReach(target, source, visited);
    }

    private boolean canReach(ProcessingNode from, ProcessingNode to, Set<ProcessingNode> visited) {
        if (from == to) {
            return true;
        }
        if (visited.contains(from)) {
            return false;
        }
        visited.add(from);

        // Check all nodes that 'from' outputs to
        for (Port outputPort : from.getOutputPorts()) {
            for (Connection conn : connections.values()) {
                if (conn.getSourcePort() == outputPort) {
                    if (canReach(conn.getTargetNode(), to, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Rebuild the processing order using topological sort.
     * Also builds parallel levels for multi-core processing.
     */
    private void rebuildProcessingOrder() {
        processingOrder.clear();
        parallelLevels.clear();

        // Kahn's algorithm for topological sort with level tracking
        Map<ProcessingNode, Integer> inDegree = new HashMap<>();
        Map<ProcessingNode, List<ProcessingNode>> adjacency = new HashMap<>();

        // Initialize
        for (ProcessingNode node : nodes.values()) {
            inDegree.put(node, 0);
            adjacency.put(node, new ArrayList<>());
        }

        // Build adjacency and count incoming edges
        for (Connection conn : connections.values()) {
            ProcessingNode source = conn.getSourceNode();
            ProcessingNode target = conn.getTargetNode();
            adjacency.get(source).add(target);
            inDegree.put(target, inDegree.get(target) + 1);
        }

        // Find all nodes with no incoming edges (level 0)
        List<ProcessingNode> currentLevel = new ArrayList<>();
        for (ProcessingNode node : nodes.values()) {
            if (inDegree.get(node) == 0) {
                currentLevel.add(node);
            }
        }

        // Process level by level
        while (!currentLevel.isEmpty()) {
            // Add current level to parallel levels
            parallelLevels.add(new ArrayList<>(currentLevel));

            // Add all nodes from current level to processing order
            processingOrder.addAll(currentLevel);

            // Find next level
            List<ProcessingNode> nextLevel = new ArrayList<>();
            for (ProcessingNode node : currentLevel) {
                for (ProcessingNode neighbor : adjacency.get(node)) {
                    int newDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newDegree);
                    if (newDegree == 0) {
                        nextLevel.add(neighbor);
                    }
                }
            }

            currentLevel = nextLevel;
        }

        // Check for cycles (should not happen if we validate on connect)
        if (processingOrder.size() != nodes.size()) {
            throw new IllegalStateException("Graph contains a cycle!");
        }

        orderDirty = false;
    }

    /**
     * Prepare all nodes for processing.
     *
     * @param sampleRate    Sample rate in Hz
     * @param maxFrameCount Maximum frames per process call
     */
    public void prepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;
        this.maxFrameCount = maxFrameCount;

        for (ProcessingNode node : nodes.values()) {
            node.prepare(sampleRate, maxFrameCount);
        }
    }

    /**
     * Process audio through the graph.
     *
     * @param input      Input audio buffer
     * @param output     Output audio buffer
     * @param frameCount Number of frames to process
     */
    public void process(float[] input, float[] output, int frameCount) {
        // Rebuild processing order if needed
        if (orderDirty) {
            rebuildProcessingOrder();
        }

        // Set input data (legacy InputNode support)
        if (inputNode != null) {
            inputNode.setInputData(input, frameCount);
        }

        // Process level by level (nodes in same level can run in parallel)
        for (List<ProcessingNode> level : parallelLevels) {
            if (parallelProcessingEnabled && level.size() >= MIN_NODES_FOR_PARALLEL && threadPool != null) {
                // Parallel processing for this level
                processLevelParallel(level, frameCount);
            } else {
                // Sequential processing
                processLevelSequential(level, frameCount);
            }
        }

        // Get output data (legacy OutputNode support)
        if (outputNode != null) {
            outputNode.copyOutputData(output, frameCount);
        }

        // Calculate levels from AudioInput/AudioOutput effect nodes
        updateLevelsFromEffectNodes(frameCount);
    }

    /**
     * Update input/output levels from AudioInputEffect and AudioOutputEffect nodes.
     */
    private void updateLevelsFromEffectNodes(int frameCount) {
        boolean foundAudioInput = false;
        float[] tunerBuffer = null;

        for (ProcessingNode node : processingOrder) {
            if (node instanceof EffectNode effectNode) {
                String effectId = effectNode.getEffect().getMetadata().id();
                if ("audioinput".equals(effectId)) {
                    // Get level from output port of AudioInputEffect
                    float[] buffer = effectNode.getOutput().getBuffer();
                    if (buffer != null) {
                        inputLevelDb = calculateLevelDb(buffer, frameCount);
                        // Use AudioInput for tuner if no specific node is set
                        if (tunerSourceNodeId == null) {
                            tunerBuffer = buffer;
                        }
                        foundAudioInput = true;
                    }
                } else if ("audiooutput".equals(effectId)) {
                    // Get post-gain level from AudioOutputEffect
                    if (effectNode.getEffect() instanceof it.denzosoft.jfx2.effects.impl.AudioOutputEffect audioOut) {
                        outputLevelDb = audioOut.getOutputLevelDb();
                    }
                    // Notify output audio listener with the input buffer (signal from chain)
                    float[] buffer = effectNode.getInput().getBuffer();
                    if (buffer != null && outputAudioListener != null) {
                        outputAudioListener.onOutputAudio(buffer, frameCount);
                    }
                }

                // Check if this is the selected tuner source node
                if (tunerSourceNodeId != null && node.getId().equals(tunerSourceNodeId)) {
                    // Use input port of the selected node for tuner
                    Port inputPort = effectNode.getInput();
                    if (inputPort != null && inputPort.getBuffer() != null) {
                        tunerBuffer = inputPort.getBuffer();
                    }
                }
            }
        }

        // Notify tuner listener
        if (inputAudioListener != null && tunerBuffer != null) {
            inputAudioListener.onInputAudio(tunerBuffer, frameCount);
        }

        // Calculate and notify FFT listener
        if (inputFFTListener != null && tunerBuffer != null) {
            processInputFFT(tunerBuffer, frameCount);
        }
    }

    /**
     * Process input audio for FFT and notify listener when ready.
     */
    private void processInputFFT(float[] buffer, int length) {
        // Accumulate samples in FFT buffer
        for (int i = 0; i < length && i < buffer.length; i++) {
            fftInputBuffer[fftBufferPos++] = buffer[i];

            // When buffer is full, compute FFT
            if (fftBufferPos >= FFT_SIZE) {
                computeFFT();
                fftBufferPos = 0;
            }
        }
    }

    /**
     * Compute FFT and notify listener.
     */
    private void computeFFT() {
        // Apply Hann window
        float[] windowed = new float[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; i++) {
            float window = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
            windowed[i] = fftInputBuffer[i] * window;
        }

        // Compute FFT (in-place, real input)
        float[] real = windowed;
        float[] imag = new float[FFT_SIZE];

        // Cooley-Tukey FFT
        int n = FFT_SIZE;
        int bits = (int) (Math.log(n) / Math.log(2));

        // Bit-reversal permutation
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (j > i) {
                float temp = real[i];
                real[i] = real[j];
                real[j] = temp;
            }
        }

        // FFT butterfly operations
        for (int size = 2; size <= n; size *= 2) {
            int halfSize = size / 2;
            float angle = (float) (-2 * Math.PI / size);

            for (int i = 0; i < n; i += size) {
                for (int j = 0; j < halfSize; j++) {
                    float cos = (float) Math.cos(angle * j);
                    float sin = (float) Math.sin(angle * j);

                    int idx1 = i + j;
                    int idx2 = i + j + halfSize;

                    float tReal = real[idx2] * cos - imag[idx2] * sin;
                    float tImag = real[idx2] * sin + imag[idx2] * cos;

                    real[idx2] = real[idx1] - tReal;
                    imag[idx2] = imag[idx1] - tImag;
                    real[idx1] = real[idx1] + tReal;
                    imag[idx1] = imag[idx1] + tImag;
                }
            }
        }

        // Calculate magnitudes (only first half - positive frequencies)
        float maxMag = 0;
        int numBins = FFT_SIZE / 2;
        for (int i = 0; i < numBins; i++) {
            fftMagnitudes[i] = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            maxMag = Math.max(maxMag, fftMagnitudes[i]);
        }

        // Normalize to 0-1 range
        if (maxMag > 0.001f) {
            for (int i = 0; i < numBins; i++) {
                fftMagnitudes[i] /= maxMag;
            }
        }

        // Notify listener
        float binFrequency = (float) sampleRate / FFT_SIZE;
        inputFFTListener.onInputFFT(fftMagnitudes, numBins, sampleRate, binFrequency);
    }

    /**
     * Process a level of nodes sequentially.
     */
    private void processLevelSequential(List<ProcessingNode> level, int frameCount) {
        for (ProcessingNode node : level) {
            processNode(node, frameCount);
        }
    }

    /**
     * Process a level of nodes in parallel using thread pool.
     */
    private void processLevelParallel(List<ProcessingNode> level, int frameCount) {
        // Create tasks for each node
        List<Future<?>> futures = new ArrayList<>(level.size());

        for (ProcessingNode node : level) {
            futures.add(threadPool.submit(() -> processNode(node, frameCount)));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();  // Wait for completion
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                // Log error but continue processing
                System.err.println("Error processing node: " + e.getCause().getMessage());
            }
        }
    }

    /**
     * Process a single node (transfer inputs, then process).
     */
    private void processNode(ProcessingNode node, int frameCount) {
        // First, transfer data from incoming connections to input ports
        for (Port inputPort : node.getInputPorts()) {
            Connection conn = inputPort.getConnection();
            if (conn != null) {
                conn.transfer(frameCount);
            } else {
                // No connection - clear the buffer
                inputPort.clearBuffer();
            }
        }

        // Process the node
        node.process(frameCount);
    }

    /**
     * Calculate RMS level in dB from audio buffer.
     */
    private float calculateLevelDb(float[] buffer, int frameCount) {
        if (buffer == null || frameCount == 0) {
            return MIN_DB;
        }

        float sum = 0f;
        int samples = Math.min(frameCount, buffer.length);

        for (int i = 0; i < samples; i++) {
            sum += buffer[i] * buffer[i];
        }

        float rms = (float) Math.sqrt(sum / samples);

        // Convert to dB
        if (rms < 1e-10f) {
            return MIN_DB;
        }

        float db = (float) (20.0 * Math.log10(rms));
        return Math.max(MIN_DB, Math.min(0f, db));
    }

    /**
     * Get the current input level in dB.
     */
    public float getInputLevelDb() {
        return inputLevelDb;
    }

    /**
     * Get the current output level in dB.
     */
    public float getOutputLevelDb() {
        return outputLevelDb;
    }

    /**
     * Check for audio device errors in the graph.
     * @return Error message if any audio device failed to open, null otherwise
     */
    public String getAudioDeviceError() {
        for (ProcessingNode node : nodes.values()) {
            if (node instanceof EffectNode effectNode) {
                if (effectNode.getEffect() instanceof it.denzosoft.jfx2.effects.impl.AudioInputEffect audioIn) {
                    if (audioIn.hasError()) {
                        return "Audio Input: " + audioIn.getLastError();
                    }
                }
                if (effectNode.getEffect() instanceof it.denzosoft.jfx2.effects.impl.AudioOutputEffect audioOut) {
                    if (!audioOut.isDeviceOpen()) {
                        return "Audio Output: Device not available";
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the input audio listener to receive input samples.
     * Used for tuner, spectrum analyzer, etc.
     */
    public void setInputAudioListener(InputAudioListener listener) {
        this.inputAudioListener = listener;
    }

    /**
     * Set the output audio listener to receive processed output samples.
     * Used for signal monitor, spectrum analyzer, etc.
     */
    public void setOutputAudioListener(OutputAudioListener listener) {
        this.outputAudioListener = listener;
    }

    /**
     * Set the input FFT listener to receive FFT magnitude data.
     * Used for spectrum analyzers, visualizers, etc.
     */
    public void setInputFFTListener(InputFFTListener listener) {
        this.inputFFTListener = listener;
    }

    /**
     * Set the node to use as tuner source.
     * If null, uses AudioInput automatically.
     * If set to a node ID, uses that node's input port.
     */
    public void setTunerSourceNode(String nodeId) {
        this.tunerSourceNodeId = nodeId;
    }

    /**
     * Get the current tuner source node ID.
     */
    public String getTunerSourceNode() {
        return tunerSourceNodeId;
    }

    /**
     * Release all resources.
     */
    public void release() {
        for (ProcessingNode node : nodes.values()) {
            node.release();
        }
    }

    /**
     * Shutdown the thread pool. Call this when the graph is no longer needed.
     */
    public void shutdown() {
        release();
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Enable or disable parallel processing.
     */
    public void setParallelProcessingEnabled(boolean enabled) {
        this.parallelProcessingEnabled = enabled;
    }

    /**
     * Check if parallel processing is enabled.
     */
    public boolean isParallelProcessingEnabled() {
        return parallelProcessingEnabled;
    }

    /**
     * Get the number of threads in the thread pool.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Get the number of parallel levels in the graph.
     */
    public int getParallelLevelCount() {
        if (orderDirty) {
            rebuildProcessingOrder();
        }
        return parallelLevels.size();
    }

    /**
     * Get info about parallel processing structure.
     */
    public String getParallelInfo() {
        if (orderDirty) {
            rebuildProcessingOrder();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Threads: ").append(threadCount);
        sb.append(", Levels: ").append(parallelLevels.size());
        sb.append(", Parallel enabled: ").append(parallelProcessingEnabled);
        sb.append("\n");
        for (int i = 0; i < parallelLevels.size(); i++) {
            List<ProcessingNode> level = parallelLevels.get(i);
            sb.append("  Level ").append(i).append(": ");
            sb.append(level.size()).append(" nodes");
            if (level.size() >= MIN_NODES_FOR_PARALLEL) {
                sb.append(" (parallel)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Reset all nodes.
     */
    public void reset() {
        for (ProcessingNode node : nodes.values()) {
            node.reset();
        }
    }

    /**
     * Clear the graph (remove all nodes and connections except input/output).
     */
    public void clear() {
        // Disconnect all
        for (Connection conn : new ArrayList<>(connections.values())) {
            conn.disconnect();
        }
        connections.clear();

        // Remove all nodes except input/output
        List<String> nodesToRemove = new ArrayList<>();
        for (String nodeId : nodes.keySet()) {
            ProcessingNode node = nodes.get(nodeId);
            if (node != inputNode && node != outputNode) {
                nodesToRemove.add(nodeId);
            }
        }
        for (String nodeId : nodesToRemove) {
            nodes.remove(nodeId);
        }

        orderDirty = true;
    }

    /**
     * Validate the graph.
     *
     * @return true if the graph is valid
     */
    public boolean validate() {
        try {
            rebuildProcessingOrder();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Getters

    public InputNode getInputNode() {
        return inputNode;
    }

    public OutputNode getOutputNode() {
        return outputNode;
    }

    public Collection<ProcessingNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Get the processing order (topologically sorted nodes).
     * Ensures the order is computed if dirty.
     *
     * @return Unmodifiable list of nodes in processing order
     */
    public List<ProcessingNode> getProcessingOrder() {
        if (orderDirty) {
            rebuildProcessingOrder();
        }
        return Collections.unmodifiableList(processingOrder);
    }

    public Collection<Connection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /**
     * Get a connection by ID.
     *
     * @param connectionId The connection ID
     * @return The connection, or null if not found
     */
    public Connection getConnection(String connectionId) {
        return connections.get(connectionId);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getConnectionCount() {
        return connections.size();
    }
}
