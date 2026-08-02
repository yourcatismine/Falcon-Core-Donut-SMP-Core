package com.falconcore.survival.sell.commands;

import com.falconcore.survival.sell.FalconSell;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FalconSellCommand implements CommandExecutor {
    private final FalconSell plugin;

    public FalconSellCommand(FalconSell plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }
}
