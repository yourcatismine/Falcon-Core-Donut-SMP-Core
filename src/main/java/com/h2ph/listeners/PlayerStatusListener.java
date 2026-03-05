package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStatusListener implements Listener {

    private final PrismSurvival plugin;

    public PlayerStatusListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDatabaseManager().updateStatusAsync(event.getPlayer().getUniqueId(), "Online");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDatabaseManager().updateStatusAsync(event.getPlayer().getUniqueId(), "Offline");
        plugin.getDatabaseManager().saveLastLocationAsync(event.getPlayer().getUniqueId(),
                event.getPlayer().getLocation());
    }
}
