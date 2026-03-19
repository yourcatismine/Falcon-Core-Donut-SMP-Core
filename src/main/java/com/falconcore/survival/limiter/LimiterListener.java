package com.falconcore.survival.limiter;

import com.h2ph.Falcon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class LimiterListener implements Listener {

    private final Falcon plugin;

    public LimiterListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (plugin.getLimiterManager() != null) {
            plugin.getLimiterManager().addChunk(event.getChunk());
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (plugin.getLimiterManager() != null) {
            plugin.getLimiterManager().removeChunk(event.getChunk());
        }
    }

    @EventHandler
    public void onEntitySpawn(org.bukkit.event.entity.EntitySpawnEvent event) {
        if (plugin.getLimiterManager() != null) {
            org.bukkit.Location loc = event.getLocation();
            plugin.getLimiterManager().addChunkCoord(loc.getWorld().getName(), loc.getBlockX() >> 4,
                    loc.getBlockZ() >> 4);
        }
    }
}
