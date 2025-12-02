package it.denzosoft.jfx2.ui;

import it.denzosoft.jfx2.audio.AudioEngine;
import it.denzosoft.jfx2.audio.AudioMetrics;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.ProcessingNode;
import it.denzosoft.jfx2.graph.SignalFlowAnalyzer;
import it.denzosoft.jfx2.graph.SignalGraph;
import it.denzosoft.jfx2.preset.FavoritesManager;
import it.denzosoft.jfx2.preset.TemplateManager;
import it.denzosoft.jfx2.preset.Rig;
import it.denzosoft.jfx2.ui.canvas.SignalFlowPanel;
import it.denzosoft.jfx2.ui.command.CommandHistory;
import it.denzosoft.jfx2.ui.dialogs.IRGeneratorDialog;
import it.denzosoft.jfx2.ui.dialogs.TemplateBrowserDialog;
import it.denzosoft.jfx2.ui.dialogs.SaveRigDialog;
import it.denzosoft.jfx2.ui.palette.EffectPalettePanel;
import it.denzosoft.jfx2.ui.panels.ConnectionInfoPanel;
import it.denzosoft.jfx2.ui.panels.MinimapPanel;
import it.denzosoft.jfx2.ui.panels.ParameterPanel;
import it.denzosoft.jfx2.ui.panels.StatusBarPanel;
import it.denzosoft.jfx2.ui.theme.DarkTheme;
import it.denzosoft.jfx2.ui.toast.ToastManager;
import it.denzosoft.jfx2.ui.icons.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Main application window for JFx2.
 */
public class MainFrame extends JFrame {

    // Window dimensions
    private static final int DEFAULT_WIDTH = 1400;
    private static final int DEFAULT_HEIGHT = 900;
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;

    // Application title
    private static final String APP_TITLE = "JFx2 - Guitar Multi-Effects Processor";

    // ==================== COMPONENTS ====================
    private SignalFlowPanel canvasPanel;
    private EffectPalettePanel palettePanel;
    private ParameterPanel parameterPanel;
    private ConnectionInfoPanel connectionInfoPanel;
    private JPanel bottomPanel;  // Container for parameter or connection panel
    private CardLayout bottomCardLayout;
    private StatusBarPanel statusBarPanel;
    private MinimapPanel minimapPanel;
    private JLabel zoomLabel;

    // ==================== UNDO/REDO ====================
    private CommandHistory commandHistory;
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;
    private JButton undoButton;
    private JButton redoButton;

    // ==================== APPLICATION STATE ====================
    private SignalGraph signalGraph;
    private AudioEngine audioEngine;
    private TemplateManager templateManager;
    private FavoritesManager favoritesManager;
    private String currentRigName = "Untitled";
    private String currentRigPath = null;
    private boolean isModified = false;

    // ==================== TIMER ====================
    private Timer statusUpdateTimer;

    public MainFrame() {
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupToolBar();
        setupStatusBar();
        setupWindowListeners();

        // Initialize application state
        signalGraph = new SignalGraph();
        // Canvas starts empty - user adds Audio Input/Output from effect tree
        audioEngine = new AudioEngine();
        templateManager = new TemplateManager();
        favoritesManager = new FavoritesManager();
        commandHistory = new CommandHistory(50);  // 50 operations undo stack

        // Listen to command history changes
        commandHistory.addPropertyChangeListener(e -> updateUndoRedoState());

        // Connect canvas to signal graph
        canvasPanel.setSignalGraph(signalGraph);

        // Connect minimap to canvas
        minimapPanel.setCanvas(canvasPanel);

        // Initialize Toast manager
        ToastManager.getInstance().initialize(this);

        // Update title
        updateTitle();
        updateUndoRedoState();

        // Start status update timer
        startStatusTimer();
    }

