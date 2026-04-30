# Varyon-Ecotale

Unified Hytale economy mod — merges **Ecotale** (core economy), **EcotaleCoins** (physical currency) and **EcotaleJobs** (reward system for mining, mob kills and crafting) into a single plugin.

## Features

### Economy (always active)

- Player balances backed by H2 (default) / MySQL / JSON storage
- `/bal` — view your balance
- `/pay <player> <amount>` — transfer funds
- `/eco` — admin panel (set, give, take, reset, view transactions)
- HUD balance display with smooth animation
- Multi-language support (de-DE, en-US, es-ES, fr-FR, ja-JP, pt-BR, ru-RU, tr-TR, zh-CN)
- VaultUnlocked integration (optional)
- Rate limiting, transaction logging, performance monitoring

### Coins — Physical Currency (optional, toggle in `config.json`)

- Six denominations: Copper, Iron, Gold, Mithril, Cobalt, Adamantite
- Pick up coins dropped in the world
- `/bank` — deposit / withdraw / exchange / consolidate
- First-time asset extraction on fresh install

### Jobs — Reward System (optional, toggle in `config.json`)

- Mining rewards by ore quality tier
- Mob kill rewards with anti-farm protection
- Crafting rewards with recipe auto-detection
- VIP multipliers (LuckPerms integration)
- Per-tier drop chance and value ranges
- NPC auto-detection on `AllNPCsLoadedEvent`

## Installation

1. Drop `Varyon-Ecotale-*.jar` into your server's `mods/` folder.
2. Start the server once — config files are generated in `mods/Varyon_Varyon-Ecotale/`.
3. Edit `config.json`, `earnings_config.yml`, `TierMappings.json`, `CraftingMappings.json`, and `Physical_Currency.json` as needed (`earnings_config.yml` is YAML; others use Hytale’s JSON codecs).
4. If Coins are enabled in `config.json`, restart once after first boot so coin assets are deployed.

## Configuration files

| File | Description |
|------|-------------|
| `config.json` | Economy settings plus `EnableCoins` / `EnableJobs` |
| `earnings_config.yml` | Mob / mining / crafting rewards, VIP, notifications (`DebugMode` here) |
| `TierMappings.json` | NPC → tier mappings (auto-updated) |
| `CraftingMappings.json` | Recipe → tier mappings (auto-updated) |
| `Physical_Currency.json` | Coin denominations and values |

The plugin ships with one mod manifest embedded in the JAR; no standalone `manifest.json` is written into the coin asset folder next to configs.

## Build

```
./gradlew fatJar
```

Output: `build/libs/Varyon-Ecotale-1.0.0.jar`

## Package structure

```
fr.varyon.ecotale
├── VaryonEcotalePlugin       # Entry point
├── shared/                   # Internal bridges & module wiring
├── economy/                  # Core economy (ex Ecotale)
├── coins/                    # Physical currency (ex EcotaleCoins)
└── jobs/                     # Reward system (ex EcotaleJobs)
```

## Dependencies

| Dependency | Scope | Notes |
|---|---|---|
| Hytale Server API | compileOnly | `server_version` in gradle.properties |
| H2 Database 2.2.224 | bundled | Default storage |
| MySQL Connector/J 9.1.0 | bundled | Optional storage |
| Gson 2.10.1 | bundled | Physical coin config |
| SnakeYAML 2.2 | bundled | `earnings_config.yml` parsing |
| VaultUnlocked 2.18.3 | compileOnly / optional runtime | Economy API bridge |

## Migration from standalone mods

Remove `Ecotale`, `EcotaleCoins` and `EcotaleJobs` jars from your mods folder before adding `Varyon-Ecotale`. Merge old `EcotaleJobs.json` into `earnings_config.yml` (YAML with the same key names works; you can rename the file after converting JSON → YAML). Remove obsolete `Modules.json` and merge `EnableCoins` / `EnableJobs` into `config.json`. Existing balances in H2 or MySQL remain compatible if storage settings match.
