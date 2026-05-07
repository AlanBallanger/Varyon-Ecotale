package fr.varyon.ecotale.jobs;

import fr.varyon.ecotale.jobs.commands.TestOresCommand;
import fr.varyon.ecotale.jobs.config.CraftingMappingsConfig;
import fr.varyon.ecotale.jobs.config.EarningsConfigLoader;
import fr.varyon.ecotale.jobs.config.EcotaleJobsConfig;
import fr.varyon.ecotale.jobs.config.TierMappingsConfig;
import fr.varyon.ecotale.jobs.systems.MobLastAttackerSystem;
import fr.varyon.ecotale.jobs.systems.MobRewardSystem;
import fr.varyon.ecotale.jobs.util.NPCAutoDetector;
import fr.varyon.ecotale.jobs.util.RewardNotifier;
import fr.varyon.ecotale.shared.ModuleInitializer;
import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.jobs.util.JobsLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.AllNPCsLoadedEvent;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;

public class JobsModule implements ModuleInitializer {

    private EcotaleJobsConfig earningsConfig;
    private final Config<TierMappingsConfig> tierMappingsConfig;
    private final Config<CraftingMappingsConfig> craftingMappingsConfig;

    private MobRewardSystem mobRewardSystem;
    private MobLastAttackerSystem lastAttackerSystem;

    private JavaPlugin plugin;

    public JobsModule(EcotaleJobsConfig earningsConfig,
                      Config<TierMappingsConfig> tierMappingsConfig,
                      Config<CraftingMappingsConfig> craftingMappingsConfig) {
        this.earningsConfig = earningsConfig;
        this.tierMappingsConfig = tierMappingsConfig;
        this.craftingMappingsConfig = craftingMappingsConfig;
    }

    @Override
    public void setup(JavaPlugin plugin) {
        this.plugin = plugin;

        TierMappingsConfig mappings = tierMappingsConfig.get();
        int fromDefaults = mappings.mergeDefaults();

        plugin.getEventRegistry().register(AllNPCsLoadedEvent.class, this::onNPCsLoaded);

        if (fromDefaults > 0) {
            tierMappingsConfig.save();
            plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs: merged %d mobs from defaults", fromDefaults);
        }

        EcotaleJobsConfig config = earningsConfig;

        RewardNotifier.configure(
            config.getNotifications().isShowRewards(),
            config.getNotifications().getMinRewardToShow(),
            null,
            config.getNotifications().isShowMobKillBreakdown());

        lastAttackerSystem = new MobLastAttackerSystem();
        plugin.getEntityStoreRegistry().registerSystem(lastAttackerSystem);

        mobRewardSystem = new MobRewardSystem(lastAttackerSystem);
        mobRewardSystem.init(config.getMobKills(), mappings);

        plugin.getEntityStoreRegistry().registerSystem(mobRewardSystem);

        plugin.getCommandRegistry().registerCommand(new TestOresCommand());
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module loaded.");
        boolean econDebug = plugin instanceof VaryonEcotalePlugin v
            && v.getEconomyConfig() != null
            && v.getEconomyConfig().isDebugMode();
        boolean earningsDebug = config.isDebugMode();
        if (econDebug || earningsDebug) {
            JobsLogger.bannerInfo(
                "[CONFIG] mob traces ON (config.json DebugMode=%s, earnings DebugMode=%s)",
                econDebug,
                earningsDebug);
        }
    }

    /**
     * Hot-reload earnings YAML and JSON mappings; re-applies running reward systems (no server restart).
     */
    public void reloadFromDisk(JavaPlugin plugin) {
        Path earningsPath = plugin.getDataDirectory().resolve("earnings_config.yml");
        EarningsConfigLoader.installDefaultYamlIfMissing(earningsPath, plugin.getLogger(), VaryonEcotalePlugin.class);
        this.earningsConfig = EarningsConfigLoader.load(earningsPath, plugin.getLogger());

        tierMappingsConfig.load();
        craftingMappingsConfig.load();

        EcotaleJobsConfig cfg = this.earningsConfig;
        RewardNotifier.configure(
            cfg.getNotifications().isShowRewards(),
            cfg.getNotifications().getMinRewardToShow(),
            null,
            cfg.getNotifications().isShowMobKillBreakdown());

        TierMappingsConfig mappings = tierMappingsConfig.get();

        if (mobRewardSystem != null) {
            mobRewardSystem.init(cfg.getMobKills(), mappings);
        }

        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs config reloaded from disk.");
        boolean econDebug = plugin instanceof VaryonEcotalePlugin v
            && v.getEconomyConfig() != null
            && v.getEconomyConfig().isDebugMode();
        boolean earningsDebug = cfg.isDebugMode();
        if (econDebug || earningsDebug) {
            JobsLogger.bannerInfo(
                "[CONFIG] reload mob traces ON (config.json DebugMode=%s, earnings DebugMode=%s)",
                econDebug,
                earningsDebug);
        }
    }

    private void onNPCsLoaded(AllNPCsLoadedEvent event) {
        TierMappingsConfig mappings = tierMappingsConfig.get();
        if (!mappings.isAutoMergeNewMobs()) return;

        Map<String, String> detected = NPCAutoDetector.detectNewNPCs(mappings);
        int added = 0;
        for (Map.Entry<String, String> e : detected.entrySet()) {
            if (!mappings.getTierMappings().containsKey(e.getKey())) {
                mappings.addMapping(e.getKey(), e.getValue());
                added++;
            }
        }
        if (added > 0) {
            tierMappingsConfig.save();
            plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs: auto-detected %d new NPCs", added);
        }
    }

    @Override
    public void shutdown() {
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module shutdown.");
    }

    public EcotaleJobsConfig getConfig() { return earningsConfig; }
    public TierMappingsConfig getTierMappings() { return tierMappingsConfig.get(); }
    public CraftingMappingsConfig getCraftingMappings() { return craftingMappingsConfig.get(); }
    public MobRewardSystem getMobRewardSystem() { return mobRewardSystem; }
}
