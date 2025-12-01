package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Context menu for blocks on the canvas.
 * Provides actions like Delete, Bypass, Duplicate.
 */
public class BlockContextMenu extends JPopupMenu {

    private final SignalFlowPanel canvas;
    private ProcessingNode targetNode;

    // Menu items
    private final JMenuItem deleteItem;
    private final JMenuItem bypassItem;
    private final JMenuItem duplicateItem;
    private final JMenuItem disconnectItem;
    private final JMenuItem renameItem;

    public BlockContextMenu(SignalFlowPanel canvas) {
        this.canvas = canvas;

        // Apply dark theme
        setBackground(DarkTheme.BG_MEDIUM);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT));

        // Delete
        deleteItem = createMenuItem("Delete", "Delete", e -> deleteNode());
        deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));

        // Bypass
        bypassItem = createMenuItem("Bypass", "B", e -> toggleBypass());

        // Duplicate
        duplicateItem = createMenuItem("Duplicate", "Ctrl+D", e -> duplicateNode());

        // Disconnect all
        disconnectItem = createMenuItem("Disconnect All", null, e -> disconnectAll());

        // Rename
        renameItem = createMenuItem("Rename...", "F2", e -> renameNode());

        // Add items
        add(bypassItem);
        add(duplicateItem);
        addSeparator();
        add(disconnectItem);
        addSeparator();
        add(renameItem);
        add(deleteItem);
    }

    /**
     * Create a styled menu item.
     */
    private JMenuItem createMenuItem(String text, String shortcut, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(DarkTheme.BG_MEDIUM);
        item.setForeground(DarkTheme.TEXT_PRIMARY);
        item.setFont(DarkTheme.FONT_REGULAR);
        item.addActionListener(action);

        if (shortcut != null && !shortcut.isEmpty()) {
            item.setToolTipText(shortcut);
        }

        return item;
    }

    /**
     * Show the context menu for a node.
     */
    public void showForNode(ProcessingNode node, Component invoker, int x, int y) {
        this.targetNode = node;

        // Update menu items based on node type
        updateMenuItems();

        show(invoker, x, y);
    }

    /**
     * Update menu items based on target node.
     */
    private void updateMenuItems() {
        if (targetNode == null) return;

        NodeType type = targetNode.getNodeType();

        // All nodes can be deleted
        deleteItem.setEnabled(true);

        // Duplicate for effect nodes only
        duplicateItem.setEnabled(type == NodeType.EFFECT);

        // Update bypass text
        boolean bypassed = targetNode.isBypassed();
        bypassItem.setText(bypassed ? "Enable" : "Bypass");

        // Bypass only for effect nodes
        bypassItem.setEnabled(type == NodeType.EFFECT);

        // Rename for effect nodes only
        renameItem.setEnabled(type == NodeType.EFFECT);
    }

    // ==================== ACTIONS ====================

    private void deleteNode() {
        if (targetNode == null) return;

        SignalGraph graph = canvas.getSignalGraph();
        if (graph != null) {
            // Remove node (connections are removed automatically by SignalGraph)
            graph.removeNode(targetNode.getId());

            // Update selection
            canvas.getController().clearSelection();

            canvas.repaint();
        }
    }

    private void toggleBypass() {
        if (targetNode == null) return;

        targetNode.setBypassed(!targetNode.isBypassed());
        canvas.repaint();
    }

    private void duplicateNode() {
        if (targetNode == null || !(targetNode instanceof EffectNode effectNode)) return;

        SignalGraph graph = canvas.getSignalGraph();
        if (graph == null) return;

        // Create a new effect instance
        var effect = effectNode.getEffect();
        if (effect == null) return;

        var metadata = effect.getMetadata();
        var newEffect = it.denzosoft.jfx2.effects.EffectFactory.getInstance().create(metadata.id());
        if (newEffect == null) return;

        // Copy parameter values
        for (var param : effect.getParameters()) {
            var newParam = newEffect.getParameter(param.getId());
            if (newParam != null) {
                newParam.setValue(param.getValue());
            }
        }

        // Create new node
        String newId = generateUniqueId(targetNode.getId());
        EffectNode newNode = new EffectNode(newId, newEffect);

        graph.addNode(newNode);

        // Position slightly offset from original
        Point pos = canvas.getController().getNodePosition(targetNode.getId());
        Point newPos = new Point(pos.x + 40, pos.y + 40);
        canvas.getController().setNodePosition(newId, newPos);
        canvas.getController().selectNode(newId);

        canvas.repaint();
    }

    private void disconnectAll() {
        if (targetNode == null) return;

        SignalGraph graph = canvas.getSignalGraph();
        if (graph == null) return;

        java.util.List<Connection> toRemove = new java.util.ArrayList<>();
        for (Connection conn : graph.getConnections()) {
            if (conn.getSourcePort().getOwner() == targetNode ||
                    conn.getTargetPort().getOwner() == targetNode) {
                toRemove.add(conn);
            }
        }

        for (Connection conn : toRemove) {
            graph.disconnect(conn.getId());
        }

        canvas.repaint();
    }

    private void renameNode() {
        if (targetNode == null || !(targetNode instanceof EffectNode)) return;

        String currentName = targetNode.getName();
        String newName = DarkTheme.showInputDialog(canvas,
                "Enter new name:",
                "Rename Block",
                currentName);

        if (newName != null && !newName.trim().isEmpty() && !newName.equals(currentName)) {
            // Note: EffectNode doesn't have setName - name comes from effect metadata
            // This is a limitation - nodes can't be renamed
            canvas.repaint();
        }
    }

    /**
     * Generate a unique node ID based on existing ID.
     */
    private String generateUniqueId(String baseId) {
        SignalGraph graph = canvas.getSignalGraph();
        String id = baseId + "_copy";
        int counter = 1;

        while (graph.getNode(id) != null) {
            id = baseId + "_copy" + counter++;
        }

        return id;
    }
}
