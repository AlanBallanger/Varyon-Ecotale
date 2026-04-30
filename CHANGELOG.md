# Changelog — Varyon-Ecotale

## [1.0.0] — 2026-04-30

### Added
- Merged `Ecotale` (core economy), `EcotaleCoins` (physical currency) and `EcotaleJobs` (mining/mob/crafting rewards) into a single mod
- Module toggles (`EnableCoins`, `EnableJobs`) live inside `config.json`
- Unified entry point `fr.varyon.ecotale.VaryonEcotalePlugin`
- Internal bridges `EconomyBridge` and `CoinsBridge` replacing the previous public `EcotaleAPI` and `PhysicalCoinsProvider` interfaces
- `fatJar` build bundling H2, MySQL Connector/J, Gson, SnakeYAML and SLF4J
- Kotlin DSL build with `hytale-mod` plugin (convention from `Varyon-Damage_Number`)
- All packages migrated from `com.ecotale.*` / `com.ecotalecoins.*` / `com.ecotalejobs.*` to `fr.varyon.ecotale.*`

### Changed
- `config.json`: economy plus `EnableCoins` / `EnableJobs`
- Coin config: `Physical_Currency.json` (Gson, `CoinsModule`)
- Jobs rewards: `earnings_config.yml` (SnakeYAML → `EcotaleJobsConfig`), plus `TierMappings.json`, `CraftingMappings.json`
- Coin asset pack no longer emits `manifest.json` or `README.txt` beside extracted textures
- Asset pack folder: `mods/Varyon_Varyon-Ecotale/` (group=Varyon, name=Varyon-Ecotale)

### Removed
- Separate `Modules.json`
- Embedded `manifest.json` + `README.txt` generation inside the coin asset extraction folder (`CoinAssetManager`)
- Public `EcotaleAPI` — replaced by `EconomyBridge` (internal)
- Public `PhysicalCoinsProvider` — replaced by `CoinsBridge` (internal)
- `EcotaleCoinsProviderImpl` registration (now handled directly via `CoinsBridge`)
- Separate per-mod dependencies in the server manifest
