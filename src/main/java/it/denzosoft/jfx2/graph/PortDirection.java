package it.denzosoft.jfx2.graph;

/**
 * Direction of a port (input or output).
 */
public enum PortDirection {

    /**
     * Input port - receives signal from another node's output.
     */
    INPUT,

    /**
     * Output port - sends signal to another node's input.
     */
    OUTPUT
}
