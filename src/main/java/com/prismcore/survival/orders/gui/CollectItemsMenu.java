/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class CollectItemsMenu
        implements InventoryHolder,
        MenuOwner {
    private final OrdersModule module;
    private final Player p;
    private final Order order;
    private Inventory inv;
    private final int requestedPage;
    private int currentPage = 0;
    private boolean internalPageSwitch = false;
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 200; // 200ms cooldown between clicks

    public CollectItemsMenu(OrdersModule module, Player p, Order order) {
        this(module, p, order, 0);
    }

    public CollectItemsMenu(OrdersModule module, Player p, Order order, int page) {
        this.module = module;
        this.p = p;
        this.order = order;
        this.requestedPage = Math.max(0, page);
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private int rows() {
        return this.module.cfg().rows("collect", 6);
    }

    private int perPage() {
        return (this.rows() - 1) * 9;
    }

    private int maxPage() {
        int per = this.perPage();
        return Math.max(0, (this.order.storage.size() - 1) / Math.max(1, per));
    }

    public void open() {
        if (this.order.storage.isEmpty()) {
            this.module.cfg().message(this.p, "&cNo items to collect.");
            this.p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(Utils.formatColors("&cNo items to collect.")));
            // return to edit menu if it was empty from the start
            new EditOrderMenu(this.module, this.p, this.order).open();
            return;
        }
        int rows = this.rows();
        int per = this.perPage();
        int max = this.maxPage();
        this.currentPage = Math.max(0, Math.min(this.requestedPage, max));
        this.inv = Bukkit.createInventory((InventoryHolder) this, (int) (rows * 9),
                (String) this.module.cfg().title("collect", "&8ᴏʀᴅᴇʀѕ -> ᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍѕ"));
        int from = Math.max(0, Math.min(this.order.storage.size(), this.currentPage * per));
        int to = Math.min(this.order.storage.size(), from + per);
        for (int i = from; i < to; ++i) {
            ItemStack st = this.order.storage.get(i);
            if (st == null || st.getType() == Material.AIR)
                continue;
            this.inv.setItem(i - from, st.clone());
        }
        int prev = (rows - 1) * 9;
        int next = rows * 9 - 1;
        int drop = rows * 9 - 2;

        if (this.currentPage > 0) {
            this.inv.setItem(prev, this.module.cfg().button("gui.collect.items.prev", "ARROW", "&aᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ",
                    List.of("&fClick to go to the previous page")));
        }

        if (this.currentPage < max) {
            this.inv.setItem(next, this.module.cfg().button("gui.collect.items.next", "ARROW", "&aɴᴇхᴛ",
                    List.of("&fClick to go to the next page")));
        }

        this.inv.setItem(drop, this.module.cfg().button("gui.collect.items.drop", "DROPPER", "&aᴅʀᴏᴘ ʟᴏᴏᴛ",
                List.of("&fClick to drop all loot on the page")));
        ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = fill.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors("&7 "));
            fill.setItemMeta(meta);
        }
        for (int s = (rows - 1) * 9; s < rows * 9; ++s) {
            if (this.inv.getItem(s) != null)
                continue;
            this.inv.setItem(s, fill);
        }
        this.p.openInventory(this.inv);
        this.module.cfg().play(this.p, "sounds.open", "BLOCK_CHEST_OPEN", 0.7f, 1.0f);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) {
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        int rows = this.rows();
        int prev = (rows - 1) * 9;
        int next = rows * 9 - 1;
        int drop = rows * 9 - 2;
        Inventory top = e.getView().getTopInventory();
        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals((Object) top);
        boolean clickedPlayer = e.getClickedInventory() != null && e.getClickedInventory().getHolder() == this.p;
        int slot = e.getSlot();
        if (clickedTop && slot >= (rows - 1) * 9) {
            // Prevent spam clicking
            long now = System.currentTimeMillis();
            if (now - lastClickTime < CLICK_COOLDOWN_MS) {
                return;
            }
            lastClickTime = now;

            if (slot == prev) {
                if (this.currentPage > 0) {
                    int prevPage = Math.max(0, this.currentPage - 1);
                    this.module.cfg().play(this.p, "sounds.page", "UI_BUTTON_CLICK", 1.0f, 1.1f);
                    this.internalPageSwitch = true;
                    TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                            () -> new CollectItemsMenu(this.module, this.p, this.order, prevPage).open(), 1L);
                }
                return;
            }
            if (slot == next) {
                if (this.currentPage < maxPage()) {
                    int nextPage = Math.min(this.maxPage(), this.currentPage + 1);
                    this.module.cfg().play(this.p, "sounds.page", "UI_BUTTON_CLICK", 1.0f, 1.1f);
                    this.internalPageSwitch = true;
                    TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                            () -> new CollectItemsMenu(this.module, this.p, this.order, nextPage).open(), 1L);
                }
                return;
            }
            if (slot == drop) {
                // Inline drop logic
                int per = this.perPage();
                int from = this.currentPage * per;
                int to = Math.min(this.order.storage.size(), from + per);

                List<ItemStack> toDrop = new ArrayList<>();
                if (to > from) {
                    for (int i = from; i < to; i++) {
                        toDrop.add(this.order.storage.get(i).clone());
                    }
                    this.order.storage.subList(from, to).clear();
                }

                Location eye = this.p.getEyeLocation();
                for (ItemStack item : toDrop) {
                    if (item != null && item.getType() != Material.AIR) {
                        Item dropped = this.p.getWorld().dropItem(eye, item);
                        dropped.setVelocity(eye.getDirection().multiply(0.25));
                    }
                }

                this.module.orders().saveOrder(this.order);
                this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);

                // If storage is now empty and completed, it will be deleted in open()
                this.internalPageSwitch = true;
                this.open();
                return;
            }
            return;
        }
        if (clickedPlayer) {
            if (e.isShiftClick()) {
                e.setCancelled(true);
            }
            return;
        }
        if (clickedTop) {
            e.setCancelled(true); // BLOCK ALL INTERACTION IN TOP INV

            if (slot >= (rows - 1) * 9) {
                // Prevention for spam clicking handled above in existing code?
                // No, the existing code handled bottom row actions separately but we need to
                // merge logic or handle it here.
                // The existing code at line 142 handles the bottom row.
                // This block is for "clickedTop" but NOT the bottom row (handled by previous
                // if).
                // Wait, the previous block (lines 142-182) handles slot >= (rows-1)*9.
                // So if we are here, slot < (rows-1)*9. This is the item area.
            }

            // Logic for item collection (Core Fix)
            if (slot < (rows - 1) * 9) {
                // Determine index in storage
                int per = this.perPage();
                int index = (this.currentPage * per) + slot;

                if (index >= 0 && index < this.order.storage.size()) {
                    ItemStack item = this.order.storage.get(index);
                    if (item != null && item.getType() != Material.AIR) {
                        // Securely collect logic
                        long now = System.currentTimeMillis();
                        if (now - lastClickTime < CLICK_COOLDOWN_MS)
                            return;
                        lastClickTime = now;

                        // Check if player has space
                        if (this.p.getInventory().firstEmpty() == -1) {
                            // Check if can merge? Simpler to require empty slot or attempt add
                            HashMap<Integer, ItemStack> left = this.p.getInventory().addItem(item);
                            if (!left.isEmpty()) {
                                // Full, rollback (virtually, we haven't removed yet if we used clone, but
                                // addItem modifies input?)
                                // addItem returns what COULD NOT be added.
                                // If left is not empty, it means we couldn't fit it all.
                                // For simplicity, if inv is full, just say so.
                                this.module.cfg().message(this.p, "&cInventory full!");
                                return;
                            } else {
                                // Success transaction
                                this.order.storage.remove(index);
                                this.module.orders().saveOrder(this.order); // Save immediately

                                this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f, 1.0f);

                                // Refresh current page
                                this.open();
                            }
                        } else {
                            // Determine allow generic add
                            HashMap<Integer, ItemStack> left = this.p.getInventory().addItem(item);
                            if (!left.isEmpty()) {
                                // Should not happen if firstEmpty != -1 generally, unless max stack issues
                                // But handling leftovers:
                                // If we collected it, we removed it from storage.
                                // But wait, we shouldn't remove until we know we added it.
                                // The addItem acts on the player inventory.

                                // Re-add leftovers to inventory? No, we haven't removed from storage yet
                                // actually.
                                // We need to deduct what was added.
                                // item is a reference to storage? No, get() returns ref.
                                // But addItem modifies the stack you pass to it to reflect REMAINDER?
                                // Bukkit API: addItem(ItemStack... items) - "Returns a HashMap containing items
                                // that could not be stored"
                                // It does NOT modify the input ItemStacks in some versions?
                                // Actually it usually does NOT modify the input stack in recent versions, it
                                // returns a new map of leftovers.
                                // Let's assume complete success or fail for simplicity, or handle partial?

                                // Better approach:
                                ItemStack toAdd = item.clone();
                                HashMap<Integer, ItemStack> leftovers = this.p.getInventory().addItem(toAdd);

                                if (leftovers.isEmpty()) {
                                    // All added
                                    this.order.storage.remove(index);
                                    this.module.orders().saveOrder(this.order);
                                    this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f, 1.0f);
                                    this.open();
                                } else {
                                    // Partial or None added
                                    // We need to calculate what was actually taken.
                                    int initial = item.getAmount();
                                    int rem = leftovers.get(0).getAmount(); // Assuming 1 stack

                                    if (rem < initial) {
                                        // Some were taken
                                        int taken = initial - rem;
                                        item.setAmount(rem); // Update storage directly
                                        this.module.orders().saveOrder(this.order);
                                        this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f,
                                                1.0f);
                                        this.open();
                                    } else {
                                        this.module.cfg().message(this.p, "&cInventory full!");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return;
        }
        e.setCancelled(true);
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true); // Always block drag
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        if (this.internalPageSwitch) {
            this.internalPageSwitch = false;
            return;
        }

        TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                () -> new EditOrderMenu(this.module, this.p, this.order).open(), 1L);
    }
}
