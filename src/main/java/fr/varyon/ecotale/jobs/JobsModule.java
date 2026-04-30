package fr.varyon.ecotale.jobs;

import fr.varyon.ecotale.jobs.commands.TestOresCommand;
import fr.varyon.ecotale.jobs.config.CraftingMappingsConfig;
import fr.varyon.ecotale.jobs.config.EcotaleJobsConfig;
import fr.varyon.ecotale.jobs.config.TierMappingsConfig;
import fr.varyon.ecotale.jobs.systems.CraftingRewardSystem;
import fr.varyon.ecotale.jobs.systems.MiningRewardSystem;
import fr.varyon.ecotale.jobs.systems.MobRewardSystem;
import fr.varyon.ecotale.jobs.util.CraftingAutoDetector;
import fr.varyon.ecotale.jobs.util.NPCAutoDetector;
import fr.varyon.ecotale.shared.ModuleInitializer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.util.Config;

import java.util.logging.Level;

public class JobsModule implements ModuleInitializer {

    private Config<EcotaleJobsConfig> configHolder;
    private Config<TierMappingsConfig> tierMappingsConfig;
    private Config<CraftingMappingsConfig> craftingMappingsConfig;
    private JavaPlugin plugin;

    @Override
    public void setup(JavaPlugin plugin) {
        this.plugin = plugin;

        this.configHolder = plugin.buildConfig(
            plugin.getDataDirectory().resolve("Jobs.json"),
            EcotaleJobsConfig.CODEC);
        configHolder.save();

        this.tierMappingsConfig = plugin.buildConfig(
            plugin.getDataDirectory().resolve("TierMappings.json"),
            TierMappingsConfig.CODEC);
        tierMappingsConfig.save();

        this.craftingMappingsConfig = plugin.buildConfig(
            plugin.getDataDirectory().resolve("CraftingMappings.json"),
            CraftingMappingsConfig.CODEC);
        craftingMappingsConfig.save();

        plugin.getEventRegistry().register(new MobRewardSystem(this));
        plugin.getEventRegistry().register(new MiningRewardSystem(this));
        plugin.getEventRegistry().register(new CraftingRewardSystem(this));

        if (configHolder.get().isEnableNPCAutoDetection()) {
            plugin.getEventRegistry().register(new NPCAutoDetector(this));
        }
        if (configHolder.get().isEnableCraftingAutoDetection()) {
            plugin.getEventRegistry().register(new CraftingAutoDetector(this));
        }

        plugin.getCommandRegistry().registerCommand(new TestOresCommand());

        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module loaded.");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module shutdown.");
    }

    public EcotaleJobsConfig getConfig() { return configHolder.get(); }
    public Config<EcotaleJobsConfig> getConfigHolder() { return configHolder; }
    public TierMappingsConfig getTierMappings() { return tierMappingsConfig.get(); }
    public CraftingMappingsConfig getCraftingMappings() { return craftingMappingsConfig.get(); }
}
