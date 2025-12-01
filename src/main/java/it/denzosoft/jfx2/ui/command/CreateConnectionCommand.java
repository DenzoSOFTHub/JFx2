package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.graph.SignalGraph;

/**
 * Command for creating a connection between two ports.
 */
public class CreateConnectionCommand implements Command {

    private final SignalGraph graph;
    private final String sourceNodeId;
    private final String sourcePortName;
    private final String targetNodeId;
    private final String targetPortName;

    public CreateConnectionCommand(SignalGraph graph,
                                   String sourceNodeId, String sourcePortName,
                                   String targetNodeId, String targetPortName) {
        this.graph = graph;
        this.sourceNodeId = sourceNodeId;
        this.sourcePortName = sourcePortName;
        this.targetNodeId = targetNodeId;
        this.targetPortName = targetPortName;
    }

    @Override
    public void execute() {
        try {
            graph.connect(sourceNodeId, sourcePortName, targetNodeId, targetPortName);
        } catch (Exception e) {
            System.err.println("Could not create connection: " + e.getMessage());
        }
    }

    @Override
    public void undo() {
        graph.disconnect(sourceNodeId, sourcePortName, targetNodeId, targetPortName);
    }

    @Override
    public String getDescription() {
        return "Create connection";
    }
}
