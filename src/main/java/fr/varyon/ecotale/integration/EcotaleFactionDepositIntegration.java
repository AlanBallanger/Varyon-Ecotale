package fr.varyon.ecotale.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.coins.currency.TokenType;
import fr.varyon.ecotale.economy.EconomyManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public final class EcotaleFactionDepositIntegration {

    private EcotaleFactionDepositIntegration() {}

    public static void onVaryonFactionDeposit(@Nonnull UUID playerUuid, long depositedFactionPoints) {
        if (depositedFactionPoints <= 0) {
            return;
        }
        long factionTokens = Math.floorDiv(depositedFactionPoints, 10);
        if (factionTokens <= 0) {
            return;
        }
        VaryonEcotalePlugin plugin = VaryonEcotalePlugin.getInstance();
        if (plugin == null) {
            return;
        }
        EconomyManager economy = plugin.getEconomyManager();
        if (economy == null) {
            return;
        }
        PlayerRef online = Universe.get().getPlayer(playerUuid);
        if (!economy.depositToken(playerUuid, TokenType.FACTION, factionTokens,
            "Varyon faction points deposit")) {
            if (online != null && online.isValid()) {
                online.sendMessage(Message.raw(
                    "Banque : impossible d'ajouter les jetons faction (débordement ?).").color(Color.RED));
            }
            return;
        }
        if (online != null && online.isValid()) {
            online.sendMessage(Message.raw(
                "Banque : jetons faction enregistrés (" + factionTokens + ").").color(Color.GREEN));
        }
    }
}
