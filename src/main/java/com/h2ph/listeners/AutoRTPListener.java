package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class AutoRTPListener implements Listener {

    private final PrismSurvival plugin;

    public AutoRTPListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (event.isBedSpawn()) {
            return;
        }

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                String worldName = player.getWorld().getName();
                org.bukkit.Location spawn = plugin.getSpawnManager().getBestSpawnForWorld(worldName);
                if (spawn != null) {
                    player.teleportAsync(spawn);
                }

                com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.isRespawnRTP()) {
                    for (ItemStack item : plugin.getRespawnGearManager().getItems()) {
                        if (item != null) {
                            player.getInventory().addItem(item.clone());
                        }
                    }
                }
            }
        }, 1L);
    }
}
