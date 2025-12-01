package it.denzosoft.jfx2.effects;

/**
 * Types of effect parameters.
 */
public enum ParameterType {
    /**
     * Continuous value (e.g., gain, frequency).
     */
    FLOAT,

    /**
     * Integer value (e.g., filter order).
     */
    INTEGER,

    /**
     * Boolean on/off value.
     */
    BOOLEAN,

    /**
     * Choice from enumerated options.
     */
    CHOICE
}
