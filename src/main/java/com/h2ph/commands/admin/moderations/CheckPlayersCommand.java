package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckPlayersCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public CheckPlayersCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("falcon.checkplayers")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (onlinePlayers.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No players are currently online.");
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
        sender.sendMessage(ChatColor.RED + " Online Players: " + ChatColor.WHITE + onlinePlayers.size());
        sender.sendMessage("");

        for (Player player : onlinePlayers) {
            String worldName = getWorldDisplayName(player.getWorld());
            sender.sendMessage(ChatColor.RED + " Player: " + ChatColor.WHITE + player.getName() + 
                             ChatColor.RED + " | World: " + ChatColor.WHITE + worldName);
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
        return true;
    }

    /**
     * Converts the world environment to a user-friendly display name
     */
    private String getWorldDisplayName(World world) {
        switch (world.getEnvironment()) {
            case NORMAL:
                return "Overworld";
            case NETHER:
                return "Nether";
            case THE_END:
                return "The End";
            default:
                return world.getName();
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}