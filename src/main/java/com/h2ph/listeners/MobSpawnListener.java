package com.h2ph.listeners;
import org.bukkit.entity.EntityType;
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
        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            return;
        }

        boolean isTargetMob = (event.getEntity() instanceof Monster)
        || (event.getEntity() instanceof Slime) || event.getEntityType() == EntityType.PHANTOM;

        if (!isTargetMob) {
            return;
        }
        
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