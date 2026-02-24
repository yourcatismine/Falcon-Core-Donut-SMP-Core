package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.InvSeeGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InvSeeListener implements Listener {

    private final PrismSurvival plugin;

    public InvSeeListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof InvSeeGUI.InvSeeHolder holder))
            return;

        // Prevent clicking the separator slots (9-17)
        if (e.getRawSlot() >= 9 && e.getRawSlot() < 18) {
            e.setCancelled(true);
            return;
        }

        Player viewer = (Player) e.getWhoClicked();
        Player target = Bukkit.getPlayer(holder.getTargetUUID());

        if (target == null || !target.isOnline()) {
            viewer.closeInventory();
            return;
        }

        // We want to sync the click to the target's inventory
        // GUI Slots:
        // 0-3: Armor (Head, Chest, Legs, Boots)
        // 8: Offhand
        // 18-53: Main Inventory (0-35)

        target.getScheduler().execute(plugin, () -> {
            updateTargetInventory(e.getInventory(), target);
        }, null, 0);
    }

    private void updateTargetInventory(Inventory gui, Player target) {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = gui.getItem(0); // Head
        armor[2] = gui.getItem(1); // Chest
        armor[1] = gui.getItem(2); // Legs
        armor[0] = gui.getItem(3); // Boots
        target.getInventory().setArmorContents(armor);

        target.getInventory().setItemInOffHand(gui.getItem(8));

        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, gui.getItem(18 + i));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof InvSeeGUI.InvSeeHolder) {
            // Task cancellation is handled by the "error throwing" hack in InvSeeGUI for
            // now,
            // or I could store task IDs. Let's stick to the current plan or improve if
            // needed.
        }
    }
}
