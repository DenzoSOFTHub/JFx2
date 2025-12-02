package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * 3D Panoramic Positioning Effect with binaural modeling.
 *
 * <p>Provides realistic spatial audio positioning using Head-Related Transfer
 * Function (HRTF) approximation techniques:</p>
 *
 * <ul>
 *   <li><b>ITD (Interaural Time Difference)</b> - Sound arrives at one ear before
 *       the other based on angle (max ~0.7ms at 90°)</li>
 *   <li><b>ILD (Interaural Level Difference)</b> - Sound is louder in the closer
 *       ear due to head shadow</li>
 *   <li><b>Head Shadow</b> - High frequencies are attenuated when sound passes
 *       around the head</li>
 *   <li><b>Pinna Filtering</b> - Frequency response changes based on elevation
 *       and front/back position</li>
 *   <li><b>Distance Modeling</b> - Amplitude attenuation and air absorption</li>
 *   <li><b>Early Reflections</b> - Room reflections aid localization</li>
 * </ul>
 *
 * <p>The angle parameter uses a 360° field where:</p>
 * <ul>
 *   <li>0° = Front center</li>
 *   <li>90° = Right</li>
 *   <li>180° = Rear center</li>
 *   <li>270° = Left</li>
 * </ul>
 */
public class Pan3DEffect extends AbstractEffect {

    // Parameters
    private Parameter angle;          // 0-360° azimuth
    private Parameter elevation;      // -90° to +90° vertical
    private Parameter distance;       // Distance from listener in meters
    private Parameter width;          // Source width (0 = point, 100 = wide)
    private Parameter roomSize;       // Early reflection room size
    private Parameter hrtfIntensity;  // HRTF modeling intensity
    private Parameter airAbsorption;  // High-frequency loss with distance
    private Parameter dopplerEnable;  // Enable Doppler effect when moving

    // DSP state
    private int currentSampleRate = 44100;

    // Delay lines for ITD (max ~1ms = 44 samples at 44.1kHz)
    private float[] delayLineL;
    private float[] delayLineR;
    private int delayWritePos = 0;
    private static final int MAX_ITD_SAMPLES = 64;

    // Head shadow low-pass filters (one per ear)
    private double lpStateL1 = 0, lpStateL2 = 0;
    private double lpStateR1 = 0, lpStateR2 = 0;

    // Pinna notch filters for elevation cues
    private double notchStateL1 = 0, notchStateL2 = 0;
    private double notchStateR1 = 0, notchStateR2 = 0;

    // High shelf for rear attenuation
    private double shelfStateL1 = 0, shelfStateL2 = 0;
    private double shelfStateR1 = 0, shelfStateR2 = 0;

    // Air absorption low-pass
    private double airLpStateL = 0, airLpStateR = 0;

    // Early reflections delay lines
    private float[] reflectionBuffer;
    private int reflectionWritePos = 0;
    private static final int REFLECTION_BUFFER_SIZE = 4410; // 100ms max

    // Smoothing for parameter changes (prevent clicks)
    private float smoothAngle = 0;
    private float smoothElevation = 0;
    private float smoothDistance = 1;
    private float prevGainL = 1, prevGainR = 1;

    // Doppler effect state
    private float prevAngle = 0;
    private float dopplerShift = 1.0f;

    // Physical constants
    private static final float SPEED_OF_SOUND = 343.0f;  // m/s
    private static final float HEAD_RADIUS = 0.0875f;    // ~8.75cm average head radius
    private static final float EAR_DISTANCE = 0.175f;    // ~17.5cm between ears

    public Pan3DEffect() {
        super(EffectMetadata.of("pan3d", "Pan 3D",
                "Realistic 3D spatial positioning with HRTF modeling", EffectCategory.MODULATION));
        initParameters();
    }

