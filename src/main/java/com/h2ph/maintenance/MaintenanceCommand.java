package com.h2ph.maintenance;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MaintenanceCommand implements CommandExecutor {

    private final MaintenanceManager maintenanceManager;

    public MaintenanceCommand(MaintenanceManager maintenanceManager) {
        this.maintenanceManager = maintenanceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("falcon.maintenance")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        boolean newState = !maintenanceManager.isMaintenanceEnabled();
        maintenanceManager.setMaintenance(newState);

        if (newState) {
            sender.sendMessage(
                    ChatColor.GRAY + "Maintenance has been " + ChatColor.GREEN + "enabled" + ChatColor.GRAY + ".");
        } else {
            sender.sendMessage(
                    ChatColor.GRAY + "Maintenance has been " + ChatColor.RED + "disabled" + ChatColor.GRAY + ".");
        }

        return true;
    }
}
