package fr.varyon.ecotale.coins.interaction;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.coins.BankPermissionHelper;
import fr.varyon.ecotale.coins.currency.BankManager;
import fr.varyon.ecotale.coins.currency.CoinType;
import fr.varyon.ecotale.coins.currency.TokenType;
import fr.varyon.ecotale.economy.EconomyManager;
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

        if (!BankPermissionHelper.canDeposit(player)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        if (!BankPermissionHelper.canDepositRightClick(player)) {
            context.getState().state = InteractionState.Failed;
            player.sendMessage(Message.raw("Tu n'as pas la permission de déposer à la banque au clic droit.").color(Color.RED));
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

        TokenType token = TokenType.fromItemId(heldItem.getItemId());
        if (token != null) {
            handleTokenDeposit(context, player, playerUuid, heldItem, token);
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
        String depStr = economyConfig != null
            ? economyConfig.formatTrailingSymbolLong(value)
            : value + " Coins";
        String bankStr = economyConfig != null
            ? economyConfig.formatTrailingSymbolLong(bank)
            : bank + " Coins";
        player.sendMessage(Message.join(
            Message.raw(depStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" déposé, ").color(Color.GREEN),
            Message.raw(bankStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" au total en banque. ").color(Color.GREEN),
            Message.raw("Tape /bank pour ouvrir ta banque.").color(Color.GRAY)
        ));
    }

    private void handleTokenDeposit(
        @Nonnull InteractionContext context,
        @Nonnull Player player,
        @Nonnull UUID playerUuid,
        @Nonnull ItemStack heldItem,
        @Nonnull TokenType token
    ) {
        int quantity = heldItem.getQuantity();

        ItemContainer container = context.getHeldItemContainer();
        if (container == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        short slot = (short) context.getHeldItemSlot();
        EconomyManager economy = VaryonEcotalePlugin.getInstance() != null
            ? VaryonEcotalePlugin.getInstance().getEconomyManager()
            : null;
        if (economy == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        var bankLock = BankManager.getPlayerLock(playerUuid);
        bankLock.lock();
        try {
            ItemStackSlotTransaction removed = container.removeItemStackFromSlot(slot, quantity);
            if (!removed.succeeded()) {
                context.getState().state = InteractionState.Failed;
                return;
            }

            if (!economy.depositToken(playerUuid, token, quantity, "Token deposit (Secondary interaction)")) {
                container.setItemStackForSlot(slot, heldItem.withQuantity(quantity));
                context.getState().state = InteractionState.Failed;
                player.sendMessage(Message.raw("Impossible de déposer ce jeton.").color(Color.RED));
                return;
            }
        } finally {
            bankLock.unlock();
        }

        context.getState().state = InteractionState.Finished;

        long total = economy.getTokenBalance(playerUuid, token);
        var cfgPlugin = VaryonEcotalePlugin.getInstance();
        var economyConfig = cfgPlugin != null ? cfgPlugin.getEconomyConfig() : null;
        String tokenFr = depositTokenLabelFr(token);
        String depStr = economyConfig != null
            ? economyConfig.formatGroupedLong(quantity) + " " + tokenFr
            : quantity + " " + tokenFr;
        String bankStr = economyConfig != null
            ? economyConfig.formatGroupedLong(total) + " " + tokenFr
            : total + " " + tokenFr;
        player.sendMessage(Message.join(
            Message.raw(depStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" déposé, ").color(Color.GREEN),
            Message.raw(bankStr).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" au total en banque. ").color(Color.GREEN),
            Message.raw("Tape /bank pour ouvrir ta banque.").color(Color.GRAY)
        ));
    }

    private static String depositTokenLabelFr(TokenType token) {
        return switch (token) {
            case COINCOIN -> "Jeton CoinCoin";
            case BUILDING -> "Jeton Construction";
            case FACTION -> "Jeton Faction";
        };
    }
}
