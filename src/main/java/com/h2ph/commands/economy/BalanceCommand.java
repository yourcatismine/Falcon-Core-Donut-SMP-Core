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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public BalanceCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player.");
                return true;
            }
            Player player = (Player) sender;
            retrieveAndSendBalance(sender, player.getUniqueId(), player.getName(), true);
        } else {
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target != null) {
                // Online player found
                retrieveAndSendBalance(sender, target.getUniqueId(), target.getName(), false);
            } else {
                // Offline player lookup (Async)
                plugin.getSchedulerAdapter().runTaskAsync(() -> {
                    // This can block/network call
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        plugin.getSchedulerAdapter().runTask(() -> {
                            String errorMsg = ChatColor.translateAlternateColorCodes('&',
                                    "&cThat player does not exist.");
                            sender.sendMessage(errorMsg);
                            if (sender instanceof Player) {
                                Player p = (Player) sender;
                                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(errorMsg));
                                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            }
                        });
                        return;
                    }

                    retrieveAndSendBalance(sender, offlinePlayer.getUniqueId(), offlinePlayer.getName(), false);
                });
            }
        }
        return true;
    }

    private void retrieveAndSendBalance(CommandSender sender, UUID targetId, String targetName,
            boolean isInternalCall) {
        // Checks
        Player targetP = Bukkit.getPlayer(targetId);
        if (targetP != null && targetP.isOnline()) {
            // Online: Data should be loaded.
            // We can access on main thread.
            // If we are currently async (from offline lookup flow?), we need to sync back.
            if (!Bukkit.isPrimaryThread()) {
                plugin.getSchedulerAdapter().runTask(() -> retrieveAndSendBalance(sender, targetId, targetName, false));
                return;
            }

            PlayerData data = plugin.getPlayerDataManager().get(targetId);
            double balance = (data != null) ? data.getMoney() : 0;
            sendMessages(sender, targetName, balance,
                    (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetId)));
        } else {
            // Offline: Must load async.
            if (Bukkit.isPrimaryThread()) {
                plugin.getSchedulerAdapter()
                        .runTaskAsync(() -> retrieveAndSendBalance(sender, targetId, targetName, false));
                return;
            }

            // We are async now.
            // Use loadPlayer to avoid touching the main thread map
            PlayerData data = plugin.getPlayerDataManager().loadPlayer(targetId);
            double balance = (data != null) ? data.getMoney() : 0;
            String loadedName = (data != null && data.getName() != null) ? data.getName() : targetName;

            // Send sync
            String finalName = loadedName;
            plugin.getSchedulerAdapter().runTask(() -> sendMessages(sender, finalName, balance,
                    (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetId))));
        }
    }

    private void sendMessages(CommandSender sender, String playerName, double balance, boolean isSelf) {
        String moneyFormatted = formatNumber(balance);
        String msg;
        if (isSelf) {
            msg = ChatColor.translateAlternateColorCodes('&', "&7You have &a$" + moneyFormatted);
        } else {
            msg = ChatColor.translateAlternateColorCodes('&', "&5" + playerName + "&7 has &a$" + moneyFormatted);
        }

        sender.sendMessage(msg);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        }
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
        // Truncate to 1 decimal place without rounding up?
        // User example: 1.5k.
        // 10.4t.
        // It seems standard truncation/formatting.
        // Math.floor(script * 10) / 10.0 provides 1 decimal place truncation.
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(token))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
