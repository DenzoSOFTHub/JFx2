# UC05 - User Interface

## Panoramica
Interazione utente con l'interfaccia grafica: canvas, controlli, navigazione.

---

## UC05.1 - Layout Principale Applicazione

### Struttura UI
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  MENU BAR                                                                    │
│  File  Edit  View  Rig  Help                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  TOOLBAR                                                                     │
│  [New] [Open] [Save] | [Undo] [Redo] | [Zoom -] [100%] [Zoom +] | [Settings]│
├─────────────┬───────────────────────────────────────────────────────────────┤
│             │                                                               │
│  PALETTE    │                    CANVAS AREA                                │
│  ─────────  │                   (Signal Flow Editor)                        │
│             │                                                               │
│  [Search]   │    ┌─────┐      ┌──────────┐      ┌─────────┐                │
│             │    │INPUT├──────┤DISTORTION├──────┤  DELAY  ├────┐           │
│  ▼ Dynamics │    └─────┘      └──────────┘      └─────────┘    │           │
│    Compressor                                                  │           │
│    Gate     │                                      ┌───────────┴─┐         │
│             │                                      │   OUTPUT    │         │
│  ▼ Drive    │                                      └─────────────┘         │
│    Overdrive│                                                               │
│    Distort  │                                                               │
│             │                                                               │
│  ▼ Modula.  │                                                               │
│    Chorus   │                                                               │
│    Phaser   │                                                               │
│             │                                                               │
│  ▼ Time     │                                                               │
│    Delay    │                                                               │
│    Reverb   │                                                               │
│             │                                                               │
│  ▼ Utility  │                                                               │
│    Splitter │                                                               │
│    Mixer    │                                                               │
│                                                                             │
├─────────────┴───────────────────────────────────────────────────────────────┤
│  PARAMETER PANEL (Context-Sensitive)                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ DISTORTION - "Heavy Drive"                              [Bypass] [X]   ││
│  │                                                                         ││
│  │   GAIN        TONE        LEVEL       MODE                              ││
│  │   ◯           ◯           ◯          [Modern ▼]                         ││
│  │  7.5         5.2         6.0                                            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────────────────┤
│  STATUS BAR: CPU: 12% | Latency: 5.3ms | 48kHz/128 | [▓▓▓░░] In [▓▓▓▓░] Out│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## UC05.2 - Drag & Drop da Palette

| Campo | Valore |
|-------|--------|
| **ID** | UC05.2 |
| **Nome** | Drag & Drop da Palette |
| **Attore** | Utente |

### Flusso Principale
1. Utente espande categoria nella palette
2. Utente inizia drag su effetto desiderato
3. Sistema mostra ghost semi-trasparente
4. Utente trascina sul canvas
5. Sistema mostra:
   - Posizione con snap a griglia (20px)
   - Indicatore validità posizione
6. Utente rilascia mouse
7. Sistema crea blocco effetto
8. Sistema seleziona nuovo blocco
9. Sistema mostra Parameter Panel per il blocco

### Feedback Visivo
| Stato | Indicazione |
|-------|-------------|
| Drag attivo | Cursore: grab, ghost trasparente |
| Posizione valida | Outline verde |
| Sopra connessione | Highlight connessione (inserimento) |

---

## UC05.3 - Selezionare Elementi

| Campo | Valore |
|-------|--------|
| **ID** | UC05.3 |
| **Nome** | Selezionare Elementi |
| **Attore** | Utente |

### Modalità di Selezione
| Azione | Comportamento |
|--------|---------------|
| Click su blocco | Seleziona singolo (deseleziona altri) |
| Shift+Click | Aggiunge/rimuove da selezione |
| Click su connessione | Seleziona connessione |
| Drag su area vuota | Marquee selection (rettangolo) |
| Ctrl+A | Seleziona tutto |
| Esc | Deseleziona tutto |

### Feedback Selezione
- Blocco selezionato: bordo blu 2px, glow
- Connessione selezionata: colore evidenziato, spessore maggiore
- Selezione multipla: tutti evidenziati

---

## UC05.4 - Spostare Blocchi

| Campo | Valore |
|-------|--------|
| **ID** | UC05.4 |
| **Nome** | Spostare Blocchi |
| **Attore** | Utente |

### Flusso Principale
1. Utente seleziona uno o più blocchi
2. Utente inizia drag su blocco selezionato
3. Sistema sposta tutti i blocchi selezionati
4. Sistema aggiorna connessioni in tempo reale
5. Utente rilascia
6. Sistema applica snap a griglia
7. Sistema registra operazione per undo

### Vincoli
- Blocchi INPUT e OUTPUT: spostabili
- Snap a griglia: 20px (configurabile)
- Shift+Drag: movimento libero (no snap)

---

## UC05.5 - Zoom e Pan Canvas

| Campo | Valore |
|-------|--------|
| **ID** | UC05.5 |
| **Nome** | Zoom e Pan Canvas |
| **Attore** | Utente |

