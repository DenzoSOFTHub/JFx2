package it.denzosoft.jfx2.nam.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Minimal JSON parser for NAM model files.
 * No external dependencies.
 */
public class JsonParser {

    private final String json;
    private int pos;

    public JsonParser(String json) {
        this.json = json;
        this.pos = 0;
    }

    /**
     * Parse a JSON file.
     */
    public static JsonValue parseFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return parse(content);
    }

    /**
     * Parse a JSON string.
     */
    public static JsonValue parse(String json) {
        return new JsonParser(json).parseValue();
    }

    private JsonValue parseValue() {
        skipWhitespace();

        if (pos >= json.length()) {
            throw new RuntimeException("Unexpected end of JSON");
        }

        char c = json.charAt(pos);

        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            return JsonValue.ofString(parseString());
        } else if (c == 't' || c == 'f') {
            return parseBoolean();
        } else if (c == 'n') {
            return parseNull();
        } else if (c == '-' || Character.isDigit(c)) {
            return JsonValue.ofNumber(parseNumber());
        } else {
            throw new RuntimeException("Unexpected character '" + c + "' at position " + pos);
        }
    }

    private JsonValue parseObject() {
        Map<String, JsonValue> map = new LinkedHashMap<>();

        expect('{');
        skipWhitespace();

        if (peek() == '}') {
            pos++;
            return JsonValue.ofObject(map);
        }

        while (true) {
            skipWhitespace();

            // Parse key
            if (peek() != '"') {
                throw new RuntimeException("Expected string key at position " + pos);
            }
            String key = parseString();

            skipWhitespace();
            expect(':');
            skipWhitespace();

            // Parse value
            JsonValue value = parseValue();
            map.put(key, value);

            skipWhitespace();

            char c = peek();
            if (c == '}') {
                pos++;
                break;
            } else if (c == ',') {
                pos++;
            } else {
                throw new RuntimeException("Expected ',' or '}' at position " + pos);
            }
        }

        return JsonValue.ofObject(map);
    }

    private JsonValue parseArray() {
        List<JsonValue> list = new ArrayList<>();

        expect('[');
        skipWhitespace();

        if (peek() == ']') {
            pos++;
            return JsonValue.ofArray(list);
        }

        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();

            char c = peek();
            if (c == ']') {
                pos++;
                break;
            } else if (c == ',') {
                pos++;
            } else {
                throw new RuntimeException("Expected ',' or ']' at position " + pos);
            }
        }

        return JsonValue.ofArray(list);
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();

        while (pos < json.length()) {
            char c = json.charAt(pos++);

            if (c == '"') {
                return sb.toString();
            } else if (c == '\\') {
                if (pos >= json.length()) {
                    throw new RuntimeException("Unexpected end of string");
                }
                char escaped = json.charAt(pos++);
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > json.length()) {
                            throw new RuntimeException("Invalid unicode escape");
                        }
                        String hex = json.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw new RuntimeException("Invalid escape sequence: \\" + escaped);
                }
            } else {
                sb.append(c);
            }
        }

        throw new RuntimeException("Unterminated string");
    }

    private double parseNumber() {
        int start = pos;

        // Optional minus
        if (peek() == '-') {
            pos++;
        }

        // Integer part
        if (peek() == '0') {
            pos++;
        } else if (Character.isDigit(peek())) {
            while (pos < json.length() && Character.isDigit(peek())) {
                pos++;
            }
        } else {
            throw new RuntimeException("Invalid number at position " + start);
        }

        // Fractional part
        if (pos < json.length() && peek() == '.') {
            pos++;
            if (!Character.isDigit(peek())) {
                throw new RuntimeException("Invalid number at position " + start);
            }
            while (pos < json.length() && Character.isDigit(peek())) {
                pos++;
            }
        }

        // Exponent part
        if (pos < json.length() && (peek() == 'e' || peek() == 'E')) {
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            if (!Character.isDigit(peek())) {
                throw new RuntimeException("Invalid number at position " + start);
            }
            while (pos < json.length() && Character.isDigit(peek())) {
                pos++;
            }
        }

        String numStr = json.substring(start, pos);
        return Double.parseDouble(numStr);
    }

    private JsonValue parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return JsonValue.ofBoolean(true);
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return JsonValue.ofBoolean(false);
        } else {
            throw new RuntimeException("Expected 'true' or 'false' at position " + pos);
        }
    }

    private JsonValue parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return JsonValue.ofNull();
        } else {
            throw new RuntimeException("Expected 'null' at position " + pos);
        }
    }

    private void skipWhitespace() {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        if (pos >= json.length()) {
            return '\0';
        }
        return json.charAt(pos);
    }

    private void expect(char expected) {
        if (pos >= json.length() || json.charAt(pos) != expected) {
            throw new RuntimeException("Expected '" + expected + "' at position " + pos);
        }
        pos++;
    }
}
