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
import fr.varyon.ecotale.jobs.util.RewardNotifier;
import fr.varyon.ecotale.shared.ModuleInitializer;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.AllNPCsLoadedEvent;

import java.util.Map;
import java.util.logging.Level;

public class JobsModule implements ModuleInitializer {

    private final EcotaleJobsConfig earningsConfig;
    private final Config<TierMappingsConfig> tierMappingsConfig;
    private final Config<CraftingMappingsConfig> craftingMappingsConfig;

    private MobRewardSystem mobRewardSystem;
    private MiningRewardSystem miningRewardSystem;
    private CraftingRewardSystem craftingRewardSystem;

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
        boolean craftingEnabled = config.getCrafting().isEnabled();

        if (craftingEnabled) {
            plugin.getEventRegistry().register(LoadedAssetsEvent.class, CraftingRecipe.class, this::onRecipesLoaded);
        }

        RewardNotifier.configure(
            config.getNotifications().isShowRewards(),
            config.getNotifications().getMinRewardToShow(),
            null);

        mobRewardSystem = new MobRewardSystem();
        mobRewardSystem.init(config.getMobKills(), mappings);

        CraftingMappingsConfig craftingMappings = null;
        if (craftingEnabled) {
            craftingMappings = craftingMappingsConfig.get();
            craftingRewardSystem = new CraftingRewardSystem();
            craftingRewardSystem.init(config.getCrafting(), craftingMappings);
        }

        plugin.getEntityStoreRegistry().registerSystem(mobRewardSystem);
        if (craftingEnabled && craftingRewardSystem != null) {
            plugin.getEntityStoreRegistry().registerSystem(craftingRewardSystem);
        }

        boolean miningEnabled = config.getMining().isEnabled();
        if (miningEnabled) {
            miningRewardSystem = new MiningRewardSystem();
            miningRewardSystem.init(config.getMining());
            plugin.getEntityStoreRegistry().registerSystem(miningRewardSystem);
        }

        if (craftingEnabled) craftingMappingsConfig.save();

        plugin.getCommandRegistry().registerCommand(new TestOresCommand());
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs module loaded.");
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

    private void onRecipesLoaded(LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) {
        CraftingMappingsConfig craftingMappings = craftingMappingsConfig.get();
        if (!craftingMappings.isAutoDetectNewRecipes()) return;

        Map<String, CraftingRecipe> loaded = event.getLoadedAssets();
        Map<String, String> detected = CraftingAutoDetector.processLoadedRecipes(loaded, craftingMappings);
        int added = 0;
        for (Map.Entry<String, String> e : detected.entrySet()) {
            craftingMappings.addItemMapping(e.getKey(), e.getValue());
            added++;
        }
        if (added > 0) {
            craftingMappingsConfig.save();
            if (craftingRewardSystem != null) craftingRewardSystem.refreshMappings(craftingMappings);
            plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Jobs: auto-detected %d new recipes", added);
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
    public CraftingRewardSystem getCraftingRewardSystem() { return craftingRewardSystem; }
    public MiningRewardSystem getMiningRewardSystem() { return miningRewardSystem; }
}
