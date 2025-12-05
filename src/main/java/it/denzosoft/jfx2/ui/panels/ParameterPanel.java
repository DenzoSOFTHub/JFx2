package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.Parameter;
import it.denzosoft.jfx2.effects.ParameterType;
import it.denzosoft.jfx2.effects.impl.DelayEffect;
import it.denzosoft.jfx2.effects.impl.FilterEffect;
import it.denzosoft.jfx2.effects.impl.GraphicEQEffect;
import it.denzosoft.jfx2.effects.impl.LooperEffect;
import it.denzosoft.jfx2.effects.impl.NoiseGateEffect;
import it.denzosoft.jfx2.effects.impl.SettingsEffect;
import it.denzosoft.jfx2.graph.EffectNode;
import it.denzosoft.jfx2.graph.EffectNode.EffectMonitorListener;
import it.denzosoft.jfx2.graph.MixerNode;
import it.denzosoft.jfx2.graph.ProcessingNode;
import it.denzosoft.jfx2.preset.Preset;
import it.denzosoft.jfx2.preset.PresetManager;
import it.denzosoft.jfx2.ui.controls.JFaderSlider;
import it.denzosoft.jfx2.ui.controls.JLedIndicator;
import it.denzosoft.jfx2.ui.controls.JLedToggle;
import it.denzosoft.jfx2.ui.controls.JRotaryKnob;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic parameter panel for editing effect parameters.
 * Displays controls based on the selected effect's parameters.
 */
public class ParameterPanel extends JPanel {

    // ==================== CURRENT EFFECT ====================
    private ProcessingNode currentNode;
    private AudioEffect currentEffect;

    // ==================== UI COMPONENTS ====================
    private JPanel headerPanel;
    private JLabel effectNameLabel;
    private JLabel effectCategoryLabel;
    private JLedIndicator clippingLed;
    private JLedToggle bypassToggle;
    private JPanel controlsPanel;
    private JLabel placeholderLabel;

    // ==================== CLIPPING UPDATE ====================
    private Timer clippingUpdateTimer;

    // ==================== PRESET UI ====================
    private JComboBox<Preset> presetComboBox;
    private JButton savePresetButton;
    private JButton deletePresetButton;
    private boolean updatingPresetCombo = false;

    // ==================== BINDINGS ====================
    private final Map<String, JComponent> parameterControls = new HashMap<>();
    private final List<PropertyChangeListener> bindingListeners = new ArrayList<>();

    // ==================== LAYOUT ====================
    private static final int CONTROL_WIDTH = 80;
    private static final int CONTROL_GAP = 16;
    private static final int PADDING = 16;

    // ==================== TAP TEMPO ====================
    private static final int TAP_TEMPO_MAX_TAPS = 8;  // Number of taps to average
    private static final long TAP_TEMPO_TIMEOUT_MS = 2000;  // Reset after 2 seconds of no taps
    private final LinkedList<Long> tapTimes = new LinkedList<>();
    private JRotaryKnob bpmKnob;  // Reference to update BPM knob when tapping

    // ==================== FILTER EFFECT ====================
    private FrequencyResponsePanel frequencyResponsePanel;

    // ==================== GRAPHIC EQ ====================
    private GraphicEQResponsePanel graphicEQResponsePanel;

    // ==================== NOISE GATE ====================
    private NoiseGateMeterPanel noiseGateMeterPanel;

    // ==================== SIGNAL VIEW ====================
    private JButton signalViewToggle;
    private JComboBox<String> graphTypeCombo;
    private SignalWaveformPanel signalWaveformPanel;
    private FrequencyAnalysisPanel frequencyAnalysisPanel;
    private SpectrumBandsPanel spectrumBandsPanel;
    private JPanel signalContainer;  // CardLayout for signal view types
    private CardLayout signalContainerLayout;
    private JPanel contentPanel;  // CardLayout container (parameters vs signal)
    private CardLayout contentLayout;
    private boolean showingSignal = false;
    private static final String CARD_PARAMETERS = "parameters";
    private static final String CARD_SIGNAL = "signal";
    private static final String SIGNAL_CARD_WAVEFORM = "waveform";
    private static final String SIGNAL_CARD_FREQUENCY = "frequency";
    private static final String SIGNAL_CARD_SPECTRUM = "spectrum";
    private EffectMonitorListener effectMonitorListener;

