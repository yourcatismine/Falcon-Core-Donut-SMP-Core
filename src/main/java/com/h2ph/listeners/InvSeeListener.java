package com.h2ph.listeners;

import com.h2ph.Falcon;
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

    private final Falcon plugin;

    public InvSeeListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof InvSeeGUI.InvSeeHolder holder))
            return;

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


        target.getScheduler().execute(plugin, () -> {
            updateTargetInventory(e.getInventory(), target);
        }, null, 0);
    }

    private void updateTargetInventory(Inventory gui, Player target) {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = gui.getItem(0);
        armor[2] = gui.getItem(1);
        armor[1] = gui.getItem(2);
        armor[0] = gui.getItem(3);
        target.getInventory().setArmorContents(armor);

        target.getInventory().setItemInOffHand(gui.getItem(8));

        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, gui.getItem(18 + i));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof InvSeeGUI.InvSeeHolder) {
        }
    }
}
