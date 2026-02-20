package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.EnderChestGUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class EnderChestGUIListener implements Listener {

    private final PrismSurvival plugin;

    public EnderChestGUIListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered when the player manually closes the GUI.
     * Plays the close sound and saves asynchronously.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof EnderChestGUI.EnderChestHolder))
            return;
        if (!(e.getPlayer() instanceof Player))
            return;

        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId();

        // Snapshot before the inventory is cleared
        ItemStack[] contents = e.getInventory().getContents().clone();

        // Close sound
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, 1f);

        // Save async — REPLACE INTO upserts the single row for this UUID
        plugin.getSchedulerAdapter().runTaskAsync(() -> plugin.getEnderChestManager().saveEnderChest(uuid, contents));
    }

    /**
     * Safety save on disconnect — ensures items are never lost on quit.
     * Saves synchronously so the data is committed before the player fully leaves.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Inventory openInv = player.getOpenInventory().getTopInventory();

        if (!(openInv.getHolder() instanceof EnderChestGUI.EnderChestHolder))
            return;

        UUID uuid = player.getUniqueId();
        ItemStack[] contents = openInv.getContents().clone();

        // Synchronous save on quit — the async save from InventoryCloseEvent may not
        // complete in time, so we save here directly on the main thread as a safety net
        plugin.getEnderChestManager().saveEnderChest(uuid, contents);
    }
}
