package it.denzosoft.jfx2.preset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Factory templates generator.
 *
 * <p>Creates default templates for common guitar tones.</p>
 */
public class FactoryTemplates {

    private final TemplateSerializer serializer = new TemplateSerializer();

    // Layout constants
    private static final int Y_CENTER = 200;      // Y position for linear chains
    private static final int X_START = 50;        // Starting X position
    private static final int X_SPACING = 180;     // Horizontal spacing between nodes

    /**
     * Generate all factory templates and save to directory.
     */
    public void generateAll(Path factoryDir) throws IOException {
        Files.createDirectories(factoryDir);

        save(createClean(), factoryDir.resolve("Clean.jfxrig"));
        save(createCrunch(), factoryDir.resolve("Crunch.jfxrig"));
        save(createLead(), factoryDir.resolve("Lead.jfxrig"));
        save(createAmbient(), factoryDir.resolve("Ambient.jfxrig"));
    }

    private void save(Rig rig, Path path) throws IOException {
        String json = serializer.serialize(rig);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        System.out.println("Created: " + path.getFileName());
    }

    /**
     * Clean tone: subtle compression, light EQ, touch of reverb.
     */
    public Rig createClean() {
        RigMetadata meta = new RigMetadata(
                "Clean",
                "JFx2 Factory",
                "Crystal clear clean tone with subtle enhancement",
                "Clean",
                "clean, jazz, country",
                RigMetadata.CURRENT_VERSION,
                "2024-01-01T00:00:00Z",
                "2024-01-01T00:00:00Z"
        );

        Rig rig = new Rig(meta);
        int x = X_START;

        // Audio Input
        rig.addNode(new Rig.NodeDefinition("input", "audioinput", "Audio In", false, x, Y_CENTER,
                Map.of(), Map.of()));
        x += X_SPACING;

        // Noise Gate
        rig.addNode(new Rig.NodeDefinition("gate", "noisegate", "Noise Gate", false, x, Y_CENTER,
                Map.of("threshold", -55.0, "attack", 0.5, "release", 50.0), Map.of()));
        x += X_SPACING;

        // Light Compressor
        rig.addNode(new Rig.NodeDefinition("comp", "compressor", "Compressor", false, x, Y_CENTER,
                Map.of("threshold", -15.0, "ratio", 2.0, "attack", 20.0, "release", 150.0, "makeup", 2.0), Map.of()));
        x += X_SPACING;

        // EQ - slight bass cut, presence boost
        rig.addNode(new Rig.NodeDefinition("eq", "parametriceq", "EQ", false, x, Y_CENTER,
                Map.of("lowFreq", 100.0, "lowGain", -2.0, "highMidFreq", 3000.0, "highMidGain", 2.0, "highMidQ", 1.5), Map.of()));
        x += X_SPACING;

        // Light Chorus
        rig.addNode(new Rig.NodeDefinition("chorus", "chorus", "Chorus", false, x, Y_CENTER,
                Map.of("rate", 0.5, "depth", 20.0, "mix", 15.0), Map.of()));
        x += X_SPACING;

        // Room Reverb
        rig.addNode(new Rig.NodeDefinition("reverb", "reverb", "Reverb", false, x, Y_CENTER,
                Map.of("roomSize", 30.0, "damp", 60.0, "mix", 15.0, "predelay", 10.0), Map.of()));
        x += X_SPACING;

        // Audio Output
        rig.addNode(new Rig.NodeDefinition("output", "audiooutput", "Audio Out", false, x, Y_CENTER,
                Map.of(), Map.of()));

        // Connections: Input -> Gate -> Comp -> EQ -> Chorus -> Reverb -> Output
        rig.addConnection(Rig.ConnectionDefinition.of("input", "out", "gate", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("gate", "out", "comp", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("comp", "out", "eq", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("eq", "out", "chorus", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("chorus", "out", "reverb", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("reverb", "out", "output", "in"));

        return rig;
    }

    /**
     * Crunch tone: light overdrive, rock rhythm sound.
     */
    public Rig createCrunch() {
        RigMetadata meta = new RigMetadata(
                "Crunch",
                "JFx2 Factory",
                "Classic rock crunch rhythm tone",
                "Crunch",
                "rock, blues, crunch",
                RigMetadata.CURRENT_VERSION,
                "2024-01-01T00:00:00Z",
                "2024-01-01T00:00:00Z"
        );

        Rig rig = new Rig(meta);
        int x = X_START;

        // Audio Input
        rig.addNode(new Rig.NodeDefinition("input", "audioinput", "Audio In", false, x, Y_CENTER,
                Map.of(), Map.of()));
        x += X_SPACING;

        // Noise Gate
        rig.addNode(new Rig.NodeDefinition("gate", "noisegate", "Noise Gate", false, x, Y_CENTER,
                Map.of("threshold", -50.0, "attack", 0.5, "release", 50.0), Map.of()));
        x += X_SPACING;

        // Compressor
        rig.addNode(new Rig.NodeDefinition("comp", "compressor", "Compressor", false, x, Y_CENTER,
                Map.of("threshold", -18.0, "ratio", 3.0, "attack", 15.0, "release", 100.0, "makeup", 3.0), Map.of()));
        x += X_SPACING;

        // Overdrive
        rig.addNode(new Rig.NodeDefinition("od", "overdrive", "Overdrive", false, x, Y_CENTER,
                Map.of("drive", 6.0, "tone", 3500.0, "level", 0.0), Map.of()));
        x += X_SPACING;

        // EQ - mid boost for crunch
        rig.addNode(new Rig.NodeDefinition("eq", "parametriceq", "EQ", false, x, Y_CENTER,
                Map.of("lowMidFreq", 800.0, "lowMidGain", 2.0, "highMidFreq", 2500.0, "highMidGain", 1.5), Map.of()));
        x += X_SPACING;

        // Short delay
        rig.addNode(new Rig.NodeDefinition("delay", "delay", "Delay", false, x, Y_CENTER,
                Map.of("time", 300.0, "feedback", 20.0, "mix", 15.0, "filter", 6000.0), Map.of()));
        x += X_SPACING;

        // Room Reverb
        rig.addNode(new Rig.NodeDefinition("reverb", "reverb", "Reverb", false, x, Y_CENTER,
                Map.of("roomSize", 25.0, "damp", 50.0, "mix", 12.0), Map.of()));
        x += X_SPACING;

        // Audio Output
        rig.addNode(new Rig.NodeDefinition("output", "audiooutput", "Audio Out", false, x, Y_CENTER,
                Map.of(), Map.of()));

        // Connections
        rig.addConnection(Rig.ConnectionDefinition.of("input", "out", "gate", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("gate", "out", "comp", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("comp", "out", "od", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("od", "out", "eq", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("eq", "out", "delay", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("delay", "out", "reverb", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("reverb", "out", "output", "in"));

        return rig;
    }

    /**
     * Lead tone: high gain distortion for solos.
     */
    public Rig createLead() {
        RigMetadata meta = new RigMetadata(
                "Lead",
                "JFx2 Factory",
                "High gain lead tone for solos",
                "Lead",
                "lead, solo, metal, rock",
                RigMetadata.CURRENT_VERSION,
                "2024-01-01T00:00:00Z",
                "2024-01-01T00:00:00Z"
        );

        Rig rig = new Rig(meta);
        int x = X_START;

        // Audio Input
        rig.addNode(new Rig.NodeDefinition("input", "audioinput", "Audio In", false, x, Y_CENTER,
                Map.of(), Map.of()));
        x += X_SPACING;

        // Noise Gate (tighter for high gain)
        rig.addNode(new Rig.NodeDefinition("gate", "noisegate", "Noise Gate", false, x, Y_CENTER,
                Map.of("threshold", -45.0, "attack", 0.3, "hold", 30.0, "release", 40.0), Map.of()));
        x += X_SPACING;

        // Compressor (sustain)
        rig.addNode(new Rig.NodeDefinition("comp", "compressor", "Compressor", false, x, Y_CENTER,
                Map.of("threshold", -20.0, "ratio", 4.0, "attack", 10.0, "release", 80.0, "makeup", 4.0), Map.of()));
        x += X_SPACING;

        // Overdrive (boost)
        rig.addNode(new Rig.NodeDefinition("od", "overdrive", "Boost", false, x, Y_CENTER,
                Map.of("drive", 3.0, "tone", 4000.0, "level", 3.0), Map.of()));
        x += X_SPACING;

        // Distortion (main gain)
        rig.addNode(new Rig.NodeDefinition("dist", "distortion", "Distortion", false, x, Y_CENTER,
                Map.of("drive", 35.0, "preTone", 4.0, "postTone", 4500.0, "clipType", 0, "level", -3.0), Map.of()));
        x += X_SPACING;

        // EQ - scoop mids for metal or boost for rock
        rig.addNode(new Rig.NodeDefinition("eq", "parametriceq", "EQ", false, x, Y_CENTER,
                Map.of("lowGain", 1.0, "lowMidFreq", 500.0, "lowMidGain", -1.0, "highMidFreq", 3000.0, "highMidGain", 3.0, "highGain", 1.0), Map.of()));
        x += X_SPACING;

        // Delay
        rig.addNode(new Rig.NodeDefinition("delay", "delay", "Delay", false, x, Y_CENTER,
                Map.of("time", 400.0, "feedback", 30.0, "mix", 20.0, "filter", 5000.0), Map.of()));
        x += X_SPACING;

        // Hall Reverb
        rig.addNode(new Rig.NodeDefinition("reverb", "reverb", "Reverb", false, x, Y_CENTER,
                Map.of("roomSize", 50.0, "damp", 40.0, "mix", 18.0, "predelay", 20.0), Map.of()));
        x += X_SPACING;

        // Audio Output
        rig.addNode(new Rig.NodeDefinition("output", "audiooutput", "Audio Out", false, x, Y_CENTER,
                Map.of(), Map.of()));

        // Connections
        rig.addConnection(Rig.ConnectionDefinition.of("input", "out", "gate", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("gate", "out", "comp", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("comp", "out", "od", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("od", "out", "dist", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("dist", "out", "eq", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("eq", "out", "delay", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("delay", "out", "reverb", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("reverb", "out", "output", "in"));

        return rig;
    }

    /**
     * Ambient tone: clean with heavy modulation and reverb.
     * Uses parallel routing: Clean path + Modulation path mixed together.
     */
    public Rig createAmbient() {
        RigMetadata meta = new RigMetadata(
                "Ambient",
                "JFx2 Factory",
                "Atmospheric ambient soundscape with parallel routing",
                "Ambient",
                "ambient, atmospheric, post-rock, shoegaze",
                RigMetadata.CURRENT_VERSION,
                "2024-01-01T00:00:00Z",
                "2024-01-01T00:00:00Z"
        );

        Rig rig = new Rig(meta);

        // Layout for parallel routing
        int yCenter = 250;
        int yTop = 120;     // Upper path
        int yBottom = 380;  // Lower path

        // Audio Input
        rig.addNode(new Rig.NodeDefinition("input", "audioinput", "Audio In", false, X_START, yCenter,
                Map.of(), Map.of()));

        // Input processing - Compressor
        rig.addNode(new Rig.NodeDefinition("comp", "compressor", "Compressor", false, X_START + X_SPACING, yCenter,
                Map.of("threshold", -20.0, "ratio", 2.5, "attack", 30.0, "release", 200.0), Map.of()));

        // Splitter for parallel paths
        rig.addNode(Rig.NodeDefinition.splitter("split", X_START + X_SPACING * 2, yCenter, 2));

        // Path A: Clean with light chorus (upper row)
        rig.addNode(new Rig.NodeDefinition("chorus", "chorus", "Chorus", false, X_START + X_SPACING * 3, yTop,
                Map.of("rate", 0.3, "depth", 40.0, "mix", 40.0), Map.of()));

        // Path B: Shimmer (phaser + tremolo) (lower row)
        rig.addNode(new Rig.NodeDefinition("phaser", "phaser", "Phaser", false, X_START + X_SPACING * 3, yBottom,
                Map.of("rate", 0.2, "depth", 60.0, "feedback", 50.0, "mix", 50.0), Map.of()));
        rig.addNode(new Rig.NodeDefinition("tremolo", "tremolo", "Tremolo", false, X_START + X_SPACING * 4, yBottom,
                Map.of("rate", 3.0, "depth", 30.0, "waveform", 0), Map.of()));

        // Mixer (2 inputs) - center, receives both paths
        Map<String, Object> mixerConfig = new LinkedHashMap<>();
        mixerConfig.put("numInputs", 2);
        mixerConfig.put("levels", List.of(1.0, 0.7));  // Path B slightly lower
        mixerConfig.put("pans", List.of(0.0, 0.0));
        mixerConfig.put("masterLevel", 1.0);
        rig.addNode(Rig.NodeDefinition.mixer("mixer", X_START + X_SPACING * 5, yCenter, 2, mixerConfig));

        // Long delay
        rig.addNode(new Rig.NodeDefinition("delay", "delay", "Delay", false, X_START + X_SPACING * 6, yCenter,
                Map.of("time", 500.0, "feedback", 45.0, "mix", 35.0, "filter", 4000.0), Map.of()));

        // Big reverb
        rig.addNode(new Rig.NodeDefinition("reverb", "reverb", "Reverb", false, X_START + X_SPACING * 7, yCenter,
                Map.of("roomSize", 80.0, "damp", 30.0, "mix", 40.0, "predelay", 30.0, "width", 100.0), Map.of()));

        // Audio Output
        rig.addNode(new Rig.NodeDefinition("output", "audiooutput", "Audio Out", false, X_START + X_SPACING * 8, yCenter,
                Map.of(), Map.of()));

        // Connections
        rig.addConnection(Rig.ConnectionDefinition.of("input", "out", "comp", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("comp", "out", "split", "in"));

        // Path A: split -> chorus -> mixer input 1
        rig.addConnection(Rig.ConnectionDefinition.of("split", "out1", "chorus", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("chorus", "out", "mixer", "in1"));

        // Path B: split -> phaser -> tremolo -> mixer input 2
        rig.addConnection(Rig.ConnectionDefinition.of("split", "out2", "phaser", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("phaser", "out", "tremolo", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("tremolo", "out", "mixer", "in2"));

        // Mixer -> delay -> reverb -> output
        rig.addConnection(Rig.ConnectionDefinition.of("mixer", "out", "delay", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("delay", "out", "reverb", "in"));
        rig.addConnection(Rig.ConnectionDefinition.of("reverb", "out", "output", "in"));

        return rig;
    }

    /**
     * Main method to generate factory templates.
     */
    public static void main(String[] args) throws IOException {
        Path factoryDir = Path.of("presets", "factory");
        new FactoryTemplates().generateAll(factoryDir);
        System.out.println("Factory templates generated in: " + factoryDir.toAbsolutePath());
    }
}
