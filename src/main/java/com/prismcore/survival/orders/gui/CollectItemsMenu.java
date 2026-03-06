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
    private static final long CLICK_COOLDOWN_MS = 200;

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
        if (this.order.completed && this.order.storage.isEmpty()) {
            this.module.orders().deleteOrder(this.order);
            this.module.cfg().message(this.p, "&aOrder completed and collected! Deleted.");
            new YourOrdersMenu(this.module, this.p).open();
            return;
        }
        if (this.order.storage.isEmpty()) {
            this.module.cfg().message(this.p, "&cNo items to collect.");
            this.p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(Utils.formatColors("&cNo items to collect.")));
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

        Inventory top = e.getView().getTopInventory();
        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals((Object) top);
        boolean clickedPlayer = e.getClickedInventory() != null && e.getClickedInventory().getHolder() == this.p;

        if (e.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        int rows = this.rows();
        int slot = e.getSlot();

        if (clickedTop) {
            int prev = (rows - 1) * 9;
            int next = rows * 9 - 1;
            int drop = rows * 9 - 2;

            if (slot >= (rows - 1) * 9) {
                e.setCancelled(true);
                if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
                    return;
                }

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
                    if (this.module.cfg().isWorldLocked(this.p.getWorld().getName())) {
                        this.module.cfg().message(this.p, "&cYou cannot drop loot in this world!");
                        this.module.cfg().play(this.p, "sounds.error", "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
                        e.setCancelled(true);
                        return;
                    }

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
                            Utils.stripOrderMetadata(item);
                            Item dropped = this.p.getWorld().dropItem(eye, item);
                            dropped.setVelocity(eye.getDirection().multiply(0.25));
                        }
                    }

                    this.module.orders().saveOrder(this.order);
                    this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);

                    this.internalPageSwitch = true;
                    this.open();
                    return;
                }
                return;
            }

            if (slot < (rows - 1) * 9) {
                org.bukkit.event.inventory.InventoryAction action = e.getAction();
                boolean isTaking = action == org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
                        || action == org.bukkit.event.inventory.InventoryAction.PICKUP_HALF
                        || action == org.bukkit.event.inventory.InventoryAction.PICKUP_ONE
                        || action == org.bukkit.event.inventory.InventoryAction.PICKUP_SOME
                        || action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY;

                if (!isTaking) {
                    e.setCancelled(true);
                    return;
                }

                e.setCancelled(true);
                int per = this.perPage();
                int index = (this.currentPage * per) + slot;

                if (index >= 0 && index < this.order.storage.size()) {
                    ItemStack item = this.order.storage.get(index);
                    if (item != null && item.getType() != Material.AIR) {
                        long now = System.currentTimeMillis();
                        if (now - lastClickTime < CLICK_COOLDOWN_MS)
                            return;
                        lastClickTime = now;

                        ItemStack toAdd = Utils.stripOrderMetadata(item.clone());
                        int initialAmount = toAdd.getAmount();

                        if (action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                            HashMap<Integer, ItemStack> leftovers = this.p.getInventory().addItem(toAdd);
                            int rem = leftovers.isEmpty() ? 0 : leftovers.get(0).getAmount();
                            if (rem < initialAmount) {
                                int taken = initialAmount - rem;
                                if (rem == 0) {
                                    this.order.storage.remove(index);
                                } else {
                                    item.setAmount(rem);
                                }
                                this.module.orders().saveOrder(this.order);
                                this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f, 1.0f);
                                this.internalPageSwitch = true;
                                this.open();
                            } else {
                                this.module.cfg().message(this.p, "&cInventory full!");
                            }
                        } else {
                            ItemStack cursor = e.getView().getCursor();
                            if (cursor == null || cursor.getType() == Material.AIR) {
                                int toTake = initialAmount;
                                if (action == org.bukkit.event.inventory.InventoryAction.PICKUP_HALF) {
                                    toTake = (int) Math.ceil(initialAmount / 2.0);
                                } else if (action == org.bukkit.event.inventory.InventoryAction.PICKUP_ONE) {
                                    toTake = 1;
                                }

                                ItemStack taking = toAdd.clone();
                                taking.setAmount(toTake);
                                e.getView().setCursor(taking);

                                if (toTake >= initialAmount) {
                                    this.order.storage.remove(index);
                                } else {
                                    item.setAmount(initialAmount - toTake);
                                }
                                this.module.orders().saveOrder(this.order);
                                this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f, 1.0f);
                                this.internalPageSwitch = true;
                                this.open();
                            } else {
                                ItemStack cursorStripped = Utils.stripOrderMetadata(cursor.clone());
                                if (cursorStripped.isSimilar(toAdd)) {
                                    int canTake = cursor.getMaxStackSize() - cursor.getAmount();
                                    if (canTake > 0) {
                                        int toTake = Math.min(canTake, initialAmount);
                                        cursor.setAmount(cursor.getAmount() + toTake);

                                        if (toTake >= initialAmount) {
                                            this.order.storage.remove(index);
                                        } else {
                                            item.setAmount(initialAmount - toTake);
                                        }
                                        this.module.orders().saveOrder(this.order);
                                        this.module.cfg().play(this.p, "sounds.click", "ENTITY_ITEM_PICKUP", 0.5f,
                                                1.0f);
                                        this.internalPageSwitch = true;
                                        this.open();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return;
        }

        if (clickedPlayer) {
            if (e.isShiftClick()) {
                e.setCancelled(true);
            }
            return;
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        for (int slot : e.getRawSlots()) {
            if (slot < e.getView().getTopInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }
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
