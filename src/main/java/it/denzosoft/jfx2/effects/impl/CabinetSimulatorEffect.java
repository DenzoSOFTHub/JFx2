package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Guitar Cabinet Simulator Effect.
 *
 * <p>Simulates a guitar speaker cabinet with configurable speakers
 * and microphone placement. Supports up to 2 microphones with
 * independent positioning.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Multiple speaker types (Celestion, Jensen, Eminence, etc.)</li>
 *   <li>Cabinet configurations (1x10, 1x12, 2x12, 4x12, etc.)</li>
 *   <li>Dual microphone support with blending</li>
 *   <li>Mic position (center to edge)</li>
 *   <li>Mic distance from speaker</li>
 *   <li>Mic angle (on-axis to off-axis)</li>
 *   <li>Speaker resonance and compression</li>
 * </ul></p>
 */
public class CabinetSimulatorEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "cabinetsim",
            "Cabinet Simulator",
            "Guitar cabinet simulation with speaker and microphone modeling",
            EffectCategory.AMP_SIM
    );

    // Cabinet parameters
    private final Parameter speakerTypeParam;
    private final Parameter cabinetConfigParam;
    private final Parameter cabinetResonanceParam;

    // Microphone 1 parameters
    private final Parameter mic1TypeParam;
    private final Parameter mic1PositionParam;
    private final Parameter mic1DistanceParam;
    private final Parameter mic1AngleParam;
    private final Parameter mic1LevelParam;

    // Microphone 2 parameters
    private final Parameter mic2TypeParam;
    private final Parameter mic2PositionParam;
    private final Parameter mic2DistanceParam;
    private final Parameter mic2AngleParam;
    private final Parameter mic2LevelParam;
    private final Parameter mic2EnabledParam;

    // Output
    private final Parameter outputParam;

    // DSP state
    private int sampleRate;

    // Speaker resonance filter (2nd order)
    private float[] speakerResonanceState = new float[2];

    // Speaker low pass filter
    private float[] speakerLPState = new float[2];

    // Speaker high pass filter (cabinet size)
    private float[] speakerHPState = new float[2];

    // Mic 1 filters
    private float[] mic1BassState = new float[2];
    private float[] mic1PresenceState = new float[2];
    private float[] mic1AirState = new float[2];

    // Mic 2 filters
    private float[] mic2BassState = new float[2];
    private float[] mic2PresenceState = new float[2];
    private float[] mic2AirState = new float[2];

    // Room/distance simulation
    private float[] roomDelayBuffer;
    private int roomDelayPos = 0;
    private static final int MAX_ROOM_DELAY = 2048;

    // Speaker compression state
    private float speakerCompState = 0;

    public CabinetSimulatorEffect() {
        super(METADATA);

        // === CABINET PARAMETERS ===

        // Speaker type (0-11 mapped to SpeakerType enum)
        speakerTypeParam = addFloatParameter("speakerType", "Speaker Type",
                "0=V30, 1=Greenback, 2=G12H, 3=Blue, 4=Jensen P12R, 5=Jensen C12N, " +
                "6=Cannabis Rex, 7=Governor, 8=G12T-75, 9=Weber Blue Dog, 10=10\", 11=15\"",
                0.0f, 11.0f, 1.0f, "");

        // Cabinet configuration: 0=1x10, 1=1x12, 2=2x10, 3=2x12, 4=4x10, 5=4x12
        cabinetConfigParam = addFloatParameter("cabinetConfig", "Cabinet",
                "0=1x10, 1=1x12, 2=2x10, 3=2x12, 4=4x10, 5=4x12",
                0.0f, 5.0f, 3.0f, "");

        // Cabinet resonance (low frequency body)
        cabinetResonanceParam = addFloatParameter("cabinetResonance", "Cab Resonance",
                "Cabinet body resonance amount",
                0.0f, 100.0f, 50.0f, "%");

        // === MICROPHONE 1 ===

        // Mic 1 type (0-11 mapped to MicrophoneType enum)
        mic1TypeParam = addFloatParameter("mic1Type", "Mic 1 Type",
                "0=SM57, 1=MD421, 2=R121, 3=e906, 4=e609, 5=C414, 6=U87, 7=M160, 8=RE20, 9=AT4050, 10=Fathead",
                0.0f, 10.0f, 0.0f, "");

        // Mic 1 position: 0=edge, 50=halfway, 100=center
        mic1PositionParam = addFloatParameter("mic1Position", "Mic 1 Position",
                "Position from edge (dark) to center (bright)",
                0.0f, 100.0f, 50.0f, "%");

        // Mic 1 distance: 0-12 inches
        mic1DistanceParam = addFloatParameter("mic1Distance", "Mic 1 Distance",
                "Distance from speaker grille in inches",
                0.0f, 12.0f, 1.0f, "in");

        // Mic 1 angle: 0=on-axis, 45=max off-axis
        mic1AngleParam = addFloatParameter("mic1Angle", "Mic 1 Angle",
                "Off-axis angle: 0=straight on, 45=angled",
                0.0f, 45.0f, 0.0f, "deg");

        // Mic 1 level
        mic1LevelParam = addFloatParameter("mic1Level", "Mic 1 Level",
                "Microphone 1 output level",
                0.0f, 100.0f, 100.0f, "%");

        // === MICROPHONE 2 ===

        // Enable mic 2
        mic2EnabledParam = addFloatParameter("mic2Enabled", "Mic 2 Enabled",
                "Enable second microphone (0=off, 1=on)",
                0.0f, 1.0f, 0.0f, "");

        // Mic 2 type
        mic2TypeParam = addFloatParameter("mic2Type", "Mic 2 Type",
                "0=SM57, 1=MD421, 2=R121, 3=e906, 4=e609, 5=C414, 6=U87, 7=M160, 8=RE20, 9=AT4050, 10=Fathead",
                0.0f, 10.0f, 2.0f, "");  // Default to R121 for classic pairing

        // Mic 2 position
        mic2PositionParam = addFloatParameter("mic2Position", "Mic 2 Position",
                "Position from edge (dark) to center (bright)",
                0.0f, 100.0f, 30.0f, "%");

        // Mic 2 distance
        mic2DistanceParam = addFloatParameter("mic2Distance", "Mic 2 Distance",
                "Distance from speaker grille in inches",
                0.0f, 12.0f, 2.0f, "in");

        // Mic 2 angle
        mic2AngleParam = addFloatParameter("mic2Angle", "Mic 2 Angle",
                "Off-axis angle",
                0.0f, 45.0f, 15.0f, "deg");

        // Mic 2 level
        mic2LevelParam = addFloatParameter("mic2Level", "Mic 2 Level",
                "Microphone 2 output level",
                0.0f, 100.0f, 70.0f, "%");

        // === OUTPUT ===
        outputParam = addFloatParameter("output", "Output",
                "Output level",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;
        roomDelayBuffer = new float[MAX_ROOM_DELAY];
        onReset();
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Get speaker type
        int speakerIndex = Math.min((int) speakerTypeParam.getValue(), SpeakerType.values().length - 1);
        SpeakerType speaker = SpeakerType.values()[speakerIndex];

        // Get cabinet config
        int cabinetConfig = (int) cabinetConfigParam.getValue();
        float cabinetResonance = cabinetResonanceParam.getValue() / 100.0f;

        // Get mic types
        int mic1Index = Math.min((int) mic1TypeParam.getValue(), MicrophoneType.values().length - 1);
        MicrophoneType mic1 = MicrophoneType.values()[mic1Index];

        int mic2Index = Math.min((int) mic2TypeParam.getValue(), MicrophoneType.values().length - 1);
        MicrophoneType mic2 = MicrophoneType.values()[mic2Index];
        boolean mic2Enabled = mic2EnabledParam.getValue() > 0.5f;

        // Mic positions
        float mic1Pos = mic1PositionParam.getValue() / 100.0f;
        float mic1Dist = mic1DistanceParam.getValue();
        float mic1Angle = mic1AngleParam.getValue();
        float mic1Level = mic1LevelParam.getValue() / 100.0f;

        float mic2Pos = mic2PositionParam.getValue() / 100.0f;
        float mic2Dist = mic2DistanceParam.getValue();
        float mic2Angle = mic2AngleParam.getValue();
        float mic2Level = mic2LevelParam.getValue() / 100.0f;

        float outputLevel = outputParam.getValue() / 100.0f * 2.0f;

        // Calculate cabinet characteristics
        float cabinetLowCut = getCabinetLowCut(cabinetConfig);
        int numSpeakers = getNumSpeakers(cabinetConfig);
        float cabinetVolume = numSpeakers * 0.2f + 0.6f; // More speakers = more volume/body

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // === SPEAKER SIMULATION ===

            // Speaker resonance (low frequency bump)
            sample = processSpeakerResonance(sample, speaker.getResonanceHz(),
                    cabinetResonance * 0.7f, speaker.getCompression());

            // Speaker high-pass (cabinet size limits bass)
            sample = processHighPass(sample, cabinetLowCut, speakerHPState);

            // Speaker low-pass (high frequency rolloff)
            sample = processLowPass(sample, speaker.getRolloffHz(), speakerLPState);

            // Speaker compression (cone breakup)
            sample = processSpeakerCompression(sample, speaker.getCompression());

            // Apply speaker EQ character
            sample = applySpeakerEQ(sample, speaker);

            // Multiple speakers interaction (slight chorus/thickening)
            if (numSpeakers > 1) {
                sample *= cabinetVolume;
            }

            // === MICROPHONE 1 ===
            float mic1Out = processMicrophone(sample, mic1, mic1Pos, mic1Dist, mic1Angle,
                    mic1BassState, mic1PresenceState, mic1AirState);
            mic1Out *= mic1Level;

            // === MICROPHONE 2 ===
            float mic2Out = 0;
            if (mic2Enabled) {
                mic2Out = processMicrophone(sample, mic2, mic2Pos, mic2Dist, mic2Angle,
                        mic2BassState, mic2PresenceState, mic2AirState);
                mic2Out *= mic2Level;

                // Add slight delay for mic 2 based on distance difference (phase alignment)
                float delayMs = Math.abs(mic2Dist - mic1Dist) * 0.0254f / 343.0f * 1000.0f;
                int delaySamples = (int) (delayMs * sampleRate / 1000.0f);
                delaySamples = Math.min(delaySamples, MAX_ROOM_DELAY - 1);

                if (delaySamples > 0) {
                    roomDelayBuffer[roomDelayPos] = mic2Out;
                    int readPos = (roomDelayPos - delaySamples + MAX_ROOM_DELAY) % MAX_ROOM_DELAY;
                    mic2Out = roomDelayBuffer[readPos];
                    roomDelayPos = (roomDelayPos + 1) % MAX_ROOM_DELAY;
                }
            }

            // === MIX ===
            float finalOut;
            if (mic2Enabled) {
                // Blend both mics
                float totalLevel = mic1Level + mic2Level;
                if (totalLevel > 0) {
                    finalOut = (mic1Out + mic2Out) / Math.max(totalLevel, 1.0f);
                } else {
                    finalOut = 0;
                }
            } else {
                finalOut = mic1Out;
            }

            // Output level
            finalOut *= outputLevel;

            // Soft clip
            if (finalOut > 1.0f) finalOut = 1.0f - 0.5f / (finalOut + 0.5f);
            else if (finalOut < -1.0f) finalOut = -1.0f + 0.5f / (-finalOut + 0.5f);

            output[i] = finalOut;
        }
    }

    /**
     * Process speaker resonance (low frequency emphasis).
     */
    private float processSpeakerResonance(float input, float resonanceHz, float amount, float compression) {
        // Resonant low shelf / peak
        float w0 = 2.0f * (float) Math.PI * resonanceHz / sampleRate;
        float q = 0.7f + amount * 0.5f;
        float gain = 1.0f + amount * 0.5f;

        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / (2.0f * q);

        float a0 = 1.0f + alpha / gain;
        float a1 = -2.0f * cosW0;
        float a2 = 1.0f - alpha / gain;
        float b0 = 1.0f + alpha * gain;
        float b1 = -2.0f * cosW0;
        float b2 = 1.0f - alpha * gain;

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * speakerResonanceState[0] - a2 * speakerResonanceState[1];
        float output = b0 * w + b1 * speakerResonanceState[0] + b2 * speakerResonanceState[1];
        speakerResonanceState[1] = speakerResonanceState[0];
        speakerResonanceState[0] = w;

        return output;
    }

    /**
     * Process speaker compression/breakup.
     */
    private float processSpeakerCompression(float input, float compressionAmount) {
        // Track signal level
        float level = Math.abs(input);
        speakerCompState = speakerCompState * 0.99f + level * 0.01f;

        // Apply compression when level is high
        float threshold = 0.5f;
        if (speakerCompState > threshold) {
            float compression = 1.0f + (speakerCompState - threshold) * compressionAmount * 2.0f;
            input /= compression;
        }

        // Soft saturation (speaker cone breakup)
        if (Math.abs(input) > 0.8f) {
            input = (float) Math.tanh(input * 1.2f) * 0.85f;
        }

        return input;
    }

    /**
     * Apply speaker's inherent EQ characteristics.
     */
    private float applySpeakerEQ(float input, SpeakerType speaker) {
        // Simple approximation of speaker EQ
        // In a full implementation, this would use multiple parametric bands
        float output = input;

        // Apply relative levels (simplified)
        output *= (speaker.getBass() + speaker.getLowMids() + speaker.getUpperMids() +
                   speaker.getPresence() + speaker.getAir()) / 5.0f;

        return output;
    }

    /**
     * Process microphone characteristics and position.
     */
    private float processMicrophone(float input, MicrophoneType mic, float position,
                                    float distance, float angle,
                                    float[] bassState, float[] presenceState, float[] airState) {
        float output = input;

        // === POSITION EFFECT ===
        // Center = bright (more highs), Edge = dark (more bass, less highs)
        float brightnessFromPosition = position;  // 0 = edge (dark), 1 = center (bright)

        // High frequency adjustment based on position
        // Center of cone has more high frequencies
        float highFreqMult = 0.6f + brightnessFromPosition * 0.6f;

        // Low frequency slightly more at edge
        float lowFreqMult = 1.1f - brightnessFromPosition * 0.2f;

        // === DISTANCE EFFECT ===
        // Proximity effect: closer = more bass (for directional mics)
        float proximityBoost = 1.0f;
        if (distance < 3.0f) {
            proximityBoost = 1.0f + (3.0f - distance) / 3.0f * mic.getProximityEffect() * 0.3f;
        }

        // High frequency loss with distance (air absorption)
        float distanceHighLoss = 1.0f - Math.min(distance / 12.0f, 0.3f);

        // === ANGLE EFFECT ===
        // Off-axis = darker, less harsh
        float angleRad = angle * (float) Math.PI / 180.0f;
        float offAxisDarkening = (float) Math.cos(angleRad);  // 1.0 at 0°, ~0.7 at 45°

        // === APPLY MIC EQ ===

        // Bass (affected by proximity and mic character)
        float bassGain = (mic.getBass() - 1.0f) * 6.0f + (proximityBoost - 1.0f) * 6.0f;
        bassGain += (lowFreqMult - 1.0f) * 3.0f;
        output = processShelf(output, 150.0f, bassGain, bassState, true);

        // Presence peak (affected by position and angle)
        float presenceGain = (mic.getPresence() - 1.0f) * 8.0f;
        presenceGain += (highFreqMult - 1.0f) * 6.0f;
        presenceGain *= offAxisDarkening;
        output = processPeaking(output, 3500.0f, presenceGain, 1.5f, presenceState);

        // Air/top end (affected by distance and angle)
        float airGain = (mic.getAir() - 1.0f) * 6.0f;
        airGain += (distanceHighLoss - 1.0f) * 6.0f;
        airGain *= offAxisDarkening;
        output = processShelf(output, 8000.0f, airGain, airState, false);

        // Apply mic's overall character
        output *= (mic.getMids() + mic.getLowMids()) / 2.0f;

        return output;
    }

    private float processHighPass(float input, float freq, float[] state) {
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) / (2.0f * 0.707f);

        float a0 = 1.0f + alpha;
        float a1 = -2.0f * cosW0;
        float a2 = 1.0f - alpha;
        float b0 = (1.0f + cosW0) / 2.0f;
        float b1 = -(1.0f + cosW0);
        float b2 = (1.0f + cosW0) / 2.0f;

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    private float processLowPass(float input, float freq, float[] state) {
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) / (2.0f * 0.707f);

        float a0 = 1.0f + alpha;
        float a1 = -2.0f * cosW0;
        float a2 = 1.0f - alpha;
        float b0 = (1.0f - cosW0) / 2.0f;
        float b1 = 1.0f - cosW0;
        float b2 = (1.0f - cosW0) / 2.0f;

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    private float processShelf(float input, float freq, float gainDb, float[] state, boolean lowShelf) {
        if (Math.abs(gainDb) < 0.1f) return input;

        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / 2.0f * 0.707f;
        float sqrtGain = (float) Math.sqrt(gain);

        float a0, a1, a2, b0, b1, b2;

        if (lowShelf) {
            a0 = (gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
            a1 = -2 * ((gain - 1) + (gain + 1) * cosW0);
            a2 = (gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
            b0 = gain * ((gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
            b1 = 2 * gain * ((gain - 1) - (gain + 1) * cosW0);
            b2 = gain * ((gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha);
        } else {
            a0 = (gain + 1) - (gain - 1) * cosW0 + 2 * sqrtGain * alpha;
            a1 = 2 * ((gain - 1) - (gain + 1) * cosW0);
            a2 = (gain + 1) - (gain - 1) * cosW0 - 2 * sqrtGain * alpha;
            b0 = gain * ((gain + 1) + (gain - 1) * cosW0 + 2 * sqrtGain * alpha);
            b1 = -2 * gain * ((gain - 1) + (gain + 1) * cosW0);
            b2 = gain * ((gain + 1) + (gain - 1) * cosW0 - 2 * sqrtGain * alpha);
        }

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    private float processPeaking(float input, float freq, float gainDb, float q, float[] state) {
        if (Math.abs(gainDb) < 0.1f) return input;

        float gain = (float) Math.pow(10.0, gainDb / 40.0);
        float w0 = 2.0f * (float) Math.PI * freq / sampleRate;
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / (2.0f * q);

        float a0 = 1.0f + alpha / gain;
        float a1 = -2.0f * cosW0;
        float a2 = 1.0f - alpha / gain;
        float b0 = 1.0f + alpha * gain;
        float b1 = -2.0f * cosW0;
        float b2 = 1.0f - alpha * gain;

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;

        float w = input - a1 * state[0] - a2 * state[1];
        float output = b0 * w + b1 * state[0] + b2 * state[1];
        state[1] = state[0];
        state[0] = w;

        return output;
    }

    /**
     * Get cabinet low frequency cutoff based on configuration.
     */
    private float getCabinetLowCut(int config) {
        return switch (config) {
            case 0 -> 100.0f;   // 1x10 - smallest
            case 1 -> 80.0f;    // 1x12
            case 2 -> 90.0f;    // 2x10
            case 3 -> 70.0f;    // 2x12
            case 4 -> 80.0f;    // 4x10
            case 5 -> 60.0f;    // 4x12 - largest, most bass
            default -> 75.0f;
        };
    }

    /**
     * Get number of speakers in cabinet.
     */
    private int getNumSpeakers(int config) {
        return switch (config) {
            case 0, 1 -> 1;     // 1x10, 1x12
            case 2, 3 -> 2;     // 2x10, 2x12
            case 4, 5 -> 4;     // 4x10, 4x12
            default -> 1;
        };
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Process mono, output to both channels
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        float[] monoIn = new float[len];
        float[] monoOut = new float[len];

        for (int i = 0; i < len; i++) {
            monoIn[i] = (inputL[i] + inputR[i]) * 0.5f;
        }

        onProcess(monoIn, monoOut, len);

        System.arraycopy(monoOut, 0, outputL, 0, len);
        System.arraycopy(monoOut, 0, outputR, 0, len);
    }

    @Override
    protected void onReset() {
        java.util.Arrays.fill(speakerResonanceState, 0);
        java.util.Arrays.fill(speakerLPState, 0);
        java.util.Arrays.fill(speakerHPState, 0);
        java.util.Arrays.fill(mic1BassState, 0);
        java.util.Arrays.fill(mic1PresenceState, 0);
        java.util.Arrays.fill(mic1AirState, 0);
        java.util.Arrays.fill(mic2BassState, 0);
        java.util.Arrays.fill(mic2PresenceState, 0);
        java.util.Arrays.fill(mic2AirState, 0);
        if (roomDelayBuffer != null) {
            java.util.Arrays.fill(roomDelayBuffer, 0);
        }
        roomDelayPos = 0;
        speakerCompState = 0;
    }

    // Convenience methods
    public void setSpeakerType(SpeakerType type) {
        for (int i = 0; i < SpeakerType.values().length; i++) {
            if (SpeakerType.values()[i] == type) {
                speakerTypeParam.setValue(i);
                break;
            }
        }
    }

    public void setMic1Type(MicrophoneType type) {
        for (int i = 0; i < MicrophoneType.values().length; i++) {
            if (MicrophoneType.values()[i] == type) {
                mic1TypeParam.setValue(i);
                break;
            }
        }
    }

    public void setMic2Type(MicrophoneType type) {
        for (int i = 0; i < MicrophoneType.values().length; i++) {
            if (MicrophoneType.values()[i] == type) {
                mic2TypeParam.setValue(i);
                break;
            }
        }
    }

    public void setMic1Position(float percent) { mic1PositionParam.setValue(percent); }
    public void setMic1Distance(float inches) { mic1DistanceParam.setValue(inches); }
    public void setMic1Angle(float degrees) { mic1AngleParam.setValue(degrees); }
    public void setMic2Enabled(boolean enabled) { mic2EnabledParam.setValue(enabled ? 1 : 0); }
    public void setMic2Position(float percent) { mic2PositionParam.setValue(percent); }
    public void setMic2Distance(float inches) { mic2DistanceParam.setValue(inches); }
    public void setMic2Angle(float degrees) { mic2AngleParam.setValue(degrees); }
}
