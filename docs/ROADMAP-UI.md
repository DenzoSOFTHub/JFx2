# JFx2 - Roadmap Implementazione UI Swing

## Panoramica

Roadmap dettagliata per l'implementazione dell'interfaccia grafica in Swing (Fasi 8-12).

**Decisioni Chiave**:
- **Framework**: Swing (built-in JDK, no JavaFX)
- **Priorita**: Aspetto visivo professionale (dark theme)
- **Componenti**: Mix pragmatico (custom per knob, standard per slider/combo)
- **Undo/Redo**: Implementato nella fase finale (12)

---

## Dipendenze

Le fasi 1-7 (core audio) sono **completate**:
- Audio Engine funzionante
- Signal Graph con topological sorting
- 17 effetti implementati
- Preset management con JSON
- CLI di test operativa

---

## FASE 8: Setup Swing e Canvas Base

**Focus**: Finestra applicazione con canvas griglia e dark theme professionale

### Struttura File

```
src/main/java/it/denzosoft/jfx2/ui/
├── MainFrame.java              # JFrame principale
├── theme/
│   └── DarkTheme.java          # Palette colori dark
└── canvas/
    ├── SignalFlowPanel.java    # JPanel canvas
    └── GridRenderer.java       # Rendering griglia
```

### Task Implementativi

| # | Task | Dettaglio |
|---|------|-----------|
| 8.1 | **DarkTheme.java** | Definire palette colori |
| 8.2 | **MainFrame.java** | BorderLayout, dimensione 1400x900 |
| 8.3 | **SignalFlowPanel.java** | JPanel con paintComponent, antialiasing, double buffering |
| 8.4 | **GridRenderer.java** | Griglia 20px con linee sottili, ogni 100px linee piu visibili |
| 8.5 | **Zoom/Pan base** | AffineTransform per zoom (25%-400%), mouse wheel + middle-drag |
| 8.6 | **Menu bar stub** | JMenuBar con File/Edit/View/Rig/Help (azioni vuote per ora) |
| 8.7 | **Toolbar stub** | JToolBar con bottoni iconici (placeholder) |

### Palette Colori DarkTheme

```java
public class DarkTheme {
    // Background
    public static final Color BG_DARK = new Color(0x1e1e1e);
    public static final Color BG_MEDIUM = new Color(0x252526);
    public static final Color BG_LIGHT = new Color(0x2d2d30);

    // Accent
    public static final Color ACCENT_PRIMARY = new Color(0x00a8ff);
    public static final Color ACCENT_SUCCESS = new Color(0x4caf50);
    public static final Color ACCENT_WARNING = new Color(0xff9800);
    public static final Color ACCENT_ERROR = new Color(0xf44336);

    // Text
    public static final Color TEXT_PRIMARY = new Color(0xe0e0e0);
    public static final Color TEXT_SECONDARY = new Color(0x9e9e9e);
    public static final Color TEXT_DISABLED = new Color(0x616161);

    // Canvas
    public static final Color GRID_LINE = new Color(0x2a2a2a);
    public static final Color GRID_LINE_MAJOR = new Color(0x3a3a3a);

    // Blocks
    public static final Color BLOCK_BG = new Color(0x37373d);
    public static final Color BLOCK_BORDER = new Color(0x4a4a4a);
    public static final Color BLOCK_SELECTED = new Color(0x264f78);

    // Ports
    public static final Color PORT_AUDIO = new Color(0x4caf50);
    public static final Color PORT_CONTROL = new Color(0xff9800);

    // Connections
    public static final Color CONNECTION_NORMAL = new Color(0x569cd6);
    public static final Color CONNECTION_SELECTED = new Color(0x00a8ff);
}
```

### Deliverable Fase 8

- [ ] Finestra dark theme funzionante
- [ ] Canvas con griglia 20px
- [ ] Zoom mouse wheel (centrato su cursore)
- [ ] Pan con middle-click drag
- [ ] Menu bar e toolbar strutturati

