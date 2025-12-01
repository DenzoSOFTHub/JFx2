# UC01 - Core Audio Engine

## Panoramica
Gestione dell'input/output audio in tempo reale con latenza minima.

---

## UC01.1 - Inizializzare Audio Engine

| Campo | Valore |
|-------|--------|
| **ID** | UC01.1 |
| **Nome** | Inizializzare Audio Engine |
| **Attore** | Sistema (all'avvio) |
| **Pre-condizioni** | Applicazione in fase di avvio |
| **Post-condizioni** | Audio engine pronto per processing |

### Flusso Principale
1. Sistema rileva dispositivi audio disponibili (input/output) via AudioSystem
2. Sistema seleziona configurazione:
   - Sample rate: 44.1kHz (standard)
   - Buffer size: 512 samples (default, configurabile)
   - Bit depth: 16-bit I/O, 32-bit float interno
3. Sistema inizializza Java Sound API (TargetDataLine/SourceDataLine)
4. Sistema pre-alloca tutti i buffer necessari
5. Sistema crea processing thread
6. Sistema segnala "ready" all'applicazione

### Flussi Alternativi
- **3a. Dispositivo non disponibile**: Mostra errore, permetti selezione alternativa

### Requisiti Non-Funzionali
- Tempo inizializzazione: < 2 secondi
- Priorità: qualità audio sopra latenza

---

## UC01.2 - Processare Audio Real-Time

| Campo | Valore |
|-------|--------|
| **ID** | UC01.2 |
| **Nome** | Processare Audio Real-Time |
| **Attore** | Sistema (audio callback) |
| **Trigger** | Buffer audio disponibile |
| **Frequenza** | ~375 volte/secondo @48kHz/128 samples |

### Flusso Principale
1. Audio callback riceve buffer input dal driver
2. Sistema converte samples da int16/int24 a float32
3. Sistema passa buffer al Signal Flow Graph
4. Signal Flow Graph processa attraverso tutti i nodi
5. Sistema converte output da float32 a formato driver
6. Sistema invia buffer output al driver

### Vincoli Real-Time (CRITICI)
- **VIETATO**: Allocazioni di memoria
- **VIETATO**: Operazioni I/O (file, network)
- **VIETATO**: Lock/synchronized blocks
- **VIETATO**: Eccezioni non gestite
- **OBBLIGATORIO**: Completare entro 2.67ms @48kHz/128

### Metriche
- CPU usage per callback: < 50% del tempo disponibile
- Dropout/glitch: 0 in condizioni normali

---

## UC01.3 - Configurare Dispositivi Audio

| Campo | Valore |
|-------|--------|
| **ID** | UC01.3 |
| **Nome** | Configurare Dispositivi Audio |
| **Attore** | Utente |
| **Pre-condizioni** | Audio engine inizializzato |

### Flusso Principale
1. Utente apre Settings > Audio
2. Sistema mostra:
   - Lista dispositivi input
   - Lista dispositivi output
   - Opzioni sample rate (44.1/48/96 kHz)
   - Opzioni buffer size (64/128/256/512)
   - Latenza stimata
3. Utente modifica configurazione
4. Sistema testa nuova configurazione
5. Se test OK: applica configurazione
6. Sistema mostra latenza effettiva

### Flussi Alternativi
- **4a. Test fallisce**: Ripristina configurazione precedente, mostra errore

---

## UC01.4 - Monitorare Performance Audio

| Campo | Valore |
|-------|--------|
| **ID** | UC01.4 |
| **Nome** | Monitorare Performance Audio |
| **Attore** | Sistema (background) |

### Flusso Principale
1. Sistema monitora continuamente:
   - CPU usage del processing thread
   - Numero di buffer underrun/overrun
   - Latenza media
   - Peak level input/output
2. Sistema aggiorna status bar ogni 100ms
3. Se CPU > 80%: warning visivo
4. Se dropout rilevato: log + notifica

### Output Visualizzato
```
CPU: 23% | Latency: 5.3ms | 48kHz/128 | [====----] In | [======--] Out
```

---

## Interfacce Tecniche

### AudioEngine Class
```java
public class AudioEngine {
    private TargetDataLine inputLine;
    private SourceDataLine outputLine;
    private Thread processingThread;
    private volatile boolean running;

    // Pre-allocated buffers
    private byte[] inputByteBuffer;
    private byte[] outputByteBuffer;
    private float[] inputFloatBuffer;
    private float[] outputFloatBuffer;

    public void initialize(AudioConfig config);
    public void start(AudioCallback callback);
    public void stop();
    public void shutdown();
    public List<Mixer.Info> getInputDevices();
    public List<Mixer.Info> getOutputDevices();
    public AudioMetrics getMetrics();
}
```

### AudioCallback Interface
```java
@FunctionalInterface
public interface AudioCallback {
    void process(float[] input, float[] output, int frameCount);
}
```

### AudioConfig Record
```java
public record AudioConfig(
    int sampleRate,      // 44100 (default)
    int bufferSize,      // 256, 512, 1024
    int inputChannels,   // 1 (mono guitar)
    int outputChannels,  // 2 (stereo)
    Mixer.Info inputDevice,
    Mixer.Info outputDevice
) {}
```

### Note Java Sound API
```java
// Formato audio standard
AudioFormat format = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    44100,      // sample rate
    16,         // bit depth
    1,          // channels (mono input)
    2,          // frame size
    44100,      // frame rate
    false       // little-endian
);

// Apertura linee
DataLine.Info inputInfo = new DataLine.Info(TargetDataLine.class, format);
inputLine = (TargetDataLine) AudioSystem.getLine(inputInfo);
inputLine.open(format, bufferSize * 2);
```
