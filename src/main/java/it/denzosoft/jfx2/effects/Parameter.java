package it.denzosoft.jfx2.effects;

/**
 * Effect parameter with thread-safe smoothing.
 *
 * <p>Parameters use exponential smoothing to avoid clicks when values change.
 * The smoothing time is 20ms by default.</p>
 */
public class Parameter {

    private final String id;
    private final String name;
    private final String description;  // Tooltip description
    private final ParameterType type;
    private final float minValue;
    private final float maxValue;
    private final float defaultValue;
    private final String unit;
    private final String[] choices;  // For CHOICE type

    // Current and target values
    private volatile float targetValue;
    private float currentValue;

    // Smoothing
    private float smoothingCoeff;
    private static final float DEFAULT_SMOOTHING_MS = 20.0f;

    /**
     * Create a float parameter without description.
     */
    public Parameter(String id, String name, float minValue, float maxValue, float defaultValue, String unit) {
        this(id, name, null, minValue, maxValue, defaultValue, unit);
    }

    /**
     * Create a float parameter with description.
     */
    public Parameter(String id, String name, String description, float minValue, float maxValue, float defaultValue, String unit) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = ParameterType.FLOAT;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.choices = null;
        this.targetValue = defaultValue;
        this.currentValue = defaultValue;
        this.smoothingCoeff = 0.0f;  // Will be set in prepare()
    }

    /**
     * Create a boolean parameter without description.
     */
    public Parameter(String id, String name, boolean defaultValue) {
        this(id, name, null, defaultValue);
    }

    /**
     * Create a boolean parameter with description.
     */
    public Parameter(String id, String name, String description, boolean defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = ParameterType.BOOLEAN;
        this.minValue = 0.0f;
        this.maxValue = 1.0f;
        this.defaultValue = defaultValue ? 1.0f : 0.0f;
        this.unit = "";
        this.choices = null;
        this.targetValue = this.defaultValue;
        this.currentValue = this.defaultValue;
        this.smoothingCoeff = 0.0f;
    }

    /**
     * Create a choice parameter without description.
     */
    public Parameter(String id, String name, String[] choices, int defaultIndex) {
        this(id, name, null, choices, defaultIndex);
    }

    /**
     * Create a choice parameter with description.
     */
    public Parameter(String id, String name, String description, String[] choices, int defaultIndex) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = ParameterType.CHOICE;
        this.minValue = 0;
        this.maxValue = choices.length - 1;
        this.defaultValue = defaultIndex;
        this.unit = "";
        this.choices = choices.clone();
        this.targetValue = defaultIndex;
        this.currentValue = defaultIndex;
        this.smoothingCoeff = 0.0f;
    }

    /**
     * Create an integer parameter without description.
     */
    public Parameter(String id, String name, int minValue, int maxValue, int defaultValue, String unit) {
        this(id, name, null, minValue, maxValue, defaultValue, unit);
    }

    /**
     * Create an integer parameter with description.
     */
    public Parameter(String id, String name, String description, int minValue, int maxValue, int defaultValue, String unit) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = ParameterType.INTEGER;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.choices = null;
        this.targetValue = defaultValue;
        this.currentValue = defaultValue;
        this.smoothingCoeff = 0.0f;
    }

    /**
     * Prepare parameter for given sample rate.
     * Calculates smoothing coefficient based on 20ms smoothing time.
     */
    public void prepare(int sampleRate) {
        // Exponential smoothing: coeff = 1 - exp(-1 / (tau * sampleRate))
        // where tau = smoothingTime / 1000
        float tau = DEFAULT_SMOOTHING_MS / 1000.0f;
        this.smoothingCoeff = 1.0f - (float) Math.exp(-1.0 / (tau * sampleRate));
    }

    /**
     * Set the target value (thread-safe).
     * The actual value will smoothly transition to this target.
     */
    public void setValue(float value) {
        this.targetValue = clamp(value);
    }

    /**
     * Set boolean value.
     */
    public void setValue(boolean value) {
        this.targetValue = value ? 1.0f : 0.0f;
    }

    /**
     * Set choice by index.
     */
    public void setChoice(int index) {
        this.targetValue = clamp(index);
    }

    /**
     * Get the current smoothed value.
     * Call this from the audio thread during processing.
     */
    public float getValue() {
        return currentValue;
    }

    /**
     * Get the target value (what the user set).
     */
    public float getTargetValue() {
        return targetValue;
    }

    /**
     * Get boolean value.
     * Returns target value directly (no smoothing) since boolean should change instantly.
     */
    public boolean getBooleanValue() {
        return targetValue >= 0.5f;
    }

    /**
     * Get choice index.
     * Returns target value directly (no smoothing) since discrete choices should change instantly.
     */
    public int getChoiceIndex() {
        return Math.round(targetValue);
    }

    /**
     * Get integer value.
     */
    public int getIntValue() {
        return Math.round(currentValue);
    }

    /**
     * Update smoothed value.
     * Call once per sample or once per buffer from audio thread.
     */
    public void smooth() {
        if (smoothingCoeff > 0) {
            currentValue += smoothingCoeff * (targetValue - currentValue);
        } else {
            currentValue = targetValue;
        }
    }

    /**
     * Update smoothed value for a whole buffer.
     * More efficient than calling smooth() per sample.
     */
    public void smoothBuffer(int frameCount) {
        if (smoothingCoeff > 0) {
            // Apply smoothing multiple times (simplified)
            float coeff = 1.0f - (float) Math.pow(1.0 - smoothingCoeff, frameCount);
            currentValue += coeff * (targetValue - currentValue);
        } else {
            currentValue = targetValue;
        }
    }

    /**
     * Immediately set value without smoothing.
     */
    public void setImmediate(float value) {
        this.targetValue = clamp(value);
        this.currentValue = this.targetValue;
    }

    /**
     * Reset to default value.
     */
    public void reset() {
        this.targetValue = defaultValue;
        this.currentValue = defaultValue;
    }

    private float clamp(float value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ParameterType getType() {
        return type;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public String getUnit() {
        return unit;
    }

    public String[] getChoices() {
        return choices != null ? choices.clone() : null;
    }

    /**
     * Get normalized value (0.0 to 1.0).
     */
    public float getNormalizedValue() {
        if (maxValue == minValue) return 0.0f;
        return (currentValue - minValue) / (maxValue - minValue);
    }

    /**
     * Set value from normalized (0.0 to 1.0).
     */
    public void setNormalizedValue(float normalized) {
        setValue(minValue + normalized * (maxValue - minValue));
    }

    @Override
    public String toString() {
        return String.format("Parameter[%s: %.2f %s (%.2f-%.2f)]", name, currentValue, unit, minValue, maxValue);
    }
}
