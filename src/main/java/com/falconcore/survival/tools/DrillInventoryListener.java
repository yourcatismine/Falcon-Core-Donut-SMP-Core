package com.falconcore.survival.tools;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class DrillInventoryListener implements Listener {

    private final ToolsManager manager;

    public DrillInventoryListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        manager.updatePlayerTools(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        manager.updatePlayerTools(event.getPlayer());
    }

    @EventHandler
    public void onHotbarScroll(PlayerItemHeldEvent event) {
        manager.updatePlayerTools(event.getPlayer());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        manager.updatePlayerTools(event.getPlayer());
    }
}