---

## FASE 9: Rendering Blocchi e Connessioni

**Focus**: Visualizzazione grafo con aspetto professionale

### Struttura File

```
ui/canvas/
├── BlockRenderer.java          # Disegno blocchi
├── PortRenderer.java           # Porte colorate
├── ConnectionRenderer.java     # Curve Bezier
└── CanvasController.java       # Sync model-view
```

### Task Implementativi

| # | Task | Dettaglio |
|---|------|-----------|
| 9.1 | **BlockRenderer** | Blocco 140x80px, bordo arrotondato 8px, gradient background, shadow |
| 9.2 | **Icone categoria** | Icone per Drive/Modulation/Time/Dynamics/Utility |
| 9.3 | **PortRenderer** | Cerchi 12px: audio=verde, control=arancione, glow su hover |
| 9.4 | **ConnectionRenderer** | CubicCurve2D bezier, gradient colore, spessore 3px |
| 9.5 | **Selezione visiva** | Bordo blu 2px + glow esterno per blocchi selezionati |
| 9.6 | **CanvasController** | Gestione selezione, sincronizzazione con SignalGraph |
| 9.7 | **Hit testing** | Rilevamento click su blocchi/porte/connessioni |

### Design Blocco

```
┌──────────────────────────┐
│ [icon] DISTORTION        │  <- Header con icona + nome
├──────────────────────────┤
│                          │
│  ● ─────────────────── ● │  <- Porte input (sx) / output (dx)
│                          │
│       [BYPASSED]         │  <- Stato bypass (se attivo)
└──────────────────────────┘
   Shadow, rounded corners, gradient

Dimensioni: 140 x 80 px
Corner radius: 8px
Port size: 12px diameter
```

### Connessioni Bezier

```java
// Control points per curva naturale
int dx = Math.abs(endX - startX) / 2;
CubicCurve2D curve = new CubicCurve2D.Double(
    startX, startY,           // P0 start
    startX + dx, startY,      // P1 control
    endX - dx, endY,          // P2 control
    endX, endY                // P3 end
);
```

### Deliverable Fase 9

- [ ] Blocchi con aspetto professionale
- [ ] Connessioni curve eleganti
- [ ] Feedback visivo selezione
- [ ] Hover effects su porte

---

## FASE 10: Drag & Drop Interazione

**Focus**: Creazione e modifica grafo interattiva

### Struttura File

```
ui/
├── palette/
│   ├── EffectPalettePanel.java    # JTree categorizzato
│   └── EffectTreeCellRenderer.java # Rendering custom celle
├── canvas/
│   ├── CanvasMouseHandler.java     # Mouse events
│   ├── ConnectionDragHandler.java  # Drag connessioni
│   └── ConnectionValidator.java    # Validazione real-time
└── dnd/
    └── EffectTransferHandler.java  # Drag & drop
```

### Task Implementativi

| # | Task | Dettaglio |
|---|------|-----------|
| 10.1 | **EffectPalettePanel** | JTree con categorie collassabili, search filter |
| 10.2 | **TreeCellRenderer** | Icone + nomi effetti stilizzati |
| 10.3 | **TransferHandler** | Drag da palette a canvas con ghost semi-trasparente |
| 10.4 | **Snap to grid** | Snap automatico 20px (Shift per movimento libero) |
| 10.5 | **Drag connessioni** | Click porta output -> drag -> rilascio porta input |
| 10.6 | **Validazione** | Highlight verde/rosso durante drag connessione |
| 10.7 | **Spostamento blocchi** | Drag su blocco selezionato muove tutta la selezione |
| 10.8 | **Marquee selection** | Drag su area vuota = selezione rettangolare |
| 10.9 | **Delete** | KeyListener per Delete/Backspace |
| 10.10 | **Input/Output lock** | Blocchi I/O non eliminabili, warning visivo |

### Categorie Palette

