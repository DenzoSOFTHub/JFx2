package it.denzosoft.jfx2.ui;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Splash screen shown during application startup.
 * Displays app name, producer, version and loading progress.
 */
public class SplashScreen extends JWindow {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 220;

    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final String version;

    public SplashScreen() {
        this.version = loadVersion();

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x1a1a2e),
                        0, getHeight(), new Color(0x16213e)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Border
                g2d.setColor(new Color(0x0f3460));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
        };
        mainPanel.setLayout(new BorderLayout());

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        // App name
        JLabel appNameLabel = new JLabel("JFx2");
        appNameLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        appNameLabel.setForeground(new Color(0xe94560));
        appNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(appNameLabel);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Guitar Multi-Effects Processor");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(0xaaaaaa));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);

        contentPanel.add(Box.createVerticalStrut(20));

        // Producer
        JLabel producerLabel = new JLabel("by DenzoSOFT");
        producerLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        producerLabel.setForeground(new Color(0x00d9ff));
        producerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(producerLabel);

        contentPanel.add(Box.createVerticalStrut(5));

        // Version
        JLabel versionLabel = new JLabel("Version " + version);
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(0x888888));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(versionLabel);

        contentPanel.add(Box.createVerticalGlue());

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Bottom panel with progress bar
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 20, 40));

        // Status label
        statusLabel = new JLabel("Initializing...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        statusLabel.setForeground(new Color(0x666666));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(statusLabel);

        bottomPanel.add(Box.createVerticalStrut(8));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(WIDTH - 80, 8));
        progressBar.setMaximumSize(new Dimension(WIDTH - 80, 8));
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(0x2a2a3e));
        progressBar.setForeground(new Color(0xe94560));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Custom UI for progress bar
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = c.getWidth();
                int height = c.getHeight();

                // Background
                g2d.setColor(new Color(0x2a2a3e));
                g2d.fillRoundRect(0, 0, width, height, height, height);

                // Progress
                int progressWidth = (int) (width * (progressBar.getValue() / 100.0));
                if (progressWidth > 0) {
                    GradientPaint gradient = new GradientPaint(
                            0, 0, new Color(0xe94560),
                            progressWidth, 0, new Color(0x00d9ff)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(0, 0, progressWidth, height, height, height);
                }
            }
        });

        bottomPanel.add(progressBar);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Load version from JAR manifest or return default.
     */
    private String loadVersion() {
        try {
            InputStream is = getClass().getResourceAsStream("/META-INF/MANIFEST.MF");
            if (is != null) {
                Manifest manifest = new Manifest(is);
                String implVersion = manifest.getMainAttributes().getValue("Implementation-Version");
                if (implVersion != null && !implVersion.isEmpty()) {
                    return implVersion;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Try to read from build.properties file (development mode)
        try {
            java.nio.file.Path buildPropsPath = java.nio.file.Path.of("build.properties");
            if (java.nio.file.Files.exists(buildPropsPath)) {
                java.util.Properties props = new java.util.Properties();
                props.load(java.nio.file.Files.newInputStream(buildPropsPath));
                String buildNum = props.getProperty("build.number", "0");
                return "2.0." + buildNum;
            }
        } catch (Exception e) {
            // Ignore
        }

        return "2.0.0-dev";
    }

    /**
     * Update progress and status message.
     */
    public void setProgress(int percent, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percent);
            statusLabel.setText(status);
        });
    }

    /**
     * Show the splash screen.
     */
    public void showSplash() {
        setVisible(true);
        toFront();
    }

    /**
     * Close the splash screen.
     */
    public void closeSplash() {
        setVisible(false);
        dispose();
    }

    /**
     * Get the loaded version string.
     */
    public String getVersion() {
        return version;
    }
}
