# JFx2 - Roadmap Implementazione

## Panoramica
Roadmap in 12 fasi per lo sviluppo dell'applicazione multi-effetti per chitarra.

**Strategia**: Prima il core audio/routing/effetti (Fasi 1-7), poi l'interfaccia grafica (Fasi 8-12).

---

## PARTE A: CORE AUDIO & EFFETTI (Fasi 1-7)

---

### FASE 1: Infrastruttura Audio Base
**Focus**: Setup progetto e audio I/O funzionante

#### Obiettivi
- [ ] Setup progetto Maven con dipendenze
- [ ] Interfaccia AudioBackend astratta
- [ ] Implementazione JavaSound backend (baseline)
- [ ] Audio callback loop funzionante
- [ ] Test: passthrough audio (input → output senza elaborazione)

#### Deliverable
- Audio che passa da input a output
- Latenza misurabile
- Nessun glitch/dropout

#### Use Case Coperti
- UC01.1 - Inizializzare Audio Engine
- UC01.2 - Processare Audio Real-Time (base)

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── audio/
│   ├── AudioBackend.java (interface)
│   ├── AudioCallback.java (interface)
│   ├── AudioConfig.java (record)
│   └── JavaSoundBackend.java
└── JFx2.java (main con test passthrough)
```

---

### FASE 2: Signal Flow Graph
**Focus**: Architettura del grafo di processing

#### Obiettivi
- [ ] Classe SignalGraph
- [ ] Classe ProcessingNode (base)
- [ ] Classe Port (input/output)
- [ ] Classe Connection
- [ ] Algoritmo ordinamento topologico
- [ ] Rilevamento cicli
- [ ] Test: grafo con nodi dummy

#### Deliverable
- Grafo configurabile programmaticamente
- Processing in ordine corretto
- Validazione no-cycle

#### Use Case Coperti
- UC02.1 - Creare Signal Flow Graph
- UC02.2 - Aggiungere Nodo al Grafo
- UC02.3 - Connettere Due Nodi
- UC02.7 - Processare Grafo Audio

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── graph/
│   ├── SignalGraph.java
│   ├── ProcessingNode.java
│   ├── Port.java
│   ├── PortType.java (enum)
│   ├── Connection.java
│   └── GraphValidator.java
```

---

### FASE 3: Sistema Parametri e Effetti Base
**Focus**: Framework per effetti con parametri thread-safe

#### Obiettivi
- [ ] Interfaccia AudioEffect
- [ ] Classe AbstractEffect (base)
- [ ] Sistema parametri con smoothing
- [ ] EffectFactory con registry
- [ ] Primo effetto: **Volume/Gain** (più semplice)
- [ ] Secondo effetto: **Noise Gate**
- [ ] Test: catena Gain → Gate

#### Deliverable
- Framework effetti funzionante
- Parametri modificabili senza click
- Due effetti base funzionanti

#### Use Case Coperti
- UC03.2 - Creare Istanza Effetto
- UC03.3 - Modificare Parametri Effetto
- UC03.4 - Bypassare Effetto

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── effects/
│   ├── AudioEffect.java (interface)
│   ├── AbstractEffect.java
│   ├── EffectFactory.java
│   ├── EffectMetadata.java (record)
│   ├── Parameter.java
│   ├── ParameterType.java (enum)
│   ├── impl/
│   │   ├── GainEffect.java
│   │   └── NoiseGateEffect.java
```

---

### FASE 4: Effetti Distorsione e Dinamica
**Focus**: Effetti gain-based essenziali

#### Obiettivi
- [ ] **Compressor** con attack/release/ratio/threshold
- [ ] **Overdrive** con soft-clipping tanh
- [ ] **Distortion** con hard-clipping e tone control
- [ ] Filtri IIR base (biquad) per tone shaping
- [ ] Test: catena Comp → OD → Dist

#### Deliverable
- 3 nuovi effetti funzionanti
- Suono di saturazione utilizzabile
- Compressore musicale

#### Use Case Coperti
- UC03.5 - Definizione Effetti Base (parziale)

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── effects/impl/
│   ├── CompressorEffect.java
│   ├── OverdriveEffect.java
│   └── DistortionEffect.java
├── dsp/
│   ├── BiquadFilter.java
│   ├── FilterType.java (enum)
│   └── WaveShaper.java
```

---

### FASE 5: Effetti Time-Based
**Focus**: Delay e riverbero

#### Obiettivi
- [ ] Classe DelayLine (circular buffer)
- [ ] **Delay** con feedback, filtering, tempo
- [ ] **Reverb** (algoritmo Freeverb o simile)
- [ ] Interpolazione per delay modulato
- [ ] Test: catena OD → Delay → Reverb

#### Deliverable
- Delay con feedback controllato
- Reverb con decay naturale
- Nessun artifact audio

