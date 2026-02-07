package com.h2ph.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class RTPListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(com.h2ph.commands.player.RTPCommand.GUI_TITLE)) {
            event.setCancelled(true);

            // Check if clicked item is not null/air and check if clicked inventory is the
            // Top Inventory (The GUI)
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                if (event.getClickedInventory() != null
                        && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                        // Navigation Logic
                        // Check if it's the Main GUI (Slot 13 is NETHERRACK)
                        if (event.getClickedInventory().getItem(13) != null && event.getClickedInventory().getItem(13)
                                .getType() == org.bukkit.Material.NETHERRACK) {
                            // Clicked Overworld (Slot 11) -> Open Sub-Menu
                            if (event.getSlot() == 11) {
                                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f,
                                        1f);
                                com.h2ph.commands.player.RTPCommand.openOverworldGUI(player);
                            }
                            // Clicked Nether (Slot 13) -> Teleport
                            else if (event.getSlot() == 13) {
                                com.h2ph.rtp.RTPManager.teleport(player, "europe", "nether");
                            }
                            // Clicked End (Slot 15) -> Teleport
                            else if (event.getSlot() == 15) {
                                com.h2ph.rtp.RTPManager.teleport(player, "europe", "end");
                            }
                        }
                        // Check if it's the Overworld Sub-Menu (Slot 10 is GRASS_BLOCK)
                        else if (event.getClickedInventory().getItem(10) != null
                                && event.getClickedInventory().getItem(10)
                                        .getType() == org.bukkit.Material.GRASS_BLOCK) {
                            if (event.getSlot() == 10) {
                                com.h2ph.rtp.RTPManager.teleport(player, "europe", "overworld");
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(com.h2ph.commands.player.RTPCommand.GUI_TITLE)) {
            // Check if it's the Overworld Sub-Menu (Slot 10 is GRASS_BLOCK)
            if (event.getInventory().getItem(10) != null
                    && event.getInventory().getItem(10).getType() == org.bukkit.Material.GRASS_BLOCK) {
                if (event.getPlayer() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getPlayer();

                    // Re-open Main Menu 1 tick later to avoid event conflicts
                    com.h2ph.PrismSurvival plugin = org.bukkit.plugin.java.JavaPlugin
                            .getPlugin(com.h2ph.PrismSurvival.class);
                    plugin.getSchedulerAdapter().runTaskLater(() -> {
                        if (com.h2ph.rtp.RTPManager.isTeleporting(player)
                                || com.h2ph.rtp.RTPManager.isOnCooldown(player)) {
                            return;
                        }
                        com.h2ph.commands.player.RTPCommand.openRTPGUI(player);
                    }, 1L);
                }
            }
        }
    }
}
