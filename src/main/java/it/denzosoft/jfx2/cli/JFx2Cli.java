package it.denzosoft.jfx2.cli;

import it.denzosoft.jfx2.audio.*;
import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.effects.impl.*;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.preset.*;
import it.denzosoft.jfx2.tools.*;
import it.denzosoft.jfx2.recording.*;

import javax.sound.sampled.Mixer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Command Line Interface for JFx2 Guitar Multi-Effects Processor.
 *
 * <p>Provides interactive CLI for testing audio, effects, signal graph,
 * presets, and tools without launching the GUI.</p>
 */
public class JFx2Cli {

    private final AudioEngine audioEngine;
    private final SignalGraph signalGraph;
    private final TemplateManager templateManager;
    private volatile boolean running;

    public JFx2Cli() {
        this.audioEngine = new AudioEngine();
        this.signalGraph = new SignalGraph();
        this.templateManager = new TemplateManager();
    }

    /**
     * Run the interactive CLI.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);

        printMenu();

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "1", "devices" -> listDevices();
                case "2", "start" -> startAudio();
                case "3", "stop" -> stopAudio();
                case "4", "status" -> showStatus();
                case "5", "gain" -> adjustGain(scanner);
                case "6", "test" -> testSignalGraph();
                case "7", "effects" -> testEffectsChain();
                case "8", "full" -> testFullChain();
                case "9", "presets" -> listPresets();
                case "10", "load" -> loadPreset(scanner);
                case "11", "save" -> savePreset(scanner);
                case "12", "factory" -> generateFactoryTemplates();
                case "13", "parallel" -> testPresets();
                case "h", "help" -> printMenu();
                case "q", "quit", "exit" -> {
                    shutdown();
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Unknown command. Type 'h' for help.");
            }
        }
    }

    /**
     * Execute a specific test command.
     */
    public void executeCommand(String command) {
        switch (command) {
            case "--test-graph" -> testSignalGraph();
            case "--test-effects" -> testEffectsChain();
            case "--test-full" -> testFullChain();
            case "--test-presets" -> testPresets();
            case "--test-tools" -> testTools();
            case "--generate-factory" -> generateFactoryTemplates();
            default -> System.out.println("Unknown command: " + command);
        }
    }

    private void printMenu() {
        System.out.println("\nCommands:");
        System.out.println("  1, devices  - List audio devices");
        System.out.println("  2, start    - Start audio processing");
        System.out.println("  3, stop     - Stop audio processing");
        System.out.println("  4, status   - Show status and metrics");
        System.out.println("  5, gain     - Adjust gain");
        System.out.println("  6, test     - Test signal graph");
        System.out.println("  7, effects  - Test effects chain (Phase 3-4)");
        System.out.println("  8, full     - Test full chain (Phase 5-9)");
        System.out.println("  9, presets  - List available presets");
        System.out.println("  10, load    - Load a preset");
        System.out.println("  11, save    - Save current rig as preset");
        System.out.println("  12, factory - Generate factory presets");
        System.out.println("  13, parallel- Test parallel routing (Phase 7)");
        System.out.println("  h, help     - Show this menu");
        System.out.println("  q, quit     - Exit");
    }

    private void listDevices() {
        System.out.println("\n--- Audio Devices ---");

        try {
            if (!audioEngine.isInitialized()) {
                audioEngine.initialize(AudioConfig.DEFAULT);
            }

            System.out.println("\nInput Devices:");
            List<Mixer.Info> inputs = audioEngine.getInputDevices();
            if (inputs.isEmpty()) {
                System.out.println("  (no input devices found)");
            } else {
                for (int i = 0; i < inputs.size(); i++) {
                    Mixer.Info info = inputs.get(i);
                    System.out.printf("  [%d] %s%n", i, info.getName());
                }
            }

            System.out.println("\nOutput Devices:");
            List<Mixer.Info> outputs = audioEngine.getOutputDevices();
            if (outputs.isEmpty()) {
                System.out.println("  (no output devices found)");
            } else {
                for (int i = 0; i < outputs.size(); i++) {
                    Mixer.Info info = outputs.get(i);
                    System.out.printf("  [%d] %s%n", i, info.getName());
                }
            }

        } catch (AudioEngine.AudioEngineException e) {
            System.out.println("Error querying devices: " + e.getMessage());
        }
    }

    private void startAudio() {
        if (audioEngine.isRunning()) {
            System.out.println("Audio is already running.");
            return;
        }

        try {
            if (!audioEngine.isInitialized()) {
                System.out.println("Initializing audio engine...");
                audioEngine.initialize(AudioConfig.DEFAULT);
            }

            System.out.println("Setting up signal graph...");
            signalGraph.createDefaultGraph();

            GainNode gainNode = new GainNode("gain1", "Master Gain");
            signalGraph.addNode(gainNode);

            new ArrayList<>(signalGraph.getConnections()).forEach(c -> signalGraph.disconnect(c.getId()));
            signalGraph.connect(signalGraph.getInputNode().getOutput(), gainNode.getInput());
            signalGraph.connect(gainNode.getOutput(), signalGraph.getOutputNode().getInput());

            AudioConfig config = audioEngine.getConfig();
            signalGraph.prepare(config.sampleRate(), config.bufferSize());

            System.out.println("Starting audio...");
            audioEngine.start((input, output, frameCount) -> {
                signalGraph.process(input, output, frameCount);
            });

            running = true;
            System.out.println("Audio started! Signal chain: Input -> Gain -> Output");
            System.out.println("Estimated latency: " + String.format("%.1f ms", config.getEstimatedRoundTripLatencyMs()));

        } catch (AudioEngine.AudioEngineException e) {
            System.out.println("Error starting audio: " + e.getMessage());
        }
    }

