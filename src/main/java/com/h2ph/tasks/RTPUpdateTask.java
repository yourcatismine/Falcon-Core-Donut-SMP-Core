package com.h2ph.tasks;

import com.h2ph.commands.player.RTPCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class RTPUpdateTask implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equals(RTPCommand.GUI_TITLE)) {
                Inventory topInventory = player.getOpenInventory().getTopInventory();

                // Distinguish between Main GUI and Sub-Menus based on contents
                if (topInventory.getItem(13) != null
                        && topInventory.getItem(13).getType() == org.bukkit.Material.NETHERRACK) {
                    // Main GUI
                    RTPCommand.updateItems(topInventory, player);
                } else if (topInventory.getItem(10) != null
                        && topInventory.getItem(10).getType() == org.bukkit.Material.GRASS_BLOCK) {
                    // Overworld Sub-Menu
                    RTPCommand.updateOverworldItems(topInventory, player);
                }
            }
        }
    }
}
