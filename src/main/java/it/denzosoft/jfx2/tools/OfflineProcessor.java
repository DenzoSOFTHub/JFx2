package it.denzosoft.jfx2.tools;

import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.graph.PortDirection;
import it.denzosoft.jfx2.preset.*;

import java.util.*;

/**
 * Offline audio processor for rendering a rig to a buffer.
 *
 * <p>Creates a copy of a rig, replaces all audio I/O nodes with buffer-based
 * nodes, and processes audio offline (not in real-time).</p>
 */
public class OfflineProcessor {

    private final int sampleRate;
    private final int blockSize;

    public OfflineProcessor(int sampleRate, int blockSize) {
        this.sampleRate = sampleRate;
        this.blockSize = blockSize;
    }

    public OfflineProcessor() {
        this(44100, 256);
    }

    /**
     * Process audio through a rig offline.
     *
     * @param rig    The rig configuration to use
     * @param input  Input samples
     * @return Output samples (same length as input)
     */
    public float[] process(Rig rig, float[] input) {
        // Create a modified rig with buffer I/O
        SignalGraph graph = createOfflineGraph(rig);

        if (graph == null) {
            System.err.println("Failed to create offline graph");
            return input.clone(); // Return dry signal
        }

        // Prepare output buffer
        float[] output = new float[input.length];

        // Find the buffer nodes
        BufferInputNode bufferInput = null;
        BufferOutputNode bufferOutput = null;

        for (ProcessingNode node : graph.getNodes()) {
            if (node instanceof BufferInputNode bin) {
                bufferInput = bin;
            } else if (node instanceof BufferOutputNode bout) {
                bufferOutput = bout;
            }
        }

        if (bufferInput == null || bufferOutput == null) {
            System.err.println("Buffer nodes not found in offline graph");
            return input.clone();
        }

        // Set the input buffer
        bufferInput.setBuffer(input);
        bufferOutput.setBuffer(output);

        // Process in blocks
        int totalSamples = input.length;
        int processed = 0;

        System.out.println("Processing " + totalSamples + " samples in blocks of " + blockSize);

        while (processed < totalSamples) {
            int framesToProcess = Math.min(blockSize, totalSamples - processed);

            bufferInput.setPosition(processed);
            bufferOutput.setPosition(processed);

            // Process this block through the graph
            graph.process(null, null, framesToProcess);

            processed += framesToProcess;

            // Progress indicator
            if (processed % (sampleRate) == 0) {
                System.out.println("  Processed " + (processed / sampleRate) + " seconds...");
            }
        }

        System.out.println("Offline processing complete: " + totalSamples + " samples");

        return output;
    }

