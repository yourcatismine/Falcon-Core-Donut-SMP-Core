/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.plugin.Plugin
 */
package com.falconcore.survival.orders.gui;

import java.util.List;
import com.falconcore.survival.orders.OrdersModule;
import com.falconcore.survival.orders.data.Order;
import com.falconcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

public class DeleteOrderMenu
        implements InventoryHolder,
        MenuOwner {
    private final OrdersModule module;
    private final Player p;
    private final Order order;
    private Inventory inv;
    private boolean processing = false;

    public DeleteOrderMenu(OrdersModule module, Player p, Order order) {
        this.module = module;
        this.p = p;
        this.order = order;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        int rows = this.module.cfg().rows("delete", 3);
        this.inv = Bukkit.createInventory((InventoryHolder) this, (int) (rows * 9),
                (String) this.module.cfg().title("delete", "&8ᴏʀᴅᴇʀѕ -> ᴅᴇʟᴇᴛᴇ ᴏʀᴅᴇʀ"));
        this.inv.setItem(this.module.cfg().slot("gui.delete.items.back", 10),
                this.module.cfg().button("gui.delete.items.back",
                        "RED_STAINED_GLASS_PANE", "&cʙᴀᴄᴋ", List.of("&fClick to go back")));
        this.inv.setItem(this.module.cfg().slot("gui.delete.items.confirm", 16),
                this.module.cfg().button("gui.delete.items.confirm", "LIME_STAINED_GLASS_PANE", "&aᴄᴏɴꜰɪʀᴍ",
                        List.of("&fClick to delete this order")));
        this.p.openInventory(this.inv);
        this.module.cfg().play(this.p, "sounds.open", "BLOCK_CHEST_OPEN", 0.7f, 1.0f);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getHolder() == this) {
            e.setCancelled(true);
        } else {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == org.bukkit.Material.AIR) {
            return;
        }
        int slot = e.getSlot();
        int back = this.module.cfg().slot("gui.delete.items.back", 10);
        int confirm = this.module.cfg().slot("gui.delete.items.confirm", 16);
        if (slot == back) {
            this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
            new EditOrderMenu(this.module, this.p, this.order).open();
            return;
        }
        if (slot == confirm) {
            if (this.processing) {
                return;
            }
            if (!this.order.storage.isEmpty()) {
                this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                this.p.sendMessage(com.falconcore.survival.orders.Utils
                        .formatColors("&cCannot delete: items were delivered just now! Please collect them first."));
                new EditOrderMenu(this.module, this.p, this.order).open();
                return;
            }

            this.processing = true;
            this.module.cfg().play(this.p, "sounds.confirm", "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.2f);
            this.module.orders().cancel(this.order);
            new YourOrdersMenu(this.module, this.p).open();
            return;
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        Player pp = (Player) e.getPlayer();
        TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) pp, () -> {
            InventoryHolder holder = pp.getOpenInventory().getTopInventory().getHolder();
            if (!(holder instanceof MenuOwner)) {
                new YourOrdersMenu(this.module, pp).open();
            }
        }, 1L);
    }
}
