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

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                if (event.getClickedInventory() != null
                        && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                        if (event.getClickedInventory().getItem(13) != null && event.getClickedInventory().getItem(13)
                                .getType() == org.bukkit.Material.NETHERRACK) {
                            if (event.getSlot() == 11) {
                                com.h2ph.rtp.RTPManager.teleport(player, "overworld");
                            }
                            else if (event.getSlot() == 13) {
                                com.h2ph.rtp.RTPManager.teleport(player, "nether");
                            }
                            else if (event.getSlot() == 15) {
                                com.h2ph.rtp.RTPManager.teleport(player, "end");
                            }
                        }
                    }
                }
            }
        }
    }


}
