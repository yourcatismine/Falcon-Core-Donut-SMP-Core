package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.EnderChestGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EnderChestCommand implements CommandExecutor {

    private final PrismSurvival plugin;

    public EnderChestCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("prismcore.enderchest")) {
            return true;
        }
        new EnderChestGUI(plugin).open(player);
        return true;
    }
}
