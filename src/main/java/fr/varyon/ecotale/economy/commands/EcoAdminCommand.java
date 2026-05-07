package fr.varyon.ecotale.economy.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.coins.currency.TokenType;
import fr.varyon.ecotale.economy.PlayerBalance;
import fr.varyon.ecotale.economy.gui.EcoAdminGui;
import fr.varyon.ecotale.economy.hud.BalanceHud;
import fr.varyon.ecotale.economy.systems.BalanceHudSystem;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Economy admin commands for managing and testing balances.
 * 
 * Commands:
 * - /eco set <player> <amount> - Set a player's balance
 * - /eco give <player> <amount> - Add to a player's balance
 * - /eco take <player> <amount> - Remove from a player's balance
 * - /eco reset - Reset to starting balance
 * - /eco top - Show top balances
 * - /eco save - Force save data
 * - /eco reload - Hot-reload config files from disk
 */
public class EcoAdminCommand extends AbstractAsyncCommand {
    
    public EcoAdminCommand() {
        super("eco", "Economy administration commands");
        this.addAliases("economy", "ecoadmin");
        this.setPermissionGroup(null); // Admin only - requires ecotale.ecotale.command.eco permission
        
        this.addSubCommand(new EcoSetCommand());
        this.addSubCommand(new EcoGiveCommand());
        this.addSubCommand(new EcoTakeCommand());
        this.addSubCommand(new EcoResetCommand());
        this.addSubCommand(new EcoTopCommand());
        this.addSubCommand(new EcoSaveCommand());
        this.addSubCommand(new EcoReloadCommand());
        this.addSubCommand(new EcoHudCommand());
        this.addSubCommand(new EcoMetricsCommand());
        this.addSubCommand(new EcoSetTokenCommand());
        this.addSubCommand(new EcoGiveTokenCommand());
        this.addSubCommand(new EcoTakeTokenCommand());
    }
    
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        
        // Open admin GUI if player
        if (sender instanceof Player player) {
            var ref = player.getReference();
            if (ref != null && ref.isValid()) {
                var store = ref.getStore();
                var world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                CompletableFuture<Void> future = new CompletableFuture<>();
                world.execute(() -> {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        player.getPageManager().openCustomPage(ref, store, new EcoAdminGui(playerRef));
                    }
                    future.complete(null);
                });
                return future;
            }
        }
        
        // Fallback: show help text
        commandContext.sender().sendMessage(Message.raw("=== Ecotale Economy Admin ===").color(new Color(255, 215, 0)));
        commandContext.sender().sendMessage(Message.raw("  /eco set <player> <amount> - Set player's balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco give <player> <amount> - Add to player's balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco take <player> <amount> - Remove from player's balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco reset - Reset to starting balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco top - Show top balances").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco metrics - Show performance stats").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco save - Force save data").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco reload - Reload configs from disk (jobs, coins, mappings)").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco settoken <type> <player> <amount> - Set token balance (coincoin/building/faction)").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco givetoken <type> <player> <amount> - Add tokens to player").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco taketoken <type> <player> <amount> - Remove tokens from player").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }
    
    // ========== SET COMMAND ==========
    private static class EcoSetCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoSetCommand() {
            super("set", "Set a player's balance to a specific amount");
            this.playerNameArg = this.withRequiredArg("player", "Player name (online or offline if stored)", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "The amount to set", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            String playerName = playerNameArg.get(ctx);
            Double amount = amountArg.get(ctx);
            if (amount == null || amount < 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                var economyManager = VaryonEcotalePlugin.getInstance().getEconomyManager();
                double oldBalance = economyManager.getBalanceLoadingStorage(targetUuid);
                economyManager.setBalance(targetUuid, amount, "Admin set");
                updateHud(targetUuid, amount);

                sender.sendMessage(Message.join(
                    Message.raw("Set ").color(Color.GREEN),
                    Message.raw(resolved.displayName).color(Color.WHITE),
                    Message.raw(": ").color(Color.GRAY),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(oldBalance)).color(Color.GRAY),
                    Message.raw(" -> ").color(Color.WHITE),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(50, 205, 50))
                ));
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }
    
    // ========== GIVE COMMAND ==========
    private static class EcoGiveCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoGiveCommand() {
            super("give", "Add money to a player's balance");
            this.addAliases("add");
            this.playerNameArg = this.withRequiredArg("player", "Player name (online or offline if stored)", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "The amount to add", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            String playerName = playerNameArg.get(ctx);

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                VaryonEcotalePlugin.getInstance().getEconomyManager().deposit(targetUuid, amount, "Admin give");
                double newBalance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(targetUuid);
                updateHud(targetUuid, newBalance);

                sender.sendMessage(Message.join(
                    Message.raw("Added ").color(Color.GREEN),
                    Message.raw("+" + VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(50, 205, 50)),
                    Message.raw(" to ").color(Color.GRAY),
                    Message.raw(resolved.displayName).color(Color.WHITE),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(newBalance)).color(Color.WHITE)
                ));
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) {
                    return CompletableFuture.completedFuture(null);
                }
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }
    
    // ========== TAKE COMMAND ==========
    private static class EcoTakeCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoTakeCommand() {
            super("take", "Remove money from a player's balance");
            this.addAliases("remove");
            this.playerNameArg = this.withRequiredArg("player", "Player name (online or offline if stored)", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "The amount to remove", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            String playerName = playerNameArg.get(ctx);

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                boolean success = VaryonEcotalePlugin.getInstance().getEconomyManager().withdraw(targetUuid, amount, "Admin take");
                double newBalance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(targetUuid);
                updateHud(targetUuid, newBalance);

                if (success) {
                    sender.sendMessage(Message.join(
                        Message.raw("Removed ").color(Color.YELLOW),
                        Message.raw("-" + VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(255, 99, 71)),
                        Message.raw(" from ").color(Color.GRAY),
                        Message.raw(resolved.displayName).color(Color.WHITE),
                        Message.raw(" | New balance: ").color(Color.GRAY),
                        Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(newBalance)).color(Color.WHITE)
                    ));
                } else {
                    sender.sendMessage(Message.join(
                        Message.raw("Insufficient funds or no account for ").color(Color.RED),
                        Message.raw(resolved.displayName).color(Color.WHITE)
                    ));
                }
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }
    
    // ========== RESET COMMAND ==========
    private static class EcoResetCommand extends AbstractAsyncCommand {
        public EcoResetCommand() {
            super("reset", "Reset balance to starting amount");
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            
            var store = ref.getStore();
            var world = store.getExternalData().getWorld();
            
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                
                double startingBalance = VaryonEcotalePlugin.getInstance().getEconomyConfig().getStartingBalance();
                VaryonEcotalePlugin.getInstance().getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");
                updateHud(playerRef.getUuid(), startingBalance);
                
                player.sendMessage(Message.join(
                    Message.raw("Balance reset to ").color(Color.GREEN),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(startingBalance)).color(new Color(50, 205, 50))
                ));
            }, world);
        }
    }
    
    // ========== TOP COMMAND ==========
    private static class EcoTopCommand extends AbstractAsyncCommand {
        public EcoTopCommand() {
            super("top", "Show top balances");
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Map<UUID, PlayerBalance> balances = VaryonEcotalePlugin.getInstance().getEconomyManager().getAllBalances();
            
            if (balances.isEmpty()) {
                ctx.sendMessage(Message.raw("No player balances found").color(Color.GRAY));
                return CompletableFuture.completedFuture(null);
            }
            
            ctx.sendMessage(Message.raw("=== Top Balances ===").color(new Color(255, 215, 0)));
            
            var h2Storage = VaryonEcotalePlugin.getInstance().getEconomyManager().getH2Storage();
            
            // Get top 10 sorted by balance
            List<PlayerBalance> top10 = balances.values().stream()
                .sorted(Comparator.comparingDouble(PlayerBalance::getBalance).reversed())
                .limit(10)
                .toList();
            
            // Build name resolution futures
            List<CompletableFuture<String>> nameFutures = top10.stream()
                .map(balance -> {
                    if (h2Storage != null) {
                        return h2Storage.getPlayerNameAsync(balance.getPlayerUuid())
                            .thenApply(name -> name != null ? name : balance.getPlayerUuid().toString().substring(0, 8) + "...");
                    } else {
                        return CompletableFuture.completedFuture(balance.getPlayerUuid().toString().substring(0, 8) + "...");
                    }
                })
                .toList();
            
            // Wait for all names to resolve, then display
            return CompletableFuture.allOf(nameFutures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    for (int i = 0; i < top10.size(); i++) {
                        PlayerBalance balance = top10.get(i);
                        String displayName = nameFutures.get(i).join(); // Already completed
                        String formatted = VaryonEcotalePlugin.getInstance().getEconomyConfig().format(balance.getBalance());
                        ctx.sendMessage(Message.join(
                            Message.raw("#" + (i + 1) + " ").color(Color.GRAY),
                            Message.raw(displayName).color(Color.WHITE),
                            Message.raw(" - ").color(Color.GRAY),
                            Message.raw(formatted).color(new Color(50, 205, 50))
                        ));
                    }
                });
        }
    }
    
    // ========== SAVE COMMAND ==========
    private static class EcoSaveCommand extends AbstractAsyncCommand {
        public EcoSaveCommand() {
            super("save", "Force save all data");
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            VaryonEcotalePlugin.getInstance().getEconomyManager().forceSave();
            ctx.sendMessage(Message.raw("✓ Economy data saved successfully").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private static class EcoReloadCommand extends AbstractAsyncCommand {
        public EcoReloadCommand() {
            super("reload", "Reload economy and jobs configs from disk");
            this.addAliases("rl", "hotreload");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            VaryonEcotalePlugin.getInstance().reloadConfigurationFromDisk();
            ctx.sendMessage(Message.raw("Configuration reloaded from disk (config, mappings, earnings, coins). HUD refreshed.")
                .color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== HUD COMMAND ==========
    private static class EcoHudCommand extends AbstractAsyncCommand {
        public EcoHudCommand() {
            super("hud", "Toggle HUD display on/off");
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            var config = VaryonEcotalePlugin.getInstance().getEconomyConfig();
            boolean newValue = !config.isEnableHudDisplay();
            config.setEnableHudDisplay(newValue);
            
            String status = newValue ? "§aEnabled" : "§cDisabled";
            ctx.sendMessage(Message.raw("HUD Display: " + status).color(newValue ? Color.GREEN : Color.RED));
            ctx.sendMessage(Message.raw("Use /eco save to persist this change").color(Color.GRAY));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== METRICS COMMAND ==========
    private static class EcoMetricsCommand extends AbstractAsyncCommand {
        public EcoMetricsCommand() {
            super("metrics", "Show performance and scaling metrics");
            this.addAliases("stats", "perf");
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            var monitor = fr.varyon.ecotale.economy.util.PerformanceMonitor.getInstance();
            if (monitor != null) {
                Color gold = new Color(255, 215, 0);
                Color white = Color.WHITE;
                Color green = new Color(50, 205, 50);

                ctx.sendMessage(Message.raw("--- Ecotale Economy Metrics ---").color(gold));
                
                ctx.sendMessage(Message.join(
                    Message.raw("Cached Balances: ").color(white),
                    Message.raw(monitor.getCachedPlayers() + " / 1000").color(green)
                ));
                
                ctx.sendMessage(Message.raw("---------------------------------").color(gold));
                ctx.sendMessage(Message.raw("System metrics moved to /guard metrics").color(Color.GRAY));
            } else {
                ctx.sendMessage(Message.raw("Performance monitor is not active.").color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== HELPER METHODS ==========
    private static void updateHud(UUID playerUuid, double newBalance) {
        BalanceHud hud = BalanceHudSystem.getHud(playerUuid);
        if (hud != null) {
            hud.updateBalance(newBalance);
        }
    }

    private static TokenType parseTokenType(String raw) {
        return TokenType.fromKey(raw);
    }

    private static void sendTokenTypeError(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Unknown token type. Use: coincoin, building, faction").color(Color.RED));
    }

    // ========== SET TOKEN COMMAND ==========
    private static class EcoSetTokenCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> tokenTypeArg;
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoSetTokenCommand() {
            super("settoken", "Set a player's token balance for a specific token type");
            this.tokenTypeArg = this.withRequiredArg("type", "Token type (coincoin, building, faction)", ArgTypes.STRING);
            this.playerNameArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "Amount to set", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            TokenType type = parseTokenType(tokenTypeArg.get(ctx));
            if (type == null) {
                sendTokenTypeError(ctx);
                return CompletableFuture.completedFuture(null);
            }

            String playerName = playerNameArg.get(ctx);
            Double amountD = amountArg.get(ctx);
            if (amountD == null || amountD < 0) {
                ctx.sendMessage(Message.raw("Amount must be non-negative").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            long amount = amountD.longValue();

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                var economy = VaryonEcotalePlugin.getInstance().getEconomyManager();
                long oldBalance = economy.getTokenBalanceLoadingStorage(targetUuid, type);
                economy.setTokenBalance(targetUuid, type, amount, "Admin set token");
                sender.sendMessage(Message.join(
                    Message.raw("Set ").color(Color.GREEN),
                    Message.raw(resolved.displayName).color(Color.WHITE),
                    Message.raw(" " + type.getDisplayName() + ": ").color(Color.GRAY),
                    Message.raw("x" + oldBalance).color(Color.GRAY),
                    Message.raw(" -> ").color(Color.WHITE),
                    Message.raw("x" + amount).color(new Color(50, 205, 50))
                ));
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) {
                    return CompletableFuture.completedFuture(null);
                }
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }

    // ========== GIVE TOKEN COMMAND ==========
    private static class EcoGiveTokenCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> tokenTypeArg;
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoGiveTokenCommand() {
            super("givetoken", "Add tokens to a player's bank balance");
            this.addAliases("addtoken");
            this.tokenTypeArg = this.withRequiredArg("type", "Token type (coincoin, building, faction)", ArgTypes.STRING);
            this.playerNameArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "Amount to give", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            TokenType type = parseTokenType(tokenTypeArg.get(ctx));
            if (type == null) {
                sendTokenTypeError(ctx);
                return CompletableFuture.completedFuture(null);
            }

            String playerName = playerNameArg.get(ctx);
            Double amountD = amountArg.get(ctx);
            if (amountD == null || amountD <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            long amount = amountD.longValue();

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                var economy = VaryonEcotalePlugin.getInstance().getEconomyManager();
                economy.getTokenBalanceLoadingStorage(targetUuid, type);
                if (!economy.depositToken(targetUuid, type, amount, "Admin give token")) {
                    sender.sendMessage(Message.raw("Failed to give tokens (overflow?).").color(Color.RED));
                    return;
                }
                long newBalance = economy.getTokenBalance(targetUuid, type);
                sender.sendMessage(Message.join(
                    Message.raw("Added ").color(Color.GREEN),
                    Message.raw("+x" + amount + " " + type.getDisplayName()).color(new Color(50, 205, 50)),
                    Message.raw(" to ").color(Color.GRAY),
                    Message.raw(resolved.displayName).color(Color.WHITE),
                    Message.raw(" | New: ").color(Color.GRAY),
                    Message.raw("x" + newBalance).color(Color.WHITE)
                ));
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }

    // ========== TAKE TOKEN COMMAND ==========
    private static class EcoTakeTokenCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> tokenTypeArg;
        private final RequiredArg<String> playerNameArg;
        private final RequiredArg<Double> amountArg;

        public EcoTakeTokenCommand() {
            super("taketoken", "Remove tokens from a player's bank balance");
            this.addAliases("removetoken");
            this.tokenTypeArg = this.withRequiredArg("type", "Token type (coincoin, building, faction)", ArgTypes.STRING);
            this.playerNameArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING);
            this.amountArg = this.withRequiredArg("amount", "Amount to remove", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();

            TokenType type = parseTokenType(tokenTypeArg.get(ctx));
            if (type == null) {
                sendTokenTypeError(ctx);
                return CompletableFuture.completedFuture(null);
            }

            String playerName = playerNameArg.get(ctx);
            Double amountD = amountArg.get(ctx);
            if (amountD == null || amountD <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            long amount = amountD.longValue();

            Runnable apply = () -> {
                var resolved = VaryonEcotalePlugin.getInstance().getEconomyManager().resolveAdminTargetByName(playerName);
                if (!resolved.success) {
                    sender.sendMessage(Message.raw(resolved.errorMessage).color(Color.RED));
                    return;
                }
                UUID targetUuid = resolved.uuid;
                var economy = VaryonEcotalePlugin.getInstance().getEconomyManager();
                economy.getTokenBalanceLoadingStorage(targetUuid, type);
                boolean ok = economy.withdrawToken(targetUuid, type, amount, "Admin take token");
                long newBalance = economy.getTokenBalance(targetUuid, type);
                if (ok) {
                    sender.sendMessage(Message.join(
                        Message.raw("Removed ").color(Color.YELLOW),
                        Message.raw("-x" + amount + " " + type.getDisplayName()).color(new Color(255, 99, 71)),
                        Message.raw(" from ").color(Color.GRAY),
                        Message.raw(resolved.displayName).color(Color.WHITE),
                        Message.raw(" | New: ").color(Color.GRAY),
                        Message.raw("x" + newBalance).color(Color.WHITE)
                    ));
                } else {
                    sender.sendMessage(Message.join(
                        Message.raw("Insufficient tokens for ").color(Color.RED),
                        Message.raw(resolved.displayName).color(Color.WHITE)
                    ));
                }
            };

            if (sender instanceof Player player) {
                var ref = player.getReference();
                if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
                var store = ref.getStore();
                World world = store.getExternalData().getWorld();
                if (world == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.runAsync(apply, world);
            }

            return CompletableFuture.runAsync(apply, HytaleServer.SCHEDULED_EXECUTOR);
        }
    }
}