    private void initParameters() {
        angle = addFloatParameter("angle", "Angle",
                "Azimuth position: 0°=Front, 90°=Right, 180°=Rear, 270°=Left",
                0.0f, 360.0f, 0.0f, "°");

        elevation = addFloatParameter("elevation", "Elevation",
                "Vertical angle: -90°=Below, 0°=Level, +90°=Above",
                -90.0f, 90.0f, 0.0f, "°");

        distance = addFloatParameter("distance", "Distance",
                "Distance from listener (affects volume and air absorption)",
                0.1f, 10.0f, 1.0f, "m");

        width = addFloatParameter("width", "Width",
                "Source width: 0%=Point source, 100%=Wide/diffuse source",
                0.0f, 100.0f, 20.0f, "%");

        roomSize = addFloatParameter("room", "Room",
                "Room size for early reflections (helps front/back localization)",
                0.0f, 100.0f, 30.0f, "%");

        hrtfIntensity = addFloatParameter("hrtf", "HRTF",
                "Head-Related Transfer Function intensity",
                0.0f, 100.0f, 80.0f, "%");

        airAbsorption = addFloatParameter("air", "Air",
                "High-frequency absorption over distance",
                0.0f, 100.0f, 50.0f, "%");

        dopplerEnable = addFloatParameter("doppler", "Doppler",
                "Doppler effect intensity when source moves",
                0.0f, 100.0f, 0.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.currentSampleRate = sampleRate;

        // Initialize delay lines
        delayLineL = new float[MAX_ITD_SAMPLES];
        delayLineR = new float[MAX_ITD_SAMPLES];
        delayWritePos = 0;

        // Reflection buffer
        reflectionBuffer = new float[REFLECTION_BUFFER_SIZE];
        reflectionWritePos = 0;

        // Reset filter states
        resetFilters();

        // Initialize smoothing
        smoothAngle = angle.getValue();
        smoothElevation = elevation.getValue();
        smoothDistance = distance.getValue();
        prevAngle = smoothAngle;
        prevGainL = prevGainR = 1.0f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono input - process as centered and output to both channels
        // This method shouldn't be called for stereo output, but handle gracefully
        System.arraycopy(input, 0, output, 0, frameCount);
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                    float[] outputL, float[] outputR, int frameCount) {
        // Get parameters
        float targetAngle = angle.getValue();
        float targetElevation = elevation.getValue();
        float targetDistance = distance.getValue();
        float widthAmt = width.getValue() / 100.0f;
        float roomAmt = roomSize.getValue() / 100.0f;
        float hrtfAmt = hrtfIntensity.getValue() / 100.0f;
        float airAmt = airAbsorption.getValue() / 100.0f;
        float dopplerAmt = dopplerEnable.getValue() / 100.0f;

        // Smoothing coefficient for parameter changes
        float smoothCoef = 0.001f;

        for (int i = 0; i < frameCount; i++) {
            // Smooth parameter changes to prevent clicks
            smoothAngle += (targetAngle - smoothAngle) * smoothCoef;
            smoothElevation += (targetElevation - smoothElevation) * smoothCoef;
            smoothDistance += (targetDistance - smoothDistance) * smoothCoef;

            // Convert angle to radians
            float angleRad = (float) Math.toRadians(smoothAngle);
            float elevRad = (float) Math.toRadians(smoothElevation);

            // Mix input to mono for processing (or use mid signal)
            float monoIn = (inputL[i] + inputR[i]) * 0.5f;
            float stereoWidth = (inputL[i] - inputR[i]) * 0.5f * widthAmt;

            // Store in reflection buffer
            reflectionBuffer[reflectionWritePos] = monoIn;
            reflectionWritePos = (reflectionWritePos + 1) % REFLECTION_BUFFER_SIZE;

            // === Distance Attenuation ===
            // Inverse distance law with minimum distance of 0.1m
            float distanceGain = 1.0f / Math.max(0.1f, smoothDistance);
            distanceGain = Math.min(1.0f, distanceGain); // Don't amplify close sources

            // === Calculate left/right gains based on angle ===
            // Using sine law for basic panning
            float sinAngle = (float) Math.sin(angleRad);
            float cosAngle = (float) Math.cos(angleRad);

            // Basic pan law (constant power)
            float panL = (float) Math.sqrt(0.5f * (1.0f - sinAngle));
            float panR = (float) Math.sqrt(0.5f * (1.0f + sinAngle));

            // === ILD (Interaural Level Difference) ===
            // Head shadow causes level difference, stronger at high frequencies
            // Simplified: more attenuation on the far ear
            float ildL = 1.0f;
            float ildR = 1.0f;
            if (hrtfAmt > 0) {
                // Angle 90° = full shadow on left ear, 270° = full shadow on right
                float shadowL = Math.max(0, sinAngle);   // Shadow on left when angle > 0
                float shadowR = Math.max(0, -sinAngle);  // Shadow on right when angle < 0
                ildL = 1.0f - shadowL * 0.3f * hrtfAmt;
                ildR = 1.0f - shadowR * 0.3f * hrtfAmt;
            }

            // === ITD (Interaural Time Difference) ===
            // Sound takes longer to reach the far ear
            // Max ITD at 90° is about 0.66ms (head radius / speed of sound)
            float itdSeconds = HEAD_RADIUS * sinAngle / SPEED_OF_SOUND;
            float itdSamples = itdSeconds * currentSampleRate * hrtfAmt;

            // Calculate delay for each ear
            float delayL = Math.max(0, -itdSamples);  // Delay left when angle < 0 (left side)
            float delayR = Math.max(0, itdSamples);   // Delay right when angle > 0 (right side)

            // Store in delay lines
            delayLineL[delayWritePos] = monoIn;
            delayLineR[delayWritePos] = monoIn;

            // Read from delay lines with interpolation
            float readPosL = delayWritePos - delayL;
            float readPosR = delayWritePos - delayR;
            while (readPosL < 0) readPosL += MAX_ITD_SAMPLES;
            while (readPosR < 0) readPosR += MAX_ITD_SAMPLES;

            int idxL1 = (int) readPosL % MAX_ITD_SAMPLES;
            int idxL2 = (idxL1 + 1) % MAX_ITD_SAMPLES;
            float fracL = readPosL - (int) readPosL;

            int idxR1 = (int) readPosR % MAX_ITD_SAMPLES;
            int idxR2 = (idxR1 + 1) % MAX_ITD_SAMPLES;
            float fracR = readPosR - (int) readPosR;

            float delayedL = delayLineL[idxL1] + fracL * (delayLineL[idxL2] - delayLineL[idxL1]);
            float delayedR = delayLineR[idxR1] + fracR * (delayLineR[idxR2] - delayLineR[idxR1]);

            delayWritePos = (delayWritePos + 1) % MAX_ITD_SAMPLES;

            // === Head Shadow Filter ===
            // Low-pass filter on the shadowed ear (high frequencies blocked by head)
            if (hrtfAmt > 0) {
                // Calculate shadow cutoff frequency (lower = more shadow)
                // Range from 2kHz (full shadow) to 20kHz (no shadow)
                float shadowAmtL = Math.max(0, sinAngle) * hrtfAmt;
                float shadowAmtR = Math.max(0, -sinAngle) * hrtfAmt;

                float cutoffL = 20000.0f - shadowAmtL * 18000.0f;
                float cutoffR = 20000.0f - shadowAmtR * 18000.0f;

                delayedL = applyLowPass(delayedL, cutoffL, true);
                delayedR = applyLowPass(delayedR, cutoffR, false);
            }

            // === Pinna (Ear) Filtering for Elevation ===
            // High frequencies are notched at certain frequencies based on elevation
            if (hrtfAmt > 0 && Math.abs(smoothElevation) > 5.0f) {
                // Notch frequency shifts with elevation (6-10kHz range)
                float notchFreq = 8000.0f + smoothElevation * 30.0f;
                delayedL = applyNotch(delayedL, notchFreq, 0.3f * hrtfAmt, true);
                delayedR = applyNotch(delayedR, notchFreq, 0.3f * hrtfAmt, false);
            }

            // === Front/Back Disambiguation ===
            // Rear sources have high-frequency roll-off due to pinna shape
            float rearFactor = -cosAngle;  // 1 at rear (180°), -1 at front (0°)
            if (rearFactor > 0 && hrtfAmt > 0) {
                // Apply high-shelf cut for rear sources
                float shelfCut = rearFactor * 6.0f * hrtfAmt;  // Up to -6dB at rear
                delayedL = applyHighShelf(delayedL, 3000.0f, -shelfCut, true);
                delayedR = applyHighShelf(delayedR, 3000.0f, -shelfCut, false);
            }

            // === Air Absorption ===
            // High frequencies are absorbed over distance
            if (airAmt > 0 && smoothDistance > 0.5f) {
                float airCutoff = 20000.0f - (smoothDistance - 0.5f) * 1500.0f * airAmt;
                airCutoff = Math.max(2000.0f, airCutoff);
                delayedL = applyAirAbsorption(delayedL, airCutoff, true);
                delayedR = applyAirAbsorption(delayedR, airCutoff, false);
            }

            // === Early Reflections ===
            // Help with front/back and distance perception
            float reflections = 0;
            if (roomAmt > 0) {
                // Multiple reflections at different delays
                int delay1 = (int) (0.01f * currentSampleRate * (1 + roomAmt));   // 10-20ms
                int delay2 = (int) (0.025f * currentSampleRate * (1 + roomAmt));  // 25-50ms
                int delay3 = (int) (0.04f * currentSampleRate * (1 + roomAmt));   // 40-80ms

                int idx1 = (reflectionWritePos - delay1 + REFLECTION_BUFFER_SIZE) % REFLECTION_BUFFER_SIZE;
                int idx2 = (reflectionWritePos - delay2 + REFLECTION_BUFFER_SIZE) % REFLECTION_BUFFER_SIZE;
                int idx3 = (reflectionWritePos - delay3 + REFLECTION_BUFFER_SIZE) % REFLECTION_BUFFER_SIZE;

                // Reflections are quieter and have different pan positions
                float ref1 = reflectionBuffer[idx1] * 0.3f * roomAmt;
                float ref2 = reflectionBuffer[idx2] * 0.2f * roomAmt;
                float ref3 = reflectionBuffer[idx3] * 0.15f * roomAmt;

                reflections = (ref1 + ref2 + ref3) * distanceGain;
            }

            // === Combine all processing ===
            float outL = delayedL * panL * ildL * distanceGain;
            float outR = delayedR * panR * ildR * distanceGain;

            // Add stereo width component (opposite panning)
            outL += stereoWidth * panR * distanceGain * 0.5f;
            outR -= stereoWidth * panL * distanceGain * 0.5f;

            // Add early reflections (spread in stereo)
            outL += reflections * 0.7f;
            outR += reflections * 0.7f;

            // === Doppler Effect ===
            if (dopplerAmt > 0) {
                // Calculate angular velocity
                float angleDelta = targetAngle - prevAngle;
                // Handle wraparound
                if (angleDelta > 180) angleDelta -= 360;
                if (angleDelta < -180) angleDelta += 360;

                // Smooth doppler shift
                float targetDoppler = 1.0f + angleDelta * 0.0001f * dopplerAmt;
                dopplerShift += (targetDoppler - dopplerShift) * 0.01f;

                // Apply pitch shift approximation (simple for now)
                // A proper implementation would use a pitch shifter
                outL *= dopplerShift;
                outR *= dopplerShift;
            }
            prevAngle = targetAngle;

            // Smooth gain changes to prevent clicks
            float gainL = outL / (monoIn + 0.0001f);
            float gainR = outR / (monoIn + 0.0001f);
            gainL = Math.max(-2, Math.min(2, gainL));
            gainR = Math.max(-2, Math.min(2, gainR));

            outputL[i] = outL;
            outputR[i] = outR;

            prevGainL = gainL;
            prevGainR = gainR;
        }
    }

    /**
     * Simple one-pole low-pass filter for head shadow.
     */
    private float applyLowPass(float input, float cutoff, boolean isLeft) {
        double w0 = 2.0 * Math.PI * cutoff / currentSampleRate;
        double alpha = Math.sin(w0) / 2.0;

        // Simplified one-pole approximation
        double coef = Math.exp(-2.0 * Math.PI * cutoff / currentSampleRate);

        if (isLeft) {
            lpStateL1 = input + (lpStateL1 - input) * coef;
            return (float) lpStateL1;
        } else {
            lpStateR1 = input + (lpStateR1 - input) * coef;
            return (float) lpStateR1;
        }
    }

    /**
     * Notch filter for pinna elevation cues.
     */
    private float applyNotch(float input, float freq, float depth, boolean isLeft) {
        // Simple notch using peak filter with negative gain
        double w0 = 2.0 * Math.PI * freq / currentSampleRate;
        double Q = 2.0;
        double alpha = Math.sin(w0) / (2.0 * Q);

        double b0 = 1 - alpha * depth;
        double b1 = -2 * Math.cos(w0);
        double b2 = 1 + alpha * depth;
        double a0 = 1 + alpha;
        double a1 = -2 * Math.cos(w0);
        double a2 = 1 - alpha;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        double output;
        if (isLeft) {
            output = b0 * input + notchStateL1;
            notchStateL1 = b1 * input - a1 * output + notchStateL2;
            notchStateL2 = b2 * input - a2 * output;
        } else {
            output = b0 * input + notchStateR1;
            notchStateR1 = b1 * input - a1 * output + notchStateR2;
            notchStateR2 = b2 * input - a2 * output;
        }

        return (float) output;
    }

    /**
     * High shelf filter for rear source attenuation.
     */
    private float applyHighShelf(float input, float freq, float gainDb, boolean isLeft) {
        double A = Math.pow(10, gainDb / 40.0);
        double w0 = 2.0 * Math.PI * freq / currentSampleRate;
        double alpha = Math.sin(w0) / 2.0 * Math.sqrt(2.0);

        double cosW0 = Math.cos(w0);

        double b0 = A * ((A + 1) + (A - 1) * cosW0 + 2 * Math.sqrt(A) * alpha);
        double b1 = -2 * A * ((A - 1) + (A + 1) * cosW0);
        double b2 = A * ((A + 1) + (A - 1) * cosW0 - 2 * Math.sqrt(A) * alpha);
        double a0 = (A + 1) - (A - 1) * cosW0 + 2 * Math.sqrt(A) * alpha;
        double a1 = 2 * ((A - 1) - (A + 1) * cosW0);
        double a2 = (A + 1) - (A - 1) * cosW0 - 2 * Math.sqrt(A) * alpha;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        double output;
        if (isLeft) {
            output = b0 * input + shelfStateL1;
            shelfStateL1 = b1 * input - a1 * output + shelfStateL2;
            shelfStateL2 = b2 * input - a2 * output;
        } else {
            output = b0 * input + shelfStateR1;
            shelfStateR1 = b1 * input - a1 * output + shelfStateR2;
            shelfStateR2 = b2 * input - a2 * output;
        }

        return (float) output;
    }

    /**
     * Simple low-pass for air absorption.
     */
    private float applyAirAbsorption(float input, float cutoff, boolean isLeft) {
        double coef = Math.exp(-2.0 * Math.PI * cutoff / currentSampleRate);

        if (isLeft) {
            airLpStateL = input + (airLpStateL - input) * coef;
            return (float) airLpStateL;
        } else {
            airLpStateR = input + (airLpStateR - input) * coef;
            return (float) airLpStateR;
        }
    }

    /**
     * Reset all filter states.
     */
    private void resetFilters() {
        lpStateL1 = lpStateL2 = 0;
        lpStateR1 = lpStateR2 = 0;
        notchStateL1 = notchStateL2 = 0;
        notchStateR1 = notchStateR2 = 0;
        shelfStateL1 = shelfStateL2 = 0;
        shelfStateR1 = shelfStateR2 = 0;
        airLpStateL = airLpStateR = 0;
        dopplerShift = 1.0f;
    }

    @Override
    public void reset() {
        if (delayLineL != null) {
            java.util.Arrays.fill(delayLineL, 0);
            java.util.Arrays.fill(delayLineR, 0);
        }
        if (reflectionBuffer != null) {
            java.util.Arrays.fill(reflectionBuffer, 0);
        }
        delayWritePos = 0;
        reflectionWritePos = 0;
        resetFilters();
        smoothAngle = angle.getValue();
        smoothElevation = elevation.getValue();
        smoothDistance = distance.getValue();
        prevAngle = smoothAngle;
        prevGainL = prevGainR = 1.0f;
    }

    @Override
    public int getLatency() {
        // ITD delay is very small (~0.7ms max at 90°)
        // Maximum is MAX_ITD_SAMPLES / 2 on average
        return MAX_ITD_SAMPLES / 2;
    }
}
