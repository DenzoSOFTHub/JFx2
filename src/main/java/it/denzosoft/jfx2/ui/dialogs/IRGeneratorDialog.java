package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.preset.Rig;
import it.denzosoft.jfx2.tools.IRGenerator;
import it.denzosoft.jfx2.tools.OfflineProcessor;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;

/**
 * Dialog for generating Impulse Responses from dry/wet audio pairs.
 */
public class IRGeneratorDialog extends JDialog {

    // File fields
    private JTextField dryFileField;
    private JTextField wetFileField;
    private JTextField outputFileField;

    // Parameters
    private JComboBox<Integer> irLengthCombo;
    private JCheckBox minimumPhaseCheck;
    private JCheckBox autoAlignCheck;
    private JSpinner regularizationSpinner;

    // Output
    private JTextArea logArea;
    private JButton generateButton;
    private JButton sweepButton;
    private JButton captureRigButton;
    private JButton closeButton;

    // State
    private File lastDirectory = new File(System.getProperty("user.dir"));
    private Rig currentRig;

    private static final Integer[] IR_LENGTHS = {512, 1024, 2048, 4096};

    public IRGeneratorDialog(Frame owner) {
        this(owner, null);
    }

    public IRGeneratorDialog(Frame owner, Rig currentRig) {
        super(owner, "IR Generator", true);
        this.currentRig = currentRig;

        setSize(700, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(DarkTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top: File selection
        mainPanel.add(createFilePanel(), BorderLayout.NORTH);

        // Center: Parameters + Log
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(createParametersPanel(), BorderLayout.NORTH);
        centerPanel.add(createLogPanel(), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "Audio Files",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);

        int row = 0;

        // Dry file
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Dry File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dryFileField = createTextField();
        dryFileField.setToolTipText("Original unprocessed audio (sweep or any signal)");
        panel.add(dryFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(createBrowseButton("dry"), gbc);

        row++;

        // Wet file
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Wet File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wetFileField = createTextField();
        wetFileField.setToolTipText("Processed audio (same content, through amp/cab/effect)");
        panel.add(wetFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(createBrowseButton("wet"), gbc);

        row++;

        // Output file
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Output IR:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        outputFileField = createTextField();
        outputFileField.setText("my_ir.wav");
        outputFileField.setToolTipText("Output IR file (WAV format)");
        panel.add(outputFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(createBrowseButton("output"), gbc);

        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "Parameters",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: IR Length, Minimum Phase
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("IR Length:"), gbc);

        gbc.gridx = 1;
        irLengthCombo = new JComboBox<>(IR_LENGTHS);
        irLengthCombo.setSelectedItem(2048);
        styleComboBox(irLengthCombo);
        irLengthCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                int samples = (Integer) value;
                float ms = samples / 44.1f;
                setText(samples + " samples (" + String.format("%.1f", ms) + " ms)");
                return this;
            }
        });
        panel.add(irLengthCombo, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(6, 24, 6, 8);
        minimumPhaseCheck = new JCheckBox("Minimum Phase");
        minimumPhaseCheck.setFont(DarkTheme.FONT_REGULAR);
        minimumPhaseCheck.setForeground(DarkTheme.TEXT_PRIMARY);
        minimumPhaseCheck.setOpaque(false);
        minimumPhaseCheck.setToolTipText("Convert to minimum phase (lower latency, may affect sound)");
        panel.add(minimumPhaseCheck, gbc);

        // Row 2: Auto-align, Regularization
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createLabel("Regularization:"), gbc);

        gbc.gridx = 1;
        SpinnerNumberModel regModel = new SpinnerNumberModel(0.001, 0.0001, 0.1, 0.0005);
        regularizationSpinner = new JSpinner(regModel);
        regularizationSpinner.setPreferredSize(new Dimension(100, 28));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(regularizationSpinner, "0.0000");
        regularizationSpinner.setEditor(editor);
        regularizationSpinner.setToolTipText("Noise reduction: higher = smoother, lower = more detail");
        panel.add(regularizationSpinner, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(6, 24, 6, 8);
        autoAlignCheck = new JCheckBox("Auto-Align");
        autoAlignCheck.setSelected(true);
        autoAlignCheck.setFont(DarkTheme.FONT_REGULAR);
        autoAlignCheck.setForeground(DarkTheme.TEXT_PRIMARY);
        autoAlignCheck.setOpaque(false);
        autoAlignCheck.setToolTipText("Automatically align dry and wet signals");
        panel.add(autoAlignCheck, gbc);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                "Output",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                DarkTheme.FONT_BOLD,
                DarkTheme.TEXT_PRIMARY));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(DarkTheme.BG_DARK);
        logArea.setForeground(DarkTheme.TEXT_PRIMARY);
        logArea.setCaretColor(DarkTheme.TEXT_PRIMARY);
        logArea.setText("Ready. Select dry and wet files, then click Generate.\n\n" +
                       "Tip: Use 'Generate Test Sweep' to create an optimal test signal.\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.getViewport().setBackground(DarkTheme.BG_DARK);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        sweepButton = new JButton("Generate Test Sweep");
        DarkTheme.styleDialogButton(sweepButton);
        sweepButton.setToolTipText("Generate a logarithmic sweep file for IR capture");
        sweepButton.addActionListener(e -> generateSweep());
        panel.add(sweepButton);

        panel.add(Box.createHorizontalStrut(8));

        captureRigButton = new JButton("Capture Rig IR");
        DarkTheme.stylePrimaryButton(captureRigButton);
        captureRigButton.setToolTipText("Process a sweep through the current rig and generate IR automatically");
        captureRigButton.addActionListener(e -> captureRigIR());
        captureRigButton.setEnabled(currentRig != null);
        panel.add(captureRigButton);

        panel.add(Box.createHorizontalStrut(8));

        generateButton = new JButton("Generate IR from Files");
        DarkTheme.styleDialogButton(generateButton);
        generateButton.addActionListener(e -> generateIR());
        panel.add(generateButton);

        panel.add(Box.createHorizontalStrut(16));

        closeButton = new JButton("Close");
        DarkTheme.styleDialogButton(closeButton);
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        return panel;
    }

    private JButton createBrowseButton(String type) {
        JButton button = new JButton("...");
        DarkTheme.styleDialogButton(button);
        button.setPreferredSize(new Dimension(40, 28));
        button.addActionListener(e -> browseFile(type));
        return button;
    }

    private void browseFile(String type) {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));

