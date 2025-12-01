package it.denzosoft.jfx2.dsp;

/**
 * Types of biquad filters.
 */
public enum FilterType {
    LOWPASS,
    HIGHPASS,
    BANDPASS,
    NOTCH,
    PEAK,      // Parametric EQ band
    LOWSHELF,
    HIGHSHELF,
    ALLPASS
}
