package com.h2ph.commands.player;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StoreCommand implements CommandExecutor {

    private final Falcon plugin;

    public StoreCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> messages = plugin.getSurvivalConfig().getStringList("store");

        if (messages.isEmpty()) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', "&fCheck out our store: &ahttps://falcon.tebex.io/"));
            return true;
        }

        for (String msg : messages) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        return true;
    }
}
