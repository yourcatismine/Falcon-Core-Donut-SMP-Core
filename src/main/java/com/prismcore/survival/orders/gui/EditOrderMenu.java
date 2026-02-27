/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.gui.CollectItemsMenu;
import com.prismcore.survival.orders.gui.DeleteOrderMenu;
import com.prismcore.survival.orders.gui.GuiVariant;
import com.prismcore.survival.orders.gui.MenuOwner;
import com.prismcore.survival.orders.gui.YourOrdersMenu;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class EditOrderMenu
        implements InventoryHolder,
        MenuOwner {
    private static final String META_SUPPRESS_CLOSE = "prismorder.suppressClose";
    private final OrdersModule module;
    private final Player p;
    private final Order order;
    private Inventory inv;

    public EditOrderMenu(OrdersModule module, Player p, Order order) {
        this.module = module;
        this.p = p;
        this.order = order;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        int rows = this.module.cfg().rows("edit", 3);
        this.inv = Bukkit.createInventory(this, rows * 9, Utils.formatColors("&8ᴏʀᴅᴇʀѕ -> ᴇᴅɪᴛ ᴏʀᴅᴇʀ"));

        // Filler
        int[] fillerSlots = new int[] { 0, 1, 2, 9, 11, 18, 19, 20 };
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(Utils.formatColors("&7 "));
            filler.setItemMeta(fm);
        }
        for (int s : fillerSlots) {
            if (s < this.inv.getSize())
                this.inv.setItem(s, filler);
        }

        // Slot 10: Target Item
        OfflinePlayer op = Bukkit.getOfflinePlayer(this.order.owner);
        String ownerName = op != null && op.getName() != null ? op.getName() : "Unknown";

        long now = System.currentTimeMillis();
        long expiryTime = this.order.creationTime + (7L * 24 * 60 * 60 * 1000);
        long deletionTime = this.order.creationTime + (37L * 24 * 60 * 60 * 1000);

        List<String> lore = new ArrayList<>();
        lore.add(Utils.formatColors("&f" + this.order.key.displayName()));
        List<String> enchantLore = this.order.key.enchantLoreLines("&7");
        if (!enchantLore.isEmpty()) {
            for (String line : enchantLore) {
                lore.add(Utils.formatColors(line));
            }
        }
        lore.add(Utils.formatColors("&a$" + Utils.abbr(this.order.priceEach) + "&f each"));
        lore.add("");
        lore.add(Utils.formatColors("&6" + Utils.abbr(this.order.delivered) + "/&a" + Utils.abbr(this.order.requested)
                + "&7 Delivered"));
        lore.add(Utils.formatColors("&6$" + Utils.abbr((double) this.order.delivered * this.order.priceEach) + "/&a$"
                + Utils.abbr(this.order.totalPrice()) + "&7 Paid"));
        lore.add("");

        if (now < expiryTime) {
            long remaining = Math.max(0, expiryTime - now);
            lore.add(Utils.formatColors("&7" + Utils.formatDuration(remaining) + " Untill Order expires"));
        } else {
            long remaining = Math.max(0, deletionTime - now);
            lore.add(Utils.formatColors("&4" + Utils.formatDuration(remaining) + " before it deletes"));
        }

        ItemStack target = new ItemStack(this.order.key.material);
        ItemMeta tm = target.getItemMeta();
        if (tm != null) {
            tm.setDisplayName(Utils.formatColors("&a" + ownerName + "'s Order"));
            tm.setLore(lore);
            tm.addItemFlags(ItemFlag.values());
            target.setItemMeta(tm);
        }
        target = GuiVariant.merge(target, this.order.key.buildIcon());
        this.inv.setItem(10, target);

        // Buttons
        boolean hasItemsToCollect = !this.order.storage.isEmpty();

        // Cancel (Slot 13)
        if (hasItemsToCollect) {
            this.inv.setItem(13,
                    makeButton(Material.BARRIER, "&cᴄᴀɴᴄᴇʟ",
                            List.of("&fCollect pending items first", "&7You cannot cancel while",
                                    "&7delivered items are waiting.")));
        } else {
            this.inv.setItem(13,
                    makeButton(Material.RED_TERRACOTTA, "&aᴄᴀɴᴄᴇʟ", List.of("&fClick to cancel/remove this order")));
        }

        // Collect (Slot 15) - Always
        this.inv.setItem(15, makeButton(Material.CHEST, "&aᴄᴏʟʟᴇᴄᴛ", List.of("&fClick to collect items")));

        this.p.openInventory(this.inv);
        // Sound removed as per request
    }

    private ItemStack makeButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            if (lore != null)
                meta.setLore(Utils.formatColors(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        // Handle clicks in the GUI itself
        if (e.getClickedInventory().getHolder() == this) {
            e.setCancelled(true);
        } else {
            // Player inventory click: block shift-clicking into the GUI
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }
        int slot = e.getSlot();

        boolean hasItemsToCollect = !this.order.storage.isEmpty();

        // Slot 13: Cancel
        if (slot == 13) {
            if (!hasItemsToCollect) {
                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Clicked Cancel in Edit Order Menu");
                this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
                this.p.setMetadata(META_SUPPRESS_CLOSE,
                        (MetadataValue) new FixedMetadataValue((Plugin) this.module.getPlugin(), (Object) true));
                new DeleteOrderMenu(this.module, this.p, this.order).open();
            } else {
                this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                this.p.sendMessage(Utils.formatColors("&cPlease collect your items before cancelling!"));
            }
            return;
        }

        // Slot 15: Collect
        if (slot == 15) {
            com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                    com.prismcore.survival.manager.ActivityLogger.LogType.ORDER, "Clicked Collect in Edit Order Menu");
            // Always open, even if empty, as per visual request (button is always there).
            // CollectItemsMenu should handle empty gracefully.
            this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
            this.p.setMetadata(META_SUPPRESS_CLOSE,
                    (MetadataValue) new FixedMetadataValue((Plugin) this.module.getPlugin(), (Object) true));
            new CollectItemsMenu(this.module, this.p, this.order).open();
            return;
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        if (this.p.hasMetadata(META_SUPPRESS_CLOSE)) {
            this.p.removeMetadata(META_SUPPRESS_CLOSE, (Plugin) this.module.getPlugin());
            return;
        }
        TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                () -> new YourOrdersMenu(this.module, this.p).open(),
                1L);
    }
}
