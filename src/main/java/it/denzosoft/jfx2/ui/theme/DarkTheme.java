package it.denzosoft.jfx2.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.Component;

/**
 * Dark theme configuration for JFx2.
 * Applies a professional dark color scheme via UIManager.
 */
public final class DarkTheme {

    // ==================== BACKGROUND COLORS ====================
    public static final Color BG_DARK = new Color(0x1e1e1e);
    public static final Color BG_MEDIUM = new Color(0x252526);
    public static final Color BG_LIGHT = new Color(0x2d2d30);
    public static final Color BG_LIGHTER = new Color(0x3c3c3c);

    // ==================== ACCENT COLORS ====================
    public static final Color ACCENT_PRIMARY = new Color(0x00a8ff);
    public static final Color ACCENT_SECONDARY = new Color(0x569cd6);
    public static final Color ACCENT_SUCCESS = new Color(0x4caf50);
    public static final Color ACCENT_WARNING = new Color(0xff9800);
    public static final Color ACCENT_ERROR = new Color(0xf44336);

    // ==================== TEXT COLORS ====================
    public static final Color TEXT_PRIMARY = new Color(0xe0e0e0);
    public static final Color TEXT_SECONDARY = new Color(0x9e9e9e);
    public static final Color TEXT_DISABLED = new Color(0x616161);
    public static final Color TEXT_ACCENT = new Color(0x4fc3f7);

    // ==================== CANVAS COLORS ====================
    public static final Color CANVAS_BG = new Color(0x1a1a1a);
    public static final Color GRID_LINE = new Color(0x2a2a2a);
    public static final Color GRID_LINE_MAJOR = new Color(0x3a3a3a);

    // ==================== BLOCK COLORS ====================
    public static final Color BLOCK_BG = new Color(0x37373d);
    public static final Color BLOCK_BG_GRADIENT_TOP = new Color(0x404045);
    public static final Color BLOCK_BG_GRADIENT_BOTTOM = new Color(0x2d2d32);
    public static final Color BLOCK_BORDER = new Color(0x4a4a4a);
    public static final Color BLOCK_BORDER_SELECTED = new Color(0x00a8ff);
    public static final Color BLOCK_HEADER = new Color(0x2a2a2e);
    public static final Color BLOCK_SHADOW = new Color(0x0a0a0a);

    // ==================== PORT COLORS ====================
    public static final Color PORT_AUDIO = new Color(0x4caf50);
    public static final Color PORT_AUDIO_HOVER = new Color(0x81c784);
    public static final Color PORT_CONTROL = new Color(0xff9800);
    public static final Color PORT_CONTROL_HOVER = new Color(0xffb74d);
    public static final Color PORT_BORDER = new Color(0x2d2d30);

    // ==================== CONNECTION COLORS ====================
    public static final Color CONNECTION_NORMAL = new Color(0x569cd6);
    public static final Color CONNECTION_MONO = new Color(0x6a9fb5);     // Blue-gray for mono
    public static final Color CONNECTION_STEREO = new Color(0x8bc34a);   // Green for stereo
    public static final Color CONNECTION_SELECTED = new Color(0x00a8ff);
    public static final Color CONNECTION_VALID = new Color(0x4caf50);
    public static final Color CONNECTION_INVALID = new Color(0xf44336);
    public static final Color CONNECTION_DRAGGING = new Color(0xffffff, true);

    // ==================== UI COMPONENT COLORS ====================
    public static final Color BUTTON_BG = new Color(0x3c3c3c);
    public static final Color BUTTON_BG_HOVER = new Color(0x4a4a4a);
    public static final Color BUTTON_BG_PRESSED = new Color(0x2d2d30);
    public static final Color BUTTON_BORDER = new Color(0x555555);

    public static final Color INPUT_BG = new Color(0x2d2d30);
    public static final Color INPUT_BORDER = new Color(0x3c3c3c);
    public static final Color INPUT_BORDER_FOCUS = new Color(0x00a8ff);

