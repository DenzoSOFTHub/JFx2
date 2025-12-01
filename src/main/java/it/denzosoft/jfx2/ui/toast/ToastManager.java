package it.denzosoft.jfx2.ui.toast;

import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Toast notification manager for showing temporary messages.
 */
public class ToastManager {

    private static ToastManager instance;

    private JFrame parentFrame;
    private JLayeredPane layeredPane;
    private final Deque<ToastPanel> activeToasts = new ArrayDeque<>();
    private static final int MAX_TOASTS = 3;
    private static final int TOAST_GAP = 8;
    private static final int MARGIN = 16;

    private ToastManager() {
    }

    public static ToastManager getInstance() {
        if (instance == null) {
            instance = new ToastManager();
        }
        return instance;
    }

    /**
     * Initialize with parent frame.
     */
    public void initialize(JFrame frame) {
        this.parentFrame = frame;
        this.layeredPane = frame.getLayeredPane();
    }

    /**
     * Show an info toast.
     */
    public void showInfo(String message) {
        show(message, ToastType.INFO, 3000);
    }

    /**
     * Show a success toast.
     */
    public void showSuccess(String message) {
        show(message, ToastType.SUCCESS, 3000);
    }

    /**
     * Show a warning toast.
     */
    public void showWarning(String message) {
        show(message, ToastType.WARNING, 4000);
    }

    /**
     * Show an error toast.
     */
    public void showError(String message) {
        show(message, ToastType.ERROR, 5000);
    }

    /**
     * Show a toast with custom duration.
     */
    public void show(String message, ToastType type, int durationMs) {
        if (parentFrame == null || layeredPane == null) {
            System.out.println("[Toast] " + type + ": " + message);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // Remove oldest if at max
            while (activeToasts.size() >= MAX_TOASTS) {
                ToastPanel oldest = activeToasts.pollFirst();
                if (oldest != null) {
                    oldest.dismiss();
                }
            }

            // Create new toast
            ToastPanel toast = new ToastPanel(message, type, durationMs, this::onToastDismissed);
            activeToasts.addLast(toast);

            // Add to layered pane
            layeredPane.add(toast, JLayeredPane.POPUP_LAYER);

            // Position all toasts
            repositionToasts();

            // Show with animation
            toast.display();
        });
    }

    /**
     * Called when a toast is dismissed.
     */
    private void onToastDismissed(ToastPanel toast) {
        SwingUtilities.invokeLater(() -> {
            activeToasts.remove(toast);
            layeredPane.remove(toast);
            repositionToasts();
            layeredPane.repaint();
        });
    }

    /**
     * Reposition all active toasts.
     */
    private void repositionToasts() {
        if (parentFrame == null) return;

        int frameWidth = parentFrame.getWidth();
        int frameHeight = parentFrame.getHeight();

        int y = frameHeight - MARGIN;

        for (ToastPanel toast : activeToasts) {
            Dimension size = toast.getPreferredSize();
            int x = (frameWidth - size.width) / 2;
            y -= size.height;

            toast.setBounds(x, y, size.width, size.height);
            y -= TOAST_GAP;
        }
    }

    /**
     * Dismiss all toasts.
     */
    public void dismissAll() {
        SwingUtilities.invokeLater(() -> {
            for (ToastPanel toast : new ArrayDeque<>(activeToasts)) {
                toast.dismiss();
            }
        });
    }

    /**
     * Toast types.
     */
    public enum ToastType {
        INFO(DarkTheme.ACCENT_PRIMARY, "info"),
        SUCCESS(DarkTheme.ACCENT_SUCCESS, "success"),
        WARNING(DarkTheme.ACCENT_WARNING, "warning"),
        ERROR(DarkTheme.ACCENT_ERROR, "error");

        final Color color;
        final String iconName;

        ToastType(Color color, String iconName) {
            this.color = color;
            this.iconName = iconName;
        }

        Icon getIcon() {
            return IconFactory.getIcon(iconName, 16);
        }
    }

    /**
     * Individual toast panel.
     */
    private static class ToastPanel extends JPanel {

        private static final int PADDING_H = 16;
        private static final int PADDING_V = 12;
        private static final int ICON_SIZE = 16;
        private static final int ICON_GAP = 10;
        private static final int CORNER_RADIUS = 8;

        private final String message;
        private final ToastType type;
        private final int durationMs;
        private final java.util.function.Consumer<ToastPanel> onDismiss;

        private Timer showTimer;
        private Timer hideTimer;
        private float opacity = 0f;
        private boolean dismissed = false;

        public ToastPanel(String message, ToastType type, int durationMs,
                          java.util.function.Consumer<ToastPanel> onDismiss) {
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
            this.onDismiss = onDismiss;

            setOpaque(false);
            calculateSize();
        }

        private void calculateSize() {
            FontMetrics fm = getFontMetrics(DarkTheme.FONT_REGULAR);
            int textWidth = fm.stringWidth(message);

            int width = PADDING_H * 2 + ICON_SIZE + ICON_GAP + textWidth;
            int height = Math.max(PADDING_V * 2 + fm.getHeight(), PADDING_V * 2 + ICON_SIZE);

            // Limit width
            width = Math.min(width, 400);

            setPreferredSize(new Dimension(width, height));
        }

        public void display() {
            // Fade in animation
            showTimer = new Timer(20, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    opacity += 0.1f;
                    if (opacity >= 1f) {
                        opacity = 1f;
                        showTimer.stop();

                        // Start auto-hide timer
                        startHideTimer();
                    }
                    repaint();
                }
            });
            showTimer.start();
        }

        private void startHideTimer() {
            hideTimer = new Timer(durationMs, e -> dismiss());
            hideTimer.setRepeats(false);
            hideTimer.start();
        }

        public void dismiss() {
            if (dismissed) return;
            dismissed = true;

            if (showTimer != null) showTimer.stop();
            if (hideTimer != null) hideTimer.stop();

            // Fade out animation
            Timer fadeOut = new Timer(20, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    opacity -= 0.1f;
                    if (opacity <= 0f) {
                        opacity = 0f;
                        ((Timer) e.getSource()).stop();
                        onDismiss.accept(ToastPanel.this);
                    }
                    repaint();
                }
            });
            fadeOut.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            // Apply opacity
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            // Background
            g2d.setColor(new Color(30, 30, 35, 240));
            g2d.fillRoundRect(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS);

            // Left accent bar
            g2d.setColor(type.color);
            g2d.fillRoundRect(0, 0, 4, h, 2, 2);

            // Icon
            Icon icon = type.getIcon();
            int iconY = (h - ICON_SIZE) / 2;
            icon.paintIcon(this, g2d, PADDING_H, iconY);

            // Message
            g2d.setFont(DarkTheme.FONT_REGULAR);
            g2d.setColor(DarkTheme.TEXT_PRIMARY);
            FontMetrics fm = g2d.getFontMetrics();
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            int textX = PADDING_H + ICON_SIZE + ICON_GAP;
            g2d.drawString(message, textX, textY);

            // Border
            g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), 100));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(0, 0, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS);

            g2d.dispose();
        }
    }
}
