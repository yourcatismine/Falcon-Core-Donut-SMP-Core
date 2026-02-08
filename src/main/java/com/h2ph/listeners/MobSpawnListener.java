package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Comparator;

public class MobSpawnListener implements Listener {

    private final PrismSurvival plugin;

    public MobSpawnListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only care about natural spawns of Monsters (Hostile mobs)
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // Find nearest player within standard spawn range (128 blocks)
        // If the nearest player has the setting enabled, cancel the spawn.
        // This effectively creates a "personal safe zone".

        Player nearest = event.getLocation().getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(event.getLocation()) <= 128 * 128)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(event.getLocation())))
                .orElse(null);

        if (nearest != null) {
            PlayerData data = plugin.getPlayerDataManager().get(nearest.getUniqueId());
            if (data != null && data.isDisableMobSpawns()) {
                event.setCancelled(true);
            }
        }
    }
}
