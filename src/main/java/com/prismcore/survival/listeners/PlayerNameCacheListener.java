package com.prismcore.survival.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.utils.PlayerNameCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Updates the PlayerNameCache when players join/leave to keep recent players up to date
 */
public class PlayerNameCacheListener implements Listener {
    
    private final PlayerNameCache playerNameCache;
    
    public PlayerNameCacheListener(PrismSurvival plugin) {
        this.playerNameCache = plugin.getPlayerNameCache();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerNameCache.addRecentPlayer(event.getPlayer().getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerNameCache.addRecentPlayer(event.getPlayer().getName());
    }
}