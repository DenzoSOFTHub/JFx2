# UC02 - Signal Routing

## Panoramica
Gestione del grafo di segnale: nodi, connessioni e flusso audio.

---

## UC02.1 - Creare Signal Flow Graph

| Campo | Valore |
|-------|--------|
| **ID** | UC02.1 |
| **Nome** | Creare Signal Flow Graph |
| **Attore** | Sistema |
| **Trigger** | Nuovo rig o caricamento preset |

### Flusso Principale
1. Sistema crea grafo vuoto
2. Sistema aggiunge nodo INPUT (sorgente audio)
3. Sistema aggiunge nodo OUTPUT (destinazione audio)
4. Sistema connette INPUT -> OUTPUT (bypass default)
5. Sistema valida grafo (no cicli, tutti output connessi)
6. Sistema calcola ordine topologico per processing

### Post-condizioni
- Grafo valido con almeno INPUT e OUTPUT
- Ordine di processing calcolato

---

## UC02.2 - Aggiungere Nodo al Grafo

| Campo | Valore |
|-------|--------|
| **ID** | UC02.2 |
| **Nome** | Aggiungere Nodo al Grafo |
| **Attore** | Utente |
| **Pre-condizioni** | Grafo esistente |

### Tipi di Nodo
| Tipo | Input Ports | Output Ports | Funzione |
|------|-------------|--------------|----------|
| INPUT | 0 | 1 (mono/stereo) | Sorgente audio |
| OUTPUT | 1 (stereo) | 0 | Destinazione audio |
| EFFECT | 1+ | 1+ | Processamento |
| SPLITTER | 1 | 2-4 | Divide segnale |
| MIXER | 2-4 | 1 | Mixa segnali |

### Flusso Principale
1. Utente seleziona tipo effetto dalla palette
2. Utente trascina sul canvas
3. Sistema crea nodo con:
   - ID univoco
   - Porte I/O secondo tipo
   - Parametri con valori default
4. Sistema posiziona nodo (snap to grid)
5. Sistema aggiorna vista canvas

### Flussi Alternativi
- **2a. Drop su connessione esistente**: Inserisce nodo in-line

---

## UC02.3 - Connettere Due Nodi

| Campo | Valore |
|-------|--------|
| **ID** | UC02.3 |
| **Nome** | Connettere Due Nodi |
| **Attore** | Utente |
| **Pre-condizioni** | Almeno 2 nodi nel grafo |

### Flusso Principale
1. Utente inizia drag da porta OUTPUT
2. Sistema mostra preview connessione (curva Bezier)
3. Sistema evidenzia porte INPUT compatibili
4. Utente rilascia su porta INPUT
5. Sistema valida connessione:
   - Tipi compatibili (mono/stereo)
   - Nessun ciclo creato
   - Porta INPUT non già connessa
6. Sistema crea connessione
7. Sistema ricalcola ordine topologico

### Regole di Validazione
```
VALIDA: OUTPUT -> INPUT (direzioni opposte)
VALIDA: MONO -> STEREO (upmix automatico)
VALIDA: STEREO -> MONO (downmix automatico)
INVALIDA: Stesso blocco (self-loop)
INVALIDA: Crea ciclo nel grafo
INVALIDA: INPUT già ha connessione (1 sola ammessa)
```

### Flussi Alternativi
- **5a. Validazione fallisce**: Mostra errore, annulla
- **4a. Rilascio su area vuota**: Mostra menu quick-add

---

## UC02.4 - Rimuovere Connessione

| Campo | Valore |
|-------|--------|
| **ID** | UC02.4 |
| **Nome** | Rimuovere Connessione |
| **Attore** | Utente |

### Flusso Principale
1. Utente seleziona connessione (click sulla curva)
2. Utente preme DELETE
3. Sistema rimuove connessione
4. Sistema ricalcola ordine topologico
5. Sistema aggiorna vista

---

## UC02.5 - Rimuovere Nodo

