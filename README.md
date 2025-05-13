# ElytraRemover

Un plugin Minecraft per server Paper 1.21.4 che rimuove o sostituisce gli elytras nelle navi dell'End.

**Autore:** Zaoldieck

## Funzionalità

Questo plugin impedisce ai giocatori di trovare elytras nelle navi volanti dell'End in diversi modi:

- **Rimuovere gli elytras** - Rimuove solo l'elytra dagli item frame
- **Rimuovere l'item frame** - Rimuove l'intero item frame quando vicino a una barca
- **Sostituire con una mela** - Sostituisce gli elytras con delle mele (impostazione predefinita)

Il plugin controlla automaticamente le nuove aree generate e fa una scansione periodica ogni 5 secondi.

## Installazione

1. Scarica `ElytraRemover.jar` dalla [sezione Releases](https://github.com/Zaoldieck/ElytraRemover/releases)
2. Carica il file nella cartella `plugins` del tuo server Minecraft
3. Riavvia il server
4. Il plugin si attiverà automaticamente

## Configurazione

Dopo il primo avvio, il plugin creerà un file `config.yml` nella cartella `plugins/ElytraRemover/`. Puoi modificare queste impostazioni:

```yaml
# Check interval in seconds (how often to scan for elytras in item frames in End ships)
check-interval: 10

# Debug mode (enables additional logging)
debug: true

# End ship detection settings
end-ship:
  # Minimum Y level for end ships (used for detection)
  min-y: 0
  
  # Maximum Y level for end ships (used for detection)
  max-y: 256
  
  # Whether to check for elytras in all item frames in The End (not just in ships with boats)
  remove-all-end-elytras: true
  
  # How aggressively to scan chunks (higher = more thorough but more resource intensive)
  # 0 = basic, 1 = normal, 2 = aggressive
  scan-intensity: 3
  
  # Action to take when finding an elytra in an item frame
  # Options: REMOVE_ELYTRA, REMOVE_FRAME, REPLACE_WITH_APPLE
  action: REPLACE_WITH_APPLE
```

## Comandi

- `/elytraremover` - Mostra informazioni sul plugin
- `/elytraremover reload` - Ricarica la configurazione
- `/elytraremover scan` - Forza una scansione manuale di tutti i chunk caricati

## Permessi

- `elytraremover.admin` - Permette di usare i comandi del plugin (default: op)

## Requisiti

- Server Minecraft Paper 1.21.4
- Java 17 o superiore

## Licenza

Copyright © 2025 Zaoldieck. Tutti i diritti riservati.