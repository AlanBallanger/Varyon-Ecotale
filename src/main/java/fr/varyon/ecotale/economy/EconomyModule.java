package fr.varyon.ecotale.economy;

import fr.varyon.ecotale.economy.commands.BalanceCommand;
import fr.varyon.ecotale.economy.commands.EcoAdminCommand;
import fr.varyon.ecotale.economy.commands.EcotaleVaryonFactionDepositCommand;
import fr.varyon.ecotale.economy.commands.PayCommand;
import fr.varyon.ecotale.economy.config.EcotaleConfig;
import fr.varyon.ecotale.economy.hud.BalanceHud;
import fr.varyon.ecotale.economy.lib.vaultunlocked.VaultUnlockedPlugin;
import fr.varyon.ecotale.economy.systems.BalanceHudSystem;
import fr.varyon.ecotale.economy.util.HudHelper;
import fr.varyon.ecotale.economy.util.PerformanceMonitor;
import fr.varyon.ecotale.shared.ModuleInitializer;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;

import java.util.logging.Level;

public class EconomyModule implements ModuleInitializer {

    private final Config<EcotaleConfig> configHolder;
    private EconomyManager economyManager;
    private JavaPlugin plugin;

    public EconomyModule(Config<EcotaleConfig> configHolder) {
        this.configHolder = configHolder;
    }

    @Override
    public void setup(JavaPlugin plugin) {
        this.plugin = plugin;
        configHolder.save();

        this.economyManager = new EconomyManager(plugin);

        initVaultUnlocked();
        HudHelper.init();

        plugin.getCommandRegistry().registerCommand(new BalanceCommand());
        plugin.getCommandRegistry().registerCommand(new PayCommand());
        plugin.getCommandRegistry().registerCommand(new EcoAdminCommand());
        plugin.getCommandRegistry().registerCommand(new EcotaleVaryonFactionDepositCommand());

        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
            Player player = event.getHolder().getComponent(Player.getComponentType());
            PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
            if (player == null || playerRef == null) return;

            economyManager.ensureAccount(playerRef.getUuid());
            var h2 = economyManager.getH2Storage();
            if (h2 != null) h2.updatePlayerName(playerRef.getUuid(), playerRef.getUsername());

            if (configHolder.get().isEnableHudDisplay()) {
                BalanceHud existing = BalanceHudSystem.getHud(playerRef.getUuid());
                if (existing != null) {
                    HudHelper.setCustomHud(player, playerRef, existing);
                } else {
                    BalanceHud hud = new BalanceHud(playerRef);
                    HudHelper.setCustomHud(player, playerRef, hud);
                    BalanceHudSystem.registerHud(playerRef.getUuid(), hud);
                }
            }
        });

        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            var ref = event.getPlayerRef();
            if (ref != null) {
                BalanceHudSystem.removePlayerHud(ref.getUuid());
                economyManager.scheduleEviction(ref.getUuid());
                economyManager.resetRateLimit(ref.getUuid());
            }
        });

        new PerformanceMonitor();
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Economy module loaded.");
    }

    private void initVaultUnlocked() {
        if (HytaleServer.get().getPluginManager().hasPlugin(
                PluginIdentifier.fromString("TheNewEconomy:VaultUnlocked"),
                SemverRange.WILDCARD)) {
            plugin.getLogger().atInfo().log("[Varyon-Ecotale] VaultUnlocked detected, enabling support.");
            VaultUnlockedPlugin.setup(plugin.getLogger());
        }
    }

    @Override
    public void shutdown() {
        if (fr.varyon.ecotale.economy.security.SecurityLogger.getInstance() != null) {
            fr.varyon.ecotale.economy.security.SecurityLogger.getInstance().shutdown();
        }
        if (PerformanceMonitor.getInstance() != null) {
            PerformanceMonitor.getInstance().shutdown();
        }
        if (economyManager != null) economyManager.shutdown();
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Economy module shutdown.");
    }

    public EconomyManager getEconomyManager() { return economyManager; }
    public EcotaleConfig getConfig() { return configHolder.get(); }
}
