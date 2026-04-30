package fr.varyon.ecotale.shared;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.coins.CoinsModule;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class CoinsBridge {

    private CoinsBridge() {}

    public static boolean isAvailable() {
        VaryonEcotalePlugin p = VaryonEcotalePlugin.getInstance();
        return p != null && p.getCoinsModule() != null && p.getCoinsModule().isEnabled();
    }

    public static long countInInventory(@Nonnull Player player) {
        return fr.varyon.ecotale.coins.currency.CoinManager.countCoins(player);
    }
    public static boolean canAfford(@Nonnull Player player, long amount) {
        return fr.varyon.ecotale.coins.currency.CoinManager.canAfford(player, amount);
    }
    public static boolean giveCoins(@Nonnull Player player, long amount) {
        return fr.varyon.ecotale.coins.currency.CoinManager.giveCoins(player, amount);
    }
    public static boolean takeCoins(@Nonnull Player player, long amount) {
        return fr.varyon.ecotale.coins.currency.CoinManager.takeCoins(player, amount);
    }
    public static void dropCoins(@Nonnull ComponentAccessor<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> cb,
                                 @Nonnull Vector3d pos, long amount) {
        fr.varyon.ecotale.coins.currency.CoinDropper.dropCoins(store, cb, pos, amount);
    }
    public static void dropCoinsAtEntity(@Nonnull Ref<EntityStore> ref,
                                         @Nonnull ComponentAccessor<EntityStore> store,
                                         @Nonnull CommandBuffer<EntityStore> cb,
                                         long amount) {
        fr.varyon.ecotale.coins.currency.CoinDropper.dropCoinsAtEntity(ref, store, cb, amount);
    }
    public static long getBankBalance(@Nonnull UUID uuid) {
        return fr.varyon.ecotale.coins.currency.BankManager.getBankBalance(uuid);
    }
}
