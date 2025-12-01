package it.denzosoft.jfx2.preset;

import java.util.*;

/**
 * Represents a preset for an effect.
 *
 * <p>A preset stores the parameter values for a specific effect type,
 * allowing users to save and recall favorite settings.</p>
 */
public class Preset {

    private final String name;
    private final String effectId;
    private final Map<String, Float> parameterValues;
    private String description;
    private String author;
    private long createdTime;
    private long modifiedTime;

    /**
     * Create a new preset.
     *
     * @param name Preset name
     * @param effectId Effect type identifier (e.g., "tubepreamp")
     */
    public Preset(String name, String effectId) {
        this.name = name;
        this.effectId = effectId;
        this.parameterValues = new LinkedHashMap<>();
        this.createdTime = System.currentTimeMillis();
        this.modifiedTime = this.createdTime;
    }

    /**
     * Create a preset with existing parameter values.
     */
    public Preset(String name, String effectId, Map<String, Float> values) {
        this(name, effectId);
        this.parameterValues.putAll(values);
    }

    /**
     * Get the preset name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the effect type this preset is for.
     */
    public String getEffectId() {
        return effectId;
    }

    /**
     * Get all parameter values.
     */
    public Map<String, Float> getParameterValues() {
        return Collections.unmodifiableMap(parameterValues);
    }

    /**
     * Get a specific parameter value.
     */
    public Float getParameterValue(String parameterId) {
        return parameterValues.get(parameterId);
    }

    /**
     * Set a parameter value.
     */
    public void setParameterValue(String parameterId, float value) {
        parameterValues.put(parameterId, value);
        modifiedTime = System.currentTimeMillis();
    }

    /**
     * Set all parameter values from a map.
     */
    public void setParameterValues(Map<String, Float> values) {
        parameterValues.clear();
        parameterValues.putAll(values);
        modifiedTime = System.currentTimeMillis();
    }

    /**
     * Get the preset description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the preset description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the preset author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set the preset author.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Get creation timestamp.
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Set creation timestamp (for loading).
     */
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * Get last modified timestamp.
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    /**
     * Set modified timestamp (for loading).
     */
    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preset that = (Preset) o;
        return Objects.equals(name, that.name) && Objects.equals(effectId, that.effectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, effectId);
    }
}
