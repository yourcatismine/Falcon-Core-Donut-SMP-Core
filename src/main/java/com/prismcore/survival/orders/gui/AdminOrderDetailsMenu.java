package com.prismcore.survival.orders.gui;

import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.gui.MenuOwner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminOrderDetailsMenu implements InventoryHolder, MenuOwner {
    private final OrdersModule module;
    private final Player admin;
    private final OfflinePlayer target;
    private Inventory inv;

    public AdminOrderDetailsMenu(OrdersModule module, Player admin, OfflinePlayer target) {
        this.module = module;
        this.admin = admin;
        this.target = target;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        String title = Utils.formatColors("&8ᴍᴀɴᴀɢᴇ: &5" + target.getName());
        this.inv = Bukkit.createInventory(this, 54, title);

        List<Order> playerOrders = this.module.orders().all().stream()
                .filter(o -> o.owner.equals(target.getUniqueId()))
                .filter(o -> !o.canceled && !o.completed)
                .collect(Collectors.toList());

        // Fill orders (0-44)
        int slot = 0;
        for (Order o : playerOrders) {
            if (slot >= 45)
                break;
            this.inv.setItem(slot++, createAdminOrderDisplayItem(o));
        }

        // Delete All Button (48)
        this.inv.setItem(48, makeItem(Material.BARRIER, "&cᴅᴇʟᴇᴛᴇ ᴀʟʟ ᴏʀᴅᴇʀѕ",
                List.of("&fClick to cancel and refund all", "&factive orders for this player.")));

        // Refresh (49)
        this.inv.setItem(49, makeItem(Material.MAP, "&5ʀᴇꜰʀᴇѕʜ", List.of("&fClick to refresh items")));

        // Collect All Drops (50)
        this.inv.setItem(50, makeItem(Material.CHEST, "&aᴄᴏʟʟᴇᴄᴛ ᴀʟʟ ᴅʀᴏᴘѕ",
                List.of("&fClick to collect all delivered items", "&ffrom all of this player's orders.")));

        // Drop All Loot (51)
        this.inv.setItem(51, makeItem(Material.DROPPER, "&aᴅʀᴏᴘ ᴀʟʟ ʟᴏᴏᴛ",
                List.of("&fClick to drop all delivered items", "&ffrom all of this player's orders on the ground.")));

        this.admin.openInventory(this.inv);
    }

    private ItemStack createAdminOrderDisplayItem(Order o) {
        List<String> lore = new ArrayList<>();
        lore.add(Utils.formatColors("&f" + o.key.displayName()));
        lore.add(Utils.formatColors("&a$" + Utils.abbr(o.priceEach) + "&f each"));
        lore.add("");
        lore.add(Utils.formatColors("&6" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + "&7 Delivered"));
        lore.add("");
        lore.add(Utils.formatColors("&cClick to cancel/refund this order"));

        ItemStack item = new ItemStack(o.key.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors("&aOrder ID: " + o.id.toString().substring(0, 8)));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            meta.setLore(Utils.formatColors(lore));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getClickedInventory().getHolder() != this) {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }

        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
            return;

        int slot = e.getSlot();

        if (slot == 48) { // Delete All
            List<Order> playerOrders = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(target.getUniqueId()))
                    .filter(o -> !o.canceled && !o.completed)
                    .collect(Collectors.toList());

            for (Order o : playerOrders) {
                this.module.orders().cancel(o);
            }

            admin.sendMessage(Utils.formatColors("&aCancelled and refunded " + playerOrders.size() + " orders."));
            admin.playSound(admin.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
            open();
            return;
        }

        if (slot == 49) { // Refresh
            admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            open();
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<Order> playerOrders = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(target.getUniqueId()))
                    .filter(o -> !o.canceled && !o.completed)
                    .collect(Collectors.toList());

            if (slot < playerOrders.size()) {
                Order o = playerOrders.get(slot);
                this.module.orders().cancel(o);
                admin.sendMessage(Utils.formatColors("&aOrder cancelled and refunded."));
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                open();
            }
        }

        if (slot == 50) { // Collect All Drops
            List<Order> playerOrders = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(target.getUniqueId()))
                    .filter(o -> !o.canceled && !o.completed)
                    .collect(Collectors.toList());

            int collectedCount = 0;
            for (Order o : playerOrders) {
                if (o.storage.isEmpty())
                    continue;

                List<ItemStack> items = new ArrayList<>(o.storage);
                o.storage.clear();

                for (ItemStack item : items) {
                    HashMap<Integer, ItemStack> leftovers = admin.getInventory().addItem(item);
                    if (!leftovers.isEmpty()) {
                        // Inventory full, drop leftovers at feet
                        for (ItemStack leftover : leftovers.values()) {
                            admin.getWorld().dropItem(admin.getLocation(), leftover);
                        }
                    }
                    collectedCount++;
                }
                this.module.orders().saveOrder(o);
            }

            if (collectedCount > 0) {
                admin.sendMessage(Utils.formatColors("&aCollected items from " + collectedCount + " orders."));
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                admin.sendMessage(Utils.formatColors("&cNo items to collect."));
            }
            open();
            return;
        }

        if (slot == 51) { // Drop All Loot
            List<Order> playerOrders = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(target.getUniqueId()))
                    .filter(o -> !o.canceled && !o.completed)
                    .collect(Collectors.toList());

            int droppedCount = 0;
            Location loc = admin.getLocation();
            for (Order o : playerOrders) {
                if (o.storage.isEmpty())
                    continue;

                List<ItemStack> items = new ArrayList<>(o.storage);
                o.storage.clear();

                for (ItemStack item : items) {
                    admin.getWorld().dropItem(loc, item);
                    droppedCount++;
                }
                this.module.orders().saveOrder(o);
            }

            if (droppedCount > 0) {
                admin.sendMessage(Utils.formatColors("&aDropped all loot on the ground."));
                admin.playSound(admin.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
            } else {
                admin.sendMessage(Utils.formatColors("&cNo items to drop."));
            }
            open();
            return;
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
    }
}
