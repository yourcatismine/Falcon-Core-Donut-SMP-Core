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
            return;
        }

        Player player = event.getPlayer();
        RTPQueueManager manager = plugin.getRTPQueueManager();
        RTPQueueManager.RTPQueueRegion regionTo = manager.getQueueAt(to);

        QueueTask currentTask = activeTasks.get(player.getUniqueId());

        if (regionTo != null) {
            if (currentTask == null) {

                QueueTask newTask = new QueueTask(plugin, player, regionTo);
                newTask.start();
                activeTasks.put(player.getUniqueId(), newTask);
            } else {
            }
        } else {
            if (currentTask != null) {
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