### Controlli Zoom
| Azione | Comportamento |
|--------|---------------|
| Mouse wheel | Zoom centrato su cursore |
| Ctrl + / Ctrl - | Zoom incrementale |
| Ctrl 0 | Reset a 100% |
| Ctrl 1 | Fit all (mostra tutto) |
| Ctrl F | Fit selection |

### Range Zoom
- Minimo: 25%
- Massimo: 400%
- Default: 100%

### Controlli Pan
| Azione | Comportamento |
|--------|---------------|
| Middle mouse drag | Pan libero |
| Space + left drag | Pan libero |
| Arrow keys | Pan incrementale (50px) |
| Minimap click | Centra su punto |

---

## UC05.6 - Interagire con Parametri

| Campo | Valore |
|-------|--------|
| **ID** | UC05.6 |
| **Nome** | Interagire con Parametri |
| **Attore** | Utente |

### Controlli Disponibili

#### Rotary Knob
```
      ╭───╮
     ╱  │  ╲      ← Indicatore posizione
    │   │   │
    │   ●   │     ← Drag verticale per ruotare
    │       │
     ╲     ╱
      ╰───╯
      7.5 dB      ← Valore formattato
```

| Azione | Comportamento |
|--------|---------------|
| Drag verticale | Cambia valore |
| Shift + Drag | Cambio fine (1/10) |
| Double-click | Reset a default |
| Right-click | Menu contestuale |
| Scroll wheel | Incremento/decremento |
| Click su valore | Input numerico diretto |

#### Slider
- Orizzontale o verticale
- Drag per cambiare valore
- Click sulla track: jump to position

#### Toggle/Switch
- Click per toggle on/off
- Stato visivo chiaro (LED acceso/spento)

#### Dropdown
- Click per aprire menu
- Selezione opzione

---

## UC05.7 - Undo/Redo

| Campo | Valore |
|-------|--------|
| **ID** | UC05.7 |
| **Nome** | Undo/Redo |
| **Attore** | Utente |
| **Shortcut** | Ctrl+Z (Undo), Ctrl+Shift+Z (Redo) |

### Operazioni Tracciabili
- Aggiunta/rimozione blocco
- Creazione/eliminazione connessione
- Modifica parametri (raggruppate per tempo)
- Spostamento blocchi
- Bypass toggle
- Rinomina blocco

### Comportamento
1. Utente esegue Undo
2. Sistema ripristina stato precedente
3. Sistema anima transizione
4. Sistema mostra toast "Undo: [azione]"

### Stack
- Limite: 100 operazioni
- Compressione: modifiche parametri entro 500ms raggruppate

---

## UC05.8 - Eliminare Elementi

| Campo | Valore |
|-------|--------|
| **ID** | UC05.8 |
| **Nome** | Eliminare Elementi |
| **Attore** | Utente |
| **Shortcut** | Delete, Backspace |

### Flusso Eliminazione Blocco
1. Utente seleziona blocco(i)
2. Utente preme Delete
3. Sistema rimuove blocchi
4. Sistema rimuove connessioni associate
5. Sistema anima fade-out
6. Sistema registra per undo

### Flusso Eliminazione Connessione
1. Utente seleziona connessione
2. Utente preme Delete
3. Sistema rimuove solo connessione

### Smart Delete
Se blocco ha 1 input E 1 output:
- Sistema offre "Reconnect predecessors to successors?"
- Se sì: bypass automatico

### Vincoli
- INPUT e OUTPUT: non eliminabili

---

## UC05.9 - Context Menu

| Campo | Valore |
|-------|--------|
| **ID** | UC05.9 |
| **Nome** | Context Menu |
| **Attore** | Utente |
| **Trigger** | Right-click |

### Menu su Blocco
```
┌─────────────────────┐
│ Bypass         B    │
│ ─────────────────── │
│ Duplicate      Ctrl+D│
│ Rename         F2   │
│ ─────────────────── │
│ Cut            Ctrl+X│
│ Copy           Ctrl+C│
│ Delete         Del  │
│ ─────────────────── │
│ Properties...       │
└─────────────────────┘
```

### Menu su Connessione
```
┌─────────────────────┐
│ Insert Effect... ▶  │
│ ─────────────────── │
│ Delete         Del  │
└─────────────────────┘
```

### Menu su Canvas (area vuota)
```
┌─────────────────────┐
│ Add Effect...   ▶   │
│ ─────────────────── │
│ Paste          Ctrl+V│
│ ─────────────────── │
│ Select All     Ctrl+A│
│ ─────────────────── │
│ Fit All        Ctrl+1│
└─────────────────────┘
```

---

## UC05.10 - Keyboard Shortcuts

### Shortcuts Globali
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
| Esc | Deseleziona / Annulla operazione |
| Space | Play/Stop (se implementato) |

