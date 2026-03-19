package com.falconcore.survival.spawners.util;

public interface SchedulerAdapter {
    Object runRepeatingSync(Runnable task, long delay, long period);
    Object runRepeatingAsync(Runnable task, long delay, long period);
    void cancel(Object handle);
}