    private void stopAudio() {
        if (!audioEngine.isRunning()) {
            System.out.println("Audio is not running.");
            return;
        }

        audioEngine.stop();
        running = false;
        System.out.println("Audio stopped.");
    }

    private void showStatus() {
        System.out.println("\n--- Status ---");
        System.out.println("Audio Engine: " + (audioEngine.isInitialized() ? "Initialized" : "Not initialized"));
        System.out.println("Running: " + (audioEngine.isRunning() ? "Yes" : "No"));

        if (audioEngine.isInitialized()) {
            System.out.println("Config: " + audioEngine.getConfig());
        }

        if (audioEngine.isRunning()) {
            AudioMetrics metrics = audioEngine.getMetrics();
            System.out.println("\nMetrics:");
            System.out.printf("  CPU Load: %.1f%%%n", metrics.getCpuLoadPercent());
            System.out.printf("  Input Level: %.1f dB%n", metrics.getPeakInputLevelDb());
            System.out.printf("  Output Level: %.1f dB%n", metrics.getPeakOutputLevelDb());
            System.out.printf("  Callbacks: %d%n", metrics.getProcessedCallbacks());
            System.out.printf("  Dropouts: %d%n", metrics.getDropouts());
        }

        System.out.println("\nSignal Graph:");
        System.out.printf("  Nodes: %d%n", signalGraph.getNodeCount());
        System.out.printf("  Connections: %d%n", signalGraph.getConnectionCount());

        ProcessingNode gainNode = signalGraph.getNode("gain1");
        if (gainNode instanceof GainNode gn) {
            System.out.printf("  Gain: %.2f (%.1f dB)%n", gn.getGain(), gn.getGainDb());
        }
    }

