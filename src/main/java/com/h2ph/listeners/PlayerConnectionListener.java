package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.ActivityLogger;
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
        plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId())
                .setLastSeenUpdate(System.currentTimeMillis());
        plugin.getActivityLogger().log(event.getPlayer().getUniqueId(), ActivityLogger.LogType.GENERAL,
                "Joined the server");

        // Log IP for alt-account tracking
        if (plugin.getOffendPlugin() != null && plugin.getOffendPlugin().getDatabaseManager() != null) {
            String ip = event.getPlayer().getAddress() != null
                    ? event.getPlayer().getAddress().getAddress().getHostAddress()
                    : "unknown";
            plugin.getOffendPlugin().getDatabaseManager().logIP(event.getPlayer().getUniqueId(), ip);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save and unload data
        plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId())
                .setLastSeenUpdate(System.currentTimeMillis());
        plugin.getActivityLogger().log(event.getPlayer().getUniqueId(), ActivityLogger.LogType.GENERAL,
                "Left the server");
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}