#### Use Case Coperti
- UC03.5 - Definizione Effetti Base (time-based)

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── effects/impl/
│   ├── DelayEffect.java
│   └── ReverbEffect.java
├── dsp/
│   ├── DelayLine.java
│   ├── AllPassFilter.java
│   └── CombFilter.java
```

---

### FASE 6: Effetti Modulazione e EQ
**Focus**: Modulazioni LFO-based e equalizzazione

#### Obiettivi
- [ ] Classe LFO (sin, tri, square, random)
- [ ] **Chorus** con delay modulato
- [ ] **Phaser** con all-pass filters
- [ ] **Tremolo** (amplitude modulation)
- [ ] **Parametric EQ** (4 bande)
- [ ] Test: full chain con tutti gli effetti

#### Deliverable
- Suite modulazione completa
- EQ parametrico funzionale
- LFO riutilizzabile

#### Use Case Coperti
- UC03.5 - Definizione Effetti Base (modulazione)
- UC03.6 - Effect Processing Pipeline

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── effects/impl/
│   ├── ChorusEffect.java
│   ├── PhaserEffect.java
│   ├── TremoloEffect.java
│   └── ParametricEQEffect.java
├── dsp/
│   └── LFO.java
```

---

### FASE 7: Routing Avanzato e Preset (CLI)
**Focus**: Parallel routing e persistenza preset

#### Obiettivi
- [ ] Nodi utility: **Splitter**, **Mixer**
- [ ] Routing parallelo funzionante
- [ ] Serializzazione preset JSON
- [ ] Caricamento preset da file
- [ ] Salvataggio preset su file
- [ ] CLI per test (load preset, modify params)

#### Deliverable
- Routing serie/parallelo
- File .jfxrig funzionanti
- Preset salvabili e caricabili

#### Use Case Coperti
- UC02.6 - Configurare Routing Parallelo
- UC04.2 - Salvare Rig
- UC04.3 - Caricare Rig

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── effects/impl/
│   ├── SplitterNode.java
│   └── MixerNode.java
├── preset/
│   ├── Rig.java
│   ├── RigMetadata.java
│   ├── PresetManager.java
│   └── PresetSerializer.java
```

---

## PARTE B: INTERFACCIA GRAFICA (Fasi 8-12)

---

### FASE 8: Setup Swing e Canvas Base
**Focus**: Struttura applicazione Swing

#### Obiettivi
- [ ] Main JFrame application window
- [ ] Layout base (BorderLayout)
- [ ] JPanel custom per signal flow canvas
- [ ] Rendering griglia con Graphics2D
- [ ] Zoom e pan base

#### Deliverable
- Finestra applicazione funzionante
- Canvas con griglia
- Zoom/pan con mouse wheel e drag

#### Use Case Coperti
- UC05.1 - Layout Principale Applicazione
- UC05.5 - Zoom e Pan Canvas (base)

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── ui/
│   ├── MainFrame.java (extends JFrame)
│   ├── canvas/
│   │   ├── SignalFlowPanel.java (extends JPanel)
│   │   └── GridRenderer.java
│   └── theme/
│       └── DarkTheme.java
```

---

### FASE 9: Rendering Blocchi e Connessioni
**Focus**: Visualizzazione grafo sul canvas

#### Obiettivi
- [ ] BlockRenderer (disegna blocchi con Graphics2D)
- [ ] PortRenderer con colori per tipo
- [ ] ConnectionRenderer (curve Bezier con CubicCurve2D)
- [ ] Sincronizzazione model → view
- [ ] Selezione visuale blocchi/connessioni

#### Deliverable
- Blocchi visibili sul canvas
- Connessioni renderizzate come curve
- Selezione con feedback visivo

#### Use Case Coperti
- UC05.3 - Selezionare Elementi

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── ui/canvas/
│   ├── BlockRenderer.java
│   ├── PortRenderer.java
│   ├── ConnectionRenderer.java
│   └── CanvasController.java
```

---

### FASE 10: Interazione Drag & Drop
**Focus**: Creazione e modifica grafo via UI

#### Obiettivi
- [ ] Effect Palette (JTree con categorie)
- [ ] Drag & drop da palette a canvas (TransferHandler)
- [ ] Drag connessioni tra porte (MouseListener/MouseMotionListener)
- [ ] Validazione connessioni real-time
- [ ] Spostamento blocchi con snap
- [ ] Delete blocchi e connessioni (KeyListener)

#### Deliverable
- Creazione grafo interattiva
- Connessioni validate
- Editing completo via mouse

#### Use Case Coperti
- UC05.2 - Drag & Drop da Palette
- UC05.4 - Spostare Blocchi
- UC05.8 - Eliminare Elementi

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── ui/
│   ├── palette/
│   │   └── EffectPalettePanel.java (JTree)
│   ├── canvas/
│   │   ├── CanvasMouseHandler.java
│   │   └── ConnectionValidator.java
```

---

### FASE 11: Parameter Panel e Controlli
**Focus**: Editing parametri effetti

#### Obiettivi
- [ ] Parameter Panel (JPanel in SOUTH)
- [ ] RotaryKnob control custom (JComponent + paintComponent)
- [ ] JSlider per parametri lineari
- [ ] JToggleButton per on/off
- [ ] JComboBox per enumerazioni
- [ ] PropertyChangeListener per sync model ↔ view
- [ ] Bypass button su blocchi

