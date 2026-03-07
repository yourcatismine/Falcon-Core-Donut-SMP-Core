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

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

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
        String title = Utils.formatColors("&8ᴍᴀɴᴀɢᴇ: " + Utils.toSmallCaps(target.getName()));
        this.inv = Bukkit.createInventory(this, 54, title);

        List<Order> playerOrders = this.module.orders().all().stream()
                .filter(o -> o.owner.equals(target.getUniqueId()))
                .filter(o -> !o.canceled && !o.completed)
                .collect(Collectors.toList());

        int slot = 0;
        for (Order o : playerOrders) {
            if (slot >= 45)
                break;
            this.inv.setItem(slot++, createAdminOrderDisplayItem(o));
        }

        this.inv.setItem(47, makeItem(Material.BARRIER, "&cᴅᴇʟᴇᴛᴇ ᴀʟʟ ᴏʀᴅᴇʀѕ",
                List.of("&fClick to cancel and refund all", "&factive orders for this player.")));

        this.inv.setItem(48, makeItem(Material.MAP, "&5ʀᴇꜰʀᴇѕʜ", List.of("&fClick to refresh items")));

        this.inv.setItem(50, makeItem(Material.CHEST, "&aᴄᴏʟʟᴇᴄᴛ ᴀʟʟ ᴅʀᴏᴘѕ",
                List.of("&fClick to collect all delivered items", "&ffrom all of this player's orders.")));

        this.inv.setItem(51, makeItem(Material.DROPPER, "&aᴅʀᴏᴘ ᴀʟʟ ʟᴏᴏᴛ",
                List.of("&fClick to drop all delivered items", "&ffrom all of this player's orders on the ground.")));

        this.admin.openInventory(this.inv);
    }

    private ItemStack createAdminOrderDisplayItem(Order o) {
        List<String> lore = new ArrayList<>();
        lore.add(Utils.formatColors("&f" + o.key.displayName()));
        List<String> enchantLore = o.key.enchantLoreLines("&7");
        if (!enchantLore.isEmpty()) {
            for (String line : enchantLore) {
                lore.add(Utils.formatColors(line));
            }
        }
        lore.add(Utils.formatColors("&a$" + Utils.abbr(o.priceEach) + "&f each"));
        lore.add("");
        lore.add(Utils.formatColors("&6" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + "&7 Delivered"));
        lore.add("");
        lore.add(Utils.formatColors("&cClick to view"));

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

        if (slot == 47) {
            List<Order> playerOrders = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(target.getUniqueId()))
                    .filter(o -> !o.canceled && !o.completed)
                    .collect(Collectors.toList());

            for (Order o : playerOrders) {
                cancelAndRefund(o);
            }

            admin.sendMessage(Utils.formatColors("&aCancelled and refunded " + playerOrders.size() + " orders."));
            admin.playSound(admin.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
            open();
            return;
        }

        if (slot == 48) {
            admin.playSound(admin.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.0f);
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
                new AdminOrderLootMenu(this.module, this.admin, this.target, o).open();
                admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }

        if (slot == 50) {
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
                    // Resume amethyst tool timers when collecting items from orders
                    com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(item);
                    HashMap<Integer, ItemStack> leftovers = admin.getInventory().addItem(item);
                    if (!leftovers.isEmpty()) {
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
                admin.playSound(admin.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.0f);
            } else {
                admin.sendMessage(Utils.formatColors("&cNo items to collect."));
            }
            open();
            return;
        }

        if (slot == 51) {
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
                    // Resume amethyst tool timers when dropping items from orders
                    com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(item);
                    admin.getWorld().dropItem(loc, item);
                    droppedCount++;
                }
                this.module.orders().saveOrder(o);
            }

            if (droppedCount > 0) {
                admin.sendMessage(Utils.formatColors("&aDropped all loot on the ground."));
                admin.playSound(admin.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
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

    public void cancelAndRefund(Order o) {
        com.prismcore.survival.auction.AuctionController auction = com.h2ph.PrismSurvival.getInstance()
                .getAuctionController();
        if (auction != null) {
            synchronized (o.storage) {
                for (ItemStack item : o.storage) {
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    ItemMeta meta = item.getItemMeta();
                    if (meta == null)
                        continue;

                    String delivererUuidStr = meta.getPersistentDataContainer().get(OrdersModule.DELIVERER_KEY,
                            PersistentDataType.STRING);
                    String recipientName = meta.getPersistentDataContainer().get(OrdersModule.RECIPIENT_KEY,
                            PersistentDataType.STRING);

                    if (delivererUuidStr != null) {
                        try {
                            UUID delivererUuid = UUID.fromString(delivererUuidStr);
                            OfflinePlayer deliverer = Bukkit.getOfflinePlayer(delivererUuid);
                            String delivererName = deliverer.getName() != null ? deliverer.getName() : "Unknown";

                            if (recipientName != null) {
                                meta.getPersistentDataContainer().set(OrdersModule.REFUND_FROM_KEY,
                                        PersistentDataType.STRING,
                                        recipientName);
                                item.setItemMeta(meta);
                            }

                            // Resume amethyst tool timers when refunding items to auction
                            com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(item);

                            com.prismcore.survival.auction.AuctionItem ai = new com.prismcore.survival.auction.AuctionItem(
                                    UUID.randomUUID(),
                                    delivererName,
                                    item,
                                    0.0,
                                    0L,
                                    0
                            );
                            auction.getAuctionManager().addItem(ai);
                        } catch (Exception ex) {
                            module.getPlugin().getLogger().warning("Failed to refund item to " + delivererUuidStr);
                        }
                    }
                }
                o.storage.clear();
            }
        }

        this.module.orders().cancel(o);
    }
}
