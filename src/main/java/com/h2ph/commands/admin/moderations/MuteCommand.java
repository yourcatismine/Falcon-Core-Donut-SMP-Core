package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import java.util.Random;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public MuteCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("prismsmp.admin.mute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mute <player> <duration> [reason]");
            return true;
        }

        String targetName = args[0];
        String durationStr = args[1];
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "No Reason Provided";

        long durationMs = parseDuration(durationStr);
        if (durationMs <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid duration format. Use 10s, 1m, 1d, 1y etc.");
            return true;
        }

        long expiry = System.currentTimeMillis() + durationMs;

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

        String muteId = String.valueOf(new Random().nextInt(900) + 100);

        data.setMuted(true);
        data.setMuteReason(reason);
        data.setMuteExpiry(expiry);
        data.setMuteId(muteId);
        data.setMutedBy(sender.getName());
        data.setMuteDate(System.currentTimeMillis());

        plugin.getPlayerDataManager().savePlayerAsync(targetUUID);

        plugin.getDatabaseManager().addMute(targetUUID, finalTargetName, muteId, reason, data.getMuteDate(), expiry,
                sender.getName());

        String adminMsg = ChatColor.translateAlternateColorCodes('&',
                "&7You muted &d" + finalTargetName + "&7 for &f" + durationStr + "&7 Reason:&c " + reason);
        sender.sendMessage(adminMsg);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(adminMsg));
        }

        if (target != null && target.isOnline()) {
            String targetMsg = ChatColor.translateAlternateColorCodes('&',
                    "&7You have been muted for &f" + durationStr + "&7 Reason:&c " + reason);
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(targetMsg));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        }

        return true;
    }

    private long parseDuration(String s) {
        if (s == null || s.isEmpty())
            return 0;
        try {
            String numberStr = s.replaceAll("[^0-9]", "");
            if (numberStr.isEmpty())
                return 0;

            long time = Long.parseLong(numberStr);
            String unit = s.replaceAll("[0-9]", "").toLowerCase();

            if (unit.equals("s"))
                return time * 1000L;
            if (unit.equals("m"))
                return time * 60000L;
            if (unit.equals("h"))
                return time * 3600000L;
            if (unit.equals("d"))
                return time * 86400000L;
            if (unit.equals("y"))
                return time * 31536000000L;

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return plugin.getPlayerNameCache().getCompletions(args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("10s", "1m", "1d", "1y").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
