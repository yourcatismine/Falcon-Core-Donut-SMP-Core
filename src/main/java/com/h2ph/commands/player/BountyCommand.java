package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import com.h2ph.gui.BountyGUI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public BountyCommand(PrismSurvival plugin) {
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

        if (args.length == 0) {
            new BountyGUI(plugin).open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                new BountyGUI(plugin).open(player);
                return true;
            }

            String targetName = args[1];
            String amountStr = args[2];

            double amount;
            try {
                amount = parseAmount(amountStr);
            } catch (NumberFormatException e) {
                // No sound, open GUI for invalid amount
                new BountyGUI(plugin).open(player);
                return true;
            }

            if (amount < 1) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            if (amount > 1_000_000_000_000.0 || !Double.isFinite(amount)) {
                // No sound, open GUI for invalid amount (exceeding max)
                new BountyGUI(plugin).open(player);
                return true;
            }

            // Pay check
            net.milkbowl.vault.economy.Economy econ = plugin.getEconomy();
            if (econ != null) {
                if (!econ.has(player, amount)) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money.");
                    return true;
                }
            } else {
                // Fallback to internal if Vault not found (though it should be)
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null)
                    data = plugin.getPlayerDataManager().loadPlayer(player.getUniqueId());

                if (data.getMoney() < amount) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money.");
                    return true;
                }
            }

            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                new com.h2ph.gui.BountyConfirmGUI(plugin).open(player, target.getUniqueId(), target.getName(), amount);
            } else {
                plugin.getSchedulerAdapter().runTaskAsync(() -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        plugin.getSchedulerAdapter().runTask(() -> {
                            // No sound, open GUI for unknown player
                            new BountyGUI(plugin).open(player);
                        });
                        return;
                    }
                    if (offlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                        plugin.getSchedulerAdapter().runTask(() -> {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        });
                        return;
                    }
                    plugin.getSchedulerAdapter().runTask(() -> {
                        new com.h2ph.gui.BountyConfirmGUI(plugin).open(player, offlinePlayer.getUniqueId(),
                                offlinePlayer.getName(), amount);
                    });
                });
            }
            return true;
        }

        new BountyGUI(plugin).open(player);
        return true;
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
        // Hide sub-commands and arguments from tab completion as requested
        return Collections.emptyList();
    }
}