    public static final Color SCROLLBAR_TRACK = new Color(0x2d2d30);
    public static final Color SCROLLBAR_THUMB = new Color(0x555555);
    public static final Color SCROLLBAR_THUMB_HOVER = new Color(0x666666);

    public static final Color SELECTION_BG = new Color(0x264f78);
    public static final Color SELECTION_TEXT = new Color(0xffffff);

    public static final Color MENU_BG = new Color(0x2d2d30);
    public static final Color MENU_BORDER = new Color(0x454545);
    public static final Color MENU_SELECTION = new Color(0x094771);

    public static final Color TOOLTIP_BG = new Color(0x3c3c3c);
    public static final Color TOOLTIP_BORDER = new Color(0x555555);

    // ==================== STATUS BAR COLORS ====================
    public static final Color STATUS_BG = new Color(0x007acc);
    public static final Color STATUS_TEXT = new Color(0xffffff);

    // ==================== METER COLORS ====================
    public static final Color METER_LOW = new Color(0x4caf50);
    public static final Color METER_MID = new Color(0xffeb3b);
    public static final Color METER_HIGH = new Color(0xf44336);
    public static final Color METER_BG = new Color(0x1e1e1e);

    // ==================== FONTS ====================
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LARGE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // ==================== CATEGORY COLORS (for effect blocks) ====================
    // Each of the 13 effect categories has a unique color
    public static final Color CATEGORY_INPUT_SOURCE = new Color(0x8bc34a);  // Light green
    public static final Color CATEGORY_OUTPUT_SINK = new Color(0xff5722);   // Deep orange
    public static final Color CATEGORY_DYNAMICS = new Color(0x9c27b0);      // Purple
    public static final Color CATEGORY_DISTORTION = new Color(0xf44336);    // Red
    public static final Color CATEGORY_MODULATION = new Color(0x2196f3);    // Blue
    public static final Color CATEGORY_DELAY = new Color(0x00bcd4);         // Cyan
    public static final Color CATEGORY_REVERB = new Color(0x009688);        // Teal
    public static final Color CATEGORY_EQ = new Color(0x4caf50);            // Green
    public static final Color CATEGORY_FILTER = new Color(0x26a69a);        // Teal variant
    public static final Color CATEGORY_AMP_SIM = new Color(0xffc107);       // Amber
    public static final Color CATEGORY_PITCH = new Color(0xff9800);         // Orange
    public static final Color CATEGORY_ACOUSTIC = new Color(0x795548);      // Brown
    public static final Color CATEGORY_UTILITY = new Color(0x607d8b);       // Blue-grey

    private DarkTheme() {
        // Utility class
    }

    /**
     * Apply the dark theme to the entire application via UIManager.
     * Call this before creating any Swing components.
     */
    public static void apply() {
        // Use cross-platform look and feel as base
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default
        }

        // Panel
        UIManager.put("Panel.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Panel.foreground", new ColorUIResource(TEXT_PRIMARY));

        // Label
        UIManager.put("Label.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Label.font", FONT_REGULAR);

        // Button
        UIManager.put("Button.background", new ColorUIResource(BUTTON_BG));
        UIManager.put("Button.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Button.font", FONT_REGULAR);
        UIManager.put("Button.select", new ColorUIResource(BUTTON_BG_PRESSED));
        UIManager.put("Button.focus", new ColorUIResource(ACCENT_PRIMARY));
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        // Metal L&F specific - disable gradient for flat look
        UIManager.put("Button.gradient", null);
        UIManager.put("Button.highlight", new ColorUIResource(BUTTON_BG_HOVER));
        UIManager.put("Button.light", new ColorUIResource(BUTTON_BG));
        UIManager.put("Button.shadow", new ColorUIResource(BUTTON_BG.darker()));
        UIManager.put("Button.darkShadow", new ColorUIResource(BUTTON_BG.darker().darker()));

