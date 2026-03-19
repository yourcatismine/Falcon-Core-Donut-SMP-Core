package com.prismcore.survival.spawners.storage;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.spawners.mob.SpawnerType;
import com.prismcore.survival.spawners.tasks.ProductionTask;
import com.prismcore.survival.spawners.tasks.HopperTransferTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import com.prismcore.survival.spawners.util.SchedulerAdapter; //
import com.prismcore.survival.spawners.util.BukkitSchedulerAdapter; //
import java.util.UUID; //
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {
    private final PrismSurvival plugin;
    private final Map<Location, SpawnerData> spawners = new ConcurrentHashMap<>();
    private ProductionTask productionTask;

    private final SchedulerAdapter scheduler; //
    private Object productionHandle;  //
    private Object hopperHandle;  //

    private final Map<Location, Integer> hopperGuiOpenCount = new ConcurrentHashMap<>(); //For pausing
    private final Map<Location, UUID> openGuiViewer = new ConcurrentHashMap<>(); //For tracking who has the GUI open

    public boolean isGuiOpen(SpawnerData data) {
        if (data == null || data.getLocation() == null) return false;
        return openGuiViewer.containsKey(data.getLocation());
    }

    public UUID getGuiViewer(SpawnerData data) {
        if (data == null || data.getLocation() == null) return null;
        return openGuiViewer.get(data.getLocation());
    }

    public boolean trySetGuiViewer(SpawnerData data, UUID viewer) {
        if (data == null || data.getLocation() == null || viewer == null) return false;
        return openGuiViewer.putIfAbsent(data.getLocation(), viewer) == null;
    }

    public void clearGuiViewer(SpawnerData data, UUID viewer) {
        if (data == null || data.getLocation() == null) return;
        openGuiViewer.computeIfPresent(data.getLocation(), (loc, u) -> u.equals(viewer) ? null : u);
    }

    public SpawnerManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.scheduler = new BukkitSchedulerAdapter(plugin); //
        long autoSave = plugin.getSpawnerConfig().getLong("settings.auto_save_interval", 6000L);
        scheduler.runRepeatingAsync(() -> saveSpawners(false), autoSave, autoSave); // periodic DB flush
    }

    public void loadSpawners() {
        try {
            Map<Location, SpawnerData> dbSpawners = plugin.getDatabaseManager().loadAllSpawnersSync();
            if (dbSpawners != null) spawners.putAll(dbSpawners);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load spawners from DB: " + e.getMessage());
        }

        productionTask = new ProductionTask(this);
        long prodInterval = plugin.getSpawnerConfig().getLong("settings.production_interval", 600L);
        productionHandle = scheduler.runRepeatingSync(productionTask::run, 0L, prodInterval);

        if (plugin.getSpawnerConfig().getBoolean("hopper.enabled", false)) {
            long checkDelayTicks = parseDelayToTicks(plugin.getSpawnerConfig().getString("hopper.check_delay", "3s"));
            int stacksPerTransfer = plugin.getSpawnerConfig().getInt("hopper.stack_per_transfer", 5);
            HopperTransferTask hopperTaskRunnable = new HopperTransferTask(this, stacksPerTransfer);
            hopperHandle = scheduler.runRepeatingSync(hopperTaskRunnable::run, 0L, checkDelayTicks);
        }
    }

    public void saveSpawners() {
        saveSpawners(true);
    }

    public void saveSpawners(boolean stopTask) {
        if (stopTask) {
            if (productionHandle != null) { scheduler.cancel(productionHandle); productionHandle = null; }
            if (hopperHandle != null) { scheduler.cancel(hopperHandle); hopperHandle = null; }
        }

        // Persist all spawners to DB asynchronously
        Map<Location, SpawnerData> snapshot = new HashMap<>(spawners);
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            for (SpawnerData data : snapshot.values()) {
                try {
                    plugin.getDatabaseManager().insertOrUpdateSpawnerSync(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save spawner at " + data.getLocation() + ": " + e.getMessage());
                }
            }
        });
    }

    public void pauseHopperFor(SpawnerData data) {
        if (data == null || data.getLocation() == null) return;
        hopperGuiOpenCount.compute(data.getLocation(), (loc, count) -> count == null ? 1 : count + 1);
    }
    
    public void resumeHopperFor(SpawnerData data) {
        if (data == null || data.getLocation() == null) return;
        hopperGuiOpenCount.computeIfPresent(data.getLocation(), (loc, count) -> {
            if (count == null || count <= 1) return null;
            return count - 1;
        });
    }
    
    public boolean isHopperPaused(SpawnerData data) {
        if (data == null || data.getLocation() == null) return false;
        Integer count = hopperGuiOpenCount.get(data.getLocation());
        return count != null && count > 0;
    }

    private long parseDelayToTicks(String raw) {
        if (raw == null || raw.isEmpty()) return 60L; raw = raw.trim().toLowerCase();
        try {
            if (raw.endsWith("s")) {
                long sec = Long.parseLong(raw.substring(0, raw.length()-1)); return Math.max(1, sec * 20L);
            } else if (raw.endsWith("m")) {
                long min = Long.parseLong(raw.substring(0, raw.length()-1)); return Math.max(1, min * 60L * 20L);
            } else if (raw.endsWith("t")) {
                long t = Long.parseLong(raw.substring(0, raw.length()-1)); return Math.max(1, t);
            } else { return Math.max(1, Long.parseLong(raw)); }
        } catch (NumberFormatException e) { return 60L; }
    }

    public SpawnerData getSpawner(Location loc) {
        return spawners.get(loc);
    }

    public void addSpawner(SpawnerData data) {
        spawners.put(data.getLocation(), data);
    }

    public void removeSpawner(Location loc) {
        spawners.remove(loc);
    }

    public Map<Location, SpawnerData> getSpawners() {
        return spawners;
    }

    public boolean isIsolated(Location loc) {
        if (loc == null || loc.getWorld() == null) return true;
        int radius = plugin.getSpawnerConfig().getInt("settings.isolation_radius", 5);
        double radiusSq = (double) radius * (double) radius;
        for (Location other : spawners.keySet()) {
            if (other == null || other.getWorld() == null) continue;
            if (other.equals(loc)) continue;
            if (!other.getWorld().equals(loc.getWorld())) continue;
            if (other.distanceSquared(loc) <= radiusSq) {
            return false;
            }
        }
        return true;
    }

    public void onChunkLoad(Chunk chunk) {
    }

    public void onChunkUnload(Chunk chunk) {
    }

    public PrismSurvival getPlugin() {
        return plugin;
    }

    public long getXPAmount() {
        return plugin.getSpawnerConfig().getLong("xp.amount_per_cycle", 5);
    }
}
