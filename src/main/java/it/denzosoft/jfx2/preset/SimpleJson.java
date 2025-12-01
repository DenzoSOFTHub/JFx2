package it.denzosoft.jfx2.preset;

import java.util.*;

/**
 * Simple JSON parser and writer.
 *
 * <p>Minimal implementation for preset serialization without external libraries.
 * Supports: objects, arrays, strings, numbers, booleans, null.</p>
 */
public class SimpleJson {

    // ==================== PARSING ====================

    private String json;
    private int pos;

    /**
     * Parse a JSON string into a Map or List.
     */
    public Object parse(String json) {
        this.json = json.trim();
        this.pos = 0;
        return parseValue();
    }

    /**
     * Parse JSON string to Map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseObject(String json) {
        Object result = parse(json);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        throw new IllegalArgumentException("JSON is not an object");
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) return null;

        char c = json.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;  // skip '{'
        skipWhitespace();

        if (json.charAt(pos) == '}') {
            pos++;
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();

            if (json.charAt(pos) == '}') {
                pos++;
                break;
            }
            expect(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++;  // skip '['
        skipWhitespace();

        if (json.charAt(pos) == ']') {
            pos++;
            return list;
        }

        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();

            if (json.charAt(pos) == ']') {
                pos++;
                break;
            }
            expect(',');
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            } else if (c == '\\') {
                if (pos < json.length()) {
                    char escaped = json.charAt(pos++);
                    switch (escaped) {
                        case '"', '\\', '/' -> sb.append(escaped);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = json.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                    }
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        if (json.charAt(pos) == '-') pos++;
        while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
        if (pos < json.length() && json.charAt(pos) == '.') {
            pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
        }
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            pos++;
            if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
        }

        String numStr = json.substring(start, pos);
        if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
            return Double.parseDouble(numStr);
        } else {
            long value = Long.parseLong(numStr);
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
            return value;
        }
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return true;
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return false;
        }
        throw new IllegalArgumentException("Expected boolean at position " + pos);
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalArgumentException("Expected null at position " + pos);
    }

    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    private void expect(char c) {
        if (pos >= json.length() || json.charAt(pos) != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    // ==================== WRITING ====================

    /**
     * Convert an object to JSON string.
     */
    public String stringify(Object obj) {
        return stringify(obj, false, 0);
    }

    /**
     * Convert an object to pretty-printed JSON string.
     */
    public String stringifyPretty(Object obj) {
        return stringify(obj, true, 0);
    }

    private String stringify(Object obj, boolean pretty, int indent) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String s) {
            return stringifyString(s);
        } else if (obj instanceof Number n) {
            return stringifyNumber(n);
        } else if (obj instanceof Boolean b) {
            return b.toString();
        } else if (obj instanceof Map<?, ?> map) {
            return stringifyMap(map, pretty, indent);
        } else if (obj instanceof List<?> list) {
            return stringifyList(list, pretty, indent);
        } else {
            return stringifyString(obj.toString());
        }
    }

    private String stringifyString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String stringifyNumber(Number n) {
        if (n instanceof Double d) {
            if (d.isInfinite() || d.isNaN()) return "null";
            // Remove trailing zeros
            String s = d.toString();
            if (s.endsWith(".0")) {
                return s.substring(0, s.length() - 2);
            }
            return s;
        } else if (n instanceof Float f) {
            if (f.isInfinite() || f.isNaN()) return "null";
            return f.toString();
        }
        return n.toString();
    }

    private String stringifyMap(Map<?, ?> map, boolean pretty, int indent) {
        if (map.isEmpty()) return "{}";

        StringBuilder sb = new StringBuilder("{");
        String sep = pretty ? ",\n" : ",";
        String ind = pretty ? "  ".repeat(indent + 1) : "";
        String endInd = pretty ? "  ".repeat(indent) : "";

        if (pretty) sb.append("\n");

        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(sep);
            first = false;

            sb.append(ind);
            sb.append(stringifyString(entry.getKey().toString()));
            sb.append(pretty ? ": " : ":");
            sb.append(stringify(entry.getValue(), pretty, indent + 1));
        }

        if (pretty) sb.append("\n").append(endInd);
        sb.append("}");
        return sb.toString();
    }

    private String stringifyList(List<?> list, boolean pretty, int indent) {
        if (list.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        String sep = pretty ? ",\n" : ",";
        String ind = pretty ? "  ".repeat(indent + 1) : "";
        String endInd = pretty ? "  ".repeat(indent) : "";

        if (pretty) sb.append("\n");

        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(sep);
            first = false;

            sb.append(ind);
            sb.append(stringify(item, pretty, indent + 1));
        }

        if (pretty) sb.append("\n").append(endInd);
        sb.append("]");
        return sb.toString();
    }
}
