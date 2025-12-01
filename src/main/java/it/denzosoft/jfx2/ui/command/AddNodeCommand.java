package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.graph.ProcessingNode;
import it.denzosoft.jfx2.graph.SignalGraph;

import java.awt.*;

/**
 * Command for adding a node to the signal graph.
 */
public class AddNodeCommand implements Command {

    private final SignalGraph graph;
    private final ProcessingNode node;
    private final Point position;
    private final Runnable positionSetter;

    public AddNodeCommand(SignalGraph graph, ProcessingNode node, Point position, Runnable positionSetter) {
        this.graph = graph;
        this.node = node;
        this.position = position;
        this.positionSetter = positionSetter;
    }

    @Override
    public void execute() {
        graph.addNode(node);
        if (positionSetter != null) {
            positionSetter.run();
        }
    }

    @Override
    public void undo() {
        graph.removeNode(node.getId());
    }

    @Override
    public String getDescription() {
        return "Add " + node.getId();
    }
}
