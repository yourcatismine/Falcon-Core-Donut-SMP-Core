package com.h2ph.commands.economy;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
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

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final Falcon plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public BalanceCommand(Falcon plugin) {
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
                retrieveAndSendBalance(sender, target.getUniqueId(), target.getName(), false);
            } else {
                plugin.getSchedulerAdapter().runTaskAsync(() -> {
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
        Player targetP = Bukkit.getPlayer(targetId);
        if (targetP != null && targetP.isOnline()) {
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
            if (Bukkit.isPrimaryThread()) {
                plugin.getSchedulerAdapter()
                        .runTaskAsync(() -> retrieveAndSendBalance(sender, targetId, targetName, false));
                return;
            }

            PlayerData data = plugin.getPlayerDataManager().loadPlayer(targetId);
            double balance = (data != null) ? data.getMoney() : 0;
            String loadedName = (data != null && data.getName() != null) ? data.getName() : targetName;

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
            msg = ChatColor.translateAlternateColorCodes('&', "&d" + playerName + "&7 has &a$" + moneyFormatted);
        }

        sender.sendMessage(msg);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        }
    }

    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "T");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "B");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "M");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "K");
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
            return plugin.getPlayerNameCache().getCompletions(args[0]);
        }
        return Collections.emptyList();
    }
}
