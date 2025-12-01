package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.ui.canvas.CanvasController;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Command for moving one or more blocks.
 * Supports coalescing for smooth drag operations.
 */
public class MoveBlockCommand implements Command {

    private final CanvasController controller;
    private final Map<String, Point> oldPositions;
    private final Map<String, Point> newPositions;

    /**
     * Create a move command for a single block.
     */
    public MoveBlockCommand(CanvasController controller, String nodeId, Point oldPos, Point newPos) {
        this.controller = controller;
        this.oldPositions = new HashMap<>();
        this.newPositions = new HashMap<>();
        this.oldPositions.put(nodeId, new Point(oldPos));
        this.newPositions.put(nodeId, new Point(newPos));
    }

    /**
     * Create a move command for multiple blocks.
     */
    public MoveBlockCommand(CanvasController controller, Map<String, Point> oldPositions, Map<String, Point> newPositions) {
        this.controller = controller;
        this.oldPositions = new HashMap<>();
        this.newPositions = new HashMap<>();

        for (Map.Entry<String, Point> entry : oldPositions.entrySet()) {
            this.oldPositions.put(entry.getKey(), new Point(entry.getValue()));
        }
        for (Map.Entry<String, Point> entry : newPositions.entrySet()) {
            this.newPositions.put(entry.getKey(), new Point(entry.getValue()));
        }
    }

    @Override
    public void execute() {
        for (Map.Entry<String, Point> entry : newPositions.entrySet()) {
            controller.setNodePosition(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<String, Point> entry : oldPositions.entrySet()) {
            controller.setNodePosition(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getDescription() {
        if (oldPositions.size() == 1) {
            return "Move " + oldPositions.keySet().iterator().next();
        }
        return "Move " + oldPositions.size() + " blocks";
    }

    @Override
    public boolean canMergeWith(Command other) {
        if (other instanceof MoveBlockCommand otherMove) {
            // Can merge if same blocks are being moved
            return otherMove.oldPositions.keySet().equals(this.newPositions.keySet());
        }
        return false;
    }

    @Override
    public Command mergeWith(Command other) {
        if (other instanceof MoveBlockCommand otherMove) {
            if (otherMove.oldPositions.keySet().equals(this.newPositions.keySet())) {
                // Keep our old positions, use their new positions
                return new MoveBlockCommand(controller, this.oldPositions, otherMove.newPositions);
            }
        }
        return null;
    }

    /**
     * Get the node IDs affected by this command.
     */
    public Set<String> getAffectedNodes() {
        return oldPositions.keySet();
    }
}
