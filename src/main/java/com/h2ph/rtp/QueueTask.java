package com.h2ph.rtp;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class QueueTask implements Runnable {

    private final Falcon plugin;
    private final Player player;
    private final RTPQueueManager.RTPQueueRegion region;
    private org.bukkit.scheduler.BukkitTask task;

    public QueueTask(Falcon plugin, Player player, RTPQueueManager.RTPQueueRegion region) {
        this.plugin = plugin;
        this.player = player;
        this.region = region;

        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.3f);

        plugin.getRTPQueueManager().joinQueue(region.name, player.getUniqueId());
    }

    public void start() {
        this.task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, this, 0L, 20L);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cleanup(false);
            cancel();
            return;
        }

        RTPQueueManager.RTPQueueRegion currentRegion = plugin.getRTPQueueManager().getQueueAt(player.getLocation());
        if (currentRegion == null || !currentRegion.name.equals(region.name)) {
            cleanup(true);
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
        plugin.getRTPQueueManager().leaveQueue(region.name, player.getUniqueId());

        if (leftQueue && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.7f);
        }
    }
}
