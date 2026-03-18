package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.ChatColor;

public class MotdListener implements Listener {
    private final PrismSurvival plugin;
    public MotdListener(PrismSurvival plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!plugin.getSurvivalConfig().getBoolean("motd.enabled", true)) {
            return;
        }
        String raw = plugin.getSurvivalConfig().getString("motd.motd", "FALCON");
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        event.setMotd(colored);
    }
}