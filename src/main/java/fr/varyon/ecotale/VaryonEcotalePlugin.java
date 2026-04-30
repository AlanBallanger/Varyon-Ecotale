package fr.varyon.ecotale;

import fr.varyon.ecotale.coins.CoinsModule;
import fr.varyon.ecotale.economy.EconomyManager;
import fr.varyon.ecotale.economy.EconomyModule;
import fr.varyon.ecotale.economy.config.EcotaleConfig;
import fr.varyon.ecotale.jobs.JobsModule;
import fr.varyon.ecotale.shared.ModulesConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public class VaryonEcotalePlugin extends JavaPlugin {

    private static VaryonEcotalePlugin instance;

    public Config<EcotaleConfig> economyConfig;
    private Config<ModulesConfig> modulesConfig;

    private EconomyModule economyModule;
    private CoinsModule coinsModule;
    private JobsModule jobsModule;

    public VaryonEcotalePlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        this.economyConfig = this.withConfig("Economy", EcotaleConfig.CODEC);
        this.modulesConfig = this.withConfig("Modules", ModulesConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;

        economyConfig.save();
        modulesConfig.save();
        ModulesConfig modules = modulesConfig.get();

        this.economyModule = new EconomyModule(economyConfig);
        economyModule.setup(this);

        if (modules.isEnableCoins()) {
            this.coinsModule = new CoinsModule();
            coinsModule.setup(this);
        } else {
            getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins module disabled via Modules.json.");
        }

        if (modules.isEnableJobs()) {
            this.jobsModule = new JobsModule(this);
            jobsModule.setup(this);
        } else {
            getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module disabled via Modules.json.");
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

    public <T> Config<T> createConfig(String name, com.hypixel.hytale.codec.builder.BuilderCodec<T> codec) {
        return this.withConfig(name, codec);
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
}
