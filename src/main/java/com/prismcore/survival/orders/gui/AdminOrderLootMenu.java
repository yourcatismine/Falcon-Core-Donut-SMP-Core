package com.prismcore.survival.orders.gui;

import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
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
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AdminOrderLootMenu implements InventoryHolder, MenuOwner {
    private static final String META_SUPPRESS_RETURN = "prismorder.suppressReturn";
    private final OrdersModule module;
    private final Player admin;
    private final OfflinePlayer target;
    private final Order order;
    private Inventory inv;
    private int page = 0;
    private boolean internalSwitch = false;

    public AdminOrderLootMenu(OrdersModule module, Player admin, OfflinePlayer target, Order order) {
        this.module = module;
        this.admin = admin;
        this.target = target;
        this.order = order;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        String title = Utils.formatColors("&8ʟᴏᴏᴛ: " + Utils.toSmallCaps(order.id.toString().substring(0, 8)));
        this.inv = Bukkit.createInventory(this, 54, title);

        synchronized (order.storage) {
            if (order.storage.isEmpty()) {
                this.inv.setItem(22, makeItem(Material.BARRIER, "&cɴᴏ ʟᴏᴏᴛ ᴛᴏ ᴄᴏʟʟᴇᴄᴛ",
                        List.of("&fThis order has no delivered items yet.")));
            } else {
                int start = page * 45;
                int end = Math.min(start + 45, order.storage.size());
                int slot = 0;

                for (int i = start; i < end; i++) {
                    ItemStack item = order.storage.get(i);
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    ItemStack display = item.clone();
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                        String delivererUuidStr = meta.getPersistentDataContainer().get(OrdersModule.DELIVERER_KEY,
                                PersistentDataType.STRING);
                        if (delivererUuidStr != null) {
                            try {
                                UUID delivererUuid = UUID.fromString(delivererUuidStr);
                                OfflinePlayer deliverer = Bukkit.getOfflinePlayer(delivererUuid);
                                String delivererName = deliverer.getName() != null ? deliverer.getName() : "Unknown";
                                lore.add(Utils.formatColors("&7Delivered by: &e" + delivererName));
                            } catch (Exception ignored) {
                            }
                        }

                        lore.add("");
                        lore.add(Utils.formatColors("&aClick to collect this item"));
                        meta.setLore(lore);
                        display.setItemMeta(meta);
                    }
                    this.inv.setItem(slot++, display);
                }
            }

            if (page > 0) {
                this.inv.setItem(45, makeItem(Material.ARROW, "&#A9833Dᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ", List.of("&fGo to page " + page)));
            }

            if ((page + 1) * 45 < order.storage.size()) {
                this.inv.setItem(53, makeItem(Material.ARROW, "&#A9833Dɴᴇхᴛ ᴘᴀɢᴇ", List.of("&fGo to page " + (page + 2))));
            }
        }

        this.inv.setItem(48, makeItem(Material.DROPPER, "&aᴅʀᴏᴘ ʟᴏᴏᴛ",
                List.of("&fClick to drop all delivered items", "&ffor this specific order on the ground.")));

        this.inv.setItem(49, makeItem(Material.TNT, "&cᴅᴇʟᴇᴛᴇ & ʀᴇꜰᴜɴᴅ ᴏʀᴅᴇʀ",
                List.of("&fClick to cancel this order and", "&frefund remaining items to AH.")));

        this.inv.setItem(50, makeItem(Material.MAP, "&dʀᴇꜰʀᴇѕʜ", List.of("&fClick to refresh items")));

        this.admin.openInventory(this.inv);
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

        if (slot >= 0 && slot < 45) {
            synchronized (order.storage) {
                int itemIndex = page * 45 + slot;

                if (itemIndex < order.storage.size()) {
                    ItemStack targetItem = order.storage.get(itemIndex);
                    if (targetItem != null && targetItem.getType() != Material.AIR) {
                        HashMap<Integer, ItemStack> leftovers = admin.getInventory()
                                .addItem(Utils.stripOrderMetadata(targetItem.clone()));
                        
                        com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(targetItem);
                        
                        order.storage.remove(itemIndex);

                        if (!leftovers.isEmpty()) {
                            for (ItemStack leftover : leftovers.values()) {
                                admin.getWorld().dropItem(admin.getLocation(), leftover);
                            }
                        }

                        this.module.orders().saveOrder(order);
                        admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        open();
                    }
                }
            }
            return;
        }

        if (slot == 45) {
            if (page > 0) {
                page--;
                this.internalSwitch = true;
                admin.playSound(admin.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                open();
            }
            return;
        }

        if (slot == 53) {
            if ((page + 1) * 45 < order.storage.size()) {
                page++;
                this.internalSwitch = true;
                admin.playSound(admin.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                open();
            }
            return;
        }

        if (slot == 48) {
            synchronized (order.storage) {
                if (order.storage.isEmpty()) {
                    admin.sendMessage(Utils.formatColors("&cNo loot to collect."));
                    admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    return;
                }

                int start = page * 45;
                if (start >= order.storage.size()) {
                    page = Math.max(0, (order.storage.size() - 1) / 45);
                    open();
                    return;
                }

                int end = Math.min(start + 45, order.storage.size());
                int droppedCount = 0;

                List<ItemStack> toDrop = new ArrayList<>();
                for (int i = end - 1; i >= start; i--) {
                    ItemStack item = order.storage.remove(i);
                    if (item != null && item.getType() != Material.AIR) {
                        com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(item);
                        toDrop.add(item);
                        droppedCount++;
                    }
                }

                for (ItemStack item : toDrop) {
                    admin.getWorld().dropItem(admin.getLocation(), item);
                }

                this.module.orders().saveOrder(order);
                admin.sendMessage(Utils.formatColors("&aDropped " + droppedCount + " items from this page."));
                admin.playSound(admin.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

                if (page > 0 && page * 45 >= order.storage.size()) {
                    page--;
                }
                open();
            }
            return;
        }

        if (slot == 49) {
            this.admin.setMetadata(META_SUPPRESS_RETURN,
                    new org.bukkit.metadata.FixedMetadataValue(this.module.getPlugin(), true));
            AdminOrderDetailsMenu detailsMenu = new AdminOrderDetailsMenu(this.module, this.admin, this.target);
            detailsMenu.cancelAndRefund(order);
            admin.sendMessage(Utils.formatColors("&aOrder cancelled and refunded."));
            admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            detailsMenu.open();
            return;
        }

        if (slot == 50) {
            this.internalSwitch = true;
            admin.playSound(admin.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.0f);
            open();
            return;
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this)
            return;
        if (this.internalSwitch) {
            this.internalSwitch = false;
            return;
        }
        if (this.admin.hasMetadata(META_SUPPRESS_RETURN)) {
            this.admin.removeMetadata(META_SUPPRESS_RETURN, this.module.getPlugin());
            return;
        }

        com.prismcore.survival.orders.util.TaskUtil.runEntityLater(this.module.getPlugin(), this.admin,
                () -> new AdminOrderDetailsMenu(this.module, this.admin, this.target).open(), 1L);
    }
}
