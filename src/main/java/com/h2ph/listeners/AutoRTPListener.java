package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.rtp.RTPManager;
import org.bukkit.configuration.file.FileConfiguration;
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

        // Ignore players who have a bed spawnpoint (only skip if they actually spawned
        // at bed)
        if (event.isBedSpawn()) {
            return;
        }

        // Load RTP config for Europe region
        FileConfiguration rtpConfig = plugin.getRTPRegionConfig("europe");
        if (rtpConfig == null || rtpConfig.getKeys(false).isEmpty()) {
            return;
        }

        // Check if auto-RTP on death is enabled
        if (!rtpConfig.getBoolean("auto-rtp-on-death", false)) {
            return;
        }

        // Folia compatibility: teleportInstant uses teleportAsync internally
        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                // Use silent=true to remove "Teleporting.." message
                RTPManager.teleportInstant(player, "europe", "overworld", true);

                // Give death items
                for (ItemStack item : plugin.getRTPDeathManager().getItems()) {
                    if (item != null) {
                        player.getInventory().addItem(item.clone());
                    }
                }
            }
        }, 1L);
    }
}
