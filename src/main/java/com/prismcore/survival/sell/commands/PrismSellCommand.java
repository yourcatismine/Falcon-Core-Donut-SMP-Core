package com.prismcore.survival.sell.commands;

import com.prismcore.survival.sell.PrismSell;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PrismSellCommand implements CommandExecutor {
    private final PrismSell plugin;

    public PrismSellCommand(PrismSell plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }
}