    /**
     * Create an offline version of the graph with buffer I/O.
     */
    private SignalGraph createOfflineGraph(Rig rig) {
        SignalGraph graph = new SignalGraph();
        EffectFactory effectFactory = EffectFactory.getInstance();

        // Track nodes and find I/O connections
        Map<String, ProcessingNode> nodeMap = new HashMap<>();
        List<String> inputNodeIds = new ArrayList<>();
        List<String> outputNodeIds = new ArrayList<>();
        String firstEffectAfterInput = null;
        String lastEffectBeforeOutput = null;

        // First pass: identify I/O nodes and their connections
        Set<String> audioInputIds = new HashSet<>();
        Set<String> audioOutputIds = new HashSet<>();

        for (Rig.NodeDefinition nodeDef : rig.getNodes()) {
            if ("audioinput".equals(nodeDef.type())) {
                audioInputIds.add(nodeDef.id());
            } else if ("audiooutput".equals(nodeDef.type())) {
                audioOutputIds.add(nodeDef.id());
            }
        }

        // Find what's connected to audio inputs/outputs
        for (Rig.ConnectionDefinition conn : rig.getConnections()) {
            if (audioInputIds.contains(conn.sourceNodeId())) {
                // This node receives from audio input
                if (firstEffectAfterInput == null) {
                    firstEffectAfterInput = conn.targetNodeId();
                }
            }
            if (audioOutputIds.contains(conn.targetNodeId())) {
                // This node sends to audio output
                lastEffectBeforeOutput = conn.sourceNodeId();
            }
        }

        System.out.println("First effect after input: " + firstEffectAfterInput);
        System.out.println("Last effect before output: " + lastEffectBeforeOutput);

        // Create buffer input node
        BufferInputNode bufferInput = new BufferInputNode("_buffer_input");
        graph.addNode(bufferInput);
        nodeMap.put("_buffer_input", bufferInput);

        // Create buffer output node
        BufferOutputNode bufferOutput = new BufferOutputNode("_buffer_output");
        graph.addNode(bufferOutput);
        nodeMap.put("_buffer_output", bufferOutput);

        // Create all non-I/O nodes
        for (Rig.NodeDefinition nodeDef : rig.getNodes()) {
            // Skip audio I/O nodes
            if ("audioinput".equals(nodeDef.type()) || "audiooutput".equals(nodeDef.type())) {
                continue;
            }

            ProcessingNode node = createNode(nodeDef, effectFactory);
            if (node != null) {
                graph.addNode(node);
                nodeMap.put(nodeDef.id(), node);
            }
        }

        // Create connections, remapping I/O
        for (Rig.ConnectionDefinition conn : rig.getConnections()) {
            String sourceId = conn.sourceNodeId();
            String targetId = conn.targetNodeId();
            String sourcePort = conn.sourcePortId();
            String targetPort = conn.targetPortId();

            // Remap audio input to buffer input
            if (audioInputIds.contains(sourceId)) {
                sourceId = "_buffer_input";
                sourcePort = "out";
            }

            // Remap audio output to buffer output
            if (audioOutputIds.contains(targetId)) {
                targetId = "_buffer_output";
                targetPort = "in";
            }

            // Skip connections between removed nodes
            if (audioInputIds.contains(sourceId) || audioOutputIds.contains(targetId)) {
                continue;
            }

            ProcessingNode sourceNode = nodeMap.get(sourceId);
            ProcessingNode targetNode = nodeMap.get(targetId);

            if (sourceNode == null || targetNode == null) {
                continue;
            }

            // Find ports
            Port srcPort = findPort(sourceNode.getOutputPorts(), sourcePort);
            Port tgtPort = findPort(targetNode.getInputPorts(), targetPort);

            if (srcPort == null) srcPort = getDefaultOutputPort(sourceNode);
            if (tgtPort == null) tgtPort = getDefaultInputPort(targetNode);

            if (srcPort != null && tgtPort != null) {
                try {
                    graph.connect(srcPort, tgtPort);
                } catch (Exception e) {
                    System.err.println("Failed to connect: " + e.getMessage());
                }
            }
        }

        return graph;
    }

    private ProcessingNode createNode(Rig.NodeDefinition nodeDef, EffectFactory effectFactory) {
        String type = nodeDef.type();

        if ("splitter".equals(type)) {
            int numOutputs = getConfigInt(nodeDef.config(), "numOutputs", 2);
            return new SplitterNode(nodeDef.id(), numOutputs);
        } else if ("mixer".equals(type)) {
            int numInputs = getConfigInt(nodeDef.config(), "numInputs", 2);
            MixerNode mixer = new MixerNode(nodeDef.id(), numInputs);

            @SuppressWarnings("unchecked")
            List<Object> levels = (List<Object>) nodeDef.config().get("levels");
            @SuppressWarnings("unchecked")
            List<Object> pans = (List<Object>) nodeDef.config().get("pans");

            if (levels != null) {
                for (int i = 0; i < Math.min(levels.size(), numInputs); i++) {
                    mixer.setLevel(i, ((Number) levels.get(i)).floatValue());
                }
            }
            if (pans != null) {
                for (int i = 0; i < Math.min(pans.size(), numInputs); i++) {
                    mixer.setPan(i, ((Number) pans.get(i)).floatValue());
                }
            }

            return mixer;
        } else {
            AudioEffect effect = effectFactory.create(type);
            if (effect == null) {
                System.err.println("Unknown effect type: " + type);
                return null;
            }

            // Apply parameters
            for (Map.Entry<String, Object> entry : nodeDef.parameters().entrySet()) {
                Parameter param = effect.getParameter(entry.getKey());
                if (param != null && entry.getValue() instanceof Number n) {
                    param.setValue(n.floatValue());
                }
            }

            EffectNode effectNode = new EffectNode(nodeDef.id(), effect);
            effectNode.setBypassed(nodeDef.bypassed());
            return effectNode;
        }
    }

