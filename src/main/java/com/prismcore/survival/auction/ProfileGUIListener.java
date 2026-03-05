package com.prismcore.survival.auction;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ProfileGUIListener implements Listener {

    private final PrismSurvival plugin;

    public ProfileGUIListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Inventory topInv = event.getView().getTopInventory();
        boolean isProfileMain = topInv.getHolder() instanceof ProfileCommand.ProfileHolder;
        boolean isProfileHomes = topInv.getHolder() instanceof ProfileHomesGUI.ProfileHomesHolder;
        boolean isProfileInventory = topInv.getHolder() instanceof ProfileInventoryGUI.ProfileInventoryHolder;

        // Handle profile inventory live syncing
        if (isProfileInventory) {
            syncProfileInventory((ProfileInventoryGUI.ProfileInventoryHolder) topInv.getHolder(), topInv, player);
            return;
        }

        // Don't process profile inventory as buttons
        if (isProfileInventory)
            return;

        if (!isProfileMain && !isProfileHomes)
            return;

        // Prevent double-click collecting items
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null)
            return;

        if (clickedInv.equals(topInv)) {
            // Clicked in Top GUI -> handle as buttons
            event.setCancelled(true);

            // Play tripwire sound if clicking a valid item
            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() != Material.AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            }

            if (isProfileMain) {
                handleProfileMainClick(event, player);
            } else if (isProfileHomes) {
                handleProfileHomesClick(event, player);
            }
        } else {
            // Clicked in Player Inventory
            if (event.isShiftClick()) {
                event.setCancelled(true); // Prevent shift-clicking items into the GUI
            }
            // Allow other interactions (move items to inventory)
        }
    }

    private void syncProfileInventory(ProfileInventoryGUI.ProfileInventoryHolder holder, Inventory inv, Player viewer) {
        Player targetPlayer = Bukkit.getPlayer(holder.getTargetPlayerUUID());
        if (targetPlayer == null || !targetPlayer.isOnline())
            return;

        // Don't sync if the target player is viewing a custom GUI (prevent interference while they edit their inventory)
        InventoryType type = targetPlayer.getOpenInventory().getTopInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
            return;
        }

        // Schedule the sync with a 1-tick delay to ensure the click is processed first
        plugin.getSchedulerAdapter().runEntityTaskLater(viewer, () -> {
            // Double-check the target player is still viewing standard inventory
            InventoryType currentType = targetPlayer.getOpenInventory().getTopInventory().getType();
            if (currentType != InventoryType.CRAFTING && currentType != InventoryType.PLAYER) {
                return;
            }

            // Sync storage items (slots 0-26)
            ItemStack[] storageItems = new ItemStack[27];
            for (int i = 0; i < 27; i++) {
                storageItems[i] = inv.getItem(i);
            }
            targetPlayer.getInventory().setStorageContents(storageItems);
            
            // Sync armor (slots 27-30: boots, leggings, chestplate, helmet)
            ItemStack[] armorItems = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack item = inv.getItem(27 + i);
                armorItems[i] = item;
            }
            targetPlayer.getInventory().setArmorContents(armorItems);
            
            // Sync offhand (slot 31)
            ItemStack offhandItem = inv.getItem(31);
            targetPlayer.getInventory().setItemInOffHand(offhandItem);
            
            // Update the target player's inventory
            targetPlayer.updateInventory();
        }, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        boolean isProfileMain = topInv.getHolder() instanceof ProfileCommand.ProfileHolder;
        boolean isProfileHomes = topInv.getHolder() instanceof ProfileHomesGUI.ProfileHomesHolder;
        boolean isProfileInventory = topInv.getHolder() instanceof ProfileInventoryGUI.ProfileInventoryHolder;

        // Sync profile inventory live on drag
        if (isProfileInventory && event.getWhoClicked() instanceof Player player) {
            syncProfileInventory((ProfileInventoryGUI.ProfileInventoryHolder) topInv.getHolder(), topInv, player);
            return;
        }

        // Don't prevent drag for profile inventory
        if (isProfileInventory)
            return;

        if (!isProfileMain && !isProfileHomes)
            return;

        // Prevent dragging items into the top inventory
        int topSize = topInv.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleProfileMainClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();

        // Slot 11: Homes button
        if (slot == 11) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                HomeManager homeManager = plugin.getHomeManager();
                ProfileHomesGUI.open(player, targetPlayer, homeManager);
            }
        }
        
        // Slot 12: Enderchest button
        if (slot == 12) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                // Open target player's enderchest
                com.h2ph.managers.EnderChestManager enderChestManager = plugin.getEnderChestManager();
                String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";
                
                // Preload and open the enderchest asynchronously
                plugin.getSchedulerAdapter().runTaskAsync(() -> {
                    ItemStack[] contents = enderChestManager.loadEnderChest(targetPlayer.getUniqueId());
                    plugin.getSchedulerAdapter().runTask(() -> {
                        org.bukkit.inventory.Inventory inv = enderChestManager.getOrCreateInventory(
                                targetPlayer.getUniqueId(), 
                                targetName, 
                                null, 
                                contents);
                        player.openInventory(inv);
                    });
                });
            }
        }
        
        // Slot 13: Inventory button (online players only)
        if (slot == 13) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                // Check if player is online
                Player onlineTarget = targetPlayer.getPlayer();
                if (onlineTarget == null || !onlineTarget.isOnline()) {
                    // Player is offline - show error
                    player.closeInventory();
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&cThis feature is only for online player.")));
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } catch (Exception e) {
                        // Fallback
                    }
                    return;
                }
                
                // Player is online - open their inventory with custom title
                // Use a task with 1 tick delay to ensure the profile GUI is fully closed first
                plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
                    ProfileInventoryGUI.open(player, onlineTarget);
                }, 1L);
            }
        }
    }

    private void handleProfileHomesClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();

        // Slot 1: Back button
        if (slot == 1) {
            ProfileHomesGUI.ProfileHomesHolder holder = (ProfileHomesGUI.ProfileHomesHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                ProfileCommand.openProfileGUI(player, targetPlayer);
            }
            return;
        }

        // Slots 11-15: Home beds (click to teleport)
        if (slot >= ProfileHomesGUI.BED_START && slot < ProfileHomesGUI.BED_START + ProfileHomesGUI.HOME_COUNT) {
            int homeNumber = slot - ProfileHomesGUI.BED_START + 1;
            Material clickedMat = event.getCurrentItem() != null ? event.getCurrentItem().getType() : Material.AIR;

            if (clickedMat == Material.PURPLE_BED) {
                ProfileHomesGUI.ProfileHomesHolder holder = (ProfileHomesGUI.ProfileHomesHolder) event.getView().getTopInventory().getHolder();
                OfflinePlayer targetPlayer = holder.getTargetPlayer();

                if (targetPlayer != null) {
                    HomeManager homeManager = plugin.getHomeManager();
                    Location homeLoc = homeManager.getHomeLocation(targetPlayer.getUniqueId(), homeNumber);

                    if (homeLoc != null) {
                        player.closeInventory();
                        
                        // Send "Teleporting..." actionbar with sound
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&7Teleporting...")));
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        } catch (Exception e) {
                            // Fallback
                        }
                        
                        // Teleport without countdown using async for Folia compatibility
                        player.teleportAsync(homeLoc).thenRun(() -> {
                            // Send "Teleported." actionbar with sound
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&7Teleported.")));
                            try {
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                            } catch (Exception e) {
                                // Fallback
                            }
                        }).exceptionally(ex -> {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&#ff4444Failed to teleport.")));
                            return null;
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        Inventory inv = event.getInventory();
        
        // Check if this is a profile inventory view
        if (inv.getHolder() instanceof ProfileInventoryGUI.ProfileInventoryHolder holder) {
            // Sync the inventory changes back to the target player if they're still online
            Player targetPlayer = Bukkit.getPlayer(holder.getTargetPlayerUUID());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                // Sync storage items (slots 0-26)
                ItemStack[] storageItems = new ItemStack[27];
                for (int i = 0; i < 27; i++) {
                    storageItems[i] = inv.getItem(i);
                }
                targetPlayer.getInventory().setStorageContents(storageItems);
                
                // Sync armor (slots 27-30: boots, leggings, chestplate, helmet)
                ItemStack[] armorItems = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    ItemStack item = inv.getItem(27 + i);
                    armorItems[i] = item;
                }
                targetPlayer.getInventory().setArmorContents(armorItems);
                
                // Sync offhand (slot 31)
                ItemStack offhandItem = inv.getItem(31);
                targetPlayer.getInventory().setItemInOffHand(offhandItem);
                
                // Update the target player's inventory
                targetPlayer.updateInventory();
            }
        }
    }
}
