package it.denzosoft.jfx2.nam.json;

import java.util.*;

/**
 * Represents a JSON value (object, array, string, number, boolean, or null).
 * Minimal implementation for parsing NAM model files.
 */
public class JsonValue {

    public enum Type {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    private final Type type;
    private final Object value;

    private JsonValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    // Factory methods
    public static JsonValue ofObject(Map<String, JsonValue> map) {
        return new JsonValue(Type.OBJECT, new LinkedHashMap<>(map));
    }

    public static JsonValue ofArray(List<JsonValue> list) {
        return new JsonValue(Type.ARRAY, new ArrayList<>(list));
    }

    public static JsonValue ofString(String s) {
        return new JsonValue(Type.STRING, s);
    }

    public static JsonValue ofNumber(double d) {
        return new JsonValue(Type.NUMBER, d);
    }

    public static JsonValue ofBoolean(boolean b) {
        return new JsonValue(Type.BOOLEAN, b);
    }

    public static JsonValue ofNull() {
        return new JsonValue(Type.NULL, null);
    }

    // Type checks
    public Type getType() { return type; }
    public boolean isObject() { return type == Type.OBJECT; }
    public boolean isArray() { return type == Type.ARRAY; }
    public boolean isString() { return type == Type.STRING; }
    public boolean isNumber() { return type == Type.NUMBER; }
    public boolean isBoolean() { return type == Type.BOOLEAN; }
    public boolean isNull() { return type == Type.NULL; }

    // Value getters
    @SuppressWarnings("unchecked")
    public Map<String, JsonValue> asObject() {
        if (type != Type.OBJECT) throw new IllegalStateException("Not an object");
        return (Map<String, JsonValue>) value;
    }

    @SuppressWarnings("unchecked")
    public List<JsonValue> asArray() {
        if (type != Type.ARRAY) throw new IllegalStateException("Not an array");
        return (List<JsonValue>) value;
    }

    public String asString() {
        if (type != Type.STRING) throw new IllegalStateException("Not a string");
        return (String) value;
    }

    public double asNumber() {
        if (type != Type.NUMBER) throw new IllegalStateException("Not a number");
        return (Double) value;
    }

    public int asInt() {
        return (int) asNumber();
    }

    public float asFloat() {
        return (float) asNumber();
    }

    public boolean asBoolean() {
        if (type != Type.BOOLEAN) throw new IllegalStateException("Not a boolean");
        return (Boolean) value;
    }

    // Convenience accessors for objects
    public JsonValue get(String key) {
        return asObject().get(key);
    }

    public boolean has(String key) {
        return isObject() && asObject().containsKey(key);
    }

    public String getString(String key) {
        JsonValue v = get(key);
        return v != null ? v.asString() : null;
    }

    public String getString(String key, String defaultValue) {
        JsonValue v = get(key);
        return v != null && v.isString() ? v.asString() : defaultValue;
    }

    public int getInt(String key) {
        return get(key).asInt();
    }

    public int getInt(String key, int defaultValue) {
        JsonValue v = get(key);
        return v != null && v.isNumber() ? v.asInt() : defaultValue;
    }

    public float getFloat(String key, float defaultValue) {
        JsonValue v = get(key);
        return v != null && v.isNumber() ? v.asFloat() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonValue v = get(key);
        return v != null && v.isBoolean() ? v.asBoolean() : defaultValue;
    }

    public List<JsonValue> getArray(String key) {
        JsonValue v = get(key);
        return v != null ? v.asArray() : null;
    }

    public JsonValue getObject(String key) {
        return get(key);
    }

    // Convert array of numbers to float array
    public float[] asFloatArray() {
        List<JsonValue> arr = asArray();
        float[] result = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).asFloat();
        }
        return result;
    }

    // Convert array of numbers to int array
    public int[] asIntArray() {
        List<JsonValue> arr = asArray();
        int[] result = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).asInt();
        }
        return result;
    }

    @Override
    public String toString() {
        return switch (type) {
            case OBJECT -> asObject().toString();
            case ARRAY -> asArray().toString();
            case STRING -> "\"" + value + "\"";
            case NUMBER, BOOLEAN -> String.valueOf(value);
            case NULL -> "null";
        };
    }
}
