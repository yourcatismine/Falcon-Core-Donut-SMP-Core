package com.h2ph.listeners;

import com.h2ph.commands.player.SettingsCommand;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class SettingsGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(SettingsCommand.GUI_TITLE)) {
            // Cancel events in top inventory (GUI)
            if (e.getClickedInventory() == null)
                return;

            if (e.getClickedInventory() == e.getView().getTopInventory()) {
                e.setCancelled(true); // Always cancel clicks in the GUI

                if (!(e.getWhoClicked() instanceof Player))
                    return;
                Player p = (Player) e.getWhoClicked();
                ItemStack current = e.getCurrentItem();

                if (current == null || current.getType() == Material.AIR) {
                    // No sound for empty slots
                    return;
                }

                // Play Tripwire sound on click for non-empty slots
                p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);

                int slot = e.getSlot();

                // Slot 0: Hide Chat
                if (slot == 0) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isHideChat();
                        data.setHideChat(newState);
                        // Save immediately
                        plugin.getPlayerDataManager().savePlayer(p.getUniqueId());

                        // Update Item
                        String hideChatStatus = newState ? "&a&lON" : "&4&lOFF";
                        org.bukkit.inventory.meta.ItemMeta meta = current.getItemMeta();
                        java.util.List<String> lore = meta.getLore();
                        if (lore != null && !lore.isEmpty()) {
                            lore.set(0, org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&fCurrently: " + hideChatStatus));
                            meta.setLore(lore);
                            current.setItemMeta(meta);
                        }
                    }
                }

                // Slot 1: Private Messages
                if (slot == 1) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isPrivateMessages();
                        data.setPrivateMessages(newState);
                        plugin.getPlayerDataManager().savePlayer(p.getUniqueId());

                        // Update Item
                        String status = newState ? "&a&lON" : "&4&lOFF";
                        org.bukkit.inventory.meta.ItemMeta meta = current.getItemMeta();
                        java.util.List<String> lore = meta.getLore();
                        if (lore != null && !lore.isEmpty()) {
                            lore.set(0, org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&fCurrently: " + status));
                            meta.setLore(lore);
                            current.setItemMeta(meta);
                        }
                    }
                }
                // Allow bottom inventory events (dragging items in player inventory)
                // BUT prevent shift-clicking into the GUI
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                }
                // Allow other interactions in bottom inventory (move, drag, drop)
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(SettingsCommand.GUI_TITLE)) {
            // Check if any of the dragged slots are in the top inventory
            for (int slot : e.getRawSlots()) {
                if (slot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
