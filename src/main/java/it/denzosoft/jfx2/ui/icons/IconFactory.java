package it.denzosoft.jfx2.ui.icons;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for generating icons programmatically using Java2D.
 * No external files needed - all icons are drawn at runtime.
 *
 * <p>Plugins can register custom effect icon painters via {@link #registerEffectIconPainter}.</p>
 */
public class IconFactory {

    private static final int DEFAULT_SIZE = 16;
    private static final Map<String, Icon> iconCache = new HashMap<>();
    private static final Map<String, EffectIconPainter> customPainters = new HashMap<>();

    /**
     * Functional interface for custom effect icon painters.
     * Plugins can implement this to provide custom icons for their effects.
     */
    @FunctionalInterface
    public interface EffectIconPainter {
        /**
         * Draw the effect icon.
         * @param g Graphics2D context (already configured with antialiasing)
         * @param size Icon size in pixels
         * @param color Category color to use
         */
        void paint(Graphics2D g, int size, Color color);
    }

    // ==================== PUBLIC API ====================

    public static Icon getIcon(String name) {
        return getIcon(name, DEFAULT_SIZE);
    }

    public static Icon getIcon(String name, int size) {
        String key = name + "_" + size;
        return iconCache.computeIfAbsent(key, k -> createIcon(name, size));
    }

    /**
     * Get an effect-specific icon with category color.
     *
     * @param effectId      The effect identifier
     * @param categoryColor The color for this effect's category
     * @param size          Icon size
     * @return The effect icon
     */
    public static Icon getEffectIcon(String effectId, Color categoryColor, int size) {
        String key = "effect_" + effectId + "_" + categoryColor.getRGB() + "_" + size;
        return iconCache.computeIfAbsent(key, k -> createEffectIcon(effectId, categoryColor, size));
    }

    /**
     * Register a custom icon painter for an effect.
     * Plugins can use this to provide custom icons for their effects.
     *
     * @param effectId The effect identifier (must match the effect's ID)
     * @param painter  The painter that will draw the icon
     */
    public static void registerEffectIconPainter(String effectId, EffectIconPainter painter) {
        customPainters.put(effectId.toLowerCase(), painter);
        // Clear any cached icons for this effect
        iconCache.entrySet().removeIf(e -> e.getKey().startsWith("effect_" + effectId.toLowerCase() + "_"));
    }

    /**
     * Register multiple icon painters at once.
     *
     * @param painters Map of effect IDs to painters
     */
    public static void registerEffectIconPainters(Map<String, EffectIconPainter> painters) {
        painters.forEach(IconFactory::registerEffectIconPainter);
    }

    /**
     * Unregister all custom painters (useful for plugin unload).
     *
     * @param effectIds The effect IDs to unregister
     */
    public static void unregisterEffectIconPainters(Iterable<String> effectIds) {
        for (String id : effectIds) {
            customPainters.remove(id.toLowerCase());
            iconCache.entrySet().removeIf(e -> e.getKey().startsWith("effect_" + id.toLowerCase() + "_"));
        }
    }

    private static Icon createEffectIcon(String effectId, Color color, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        // Check for custom painter first (from plugins)
        EffectIconPainter customPainter = customPainters.get(effectId.toLowerCase());
        if (customPainter != null) {
            customPainter.paint(g2d, size, color);
            g2d.dispose();
            return new ImageIcon(image);
        }

        switch (effectId.toLowerCase()) {
            // Input Sources
            case "audioinput" -> drawAudioInputIcon(g2d, size, color);
            case "wavfileinput" -> drawWavFileInputIcon(g2d, size, color);
            case "oscillator" -> drawOscillatorEffectIcon(g2d, size, color);
            case "drummachine" -> drawDrumMachineIcon(g2d, size, color);

            // Output Sinks
            case "audiooutput" -> drawAudioOutputIcon(g2d, size, color);
            case "wavfileoutput" -> drawWavFileOutputIcon(g2d, size, color);

            // Gain/Dynamics
            case "gain" -> drawGainIcon(g2d, size, color);
            case "noisegate" -> drawNoiseGateIcon(g2d, size, color);
            case "compressor" -> drawCompressorIcon(g2d, size, color);
            case "limiter" -> drawLimiterIcon(g2d, size, color);
            case "volumeswell" -> drawVolumeSwellIcon(g2d, size, color);
            case "sustainer" -> drawSustainerIcon(g2d, size, color);

            // Distortion
            case "overdrive" -> drawOverdriveIcon(g2d, size, color);
            case "drive" -> drawDriveIcon(g2d, size, color);
            case "distortion" -> drawDistortionIcon(g2d, size, color);
            case "fuzz" -> drawFuzzIcon(g2d, size, color);

            // Delay
            case "delay" -> drawDelayIcon(g2d, size, color);
            case "tapeecho" -> drawTapeEchoIcon(g2d, size, color);
            case "multitap" -> drawMultiTapIcon(g2d, size, color);
            case "reversedelay" -> drawReverseDelayIcon(g2d, size, color);
            case "pingpong" -> drawPingPongIcon(g2d, size, color);

            // Reverb
            case "reverb" -> drawReverbIcon(g2d, size, color);
            case "springreverb" -> drawSpringReverbIcon(g2d, size, color);
            case "shimmerreverb" -> drawShimmerReverbIcon(g2d, size, color);

            // Modulation
            case "chorus" -> drawChorusIcon(g2d, size, color);
            case "flanger" -> drawFlangerIcon(g2d, size, color);
            case "phaser" -> drawPhaserIcon(g2d, size, color);
            case "tremolo" -> drawTremoloIcon(g2d, size, color);
            case "vibrato" -> drawVibratoIcon(g2d, size, color);
            case "panner" -> drawPannerIcon(g2d, size, color);
            case "ringmod" -> drawRingModIcon(g2d, size, color);
            case "univibe" -> drawUniVibeIcon(g2d, size, color);
            case "rotary" -> drawRotaryIcon(g2d, size, color);

            // EQ
            case "filter" -> drawFilterIcon(g2d, size, color);
            case "parametriceq" -> drawParametricEQIcon(g2d, size, color);
            case "graphiceq" -> drawGraphicEQIcon(g2d, size, color);

            // Amp Simulation
            case "amp" -> drawAmpIcon(g2d, size, color);
            case "cabsim" -> drawCabinetIcon(g2d, size, color);
            case "irloader" -> drawIRLoaderIcon(g2d, size, color);

            // Filter
            case "wah" -> drawWahIcon(g2d, size, color);
            case "envelopefilter" -> drawEnvelopeFilterIcon(g2d, size, color);
            case "talkbox" -> drawTalkBoxIcon(g2d, size, color);
            case "synth" -> drawSynthIcon(g2d, size, color);

            // Pitch
            case "pitchshift" -> drawPitchShiftIcon(g2d, size, color);
            case "octaver" -> drawOctaverIcon(g2d, size, color);

            // Utility
            case "splitter" -> drawSplitterEffectIcon(g2d, size, color);
            case "mixer" -> drawMixerEffectIcon(g2d, size, color);
            case "mono2stereo" -> drawMono2StereoIcon(g2d, size, color);
            case "stereo2mono" -> drawStereo2MonoIcon(g2d, size, color);
            case "looper" -> drawLooperIcon(g2d, size, color);
            case "settings" -> drawSettingsEffectIcon(g2d, size, color);

            // Output - additional
            case "midirecorder" -> drawMidiRecorderIcon(g2d, size, color);

            // Dynamics - additional
            case "noisesuppressor" -> drawNoiseSuppressorIcon(g2d, size, color);
            case "multibandcomp" -> drawMultibandCompIcon(g2d, size, color);
            case "autosustain" -> drawAutoSustainIcon(g2d, size, color);

            // Distortion - additional
            case "tubedist" -> drawTubeDistortionIcon(g2d, size, color);

            // Delay - additional
            case "quaddelay" -> drawQuadDelayIcon(g2d, size, color);

            // Reverb - additional
            case "platereverb" -> drawPlateReverbIcon(g2d, size, color);
            case "roomreverb" -> drawRoomReverbIcon(g2d, size, color);
            case "stereoimagereverb" -> drawStereoImageReverbIcon(g2d, size, color);

            // Modulation - additional
            case "pan3d" -> drawPan3DIcon(g2d, size, color);

            // EQ - additional
            case "pickupemu" -> drawPickupEmulatorIcon(g2d, size, color);

            // Amp Sim - additional
            case "neuralamp", "nam" -> drawNeuralAmpIcon(g2d, size, color);
            case "tubepreamp" -> drawTubePreampIcon(g2d, size, color);
            case "tubepoweramp" -> drawTubePowerAmpIcon(g2d, size, color);
            case "cabinetsim" -> drawCabinetSimulatorIcon(g2d, size, color);

            // Filter/Synth - additional
            case "synthdrone" -> drawSynthDroneIcon(g2d, size, color);
            case "pitchsynth" -> drawPitchSynthIcon(g2d, size, color);
            case "acousticsim" -> drawAcousticSimIcon(g2d, size, color);

            // Pitch - additional
            case "harmonizer" -> drawHarmonizerIcon(g2d, size, color);
            case "autotuner" -> drawAutoTunerIcon(g2d, size, color);

            // Acoustic
            case "bodyresonance" -> drawBodyResonanceIcon(g2d, size, color);
            case "antifeedback" -> drawAntiFeedbackIcon(g2d, size, color);
            case "12string" -> drawTwelveStringIcon(g2d, size, color);
            case "piezosweetener" -> drawPiezoSweetenerIcon(g2d, size, color);
            case "acousticcomp" -> drawAcousticCompressorIcon(g2d, size, color);
            case "acousticeq" -> drawAcousticEQIcon(g2d, size, color);

            default -> drawDefaultEffectIcon(g2d, size, color);
        }

        g2d.dispose();
        return new ImageIcon(image);
    }

    // ==================== ICON CREATION ====================

    private static Icon createIcon(String name, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        switch (name.toLowerCase()) {
            // File operations
            case "new" -> drawNewIcon(g2d, size);
            case "open" -> drawOpenIcon(g2d, size);
            case "save" -> drawSaveIcon(g2d, size);

            // Edit operations
            case "undo" -> drawUndoIcon(g2d, size);
            case "redo" -> drawRedoIcon(g2d, size);
            case "cut" -> drawCutIcon(g2d, size);
            case "copy" -> drawCopyIcon(g2d, size);
            case "paste" -> drawPasteIcon(g2d, size);
            case "delete" -> drawDeleteIcon(g2d, size);

            // Zoom
            case "zoom_in" -> drawZoomInIcon(g2d, size);
            case "zoom_out" -> drawZoomOutIcon(g2d, size);
            case "zoom_fit" -> drawZoomFitIcon(g2d, size);

            // Audio
            case "play" -> drawPlayIcon(g2d, size);
            case "stop" -> drawStopIcon(g2d, size);
            case "pause" -> drawPauseIcon(g2d, size);

            // Settings
            case "settings" -> drawSettingsIcon(g2d, size);

            // Status
            case "running" -> drawRunningIcon(g2d, size);
            case "stopped" -> drawStoppedIcon(g2d, size);

            // Toast
            case "info" -> drawInfoIcon(g2d, size);
            case "success" -> drawSuccessIcon(g2d, size);
            case "warning" -> drawWarningIcon(g2d, size);
            case "error" -> drawErrorIcon(g2d, size);

            // Effect categories
            case "input" -> drawInputIcon(g2d, size);
            case "output" -> drawOutputIcon(g2d, size);
            case "input_source" -> drawInputSourceIcon(g2d, size);
            case "wav_file" -> drawWavFileIcon(g2d, size);
            case "oscillator" -> drawOscillatorIcon(g2d, size);
            case "output_sink" -> drawOutputSinkIcon(g2d, size);
            case "wav_recorder" -> drawWavRecorderIcon(g2d, size);
            case "dynamics" -> drawDynamicsIcon(g2d, size);
            case "distortion" -> drawDistortionCategoryIcon(g2d, size);
            case "drive" -> drawDriveIcon(g2d, size);
            case "modulation" -> drawModulationIcon(g2d, size);
            case "delay" -> drawDelayCategoryIcon(g2d, size);
            case "reverb" -> drawReverbCategoryIcon(g2d, size);
            case "time" -> drawTimeIcon(g2d, size);
            case "eq" -> drawEQIcon(g2d, size);
            case "filter" -> drawFilterCategoryIcon(g2d, size);
            case "amp_sim" -> drawAmpSimCategoryIcon(g2d, size);
            case "pitch" -> drawPitchIcon(g2d, size);
            case "acoustic" -> drawAcousticCategoryIcon(g2d, size);
            case "utility" -> drawUtilityIcon(g2d, size);
            case "effect" -> drawEffectIcon(g2d, size);
            case "splitter" -> drawSplitterIcon(g2d, size);
            case "mixer" -> drawMixerIcon(g2d, size);

            // Tree expand/collapse
            case "expand" -> drawExpandIcon(g2d, size);
            case "collapse" -> drawCollapseIcon(g2d, size);

            // Favorites
            case "favorite" -> drawFavoriteIcon(g2d, size);
            case "favorite_empty" -> drawFavoriteEmptyIcon(g2d, size);
            case "star" -> drawStarIcon(g2d, size);
            case "star_empty" -> drawStarEmptyIcon(g2d, size);

            default -> drawDefaultIcon(g2d, size);
        }

        g2d.dispose();
        return new ImageIcon(image);
    }

    private static void setupGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ==================== FILE ICONS ====================

    private static void drawNewIcon(Graphics2D g, int size) {
        int m = size / 8;
        int w = size - m * 2;
        int h = size - m * 2;
        int fold = w / 3;

        Path2D path = new Path2D.Double();
        path.moveTo(m, m);
        path.lineTo(m + w - fold, m);
        path.lineTo(m + w, m + fold);
        path.lineTo(m + w, m + h);
        path.lineTo(m, m + h);
        path.closePath();

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(path);

        // Fold corner
        g.drawLine(m + w - fold, m, m + w - fold, m + fold);
        g.drawLine(m + w - fold, m + fold, m + w, m + fold);

        // Plus sign
        int cx = size / 2;
        int cy = size / 2 + m;
        int ps = size / 6;
        g.drawLine(cx - ps, cy, cx + ps, cy);
        g.drawLine(cx, cy - ps, cx, cy + ps);
    }

    private static void drawOpenIcon(Graphics2D g, int size) {
        int m = size / 8;

        // Folder back
        Path2D folder = new Path2D.Double();
        folder.moveTo(m, m + size / 4);
        folder.lineTo(m, size - m);
        folder.lineTo(size - m, size - m);
        folder.lineTo(size - m, m + size / 3);
        folder.lineTo(size / 2, m + size / 3);
        folder.lineTo(size / 2 - size / 6, m + size / 4);
        folder.closePath();

        g.setColor(DarkTheme.ACCENT_PRIMARY);
        g.fill(folder);
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(folder);
    }

    private static void drawSaveIcon(Graphics2D g, int size) {
        int m = size / 8;
        int w = size - m * 2;
        int h = size - m * 2;

        // Floppy disk body
        RoundRectangle2D disk = new RoundRectangle2D.Double(m, m, w, h, 3, 3);
        g.setColor(DarkTheme.ACCENT_PRIMARY);
        g.fill(disk);
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(disk);

        // Metal slider
        int sliderW = w * 2 / 3;
        int sliderH = h / 3;
        g.setColor(DarkTheme.BG_MEDIUM);
        g.fillRect(m + (w - sliderW) / 2, m, sliderW, sliderH);

        // Label area
        int labelH = h / 3;
        g.setColor(DarkTheme.BG_LIGHT);
        g.fillRect(m + 2, size - m - labelH - 2, w - 4, labelH);
    }

    // ==================== EDIT ICONS ====================

    private static void drawUndoIcon(Graphics2D g, int size) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        int radius = size / 3;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Counter-clockwise curved arrow (270° arc starting from right)
        Arc2D arc = new Arc2D.Double(cx - radius, cy - radius + 2, radius * 2, radius * 2, -30, 250, Arc2D.OPEN);
        g.draw(arc);

        // Arrow head pointing left at the end of arc
        int ax = m + 1;
        int ay = cy + 2;
        int ah = size / 5;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(ax + ah, ay - ah);
        arrow.lineTo(ax, ay);
        arrow.lineTo(ax + ah, ay + ah);
        g.draw(arrow);
    }

    private static void drawRedoIcon(Graphics2D g, int size) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        int radius = size / 3;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Clockwise curved arrow (270° arc starting from left)
        Arc2D arc = new Arc2D.Double(cx - radius, cy - radius + 2, radius * 2, radius * 2, 210, -250, Arc2D.OPEN);
        g.draw(arc);

        // Arrow head pointing right at the end of arc
        int ax = size - m - 1;
        int ay = cy + 2;
        int ah = size / 5;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(ax - ah, ay - ah);
        arrow.lineTo(ax, ay);
        arrow.lineTo(ax - ah, ay + ah);
        g.draw(arrow);
    }

    private static void drawCutIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f));

        // Scissors
        int cx = size / 2;
        int cy = size / 2;
        g.drawOval(m, cy, size / 4, size / 3);
        g.drawOval(size - m - size / 4, cy, size / 4, size / 3);
        g.drawLine(m + size / 8, cy, cx, m);
        g.drawLine(size - m - size / 8, cy, cx, m);
    }

    private static void drawCopyIcon(Graphics2D g, int size) {
        int m = size / 6;
        int offset = size / 5;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.3f));

        // Back document
        g.drawRect(m + offset, m, size - m * 2 - offset, size - m * 2 - offset);

        // Front document
        g.setColor(DarkTheme.BG_MEDIUM);
        g.fillRect(m, m + offset, size - m * 2 - offset, size - m * 2 - offset);
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.drawRect(m, m + offset, size - m * 2 - offset, size - m * 2 - offset);
    }

    private static void drawPasteIcon(Graphics2D g, int size) {
        int m = size / 6;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.3f));

        // Clipboard
        g.drawRect(m, m + size / 6, size - m * 2, size - m * 2 - size / 6);

        // Clip
        int clipW = size / 3;
        g.fillRect(size / 2 - clipW / 2, m, clipW, size / 5);
    }

    private static void drawDeleteIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(m, m, size - m, size - m);
        g.drawLine(size - m, m, m, size - m);
    }

    // ==================== ZOOM ICONS ====================

    private static void drawZoomInIcon(Graphics2D g, int size) {
        drawMagnifier(g, size);
        int cx = size * 2 / 5;
        int cy = size * 2 / 5;
        int ps = size / 6;
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx - ps, cy, cx + ps, cy);
        g.drawLine(cx, cy - ps, cx, cy + ps);
    }

    private static void drawZoomOutIcon(Graphics2D g, int size) {
        drawMagnifier(g, size);
        int cx = size * 2 / 5;
        int cy = size * 2 / 5;
        int ps = size / 6;
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx - ps, cy, cx + ps, cy);
    }

    private static void drawZoomFitIcon(Graphics2D g, int size) {
        int m = size / 6;
        int cornerLen = size / 4;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Four corners
        // Top-left
        g.drawLine(m, m + cornerLen, m, m);
        g.drawLine(m, m, m + cornerLen, m);
        // Top-right
        g.drawLine(size - m - cornerLen, m, size - m, m);
        g.drawLine(size - m, m, size - m, m + cornerLen);
        // Bottom-left
        g.drawLine(m, size - m - cornerLen, m, size - m);
        g.drawLine(m, size - m, m + cornerLen, size - m);
        // Bottom-right
        g.drawLine(size - m - cornerLen, size - m, size - m, size - m);
        g.drawLine(size - m, size - m, size - m, size - m - cornerLen);
    }

    private static void drawMagnifier(Graphics2D g, int size) {
        int m = size / 6;
        int glassSize = size / 2;

        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(m, m, glassSize, glassSize);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(m + glassSize - 2, m + glassSize - 2, size - m, size - m);
    }

    // ==================== AUDIO ICONS ====================

    private static void drawPlayIcon(Graphics2D g, int size) {
        int m = size / 5;
        int[] xPoints = {m, size - m, m};
        int[] yPoints = {m, size / 2, size - m};

        g.setColor(DarkTheme.ACCENT_SUCCESS);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private static void drawStopIcon(Graphics2D g, int size) {
        int m = size / 4;
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.fillRect(m, m, size - m * 2, size - m * 2);
    }

    private static void drawPauseIcon(Graphics2D g, int size) {
        int m = size / 4;
        int barW = size / 5;
        int gap = size / 6;

        g.setColor(DarkTheme.ACCENT_WARNING);
        g.fillRect(m, m, barW, size - m * 2);
        g.fillRect(size - m - barW, m, barW, size - m * 2);
    }

    // ==================== SETTINGS ICON ====================

    private static void drawSettingsIcon(Graphics2D g, int size) {
        int cx = size / 2;
        int cy = size / 2;
        int outerR = size / 2 - 2;
        int innerR = size / 4;
        int teeth = 8;

        Path2D gear = new Path2D.Double();
        for (int i = 0; i < teeth * 2; i++) {
            double angle = Math.PI * i / teeth;
            double r = (i % 2 == 0) ? outerR : outerR - 3;
            double x = cx + r * Math.cos(angle - Math.PI / 2);
            double y = cy + r * Math.sin(angle - Math.PI / 2);
            if (i == 0) {
                gear.moveTo(x, y);
            } else {
                gear.lineTo(x, y);
            }
        }
        gear.closePath();

        g.setColor(DarkTheme.TEXT_SECONDARY);
        g.fill(gear);
        g.setColor(DarkTheme.BG_DARK);
        g.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
    }

    // ==================== STATUS ICONS ====================

    private static void drawRunningIcon(Graphics2D g, int size) {
        int m = size / 4;
        g.setColor(DarkTheme.ACCENT_SUCCESS);
        g.fillOval(m, m, size - m * 2, size - m * 2);
    }

    private static void drawStoppedIcon(Graphics2D g, int size) {
        int m = size / 4;
        g.setColor(DarkTheme.TEXT_DISABLED);
        g.fillOval(m, m, size - m * 2, size - m * 2);
    }

    // ==================== TOAST ICONS ====================

    private static void drawInfoIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.ACCENT_PRIMARY);
        g.fillOval(m, m, size - m * 2, size - m * 2);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
        FontMetrics fm = g.getFontMetrics();
        String text = "i";
        int tx = (size - fm.stringWidth(text)) / 2;
        int ty = (size + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    private static void drawSuccessIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.ACCENT_SUCCESS);
        g.fillOval(m, m, size - m * 2, size - m * 2);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = size / 2;
        int cy = size / 2;
        g.drawLine(cx - size / 5, cy, cx - size / 10, cy + size / 5);
        g.drawLine(cx - size / 10, cy + size / 5, cx + size / 4, cy - size / 5);
    }

    private static void drawWarningIcon(Graphics2D g, int size) {
        int m = size / 6;
        int[] xPoints = {size / 2, m, size - m};
        int[] yPoints = {m, size - m, size - m};

        g.setColor(DarkTheme.ACCENT_WARNING);
        g.fillPolygon(xPoints, yPoints, 3);

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
        FontMetrics fm = g.getFontMetrics();
        String text = "!";
        int tx = (size - fm.stringWidth(text)) / 2;
        int ty = size - m - 2;
        g.drawString(text, tx, ty);
    }

    private static void drawErrorIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.fillOval(m, m, size - m * 2, size - m * 2);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int innerM = size / 3;
        g.drawLine(innerM, innerM, size - innerM, size - innerM);
        g.drawLine(size - innerM, innerM, innerM, size - innerM);
    }

    // ==================== EFFECT CATEGORY ICONS ====================

    private static void drawInputIcon(Graphics2D g, int size) {
        int m = size / 4;
        int[] xPoints = {m, size - m, m};
        int[] yPoints = {m, size / 2, size - m};
        g.setColor(DarkTheme.CATEGORY_INPUT_SOURCE);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private static void drawOutputIcon(Graphics2D g, int size) {
        int m = size / 4;
        g.setColor(DarkTheme.CATEGORY_OUTPUT_SINK);
        g.fillRect(m, m, size - m * 2, size - m * 2);
    }

    private static void drawInputSourceIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_INPUT_SOURCE);

        // Speaker/audio source icon
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Speaker cone
        int coneX = m;
        int coneY = size / 3;
        int coneW = size / 3;
        int coneH = size / 3;
        g.fillRect(coneX, coneY, coneW / 2, coneH);

        // Speaker flare
        int[] xPoints = {coneX + coneW / 2, coneX + coneW, coneX + coneW, coneX + coneW / 2};
        int[] yPoints = {coneY, m, size - m, coneY + coneH};
        g.fillPolygon(xPoints, yPoints, 4);

        // Sound waves
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int waveX = coneX + coneW + 3;
        g.drawArc(waveX, size / 3, size / 5, size / 3, -60, 120);
        g.drawArc(waveX + 3, size / 4, size / 4, size / 2, -60, 120);
    }

    private static void drawWavFileIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.ACCENT_PRIMARY);

        // File shape
        int fold = size / 4;
        Path2D file = new Path2D.Double();
        file.moveTo(m, m);
        file.lineTo(size - m - fold, m);
        file.lineTo(size - m, m + fold);
        file.lineTo(size - m, size - m);
        file.lineTo(m, size - m);
        file.closePath();

        g.setStroke(new BasicStroke(1.3f));
        g.draw(file);

        // Waveform inside
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int waveY = size / 2;
        int waveH = size / 5;
        g.drawLine(m + 2, waveY, m + 4, waveY - waveH);
        g.drawLine(m + 4, waveY - waveH, m + 6, waveY + waveH);
        g.drawLine(m + 6, waveY + waveH, m + 8, waveY);
        g.drawLine(m + 8, waveY, m + 10, waveY - waveH / 2);
        g.drawLine(m + 10, waveY - waveH / 2, size - m - 2, waveY);
    }

    private static void drawOscillatorIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.ACCENT_SUCCESS);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Sine wave
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 2) * (size / 3);
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawOutputSinkIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.CATEGORY_OUTPUT_SINK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Arrow pointing into disk
        int arrowX = size / 3;
        g.drawLine(m, size / 2, arrowX + 2, size / 2);
        g.drawLine(arrowX - 2, size / 3, arrowX + 2, size / 2);
        g.drawLine(arrowX - 2, size * 2 / 3, arrowX + 2, size / 2);

        // Disk shape
        int diskX = size / 2;
        int diskR = size / 3;
        g.drawOval(diskX - diskR / 2, size / 2 - diskR / 2, diskR, diskR);
        g.drawOval(diskX + diskR / 4, size / 2 - 2, 4, 4);
    }

    private static void drawWavRecorderIcon(Graphics2D g, int size) {
        int m = size / 6;

        // File shape with red record dot
        g.setColor(DarkTheme.ACCENT_PRIMARY);
        int fold = size / 4;
        Path2D file = new Path2D.Double();
        file.moveTo(m, m);
        file.lineTo(size - m - fold, m);
        file.lineTo(size - m, m + fold);
        file.lineTo(size - m, size - m);
        file.lineTo(m, size - m);
        file.closePath();

        g.setStroke(new BasicStroke(1.3f));
        g.draw(file);

        // Record indicator (red circle)
        g.setColor(DarkTheme.ACCENT_ERROR);
        int dotSize = size / 4;
        g.fillOval(size - m - dotSize, size - m - dotSize, dotSize, dotSize);

        // Small waveform
        g.setColor(DarkTheme.ACCENT_PRIMARY);
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int waveY = size / 2;
        int waveH = size / 6;
        g.drawLine(m + 2, waveY, m + 3, waveY - waveH);
        g.drawLine(m + 3, waveY - waveH, m + 5, waveY + waveH);
        g.drawLine(m + 5, waveY + waveH, m + 7, waveY);
    }

    private static void drawDynamicsIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_DYNAMICS);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(m, size / 2, size / 3, size / 2);
        g.drawLine(size / 3, m, size / 3, size - m);
        g.drawLine(size * 2 / 3, m, size * 2 / 3, size - m);
        g.drawLine(size * 2 / 3, size / 2, size - m, size / 2);
    }

    private static void drawDriveIcon(Graphics2D g, int size) {
        int cx = size / 2;
        int cy = size / 2;

        // Lightning bolt
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(size * 0.6, size * 0.1);
        bolt.lineTo(size * 0.3, size * 0.5);
        bolt.lineTo(size * 0.5, size * 0.5);
        bolt.lineTo(size * 0.4, size * 0.9);
        bolt.lineTo(size * 0.7, size * 0.5);
        bolt.lineTo(size * 0.5, size * 0.5);
        bolt.closePath();

        g.setColor(DarkTheme.CATEGORY_DISTORTION);
        g.fill(bolt);
    }

    private static void drawModulationIcon(Graphics2D g, int size) {
        int m = size / 6;
        g.setColor(DarkTheme.CATEGORY_MODULATION);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Sine wave
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 2) * (size / 3);
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawTimeIcon(Graphics2D g, int size) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;

        g.setColor(DarkTheme.CATEGORY_DELAY);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(m, m, size - m * 2, size - m * 2);

        // Clock hands
        g.drawLine(cx, cy, cx, m + 3);
        g.drawLine(cx, cy, cx + size / 5, cy + size / 6);
    }

    private static void drawEQIcon(Graphics2D g, int size) {
        int m = size / 5;
        int barW = size / 5;
        int gap = 2;

        g.setColor(DarkTheme.CATEGORY_EQ);

        // Three frequency bars
        g.fillRect(m, size / 2, barW, size / 2 - m);
        g.fillRect(size / 2 - barW / 2, m + 2, barW, size - m * 2 - 2);
        g.fillRect(size - m - barW, size / 3, barW, size * 2 / 3 - m);
    }

    private static void drawPitchIcon(Graphics2D g, int size) {
        int m = size / 5;

        g.setColor(DarkTheme.CATEGORY_PITCH);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Music note
        g.fillOval(m, size - m - size / 4, size / 3, size / 4);
        g.drawLine(m + size / 3, size - m - size / 8, m + size / 3, m);
        g.drawLine(m + size / 3, m, size - m, m + size / 6);
    }

    private static void drawUtilityIcon(Graphics2D g, int size) {
        drawSettingsIcon(g, size);
    }

    private static void drawEffectIcon(Graphics2D g, int size) {
        drawStarIcon(g, size);
    }

    private static void drawSplitterIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Fork shape
        g.drawLine(m, size / 2, size / 2, size / 2);
        g.drawLine(size / 2, size / 2, size - m, m);
        g.drawLine(size / 2, size / 2, size - m, size - m);
    }

    private static void drawMixerIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Merge shape
        g.drawLine(m, m, size / 2, size / 2);
        g.drawLine(m, size - m, size / 2, size / 2);
        g.drawLine(size / 2, size / 2, size - m, size / 2);
    }

    // ==================== CATEGORY ICONS (with unique colors) ====================

    private static void drawDistortionCategoryIcon(Graphics2D g, int size) {
        g.setColor(DarkTheme.CATEGORY_DISTORTION);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Lightning bolt (distortion symbol)
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(size * 0.5, size * 0.1);
        bolt.lineTo(size * 0.3, size * 0.5);
        bolt.lineTo(size * 0.5, size * 0.5);
        bolt.lineTo(size * 0.4, size * 0.9);
        bolt.lineTo(size * 0.7, size * 0.5);
        bolt.lineTo(size * 0.5, size * 0.5);
        bolt.closePath();
        g.fill(bolt);
    }

    private static void drawDelayCategoryIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_DELAY);
        g.setStroke(new BasicStroke(1.5f));

        // Clock with echo
        g.drawOval(m, m, size - m * 2, size - m * 2);
        int cx = size / 2;
        int cy = size / 2;
        g.drawLine(cx, cy, cx, m + 3);
        g.drawLine(cx, cy, cx + size / 5, cy + size / 6);

        // Echo lines
        g.setStroke(new BasicStroke(1f));
        g.drawArc(size - m - 4, m + 2, 6, size / 2, -60, 120);
    }

    private static void drawReverbCategoryIcon(Graphics2D g, int size) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(DarkTheme.CATEGORY_REVERB);
        g.setStroke(new BasicStroke(1.5f));

        // Expanding sound waves
        for (int i = 0; i < 3; i++) {
            int r = 3 + i * 4;
            int alpha = 255 - i * 60;
            g.setColor(new Color(
                DarkTheme.CATEGORY_REVERB.getRed(),
                DarkTheme.CATEGORY_REVERB.getGreen(),
                DarkTheme.CATEGORY_REVERB.getBlue(), alpha));
            g.drawArc(cx - r, cy - r, r * 2, r * 2, -45, 90);
        }
    }

    private static void drawFilterCategoryIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_FILTER);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Wah pedal curve
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size * 2 / 3);
        curve.quadTo(size / 3, m, size / 2, size / 2);
        curve.quadTo(size * 2 / 3, size - m, size - m, size / 3);
        g.draw(curve);
    }

    private static void drawAmpSimCategoryIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_AMP_SIM);
        g.setStroke(new BasicStroke(1.5f));

        // Amp front with speaker
        g.drawRoundRect(m, m, size - m * 2, size - m * 2, 4, 4);

        // Speaker grill
        int cx = size / 2;
        int cy = size / 2 + 2;
        int r = size / 4;
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.fillOval(cx - 2, cy - 2, 4, 4);

        // Control knob on top
        g.fillOval(size / 2 - 2, m + 2, 4, 4);
    }

    private static void drawAcousticCategoryIcon(Graphics2D g, int size) {
        int m = size / 5;
        g.setColor(DarkTheme.CATEGORY_ACOUSTIC);
        g.setStroke(new BasicStroke(1.5f));

        // Acoustic guitar body
        int bodyW = size - m * 2;
        int bodyH = size - m * 2;

        // Upper bout (smaller)
        g.drawArc(m + 2, m, bodyW - 4, bodyH / 2, 0, 180);

        // Lower bout (larger)
        g.drawArc(m, size / 2 - 2, bodyW, bodyH / 2 + 2, 180, 180);

        // Sound hole
        g.drawOval(size / 2 - 3, size / 2, 6, 6);
    }

    // ==================== TREE ICONS ====================

    private static void drawExpandIcon(Graphics2D g, int size) {
        int m = size / 4;
        int[] xPoints = {m, size - m, size / 2};
        int[] yPoints = {m, m, size - m};
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private static void drawCollapseIcon(Graphics2D g, int size) {
        int m = size / 4;
        int[] xPoints = {m, size / 2, m};
        int[] yPoints = {m, size / 2, size - m};
        g.setColor(DarkTheme.TEXT_PRIMARY);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    // ==================== FAVORITE ICONS ====================

    private static void drawFavoriteIcon(Graphics2D g, int size) {
        drawHeart(g, size, true);
    }

    private static void drawFavoriteEmptyIcon(Graphics2D g, int size) {
        drawHeart(g, size, false);
    }

    private static void drawHeart(Graphics2D g, int size, boolean filled) {
        int m = size / 6;
        double w = size - m * 2;
        double h = size - m * 2;

        Path2D heart = new Path2D.Double();
        double cx = size / 2.0;
        double top = m + h * 0.3;

        heart.moveTo(cx, m + h);
        heart.curveTo(m, m + h * 0.6, m, top, cx, top);
        heart.curveTo(size - m, top, size - m, m + h * 0.6, cx, m + h);

        if (filled) {
            g.setColor(DarkTheme.ACCENT_ERROR);
            g.fill(heart);
        } else {
            g.setColor(DarkTheme.TEXT_DISABLED);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(heart);
        }
    }

    private static void drawStarIcon(Graphics2D g, int size) {
        drawStar(g, size, true);
    }

    private static void drawStarEmptyIcon(Graphics2D g, int size) {
        drawStar(g, size, false);
    }

    private static void drawStar(Graphics2D g, int size, boolean filled) {
        int cx = size / 2;
        int cy = size / 2;
        int outerR = size / 2 - 2;
        int innerR = outerR * 2 / 5;

        int[] xPoints = new int[10];
        int[] yPoints = new int[10];

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * i / 5 - Math.PI / 2;
            int r = (i % 2 == 0) ? outerR : innerR;
            xPoints[i] = (int) (cx + r * Math.cos(angle));
            yPoints[i] = (int) (cy + r * Math.sin(angle));
        }

        if (filled) {
            g.setColor(DarkTheme.ACCENT_WARNING);
            g.fillPolygon(xPoints, yPoints, 10);
        } else {
            g.setColor(DarkTheme.TEXT_DISABLED);
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(xPoints, yPoints, 10);
        }
    }

    // ==================== DEFAULT ICON ====================

    private static void drawDefaultIcon(Graphics2D g, int size) {
        int m = size / 4;
        g.setColor(DarkTheme.TEXT_DISABLED);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(m, m, size - m * 2, size - m * 2);
        g.drawLine(m, m, size - m, size - m);
        g.drawLine(size - m, m, m, size - m);
    }

    // ==================== EFFECT-SPECIFIC ICONS ====================

    private static void drawDefaultEffectIcon(Graphics2D g, int size, Color color) {
        int m = size / 4;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(m, m, size - m * 2, size - m * 2);
        g.fillOval(size / 2 - 2, size / 2 - 2, 4, 4);
    }

    // --- Input Sources ---

    private static void drawWavFileInputIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);

        // File shape
        int fold = size / 4;
        Path2D file = new Path2D.Double();
        file.moveTo(m, m);
        file.lineTo(size - m - fold, m);
        file.lineTo(size - m, m + fold);
        file.lineTo(size - m, size - m);
        file.lineTo(m, size - m);
        file.closePath();
        g.setStroke(new BasicStroke(1.2f));
        g.draw(file);

        // Play triangle
        int px = size / 2 - 2;
        int py = size / 2;
        g.fillPolygon(new int[]{px - 2, px - 2, px + 4}, new int[]{py - 4, py + 4, py}, 3);
    }

    private static void drawOscillatorEffectIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Sine wave
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 2) * (size / 4);
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawDrumMachineIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        int padSize = (size - m * 2 - 2) / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 2x2 drum pad grid
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int x = m + col * (padSize + 2);
                int y = m + row * (padSize + 2);
                g.drawRoundRect(x, y, padSize, padSize, 3, 3);

                // Fill some pads to show active state
                if ((row == 0 && col == 0) || (row == 1 && col == 1)) {
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                    g.fillRoundRect(x + 1, y + 1, padSize - 2, padSize - 2, 2, 2);
                    g.setColor(color);
                }
            }
        }

        // Kick drum symbol in bottom-left pad
        int kickX = m + 2;
        int kickY = m + padSize + 4;
        g.fillOval(kickX + padSize / 4, kickY + padSize / 4, padSize / 2, padSize / 2 - 2);

        // Hi-hat symbol in top-right pad (two lines)
        int hhX = m + padSize + 4;
        int hhY = m + 2;
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(hhX + 2, hhY + padSize / 2 - 2, hhX + padSize - 4, hhY + padSize / 2 - 2);
        g.drawLine(hhX + 2, hhY + padSize / 2 + 1, hhX + padSize - 4, hhY + padSize / 2 + 1);
    }

    // --- Output Sinks ---

    private static void drawWavFileOutputIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);

        // File shape
        int fold = size / 4;
        Path2D file = new Path2D.Double();
        file.moveTo(m, m);
        file.lineTo(size - m - fold, m);
        file.lineTo(size - m, m + fold);
        file.lineTo(size - m, size - m);
        file.lineTo(m, size - m);
        file.closePath();
        g.setStroke(new BasicStroke(1.2f));
        g.draw(file);

        // Record dot (red)
        g.setColor(DarkTheme.ACCENT_ERROR);
        int dotSize = size / 4;
        g.fillOval(size - m - dotSize, size - m - dotSize, dotSize, dotSize);
    }

    // --- Gain/Dynamics ---

    private static void drawGainIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Fader slot
        int cx = size / 2;
        g.drawLine(cx, m, cx, size - m);

        // Fader knob
        int knobY = size / 3;
        g.fillRect(cx - 4, knobY - 2, 8, 4);
    }

    private static void drawNoiseGateIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Gate (rectangle with gap)
        g.drawLine(m, m, m, size - m);
        g.drawLine(m, m, size / 3, m);
        g.drawLine(m, size - m, size / 3, size - m);

        g.drawLine(size - m, m, size - m, size - m);
        g.drawLine(size * 2 / 3, m, size - m, m);
        g.drawLine(size * 2 / 3, size - m, size - m, size - m);

        // Arrow through
        g.drawLine(size / 3 + 2, size / 2, size * 2 / 3 - 2, size / 2);
    }

    private static void drawCompressorIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Compression curve (knee)
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size - m);
        curve.lineTo(size / 2, size / 2);
        curve.lineTo(size - m, size / 3);
        g.draw(curve);

        // Threshold line
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
        g.drawLine(m, size / 2, size - m, size / 2);
    }

    // --- Distortion ---

    private static void drawOverdriveIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Tube/valve shape
        int tubeW = size / 3;
        int tubeH = size - m * 2;
        int tubeX = size / 2 - tubeW / 2;
        g.drawRoundRect(tubeX, m, tubeW, tubeH, 4, 4);

        // Glow inside
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        g.fillOval(tubeX + 2, m + tubeH / 3, tubeW - 4, tubeH / 3);
    }

    private static void drawDistortionIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Clipped waveform
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        wave.lineTo(m + 2, size / 2);
        wave.lineTo(m + 3, m + 2);  // Clipped top
        wave.lineTo(size / 2 - 2, m + 2);
        wave.lineTo(size / 2, size / 2);
        wave.lineTo(size / 2 + 2, size - m - 2);  // Clipped bottom
        wave.lineTo(size - m - 3, size - m - 2);
        wave.lineTo(size - m - 2, size / 2);
        wave.lineTo(size - m, size / 2);
        g.draw(wave);
    }

    // --- Time-based ---

    private static void drawDelayIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Multiple fading echoes
        for (int i = 0; i < 3; i++) {
            int alpha = 255 - i * 70;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int offset = i * 3;
            int barH = size / 2 - i * 3;
            g.fillRect(m + offset + i * 3, size / 2 - barH / 2, 3, barH);
        }
    }

    private static void drawReverbIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        g.setStroke(new BasicStroke(1.5f));

        // Expanding circles (reverb waves)
        for (int i = 0; i < 3; i++) {
            int alpha = 200 - i * 60;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int r = 3 + i * 4;
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    // --- Modulation ---

    private static void drawChorusIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Multiple overlapping waves
        for (int w = 0; w < 2; w++) {
            int alpha = w == 0 ? 255 : 150;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int offset = w * 2;
            Path2D wave = new Path2D.Double();
            wave.moveTo(m, size / 2 + offset);
            for (int x = m; x <= size - m; x++) {
                double progress = (double) (x - m) / (size - m * 2);
                double y = size / 2 + offset + Math.sin(progress * Math.PI * 2) * (size / 5);
                wave.lineTo(x, y);
            }
            g.draw(wave);
        }
    }

    private static void drawPhaserIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Phase shift representation (wave with notch)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        wave.lineTo(size / 3, size / 2);
        wave.lineTo(size / 3 + 2, m);
        wave.lineTo(size * 2 / 3 - 2, size - m);
        wave.lineTo(size * 2 / 3, size / 2);
        wave.lineTo(size - m, size / 2);
        g.draw(wave);
    }

    private static void drawTremoloIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Amplitude modulated wave (varying height)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double amp = 0.3 + 0.7 * Math.abs(Math.sin(progress * Math.PI * 2));
            double y = size / 2 + Math.sin(progress * Math.PI * 6) * (size / 4) * amp;
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawRingModIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));

        // Ring shape with modulation
        g.drawOval(cx - size / 3, cy - size / 3, size * 2 / 3, size * 2 / 3);

        // Cross lines (multiplication symbol)
        int s = size / 5;
        g.drawLine(cx - s, cy - s, cx + s, cy + s);
        g.drawLine(cx + s, cy - s, cx - s, cy + s);
    }

    // --- EQ ---

    private static void drawParametricEQIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // EQ curve with peak
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size * 2 / 3);
        curve.quadTo(size / 3, size * 2 / 3, size / 2, m + 2);  // Peak
        curve.quadTo(size * 2 / 3, size * 2 / 3, size - m, size * 2 / 3);
        g.draw(curve);

        // Baseline
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
        g.drawLine(m, size * 2 / 3, size - m, size * 2 / 3);
    }

    // --- Amp Simulation ---

    private static void drawAmpIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Amp cabinet shape
        g.drawRoundRect(m, m, size - m * 2, size - m * 2, 4, 4);

        // Speaker grille
        int grillY = m + 3;
        int grillH = (size - m * 2) / 2;
        for (int i = 0; i < 3; i++) {
            int y = grillY + i * (grillH / 3);
            g.drawLine(m + 3, y, size - m - 3, y);
        }

        // Knobs
        int knobY = size - m - 4;
        g.fillOval(size / 3 - 2, knobY, 4, 4);
        g.fillOval(size * 2 / 3 - 2, knobY, 4, 4);
    }

    private static void drawCabinetIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Cabinet box
        g.drawRoundRect(m, m, size - m * 2, size - m * 2, 3, 3);

        // Speaker cone
        int cx = size / 2;
        int cy = size / 2;
        g.drawOval(cx - size / 4, cy - size / 4, size / 2, size / 2);
        g.drawOval(cx - size / 8, cy - size / 8, size / 4, size / 4);
        g.fillOval(cx - 2, cy - 2, 4, 4);
    }

    // --- Filter ---

    private static void drawWahIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Pedal shape (side view)
        Path2D pedal = new Path2D.Double();
        pedal.moveTo(m, size - m);
        pedal.lineTo(m, size / 2);
        pedal.lineTo(size - m, m);
        pedal.lineTo(size - m, size - m);
        pedal.closePath();
        g.draw(pedal);

        // Pivot point
        g.fillOval(m + 2, size / 2 - 2, 4, 4);
    }

    // --- Pitch ---

    private static void drawPitchShiftIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        int cx = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Up arrow
        g.drawLine(cx - 4, m + 4, cx - 4, size / 2 - 2);
        g.drawLine(cx - 7, m + 7, cx - 4, m + 4);
        g.drawLine(cx - 1, m + 7, cx - 4, m + 4);

        // Down arrow
        g.drawLine(cx + 4, size / 2 + 2, cx + 4, size - m - 4);
        g.drawLine(cx + 1, size - m - 7, cx + 4, size - m - 4);
        g.drawLine(cx + 7, size - m - 7, cx + 4, size - m - 4);
    }

    private static void drawOctaverIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Two octaves (8va symbol-like)
        g.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        g.drawString("8", m + 1, size / 2 + 2);

        // Small wave underneath
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size - m - 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size - m - 2 + Math.sin(progress * Math.PI * 4) * 2;
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    // --- Utility ---

    private static void drawSplitterEffectIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Fork shape (one to many)
        g.drawLine(m, size / 2, size / 2, size / 2);
        g.drawLine(size / 2, size / 2, size - m, m + 2);
        g.drawLine(size / 2, size / 2, size - m, size / 2);
        g.drawLine(size / 2, size / 2, size - m, size - m - 2);

        // Input dot
        g.fillOval(m - 2, size / 2 - 2, 4, 4);
    }

    private static void drawMixerEffectIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Merge shape (many to one)
        g.drawLine(m, m + 2, size / 2, size / 2);
        g.drawLine(m, size / 2, size / 2, size / 2);
        g.drawLine(m, size - m - 2, size / 2, size / 2);
        g.drawLine(size / 2, size / 2, size - m, size / 2);

        // Output dot
        g.fillOval(size - m - 2, size / 2 - 2, 4, 4);
    }

    // ==================== NEW EFFECT ICONS ====================

    // --- Input/Output ---

    private static void drawAudioInputIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Microphone shape
        int micW = size / 3;
        int micH = size / 2;
        g.drawRoundRect(size / 2 - micW / 2, m, micW, micH, micW, micW);

        // Stand
        g.drawArc(size / 3, size / 2, size / 3, size / 4, 0, -180);
        g.drawLine(size / 2, size * 3 / 4, size / 2, size - m);
    }

    private static void drawAudioOutputIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Speaker shape
        g.fillRect(m, size / 3, size / 4, size / 3);
        int[] xPoints = {m + size / 4, size - m, size - m, m + size / 4};
        int[] yPoints = {size / 3, m, size - m, size * 2 / 3};
        g.fillPolygon(xPoints, yPoints, 4);
    }

    // --- Dynamics ---

    private static void drawLimiterIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Brick wall with flat top
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size - m);
        curve.lineTo(size / 2 - 2, size / 3);
        curve.lineTo(size - m, size / 3);  // Flat ceiling
        g.draw(curve);

        // Ceiling line
        g.setStroke(new BasicStroke(2f));
        g.drawLine(m, size / 3, size - m, size / 3);
    }

    private static void drawVolumeSwellIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Rising curve (violin bow effect)
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size - m);
        curve.quadTo(size / 2, size - m, size - m, m);
        g.draw(curve);

        // Small note at end
        g.fillOval(size - m - 4, m - 2, 5, 4);
    }

    private static void drawSustainerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Infinite sustain line
        g.drawLine(m, size / 2, size - m, size / 2);

        // Vibrating waves
        for (int i = 0; i < 3; i++) {
            int x = m + 3 + i * (size - m * 2 - 6) / 2;
            g.drawLine(x, size / 2 - 3, x, size / 2 + 3);
        }

        // Infinity symbol hint
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(size / 4, size / 2 + 2, size / 4, size / 4);
        g.drawOval(size / 2, size / 2 + 2, size / 4, size / 4);
    }

    // --- Distortion ---

    private static void drawDriveIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Soft clipped waveform (rounded)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        wave.quadTo(m + 2, m + 2, size / 3, m + 3);
        wave.lineTo(size / 2, m + 3);
        wave.quadTo(size / 2 + 2, size / 2, size / 2 + 2, size - m - 3);
        wave.lineTo(size * 2 / 3, size - m - 3);
        wave.quadTo(size - m - 2, size - m - 2, size - m, size / 2);
        g.draw(wave);
    }

    private static void drawFuzzIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Square wave (extreme fuzz)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        wave.lineTo(m + 2, size / 2);
        wave.lineTo(m + 2, m);
        wave.lineTo(size / 2, m);
        wave.lineTo(size / 2, size - m);
        wave.lineTo(size - m - 2, size - m);
        wave.lineTo(size - m - 2, size / 2);
        wave.lineTo(size - m, size / 2);
        g.draw(wave);
    }

    // --- Delay ---

    private static void drawTapeEchoIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Tape reels
        int reelR = size / 5;
        g.drawOval(m, size / 2 - reelR, reelR * 2, reelR * 2);
        g.drawOval(size - m - reelR * 2, size / 2 - reelR, reelR * 2, reelR * 2);

        // Tape path
        g.drawLine(m + reelR * 2, size / 2, size - m - reelR * 2, size / 2);

        // Center dots
        g.fillOval(m + reelR - 2, size / 2 - 2, 4, 4);
        g.fillOval(size - m - reelR - 2, size / 2 - 2, 4, 4);
    }

    private static void drawMultiTapIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Multiple delay taps at different times
        int[] heights = {size / 2, size / 3, size / 2 + size / 6, size / 3 + size / 8};
        for (int i = 0; i < 4; i++) {
            int x = m + i * (size - m * 2) / 4;
            int h = heights[i];
            int alpha = 255 - i * 40;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillRect(x, size / 2 - (h - size / 2) / 2, 3, h / 2);
        }
    }

    private static void drawReverseDelayIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Backwards arrow
        g.drawLine(size - m, size / 2, m, size / 2);
        g.drawLine(m + 4, size / 3, m, size / 2);
        g.drawLine(m + 4, size * 2 / 3, m, size / 2);

        // Fading bars (reversed)
        for (int i = 0; i < 3; i++) {
            int alpha = 100 + i * 50;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int x = size - m - 2 - i * 4;
            g.fillRect(x, size / 3, 2, size / 3);
        }
    }

    private static void drawPingPongIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Bouncing path L-R-L
        Path2D path = new Path2D.Double();
        path.moveTo(m, m);
        path.lineTo(size - m, size / 3);
        path.lineTo(m, size * 2 / 3);
        path.lineTo(size - m, size - m);
        g.draw(path);

        // Dots at bounce points
        g.fillOval(m - 2, m - 2, 4, 4);
        g.fillOval(size - m - 2, size / 3 - 2, 4, 4);
        g.fillOval(m - 2, size * 2 / 3 - 2, 4, 4);
    }

    // --- Reverb ---

    private static void drawSpringReverbIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Spring coil shape
        Path2D spring = new Path2D.Double();
        spring.moveTo(m, size / 2);
        int coils = 4;
        for (int i = 0; i <= coils; i++) {
            float x = m + (size - m * 2) * i / coils;
            float y = size / 2 + (i % 2 == 0 ? -size / 6 : size / 6);
            spring.lineTo(x, y);
        }
        g.draw(spring);

        // End mounts
        g.fillRect(m - 2, size / 2 - 3, 3, 6);
        g.fillRect(size - m - 1, size / 2 - 3, 3, 6);
    }

    private static void drawShimmerReverbIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        g.setStroke(new BasicStroke(1.5f));

        // Sparkling reverb circles with stars
        for (int i = 0; i < 3; i++) {
            int alpha = 200 - i * 50;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int r = 3 + i * 4;
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }

        // Small sparkles/stars
        g.setColor(color);
        int[] starX = {m + 2, size - m - 3, size / 2};
        int[] starY = {m + 2, m + 4, size - m - 3};
        for (int i = 0; i < 3; i++) {
            drawMiniStar(g, starX[i], starY[i], 3);
        }
    }

    private static void drawMiniStar(Graphics2D g, int cx, int cy, int r) {
        g.drawLine(cx - r, cy, cx + r, cy);
        g.drawLine(cx, cy - r, cx, cy + r);
    }

    // --- Modulation ---

    private static void drawFlangerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Jet sweep (comb filter visualization)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 3) * (size / 4) * (1 - progress * 0.5);
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawVibratoIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Pitch wobble (wavy line)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 4) * (size / 5);
            wave.lineTo(x, y);
        }
        g.draw(wave);

        // Up/down arrows on sides
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(m - 1, size / 3, m - 1, size * 2 / 3);
        g.drawLine(m - 3, size / 3 + 2, m - 1, size / 3);
        g.drawLine(m + 1, size / 3 + 2, m - 1, size / 3);
    }

    private static void drawPannerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // L and R with arrow between
        g.setFont(new Font("SansSerif", Font.BOLD, size / 3));
        g.drawString("L", m, size / 2 + 2);
        g.drawString("R", size - m - size / 4, size / 2 + 2);

        // Bouncing arrow
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(size / 3, size * 2 / 3, size * 2 / 3, size * 2 / 3);
        g.drawLine(size / 3 + 3, size * 2 / 3 - 3, size / 3, size * 2 / 3);
        g.drawLine(size * 2 / 3 - 3, size * 2 / 3 - 3, size * 2 / 3, size * 2 / 3);
    }

    private static void drawUniVibeIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Swirling pattern (psychedelic)
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            int x1 = (int) (cx + Math.cos(angle) * size / 5);
            int y1 = (int) (cy + Math.sin(angle) * size / 5);
            int x2 = (int) (cx + Math.cos(angle + 0.5) * size / 3);
            int y2 = (int) (cy + Math.sin(angle + 0.5) * size / 3);
            g.drawLine(x1, y1, x2, y2);
        }

        // Center circle
        g.drawOval(cx - 3, cy - 3, 6, 6);
    }

    private static void drawRotaryIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Rotating speaker cabinet
        g.drawRoundRect(size / 5, size / 5, size * 3 / 5, size * 3 / 5, 4, 4);

        // Rotating horn (tilted)
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - size / 5, cy, cx + size / 5, cy);
        g.fillOval(cx - size / 6, cy - 2, size / 6, 4);
        g.fillOval(cx, cy - 2, size / 6, 4);

        // Rotation arrow
        g.setStroke(new BasicStroke(1f));
        g.drawArc(cx - size / 4, cy - size / 4, size / 2, size / 2, 45, 270);
    }

    // --- EQ ---

    private static void drawFilterIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Filter curve (lowpass shape)
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, m + 2);
        curve.lineTo(size / 2, m + 2);
        curve.quadTo(size * 3 / 4, m + 2, size * 3 / 4, size / 2);
        curve.lineTo(size - m, size - m);
        g.draw(curve);
    }

    private static void drawGraphicEQIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        int barW = (size - m * 2) / 6;
        g.setColor(color);

        // Multiple EQ sliders at different heights
        int[] heights = {size / 2, size / 3, size / 4, size / 3, size / 2, size * 2 / 3};
        for (int i = 0; i < 6; i++) {
            int x = m + i * barW;
            int h = heights[i];
            g.fillRect(x, size - m - h, barW - 1, h);
        }
    }

    // --- Amp Sim ---

    private static void drawIRLoaderIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Impulse spike
        Path2D ir = new Path2D.Double();
        ir.moveTo(m, size * 2 / 3);
        ir.lineTo(size / 3, size * 2 / 3);
        ir.lineTo(size / 3 + 2, m);
        ir.lineTo(size / 3 + 4, size * 2 / 3);
        ir.quadTo(size * 2 / 3, size * 2 / 3 + size / 6, size - m, size * 2 / 3);
        g.draw(ir);

        // File corner
        g.drawLine(size - m - 4, m, size - m - 4, m + 4);
        g.drawLine(size - m - 4, m + 4, size - m, m + 4);
    }

    // --- Filter Effects ---

    private static void drawEnvelopeFilterIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Envelope curve controlling filter
        Path2D env = new Path2D.Double();
        env.moveTo(m, size - m);
        env.lineTo(m + 3, m);
        env.quadTo(size / 2, m + size / 4, size - m, size * 2 / 3);
        g.draw(env);

        // "Wah" text hint
        g.setFont(new Font("SansSerif", Font.PLAIN, size / 4));
        g.drawString("~", size / 2, size - m);
    }

    private static void drawTalkBoxIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Mouth shape
        g.drawOval(m, size / 3, size - m * 2, size / 2);

        // Lips
        g.drawLine(m + 2, size / 2 + 2, size - m - 2, size / 2 + 2);

        // Sound waves from mouth
        g.drawArc(size - m, size / 3, size / 4, size / 2, -60, 120);
    }

    private static void drawSynthIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Keyboard keys
        int keyW = (size - m * 2) / 5;
        for (int i = 0; i < 5; i++) {
            g.drawRect(m + i * keyW, size / 2, keyW - 1, size / 2 - m);
        }

        // Black keys
        g.fillRect(m + keyW - 2, size / 2, 4, size / 4);
        g.fillRect(m + keyW * 2 - 2, size / 2, 4, size / 4);
        g.fillRect(m + keyW * 4 - 2, size / 2, 4, size / 4);

        // Wave on top
        g.setStroke(new BasicStroke(1.5f));
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 3);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 3 + Math.sin(progress * Math.PI * 3) * size / 8;
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    // --- Utility ---

    private static void drawMono2StereoIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // One input
        g.fillOval(m - 2, size / 2 - 2, 4, 4);
        g.drawLine(m, size / 2, size / 2, size / 2);

        // Split to two outputs
        g.drawLine(size / 2, size / 2, size - m, m + 2);
        g.drawLine(size / 2, size / 2, size - m, size - m - 2);

        // L and R labels
        g.setFont(new Font("SansSerif", Font.PLAIN, size / 5));
        g.drawString("L", size - m + 1, m + 5);
        g.drawString("R", size - m + 1, size - m + 2);
    }

    // ==================== ADDITIONAL EFFECT ICONS ====================

    // --- Utility ---

    private static void drawStereo2MonoIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Two inputs merging to one
        g.drawLine(m, m + 2, size / 2, size / 2);
        g.drawLine(m, size - m - 2, size / 2, size / 2);
        g.drawLine(size / 2, size / 2, size - m, size / 2);

        // L and R labels
        g.setFont(new Font("SansSerif", Font.PLAIN, size / 5));
        g.drawString("L", m - 2, m + 6);
        g.drawString("R", m - 2, size - m + 2);

        // Output dot
        g.fillOval(size - m - 2, size / 2 - 2, 4, 4);
    }

    private static void drawLooperIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        int r = size / 3;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Circular loop arrow
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 30, 300);

        // Arrow head
        int ax = cx + (int)(r * Math.cos(Math.toRadians(30)));
        int ay = cy - (int)(r * Math.sin(Math.toRadians(30)));
        g.drawLine(ax, ay, ax - 4, ay - 3);
        g.drawLine(ax, ay, ax - 1, ay + 5);

        // Record dot in center
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.fillOval(cx - 3, cy - 3, 6, 6);
    }

    private static void drawSettingsEffectIcon(Graphics2D g, int size, Color color) {
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Three horizontal sliders
        int[] positions = {size / 4, size / 2, size * 3 / 4};
        int[] knobPos = {size / 3, size * 2 / 3, size / 2};
        for (int i = 0; i < 3; i++) {
            int y = size / 5 + i * size / 4;
            g.drawLine(size / 5, y, size - size / 5, y);
            g.fillOval(knobPos[i] - 2, y - 2, 5, 5);
        }
    }

    // --- Output ---

    private static void drawMidiRecorderIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.3f));

        // MIDI connector shape (5-pin DIN)
        g.drawOval(m, m, size - m * 2, size - m * 2);

        // 5 pins
        int cx = size / 2;
        int cy = size / 2;
        int pinR = size / 4;
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 3;
            if (i < 5) {
                int px = (int)(cx + pinR * Math.cos(angle));
                int py = (int)(cy - pinR * Math.sin(angle));
                g.fillOval(px - 2, py - 2, 4, 4);
            }
        }

        // Record indicator
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.fillOval(size - m - 4, size - m - 4, 5, 5);
    }

    // --- Dynamics ---

    private static void drawNoiseSuppressorIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Wave being suppressed (small amplitude)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double amp = Math.max(0, Math.sin(progress * Math.PI) - 0.3) * 0.5;
            double y = size / 2 + Math.sin(progress * Math.PI * 4) * (size / 4) * amp;
            wave.lineTo(x, y);
        }
        g.draw(wave);

        // X mark for suppression
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(size - m - 3, m + 2, size - m + 1, m + 6);
        g.drawLine(size - m + 1, m + 2, size - m - 3, m + 6);
    }

    private static void drawMultibandCompIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        int bandW = (size - m * 2) / 3;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Three bands with different compression curves
        int[] heights = {size / 2, size / 3, size * 2 / 5};
        for (int i = 0; i < 3; i++) {
            int x = m + i * bandW;
            int h = heights[i];
            // Compression knee curve
            g.drawLine(x + 2, size - m, x + bandW / 2, size / 2);
            g.drawLine(x + bandW / 2, size / 2, x + bandW - 2, size / 2 - h / 3);
        }

        // Band separators
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
        g.drawLine(m + bandW, m, m + bandW, size - m);
        g.drawLine(m + bandW * 2, m, m + bandW * 2, size - m);
    }

    private static void drawAutoSustainIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Sustaining envelope (flat decay)
        Path2D env = new Path2D.Double();
        env.moveTo(m, size - m);
        env.lineTo(m + 3, m + 2);
        env.lineTo(size - m - 3, m + 2);
        env.lineTo(size - m, size / 2);
        g.draw(env);

        // "A" for auto
        g.setFont(new Font("SansSerif", Font.BOLD, size / 4));
        g.drawString("A", size / 2 - 3, size - m + 2);
    }

    // --- Distortion ---

    private static void drawTubeDistortionIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Tube shape
        int tubeW = size / 2;
        int tubeH = size - m * 2;
        int tubeX = size / 2 - tubeW / 2;
        g.drawRoundRect(tubeX, m, tubeW, tubeH, 6, 6);

        // Glowing filament
        g.setColor(new Color(255, 150, 50, 180));
        g.fillOval(tubeX + 4, m + tubeH / 3, tubeW - 8, tubeH / 3);

        // Distortion waves
        g.setColor(color);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(m - 2, size / 2 - 3, tubeX - 2, size / 2 - 3);
        g.drawLine(m - 2, size / 2 + 3, tubeX - 2, size / 2 + 3);
    }

    // --- Delay ---

    private static void drawQuadDelayIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);

        // Four delay taps in a grid
        int tapSize = (size - m * 2 - 2) / 2;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int x = m + col * (tapSize + 2);
                int y = m + row * (tapSize + 2);
                int alpha = 255 - (row * 2 + col) * 40;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g.fillRoundRect(x, y, tapSize, tapSize, 3, 3);
            }
        }
    }

    // --- Reverb ---

    private static void drawPlateReverbIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Metal plate (rectangle)
        g.drawRect(m, m + 2, size - m * 2, size - m * 2 - 4);

        // Vibration lines
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            int y = m + 4 + i * (size - m * 2 - 8) / 3;
            g.drawLine(m + 3, y, size - m - 3, y);
        }

        // Corner mounts
        g.fillOval(m, m, 4, 4);
        g.fillOval(size - m - 4, m, 4, 4);
        g.fillOval(m, size - m - 4, 4, 4);
        g.fillOval(size - m - 4, size - m - 4, 4, 4);
    }

    private static void drawRoomReverbIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Room shape (3D box perspective)
        // Back wall
        g.drawRect(m + 2, m + 2, size - m * 2 - 4, size - m * 2 - 4);

        // Floor lines (perspective)
        g.drawLine(m, size - m, m + 2, size - m - 2);
        g.drawLine(size - m, size - m, size - m - 2, size - m - 2);

        // Sound waves in room
        g.setStroke(new BasicStroke(1f));
        int cx = size / 2;
        int cy = size / 2;
        g.drawArc(cx - 4, cy - 4, 8, 8, 0, 360);
        g.drawArc(cx - 7, cy - 7, 14, 14, 30, 120);
    }

    private static void drawStereoImageReverbIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Two speakers with reverb
        int spkW = size / 4;

        // Left speaker
        g.drawRect(m, size / 3, spkW, size / 3);

        // Right speaker
        g.drawRect(size - m - spkW, size / 3, spkW, size / 3);

        // Converging reverb waves
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = size / 2;
        g.drawArc(m + spkW, size / 4, size / 4, size / 2, -60, 120);
        g.drawArc(size / 2 - size / 8, size / 4, size / 4, size / 2, 120, 120);
    }

    // --- Modulation ---

    private static void drawPan3DIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // 3D sphere (circle with latitude/longitude)
        g.drawOval(m, m, size - m * 2, size - m * 2);

        // Equator
        g.drawArc(m, cy - size / 8, size - m * 2, size / 4, 0, 180);

        // Meridian
        g.drawArc(cx - size / 8, m, size / 4, size - m * 2, 90, 180);

        // Sound source dot
        g.setColor(DarkTheme.ACCENT_SUCCESS);
        g.fillOval(cx + size / 5, cy - size / 5, 5, 5);
    }

    // --- EQ ---

    private static void drawPickupEmulatorIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Pickup shape (humbucker)
        int pkW = size - m * 2;
        int pkH = size / 3;
        g.drawRoundRect(m, size / 2 - pkH / 2, pkW, pkH, 4, 4);

        // Pole pieces (6 dots)
        int poleSpacing = pkW / 7;
        for (int i = 0; i < 6; i++) {
            int x = m + poleSpacing + i * poleSpacing - 2;
            g.fillOval(x, size / 2 - 2, 4, 4);
        }

        // Mounting screws
        g.fillOval(m + 2, size / 2 - pkH / 2 - 3, 3, 3);
        g.fillOval(size - m - 5, size / 2 - pkH / 2 - 3, 3, 3);
    }

    // --- Amp Sim ---

    private static void drawNeuralAmpIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Neural network nodes
        int[] layerX = {m + 2, size / 2, size - m - 2};
        int[][] nodeY = {
            {size / 3, size * 2 / 3},
            {size / 4, size / 2, size * 3 / 4},
            {size / 3, size * 2 / 3}
        };

        // Draw connections first
        g.setStroke(new BasicStroke(0.8f));
        for (int y1 : nodeY[0]) {
            for (int y2 : nodeY[1]) {
                g.drawLine(layerX[0], y1, layerX[1], y2);
            }
        }
        for (int y1 : nodeY[1]) {
            for (int y2 : nodeY[2]) {
                g.drawLine(layerX[1], y1, layerX[2], y2);
            }
        }

        // Draw nodes
        g.setColor(color);
        for (int layer = 0; layer < 3; layer++) {
            for (int y : nodeY[layer]) {
                g.fillOval(layerX[layer] - 3, y - 3, 6, 6);
            }
        }
    }

    private static void drawTubePreampIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Small tube
        int tubeW = size / 3;
        int tubeH = size / 2;
        int tubeX = size / 2 - tubeW / 2;
        g.drawRoundRect(tubeX, m + 2, tubeW, tubeH, 5, 5);

        // Filament glow
        g.setColor(new Color(255, 200, 100, 150));
        g.fillOval(tubeX + 3, m + tubeH / 3, tubeW - 6, tubeH / 3);

        // "PRE" label
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.PLAIN, size / 5));
        g.drawString("PRE", m, size - m);
    }

    private static void drawTubePowerAmpIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Larger tube
        int tubeW = size / 2;
        int tubeH = size / 2;
        int tubeX = size / 2 - tubeW / 2;
        g.drawRoundRect(tubeX, m, tubeW, tubeH, 6, 6);

        // Bigger filament glow
        g.setColor(new Color(255, 150, 50, 180));
        g.fillOval(tubeX + 4, m + tubeH / 4, tubeW - 8, tubeH / 2);

        // "PWR" label
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.BOLD, size / 5));
        g.drawString("PWR", m - 2, size - m + 2);
    }

    private static void drawCabinetSimulatorIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Cabinet front
        g.drawRoundRect(m, m, size - m * 2, size - m * 2, 4, 4);

        // 2x2 speakers
        int spkR = (size - m * 2 - 6) / 4;
        int offset = m + 2;
        g.drawOval(offset, offset, spkR * 2, spkR * 2);
        g.drawOval(size - offset - spkR * 2, offset, spkR * 2, spkR * 2);
        g.drawOval(offset, size - offset - spkR * 2, spkR * 2, spkR * 2);
        g.drawOval(size - offset - spkR * 2, size - offset - spkR * 2, spkR * 2, spkR * 2);

        // Mic symbol
        g.setColor(DarkTheme.ACCENT_PRIMARY);
        g.fillOval(size / 2 - 2, size / 2 - 2, 4, 4);
    }

    // --- Filter/Synth ---

    private static void drawSynthDroneIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Sustained flat line with slight wobble
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y = size / 2 + Math.sin(progress * Math.PI * 8) * 2;
            wave.lineTo(x, y);
        }
        g.draw(wave);

        // Infinity symbol for drone
        g.setStroke(new BasicStroke(1.5f));
        int iy = size * 3 / 4;
        g.drawOval(m + 2, iy - 3, size / 4, 6);
        g.drawOval(size / 2 - 2, iy - 3, size / 4, 6);
    }

    private static void drawPitchSynthIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Pitch detection to synth
        // Input wave (small)
        g.drawLine(m, size / 3, m + size / 4, size / 3);

        // Arrow
        g.drawLine(m + size / 4 + 2, size / 3, size / 2, size / 2);

        // Synth wave (output)
        Path2D wave = new Path2D.Double();
        wave.moveTo(size / 2, size / 2);
        for (int x = size / 2; x <= size - m; x++) {
            double progress = (double) (x - size / 2) / (size / 2 - m);
            double y = size / 2 + Math.sin(progress * Math.PI * 3) * (size / 5);
            wave.lineTo(x, y);
        }
        g.draw(wave);
    }

    private static void drawAcousticSimIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Acoustic guitar body shape
        int bodyW = size - m * 2;
        int bodyH = size - m * 2;

        // Upper bout
        g.drawArc(m, m, bodyW, bodyH / 2, 0, 180);

        // Lower bout (larger)
        g.drawArc(m - 2, size / 2 - 2, bodyW + 4, bodyH / 2 + 2, 180, 180);

        // Waist
        g.drawLine(m, size / 2, m + 3, size / 2);
        g.drawLine(size - m, size / 2, size - m - 3, size / 2);

        // Sound hole
        g.drawOval(size / 2 - 4, size / 2 - 2, 8, 8);
    }

    // --- Pitch ---

    private static void drawHarmonizerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Two parallel note lines (harmony)
        Path2D wave1 = new Path2D.Double();
        Path2D wave2 = new Path2D.Double();
        wave1.moveTo(m, size / 3);
        wave2.moveTo(m, size * 2 / 3);

        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double y1 = size / 3 + Math.sin(progress * Math.PI * 2) * (size / 8);
            double y2 = size * 2 / 3 + Math.sin(progress * Math.PI * 2) * (size / 8);
            wave1.lineTo(x, y1);
            wave2.lineTo(x, y2);
        }

        g.draw(wave1);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g.draw(wave2);

        // Interval marker
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.PLAIN, size / 5));
        g.drawString("3", size - m, size / 2 + 2);
    }

    private static void drawAutoTunerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Pitch correction (wobbly to straight)
        Path2D wave = new Path2D.Double();
        wave.moveTo(m, size / 2);
        for (int x = m; x <= size - m; x++) {
            double progress = (double) (x - m) / (size - m * 2);
            double wobble = (1 - progress) * Math.sin(progress * Math.PI * 6) * (size / 6);
            double y = size / 2 + wobble;
            wave.lineTo(x, y);
        }
        g.draw(wave);

        // Target pitch line
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
        g.drawLine(m, size / 2, size - m, size / 2);
    }

    // --- Acoustic ---

    private static void drawBodyResonanceIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Guitar body outline
        g.drawOval(m, m + 2, size - m * 2, size - m * 2 - 4);

        // Resonance waves inside
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i <= 2; i++) {
            int r = i * size / 6;
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }

        // Sound hole
        g.fillOval(cx - 3, cy - 3, 6, 6);
    }

    private static void drawAntiFeedbackIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Feedback loop with X
        int cx = size / 2;
        int cy = size / 2;
        int r = size / 3;

        // Circular arrow (feedback)
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 45, 270);

        // X through it (anti)
        g.setColor(DarkTheme.ACCENT_ERROR);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(m + 2, m + 2, size - m - 2, size - m - 2);
        g.drawLine(size - m - 2, m + 2, m + 2, size - m - 2);
    }

    private static void drawTwelveStringIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1f));

        // 12 string pairs (6 pairs of 2)
        int stringSpacing = (size - m * 2) / 7;
        for (int i = 0; i < 6; i++) {
            int x = m + stringSpacing / 2 + i * stringSpacing;
            // Main string
            g.drawLine(x, m, x, size - m);
            // Octave string (thinner, slightly offset)
            g.setStroke(new BasicStroke(0.5f));
            g.drawLine(x + 2, m, x + 2, size - m);
            g.setStroke(new BasicStroke(1f));
        }

        // "12" label
        g.setFont(new Font("SansSerif", Font.BOLD, size / 4));
        g.drawString("12", m - 2, size - m + 4);
    }

    private static void drawPiezoSweetenerIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        // Piezo crystal shape (hexagon-ish)
        int cx = size / 2;
        int cy = size / 2;
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 6 + i * Math.PI / 3;
            xPoints[i] = (int)(cx + size / 3 * Math.cos(angle));
            yPoints[i] = (int)(cy + size / 3 * Math.sin(angle));
        }
        g.drawPolygon(xPoints, yPoints, 6);

        // Sparkle/sweet indicator
        g.setColor(DarkTheme.ACCENT_WARNING);
        g.fillOval(size - m - 2, m, 4, 4);
        drawMiniStar(g, size - m, m + 2, 2);
    }

    private static void drawAcousticCompressorIcon(Graphics2D g, int size, Color color) {
        int m = size / 5;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Compression curve
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size - m);
        curve.lineTo(size / 2, size / 2);
        curve.lineTo(size - m, size / 3);
        g.draw(curve);

        // Guitar body hint (small)
        g.setStroke(new BasicStroke(1f));
        g.drawArc(m - 2, m, size / 3, size / 4, 0, 180);
    }

    private static void drawAcousticEQIcon(Graphics2D g, int size, Color color) {
        int m = size / 6;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // EQ curve suited for acoustic
        Path2D curve = new Path2D.Double();
        curve.moveTo(m, size * 2 / 3);
        curve.quadTo(size / 4, size / 2, size / 3, size / 2);  // Low cut
        curve.quadTo(size / 2, size / 3, size * 2 / 3, size / 2);  // Mid scoop
        curve.quadTo(size * 3 / 4, size / 2, size - m, size / 3);  // High shelf
        g.draw(curve);

        // Guitar sound hole hint
        g.setStroke(new BasicStroke(1f));
        g.drawOval(size / 2 - 3, size - m - 4, 6, 6);
    }
}
