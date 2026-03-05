package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobSpawnListener implements Listener {

    private final PrismSurvival plugin;

    public MobSpawnListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only care about natural spawns of Monsters (Hostile mobs) and Slimes
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        if (!(event.getEntity() instanceof Monster) && !(event.getEntity() instanceof Slime)) {
            return;
        }

        // Radius should be exactly 150 as requested.
        // If ANY player within 150 blocks has the setting enabled, cancel the spawn.
        double radiusSquared = 150.0 * 150.0;

        boolean shouldCancel = event.getLocation().getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(event.getLocation()) <= radiusSquared)
                .anyMatch(p -> {
                    PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    return data != null && data.isDisableMobSpawns();
                });

        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
}
