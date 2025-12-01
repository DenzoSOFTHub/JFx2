package it.denzosoft.jfx2.preset;

import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.effects.impl.LooperEffect;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.canvas.CanvasController;

import java.awt.Point;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Manages rig template loading, saving, and application to the signal graph.
 *
 * <p>Templates are complete rig configurations that can be loaded and applied
 * to the signal graph. This is different from effect presets which only store
 * parameter values for individual effects.</p>
 */
public class TemplateManager {

    private static final String TEMPLATE_EXTENSION = ".jfxrig";
    private static final String FACTORY_SUBDIR = "factory";

    private final Path templatesDir;
    private final TemplateSerializer serializer;
    private final EffectFactory effectFactory;

    /**
     * Create a template manager with default templates directory (./presets).
     */
    public TemplateManager() {
        this(Path.of("presets"));
    }

    /**
     * Create a template manager with custom templates directory.
     */
    public TemplateManager(Path templatesDir) {
        this.templatesDir = templatesDir;
        this.serializer = new TemplateSerializer();
        this.effectFactory = EffectFactory.getInstance();

        // Ensure directories exist
        try {
            Files.createDirectories(templatesDir);
            Files.createDirectories(templatesDir.resolve(FACTORY_SUBDIR));
        } catch (IOException e) {
            System.err.println("Warning: Could not create templates directory: " + e.getMessage());
        }
    }

    // ==================== LOADING ====================

    /**
     * Load a template from file.
     */
    public Rig load(String filename) throws IOException {
        Path path = resolveTemplatePath(filename);
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return serializer.deserialize(json);
    }

    /**
     * Load a template and apply it to a signal graph.
     *
     * @param filename   Template filename
     * @param graph      Target signal graph
     * @param controller Canvas controller for restoring positions (can be null)
     */
    public void loadAndApply(String filename, SignalGraph graph, CanvasController controller) throws IOException {
        Rig rig = load(filename);
        applyToGraph(rig, graph, controller);
    }

    /**
     * Load a template and apply it to a signal graph (without position restoration).
     */
    public void loadAndApply(String filename, SignalGraph graph) throws IOException {
        loadAndApply(filename, graph, null);
    }

    // ==================== SAVING ====================

