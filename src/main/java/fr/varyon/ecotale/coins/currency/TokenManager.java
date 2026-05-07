package fr.varyon.ecotale.coins.currency;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages physical token operations in player inventory.
 * Each TokenType is a separate currency: no conversion between them.
 */
public final class TokenManager {

    private static final Logger LOGGER = Logger.getLogger("EcotaleTokens");
    private static final int MAX_STACK_SIZE = 999;

    private TokenManager() {}

    public static int countTokens(@Nonnull Player player, @Nonnull TokenType type) {
        Inventory inv = player.getInventory();
        long total = 0L;
        total += countInContainer(inv.getStorage(), type);
        total += countInContainer(inv.getHotbar(), type);
        total += countInContainer(inv.getBackpack(), type);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static Map<TokenType, Integer> getBreakdown(@Nonnull Player player) {
        Map<TokenType, Integer> map = new EnumMap<>(TokenType.class);
        for (TokenType t : TokenType.values()) {
            map.put(t, countTokens(player, t));
        }
        return map;
    }

    private static long countInContainer(@Nonnull ItemContainer container, @Nonnull TokenType type) {
        long total = 0L;
        for (short i = 0; i < container.getCapacity(); i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && !stack.isEmpty() && type.getItemId().equals(stack.getItemId())) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    public static int countFreeStorageSlots(@Nonnull Player player) {
        Inventory inv = player.getInventory();
        int free = 0;
        ItemContainer storage = inv.getStorage();
        for (short i = 0; i < storage.getCapacity(); i++) {
            ItemStack stack = storage.getItemStack(i);
            if (stack == null || stack.isEmpty()) free++;
        }
        return free;
    }

    public static int spaceInExistingStacks(@Nonnull Player player, @Nonnull TokenType type) {
        Inventory inv = player.getInventory();
        int space = 0;
        ItemContainer storage = inv.getStorage();
        for (short i = 0; i < storage.getCapacity(); i++) {
            ItemStack stack = storage.getItemStack(i);
            if (stack != null && !stack.isEmpty() && type.getItemId().equals(stack.getItemId())) {
                space += Math.max(0, MAX_STACK_SIZE - stack.getQuantity());
            }
        }
        return space;
    }

    public static boolean canFit(@Nonnull Player player, @Nonnull TokenType type, int count) {
        if (count <= 0) return true;
        int existing = spaceInExistingStacks(player, type);
        int needsNew = count - existing;
        if (needsNew <= 0) return true;
        int slotsNeeded = (needsNew + MAX_STACK_SIZE - 1) / MAX_STACK_SIZE;
        return slotsNeeded <= countFreeStorageSlots(player);
    }

    public static boolean giveTokens(@Nonnull Player player, @Nonnull TokenType type, int count) {
        if (count <= 0) return true;
        if (!canFit(player, type, count)) {
            return false;
        }

        Inventory inv = player.getInventory();
        ItemContainer storage = inv.getStorage();
        int remaining = count;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, MAX_STACK_SIZE);
            ItemStack stack = new ItemStack(type.getItemId(), stackSize);
            ItemStackTransaction tx = storage.addItemStack(stack);
            if (!tx.succeeded()) {
                return false;
            }
            remaining -= stackSize;
        }
        return true;
    }

    public static boolean takeTokens(@Nonnull Player player, @Nonnull TokenType type, int count) {
        if (count <= 0) return true;
        int have = countTokens(player, type);
        if (have < count) return false;

        Inventory inv = player.getInventory();
        int remaining = count;
        remaining = removeFromContainer(inv.getStorage(), type, remaining);
        remaining = removeFromContainer(inv.getHotbar(), type, remaining);
        remaining = removeFromContainer(inv.getBackpack(), type, remaining);
        return remaining <= 0;
    }

    private static int removeFromContainer(@Nonnull ItemContainer container, @Nonnull TokenType type, int remaining) {
        if (remaining <= 0) return 0;
        for (short i = 0; i < container.getCapacity(); i++) {
            if (remaining <= 0) break;
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || !type.getItemId().equals(stack.getItemId())) continue;
            int qty = stack.getQuantity();
            if (qty <= remaining) {
                container.removeItemStack(stack);
                remaining -= qty;
            } else {
                container.setItemStackForSlot(i, stack.withQuantity(qty - remaining));
                remaining = 0;
            }
        }
        return remaining;
    }
}