```
Effect Palette
├── Dynamics
│   ├── Compressor
│   ├── Noise Gate
│   └── Gain
├── Drive
│   ├── Overdrive
│   ├── Distortion
│   └── Amp Sim
├── Modulation
│   ├── Chorus
│   ├── Phaser
│   ├── Tremolo
│   └── Ring Mod
├── Time
│   ├── Delay
│   └── Reverb
├── EQ & Filter
│   ├── Parametric EQ
│   ├── Wah
│   └── Cabinet Sim
├── Pitch
│   ├── Octaver
│   └── Pitch Shifter
└── Utility
    ├── Splitter
    └── Mixer
```

### Feedback Drag

```
Durante drag connessione:
   ● ─────────── ○    Verde = connessione valida
   ● ─────────── ✕    Rosso = incompatibile

Durante drag blocco:
   [Ghost 50% opacity] + [Outline posizione finale con snap]
```

### Deliverable Fase 10

- [ ] Palette effetti navigabile con categorie
- [ ] Drag & drop da palette a canvas
- [ ] Creazione connessioni via drag
- [ ] Selezione multipla e spostamento
- [ ] Delete con KeyListener

---

## FASE 11: Parameter Panel e Controlli

**Focus**: Editing parametri real-time con controlli professionali

### Struttura File

```
ui/
├── controls/
│   ├── JRotaryKnob.java        # Knob custom (PRIORITARIO)
│   ├── JLedToggle.java         # Toggle con LED
│   └── ParameterBinding.java   # Data binding
├── panels/
│   ├── ParameterPanel.java     # Panel contestuale
│   └── EffectHeaderPanel.java  # Header con nome + bypass
```

### Task Implementativi

| # | Task | Tipo Componente |
|---|------|-----------------|
| 11.1 | **JRotaryKnob** | **Custom** - drag verticale, Shift=fine, double-click=reset |
| 11.2 | **Rendering knob** | Custom Graphics2D - arco indicatore, valore, etichetta |
| 11.3 | **JLedToggle** | Custom semplice - bypass on/off con LED |
| 11.4 | **Slider parametri** | **Standard JSlider** con styling dark |
| 11.5 | **Combo enum** | **Standard JComboBox** per parametri enum |
| 11.6 | **ParameterPanel** | Layout dinamico basato su effetto selezionato |
| 11.7 | **Data binding** | PropertyChangeListener per sync bidirezionale |
| 11.8 | **Input numerico** | Click su valore -> JTextField inline |
| 11.9 | **Bypass su blocco** | Icona bypass cliccabile sul blocco |

### Approccio Pragmatico Componenti

| Tipo Parametro | Componente | Motivo |
|----------------|------------|--------|
| Gain, Tone, Mix, Level | **JRotaryKnob** (custom) | UX professionale per parametri chiave |
| Time, Rate | **JSlider** (standard) | Valori lineari, slider appropriato |
| Bypass, Enabled | **JLedToggle** (custom) | Feedback visivo LED |
| Mode, Type | **JComboBox** (standard) | Selezione enum standard |

### JRotaryKnob Specifiche

```java
public class JRotaryKnob extends JComponent {
    // Dimensioni
    private static final int SIZE = 60;
    private static final int ARC_WIDTH = 6;

    // Range
    private double value;      // Current value
    private double min, max;   // Range
    private double defaultVal; // Double-click reset

    // Interazione
    // - Drag verticale: +1px = +0.5% range
    // - Shift+Drag: +1px = +0.05% range (fine)
    // - Double-click: reset to default
    // - Scroll wheel: increment/decrement

    // Rendering
    // - Background arc (270 gradi, da 135 a 405)
    // - Value arc (proporzionale)
    // - Center indicator dot
    // - Value text sotto
    // - Label sopra
}
```

### Layout Parameter Panel

