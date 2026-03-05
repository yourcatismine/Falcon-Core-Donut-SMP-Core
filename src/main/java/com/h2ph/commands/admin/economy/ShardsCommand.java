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

public class ShardsCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public ShardsCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Check if sender is a player (required for all non-admin commands)
        if (!(sender instanceof Player)) {
            // Console can only use admin commands
            if (args.length >= 3 && sender.hasPermission("prismcore.admin.shards")) {
                return handleAdminCommand(sender, args);
            }
            sender.sendMessage(ChatColor.RED + "Only players can check shard balances.");
            return true;
        }

        Player player = (Player) sender;

        // No args - show player's own shards
        if (args.length == 0) {
            showOwnShards(player);
            return true;
        }

        // 1 arg
        if (args.length == 1) {
            String arg = args[0];

            // Check if it's an admin subcommand without permission - silently show own
            // shards
            if (arg.equalsIgnoreCase("give") || arg.equalsIgnoreCase("set") || arg.equalsIgnoreCase("remove")) {
                showOwnShards(player);
                return true;
            }

            // Otherwise treat as player name lookup
            showOtherPlayerShards(player, arg);
            return true;
        }

        // Handle pay command
        if (args[0].equalsIgnoreCase("pay")) {
            return handlePayCommand(player, args);
        }

        // 3+ args - admin command
        if (sender.hasPermission("prismcore.admin.shards")) {
            return handleAdminCommand(sender, args);
        }

        // No permission for admin command/Invalid args - silently show own shards
        showOwnShards(player);
        return true;
    }

    /**
     * Show player's own shard balance in chat and actionbar
     */
    private void showOwnShards(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        int shards = (int) data.getShards();
        String formattedShards = formatNumber(shards);

        String message = ChatColor.GRAY + "Your shards: " + ChatColor.DARK_PURPLE + formattedShards;
        player.sendMessage(message);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(message));
    }

    /**
     * Show another player's shard balance
     */
    private void showOtherPlayerShards(Player sender, String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            processShowShards(sender, onlineTarget);
            return;
        }

        // Validate name format before lookup (3-16 chars, alphanumeric)
        if (!targetName.matches("[a-zA-Z0-9_]{3,16}")) {
            String errorMsg = ChatColor.RED + "That user does not exist.";
            sender.sendMessage(errorMsg);
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                plugin.getSchedulerAdapter().runTask(() -> processShowShards(sender, target));
            } catch (Exception e) {
                plugin.getSchedulerAdapter().runTask(() -> {
                    String errorMsg = ChatColor.RED + "That user does not exist.";
                    sender.sendMessage(errorMsg);
                    playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
                });
            }
        });
    }

    private void processShowShards(Player sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String errorMsg = ChatColor.RED + "That user does not exist.";
            sender.sendMessage(errorMsg);
            sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(errorMsg));
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (data == null) {
            data = plugin.getPlayerDataManager().loadPlayer(target.getUniqueId());
        }

        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Could not load data for " + target.getName());
            return;
        }

        int shards = (int) data.getShards();
        String formattedShards = formatNumber(shards);

        String message = ChatColor.GRAY + target.getName() + "'s shards: " + ChatColor.DARK_PURPLE + formattedShards;
        sender.sendMessage(message);
        sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(message));

        // Unload if they are offline and weren't loaded
        if (!target.isOnline()) {
            plugin.getPlayerDataManager().unload(target.getUniqueId());
        }
    }

    /**
     * Format numbers with k/m suffixes
     * Examples: 10, 10k, 10m
     */
    private String formatNumber(int number) {
        if (number >= 1_000_000) {
            // Millions
            int millions = number / 1_000_000;
            return millions + "m";
        } else if (number >= 1_000) {
            // Thousands
            int thousands = number / 1_000;
            return thousands + "k";
        } else {
            // Less than 1000
            return String.valueOf(number);
        }
    }

    /**
     * Handle pay command
     */
    private boolean handlePayCommand(Player sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shards pay <gamertag> <amount>");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        String targetName = args[1];
        String amountStr = args[2];
        int amount;

        // Prevent paying self
        if (sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(ChatColor.RED + "You cannot pay yourself!");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        // Parse amount
        try {
            amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    ChatColor.RED + "Invalid amount! Use numbers or suffixes (k, m, b, t). Example: 10k, 100m, 1t");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive!");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        // Validate name length
        if (targetName.length() < 3 || targetName.length() > 16) {
            String errorMsg = ChatColor.RED + "That player does not exist.";
            sender.sendMessage(errorMsg);
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            processPay(sender, onlineTarget, amount);
            return true;
        }

        // Validate name format before lookup
        if (!targetName.matches("[a-zA-Z0-9_]{3,16}")) {
            String errorMsg = ChatColor.RED + "That player does not exist.";
            sender.sendMessage(errorMsg);
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                plugin.getSchedulerAdapter().runTask(() -> processPay(sender, target, amount));
            } catch (Exception e) {
                plugin.getSchedulerAdapter().runTask(() -> {
                    String errorMsg = ChatColor.RED + "That player does not exist.";
                    sender.sendMessage(errorMsg);
                    playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
                });
            }
        });
        return true;
    }

    private void processPay(Player sender, OfflinePlayer target, int amount) {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getSchedulerAdapter().runTask(() -> processPay(sender, target, amount));
            return;
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String errorMsg = ChatColor.RED + "That player does not exist.";
            sender.sendMessage(errorMsg);
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // Load sender data
        PlayerData senderData = plugin.getPlayerDataManager().get(sender.getUniqueId());
        if (senderData == null) {
            senderData = plugin.getPlayerDataManager().loadPlayer(sender.getUniqueId());
        }

        if (senderData.getShards() < amount) {
            sender.sendMessage(ChatColor.RED + "You do not have enough shards!");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return;
        }

        final PlayerData finalSenderData = senderData;

        // Move IO to async thread
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            PlayerData targetData = plugin.getPlayerDataManager().get(target.getUniqueId());
            boolean targetWasLoaded = targetData != null;
            if (targetData == null) {
                targetData = plugin.getPlayerDataManager().loadPlayer(target.getUniqueId());
            }

            if (targetData == null) {
                plugin.getSchedulerAdapter().runTask(
                        () -> sender.sendMessage(ChatColor.RED + "Could not load data for " + target.getName()));
                return;
            }

            final PlayerData finalTargetData = targetData;

            // Check if target has ignored the sender
            if (finalTargetData.isIgnoring(sender.getUniqueId())) {
                plugin.getSchedulerAdapter().runTask(() -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You are ignored by this player."));
                    playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
                });
                return;
            }

            // Execute transaction on main thread for safety
            plugin.getSchedulerAdapter().runTask(() -> {
                finalSenderData.removeShards(amount, "Payment to " + target.getName());
                finalTargetData.addShards(amount, "Payment from " + sender.getName());

                // Save data async
                plugin.getPlayerDataManager().savePlayerAsync(sender.getUniqueId());
                plugin.getPlayerDataManager().savePlayerAsync(target.getUniqueId());

                if (!targetWasLoaded && !target.isOnline()) {
                    plugin.getPlayerDataManager().unload(target.getUniqueId());
                }

                // Notify sender
                sender.sendMessage(ChatColor.GRAY + "You paid " + target.getName() + " " +
                        ChatColor.DARK_PURPLE + formatNumber(amount) + " shards");
                playSound(sender, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME);

                // Notify target if online
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        String receiverMsg = ChatColor.DARK_PURPLE + sender.getName() + ChatColor.GRAY + " paid you " +
                                ChatColor.DARK_PURPLE + formatNumber(amount);
                        targetPlayer.sendMessage(receiverMsg);
                        targetPlayer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(receiverMsg));
                        playSound(targetPlayer, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_PLACE);
                    }
                }
            });
        });
    }

    /**
     * Handle admin commands (give/set/remove)
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shards <give|set|remove> <player> <amount>");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        String amountStr = args[2];
        int amount;

        // Parse amount
        try {
            amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    ChatColor.RED + "Invalid amount! Use numbers or suffixes (k, m, b, t). Example: 10k, 100m, 1t");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        // Validate action
        if (!action.equals("give") && !action.equals("set") && !action.equals("remove")) {
            sender.sendMessage(ChatColor.RED + "Invalid action! Use: give, set, or remove");
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        // Online check first
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            processAdminCommand(sender, onlineTarget, action, amount);
            return true;
        }

        // Async lookup
        // Validate name format before lookup
        if (!targetName.matches("[a-zA-Z0-9_]{3,16}")) {
            String errorMsg = ChatColor.RED + "That user does not exist.";
            sender.sendMessage(errorMsg);
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return true;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                plugin.getSchedulerAdapter().runTask(() -> processAdminCommand(sender, target, action, amount));
            } catch (Exception e) {
                plugin.getSchedulerAdapter().runTask(() -> {
                    String errorMsg = ChatColor.RED + "That user does not exist.";
                    sender.sendMessage(errorMsg);
                    playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
                });
            }
        });
        return true;
    }

    private void processAdminCommand(CommandSender sender, OfflinePlayer target, String action, int amount) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String errorMsg = ChatColor.RED + "That user does not exist.";
            sender.sendMessage(errorMsg);
            // Send actionbar if sender is a player
            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(errorMsg));
            }
            playSound(sender, org.bukkit.Sound.ENTITY_VILLAGER_NO);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        boolean wasLoaded = data != null;
        if (data == null) {
            data = plugin.getPlayerDataManager().loadPlayer(target.getUniqueId());
        }

        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Could not load data for " + target.getName());
            return;
        }

        double currentShards = data.getShards();
        double newShards = currentShards;

        switch (action) {
            case "give":
                newShards = currentShards + amount;
                data.setShards(newShards, "Admin Adjustment");
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.GOLD + amount +
                        ChatColor.GREEN + " shards to " + ChatColor.YELLOW + target.getName() +
                        ChatColor.GREEN + ". New balance: " + ChatColor.GOLD + (int) newShards);
                break;

            case "set":
                newShards = amount;
                data.setShards(newShards, "Admin Adjustment");
                sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + target.getName() +
                        ChatColor.GREEN + "'s shards to " + ChatColor.GOLD + amount);
                break;

            case "remove":
                newShards = Math.max(0, currentShards - amount);
                data.setShards(newShards, "Admin Adjustment");
                int actualRemoved = (int) (currentShards - newShards);
                sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.GOLD + actualRemoved +
                        ChatColor.GREEN + " shards from " + ChatColor.YELLOW + target.getName() +
                        ChatColor.GREEN + ". New balance: " + ChatColor.GOLD + (int) newShards);
                break;
        }

        plugin.getPlayerDataManager().savePlayerAsync(target.getUniqueId());
        if (!wasLoaded && !target.isOnline()) {
            plugin.getPlayerDataManager().unload(target.getUniqueId());
        }
        playSound(sender, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        if (target.isOnline()) {
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(ChatColor.GRAY + "Your shard balance has been updated to " +
                        ChatColor.DARK_PURPLE + (int) newShards + " shards");
            }
        }
    }

    /**
     * Play a sound to the command sender if they are a player
     */
    private void playSound(CommandSender sender, org.bukkit.Sound sound) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            try {
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Parse amount with support for k/m/b/t suffixes
     * Examples: 10k = 10000, 1m = 1000000, 5b = 5000000000, 1t = 1000000000000
     */
    private int parseAmount(String input) throws NumberFormatException {
        input = input.toLowerCase().trim();

        if (input.isEmpty()) {
            throw new NumberFormatException("Empty amount");
        }

        char lastChar = input.charAt(input.length() - 1);
        int multiplier = 1;
        String numberPart = input;

        // Check for suffix
        if (Character.isLetter(lastChar)) {
            numberPart = input.substring(0, input.length() - 1);

            switch (lastChar) {
                case 'k':
                    multiplier = 1_000;
                    break;
                case 'm':
                    multiplier = 1_000_000;
                    break;
                case 'b':
                    multiplier = 1_000_000_000;
                    break;
                case 't':
                    // For trillion, we need to be careful with int overflow
                    // Parse as long first, then convert
                    double base = Double.parseDouble(numberPart);
                    if (!Double.isFinite(base)) {
                        throw new NumberFormatException("Shards amount is not finite");
                    }
                    long result = (long) (base * 1_000_000_000_000L);
                    if (result > Integer.MAX_VALUE) {
                        return Integer.MAX_VALUE;
                    }
                    return (int) result;
                default:
                    throw new NumberFormatException("Invalid suffix: " + lastChar);
            }
        }

        // Parse the number part (can be decimal like 1.5k)
        double base = Double.parseDouble(numberPart);
        if (!Double.isFinite(base)) {
            throw new NumberFormatException("Shards amount is not finite");
        }
        long result = (long) (base * multiplier);

        // Clamp to int range
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < 0) {
            return 0;
        }

        return (int) result;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // First argument: action
        if (args.length == 1) {
            List<String> actions = new ArrayList<>();
            actions.add("pay");

            // Only show admin subcommands to players with admin permission
            if (sender.hasPermission("prismcore.admin.shards")) {
                actions.add("give");
                actions.add("set");
                actions.add("remove");
            }

            return actions.stream()
                    .filter(action -> action.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // pay command args
        if (args[0].equalsIgnoreCase("pay")) {
            // Second argument: player name
            if (args.length == 2) {
                // Use async player name cache to prevent TPS drops
                completions = plugin.getPlayerNameCache().getCompletions(args[1]);
                // Remove self from suggestions for shards command
                if (sender instanceof Player) {
                    completions.remove(sender.getName());
                }
                return completions;
            }
            // Third argument: amount suggestions
            if (args.length == 3) {
                List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000", "1k", "10k", "100k", "1m");
                return amounts.stream()
                        .filter(amount -> amount.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Admin command args
        if (sender.hasPermission("prismcore.admin.shards")) {
            String action = args[0].toLowerCase();
            if (action.equals("give") || action.equals("set") || action.equals("remove")) {
                // Second argument: player name
                if (args.length == 2) {
                    // Use async player name cache to prevent TPS drops
                    return plugin.getPlayerNameCache().getCompletions(args[1]);
                }

                // Third argument: amount suggestions
                if (args.length == 3) {
                    List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000", "1k", "10k", "100k", "1m");
                    return amounts.stream()
                            .filter(amount -> amount.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return completions;
    }
}
