package it.denzosoft.jfx2.graph;

/**
 * A connection between two ports in the signal graph.
 *
 * <p>Connections always go from an output port to an input port.
 * They handle signal type conversion (mono<->stereo) automatically.</p>
 */
public class Connection {

    private final String id;
    private final Port sourcePort;  // Output port
    private final Port targetPort;  // Input port
    private float gain;             // Connection gain (default 1.0)

    /**
     * Create a new connection.
     *
     * @param id         Unique identifier
     * @param sourcePort Output port (source of signal)
     * @param targetPort Input port (destination of signal)
     */
    public Connection(String id, Port sourcePort, Port targetPort) {
        if (sourcePort.getDirection() != PortDirection.OUTPUT) {
            throw new IllegalArgumentException("Source port must be an output port");
        }
        if (targetPort.getDirection() != PortDirection.INPUT) {
            throw new IllegalArgumentException("Target port must be an input port");
        }
        if (!sourcePort.canConnectTo(targetPort)) {
            throw new IllegalArgumentException("Cannot connect these ports");
        }

        this.id = id;
        this.sourcePort = sourcePort;
        this.targetPort = targetPort;
        this.gain = 1.0f;

        // Register connection with ports
        targetPort.setConnection(this);
    }

    /**
     * Transfer signal from source to target, handling type conversion.
     *
     * @param frameCount Number of frames to transfer
     */
    public void transfer(int frameCount) {
        float[] source = sourcePort.getBuffer();
        float[] target = targetPort.getBuffer();

        if (source == null || target == null) {
            return;
        }

        PortType sourceType = sourcePort.getType();
        PortType targetType = targetPort.getType();

        if (sourceType == targetType) {
            // Same type - direct copy with gain
            int sampleCount = frameCount * sourceType.getChannelCount();
            for (int i = 0; i < sampleCount && i < source.length && i < target.length; i++) {
                target[i] = source[i] * gain;
            }
        } else if (sourceType == PortType.AUDIO_MONO && targetType == PortType.AUDIO_STEREO) {
            // Mono to stereo - duplicate to both channels
            for (int i = 0; i < frameCount && i < source.length; i++) {
                float sample = source[i] * gain;
                int targetIdx = i * 2;
                if (targetIdx + 1 < target.length) {
                    target[targetIdx] = sample;      // Left
                    target[targetIdx + 1] = sample;  // Right
                }
            }
        } else if (sourceType == PortType.AUDIO_STEREO && targetType == PortType.AUDIO_MONO) {
            // Stereo to mono - average both channels
            for (int i = 0; i < frameCount; i++) {
                int sourceIdx = i * 2;
                if (sourceIdx + 1 < source.length && i < target.length) {
                    float left = source[sourceIdx];
                    float right = source[sourceIdx + 1];
                    target[i] = (left + right) * 0.5f * gain;
                }
            }
        }
    }

    /**
     * Disconnect this connection from its ports.
     */
    public void disconnect() {
        targetPort.clearConnection();
    }

    /**
     * Get the source node of this connection.
     */
    public ProcessingNode getSourceNode() {
        return sourcePort.getOwner();
    }

    /**
     * Get the target node of this connection.
     */
    public ProcessingNode getTargetNode() {
        return targetPort.getOwner();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public Port getSourcePort() {
        return sourcePort;
    }

    public Port getTargetPort() {
        return targetPort;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    @Override
    public String toString() {
        return String.format("Connection[%s: %s -> %s]", id, sourcePort, targetPort);
    }
}
