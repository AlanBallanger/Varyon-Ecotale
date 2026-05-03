package fr.varyon.ecotale;

import fr.varyon.ecotale.coins.CoinsModule;
import fr.varyon.ecotale.economy.EconomyManager;
import fr.varyon.ecotale.economy.EconomyModule;
import fr.varyon.ecotale.economy.config.EcotaleConfig;
import fr.varyon.ecotale.jobs.JobsModule;
import fr.varyon.ecotale.jobs.config.CraftingMappingsConfig;
import fr.varyon.ecotale.jobs.config.EarningsConfigLoader;
import fr.varyon.ecotale.jobs.config.EcotaleJobsConfig;
import fr.varyon.ecotale.jobs.config.TierMappingsConfig;
import fr.varyon.ecotale.economy.systems.BalanceHudSystem;
import fr.varyon.ecotale.economy.util.TranslationHelper;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.nio.file.Path;
import java.util.logging.Level;

public class VaryonEcotalePlugin extends JavaPlugin {

    private static VaryonEcotalePlugin instance;

    public Config<EcotaleConfig> economyConfig;
    private Config<TierMappingsConfig> tierMappingsConfig;
    private Config<CraftingMappingsConfig> craftingMappingsConfig;

    private EconomyModule economyModule;
    private CoinsModule coinsModule;
    private JobsModule jobsModule;

    public VaryonEcotalePlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        this.economyConfig = this.withConfig("config", EcotaleConfig.CODEC);
        this.tierMappingsConfig = this.withConfig("TierMappings", TierMappingsConfig.CODEC);
        this.craftingMappingsConfig = this.withConfig("CraftingMappings", CraftingMappingsConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;

        economyConfig.save();

        EcotaleConfig cfg = economyConfig.get();

        this.economyModule = new EconomyModule(economyConfig);
        economyModule.setup(this);

        if (cfg.isEnableCoins()) {
            this.coinsModule = new CoinsModule();
            coinsModule.setup(this);
        } else {
            getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins module disabled (config.json).");
        }

        if (cfg.isEnableJobs()) {
            Path earningsPath = getDataDirectory().resolve("earnings_config.yml");
            EarningsConfigLoader.installDefaultYamlIfMissing(earningsPath, getLogger(), VaryonEcotalePlugin.class);
            EcotaleJobsConfig earnings = EarningsConfigLoader.load(earningsPath, getLogger());
            this.jobsModule = new JobsModule(earnings, tierMappingsConfig, craftingMappingsConfig);
            jobsModule.setup(this);
        } else {
            getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module disabled (config.json).");
        }

        getLogger().at(Level.INFO).log("[Varyon-Ecotale] Plugin fully loaded.");
    }

    @Override
    protected void shutdown() {
        if (jobsModule != null) jobsModule.shutdown();
        if (coinsModule != null) coinsModule.shutdown();
        if (economyModule != null) economyModule.shutdown();
        getLogger().at(Level.INFO).log("[Varyon-Ecotale] Plugin shutdown complete.");
    }

    public static VaryonEcotalePlugin getInstance() { return instance; }

    public EcotaleConfig getEconomyConfig() {
        return economyConfig != null ? economyConfig.get() : null;
    }

    public EconomyManager getEconomyManager() {
        return economyModule != null ? economyModule.getEconomyManager() : null;
    }

    public CoinsModule getCoinsModule() { return coinsModule; }
    public JobsModule getJobsModule() { return jobsModule; }
    public EconomyModule getEconomyModule() { return economyModule; }

    /**
     * Reload economy config.json, Tier/Crafting mappings, earnings YAML (jobs), Physical_Currency (coins), HUDs.
     * Does not change EnableJobs/EnableCoins or storage provider until server restart.
     */
    public void reloadConfigurationFromDisk() {
        economyConfig.load();
        TranslationHelper.invalidateCache();
        tierMappingsConfig.load();
        craftingMappingsConfig.load();
        if (jobsModule != null) {
            jobsModule.reloadFromDisk(this);
        }
        if (coinsModule != null && coinsModule.isEnabled()) {
            if (!coinsModule.reloadPhysicalCurrency()) {
                getLogger().at(Level.WARNING).log("[Varyon-Ecotale] Physical_Currency.json reload reported failure.");
            }
        }
        BalanceHudSystem.refreshAllHuds();
        getLogger().at(Level.INFO).log("[Varyon-Ecotale] Configuration reload complete.");
    }
}
