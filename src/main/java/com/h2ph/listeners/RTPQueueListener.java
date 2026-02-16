package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.rtp.QueueTask;
import com.h2ph.rtp.RTPQueueManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RTPQueueListener implements Listener {

    private final PrismSurvival plugin;
    private final Map<UUID, QueueTask> activeTasks = new HashMap<>();

    public RTPQueueListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null)
            return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return; // Ignore small movements
        }

        Player player = event.getPlayer();
        RTPQueueManager manager = plugin.getRTPQueueManager();
        RTPQueueManager.RTPQueueRegion regionTo = manager.getQueueAt(to);

        QueueTask currentTask = activeTasks.get(player.getUniqueId());

        if (regionTo != null) {
            // Player entered or is in a queue region
            if (currentTask == null) {
                // Determine parameters based on region name or config?
                // For now passing default or derived from region name mapping if needed.
                // Assuming "europe" region maps to "europe" worldType normal.

                QueueTask newTask = new QueueTask(plugin, player, regionTo);
                newTask.start();
                activeTasks.put(player.getUniqueId(), newTask);
            } else {
                // Already in queue, perform checks in task or here?
                // Task handles "leaving" check, but here we can optimize.
            }
        } else {
            // Player is NOT in a queue region
            if (currentTask != null) {
                // Check if they just left
                // Task will handle it on next tick usually, but we can force cancel for
                // responsiveness
                currentTask.cleanup(true);
                currentTask.cancel();
                activeTasks.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        QueueTask task = activeTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cleanup(false);
            task.cancel();
        }
    }
}
