/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.InventoryHolder
 */
package com.prismcore.survival.orders.gui;

import com.h2ph.PrismSurvival;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener
        implements Listener {

    private static final java.util.Map<java.util.UUID, Long> lastDeliveryClickTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DELIVERY_MIN_CLICK_INTERVAL = 100;

    private final PrismSurvival plugin;

    public MenuListener(PrismSurvival pl) {
        this.plugin = pl;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (!(inventoryHolder instanceof MenuOwner)) {
            return;
        }

        if (inventoryHolder instanceof com.prismcore.survival.orders.gui.ConfirmDeliveryMenu) {
            if (e.getWhoClicked() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) e.getWhoClicked();
                java.util.UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                Long lastTime = lastDeliveryClickTimes.get(playerId);

                if (lastTime != null && (currentTime - lastTime) < DELIVERY_MIN_CLICK_INTERVAL) {
                    e.setCancelled(true);
                    return;
                }

                lastDeliveryClickTimes.put(playerId, currentTime);
            }
        }

        MenuOwner owner = (MenuOwner) inventoryHolder;
        owner.onClick(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof MenuOwner) {
            MenuOwner owner = (MenuOwner) inventoryHolder;
            owner.onClose(e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (!(inventoryHolder instanceof MenuOwner)) {
            return;
        }
        MenuOwner owner = (MenuOwner) inventoryHolder;
        owner.onDrag(e);
    }
}
