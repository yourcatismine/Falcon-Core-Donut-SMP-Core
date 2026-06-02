package com.h2ph.commands.admin.moderations;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnmuteCommand implements CommandExecutor, TabCompleter {

    private final Falcon plugin;

    public UnmuteCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("falcon.mute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;
        String finalTargetName;

        if (target != null) {
            targetUUID = target.getUniqueId();
            finalTargetName = target.getName();
        } else {
            targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
            finalTargetName = targetName;
        }

        PlayerData data = plugin.getPlayerDataManager().get(targetUUID);
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Could not find player data for " + targetName);
            return true;
        }

        if (!data.isMuted()) {
            sender.sendMessage(ChatColor.RED + finalTargetName + " is not muted.");
            return true;
        }

        data.setMuted(false);
        data.setMuteExpiry(0);
        data.setMuteReason(null);
        plugin.getPlayerDataManager().savePlayerAsync(targetUUID);

        plugin.getDatabaseManager().removeMute(targetUUID);

        String adminMsg = ChatColor.translateAlternateColorCodes('&', "&d" + finalTargetName + "&7 has been unmuted.");
        sender.sendMessage(adminMsg);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(adminMsg));
        }

        if (target != null && target.isOnline()) {
            String targetMsg = ChatColor.translateAlternateColorCodes('&', "&7You have been unmuted.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(targetMsg));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return plugin.getPlayerNameCache().getCompletions(args[0]);
        }
        return Collections.emptyList();
    }
}