    public ParameterPanel() {
        setLayout(new BorderLayout());
        setBackground(DarkTheme.BG_LIGHT);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BG_DARK));

        createHeaderPanel();
        createControlsPanel();
        createPlaceholder();

        // Timer to update clipping LED (60 fps for responsive feedback)
        clippingUpdateTimer = new Timer(16, e -> updateClippingLed());
        clippingUpdateTimer.start();

        showPlaceholder();
    }

    /**
     * Update the clipping LED based on current node state.
     */
    private void updateClippingLed() {
        if (clippingLed != null && currentNode != null) {
            clippingLed.setActive(currentNode.isClipping());
        } else if (clippingLed != null) {
            clippingLed.setActive(false);
        }
    }

    /**
     * Create the header panel with effect name, preset selector, and bypass toggle.
     */
    private void createHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setBackground(DarkTheme.BG_MEDIUM);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, PADDING, 8, PADDING));

        // Left: Icon and name
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        effectNameLabel = new JLabel("No effect selected");
        effectNameLabel.setFont(DarkTheme.FONT_BOLD.deriveFont(14f));
        effectNameLabel.setForeground(DarkTheme.TEXT_PRIMARY);

        effectCategoryLabel = new JLabel("");
        effectCategoryLabel.setFont(DarkTheme.FONT_SMALL);
        effectCategoryLabel.setForeground(DarkTheme.TEXT_SECONDARY);

        leftPanel.add(effectNameLabel);
        leftPanel.add(effectCategoryLabel);

        // Center: Preset selector
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        presetPanel.setOpaque(false);

        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setFont(DarkTheme.FONT_SMALL);
        presetLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        presetPanel.add(presetLabel);

        presetComboBox = new JComboBox<>();
        presetComboBox.setRenderer(new PresetComboRenderer());
        presetComboBox.setPreferredSize(new Dimension(180, 26));
        presetComboBox.setBackground(DarkTheme.BG_LIGHT);
        presetComboBox.setForeground(DarkTheme.TEXT_PRIMARY);
        presetComboBox.setToolTipText("Select a saved preset to apply");
        presetComboBox.addActionListener(e -> {
            if (!updatingPresetCombo && currentEffect != null) {
                Preset selected = (Preset) presetComboBox.getSelectedItem();
                if (selected != null) {
                    applySelectedPreset(selected);
                }
            }
        });
        presetPanel.add(presetComboBox);

        savePresetButton = new JButton("Save");
        savePresetButton.setFont(DarkTheme.FONT_SMALL);
        savePresetButton.setForeground(DarkTheme.TEXT_PRIMARY);
        savePresetButton.setBackground(DarkTheme.BG_LIGHT);
        savePresetButton.setFocusPainted(false);
        savePresetButton.setPreferredSize(new Dimension(60, 26));
        savePresetButton.setToolTipText("Save current settings as a new preset");
        savePresetButton.addActionListener(e -> saveCurrentAsPreset());
        presetPanel.add(savePresetButton);

        deletePresetButton = new JButton("Del");
        deletePresetButton.setFont(DarkTheme.FONT_SMALL);
        deletePresetButton.setForeground(DarkTheme.ACCENT_ERROR);
        deletePresetButton.setBackground(DarkTheme.BG_LIGHT);
        deletePresetButton.setFocusPainted(false);
        deletePresetButton.setPreferredSize(new Dimension(50, 26));
        deletePresetButton.setToolTipText("Delete the selected preset");
        deletePresetButton.addActionListener(e -> deleteSelectedPreset());
        presetPanel.add(deletePresetButton);

        // Right: Graph type combo, Signal toggle and Bypass toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        // Graph type combo (visible only when signal view is active)
        String[] graphTypes = {"Signal", "Frequency Analysis", "Spectrum"};
        graphTypeCombo = new JComboBox<>(graphTypes);
        graphTypeCombo.setFont(DarkTheme.FONT_SMALL);
        graphTypeCombo.setForeground(DarkTheme.TEXT_PRIMARY);
        graphTypeCombo.setBackground(DarkTheme.BG_LIGHT);
        graphTypeCombo.setPreferredSize(new Dimension(130, 26));
        graphTypeCombo.setToolTipText("Select the type of signal visualization");
        graphTypeCombo.setVisible(false);  // Hidden by default
        graphTypeCombo.addActionListener(e -> switchGraphType());
        rightPanel.add(graphTypeCombo);

        // Signal/Parameters toggle button
        signalViewToggle = new JButton("Signal");
        signalViewToggle.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD));
        signalViewToggle.setForeground(DarkTheme.TEXT_PRIMARY);
        signalViewToggle.setBackground(DarkTheme.BG_LIGHT);
        signalViewToggle.setFocusPainted(false);
        signalViewToggle.setPreferredSize(new Dimension(80, 26));
        signalViewToggle.setToolTipText("Toggle between parameters and signal waveform view");
        signalViewToggle.addActionListener(e -> toggleSignalView());
        rightPanel.add(signalViewToggle);

        // Clipping LED indicator
        clippingLed = new JLedIndicator("Clip");
        clippingLed.setToolTipText("Clipping indicator - lights up when output exceeds -1 to +1 range");
        rightPanel.add(clippingLed);

        bypassToggle = new JLedToggle("Bypass");
        bypassToggle.setLedOnColor(DarkTheme.ACCENT_WARNING);
        bypassToggle.addPropertyChangeListener("selected", e -> {
            if (currentNode != null) {
                currentNode.setBypassed(bypassToggle.isSelected());
                firePropertyChange("bypassChanged", null, currentNode);
            }
        });

        rightPanel.add(bypassToggle);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(presetPanel, BorderLayout.CENTER);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     * Custom renderer for preset combobox.
     */
    private static class PresetComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value == null) {
                setText("-- Select Preset --");
                setForeground(DarkTheme.TEXT_DISABLED);
            } else if (value instanceof Preset preset) {
                setText(preset.getName());
                setForeground(isSelected ? DarkTheme.TEXT_PRIMARY : DarkTheme.TEXT_SECONDARY);
            }

            setBackground(isSelected ? DarkTheme.BG_LIGHTER : DarkTheme.BG_LIGHT);
            return this;
        }
    }

    /**
     * Update the preset combobox with presets for the current effect.
     */
    private void updatePresetComboBox() {
        updatingPresetCombo = true;
        try {
            presetComboBox.removeAllItems();
            presetComboBox.addItem(null);  // "Select Preset" placeholder

            if (currentEffect != null) {
                String effectId = currentEffect.getMetadata().id();
                List<Preset> presets = PresetManager.getInstance().getPresetsForEffect(effectId);
                for (Preset preset : presets) {
                    presetComboBox.addItem(preset);
                }

                // Enable preset controls
                presetComboBox.setEnabled(true);
                savePresetButton.setEnabled(true);
            } else {
                presetComboBox.setEnabled(false);
                savePresetButton.setEnabled(false);
            }

            presetComboBox.setSelectedIndex(0);  // Select placeholder
            deletePresetButton.setEnabled(false);
        } finally {
            updatingPresetCombo = false;
        }
    }

    /**
     * Apply the selected preset to the current effect.
     */
    private void applySelectedPreset(Preset preset) {
        if (currentEffect == null || preset == null) return;

        try {
            PresetManager.getInstance().applyPreset(preset, currentEffect);
            refresh();  // Update all controls
            deletePresetButton.setEnabled(true);
            firePropertyChange("presetApplied", null, preset);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to apply preset: " + e.getMessage(),
                    "Preset Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save current effect settings as a new preset.
     */
    private void saveCurrentAsPreset() {
        if (currentEffect == null) return;

        String effectName = currentEffect.getMetadata().name();
        String defaultName = effectName + " Preset";

        String name = JOptionPane.showInputDialog(this,
                "Enter preset name:",
                "Save Preset - " + effectName,
                JOptionPane.PLAIN_MESSAGE);

        if (name == null || name.trim().isEmpty()) {
            return;  // Cancelled
        }

        name = name.trim();

        // Check if preset already exists
        String effectId = currentEffect.getMetadata().id();
        if (PresetManager.getInstance().presetExists(effectId, name)) {
            int response = JOptionPane.showConfirmDialog(this,
                    "Preset '" + name + "' already exists. Overwrite?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Preset preset = PresetManager.getInstance().savePreset(currentEffect, name);
            updatePresetComboBox();

            // Select the newly saved preset
            for (int i = 0; i < presetComboBox.getItemCount(); i++) {
                Preset item = presetComboBox.getItemAt(i);
                if (item != null && item.getName().equals(name)) {
                    updatingPresetCombo = true;
                    presetComboBox.setSelectedIndex(i);
                    updatingPresetCombo = false;
                    deletePresetButton.setEnabled(true);
                    break;
                }
            }

            firePropertyChange("presetSaved", null, preset);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save preset: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Delete the currently selected preset.
     */
    private void deleteSelectedPreset() {
        Preset selected = (Preset) presetComboBox.getSelectedItem();
        if (selected == null) return;

        int response = JOptionPane.showConfirmDialog(this,
                "Delete preset '" + selected.getName() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            if (PresetManager.getInstance().deletePreset(selected)) {
                updatePresetComboBox();
                firePropertyChange("presetDeleted", null, selected);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to delete preset",
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Create the main controls panel with CardLayout for switching views.
     */
    private void createControlsPanel() {
        // Controls panel for parameters
        controlsPanel = new JPanel();
        controlsPanel.setBackground(DarkTheme.BG_LIGHT);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));

        JScrollPane controlsScrollPane = new JScrollPane(controlsPanel);
        controlsScrollPane.setBorder(null);
        controlsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        controlsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlsScrollPane.getViewport().setBackground(DarkTheme.BG_LIGHT);
        controlsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Create signal visualization panels
        signalWaveformPanel = new SignalWaveformPanel();
        frequencyAnalysisPanel = new FrequencyAnalysisPanel();
        spectrumBandsPanel = new SpectrumBandsPanel();

        // Signal container with CardLayout to switch between graph types
        signalContainerLayout = new CardLayout();
        signalContainer = new JPanel(signalContainerLayout);
        signalContainer.setBackground(DarkTheme.BG_DARK);
        signalContainer.add(signalWaveformPanel, SIGNAL_CARD_WAVEFORM);
        signalContainer.add(frequencyAnalysisPanel, SIGNAL_CARD_FREQUENCY);
        signalContainer.add(spectrumBandsPanel, SIGNAL_CARD_SPECTRUM);

        JScrollPane signalScrollPane = new JScrollPane(signalContainer);
        signalScrollPane.setBorder(null);
        signalScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        signalScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        signalScrollPane.getViewport().setBackground(DarkTheme.BG_DARK);

        // CardLayout container to switch between parameters and signal views
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.add(controlsScrollPane, CARD_PARAMETERS);
        contentPanel.add(signalScrollPane, CARD_SIGNAL);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Create placeholder shown when no effect is selected.
     */
    private void createPlaceholder() {
        placeholderLabel = new JLabel("Select an effect block to edit its parameters");
        placeholderLabel.setForeground(DarkTheme.TEXT_DISABLED);
        placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        placeholderLabel.setFont(DarkTheme.FONT_REGULAR);
    }

    /**
     * Show placeholder when no effect is selected.
     */
    private void showPlaceholder() {
        controlsPanel.removeAll();
        controlsPanel.setLayout(new BorderLayout());
        controlsPanel.add(placeholderLabel, BorderLayout.CENTER);
        headerPanel.setVisible(false);
        revalidate();
        repaint();
    }

    /**
     * Set the effect to display.
     */
    public void setEffect(ProcessingNode node) {
        // Clear previous bindings
        clearBindings();

        // Reset to parameters view when changing effects
        if (showingSignal) {
            showingSignal = false;
            signalViewToggle.setText("Signal");
            signalViewToggle.setBackground(DarkTheme.BG_LIGHT);
            graphTypeCombo.setVisible(false);
            graphTypeCombo.setSelectedIndex(0);  // Reset to Signal
            contentLayout.show(contentPanel, CARD_PARAMETERS);
            stopSignalVisualization();
        }

        this.currentNode = node;

        if (node == null) {
            this.currentEffect = null;
            showPlaceholder();
            return;
        }

        // Check if it's an EffectNode with parameters
        if (node instanceof EffectNode effectNode) {
            this.currentEffect = effectNode.getEffect();
            if (currentEffect != null) {
                // Show header
                headerPanel.setVisible(true);

                // Update header with effect metadata
                var metadata = currentEffect.getMetadata();
                effectNameLabel.setText(metadata.name());
                effectCategoryLabel.setText("- " + metadata.category().getDisplayName());
                bypassToggle.setSelected(node.isBypassed());
                bypassToggle.setEnabled(true);

                // Update preset combobox
                updatePresetComboBox();

                // Build controls
                buildControls();
                return;
            }
        }

        // For non-EffectNode nodes (Input, Output, GainNode, etc.)
        // Show header with node info but no parameter controls
        headerPanel.setVisible(true);
        effectNameLabel.setText(node.getName());

        // Disable preset controls for non-effect nodes
        this.currentEffect = null;
        presetComboBox.removeAllItems();
        presetComboBox.setEnabled(false);
        savePresetButton.setEnabled(false);
        deletePresetButton.setEnabled(false);

        // Show node type
        String typeLabel = switch (node.getNodeType()) {
            case INPUT -> "Input";
            case OUTPUT -> "Output";
            case EFFECT -> "Effect";
            case SPLITTER -> "Splitter";
            case MIXER -> "Mixer";
        };
        effectCategoryLabel.setText("- " + typeLabel);

        // Bypass toggle only for effect nodes
        boolean canBypass = node.getNodeType() == it.denzosoft.jfx2.graph.NodeType.EFFECT;
        bypassToggle.setEnabled(canBypass);
        bypassToggle.setSelected(canBypass && node.isBypassed());

        // Check for MixerNode with special controls
        if (node instanceof MixerNode mixerNode) {
            buildMixerControls(mixerNode);
            return;
        }

        // Show message that this node has no editable parameters
        controlsPanel.removeAll();
        controlsPanel.setLayout(new BorderLayout());
        JLabel noParamsLabel = new JLabel("This node has no editable parameters");
        noParamsLabel.setForeground(DarkTheme.TEXT_DISABLED);
        noParamsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controlsPanel.add(noParamsLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Update the bypass toggle to match the current node's bypass state.
     * Called when bypass is toggled from outside (e.g., double-click on canvas).
     */
    public void updateBypassState() {
        if (currentNode != null) {
            bypassToggle.setSelected(currentNode.isBypassed());
        }
    }

    /**
     * Build parameter controls for current effect.
     */
    private void buildControls() {
        controlsPanel.removeAll();
        parameterControls.clear();
        bpmKnob = null;  // Reset BPM knob reference
        tapTimes.clear();  // Reset tap times

        // Stop previous frequency response panel if any
        if (frequencyResponsePanel != null) {
            frequencyResponsePanel.stopUpdates();
            frequencyResponsePanel = null;
        }
        graphicEQResponsePanel = null;

        // Stop previous noise gate meter panel if any
        if (noiseGateMeterPanel != null) {
            noiseGateMeterPanel.stopUpdates();
            noiseGateMeterPanel = null;
        }

        if (currentEffect == null) return;

        // Check for special effects with custom UI
        if (currentEffect instanceof FilterEffect filterEffect) {
            buildFilterControls(filterEffect);
            return;
        }

        if (currentEffect instanceof GraphicEQEffect graphicEQ) {
            buildGraphicEQControls(graphicEQ);
            return;
        }

        if (currentEffect instanceof LooperEffect looper) {
            buildLooperControls(looper);
            return;
        }

        if (currentEffect instanceof NoiseGateEffect noiseGate) {
            buildNoiseGateControls(noiseGate);
            return;
        }

        if (currentEffect instanceof SettingsEffect settings) {
            buildSettingsControls(settings);
            return;
        }

        boolean isDelayEffect = currentEffect instanceof DelayEffect;

        List<Parameter> parameters = currentEffect.getParameters();
        int[] rowSizes = currentEffect.getParameterRowSizes();

        // Check if effect defines row-based layout
        if (rowSizes != null && rowSizes.length > 0) {
            buildRowBasedControls(parameters, rowSizes, isDelayEffect);
        } else {
            // Standard single-row FlowLayout for other effects
            controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));

            for (Parameter param : parameters) {
                JComponent control = createControlForParameter(param);
                if (control != null) {
                    // Wrap in labeled panel
                    JPanel wrapper = createControlWrapper(param, control);
                    controlsPanel.add(wrapper);
                    parameterControls.put(param.getId(), control);

                    // For DelayEffect, keep reference to BPM knob and add TAP button after it
                    if (isDelayEffect && "bpm".equals(param.getId()) && control instanceof JRotaryKnob) {
                        bpmKnob = (JRotaryKnob) control;
                        JPanel tapButton = createTapTempoButton();
                        controlsPanel.add(tapButton);
                    }
                }
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Build controls with row-based layout using parameter row sizes.
     */
    private void buildRowBasedControls(List<Parameter> parameters, int[] rowSizes, boolean isDelayEffect) {
        // Use compact row height for effects with many rows
        final int rowHeight = rowSizes.length > 3 ? 95 : 115;

        // Use BoxLayout for vertical stacking - controlsPanel is already in a JScrollPane
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        int paramIndex = 0;
        for (int rowIndex = 0; rowIndex < rowSizes.length && paramIndex < parameters.size(); rowIndex++) {
            int paramsInRow = rowSizes[rowIndex];

            // Create a row panel with FlowLayout and FIXED height
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP / 2, 2)) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.height = rowHeight;
                    return d;
                }
                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(100, rowHeight);
                }
                @Override
                public Dimension getMaximumSize() {
                    Dimension d = super.getMaximumSize();
                    d.height = rowHeight;
                    return d;
                }
            };
            rowPanel.setBackground(rowIndex % 2 == 0 ? DarkTheme.BG_LIGHT : DarkTheme.BG_MEDIUM);
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Add parameters for this row
            for (int i = 0; i < paramsInRow && paramIndex < parameters.size(); i++) {
                Parameter param = parameters.get(paramIndex);
                paramIndex++;

                JComponent control = createControlForParameter(param);
                if (control != null) {
                    JPanel wrapper = createCompactControlWrapper(param, control);
                    rowPanel.add(wrapper);
                    parameterControls.put(param.getId(), control);

                    // For DelayEffect, keep reference to BPM knob and add TAP button after it
                    if (isDelayEffect && "bpm".equals(param.getId()) && control instanceof JRotaryKnob) {
                        bpmKnob = (JRotaryKnob) control;
                        JPanel tapButton = createTapTempoButton();
                        rowPanel.add(tapButton);
                    }
                }
            }

            controlsPanel.add(rowPanel);
        }
    }

    /**
     * Build controls for FilterEffect with frequency response graph.
     */
    private void buildFilterControls(FilterEffect filterEffect) {
        // Use BorderLayout: controls on left (fixed), graph fills remaining space
        controlsPanel.setLayout(new BorderLayout(CONTROL_GAP, 0));

        // Left side: 5 rows of band controls + output
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(DarkTheme.BG_LIGHT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Create a row for each band
        for (int band = 0; band < FilterEffect.NUM_BANDS; band++) {
            JPanel bandRow = createFilterBandRow(filterEffect, band);
            leftPanel.add(bandRow);
        }

        // Add output control row
        JPanel outputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        outputRow.setBackground(DarkTheme.BG_LIGHT);
        outputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // Find the output parameter
        for (Parameter param : filterEffect.getParameters()) {
            if ("output".equals(param.getId())) {
                JComponent control = createControlForParameter(param);
                if (control != null) {
                    JPanel wrapper = createCompactControlWrapper(param, control);
                    outputRow.add(wrapper);
                    parameterControls.put(param.getId(), control);

                    // Add change listener for real-time graph update
                    addFilterParameterListener(control);
                }
                break;
            }
        }
        leftPanel.add(outputRow);

        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(null);
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScroll.getViewport().setBackground(DarkTheme.BG_LIGHT);
        // Fixed width for controls - enough for 4 parameters per band + band label
        leftScroll.setPreferredSize(new Dimension(420, 0));

        // Right side: frequency response graph fills all remaining space
        frequencyResponsePanel = new FrequencyResponsePanel();
        frequencyResponsePanel.setFilterEffect(filterEffect);
        frequencyResponsePanel.setMinimumSize(new Dimension(200, 120));

        // Wrap graph in a panel with padding
        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBackground(DarkTheme.BG_LIGHT);
        graphPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        graphPanel.add(frequencyResponsePanel, BorderLayout.CENTER);

        // Controls on WEST (fixed width), graph in CENTER (expands to fill)
        controlsPanel.add(leftScroll, BorderLayout.WEST);
        controlsPanel.add(graphPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Build controls for GraphicEQEffect with vertical sliders for each band.
     */
    private void buildGraphicEQControls(GraphicEQEffect graphicEQ) {
        controlsPanel.setLayout(new BorderLayout(CONTROL_GAP, 0));

        // Left side: Band sliders + Q and Output knobs
        JPanel leftPanel = new JPanel(new BorderLayout(8, 0));
        leftPanel.setBackground(DarkTheme.BG_LIGHT);

        // Sliders panel for 10 bands
        JPanel slidersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        slidersPanel.setBackground(DarkTheme.BG_LIGHT);

        for (int i = 0; i < GraphicEQEffect.NUM_BANDS; i++) {
            JPanel bandStrip = createGraphicEQBandStrip(graphicEQ, i);
            slidersPanel.add(bandStrip);
        }

        // Controls panel for Q and Output
        JPanel controlsRightPanel = new JPanel();
        controlsRightPanel.setLayout(new BoxLayout(controlsRightPanel, BoxLayout.Y_AXIS));
        controlsRightPanel.setBackground(DarkTheme.BG_MEDIUM);
        controlsRightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BG_LIGHTER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Q knob
        for (Parameter param : graphicEQ.getParameters()) {
            if ("q".equals(param.getId())) {
                JRotaryKnob qKnob = (JRotaryKnob) createControlForParameter(param);
                qKnob.setLabel("Q");
                qKnob.setArcForegroundColor(DarkTheme.ACCENT_PRIMARY);
                qKnob.setAlignmentX(Component.CENTER_ALIGNMENT);
                controlsRightPanel.add(qKnob);
                parameterControls.put(param.getId(), qKnob);
                addGraphicEQParameterListener(qKnob);
                break;
            }
        }

        controlsRightPanel.add(Box.createVerticalStrut(16));

        // Output knob
        for (Parameter param : graphicEQ.getParameters()) {
            if ("output".equals(param.getId())) {
                JRotaryKnob outputKnob = (JRotaryKnob) createControlForParameter(param);
                outputKnob.setLabel("Output");
                outputKnob.setArcForegroundColor(DarkTheme.ACCENT_SUCCESS);
                outputKnob.setAlignmentX(Component.CENTER_ALIGNMENT);
                controlsRightPanel.add(outputKnob);
                parameterControls.put(param.getId(), outputKnob);
                addGraphicEQParameterListener(outputKnob);
                break;
            }
        }

        controlsRightPanel.add(Box.createVerticalGlue());

        // Flatten button
        JButton flattenBtn = new JButton("FLAT");
        flattenBtn.setFont(DarkTheme.FONT_BOLD.deriveFont(10f));
        flattenBtn.setForeground(DarkTheme.TEXT_PRIMARY);
        flattenBtn.setBackground(DarkTheme.BG_LIGHT);
        flattenBtn.setFocusPainted(false);
        flattenBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        flattenBtn.setMaximumSize(new Dimension(60, 25));
        flattenBtn.setToolTipText("Reset all bands to 0 dB (flat response)");
        flattenBtn.addActionListener(e -> {
            graphicEQ.flatten();
            refresh();
            if (graphicEQResponsePanel != null) {
                graphicEQResponsePanel.repaint();
            }
        });
        controlsRightPanel.add(flattenBtn);

        leftPanel.add(slidersPanel, BorderLayout.CENTER);
        leftPanel.add(controlsRightPanel, BorderLayout.EAST);

        // Right side: EQ response graph
        graphicEQResponsePanel = new GraphicEQResponsePanel();
        graphicEQResponsePanel.setGraphicEQ(graphicEQ);
        graphicEQResponsePanel.setMinimumSize(new Dimension(200, 120));

        JPanel graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBackground(DarkTheme.BG_LIGHT);
        graphPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        graphPanel.add(graphicEQResponsePanel, BorderLayout.CENTER);

        // Left controls take what they need, graph fills remaining space
        controlsPanel.add(leftPanel, BorderLayout.WEST);
        controlsPanel.add(graphPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Build controls for LooperEffect with file chooser and action buttons.
     */
    private void buildLooperControls(LooperEffect looper) {
        controlsPanel.setLayout(new BorderLayout(CONTROL_GAP, CONTROL_GAP));

        // Top panel: File selection
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filePanel.setBackground(DarkTheme.BG_MEDIUM);
        filePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel fileLabel = new JLabel("Loop File:");
        fileLabel.setFont(DarkTheme.FONT_BOLD);
        fileLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        filePanel.add(fileLabel);

        // File path text field
        JTextField filePathField = new JTextField(30);
        filePathField.setText(looper.getWavFilePath());
        filePathField.setEditable(false);
        filePathField.setBackground(DarkTheme.BG_LIGHT);
        filePathField.setForeground(DarkTheme.TEXT_PRIMARY);
        filePathField.setToolTipText("Path to WAV file to use as loop base");
        filePanel.add(filePathField);

        // Browse button
        JButton browseButton = new JButton("Browse...");
        browseButton.setFont(DarkTheme.FONT_SMALL);
        browseButton.setForeground(DarkTheme.TEXT_PRIMARY);
        browseButton.setBackground(DarkTheme.BG_LIGHT);
        browseButton.setFocusPainted(false);
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select WAV File for Loop");
            chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));

            // Start from last directory if available
            String currentPath = looper.getWavFilePath();
            if (currentPath != null && !currentPath.isEmpty()) {
                File current = new File(currentPath);
                if (current.getParentFile() != null && current.getParentFile().exists()) {
                    chooser.setCurrentDirectory(current.getParentFile());
                }
            }

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                String path = selectedFile.getAbsolutePath();
                filePathField.setText(path);
                looper.setWavFile(path, looper.isAutoPlay());
                firePropertyChange("parameterChanged", null, looper);
            }
        });
        filePanel.add(browseButton);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.setFont(DarkTheme.FONT_SMALL);
        clearButton.setForeground(DarkTheme.ACCENT_ERROR);
        clearButton.setBackground(DarkTheme.BG_LIGHT);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> {
            filePathField.setText("");
            looper.setWavFile("", false);
            firePropertyChange("parameterChanged", null, looper);
        });
        filePanel.add(clearButton);

        // Auto-play checkbox
        JCheckBox autoPlayCheck = new JCheckBox("Auto-play on Start");
        autoPlayCheck.setSelected(looper.isAutoPlay());
        autoPlayCheck.setFont(DarkTheme.FONT_SMALL);
        autoPlayCheck.setForeground(DarkTheme.TEXT_PRIMARY);
        autoPlayCheck.setBackground(DarkTheme.BG_MEDIUM);
        autoPlayCheck.setToolTipText("Automatically start playing the loop when the rig starts");
        autoPlayCheck.addActionListener(e -> {
            looper.setWavFile(looper.getWavFilePath(), autoPlayCheck.isSelected());
            firePropertyChange("parameterChanged", null, looper);
        });
        filePanel.add(autoPlayCheck);

        controlsPanel.add(filePanel, BorderLayout.NORTH);

        // Center panel: Standard parameter controls
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));
        paramsPanel.setBackground(DarkTheme.BG_LIGHT);

        // Add standard controls for level and feedback
        for (Parameter param : looper.getParameters()) {
            // Skip action buttons, they're handled separately
            if (param.getType() == ParameterType.BOOLEAN &&
                (param.getId().equals("rec") || param.getId().equals("stop") ||
                 param.getId().equals("overdub") || param.getId().equals("pause") ||
                 param.getId().equals("delete") || param.getId().equals("mute") ||
                 param.getId().equals("save"))) {
                continue;
            }

            JComponent control = createControlForParameter(param);
            if (control != null) {
                JPanel wrapper = createControlWrapper(param, control);
                paramsPanel.add(wrapper);
                parameterControls.put(param.getId(), control);
            }
        }

        // Add action buttons panel
        JPanel buttonsPanel = createLooperButtonsPanel(looper);
        paramsPanel.add(buttonsPanel);

        controlsPanel.add(paramsPanel, BorderLayout.CENTER);

        // Bottom panel: Status info
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        statusPanel.setBackground(DarkTheme.BG_MEDIUM);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel statusLabel = new JLabel("Status: " + looper.getState());
        statusLabel.setFont(DarkTheme.FONT_SMALL);
        statusLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        statusPanel.add(statusLabel);

        JLabel layersLabel = new JLabel("Layers: " + looper.getLayerCount());
        layersLabel.setFont(DarkTheme.FONT_SMALL);
        layersLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        statusPanel.add(layersLabel);

        JLabel lengthLabel = new JLabel(String.format("Length: %.1fs", looper.getLoopLengthSeconds()));
        lengthLabel.setFont(DarkTheme.FONT_SMALL);
        lengthLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        statusPanel.add(lengthLabel);

        controlsPanel.add(statusPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    /**
     * Create action buttons panel for looper.
     */
    private JPanel createLooperButtonsPanel(LooperEffect looper) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 4, 4, 4));
        panel.setBackground(DarkTheme.BG_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // REC button
        JButton recBtn = createLooperButton("REC", DarkTheme.ACCENT_ERROR,
                "Start recording a new loop");
        recBtn.addActionListener(e -> looper.triggerRec());
        panel.add(recBtn);

        // STOP button
        JButton stopBtn = createLooperButton("STOP", DarkTheme.ACCENT_WARNING,
                "Stop recording and start playback");
        stopBtn.addActionListener(e -> looper.triggerStop());
        panel.add(stopBtn);

        // OVERDUB button
        JButton overdubBtn = createLooperButton("OVERDUB", DarkTheme.ACCENT_PRIMARY,
                "Record a new layer while playing");
        overdubBtn.addActionListener(e -> looper.triggerOverdub());
        panel.add(overdubBtn);

        // PAUSE button
        JButton pauseBtn = createLooperButton("PAUSE", DarkTheme.TEXT_SECONDARY,
                "Pause/resume loop playback");
        pauseBtn.addActionListener(e -> looper.triggerPause());
        panel.add(pauseBtn);

        // DELETE button
        JButton deleteBtn = createLooperButton("DELETE", DarkTheme.ACCENT_ERROR.darker(),
                "Delete the last recorded layer");
        deleteBtn.addActionListener(e -> looper.triggerDelete());
        panel.add(deleteBtn);

        // MUTE button
        JButton muteBtn = createLooperButton("MUTE", new Color(0x888888),
                "Mute loop output");
        muteBtn.addActionListener(e -> looper.setMuted(!looper.isMuted()));
        panel.add(muteBtn);

        // SAVE button
        JButton saveBtn = createLooperButton("SAVE", DarkTheme.ACCENT_SUCCESS,
                "Export all layers to WAV file");
        saveBtn.addActionListener(e -> saveLoopWithDialog(looper));
        panel.add(saveBtn);

        return panel;
    }

    /**
     * Show dialog to save loop with custom name.
     */
    private void saveLoopWithDialog(LooperEffect looper) {
        if (looper.getLayerCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Nothing to save. Record a loop first.",
                    "Save Loop",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generate default name with timestamp
        String defaultName = "loop_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        String name = (String) JOptionPane.showInputDialog(this,
                "Enter a name for the loop:\n(Will be saved to " + LooperEffect.getLoopsDirectory() + ")",
                "Save Loop",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultName);

        if (name != null && !name.trim().isEmpty()) {
            java.nio.file.Path savedPath = looper.saveToFile(name);
            if (savedPath != null) {
                JOptionPane.showMessageDialog(this,
                        "Loop saved to:\n" + savedPath,
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to save loop. Check console for details.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Create a styled looper button.
     */
    private JButton createLooperButton(String text, Color color, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(DarkTheme.FONT_BOLD.deriveFont(10f));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setPreferredSize(new Dimension(70, 35));
        btn.setToolTipText(tooltip);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return btn;
    }

    /**
     * Build controls for NoiseGateEffect with visual metering.
     */
    private void buildNoiseGateControls(NoiseGateEffect noiseGate) {
        controlsPanel.setLayout(new BorderLayout(CONTROL_GAP, 0));

        // Left side: Standard parameter controls
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));
        paramsPanel.setBackground(DarkTheme.BG_LIGHT);

        for (Parameter param : noiseGate.getParameters()) {
            JComponent control = createControlForParameter(param);
            if (control != null) {
                JPanel wrapper = createControlWrapper(param, control);
                paramsPanel.add(wrapper);
                parameterControls.put(param.getId(), control);
            }
        }

        // Right side: Level meters panel
        noiseGateMeterPanel = new NoiseGateMeterPanel();
        noiseGateMeterPanel.setNoiseGate(noiseGate);
        noiseGateMeterPanel.setPreferredSize(new Dimension(200, 160));

        // Wrap meters in a panel with title
        JPanel meterContainer = new JPanel(new BorderLayout());
        meterContainer.setBackground(DarkTheme.BG_MEDIUM);
        meterContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BG_LIGHTER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel metersLabel = new JLabel("GATE ACTIVITY");
        metersLabel.setFont(DarkTheme.FONT_BOLD.deriveFont(10f));
        metersLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        metersLabel.setHorizontalAlignment(SwingConstants.CENTER);
        meterContainer.add(metersLabel, BorderLayout.NORTH);
        meterContainer.add(noiseGateMeterPanel, BorderLayout.CENTER);

        // Add legend panel
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        legendPanel.setOpaque(false);

        JLabel threshLabel = new JLabel("\u25B6 Threshold");
        threshLabel.setFont(DarkTheme.FONT_SMALL.deriveFont(8f));
        threshLabel.setForeground(new Color(0xe94560));
        legendPanel.add(threshLabel);

        meterContainer.add(legendPanel, BorderLayout.SOUTH);

        // Layout: parameters on left, meters on right
        controlsPanel.add(paramsPanel, BorderLayout.CENTER);
        controlsPanel.add(meterContainer, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    /**
     * Build controls for SettingsEffect with TAP tempo button.
     */
    private void buildSettingsControls(SettingsEffect settings) {
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));

        // BPM display knob
        Parameter bpmParam = settings.getParameter("bpm");
        JRotaryKnob bpmKnob = new JRotaryKnob(
                bpmParam.getMinValue(),
                bpmParam.getMaxValue(),
                bpmParam.getDefaultValue(),
                "BPM"
        );
        bpmKnob.setValue(bpmParam.getTargetValue());
        bpmKnob.setLabel("Tempo");
        bpmKnob.setArcForegroundColor(DarkTheme.ACCENT_PRIMARY);
        bpmKnob.setToolTipText("Current tempo in BPM. Use TAP button or turn knob to set.");

        // Update parameter when knob changes
        bpmKnob.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                settings.setBpm((float) (double) e.getNewValue());
                firePropertyChange("parameterChanged", null, bpmParam);
            }
        });

        JPanel bpmWrapper = createControlWrapper(bpmParam, bpmKnob);
        controlsPanel.add(bpmWrapper);
        parameterControls.put(bpmParam.getId(), bpmKnob);

        // Quarter note duration display (create first so tap can update it)
        JPanel durationPanel = new JPanel();
        durationPanel.setLayout(new BoxLayout(durationPanel, BoxLayout.Y_AXIS));
        durationPanel.setOpaque(false);
        durationPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel durationTitle = new JLabel("Note Durations");
        durationTitle.setFont(DarkTheme.FONT_BOLD);
        durationTitle.setForeground(DarkTheme.TEXT_PRIMARY);
        durationTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        durationPanel.add(durationTitle);
        durationPanel.add(Box.createVerticalStrut(8));

        // Duration labels that update with BPM
        JLabel quarterLabel = new JLabel();
        JLabel eighthLabel = new JLabel();
        JLabel sixteenthLabel = new JLabel();
        JLabel dottedEighthLabel = new JLabel();

        for (JLabel label : new JLabel[]{quarterLabel, eighthLabel, sixteenthLabel, dottedEighthLabel}) {
            label.setFont(DarkTheme.FONT_SMALL);
            label.setForeground(DarkTheme.TEXT_SECONDARY);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            durationPanel.add(label);
        }

        // Update duration labels function
        Runnable updateDurations = () -> {
            quarterLabel.setText(String.format("1/4 note: %.0f ms", settings.getQuarterNoteMs()));
            eighthLabel.setText(String.format("1/8 note: %.0f ms", settings.getEighthNoteMs()));
            sixteenthLabel.setText(String.format("1/16 note: %.0f ms", settings.getSixteenthNoteMs()));
            dottedEighthLabel.setText(String.format("Dotted 1/8: %.0f ms", settings.getDottedEighthNoteMs()));
        };
        updateDurations.run();

        // TAP button
        JPanel tapPanel = new JPanel();
        tapPanel.setLayout(new BoxLayout(tapPanel, BoxLayout.Y_AXIS));
        tapPanel.setOpaque(false);

        // Spacer to align with knob
        tapPanel.add(Box.createVerticalStrut(15));

        JButton tapButton = new JButton("TAP");
        tapButton.setFont(DarkTheme.FONT_BOLD.deriveFont(16f));
        tapButton.setForeground(Color.WHITE);
        tapButton.setBackground(DarkTheme.ACCENT_PRIMARY);
        tapButton.setFocusPainted(false);
        tapButton.setBorderPainted(true);
        tapButton.setPreferredSize(new Dimension(80, 60));
        tapButton.setMaximumSize(new Dimension(80, 60));
        tapButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        tapButton.setToolTipText("Tap repeatedly to set tempo");
        tapButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.ACCENT_PRIMARY.darker(), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        // Tap handling - works without audio engine running
        tapButton.addActionListener(e -> {
            // Call tap to record timestamp and calculate BPM
            settings.tap();

            // Read the updated BPM value (use getTargetValue, not getValue)
            float currentBpm = bpmParam.getTargetValue();

            // Update UI
            bpmKnob.setValue(currentBpm);
            updateDurations.run();
        });

        tapPanel.add(tapButton);

        // Label below button
        JLabel tapLabel = new JLabel("Tap Tempo");
        tapLabel.setFont(DarkTheme.FONT_SMALL);
        tapLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        tapLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tapPanel.add(Box.createVerticalStrut(4));
        tapPanel.add(tapLabel);

        controlsPanel.add(tapPanel);

        // Update durations when BPM knob changes manually
        bpmKnob.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                updateDurations.run();
            }
        });

        controlsPanel.add(durationPanel);

        revalidate();
        repaint();
    }

    /**
     * Create a vertical strip for a single graphic EQ band with fader.
     */
    private JPanel createGraphicEQBandStrip(GraphicEQEffect graphicEQ, int band) {
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
        strip.setBackground(DarkTheme.BG_LIGHT);
        strip.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        // Find the gain parameter for this band
        Parameter gainParam = null;
        for (Parameter param : graphicEQ.getParameters()) {
            if (("gain" + (band + 1)).equals(param.getId())) {
                gainParam = param;
                break;
            }
        }

        if (gainParam == null) return strip;

        // Mixer-style fader
        JFaderSlider fader = new JFaderSlider(-12, 12, 0, "dB");
        fader.setValue(gainParam.getTargetValue());
        fader.setAlignmentX(Component.CENTER_ALIGNMENT);

        // First band is HP, last band is LP - different behavior
        if (band == 0) {
            fader.setBipolar(false);  // HP doesn't have boost/cut
            fader.setToolTipText("High-Pass @ " + GraphicEQEffect.BAND_LABELS[band] + " Hz: Controls resonance (Q)");
        } else if (band == GraphicEQEffect.NUM_BANDS - 1) {
            fader.setBipolar(false);  // LP doesn't have boost/cut
            fader.setToolTipText("Low-Pass @ " + GraphicEQEffect.BAND_LABELS[band] + " Hz: Controls resonance (Q)");
        } else {
            fader.setBipolar(true);
            fader.setToolTipText(GraphicEQEffect.BAND_LABELS[band] + " Hz: Boost or cut this frequency band");
        }

        // Update parameter on change
        final Parameter param = gainParam;
        fader.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                float dB = (float) (double) e.getNewValue();
                param.setValue(dB);

                // Update graph
                if (graphicEQResponsePanel != null) {
                    graphicEQResponsePanel.repaint();
                }

                firePropertyChange("parameterChanged", null, param);
            }
        });

        strip.add(fader);
        strip.add(Box.createVerticalStrut(2));

        // Frequency label at bottom - indicate HP/LP for first/last bands
        String labelText;
        Color labelColor;
        if (band == 0) {
            labelText = "HP";
            labelColor = new Color(0xFF6B6B);  // Red for HP
        } else if (band == GraphicEQEffect.NUM_BANDS - 1) {
            labelText = "LP";
            labelColor = new Color(0x4ECDC4);  // Teal for LP
        } else {
            labelText = GraphicEQEffect.BAND_LABELS[band];
            labelColor = DarkTheme.TEXT_SECONDARY;
        }

        JLabel freqLabel = new JLabel(labelText);
        freqLabel.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
        freqLabel.setForeground(labelColor);
        freqLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        strip.add(freqLabel);

        parameterControls.put(gainParam.getId(), fader);

        return strip;
    }

    /**
     * Add change listener for graphic EQ parameters to update graph.
     */
    private void addGraphicEQParameterListener(JComponent control) {
        if (control instanceof JRotaryKnob knob) {
            knob.addPropertyChangeListener("value", e -> {
                if (graphicEQResponsePanel != null) {
                    graphicEQResponsePanel.repaint();
                }
            });
        }
    }

    /**
     * Create a row of controls for a single filter band.
     */
    private JPanel createFilterBandRow(FilterEffect filterEffect, int band) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(band % 2 == 0 ? DarkTheme.BG_LIGHT : DarkTheme.BG_MEDIUM);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // Band label
        JLabel bandLabel = new JLabel("Band " + (band + 1));
        bandLabel.setFont(DarkTheme.FONT_BOLD.deriveFont(10f));
        bandLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        bandLabel.setPreferredSize(new Dimension(45, 20));
        row.add(bandLabel);

        // Find parameters for this band
        String[] paramIds = {
            "type" + (band + 1),
            "freq" + (band + 1),
            "q" + (band + 1),
            "gain" + (band + 1)
        };

        for (String paramId : paramIds) {
            for (Parameter param : filterEffect.getParameters()) {
                if (paramId.equals(param.getId())) {
                    JComponent control = createControlForParameter(param);
                    if (control != null) {
                        JPanel wrapper = createCompactControlWrapper(param, control);
                        row.add(wrapper);
                        parameterControls.put(param.getId(), control);

                        // Add change listener for real-time graph update
                        addFilterParameterListener(control);
                    }
                    break;
                }
            }
        }

        return row;
    }

    /**
     * Create a compact wrapper for filter controls.
     */
    private JPanel createCompactControlWrapper(Parameter param, JComponent control) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        String shortName = getShortParamName(param.getName());

        // For JRotaryKnob, set short label directly on the knob (it displays above)
        // The knob already shows its value below, so no extra label needed
        if (control instanceof JRotaryKnob knob) {
            knob.setLabel(shortName);
            control.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrapper.add(control);
        } else {
            // For other controls (combo boxes, etc.), add label below
            control.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrapper.add(control);

            JLabel label = new JLabel(shortName);
            label.setFont(DarkTheme.FONT_SMALL.deriveFont(9f));
            label.setForeground(DarkTheme.TEXT_SECONDARY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrapper.add(label);
        }

        return wrapper;
    }

    /**
     * Get short parameter name for compact display.
     */
    private String getShortParamName(String fullName) {
        if (fullName.contains("Type")) return "Type";
        if (fullName.contains("Freq")) return "Freq";
        if (fullName.contains(" Q")) return "Q";
        if (fullName.contains("Gain")) return "Gain";
        if (fullName.contains("Output")) return "Output";
        return fullName;
    }

    /**
     * Add change listener to update frequency response graph.
     */
    private void addFilterParameterListener(JComponent control) {
        if (control instanceof JRotaryKnob knob) {
            knob.addPropertyChangeListener("value", e -> {
                if (frequencyResponsePanel != null) {
                    frequencyResponsePanel.repaint();
                }
            });
        } else if (control instanceof JComboBox<?> combo) {
            combo.addActionListener(e -> {
                if (frequencyResponsePanel != null) {
                    frequencyResponsePanel.repaint();
                }
            });
        }
    }

    /**
     * Create a TAP tempo button for the Delay effect.
     */
    private JPanel createTapTempoButton() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Spacer to align with knob label position
        panel.add(Box.createVerticalStrut(20));

        JButton tapButton = new JButton("TAP");
        tapButton.setFont(DarkTheme.FONT_BOLD);
        tapButton.setForeground(DarkTheme.TEXT_PRIMARY);
        tapButton.setBackground(DarkTheme.ACCENT_PRIMARY);
        tapButton.setFocusPainted(false);
        tapButton.setBorderPainted(true);
        tapButton.setPreferredSize(new Dimension(60, 50));
        tapButton.setMaximumSize(new Dimension(60, 50));
        tapButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        tapButton.setToolTipText("Tap repeatedly to set BPM from your rhythm. Average of last " + TAP_TEMPO_MAX_TAPS + " taps.");

        // Style the button
        tapButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.ACCENT_PRIMARY.darker(), 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Mouse listener for visual feedback and tap handling
        tapButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                tapButton.setBackground(DarkTheme.ACCENT_PRIMARY.brighter());
                handleTap();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                tapButton.setBackground(DarkTheme.ACCENT_PRIMARY);
            }
        });

        panel.add(tapButton);

        // Info label
        JLabel infoLabel = new JLabel("Tempo");
        infoLabel.setFont(DarkTheme.FONT_SMALL);
        infoLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalStrut(4));
        panel.add(infoLabel);

        return panel;
    }

    /**
     * Handle a tap event for tap tempo.
     * Records the time, calculates average interval, and updates BPM.
     */
    private void handleTap() {
        long now = System.currentTimeMillis();

        // Check if we should reset (timeout since last tap)
        if (!tapTimes.isEmpty()) {
            long lastTap = tapTimes.getLast();
            if (now - lastTap > TAP_TEMPO_TIMEOUT_MS) {
                tapTimes.clear();
            }
        }

        // Add the new tap time
        tapTimes.add(now);

        // Keep only the last N taps
        while (tapTimes.size() > TAP_TEMPO_MAX_TAPS) {
            tapTimes.removeFirst();
        }

        // Need at least 2 taps to calculate BPM
        if (tapTimes.size() < 2) {
            return;
        }

        // Calculate average interval between taps
        long totalInterval = 0;
        int intervalCount = 0;

        Long previousTime = null;
        for (Long tapTime : tapTimes) {
            if (previousTime != null) {
                totalInterval += (tapTime - previousTime);
                intervalCount++;
            }
            previousTime = tapTime;
        }

        if (intervalCount == 0) return;

        double avgIntervalMs = (double) totalInterval / intervalCount;

        // Convert to BPM: 60000 ms / interval = beats per minute
        double bpm = 60000.0 / avgIntervalMs;

        // Clamp to valid range (matching delay parameter: 40-240 BPM)
        bpm = Math.max(40.0, Math.min(240.0, bpm));

        // Update the BPM parameter and knob
        if (currentEffect instanceof DelayEffect delayEffect) {
            delayEffect.setBpm((float) bpm);
            if (bpmKnob != null) {
                bpmKnob.setValue(bpm);
            }
            firePropertyChange("parameterChanged", null, currentEffect.getParameter("bpm"));
        }
    }

    /**
     * Build controls for MixerNode channels.
     */
    private void buildMixerControls(MixerNode mixerNode) {
        controlsPanel.removeAll();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, CONTROL_GAP, CONTROL_GAP));
        parameterControls.clear();

        int numInputs = mixerNode.getNumInputs();

        // Create channel strip for each input
        for (int ch = 0; ch < numInputs; ch++) {
            JPanel channelStrip = createMixerChannelStrip(mixerNode, ch);
            controlsPanel.add(channelStrip);
        }

        // Add master level control
        JPanel masterPanel = createMasterLevelControl(mixerNode);
        controlsPanel.add(masterPanel);

        // Add stereo mode selector
        JPanel stereoModePanel = createStereoModeControl(mixerNode);
        controlsPanel.add(stereoModePanel);

        revalidate();
        repaint();
    }

    /**
     * Create stereo mode selector for mixer.
     */
    private JPanel createStereoModeControl(MixerNode mixer) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkTheme.BG_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Label
        JLabel label = new JLabel("MODE");
        label.setFont(DarkTheme.FONT_BOLD);
        label.setForeground(DarkTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));

        // Stereo mode combo box
        String[] modes = {"Mono", "Stereo"};
        JComboBox<String> modeCombo = new JComboBox<>(modes);
        modeCombo.setBackground(DarkTheme.BG_LIGHT);
        modeCombo.setForeground(DarkTheme.TEXT_PRIMARY);
        modeCombo.setPreferredSize(new Dimension(80, 28));
        modeCombo.setMaximumSize(new Dimension(80, 28));
        modeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        modeCombo.setToolTipText("Mono: pan affects level, Stereo: true L/R panning with sum output");

        // Set current mode
        switch (mixer.getStereoMode()) {
            case MONO -> modeCombo.setSelectedIndex(0);
            case STEREO -> modeCombo.setSelectedIndex(1);
        }

        modeCombo.addActionListener(e -> {
            MixerNode.StereoMode mode = switch (modeCombo.getSelectedIndex()) {
                case 0 -> MixerNode.StereoMode.MONO;
                default -> MixerNode.StereoMode.STEREO;
            };
            mixer.setStereoMode(mode);
            firePropertyChange("parameterChanged", null, mixer);
        });

        panel.add(modeCombo);
        panel.add(Box.createVerticalStrut(8));

        // Info label
        JLabel infoLabel = new JLabel("Output");
        infoLabel.setFont(DarkTheme.FONT_SMALL);
        infoLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(infoLabel);

        return panel;
    }

    /**
     * Create a channel strip panel for a mixer input.
     */
    private JPanel createMixerChannelStrip(MixerNode mixer, int channel) {
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
        strip.setBackground(DarkTheme.BG_MEDIUM);
        strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Channel label
        JLabel label = new JLabel("CH " + (channel + 1));
        label.setFont(DarkTheme.FONT_BOLD);
        label.setForeground(DarkTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        strip.add(label);
        strip.add(Box.createVerticalStrut(8));

        // Gain knob
        JRotaryKnob gainKnob = new JRotaryKnob(-24.0, 6.0, 0.0, "dB");
        gainKnob.setValue(linearToDb(mixer.getLevel(channel)));
        gainKnob.setLabel("Gain");
        gainKnob.setArcForegroundColor(DarkTheme.ACCENT_SUCCESS);
        gainKnob.setPreferredSize(new Dimension(CONTROL_WIDTH, 80));
        gainKnob.setAlignmentX(Component.CENTER_ALIGNMENT);
        gainKnob.setToolTipText("Input level for this channel. 0 dB = unity gain, boost or cut as needed.");

        final int ch = channel;
        gainKnob.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                float db = (float) (double) e.getNewValue();
                mixer.setLevelDb(ch, db);
                firePropertyChange("parameterChanged", null, mixer);
            }
        });
        strip.add(gainKnob);
        strip.add(Box.createVerticalStrut(8));

        // Pan knob
        JRotaryKnob panKnob = new JRotaryKnob(-100.0, 100.0, 0.0, "%");
        panKnob.setValue(mixer.getPan(channel) * 100);
        panKnob.setLabel("Pan");
        panKnob.setBipolar(true);  // Arc from center
        panKnob.setArcForegroundColor(DarkTheme.ACCENT_PRIMARY);
        panKnob.setPreferredSize(new Dimension(CONTROL_WIDTH, 80));
        panKnob.setAlignmentX(Component.CENTER_ALIGNMENT);
        panKnob.setToolTipText("Stereo position: -100% = hard left, 0% = center, +100% = hard right.");

        panKnob.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                float panPercent = (float) (double) e.getNewValue();
                mixer.setPan(ch, panPercent / 100.0f);
                firePropertyChange("parameterChanged", null, mixer);
            }
        });
        strip.add(panKnob);
        strip.add(Box.createVerticalStrut(8));

        // Mute toggle
        JLedToggle muteToggle = new JLedToggle("Mute");
        muteToggle.setSelected(mixer.isMuted(channel));
        muteToggle.setLedOnColor(DarkTheme.ACCENT_ERROR);
        muteToggle.setToolTipText("Silences this channel without changing the gain setting.");
        muteToggle.setAlignmentX(Component.CENTER_ALIGNMENT);

        muteToggle.addPropertyChangeListener("selected", e -> {
            if ("selected".equals(e.getPropertyName())) {
                mixer.setMute(ch, (Boolean) e.getNewValue());
                firePropertyChange("parameterChanged", null, mixer);
            }
        });
        strip.add(muteToggle);

        return strip;
    }

    /**
     * Create master level control for mixer.
     */
    private JPanel createMasterLevelControl(MixerNode mixer) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkTheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.ACCENT_PRIMARY, 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Master label
        JLabel label = new JLabel("MASTER");
        label.setFont(DarkTheme.FONT_BOLD);
        label.setForeground(DarkTheme.ACCENT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));

        // Master level knob
        JRotaryKnob masterKnob = new JRotaryKnob(-24.0, 6.0, 0.0, "dB");
        masterKnob.setValue(linearToDb(mixer.getMasterLevel()));
        masterKnob.setLabel("Level");
        masterKnob.setArcForegroundColor(DarkTheme.ACCENT_WARNING);
        masterKnob.setPreferredSize(new Dimension(CONTROL_WIDTH, 80));
        masterKnob.setAlignmentX(Component.CENTER_ALIGNMENT);
        masterKnob.setToolTipText("Overall output level of the mixer. Controls the combined signal from all channels.");

        masterKnob.addPropertyChangeListener("value", e -> {
            if ("value".equals(e.getPropertyName())) {
                float db = (float) (double) e.getNewValue();
                mixer.setMasterLevelDb(db);
                firePropertyChange("parameterChanged", null, mixer);
            }
        });
        panel.add(masterKnob);

        return panel;
    }

    /**
     * Convert linear gain to dB.
     */
    private double linearToDb(float linear) {
        if (linear <= 0) return -60.0;
        return 20.0 * Math.log10(linear);
    }

    /**
     * Create appropriate control for a parameter type.
     */
    private JComponent createControlForParameter(Parameter param) {
        return switch (param.getType()) {
            case FLOAT -> createKnobControl(param);
            case INTEGER -> createSliderControl(param);
            case BOOLEAN -> createToggleControl(param);
            case CHOICE -> createComboControl(param);
        };
    }

    /**
     * Create a rotary knob for float parameters.
     */
    private JRotaryKnob createKnobControl(Parameter param) {
        JRotaryKnob knob = new JRotaryKnob(
                param.getMinValue(),
                param.getMaxValue(),
                param.getDefaultValue(),
                param.getUnit()
        );
        knob.setValue(param.getTargetValue());
        knob.setLabel(param.getName());

        // Set tooltip from parameter description
        if (param.getDescription() != null && !param.getDescription().isEmpty()) {
            knob.setToolTipText(param.getDescription());
        }

        // Color and behavior based on common parameter names
        String name = param.getName().toLowerCase();
        String id = param.getId().toLowerCase();
        if (name.contains("gain") || name.contains("level") || name.contains("volume")) {
            knob.setArcForegroundColor(DarkTheme.ACCENT_SUCCESS);
        } else if (name.contains("drive") || name.contains("distortion")) {
            knob.setArcForegroundColor(DarkTheme.CATEGORY_DISTORTION);
        } else if (name.contains("mix") || name.contains("wet") || name.contains("dry")) {
            knob.setArcForegroundColor(DarkTheme.ACCENT_PRIMARY);
        }

        // Set bipolar mode for pan controls (arc from center)
        if (name.contains("pan") || id.contains("pan") || name.contains("balance")) {
            knob.setBipolar(true);
        }

        // Bind to parameter
        PropertyChangeListener listener = e -> {
            if ("value".equals(e.getPropertyName())) {
                param.setValue((float) (double) e.getNewValue());
                firePropertyChange("parameterChanged", null, param);
            }
        };
        knob.addPropertyChangeListener("value", listener);
        bindingListeners.add(listener);

        return knob;
    }

    /**
     * Create a slider for integer parameters.
     */
    private JSlider createSliderControl(Parameter param) {
        JSlider slider = new JSlider(
                (int) param.getMinValue(),
                (int) param.getMaxValue(),
                (int) param.getTargetValue()
        );
        slider.setBackground(DarkTheme.BG_LIGHT);
        slider.setForeground(DarkTheme.TEXT_PRIMARY);
        slider.setPreferredSize(new Dimension(100, 30));

        // Set tooltip from parameter description
        if (param.getDescription() != null && !param.getDescription().isEmpty()) {
            slider.setToolTipText(param.getDescription());
        }

        // Paint ticks for large ranges
        int range = (int) (param.getMaxValue() - param.getMinValue());
        if (range > 10) {
            slider.setMajorTickSpacing(range / 4);
            slider.setPaintTicks(true);
        }

        // Bind to parameter
        ChangeListener listener = e -> {
            if (!slider.getValueIsAdjusting()) {
                param.setValue(slider.getValue());
                firePropertyChange("parameterChanged", null, param);
            }
        };
        slider.addChangeListener(listener);

        return slider;
    }

    /**
     * Create a toggle for boolean parameters.
     */
    private JLedToggle createToggleControl(Parameter param) {
        JLedToggle toggle = new JLedToggle(param.getName());
        toggle.setSelected(param.getBooleanValue());

        // Set tooltip from parameter description
        if (param.getDescription() != null && !param.getDescription().isEmpty()) {
            toggle.setToolTipText(param.getDescription());
        }

        // Bind to parameter
        PropertyChangeListener listener = e -> {
            if ("selected".equals(e.getPropertyName())) {
                param.setValue((Boolean) e.getNewValue());
                firePropertyChange("parameterChanged", null, param);
            }
        };
        toggle.addPropertyChangeListener("selected", listener);
        bindingListeners.add(listener);

        return toggle;
    }

    /**
     * Create a combo box for choice parameters.
     */
    private JComboBox<String> createComboControl(Parameter param) {
        String[] choices = param.getChoices();
        JComboBox<String> combo = new JComboBox<>(choices);
        combo.setSelectedIndex(param.getChoiceIndex());
        combo.setBackground(DarkTheme.BG_MEDIUM);
        combo.setForeground(DarkTheme.TEXT_PRIMARY);
        combo.setFont(DarkTheme.FONT_REGULAR);
        combo.setPreferredSize(new Dimension(90, 28));
        combo.setMinimumSize(new Dimension(80, 28));
        combo.setMaximumSize(new Dimension(120, 28));

        // Set tooltip from parameter description
        if (param.getDescription() != null && !param.getDescription().isEmpty()) {
            combo.setToolTipText(param.getDescription());
        }

        // Bind to parameter
        combo.addActionListener(e -> {
            param.setChoice(combo.getSelectedIndex());
            firePropertyChange("parameterChanged", null, param);
        });

        return combo;
    }

    /**
     * Create a wrapper panel with label for a control.
     */
    private JPanel createControlWrapper(Parameter param, JComponent control) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        // For knobs, the label is already included
        if (control instanceof JRotaryKnob) {
            wrapper.add(control);
        } else if (control instanceof JLedToggle) {
            // Toggle has its own label
            wrapper.add(control);
        } else {
            // Add label above control
            JLabel label = new JLabel(param.getName());
            label.setFont(DarkTheme.FONT_SMALL);
            label.setForeground(DarkTheme.TEXT_SECONDARY);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(label);
            wrapper.add(Box.createVerticalStrut(4));

            control.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(control);

            // Add value label for sliders
            if (control instanceof JSlider slider) {
                JLabel valueLabel = new JLabel(formatSliderValue(param, slider.getValue()));
                valueLabel.setFont(DarkTheme.FONT_SMALL);
                valueLabel.setForeground(DarkTheme.TEXT_PRIMARY);
                valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                wrapper.add(valueLabel);

                slider.addChangeListener(e -> {
                    valueLabel.setText(formatSliderValue(param, slider.getValue()));
                });
            }
        }

        return wrapper;
    }

    /**
     * Format slider value for display.
     */
    private String formatSliderValue(Parameter param, int value) {
        String unit = param.getUnit();
        if (unit != null && !unit.isEmpty()) {
            return value + " " + unit;
        }
        return String.valueOf(value);
    }

    /**
     * Clear all parameter bindings.
     */
    private void clearBindings() {
        bindingListeners.clear();
        parameterControls.clear();

        // Stop frequency response panel updates
        if (frequencyResponsePanel != null) {
            frequencyResponsePanel.stopUpdates();
            frequencyResponsePanel = null;
        }

        // Clear graphic EQ panel
        graphicEQResponsePanel = null;

        // Stop noise gate meter panel updates
        if (noiseGateMeterPanel != null) {
            noiseGateMeterPanel.stopUpdates();
            noiseGateMeterPanel = null;
        }

        // Disconnect effect monitor listener
        disconnectMonitorListener();
    }

    /**
     * Refresh the display (update values from parameters).
     */
    public void refresh() {
        if (currentEffect == null) return;

        for (Parameter param : currentEffect.getParameters()) {
            JComponent control = parameterControls.get(param.getId());
            if (control == null) continue;

            if (control instanceof JRotaryKnob knob) {
                knob.setValue(param.getTargetValue());
            } else if (control instanceof JFaderSlider fader) {
                fader.setValue(param.getTargetValue());
            } else if (control instanceof JSlider slider) {
                slider.setValue((int) param.getTargetValue());
            } else if (control instanceof JLedToggle toggle) {
                toggle.setSelected(param.getBooleanValue());
            } else if (control instanceof JComboBox<?> combo) {
                combo.setSelectedIndex(param.getChoiceIndex());
            }
        }

        if (currentNode != null) {
            bypassToggle.setSelected(currentNode.isBypassed());
        }
    }

    /**
     * Get the currently displayed node.
     */
    public ProcessingNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Get the currently displayed effect.
     */
    public AudioEffect getCurrentEffect() {
        return currentEffect;
    }

    /**
     * Toggle between Parameters and Signal waveform view.
     */
    private void toggleSignalView() {
        showingSignal = !showingSignal;

        if (showingSignal) {
            // Switch to signal view
            signalViewToggle.setText("Parameters");
            signalViewToggle.setBackground(DarkTheme.ACCENT_PRIMARY);
            graphTypeCombo.setVisible(true);
            contentLayout.show(contentPanel, CARD_SIGNAL);

            // Start all visualization panels
            startSignalVisualization();

            // Connect monitor listener to receive audio samples
            connectMonitorListener();
        } else {
            // Switch to parameters view
            signalViewToggle.setText("Signal");
            signalViewToggle.setBackground(DarkTheme.BG_LIGHT);
            graphTypeCombo.setVisible(false);
            contentLayout.show(contentPanel, CARD_PARAMETERS);

            // Stop all visualization panels and disconnect listener
            stopSignalVisualization();
            disconnectMonitorListener();
        }
    }

    /**
     * Switch between different signal graph types.
     */
    private void switchGraphType() {
        int selected = graphTypeCombo.getSelectedIndex();
        switch (selected) {
            case 0 -> signalContainerLayout.show(signalContainer, SIGNAL_CARD_WAVEFORM);
            case 1 -> signalContainerLayout.show(signalContainer, SIGNAL_CARD_FREQUENCY);
            case 2 -> signalContainerLayout.show(signalContainer, SIGNAL_CARD_SPECTRUM);
        }
    }

    /**
     * Start all signal visualization panels.
     */
    private void startSignalVisualization() {
        signalWaveformPanel.setEffect(currentEffect);
        signalWaveformPanel.clearBuffers();
        signalWaveformPanel.startUpdates();

        frequencyAnalysisPanel.clearBuffers();
        frequencyAnalysisPanel.startUpdates();

        spectrumBandsPanel.clearBuffers();
        spectrumBandsPanel.startUpdates();
    }

    /**
     * Stop all signal visualization panels.
     */
    private void stopSignalVisualization() {
        signalWaveformPanel.stopUpdates();
        frequencyAnalysisPanel.stopUpdates();
        spectrumBandsPanel.stopUpdates();
    }

    /**
     * Connect monitor listener to current effect node.
     */
    private void connectMonitorListener() {
        if (currentNode instanceof EffectNode effectNode) {
            // Create listener that feeds samples to all visualization panels
            effectMonitorListener = (input, output, frameCount) -> {
                // Feed to waveform panel
                signalWaveformPanel.addSamplesMono(input, output, frameCount);
                // Feed to frequency analysis panel
                frequencyAnalysisPanel.addSamples(input, output, frameCount);
                // Feed to spectrum bands panel
                spectrumBandsPanel.addSamples(input, output, frameCount);
            };
            effectNode.setMonitorListener(effectMonitorListener);
        }
    }

    /**
     * Disconnect monitor listener from current effect node.
     */
    private void disconnectMonitorListener() {
        if (currentNode instanceof EffectNode effectNode) {
            effectNode.setMonitorListener(null);
        }
        effectMonitorListener = null;
    }

    /**
     * Get the signal waveform panel for feeding audio data.
     */
    public SignalWaveformPanel getSignalWaveformPanel() {
        return signalWaveformPanel;
    }

    /**
     * Check if signal view is currently active.
     */
    public boolean isShowingSignal() {
        return showingSignal;
    }
}
