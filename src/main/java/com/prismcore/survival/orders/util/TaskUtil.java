/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.prismcore.survival.orders.util;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class TaskUtil {
    private static volatile Boolean FOLIA = null;

    private TaskUtil() {
    }

    public static boolean isFolia() {
        if (FOLIA != null) {
            return FOLIA;
        }
        try {
            Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler", new Class[0]);
            Bukkit.getServer().getClass().getMethod("getRegionScheduler", new Class[0]);
            FOLIA = true;
        } catch (Throwable t) {
            FOLIA = false;
        }
        return FOLIA;
    }

    public static Handle runGlobal(Plugin plugin, Runnable r) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(r, "runnable");
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, r));
        }
        Object sched = TaskUtil.getGlobalScheduler();
        if (TaskUtil.tryInvokeExecute(sched, plugin, r)) {
            return new ReflectHandle(null);
        }
        Object task = TaskUtil.tryInvokeRunGlobal(sched, plugin, r);
        return new ReflectHandle(task);
    }

    public static Handle runGlobalLater(Plugin plugin, Runnable r, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(r, "runnable");
        delayTicks = Math.max(0L, delayTicks);
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin, r, delayTicks));
        }
        Object sched = TaskUtil.getGlobalScheduler();
        Object task = TaskUtil.tryInvokeRunDelayedGlobal(sched, plugin, r, delayTicks);
        return new ReflectHandle(task);
    }

    public static Handle runGlobalTimer(Plugin plugin, Runnable r, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(r, "runnable");
        delayTicks = Math.max(0L, delayTicks);
        periodTicks = Math.max(1L, periodTicks);
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin, r, delayTicks, periodTicks));
        }
        Object sched = TaskUtil.getGlobalScheduler();
        Object task = TaskUtil.tryInvokeRunAtFixedRateGlobal(sched, plugin, r, delayTicks, periodTicks);
        return new ReflectHandle(task);
    }

    public static Handle runAtLocation(Plugin plugin, Location loc, Runnable r) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(loc, "loc");
        Objects.requireNonNull(r, "runnable");
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, r));
        }
        Object sched = TaskUtil.getRegionScheduler();
        if (TaskUtil.tryInvokeExecuteLocation(sched, plugin, loc, r)) {
            return new ReflectHandle(null);
        }
        Object task = TaskUtil.tryInvokeRunLocation(sched, plugin, loc, r);
        return new ReflectHandle(task);
    }

    public static Handle runAtLocationLater(Plugin plugin, Location loc, Runnable r, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(loc, "loc");
        Objects.requireNonNull(r, "runnable");
        delayTicks = Math.max(0L, delayTicks);
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin, r, delayTicks));
        }
        Object sched = TaskUtil.getRegionScheduler();
        Object task = TaskUtil.tryInvokeRunDelayedLocation(sched, plugin, loc, r, delayTicks);
        return new ReflectHandle(task);
    }

    public static Handle runAtLocationTimer(Plugin plugin, Location loc, Runnable r, long delayTicks,
            long periodTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(loc, "loc");
        Objects.requireNonNull(r, "runnable");
        delayTicks = Math.max(0L, delayTicks);
        periodTicks = Math.max(1L, periodTicks);
        if (!TaskUtil.isFolia()) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin, r, delayTicks, periodTicks));
        }
        Object sched = TaskUtil.getRegionScheduler();
        Object task = TaskUtil.tryInvokeRunAtFixedRateLocation(sched, plugin, loc, r, delayTicks, periodTicks);
        return new ReflectHandle(task);
    }

    public static Handle runEntity(Plugin plugin, Entity e, Runnable r) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(r, "runnable");
        if (!TaskUtil.isFolia() || e == null) {
            return new BukkitHandle(Bukkit.getScheduler().runTask(plugin, r));
        }
        Object sched = TaskUtil.getEntityScheduler(e);
        if (sched != null) {
            if (TaskUtil.tryInvokeExecuteEntity(sched, plugin, r)) {
                return new ReflectHandle(null);
            }
            Object task = TaskUtil.tryInvokeRunEntity(sched, plugin, r);
            if (task != null) {
                return new ReflectHandle(task);
            }
        }
        return TaskUtil.runAtLocation(plugin, e.getLocation(), r);
    }

    public static Handle runEntityLater(Plugin plugin, Entity e, Runnable r, long delayTicks) {
        Object task;
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(r, "runnable");
        delayTicks = Math.max(0L, delayTicks);
        if (!TaskUtil.isFolia() || e == null) {
            return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin, r, delayTicks));
        }
        Object sched = TaskUtil.getEntityScheduler(e);
        if (sched != null && (task = TaskUtil.tryInvokeRunDelayedEntity(sched, plugin, r, delayTicks)) != null) {
            return new ReflectHandle(task);
        }
        return TaskUtil.runAtLocationLater(plugin, e.getLocation(), r, delayTicks);
    }

    private static Object getGlobalScheduler() {
        try {
            Method m = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler", new Class[0]);
            return m.invoke((Object) Bukkit.getServer(), new Object[0]);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getRegionScheduler() {
        try {
            Method m = Bukkit.getServer().getClass().getMethod("getRegionScheduler", new Class[0]);
            return m.invoke((Object) Bukkit.getServer(), new Object[0]);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getEntityScheduler(Entity e) {
        try {
            Method m = e.getClass().getMethod("getScheduler", new Class[0]);
            return m.invoke((Object) e, new Object[0]);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Consumer<Object> consumerOf(Runnable r) {
        return ignored -> {
            try {
                r.run();
            } catch (Throwable throwable) {
            }
        };
    }

    private static boolean tryInvokeExecute(Object sched, Plugin plugin, Runnable r) {
        if (sched == null) {
            return false;
        }
        try {
            Method m = sched.getClass().getMethod("execute", Plugin.class, Runnable.class);
            m.invoke(sched, plugin, r);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryInvokeExecuteLocation(Object sched, Plugin plugin, Location loc, Runnable r) {
        if (sched == null) {
            return false;
        }
        try {
            Method m = sched.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
            m.invoke(sched, plugin, loc, r);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryInvokeExecuteEntity(Object sched, Plugin plugin, Runnable r) {
        if (sched == null) {
            return false;
        }
        try {
            Method m = sched.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class);
            m.invoke(sched, plugin, r, (Runnable) () -> {
            });
            return true;
        } catch (Throwable m) {
            try {
                Method m2 = sched.getClass().getMethod("execute", Plugin.class, Runnable.class);
                m2.invoke(sched, plugin, r);
                return true;
            } catch (Throwable throwable) {
                return false;
            }
        }
    }

    private static Object tryInvokeRunGlobal(Object sched, Plugin plugin, Runnable r) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("run", Plugin.class, Consumer.class);
            return m.invoke(sched, plugin, TaskUtil.consumerOf(r));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunDelayedGlobal(Object sched, Plugin plugin, Runnable r, long delayTicks) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Long.TYPE);
            return m.invoke(sched, plugin, TaskUtil.consumerOf(r), delayTicks);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunAtFixedRateGlobal(Object sched, Plugin plugin, Runnable r, long delayTicks,
            long periodTicks) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, Long.TYPE, Long.TYPE);
            return m.invoke(sched, plugin, TaskUtil.consumerOf(r), delayTicks, periodTicks);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunLocation(Object sched, Plugin plugin, Location loc, Runnable r) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("run", Plugin.class, Location.class, Consumer.class);
            return m.invoke(sched, plugin, loc, TaskUtil.consumerOf(r));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunDelayedLocation(Object sched, Plugin plugin, Location loc, Runnable r,
            long delayTicks) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class,
                    Long.TYPE);
            return m.invoke(sched, plugin, loc, TaskUtil.consumerOf(r), delayTicks);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunAtFixedRateLocation(Object sched, Plugin plugin, Location loc, Runnable r,
            long delayTicks, long periodTicks) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class,
                    Long.TYPE, Long.TYPE);
            return m.invoke(sched, plugin, loc, TaskUtil.consumerOf(r), delayTicks, periodTicks);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeRunEntity(Object sched, Plugin plugin, Runnable r) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            return m.invoke(sched, plugin, TaskUtil.consumerOf(r), (Runnable) () -> {
            });
        } catch (Throwable m) {
            try {
                Method m2 = sched.getClass().getMethod("run", Plugin.class, Consumer.class);
                return m2.invoke(sched, plugin, TaskUtil.consumerOf(r));
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    private static Object tryInvokeRunDelayedEntity(Object sched, Plugin plugin, Runnable r, long delayTicks) {
        if (sched == null) {
            return null;
        }
        try {
            Method m = sched.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class,
                    Long.TYPE);
            return m.invoke(sched, plugin, TaskUtil.consumerOf(r), (Runnable) () -> {
            }, delayTicks);
        } catch (Throwable m) {
            try {
                Method m2 = sched.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Long.TYPE);
                return m2.invoke(sched, plugin, TaskUtil.consumerOf(r), delayTicks);
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    private static final class BukkitHandle
            implements Handle {
        private final BukkitTask task;

        BukkitHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (this.task != null) {
                this.task.cancel();
            }
        }
    }

    private static final class ReflectHandle
            implements Handle {
        private final Object task;

        ReflectHandle(Object task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (this.task == null) {
                return;
            }
            try {
                Method m = this.task.getClass().getMethod("cancel", new Class[0]);
                m.invoke(this.task, new Object[0]);
                return;
            } catch (Throwable m) {
                try {
                    Method m2 = this.task.getClass().getMethod("cancel", Boolean.TYPE);
                    m2.invoke(this.task, true);
                } catch (Throwable throwable) {
                }
                return;
            }
        }
    }

    public static interface Handle {
        public void cancel();
    }
}
