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
        if (e.getView().getTopInventory().getHolder() instanceof SettingsCommand.SettingsHolder) {
            // Cancel events in top inventory (GUI)
            if (e.getClickedInventory() == null)
                return;

            if (e.getClickedInventory().equals(e.getView().getTopInventory())) {
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

                // Slot 2: Pay Alerts
                if (slot == 2) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isPayAlerts();
                        data.setPayAlerts(newState);
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

                // Slot 3: Quick Auction Buy
                if (slot == 3) {
                    if (!p.hasPermission("prismsmp.quick.auction")) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&cYou do not have permission to use Quick Auction Buy. &5PrismPlus &conly!"));
                        return;
                    }

                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isQuickAuctionBuy();
                        data.setQuickAuctionBuy(newState);
                        plugin.getPlayerDataManager().savePlayer(p.getUniqueId());

                        // Update Item
                        String status = newState ? "&a&lON" : "&4&lOFF";
                        org.bukkit.inventory.meta.ItemMeta meta = current.getItemMeta();
                        java.util.List<String> lore = new java.util.ArrayList<>();
                        // We need to re-build lore carefully since it has the "Access Only" text if no
                        // perm, but here we Checked perm already.
                        // Wait, if they have perm, the lore is just "Currently: STATUS".
                        // If they don't, we return early above.
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&fCurrently: " + status));
                        if (lore.size() > 1) { // Preserve extra lines/spacers if any?
                            // Actually SettingsCommand only adds extra lines if !hasPerm.
                            // So here we just set the one line.
                        }
                        meta.setLore(lore);
                        current.setItemMeta(meta);
                    }
                }
                // Slot 4: Disable Mob Spawns
                if (slot == 4) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isDisableMobSpawns();
                        data.setDisableMobSpawns(newState);
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
                // Slot 5: Sound Notifications
                if (slot == 5) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isSoundNotifications();
                        data.setSoundNotifications(newState);
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
                // Slot 6: TPA Confirm Menus
                if (slot == 6) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isTpaConfirmMenus();
                        data.setTpaConfirmMenus(newState);
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
                // Slot 7: Duel Requests
                if (slot == 7) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isDuelRequests();
                        data.setDuelRequests(newState);
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

                // Slot 8: TPA Requests
                if (slot == 8) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isTpaRequests();
                        data.setTpaRequests(newState);
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

                // Slot 9: TPA Here Requests
                if (slot == 9) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isTpaHereRequests();
                        data.setTpaHereRequests(newState);
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

                // Slot 10: Payments
                if (slot == 10) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isPayments();
                        data.setPayments(newState);
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

                // Slot 11: Shards Notifier
                if (slot == 11) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isShardsNotifier();
                        data.setShardsNotifier(newState);
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

                // Slot 12: Scoreboard
                if (slot == 12) {
                    com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
                    com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    if (data != null) {
                        boolean newState = !data.isShowScoreboard();
                        data.setShowScoreboard(newState);
                        plugin.getPlayerDataManager().savePlayer(p.getUniqueId());

                        // Update Scoreboard Visibilty
                        if (newState) {
                            plugin.getScoreboardManager().reloadScoreboard(p);
                        } else {
                            plugin.getScoreboardManager().removeScoreboard(p);
                        }

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
            } else {
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                }
                // Allow other interactions in bottom inventory (move, drag, drop)
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof SettingsCommand.SettingsHolder) {
            // Check if any of the dragged slots are in the top inventory
            for (int slot : e.getRawSlots()) {
                if (slot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
            // Double check drag into top inventory logic - if drag involves top inventory,
            // block it?
            // Actually raw slots < size handles it.
        }
    }
}
