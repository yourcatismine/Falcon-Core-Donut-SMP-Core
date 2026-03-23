package com.falconcore.survival.spawners.tasks;

import com.falconcore.survival.spawners.storage.SpawnerData;
import com.falconcore.survival.spawners.storage.SpawnerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HopperTransferTask extends BukkitRunnable {
    private final SpawnerManager manager;
    private final int stacksPerRun;
    private final boolean isFolia;

    public HopperTransferTask(SpawnerManager manager, int stacksPerRun) {
        this.manager = manager;
        this.stacksPerRun = Math.max(1, stacksPerRun);
        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
        } catch (ClassNotFoundException ignored) {}
        this.isFolia = foliaDetected;
    }

    @Override
    public void run() {
        if (!manager.getPlugin().getSpawnerConfig().getBoolean("hopper.enabled", false)) return;

        List<SpawnerData> snapshot = new ArrayList<>(manager.getSpawners().values());

        for (SpawnerData data : snapshot) {
            Location spawnerLoc = data.getLocation();
            if (spawnerLoc == null || spawnerLoc.getWorld() == null) continue;

            if (!spawnerLoc.getWorld().isChunkLoaded(spawnerLoc.getBlockX() >> 4, spawnerLoc.getBlockZ() >> 4)) {
                continue;
            }

            if (isFolia) {
                scheduleRegionTask(spawnerLoc, data);
            } else {
                processHopperTransfer(data, spawnerLoc);
            }
        }
    }

    private void scheduleRegionTask(Location loc, SpawnerData data) {
        try {
            Object regionScheduler = Bukkit.getServer().getClass()
                    .getMethod("getRegionScheduler")
                    .invoke(Bukkit.getServer());

            Method executeMethod = regionScheduler.getClass().getMethod(
                    "execute", org.bukkit.plugin.Plugin.class, Location.class, Runnable.class);

            executeMethod.invoke(regionScheduler, manager.getPlugin(), loc,
                    (Runnable) () -> processHopperTransfer(data, loc));
        } catch (Exception e) {
            manager.getPlugin().getLogger().severe("Failed to schedule hopper region task: " + e.getMessage());
        }
    }

    private void processHopperTransfer(SpawnerData data, Location spawnerLoc) {
        if (manager.isHopperPaused(data)) return;
        Block below = spawnerLoc.getBlock().getRelative(0, -1, 0);
        if (below == null || below.getType() != Material.HOPPER) return;

        org.bukkit.block.BlockState state = below.getState();
        if (!(state instanceof Hopper)) return;
        Inventory hopperInv = ((Hopper) state).getInventory();
        if (hopperInv == null) return;

        AtomicInteger transferredStacks = new AtomicInteger(0);

        Iterator<Map.Entry<Material, Long>> it = data.getAccumulatedDrops().entrySet().iterator();
        while (it.hasNext() && transferredStacks.get() < stacksPerRun) {
            Map.Entry<Material, Long> entry = it.next();
            Material mat = entry.getKey();
            long stored = entry.getValue();
            if (stored <= 0) {
                it.remove();
                continue;
            }

            while (stored > 0 && transferredStacks.get() < stacksPerRun) {
                int toTransfer = (int) Math.min(64, stored);
                ItemStack stack = new ItemStack(mat, toTransfer);
                Map<Integer, ItemStack> leftover = hopperInv.addItem(stack);
                int actuallyAdded = toTransfer;
                if (leftover != null && !leftover.isEmpty()) {
                    ItemStack rem = leftover.values().iterator().next();
                    actuallyAdded = toTransfer - rem.getAmount();
                }
                if (actuallyAdded <= 0) {
                    transferredStacks.set(stacksPerRun);
                    break;
                }

                stored -= actuallyAdded;
                transferredStacks.incrementAndGet();

                if (stored <= 0) {
                    it.remove();
                    break;
                } else {
                    entry.setValue(stored);
                }
            }
        }
    }
}