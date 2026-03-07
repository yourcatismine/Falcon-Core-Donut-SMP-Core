package com.prismcore.survival.auction;

import java.util.Collections;
import java.util.List;
import com.prismcore.survival.auction.AuctionController;
import com.prismcore.survival.auction.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class AdminCommand
        implements CommandExecutor,
        TabCompleter {
    private final AuctionController controller;

    public AdminCommand(AuctionController controller) {
        this.controller = controller;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutauction.reload")) {
            sender.sendMessage(Utils.formatColors("&#ff4444You do not have permission to use this command."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            Bukkit.getOnlinePlayers().forEach(p -> p.closeInventory());
            this.controller.reloadAllConfigs();
            this.controller.getAuctionManager().saveToConfig();
            this.controller.getAuctionManager().loadFromConfig();
            String msg = Utils.formatColors("&#44ff44Auction system reloaded.");
            sender.sendMessage(msg);
            return true;
        }
        sender.sendMessage(Utils.formatColors("&#ff4444Usage: /ah reload"));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("donutauction.reload")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
