package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PrismSurvival plugin;

    public PlayerConnectionListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Pre-load data asynchronously if possible, but for now blocking on main thread
        // is standard for simple YAML
        plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save and unload data
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}
