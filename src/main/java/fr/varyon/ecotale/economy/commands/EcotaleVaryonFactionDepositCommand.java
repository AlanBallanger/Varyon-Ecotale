package fr.varyon.ecotale.economy.commands;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import fr.varyon.ecotale.integration.EcotaleFactionDepositIntegration;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class EcotaleVaryonFactionDepositCommand extends AbstractAsyncCommand {

    private final RequiredArg<String> uuidArg;
    private final RequiredArg<Double> amountArg;

    public EcotaleVaryonFactionDepositCommand() {
        super("varyonfactiondeposit", "Internal: sync faction bank after Varyon faction deposit");
        this.uuidArg = this.withRequiredArg("uuid", "Player UUID", ArgTypes.STRING);
        this.amountArg = this.withRequiredArg("amount", "Faction points deposited (bank credits floor(points/10))", ArgTypes.DOUBLE);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        CommandSender sender = ctx.sender();
        if (sender instanceof Player) {
            return CompletableFuture.completedFuture(null);
        }

        String uuidStr = uuidArg.get(ctx);
        Double amountD = amountArg.get(ctx);
        if (amountD == null || amountD <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr.trim());
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(null);
        }

        long amount = amountD.longValue();
        return CompletableFuture.runAsync(() -> EcotaleFactionDepositIntegration.onVaryonFactionDeposit(uuid, amount),
            HytaleServer.SCHEDULED_EXECUTOR);
    }
}
