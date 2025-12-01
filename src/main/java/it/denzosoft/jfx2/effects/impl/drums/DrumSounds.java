package it.denzosoft.jfx2.effects.impl.drums;

import java.util.Random;

/**
 * High-quality synthesized drum sounds for the drum machine.
 *
 * <p>Improved synthesis with multiple layers and realistic envelopes.</p>
 */
public class DrumSounds implements DrumSoundProvider {

    private final int sampleRate;
    private final Random random = new Random(42); // Consistent random for reproducibility

    // Pre-rendered drum samples (mono, normalized)
    private float[] kick;
    private float[] snare;
    private float[] hihatClosed;
    private float[] hihatOpen;
    private float[] crash;
    private float[] ride;
    private float[] rideBell;
    private float[] tomHigh;
    private float[] tomMid;
    private float[] tomLow;
    private float[] rimshot;
    private float[] clap;
    private float[] cowbell;
    private float[] sticks;

    public DrumSounds(int sampleRate) {
        this.sampleRate = sampleRate;
        generateAllSounds();
    }

    private void generateAllSounds() {
        kick = generateKick();
        snare = generateSnare();
        hihatClosed = generateHihatClosed();
        hihatOpen = generateHihatOpen();
        crash = generateCrash();
        ride = generateRide();
        rideBell = generateRideBell();
        tomHigh = generateTom(200);
        tomMid = generateTom(150);
        tomLow = generateTom(100);
        rimshot = generateRimshot();
        clap = generateClap();
        cowbell = generateCowbell();
        sticks = generateSticks();
    }

    // ==================== UTILITIES ====================

    private float noise() {
        return random.nextFloat() * 2 - 1;
    }

    private float[] applyLowPass(float[] samples, float cutoffHz) {
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / sampleRate;
        float alpha = dt / (rc + dt);

        float[] output = new float[samples.length];
        float prev = 0;
        for (int i = 0; i < samples.length; i++) {
            prev = prev + alpha * (samples[i] - prev);
            output[i] = prev;
        }
        return output;
    }

    private float[] applyHighPass(float[] samples, float cutoffHz) {
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / sampleRate;
        float alpha = rc / (rc + dt);

        float[] output = new float[samples.length];
        float prevIn = 0;
        float prevOut = 0;
        for (int i = 0; i < samples.length; i++) {
            output[i] = alpha * (prevOut + samples[i] - prevIn);
            prevIn = samples[i];
            prevOut = output[i];
        }
        return output;
    }

    private float saturate(float x) {
        // Soft clipping
        if (x > 1) return 1;
        if (x < -1) return -1;
        return 1.5f * x - 0.5f * x * x * x;
    }

    // ==================== KICK DRUM ====================
    // Realistic kick with beater attack, shell resonance, and air movement

    private float[] generateKick() {
        int length = (int) (sampleRate * 0.4);
        float[] samples = new float[length];

        double phase = 0;

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // === BEATER CLICK (0-10ms) ===
            float beaterEnv = (float) Math.exp(-t * 200);
            float beater = 0;
            // Multiple high harmonics for the "thwack"
            for (int h = 1; h <= 5; h++) {
                beater += (float) Math.sin(2 * Math.PI * (800 + h * 400) * t) / h;
            }
            beater *= beaterEnv * 0.3f;

            // === SHELL TONE (main body) ===
            // Pitch drops from ~100Hz to ~55Hz
            float pitchEnv = (float) Math.exp(-t * 35);
            float freq = 55 + 50 * pitchEnv;
            phase += 2 * Math.PI * freq / sampleRate;

            // Fundamental
            float shell = (float) Math.sin(phase);
            // Second harmonic adds punch
            shell += 0.5f * (float) Math.sin(phase * 2);
            // Sub adds weight
            shell += 0.3f * (float) Math.sin(phase * 0.5);

            float shellEnv = (float) Math.exp(-t * 8);
            shell *= shellEnv;

            // === AIR MOVEMENT (low thump) ===
            float airEnv = (float) Math.exp(-t * 15);
            float air = (float) Math.sin(2 * Math.PI * 40 * t) * airEnv * 0.4f;

            // === MIX ===
            float mix = beater + shell * 0.8f + air;
            samples[i] = saturate(mix);
        }

