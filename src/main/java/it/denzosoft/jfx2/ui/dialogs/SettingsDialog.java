package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.audio.AudioConfig;
import it.denzosoft.jfx2.audio.AudioEngine;
import it.denzosoft.jfx2.audio.AudioSettings;
import it.denzosoft.jfx2.audio.SoundfontSettings;
import it.denzosoft.jfx2.effects.impl.AudioInputEffect;
import it.denzosoft.jfx2.graph.InputNode;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Dialog for configuring application settings.
 *
 * <p>Contains tabs for:
 * <ul>
 *   <li>Audio - sample rate, buffer size</li>
 *   <li>Soundfont - MIDI soundfont configuration</li>
 * </ul>
 * </p>
 */
public class SettingsDialog extends JDialog {

    private final AudioEngine audioEngine;

    // Audio tab components
    private JComboBox<Integer> sampleRateCombo;
    private JComboBox<Integer> bufferSizeCombo;
    private JComboBox<String> inputChannelsCombo;
    private JLabel latencyLabel;

    // Soundfont tab components
    private JRadioButton defaultSoundfontRadio;
    private JRadioButton externalSoundfontRadio;
    private JTextField sf2PathField;
    private JButton browseSf2Button;
    private JLabel sf2StatusLabel;

    // Tuner tab components
    private JSpinner referenceFreqSpinner;
    private JLabel referenceNoteLabel;

    // State
    private boolean confirmed = false;
    private AudioConfig resultConfig;

    // Options
    private static final Integer[] SAMPLE_RATES = {44100, 48000, 88200, 96000};
    private static final Integer[] BUFFER_SIZES = {64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};

