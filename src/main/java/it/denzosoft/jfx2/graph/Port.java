package it.denzosoft.jfx2.graph;

/**
 * A port on a processing node for connecting signals.
 *
 * <p>Input ports can have at most one incoming connection.
 * Output ports can have multiple outgoing connections.</p>
 */
public class Port {

    private final String id;
    private final String name;
    private final PortType type;
    private final PortDirection direction;
    private final ProcessingNode owner;

    // Connection (for input ports, only one; for output ports, managed by Connection class)
    private Connection connection;

    // Buffer for this port's signal data
    private float[] buffer;

    /**
     * Create a new port.
     *
     * @param id        Unique identifier within the node
     * @param name      Display name
     * @param type      Signal type (mono/stereo)
     * @param direction Input or output
     * @param owner     The node that owns this port
     */
    public Port(String id, String name, PortType type, PortDirection direction, ProcessingNode owner) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.direction = direction;
        this.owner = owner;
    }

    /**
     * Check if this port can connect to another port.
     *
     * @param other The target port
     * @return true if connection is valid
     */
    public boolean canConnectTo(Port other) {
        // Cannot connect to self
        if (this == other) {
            return false;
        }

        // Cannot connect ports on the same node
        if (this.owner == other.owner) {
            return false;
        }

        // Must be opposite directions (output -> input)
        if (this.direction == other.direction) {
            return false;
        }

        // Check type compatibility
        if (!this.type.isCompatibleWith(other.type)) {
            return false;
        }

        // Input ports can only have one connection
        Port inputPort = (this.direction == PortDirection.INPUT) ? this : other;
        if (inputPort.isConnected()) {
            return false;
        }

        return true;
    }

    /**
     * Check if this port has a connection.
     */
    public boolean isConnected() {
        return connection != null;
    }

    /**
     * Get the connection on this port (for input ports).
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Set the connection on this port.
     * Should only be called by Connection class.
     */
    void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Clear the connection on this port.
     * Should only be called by Connection class.
     */
    void clearConnection() {
        this.connection = null;
    }

    /**
     * Allocate buffer for this port.
     *
     * @param frameCount Number of frames
     */
    public void allocateBuffer(int frameCount) {
        int sampleCount = frameCount * type.getChannelCount();
        if (buffer == null || buffer.length != sampleCount) {
            buffer = new float[sampleCount];
        }
    }

    /**
     * Get the buffer for this port.
     */
    public float[] getBuffer() {
        return buffer;
    }

    /**
     * Clear the buffer (fill with zeros).
     */
    public void clearBuffer() {
        if (buffer != null) {
            java.util.Arrays.fill(buffer, 0.0f);
        }
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PortType getType() {
        return type;
    }

    public PortDirection getDirection() {
        return direction;
    }

    public ProcessingNode getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return String.format("Port[%s.%s, %s %s]", owner.getId(), id, direction, type);
    }
}
