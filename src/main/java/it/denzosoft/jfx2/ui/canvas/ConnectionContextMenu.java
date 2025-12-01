package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.SignalGraph;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Context menu for connections on the canvas.
 */
public class ConnectionContextMenu extends JPopupMenu {

    private final SignalFlowPanel canvas;
    private Connection targetConnection;

    public ConnectionContextMenu(SignalFlowPanel canvas) {
        this.canvas = canvas;

        setBackground(DarkTheme.BG_MEDIUM);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT));

        // Delete connection
        JMenuItem deleteItem = createMenuItem("Delete Connection", e -> deleteConnection());
        deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));

        add(deleteItem);
    }

    private JMenuItem createMenuItem(String text, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(DarkTheme.BG_MEDIUM);
        item.setForeground(DarkTheme.TEXT_PRIMARY);
        item.setFont(DarkTheme.FONT_REGULAR);
        item.addActionListener(action);
        return item;
    }

    /**
     * Show the context menu for a connection.
     */
    public void showForConnection(Connection connection, Component invoker, int x, int y) {
        this.targetConnection = connection;
        show(invoker, x, y);
    }

    private void deleteConnection() {
        if (targetConnection == null) return;

        SignalGraph graph = canvas.getSignalGraph();
        if (graph != null) {
            graph.disconnect(targetConnection.getId());
            canvas.getController().clearConnectionSelection();
            canvas.repaint();
        }
    }
}
