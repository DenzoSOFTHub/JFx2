package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.SignalGraph;

/**
 * Command for deleting a connection.
 */
public class DeleteConnectionCommand implements Command {

    private final SignalGraph graph;
    private final String sourceNodeId;
    private final String sourcePortName;
    private final String targetNodeId;
    private final String targetPortName;

    public DeleteConnectionCommand(SignalGraph graph, Connection connection) {
        this.graph = graph;
        this.sourceNodeId = connection.getSourcePort().getOwner().getId();
        this.sourcePortName = connection.getSourcePort().getName();
        this.targetNodeId = connection.getTargetPort().getOwner().getId();
        this.targetPortName = connection.getTargetPort().getName();
    }

    public DeleteConnectionCommand(SignalGraph graph,
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
        graph.disconnect(sourceNodeId, sourcePortName, targetNodeId, targetPortName);
    }

    @Override
    public void undo() {
        try {
            graph.connect(sourceNodeId, sourcePortName, targetNodeId, targetPortName);
        } catch (Exception e) {
            System.err.println("Could not restore connection: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Delete connection";
    }
}
