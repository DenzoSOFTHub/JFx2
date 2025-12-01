package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.preset.FavoritesManager;
import it.denzosoft.jfx2.preset.TemplateManager;
import it.denzosoft.jfx2.preset.TemplateSerializer;
import it.denzosoft.jfx2.preset.Rig;
import it.denzosoft.jfx2.preset.RigMetadata;
import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Dialog for browsing and loading rig templates.
 */
public class TemplateBrowserDialog extends JDialog {

    private final TemplateManager templateManager;
    private final FavoritesManager favoritesManager;
    private final TemplateSerializer serializer = new TemplateSerializer();

    // UI Components
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JTable templateTable;
    private TemplateTableModel tableModel;
    private JPanel previewPanel;
    private JLabel previewName;
    private JLabel previewAuthor;
    private JLabel previewCategory;
    private JLabel previewDescription;
    private JLabel previewNodes;
    private RatingPanel previewRating;
    private JToggleButton favoriteButton;

    // State
    private List<TemplateEntry> allTemplates = new ArrayList<>();
    private List<TemplateEntry> filteredTemplates = new ArrayList<>();
    private TemplateEntry selectedTemplate;
    private boolean confirmed = false;

    // Filter modes
    private static final String ALL_CATEGORIES = "All Categories";
    private static final String FAVORITES_FILTER = "Favorites";
    private static final String RECENTLY_USED = "Recently Used";
    private static final String[] CATEGORIES = {
            ALL_CATEGORIES, FAVORITES_FILTER, RECENTLY_USED,
            "---",  // separator
            "Clean", "Crunch", "Lead", "Ambient", "Metal", "Blues", "Custom"
    };

