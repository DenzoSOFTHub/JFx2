package it.denzosoft.jfx2.effects.impl.drums;

import it.denzosoft.jfx2.effects.impl.drums.DrumSounds.DrumSound;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a drum pattern with events on a grid.
 *
 * <p>Patterns are defined on a 16th note grid (16 steps per bar).
 * Each step can have multiple drum hits with individual velocities.</p>
 */
public class DrumPattern {

    private final String name;
    private final String style;
    private final int beatsPerBar;       // Time signature numerator (4 for 4/4)
    private final int beatUnit;          // Time signature denominator (4 for 4/4)
    private final int bars;              // Number of bars in pattern
    private final int stepsPerBeat;      // Usually 4 (16th notes)

    // Pattern data: [step][hits]
    private final List<List<DrumHit>> steps;

    // Accent positions (for count-in and visual feedback)
    private final boolean[] accents;

    public DrumPattern(String name, String style, int beatsPerBar, int beatUnit, int bars) {
        this.name = name;
        this.style = style;
        this.beatsPerBar = beatsPerBar;
        this.beatUnit = beatUnit;
        this.bars = bars;
        this.stepsPerBeat = 4;  // 16th note resolution

        int totalSteps = bars * beatsPerBar * stepsPerBeat;
        this.steps = new ArrayList<>(totalSteps);
        for (int i = 0; i < totalSteps; i++) {
            steps.add(new ArrayList<>());
        }

        this.accents = new boolean[totalSteps];
        // Mark beat accents (downbeats)
        for (int i = 0; i < totalSteps; i += stepsPerBeat) {
            accents[i] = true;
        }
    }

    /**
     * Add a drum hit at a specific step.
     *
     * @param step     Step number (0-based)
     * @param sound    Drum sound to play
     * @param velocity Velocity (0.0 - 1.0)
     * @param pan      Stereo pan (-1.0 left, 0 center, 1.0 right)
     */
    public DrumPattern addHit(int step, DrumSound sound, float velocity, float pan) {
        if (step >= 0 && step < steps.size()) {
            steps.get(step).add(new DrumHit(sound, velocity, pan));
        }
        return this;
    }

    /**
     * Add a drum hit at a specific bar, beat, and subdivision.
     */
    public DrumPattern addHit(int bar, int beat, int sixteenth, DrumSound sound, float velocity, float pan) {
        int step = bar * beatsPerBar * stepsPerBeat + beat * stepsPerBeat + sixteenth;
        return addHit(step, sound, velocity, pan);
    }

    /**
     * Get hits at a specific step.
     */
    public List<DrumHit> getHitsAt(int step) {
        if (step >= 0 && step < steps.size()) {
            return steps.get(step);
        }
        return List.of();
    }

    /**
     * Check if a step is accented (on the beat).
     */
    public boolean isAccent(int step) {
        return step >= 0 && step < accents.length && accents[step];
    }

    /**
     * Get total number of steps in the pattern.
     */
    public int getTotalSteps() {
        return steps.size();
    }

    public String getName() { return name; }
    public String getStyle() { return style; }
    public int getBeatsPerBar() { return beatsPerBar; }
    public int getBeatUnit() { return beatUnit; }
    public int getBars() { return bars; }
    public int getStepsPerBeat() { return stepsPerBeat; }

    /**
     * Represents a single drum hit.
     */
    public record DrumHit(DrumSound sound, float velocity, float pan) {
        public DrumHit(DrumSound sound, float velocity) {
            this(sound, velocity, 0.0f);  // Center pan
        }
    }

    // ==================== FACTORY PATTERNS ====================

    /**
     * Create a simple metronome pattern (for count-in).
     */
    public static DrumPattern createCountIn(int beatsPerBar) {
        DrumPattern pattern = new DrumPattern("Count-In", "Metronome", beatsPerBar, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < beatsPerBar; beat++) {
                // Accent on beat 1, regular on others
                float velocity = (beat == 0) ? 1.0f : 0.7f;
                pattern.addHit(bar, beat, 0, DrumSound.STICKS, velocity, 0);
            }
        }