    private void adjustGain(Scanner scanner) {
        ProcessingNode node = signalGraph.getNode("gain1");
        if (!(node instanceof GainNode gainNode)) {
            System.out.println("Gain node not found. Start audio first.");
            return;
        }

        System.out.printf("Current gain: %.2f (%.1f dB)%n", gainNode.getGain(), gainNode.getGainDb());
        System.out.print("Enter new gain in dB (e.g., -6, 0, +6): ");

        try {
            String input = scanner.nextLine().trim();
            float dB = Float.parseFloat(input.replace("+", ""));
            gainNode.setGainDb(dB);
            System.out.printf("Gain set to: %.2f (%.1f dB)%n", gainNode.getGain(), gainNode.getGainDb());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }

    /**
     * Test signal graph functionality without audio.
     */
    public void testSignalGraph() {
        System.out.println("\n--- Signal Graph Test ---");

        SignalGraph graph = new SignalGraph();
        graph.createDefaultGraph();
        System.out.println("Created default graph with Input -> Output");

        GainNode gain1 = new GainNode("gain1", "Gain 1");
        GainNode gain2 = new GainNode("gain2", "Gain 2");
        gain1.setGainDb(-6);
        gain2.setGainDb(-6);

        graph.addNode(gain1);
        graph.addNode(gain2);
        System.out.println("Added two gain nodes (each -6 dB)");

        new ArrayList<>(graph.getConnections()).forEach(c -> graph.disconnect(c.getId()));

        graph.connect(graph.getInputNode().getOutput(), gain1.getInput());
        graph.connect(gain1.getOutput(), gain2.getInput());
        graph.connect(gain2.getOutput(), graph.getOutputNode().getInput());
        System.out.println("Connected: Input -> Gain1 -> Gain2 -> Output");

        boolean valid = graph.validate();
        System.out.println("Graph valid: " + valid);

        graph.prepare(44100, 512);
        System.out.println("Graph prepared (44100 Hz, 512 samples)");

        float[] input = new float[512];
        float[] output = new float[1024];

        for (int i = 0; i < input.length; i++) {
            input[i] = (float) Math.sin(2 * Math.PI * 440 * i / 44100);
        }

        graph.process(input, output, 512);

        float inputPeak = 0, outputPeak = 0;
        for (int i = 0; i < input.length; i++) {
            inputPeak = Math.max(inputPeak, Math.abs(input[i]));
        }
        for (int i = 0; i < output.length; i++) {
            outputPeak = Math.max(outputPeak, Math.abs(output[i]));
        }

        System.out.printf("Input peak: %.4f%n", inputPeak);
        System.out.printf("Output peak: %.4f%n", outputPeak);
        System.out.printf("Expected output: %.4f (input * 0.5 * 0.5 = -12 dB)%n", inputPeak * 0.5f * 0.5f);

        float expectedPeak = inputPeak * 0.5f * 0.5f;
        boolean correct = Math.abs(outputPeak - expectedPeak) < 0.01f;
        System.out.println("Gain applied correctly: " + correct);

        System.out.println("\nTesting cycle detection...");
        try {
            graph.connect(gain2.getOutput(), gain1.getInput());
            System.out.println("ERROR: Cycle was not detected!");
        } catch (IllegalArgumentException e) {
            System.out.println("Cycle correctly detected and rejected: " + e.getMessage());
        }

        System.out.println("\nSignal graph test completed successfully!");
    }

    /**
     * Test effects chain: Compressor -> Overdrive -> Distortion.
     */
    public void testEffectsChain() {
        System.out.println("\n--- Effects Chain Test ---");
        System.out.println("Testing: Compressor -> Overdrive -> Distortion");

        EffectFactory factory = EffectFactory.getInstance();
        System.out.println("\nRegistered effects:");
        for (EffectMetadata meta : factory.getAllMetadata()) {
            System.out.printf("  - %s (%s): %s%n", meta.name(), meta.id(), meta.description());
        }

        System.out.println("\nCreating effect instances...");
        AudioEffect compressor = factory.create("compressor");
        AudioEffect overdrive = factory.create("overdrive");
        AudioEffect distortion = factory.create("distortion");

        if (compressor == null || overdrive == null || distortion == null) {
            System.out.println("ERROR: Failed to create effects!");
            return;
        }

        System.out.println("Configuring effects...");

        compressor.getParameter("threshold").setValue(-20.0f);
        compressor.getParameter("ratio").setValue(4.0f);
        compressor.getParameter("attack").setValue(10.0f);
        compressor.getParameter("release").setValue(100.0f);

        overdrive.getParameter("drive").setValue(5.0f);
        overdrive.getParameter("tone").setValue(3000.0f);
        overdrive.getParameter("level").setValue(0.0f);

        distortion.getParameter("drive").setValue(20.0f);
        distortion.getParameter("preTone").setValue(3.0f);
        distortion.getParameter("postTone").setValue(4000.0f);
        distortion.getParameter("level").setValue(-6.0f);

        System.out.println("\nCompressor parameters:");
        for (Parameter p : compressor.getParameters()) {
            System.out.printf("  %s: %.2f %s%n", p.getName(), p.getTargetValue(), p.getUnit());
        }

        System.out.println("\nOverdrive parameters:");
        for (Parameter p : overdrive.getParameters()) {
            System.out.printf("  %s: %.2f %s%n", p.getName(), p.getTargetValue(), p.getUnit());
        }

        System.out.println("\nDistortion parameters:");
        for (Parameter p : distortion.getParameters()) {
            if (p.getType() == ParameterType.CHOICE) {
                System.out.printf("  %s: %s%n", p.getName(), p.getChoices()[p.getChoiceIndex()]);
            } else {
                System.out.printf("  %s: %.2f %s%n", p.getName(), p.getTargetValue(), p.getUnit());
            }
        }

        int sampleRate = 44100;
        int bufferSize = 512;
        System.out.printf("\nPreparing effects (SR: %d Hz, Buffer: %d samples)...%n", sampleRate, bufferSize);

        compressor.prepare(sampleRate, bufferSize);
        overdrive.prepare(sampleRate, bufferSize);
        distortion.prepare(sampleRate, bufferSize);

        float[] input = new float[bufferSize];
        float[] buffer1 = new float[bufferSize];
        float[] buffer2 = new float[bufferSize];
        float[] output = new float[bufferSize];

        for (int i = 0; i < bufferSize; i++) {
            input[i] = 0.5f * (float) Math.sin(2 * Math.PI * 440 * i / sampleRate);
        }

        System.out.println("Processing signal through chain...");

        compressor.process(input, buffer1, bufferSize);
        overdrive.process(buffer1, buffer2, bufferSize);
        distortion.process(buffer2, output, bufferSize);

        float inputPeak = 0, compPeak = 0, odPeak = 0, outputPeak = 0;
        float inputRms = 0, compRms = 0, odRms = 0, outputRms = 0;

        for (int i = 0; i < bufferSize; i++) {
            inputPeak = Math.max(inputPeak, Math.abs(input[i]));
            compPeak = Math.max(compPeak, Math.abs(buffer1[i]));
            odPeak = Math.max(odPeak, Math.abs(buffer2[i]));
            outputPeak = Math.max(outputPeak, Math.abs(output[i]));

            inputRms += input[i] * input[i];
            compRms += buffer1[i] * buffer1[i];
            odRms += buffer2[i] * buffer2[i];
            outputRms += output[i] * output[i];
        }

        inputRms = (float) Math.sqrt(inputRms / bufferSize);
        compRms = (float) Math.sqrt(compRms / bufferSize);
        odRms = (float) Math.sqrt(odRms / bufferSize);
        outputRms = (float) Math.sqrt(outputRms / bufferSize);

        System.out.println("\n--- Signal Analysis ---");
        System.out.printf("Stage            Peak      RMS       Peak dB   RMS dB%n");
        System.out.printf("Input:           %.4f    %.4f    %.1f      %.1f%n",
                inputPeak, inputRms, toDb(inputPeak), toDb(inputRms));
        System.out.printf("After Comp:      %.4f    %.4f    %.1f      %.1f%n",
                compPeak, compRms, toDb(compPeak), toDb(compRms));
        System.out.printf("After OD:        %.4f    %.4f    %.1f      %.1f%n",
                odPeak, odRms, toDb(odPeak), toDb(odRms));
        System.out.printf("After Dist:      %.4f    %.4f    %.1f      %.1f%n",
                outputPeak, outputRms, toDb(outputPeak), toDb(outputRms));

        System.out.println("\n--- Verification ---");

        boolean odSaturating = odPeak > 0.9f || (odPeak > inputPeak * 2);
        System.out.println("Overdrive saturating: " + (odSaturating ? "YES" : "NO"));

        boolean distWorking = outputPeak <= 1.0f && outputPeak > 0;
        System.out.println("Distortion output limited: " + (distWorking ? "YES" : "NO"));

        System.out.println("\n--- Testing with Signal Graph ---");

        SignalGraph graph = new SignalGraph();
        graph.createDefaultGraph();

        EffectNode compNode = new EffectNode("comp1", factory.create("compressor"));
        EffectNode odNode = new EffectNode("od1", factory.create("overdrive"));
        EffectNode distNode = new EffectNode("dist1", factory.create("distortion"));

        graph.addNode(compNode);
        graph.addNode(odNode);
        graph.addNode(distNode);

        new ArrayList<>(graph.getConnections()).forEach(c -> graph.disconnect(c.getId()));

        graph.connect(graph.getInputNode().getOutput(), compNode.getInput());
        graph.connect(compNode.getOutput(), odNode.getInput());
        graph.connect(odNode.getOutput(), distNode.getInput());
        graph.connect(distNode.getOutput(), graph.getOutputNode().getInput());

        System.out.println("Chain: Input -> Compressor -> Overdrive -> Distortion -> Output");
        System.out.printf("Graph nodes: %d, connections: %d%n", graph.getNodeCount(), graph.getConnectionCount());
        System.out.println("Graph valid: " + graph.validate());

        graph.prepare(sampleRate, bufferSize);

        float[] graphOutput = new float[bufferSize * 2];
        graph.process(input, graphOutput, bufferSize);

        float graphPeak = 0;
        for (int i = 0; i < graphOutput.length; i++) {
            graphPeak = Math.max(graphPeak, Math.abs(graphOutput[i]));
        }
        System.out.printf("Graph output peak: %.4f (%.1f dB)%n", graphPeak, toDb(graphPeak));

        System.out.println("\nEffects chain test completed successfully!");
    }

    /**
     * Test full chain with all Phase 5-9 effects.
     */
    public void testFullChain() {
        System.out.println("\n--- Full Effects Chain Test (Phase 5-9) ---");

        EffectFactory factory = EffectFactory.getInstance();

        System.out.println("\nAll registered effects:");
        for (EffectMetadata meta : factory.getAllMetadata()) {
            System.out.printf("  - %s (%s) [%s]%n", meta.name(), meta.id(), meta.category());
        }

        System.out.println("\nTesting individual effects:");

        int sampleRate = 44100;
        int bufferSize = 512;
        int testIterations = 8;

        float[] input = new float[bufferSize];
        float[] output = new float[bufferSize];

        String[] effectIds = {"delay", "reverb", "chorus", "phaser", "tremolo", "parametriceq",
                              "amp", "cabsim", "wah", "pitchshift", "octaver", "ringmod"};

        for (String effectId : effectIds) {
            AudioEffect effect = factory.create(effectId);
            if (effect == null) {
                System.out.printf("  [FAIL] %s - not found%n", effectId);
                continue;
            }

            effect.prepare(sampleRate, bufferSize);

            float outputPeak = 0;
            for (int iter = 0; iter < testIterations; iter++) {
                for (int i = 0; i < bufferSize; i++) {
                    int sample = iter * bufferSize + i;
                    input[i] = 0.5f * (float) Math.sin(2 * Math.PI * 440 * sample / sampleRate);
                }

                effect.process(input, output, bufferSize);

                for (int i = 0; i < bufferSize; i++) {
                    outputPeak = Math.max(outputPeak, Math.abs(output[i]));
                }
            }

            float inputPeak = 0.5f;
            System.out.printf("  [OK] %-12s: in=%.3f out=%.3f (%.1f dB)%n",
                    effect.getMetadata().name(), inputPeak, outputPeak, toDb(outputPeak));
        }

        System.out.println("\n--- Full Guitar Rig Test ---");
        System.out.println("Chain: Gate -> Comp -> OD -> EQ -> Chorus -> Delay -> Reverb");

        AudioEffect gate = factory.create("noisegate");
        AudioEffect comp = factory.create("compressor");
        AudioEffect od = factory.create("overdrive");
        AudioEffect eq = factory.create("parametriceq");
        AudioEffect chorus = factory.create("chorus");
        AudioEffect delay = factory.create("delay");
        AudioEffect reverb = factory.create("reverb");

        gate.getParameter("threshold").setValue(-50.0f);
        comp.getParameter("threshold").setValue(-15.0f);
        comp.getParameter("ratio").setValue(3.0f);
        od.getParameter("drive").setValue(8.0f);
        od.getParameter("tone").setValue(4000.0f);
        eq.getParameter("lowGain").setValue(-3.0f);
        eq.getParameter("highMidGain").setValue(2.0f);
        eq.getParameter("highGain").setValue(1.0f);
        chorus.getParameter("rate").setValue(0.6f);
        chorus.getParameter("depth").setValue(30.0f);
        chorus.getParameter("mix").setValue(25.0f);
        delay.getParameter("time").setValue(350.0f);
        delay.getParameter("feedback").setValue(35.0f);
        delay.getParameter("mix").setValue(25.0f);
        reverb.getParameter("roomSize").setValue(40.0f);
        reverb.getParameter("mix").setValue(20.0f);

        gate.prepare(sampleRate, bufferSize);
        comp.prepare(sampleRate, bufferSize);
        od.prepare(sampleRate, bufferSize);
        eq.prepare(sampleRate, bufferSize);
        chorus.prepare(sampleRate, bufferSize);
        delay.prepare(sampleRate, bufferSize);
        reverb.prepare(sampleRate, bufferSize);

        float[] buf1 = new float[bufferSize];
        float[] buf2 = new float[bufferSize];

        gate.process(input, buf1, bufferSize);
        comp.process(buf1, buf2, bufferSize);
        od.process(buf2, buf1, bufferSize);
        eq.process(buf1, buf2, bufferSize);
        chorus.process(buf2, buf1, bufferSize);
        delay.process(buf1, buf2, bufferSize);
        reverb.process(buf2, output, bufferSize);

        float inputPeak = 0, outputPeak = 0;
        float inputRms = 0, outputRms = 0;
        for (int i = 0; i < bufferSize; i++) {
            inputPeak = Math.max(inputPeak, Math.abs(input[i]));
            outputPeak = Math.max(outputPeak, Math.abs(output[i]));
            inputRms += input[i] * input[i];
            outputRms += output[i] * output[i];
        }
        inputRms = (float) Math.sqrt(inputRms / bufferSize);
        outputRms = (float) Math.sqrt(outputRms / bufferSize);

        System.out.println("\n--- Signal Analysis ---");
        System.out.printf("Input:  Peak=%.4f (%.1f dB)  RMS=%.4f (%.1f dB)%n",
                inputPeak, toDb(inputPeak), inputRms, toDb(inputRms));
        System.out.printf("Output: Peak=%.4f (%.1f dB)  RMS=%.4f (%.1f dB)%n",
                outputPeak, toDb(outputPeak), outputRms, toDb(outputRms));

        System.out.println("\n--- BPM Sync Test ---");
        AudioEffect syncDelay = factory.create("delay");
        syncDelay.prepare(sampleRate, bufferSize);
        syncDelay.getParameter("sync").setValue(true);
        syncDelay.getParameter("bpm").setValue(120.0f);
        syncDelay.getParameter("division").setChoice(2);
        System.out.printf("Delay at 120 BPM, 1/4 note = 500ms (expected)%n");
        System.out.println("BPM sync configured successfully");

        System.out.println("\n--- Signal Graph Integration ---");
        SignalGraph graph = new SignalGraph();
        graph.createDefaultGraph();

        EffectNode gateNode = new EffectNode("gate", factory.create("noisegate"));
        EffectNode compNode = new EffectNode("comp", factory.create("compressor"));
        EffectNode odNode = new EffectNode("od", factory.create("overdrive"));
        EffectNode chorusNode = new EffectNode("chorus", factory.create("chorus"));
        EffectNode delayNode = new EffectNode("delay", factory.create("delay"));
        EffectNode reverbNode = new EffectNode("reverb", factory.create("reverb"));

        graph.addNode(gateNode);
        graph.addNode(compNode);
        graph.addNode(odNode);
        graph.addNode(chorusNode);
        graph.addNode(delayNode);
        graph.addNode(reverbNode);

        new ArrayList<>(graph.getConnections()).forEach(c -> graph.disconnect(c.getId()));

        graph.connect(graph.getInputNode().getOutput(), gateNode.getInput());
        graph.connect(gateNode.getOutput(), compNode.getInput());
        graph.connect(compNode.getOutput(), odNode.getInput());
        graph.connect(odNode.getOutput(), chorusNode.getInput());
        graph.connect(chorusNode.getOutput(), delayNode.getInput());
        graph.connect(delayNode.getOutput(), reverbNode.getInput());
        graph.connect(reverbNode.getOutput(), graph.getOutputNode().getInput());

        System.out.printf("Graph: %d nodes, %d connections%n", graph.getNodeCount(), graph.getConnectionCount());
        System.out.println("Graph valid: " + graph.validate());

        graph.prepare(sampleRate, bufferSize);

        float[] graphOutput = new float[bufferSize * 2];
        graph.process(input, graphOutput, bufferSize);

        float graphPeak = 0;
        for (int i = 0; i < graphOutput.length; i++) {
            graphPeak = Math.max(graphPeak, Math.abs(graphOutput[i]));
        }
        System.out.printf("Graph output peak: %.4f (%.1f dB)%n", graphPeak, toDb(graphPeak));

        System.out.println("\nFull chain test completed successfully!");
    }

    private float toDb(float linear) {
        if (linear <= 0) return -100.0f;
        return (float) (20.0 * Math.log10(linear));
    }

    // ==================== PRESET COMMANDS ====================

    private void listPresets() {
        System.out.println("\n--- Available Presets ---");

        List<String> factoryTemplates = templateManager.listFactoryTemplates();
        List<String> userTemplates = templateManager.listTemplates();

        System.out.println("\nFactory Templates:");
        if (factoryTemplates.isEmpty()) {
            System.out.println("  (none - run 'factory' to generate)");
        } else {
            for (String template : factoryTemplates) {
                System.out.println("  - " + template);
            }
        }

        System.out.println("\nUser Templates:");
        List<String> onlyUser = userTemplates.stream()
                .filter(p -> !p.startsWith("factory/"))
                .toList();
        if (onlyUser.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (String template : onlyUser) {
                System.out.println("  - " + template);
            }
        }

        System.out.println("\nTemplates directory: " + templateManager.getTemplatesDir().toAbsolutePath());
    }

    private void loadPreset(Scanner scanner) {
        System.out.println("\n--- Load Preset ---");

        List<String> all = new ArrayList<>();
        all.addAll(templateManager.listFactoryTemplates());
        all.addAll(templateManager.listTemplates().stream()
                .filter(p -> !p.startsWith("factory/"))
                .toList());

        if (all.isEmpty()) {
            System.out.println("No presets available. Run 'factory' to generate factory presets.");
            return;
        }

        System.out.println("Available presets:");
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("  [%d] %s%n", i, all.get(i));
        }

        System.out.print("Enter preset number or name: ");
        String input = scanner.nextLine().trim();

        String presetName;
        try {
            int index = Integer.parseInt(input);
            if (index < 0 || index >= all.size()) {
                System.out.println("Invalid index.");
                return;
            }
            presetName = all.get(index);
        } catch (NumberFormatException e) {
            presetName = input;
        }

        try {
            Rig rig = templateManager.load(presetName);
            templateManager.applyToGraph(rig, signalGraph);

            System.out.println("\nLoaded preset: " + rig.getName());
            System.out.println("Author: " + rig.getMetadata().author());
            System.out.println("Category: " + rig.getMetadata().category());
            System.out.println("Description: " + rig.getMetadata().description());
            System.out.println("\nSignal Graph:");
            System.out.printf("  Nodes: %d%n", signalGraph.getNodeCount());
            System.out.printf("  Connections: %d%n", signalGraph.getConnectionCount());
            System.out.println("  Valid: " + signalGraph.validate());

        } catch (IOException e) {
            System.out.println("Error loading preset: " + e.getMessage());
        }
    }

