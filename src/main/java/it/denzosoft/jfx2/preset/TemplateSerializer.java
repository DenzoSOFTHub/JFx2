package it.denzosoft.jfx2.preset;

import java.util.*;

/**
 * Serializes and deserializes Rig templates to/from JSON.
 */
public class TemplateSerializer {

    private final SimpleJson json = new SimpleJson();

    /**
     * Serialize a Rig to JSON string.
     */
    public String serialize(Rig rig) {
        Map<String, Object> root = new LinkedHashMap<>();

        // Metadata
        root.put("metadata", serializeMetadata(rig.getMetadata()));

        // Nodes
        List<Object> nodesList = new ArrayList<>();
        for (Rig.NodeDefinition node : rig.getNodes()) {
            nodesList.add(serializeNode(node));
        }
        root.put("nodes", nodesList);

        // Connections
        List<Object> connsList = new ArrayList<>();
        for (Rig.ConnectionDefinition conn : rig.getConnections()) {
            connsList.add(serializeConnection(conn));
        }
        root.put("connections", connsList);

        return json.stringifyPretty(root);
    }

    private Map<String, Object> serializeMetadata(RigMetadata meta) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", meta.name());
        map.put("author", meta.author());
        map.put("description", meta.description());
        map.put("category", meta.category());
        map.put("tags", meta.tags());
        map.put("version", meta.version());
        map.put("createdAt", meta.createdAt());
        map.put("modifiedAt", meta.modifiedAt());
        return map;
    }

    private Map<String, Object> serializeNode(Rig.NodeDefinition node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.id());
        map.put("type", node.type());
        map.put("name", node.name());
        map.put("bypassed", node.bypassed());
        map.put("x", node.x());
        map.put("y", node.y());

        if (!node.parameters().isEmpty()) {
            map.put("parameters", new LinkedHashMap<>(node.parameters()));
        }

        if (!node.config().isEmpty()) {
            map.put("config", new LinkedHashMap<>(node.config()));
        }

        return map;
    }

    private Map<String, Object> serializeConnection(Rig.ConnectionDefinition conn) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sourceNode", conn.sourceNodeId());
        map.put("sourcePort", conn.sourcePortId());
        map.put("targetNode", conn.targetNodeId());
        map.put("targetPort", conn.targetPortId());
        return map;
    }

    /**
     * Deserialize a Rig from JSON string.
     */
    public Rig deserialize(String jsonString) {
        Map<String, Object> root = json.parseObject(jsonString);

        // Metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap = (Map<String, Object>) root.get("metadata");
        RigMetadata metadata = deserializeMetadata(metaMap);

        Rig rig = new Rig(metadata);

        // Nodes
        @SuppressWarnings("unchecked")
        List<Object> nodesList = (List<Object>) root.get("nodes");
        if (nodesList != null) {
            for (Object nodeObj : nodesList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nodeMap = (Map<String, Object>) nodeObj;
                rig.addNode(deserializeNode(nodeMap));
            }
        }

        // Connections
        @SuppressWarnings("unchecked")
        List<Object> connsList = (List<Object>) root.get("connections");
        if (connsList != null) {
            for (Object connObj : connsList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> connMap = (Map<String, Object>) connObj;
                rig.addConnection(deserializeConnection(connMap));
            }
        }

        return rig;
    }

    private RigMetadata deserializeMetadata(Map<String, Object> map) {
        return new RigMetadata(
                getString(map, "name", "Untitled"),
                getString(map, "author", "Unknown"),
                getString(map, "description", ""),
                getString(map, "category", "Custom"),
                getString(map, "tags", ""),
                getString(map, "version", RigMetadata.CURRENT_VERSION),
                getString(map, "createdAt", ""),
                getString(map, "modifiedAt", "")
        );
    }

    private Rig.NodeDefinition deserializeNode(Map<String, Object> map) {
        String id = getString(map, "id", "node");
        String type = getString(map, "type", "gain");
        String name = getString(map, "name", type);
        boolean bypassed = getBoolean(map, "bypassed", false);
        int x = getInt(map, "x", 0);
        int y = getInt(map, "y", 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) map.getOrDefault("parameters", new HashMap<>());

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) map.getOrDefault("config", new HashMap<>());

        return new Rig.NodeDefinition(id, type, name, bypassed, x, y, parameters, config);
    }

    private Rig.ConnectionDefinition deserializeConnection(Map<String, Object> map) {
        return new Rig.ConnectionDefinition(
                getString(map, "sourceNode", ""),
                getString(map, "sourcePort", "out"),
                getString(map, "targetNode", ""),
                getString(map, "targetPort", "in")
        );
    }

    // Utility methods

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number n) return n.floatValue();
        if (value instanceof String s) return Float.parseFloat(s);
        return defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) return Integer.parseInt(s);
        return defaultValue;
    }
}
