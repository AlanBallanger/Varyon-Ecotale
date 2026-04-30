package fr.varyon.ecotale.shared;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.EconomyManager;
import fr.varyon.ecotale.economy.PlayerBalance;
import fr.varyon.ecotale.economy.config.EcotaleConfig;
import fr.varyon.ecotale.economy.storage.H2StorageProvider;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EconomyBridge {

    private EconomyBridge() {}

    private static EconomyManager mgr() {
        EconomyManager m = VaryonEcotalePlugin.getInstance().getEconomyManager();
        if (m == null) throw new IllegalStateException("EconomyManager not initialized");
        return m;
    }

    private static EcotaleConfig cfg() {
        return VaryonEcotalePlugin.getInstance().getEconomyConfig();
    }

    public static double getBalance(@Nonnull UUID uuid) { return mgr().getBalance(uuid); }
    public static boolean hasBalance(@Nonnull UUID uuid, double amt) { return mgr().hasBalance(uuid, amt); }

    public static boolean deposit(@Nonnull UUID uuid, double amt, @Nonnull String reason) {
        return mgr().deposit(uuid, amt, reason);
    }
    public static boolean withdraw(@Nonnull UUID uuid, double amt, @Nonnull String reason) {
        return mgr().withdraw(uuid, amt, reason);
    }
    public static EconomyManager.TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amt, @Nonnull String reason) {
        return mgr().transfer(from, to, amt, reason);
    }
    public static void setBalance(@Nonnull UUID uuid, double amt, @Nonnull String reason) {
        mgr().setBalance(uuid, amt, reason);
    }

    public static String getCurrencySymbol() { return cfg().getCurrencySymbol(); }
    public static String getHudPrefix() { return cfg().getHudPrefix(); }
    public static String format(double amt) { return cfg().format(amt); }
    public static String getLanguage() { return cfg().getLanguage(); }
    public static boolean isUsePlayerLanguage() { return cfg().isUsePlayerLanguage(); }
    public static double getMaxBalance() { return cfg().getMaxBalance(); }

    public static List<PlayerBalance> getTopBalances(int limit) {
        var storage = mgr().getStorage();
        if (storage instanceof H2StorageProvider h2) return h2.getTopBalances(limit).join();
        return mgr().getAllBalances().values().stream()
            .sorted((a, b) -> Double.compare(b.getBalance(), a.getBalance()))
            .limit(limit).toList();
    }
    public static Set<UUID> getAllPlayerUUIDs() { return mgr().getAllBalances().keySet(); }
    public static double getTotalCirculating() {
        return mgr().getAllBalances().values().stream().mapToDouble(PlayerBalance::getBalance).sum();
    }
}
