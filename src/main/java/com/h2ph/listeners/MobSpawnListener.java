package com.h2ph.listeners;

import org.bukkit.entity.EntityType;
import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.Location;

public class MobSpawnListener implements Listener {

    private final Falcon plugin;
    private static final double RADIUS_SQUARED = 150.0 * 150.0;

    public MobSpawnListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        EntityType type = event.getEntityType();
        boolean isTargetMob = (event.getEntity() instanceof Monster)
        || (event.getEntity() instanceof Slime) || type == EntityType.PHANTOM
        || type == EntityType.GHAST || type == EntityType.HOGLIN
        || type == EntityType.SQUID || type == EntityType.GLOW_SQUID
        || type == EntityType.BAT;

        if (!isTargetMob) {
            return;
        }
        
        Location eventLoc = event.getLocation();
        if (eventLoc == null || eventLoc.getWorld() == null) return;

        // OPTIMIZATION: Using a classic loop instead of streams to avoid lambda overhead (ReferencePipeline.anyMatch)
        // which was taking significant CPU time in the trace.
        for (Player player : eventLoc.getWorld().getPlayers()) {
            // Location.distanceSquared is much faster than .distance
            if (player.getLocation().distanceSquared(eventLoc) <= RADIUS_SQUARED) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.isDisableMobSpawns()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}