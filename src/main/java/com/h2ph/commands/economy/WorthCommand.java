package com.h2ph.commands.economy;

import com.h2ph.Falcon;
import com.h2ph.gui.WorthGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorthCommand implements CommandExecutor {

    private final Falcon plugin;

    public WorthCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        new WorthGUI(plugin, player).open();
        return true;
    }
}