```
┌────────────────────────────────────────────────────────────────┐
│ [icon] DELAY - "Tape Echo"                [● Bypass] [X Close] │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   TIME        FEEDBACK      MIX          FILTER               │
│    ◯            ◯           ◯          [Warm ▼]               │
│   350ms        45%          30%                               │
│                                                                │
│   └── JSlider ───────────────────────────────────────────────┘│
│        TIME FINE: [===========|=======] 350.0 ms              │
└────────────────────────────────────────────────────────────────┘

Altezza panel: 150-180px
Padding: 16px
Gap tra controlli: 24px
```

### Deliverable Fase 11

- [ ] JRotaryKnob custom funzionante
- [ ] JLedToggle per bypass
- [ ] Parameter panel contestuale
- [ ] Binding bidirezionale model <-> view
- [ ] Modifica parametri in tempo reale senza click

---

## FASE 12: Preset Browser e Finishing

**Focus**: Gestione preset, undo/redo, polish finale

### Struttura File

```
ui/
├── dialogs/
│   ├── PresetBrowserDialog.java   # Browser preset
│   ├── SaveRigDialog.java         # Salvataggio con metadata
│   └── SettingsDialog.java        # Configurazione audio
├── panels/
│   ├── StatusBarPanel.java        # CPU, latency, meters
│   └── MinimapPanel.java          # Navigazione grafi grandi
├── commands/
│   ├── Command.java               # Interface
│   ├── CommandHistory.java        # Undo/Redo stack
│   ├── AddBlockCommand.java
│   ├── DeleteBlockCommand.java
│   ├── MoveBlockCommand.java
│   └── ParameterChangeCommand.java
└── util/
    ├── ToastNotification.java     # Notifiche temporanee
    └── KeyboardShortcuts.java     # InputMap/ActionMap
```

### Task Implementativi

| # | Task | Priorita |
|---|------|----------|
| 12.1 | **PresetBrowserDialog** | Alta - JDialog con JTree categorie + JList preset |
| 12.2 | **Search/filter** | Alta - JTextField per ricerca istantanea |
| 12.3 | **Preview info** | Media - Panel destro con dettagli preset |
| 12.4 | **SaveRigDialog** | Alta - Form con nome, autore, categoria, tags |
| 12.5 | **StatusBarPanel** | Alta - CPU %, latency, sample rate, meters |
| 12.6 | **Meters** | Media - VU meter input/output |
| 12.7 | **CommandHistory** | Alta - Stack 100 operazioni, compressione 500ms |
| 12.8 | **Commands base** | Alta - Add, Delete, Move, Parameter change |
| 12.9 | **Keyboard shortcuts** | Alta - Ctrl+Z, Ctrl+S, Ctrl+N, Delete, B, F2 |
| 12.10 | **Context menus** | Media - JPopupMenu per blocchi/connessioni/canvas |
| 12.11 | **MinimapPanel** | Bassa - Thumbnail navigabile (auto >10 blocchi) |
| 12.12 | **ToastNotification** | Bassa - Feedback azioni con fade |
| 12.13 | **Favorites/rating** | Bassa - Stelle + cuore |

### Command Pattern

```java
public interface Command {
    void execute();
    void undo();
    String getDescription();
}

public class CommandHistory {
    private Deque<Command> undoStack = new ArrayDeque<>();
    private Deque<Command> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 100;

    public void execute(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        trimHistory();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        }
    }
}
```

### Keyboard Shortcuts

| Shortcut | Azione |
|----------|--------|
| Ctrl+N | Nuovo rig |
| Ctrl+O | Apri rig |
| Ctrl+S | Salva |
| Ctrl+Shift+S | Salva come |
| Ctrl+Z | Undo |
| Ctrl+Shift+Z | Redo |
| Ctrl+A | Seleziona tutto |
| Delete | Elimina selezione |
| B | Toggle bypass |
| F2 | Rinomina blocco |
| Ctrl+D | Duplica |
| Esc | Deseleziona |

### Layout Preset Browser

