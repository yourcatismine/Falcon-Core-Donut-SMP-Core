package com.h2ph.commands.admin.economy;

import com.prismcore.survival.manager.PlayerData;
import com.h2ph.PrismSurvival;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public EconomyCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!sender.hasPermission("falcon.economy")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy <give|set|remove> <player> <amount>");
            return true;
        }

        return handleAdminCommand(sender, args);
    }

    /**
     * Format numbers with k/m/b/t suffixes
     */
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
            if (number % 1 == 0) {
                return String.valueOf((long) number);
            }
            return String.format("%.2f", number);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.#");
        return df.format(scaled) + suffix;
    }

    private boolean isAdminAction(String arg) {
        return arg.equalsIgnoreCase("give") ||
                arg.equalsIgnoreCase("set") ||
                arg.equalsIgnoreCase("remove");
    }

    /**
     * Handle admin commands (give/set/remove)
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        String action = args[0].toLowerCase();
        String targetName = args[1];
        String amountStr = args[2];
        double amount;

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy <give|set|remove> <player> <amount>");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        try {
            amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    ChatColor.RED + "Invalid amount! Examples: 100h, 10k, 1.5m");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        if (!isAdminAction(action)) {
            sender.sendMessage(ChatColor.RED + "Invalid action! Use: give, set, remove");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "That user does not exist.");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        double currentMoney = data.getMoney();
        double newMoney = currentMoney;

        switch (action) {
            case "give":
                newMoney = currentMoney + amount;
                data.setMoney(newMoney, "Admin Adjustment");
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.GOLD + "$" + formatNumber(amount) +
                        ChatColor.GREEN + " to " + ChatColor.YELLOW + targetName +
                        ChatColor.GREEN + ". New balance: " + ChatColor.GOLD + "$" + formatNumber(newMoney));
                break;

            case "set":
                newMoney = amount;
                data.setMoney(newMoney, "Admin Adjustment");
                sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + targetName +
                        ChatColor.GREEN + "'s balance to " + ChatColor.GOLD + "$" + formatNumber(newMoney));
                break;

            case "remove":
                newMoney = Math.max(0, currentMoney - amount);
                data.setMoney(newMoney, "Admin Adjustment");
                double actualRemoved = currentMoney - newMoney;
                sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.GOLD + "$" + formatNumber(actualRemoved) +
                        ChatColor.GREEN + " from " + ChatColor.YELLOW + targetName +
                        ChatColor.GREEN + ". New balance: " + ChatColor.GOLD + "$" + formatNumber(newMoney));
                break;
        }

        plugin.getPlayerDataManager().savePlayerAsync(target.getUniqueId());
        playSound(sender, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ChatColor.GRAY + "Your balance has been updated to " +
                    ChatColor.GREEN + "$" + formatNumber(newMoney));
        }

        return true;
    }

    private void playSound(CommandSender sender, org.bukkit.Sound sound) {
        if (sender instanceof Player) {
            try {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), sound, 1f, 1f);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Parse amount with suffixes: h, k, m, b, t
     */
    private double parseAmount(String input) throws NumberFormatException {
        input = input.toLowerCase().trim();
        if (input.isEmpty())
            throw new NumberFormatException("Empty amount");

        char lastChar = input.charAt(input.length() - 1);
        double multiplier = 1.0;
        String numberPart = input;

        if (Character.isLetter(lastChar)) {
            numberPart = input.substring(0, input.length() - 1);
            switch (lastChar) {
                case 'h':
                    multiplier = 100.0;
                    break;
                case 'k':
                    multiplier = 1_000.0;
                    break;
                case 'm':
                    multiplier = 1_000_000.0;
                    break;
                case 'b':
                    multiplier = 1_000_000_000.0;
                    break;
                case 't':
                    multiplier = 1_000_000_000_000.0;
                    break;
                default:
                    throw new NumberFormatException("Invalid suffix: " + lastChar);
            }
        }

        double base = Double.parseDouble(numberPart);
        double result = base * multiplier;

        if (!Double.isFinite(result)) {
            throw new NumberFormatException("Amount is not finite");
        }

        if (result < 0)
            return 0;
        return result;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {

        if (!sender.hasPermission("falcon.economy")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> actions = Arrays.asList("give", "set", "remove");
            return actions.stream()
                    .filter(a -> a.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return plugin.getPlayerNameCache().getCompletions(args[1]);
        }

        if (args.length == 3) {
            return Arrays.asList("100h", "10k", "100k", "1m", "10m", "1b");
        }

        return new ArrayList<>();
    }
}