    public TemplateBrowserDialog(Frame owner, TemplateManager templateManager) {
        super(owner, "Template Browser", true);
        this.templateManager = templateManager;
        this.favoritesManager = new FavoritesManager(templateManager.getTemplatesDir());

        setSize(750, 550);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents();
        loadTemplates();
        applyFilter();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(DarkTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top: Search and filter
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: Split pane with list and preview
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(DarkTheme.BG_DARK);
        splitPane.setDividerLocation(400);
        splitPane.setBorder(null);

        // Left: Template list
        JScrollPane tableScroll = createTemplateTable();
        splitPane.setLeftComponent(tableScroll);

        // Right: Preview
        previewPanel = createPreviewPanel();
        splitPane.setRightComponent(previewPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        // Search field
        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        searchPanel.add(searchLabel, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setBackground(DarkTheme.BG_MEDIUM);
        searchField.setForeground(DarkTheme.TEXT_PRIMARY);
        searchField.setCaretColor(DarkTheme.TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Category filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        filterPanel.setOpaque(false);

        JLabel catLabel = new JLabel("Category:");
        catLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        filterPanel.add(catLabel);

        categoryCombo = new JComboBox<>(CATEGORIES);
        categoryCombo.setBackground(DarkTheme.BG_MEDIUM);
        categoryCombo.setForeground(DarkTheme.TEXT_PRIMARY);
        categoryCombo.addActionListener(e -> applyFilter());
        filterPanel.add(categoryCombo);

        panel.add(searchPanel, BorderLayout.CENTER);
        panel.add(filterPanel, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane createTemplateTable() {
        tableModel = new TemplateTableModel();
        templateTable = new JTable(tableModel);
        templateTable.setBackground(DarkTheme.BG_MEDIUM);
        templateTable.setForeground(DarkTheme.TEXT_PRIMARY);
        templateTable.setSelectionBackground(DarkTheme.ACCENT_PRIMARY);
        templateTable.setSelectionForeground(DarkTheme.TEXT_PRIMARY);
        templateTable.setGridColor(DarkTheme.BG_LIGHT);
        templateTable.setRowHeight(28);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateTable.setShowGrid(false);
        templateTable.setIntercellSpacing(new Dimension(0, 1));

        // Custom renderer
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? DarkTheme.BG_MEDIUM : DarkTheme.BG_LIGHT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        for (int i = 0; i < templateTable.getColumnCount(); i++) {
            templateTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Column widths
        templateTable.getColumnModel().getColumn(0).setPreferredWidth(25);
        templateTable.getColumnModel().getColumn(0).setMaxWidth(25);
        templateTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        templateTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        templateTable.getColumnModel().getColumn(3).setPreferredWidth(90);

        // Selection listener
        templateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = templateTable.getSelectedRow();
                if (row >= 0 && row < filteredTemplates.size()) {
                    selectedTemplate = filteredTemplates.get(row);
                    updatePreview();
                }
            }
        });

        // Double-click to load
        templateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectedTemplate != null) {
                    confirmed = true;
                    dispose();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(templateTable);
        scroll.setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT));
        scroll.getViewport().setBackground(DarkTheme.BG_MEDIUM);

        return scroll;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkTheme.BG_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel title = new JLabel("Preview");
        title.setFont(DarkTheme.FONT_BOLD);
        title.setForeground(DarkTheme.TEXT_SECONDARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));

        previewName = createPreviewLabel("", DarkTheme.FONT_BOLD.deriveFont(16f), DarkTheme.TEXT_PRIMARY);
        panel.add(previewName);
        panel.add(Box.createVerticalStrut(4));

        previewAuthor = createPreviewLabel("", DarkTheme.FONT_REGULAR, DarkTheme.TEXT_SECONDARY);
        panel.add(previewAuthor);
        panel.add(Box.createVerticalStrut(8));

        previewCategory = createPreviewLabel("", DarkTheme.FONT_REGULAR, DarkTheme.ACCENT_PRIMARY);
        panel.add(previewCategory);
        panel.add(Box.createVerticalStrut(12));

        previewDescription = createPreviewLabel("", DarkTheme.FONT_REGULAR, DarkTheme.TEXT_SECONDARY);
        panel.add(previewDescription);
        panel.add(Box.createVerticalStrut(16));

        previewNodes = createPreviewLabel("", DarkTheme.FONT_SMALL, DarkTheme.TEXT_DISABLED);
        panel.add(previewNodes);
        panel.add(Box.createVerticalStrut(16));

        // Rating and favorite section
        JPanel ratingSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ratingSection.setOpaque(false);
        ratingSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        ratingSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel ratingLabel = new JLabel("Rating:");
        ratingLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        ratingSection.add(ratingLabel);

        previewRating = new RatingPanel();
        ratingSection.add(previewRating);

        panel.add(ratingSection);
        panel.add(Box.createVerticalStrut(12));

        // Favorite toggle button
        favoriteButton = new JToggleButton("Add to Favorites", IconFactory.getIcon("favorite_empty", 16));
        DarkTheme.styleToggleButton(favoriteButton);
        favoriteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        favoriteButton.setIconTextGap(6);
        favoriteButton.addActionListener(e -> {
            if (selectedTemplate != null) {
                String templateId = getTemplateId(selectedTemplate);
                if (favoriteButton.isSelected()) {
                    favoritesManager.addFavorite(templateId);
                    favoriteButton.setIcon(IconFactory.getIcon("favorite", 16));
                    favoriteButton.setText("Remove from Favorites");
                } else {
                    favoritesManager.removeFavorite(templateId);
                    favoriteButton.setIcon(IconFactory.getIcon("favorite_empty", 16));
                    favoriteButton.setText("Add to Favorites");
                }
                tableModel.fireTableDataChanged();
            }
        });
        panel.add(favoriteButton);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JLabel createPreviewLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        DarkTheme.styleDialogButton(cancelBtn);
        cancelBtn.addActionListener(e -> dispose());

        JButton loadBtn = new JButton("Load");
        DarkTheme.stylePrimaryButton(loadBtn);
        loadBtn.addActionListener(e -> {
            if (selectedTemplate != null) {
                confirmed = true;
                dispose();
            }
        });

        panel.add(cancelBtn);
        panel.add(loadBtn);

        return panel;
    }

    private void loadTemplates() {
        allTemplates.clear();

        // Load from templates directory
        Path templatesDir = templateManager.getTemplatesDir();
        loadTemplatesFromDirectory(templatesDir, false);

        // Load factory templates
        Path factoryDir = templatesDir.resolve("factory");
        loadTemplatesFromDirectory(factoryDir, true);

        // Sort by name
        allTemplates.sort(Comparator.comparing(p -> p.metadata.name()));
    }

    private void loadTemplatesFromDirectory(Path dir, boolean isFactory) {
        if (!Files.isDirectory(dir)) return;

        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".jfxrig"))
                    .forEach(path -> {
                        try {
                            String json = Files.readString(path);
                            Rig rig = serializer.deserialize(json);
                            allTemplates.add(new TemplateEntry(
                                    path.getFileName().toString(),
                                    path,
                                    rig.getMetadata(),
                                    rig.getNodes().size(),
                                    isFactory
                            ));
                        } catch (Exception e) {
                            System.err.println("Error loading template: " + path + " - " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            // Ignore
        }
    }

    private void applyFilter() {
        String searchText = searchField.getText().toLowerCase().trim();
        String category = (String) categoryCombo.getSelectedItem();

        // Skip separator item
        if ("---".equals(category)) {
            categoryCombo.setSelectedItem(ALL_CATEGORIES);
            category = ALL_CATEGORIES;
        }

        filteredTemplates.clear();
        for (TemplateEntry template : allTemplates) {
            String templateId = getTemplateId(template);

            // Category filter
            if (FAVORITES_FILTER.equals(category)) {
                // Show only favorites
                if (!favoritesManager.isFavorite(templateId)) {
                    continue;
                }
            } else if (RECENTLY_USED.equals(category)) {
                // Show only recently used
                if (!favoritesManager.isRecentlyUsed(templateId)) {
                    continue;
                }
            } else if (!ALL_CATEGORIES.equals(category) &&
                    !category.equalsIgnoreCase(template.metadata.category())) {
                continue;
            }

            // Search filter
            if (!searchText.isEmpty()) {
                String name = template.metadata.name().toLowerCase();
                String tags = template.metadata.tags().toLowerCase();
                String desc = template.metadata.description().toLowerCase();
                String author = template.metadata.author().toLowerCase();

                if (!name.contains(searchText) &&
                        !tags.contains(searchText) &&
                        !desc.contains(searchText) &&
                        !author.contains(searchText)) {
                    continue;
                }
            }

            filteredTemplates.add(template);
        }

        // Sort by rating for favorites/recently used, by name otherwise
        if (FAVORITES_FILTER.equals(category) || RECENTLY_USED.equals(category)) {
            filteredTemplates.sort((a, b) -> {
                int ratingA = favoritesManager.getRating(getTemplateId(a));
                int ratingB = favoritesManager.getRating(getTemplateId(b));
                if (ratingA != ratingB) return Integer.compare(ratingB, ratingA);
                return a.metadata.name().compareToIgnoreCase(b.metadata.name());
            });
        }

        tableModel.fireTableDataChanged();

        // Clear selection if filtered list changed
        if (!filteredTemplates.isEmpty() && templateTable.getSelectedRow() < 0) {
            templateTable.setRowSelectionInterval(0, 0);
        }
    }

    private void updatePreview() {
        if (selectedTemplate == null) {
            previewName.setText("");
            previewAuthor.setText("");
            previewCategory.setText("");
            previewDescription.setText("");
            previewNodes.setText("");
            previewRating.setRating(0);
            favoriteButton.setSelected(false);
            favoriteButton.setIcon(IconFactory.getIcon("favorite_empty", 16));
            favoriteButton.setText("Add to Favorites");
            favoriteButton.setEnabled(false);
            return;
        }

        RigMetadata meta = selectedTemplate.metadata;
        previewName.setText(meta.name());
        previewAuthor.setText("by " + meta.author());
        previewCategory.setText(meta.category() + (selectedTemplate.isFactory ? " (Factory)" : ""));
        previewDescription.setText("<html>" + meta.description() + "</html>");
        previewNodes.setText(selectedTemplate.nodeCount + " effect blocks");

        // Update rating and favorite state
        String templateId = getTemplateId(selectedTemplate);
        previewRating.setRating(favoritesManager.getRating(templateId));
        boolean isFavorite = favoritesManager.isFavorite(templateId);
        favoriteButton.setSelected(isFavorite);
        favoriteButton.setIcon(IconFactory.getIcon(isFavorite ? "favorite" : "favorite_empty", 16));
        favoriteButton.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
        favoriteButton.setEnabled(true);

        // Mark as recently used
        favoritesManager.markRecentlyUsed(templateId);
    }

    /**
     * Show the dialog and return the selected template filename, or null if cancelled.
     */
    public String showDialog() {
        setVisible(true);
        if (confirmed && selectedTemplate != null) {
            // Return relative path for non-factory, full filename for factory
            String filename = selectedTemplate.filename;
            if (selectedTemplate.isFactory) {
                return "factory/" + filename;
            }
            return filename.replace(".jfxrig", "");
        }
        return null;
    }

    /**
     * Get the selected template entry.
     */
    public TemplateEntry getSelectedTemplate() {
        return selectedTemplate;
    }

    /**
     * Template entry record.
     */
    public record TemplateEntry(
            String filename,
            Path path,
            RigMetadata metadata,
            int nodeCount,
            boolean isFactory
    ) {
    }

    /**
     * Table model for templates.
     */
    private class TemplateTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"", "Name", "Category", "Rating"};

        @Override
        public int getRowCount() {
            return filteredTemplates.size();
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            TemplateEntry entry = filteredTemplates.get(rowIndex);
            String templateId = getTemplateId(entry);
            return switch (columnIndex) {
                case 0 -> favoritesManager.isFavorite(templateId) ? "*" : "";  // Favorite
                case 1 -> entry.metadata.name() + (entry.isFactory ? " *" : "");
                case 2 -> entry.metadata.category();
                case 3 -> getStarsString(favoritesManager.getRating(templateId));
                default -> "";
            };
        }
    }

