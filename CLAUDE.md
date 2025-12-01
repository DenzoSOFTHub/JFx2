# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a Maven-based Java project targeting Java 21.

```bash
# Compile the project
mvn compile

# Package as JAR
mvn package

# Run the application
mvn exec:java

# Clean build artifacts
mvn clean
```

## Project Overview

**JFx2** is a guitar multi-effects processor application with node-based signal routing, written in Java.

### Architecture
- **Audio Engine**: Real-time DSP with low-latency audio I/O
- **Signal Graph**: Node-based routing with topological processing order
- **Effects**: Modular effect plugins (distortion, delay, reverb, modulation, etc.)
- **UI**: JavaFX with drag-and-drop signal flow editor

## Project Structure

- **Group ID**: it.denzosoft
- **Artifact ID**: JFx2
- **Main class**: `it.denzosoft.jfx2.JFx2`
- **Source root**: `src/main/java/`

### Package Organization (planned)
```
it.denzosoft.jfx2/
├── audio/      # Audio I/O backend (JavaSound, PortAudio)
├── graph/      # Signal flow graph and routing
├── effects/    # Effect implementations
├── dsp/        # DSP utilities (filters, delay lines, LFO)
├── preset/     # Rig/preset management
└── ui/         # JavaFX user interface
```

## Documentation

- **Roadmap**: `docs/ROADMAP.md` - 12-phase implementation plan
- **Use Cases**: `docs/use-cases/` - Detailed functional specifications
  - UC01: Audio Engine
  - UC02: Signal Routing
  - UC03: Effects System
  - UC04: Preset Management
  - UC05: User Interface

## Key Technical Decisions

- **Audio Backend**: Java Sound API only (javax.sound.sampled) - no external libraries
- **Buffer Size**: 256-512 samples @ 44.1kHz (latency not critical, quality prioritized)
- **UI Framework**: Swing (built-in JDK)
- **Preset Format**: JSON with custom parser (.jfxrig files)
- **DSP**: 32-bit float internal processing
- **Target Platform**: Windows
- **Dependencies**: None - pure Java, no external libraries, no JNI
