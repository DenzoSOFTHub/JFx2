package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.preset.RigMetadata;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for saving a rig with metadata.
 */
public class SaveRigDialog extends JDialog {

    // UI Components
    private JTextField nameField;
    private JTextField authorField;
    private JComboBox<String> categoryCombo;
    private JTextField tagsField;
    private JTextArea descriptionArea;

    // State
    private boolean confirmed = false;
    private RigMetadata result;

    // Categories
    private static final String[] CATEGORIES = {
            "Clean", "Crunch", "Lead", "Ambient", "Metal", "Blues",
            "Jazz", "Country", "Funk", "Experimental", "Custom"
    };

    public SaveRigDialog(Frame owner, RigMetadata existingMetadata) {
        super(owner, "Save Rig", true);

        setSize(450, 400);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents(existingMetadata);
    }

    private void initComponents(RigMetadata existingMetadata) {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(DarkTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Form panel
        JPanel formPanel = createFormPanel(existingMetadata);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createFormPanel(RigMetadata meta) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        nameField = createTextField(meta != null ? meta.name() : "");
        panel.add(nameField, gbc);

        // Author
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Author:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        authorField = createTextField(meta != null ? meta.author() : System.getProperty("user.name", "User"));
        panel.add(authorField, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Category:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        categoryCombo = new JComboBox<>(CATEGORIES);
        categoryCombo.setBackground(DarkTheme.BG_MEDIUM);
        categoryCombo.setForeground(DarkTheme.TEXT_PRIMARY);
        categoryCombo.setEditable(true);
        if (meta != null && meta.category() != null) {
            categoryCombo.setSelectedItem(meta.category());
        }
        panel.add(categoryCombo, gbc);

        // Tags
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(createLabel("Tags:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        tagsField = createTextField(meta != null ? meta.tags() : "");
        tagsField.setToolTipText("Comma-separated tags for search");
        panel.add(tagsField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(createLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        descriptionArea = new JTextArea(meta != null ? meta.description() : "");
        descriptionArea.setBackground(DarkTheme.BG_MEDIUM);
        descriptionArea.setForeground(DarkTheme.TEXT_PRIMARY);
        descriptionArea.setCaretColor(DarkTheme.TEXT_PRIMARY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT));
        scrollPane.setPreferredSize(new Dimension(200, 100));
        panel.add(scrollPane, gbc);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DarkTheme.TEXT_SECONDARY);
        label.setFont(DarkTheme.FONT_REGULAR);
        return label;
    }

    private JTextField createTextField(String text) {
        JTextField field = new JTextField(text);
        field.setBackground(DarkTheme.BG_MEDIUM);
        field.setForeground(DarkTheme.TEXT_PRIMARY);
        field.setCaretColor(DarkTheme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        field.setPreferredSize(new Dimension(250, 28));
        return field;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        DarkTheme.styleDialogButton(cancelBtn);
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save");
        DarkTheme.stylePrimaryButton(saveBtn);
        saveBtn.addActionListener(e -> {
            if (validateInput()) {
                createResult();
                confirmed = true;
                dispose();
            }
        });

        panel.add(cancelBtn);
        panel.add(saveBtn);

        return panel;
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a name for the preset.");
            nameField.requestFocus();
            return false;
        }

        // Check for invalid filename characters
        if (name.matches(".*[<>:\"/\\\\|?*].*")) {
            showError("Name contains invalid characters.");
            nameField.requestFocus();
            return false;
        }

        return true;
    }

    private void showError(String message) {
        DarkTheme.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    private void createResult() {
        String now = java.time.Instant.now().toString();
        result = new RigMetadata(
                nameField.getText().trim(),
                authorField.getText().trim(),
                descriptionArea.getText().trim(),
                (String) categoryCombo.getSelectedItem(),
                tagsField.getText().trim(),
                RigMetadata.CURRENT_VERSION,
                now,
                now
        );
    }

    /**
     * Show the dialog and return the metadata, or null if cancelled.
     */
    public RigMetadata showDialog() {
        setVisible(true);
        return confirmed ? result : null;
    }

    /**
     * Get the suggested filename based on the name.
     */
    public String getSuggestedFilename() {
        if (result == null) return null;
        return result.name().toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_-]", "");
    }
}