    private void initializeComponents() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Start maximized to fill the screen
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Set icon (if available)
        try {
            // Could load from resources
        } catch (Exception ignored) {
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // ==================== CENTER: Canvas ====================
        canvasPanel = new SignalFlowPanel();
        canvasPanel.addPropertyChangeListener("zoom", e -> updateZoomLabel());
        canvasPanel.addPropertyChangeListener("effectAdded", e -> markModified());
        canvasPanel.addPropertyChangeListener("connectionCreated", e -> markModified());
        canvasPanel.addPropertyChangeListener("nodesDeleted", e -> markModified());
        canvasPanel.addPropertyChangeListener("bypassChanged", e -> markModified());
        canvasPanel.addPropertyChangeListener("bypassToggled", e -> {
            // Update parameter panel's bypass toggle when bypass is changed via double-click
            parameterPanel.updateBypassState();
            markModified();
        });
        canvasPanel.addPropertyChangeListener("nodesDuplicated", e -> markModified());

        // ==================== WEST: Effect Palette + Minimap ====================
        palettePanel = new EffectPalettePanel();

        minimapPanel = new MinimapPanel();

        // Combine palette and minimap in a vertical layout
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setBackground(DarkTheme.BG_DARK);
        westPanel.add(palettePanel, BorderLayout.CENTER);
        westPanel.add(minimapPanel, BorderLayout.SOUTH);
        westPanel.setPreferredSize(new Dimension(220, 0));
        westPanel.setMinimumSize(new Dimension(150, 0));

        // Listen for double-click on effects (alternative to drag)
        palettePanel.addPropertyChangeListener("effectDoubleClicked", e -> {
            EffectMetadata metadata = (EffectMetadata) e.getNewValue();
            if (metadata != null) {
                addEffectToCanvas(metadata);
            }
        });

        // ==================== SOUTH: Parameter/Connection Panel ====================
        parameterPanel = new ParameterPanel();
        connectionInfoPanel = new ConnectionInfoPanel();

        // Create card layout for switching between panels
        bottomCardLayout = new CardLayout();
        bottomPanel = new JPanel(bottomCardLayout);
        bottomPanel.setPreferredSize(new Dimension(0, 200));
        bottomPanel.setMinimumSize(new Dimension(0, 180));
        bottomPanel.add(parameterPanel, "parameters");
        bottomPanel.add(connectionInfoPanel, "connection");

        // Connect node selection to parameter panel
        canvasPanel.addPropertyChangeListener("selection", e -> {
            ProcessingNode selectedNode = (ProcessingNode) e.getNewValue();
            if (selectedNode != null) {
                parameterPanel.setEffect(selectedNode);
                bottomCardLayout.show(bottomPanel, "parameters");

                // Update tuner source if no AudioInput exists
                if (!hasAudioInputNode()) {
                    signalGraph.setTunerSourceNode(selectedNode.getId());
                }
            } else if (!canvasPanel.getController().hasConnectionSelection()) {
                parameterPanel.setEffect(null);
                bottomCardLayout.show(bottomPanel, "parameters");

                // Reset tuner source when nothing selected
                if (!hasAudioInputNode()) {
                    signalGraph.setTunerSourceNode(null);
                }
            }
        });

        // Connect connection selection to connection info panel
        canvasPanel.addPropertyChangeListener("connectionSelection", e -> {
            Connection selectedConnection = (Connection) e.getNewValue();
            if (selectedConnection != null) {
                SignalFlowAnalyzer.SignalType signalType =
                    canvasPanel.getController().getConnectionSignalType(selectedConnection.getId());
                connectionInfoPanel.setConnection(selectedConnection, signalType);
                bottomCardLayout.show(bottomPanel, "connection");
            } else {
                connectionInfoPanel.setConnection(null, SignalFlowAnalyzer.SignalType.UNKNOWN);
            }
        });

        // Listen for parameter changes
        parameterPanel.addPropertyChangeListener("parameterChanged", e -> markModified());
        parameterPanel.addPropertyChangeListener("bypassChanged", e -> {
            canvasPanel.repaint();
            markModified();
        });

        // ==================== SPLIT PANE: Palette | Canvas ====================
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPanel, canvasPanel);
        mainSplit.setDividerLocation(220);
        mainSplit.setDividerSize(6);
        mainSplit.setContinuousLayout(true);
        mainSplit.setOneTouchExpandable(true);

