package it.denzosoft.jfx2.ui.palette;

import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom tree cell renderer for the effect palette.
 * Displays category icons and effect names with styling.
 */
public class EffectTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final int ROW_HEIGHT = 24;
    private static final int ICON_SIZE = 16;

    public EffectTreeCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf,
                                                  int row, boolean hasFocus) {

        // Reset background
        setBackground(selected ? DarkTheme.BLOCK_BORDER_SELECTED : DarkTheme.BG_DARK);
        setForeground(DarkTheme.TEXT_PRIMARY);

        if (value instanceof DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();

            if (userObject instanceof EffectCategory category) {
                // Category node
                renderCategory(category, expanded);
            } else if (userObject instanceof EffectMetadata metadata) {
                // Effect node
                renderEffect(metadata, selected);
            } else {
                // Root or other
                setText(userObject.toString());
                setIcon(null);
            }
        }

        // Border padding
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        return this;
    }

    /**
     * Render a category node.
     */
    private void renderCategory(EffectCategory category, boolean expanded) {
        Icon expandIcon = IconFactory.getIcon(expanded ? "collapse" : "expand", 12);
        Icon categoryIcon = getCategoryIcon(category);

        setText(category.getDisplayName());
        setFont(DarkTheme.FONT_BOLD);
        setForeground(DarkTheme.getCategoryColor(category.name()));
        setIcon(categoryIcon);
        setIconTextGap(6);
    }

    /**
     * Render an effect node.
     */
    private void renderEffect(EffectMetadata metadata, boolean selected) {
        Icon icon = getEffectIcon(metadata);
        setText("  " + metadata.name());
        setFont(DarkTheme.FONT_REGULAR);
        setForeground(selected ? DarkTheme.TEXT_PRIMARY : DarkTheme.TEXT_SECONDARY);
        setToolTipText(metadata.description());
        setIcon(icon);
        setIconTextGap(4);
    }

    /**
     * Get icon for a category.
     */
    private Icon getCategoryIcon(EffectCategory category) {
        String iconName = switch (category) {
            case INPUT_SOURCE -> "input_source";
            case OUTPUT_SINK -> "output_sink";
            case DYNAMICS -> "dynamics";
            case GAIN -> "dynamics";
            case DISTORTION -> "drive";
            case MODULATION -> "modulation";
            case DELAY -> "time";
            case REVERB -> "time";
            case EQ -> "eq";
            case FILTER -> "eq";
            case AMP_SIM -> "drive";
            case PITCH -> "pitch";
            case ACOUSTIC -> "eq";
            case UTILITY -> "utility";
        };
        return IconFactory.getIcon(iconName, ICON_SIZE);
    }

    /**
     * Get icon for an effect.
     * Uses effect-specific icons with category color.
     */
    private Icon getEffectIcon(EffectMetadata metadata) {
        Color categoryColor = DarkTheme.getCategoryColor(metadata.category().name());
        return IconFactory.getEffectIcon(metadata.id(), categoryColor, ICON_SIZE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, ROW_HEIGHT);
        return size;
    }
}