    /**
     * Save a template to file.
     */
    public void save(Rig rig, String filename) throws IOException {
        // Update modification time
        rig.setMetadata(rig.getMetadata().withModifiedNow());

        String json = serializer.serialize(rig);
        Path path = templatesDir.resolve(ensureExtension(filename));
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    /**
     * Create a Rig from the current signal graph configuration (without positions).
     */
    public Rig createFromGraph(SignalGraph graph, String name, String category) {
        return createFromGraph(graph, null, name, category);
    }

    /**
     * Create a Rig from the current signal graph configuration.
     *
     * @param graph      The signal graph
     * @param controller The canvas controller (for node positions)
     * @param name       Rig name
     * @param category   Rig category
     */
    public Rig createFromGraph(SignalGraph graph, CanvasController controller, String name, String category) {
        RigMetadata metadata = RigMetadata.of(name, category);
        Rig rig = new Rig(metadata);

        // Export all nodes (including input/output effects)
        for (ProcessingNode node : graph.getNodes()) {
            // Skip old-style InputNode/OutputNode (graph infrastructure nodes)
            if (node instanceof InputNode || node instanceof OutputNode) {
                continue;
            }

            Point pos = controller != null ? controller.getNodePosition(node.getId()) : new Point(0, 0);
            Rig.NodeDefinition nodeDef = exportNode(node, pos.x, pos.y);
            if (nodeDef != null) {
                rig.addNode(nodeDef);
            }
        }

        // Export connections
        for (Connection conn : graph.getConnections()) {
            ProcessingNode source = conn.getSourceNode();
            ProcessingNode target = conn.getTargetNode();

            // Map input/output nodes to special IDs
            String sourceId = (source instanceof InputNode) ? "input" : source.getId();
            String targetId = (target instanceof OutputNode) ? "output" : target.getId();

            rig.addConnection(new Rig.ConnectionDefinition(
                    sourceId,
                    conn.getSourcePort().getId(),
                    targetId,
                    conn.getTargetPort().getId()
            ));
        }

        return rig;
    }

    private Rig.NodeDefinition exportNode(ProcessingNode node, int x, int y) {
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> config = new LinkedHashMap<>();

        if (node instanceof EffectNode effectNode) {
            // Export effect parameters
            AudioEffect effect = effectNode.getEffect();
            for (Parameter param : effect.getParameters()) {
                params.put(param.getId(), param.getTargetValue());
            }

            // Special handling for LooperEffect - save file path and autoplay
            if (effect instanceof LooperEffect looper) {
                String wavFile = looper.getWavFilePath();
                if (wavFile != null && !wavFile.isEmpty()) {
                    config.put("wavFilePath", wavFile);
                    config.put("autoPlay", looper.isAutoPlay());
                }
            }

            return new Rig.NodeDefinition(
                    node.getId(),
                    effect.getMetadata().id(),
                    node.getName(),
                    node.isBypassed(),
                    x, y,
                    params,
                    config
            );
        } else if (node instanceof SplitterNode splitter) {
            config.put("numOutputs", splitter.getNumOutputs());
            return new Rig.NodeDefinition(
                    node.getId(),
                    "splitter",
                    node.getName(),
                    node.isBypassed(),
                    x, y,
                    params,
                    config
            );
        } else if (node instanceof MixerNode mixer) {
            config.put("numInputs", mixer.getNumInputs());
            config.put("stereoMode", mixer.getStereoMode().name());
            // Export mixer levels and pans
            List<Object> levels = new ArrayList<>();
            List<Object> pans = new ArrayList<>();
            for (int i = 0; i < mixer.getNumInputs(); i++) {
                levels.add(mixer.getLevel(i));
                pans.add(mixer.getPan(i));
            }
            config.put("levels", levels);
            config.put("pans", pans);
            config.put("masterLevel", mixer.getMasterLevel());
            return new Rig.NodeDefinition(
                    node.getId(),
                    "mixer",
                    node.getName(),
                    node.isBypassed(),
                    x, y,
                    params,
                    config
            );
        }

        return null;
    }

    // ==================== APPLYING ====================

    /**
     * Apply a Rig to a SignalGraph (without position restoration).
     */
    public void applyToGraph(Rig rig, SignalGraph graph) {
        applyToGraph(rig, graph, null);
    }

    /**
     * Apply a Rig to a SignalGraph.
     *
     * @param rig        The rig to apply
     * @param graph      Target signal graph
     * @param controller Canvas controller for restoring positions (can be null)
     */
    public void applyToGraph(Rig rig, SignalGraph graph, CanvasController controller) {
        // Clear existing graph
        graph.clear();

        // Create nodes
        Map<String, ProcessingNode> nodeMap = new HashMap<>();

        // Note: We no longer create default input/output nodes
        // The rig should contain AudioInputEffect and AudioOutputEffect nodes

        for (Rig.NodeDefinition nodeDef : rig.getNodes()) {
            ProcessingNode node = createNode(nodeDef);
            if (node != null) {
                graph.addNode(node);
                nodeMap.put(nodeDef.id(), node);

                // Restore position
                if (controller != null) {
                    controller.setNodePosition(nodeDef.id(), nodeDef.x(), nodeDef.y());
                }
            }
        }

        // Create connections
        for (Rig.ConnectionDefinition connDef : rig.getConnections()) {
            ProcessingNode sourceNode = nodeMap.get(connDef.sourceNodeId());
            ProcessingNode targetNode = nodeMap.get(connDef.targetNodeId());

            if (sourceNode == null || targetNode == null) {
                System.err.println("Warning: Could not find nodes for connection: " +
                        connDef.sourceNodeId() + " -> " + connDef.targetNodeId());
                continue;
            }

            // Find ports
            Port sourcePort = findPort(sourceNode.getOutputPorts(), connDef.sourcePortId());
            Port targetPort = findPort(targetNode.getInputPorts(), connDef.targetPortId());

            if (sourcePort == null) {
                // Try default output port
                sourcePort = getDefaultOutputPort(sourceNode);
            }
            if (targetPort == null) {
                // Try default input port
                targetPort = getDefaultInputPort(targetNode);
            }

            if (sourcePort != null && targetPort != null) {
                try {
                    graph.connect(sourcePort, targetPort);
                } catch (Exception e) {
                    System.err.println("Warning: Could not create connection: " + e.getMessage());
                }
            }
        }
    }

    private ProcessingNode createNode(Rig.NodeDefinition nodeDef) {
        String type = nodeDef.type();

        if ("splitter".equals(type)) {
            int numOutputs = getConfigInt(nodeDef.config(), "numOutputs", 2);
            return new SplitterNode(nodeDef.id(), numOutputs);
        } else if ("mixer".equals(type)) {
            int numInputs = getConfigInt(nodeDef.config(), "numInputs", 2);
            MixerNode mixer = new MixerNode(nodeDef.id(), numInputs);

            // Apply levels and pans
            @SuppressWarnings("unchecked")
            List<Object> levels = (List<Object>) nodeDef.config().get("levels");
            @SuppressWarnings("unchecked")
            List<Object> pans = (List<Object>) nodeDef.config().get("pans");

            if (levels != null) {
                for (int i = 0; i < Math.min(levels.size(), numInputs); i++) {
                    mixer.setLevel(i, ((Number) levels.get(i)).floatValue());
                }
            }
            if (pans != null) {
                for (int i = 0; i < Math.min(pans.size(), numInputs); i++) {
                    mixer.setPan(i, ((Number) pans.get(i)).floatValue());
                }
            }
            Object masterLevel = nodeDef.config().get("masterLevel");
            if (masterLevel instanceof Number n) {
                mixer.setMasterLevel(n.floatValue());
            }

            // Apply stereo mode
            Object stereoModeObj = nodeDef.config().get("stereoMode");
            if (stereoModeObj instanceof String stereoModeStr) {
                try {
                    // Handle legacy AUTO mode by converting to STEREO
                    if ("AUTO".equals(stereoModeStr)) {
                        mixer.setStereoMode(MixerNode.StereoMode.STEREO);
                    } else {
                        mixer.setStereoMode(MixerNode.StereoMode.valueOf(stereoModeStr));
                    }
                } catch (IllegalArgumentException e) {
                    // Keep default STEREO
                }
            }

            return mixer;
        } else {
            // Effect node
            AudioEffect effect = effectFactory.create(type);
            if (effect == null) {
                System.err.println("Warning: Unknown effect type: " + type);
                return null;
            }

            // Apply parameters
            for (Map.Entry<String, Object> entry : nodeDef.parameters().entrySet()) {
                Parameter param = effect.getParameter(entry.getKey());
                if (param != null && entry.getValue() instanceof Number n) {
                    param.setValue(n.floatValue());
                }
            }

            // Special handling for LooperEffect - apply file path and autoplay
            if (effect instanceof LooperEffect looper) {
                Object wavFilePath = nodeDef.config().get("wavFilePath");
                Object autoPlay = nodeDef.config().get("autoPlay");
                if (wavFilePath instanceof String path && !path.isEmpty()) {
                    boolean auto = autoPlay instanceof Boolean b ? b : false;
                    looper.setWavFile(path, auto);
                }
            }

            EffectNode effectNode = new EffectNode(nodeDef.id(), effect);
            effectNode.setBypassed(nodeDef.bypassed());
            return effectNode;
        }
    }

    private Port findPort(List<Port> ports, String portId) {
        for (Port port : ports) {
            if (port.getId().equals(portId)) {
                return port;
            }
        }
        return null;
    }

    private Port getDefaultOutputPort(ProcessingNode node) {
        if (node instanceof InputNode in) return in.getOutput();
        if (node instanceof EffectNode en) return en.getOutput();
        if (node instanceof SplitterNode sn) return sn.getOutput(0);
        if (node instanceof MixerNode mn) return mn.getOutput();
        if (node instanceof GainNode gn) return gn.getOutput();
        List<Port> outputs = node.getOutputPorts();
        return outputs.isEmpty() ? null : outputs.get(0);
    }

    private Port getDefaultInputPort(ProcessingNode node) {
        if (node instanceof OutputNode out) return out.getInput();
        if (node instanceof EffectNode en) return en.getInput();
        if (node instanceof SplitterNode sn) return sn.getInput();
        if (node instanceof MixerNode mn) return mn.getInput(0);
        if (node instanceof GainNode gn) return gn.getInput();
        List<Port> inputs = node.getInputPorts();
        return inputs.isEmpty() ? null : inputs.get(0);
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number n) return n.intValue();
        return defaultValue;
    }

