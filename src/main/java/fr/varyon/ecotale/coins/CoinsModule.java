package fr.varyon.ecotale.coins;

import fr.varyon.ecotale.coins.commands.BankCommand;
import fr.varyon.ecotale.coins.config.CoinConfig;
import fr.varyon.ecotale.coins.currency.CoinAssetManager;
import fr.varyon.ecotale.shared.ModuleInitializer;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.nio.file.Path;
import java.util.logging.Level;

public class CoinsModule implements ModuleInitializer {

    private CoinConfig coinConfig;
    private CoinAssetManager coinAssetManager;
    private boolean enabled = false;
    private JavaPlugin plugin;

    @Override
    public void setup(JavaPlugin plugin) {
        this.plugin = plugin;

        Path configPath = plugin.getDataDirectory().resolve("Physical_Currency.json");
        this.coinConfig = new CoinConfig(configPath, plugin.getLogger());

        if (!coinConfig.load()) {
            plugin.getLogger().at(Level.SEVERE).log("[Varyon-Ecotale] Failed to load Physical_Currency.json, disabling coins module.");
            return;
        }

        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins enabled:");
        coinConfig.getEnabledCoinsInOrder().forEach((name, cfg) ->
            plugin.getLogger().at(Level.INFO).log("  - " + cfg.displayName + ": " + cfg.value + " base units"));

        Path assetPackPath = plugin.getDataDirectory().getParent().resolve("Varyon_Varyon-Ecotale");
        this.coinAssetManager = new CoinAssetManager(assetPackPath, plugin.getLogger());
        this.coinAssetManager.initialize();

        plugin.getCommandRegistry().registerCommand(new BankCommand());

        this.enabled = true;

        if (coinAssetManager.isFirstTimeSetup()) {
            scheduleFirstTimeRestart();
            return;
        }

        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins module loaded.");
    }

    private void scheduleFirstTimeRestart() {
        new Thread(() -> {
            try {
                HytaleServer s = HytaleServer.get();
                while (!s.isBooted() && !s.isShuttingDown()) Thread.sleep(100);
                if (s.isShuttingDown()) return;
                Thread.sleep(2000);
                plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins first-time setup, restarting in 5s...");
                Thread.sleep(5000);
                s.shutdownServer(ShutdownReason.SHUTDOWN.withMessage("Varyon-Ecotale Coins first-time setup."));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "VaryonEcotale-CoinsFirstTimeSetup").start();
    }

    @Override
    public void shutdown() {
        plugin.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Coins module shutdown.");
    }

    public boolean isEnabled() { return enabled; }
    public CoinConfig getCoinConfig() { return coinConfig; }
    public CoinAssetManager getCoinAssetManager() { return coinAssetManager; }
}
