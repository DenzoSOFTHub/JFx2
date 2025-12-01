package it.denzosoft.jfx2.graph;

/**
 * Type of processing node.
 */
public enum NodeType {

    /**
     * Audio input node - receives signal from audio interface.
     */
    INPUT,

    /**
     * Audio output node - sends signal to audio interface.
     */
    OUTPUT,

    /**
     * Effect node - processes audio (distortion, delay, etc.).
     */
    EFFECT,

    /**
     * Splitter node - splits signal into multiple paths.
     */
    SPLITTER,

    /**
     * Mixer node - mixes multiple signals into one.
     */
    MIXER
}