    public SettingsDialog(Frame owner, AudioEngine audioEngine) {
        super(owner, "Settings", true);
        this.audioEngine = audioEngine;

        setSize(500, 400);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents();
        loadCurrentSettings();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(DarkTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DarkTheme.BG_DARK);
        tabbedPane.setForeground(DarkTheme.TEXT_PRIMARY);
        tabbedPane.setFont(DarkTheme.FONT_REGULAR);

        // Audio tab
        tabbedPane.addTab("Audio", createAudioPanel());

        // Tuner tab
        tabbedPane.addTab("Tuner", createTunerPanel());

        // Soundfont tab
        tabbedPane.addTab("Soundfont", createSoundfontPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Buttons panel
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    // ==================== AUDIO TAB ====================

    private JPanel createAudioPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "Audio Configuration",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);

        int row = 0;

        // Sample Rate
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Sample Rate:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        sampleRateCombo = new JComboBox<>(SAMPLE_RATES);
        styleComboBox(sampleRateCombo);
        sampleRateCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value + " Hz");
                return this;
            }
        });
        sampleRateCombo.addActionListener(e -> updateLatencyLabel());
        formPanel.add(sampleRateCombo, gbc);

        row++;

        // Buffer Size
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Buffer Size:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        bufferSizeCombo = new JComboBox<>(BUFFER_SIZES);
        styleComboBox(bufferSizeCombo);
        bufferSizeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value + " samples");
                return this;
            }
        });
        bufferSizeCombo.addActionListener(e -> updateLatencyLabel());
        formPanel.add(bufferSizeCombo, gbc);

        row++;

        // Input Channels (Mono/Stereo)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Input Mode:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        inputChannelsCombo = new JComboBox<>(new String[]{"Mono", "Stereo"});
        styleComboBox(inputChannelsCombo);
        formPanel.add(inputChannelsCombo, gbc);

        row++;

        // Latency display
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Estimated Latency:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        latencyLabel = new JLabel("0 ms");
        latencyLabel.setForeground(DarkTheme.ACCENT_PRIMARY);
        latencyLabel.setFont(DarkTheme.FONT_BOLD);
        formPanel.add(latencyLabel, gbc);

        row++;

        // Reset Input Devices button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 12, 10, 12);

        JButton resetDevicesBtn = new JButton("Reset Input Devices");
        DarkTheme.styleDialogButton(resetDevicesBtn);
        resetDevicesBtn.setToolTipText("Close and reopen all audio input devices. " +
                "Use this if devices are unresponsive or not detected.");
        resetDevicesBtn.addActionListener(e -> resetInputDevices());
        formPanel.add(resetDevicesBtn, gbc);

        // Reset gridwidth for any future rows
        gbc.gridwidth = 1;

        panel.add(formPanel, BorderLayout.NORTH);

        // Info text
        JTextArea infoText = new JTextArea(
                "Note: Changes will take effect after restarting audio processing.\n" +
                "Smaller buffer sizes reduce latency but may cause audio glitches."
        );
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setForeground(DarkTheme.TEXT_SECONDARY);
        infoText.setFont(DarkTheme.FONT_SMALL);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        panel.add(infoText, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TUNER TAB ====================

    private JPanel createTunerPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "Tuner Configuration",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);

        int row = 0;

        // Reference Frequency (A4)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Reference Frequency (A4):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        // Spinner with 1 Hz increments (integer)
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(440, 400, 480, 1);
        referenceFreqSpinner = new JSpinner(spinnerModel);
        referenceFreqSpinner.setPreferredSize(new Dimension(80, 28));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(referenceFreqSpinner, "#");
        JFormattedTextField textField = editor.getTextField();
        textField.setBackground(DarkTheme.BG_MEDIUM);
        textField.setForeground(DarkTheme.TEXT_PRIMARY);
        textField.setCaretColor(DarkTheme.TEXT_PRIMARY);
        referenceFreqSpinner.setEditor(editor);

        JPanel freqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        freqPanel.setOpaque(false);
        freqPanel.add(referenceFreqSpinner);
        freqPanel.add(new JLabel("Hz") {{
            setForeground(DarkTheme.TEXT_SECONDARY);
        }});

        formPanel.add(freqPanel, gbc);

        row++;

        // Current note frequencies preview
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createLabel("Standard Guitar Tuning:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        referenceNoteLabel = new JLabel();
        referenceNoteLabel.setForeground(DarkTheme.ACCENT_PRIMARY);
        referenceNoteLabel.setFont(DarkTheme.FONT_MONO);
        formPanel.add(referenceNoteLabel, gbc);

        // Update label when spinner changes
        referenceFreqSpinner.addChangeListener(e -> updateReferenceNoteLabel());
        updateReferenceNoteLabel();

        panel.add(formPanel, BorderLayout.NORTH);

        // Info text
        JTextArea infoText = new JTextArea(
                "The reference frequency determines the pitch standard for tuning.\n" +
                "Standard concert pitch is A4 = 440 Hz.\n" +
                "Some orchestras use A4 = 442 Hz, baroque music often uses A4 = 415 Hz.\n\n" +
                "This setting affects the tuner display and frequency analysis."
        );
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setForeground(DarkTheme.TEXT_SECONDARY);
        infoText.setFont(DarkTheme.FONT_SMALL);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        panel.add(infoText, BorderLayout.CENTER);

        return panel;
    }

    private void updateReferenceNoteLabel() {
        double a4 = ((Number) referenceFreqSpinner.getValue()).doubleValue();
        // Calculate standard guitar string frequencies
        // E2, A2, D3, G3, B3, E4
        double e2 = a4 * Math.pow(2, -29.0 / 12);  // E2 = A4 - 29 semitones
        double a2 = a4 * Math.pow(2, -24.0 / 12);  // A2 = A4 - 24 semitones
        double d3 = a4 * Math.pow(2, -19.0 / 12);  // D3 = A4 - 19 semitones
        double g3 = a4 * Math.pow(2, -14.0 / 12);  // G3 = A4 - 14 semitones
        double b3 = a4 * Math.pow(2, -10.0 / 12);  // B3 = A4 - 10 semitones
        double e4 = a4 * Math.pow(2, -5.0 / 12);   // E4 = A4 - 5 semitones

        referenceNoteLabel.setText(String.format(
                "E2=%.1f  A2=%.1f  D3=%.1f  G3=%.1f  B3=%.1f  E4=%.1f Hz",
                e2, a2, d3, g3, b3, e4));
    }

    // ==================== SOUNDFONT TAB ====================

    private JPanel createSoundfontPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "MIDI Soundfont",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Radio button group
        ButtonGroup soundfontGroup = new ButtonGroup();

        // Default wavetable option
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        defaultSoundfontRadio = new JRadioButton("Use default Java wavetable synthesizer");
        defaultSoundfontRadio.setFont(DarkTheme.FONT_REGULAR);
        defaultSoundfontRadio.setForeground(DarkTheme.TEXT_PRIMARY);
        defaultSoundfontRadio.setOpaque(false);
        defaultSoundfontRadio.addActionListener(e -> updateSoundfontUI());
        soundfontGroup.add(defaultSoundfontRadio);
        formPanel.add(defaultSoundfontRadio, gbc);

        row++;

        // External SF2 option
        gbc.gridy = row;
        externalSoundfontRadio = new JRadioButton("Use external SoundFont file (SF2/DLS):");
        externalSoundfontRadio.setFont(DarkTheme.FONT_REGULAR);
        externalSoundfontRadio.setForeground(DarkTheme.TEXT_PRIMARY);
        externalSoundfontRadio.setOpaque(false);
        externalSoundfontRadio.addActionListener(e -> updateSoundfontUI());
        soundfontGroup.add(externalSoundfontRadio);
        formPanel.add(externalSoundfontRadio, gbc);

        row++;

        // SF2 path field
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 32, 8, 8);
        sf2PathField = new JTextField();
        sf2PathField.setFont(DarkTheme.FONT_REGULAR);
        sf2PathField.setBackground(DarkTheme.BG_LIGHT);
        sf2PathField.setForeground(DarkTheme.TEXT_PRIMARY);
        sf2PathField.setCaretColor(DarkTheme.TEXT_PRIMARY);
        sf2PathField.setPreferredSize(new Dimension(280, 28));
        sf2PathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateSf2Path(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateSf2Path(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateSf2Path(); }
        });
        formPanel.add(sf2PathField, gbc);

        // Browse button
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(4, 0, 8, 12);
        browseSf2Button = new JButton("Browse...");
        DarkTheme.styleDialogButton(browseSf2Button);
        browseSf2Button.addActionListener(e -> browseSf2File());
        formPanel.add(browseSf2Button, gbc);

        row++;

        // Status label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 32, 8, 12);
        sf2StatusLabel = new JLabel(" ");
        sf2StatusLabel.setFont(DarkTheme.FONT_SMALL);
        formPanel.add(sf2StatusLabel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Info text
        JTextArea infoText = new JTextArea(
                "SoundFont files (.sf2) and DLS files (.dls) contain instrument samples\n" +
                "for MIDI playback. This affects the Drum Machine's MIDI sound source.\n\n" +
                "Popular free SoundFonts:\n" +
                "- FluidR3 GM (General MIDI)\n" +
                "- SGM-V2.01 (High quality GM)\n" +
                "- Arachno SoundFont"
        );
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setForeground(DarkTheme.TEXT_SECONDARY);
        infoText.setFont(DarkTheme.FONT_SMALL);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        panel.add(infoText, BorderLayout.CENTER);

        return panel;
    }

    private void updateSoundfontUI() {
        boolean useExternal = externalSoundfontRadio.isSelected();
        sf2PathField.setEnabled(useExternal);
        browseSf2Button.setEnabled(useExternal);

        if (useExternal) {
            validateSf2Path();
        } else {
            sf2StatusLabel.setText("Using built-in Java synthesizer sounds");
            sf2StatusLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        }
    }

    private void validateSf2Path() {
        String path = sf2PathField.getText().trim();
        if (path.isEmpty()) {
            sf2StatusLabel.setText("Please specify a SoundFont file");
            sf2StatusLabel.setForeground(DarkTheme.TEXT_SECONDARY);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            sf2StatusLabel.setText("File not found");
            sf2StatusLabel.setForeground(DarkTheme.ACCENT_ERROR);
        } else if (!file.isFile()) {
            sf2StatusLabel.setText("Not a file");
            sf2StatusLabel.setForeground(DarkTheme.ACCENT_ERROR);
        } else if (!path.toLowerCase().endsWith(".sf2") && !path.toLowerCase().endsWith(".dls")) {
            sf2StatusLabel.setText("File must be .sf2 or .dls");
            sf2StatusLabel.setForeground(DarkTheme.ACCENT_WARNING);
        } else {
            long sizeMB = file.length() / (1024 * 1024);
            sf2StatusLabel.setText("Valid SoundFont (" + sizeMB + " MB)");
            sf2StatusLabel.setForeground(DarkTheme.ACCENT_SUCCESS);
        }
    }

    private void browseSf2File() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select SoundFont File");
        chooser.setFileFilter(new FileNameExtensionFilter("SoundFont files (*.sf2, *.dls)", "sf2", "dls"));

        // Start from current path if valid
        String currentPath = sf2PathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File current = new File(currentPath);
            if (current.exists()) {
                chooser.setCurrentDirectory(current.getParentFile());
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sf2PathField.setText(chooser.getSelectedFile().getAbsolutePath());
            externalSoundfontRadio.setSelected(true);
            updateSoundfontUI();
        }
    }

    // ==================== BUTTON PANEL ====================

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panel.setOpaque(false);

        JButton defaultsBtn = new JButton("Defaults");
        DarkTheme.styleDialogButton(defaultsBtn);
        defaultsBtn.addActionListener(e -> loadDefaults());

        JButton cancelBtn = new JButton("Cancel");
        DarkTheme.styleDialogButton(cancelBtn);
        cancelBtn.addActionListener(e -> dispose());

        JButton applyBtn = new JButton("Apply");
        DarkTheme.stylePrimaryButton(applyBtn);
        applyBtn.addActionListener(e -> {
            saveSettings();
            confirmed = true;
            dispose();
        });

        panel.add(defaultsBtn);
        panel.add(cancelBtn);
        panel.add(applyBtn);

        return panel;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Reset all audio input devices.
     * This closes all open input devices and allows them to be reopened.
     */
    private void resetInputDevices() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Run in background to avoid UI freeze
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                // Reset both InputNode and AudioInputEffect devices
                InputNode.resetAllDevices();
                AudioInputEffect.resetAllDevices();
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());

                // Show confirmation
                JOptionPane.showMessageDialog(
                        SettingsDialog.this,
                        "All audio input devices have been reset.\n" +
                        "Devices will reconnect automatically when needed.",
                        "Devices Reset",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        };

        worker.execute();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DarkTheme.TEXT_SECONDARY);
        label.setFont(DarkTheme.FONT_REGULAR);
        return label;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(DarkTheme.BG_MEDIUM);
        combo.setForeground(DarkTheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(250, 28));
    }

    private void loadCurrentSettings() {
        // Audio settings
        AudioConfig config = audioEngine.getConfig();
        if (config == null) {
            config = AudioConfig.DEFAULT;
        }
        sampleRateCombo.setSelectedItem(config.sampleRate());
        bufferSizeCombo.setSelectedItem(config.bufferSize());

        // Input channels and tuner settings from AudioSettings
        AudioSettings audioSettings = AudioSettings.getInstance();
        inputChannelsCombo.setSelectedIndex(audioSettings.isStereoInput() ? 1 : 0);
        referenceFreqSpinner.setValue((int) audioSettings.getTunerReferenceFrequency());
        updateReferenceNoteLabel();

        updateLatencyLabel();

        // Soundfont settings
        SoundfontSettings sfSettings = SoundfontSettings.getInstance();
        if (sfSettings.isUseExternalSoundfont()) {
            externalSoundfontRadio.setSelected(true);
            sf2PathField.setText(sfSettings.getSf2FilePath());
        } else {
            defaultSoundfontRadio.setSelected(true);
        }
        updateSoundfontUI();
    }

    private void loadDefaults() {
        // Audio defaults
        sampleRateCombo.setSelectedItem(AudioConfig.DEFAULT.sampleRate());
        bufferSizeCombo.setSelectedItem(AudioConfig.DEFAULT.bufferSize());
        inputChannelsCombo.setSelectedIndex(0);  // Default: Mono
        updateLatencyLabel();

        // Tuner defaults
        referenceFreqSpinner.setValue(440);
        updateReferenceNoteLabel();

        // Soundfont defaults
        defaultSoundfontRadio.setSelected(true);
        sf2PathField.setText("");
        updateSoundfontUI();
    }

    private void updateLatencyLabel() {
        Integer sampleRate = (Integer) sampleRateCombo.getSelectedItem();
        Integer bufferSize = (Integer) bufferSizeCombo.getSelectedItem();

        if (sampleRate != null && bufferSize != null) {
            double latency = (bufferSize * 2000.0) / sampleRate + 2.0;
            latencyLabel.setText(String.format("%.1f ms (round-trip)", latency));

            if (latency < 15) {
                latencyLabel.setForeground(DarkTheme.ACCENT_SUCCESS);
            } else if (latency < 30) {
                latencyLabel.setForeground(DarkTheme.ACCENT_WARNING);
            } else {
                latencyLabel.setForeground(DarkTheme.ACCENT_ERROR);
            }
        }
    }

    private void saveSettings() {
        // Create audio config
        Integer sampleRate = (Integer) sampleRateCombo.getSelectedItem();
        Integer bufferSize = (Integer) bufferSizeCombo.getSelectedItem();
        int inputChannels = inputChannelsCombo.getSelectedIndex() == 1 ? 2 : 1;

        resultConfig = new AudioConfig(
                sampleRate != null ? sampleRate : 44100,
                bufferSize != null ? bufferSize : 512,
                inputChannels,
                2
        );

        // Save audio settings (input channels and tuner)
        AudioSettings audioSettings = AudioSettings.getInstance();
        audioSettings.setInputChannels(inputChannels);
        audioSettings.setSampleRate(sampleRate != null ? sampleRate : 44100);
        audioSettings.setBufferSize(bufferSize != null ? bufferSize : 512);
        audioSettings.setTunerReferenceFrequency(((Number) referenceFreqSpinner.getValue()).doubleValue());
        audioSettings.save();

        // Save soundfont settings
        SoundfontSettings sfSettings = SoundfontSettings.getInstance();
        sfSettings.setUseExternalSoundfont(externalSoundfontRadio.isSelected());
        sfSettings.setSf2FilePath(sf2PathField.getText().trim());
        sfSettings.save();

        System.out.println("[SettingsDialog] Saved soundfont settings: " + sfSettings);
    }

    /**
     * Show the dialog and return the new config, or null if cancelled.
     */
    public AudioConfig showDialog() {
        setVisible(true);
        return confirmed ? resultConfig : null;
    }
}
