package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.EffectFactory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.palette.EffectPalettePanel;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.geom.Point2D;

/**
 * Handles drag & drop of effects onto the canvas.
 */
public class CanvasDropHandler implements DropTargetListener {

    private final SignalFlowPanel canvas;
    private Point dropPreviewLocation;
    private boolean validDrop = false;

    public CanvasDropHandler(SignalFlowPanel canvas) {
        this.canvas = canvas;
        new DropTarget(canvas, DnDConstants.ACTION_COPY, this, true);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (isEffectDrag(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            validDrop = true;
        } else {
            dtde.rejectDrag();
            validDrop = false;
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (validDrop) {
            // Update preview location
            Point screenPoint = dtde.getLocation();
            Point2D worldPoint = canvas.screenToWorld(screenPoint);

            // Snap to grid
            int gridSize = GridRenderer.GRID_SIZE;
            int x = (int) (Math.round(worldPoint.getX() / gridSize) * gridSize);
            int y = (int) (Math.round(worldPoint.getY() / gridSize) * gridSize);

            dropPreviewLocation = new Point(x, y);
            canvas.setDropPreview(dropPreviewLocation);
            canvas.repaint();
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        if (isEffectDrag(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        dropPreviewLocation = null;
        canvas.setDropPreview(null);
        canvas.repaint();
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            Transferable transferable = dtde.getTransferable();

            if (transferable.isDataFlavorSupported(EffectPalettePanel.EFFECT_FLAVOR)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                EffectMetadata metadata = (EffectMetadata) transferable.getTransferData(
                        EffectPalettePanel.EFFECT_FLAVOR);

                if (metadata != null && dropPreviewLocation != null) {
                    // Create and add the effect
                    addEffectToCanvas(metadata, dropPreviewLocation);
                    dtde.dropComplete(true);
                } else {
                    dtde.dropComplete(false);
                }
            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                // Fallback: use effect ID
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                String effectId = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                EffectMetadata metadata = EffectFactory.getInstance().getMetadata(effectId);

                if (metadata != null && dropPreviewLocation != null) {
                    addEffectToCanvas(metadata, dropPreviewLocation);
                    dtde.dropComplete(true);
                } else {
                    dtde.dropComplete(false);
                }
            } else {
                dtde.rejectDrop();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dtde.rejectDrop();
        } finally {
            dropPreviewLocation = null;
            canvas.setDropPreview(null);
            canvas.repaint();
        }
    }

    /**
     * Check if the drag contains an effect.
     */
    private boolean isEffectDrag(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(EffectPalettePanel.EFFECT_FLAVOR) ||
                dtde.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    /**
     * Add an effect to the canvas at the specified position.
     */
    private void addEffectToCanvas(EffectMetadata metadata, Point position) {
        SignalGraph graph = canvas.getSignalGraph();
        if (graph == null) {
            System.err.println("Cannot add effect: no signal graph");
            return;
        }

        ProcessingNode node;
        String nodeId = generateNodeId(metadata.name());

        // Handle special node types that need multiple ports
        switch (metadata.id()) {
            case "mixer" -> {
                // Create MixerNode with 4 inputs
                node = new MixerNode(nodeId, 4);
            }
            case "splitter" -> {
                // Create SplitterNode with 4 outputs
                node = new SplitterNode(nodeId, 4);
            }
            default -> {
                // Create standard effect node
                AudioEffect effect = EffectFactory.getInstance().create(metadata.id());
                if (effect == null) {
                    System.err.println("Cannot create effect: " + metadata.id());
                    return;
                }
                node = new EffectNode(nodeId, effect);
            }
        }

        graph.addNode(node);

        // Set position in controller
        canvas.getController().setNodePosition(node.getId(), position);
        canvas.getController().selectNode(node.getId());
    }

    /**
     * Generate a unique node ID.
     */
    private String generateNodeId(String baseName) {
        String baseId = baseName.toLowerCase().replaceAll("\\s+", "_");
        SignalGraph graph = canvas.getSignalGraph();

        int counter = 1;
        String id = baseId;
        while (graph.getNode(id) != null) {
            id = baseId + "_" + counter++;
        }
        return id;
    }
}