```
┌─────────────────────────────────────────────────────────────────┐
│ Preset Browser                                           [X]    │
├─────────────────────────────────────────────────────────────────┤
│ [Search...                                            ] [★]     │
├──────────────────────┬──────────────────────────────────────────┤
│ Categories           │ Presets                                  │
│ ─────────────────    │ ──────────────────────────────────────   │
│ ▼ All Presets        │ [★] Clean Tone           - Rock         │
│ ▼ By Category        │ [★] Crunch Machine       - Rock         │
│   ▸ Clean            │     Modern Lead          - Metal        │
│   ▸ Rock             │     Ambient Delay        - Ambient      │
│   ▸ Metal            │     ...                                 │
│   ▸ Blues            │                                         │
│   ▸ Ambient          ├──────────────────────────────────────────┤
│ ▼ By Author          │ Preview                                  │
│ ▼ Favorites          │ ──────────────────────────────────────   │
│ ▼ Recently Used      │ Name: Crunch Machine                    │
│                      │ Author: Factory                         │
│                      │ Category: Rock                          │
│                      │ Rating: ★★★★☆                           │
│                      │ Tags: overdrive, classic, british       │
├──────────────────────┴──────────────────────────────────────────┤
│                              [Cancel]  [Load]                   │
└─────────────────────────────────────────────────────────────────┘
```

### Deliverable Fase 12

- [ ] Preset browser completo con search
- [ ] Save dialog con metadata
- [ ] Undo/redo funzionante (100 operazioni)
- [ ] Tutti i keyboard shortcuts
- [ ] Status bar con metriche real-time
- [ ] Context menus
- [ ] Minimap (opzionale)
- [ ] Toast notifications (opzionale)

---

## Riepilogo Complessita

| Fase | Focus | Custom Components | Standard Swing | Complessita |
|------|-------|-------------------|----------------|-------------|
| **8** | Setup + Canvas | DarkTheme, GridRenderer | JFrame, JPanel, JMenuBar | Media |
| **9** | Rendering | Block/Port/ConnectionRenderer | - | Media-Alta |
| **10** | Interazione | MouseHandlers, TransferHandler | JTree | Alta |
| **11** | Controlli | JRotaryKnob, JLedToggle | JSlider, JComboBox | Media |
| **12** | Polish | StatusBar, Commands | JDialog, JPopupMenu | Alta |

---

## Note Tecniche

### Thread Safety

```
UI Thread (EDT)           Audio Thread
     │                         │
     │  PropertyChangeEvent    │
     ├────────────────────────>│  Parameter update
     │                         │  (AtomicDouble)
     │                         │
     │  Repaint request        │
     │<────────────────────────┤  Metrics update
     │                         │  (SwingUtilities.invokeLater)
```

### Performance

- **Canvas**: Double buffering abilitato
- **Repaint**: Solo dirty regions quando possibile
- **Meters**: Update ogni 100ms max
- **Rendering**: Cache per elementi statici (griglia)

### File Preset

I preset usano il formato JSON definito in Fase 7:

```json
{
  "formatVersion": "1.0",
  "metadata": {
    "name": "Crunch Machine",
    "author": "Factory",
    "category": "Rock",
    "tags": ["overdrive", "classic"],
    "rating": 4,
    "favorite": true
  },
  "globalSettings": {
    "bpm": 120,
    "inputGain": 0.0,
    "outputGain": 0.0
  },
  "graph": {
    "nodes": [...],
    "connections": [...]
  },
  "viewState": {
    "zoom": 1.0,
    "panX": 0,
    "panY": 0
  }
}
```

---

## Riferimenti

- **ROADMAP.md**: Roadmap completa (Fasi 1-12)
- **UC05-UserInterface.md**: Specifiche dettagliate UI
- **UC04-PresetManagement.md**: Gestione preset
- **Codice esistente**: `src/main/java/it/denzosoft/jfx2/`
