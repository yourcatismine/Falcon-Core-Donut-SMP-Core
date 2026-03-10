package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player movement between chunks to record who has walked where
 */
public class ChunkTrackingListener implements Listener {

    private final PrismSurvival plugin;

    private final Map<UUID, String> lastChunk = new HashMap<>();

    public ChunkTrackingListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null)
            return;

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Chunk toChunk = to.getChunk();
        String currentChunk = toChunk.getWorld().getName() + ":" + toChunk.getX() + ":" + toChunk.getZ();

        String previousChunk = lastChunk.get(playerId);
        if (currentChunk.equals(previousChunk)) {
            return;
        }

        lastChunk.put(playerId, currentChunk);

    }

    /**
     * Clean up tracking when player leaves
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastChunk.remove(event.getPlayer().getUniqueId());
    }
}