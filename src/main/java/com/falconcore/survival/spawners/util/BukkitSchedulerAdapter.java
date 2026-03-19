package com.falconcore.survival.spawners.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final boolean isFolia;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Falcon-scheduler");
        t.setDaemon(true);
        return t;
    });

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
        } catch (ClassNotFoundException ignored) {
        }
        this.isFolia = foliaDetected;
    }

    @Override
    public Object runRepeatingSync(Runnable task, long delay, long period) {
        if (isFolia) {
            return scheduleFoliaRepeating(task, delay, period);
        }
        try {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        } catch (UnsupportedOperationException ex) {
            return scheduleFoliaRepeating(task, delay, period);
        }
    }

    private Object scheduleFoliaRepeating(Runnable task, long delay, long period) {
        try {
            Object globalScheduler = Bukkit.getServer().getClass()
                    .getMethod("getGlobalRegionScheduler")
                    .invoke(Bukkit.getServer());

            Method runAtFixedRate = globalScheduler.getClass().getMethod(
                    "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class);

            java.util.function.Consumer<?> consumer = scheduledTask -> task.run();
            Object foliaTask = runAtFixedRate.invoke(globalScheduler, plugin, consumer, Math.max(1, delay),
                    Math.max(1, period));
            return foliaTask;
        } catch (Exception e) {
            plugin.getLogger().severe("Eerror: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object runRepeatingAsync(Runnable task, long delay, long period) {
        try {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        } catch (UnsupportedOperationException ex) {
            long initialMs = Math.max(0, delay) * 50L;
            long periodMs = Math.max(1, period) * 50L;
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialMs, periodMs, TimeUnit.MILLISECONDS);
            return future;
        }
    }

    @Override
    public void cancel(Object handle) {
        if (handle == null)
            return;
        if (handle instanceof BukkitTask) {
            ((BukkitTask) handle).cancel();
            return;
        }
        if (handle instanceof ScheduledFuture) {
            ((ScheduledFuture<?>) handle).cancel(false);
            return;
        }

        try {
            Method cancelMethod = handle.getClass().getMethod("cancel");
            cancelMethod.invoke(handle);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not cancel Folia task: " + e.getMessage());
        }
    }
}