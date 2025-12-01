package it.denzosoft.jfx2.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for processing nodes.
 *
 * <p>Provides common functionality for port management and bypass handling.</p>
 */
public abstract class AbstractNode implements ProcessingNode {

    protected final String id;
    protected String name;  // Mutable for renaming
    protected final NodeType nodeType;

    protected final List<Port> inputPorts;
    protected final List<Port> outputPorts;

    protected int sampleRate;
    protected int maxFrameCount;
    protected volatile boolean bypassed;

    protected AbstractNode(String id, String name, NodeType nodeType) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.bypassed = false;
    }

    /**
     * Add an input port to this node.
     *
     * @param portId   Port identifier
     * @param portName Display name
     * @param type     Signal type
     * @return The created port
     */
    protected Port addInputPort(String portId, String portName, PortType type) {
        Port port = new Port(portId, portName, type, PortDirection.INPUT, this);
        inputPorts.add(port);
        return port;
    }

    /**
     * Add an output port to this node.
     *
     * @param portId   Port identifier
     * @param portName Display name
     * @param type     Signal type
     * @return The created port
     */
    protected Port addOutputPort(String portId, String portName, PortType type) {
        Port port = new Port(portId, portName, type, PortDirection.OUTPUT, this);
        outputPorts.add(port);
        return port;
    }

    @Override
    public void prepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;
        this.maxFrameCount = maxFrameCount;

        // Allocate buffers for all ports
        for (Port port : inputPorts) {
            port.allocateBuffer(maxFrameCount);
        }
        for (Port port : outputPorts) {
            port.allocateBuffer(maxFrameCount);
        }
    }

    @Override
    public void release() {
        // Default: nothing to release
    }

    @Override
    public void reset() {
        // Default: clear all port buffers
        for (Port port : inputPorts) {
            port.clearBuffer();
        }
        for (Port port : outputPorts) {
            port.clearBuffer();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public List<Port> getInputPorts() {
        return Collections.unmodifiableList(inputPorts);
    }

    @Override
    public List<Port> getOutputPorts() {
        return Collections.unmodifiableList(outputPorts);
    }

    @Override
    public Port getInputPort(String portId) {
        for (Port port : inputPorts) {
            if (port.getId().equals(portId)) {
                return port;
            }
        }
        return null;
    }

    @Override
    public Port getOutputPort(String portId) {
        for (Port port : outputPorts) {
            if (port.getId().equals(portId)) {
                return port;
            }
        }
        return null;
    }

    @Override
    public boolean isBypassed() {
        return bypassed;
    }

    @Override
    public void setBypassed(boolean bypassed) {
        this.bypassed = bypassed;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s]", getClass().getSimpleName(), id, name);
    }
}