### Shortcuts Canvas
| Shortcut | Azione |
|----------|--------|
| Ctrl++ | Zoom in |
| Ctrl+- | Zoom out |
| Ctrl+0 | Zoom 100% |
| Ctrl+1 | Fit all |
| Ctrl+F | Fit selection |
| Arrow keys | Pan canvas |

### Shortcuts Editing
| Shortcut | Azione |
|----------|--------|
| B | Toggle bypass blocco selezionato |
| F2 | Rinomina blocco |
| Ctrl+D | Duplica selezione |
| Ctrl+C | Copia |
| Ctrl+X | Taglia |
| Ctrl+V | Incolla |

---

## UC05.11 - Status Bar

### Contenuto
```
CPU: 23% | Latency: 5.3ms | 48kHz/128 | [████░░░░] In | [██████░░] Out | Rig: MyTone.jfxrig
```

### Elementi
| Elemento | Descrizione |
|----------|-------------|
| CPU | Percentuale uso audio thread |
| Latency | Latenza round-trip stimata |
| Sample Rate/Buffer | Configurazione audio attiva |
| Input Meter | Livello input (peak) |
| Output Meter | Livello output (peak) |
| Rig Name | Nome file corrente (* se modificato) |

### Aggiornamento
- Metriche audio: ogni 100ms
- Nome rig: su modifica/salvataggio

---

## UC05.12 - Minimap (Grafi Complessi)

| Campo | Valore |
|-------|--------|
| **ID** | UC05.12 |
| **Nome** | Minimap Navigation |
| **Attore** | Utente |
| **Trigger** | Grafo > 10 blocchi |

### Comportamento
```
┌─────────────────┐
│ ┌───┐     ┌───┐│
│ │   │─────│   ││
│ └───┘     └───┘│
│      ┌─────────┤
│      │ VIEWPORT││  ← Rettangolo viewport
│      └─────────┤
│ ┌───┐     ┌───┐│
│ │   │─────│   ││
│ └───┘     └───┘│
└─────────────────┘
```

### Interazione
| Azione | Comportamento |
|--------|---------------|
| Click su minimap | Centra viewport su punto |
| Drag rettangolo viewport | Pan canvas |
| Double-click | Fit all |

---

## Interfacce Tecniche

### CanvasController Class
```java
public class CanvasController implements MouseListener, MouseMotionListener,
                                         MouseWheelListener, KeyListener {
    private SignalFlowPanel canvas;
    private double zoom = 1.0;
    private double panX, panY;
    private Set<String> selectedNodes;

    // Mouse handling
    void mousePressed(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void mouseReleased(MouseEvent e);
    void mouseMoved(MouseEvent e);
    void mouseWheelMoved(MouseWheelEvent e);
    void keyPressed(KeyEvent e);

    // View control
    void setZoom(double zoom);
    void setPan(double x, double y);
    void fitAll();
    void fitSelection();

    // Selection
    void selectNode(String nodeId);
    void selectConnection(String connectionId);
    void clearSelection();
    Set<String> getSelectedNodes();
}
```

### CommandHistory Class
```java
public class CommandHistory {
    private Deque<Command> undoStack;
    private Deque<Command> redoStack;
    private static final int MAX_HISTORY = 100;

    void execute(Command command);
    void undo();
    void redo();
    boolean canUndo();
    boolean canRedo();
    String getUndoDescription();
    String getRedoDescription();
}

public interface Command {
    void execute();
    void undo();
    String getDescription();
}
```

### SignalFlowPanel Class (Swing)
```java
public class SignalFlowPanel extends JPanel {
    private SignalGraph model;
    private CanvasController controller;
    private AffineTransform viewTransform;

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // Apply view transform (zoom/pan)
        g2d.setTransform(viewTransform);
        // Render grid, blocks, connections
        renderGrid(g2d);
        renderConnections(g2d);
        renderBlocks(g2d);
        renderSelection(g2d);
    }

    void setModel(SignalGraph model);
    void repaintCanvas();
}
```

### JRotaryKnob Class (Custom Swing Component)
```java
public class JRotaryKnob extends JComponent {
    private double value;
    private double min, max;
    private String label;
    private List<ChangeListener> listeners;

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // Draw knob background, indicator, value
    }

    // Mouse drag changes value
    private class KnobMouseHandler extends MouseAdapter {
        void mouseDragged(MouseEvent e) {
            // Vertical drag = value change
        }
    }

    public void setValue(double value);
    public double getValue();
    public void addChangeListener(ChangeListener l);
}
```

### MainFrame Layout (Swing)
```java
public class MainFrame extends JFrame {
    public MainFrame() {
        setLayout(new BorderLayout());

        // Toolbar (NORTH)
        add(createToolbar(), BorderLayout.NORTH);

        // Palette (WEST)
        add(new EffectPalettePanel(), BorderLayout.WEST);

        // Canvas (CENTER)
        add(new SignalFlowPanel(), BorderLayout.CENTER);

        // Parameter Panel (SOUTH)
        add(new ParameterPanel(), BorderLayout.SOUTH);
    }
}
```