| Campo | Valore |
|-------|--------|
| **ID** | UC02.5 |
| **Nome** | Rimuovere Nodo |
| **Attore** | Utente |

### Flusso Principale
1. Utente seleziona nodo
2. Utente preme DELETE
3. Sistema rimuove TUTTE le connessioni del nodo
4. Sistema rimuove nodo
5. Sistema ricalcola ordine topologico

### Flussi Alternativi
- **2a. Nodo è INPUT o OUTPUT**: Operazione non permessa

---

## UC02.6 - Configurare Routing Parallelo

| Campo | Valore |
|-------|--------|
| **ID** | UC02.6 |
| **Nome** | Configurare Routing Parallelo |
| **Attore** | Utente |

### Scenario: Wet/Dry Mix
```
         ┌──► [EFFECT] ──┐
INPUT ──►│               ├──► MIXER ──► OUTPUT
         └──► [bypass] ──┘
```

### Flusso Principale
1. Utente aggiunge SPLITTER dopo INPUT
2. Utente connette INPUT -> SPLITTER
3. Utente connette SPLITTER.OUT_A -> EFFECT -> MIXER.IN_A
4. Utente connette SPLITTER.OUT_B -> MIXER.IN_B (dry path)
5. Utente connette MIXER -> OUTPUT
6. Sistema processa entrambi i path in parallelo
7. MIXER somma i segnali con livelli configurabili

### Parametri MIXER
| Parametro | Range | Default | Descrizione |
|-----------|-------|---------|-------------|
| Level A | -inf..+6dB | 0dB | Livello path A |
| Level B | -inf..+6dB | 0dB | Livello path B |
| Pan A | -100..+100 | 0 | Pan path A |
| Pan B | -100..+100 | 0 | Pan path B |

---

## UC02.7 - Processare Grafo Audio

| Campo | Valore |
|-------|--------|
| **ID** | UC02.7 |
| **Nome** | Processare Grafo Audio |
| **Attore** | Sistema (audio thread) |
| **Trigger** | Ogni audio callback |

### Flusso Principale
1. Sistema riceve buffer input
2. Sistema copia input nel buffer del nodo INPUT
3. Per ogni nodo in ordine topologico:
   a. Raccoglie input da nodi predecessori
   b. Esegue processing del nodo
   c. Scrive output nel buffer del nodo
4. Sistema copia buffer nodo OUTPUT all'output

### Algoritmo Ordine Topologico
```
function topologicalSort(graph):
    visited = Set()
    order = List()

    for each node in graph.nodes:
        if node not in visited:
            dfs(node, visited, order)

    return reverse(order)

function dfs(node, visited, order):
    visited.add(node)
    for each successor in node.outputs:
        if successor not in visited:
            dfs(successor, visited, order)
    order.add(node)
```

---

## Interfacce Tecniche

### ProcessingNode Interface
```java
public interface ProcessingNode {
    String getId();
    NodeType getType();
    List<Port> getInputPorts();
    List<Port> getOutputPorts();
    void process(int frameCount);
    float[] getOutputBuffer(String portId);
    void setInputBuffer(String portId, float[] buffer);
}
```

### SignalGraph Class
```java
public class SignalGraph {
    private Map<String, ProcessingNode> nodes;
    private List<Connection> connections;
    private List<ProcessingNode> processingOrder;

    public void addNode(ProcessingNode node);
    public void removeNode(String nodeId);
    public void connect(String sourceNodeId, String sourcePortId,
                       String targetNodeId, String targetPortId);
    public void disconnect(String connectionId);
    public void process(float[] input, float[] output, int frames);
    public boolean validate(); // Check for cycles
    public void rebuildProcessingOrder();
}
```

### Port Class
```java
public class Port {
    private String id;
    private String name;
    private PortDirection direction; // INPUT, OUTPUT
    private PortType type;           // AUDIO_MONO, AUDIO_STEREO
    private ProcessingNode owner;
    private Connection connection;   // null if disconnected

    public boolean canConnectTo(Port other);
    public Point2D getPosition();
}
```
