package com.h2ph.commands.player;

import com.h2ph.managers.PrivateMessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        com.prismcore.survival.manager.PlayerData senderData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(player.getUniqueId());
        if (senderData != null && senderData.isMuted()) {
            String reason = senderData.getMuteReason();
            if (reason == null || reason.isEmpty())
                reason = "No Reason Provided";
            long expiry = senderData.getMuteExpiry();

            String durationLeft = "Permanent";
            if (expiry > 0) {
                long totalSeconds = (expiry - System.currentTimeMillis()) / 1000;
                durationLeft = formatDuration(totalSeconds);
            }

            String msg = ChatColor.translateAlternateColorCodes('&',
                    "&7You have been muted for &f" + durationLeft + "&7 Reason:&c " + reason);

            player.sendMessage(msg);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        PrivateMessageManager pmManager = com.h2ph.PrismSurvival.getInstance().getPrivateMessageManager();

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /reply <message> or /reply <player> <message>");
            return true;
        }

        Player target = null;
        String message = "";

        Player potentialTarget = Bukkit.getPlayer(args[0]);
        if (potentialTarget != null) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /reply <player> <message>");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            target = potentialTarget;

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            message = messageBuilder.toString().trim();

            if (!pmManager.hasConversation(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "You dont have a previous private messages to this player.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else {
            UUID lastMessengerId = pmManager.getReplyTarget(player.getUniqueId());
            if (lastMessengerId == null) {
                player.sendMessage(ChatColor.RED + "You have nobody to reply to.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            target = Bukkit.getPlayer(lastMessengerId);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player is not online.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            message = messageBuilder.toString().trim();
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot message yourself.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(target.getUniqueId());
        if (data != null && !data.isPrivateMessages()) {
            String errorMsg = ChatColor.RED + "User disabled private messages.";
            player.sendMessage(errorMsg);
            player.sendActionBar(Component.text("User disabled private messages.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String senderFormat = ChatColor.translateAlternateColorCodes('&',
                "&6you -> " + target.getName() + ":&f " + message);
        String receiverFormat = ChatColor.translateAlternateColorCodes('&',
                "&6" + player.getName() + " -> you:&f " + message);

        player.sendMessage(senderFormat);
        target.sendMessage(receiverFormat);

        pmManager.setReplyTarget(target.getUniqueId(), player.getUniqueId());
        pmManager.setReplyTarget(player.getUniqueId(), target.getUniqueId());

        com.prismcore.survival.manager.PlayerData targetData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(target.getUniqueId());
        if (targetData != null && targetData.isSoundNotifications()) {
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0)
            return "Expired";
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0)
            sb.append(d).append("d ");
        if (h > 0)
            sb.append(h).append("h ");
        if (m > 0)
            sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0)
            sb.append(s).append("s");

        return sb.toString().trim();
    }
}
