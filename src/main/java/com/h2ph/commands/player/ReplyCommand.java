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
import com.prismcore.survival.manager.ActivityLogger;
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
        PrivateMessageManager pmManager = com.h2ph.PrismSurvival.getInstance().getPrivateMessageManager();

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /reply <message> or /reply <player> <message>");
            return true;
        }

        Player target = null;
        String message = "";

        // Check if first argument is a player
        Player potentialTarget = Bukkit.getPlayer(args[0]);
        if (potentialTarget != null) {
            // Case: /reply <player> <message>
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /reply <player> <message>");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            target = potentialTarget;

            // Build message from args[1] onwards
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            message = messageBuilder.toString().trim();

            // Check if there is an existing conversation
            if (!pmManager.hasConversation(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "You dont have a previous private messages to this player.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else {
            // Case: /reply <message> (Reply to last messenger)
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

            // Build message from args[0] onwards
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

        // Check if target has private messages enabled
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(target.getUniqueId());
        if (data != null && !data.isPrivateMessages()) {
            String errorMsg = ChatColor.RED + "User disabled private messages.";
            player.sendMessage(errorMsg);
            player.sendActionBar(Component.text("User disabled private messages.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Send messages
        String senderFormat = ChatColor.translateAlternateColorCodes('&',
                "&dyou -> " + target.getName() + ":&f " + message);
        String receiverFormat = ChatColor.translateAlternateColorCodes('&',
                "&d" + player.getName() + " -> you:&f " + message);

        player.sendMessage(senderFormat);
        target.sendMessage(receiverFormat);

        com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(player.getUniqueId(),
                ActivityLogger.LogType.MESSAGE, "PM to " + target.getName() + ": " + message);
        com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(target.getUniqueId(),
                ActivityLogger.LogType.MESSAGE, "PM from " + player.getName() + ": " + message);

        // Update reply targets
        pmManager.setReplyTarget(target.getUniqueId(), player.getUniqueId());
        pmManager.setReplyTarget(player.getUniqueId(), target.getUniqueId());

        // Sound Notification
        com.prismcore.survival.manager.PlayerData targetData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(target.getUniqueId());
        if (targetData != null && targetData.isSoundNotifications()) {
            // Sound removed upon request
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
}
