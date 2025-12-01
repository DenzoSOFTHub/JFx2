package it.denzosoft.jfx2;

import it.denzosoft.jfx2.cli.JFx2Cli;
import it.denzosoft.jfx2.ui.MainFrame;

/**
 * JFx2 Guitar Multi-Effects Processor
 *
 * <p>Main entry point for the application.
 * Launches either the GUI (default) or CLI based on command line arguments.</p>
 */
public class JFx2 {

    public static void main(String[] args) {
        // Check for CLI mode flags
        if (args.length > 0) {
            String flag = args[0];

            // Help flag
            if (flag.equals("--help") || flag.equals("-h")) {
                printUsage();
                return;
            }

            // CLI mode requested
            if (flag.equals("--cli") || flag.startsWith("--test") || flag.equals("--generate-factory")) {
                printBanner();

                JFx2Cli cli = new JFx2Cli();

                if (flag.equals("--cli")) {
                    cli.run();
                } else {
                    cli.executeCommand(flag);
                }
                return;
            }

            // Unknown flag
            System.out.println("Unknown option: " + flag);
            printUsage();
            System.exit(1);
        }

        // Default: Launch GUI
        System.out.println("Starting JFx2 GUI...");
        MainFrame.launch();
    }

    private static void printBanner() {
        System.out.println("===========================================");
        System.out.println("  JFx2 Guitar Multi-Effects Processor");
        System.out.println("  Version 1.0 - CLI Mode");
        System.out.println("===========================================");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("JFx2 Guitar Multi-Effects Processor");
        System.out.println();
        System.out.println("Usage: java -jar JFx2.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  (no args)           Launch GUI (default)");
        System.out.println("  --cli               Run interactive CLI");
        System.out.println("  --test-graph        Test signal graph");
        System.out.println("  --test-effects      Test effects chain");
        System.out.println("  --test-full         Test full audio chain");
        System.out.println("  --test-presets      Test preset system");
        System.out.println("  --test-tools        Test tuner/metronome/recorder");
        System.out.println("  --generate-factory  Generate factory presets");
        System.out.println("  --help, -h          Show this help");
    }
}
