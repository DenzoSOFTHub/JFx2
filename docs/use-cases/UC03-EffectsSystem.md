# UC03 - Effects System

## Panoramica
Gestione degli effetti audio: creazione, parametri, bypass e processing.

---

## UC03.1 - Catalogo Effetti Disponibili

### Categorie e Effetti

| Categoria | Effetti | Priorità Implementazione |
|-----------|---------|-------------------------|
| **Dynamics** | Compressor, Noise Gate, Limiter | FASE 1 |
| **Gain/Drive** | Clean Boost, Overdrive, Distortion, Fuzz | FASE 1 |
| **EQ/Filter** | Parametric EQ, Graphic EQ, Wah, Auto-Wah | FASE 2 |
| **Modulation** | Chorus, Flanger, Phaser, Tremolo, Vibrato | FASE 2 |
| **Delay** | Digital Delay, Tape Delay, Ping-Pong | FASE 3 |
| **Reverb** | Room, Hall, Plate, Spring | FASE 3 |
| **Pitch** | Octaver, Pitch Shifter, Harmonizer | FASE 4 |
| **Amp Sim** | Preamp Models, Cabinet IR Loader | FASE 4 |
| **Utility** | Splitter, Mixer, Volume, Tuner | FASE 1 |

---

## UC03.2 - Creare Istanza Effetto

| Campo | Valore |
|-------|--------|
| **ID** | UC03.2 |
| **Nome** | Creare Istanza Effetto |
| **Attore** | Utente (via drag-drop) o Sistema (load preset) |

### Flusso Principale
1. Sistema riceve richiesta creazione (tipo effetto)
2. EffectFactory crea istanza appropriata
3. Sistema inizializza parametri a valori default
4. Sistema pre-alloca buffer interni dell'effetto
5. Sistema registra effetto nel grafo

### Factory Pattern
```java
EffectBlock effect = EffectFactory.create("distortion");
// Restituisce DistortionEffect con parametri default
```

---

## UC03.3 - Modificare Parametri Effetto

| Campo | Valore |
|-------|--------|
| **ID** | UC03.3 |
| **Nome** | Modificare Parametri Effetto |
| **Attore** | Utente |
| **Pre-condizioni** | Effetto selezionato |

### Flusso Principale
1. Utente interagisce con controllo parametro (knob/slider)
2. UI invia nuovo valore (thread-safe)
3. Sistema applica smoothing per evitare click
4. Audio thread legge valore smoothed
5. Effetto usa nuovo valore nel processing

### Smoothing dei Parametri
```
Per evitare click/pop audio:
- Tempo smoothing: 20ms default
- Formula: current += coeff * (target - current)
- coeff = 1 - exp(-1 / (tau * sampleRate))
```

### Interazioni Utente
| Azione | Risultato |
|--------|-----------|
| Drag verticale su knob | Cambia valore |
| Shift + Drag | Cambio fine (1/10 velocità) |
| Double-click | Reset a default |
| Right-click | Menu contestuale |
| Scroll wheel | Incremento/decremento |

---

## UC03.4 - Bypassare Effetto

| Campo | Valore |
|-------|--------|
| **ID** | UC03.4 |
| **Nome** | Bypassare Effetto |
| **Attore** | Utente |

### Flusso Principale
1. Utente clicca pulsante Bypass (o tasto 'B')
2. Sistema attiva flag bypass (atomic)
3. Nel processing: se bypass, copia input->output senza elaborazione
4. UI mostra stato bypass (opacità ridotta)

### Implementazione
```java
public void process(float[] input, float[] output, int frames) {
    if (bypassed.get()) {
        System.arraycopy(input, 0, output, 0, frames);
        return;
    }
    // Normal processing...
}
```

---

## UC03.5 - Definizione Effetti Base

### Noise Gate

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Threshold | -80..0 | -40 | dB |
| Attack | 0.01..10 | 0.1 | ms |
| Release | 5..500 | 50 | ms |
| Range | -80..0 | -80 | dB |

**Algoritmo:**
```
if (inputLevel < threshold):
    gain = range
else:
    gain = 0dB
output = input * smoothed(gain)
```

---

### Compressor

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Threshold | -60..0 | -20 | dB |
| Ratio | 1..20 | 4 | :1 |
| Attack | 0.1..100 | 10 | ms |
| Release | 10..1000 | 100 | ms |
| Makeup | 0..24 | 0 | dB |
| Knee | 0..12 | 3 | dB |

**Algoritmo:**
```
inputDb = 20 * log10(abs(input))
if (inputDb > threshold):
    gainReduction = (inputDb - threshold) * (1 - 1/ratio)
else:
    gainReduction = 0
output = input * db2linear(-gainReduction + makeup)
```

---

### Overdrive/Distortion

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Gain | 0..10 | 5 | - |
| Tone | 0..10 | 5 | - |
| Level | 0..10 | 5 | - |
| Mode | enum | Classic | - |

**Modi disponibili:** Classic, Modern, Tube, Fuzz

