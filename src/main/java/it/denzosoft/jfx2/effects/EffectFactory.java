package it.denzosoft.jfx2.effects;

import it.denzosoft.jfx2.effects.impl.*;
import it.denzosoft.jfx2.effects.impl.drums.DrumMachine;

import java.util.*;
import java.util.function.Supplier;

/**
 * Factory for creating effect instances.
 *
 * <p>Maintains a registry of available effect types and creates instances on demand.</p>
 */
public class EffectFactory {

    private static final EffectFactory INSTANCE = new EffectFactory();

    private final Map<String, Supplier<AudioEffect>> registry;
    private final Map<String, EffectMetadata> metadataRegistry;

    private EffectFactory() {
        this.registry = new LinkedHashMap<>();
        this.metadataRegistry = new LinkedHashMap<>();
        registerBuiltInEffects();
    }

    /**
     * Get the singleton factory instance.
     */
    public static EffectFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Register built-in effects.
     */
    private void registerBuiltInEffects() {
        // Input Sources (generators and audio input)
        register("audioinput", AudioInputEffect::new);
        register("wavfileinput", WavFileInputEffect::new);
        register("oscillator", OscillatorEffect::new);
        register("drummachine", DrumMachine::new);

        // Output Sinks (audio output and recorders)
        register("audiooutput", AudioOutputEffect::new);
        register("wavfileoutput", WavFileOutputEffect::new);

        // Gain/Dynamics
        register("gain", GainEffect::new);
        register("noisegate", NoiseGateEffect::new);
        register("noisesuppressor", NoiseSuppressorEffect::new);
        register("compressor", CompressorEffect::new);
        register("multibandcomp", MultibandCompressorEffect::new);
        register("limiter", LimiterEffect::new);
        register("volumeswell", VolumeSwellEffect::new);
        register("sustainer", SustainerEffect::new);
        register("autosustain", AutoSustainEffect::new);

        // Distortion
        register("overdrive", OverdriveEffect::new);
        register("drive", DriveEffect::new);
        register("distortion", DistortionEffect::new);
        register("fuzz", FuzzEffect::new);
        register("tubedist", TubeDistortionEffect::new);

        // Time-based / Delay
        register("delay", DelayEffect::new);
        register("tapeecho", TapeEchoEffect::new);
        register("multitap", MultiTapDelayEffect::new);
        register("reversedelay", ReverseDelayEffect::new);
        register("pingpong", PingPongDelayEffect::new);
        register("quaddelay", QuadDelayEffect::new);

        // Reverb
        register("reverb", ReverbEffect::new);
        register("springreverb", SpringReverbEffect::new);
        register("shimmerreverb", ShimmerReverbEffect::new);

        // Modulation
        register("chorus", ChorusEffect::new);
        register("flanger", FlangerEffect::new);
        register("phaser", PhaserEffect::new);
        register("tremolo", TremoloEffect::new);
        register("vibrato", VibratoEffect::new);
        register("panner", PannerEffect::new);
        register("ringmod", RingModulatorEffect::new);
        register("univibe", UniVibeEffect::new);
        register("rotary", RotaryEffect::new);

        // EQ
        register("filter", FilterEffect::new);
        register("parametriceq", ParametricEQEffect::new);
        register("graphiceq", GraphicEQEffect::new);

        // Amp Simulation (Phase 8)
        register("amp", AmpEffect::new);
        register("cabsim", CabinetSimEffect::new);
        register("irloader", IRLoaderEffect::new);
        register("neuralamp", NeuralAmpEffect::new);
        register("nam", NAMEffect::new);
        register("tubepreamp", TubePreampEffect::new);
        register("tubepoweramp", TubePowerAmpEffect::new);
        register("cabinetsim", CabinetSimulatorEffect::new);

        // Filter (Phase 9)
        register("wah", WahEffect::new);
        register("envelopefilter", EnvelopeFilterEffect::new);
        register("talkbox", TalkBoxEffect::new);
        register("synth", SynthEffect::new);

        // Pitch (Phase 9)
        register("pitchshift", PitchShifterEffect::new);
        register("octaver", OctaverEffect::new);
        register("harmonizer", HarmonizerEffect::new);

        // Utility
        register("splitter", SplitterEffect::new);
        register("mixer", MixerEffect::new);
        register("mono2stereo", MonoToStereoEffect::new);
        register("looper", LooperEffect::new);
    }

    /**
     * Register an effect type.
     *
     * @param id      Unique identifier
     * @param factory Factory function to create instances
     */
    public void register(String id, Supplier<AudioEffect> factory) {
        registry.put(id, factory);
        // Create a temporary instance to get metadata
        AudioEffect temp = factory.get();
        metadataRegistry.put(id, temp.getMetadata());
    }

    /**
     * Create an effect instance.
     *
     * @param id Effect type identifier
     * @return New effect instance, or null if not found
     */
    public AudioEffect create(String id) {
        Supplier<AudioEffect> factory = registry.get(id);
        if (factory == null) {
            return null;
        }
        return factory.get();
    }

    /**
     * Get metadata for an effect type.
     *
     * @param id Effect type identifier
     * @return Metadata, or null if not found
     */
    public EffectMetadata getMetadata(String id) {
        return metadataRegistry.get(id);
    }

    /**
     * Get all registered effect IDs.
     */
    public Set<String> getRegisteredEffects() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /**
     * Get all effect metadata.
     */
    public Collection<EffectMetadata> getAllMetadata() {
        return Collections.unmodifiableCollection(metadataRegistry.values());
    }

    /**
     * Get effects by category.
     */
    public List<EffectMetadata> getEffectsByCategory(EffectCategory category) {
        List<EffectMetadata> result = new ArrayList<>();
        for (EffectMetadata meta : metadataRegistry.values()) {
            if (meta.category() == category) {
                result.add(meta);
            }
        }
        return result;
    }

    /**
     * Check if an effect type is registered.
     */
    public boolean isRegistered(String id) {
        return registry.containsKey(id);
    }
}
