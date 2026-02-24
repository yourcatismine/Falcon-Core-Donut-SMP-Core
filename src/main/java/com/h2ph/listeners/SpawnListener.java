package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnListener implements Listener {

    private final PrismSurvival plugin;

    public SpawnListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (data != null && data.isCombatLogged()) {
            // Respect bed or anchor spawn points
            if (event.isBedSpawn() || player.getBedSpawnLocation() != null) {
                data.setCombatLogged(false);
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                return;
            }

            Location spawn = plugin.getSpawnManager().getGlobalSpawn();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            // Reset the flag after respawning
            data.setCombatLogged(false);
            plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (data != null && data.isCombatLogged()) {
            // Respect bed or anchor spawn points
            if (player.getBedSpawnLocation() != null) {
                data.setCombatLogged(false);
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                return;
            }

            Location spawn = plugin.getSpawnManager().getGlobalSpawn();
            if (spawn != null) {
                plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
                    player.teleport(spawn);
                    if (data.isCombatLogged()) {
                        data.setCombatLogged(false);
                        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                    }
                }, 1L);
            }
        }
    }
}
