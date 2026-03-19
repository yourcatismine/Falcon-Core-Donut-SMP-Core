package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.VanishManager;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public VanishCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("falcon.vanish")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        VanishManager vanishManager = plugin.getVanishManager();
        vanishManager.toggleVanish(player);

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean isVanished = data.isVanished();

        if (isVanished) {
            String chatMsg = ChatColor.translateAlternateColorCodes('&', "&7You are in vanished.");
            String actionMsg = ChatColor.translateAlternateColorCodes('&', "&fVanish Activated");

            player.sendMessage(chatMsg);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else {
            String chatMsg = ChatColor.translateAlternateColorCodes('&', "&7You are unvanished.");
            String actionMsg = ChatColor.translateAlternateColorCodes('&', "&fVanish Deactivated");

            player.sendMessage(chatMsg);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
