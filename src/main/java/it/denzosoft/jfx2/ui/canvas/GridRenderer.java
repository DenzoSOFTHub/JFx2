package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Renders the background grid for the signal flow canvas.
 * Supports different grid sizes and adapts to zoom level.
 */
public class GridRenderer {

    /** Default grid spacing in pixels */
    public static final int DEFAULT_GRID_SIZE = 20;

    /** Grid size constant for snap operations */
    public static final int GRID_SIZE = DEFAULT_GRID_SIZE;

    /** Major grid line interval (every N minor lines) */
    public static final int MAJOR_GRID_INTERVAL = 5;

    private int gridSize = DEFAULT_GRID_SIZE;
    private boolean showGrid = true;
    private boolean showMajorGrid = true;
    private boolean showDots = false;

    private Color backgroundColor = DarkTheme.CANVAS_BG;
    private Color minorGridColor = DarkTheme.GRID_LINE;
    private Color majorGridColor = DarkTheme.GRID_LINE_MAJOR;

    /** Minimum zoom level to show minor grid (below this, only major grid) */
    private static final double MIN_ZOOM_FOR_MINOR_GRID = 0.5;

    /** Minimum zoom level to show any grid */
    private static final double MIN_ZOOM_FOR_GRID = 0.25;

    public GridRenderer() {
    }

    /**
     * Render the grid within the visible area.
     *
     * @param g2d       Graphics context (already transformed for zoom/pan)
     * @param visibleArea The visible area in world coordinates
     * @param zoom      Current zoom level
     */
    public void render(Graphics2D g2d, Rectangle2D visibleArea, double zoom) {
        if (!showGrid) {
            // Just fill background
            g2d.setColor(backgroundColor);
            g2d.fill(visibleArea);
            return;
        }

        // Fill background
        g2d.setColor(backgroundColor);
        g2d.fill(visibleArea);

        // Don't render grid if zoomed out too much
        if (zoom < MIN_ZOOM_FOR_GRID) {
            return;
        }

        // Calculate grid bounds
        int startX = (int) (Math.floor(visibleArea.getMinX() / gridSize) * gridSize);
        int startY = (int) (Math.floor(visibleArea.getMinY() / gridSize) * gridSize);
        int endX = (int) (Math.ceil(visibleArea.getMaxX() / gridSize) * gridSize);
        int endY = (int) (Math.ceil(visibleArea.getMaxY() / gridSize) * gridSize);

        // Determine if we should show minor grid based on zoom
        boolean drawMinorGrid = zoom >= MIN_ZOOM_FOR_MINOR_GRID;

        if (showDots) {
            renderDotGrid(g2d, startX, startY, endX, endY, drawMinorGrid);
        } else {
            renderLineGrid(g2d, startX, startY, endX, endY, drawMinorGrid);
        }
    }

    /**
     * Render grid as lines.
     */
    private void renderLineGrid(Graphics2D g2d, int startX, int startY, int endX, int endY, boolean drawMinorGrid) {
        int majorSize = gridSize * MAJOR_GRID_INTERVAL;

        // Set rendering hints for crisp lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw minor grid lines
        if (drawMinorGrid) {
            g2d.setColor(minorGridColor);
            g2d.setStroke(new BasicStroke(1.0f));

            // Vertical lines
            for (int x = startX; x <= endX; x += gridSize) {
                if (x % majorSize != 0) {  // Skip major grid positions
                    g2d.drawLine(x, startY, x, endY);
                }
            }

            // Horizontal lines
            for (int y = startY; y <= endY; y += gridSize) {
                if (y % majorSize != 0) {  // Skip major grid positions
                    g2d.drawLine(startX, y, endX, y);
                }
            }
        }

        // Draw major grid lines
        if (showMajorGrid) {
            g2d.setColor(majorGridColor);
            g2d.setStroke(new BasicStroke(1.0f));

            // Vertical lines
            int majorStartX = (int) (Math.floor((double) startX / majorSize) * majorSize);
            for (int x = majorStartX; x <= endX; x += majorSize) {
                g2d.drawLine(x, startY, x, endY);
            }

            // Horizontal lines
            int majorStartY = (int) (Math.floor((double) startY / majorSize) * majorSize);
            for (int y = majorStartY; y <= endY; y += majorSize) {
                g2d.drawLine(startX, y, endX, y);
            }
        }

        // Draw origin lines (optional - thicker)
        if (startX <= 0 && endX >= 0) {
            g2d.setColor(majorGridColor);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(0, startY, 0, endY);
        }
        if (startY <= 0 && endY >= 0) {
            g2d.setColor(majorGridColor);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(startX, 0, endX, 0);
        }
    }

    /**
     * Render grid as dots (alternative style).
     */
    private void renderDotGrid(Graphics2D g2d, int startX, int startY, int endX, int endY, boolean drawMinorGrid) {
        int majorSize = gridSize * MAJOR_GRID_INTERVAL;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw minor dots
        if (drawMinorGrid) {
            g2d.setColor(minorGridColor);
            int dotSize = 2;

            for (int x = startX; x <= endX; x += gridSize) {
                for (int y = startY; y <= endY; y += gridSize) {
                    if (x % majorSize != 0 || y % majorSize != 0) {
                        g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
                    }
                }
            }
        }

        // Draw major dots
        if (showMajorGrid) {
            g2d.setColor(majorGridColor);
            int dotSize = 4;

            int majorStartX = (int) (Math.floor((double) startX / majorSize) * majorSize);
            int majorStartY = (int) (Math.floor((double) startY / majorSize) * majorSize);

            for (int x = majorStartX; x <= endX; x += majorSize) {
                for (int y = majorStartY; y <= endY; y += majorSize) {
                    g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
                }
            }
        }
    }

    /**
     * Snap a point to the nearest grid intersection.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return Point snapped to grid
     */
    public Point snapToGrid(int x, int y) {
        int snappedX = Math.round((float) x / gridSize) * gridSize;
        int snappedY = Math.round((float) y / gridSize) * gridSize;
        return new Point(snappedX, snappedY);
    }

    /**
     * Snap a point to the nearest grid intersection.
     */
    public Point snapToGrid(Point p) {
        return snapToGrid(p.x, p.y);
    }

    /**
     * Snap a value to the nearest grid line.
     */
    public int snapToGrid(int value) {
        return Math.round((float) value / gridSize) * gridSize;
    }

    // ==================== GETTERS AND SETTERS ====================

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = Math.max(5, Math.min(100, gridSize));
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isShowMajorGrid() {
        return showMajorGrid;
    }

    public void setShowMajorGrid(boolean showMajorGrid) {
        this.showMajorGrid = showMajorGrid;
    }

    public boolean isShowDots() {
        return showDots;
    }

    public void setShowDots(boolean showDots) {
        this.showDots = showDots;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getMinorGridColor() {
        return minorGridColor;
    }

    public void setMinorGridColor(Color minorGridColor) {
        this.minorGridColor = minorGridColor;
    }

    public Color getMajorGridColor() {
        return majorGridColor;
    }

    public void setMajorGridColor(Color majorGridColor) {
        this.majorGridColor = majorGridColor;
    }
}