        // ==================== SPLIT PANE: (Palette|Canvas) / Parameters ====================
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, bottomPanel);
        verticalSplit.setDividerLocation(DEFAULT_HEIGHT - 230);  // Leave 230px for bottom panel (200 content + borders)
        verticalSplit.setDividerSize(6);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setResizeWeight(1.0);  // Top gets extra space

        add(verticalSplit, BorderLayout.CENTER);
    }

    /**
     * Add an effect to the canvas from the palette (double-click).
     */
    private void addEffectToCanvas(EffectMetadata metadata) {
        if (signalGraph == null || metadata == null) return;

        var effect = it.denzosoft.jfx2.effects.EffectFactory.getInstance().create(metadata.id());
        if (effect == null) return;

        // Create node
        String nodeId = generateNodeId(metadata.name());
        var node = new it.denzosoft.jfx2.graph.EffectNode(nodeId, effect);
        signalGraph.addNode(node);

        // Position in center of visible area
        var viewState = canvasPanel.getViewState();
        int x = (int) viewState.panX();
        int y = (int) viewState.panY();

        canvasPanel.getController().setNodePosition(nodeId, new Point(x, y));
        canvasPanel.getController().selectNode(nodeId);
        canvasPanel.repaint();
        markModified();
        setStatus("Added: " + metadata.name());
    }

    /**
     * Generate a unique node ID.
     */
    private String generateNodeId(String baseName) {
        String baseId = baseName.toLowerCase().replaceAll("\\s+", "_");
        int counter = 1;
        String id = baseId;
        while (signalGraph.getNode(id) != null) {
            id = baseId + "_" + counter++;
        }
        return id;
    }


    // ==================== MENU BAR ====================

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(DarkTheme.BG_LIGHT);

        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createRigMenu());
        menuBar.add(createToolsMenu());
        menuBar.add(createHelpMenu());

        setJMenuBar(menuBar);
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem = createMenuItem("New Rig", KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, this::newRig);
        JMenuItem openItem = createMenuItem("Open...", KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, this::openRig);
        JMenuItem saveItem = createMenuItem("Save", KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, this::saveRig);
        JMenuItem saveAsItem = createMenuItem("Save As...", KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, this::saveRigAs);
        JMenuItem exitItem = createMenuItem("Exit", KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK, this::exitApplication);

        menu.add(newItem);
        menu.add(openItem);
        menu.addSeparator();
        menu.add(saveItem);
        menu.add(saveAsItem);
        menu.addSeparator();
        menu.add(exitItem);

        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        undoMenuItem = createMenuItem("Undo", KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, this::undo);
        redoMenuItem = createMenuItem("Redo", KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, this::redo);
        JMenuItem cutItem = createMenuItem("Cut", KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK, this::cut);
        JMenuItem copyItem = createMenuItem("Copy", KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, this::copy);
        JMenuItem pasteItem = createMenuItem("Paste", KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, this::paste);
        JMenuItem deleteItem = createMenuItem("Delete", KeyEvent.VK_DELETE, 0, this::delete);
        JMenuItem selectAllItem = createMenuItem("Select All", KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK, this::selectAll);

        // Cut/copy/paste disabled for now
        cutItem.setEnabled(false);
        copyItem.setEnabled(false);
        pasteItem.setEnabled(false);

        menu.add(undoMenuItem);
        menu.add(redoMenuItem);
        menu.addSeparator();
        menu.add(cutItem);
        menu.add(copyItem);
        menu.add(pasteItem);
        menu.add(deleteItem);
        menu.addSeparator();
        menu.add(selectAllItem);

        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);

        JMenuItem zoomInItem = createMenuItem("Zoom In", KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK, () -> canvasPanel.zoomIn());
        JMenuItem zoomOutItem = createMenuItem("Zoom Out", KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK, () -> canvasPanel.zoomOut());
        JMenuItem zoom100Item = createMenuItem("Zoom 100%", KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK, () -> canvasPanel.resetZoom());
        JMenuItem fitAllItem = createMenuItem("Fit All", KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK, () -> canvasPanel.fitAll());
        JMenuItem centerItem = createMenuItem("Center View", KeyEvent.VK_HOME, 0, () -> canvasPanel.centerView());

        JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Show Grid");
        gridItem.setSelected(true);
        gridItem.addActionListener(e -> {
            canvasPanel.getGridRenderer().setShowGrid(gridItem.isSelected());
            canvasPanel.repaint();
        });

        menu.add(zoomInItem);
        menu.add(zoomOutItem);
        menu.add(zoom100Item);
        menu.add(fitAllItem);
        menu.add(centerItem);
        menu.addSeparator();
        menu.add(gridItem);

        return menu;
    }

    private JMenu createRigMenu() {
        JMenu menu = new JMenu("Rig");
        menu.setMnemonic(KeyEvent.VK_R);

        JMenuItem browserItem = createMenuItem("Template Browser...", KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK, this::openTemplateBrowser);
        JMenuItem audioItem = createMenuItem("Audio Settings...", 0, 0, this::openAudioSettings);
        JMenuItem startItem = createMenuItem("Start Audio", KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK, this::toggleAudio);

        menu.add(browserItem);
        menu.addSeparator();
        menu.add(audioItem);
        menu.add(startItem);

        return menu;
    }

    private JMenu createToolsMenu() {
        JMenu menu = new JMenu("Tools");
        menu.setMnemonic(KeyEvent.VK_T);

        JMenuItem tunerItem = createMenuItem("Tuner...", KeyEvent.VK_T, 0, this::openTuner);
        JMenuItem irGeneratorItem = createMenuItem("IR Generator...", 0, 0, this::openIRGenerator);
        JMenuItem irManagerItem = createMenuItem("IR Manager...", 0, 0, this::openIRManager);

        menu.add(tunerItem);
        menu.addSeparator();
        menu.add(irGeneratorItem);
        menu.add(irManagerItem);

        return menu;
    }

    // Tuner dialog reference (to feed audio when SignalGraph is active)
    private it.denzosoft.jfx2.ui.dialogs.TunerDialog tunerDialog;

    private void openTuner() {
        if (tunerDialog != null && tunerDialog.isVisible()) {
            tunerDialog.toFront();
            return;
        }

        tunerDialog = new it.denzosoft.jfx2.ui.dialogs.TunerDialog(this, audioEngine);
        tunerDialog.setSampleRate(audioEngine != null && audioEngine.isInitialized()
                ? audioEngine.getConfig().sampleRate() : 44100);
        tunerDialog.setVisible(true);
    }

    private void openIRManager() {
        it.denzosoft.jfx2.ui.dialogs.IRManagerDialog dialog =
                new it.denzosoft.jfx2.ui.dialogs.IRManagerDialog(this);
        dialog.setVisible(true);
    }

    private void openIRGenerator() {
        // Create rig from current signal graph to pass to the dialog
        Rig currentRig = null;
        if (signalGraph != null && !signalGraph.getNodes().isEmpty()) {
            try {
                currentRig = templateManager.createFromGraph(signalGraph, canvasPanel.getController(), currentRigName, "Custom");
            } catch (Exception e) {
                // If we can't create the rig, just open without it
                System.err.println("Could not create rig from graph: " + e.getMessage());
            }
        }

        IRGeneratorDialog dialog = new IRGeneratorDialog(this, currentRig);
        dialog.setVisible(true);
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = createMenuItem("About JFx2", 0, 0, this::showAbout);
        JMenuItem shortcutsItem = createMenuItem("Keyboard Shortcuts", KeyEvent.VK_F1, 0, this::showShortcuts);

        menu.add(shortcutsItem);
        menu.addSeparator();
        menu.add(aboutItem);

        return menu;
    }

    private JMenuItem createMenuItem(String text, int keyCode, int modifiers, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        if (keyCode != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    // ==================== TOOLBAR ====================

    private void setupToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(DarkTheme.BG_LIGHT);
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.MENU_BORDER));

        // File operations
        toolBar.add(createToolButton("new", "New", "Create new rig (Ctrl+N)", this::newRig));
        toolBar.add(createToolButton("open", "Open", "Open rig (Ctrl+O)", this::openRig));
        toolBar.add(createToolButton("save", "Save", "Save rig (Ctrl+S)", this::saveRig));

        toolBar.addSeparator();

        // Edit operations
        undoButton = createToolButton("undo", "Undo", "Undo (Ctrl+Z)", this::undo);
        redoButton = createToolButton("redo", "Redo", "Redo (Ctrl+Shift+Z)", this::redo);
        toolBar.add(undoButton);
        toolBar.add(redoButton);

        toolBar.addSeparator();

        // Zoom controls
        toolBar.add(createToolButton("zoom_out", null, "Zoom out (Ctrl+-)", () -> canvasPanel.zoomOut()));
        zoomLabel = new JLabel("100%");
        zoomLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        zoomLabel.setPreferredSize(new Dimension(50, 24));
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        toolBar.add(zoomLabel);
        toolBar.add(createToolButton("zoom_in", null, "Zoom in (Ctrl++)", () -> canvasPanel.zoomIn()));
        toolBar.add(createToolButton("zoom_fit", "Fit", "Fit all (Ctrl+1)", () -> canvasPanel.fitAll()));

        toolBar.addSeparator();

        // Audio control
        JToggleButton audioToggle = new JToggleButton("Start", IconFactory.getIcon("play"));
        audioToggle.setToolTipText("Start/Stop audio processing (Ctrl+Space)");
        audioToggle.setFocusPainted(false);
        audioToggle.setContentAreaFilled(true);
        audioToggle.setOpaque(true);
        audioToggle.setBackground(DarkTheme.BUTTON_BG);
        audioToggle.setForeground(DarkTheme.TEXT_PRIMARY);
        audioToggle.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        audioToggle.addActionListener(e -> {
            if (audioToggle.isSelected()) {
                startAudio();
                audioToggle.setText("Stop");
                audioToggle.setIcon(IconFactory.getIcon("stop"));
            } else {
                stopAudio();
                audioToggle.setText("Start");
                audioToggle.setIcon(IconFactory.getIcon("play"));
            }
        });
        toolBar.add(audioToggle);

        toolBar.add(Box.createHorizontalGlue());

        // Settings
        toolBar.add(createToolButton("settings", "Settings", "Audio settings", this::openAudioSettings));

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolButton(String iconName, String text, String tooltip, Runnable action) {
        JButton button;
        Icon icon = IconFactory.getIcon(iconName);
        if (text != null && !text.isEmpty()) {
            button = new JButton(text, icon);
        } else {
            button = new JButton(icon);
        }
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(DarkTheme.BUTTON_BG);
        button.setForeground(DarkTheme.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.addActionListener(e -> action.run());

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(DarkTheme.BUTTON_BG_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(DarkTheme.BUTTON_BG);
            }
        });

        return button;
    }

    // ==================== STATUS BAR ====================

    private void setupStatusBar() {
        statusBarPanel = new StatusBarPanel();
        add(statusBarPanel, BorderLayout.SOUTH);
    }

    private void startStatusTimer() {
        statusUpdateTimer = new Timer(100, e -> updateStatus());
        statusUpdateTimer.start();
    }

    private void updateStatus() {
        if (audioEngine.isRunning()) {
            AudioMetrics metrics = audioEngine.getMetrics();
            statusBarPanel.setEngineRunning(true);
            statusBarPanel.setCpuUsage((float) metrics.getCpuLoadPercent());
            statusBarPanel.setLatency((float) audioEngine.getConfig().getEstimatedRoundTripLatencyMs());

            // Update level meters from signal graph
            statusBarPanel.setInputLevel(signalGraph.getInputLevelDb());
            statusBarPanel.setOutputLevel(signalGraph.getOutputLevelDb());
        } else {
            statusBarPanel.setEngineRunning(false);
            // Reset meters when stopped
            statusBarPanel.setInputLevel(-60f);
            statusBarPanel.setOutputLevel(-60f);
        }
    }

    /**
     * Update undo/redo menu and button state.
     */
    private void updateUndoRedoState() {
        boolean canUndo = commandHistory.canUndo();
        boolean canRedo = commandHistory.canRedo();

        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
            String undoDesc = commandHistory.getUndoDescription();
            undoMenuItem.setText(undoDesc != null ? "Undo " + undoDesc : "Undo");
        }

        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(canRedo);
            String redoDesc = commandHistory.getRedoDescription();
            redoMenuItem.setText(redoDesc != null ? "Redo " + redoDesc : "Redo");
        }

        if (undoButton != null) undoButton.setEnabled(canUndo);
        if (redoButton != null) redoButton.setEnabled(canRedo);
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("%.0f%%", canvasPanel.getZoom() * 100));
        }
    }

    // ==================== WINDOW LISTENERS ====================

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        // Handle component resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Could adjust layout here
            }
        });
    }

    // ==================== ACTIONS ====================

    private void newRig() {
        if (isModified) {
            int result = DarkTheme.showConfirmDialog(this,
                    "Save changes to current rig?",
                    "New Rig");
            if (result == JOptionPane.CANCEL_OPTION) return;
            if (result == JOptionPane.YES_OPTION) saveRig();
        }

        signalGraph = new SignalGraph();
        // Canvas starts empty - user adds Audio Input/Output from effect tree
        canvasPanel.setSignalGraph(signalGraph);
        currentRigName = "Untitled";
        currentRigPath = null;
        isModified = false;
        commandHistory.clear();
        updateTitle();
        updateUndoRedoState();
        canvasPanel.centerView();
        canvasPanel.resetZoom();
        ToastManager.getInstance().showInfo("New rig created");
    }

    private void openRig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Rig");
        chooser.setCurrentDirectory(templateManager.getTemplatesDir().toFile());
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JFx2 Rig (*.jfxrig)", "jfxrig"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                var rig = templateManager.load(path);
                templateManager.applyToGraph(rig, signalGraph, canvasPanel.getController());
                canvasPanel.setSignalGraph(signalGraph);
                currentRigName = rig.getName();
                currentRigPath = path;
                isModified = false;
                commandHistory.clear();
                updateTitle();
                updateUndoRedoState();

                // Prepare the graph so it's ready when Start is pressed
                prepareGraphForAudio();

                ToastManager.getInstance().showSuccess("Loaded: " + currentRigName);
            } catch (Exception e) {
                ToastManager.getInstance().showError("Error loading: " + e.getMessage());
            }
        }
    }

    private void saveRig() {
        if (currentRigName.equals("Untitled") || currentRigPath == null) {
            saveRigAs();
        } else {
            try {
                var rig = templateManager.createFromGraph(signalGraph, canvasPanel.getController(), currentRigName, "Custom");
                templateManager.save(rig, currentRigPath);
                isModified = false;
                updateTitle();
                ToastManager.getInstance().showSuccess("Saved: " + currentRigName);
            } catch (Exception e) {
                ToastManager.getInstance().showError("Error saving: " + e.getMessage());
            }
        }
    }

    private void saveRigAs() {
        // Get current metadata for dialog
        var currentRig = templateManager.createFromGraph(signalGraph, canvasPanel.getController(), currentRigName, "Custom");

        SaveRigDialog dialog = new SaveRigDialog(this, currentRig.getMetadata());
        var metadata = dialog.showDialog();

        if (metadata != null) {
            try {
                String filename = dialog.getSuggestedFilename();
                var rig = templateManager.createFromGraph(signalGraph, canvasPanel.getController(), metadata.name(), metadata.category());
                rig.setMetadata(metadata);
                templateManager.save(rig, filename);

                currentRigName = metadata.name();
                currentRigPath = filename;
                isModified = false;
                updateTitle();
                ToastManager.getInstance().showSuccess("Saved: " + currentRigName);
            } catch (Exception e) {
                ToastManager.getInstance().showError("Error saving: " + e.getMessage());
            }
        }
    }

    private void exitApplication() {
        if (isModified) {
            int result = DarkTheme.showConfirmDialog(this,
                    "Save changes before exit?",
                    "Exit");
            if (result == JOptionPane.CANCEL_OPTION) return;
            if (result == JOptionPane.YES_OPTION) saveRig();
        }

        stopAudio();
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        audioEngine.shutdown();
        signalGraph.shutdown();  // Shutdown thread pool
        dispose();
        System.exit(0);
    }

    // Edit actions
    private void undo() {
        if (commandHistory.canUndo()) {
            commandHistory.undo();
            canvasPanel.repaint();
            ToastManager.getInstance().showInfo("Undo: " + commandHistory.getRedoDescription());
        }
    }

    private void redo() {
        if (commandHistory.canRedo()) {
            String desc = commandHistory.getRedoDescription();
            commandHistory.redo();
            canvasPanel.repaint();
            ToastManager.getInstance().showInfo("Redo: " + desc);
        }
    }

    private void cut() {
        ToastManager.getInstance().showInfo("Cut - Coming soon");
    }

    private void copy() {
        ToastManager.getInstance().showInfo("Copy - Coming soon");
    }

    private void paste() {
        ToastManager.getInstance().showInfo("Paste - Coming soon");
    }

    private void delete() {
        // Directly call delete on the canvas panel
        canvasPanel.deleteSelectedNodes();
    }

    private void selectAll() {
        // Directly call selectAll on the canvas panel
        canvasPanel.selectAllNodes();
    }

    private void openTemplateBrowser() {
        TemplateBrowserDialog dialog = new TemplateBrowserDialog(this, templateManager);
        String selectedPreset = dialog.showDialog();

        if (selectedPreset != null) {
            try {
                Rig rig = templateManager.load(selectedPreset);
                templateManager.applyToGraph(rig, signalGraph, canvasPanel.getController());
                canvasPanel.setSignalGraph(signalGraph);
                currentRigName = rig.getName();
                currentRigPath = selectedPreset;
                isModified = false;
                updateTitle();

                // Mark as used in favorites
                favoritesManager.markUsed(selectedPreset);

                // Prepare the graph so it's ready when Start is pressed
                prepareGraphForAudio();

                ToastManager.getInstance().showSuccess("Loaded: " + currentRigName);
            } catch (IOException e) {
                ToastManager.getInstance().showError("Failed to load preset: " + e.getMessage());
            }
        }
    }

    /**
     * Prepare the signal graph for audio processing.
     * Initializes the audio engine if needed and prepares all nodes.
     */
    private void prepareGraphForAudio() {
        try {
            if (!audioEngine.isInitialized()) {
                audioEngine.initialize(it.denzosoft.jfx2.audio.AudioConfig.DEFAULT);
            }
            signalGraph.prepare(audioEngine.getConfig().sampleRate(), audioEngine.getConfig().bufferSize());
        } catch (Exception e) {
            System.err.println("Failed to prepare graph for audio: " + e.getMessage());
        }
    }

    private void openAudioSettings() {
        it.denzosoft.jfx2.ui.dialogs.SettingsDialog dialog =
                new it.denzosoft.jfx2.ui.dialogs.SettingsDialog(this, audioEngine);
        it.denzosoft.jfx2.audio.AudioConfig newConfig = dialog.showDialog();

        if (newConfig != null) {
            boolean wasRunning = audioEngine.isRunning();

            // Stop audio if running
            if (wasRunning) {
                audioEngine.stop();
            }

            try {
                // Apply new configuration
                audioEngine.shutdown();
                audioEngine.initialize(newConfig);

                // Restart if was running
                if (wasRunning) {
                    signalGraph.prepare(newConfig.sampleRate(), newConfig.bufferSize());
                    audioEngine.start((input, output, frameCount) -> signalGraph.process(input, output, frameCount));
                }

                ToastManager.getInstance().showSuccess("Audio settings updated");
            } catch (Exception e) {
                ToastManager.getInstance().showError("Failed to apply settings: " + e.getMessage());
            }
        }
    }

    private void toggleAudio() {
        if (audioEngine.isRunning()) {
            stopAudio();
        } else {
            startAudio();
        }
    }

    private void startAudio() {
        try {
            // Check if graph has any input source and output sink nodes
            boolean hasInputSource = false;
            boolean hasOutputSink = false;
            for (ProcessingNode node : signalGraph.getNodes()) {
                if (node instanceof it.denzosoft.jfx2.graph.EffectNode effectNode) {
                    var category = effectNode.getEffect().getMetadata().category();
                    if (category == it.denzosoft.jfx2.effects.EffectCategory.INPUT_SOURCE) hasInputSource = true;
                    if (category == it.denzosoft.jfx2.effects.EffectCategory.OUTPUT_SINK) hasOutputSink = true;
                }
            }

            // Only create default nodes if BOTH are missing
            if (!hasInputSource && !hasOutputSink) {
                createDefaultAudioNodes(true, true);
            } else if (!hasInputSource) {
                createDefaultAudioNodes(true, false);
            } else if (!hasOutputSink) {
                createDefaultAudioNodes(false, true);
            }

            if (!audioEngine.isInitialized()) {
                audioEngine.initialize(it.denzosoft.jfx2.audio.AudioConfig.DEFAULT);
            }
            signalGraph.prepare(audioEngine.getConfig().sampleRate(), audioEngine.getConfig().bufferSize());

            // Setup tuner and input meter to receive input audio
            statusBarPanel.setTunerSampleRate(audioEngine.getConfig().sampleRate());
            signalGraph.setInputAudioListener((samples, length) -> {
                statusBarPanel.feedInputAudio(samples, length);
                // Also feed TunerDialog if open and using SignalGraph
                if (tunerDialog != null && tunerDialog.isVisible() && tunerDialog.isUsingExternalSource()) {
                    tunerDialog.feedAudio(samples, length);
                }
            });

            // Setup signal monitor and output meter to receive output audio
            statusBarPanel.setSignalMonitorSampleRate(audioEngine.getConfig().sampleRate());
            signalGraph.setOutputAudioListener((samples, length) ->
                    statusBarPanel.feedOutputAudio(samples, length));

            // Setup input FFT listener for spectrum analyzer
            signalGraph.setInputFFTListener((magnitudes, numBins, sr, binFreq) ->
                    statusBarPanel.feedInputFFT(magnitudes, numBins, sr, binFreq));

            audioEngine.start((input, output, frameCount) -> signalGraph.process(input, output, frameCount));

            // Check for audio device errors after starting
            String deviceError = signalGraph.getAudioDeviceError();
            if (deviceError != null) {
                audioEngine.stop();
                DarkTheme.showMessageDialog(this,
                        "Failed to open audio device:\n" + deviceError,
                        "Audio Device Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            setStatus("Audio started");
        } catch (Exception e) {
            DarkTheme.showMessageDialog(this,
                    "Error starting audio: " + e.getMessage(),
                    "Audio Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Create default audio input/output nodes if missing.
     */
    private void createDefaultAudioNodes(boolean createInput, boolean createOutput) {
        var effectFactory = it.denzosoft.jfx2.effects.EffectFactory.getInstance();
        it.denzosoft.jfx2.graph.EffectNode inputNode = null;
        it.denzosoft.jfx2.graph.EffectNode outputNode = null;

        if (createInput) {
            var inputEffect = effectFactory.create("audioinput");
            if (inputEffect != null) {
                inputNode = new it.denzosoft.jfx2.graph.EffectNode("audio_input", inputEffect);
                signalGraph.addNode(inputNode);
                canvasPanel.getController().setNodePosition("audio_input", new java.awt.Point(100, 200));
            }
        }

        if (createOutput) {
            var outputEffect = effectFactory.create("audiooutput");
            if (outputEffect != null) {
                outputNode = new it.denzosoft.jfx2.graph.EffectNode("audio_output", outputEffect);
                signalGraph.addNode(outputNode);
                canvasPanel.getController().setNodePosition("audio_output", new java.awt.Point(400, 200));
            }
        }

        // Connect input to output if both were created and canvas was empty
        if (inputNode != null && outputNode != null && signalGraph.getNodeCount() == 2) {
            try {
                signalGraph.connect(inputNode.getOutput(), outputNode.getInput());
            } catch (Exception e) {
                System.err.println("Could not connect default audio nodes: " + e.getMessage());
            }
        }

        canvasPanel.repaint();
        ToastManager.getInstance().showInfo("Created default audio nodes");
    }

    private void stopAudio() {
        audioEngine.stop();
        signalGraph.setInputAudioListener(null);  // Disconnect tuner
        signalGraph.setTunerSourceNode(null);     // Reset tuner source
        statusBarPanel.resetTuner();
        setStatus("Audio stopped");
    }

    /**
     * Check if the graph has an AudioInput node.
     */
    private boolean hasAudioInputNode() {
        for (ProcessingNode node : signalGraph.getNodes()) {
            if (node instanceof it.denzosoft.jfx2.graph.EffectNode effectNode) {
                if ("audioinput".equals(effectNode.getEffect().getMetadata().id())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showAbout() {
        int cores = Runtime.getRuntime().availableProcessors();
        int audioThreads = signalGraph.getThreadCount();

        DarkTheme.showMessageDialog(this,
                String.format("""
                JFx2 - Guitar Multi-Effects Processor
                Version 1.0

                A node-based guitar effects processor
                with real-time audio processing.

                Features:
                - Dark theme UI
                - Signal flow canvas with zoom/pan
                - Drag & drop effects from palette
                - Visual connection routing
                - Real-time audio processing
                - Multi-core parallel DSP

                System:
                - CPU Cores: %d
                - Audio Threads: %d
                - Parallel Processing: %s

                Pure Java - No external dependencies
                """, cores, audioThreads,
                        signalGraph.isParallelProcessingEnabled() ? "Enabled" : "Disabled"),
                "About JFx2",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShortcuts() {
        DarkTheme.showMessageDialog(this,
                """
                Keyboard Shortcuts:

                File:
                  Ctrl+N        New Rig
                  Ctrl+O        Open Rig
                  Ctrl+S        Save Rig
                  Ctrl+Shift+S  Save As

                View:
                  Ctrl++        Zoom In
                  Ctrl+-        Zoom Out
                  Ctrl+0        Zoom 100%
                  Ctrl+1        Fit All
                  Arrow Keys    Pan Canvas
                  Space+Drag    Pan Canvas
                  Mouse Wheel   Zoom at cursor
                  Middle Drag   Pan Canvas

                Edit:
                  Delete        Delete Selection
                  Ctrl+A        Select All
                  Ctrl+D        Duplicate Selection
                  B             Toggle Bypass
                  F2            Rename Block
                  Escape        Cancel / Deselect

                Canvas:
                  Click         Select block
                  Shift+Click   Add to selection
                  Drag          Move selection
                  Right-click   Context menu
                  Output drag   Create connection
                """,
                "Keyboard Shortcuts",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== UTILITY ====================

    private void updateTitle() {
        String modified = isModified ? " *" : "";
        setTitle(currentRigName + modified + " - " + APP_TITLE);
    }

    private void setStatus(String message) {
        statusBarPanel.setMessage(message);
    }

    public void markModified() {
        if (!isModified) {
            isModified = true;
            updateTitle();
        }
    }

    // ==================== GETTERS ====================

    public SignalFlowPanel getCanvasPanel() {
        return canvasPanel;
    }

    public SignalGraph getSignalGraph() {
        return signalGraph;
    }

    public AudioEngine getAudioEngine() {
        return audioEngine;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    // ==================== STATIC LAUNCH ====================

    /**
     * Launch the application GUI.
     */
    public static void launch() {
        // Show splash screen first
        SplashScreen splash = new SplashScreen();
        splash.showSplash();
        splash.setProgress(0, "Starting JFx2...");

        // Apply dark theme
        splash.setProgress(10, "Applying theme...");
        DarkTheme.apply();

        SwingUtilities.invokeLater(() -> {
            try {
                splash.setProgress(20, "Initializing components...");
                Thread.sleep(100);

                splash.setProgress(30, "Loading effect library...");
                // Pre-load effect factory
                it.denzosoft.jfx2.effects.EffectFactory.getInstance();
                Thread.sleep(100);

                splash.setProgress(50, "Creating main window...");
                MainFrame frame = new MainFrame();
                Thread.sleep(100);

                splash.setProgress(70, "Loading presets...");
                Thread.sleep(100);

                splash.setProgress(85, "Preparing audio engine...");
                frame.pack();
                frame.setLocationRelativeTo(null);
                Thread.sleep(100);

                splash.setProgress(100, "Ready!");
                Thread.sleep(300);

                // Close splash and show main window
                splash.closeSplash();
                frame.setVisible(true);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                splash.closeSplash();
            }
        });
    }
}
