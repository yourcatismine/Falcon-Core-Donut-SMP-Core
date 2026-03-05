package com.h2ph.commands.economy;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    // Reuse format from BalanceCommand for consistency if needed, but we need
    // parsing here.
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public PayCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        String targetName = args[0];
        String amountStr = args[1];

        // Parse amount
        double amount;
        try {
            amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount format. Examples: 100, 1k, 1m");
            return true;
        }

        if (amount <= 0 || !Double.isFinite(amount)) {
            sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
            return true;
        }

        // Get Target (Async lookup if not online, but for pay command usually we
        // require some validation)
        // Check if online first
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            processPayment(player, target.getUniqueId(), target.getName(), amount);
        } else {
            // Async lookup for offline player
            plugin.getSchedulerAdapter().runTaskAsync(() -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    plugin.getSchedulerAdapter().runTask(() -> {
                        sendError(player, "&cThat player does not exist.");
                    });
                    return;
                }
                processPayment(player, offlinePlayer.getUniqueId(), offlinePlayer.getName(), amount);
            });
        }

        return true;
    }

    private void processPayment(Player sender, UUID targetId, String targetName, double amount) {
        // Ensure not paying self - prompt says "You cannot pay yourself."
        if (sender.getUniqueId().equals(targetId)) {
            // Need to run on main thread if we are async
            if (!Bukkit.isPrimaryThread()) {
                plugin.getSchedulerAdapter().runTask(() -> sendError(sender, "&cYou cannot pay yourself."));
            } else {
                sendError(sender, "&cYou cannot pay yourself.");
            }
            return;
        }

        // We need to modify data. This must be thread-safe or on main thread if
        // PlayerData is not thread-safe.
        // PlayerDataManager looks like it loads/saves files. The cached map is HashMap
        // (not thread safe).
        // Best to run transaction on main thread or synchronized.
        // Given we might have come from async, let's reschedule to main for the
        // transaction part.
        if (!Bukkit.isPrimaryThread()) {
            plugin.getSchedulerAdapter().runTask(() -> processPayment(sender, targetId, targetName, amount));
            return;
        }

        PlayerData senderData = plugin.getPlayerDataManager().get(sender.getUniqueId());
        if (senderData == null) {
            // This should rarely happen for an online player, but if it does, load it
            senderData = plugin.getPlayerDataManager().loadPlayer(sender.getUniqueId());
        }

        if (senderData.getMoney() < amount) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cYou do not have enough money.");
            sender.sendMessage(msg);
            sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            return;
        }

        final PlayerData finalSenderData = senderData;

        // Finalize transaction asynchronously to avoid TPS drop from DB/File IO
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            PlayerData targetData = plugin.getPlayerDataManager().get(targetId);
            boolean targetWasLoaded = targetData != null;
            if (targetData == null) {
                targetData = plugin.getPlayerDataManager().loadPlayer(targetId);
            }

            if (targetData == null) {
                plugin.getSchedulerAdapter().runTask(() -> sendError(sender, "&cCould not load data for that player."));
                return;
            }

            final PlayerData finalTargetData = targetData;

            // Check if target has disabled payments
            if (!finalTargetData.isPayments()) {
                plugin.getSchedulerAdapter().runTask(() -> sendError(sender, "&cUser disabled payments."));
                return;
            }

            // Check if target has ignored the sender
            if (finalTargetData.isIgnoring(sender.getUniqueId())) {
                plugin.getSchedulerAdapter().runTask(() -> sendError(sender, "&7You are ignored by this player."));
                return;
            }

            // Return to main thread for the actual economy modification to ensure safety
            plugin.getSchedulerAdapter().runTask(() -> {
                // Transaction using global EconomyHandler (Vault-aware)
                if (!com.prismcore.survival.auction.EconomyHandler.chargePlayer(sender, amount,
                        "Payment to " + targetName)) {
                    sendError(sender, "&cTransaction failed.");
                    return;
                }

                // Deposit to target (handles offline/Vault automatically)
                com.prismcore.survival.auction.EconomyHandler.depositOfflinePlayer(Bukkit.getOfflinePlayer(targetId),
                        amount,
                        "Payment from " + sender.getName());

                // Save data asynchronously
                plugin.getSchedulerAdapter().runTaskAsync(() -> {
                    if (!com.prismcore.survival.auction.EconomyHandler.usingVault()) {
                        plugin.getPlayerDataManager().savePlayerAsync(sender.getUniqueId());
                        plugin.getPlayerDataManager().savePlayerAsync(targetId);

                        if (!targetWasLoaded && Bukkit.getPlayer(targetId) == null) {
                            plugin.getPlayerDataManager().unload(targetId);
                        }
                    }
                });

                // Success Messages and Notifications
                sendSuccess(sender, targetId, targetName, amount, finalTargetData);
            });
        });
    }

    private void sendSuccess(Player sender, UUID targetId, String targetName, double amount, PlayerData targetData) {
        String moneyFormatted = formatNumber(amount);
        String senderMsg = ChatColor.translateAlternateColorCodes('&',
                "&7You paid &5" + targetName + "&a $" + moneyFormatted);
        sender.sendMessage(senderMsg);
        sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(senderMsg));

        Player targetOnline = Bukkit.getPlayer(targetId);
        if (targetOnline != null) {
            if (!targetData.isPayAlerts()) {
                return;
            }

            String targetMsg = ChatColor.translateAlternateColorCodes('&',
                    "&5" + sender.getName() + "&7 has paid you&a $" + moneyFormatted);
            targetOnline.sendMessage(targetMsg);
            targetOnline.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(targetMsg));

            if (targetData.isSoundNotifications()) {
                targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }

    private void sendError(Player player, String message) {
        String msg = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private double parseAmount(String amountStr) throws NumberFormatException {
        amountStr = amountStr.toLowerCase();
        double multiplier = 1.0;
        if (amountStr.endsWith("k")) {
            multiplier = 1_000.0;
            amountStr = amountStr.substring(0, amountStr.length() - 1);
        } else if (amountStr.endsWith("m")) {
            multiplier = 1_000_000.0;
            amountStr = amountStr.substring(0, amountStr.length() - 1);
        } else if (amountStr.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            amountStr = amountStr.substring(0, amountStr.length() - 1);
        } else if (amountStr.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            amountStr = amountStr.substring(0, amountStr.length() - 1);
        }

        double val = Double.parseDouble(amountStr);
        double result = val * multiplier;
        if (!Double.isFinite(result)) {
            throw new NumberFormatException("Amount is not finite");
        }
        return result;
    }

    // Formatting logic replicated from BalanceCommand for consistency in message
    // display
    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "t");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "b");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "m");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "k");
        } else {
            return DF.format(Math.floor(number * 10) / 10.0);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            // Use async player name cache to prevent TPS drops
            return plugin.getPlayerNameCache().getCompletions(args[0]);
        } else if (args.length == 2) {
            // Suggest amounts?
            List<String> suggestions = new ArrayList<>();
            suggestions.add("100");
            suggestions.add("1k");
            suggestions.add("10k");
            suggestions.add("1m");
            return suggestions;
        }
        return Collections.emptyList();
    }
}
