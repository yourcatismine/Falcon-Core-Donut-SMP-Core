package com.h2ph.maintenance;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class MaintenanceListener implements Listener {

    private final MaintenanceManager maintenanceManager;

    public MaintenanceListener(MaintenanceManager maintenanceManager) {
        this.maintenanceManager = maintenanceManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!maintenanceManager.isMaintenanceEnabled()) {
            return;
        }

        if (maintenanceManager.canBypass(event.getPlayer())) {
            return;
        }

        FileConfiguration config = maintenanceManager.getMaintenanceConfig();
        List<String> messageLines = config.getStringList("disconnect-message");
        String kickMessage = ChatColor.translateAlternateColorCodes('&', String.join("\n", messageLines));

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
    }

    @EventHandler
    public void onServerListPing(org.bukkit.event.server.ServerListPingEvent event) {
        if (!maintenanceManager.isMaintenanceEnabled()) {
            return;
        }

        FileConfiguration config = maintenanceManager.getMaintenanceConfig();
        List<String> messageLines = config.getStringList("ping-message");
        if (!messageLines.isEmpty()) {
            event.setMotd(ChatColor.translateAlternateColorCodes('&',
                    messageLines.get(0) + "\n" + (messageLines.size() > 1 ? messageLines.get(1) : "")));
        }
    }
}