**Algoritmo base (soft clip):**
```
// Pre-gain
x = input * gainAmount

// Soft clipping (tanh waveshaping)
y = tanh(x)

// Tone (low-pass filter)
y = lowpass(y, toneFrequency)

// Output level
output = y * levelAmount
```

---

### Delay

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Time | 20..2000 | 375 | ms |
| Feedback | 0..100 | 40 | % |
| Mix | 0..100 | 50 | % |
| HighCut | 500..20000 | 8000 | Hz |
| LowCut | 20..2000 | 80 | Hz |
| Sync | bool | false | - |
| PingPong | bool | false | - |

**Algoritmo:**
```
// Delay line (circular buffer)
delayedSample = delayBuffer[readIndex]

// Feedback with filtering
filtered = highCut(lowCut(delayedSample))
delayBuffer[writeIndex] = input + filtered * feedback

// Mix
output = input * (1 - mix) + delayedSample * mix
```

---

### Reverb (Freeverb-style)

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Size | 0..100 | 50 | % |
| Decay | 0.1..30 | 2.5 | s |
| PreDelay | 0..200 | 20 | ms |
| Damping | 0..100 | 50 | % |
| Mix | 0..100 | 30 | % |
| Type | enum | Hall | - |

**Tipi:** Room, Hall, Plate, Spring, Shimmer

**Algoritmo (semplificato):**
```
// Pre-delay
preDelayed = preDelayBuffer[readIndex]

// Comb filters (8 in parallelo)
combOut = sum(combFilter[i](preDelayed)) for i in 0..7

// All-pass filters (4 in serie)
allpassOut = allpass[3](allpass[2](allpass[1](allpass[0](combOut))))

// Mix
output = input * (1 - mix) + allpassOut * mix
```

---

### Chorus

| Parametro | Range | Default | Unità |
|-----------|-------|---------|-------|
| Rate | 0.1..10 | 1.5 | Hz |
| Depth | 0..100 | 50 | % |
| Delay | 1..30 | 7 | ms |
| Mix | 0..100 | 50 | % |
| Voices | 1..4 | 2 | - |

**Algoritmo:**
```
// LFO modulation
lfoValue = sin(2 * PI * rate * time)
modulatedDelay = baseDelay + depth * lfoValue

// Interpolated delay read
delayed = interpolate(delayBuffer, modulatedDelay)

// Mix
output = input * (1 - mix) + delayed * mix
```

---

### Parametric EQ (4 bande)

| Banda | Freq Range | Gain Range | Q Range | Tipo |
|-------|------------|------------|---------|------|
| Band 1 | 20-500 Hz | ±15 dB | - | Low Shelf |
| Band 2 | 100-2000 Hz | ±15 dB | 0.1-10 | Peaking |
| Band 3 | 500-8000 Hz | ±15 dB | 0.1-10 | Peaking |
| Band 4 | 2-20 kHz | ±15 dB | - | High Shelf |

**Implementazione:** Biquad filters (IIR) in serie

---

## UC03.6 - Effect Processing Pipeline

### Ordine di Processing Consigliato

```
INPUT
  │
  ▼
┌─────────────────┐
│ 1. INPUT BUFFER │
│ 2. NOISE GATE   │  ← Detection
│ 3. WAH/FILTER   │  ← Pre-gain
│ 4. COMPRESSOR   │  ← Opzionale qui
│ 5. PITCH FX     │  ← Tracking migliore pre-dist
│ 6. DRIVE/DIST   │  ← Gain stages
│ 7. NOISE GATE   │  ← Gating post-gain
│ 8. AMP SIM      │  ← Preamp + Poweramp
│ 9. EQ           │  ← Tone shaping
│ 10. MODULATION  │  ← Chorus/Flanger/etc
│ 11. DELAY       │  ← Time-based
│ 12. REVERB      │  ← Space
│ 13. CAB SIM     │  ← Speaker + Mic
│ 14. LIMITER     │  ← Protezione output
│ 15. OUTPUT      │
└─────────────────┘
  │
  ▼
OUTPUT
```

---

## Interfacce Tecniche

### AudioEffect Interface
```java
public interface AudioEffect {
    void prepare(int sampleRate, int maxBlockSize);
    void process(float[] input, float[] output, int frames);
    void release();
    void reset();

    EffectMetadata getMetadata();
    List<Parameter> getParameters();
    Parameter getParameter(String id);

    boolean isBypassed();
    void setBypassed(boolean bypassed);
}
```

### Parameter Class
```java
public class Parameter {
    private final String id;
    private final String name;
    private final ParameterType type;
    private final double min, max, defaultValue;
    private final String unit;
    private final ScaleType scale;

    private AtomicReference<Double> targetValue;
    private double currentValue; // Smoothed

    public void setValue(double value);
    public double getSmoothedValue(); // Called from audio thread
    public String getFormattedValue();
}
```

### EffectFactory Class
```java
public class EffectFactory {
    private static Map<String, Supplier<AudioEffect>> registry;

    public static void register(String type, Supplier<AudioEffect> factory);
    public static AudioEffect create(String type);
    public static List<String> getAvailableTypes();
    public static EffectMetadata getMetadata(String type);
}
```
