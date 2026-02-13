package com.h2ph.commands.player;

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
import java.util.stream.Collectors;

public class MsgCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String targetName = args[0];

        // Build the message from args[1] onwards
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        Player target = Bukkit.getPlayer(targetName);

        if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Check if player exists (online)
        if (target == null) {
            // Check if they exist offline
            if (Bukkit.getOfflinePlayer(targetName).hasPlayedBefore()) {
                // User is offline
                String offlineMsg = ChatColor.RED + "This user is not online.";
                player.sendMessage(offlineMsg);
                player.sendActionBar(Component.text("This user is not online.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            } else {
                // User does not exist
                String noExistMsg = ChatColor.RED + "That user does not exist.";
                player.sendMessage(noExistMsg);
                player.sendActionBar(Component.text("That user does not exist.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
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
                "&5YOU -> " + target.getName() + " - &7" + message);
        String receiverFormat = ChatColor.translateAlternateColorCodes('&',
                "&5" + player.getName() + " -> YOU -&7 " + message);

        player.sendMessage(senderFormat);
        target.sendMessage(receiverFormat);

        com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(player.getUniqueId(),
                ActivityLogger.LogType.MESSAGE, "PM to " + target.getName() + ": " + message);
        com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(target.getUniqueId(),
                ActivityLogger.LogType.MESSAGE, "PM from " + player.getName() + ": " + message);

        // Sound Notification
        com.prismcore.survival.manager.PlayerData targetData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(target.getUniqueId());
        if (targetData != null && targetData.isSoundNotifications()) {
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
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