        samples = applyLowPass(samples, 8000);
        return normalize(samples);
    }

    // ==================== SNARE DRUM ====================
    // Body tone + snare wires + attack crack

    private float[] generateSnare() {
        int length = (int) (sampleRate * 0.25);
        float[] samples = new float[length];

        // Pre-generate noise for consistency
        float[] noiseBuffer = new float[length];
        for (int i = 0; i < length; i++) {
            noiseBuffer[i] = noise();
        }
        // Filter the noise
        noiseBuffer = applyHighPass(noiseBuffer, 2000);
        noiseBuffer = applyLowPass(noiseBuffer, 10000);

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // === ATTACK CRACK ===
            float crackEnv = (float) Math.exp(-t * 150);
            float crack = noise() * crackEnv * 0.4f;

            // === SHELL BODY (200Hz fundamental) ===
            float bodyEnv = (float) Math.exp(-t * 25);
            float pitchDrop = 1.0f + 0.1f * (float) Math.exp(-t * 50);
            float bodyFreq = 185 * pitchDrop;
            float body = (float) Math.sin(2 * Math.PI * bodyFreq * t);
            // Add harmonics for body character
            body += 0.4f * (float) Math.sin(2 * Math.PI * bodyFreq * 2.2f * t);
            body += 0.2f * (float) Math.sin(2 * Math.PI * bodyFreq * 3.5f * t);
            body *= bodyEnv * 0.5f;

            // === SNARE WIRES ===
            float wireEnv = (float) Math.exp(-t * 15);
            float wires = noiseBuffer[i] * wireEnv * 0.6f;

            // === MIX ===
            samples[i] = crack + body + wires;
        }

        return normalize(samples);
    }

    // ==================== HI-HAT CLOSED ====================
    // Metallic cymbal - high frequencies, lots of noise

    private float[] generateHihatClosed() {
        int length = (int) (sampleRate * 0.05);
        float[] samples = new float[length];

        // High metallic frequencies - inharmonic ratios
        float[] freqs = {425, 640, 895, 1180, 1520, 1950, 2580, 3420, 4500, 5800};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Very fast decay
            float env = (float) Math.exp(-t * 120);

            float sum = 0;
            for (int j = 0; j < freqs.length; j++) {
                // Each partial has slightly different decay
                float partialEnv = (float) Math.exp(-t * (100 + j * 15));
                float amp = 1.0f / (float) Math.sqrt(j + 1);
                sum += Math.sin(2 * Math.PI * freqs[j] * t) * amp * partialEnv;
            }

            // Lots of high-frequency noise for metallic sizzle
            float n = noise() * 0.6f;

            samples[i] = (sum * 0.4f + n * 0.6f) * env;
        }

        // Strong high-pass for brightness
        samples = applyHighPass(samples, 5000);
        return normalize(samples);
    }

    // ==================== HI-HAT OPEN ====================

    private float[] generateHihatOpen() {
        int length = (int) (sampleRate * 0.35);
        float[] samples = new float[length];

        // Same high metallic frequencies as closed
        float[] freqs = {425, 640, 895, 1180, 1520, 1950, 2580, 3420, 4500, 5800, 7200};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Slower decay for open hat
            float env = (float) Math.exp(-t * 7);

            float sum = 0;
            for (int j = 0; j < freqs.length; j++) {
                float partialEnv = (float) Math.exp(-t * (5 + j * 0.8f));
                // Shimmer
                float shimmer = 1 + 0.003f * (float) Math.sin(2 * Math.PI * 5 * t);
                float amp = 1.0f / (float) Math.sqrt(j + 1);
                sum += Math.sin(2 * Math.PI * freqs[j] * shimmer * t) * amp * partialEnv;
            }

            // More noise for sustain
            float n = noise() * 0.5f;

            samples[i] = (sum * 0.4f + n * 0.6f) * env;
        }

        samples = applyHighPass(samples, 4000);
        return normalize(samples);
    }

    // ==================== CRASH CYMBAL ====================
    // Real cymbal is almost entirely filtered noise

    private float[] generateCrash() {
        int length = (int) (sampleRate * 2.5);
        float[] samples = new float[length];

        // Pre-generate and filter noise for the wash
        float[] noiseWash = new float[length];
        for (int i = 0; i < length; i++) {
            noiseWash[i] = noise();
        }
        // Band-pass for cymbal character (not too high to avoid harshness)
        noiseWash = applyHighPass(noiseWash, 2000);
        noiseWash = applyLowPass(noiseWash, 12000);

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Fast attack, long decay
            float attack = 1 - (float) Math.exp(-t * 200);
            float decay = (float) Math.exp(-t * 1.2f);
            float env = attack * decay;

            // Main wash
            float washEnv = (float) Math.exp(-t * 1.3f);
            float wash = noiseWash[i] * washEnv;

            // Bright attack burst
            float attackEnv = (float) Math.exp(-t * 15);
            float attackNoise = noise() * attackEnv;

            // Mix: all noise, no tonal modes
            samples[i] = (wash * 0.7f + attackNoise * 0.4f) * env;
        }

        samples = applyHighPass(samples, 600);
        return normalize(samples);
    }

    // ==================== RIDE CYMBAL ====================
    // Defined stick ping with sustain - more metallic than crash

    private float[] generateRide() {
        int length = (int) (sampleRate * 1.5);
        float[] samples = new float[length];

        // Inharmonic frequencies
        float[] freqs = {467, 743, 1021, 1378, 1812, 2389, 3156, 4187};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Clear attack
            float attack = 1 - (float) Math.exp(-t * 400);
            float decay = (float) Math.exp(-t * 2.5f);
            float env = attack * decay;

            float sum = 0;
            for (int j = 0; j < freqs.length; j++) {
                float partialDecay = (float) Math.exp(-t * (2f + j * 0.4f));
                float amp = 1.0f / (float) Math.sqrt(j + 1);
                sum += Math.sin(2 * Math.PI * freqs[j] * t) * amp * partialDecay;
            }

            // Stick ping - high frequency click
            float pingEnv = (float) Math.exp(-t * 80);
            float ping = noise() * pingEnv * 0.2f;

            // Metallic noise
            float noiseEnv = (float) Math.exp(-t * 4);
            float n = noise() * noiseEnv * 0.3f;

            samples[i] = (sum * 0.5f + ping + n * 0.3f) * env;
        }

        samples = applyHighPass(samples, 500);
        return normalize(samples);
    }

    // ==================== RIDE BELL ====================
    // Bell of ride cymbal - metallic ping, NOT piano-like

    private float[] generateRideBell() {
        int length = (int) (sampleRate * 0.8);
        float[] samples = new float[length];

        // Inharmonic bell frequencies - avoid integer ratios!
        float[] freqs = {936, 1287, 1683, 2214, 2891, 3752, 4890};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            float attack = 1 - (float) Math.exp(-t * 500);
            float decay = (float) Math.exp(-t * 4);
            float env = attack * decay;

            float sum = 0;
            for (int j = 0; j < freqs.length; j++) {
                float partialEnv = (float) Math.exp(-t * (3 + j * 0.5f));
                float amp = 1.0f / (float) Math.sqrt(j + 1);
                sum += Math.sin(2 * Math.PI * freqs[j] * t) * amp * partialEnv;
            }

            // Add some noise for metallic character
            float noiseEnv = (float) Math.exp(-t * 20);
            float n = noise() * noiseEnv * 0.15f;

            samples[i] = (sum + n) * env;
        }

        samples = applyHighPass(samples, 600);
        return normalize(samples);
    }

    // ==================== TOM ====================

    private float[] generateTom(float baseFreq) {
        float sizeRatio = 200f / baseFreq;
        int length = (int) (sampleRate * 0.35 * sizeRatio);
        float[] samples = new float[length];

        double phase = 0;

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Pitch envelope - drops about 20%
            float pitchEnv = (float) Math.exp(-t * 20);
            float freq = baseFreq * (0.85f + 0.15f * pitchEnv);
            phase += 2 * Math.PI * freq / sampleRate;

            // Body
            float body = (float) Math.sin(phase);
            body += 0.3f * (float) Math.sin(phase * 1.5);
            body += 0.15f * (float) Math.sin(phase * 2.3);

            float env = (float) Math.exp(-t * (7 / sizeRatio));

            // Attack
            float attackEnv = (float) Math.exp(-t * 80);
            float attack = noise() * attackEnv * 0.2f;

            samples[i] = body * env + attack;
        }

        samples = applyLowPass(samples, 3000 + baseFreq * 10);
        return normalize(samples);
    }

    // ==================== RIMSHOT ====================

    private float[] generateRimshot() {
        int length = (int) (sampleRate * 0.08);
        float[] samples = new float[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Crack
            float crackEnv = (float) Math.exp(-t * 120);
            float crackFreq = 1500 * (float) Math.exp(-t * 40);
            float crack = (float) Math.sin(2 * Math.PI * crackFreq * t) * crackEnv;

            // Noise pop
            float noiseEnv = (float) Math.exp(-t * 100);
            float n = noise() * noiseEnv * 0.5f;

            // Wood tone
            float woodEnv = (float) Math.exp(-t * 60);
            float wood = (float) Math.sin(2 * Math.PI * 500 * t) * woodEnv * 0.3f;

            samples[i] = crack * 0.4f + n + wood;
        }

        samples = applyHighPass(samples, 400);
        return normalize(samples);
    }

    // ==================== CLAP ====================

    private float[] generateClap() {
        int length = (int) (sampleRate * 0.3);
        float[] samples = new float[length];

        // Pre-generate filtered noise
        float[] noiseBuffer = new float[length];
        for (int i = 0; i < length; i++) {
            noiseBuffer[i] = noise();
        }
        noiseBuffer = applyHighPass(noiseBuffer, 800);
        noiseBuffer = applyLowPass(noiseBuffer, 6000);

        // Multiple clap layers
        int[] offsets = {0, 18, 38, 65};
        float[] vols = {0.75f, 1.0f, 0.9f, 0.65f};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;
            float sum = 0;

            for (int c = 0; c < offsets.length; c++) {
                if (i >= offsets[c]) {
                    float ct = (float) (i - offsets[c]) / sampleRate;
                    float clapEnv = (float) Math.exp(-ct * 45);
                    sum += noiseBuffer[i] * clapEnv * vols[c];
                }
            }

            // Reverb tail
            float tailEnv = (float) Math.exp(-t * 12);
            float tail = noise() * tailEnv * 0.12f;

            samples[i] = sum * 0.6f + tail;
        }

        return normalize(samples);
    }

    // ==================== COWBELL ====================
    // Metallic percussion - hollow metal sound

    private float[] generateCowbell() {
        int length = (int) (sampleRate * 0.2);
        float[] samples = new float[length];

        // Inharmonic frequencies for hollow metal
        float[] freqs = {562, 845, 1127, 1685};

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            // Quick decay
            float env = (float) Math.exp(-t * 18);

            float sum = 0;
            for (int j = 0; j < freqs.length; j++) {
                float partialEnv = (float) Math.exp(-t * (15 + j * 5));
                // Square-ish wave for metallic harshness
                float phase = 2 * (float) Math.PI * freqs[j] * t;
                float wave = (float) Math.sin(phase);
                wave += 0.3f * (float) Math.sin(phase * 3);  // Add odd harmonic
                float amp = 1.0f / (j + 1);
                sum += wave * amp * partialEnv;
            }

            // Strike noise
            float strikeEnv = (float) Math.exp(-t * 200);
            float strike = noise() * strikeEnv * 0.25f;

            samples[i] = (sum + strike) * env;
        }

        samples = applyHighPass(samples, 400);
        samples = applyLowPass(samples, 6000);
        return normalize(samples);
    }

    // ==================== STICKS ====================

    private float[] generateSticks() {
        int length = (int) (sampleRate * 0.04);
        float[] samples = new float[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / sampleRate;

            float env = (float) Math.exp(-t * 150);

            // Wood resonance
            float tone1 = (float) Math.sin(2 * Math.PI * 2500 * t);
            float tone2 = (float) Math.sin(2 * Math.PI * 3200 * t) * 0.5f;
            float tone3 = (float) Math.sin(2 * Math.PI * 4000 * t) * 0.25f;

            // Noise click
            float noiseEnv = (float) Math.exp(-t * 250);
            float n = noise() * noiseEnv * 0.3f;

            samples[i] = (tone1 + tone2 + tone3) * env * 0.6f + n;
        }

        samples = applyHighPass(samples, 1800);
        return normalize(samples);
    }

    // ==================== NORMALIZE ====================

    private float[] normalize(float[] samples) {
        float max = 0;
        for (float s : samples) {
            max = Math.max(max, Math.abs(s));
        }
        if (max > 0) {
            float gain = 0.92f / max;
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= gain;
            }
        }
        return samples;
    }

    // ==================== INTERFACE METHODS ====================

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public String getName() {
        return "Synthesized Drums";
    }

    // ==================== GETTERS ====================

    @Override
    public float[] getKick() { return kick; }
    public float[] getSnare() { return snare; }
    public float[] getHihatClosed() { return hihatClosed; }
    public float[] getHihatOpen() { return hihatOpen; }
    public float[] getCrash() { return crash; }
    public float[] getRide() { return ride; }
    public float[] getRideBell() { return rideBell; }
    public float[] getTomHigh() { return tomHigh; }
    public float[] getTomMid() { return tomMid; }
    public float[] getTomLow() { return tomLow; }
    public float[] getRimshot() { return rimshot; }
    public float[] getClap() { return clap; }
    public float[] getCowbell() { return cowbell; }
    public float[] getSticks() { return sticks; }

    public float[] getSound(DrumSound type) {
        return switch (type) {
            case KICK -> kick;
            case SNARE -> snare;
            case HIHAT_CLOSED -> hihatClosed;
            case HIHAT_OPEN -> hihatOpen;
            case CRASH -> crash;
            case RIDE -> ride;
            case RIDE_BELL -> rideBell;
            case TOM_HIGH -> tomHigh;
            case TOM_MID -> tomMid;
            case TOM_LOW -> tomLow;
            case RIMSHOT -> rimshot;
            case CLAP -> clap;
            case COWBELL -> cowbell;
            case STICKS -> sticks;
            case REST -> new float[0];
        };
    }

    public enum DrumSound {
        KICK,
        SNARE,
        HIHAT_CLOSED,
        HIHAT_OPEN,
        CRASH,
        RIDE,
        RIDE_BELL,
        TOM_HIGH,
        TOM_MID,
        TOM_LOW,
        RIMSHOT,
        CLAP,
        COWBELL,
        STICKS,
        REST
    }
}