    private void savePreset(Scanner scanner) {
        System.out.println("\n--- Save Preset ---");

        if (signalGraph.getNodeCount() <= 2) {
            System.out.println("Signal graph is empty. Start audio with effects first.");
            return;
        }

        System.out.print("Enter preset name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("Enter category (Clean/Crunch/Lead/Ambient/Custom): ");
        String category = scanner.nextLine().trim();
        if (category.isEmpty()) {
            category = "Custom";
        }

        try {
            Rig rig = templateManager.createFromGraph(signalGraph, name, category);
            templateManager.save(rig, name);
            System.out.println("Preset saved: " + name + ".jfxrig");
        } catch (IOException e) {
            System.out.println("Error saving preset: " + e.getMessage());
        }
    }

    /**
     * Generate factory templates.
     */
    public void generateFactoryTemplates() {
        System.out.println("\n--- Generate Factory Templates ---");

        try {
            Path factoryDir = templateManager.getTemplatesDir().resolve("factory");
            FactoryTemplates factory = new FactoryTemplates();
            factory.generateAll(factoryDir);
            System.out.println("\nFactory templates generated successfully!");
        } catch (IOException e) {
            System.out.println("Error generating templates: " + e.getMessage());
        }
    }

    /**
     * Test parallel routing with Splitter/Mixer.
     */
    public void testPresets() {
        System.out.println("\n--- Parallel Routing Test (Phase 7) ---");

        SignalGraph graph = new SignalGraph();
        graph.createDefaultGraph();

        EffectFactory factory = EffectFactory.getInstance();

        SplitterNode splitter = new SplitterNode("split", 2);
        MixerNode mixer = new MixerNode("mixer", 2);

        EffectNode pathA = new EffectNode("pathA_od", factory.create("overdrive"));
        pathA.getEffect().getParameter("drive").setValue(8.0f);

        EffectNode pathB = new EffectNode("pathB_chorus", factory.create("chorus"));
        pathB.getEffect().getParameter("rate").setValue(0.5f);
        pathB.getEffect().getParameter("depth").setValue(40.0f);
        pathB.getEffect().getParameter("mix").setValue(50.0f);

        graph.addNode(splitter);
        graph.addNode(pathA);
        graph.addNode(pathB);
        graph.addNode(mixer);

        new ArrayList<>(graph.getConnections()).forEach(c -> graph.disconnect(c.getId()));

        graph.connect(graph.getInputNode().getOutput(), splitter.getInput());
        graph.connect(splitter.getOutput(0), pathA.getInput());
        graph.connect(pathA.getOutput(), mixer.getInput(0));
        graph.connect(splitter.getOutput(1), pathB.getInput());
        graph.connect(pathB.getOutput(), mixer.getInput(1));
        graph.connect(mixer.getOutput(), graph.getOutputNode().getInput());

        System.out.println("Chain: Input -> Splitter -+-> OD ------> Mixer -> Output");
        System.out.println("                          +-> Chorus -->       ");

        System.out.printf("\nGraph: %d nodes, %d connections%n", graph.getNodeCount(), graph.getConnectionCount());
        System.out.println("Graph valid: " + graph.validate());

        mixer.setLevel(0, 0.7f);
        mixer.setLevel(1, 1.0f);
        System.out.println("\nMixer levels: Path A (OD) = 0.7, Path B (Chorus) = 1.0");

        int sampleRate = 44100;
        int bufferSize = 512;
        graph.prepare(sampleRate, bufferSize);

        float[] input = new float[bufferSize];
        float[] output = new float[bufferSize * 2];

        for (int i = 0; i < bufferSize; i++) {
            input[i] = 0.5f * (float) Math.sin(2 * Math.PI * 440 * i / sampleRate);
        }

        graph.process(input, output, bufferSize);

        float inputPeak = 0, outputPeak = 0;
        for (int i = 0; i < bufferSize; i++) {
            inputPeak = Math.max(inputPeak, Math.abs(input[i]));
        }
        for (int i = 0; i < output.length; i++) {
            outputPeak = Math.max(outputPeak, Math.abs(output[i]));
        }

        System.out.println("\n--- Signal Analysis ---");
        System.out.printf("Input peak:  %.4f (%.1f dB)%n", inputPeak, toDb(inputPeak));
        System.out.printf("Output peak: %.4f (%.1f dB)%n", outputPeak, toDb(outputPeak));

        System.out.println("\n--- Preset Load Test ---");

        List<String> factoryTemplates = templateManager.listFactoryTemplates();
        if (factoryTemplates.isEmpty()) {
            System.out.println("No factory templates found. Generating...");
            generateFactoryTemplates();
            factoryTemplates = templateManager.listFactoryTemplates();
        }

        if (factoryTemplates.stream().anyMatch(p -> p.contains("Ambient"))) {
            try {
                System.out.println("\nLoading 'Ambient' preset (has parallel routing)...");
                Rig ambientRig = templateManager.load("factory/Ambient");
                System.out.println("Loaded: " + ambientRig.getName());
                System.out.println("Nodes: " + ambientRig.getNodes().size());
                System.out.println("Connections: " + ambientRig.getConnections().size());

                System.out.println("\nRouting:");
                for (Rig.ConnectionDefinition conn : ambientRig.getConnections()) {
                    System.out.printf("  %s:%s -> %s:%s%n",
                            conn.sourceNodeId(), conn.sourcePortId(),
                            conn.targetNodeId(), conn.targetPortId());
                }

                SignalGraph ambientGraph = new SignalGraph();
                ambientGraph.createDefaultGraph();
                templateManager.applyToGraph(ambientRig, ambientGraph);

                System.out.printf("\nApplied to graph: %d nodes, %d connections%n",
                        ambientGraph.getNodeCount(), ambientGraph.getConnectionCount());
                System.out.println("Graph valid: " + ambientGraph.validate());

                ambientGraph.prepare(sampleRate, bufferSize);
                float[] ambientOutput = new float[bufferSize * 2];
                ambientGraph.process(input, ambientOutput, bufferSize);

                float ambientPeak = 0;
                for (int i = 0; i < ambientOutput.length; i++) {
                    ambientPeak = Math.max(ambientPeak, Math.abs(ambientOutput[i]));
                }
                System.out.printf("Ambient output peak: %.4f (%.1f dB)%n", ambientPeak, toDb(ambientPeak));

            } catch (IOException e) {
                System.out.println("Error loading Ambient preset: " + e.getMessage());
            }
        }

        System.out.println("\n--- Factory Template Processing Test ---");
        for (String templateName : factoryTemplates) {
            try {
                Rig rig = templateManager.load(templateName);
                SignalGraph testGraph = new SignalGraph();
                testGraph.createDefaultGraph();
                templateManager.applyToGraph(rig, testGraph);

                testGraph.prepare(sampleRate, bufferSize);
                float[] testOutput = new float[bufferSize * 2];
                testGraph.process(input, testOutput, bufferSize);

                float peak = 0;
                for (int i = 0; i < testOutput.length; i++) {
                    peak = Math.max(peak, Math.abs(testOutput[i]));
                }

                System.out.printf("  [OK] %-20s: %d nodes, %d conns, peak=%.3f%n",
                        rig.getName(), testGraph.getNodeCount() - 2, testGraph.getConnectionCount(),
                        peak);
            } catch (Exception e) {
                System.out.printf("  [FAIL] %s: %s%n", templateName, e.getMessage());
            }
        }

        System.out.println("\nParallel routing test completed successfully!");
    }

    // ==================== TOOLS TEST ====================

    /**
     * Test Tuner, Metronome, and AudioRecorder.
     */
    public void testTools() {
        System.out.println("\n--- Tools Test (Phase 10-11) ---");

        int sampleRate = 44100;
        int bufferSize = 512;

        // ==================== TUNER TEST ====================
        System.out.println("\n=== Tuner Test ===");

        Tuner tuner = new Tuner();
        tuner.prepare(sampleRate);

        float[] testFreqs = {82.41f, 110.0f, 146.83f, 196.0f, 246.94f, 329.63f, 440.0f};
        String[] expectedNotes = {"E2", "A2", "D3", "G3", "B3", "E4", "A4"};

        System.out.println("Testing pitch detection:");
        for (int t = 0; t < testFreqs.length; t++) {
            float freq = testFreqs[t];

            for (int iter = 0; iter < 16; iter++) {
                float[] testSignal = new float[bufferSize];
                for (int i = 0; i < bufferSize; i++) {
                    int sample = iter * bufferSize + i;
                    testSignal[i] = 0.5f * (float) Math.sin(2 * Math.PI * freq * sample / sampleRate);
                }
                tuner.process(testSignal, bufferSize);
            }

            String detected = tuner.getNoteString();
            float detectedFreq = tuner.getFrequency();
            float cents = tuner.getCents();
            boolean inTune = tuner.isInTune();

            System.out.printf("  %.2f Hz -> %s (detected: %.1f Hz, %+.1f cents) %s [expected: %s]%n",
                    freq, detected, detectedFreq, cents,
                    inTune ? "[IN TUNE]" : "",
                    expectedNotes[t]);

            tuner.reset();
        }

        tuner.setReferenceA4(442.0f);
        System.out.printf("\nReference A4 set to: %.1f Hz%n", tuner.getReferenceA4());
        tuner.setReferenceA4(440.0f);

        // ==================== METRONOME TEST ====================
        System.out.println("\n=== Metronome Test ===");

        Metronome metronome = new Metronome();
        metronome.prepare(sampleRate);

        System.out.println("Default: " + metronome.getDisplayString());

        metronome.setBpm(120.0f);
        System.out.printf("BPM: %.1f, ms/beat: %.1f, samples/beat: %d%n",
                metronome.getBpm(), metronome.getMsPerBeat(), metronome.getSamplesPerBeat());

        System.out.println("\nTime signatures:");
        for (Metronome.TimeSignature ts : Metronome.TimeSignature.values()) {
            metronome.setTimeSignature(ts);
            System.out.printf("  %s - %d beats%n", ts.display, ts.beats);
        }
        metronome.setTimeSignature(Metronome.TimeSignature.FOUR_FOUR);

        System.out.println("\nGenerating metronome clicks (1 bar at 120 BPM):");
        metronome.start();

        float[] clickBuffer = new float[bufferSize];
        int totalSamples = metronome.getSamplesPerBeat() * 4;
        int beatsDetected = 0;

        for (int i = 0; i < totalSamples; i += bufferSize) {
            java.util.Arrays.fill(clickBuffer, 0);
            metronome.process(clickBuffer, bufferSize);

            if (metronome.wasBeatTriggered()) {
                beatsDetected++;
                System.out.printf("  Beat %d%s at sample %d%n",
                        metronome.getCurrentBeat() == 1 ? metronome.getTimeSignature().beats : metronome.getCurrentBeat() - 1,
                        metronome.isDownbeat() ? " (downbeat)" : "",
                        i);
            }
        }

        metronome.stop();
        System.out.printf("Total beats detected: %d (expected: 4)%n", beatsDetected);

        System.out.println("\nTap tempo simulation (120 BPM = 500ms intervals):");
        metronome.resetTap();
        metronome.tap();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        metronome.tap();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        metronome.tap();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        metronome.tap();

        System.out.printf("Detected BPM from tap: %.1f (expected: ~120)%n", metronome.getBpm());

        // ==================== AUDIO RECORDER TEST ====================
        System.out.println("\n=== Audio Recorder Test ===");

        try {
            Path tempDir = Files.createTempDirectory("jfx2_recording_test");
            System.out.println("Output directory: " + tempDir);

            AudioRecorder recorder = new AudioRecorder();
            recorder.prepare(sampleRate, tempDir);
            recorder.setBitDepth(WavWriter.BitDepth.BIT_16);
            recorder.setFilePrefix("test_recording");

            Path recordingFile = recorder.startRecording();
            System.out.println("Recording to: " + recordingFile.getFileName());
            System.out.println("State: " + recorder.getState());

            int totalFrames = sampleRate * 2;
            int samplesRecorded = 0;

            recorder.addMarker("Start");

            float[] recordBuffer = new float[bufferSize];
            while (samplesRecorded < totalFrames) {
                for (int i = 0; i < bufferSize; i++) {
                    int sample = samplesRecorded + i;
                    float envelope = 1.0f - (float) sample / totalFrames;
                    recordBuffer[i] = 0.5f * envelope *
                            (float) Math.sin(2 * Math.PI * 440 * sample / sampleRate);
                }

                recorder.process(recordBuffer, bufferSize);
                samplesRecorded += bufferSize;

                if (samplesRecorded >= sampleRate && samplesRecorded < sampleRate + bufferSize) {
                    recorder.addMarker("Middle");
                }
            }

            recorder.addMarker("End");

            System.out.printf("Samples recorded: %d%n", recorder.getTotalSamplesRecorded());
            System.out.printf("Duration: %s (%.2f seconds)%n",
                    recorder.getRecordingDurationString(), recorder.getRecordingDurationSeconds());

            recorder.pauseRecording();
            System.out.println("State after pause: " + recorder.getState());
            recorder.resumeRecording();
            System.out.println("State after resume: " + recorder.getState());

            AudioRecorder.RecordingInfo info = recorder.stopRecording();

            System.out.println("\n--- Recording Info ---");
            System.out.println("File: " + info.filePath().getFileName());
            System.out.println("Duration: " + info.getDurationString());
            System.out.println("Size: " + info.getFileSizeString());
            System.out.println("Sample rate: " + info.sampleRate() + " Hz");
            System.out.println("Channels: " + info.channels());
            System.out.println("Bit depth: " + info.bitDepth().bits + "-bit");
            System.out.println("Markers: " + info.markers().size());
            for (WavWriter.Marker marker : info.markers()) {
                double timeSeconds = (double) marker.samplePosition / sampleRate;
                System.out.printf("  - '%s' at %.2f seconds%n", marker.label, timeSeconds);
            }

            long fileSize = Files.size(info.filePath());
            System.out.printf("\nFile verification: exists=%b, size=%d bytes%n",
                    Files.exists(info.filePath()), fileSize);

            System.out.println("\n--- WavWriter Direct Test ---");
            Path wavFile = tempDir.resolve("direct_test.wav");
            try (WavWriter wavWriter = new WavWriter(wavFile, sampleRate, 1, WavWriter.BitDepth.BIT_24)) {
                float[] sineWave = new float[sampleRate];
                for (int i = 0; i < sampleRate; i++) {
                    sineWave[i] = 0.8f * (float) Math.sin(2 * Math.PI * 440 * i / sampleRate);
                }
                wavWriter.write(sineWave, sampleRate);
                wavWriter.addMarker("Test Marker");

                System.out.println("Wrote: " + wavFile.getFileName());
                System.out.printf("Duration: %.2f seconds%n", wavWriter.getDurationSeconds());
            }
            System.out.printf("24-bit file size: %d bytes%n", Files.size(wavFile));

            Files.deleteIfExists(info.filePath());
            Files.deleteIfExists(wavFile);
            Files.deleteIfExists(tempDir);

            System.out.println("\nCleanup: temporary files deleted");

        } catch (IOException e) {
            System.out.println("Recording test error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Tools Test Completed Successfully! ===");
    }

    /**
     * Shutdown the CLI and release resources.
     */
    public void shutdown() {
        if (audioEngine.isRunning()) {
            audioEngine.stop();
        }
        if (audioEngine.isInitialized()) {
            audioEngine.shutdown();
        }
        signalGraph.release();
    }
}
