package com.prismcore.survival.survival;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemMerger implements Listener {

    private final PrismSurvival plugin;
    private final double RADIUS = 5.0;
    private final int MAX_STACK = 64;

    public ItemMerger(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> taskRef = new java.util.concurrent.atomic.AtomicReference<>();

        // Run next tick to ensure item is fully in world and valid
        // Check merge every 5 seconds (100 ticks)
        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(item, new Runnable() {
            @Override
            public void run() {
                if (!item.isValid() || item.isDead()) {
                    org.bukkit.scheduler.BukkitTask t = taskRef.get();
                    if (t != null) {
                        t.cancel();
                    }
                    return;
                }

                // Scan nearby entities (Filtered scan is slightly faster on Paper/Folia)
                for (Entity entity : item.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
                    if (!(entity instanceof Item other)) {
                        continue;
                    }

                    if (!other.isValid() || other.isDead() || other.equals(item)) {
                        continue;
                    }

                    // Greedy merge: Try to pull FROM other INTO item
                    tryMerge(other, item);

                    // If we are full, stop looking
                    if (item.getItemStack().getAmount() >= MAX_STACK) {
                        break;
                    }
                }
            }
        }, 100L, 100L);
        taskRef.set(task);
    }

    private void tryMerge(Item source, Item target) {
        ItemStack sourceStack = source.getItemStack();
        ItemStack targetStack = target.getItemStack();

        // Check compatibility
        if (!sourceStack.isSimilar(targetStack)) {
            return;
        }

        int sourceAmount = sourceStack.getAmount();
        int targetAmount = targetStack.getAmount();

        // If target is already full, skip
        if (targetAmount >= MAX_STACK) {
            return;
        }

        int transfer = Math.min(sourceAmount, MAX_STACK - targetAmount);

        if (transfer > 0) {
            // Update target amount
            targetStack.setAmount(targetAmount + transfer);
            target.setItemStack(targetStack);

            // Update source amount
            if (sourceAmount - transfer <= 0) {
                source.remove();
            } else {
                sourceStack.setAmount(sourceAmount - transfer);
                source.setItemStack(sourceStack);
            }
        }
    }
}
