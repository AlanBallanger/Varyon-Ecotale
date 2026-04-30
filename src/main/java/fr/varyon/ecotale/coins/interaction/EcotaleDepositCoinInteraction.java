package fr.varyon.ecotale.coins.interaction;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.coins.currency.BankManager;
import fr.varyon.ecotale.coins.currency.CoinType;
import fr.varyon.ecotale.shared.EconomyBridge;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.UUID;

public final class EcotaleDepositCoinInteraction extends SimpleInstantInteraction {

    public static final String INTERACTION_ID = "VaryonEcotale_DepositCoin";

    public static final BuilderCodec<EcotaleDepositCoinInteraction> CODEC =
        BuilderCodec.builder(
            EcotaleDepositCoinInteraction.class,
            EcotaleDepositCoinInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public EcotaleDepositCoinInteraction() {
        super(INTERACTION_ID);
    }

    public static void registerAssets(@Nonnull String packId) {
        RootInteraction root = new RootInteraction(INTERACTION_ID, INTERACTION_ID);
        RootInteraction.getAssetStore().loadAssets(packId, List.of(root));
        Interaction.getAssetStore().loadAssets(packId, List.of(new EcotaleDepositCoinInteraction()));
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = context.getEntity();
        if (ref == null || !ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (!player.hasPermission("ecotale.ecotalecoins.command.bank")) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUID playerUuid = playerRef.getUuid();

        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null || heldItem.isEmpty()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        CoinType coin = CoinType.fromItemId(heldItem.getItemId());
        if (coin == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        int quantity = heldItem.getQuantity();
        long value = coin.getValue() * (long) quantity;

        ItemContainer container = context.getHeldItemContainer();
        if (container == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        short slot = (short) context.getHeldItemSlot();

        var bankLock = BankManager.getPlayerLock(playerUuid);
        bankLock.lock();
        try {
            ItemStackSlotTransaction removed = container.removeItemStackFromSlot(slot, quantity);
            if (!removed.succeeded()) {
                context.getState().state = InteractionState.Failed;
                return;
            }

            if (!EconomyBridge.deposit(playerUuid, (double) value, "Bank deposit (coin Secondary interaction)")) {
                container.setItemStackForSlot(slot, heldItem.withQuantity(quantity));
                context.getState().state = InteractionState.Failed;
                player.sendMessage(Message.raw("Impossible de déposer (banque ou solde maximal).").color(Color.RED));
                return;
            }
        } finally {
            bankLock.unlock();
        }

        context.getState().state = InteractionState.Finished;

        long bank = BankManager.getBankBalance(playerUuid);
        var plugin = VaryonEcotalePlugin.getInstance();
        var economyConfig = plugin != null ? plugin.getEconomyConfig() : null;
        String depStr = economyConfig != null ? economyConfig.format((double) value) : String.valueOf(value);
        String bankStr = economyConfig != null ? economyConfig.format((double) bank) : String.valueOf(bank);
        player.sendMessage(Message.join(
            Message.raw(depStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" déposé. ").color(Color.GREEN),
            Message.raw(bankStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" total en banque. ").color(Color.GREEN),
            Message.raw("Tape /bank pour ouvrir ta banque.").color(Color.GRAY)
        ));
    }
}
