/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 */
package com.falconcore.survival.orders.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public interface MenuOwner {
    public void onClick(InventoryClickEvent var1);

    default public void onClose(InventoryCloseEvent e) {
    }

    default public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }
}
