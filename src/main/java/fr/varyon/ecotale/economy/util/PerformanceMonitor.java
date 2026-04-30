package fr.varyon.ecotale.economy.util;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.EconomyManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors economy-specific performance metrics (cache, database latency).
 */
public class PerformanceMonitor {
    private static PerformanceMonitor instance;
    private final ScheduledExecutorService scheduler;
    
    // Economy Metrics
    private int cachedPlayers;
    
    public PerformanceMonitor() {
        instance = this;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("Ecotale-EconomyMetrics-", 0).factory()
        );
        this.scheduler.scheduleAtFixedRate(this::updateMetrics, 0, 10, TimeUnit.SECONDS);
    }
    
    private void updateMetrics() {
        try {
            EconomyManager em = VaryonEcotalePlugin.getInstance().getEconomyManager();
            if (em != null) {
                this.cachedPlayers = em.getCachedPlayerCount();
            }
        } catch (Exception ignored) {}
    }
    
    public int getCachedPlayers() { return cachedPlayers; }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    public static PerformanceMonitor getInstance() {
        return instance;
    }
}
