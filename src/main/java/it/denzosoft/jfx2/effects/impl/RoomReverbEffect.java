package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Room Reverb effect - physically-based room simulation.
 *
 * <p>Simulates realistic room acoustics based on:
 * <ul>
 *   <li>Room dimensions (length, width, height)</li>
 *   <li>Wall material absorption characteristics</li>
 *   <li>Early reflections from walls, floor, ceiling</li>
 *   <li>Late diffuse reverb tail</li>
 *   <li>Air absorption (high frequency roll-off)</li>
 *   <li>Source/listener positioning</li>
 * </ul>
 * </p>
 */
public class RoomReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "roomreverb",
            "Room Reverb",
            "Physically-based room simulation with realistic acoustics",
            EffectCategory.REVERB
    );

    // Wall material types
    private static final String[] WALL_MATERIALS = {
            "Concrete",      // Hard, reflective
            "Brick",         // Medium-hard
            "Wood Panel",    // Warm, medium absorption
            "Drywall",       // Common, balanced
            "Glass",         // Very reflective, bright
            "Carpet/Drapes", // High absorption, dead
            "Acoustic Tile"  // Very absorptive
    };

    // Room type presets
    private static final String[] ROOM_TYPES = {
            "Custom",
            "Small Room",
            "Medium Room",
            "Large Hall",
            "Cathedral",
            "Bathroom",
            "Studio Booth"
    };

    // Absorption coefficients per material [low, mid, high]
    private static final float[][] MATERIAL_ABSORPTION = {
            {0.02f, 0.02f, 0.03f},  // Concrete
            {0.03f, 0.04f, 0.05f},  // Brick
            {0.10f, 0.07f, 0.06f},  // Wood Panel
            {0.08f, 0.05f, 0.04f},  // Drywall
            {0.03f, 0.02f, 0.02f},  // Glass
            {0.15f, 0.35f, 0.50f},  // Carpet/Drapes
            {0.25f, 0.60f, 0.80f}   // Acoustic Tile
    };

    // Parameters - Room Geometry
    private final Parameter roomTypeParam;
    private final Parameter lengthParam;
    private final Parameter widthParam;
    private final Parameter heightParam;

    // Parameters - Materials
    private final Parameter wallMaterialParam;
    private final Parameter floorAbsorptionParam;
    private final Parameter ceilingAbsorptionParam;

    // Parameters - Acoustics
    private final Parameter mixParam;
    private final Parameter erLevelParam;
    private final Parameter tailLevelParam;
    private final Parameter airAbsorptionParam;
    private final Parameter diffusionParam;

    // Parameters - Position
    private final Parameter sourceDistanceParam;
    private final Parameter listenerPosParam;

    // Speed of sound (m/s)
    private static final float SPEED_OF_SOUND = 343.0f;

    // Early reflections (6 primary reflections: 4 walls + floor + ceiling)
    private static final int NUM_EARLY_REFLECTIONS = 6;
    private float[][] erDelayBuffer;
    private int[] erWritePos;
    private int[] erDelaySamples;
    private float[] erGains;
    private float[] erPanL;
    private float[] erPanR;

    // Additional early reflections (secondary)
    private static final int NUM_SECONDARY_ER = 8;
    private float[][] erSecondaryBuffer;
    private int[] erSecondaryWritePos;
    private int[] erSecondaryDelay;
    private float[] erSecondaryGain;

    // Late reverb - Feedback Delay Network (8 delays for density)
    private static final int NUM_FDN_DELAYS = 8;
    private float[][] fdnDelayL;
    private float[][] fdnDelayR;
    private int[] fdnWritePos;
    private int[] fdnDelayTimes;

    // Damping filters (3-band: low, mid, high)
    private float[] dampLowL, dampLowR;
    private float[] dampMidL, dampMidR;
    private float[] dampHighL, dampHighR;

    // All-pass diffusors
    private static final int NUM_DIFFUSORS = 4;
    private float[][] diffusorBuffer;
    private int[] diffusorWritePos;
    private static final int[] DIFFUSOR_TIMES = {142, 107, 379, 277};
    private static final float DIFFUSOR_GAIN = 0.6f;

    // Air absorption filter state
    private float airAbsStateL, airAbsStateR;

    // Modulation
    private double[] lfoPhase;
    private static final double[] LFO_RATES = {0.3, 0.4, 0.35, 0.45, 0.5, 0.55, 0.38, 0.42};

    public RoomReverbEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        // === ROW 1: ROOM geometry & materials ===
        roomTypeParam = addChoiceParameter("roomType", "Room",
                "Room type preset",
                ROOM_TYPES, 2);

        lengthParam = addFloatParameter("length", "Length",
                "Room length in meters",
                2.0f, 50.0f, 8.0f, "m");

        widthParam = addFloatParameter("width", "Width",
                "Room width in meters",
                2.0f, 40.0f, 6.0f, "m");

        heightParam = addFloatParameter("height", "Height",
                "Room height in meters",
                2.0f, 20.0f, 3.0f, "m");

        wallMaterialParam = addChoiceParameter("wallMat", "Walls",
                "Wall surface material",
                WALL_MATERIALS, 3); // Default: Drywall

        floorAbsorptionParam = addFloatParameter("floor", "Floor",
                "Floor absorption (0=reflective, 100=absorptive)",
                0.0f, 100.0f, 30.0f, "%");

        ceilingAbsorptionParam = addFloatParameter("ceiling", "Ceiling",
                "Ceiling absorption",
                0.0f, 100.0f, 20.0f, "%");

        // === ROW 2: AUDIO parameters ===
        mixParam = addFloatParameter("mix", "Mix",
                "Dry/wet balance",
                0.0f, 100.0f, 40.0f, "%");

        erLevelParam = addFloatParameter("erLevel", "Early",
                "Early reflections level",
                0.0f, 100.0f, 80.0f, "%");

        tailLevelParam = addFloatParameter("tailLevel", "Tail",
                "Late reverb tail level",
                0.0f, 100.0f, 70.0f, "%");

        diffusionParam = addFloatParameter("diffusion", "Diffusion",
                "Reverb density/smoothness",
                0.0f, 100.0f, 70.0f, "%");

        airAbsorptionParam = addFloatParameter("airAbs", "Air Abs",
                "High frequency absorption by air",
                0.0f, 100.0f, 30.0f, "%");

        sourceDistanceParam = addFloatParameter("srcDist", "Source",
                "Sound source distance from listener",
                0.5f, 20.0f, 3.0f, "m");

        listenerPosParam = addFloatParameter("listPos", "Position",
                "Listener position in room (0=center, 100=corner)",
                0.0f, 100.0f, 30.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Calculate max delay sizes based on largest possible room
        float maxRoomDim = 50.0f; // meters
        int maxDelaySamples = (int) (maxRoomDim * 2 / SPEED_OF_SOUND * sampleRate) + 1000;

        // Early reflections buffers
        erDelayBuffer = new float[NUM_EARLY_REFLECTIONS][maxDelaySamples];
        erWritePos = new int[NUM_EARLY_REFLECTIONS];
        erDelaySamples = new int[NUM_EARLY_REFLECTIONS];
        erGains = new float[NUM_EARLY_REFLECTIONS];
        erPanL = new float[NUM_EARLY_REFLECTIONS];
        erPanR = new float[NUM_EARLY_REFLECTIONS];

        // Secondary early reflections
        erSecondaryBuffer = new float[NUM_SECONDARY_ER][maxDelaySamples];
        erSecondaryWritePos = new int[NUM_SECONDARY_ER];
        erSecondaryDelay = new int[NUM_SECONDARY_ER];
        erSecondaryGain = new float[NUM_SECONDARY_ER];

        // FDN delays
        fdnDelayL = new float[NUM_FDN_DELAYS][maxDelaySamples];
        fdnDelayR = new float[NUM_FDN_DELAYS][maxDelaySamples];
        fdnWritePos = new int[NUM_FDN_DELAYS];
        fdnDelayTimes = new int[NUM_FDN_DELAYS];

        // Damping filter states
        dampLowL = new float[NUM_FDN_DELAYS];
        dampLowR = new float[NUM_FDN_DELAYS];
        dampMidL = new float[NUM_FDN_DELAYS];
        dampMidR = new float[NUM_FDN_DELAYS];
        dampHighL = new float[NUM_FDN_DELAYS];
        dampHighR = new float[NUM_FDN_DELAYS];

        // Diffusor buffers
        diffusorBuffer = new float[NUM_DIFFUSORS][1024];
        diffusorWritePos = new int[NUM_DIFFUSORS];

        // LFO phases
        lfoPhase = new double[NUM_FDN_DELAYS];

        // Calculate initial room parameters
        updateRoomParameters();
    }

    private void updateRoomParameters() {
        float length = lengthParam.getTargetValue();
        float width = widthParam.getTargetValue();
        float height = heightParam.getTargetValue();
        float srcDist = sourceDistanceParam.getTargetValue();
        float listPos = listenerPosParam.getTargetValue() / 100.0f;

        // Get wall material absorption
        int wallMat = wallMaterialParam.getChoiceIndex();
        float[] wallAbs = MATERIAL_ABSORPTION[wallMat];
        float floorAbs = floorAbsorptionParam.getTargetValue() / 100.0f;
        float ceilingAbs = ceilingAbsorptionParam.getTargetValue() / 100.0f;

        // Calculate listener position (offset from center based on listPos)
        float listX = length * 0.5f + listPos * length * 0.3f;
        float listY = width * 0.5f + listPos * width * 0.2f;
        float listZ = height * 0.4f; // Ear height

        // Source position (in front of listener)
        float srcX = listX - srcDist;
        float srcY = listY;
        float srcZ = listZ;

        // Calculate early reflections using image-source method (simplified)
        // ER 0: Left wall
        float distLeftWall = 2 * srcY;
        erDelaySamples[0] = metersToSamples(distLeftWall);
        erGains[0] = calculateReflectionGain(distLeftWall, wallAbs[1]);
        erPanL[0] = 0.8f; erPanR[0] = 0.3f;

        // ER 1: Right wall
        float distRightWall = 2 * (width - srcY);
        erDelaySamples[1] = metersToSamples(distRightWall);
        erGains[1] = calculateReflectionGain(distRightWall, wallAbs[1]);
        erPanL[1] = 0.3f; erPanR[1] = 0.8f;

        // ER 2: Front wall
        float distFrontWall = 2 * srcX;
        erDelaySamples[2] = metersToSamples(distFrontWall);
        erGains[2] = calculateReflectionGain(distFrontWall, wallAbs[1]);
        erPanL[2] = 0.5f; erPanR[2] = 0.5f;

        // ER 3: Back wall
        float distBackWall = 2 * (length - srcX);
        erDelaySamples[3] = metersToSamples(distBackWall);
        erGains[3] = calculateReflectionGain(distBackWall, wallAbs[1]);
        erPanL[3] = 0.6f; erPanR[3] = 0.4f;

        // ER 4: Floor
        float distFloor = 2 * srcZ;
        erDelaySamples[4] = metersToSamples(distFloor);
        erGains[4] = calculateReflectionGain(distFloor, floorAbs);
        erPanL[4] = 0.5f; erPanR[4] = 0.5f;

        // ER 5: Ceiling
        float distCeiling = 2 * (height - srcZ);
        erDelaySamples[5] = metersToSamples(distCeiling);
        erGains[5] = calculateReflectionGain(distCeiling, ceilingAbs);
        erPanL[5] = 0.5f; erPanR[5] = 0.5f;

        // Secondary reflections (corner reflections, approximated)
        float[] secDistances = {
                (float) Math.sqrt(distLeftWall * distLeftWall + distFrontWall * distFrontWall),
                (float) Math.sqrt(distRightWall * distRightWall + distFrontWall * distFrontWall),
                (float) Math.sqrt(distLeftWall * distLeftWall + distBackWall * distBackWall),
                (float) Math.sqrt(distRightWall * distRightWall + distBackWall * distBackWall),
                (float) Math.sqrt(distFloor * distFloor + distLeftWall * distLeftWall),
                (float) Math.sqrt(distFloor * distFloor + distRightWall * distRightWall),
                (float) Math.sqrt(distCeiling * distCeiling + distLeftWall * distLeftWall),
                (float) Math.sqrt(distCeiling * distCeiling + distRightWall * distRightWall)
        };

        for (int i = 0; i < NUM_SECONDARY_ER; i++) {
            erSecondaryDelay[i] = metersToSamples(secDistances[i]);
            // Secondary reflections are weaker (two bounces)
            erSecondaryGain[i] = calculateReflectionGain(secDistances[i], wallAbs[1]) * 0.5f;
        }

        // FDN delay times based on room modes
        float roomVolume = length * width * height;
        float rt60 = calculateRT60(roomVolume, length, width, height, wallAbs[1], floorAbs, ceilingAbs);

        // Use mutually prime delay times based on room dimensions
        float[] modeDelays = {
                length / SPEED_OF_SOUND,
                width / SPEED_OF_SOUND,
                height / SPEED_OF_SOUND,
                (float) Math.sqrt(length * length + width * width) / SPEED_OF_SOUND,
                (float) Math.sqrt(length * length + height * height) / SPEED_OF_SOUND,
                (float) Math.sqrt(width * width + height * height) / SPEED_OF_SOUND,
                (float) Math.sqrt(length * length + width * width + height * height) / SPEED_OF_SOUND,
                (length + width + height) / 3 / SPEED_OF_SOUND
        };

        for (int i = 0; i < NUM_FDN_DELAYS; i++) {
            fdnDelayTimes[i] = (int) (modeDelays[i] * sampleRate * (0.8f + 0.4f * (i / (float) NUM_FDN_DELAYS)));
            fdnDelayTimes[i] = Math.max(100, Math.min(fdnDelayTimes[i], fdnDelayL[i].length - 100));
        }
    }

    private int metersToSamples(float meters) {
        return Math.max(1, (int) (meters / SPEED_OF_SOUND * sampleRate));
    }

    private float calculateReflectionGain(float distance, float absorption) {
        // Inverse distance law + absorption
        float distanceAtten = 1.0f / (1.0f + distance * 0.5f);
        float reflectionCoef = 1.0f - absorption;
        return distanceAtten * reflectionCoef;
    }

    private float calculateRT60(float volume, float l, float w, float h,
                                 float wallAbs, float floorAbs, float ceilingAbs) {
        // Sabine equation: RT60 = 0.161 * V / A
        float surfaceArea = 2 * (l * w + l * h + w * h);
        float avgAbsorption = (wallAbs * 2 * (l * h + w * h) + floorAbs * l * w + ceilingAbs * l * w) / surfaceArea;
        float totalAbsorption = surfaceArea * avgAbsorption;
        return 0.161f * volume / Math.max(0.01f, totalAbsorption);
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float[] tempR = new float[frameCount];
        System.arraycopy(input, 0, tempR, 0, frameCount);
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];
        processInternal(input, tempR, outL, outR, frameCount);
        for (int i = 0; i < frameCount; i++) {
            output[i] = (outL[i] + outR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        processInternal(inputL, inputR, outputL, outputR, frameCount);
    }

    private void processInternal(float[] inputL, float[] inputR,
                                  float[] outputL, float[] outputR, int frameCount) {

        // Update room parameters if needed
        updateRoomParameters();

        float mix = mixParam.getValue() / 100.0f;
        float dry = 1.0f - mix;
        float wet = mix;

        float erLevel = erLevelParam.getValue() / 100.0f;
        float tailLevel = tailLevelParam.getValue() / 100.0f;
        float diffusion = diffusionParam.getValue() / 100.0f;
        float airAbs = airAbsorptionParam.getValue() / 100.0f;

        // Get absorption values
        int wallMat = wallMaterialParam.getChoiceIndex();
        float[] wallAbs = MATERIAL_ABSORPTION[wallMat];

        // Calculate feedback coefficient for RT60
        float length = lengthParam.getValue();
        float width = widthParam.getValue();
        float height = heightParam.getValue();
        float volume = length * width * height;
        float rt60 = calculateRT60(volume, length, width, height,
                wallAbs[1], floorAbsorptionParam.getValue() / 100.0f,
                ceilingAbsorptionParam.getValue() / 100.0f);

        // FDN feedback based on RT60
        float avgDelay = 0;
        for (int d : fdnDelayTimes) avgDelay += d;
        avgDelay /= NUM_FDN_DELAYS;
        float feedback = (float) Math.exp(-3.0 * avgDelay / (rt60 * sampleRate));
        feedback = Math.min(0.97f, feedback);

        // Air absorption coefficient
        float airAbsCoef = 0.0001f + 0.002f * airAbs;

        for (int i = 0; i < frameCount; i++) {
            float inL = inputL[i];
            float inR = inputR[i];
            float inMono = (inL + inR) * 0.5f;

            // === EARLY REFLECTIONS ===
            float erOutL = 0, erOutR = 0;

            // Primary early reflections
            for (int er = 0; er < NUM_EARLY_REFLECTIONS; er++) {
                // Write to buffer
                erDelayBuffer[er][erWritePos[er]] = inMono;

                // Read with delay
                int readPos = (erWritePos[er] - erDelaySamples[er] + erDelayBuffer[er].length) % erDelayBuffer[er].length;
                float erSample = erDelayBuffer[er][readPos] * erGains[er];

                erOutL += erSample * erPanL[er];
                erOutR += erSample * erPanR[er];

                erWritePos[er] = (erWritePos[er] + 1) % erDelayBuffer[er].length;
            }

            // Secondary early reflections
            for (int er = 0; er < NUM_SECONDARY_ER; er++) {
                erSecondaryBuffer[er][erSecondaryWritePos[er]] = inMono;

                int readPos = (erSecondaryWritePos[er] - erSecondaryDelay[er] + erSecondaryBuffer[er].length) % erSecondaryBuffer[er].length;
                float erSample = erSecondaryBuffer[er][readPos] * erSecondaryGain[er];

                // Alternate panning for width
                if (er % 2 == 0) {
                    erOutL += erSample * 0.6f;
                    erOutR += erSample * 0.4f;
                } else {
                    erOutL += erSample * 0.4f;
                    erOutR += erSample * 0.6f;
                }

                erSecondaryWritePos[er] = (erSecondaryWritePos[er] + 1) % erSecondaryBuffer[er].length;
            }

            erOutL *= erLevel;
            erOutR *= erLevel;

            // === DIFFUSION ===
            float diffIn = (erOutL + erOutR) * 0.5f * diffusion + inMono * (1 - diffusion) * 0.3f;

            for (int d = 0; d < NUM_DIFFUSORS; d++) {
                int readPos = (diffusorWritePos[d] - DIFFUSOR_TIMES[d] + diffusorBuffer[d].length) % diffusorBuffer[d].length;
                float apOut = diffusorBuffer[d][readPos];
                float apIn = diffIn + DIFFUSOR_GAIN * apOut;
                diffusorBuffer[d][diffusorWritePos[d]] = apIn;
                diffIn = apOut - DIFFUSOR_GAIN * apIn;
                diffusorWritePos[d] = (diffusorWritePos[d] + 1) % diffusorBuffer[d].length;
            }

            // === LATE REVERB (FDN) ===
            float fdnOutL = 0, fdnOutR = 0;

            for (int d = 0; d < NUM_FDN_DELAYS; d++) {
                // Modulated delay time
                int modOffset = (int) (8 * Math.sin(2 * Math.PI * lfoPhase[d]));
                int delayTime = fdnDelayTimes[d] + modOffset;
                delayTime = Math.max(1, Math.min(delayTime, fdnDelayL[d].length - 1));

                int readPos = (fdnWritePos[d] - delayTime + fdnDelayL[d].length) % fdnDelayL[d].length;

                float delOutL = fdnDelayL[d][readPos];
                float delOutR = fdnDelayR[d][readPos];

                // 3-band damping based on wall material
                // Low frequencies
                dampLowL[d] += 0.1f * (delOutL - dampLowL[d]);
                dampLowR[d] += 0.1f * (delOutR - dampLowR[d]);
                float lowL = dampLowL[d] * (1 - wallAbs[0]);
                float lowR = dampLowR[d] * (1 - wallAbs[0]);

                // Mid frequencies
                dampMidL[d] += 0.3f * (delOutL - dampMidL[d]);
                dampMidR[d] += 0.3f * (delOutR - dampMidR[d]);
                float midL = (dampMidL[d] - dampLowL[d]) * (1 - wallAbs[1]);
                float midR = (dampMidR[d] - dampLowR[d]) * (1 - wallAbs[1]);

                // High frequencies
                dampHighL[d] += 0.7f * (delOutL - dampHighL[d]);
                dampHighR[d] += 0.7f * (delOutR - dampHighR[d]);
                float highL = (delOutL - dampHighL[d]) * (1 - wallAbs[2]);
                float highR = (delOutR - dampHighR[d]) * (1 - wallAbs[2]);

                delOutL = lowL + midL + highL;
                delOutR = lowR + midR + highR;

                // Accumulate with Hadamard-like mixing
                float sign = ((d & 1) == 0) ? 1.0f : -1.0f;
                float sign2 = ((d & 2) == 0) ? 1.0f : -1.0f;
                fdnOutL += delOutL * sign;
                fdnOutR += delOutR * sign2;

                // Update LFO
                lfoPhase[d] += LFO_RATES[d] / sampleRate;
                if (lfoPhase[d] >= 1.0) lfoPhase[d] -= 1.0;
            }

            fdnOutL *= 0.35f;
            fdnOutR *= 0.35f;

            // Write back to FDN with feedback and input
            for (int d = 0; d < NUM_FDN_DELAYS; d++) {
                float fbL = feedback * (d < 4 ? fdnOutL : fdnOutR);
                float fbR = feedback * (d < 4 ? fdnOutR : fdnOutL);

                fdnDelayL[d][fdnWritePos[d]] = diffIn * 0.5f + fbL;
                fdnDelayR[d][fdnWritePos[d]] = diffIn * 0.5f + fbR;

                fdnWritePos[d] = (fdnWritePos[d] + 1) % fdnDelayL[d].length;
            }

            // Apply air absorption (simple lowpass)
            airAbsStateL += airAbsCoef * (fdnOutL - airAbsStateL);
            airAbsStateR += airAbsCoef * (fdnOutR - airAbsStateR);
            float tailL = fdnOutL - airAbsStateL * airAbs;
            float tailR = fdnOutR - airAbsStateR * airAbs;

            tailL *= tailLevel;
            tailR *= tailLevel;

            // === FINAL MIX ===
            float wetL = erOutL + tailL;
            float wetR = erOutR + tailR;

            outputL[i] = inL * dry + wetL * wet;
            outputR[i] = inR * dry + wetR * wet;
        }
    }

    @Override
    protected void onReset() {
        for (int i = 0; i < NUM_EARLY_REFLECTIONS; i++) {
            if (erDelayBuffer[i] != null) java.util.Arrays.fill(erDelayBuffer[i], 0);
            erWritePos[i] = 0;
        }
        for (int i = 0; i < NUM_SECONDARY_ER; i++) {
            if (erSecondaryBuffer[i] != null) java.util.Arrays.fill(erSecondaryBuffer[i], 0);
            erSecondaryWritePos[i] = 0;
        }
        for (int i = 0; i < NUM_FDN_DELAYS; i++) {
            if (fdnDelayL[i] != null) java.util.Arrays.fill(fdnDelayL[i], 0);
            if (fdnDelayR[i] != null) java.util.Arrays.fill(fdnDelayR[i], 0);
            fdnWritePos[i] = 0;
            dampLowL[i] = dampLowR[i] = 0;
            dampMidL[i] = dampMidR[i] = 0;
            dampHighL[i] = dampHighR[i] = 0;
            lfoPhase[i] = 0;
        }
        for (int i = 0; i < NUM_DIFFUSORS; i++) {
            if (diffusorBuffer[i] != null) java.util.Arrays.fill(diffusorBuffer[i], 0);
            diffusorWritePos[i] = 0;
        }
        airAbsStateL = airAbsStateR = 0;
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1 (Room): Room Type, Length, Width, Height, Walls, Floor, Ceiling
        // Row 2 (Audio): Mix, Early, Tail, Diffusion, Air Abs, Source, Position
        return new int[] {7, 7};
    }
}
