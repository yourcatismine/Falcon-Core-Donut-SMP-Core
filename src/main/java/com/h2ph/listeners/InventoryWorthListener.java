package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.managers.InventoryWorthManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryWorthListener implements Listener {

    private final Falcon plugin;

    public InventoryWorthListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getInventoryWorthManager().scheduleActivation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getInventoryWorthManager().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        plugin.getInventoryWorthManager().stripWorthLore(player);

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;

            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                    player.updateInventory();
                }
            } else {
                player.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        plugin.getInventoryWorthManager().stripWorthLore(player);

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;

            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                    player.updateInventory();
                }
            } else {
                player.updateInventory();
            }
        }, 1L);
    }



    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                    player.updateInventory();
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        org.bukkit.inventory.Inventory top = event.getInventory();
        if (plugin.getInventoryWorthManager().isStandardContainer(top)) {
            if (event.getViewers().size() <= 1) {
                for (int i = 0; i < top.getSize(); i++) {
                    ItemStack item = top.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        ItemStack clone = item.clone();
                        if (plugin.getInventoryWorthManager().stripFromItem(clone)) {
                            top.setItem(i, clone);
                        }
                    }
                }
            }
        }

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;
            if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                player.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        plugin.getInventoryWorthManager().stripFromItem(event.getItemDrop().getItemStack());
        
        plugin.getInventoryWorthManager().stripWorthLore(player);
        
        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;
            if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                player.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;

        for (ItemStack drop : event.getDrops()) {
            plugin.getInventoryWorthManager().stripFromItem(drop);
        }
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getInventoryWorthManager().isActive(player)) return;
        
        plugin.getInventoryWorthManager().stripWorthLore(player);

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;
            if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                player.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getNewGameMode() == org.bukkit.GameMode.CREATIVE) {
            plugin.getInventoryWorthManager().stripWorthLore(player);
        } else if (player.getGameMode() == org.bukkit.GameMode.CREATIVE && event.getNewGameMode() != org.bukkit.GameMode.CREATIVE) {
            plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
                if (player.isOnline() && plugin.getInventoryWorthManager().isActive(player)) {
                    if (plugin.getInventoryWorthManager().applyWorthLore(player)) {
                        player.updateInventory();
                    }
                }
            }, 5L);
        }
    }
}
