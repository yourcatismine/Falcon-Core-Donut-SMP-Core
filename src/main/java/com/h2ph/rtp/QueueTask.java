package com.h2ph.rtp;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class QueueTask implements Runnable {

    private final PrismSurvival plugin;
    private final Player player;
    private final RTPQueueManager.RTPQueueRegion region;
    private org.bukkit.scheduler.BukkitTask task;

    public QueueTask(PrismSurvival plugin, Player player, RTPQueueManager.RTPQueueRegion region) {
        this.plugin = plugin;
        this.player = player;
        this.region = region;

        // Play enter sound: Cave 3 (Pitch 0.3 approx)
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.3f);

        // Join global queue
        plugin.getRTPQueueManager().joinQueue(region.name, player.getUniqueId());
    }

    public void start() {
        // Use scheduler adapter to support Folia, scheduling on the Entity (Player) to
        // ensure proper thread context
        this.task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, this, 0L, 20L);
    }

    @Override
    public void run() {
        // Validate player and region
        if (!player.isOnline()) {
            cleanup(false);
            cancel();
            return;
        }

        RTPQueueManager.RTPQueueRegion currentRegion = plugin.getRTPQueueManager().getQueueAt(player.getLocation());
        if (currentRegion == null || !currentRegion.name.equals(region.name)) {
            cleanup(true); // User left voluntarily
            cancel();
            return;
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void cleanup(boolean leftQueue) {
        // Leave global queue
        plugin.getRTPQueueManager().leaveQueue(region.name, player.getUniqueId());

        if (leftQueue && player.isOnline()) {
            // Play leave sound: Cave 7 (Pitch 0.7 approx)
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.7f);
        }
    }
}