    /**
     * Get template ID from entry.
     */
    private String getTemplateId(TemplateEntry entry) {
        if (entry.isFactory) {
            return "factory/" + entry.filename;
        }
        return entry.filename;
    }

    /**
     * Convert rating to stars string.
     */
    private String getStarsString(int rating) {
        if (rating <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            sb.append("*");  // Filled star
        }
        for (int i = rating; i < 5; i++) {
            sb.append("-");  // Empty star
        }
        return sb.toString();
    }

    /**
     * Rating panel with clickable stars.
     */
    private class RatingPanel extends JPanel {
        private int rating = 0;
        private int hoverRating = -1;
        private static final int STAR_SIZE = 20;

        public RatingPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(STAR_SIZE * 5 + 10, STAR_SIZE + 4));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedTemplate != null) {
                        int newRating = getStarAtPoint(e.getX());
                        if (newRating == rating) {
                            newRating = 0;  // Click same star to clear
                        }
                        rating = newRating;
                        String templateId = getTemplateId(selectedTemplate);
                        favoritesManager.setRating(templateId, rating);
                        tableModel.fireTableDataChanged();
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverRating = -1;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverRating = getStarAtPoint(e.getX());
                    repaint();
                }
            });
        }

        private int getStarAtPoint(int x) {
            return Math.min(5, Math.max(1, (x / STAR_SIZE) + 1));
        }

        public void setRating(int rating) {
            this.rating = rating;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int displayRating = hoverRating > 0 ? hoverRating : rating;

            for (int i = 0; i < 5; i++) {
                int x = i * STAR_SIZE + 2;
                int y = 2;

                if (i < displayRating) {
                    g2d.setColor(DarkTheme.ACCENT_WARNING);
                    drawFilledStar(g2d, x, y, STAR_SIZE - 4);
                } else {
                    g2d.setColor(DarkTheme.TEXT_DISABLED);
                    drawEmptyStar(g2d, x, y, STAR_SIZE - 4);
                }
            }

            g2d.dispose();
        }

        private void drawFilledStar(Graphics2D g, int x, int y, int size) {
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            double centerX = x + size / 2.0;
            double centerY = y + size / 2.0;
            double outerRadius = size / 2.0;
            double innerRadius = outerRadius * 0.4;

            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 2 + i * Math.PI / 5;
                double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                xPoints[i] = (int) (centerX + Math.cos(angle) * radius);
                yPoints[i] = (int) (centerY - Math.sin(angle) * radius);
            }

            g.fillPolygon(xPoints, yPoints, 10);
        }

        private void drawEmptyStar(Graphics2D g, int x, int y, int size) {
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            double centerX = x + size / 2.0;
            double centerY = y + size / 2.0;
            double outerRadius = size / 2.0;
            double innerRadius = outerRadius * 0.4;

            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 2 + i * Math.PI / 5;
                double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                xPoints[i] = (int) (centerX + Math.cos(angle) * radius);
                yPoints[i] = (int) (centerY - Math.sin(angle) * radius);
            }

            g.drawPolygon(xPoints, yPoints, 10);
        }
    }
}
