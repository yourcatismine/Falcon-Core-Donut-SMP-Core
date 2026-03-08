package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CheckMuteCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public CheckMuteCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("prismsmp.admin.mute")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /checkmute <player>");
            return true;
        }

        String targetName = args[0];

        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            Player targetOnline = Bukkit.getPlayer(targetName);
            UUID targetUUID;
            String displayName;

            if (targetOnline != null) {
                targetUUID = targetOnline.getUniqueId();
                displayName = targetOnline.getName();
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' has never played on this server.");
                    return;
                }
                targetUUID = offlinePlayer.getUniqueId();
                displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : targetName;
            }

            com.prismcore.survival.manager.DatabaseManager.MuteInfo muteInfo = plugin.getDatabaseManager()
                    .getMuteInfo(targetUUID);
            PlayerData data = plugin.getPlayerDataManager().loadPlayer(targetUUID);

            plugin.getSchedulerAdapter().runTask(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");

                boolean isMuted = data.isMuted() || muteInfo != null;
                String status = isMuted ? ChatColor.RED + "Muted" : ChatColor.GREEN + "Not Muted";

                String reason = "None";
                String muteId = "N/A";
                String mutedBy = "N/A";
                long muteDate = 0;
                long muteExpiry = 0;

                if (muteInfo != null) {
                    reason = muteInfo.reason;
                    muteId = "#" + muteInfo.id;
                    mutedBy = muteInfo.mutedBy;
                    muteDate = muteInfo.date;
                    muteExpiry = muteInfo.expire;
                } else if (isMuted) {
                    reason = data.getMuteReason() != null ? data.getMuteReason() : "None";
                    muteId = data.getMuteId() != null ? "#" + data.getMuteId() : "N/A";
                    mutedBy = data.getMutedBy() != null ? data.getMutedBy() : "N/A";
                    muteDate = data.getMuteDate();
                    muteExpiry = data.getMuteExpiry();
                }

                String dateStr = muteDate > 0 ? sdf.format(new Date(muteDate)) : "N/A";

                String expiryStr;
                if (!isMuted) {
                    expiryStr = "N/A";
                } else if (muteExpiry == -1) {
                    expiryStr = "Never (Permanent)";
                } else {
                    expiryStr = sdf.format(new Date(muteExpiry));
                }

                sender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&6&m---------------------------------"));
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Mute Details: " + ChatColor.WHITE + displayName);
                sender.sendMessage("");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Status: " + status);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Reason: " + ChatColor.WHITE + reason);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Mute ID: " + ChatColor.WHITE + muteId);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Muted By: " + ChatColor.WHITE + mutedBy);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Date: " + ChatColor.WHITE + dateStr);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + " Expires: " + ChatColor.WHITE + expiryStr);
                sender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&6&m---------------------------------"));
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> mutedPlayers = plugin.getDatabaseManager().getMutedPlayerNames();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!mutedPlayers.contains(player.getName())) {
                    PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                    if (data.isMuted()) {
                        mutedPlayers.add(player.getName());
                    }
                }
            }
            return StringUtil.copyPartialMatches(args[0], mutedPlayers, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