        int result;
        if ("output".equals(type)) {
            chooser.setDialogTitle("Save IR As");
            result = chooser.showSaveDialog(this);
        } else {
            chooser.setDialogTitle("Select " + type + " file");
            result = chooser.showOpenDialog(this);
        }

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();

            String path = file.getAbsolutePath();
            if ("output".equals(type) && !path.toLowerCase().endsWith(".wav")) {
                path += ".wav";
            }

            switch (type) {
                case "dry" -> dryFileField.setText(path);
                case "wet" -> wetFileField.setText(path);
                case "output" -> outputFileField.setText(path);
            }
        }
    }

    private void generateSweep() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("Save Test Sweep As");
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));
        chooser.setSelectedFile(new File("test_sweep.wav"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();

            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".wav")) {
                path += ".wav";
            }

            final String outputPath = path;

            // Disable buttons during generation
            setButtonsEnabled(false);
            logArea.setText("Generating test sweep...\n");

            // Run in background
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() {
                    try {
                        // Redirect stdout to capture output
                        PrintStream oldOut = System.out;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream newOut = new PrintStream(baos);
                        System.setOut(newOut);

                        // Generate sweep (3 seconds, 44100 Hz)
                        String[] args = {"sweep", outputPath, "3", "44100"};
                        IRGenerator.main(args);

                        System.setOut(oldOut);
                        publish(baos.toString());

                    } catch (Exception e) {
                        publish("Error: " + e.getMessage() + "\n");
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String text : chunks) {
                        logArea.append(text);
                    }
                }

                @Override
                protected void done() {
                    setButtonsEnabled(true);
                    logArea.append("\nSweep generation complete.\n");
                    logArea.append("Now play this file through your amp/cab and record the output.\n");

                    // Auto-fill dry file field
                    dryFileField.setText(outputPath);
                }
            }.execute();
        }
    }

    private void generateIR() {
        // Validate inputs
        String dryPath = dryFileField.getText().trim();
        String wetPath = wetFileField.getText().trim();
        String outputPath = outputFileField.getText().trim();

        if (dryPath.isEmpty()) {
            showError("Please select a dry file.");
            return;
        }
        if (wetPath.isEmpty()) {
            showError("Please select a wet file.");
            return;
        }
        if (outputPath.isEmpty()) {
            showError("Please specify an output file.");
            return;
        }

        if (!new File(dryPath).exists()) {
            showError("Dry file not found: " + dryPath);
            return;
        }
        if (!new File(wetPath).exists()) {
            showError("Wet file not found: " + wetPath);
            return;
        }

        // Ensure output has .wav extension
        if (!outputPath.toLowerCase().endsWith(".wav")) {
            outputPath += ".wav";
            outputFileField.setText(outputPath);
        }

        // Get parameters
        int irLength = (Integer) irLengthCombo.getSelectedItem();
        boolean minimumPhase = minimumPhaseCheck.isSelected();
        boolean autoAlign = autoAlignCheck.isSelected();
        double regularization = (Double) regularizationSpinner.getValue();

        final String finalOutputPath = outputPath;

        // Disable buttons during generation
        setButtonsEnabled(false);
        logArea.setText("Starting IR generation...\n\n");

        // Run in background
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Redirect stdout to capture output
                    PrintStream oldOut = System.out;
                    PrintStream oldErr = System.err;

                    PipedOutputStream pos = new PipedOutputStream();
                    PipedInputStream pis = new PipedInputStream(pos);
                    PrintStream newOut = new PrintStream(pos, true);

                    System.setOut(newOut);
                    System.setErr(newOut);

                    // Start reader thread
                    Thread readerThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pis))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                final String l = line;
                                SwingUtilities.invokeLater(() -> {
                                    logArea.append(l + "\n");
                                    logArea.setCaretPosition(logArea.getDocument().getLength());
                                });
                            }
                        } catch (IOException e) {
                            // Stream closed
                        }
                    });
                    readerThread.start();

                    // Build arguments
                    java.util.List<String> argList = new java.util.ArrayList<>();
                    argList.add("generate");
                    argList.add(dryPath);
                    argList.add(wetPath);
                    argList.add(finalOutputPath);
                    argList.add(String.valueOf(irLength));

                    if (minimumPhase) {
                        argList.add("--minimum-phase");
                    }
                    if (!autoAlign) {
                        argList.add("--no-align");
                    }
                    argList.add("--reg");
                    argList.add(String.valueOf(regularization));

                    String[] args = argList.toArray(new String[0]);
                    IRGenerator.main(args);

                    // Close streams
                    newOut.close();
                    readerThread.join(1000);

                    System.setOut(oldOut);
                    System.setErr(oldErr);

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("\nError: " + e.getMessage() + "\n");
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                logArea.append("\n=== Generation complete ===\n");
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        generateButton.setEnabled(enabled);
        sweepButton.setEnabled(enabled);
        captureRigButton.setEnabled(enabled && currentRig != null);
        closeButton.setEnabled(enabled);
    }

    /**
     * Capture IR from the current rig by processing a sweep through it offline.
     */
    private void captureRigIR() {
        // Validate output path
        String outputPath = outputFileField.getText().trim();
        if (outputPath.isEmpty()) {
            showError("Please specify an output file for the IR.");
            return;
        }

        if (!outputPath.toLowerCase().endsWith(".wav")) {
            outputPath += ".wav";
            outputFileField.setText(outputPath);
        }

        if (currentRig == null) {
            showError("No rig loaded. Please load a rig first.");
            return;
        }

        // Get parameters
        int irLength = (Integer) irLengthCombo.getSelectedItem();
        boolean minimumPhase = minimumPhaseCheck.isSelected();
        double regularization = (Double) regularizationSpinner.getValue();

        final String finalOutputPath = outputPath;

        // Disable buttons during processing
        setButtonsEnabled(false);
        logArea.setText("=== Capture Rig IR ===\n\n");
        logArea.append("Rig: " + currentRig.getName() + "\n");
        logArea.append("Output: " + finalOutputPath + "\n");
        logArea.append("IR Length: " + irLength + " samples\n\n");

        // Run in background
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    int sampleRate = 44100;
                    float duration = 3.0f;  // 3 second sweep

                    // Step 1: Generate sweep in memory
                    publish("Generating test sweep in memory...\n");
                    float[] sweep = generateLogSweep(20f, 20000f, duration, sampleRate);

                    // Add silence padding (0.5s before and after)
                    int silenceSamples = sampleRate / 2;
                    float[] sweepWithSilence = new float[silenceSamples + sweep.length + silenceSamples];
                    System.arraycopy(sweep, 0, sweepWithSilence, silenceSamples, sweep.length);

                    publish("  Sweep length: " + sweepWithSilence.length + " samples (" +
                           String.format("%.2f", sweepWithSilence.length / (float) sampleRate) + "s)\n\n");

                    // Step 2: Process through the rig offline
                    publish("Processing sweep through rig...\n");
                    OfflineProcessor processor = new OfflineProcessor(sampleRate, 256);
                    float[] wetSignal = processor.process(currentRig, sweepWithSilence);
                    publish("  Processing complete.\n\n");

                    // Step 3: Generate IR using Wiener deconvolution
                    publish("Generating IR from dry/wet signals...\n");
                    float[] ir = generateIRFromSignals(sweepWithSilence, wetSignal, irLength, (float) regularization);

                    // Step 4: Apply noise gate
                    publish("Applying noise gate...\n");
                    applyNoiseGate(ir);

                    // Step 5: Convert to minimum phase if requested
                    if (minimumPhase) {
                        publish("Converting to minimum phase...\n");
                        ir = toMinimumPhase(ir);
                    }

                    // Step 6: Normalize and fade out
                    publish("Normalizing and applying fade out...\n");
                    normalize(ir);
                    applyFadeOut(ir, irLength / 4);

                    // Step 7: Save
                    publish("Saving IR to: " + finalOutputPath + "\n");
                    saveWav(ir, finalOutputPath, sampleRate);

                    publish("\n=== IR Generation Complete ===\n");
                    publish("Saved: " + finalOutputPath + "\n");
                    publish("Length: " + irLength + " samples (" +
                           String.format("%.2f", irLength * 1000.0 / sampleRate) + " ms)\n");

                } catch (Exception e) {
                    publish("\nError: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String text : chunks) {
                    logArea.append(text);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
            }
        }.execute();
    }

    // ==================== DSP UTILITIES FOR IR GENERATION ====================

    /**
     * Generate logarithmic sine sweep.
     */
    private float[] generateLogSweep(float startFreq, float endFreq, float durationSeconds, int sampleRate) {
        int numSamples = (int)(durationSeconds * sampleRate);
        float[] sweep = new float[numSamples];

        double k = Math.log(endFreq / startFreq) / durationSeconds;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            double phase = 2.0 * Math.PI * startFreq * (Math.exp(k * t) - 1) / k;
            sweep[i] = (float) Math.sin(phase);
        }

        // Apply smooth fade in/out (raised cosine)
        int fadeLen = sampleRate / 10;  // 100ms fade
        for (int i = 0; i < fadeLen; i++) {
            float fade = 0.5f * (1 - (float)Math.cos(Math.PI * i / fadeLen));
            sweep[i] *= fade;
            sweep[numSamples - 1 - i] *= fade;
        }

        return sweep;
    }

    /**
     * Generate IR from dry and wet signals using Wiener deconvolution.
     */
    private float[] generateIRFromSignals(float[] dry, float[] wet, int irLength, float regularization) {
        int length = Math.min(dry.length, wet.length);
        int fftSize = nextPowerOf2(length + irLength);

        float[] dryReal = new float[fftSize];
        float[] dryImag = new float[fftSize];
        float[] wetReal = new float[fftSize];
        float[] wetImag = new float[fftSize];

        System.arraycopy(dry, 0, dryReal, 0, Math.min(length, fftSize));
        System.arraycopy(wet, 0, wetReal, 0, Math.min(length, fftSize));

        // Apply Blackman window
        applyBlackmanWindow(dryReal, length);
        applyBlackmanWindow(wetReal, length);

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);

        // Forward FFT
        fft(dryReal, dryImag, fftSize, twiddleReal, twiddleImag, false);
        fft(wetReal, wetImag, fftSize, twiddleReal, twiddleImag, false);

        // Wiener deconvolution: H = conj(X) * Y / (|X|² + λ)
        float[] irReal = new float[fftSize];
        float[] irImag = new float[fftSize];

        for (int i = 0; i < fftSize; i++) {
            float xr = dryReal[i];
            float xi = dryImag[i];
            float yr = wetReal[i];
            float yi = wetImag[i];

            float powerX = xr * xr + xi * xi;
            float denom = powerX + regularization;

            irReal[i] = (xr * yr + xi * yi) / denom;
            irImag[i] = (xr * yi - xi * yr) / denom;
        }

        // Inverse FFT
        fft(irReal, irImag, fftSize, twiddleReal, twiddleImag, true);

        // Extract IR
        float[] ir = new float[irLength];
        System.arraycopy(irReal, 0, ir, 0, irLength);

        return ir;
    }

    /**
     * Apply noise gate to remove pre-response noise.
     */
    private void applyNoiseGate(float[] ir) {
        int peakIdx = 0;
        float peakVal = 0;
        for (int i = 0; i < ir.length; i++) {
            if (Math.abs(ir[i]) > peakVal) {
                peakVal = Math.abs(ir[i]);
                peakIdx = i;
            }
        }

        float threshold = peakVal * 0.001f; // -60dB

        int responseStart = peakIdx;
        for (int i = peakIdx - 1; i >= 0; i--) {
            if (Math.abs(ir[i]) > threshold * 10) {
                responseStart = i;
            } else {
                break;
            }
        }

        for (int i = 0; i < responseStart; i++) {
            ir[i] *= 0.001f;
        }
    }

    /**
     * Convert to minimum phase IR.
     */
    private float[] toMinimumPhase(float[] ir) {
        int fftSize = nextPowerOf2(ir.length * 2);

        float[] real = new float[fftSize];
        float[] imag = new float[fftSize];
        System.arraycopy(ir, 0, real, 0, ir.length);

        float[] twiddleReal = new float[fftSize / 2];
        float[] twiddleImag = new float[fftSize / 2];
        computeTwiddleFactors(twiddleReal, twiddleImag, fftSize);

        fft(real, imag, fftSize, twiddleReal, twiddleImag, false);

        float[] logMag = new float[fftSize];
        for (int i = 0; i < fftSize; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            logMag[i] = (float) Math.log(Math.max(mag, 1e-10));
        }

        float[] hilbertReal = new float[fftSize];
        float[] hilbertImag = new float[fftSize];
        System.arraycopy(logMag, 0, hilbertReal, 0, fftSize);

        fft(hilbertReal, hilbertImag, fftSize, twiddleReal, twiddleImag, false);

        for (int i = 1; i < fftSize / 2; i++) {
            hilbertImag[i] = -hilbertReal[i];
            hilbertReal[i] = hilbertImag[i];
        }
        hilbertReal[0] = hilbertImag[0] = 0;
        hilbertReal[fftSize/2] = hilbertImag[fftSize/2] = 0;

        fft(hilbertReal, hilbertImag, fftSize, twiddleReal, twiddleImag, true);

        float[] minPhaseReal = new float[fftSize];
        float[] minPhaseImag = new float[fftSize];

        for (int i = 0; i < fftSize; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            float phase = -hilbertImag[i];
            minPhaseReal[i] = mag * (float) Math.cos(phase);
            minPhaseImag[i] = mag * (float) Math.sin(phase);
        }

        fft(minPhaseReal, minPhaseImag, fftSize, twiddleReal, twiddleImag, true);

        float[] result = new float[ir.length];
        System.arraycopy(minPhaseReal, 0, result, 0, ir.length);
        return result;
    }

    private void normalize(float[] ir) {
        float maxAbs = 0;
        for (float sample : ir) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }
        if (maxAbs > 0) {
            float scale = 0.99f / maxAbs;
            for (int i = 0; i < ir.length; i++) {
                ir[i] *= scale;
            }
        }
    }

    private void applyFadeOut(float[] ir, int fadeLength) {
        fadeLength = Math.min(fadeLength, ir.length / 2);
        int startFade = ir.length - fadeLength;
        for (int i = startFade; i < ir.length; i++) {
            float t = (float)(i - startFade) / fadeLength;
            float fade = 0.5f * (1 + (float)Math.cos(Math.PI * t));
            ir[i] *= fade;
        }
    }

    private void applyBlackmanWindow(float[] signal, int length) {
        for (int i = 0; i < Math.min(length, signal.length); i++) {
            double window = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (length - 1))
                          + 0.08 * Math.cos(4 * Math.PI * i / (length - 1));
            signal[i] *= (float) window;
        }
    }

    // ==================== FFT IMPLEMENTATION ====================

    private void fft(float[] real, float[] imag, int n, float[] twiddleReal, float[] twiddleImag, boolean inverse) {
        int halfN = n / 2;

        for (int i = 0, j = 0; i < n; i++) {
            if (j > i) {
                float tempR = real[i], tempI = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempR;
                imag[j] = tempI;
            }
            int m = halfN;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        for (int mmax = 1; mmax < n; mmax <<= 1) {
            int step = mmax << 1;
            int twiddleStep = n / step;

            for (int m = 0; m < mmax; m++) {
                int twiddleIdx = m * twiddleStep;
                float wr = twiddleReal[twiddleIdx];
                float wi = inverse ? -twiddleImag[twiddleIdx] : twiddleImag[twiddleIdx];

                for (int i = m; i < n; i += step) {
                    int j = i + mmax;
                    float tr = wr * real[j] - wi * imag[j];
                    float ti = wr * imag[j] + wi * real[j];
                    real[j] = real[i] - tr;
                    imag[j] = imag[i] - ti;
                    real[i] += tr;
                    imag[i] += ti;
                }
            }
        }

        if (inverse) {
            float scale = 1.0f / n;
            for (int i = 0; i < n; i++) {
                real[i] *= scale;
                imag[i] *= scale;
            }
        }
    }

    private void computeTwiddleFactors(float[] twiddleReal, float[] twiddleImag, int fftSize) {
        for (int i = 0; i < fftSize / 2; i++) {
            double angle = -2.0 * Math.PI * i / fftSize;
            twiddleReal[i] = (float) Math.cos(angle);
            twiddleImag[i] = (float) Math.sin(angle);
        }
    }

    private int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) power <<= 1;
        return power;
    }

    // ==================== WAV FILE I/O ====================

    private void saveWav(float[] samples, String path, int sampleRate) throws Exception {
        byte[] audioBytes = new byte[samples.length * 2];
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(audioBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        for (float sample : samples) {
            float clipped = Math.max(-1.0f, Math.min(1.0f, sample));
            bb.putShort((short)(clipped * 32767));
        }

        javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                false
        );

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(audioBytes);
        javax.sound.sampled.AudioInputStream ais = new javax.sound.sampled.AudioInputStream(bais, format, samples.length);

        javax.sound.sampled.AudioSystem.write(ais, javax.sound.sampled.AudioFileFormat.Type.WAVE, new File(path));
        ais.close();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DarkTheme.FONT_REGULAR);
        label.setForeground(DarkTheme.TEXT_PRIMARY);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(DarkTheme.FONT_REGULAR);
        field.setBackground(DarkTheme.BG_LIGHT);
        field.setForeground(DarkTheme.TEXT_PRIMARY);
        field.setCaretColor(DarkTheme.TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(300, 28));
        return field;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(DarkTheme.FONT_REGULAR);
        combo.setBackground(DarkTheme.BG_LIGHT);
        combo.setForeground(DarkTheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(200, 28));
    }
}