#### Deliverable
- Modifica parametri real-time
- Controlli responsive
- Feedback visivo immediato

#### Use Case Coperti
- UC05.6 - Interagire con Parametri
- UC03.3 - Modificare Parametri Effetto (UI)
- UC03.4 - Bypassare Effetto (UI)

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── ui/
│   ├── controls/
│   │   ├── JRotaryKnob.java (extends JComponent)
│   │   ├── JParameterSlider.java
│   │   └── JToggleSwitch.java
│   ├── panels/
│   │   └── ParameterPanel.java (extends JPanel)
```

---

### FASE 12: Preset Browser e Finishing
**Focus**: Gestione preset e polish finale

#### Obiettivi
- [ ] Preset Browser dialog (JDialog con JList/JTree)
- [ ] Categorie e ricerca (JTextField filter)
- [ ] Favorites e rating
- [ ] Save/Save As dialogs (JFileChooser + custom panel)
- [ ] Undo/Redo stack
- [ ] Context menus (JPopupMenu)
- [ ] Keyboard shortcuts (InputMap/ActionMap)
- [ ] Status bar con metriche (JPanel in SOUTH)
- [ ] Minimap per grafi grandi (JPanel con thumbnail)

#### Deliverable
- Applicazione completa e utilizzabile
- Gestione preset intuitiva
- UX polish

#### Use Case Coperti
- UC04.1 - Creare Nuovo Rig (UI)
- UC04.4 - Navigare Preset Browser
- UC04.5 - Gestire Preferiti
- UC04.6 - Valutare Preset
- UC05.7 - Undo/Redo
- UC05.9 - Context Menu
- UC05.10 - Keyboard Shortcuts
- UC05.11 - Status Bar
- UC05.12 - Minimap

#### Struttura Codice
```
src/main/java/it/denzosoft/jfx2/
├── ui/
│   ├── dialogs/
│   │   ├── PresetBrowserDialog.java (extends JDialog)
│   │   ├── SaveRigDialog.java
│   │   └── SettingsDialog.java
│   ├── panels/
│   │   ├── StatusBarPanel.java
│   │   └── MinimapPanel.java
│   ├── commands/
│   │   ├── Command.java (interface)
│   │   ├── CommandHistory.java
│   │   ├── AddBlockCommand.java
│   │   ├── DeleteBlockCommand.java
│   │   └── MoveBlockCommand.java
```

---

## Riepilogo Fasi

| Fase | Nome | Focus | Dipendenze |
|------|------|-------|------------|
| 1 | Infrastruttura Audio | Audio I/O | - |
| 2 | Signal Flow Graph | Routing engine | Fase 1 |
| 3 | Sistema Parametri | Framework effetti | Fase 2 |
| 4 | Distorsione/Dinamica | Effetti gain | Fase 3 |
| 5 | Time-Based | Delay/Reverb | Fase 3 |
| 6 | Modulazione/EQ | Chorus/Phaser/EQ | Fase 3, 5 |
| 7 | Routing Avanzato | Parallel + Preset | Fase 2-6 |
| 8 | JavaFX Setup | UI base | Fase 7 |
| 9 | Rendering Grafo | Visualizzazione | Fase 8 |
| 10 | Drag & Drop | Interazione | Fase 9 |
| 11 | Parameter Panel | Controlli | Fase 10 |
| 12 | Preset Browser | Polish finale | Fase 11 |

---

## Struttura Finale Progetto

```
JFx2/
├── pom.xml
├── CLAUDE.md
├── docs/
│   ├── ROADMAP.md
│   ├── use-cases/
│   │   ├── UC01-AudioEngine.md
│   │   ├── UC02-SignalRouting.md
│   │   ├── UC03-EffectsSystem.md
│   │   ├── UC04-PresetManagement.md
│   │   └── UC05-UserInterface.md
│   └── architecture/
│       └── (diagrammi futuri)
├── src/
│   ├── main/
│   │   ├── java/it/denzosoft/jfx2/
│   │   │   ├── JFx2.java
│   │   │   ├── audio/
│   │   │   ├── graph/
│   │   │   ├── effects/
│   │   │   ├── dsp/
│   │   │   ├── preset/
│   │   │   └── ui/
│   │   └── resources/
│   │       └── styles/
│   └── test/
│       └── java/it/denzosoft/jfx2/
└── presets/
    └── factory/
```

---

## Note Implementative

### Priorità Tecnica
1. **Qualità Audio**: Effetti di alta qualità, priorità su latenza
2. **Stabilità**: Zero glitch in condizioni normali
3. **CPU**: < 30% su hardware modesto
4. **Usabilità**: Workflow intuitivo per chitarristi

### Stack Tecnologico
- Java 21+
- **Swing** (built-in JDK) per UI
- **Java Sound API** (javax.sound.sampled) per audio
- **Parser JSON custom** per preset
- **Nessuna libreria esterna, nessun JNI**

### Target Platform
- Windows

### Testing Strategy
- Unit test per DSP (confronto con valori attesi)
- Integration test per grafo completo
- Test con file audio per qualità effetti