    private Port findPort(List<Port> ports, String portId) {
        for (Port port : ports) {
            if (port.getId().equals(portId)) {
                return port;
            }
        }
        return null;
    }

    private Port getDefaultOutputPort(ProcessingNode node) {
        List<Port> outputs = node.getOutputPorts();
        return outputs.isEmpty() ? null : outputs.get(0);
    }

    private Port getDefaultInputPort(ProcessingNode node) {
        List<Port> inputs = node.getInputPorts();
        return inputs.isEmpty() ? null : inputs.get(0);
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number n) return n.intValue();
        return defaultValue;
    }

    // ==================== BUFFER NODES ====================

    /**
     * A node that reads from a buffer (replaces AudioInput).
     */
    public static class BufferInputNode implements ProcessingNode {
        private final String id;
        private String name = "Buffer Input";
        private final Port outputPort;
        private float[] buffer;
        private int position;

        public BufferInputNode(String id) {
            this.id = id;
            this.outputPort = new Port("out", "Output", PortType.AUDIO_MONO, PortDirection.OUTPUT, this);
        }

        public void setBuffer(float[] buffer) {
            this.buffer = buffer;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getName() { return name; }

        @Override
        public void setName(String name) { this.name = name; }

        @Override
        public NodeType getNodeType() { return NodeType.INPUT; }

        @Override
        public List<Port> getInputPorts() { return List.of(); }

        @Override
        public List<Port> getOutputPorts() { return List.of(outputPort); }

        @Override
        public Port getInputPort(String portId) { return null; }

        @Override
        public Port getOutputPort(String portId) {
            return "out".equals(portId) ? outputPort : null;
        }

        @Override
        public void prepare(int sampleRate, int maxFrameCount) {
            outputPort.allocateBuffer(maxFrameCount);
        }

        @Override
        public boolean isBypassed() { return false; }

        @Override
        public void setBypassed(boolean bypassed) { }

        @Override
        public void process(int frameCount) {
            float[] outBuffer = outputPort.getBuffer();
            if (buffer != null && outBuffer != null) {
                for (int i = 0; i < frameCount; i++) {
                    int srcIdx = position + i;
                    if (srcIdx < buffer.length) {
                        outBuffer[i] = buffer[srcIdx];
                    } else {
                        outBuffer[i] = 0;
                    }
                }
            }
        }

        @Override
        public void reset() { position = 0; }

        @Override
        public void release() { }
    }

    /**
     * A node that writes to a buffer (replaces AudioOutput).
     */
    public static class BufferOutputNode implements ProcessingNode {
        private final String id;
        private String name = "Buffer Output";
        private final Port inputPort;
        private float[] buffer;
        private int position;

        public BufferOutputNode(String id) {
            this.id = id;
            this.inputPort = new Port("in", "Input", PortType.AUDIO_MONO, PortDirection.INPUT, this);
        }

        public void setBuffer(float[] buffer) {
            this.buffer = buffer;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getName() { return name; }

        @Override
        public void setName(String name) { this.name = name; }

        @Override
        public NodeType getNodeType() { return NodeType.OUTPUT; }

        @Override
        public List<Port> getInputPorts() { return List.of(inputPort); }

        @Override
        public List<Port> getOutputPorts() { return List.of(); }

        @Override
        public Port getInputPort(String portId) {
            return "in".equals(portId) ? inputPort : null;
        }

        @Override
        public Port getOutputPort(String portId) { return null; }

        @Override
        public void prepare(int sampleRate, int maxFrameCount) {
            inputPort.allocateBuffer(maxFrameCount);
        }

        @Override
        public boolean isBypassed() { return false; }

        @Override
        public void setBypassed(boolean bypassed) { }

        @Override
        public void process(int frameCount) {
            float[] inBuffer = inputPort.getBuffer();
            if (buffer != null && inBuffer != null) {
                for (int i = 0; i < frameCount; i++) {
                    int dstIdx = position + i;
                    if (dstIdx < buffer.length) {
                        buffer[dstIdx] = inBuffer[i];
                    }
                }
            }
        }

        @Override
        public void reset() { position = 0; }

        @Override
        public void release() { }
    }
}
