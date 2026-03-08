package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DiscordCommand implements CommandExecutor {

    private final PrismSurvival plugin;

    public DiscordCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> messages = plugin.getSurvivalConfig().getStringList("discord");

        if (messages.isEmpty()) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', "&fJoin our discord: &ahttps://discord.gg/psmp"));
            return true;
        }

        for (String msg : messages) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        return true;
    }
}
