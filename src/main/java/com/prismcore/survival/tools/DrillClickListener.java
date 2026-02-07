package com.prismcore.survival.tools;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DrillClickListener implements Listener {

    private final ToolsManager manager;

    public DrillClickListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        HumanEntity humanEntity = evt.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        manager.updatePlayerTools(player);
    }
}
