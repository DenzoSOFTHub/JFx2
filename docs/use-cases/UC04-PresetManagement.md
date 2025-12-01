# UC04 - Preset Management

## Panoramica
Gestione completa dei preset (rig): salvataggio, caricamento, organizzazione e condivisione.

---

## UC04.1 - Creare Nuovo Rig

| Campo | Valore |
|-------|--------|
| **ID** | UC04.1 |
| **Nome** | Creare Nuovo Rig |
| **Attore** | Utente |
| **Shortcut** | Ctrl+N |

### Flusso Principale
1. Utente richiede nuovo rig (menu o shortcut)
2. Se ci sono modifiche non salvate:
   - Sistema chiede "Salvare le modifiche?"
   - [Save] → salva e continua
   - [Don't Save] → continua senza salvare
   - [Cancel] → annulla operazione
3. Sistema crea nuovo grafo con:
   - Nodo INPUT
   - Nodo OUTPUT
   - Connessione INPUT → OUTPUT
4. Sistema resetta titolo a "Untitled Rig"
5. Sistema centra vista sul grafo

### Post-condizioni
- Rig vuoto pronto per editing
- Flag "modified" = false

---

## UC04.2 - Salvare Rig

| Campo | Valore |
|-------|--------|
| **ID** | UC04.2 |
| **Nome** | Salvare Rig |
| **Attore** | Utente |
| **Shortcut** | Ctrl+S (Save), Ctrl+Shift+S (Save As) |

### Flusso Principale (Save - file esistente)
1. Utente preme Ctrl+S
2. Sistema serializza stato corrente
3. Sistema scrive file .jfxrig
4. Sistema mostra notifica "Saved"
5. Sistema aggiorna flag modified = false

### Flusso Principale (Save As - nuovo file)
1. Utente preme Ctrl+Shift+S
2. Sistema mostra dialog:
   ```
   ┌─────────────────────────────────────────┐
   │ Save Rig                                │
   ├─────────────────────────────────────────┤
   │ Name:     [_________________________]   │
   │ Category: [Clean         ▼]             │
   │ Tags:     [Blues] [Lead] [+ Add]        │
   │ Description:                            │
   │ [_____________________________________] │
   │                                         │
   │ Location: ~/JFx Presets/                │
   │                                         │
   │           [Cancel]  [Save]              │
   └─────────────────────────────────────────┘
   ```
3. Utente compila metadati
4. Utente clicca Save
5. Sistema genera:
   - UUID per il rig
   - Timestamp creazione
   - Thumbnail del grafo
6. Sistema salva file
7. Sistema aggiorna titolo finestra

### Flussi Alternativi
- **1a. File non ancora salvato**: Passa a Save As

---

## UC04.3 - Caricare Rig

| Campo | Valore |
|-------|--------|
| **ID** | UC04.3 |
| **Nome** | Caricare Rig |
| **Attore** | Utente |
| **Shortcut** | Ctrl+O |

### Flusso Principale
1. Utente apre Preset Browser
2. Sistema mostra:
   - Albero categorie (sinistra)
   - Lista preset (centro)
   - Preview (destra)
3. Utente naviga/filtra/cerca
4. Utente seleziona preset
5. Sistema mostra preview:
   - Thumbnail grafo
   - Nome, autore, data
   - Tags e categoria
   - Lista effetti usati
6. Utente conferma (doppio-click o Load)
7. Sistema verifica modifiche non salvate
8. Sistema carica e deserializza rig
9. Sistema ricostruisce grafo
10. Sistema centra vista

### Flussi Alternativi
- **8a. Effetto non disponibile**: Sostituisce con placeholder, warning
- **8a. Versione incompatibile**: Tenta migrazione, warning se fallisce

---

## UC04.4 - Navigare Preset Browser

| Campo | Valore |
|-------|--------|
| **ID** | UC04.4 |
| **Nome** | Navigare Preset Browser |
| **Attore** | Utente |

### Struttura Browser
```
┌──────────────┬─────────────────────┬───────────────────┐
│ CATEGORIES   │ PRESET LIST         │ PREVIEW           │
├──────────────┼─────────────────────┼───────────────────┤
│ ▼ All        │ [Search...        ] │ ┌───────────────┐ │
│   Favorites★ │                     │ │  [THUMBNAIL]  │ │
│   Recent     │ Clean Jazz Tone     │ │               │ │
│              │ ★★★★☆  Jazz, Clean  │ └───────────────┘ │
│ ▼ By Type    │                     │                   │
│   Clean      │ Bluesy Crunch       │ Clean Jazz Tone   │
│   Crunch     │ ★★★★★  Blues        │ by: Factory       │
│   High Gain  │                     │ Created: 2024-01  │
│   Ambient    │ Metal Rhythm        │                   │
│              │ ★★★★☆  Metal        │ [Blues] [Clean]   │
│ ▼ By Genre   │                     │                   │
│   Blues      │ ...                 │ Effects:          │
│   Rock       │                     │ • Compressor      │
│   Metal      │                     │ • Overdrive       │
│   Jazz       │                     │ • Delay           │
│              │                     │ • Reverb          │
└──────────────┴─────────────────────┴───────────────────┘
```

### Funzionalità Filtro/Ricerca
| Azione | Comportamento |
|--------|---------------|
| Click categoria | Filtra per categoria |
| Testo in search | Cerca in nome, descrizione, tags |
| Click stella | Toggle favorite |
| Ordinamento | Per nome, data, rating |

---

## UC04.5 - Gestire Preferiti

| Campo | Valore |
|-------|--------|
| **ID** | UC04.5 |
| **Nome** | Gestire Preferiti |
| **Attore** | Utente |

### Flusso Principale
1. Utente visualizza preset nel browser
2. Utente clicca icona stella
3. Sistema toggle stato favorite
4. Sistema aggiorna persistenza
5. Preset appare/scompare da categoria "Favorites"

---

## UC04.6 - Valutare Preset

| Campo | Valore |
|-------|--------|
| **ID** | UC04.6 |
| **Nome** | Valutare Preset |
| **Attore** | Utente |

### Flusso Principale
1. Utente seleziona preset
2. Utente clicca su stelle rating (1-5)
3. Sistema salva rating
4. Rating visibile nel browser e preview

---

## UC04.7 - Duplicare Preset

| Campo | Valore |
|-------|--------|
| **ID** | UC04.7 |
| **Nome** | Duplicare Preset |
| **Attore** | Utente |

### Flusso Principale
1. Utente right-click su preset
2. Utente seleziona "Duplicate"
3. Sistema crea copia con:
   - Nuovo UUID
   - Nome + " (Copy)"
   - Nuovo timestamp
4. Sistema apre dialog Save As
5. Utente modifica nome/metadati
6. Sistema salva nuovo file

---

## UC04.8 - Eliminare Preset

| Campo | Valore |
|-------|--------|
| **ID** | UC04.8 |
| **Nome** | Eliminare Preset |
| **Attore** | Utente |

### Flusso Principale
1. Utente right-click su preset
2. Utente seleziona "Delete"
3. Sistema chiede conferma
4. Sistema elimina file
5. Sistema rimuove da cache
6. Sistema aggiorna lista

### Flussi Alternativi
- **3a. Utente annulla**: Nessuna azione

---

## UC04.9 - Esportare/Importare Preset

| Campo | Valore |
|-------|--------|
| **ID** | UC04.9 |
| **Nome** | Esportare/Importare Preset |
| **Attore** | Utente |

### Flusso Export
1. Utente right-click su preset
2. Utente seleziona "Export..."
3. Sistema mostra file chooser
4. Utente sceglie destinazione
5. Sistema copia file .jfxrig

### Flusso Import
1. Utente seleziona File > Import Preset
2. Sistema mostra file chooser
3. Utente seleziona file .jfxrig
4. Sistema valida formato
5. Sistema importa in libreria preset
6. Sistema assegna nuovo UUID (evita conflitti)

---

## Struttura Dati Preset

### Formato File (.jfxrig)
```json
{
  "formatVersion": "1.0",
  "metadata": {
    "id": "uuid-v4",
    "name": "Preset Name",
    "author": "User Name",
    "description": "Description text",
    "category": "Clean|Crunch|High Gain|Ambient",
    "tags": ["Blues", "Lead", "Warm"],
    "createdAt": "2024-01-15T14:30:00Z",
    "modifiedAt": "2024-01-16T09:15:00Z",
    "version": 1,
    "userRating": 4,
    "isFavorite": true
  },
  "globalSettings": {
    "bpm": 120,
    "inputGain": 0.0,
    "outputGain": 0.0
  },
  "graph": {
    "nodes": [
      {
        "id": "node-uuid",
        "type": "distortion",
        "name": "Custom Name",
        "x": 400,
        "y": 200,
        "bypassed": false,
        "parameters": {
          "gain": 7.5,
          "tone": 6.0,
          "level": 5.5,
          "mode": "Modern"
        }
      }
    ],
    "connections": [
      {
        "id": "conn-uuid",
        "sourceNode": "input",
        "sourcePort": "out",
        "targetNode": "node-uuid",
        "targetPort": "in"
      }
    ]
  },
  "viewState": {
    "zoom": 1.0,
    "panX": 0,
    "panY": 0
  }
}
```

### Categorie Predefinite
| Categoria | Descrizione |
|-----------|-------------|
| Clean | Suoni puliti, jazz, country |
| Crunch | Leggera saturazione, blues, rock classico |
| High Gain | Distorsione pesante, metal, hard rock |
| Ambient | Riverberi, delay, soundscape |
| Acoustic | Simulazioni acustiche |
| Bass | Preset ottimizzati per basso |
| Custom | Categoria utente |

---

## Interfacce Tecniche

### PresetManager Class
```java
public class PresetManager {
    private final Path presetsDirectory;
    private final JsonParser jsonParser;  // Custom parser

    // Persistence
    void savePreset(Rig rig, Path path);
    Rig loadPreset(Path path);

    // Library
    void loadAllPresets();
    List<RigMetadata> getAllPresets();
    List<RigMetadata> getPresetsByCategory(String category);
    List<RigMetadata> getPresetsByTag(String tag);
    List<RigMetadata> getFavorites();
    List<RigMetadata> getRecent(int limit);
    List<RigMetadata> search(String query);

    // Management
    void setFavorite(String presetId, boolean favorite);
    void setRating(String presetId, int rating);
    void deletePreset(String presetId);
    void duplicatePreset(String presetId, String newName);

    // Import/Export
    void exportPreset(String presetId, Path destination);
    RigMetadata importPreset(Path source);
}
```

### Rig Class
```java
public class Rig {
    private RigMetadata metadata;
    private SignalGraphState graphState;
    private GlobalSettings globalSettings;
    private ViewState viewState;

    public static Rig fromJson(String json);
    public String toJson();
    public Rig deepCopy();
}
```

### RigMetadata Class
```java
public class RigMetadata {
    private String id;
    private String name;
    private String author;
    private String description;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private int version;
    private int userRating;
    private boolean isFavorite;
    private byte[] thumbnailPng;
}
```

### Custom JSON Parser (no external libraries)
```java
public class JsonParser {
    // Parse JSON string to Map/List structure
    public Object parse(String json);

    // Serialize object to JSON string
    public String stringify(Object obj, boolean prettyPrint);
}

public class JsonObject extends LinkedHashMap<String, Object> {
    public String getString(String key);
    public int getInt(String key);
    public double getDouble(String key);
    public boolean getBoolean(String key);
    public JsonObject getObject(String key);
    public JsonArray getArray(String key);
}

public class JsonArray extends ArrayList<Object> {
    public String getString(int index);
    public JsonObject getObject(int index);
    // ...
}
```

### Note Implementazione Parser
Il parser custom supporterà:
- Tipi base: string, number, boolean, null
- Strutture: object {}, array []
- Escape sequences in stringhe
- Pretty print per leggibilità
- Validazione formato