    // ==================== LISTING ====================

    /**
     * List all available templates.
     */
    public List<String> listTemplates() {
        List<String> templates = new ArrayList<>();
        listTemplatesInDir(templatesDir, "", templates);
        return templates;
    }

    /**
     * List factory templates.
     */
    public List<String> listFactoryTemplates() {
        List<String> templates = new ArrayList<>();
        listTemplatesInDir(templatesDir.resolve(FACTORY_SUBDIR), FACTORY_SUBDIR + "/", templates);
        return templates;
    }

    private void listTemplatesInDir(Path dir, String prefix, List<String> templates) {
        if (!Files.isDirectory(dir)) return;

        try (var stream = Files.list(dir)) {
            stream.forEach(path -> {
                String filename = path.getFileName().toString();
                if (filename.endsWith(TEMPLATE_EXTENSION)) {
                    templates.add(prefix + filename.substring(0, filename.length() - TEMPLATE_EXTENSION.length()));
                }
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    // ==================== UTILITY ====================

    private Path resolveTemplatePath(String filename) {
        String name = ensureExtension(filename);
        Path path = templatesDir.resolve(name);
        if (Files.exists(path)) return path;

        // Try factory directory
        path = templatesDir.resolve(FACTORY_SUBDIR).resolve(name);
        if (Files.exists(path)) return path;

        // Return original path (will fail on load)
        return templatesDir.resolve(name);
    }

    private String ensureExtension(String filename) {
        if (!filename.endsWith(TEMPLATE_EXTENSION)) {
            return filename + TEMPLATE_EXTENSION;
        }
        return filename;
    }

    /**
     * Get the templates directory.
     */
    public Path getTemplatesDir() {
        return templatesDir;
    }
}
