package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.dsp.FFTConvolver;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * IR Manager dialog for browsing and previewing impulse response files.
 */
public class IRManagerDialog extends JDialog {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;

    // UI Components
    private JTable irTable;
    private IRTableModel tableModel;
    private JLabel sampleFileLabel;
    private JButton playButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private IRVisualizationPanel visualizationPanel;

    // Audio state
    private Path sampleFilePath;
    private float[] sampleDataL;
    private float[] sampleDataR;
    private boolean sampleStereo;
    private int sampleLength;

    // Playback state
    private volatile boolean isPlaying;
    private Thread playbackThread;
    private SourceDataLine audioLine;
    private FFTConvolver convolverL;
    private FFTConvolver convolverR;
    private volatile IRInfo currentIR;

    // IR directory
    private Path irsDirectory;

    public IRManagerDialog(Frame owner) {
        super(owner, "IR Manager", true);
        findIRsDirectory();
        initComponents();
        loadIRList();
        pack();
        setMinimumSize(new Dimension(800, 650));
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopPlayback();
            }
        });
    }

    private void findIRsDirectory() {
        try {
            Path classPath = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(classPath)) {
                classPath = classPath.getParent();
            }

            Path irsPath = classPath.resolve("IRs");
            if (Files.exists(irsPath) && Files.isDirectory(irsPath)) {
                irsDirectory = irsPath;
                return;
            }

            irsPath = Path.of("IRs");
            if (Files.exists(irsPath) && Files.isDirectory(irsPath)) {
                irsDirectory = irsPath.toAbsolutePath();
                return;
            }

            irsPath = Path.of("../IRs");
            if (Files.exists(irsPath) && Files.isDirectory(irsPath)) {
                irsDirectory = irsPath.toAbsolutePath();
                return;
            }
        } catch (Exception e) {
            // Ignore
        }
        irsDirectory = Path.of("IRs");
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(DarkTheme.BG_DARK);

        // Top - IR table with header
        JPanel topPanel = createTablePanel();
        add(topPanel, BorderLayout.CENTER);

        // Bottom - Visualization + Controls
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Header
        JLabel header = new JLabel("Impulse Responses in: " + irsDirectory);
        header.setForeground(DarkTheme.TEXT_SECONDARY);
        header.setFont(header.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(header, BorderLayout.NORTH);

        // Table
        tableModel = new IRTableModel();
        irTable = new JTable(tableModel);
        irTable.setBackground(DarkTheme.BG_MEDIUM);
        irTable.setForeground(DarkTheme.TEXT_PRIMARY);
        irTable.setGridColor(DarkTheme.MENU_BORDER);
        irTable.setSelectionBackground(DarkTheme.ACCENT_PRIMARY);
        irTable.setSelectionForeground(DarkTheme.TEXT_PRIMARY);
        irTable.setRowHeight(24);
        irTable.getTableHeader().setBackground(DarkTheme.BG_LIGHT);
        irTable.getTableHeader().setForeground(DarkTheme.TEXT_PRIMARY);

        irTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        irTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        irTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        irTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        irTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        irTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        irTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        irTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        irTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onIRSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(irTable);
        scrollPane.setBackground(DarkTheme.BG_DARK);
        scrollPane.getViewport().setBackground(DarkTheme.BG_MEDIUM);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(DarkTheme.BG_LIGHT);
        refreshButton.setForeground(DarkTheme.TEXT_PRIMARY);
        refreshButton.addActionListener(e -> loadIRList());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DarkTheme.BG_DARK);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // IR Visualization panel
        visualizationPanel = new IRVisualizationPanel();
        visualizationPanel.setPreferredSize(new Dimension(700, 180));
        visualizationPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.MENU_BORDER),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        panel.add(visualizationPanel, BorderLayout.CENTER);

        // Controls panel (sample file + play/stop + close)
        JPanel controlsPanel = new JPanel(new BorderLayout(10, 5));
        controlsPanel.setBackground(DarkTheme.BG_DARK);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Left: Sample file selection
        JPanel samplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        samplePanel.setBackground(DarkTheme.BG_DARK);

        JLabel sampleTitle = new JLabel("Sample:");
        sampleTitle.setForeground(DarkTheme.TEXT_PRIMARY);
        sampleTitle.setFont(sampleTitle.getFont().deriveFont(Font.BOLD));
        samplePanel.add(sampleTitle);

        sampleFileLabel = new JLabel("No sample loaded");
        sampleFileLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        sampleFileLabel.setPreferredSize(new Dimension(200, 20));
        samplePanel.add(sampleFileLabel);

        JButton browseButton = new JButton("Browse...");
        browseButton.setBackground(DarkTheme.BG_LIGHT);
        browseButton.setForeground(DarkTheme.TEXT_PRIMARY);
        browseButton.addActionListener(e -> browseSampleFile());
        samplePanel.add(browseButton);

        controlsPanel.add(samplePanel, BorderLayout.WEST);

        // Center: Play/Stop buttons
        JPanel playbackPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        playbackPanel.setBackground(DarkTheme.BG_DARK);

        playButton = new JButton("Play");
        playButton.setBackground(DarkTheme.ACCENT_PRIMARY);
        playButton.setForeground(Color.WHITE);
        playButton.setEnabled(false);
        playButton.addActionListener(e -> startPlayback());
        playbackPanel.add(playButton);

        stopButton = new JButton("Stop");
        stopButton.setBackground(DarkTheme.BG_LIGHT);
        stopButton.setForeground(DarkTheme.TEXT_PRIMARY);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopPlayback());
        playbackPanel.add(stopButton);

        controlsPanel.add(playbackPanel, BorderLayout.CENTER);

        // Right: Close button
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBackground(DarkTheme.BG_DARK);

        JButton closeButton = new JButton("Close");
        closeButton.setBackground(DarkTheme.BG_LIGHT);
        closeButton.setForeground(DarkTheme.TEXT_PRIMARY);
        closeButton.addActionListener(e -> {
            stopPlayback();
            dispose();
        });
        closePanel.add(closeButton);

        controlsPanel.add(closePanel, BorderLayout.EAST);

        panel.add(controlsPanel, BorderLayout.SOUTH);

        // Status label
        statusLabel = new JLabel("Select an IR to view its profile");
        statusLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panel.add(statusLabel, BorderLayout.NORTH);

        return panel;
    }

    private void loadIRList() {
        List<IRInfo> irList = new ArrayList<>();

        if (Files.exists(irsDirectory) && Files.isDirectory(irsDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(irsDirectory, "*.wav")) {
                for (Path path : stream) {
                    IRInfo info = loadIRInfo(path);
                    if (info != null) {
                        irList.add(info);
                    }
                }
            } catch (IOException e) {
                statusLabel.setText("Error reading IR directory: " + e.getMessage());
            }
        }

        irList.sort(Comparator.comparing(ir -> ir.name.toLowerCase()));
        tableModel.setData(irList);
        statusLabel.setText("Found " + irList.size() + " IR files");
    }

    private IRInfo loadIRInfo(Path path) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(path.toFile());
            AudioFormat format = audioStream.getFormat();

            long frames = audioStream.getFrameLength();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            float duration = frames / (float) sampleRate;

            audioStream.close();

            return new IRInfo(path.getFileName().toString(), path, (int) frames, sampleRate, channels, duration);
        } catch (Exception e) {
            return null;
        }
    }

    private void browseSampleFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Files", "wav"));
        chooser.setDialogTitle("Select Sample File");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadSampleFile(chooser.getSelectedFile().toPath());
        }
    }

    private void loadSampleFile(Path path) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(path.toFile());
            AudioFormat format = audioStream.getFormat();

            AudioFormat targetFormat = new AudioFormat(SAMPLE_RATE, 16, format.getChannels(), true, false);
            if (!format.matches(targetFormat)) {
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
                format = targetFormat;
            }

            sampleStereo = format.getChannels() >= 2;
            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            int bytesPerFrame = format.getFrameSize();
            sampleLength = audioBytes.length / bytesPerFrame;

            sampleDataL = new float[sampleLength];
            sampleDataR = sampleStereo ? new float[sampleLength] : null;

            for (int i = 0; i < sampleLength; i++) {
                int pos = i * bytesPerFrame;
                int lo = audioBytes[pos] & 0xFF;
                int hi = audioBytes[pos + 1];
                sampleDataL[i] = (short) ((hi << 8) | lo) / 32768.0f;

                if (sampleStereo) {
                    lo = audioBytes[pos + 2] & 0xFF;
                    hi = audioBytes[pos + 3];
                    sampleDataR[i] = (short) ((hi << 8) | lo) / 32768.0f;
                }
            }

            sampleFilePath = path;
            sampleFileLabel.setText(path.getFileName().toString());
            sampleFileLabel.setToolTipText(String.format("%.1fs, %s", sampleLength / (float) SAMPLE_RATE,
                    sampleStereo ? "stereo" : "mono"));

            updatePlayButtonState();
            statusLabel.setText("Sample loaded. Select an IR and click Play.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading sample: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onIRSelected() {
        int row = irTable.getSelectedRow();
        if (row >= 0) {
            IRInfo ir = tableModel.getData().get(row);

            // Load and display IR visualization
            loadIRVisualization(ir);

            if (isPlaying) {
                loadIRForPlayback(ir);
            }

            updatePlayButtonState();
        }
    }

    private void loadIRVisualization(IRInfo ir) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(ir.path.toFile());
            AudioFormat format = audioStream.getFormat();

            AudioFormat targetFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            if (format.getChannels() > 1 || format.getSampleRate() != SAMPLE_RATE) {
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            }

            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            int bytesPerFrame = 2; // 16-bit mono
            int irLength = audioBytes.length / bytesPerFrame;

            float[] irData = new float[irLength];
            for (int i = 0; i < irLength; i++) {
                int pos = i * bytesPerFrame;
                int lo = audioBytes[pos] & 0xFF;
                int hi = audioBytes[pos + 1];
                irData[i] = (short) ((hi << 8) | lo) / 32768.0f;
            }

            visualizationPanel.setIRData(irData, ir.sampleRate, ir.name);
            statusLabel.setText("IR: " + ir.name + " (" + ir.samples + " samples, " +
                    String.format("%.2fs", ir.duration) + ")");

        } catch (Exception e) {
            statusLabel.setText("Error loading IR for visualization: " + e.getMessage());
        }
    }

    private void updatePlayButtonState() {
        boolean canPlay = sampleDataL != null && irTable.getSelectedRow() >= 0;
        playButton.setEnabled(canPlay && !isPlaying);
        stopButton.setEnabled(isPlaying);
    }

    private void startPlayback() {
        int row = irTable.getSelectedRow();
        if (row < 0 || sampleDataL == null) return;

        IRInfo ir = tableModel.getData().get(row);

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format, BUFFER_SIZE * 4);
            audioLine.start();

            convolverL = new FFTConvolver();
            convolverR = new FFTConvolver();

            loadIRForPlayback(ir);

            isPlaying = true;
            updatePlayButtonState();
            statusLabel.setText("Playing with IR: " + ir.name);

            playbackThread = new Thread(this::playbackLoop, "IR-Preview-Playback");
            playbackThread.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error starting playback: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            stopPlayback();
        }
    }

    private void loadIRForPlayback(IRInfo ir) {
        currentIR = ir;

        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(ir.path.toFile());
            AudioFormat format = audioStream.getFormat();

            AudioFormat targetFormat = new AudioFormat(SAMPLE_RATE, 16, format.getChannels(), true, false);
            if (!format.matches(targetFormat)) {
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
                format = targetFormat;
            }

            byte[] audioBytes = audioStream.readAllBytes();
            audioStream.close();

            int bytesPerFrame = format.getFrameSize();
            int irLength = audioBytes.length / bytesPerFrame;
            boolean irStereo = format.getChannels() >= 2;

            float[] irDataL = new float[irLength];
            float[] irDataR = new float[irLength];

            for (int i = 0; i < irLength; i++) {
                int pos = i * bytesPerFrame;
                int lo = audioBytes[pos] & 0xFF;
                int hi = audioBytes[pos + 1];
                irDataL[i] = (short) ((hi << 8) | lo) / 32768.0f;

                if (irStereo) {
                    lo = audioBytes[pos + 2] & 0xFF;
                    hi = audioBytes[pos + 3];
                    irDataR[i] = (short) ((hi << 8) | lo) / 32768.0f;
                } else {
                    irDataR[i] = irDataL[i];
                }
            }

            float peak = 0;
            for (int i = 0; i < irLength; i++) {
                peak = Math.max(peak, Math.abs(irDataL[i]));
                peak = Math.max(peak, Math.abs(irDataR[i]));
            }
            if (peak > 0.001f) {
                float scale = 0.5f / peak;
                for (int i = 0; i < irLength; i++) {
                    irDataL[i] *= scale;
                    irDataR[i] *= scale;
                }
            }

            synchronized (this) {
                convolverL.prepare(irDataL, irLength, BUFFER_SIZE);
                convolverR.prepare(irDataR, irLength, BUFFER_SIZE);
            }

        } catch (Exception e) {
            statusLabel.setText("Error loading IR: " + e.getMessage());
        }
    }

    private void playbackLoop() {
        float[] inputL = new float[BUFFER_SIZE];
        float[] inputR = new float[BUFFER_SIZE];
        float[] outputL = new float[BUFFER_SIZE];
        float[] outputR = new float[BUFFER_SIZE];
        byte[] outputBytes = new byte[BUFFER_SIZE * 4];

        int position = 0;

        while (isPlaying && position < sampleLength) {
            int framesToProcess = Math.min(BUFFER_SIZE, sampleLength - position);

            for (int i = 0; i < framesToProcess; i++) {
                inputL[i] = sampleDataL[position + i];
                inputR[i] = sampleStereo ? sampleDataR[position + i] : sampleDataL[position + i];
            }

            for (int i = framesToProcess; i < BUFFER_SIZE; i++) {
                inputL[i] = 0;
                inputR[i] = 0;
            }

            synchronized (this) {
                convolverL.process(inputL, outputL, BUFFER_SIZE);
                convolverR.process(inputR, outputR, BUFFER_SIZE);
            }

            for (int i = 0; i < BUFFER_SIZE; i++) {
                float l = Math.max(-1, Math.min(1, outputL[i]));
                float r = Math.max(-1, Math.min(1, outputR[i]));

                short sl = (short) (l * 32767);
                short sr = (short) (r * 32767);

                outputBytes[i * 4] = (byte) (sl & 0xFF);
                outputBytes[i * 4 + 1] = (byte) ((sl >> 8) & 0xFF);
                outputBytes[i * 4 + 2] = (byte) (sr & 0xFF);
                outputBytes[i * 4 + 3] = (byte) ((sr >> 8) & 0xFF);
            }

            audioLine.write(outputBytes, 0, BUFFER_SIZE * 4);
            position += framesToProcess;

            final int pos = position;
            SwingUtilities.invokeLater(() -> {
                if (isPlaying && currentIR != null) {
                    float progress = pos / (float) sampleLength * 100;
                    statusLabel.setText(String.format("Playing: %s (%.0f%%)", currentIR.name, progress));
                }
            });
        }

        SwingUtilities.invokeLater(() -> {
            if (isPlaying) {
                stopPlayback();
                statusLabel.setText("Playback finished");
            }
        });
    }

    private void stopPlayback() {
        isPlaying = false;

        if (playbackThread != null) {
            try {
                playbackThread.join(500);
            } catch (InterruptedException ignored) {}
            playbackThread = null;
        }

        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
            audioLine = null;
        }

        convolverL = null;
        convolverR = null;
        updatePlayButtonState();
    }

    // ==================== IR VISUALIZATION PANEL ====================

    private static class IRVisualizationPanel extends JPanel {
        private float[] irData;
        private int sampleRate;
        private String irName;
        private float[] spectrum;

        public IRVisualizationPanel() {
            setBackground(DarkTheme.BG_MEDIUM);
        }

        public void setIRData(float[] data, int sampleRate, String name) {
            this.irData = data;
            this.sampleRate = sampleRate;
            this.irName = name;
            this.spectrum = computeSpectrum(data);
            repaint();
        }

        private float[] computeSpectrum(float[] data) {
            if (data == null || data.length == 0) return null;

            // Use power of 2 FFT size
            int fftSize = 2048;
            float[] real = new float[fftSize];
            float[] imag = new float[fftSize];

            // Copy data with windowing (Hann window)
            int copyLen = Math.min(data.length, fftSize);
            for (int i = 0; i < copyLen; i++) {
                float window = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (copyLen - 1)));
                real[i] = data[i] * window;
            }

            // Simple DFT (for visualization only)
            float[] magnitude = new float[fftSize / 2];
            for (int k = 0; k < fftSize / 2; k++) {
                float re = 0, im = 0;
                for (int n = 0; n < fftSize; n++) {
                    float angle = (float) (2 * Math.PI * k * n / fftSize);
                    re += real[n] * Math.cos(angle);
                    im -= real[n] * Math.sin(angle);
                }
                magnitude[k] = (float) Math.sqrt(re * re + im * im);
            }

            // Convert to dB
            float maxMag = 0;
            for (float m : magnitude) maxMag = Math.max(maxMag, m);

            float[] spectrum = new float[magnitude.length];
            for (int i = 0; i < magnitude.length; i++) {
                if (maxMag > 0 && magnitude[i] > 0) {
                    spectrum[i] = 20 * (float) Math.log10(magnitude[i] / maxMag);
                } else {
                    spectrum[i] = -100;
                }
            }

            return spectrum;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int halfWidth = width / 2 - 10;

            // Draw labels
            g2.setColor(DarkTheme.TEXT_SECONDARY);
            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            g2.drawString("Time Domain", 10, 15);
            g2.drawString("Frequency Response", halfWidth + 20, 15);

            if (irData == null || irData.length == 0) {
                g2.setColor(DarkTheme.TEXT_DISABLED);
                g2.drawString("Select an IR to view", width / 2 - 60, height / 2);
                return;
            }

            int plotTop = 25;
            int plotHeight = height - plotTop - 10;

            // Draw time domain waveform (left half)
            drawWaveform(g2, 5, plotTop, halfWidth - 5, plotHeight);

            // Draw frequency response (right half)
            drawSpectrum(g2, halfWidth + 10, plotTop, halfWidth - 5, plotHeight);
        }

        private void drawWaveform(Graphics2D g2, int x, int y, int w, int h) {
            // Background
            g2.setColor(DarkTheme.BG_DARK);
            g2.fillRect(x, y, w, h);

            // Grid
            g2.setColor(DarkTheme.GRID_LINE);
            int midY = y + h / 2;
            g2.drawLine(x, midY, x + w, midY);

            // Time markers
            g2.setColor(DarkTheme.TEXT_DISABLED);
            g2.setFont(getFont().deriveFont(9f));
            float duration = irData.length / (float) sampleRate;
            g2.drawString("0", x + 2, y + h - 2);
            g2.drawString(String.format("%.2fs", duration), x + w - 30, y + h - 2);

            // Waveform
            g2.setColor(DarkTheme.ACCENT_PRIMARY);
            Path2D path = new Path2D.Float();

            float peak = 0;
            for (float v : irData) peak = Math.max(peak, Math.abs(v));
            if (peak < 0.001f) peak = 1;

            int step = Math.max(1, irData.length / w);
            boolean first = true;

            for (int i = 0; i < w; i++) {
                int sampleIdx = i * irData.length / w;
                float maxVal = 0;
                for (int j = 0; j < step && sampleIdx + j < irData.length; j++) {
                    maxVal = Math.max(maxVal, Math.abs(irData[sampleIdx + j]));
                }

                float normalized = maxVal / peak;
                int yPos = midY - (int) (normalized * (h / 2 - 5));

                if (first) {
                    path.moveTo(x + i, yPos);
                    first = false;
                } else {
                    path.lineTo(x + i, yPos);
                }
            }

            g2.draw(path);

            // Draw envelope (mirrored)
            g2.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRGB() & 0x40FFFFFF, true));
            Path2D envelope = new Path2D.Float();
            envelope.moveTo(x, midY);

            for (int i = 0; i < w; i++) {
                int sampleIdx = i * irData.length / w;
                float maxVal = 0;
                for (int j = 0; j < step && sampleIdx + j < irData.length; j++) {
                    maxVal = Math.max(maxVal, Math.abs(irData[sampleIdx + j]));
                }
                float normalized = maxVal / peak;
                envelope.lineTo(x + i, midY - (int) (normalized * (h / 2 - 5)));
            }

            for (int i = w - 1; i >= 0; i--) {
                int sampleIdx = i * irData.length / w;
                float maxVal = 0;
                for (int j = 0; j < step && sampleIdx + j < irData.length; j++) {
                    maxVal = Math.max(maxVal, Math.abs(irData[sampleIdx + j]));
                }
                float normalized = maxVal / peak;
                envelope.lineTo(x + i, midY + (int) (normalized * (h / 2 - 5)));
            }

            envelope.closePath();
            g2.fill(envelope);
        }

        private void drawSpectrum(Graphics2D g2, int x, int y, int w, int h) {
            // Background
            g2.setColor(DarkTheme.BG_DARK);
            g2.fillRect(x, y, w, h);

            if (spectrum == null) return;

            // Grid lines for dB
            g2.setColor(DarkTheme.GRID_LINE);
            for (int db = -60; db <= 0; db += 20) {
                int yPos = y + (int) ((-db / 80.0f) * h);
                g2.drawLine(x, yPos, x + w, yPos);
            }

            // Frequency markers (logarithmic scale)
            g2.setColor(DarkTheme.TEXT_DISABLED);
            g2.setFont(getFont().deriveFont(9f));
            int[] freqs = {100, 1000, 10000};
            for (int freq : freqs) {
                float logPos = (float) (Math.log10(freq) - Math.log10(20)) /
                               (float) (Math.log10(20000) - Math.log10(20));
                int xPos = x + (int) (logPos * w);
                g2.drawLine(xPos, y, xPos, y + h);
                g2.drawString(freq >= 1000 ? (freq / 1000) + "k" : String.valueOf(freq), xPos - 10, y + h - 2);
            }

            // dB labels
            g2.drawString("0dB", x + 2, y + 10);
            g2.drawString("-60dB", x + 2, y + h - 15);

            // Spectrum curve
            g2.setColor(DarkTheme.ACCENT_SUCCESS);
            Path2D path = new Path2D.Float();
            boolean first = true;

            float nyquist = sampleRate / 2.0f;

            for (int i = 0; i < w; i++) {
                // Logarithmic frequency mapping
                float logPos = i / (float) w;
                float freq = (float) (20 * Math.pow(1000, logPos)); // 20 Hz to 20 kHz

                int binIndex = (int) (freq / nyquist * spectrum.length);
                binIndex = Math.max(0, Math.min(spectrum.length - 1, binIndex));

                float db = Math.max(-80, spectrum[binIndex]);
                int yPos = y + (int) ((-db / 80.0f) * h);
                yPos = Math.max(y, Math.min(y + h, yPos));

                if (first) {
                    path.moveTo(x + i, yPos);
                    first = false;
                } else {
                    path.lineTo(x + i, yPos);
                }
            }

            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(path);

            // Fill under curve
            g2.setColor(new Color(DarkTheme.ACCENT_SUCCESS.getRGB() & 0x30FFFFFF, true));
            path.lineTo(x + w, y + h);
            path.lineTo(x, y + h);
            path.closePath();
            g2.fill(path);
        }
    }

    // ==================== DATA CLASSES ====================

    private static class IRInfo {
        final String name;
        final Path path;
        final int samples;
        final int sampleRate;
        final int channels;
        final float duration;

        IRInfo(String name, Path path, int samples, int sampleRate, int channels, float duration) {
            this.name = name;
            this.path = path;
            this.samples = samples;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.duration = duration;
        }
    }

    private static class IRTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Name", "Samples", "Duration", "Channels", "Sample Rate"};
        private List<IRInfo> data = new ArrayList<>();

        public void setData(List<IRInfo> data) {
            this.data = data;
            fireTableDataChanged();
        }

        public List<IRInfo> getData() {
            return data;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            IRInfo ir = data.get(row);
            return switch (col) {
                case 0 -> ir.name;
                case 1 -> String.format("%,d", ir.samples);
                case 2 -> String.format("%.2fs", ir.duration);
                case 3 -> ir.channels == 1 ? "Mono" : "Stereo";
                case 4 -> String.format("%,d Hz", ir.sampleRate);
                default -> "";
            };
        }
    }
}
