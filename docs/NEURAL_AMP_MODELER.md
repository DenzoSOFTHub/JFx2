# Neural Amp Modeler

Modellazione di amplificatori ed effetti tramite reti neurali.

## Panoramica

Il Neural Amp Modeler permette di catturare le caratteristiche sonore di amplificatori, pedali e qualsiasi altra trasformazione audio usando una rete neurale. Il sistema apprende la trasformazione confrontando un segnale "dry" (non processato) con il corrispondente segnale "wet" (processato attraverso l'equipaggiamento da modellare).

### Componenti

| File | Descrizione |
|------|-------------|
| `nn/NeuralNetwork.java` | Rete neurale feedforward |
| `nn/DenseLayer.java` | Layer fully-connected con backpropagation |
| `nn/ActivationFunction.java` | Funzioni di attivazione (tanh, ReLU, etc.) |
| `tools/NeuralNetworkTrainer.java` | Tool per l'addestramento |
| `effects/impl/NeuralAmpEffect.java` | Effetto audio che usa il modello |

---

## Architettura della Rete Neurale

### Struttura

La rete è di tipo **feedforward** (Multi-Layer Perceptron) progettata per processare audio in tempo reale:

```
Input Window (64 samples)
        │
        ▼
┌───────────────────┐
│  Dense Layer 1    │  64 → 32 neuroni (tanh)
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  Dense Layer 2    │  32 → 16 neuroni (tanh)
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  Dense Layer 3    │  16 → 8 neuroni (tanh)
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  Output Layer     │  8 → 1 neurone (lineare)
└───────────────────┘
        │
        ▼
  Output Sample
```

### Sliding Window

Per catturare il contesto temporale (essenziale per effetti come distorsione e compressione), la rete usa una **finestra scorrevole** di campioni:

- **Input**: ultimi N campioni (default: 64 = ~1.5ms a 44.1kHz)
- **Output**: 1 campione predetto

Questo permette alla rete di "vedere" la storia recente del segnale, catturando:
- Dinamiche di attacco/rilascio
- Comportamento non-lineare dipendente dal livello
- Interazioni tra armoniche successive

### Funzioni di Attivazione

| Funzione | Formula | Uso |
|----------|---------|-----|
| `LINEAR` | f(x) = x | Layer di output |
| `TANH` | f(x) = tanh(x) | Default per layer nascosti |
| `RELU` | f(x) = max(0, x) | Alternativa veloce |
| `LEAKY_RELU` | f(x) = x if x>0, else 0.01x | Evita neuroni "morti" |
| `SIGMOID` | f(x) = 1/(1+e^-x) | Range [0,1] |
| `SOFTPLUS` | f(x) = ln(1+e^x) | ReLU smooth |

### Inizializzazione dei Pesi

Usa **Xavier/Glorot initialization**:

```
scale = sqrt(2 / (input_size + output_size))
weight = random_gaussian() * scale
```

Questa inizializzazione mantiene la varianza dei gradienti stabile attraverso i layer.

---

## Processo di Addestramento

### Algoritmo

1. **Caricamento Audio**: I file WAV dry e wet vengono caricati e convertiti in array di float normalizzati [-1, 1]

2. **Preparazione Dati**: Per ogni posizione i nel segnale:
   - Input: campioni da i a i+windowSize-1 del segnale dry
   - Target: campione i+windowSize-1 del segnale wet

3. **Training Loop**:
   ```
   per ogni epoch:
       per ogni chunk:
           per ogni sample nel chunk:
               1. Forward pass: calcola output
               2. Calcola loss (MSE)
               3. Backward pass: calcola gradienti
               4. Accumula gradienti

               se batch completo:
                   5. Aggiorna pesi con Adam
                   6. Azzera gradienti

           se loss < threshold:
               interrompi (convergenza)

       se nessun miglioramento per N epochs:
           interrompi (early stopping)
   ```

### Ottimizzatore Adam

L'addestramento usa **Adam** (Adaptive Moment Estimation):

```
m = β1 * m + (1 - β1) * gradient          # Momento primo
v = β2 * v + (1 - β2) * gradient²         # Momento secondo

m_hat = m / (1 - β1^t)                     # Bias correction
v_hat = v / (1 - β2^t)

weight -= lr * m_hat / (sqrt(v_hat) + ε)
```

Parametri default:
- β1 = 0.9
- β2 = 0.999
- ε = 1e-8

### Chunk-Based Training

Per gestire file audio lunghi senza esaurire la memoria:

- Il training procede per **chunk** (default: 44100 samples = 1 secondo)
- Permette di interrompere il training se la rete converge prima di processare tutto il file
- Mantiene la stessa efficacia del training su tutto il dataset

### Early Stopping

Il training si interrompe automaticamente quando:
- La loss scende sotto `convergenceThreshold` (default: 0.0001)
- Non c'è miglioramento per `convergencePatience` epochs consecutive (default: 5)

---

## Formato File .jfxnn

Il modello addestrato viene salvato in formato testo:

```
JFXNN1                          # Magic number + versione
64                              # inputWindowSize
1                               # outputSize
4                               # numero di layer

64,32,TANH                      # Layer 0: input→hidden1
0.123,0.456,...                 # Pesi riga 0 (32 valori)
0.789,0.012,...                 # Pesi riga 1
...                             # (32 righe di pesi)
0.111,0.222,...                 # Bias (32 valori)

32,16,TANH                      # Layer 1: hidden1→hidden2
...                             # Pesi e bias

16,8,TANH                       # Layer 2: hidden2→hidden3
...

8,1,LINEAR                      # Layer 3: hidden3→output
...
```

---

## Guida all'Uso

### Preparazione dei Dati

1. **Registra il segnale dry**: Suona una chitarra (o altra sorgente) e registra il segnale diretto (DI box o linea)

2. **Registra il segnale wet**: Contemporaneamente, registra lo stesso segnale passato attraverso l'amp/effetto da modellare

3. **Requisiti**:
   - Stesso numero di campioni in entrambi i file
   - Perfettamente allineati temporalmente
   - Formato WAV (8/16/24/32 bit, mono o stereo)
   - Durata consigliata: 30-120 secondi
   - Contenuto vario: accordi, singole note, dinamiche diverse

### Addestramento da Linea di Comando

```bash
# Sintassi base
java -cp target/classes it.denzosoft.jfx2.tools.NeuralNetworkTrainer \
    dry.wav wet.wav model.jfxnn

# Con opzioni
java -cp target/classes it.denzosoft.jfx2.tools.NeuralNetworkTrainer \
    dry.wav wet.wav model.jfxnn \
    --window 64 \
    --hidden 32,16,8 \
    --lr 0.001 \
    --batch 64 \
    --epochs 100 \
    --threshold 0.0001
```

**Opzioni disponibili**:

| Opzione | Default | Descrizione |
|---------|---------|-------------|
| `--window <n>` | 64 | Dimensione finestra input (campioni) |
| `--hidden <sizes>` | 32,16,8 | Dimensioni layer nascosti |
| `--lr <rate>` | 0.001 | Learning rate |
| `--batch <n>` | 64 | Batch size |
| `--epochs <n>` | 100 | Numero massimo di epochs |
| `--threshold <val>` | 0.0001 | Soglia convergenza (MSE) |

### Addestramento Programmatico

```java
import it.denzosoft.jfx2.tools.NeuralNetworkTrainer;
import it.denzosoft.jfx2.nn.ActivationFunction;

NeuralNetworkTrainer trainer = new NeuralNetworkTrainer()
    .setInputWindowSize(64)
    .setHiddenSizes(32, 16, 8)
    .setHiddenActivation(ActivationFunction.TANH)
    .setLearningRate(0.001f)
    .setBatchSize(64)
    .setMaxEpochs(100)
    .setConvergenceThreshold(0.0001f)
    .setVerbose(true);

float finalLoss = trainer.train("dry.wav", "wet.wav", "model.jfxnn");
System.out.println("Training completato con loss: " + finalLoss);
```

### Uso dell'Effetto

```java
import it.denzosoft.jfx2.effects.impl.NeuralAmpEffect;

// Creazione e caricamento modello
NeuralAmpEffect effect = new NeuralAmpEffect();
boolean success = effect.loadModel("model.jfxnn");

if (success) {
    // Configurazione
    effect.setInputGain(0.0f);   // dB
    effect.setOutputGain(0.0f);  // dB
    effect.setMix(100.0f);       // %

    // Preparazione
    effect.prepare(44100, 256);

    // Processing
    float[] input = ...;
    float[] output = new float[256];
    effect.process(input, output, 256);
}
```

### Uso nel Graph

L'effetto `neuralamp` può essere aggiunto al signal graph come qualsiasi altro effetto:

```java
SignalGraph graph = new SignalGraph();

// Aggiungi nodi
AudioInputEffect input = (AudioInputEffect) graph.addNode("audioinput");
NeuralAmpEffect amp = (NeuralAmpEffect) graph.addNode("neuralamp");
AudioOutputEffect output = (AudioOutputEffect) graph.addNode("audiooutput");

// Carica modello
amp.loadModel("fender_twin.jfxnn");

// Connetti
graph.connect(input.getId(), amp.getId());
graph.connect(amp.getId(), output.getId());

// Avvia
graph.prepare(44100, 256);
graph.start();
```

---

## Configurazioni Consigliate

### Overdrive/Distortion

```bash
--window 64 --hidden 32,16,8 --lr 0.001 --epochs 100
```

Cattura bene la non-linearità della saturazione.

### Amplificatore Valvolare

```bash
--window 128 --hidden 64,32,16 --lr 0.0005 --epochs 200
```

Finestra più ampia per catturare le dinamiche delle valvole.

### Compressore

```bash
--window 256 --hidden 64,32,16 --lr 0.0005 --epochs 150
```

Finestra grande per catturare attack/release.

### Delay/Reverb

Non adatto per effetti a lungo delay. La rete è progettata per trasformazioni istantanee o quasi-istantanee (< 5ms).

---

## Troubleshooting

### La loss non diminuisce

- Aumenta il learning rate (prova 0.01)
- Verifica che i file audio siano allineati
- Controlla che i file non siano identici (dry != wet)

### La loss è molto alta (> 0.1)

- I file potrebbero non essere allineati temporalmente
- La trasformazione potrebbe essere troppo complessa
- Prova una rete più grande (`--hidden 64,32,16`)

### Il modello produce artefatti

- La rete potrebbe essere troppo piccola
- Prova una finestra più grande (`--window 128`)
- Aumenta gli epochs per un training più lungo

### Out of memory

- Riduci il batch size (`--batch 32`)
- I chunk sono già gestiti automaticamente

---

## Limitazioni

1. **Solo trasformazioni istantanee**: Non adatto per delay > 5ms o reverb lunghi

2. **Mono processing**: Il segnale stereo viene mixato a mono per il processing

3. **Latenza**: La finestra di input introduce una latenza pari a windowSize campioni

4. **Generalizzazione**: Il modello funziona meglio su segnali simili a quelli usati per il training

5. **Risorse CPU**: L'inferenza richiede ~1000 moltiplicazioni per campione (architettura default)

---

## Esempi Pratici

### Modellare un Tube Screamer

```bash
# 1. Registra 60 secondi di chitarra pulita
#    Output: guitar_dry.wav

# 2. Registra lo stesso take attraverso il Tube Screamer
#    Output: guitar_ts.wav

# 3. Addestra il modello
java -cp target/classes it.denzosoft.jfx2.tools.NeuralNetworkTrainer \
    guitar_dry.wav guitar_ts.wav tubescreamer.jfxnn \
    --window 64 --hidden 32,16,8 --epochs 100

# Output atteso:
# === Neural Network Trainer ===
# Loading dry audio: guitar_dry.wav
#   Samples: 2646000
# Loading wet audio: guitar_ts.wav
#   Samples: 2646000
#
# Neural Network Summary:
#   Input window: 64 samples
#   Output: 1 sample(s)
#   Layers:
#     [0] 64 -> 32 (TANH)
#     [1] 32 -> 16 (TANH)
#     [2] 16 -> 8 (TANH)
#     [3] 8 -> 1 (LINEAR)
#   Total parameters: 2825
#
# Epoch   1/100 - Loss: 0.045632 - Time: 12.3s
# Epoch   2/100 - Loss: 0.023456 - Time: 24.1s
# ...
# Epoch  47/100 - Loss: 0.000098 - Time: 578.2s
# Converged at epoch 47 (loss: 0.000098)
#
# Training complete. Final loss: 0.000098, Time: 578.2s
# Model saved to: tubescreamer.jfxnn
```

### Usare il Modello nell'Applicazione

1. Avvia JFx2 in modalità GUI
2. Aggiungi un nodo "Neural Amp" al graph
3. Nel pannello parametri, clicca "Load Model"
4. Seleziona `tubescreamer.jfxnn`
5. Regola Input Gain, Output Gain e Mix a piacere
