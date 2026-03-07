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
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class ConfirmDeliveryMenu
        implements InventoryHolder,
        MenuOwner {
    
    private static final java.util.Set<java.util.UUID> processingOrders = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    private final OrdersModule module;
    private final Player p;
    private final Order order;
    private final List<ItemStack> acceptedDirect;
    private final List<ItemStack> acceptedFromShulkers;
    private final List<ItemStack> processedShulkers;
    private final List<ItemStack> originalShulkers;
    private final int acceptedAmount;
    private Inventory inv;
    private boolean finalized = false;

    public ConfirmDeliveryMenu(OrdersModule module, Player p, Order order,
            List<ItemStack> acceptedDirect, List<ItemStack> acceptedFromShulkers,
            List<ItemStack> processedShulkers, List<ItemStack> originalShulkers, int acceptedAmount) {
        this.module = module;
        this.p = p;
        this.order = order;
        this.acceptedDirect = acceptedDirect;
        this.acceptedFromShulkers = acceptedFromShulkers;
        this.processedShulkers = processedShulkers;
        this.originalShulkers = originalShulkers;
        this.acceptedAmount = acceptedAmount;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        int rows = this.module.cfg().rows("confirm", 3);
        this.inv = Bukkit.createInventory((InventoryHolder) this, (int) (rows * 9),
                (String) this.module.cfg().title("confirm", "&8ᴏʀᴅᴇʀѕ -> ᴄᴏɴꜰɪʀᴍ ᴅᴇʟɪᴠᴇʀʏ"));
        int cancelSlot = 11;
        int summarySlot = 13;
        int confirmSlot = 15;

        this.inv.setItem(summarySlot, createOrderItem(this.order));

        this.inv.setItem(cancelSlot, this.module.cfg().button("gui.confirm.items.cancel", "RED_STAINED_GLASS_PANE",
                "&4ᴄᴀɴᴄᴇʟ", List.of("&fClick to cancel")));

        this.inv.setItem(confirmSlot, this.module.cfg().dynamicItem(Material.LIME_STAINED_GLASS_PANE,
                "gui.confirm.items.confirm", "&aᴄᴏɴꜰɪʀᴍ", List.of("&fClick to deliver"), null));

        this.p.openInventory(this.inv);
        this.module.cfg().play(this.p, "sounds.open", "BLOCK_CHEST_OPEN", 0.7f, 1.0f);

        if (!this.p.hasMetadata("prismorder.deliveryStartTime")) {
            this.p.setMetadata("prismorder.deliveryStartTime",
                    new FixedMetadataValue(this.module.getPlugin(), System.currentTimeMillis()));
        }
    }

    private ItemStack createOrderItem(Order o) {
        List<String> lore = new ArrayList<>();
        lore.add(Utils.formatColors("&a" + Utils.abbr(o.requested) + " &f" + o.key.displayName()));
        List<String> enchantLore = o.key.enchantLoreLines("&7");
        if (!enchantLore.isEmpty()) {
            for (String line : enchantLore) {
                lore.add(Utils.formatColors(line));
            }
        }
        lore.add(Utils.formatColors("&a$" + Utils.abbr(o.priceEach) + " &feach"));
        lore.add("");
        lore.add(Utils.formatColors("&6" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + " &7Delivered"));
        lore.add(Utils.formatColors("&6$" + Utils.abbr(o.paid) + "/&a" + Utils.abbr(o.totalPrice()) + " &7Paid"));
        lore.add("");

        long created = o.creationTime;
        if (created == 0)
            created = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long expiryTime = o.creationTime + (7L * 24 * 60 * 60 * 1000);
        long deletionTime = o.creationTime + (37L * 24 * 60 * 60 * 1000);

        if (now < expiryTime) {
            long remaining = Math.max(0, expiryTime - now);
            lore.add(Utils.formatColors("&7" + Utils.formatDuration(remaining) + " Untill Order expires"));
        } else {
            long remaining = Math.max(0, deletionTime - now);
            lore.add(Utils.formatColors("&4" + Utils.formatDuration(remaining) + " before it deletes"));
        }

        ItemStack item = new ItemStack(o.key.material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String ownerName = Bukkit.getOfflinePlayer(o.owner).getName();
            if (ownerName == null)
                ownerName = "Unknown";

            meta.setDisplayName(Utils.formatColors("&a" + ownerName + "'s Order"));
            meta.setLore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            item.setItemMeta(meta);
        }
        return GuiVariant.merge(item, o.key.buildIcon());
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
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        int slot = e.getSlot();
        if (slot == 11) {
            com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                    com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                    "Clicked Cancel in Confirm Delivery Menu");
            this.finalized = true;

            for (ItemStack shulker : this.originalShulkers) {
                // Resume amethyst tool timers when cancelling delivery
                com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(shulker);
                this.giveBackOrDrop(shulker);
            }

            for (ItemStack directItem : this.acceptedDirect) {
                // Resume amethyst tool timers when cancelling delivery
                com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(directItem);
                this.giveBackOrDrop(directItem);
            }

            TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                    () -> new DeliverItemsMenu(this.module, this.p, this.order).open(), 1L);
            return;
        }
        if (slot == 15) {
            if (!processingOrders.add(this.order.id)) {
                this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                this.p.sendMessage(Utils.formatColors("&cDelivery already in progress! Please wait."));
                return;
            }
            
            try {
                this.finalized = true;

                Order freshOrder = this.module.orders().getOrder(this.order.id);
                if (freshOrder == null || freshOrder.canceled || freshOrder.completed) {
                    this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                    this.p.sendMessage(Utils.formatColors("&cThis order is no longer active!"));

                    for (ItemStack shulker : this.originalShulkers) {
                        this.giveBackOrDrop(shulker);
                    }
                    for (ItemStack directItem : this.acceptedDirect) {
                        this.giveBackOrDrop(directItem);
                    }

                    this.p.closeInventory();
                    return;
                }

                this.module.cfg().play(this.p, "sounds.confirm", "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.2f);

                List<ItemStack> allAccepted = new ArrayList<>();
                allAccepted.addAll(this.acceptedDirect);
                allAccepted.addAll(this.acceptedFromShulkers);

                try {
                    this.module.orders().applyDelivery(this.order, allAccepted, this.acceptedAmount, this.p.getUniqueId());
                } catch (IllegalStateException ex) {
                    this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                    this.p.sendMessage(Utils.formatColors("&cDelivery failed: " + ex.getMessage()));
                    
                    for (ItemStack shulker : this.originalShulkers) {
                        this.giveBackOrDrop(shulker);
                    }
                    for (ItemStack directItem : this.acceptedDirect) {
                        this.giveBackOrDrop(directItem);
                    }
                    
                    this.p.closeInventory();
                    return;
                }

                for (ItemStack processedShulker : this.processedShulkers) {
                    // Resume amethyst tool timers when returning processed shulkers
                    com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(processedShulker);
                    this.giveBackOrDrop(processedShulker);
                }

                long startTime = this.p.hasMetadata("prismorder.deliveryStartTime")
                        ? this.p.getMetadata("prismorder.deliveryStartTime").get(0).asLong()
                        : System.currentTimeMillis();
                double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Clicked Confirm and delivered " + acceptedAmount + " items in " + String.format("%.1f", seconds)
                                + "s");
                this.p.removeMetadata("prismorder.deliveryStartTime", this.module.getPlugin());

                Order finalOrder = this.module.orders().getOrder(this.order.id);
                if (finalOrder != null) {
                    this.order.delivered = finalOrder.delivered;
                    this.order.completed = finalOrder.completed;
                }

                if (this.order.remainingAmount() > 0) {
                    TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                            () -> new DeliverItemsMenu(this.module, this.p, this.order).open(), 1L);
                } else {
                    TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                            () -> new OrdersMainMenu(this.module, this.p).open(), 1L);
                }
            } finally {
                processingOrders.remove(this.order.id);
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        if (!this.finalized) {
            for (ItemStack shulker : this.originalShulkers) {
                // Resume amethyst tool timers when returning original shulkers
                com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(shulker);
                this.giveBackOrDrop(shulker);
            }
            for (ItemStack directItem : this.acceptedDirect) {
                // Resume amethyst tool timers when returning direct items
                com.prismcore.survival.tools.ToolsManager.getInstance().resumeOrdersTimers(directItem);
                this.giveBackOrDrop(directItem);
            }
            TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                    () -> new OrdersMainMenu(this.module, this.p).open(),
                    1L);
        }
    }

    private void giveBackOrDrop(ItemStack is) {
        HashMap<Integer, ItemStack> leftovers = this.p.getInventory().addItem(new ItemStack[] { is });
        leftovers.values().forEach(rem -> this.p.getWorld().dropItemNaturally(this.p.getLocation(), rem));
    }
}