        // ToggleButton
        UIManager.put("ToggleButton.background", new ColorUIResource(BUTTON_BG));
        UIManager.put("ToggleButton.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("ToggleButton.select", new ColorUIResource(ACCENT_PRIMARY));

        // TextField
        UIManager.put("TextField.background", new ColorUIResource(INPUT_BG));
        UIManager.put("TextField.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TextField.caretForeground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        // TextArea
        UIManager.put("TextArea.background", new ColorUIResource(INPUT_BG));
        UIManager.put("TextArea.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TextArea.caretForeground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TextArea.font", FONT_MONO);

        // ComboBox
        UIManager.put("ComboBox.background", new ColorUIResource(INPUT_BG));
        UIManager.put("ComboBox.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(SELECTION_BG));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("ComboBox.font", FONT_REGULAR);

        // List
        UIManager.put("List.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("List.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("List.selectionBackground", new ColorUIResource(SELECTION_BG));
        UIManager.put("List.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("List.font", FONT_REGULAR);

        // Table
        UIManager.put("Table.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Table.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Table.selectionBackground", new ColorUIResource(SELECTION_BG));
        UIManager.put("Table.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("Table.gridColor", new ColorUIResource(BG_LIGHTER));
        UIManager.put("TableHeader.background", new ColorUIResource(BG_LIGHT));
        UIManager.put("TableHeader.foreground", new ColorUIResource(TEXT_PRIMARY));

        // Tree
        UIManager.put("Tree.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Tree.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Tree.textBackground", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Tree.textForeground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Tree.selectionBackground", new ColorUIResource(SELECTION_BG));
        UIManager.put("Tree.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("Tree.hash", new ColorUIResource(BG_LIGHTER));
        UIManager.put("Tree.font", FONT_REGULAR);

        // ScrollPane
        UIManager.put("ScrollPane.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BG_LIGHT));

        // ScrollBar
        UIManager.put("ScrollBar.background", new ColorUIResource(SCROLLBAR_TRACK));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(SCROLLBAR_THUMB));
        UIManager.put("ScrollBar.thumbHighlight", new ColorUIResource(SCROLLBAR_THUMB_HOVER));
        UIManager.put("ScrollBar.track", new ColorUIResource(SCROLLBAR_TRACK));
        UIManager.put("ScrollBar.width", 12);

        // Menu
        UIManager.put("Menu.background", new ColorUIResource(MENU_BG));
        UIManager.put("Menu.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Menu.selectionBackground", new ColorUIResource(MENU_SELECTION));
        UIManager.put("Menu.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("Menu.font", FONT_REGULAR);

        // MenuBar
        UIManager.put("MenuBar.background", new ColorUIResource(BG_LIGHT));
        UIManager.put("MenuBar.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, MENU_BORDER));

        // MenuItem
        UIManager.put("MenuItem.background", new ColorUIResource(MENU_BG));
        UIManager.put("MenuItem.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(MENU_SELECTION));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(SELECTION_TEXT));
        UIManager.put("MenuItem.acceleratorForeground", new ColorUIResource(TEXT_SECONDARY));
        UIManager.put("MenuItem.font", FONT_REGULAR);

        // PopupMenu
        UIManager.put("PopupMenu.background", new ColorUIResource(MENU_BG));
        UIManager.put("PopupMenu.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(MENU_BORDER));

        // Separator
        UIManager.put("Separator.foreground", new ColorUIResource(BG_LIGHTER));
        UIManager.put("Separator.background", new ColorUIResource(BG_MEDIUM));

        // SplitPane
        UIManager.put("SplitPane.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.draggingColor", new ColorUIResource(ACCENT_PRIMARY));

        // TabbedPane
        UIManager.put("TabbedPane.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("TabbedPane.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TabbedPane.selected", new ColorUIResource(BG_LIGHT));
        UIManager.put("TabbedPane.contentAreaColor", new ColorUIResource(BG_MEDIUM));
        UIManager.put("TabbedPane.focus", new ColorUIResource(ACCENT_PRIMARY));

        // ToolBar
        UIManager.put("ToolBar.background", new ColorUIResource(BG_LIGHT));
        UIManager.put("ToolBar.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("ToolBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, MENU_BORDER));
        UIManager.put("ToolBar.dockingBackground", new ColorUIResource(BG_LIGHT));
        UIManager.put("ToolBar.floatingBackground", new ColorUIResource(BG_LIGHT));

        // ToolTip
        UIManager.put("ToolTip.background", new ColorUIResource(TOOLTIP_BG));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TOOLTIP_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        UIManager.put("ToolTip.font", FONT_SMALL);

        // Slider
        UIManager.put("Slider.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Slider.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("Slider.focus", new ColorUIResource(ACCENT_PRIMARY));

        // ProgressBar
        UIManager.put("ProgressBar.background", new ColorUIResource(BG_LIGHT));
        UIManager.put("ProgressBar.foreground", new ColorUIResource(ACCENT_PRIMARY));
        UIManager.put("ProgressBar.selectionBackground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(BG_DARK));

        // CheckBox
        UIManager.put("CheckBox.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("CheckBox.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("CheckBox.font", FONT_REGULAR);

        // RadioButton
        UIManager.put("RadioButton.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("RadioButton.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("RadioButton.font", FONT_REGULAR);

        // Spinner
        UIManager.put("Spinner.background", new ColorUIResource(INPUT_BG));
        UIManager.put("Spinner.foreground", new ColorUIResource(TEXT_PRIMARY));

        // OptionPane
        UIManager.put("OptionPane.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("OptionPane.foreground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("OptionPane.messageFont", FONT_REGULAR);
        UIManager.put("OptionPane.buttonFont", FONT_REGULAR);
        UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(8, 0, 0, 0));
        UIManager.put("OptionPane.buttonMinimumWidth", 80);

        // Panel used inside OptionPane
        UIManager.put("Panel.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("Panel.foreground", new ColorUIResource(TEXT_PRIMARY));

        // FileChooser
        UIManager.put("FileChooser.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("FileChooser.foreground", new ColorUIResource(TEXT_PRIMARY));

        // InternalFrame
        UIManager.put("InternalFrame.background", new ColorUIResource(BG_MEDIUM));
        UIManager.put("InternalFrame.titleFont", FONT_BOLD);

        // Desktop
        UIManager.put("Desktop.background", new ColorUIResource(BG_DARK));

        // TitledBorder
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(TEXT_PRIMARY));
        UIManager.put("TitledBorder.font", FONT_BOLD);
    }

    /**
     * Get a color for an effect category.
     * Accepts EffectCategory.name() values (e.g., "INPUT_SOURCE", "DYNAMICS").
     */
    public static Color getCategoryColor(String category) {
        if (category == null) return CATEGORY_UTILITY;

        return switch (category.toUpperCase()) {
            case "INPUT_SOURCE" -> CATEGORY_INPUT_SOURCE;
            case "OUTPUT_SINK" -> CATEGORY_OUTPUT_SINK;
            case "DYNAMICS" -> CATEGORY_DYNAMICS;
            case "DISTORTION" -> CATEGORY_DISTORTION;
            case "MODULATION" -> CATEGORY_MODULATION;
            case "DELAY" -> CATEGORY_DELAY;
            case "REVERB" -> CATEGORY_REVERB;
            case "EQ" -> CATEGORY_EQ;
            case "FILTER" -> CATEGORY_FILTER;
            case "AMP_SIM" -> CATEGORY_AMP_SIM;
            case "PITCH" -> CATEGORY_PITCH;
            case "ACOUSTIC" -> CATEGORY_ACOUSTIC;
            case "UTILITY" -> CATEGORY_UTILITY;
            default -> CATEGORY_UTILITY;
        };
    }

    /**
     * Create a border with rounded corners (for custom painting).
     */
    public static Border createRoundedBorder(Color color, int radius) {
        return BorderFactory.createLineBorder(color, 1, true);
    }

    /**
     * Style a button with proper dark theme colors.
     * Ensures the background color is actually applied.
     */
    public static void styleButton(javax.swing.JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
    }

    /**
     * Style a standard dialog button (dark background).
     */
    public static void styleDialogButton(javax.swing.JButton button) {
        styleButton(button, BUTTON_BG, TEXT_PRIMARY);
    }

    /**
     * Style a primary action button (accent color).
     */
    public static void stylePrimaryButton(javax.swing.JButton button) {
        styleButton(button, ACCENT_PRIMARY, TEXT_PRIMARY);
    }

    /**
     * Style a toggle button with proper dark theme colors.
     */
    public static void styleToggleButton(javax.swing.JToggleButton button) {
        button.setBackground(BUTTON_BG);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BG.darker(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    /**
     * Show a styled confirmation dialog with Yes/No/Cancel buttons.
     * Works around Metal L&F button styling issues.
     *
     * @param parent  Parent component
     * @param message Dialog message
     * @param title   Dialog title
     * @return JOptionPane.YES_OPTION, NO_OPTION, or CANCEL_OPTION
     */
    public static int showConfirmDialog(Component parent, String message, String title) {
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        JButton cancelButton = new JButton("Cancel");

        styleDialogButton(yesButton);
        styleDialogButton(noButton);
        styleDialogButton(cancelButton);

        Object[] options = {yesButton, noButton, cancelButton};

        JOptionPane pane = new JOptionPane(
                message,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                null,
                options,
                yesButton
        );

        JDialog dialog = pane.createDialog(parent, title);
        dialog.getContentPane().setBackground(BG_MEDIUM);

        // Set up button actions
        final int[] result = {JOptionPane.CANCEL_OPTION};
        yesButton.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });
        noButton.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            result[0] = JOptionPane.CANCEL_OPTION;
            dialog.dispose();
        });

        dialog.setVisible(true);
        return result[0];
    }

    /**
     * Show a styled message dialog.
     *
     * @param parent      Parent component
     * @param message     Dialog message
     * @param title       Dialog title
     * @param messageType JOptionPane message type constant
     */
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JButton okButton = new JButton("OK");
        styleDialogButton(okButton);

        Object[] options = {okButton};

        JOptionPane pane = new JOptionPane(
                message,
                messageType,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                okButton
        );

        JDialog dialog = pane.createDialog(parent, title);
        dialog.getContentPane().setBackground(BG_MEDIUM);

        okButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Show a styled input dialog.
     *
     * @param parent       Parent component
     * @param message      Dialog message
     * @param title        Dialog title
     * @param initialValue Initial value in text field
     * @return User input or null if cancelled
     */
    public static String showInputDialog(Component parent, String message, String title, String initialValue) {
        JTextField textField = new JTextField(initialValue != null ? initialValue : "", 20);
        textField.setBackground(INPUT_BG);
        textField.setForeground(TEXT_PRIMARY);
        textField.setCaretColor(TEXT_PRIMARY);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        Object[] inputComponents = {message, textField};
        Object[] options = {okButton, cancelButton};

        JOptionPane pane = new JOptionPane(
                inputComponents,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options,
                okButton
        );

        JDialog dialog = pane.createDialog(parent, title);
        dialog.getContentPane().setBackground(BG_MEDIUM);

        final String[] result = {null};
        okButton.addActionListener(e -> {
            result[0] = textField.getText();
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        // Allow Enter key to submit
        textField.addActionListener(e -> {
            result[0] = textField.getText();
            dialog.dispose();
        });

        dialog.setVisible(true);
        return result[0];
    }
}
