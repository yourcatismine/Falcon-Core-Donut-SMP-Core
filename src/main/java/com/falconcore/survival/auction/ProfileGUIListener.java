package com.falconcore.survival.auction;

import com.h2ph.Falcon;
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

    private final Falcon plugin;

    public ProfileGUIListener(Falcon plugin) {
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

        if (isProfileInventory) {
            syncProfileInventory((ProfileInventoryGUI.ProfileInventoryHolder) topInv.getHolder(), topInv, player);
            return;
        }

        if (isProfileInventory)
            return;

        if (!isProfileMain && !isProfileHomes)
            return;

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null)
            return;

        if (clickedInv.equals(topInv)) {
            event.setCancelled(true);

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
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    private void syncProfileInventory(ProfileInventoryGUI.ProfileInventoryHolder holder, Inventory inv, Player viewer) {
        Player targetPlayer = Bukkit.getPlayer(holder.getTargetPlayerUUID());
        if (targetPlayer == null || !targetPlayer.isOnline())
            return;

        InventoryType type = targetPlayer.getOpenInventory().getTopInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
            return;
        }

        plugin.getSchedulerAdapter().runEntityTaskLater(viewer, () -> {
            InventoryType currentType = targetPlayer.getOpenInventory().getTopInventory().getType();
            if (currentType != InventoryType.CRAFTING && currentType != InventoryType.PLAYER) {
                return;
            }

            ItemStack[] storageItems = new ItemStack[27];
            for (int i = 0; i < 27; i++) {
                storageItems[i] = inv.getItem(i);
            }
            targetPlayer.getInventory().setStorageContents(storageItems);
            
            ItemStack[] armorItems = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack item = inv.getItem(27 + i);
                armorItems[i] = item;
            }
            targetPlayer.getInventory().setArmorContents(armorItems);
            
            ItemStack offhandItem = inv.getItem(31);
            targetPlayer.getInventory().setItemInOffHand(offhandItem);
            
            targetPlayer.updateInventory();
        }, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        boolean isProfileMain = topInv.getHolder() instanceof ProfileCommand.ProfileHolder;
        boolean isProfileHomes = topInv.getHolder() instanceof ProfileHomesGUI.ProfileHomesHolder;
        boolean isProfileInventory = topInv.getHolder() instanceof ProfileInventoryGUI.ProfileInventoryHolder;

        if (isProfileInventory && event.getWhoClicked() instanceof Player player) {
            syncProfileInventory((ProfileInventoryGUI.ProfileInventoryHolder) topInv.getHolder(), topInv, player);
            return;
        }

        if (isProfileInventory)
            return;

        if (!isProfileMain && !isProfileHomes)
            return;

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

        if (slot == 11) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                HomeManager homeManager = plugin.getHomeManager();
                ProfileHomesGUI.open(player, targetPlayer, homeManager);
            }
        }
        
        if (slot == 12) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                com.h2ph.managers.EnderChestManager enderChestManager = plugin.getEnderChestManager();
                String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";
                
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
        
        if (slot == 13) {
            ProfileCommand.ProfileHolder holder = (ProfileCommand.ProfileHolder) event.getView().getTopInventory().getHolder();
            OfflinePlayer targetPlayer = holder.getTargetPlayer();

            if (targetPlayer != null) {
                Player onlineTarget = targetPlayer.getPlayer();
                if (onlineTarget == null || !onlineTarget.isOnline()) {
                    player.closeInventory();
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&cThis feature is only for online player.")));
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } catch (Exception e) {
                    }
                    return;
                }
                
                plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
                    ProfileInventoryGUI.open(player, onlineTarget);
                }, 1L);
            }
        }
    }

    private void handleProfileHomesClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();

        if ((slot >= ProfileHomesGUI.BED_START && slot < ProfileHomesGUI.BED_START + 5) ||
            (slot >= ProfileHomesGUI.BED_START_2 && slot < ProfileHomesGUI.BED_START_2 + 5)) {
            
            int homeNumber;
            if (slot >= ProfileHomesGUI.BED_START && slot < ProfileHomesGUI.BED_START + 5) {
                homeNumber = slot - ProfileHomesGUI.BED_START + 1;
            } else {
                homeNumber = slot - ProfileHomesGUI.BED_START_2 + 6;
            }
            Material clickedMat = event.getCurrentItem() != null ? event.getCurrentItem().getType() : Material.AIR;

            if (clickedMat == Material.PURPLE_BED) {
                ProfileHomesGUI.ProfileHomesHolder holder = (ProfileHomesGUI.ProfileHomesHolder) event.getView().getTopInventory().getHolder();
                OfflinePlayer targetPlayer = holder.getTargetPlayer();

                if (targetPlayer != null) {
                    HomeManager homeManager = plugin.getHomeManager();
                    Location homeLoc = homeManager.getHomeLocation(targetPlayer.getUniqueId(), homeNumber);

                    if (homeLoc != null) {
                        player.closeInventory();
                        
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&7Teleporting...")));
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        } catch (Exception e) {
                        }
                        
                        player.teleportAsync(homeLoc).thenRun(() -> {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Utils.formatColors("&7Teleported.")));
                            try {
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                            } catch (Exception e) {
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
        
        if (inv.getHolder() instanceof ProfileInventoryGUI.ProfileInventoryHolder holder) {
            Player targetPlayer = Bukkit.getPlayer(holder.getTargetPlayerUUID());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                ItemStack[] storageItems = new ItemStack[27];
                for (int i = 0; i < 27; i++) {
                    storageItems[i] = inv.getItem(i);
                }
                targetPlayer.getInventory().setStorageContents(storageItems);
                
                ItemStack[] armorItems = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    ItemStack item = inv.getItem(27 + i);
                    armorItems[i] = item;
                }
                targetPlayer.getInventory().setArmorContents(armorItems);
                
                ItemStack offhandItem = inv.getItem(31);
                targetPlayer.getInventory().setItemInOffHand(offhandItem);
                
                targetPlayer.updateInventory();
            }
        }
    }
}
