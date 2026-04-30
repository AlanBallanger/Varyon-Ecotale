# Changelog — Varyon-Ecotale

## [1.0.0] — 2026-04-30

### Added
- Merged `Ecotale` (core economy), `EcotaleCoins` (physical currency) and `EcotaleJobs` (mining/mob/crafting rewards) into a single mod
- Unified entry point `fr.varyon.ecotale.VaryonEcotalePlugin`
- Module toggle system via `Modules.json` (`EnableCoins`, `EnableJobs`)
- Internal bridges `EconomyBridge` and `CoinsBridge` replacing the previous public `EcotaleAPI` and `PhysicalCoinsProvider` interfaces
- `fatJar` build bundling H2, MySQL Connector/J, Gson and SLF4J
- Kotlin DSL build with `hytale-mod` plugin (convention from `Varyon-Damage_Number`)
- All packages migrated from `com.ecotale.*` / `com.ecotalecoins.*` / `com.ecotalejobs.*` to `fr.varyon.ecotale.*`

### Changed
- Storage configs split: `Economy.json` (economy settings) + `Modules.json` (module toggles)
- Coin config remains `Coins.json` (Gson-based, loaded by `CoinsModule`)
- Jobs configs: `EcotaleJobs.json`, `TierMappings.json`, `CraftingMappings.json`
- Asset pack folder: `mods/Varyon_Varyon-Ecotale/` (group=Varyon, name=Varyon-Ecotale)

### Removed
- Public `EcotaleAPI` — replaced by `EconomyBridge` (internal)
- Public `PhysicalCoinsProvider` — replaced by `CoinsBridge` (internal)
- `EcotaleCoinsProviderImpl` registration (now handled directly via `CoinsBridge`)
- Separate per-mod dependencies in the server manifest
