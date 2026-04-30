package fr.varyon.ecotale.economy.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.PlayerBalance;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

/**
 * Balance command - shows player's current balance
 */
public class BalanceCommand extends AbstractAsyncCommand {
    
    public BalanceCommand() {
        super("bal", "Check your balance");
        this.addAliases("balance", "money");
    }
    
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!(sender instanceof Player player)) {
            commandContext.sendMessage(Message.raw("This command can only be used by players.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            commandContext.sendMessage(Message.raw("Error: Could not get player data.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                player.sendMessage(Message.raw("Error: Could not get player data.").color(Color.RED));
                return;
            }

            VaryonEcotalePlugin.getInstance().getEconomyManager().ensureAccount(playerRef.getUuid());
            PlayerBalance balance = VaryonEcotalePlugin.getInstance().getEconomyManager().getPlayerBalance(playerRef.getUuid());

            if (balance == null) {
                player.sendMessage(Message.raw("Error: Could not load balance.").color(Color.RED));
                return;
            }

            String formattedBalance = VaryonEcotalePlugin.getInstance().getEconomyConfig().format(balance.getBalance());
            String earnedStr = VaryonEcotalePlugin.getInstance().getEconomyConfig().formatShort(balance.getTotalEarned());
            String spentStr = VaryonEcotalePlugin.getInstance().getEconomyConfig().formatShort(balance.getTotalSpent());

            player.sendMessage(Message.raw("----- Your Balance -----").color(new Color(255, 215, 0)));
            player.sendMessage(Message.join(
                Message.raw("  Balance: ").color(Color.GRAY),
                Message.raw(formattedBalance).color(new Color(50, 205, 50)).bold(true)
            ));

        }, player.getWorld());
    }
}
