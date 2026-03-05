package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CheckHistoryCommand implements CommandExecutor {

    private final PrismSurvival plugin;

    public CheckHistoryCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prism.admin.checkhistory")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "Could not load your player data.");
            return true;
        }

        // Toggle the history logging state
        boolean newState = !data.isCheckHistory();
        data.setCheckHistory(newState);
        
        // Save the change
        plugin.getPlayerDataManager().savePlayerAsync(player.getUniqueId());

        if (newState) {
            // History logging turned ON - Actionbar only always show
            String actionMsg = ChatColor.translateAlternateColorCodes('&', "&7Logging History on.");

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else {
            // History logging turned OFF - Actionbar only
            String actionMsg = ChatColor.translateAlternateColorCodes('&', "&7Logging History off.");

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        return true;
    }
}