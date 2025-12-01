package it.denzosoft.jfx2.ui.palette;

import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectFactory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.util.List;
import java.util.*;

/**
 * Effect palette panel with categorized tree view and search.
 * Supports drag & drop to canvas.
 */
public class EffectPalettePanel extends JPanel {

    // Data flavor for effect drag & drop
    public static final DataFlavor EFFECT_FLAVOR;
    static {
        try {
            EFFECT_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=" + EffectMetadata.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final JTree effectTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final JTextField searchField;
    private final EffectFactory effectFactory;

    // Category nodes for filtering
    private final Map<EffectCategory, DefaultMutableTreeNode> categoryNodes = new LinkedHashMap<>();

    public EffectPalettePanel() {
        this.effectFactory = EffectFactory.getInstance();

        setLayout(new BorderLayout(0, 4));
        setBackground(DarkTheme.BG_MEDIUM);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Title
        JLabel titleLabel = new JLabel("Effects");
        titleLabel.setFont(DarkTheme.FONT_BOLD);
        titleLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Search field
        searchField = createSearchField();

        // Top panel with title and search
        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Create tree
        rootNode = new DefaultMutableTreeNode("Effects");
        treeModel = new DefaultTreeModel(rootNode);
        effectTree = createEffectTree();

        // Populate tree
        populateTree();

        // Tree scroll pane
        JScrollPane scrollPane = new JScrollPane(effectTree);
        scrollPane.setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT));
        scrollPane.getViewport().setBackground(DarkTheme.BG_DARK);
        add(scrollPane, BorderLayout.CENTER);

        // Setup drag source
        setupDragSource();
    }

    /**
     * Create the search field with real-time filtering.
     */
    private JTextField createSearchField() {
        JTextField field = new JTextField();
        field.setBackground(DarkTheme.BG_DARK);
        field.setForeground(DarkTheme.TEXT_PRIMARY);
        field.setCaretColor(DarkTheme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Placeholder text simulation
        field.setToolTipText("Search effects...");

        // Real-time filtering
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTree(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTree(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTree(); }
        });

        return field;
    }

    /**
     * Create the effect tree with custom rendering.
     */
    private JTree createEffectTree() {
        JTree tree = new JTree(treeModel);
        tree.setBackground(DarkTheme.BG_DARK);
        tree.setForeground(DarkTheme.TEXT_PRIMARY);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setCellRenderer(new EffectTreeCellRenderer());

        // Expand all by default
        tree.expandRow(0);

        // Selection listener
        tree.addTreeSelectionListener(e -> {
            // Could fire events for detail panel
        });

        // Double-click to add (alternative to drag)
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        Object node = path.getLastPathComponent();
                        if (node instanceof DefaultMutableTreeNode treeNode) {
                            Object userObj = treeNode.getUserObject();
                            if (userObj instanceof EffectMetadata meta) {
                                firePropertyChange("effectDoubleClicked", null, meta);
                            }
                        }
                    }
                }
            }
        });

        return tree;
    }

    /**
     * Populate tree with categories and effects.
     */
    private void populateTree() {
        rootNode.removeAllChildren();
        categoryNodes.clear();

        // Create category nodes in order
        for (EffectCategory category : EffectCategory.values()) {
            List<EffectMetadata> effects = effectFactory.getEffectsByCategory(category);
            if (!effects.isEmpty()) {
                DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(category);
                categoryNodes.put(category, catNode);
                rootNode.add(catNode);

                // Sort effects by name
                effects.sort(Comparator.comparing(EffectMetadata::name));

                for (EffectMetadata meta : effects) {
                    catNode.add(new DefaultMutableTreeNode(meta));
                }
            }
        }

        treeModel.reload();
        expandAllCategories();
    }

    /**
     * Filter tree based on search text.
     */
    private void filterTree() {
        String filter = searchField.getText().toLowerCase().trim();

        if (filter.isEmpty()) {
            // Show all
            populateTree();
            return;
        }

        rootNode.removeAllChildren();

        for (EffectCategory category : EffectCategory.values()) {
            List<EffectMetadata> effects = effectFactory.getEffectsByCategory(category);
            List<EffectMetadata> matching = effects.stream()
                    .filter(m -> m.name().toLowerCase().contains(filter) ||
                            m.description().toLowerCase().contains(filter) ||
                            category.getDisplayName().toLowerCase().contains(filter))
                    .sorted(Comparator.comparing(EffectMetadata::name))
                    .toList();

            if (!matching.isEmpty()) {
                DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(category);
                rootNode.add(catNode);

                for (EffectMetadata meta : matching) {
                    catNode.add(new DefaultMutableTreeNode(meta));
                }
            }
        }

        treeModel.reload();
        expandAllCategories();
    }

    /**
     * Expand all category nodes.
     */
    private void expandAllCategories() {
        for (int i = 0; i < effectTree.getRowCount(); i++) {
            effectTree.expandRow(i);
        }
    }

    /**
     * Setup drag source for the tree.
     */
    private void setupDragSource() {
        DragSource dragSource = DragSource.getDefaultDragSource();

        dragSource.createDefaultDragGestureRecognizer(
                effectTree,
                DnDConstants.ACTION_COPY,
                dge -> {
                    TreePath path = effectTree.getSelectionPath();
                    if (path == null) return;

                    Object node = path.getLastPathComponent();
                    if (node instanceof DefaultMutableTreeNode treeNode) {
                        Object userObj = treeNode.getUserObject();
                        if (userObj instanceof EffectMetadata meta) {
                            Transferable transferable = new EffectTransferable(meta);
                            dge.startDrag(
                                    DragSource.DefaultCopyDrop,
                                    transferable,
                                    new EffectDragSourceListener()
                            );
                        }
                    }
                }
        );
    }

    /**
     * Get the currently selected effect metadata.
     */
    public EffectMetadata getSelectedEffect() {
        TreePath path = effectTree.getSelectionPath();
        if (path == null) return null;

        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode treeNode) {
            Object userObj = treeNode.getUserObject();
            if (userObj instanceof EffectMetadata meta) {
                return meta;
            }
        }
        return null;
    }

    /**
     * Transferable wrapper for effect metadata.
     */
    public static class EffectTransferable implements Transferable {
        private final EffectMetadata metadata;

        public EffectTransferable(EffectMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{EFFECT_FLAVOR, DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(EFFECT_FLAVOR) || flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(EFFECT_FLAVOR)) {
                return metadata;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return metadata.id();
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * Drag source listener for visual feedback.
     */
    private static class EffectDragSourceListener implements DragSourceListener {
        @Override
        public void dragEnter(DragSourceDragEvent dsde) {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
        }

        @Override
        public void dragOver(DragSourceDragEvent dsde) {}

        @Override
        public void dropActionChanged(DragSourceDragEvent dsde) {}

        @Override
        public void dragExit(DragSourceEvent dse) {
            dse.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent dsde) {}
    }
}
