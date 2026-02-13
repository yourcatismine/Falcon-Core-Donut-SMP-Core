package com.prismcore.survival.scheduler;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerAdapter {

    private final PrismSurvival plugin;

    public SchedulerAdapter(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    /**
     * Run a task on the main thread (global region for Folia)
     */
    public void runTask(Runnable task) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }
        try {
            // Try Folia's global region scheduler first
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler for non-Folia servers
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task async
     */
    public void runTaskAsync(Runnable task) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }
        try {
            // Try Folia's async scheduler first
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit async scheduler for non-Folia servers
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runTaskAsynchronously(Runnable task) {
        runTaskAsync(task);
    }

    /**
     * Run a task later on the main thread
     */
    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        try {
            // Try Folia's global region scheduler first
            Object scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> task.run(),
                    delayTicks);
            return new FoliaBukkitTaskWrapper(scheduledTask);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler for non-Folia servers
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a task later async
     */
    public void runTaskLaterAsync(Runnable task, long delayTicks) {
        try {
            // Try Folia's async scheduler first
            Bukkit.getAsyncScheduler().runDelayed(plugin, st -> task.run(), delayTicks * 50,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit async scheduler for non-Folia servers
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    /**
     * Run a repeating task
     */
    /**
     * Run a repeating task on the main thread
     */
    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        try {
            // Try Folia's global region scheduler first
            // Note: runAtFixedRate handles repeating tasks
            // Folia requires initialDelayTicks > 0
            long actualDelay = Math.max(1L, delayTicks);
            Object scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                    st -> task.run(), actualDelay, periodTicks);
            return new FoliaBukkitTaskWrapper(scheduledTask);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler for non-Folia servers
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Run a repeating task async
     */
    public BukkitTask runTaskTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        try {
            // Try Folia's async scheduler first
            // Async scheduler uses milliseconds (50ms = 1 tick)
            // Folia requires initialDelay > 0
            long actualDelayMs = Math.max(1L, delayTicks) * 50;
            Object scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                    st -> task.run(), actualDelayMs, periodTicks * 50, java.util.concurrent.TimeUnit.MILLISECONDS);
            return new FoliaBukkitTaskWrapper(scheduledTask);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit async scheduler for non-Folia servers
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Run a task immediately on the entity's thread (Entity Scheduler for Folia)
     */
    public void runEntityTask(org.bukkit.entity.Entity entity, Runnable task) {
        try {
            // Try Folia's entity scheduler
            entity.getScheduler().run(plugin, st -> task.run(), null);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to simple runTask (not thread-safe in the same way, but safe for
            // Bukkit)
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task later on the entity's thread (Entity Scheduler for Folia)
     */
    public void runEntityTaskLater(org.bukkit.entity.Entity entity, Runnable task, long delayTicks) {
        try {
            // Try Folia's entity scheduler
            entity.getScheduler().runDelayed(plugin, st -> task.run(), null, delayTicks);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a repeating task on the entity's thread (Entity Scheduler for Folia)
     */
    public BukkitTask runEntityTaskTimer(org.bukkit.entity.Entity entity, Runnable task, long delayTicks,
            long periodTicks) {
        try {
            // Try Folia's entity scheduler
            // Folia's runAtFixedRate requires initialDelayTicks > 0
            long actualDelay = Math.max(1L, delayTicks);
            Object scheduledTask = entity.getScheduler().runAtFixedRate(plugin, st -> task.run(), null, actualDelay,
                    periodTicks);
            return new FoliaBukkitTaskWrapper(scheduledTask);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler
            // For entity tasks on Bukkit, we usually just use global scheduler, but we can
            // try to be specific if needed.
            // Using global scheduler is fine for Bukkit.
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Run a task at a specific location (Region Scheduler for Folia)
     */
    public void runAtLocation(org.bukkit.Location location, Runnable task) {
        try {
            // Try Folia's region scheduler first
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Fall back to Bukkit scheduler (Global/Main thread)
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Wrapper for Folia tasks to implement BukkitTask interface
     * This allows us to return a BukkitTask even when using Folia schedulers
     */
    private static class FoliaBukkitTaskWrapper implements BukkitTask {
        private final Object foliaTask;

        public FoliaBukkitTaskWrapper(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        @Override
        public int getTaskId() {
            return -1; // Folia tasks don't have integer IDs like Bukkit
        }

        @Override
        public org.bukkit.plugin.Plugin getOwner() {
            try {
                java.lang.reflect.Method getOwner = foliaTask.getClass().getMethod("getOwner");
                getOwner.setAccessible(true);
                return (org.bukkit.plugin.Plugin) getOwner.invoke(foliaTask);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean isSync() {
            // Start by assuming it's sync if checking usually happens on main thread tasks
            // Ideally we check instance type but for wrapper simplicity we guess or check
            // class name
            return !foliaTask.getClass().getName().contains("Async");
        }

        @Override
        public boolean isCancelled() {
            try {
                java.lang.reflect.Method isCancelled = foliaTask.getClass().getMethod("isCancelled");
                isCancelled.setAccessible(true);
                return (boolean) isCancelled.invoke(foliaTask);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void cancel() {
            try {
                java.lang.reflect.Method cancel = foliaTask.getClass().getMethod("cancel");
                cancel.setAccessible(true);
                cancel.invoke(foliaTask);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
