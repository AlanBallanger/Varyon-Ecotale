package fr.varyon.ecotale.coins;

import com.hypixel.hytale.server.core.entity.entities.Player;

public final class BankPermissionHelper {

    private static final String LEGACY_BANK = "ecotale.ecotalecoins.command.bank";
    private static final String VARYON_BANK = "varyon.varyon-ecotale.command.bank";
    private static final String VARYON_DEPOSIT = "varyon.varyon-ecotale.command.bank.deposit";
    private static final String VARYON_DEPOSIT_RIGHTCLICK = "varyon.varyon-ecotale.bank.deposit-rightclick";
    private static final String VARYON_WITHDRAW = "varyon.varyon-ecotale.command.bank.withdraw";

    private BankPermissionHelper() {}

    public static boolean canUseBankUi(Player player) {
        return player.hasPermission(LEGACY_BANK) || player.hasPermission(VARYON_BANK);
    }

    public static boolean canDeposit(Player player) {
        return player.hasPermission(LEGACY_BANK)
            || player.hasPermission(VARYON_BANK)
            || player.hasPermission(VARYON_DEPOSIT);
    }

    public static boolean canDepositRightClick(Player player) {
        return player.hasPermission(VARYON_DEPOSIT_RIGHTCLICK);
    }

    public static boolean canWithdraw(Player player) {
        return player.hasPermission(LEGACY_BANK)
            || player.hasPermission(VARYON_BANK)
            || player.hasPermission(VARYON_WITHDRAW);
    }
}