        return pattern;
    }

    /**
     * Time Beat pattern - simple metronome with accented downbeat.
     * Uses hi-hat for clean time reference.
     */
    public static DrumPattern createTimeBeat() {
        DrumPattern p = new DrumPattern("Time Beat", "Metronome", 4, 4, 1);

        // Simple quarter note pattern with accent on 1
        for (int beat = 0; beat < 4; beat++) {
            float velocity = (beat == 0) ? 1.0f : 0.7f;
            // Use rimshot for clean, clear time reference
            p.addHit(0, beat, 0, DrumSound.RIMSHOT, velocity, 0);
        }

        return p;
    }

    /**
     * Basic rock pattern - 4/4, driving eighths on hi-hat.
     */
    public static DrumPattern createRock() {
        DrumPattern p = new DrumPattern("Rock", "Rock", 4, 4, 2);

        // Standard rock beat - 2 bars
        for (int bar = 0; bar < 2; bar++) {
            // Kick on 1 and 3
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);

            // Snare on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);

            // Hi-hat on every 8th note
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }

        // Crash on bar 1 beat 1
        p.addHit(0, 0, 0, DrumSound.CRASH, 0.8f, -0.3f);

        return p;
    }

    /**
     * Heavy metal pattern - double bass, fast hi-hats.
     */
    public static DrumPattern createMetal() {
        DrumPattern p = new DrumPattern("Metal", "Metal", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Double bass pattern
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
                p.addHit(bar, beat, 2, DrumSound.KICK, 0.9f, 0);
            }

            // Snare on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);

            // 16th note hi-hats
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0) ? 0.8f : 0.5f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }

        // China/crash accents
        p.addHit(0, 0, 0, DrumSound.CRASH, 0.9f, -0.4f);

        return p;
    }

    /**
     * Blues shuffle pattern - 12/8 feel in 4/4.
     */
    public static DrumPattern createBlues() {
        DrumPattern p = new DrumPattern("Blues Shuffle", "Blues", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Kick on 1 and 3
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);

            // Snare on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);

            // Shuffle hi-hat (swing feel - skip the 2nd 16th)
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);  // Swing note
            }
        }

        return p;
    }

    /**
     * Jazz swing pattern - ride cymbal focus.
     */
    public static DrumPattern createJazz() {
        DrumPattern p = new DrumPattern("Jazz Swing", "Jazz", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Light kick on 1 and 3 (feathering)
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.4f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.3f, 0);

            // Hi-hat foot on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);
            p.addHit(bar, 3, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);

            // Ride cymbal - swing pattern
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                p.addHit(bar, beat, 3, DrumSound.RIDE, 0.4f, 0.2f);  // Swing triplet
            }
        }

        // Occasional snare ghost notes (bar 2)
        p.addHit(1, 1, 2, DrumSound.SNARE, 0.25f, 0);
        p.addHit(1, 3, 2, DrumSound.SNARE, 0.25f, 0);

        return p;
    }

    /**
     * Ballad pattern - slow, open hi-hat.
     */
    public static DrumPattern createBallad() {
        DrumPattern p = new DrumPattern("Ballad", "Ballad", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Soft kick on 1
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.5f, 0);

            // Snare on 3
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.6f, 0);

            // Open hi-hat on quarters
            for (int beat = 0; beat < 4; beat++) {
                if (beat == 0 || beat == 2) {
                    p.addHit(bar, beat, 0, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
                } else {
                    p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }

        return p;
    }

    /**
     * Funk pattern - syncopated, ghost notes.
     */
    public static DrumPattern createFunk() {
        DrumPattern p = new DrumPattern("Funk", "Funk", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Syncopated kick
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.8f, 0);

            // Snare on 2 and 4 with ghost notes
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);

            // Ghost notes
            p.addHit(bar, 0, 2, DrumSound.SNARE, 0.25f, 0);
            p.addHit(bar, 1, 2, DrumSound.SNARE, 0.2f, 0);
            p.addHit(bar, 2, 2, DrumSound.SNARE, 0.25f, 0);
            p.addHit(bar, 3, 2, DrumSound.SNARE, 0.2f, 0);

            // 16th note hi-hat with accents
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0 || sub == 2) ? 0.7f : 0.4f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }

        // Open hi-hat accent
        p.addHit(0, 3, 2, DrumSound.HIHAT_OPEN, 0.6f, 0.3f);

        return p;
    }

    /**
     * Reggae pattern - one drop.
     */
    public static DrumPattern createReggae() {
        DrumPattern p = new DrumPattern("Reggae One Drop", "Reggae", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // One drop: kick and snare together on 3
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.8f, 0);

            // Rimshot on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.6f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.6f, 0);

            // Hi-hat on upbeats (and)
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }

        return p;
    }

    /**
     * Country train beat pattern.
     */
    public static DrumPattern createCountry() {
        DrumPattern p = new DrumPattern("Country Train", "Country", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Kick on 1 and 3
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);

            // Snare on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);

            // Train beat hi-hat (alternating closed/open)
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 1, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
            }
        }

        return p;
    }

    /**
     * Disco pattern - four on the floor.
     */
    public static DrumPattern createDisco() {
        DrumPattern p = new DrumPattern("Disco", "Disco", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Four on the floor kick
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
            }

            // Snare on 2 and 4
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);

            // Open hi-hat on upbeats
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.7f, 0.3f);
            }
        }

        return p;
    }

    /**
     * Latin/Bossa Nova pattern.
     */
    public static DrumPattern createBossaNova() {
        DrumPattern p = new DrumPattern("Bossa Nova", "Latin", 4, 4, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Cross-stick pattern (clave-inspired)
            p.addHit(bar, 0, 0, DrumSound.RIMSHOT, 0.7f, 0);
            p.addHit(bar, 0, 3, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 2, 2, DrumSound.RIMSHOT, 0.6f, 0);

            // Bass drum
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.55f, 0);

            // Brushes on hi-hat
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.35f, 0.3f);
            }
        }

        return p;
    }

    /**
     * 6/8 pattern (waltz feel).
     */
    public static DrumPattern createWaltz() {
        DrumPattern p = new DrumPattern("6/8 Waltz", "Waltz", 6, 8, 2);

        for (int bar = 0; bar < 2; bar++) {
            // Kick on 1 and 4
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.7f, 0);

            // Snare on 4
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.6f, 0);

            // Hi-hat on all beats
            for (int beat = 0; beat < 6; beat++) {
                float vel = (beat == 0 || beat == 3) ? 0.7f : 0.4f;
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, vel, 0.3f);
            }
        }

        return p;
    }

    /**
     * Get all predefined patterns.
     */
    public static List<DrumPattern> getAllPatterns() {
        List<DrumPattern> patterns = new ArrayList<>();
        patterns.add(createTimeBeat());  // First pattern - simple metronome
        patterns.add(createRock());
        patterns.add(createMetal());
        patterns.add(createBlues());
        patterns.add(createJazz());
        patterns.add(createBallad());
        patterns.add(createFunk());
        patterns.add(createReggae());
        patterns.add(createCountry());
        patterns.add(createDisco());
        patterns.add(createBossaNova());
        patterns.add(createWaltz());
        return patterns;
    }

    /**
     * Get pattern by name.
     */
    public static DrumPattern getPatternByName(String name) {
        for (DrumPattern p : getAllPatterns()) {
            if (p.getName().equalsIgnoreCase(name) || p.getStyle().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return createRock();  // Default
    }
}
