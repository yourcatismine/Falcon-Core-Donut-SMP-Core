
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class YourOrdersMenu implements InventoryHolder, MenuOwner {
    private static final String META_SUPPRESS_CLOSE = "prismorder.suppressClose";
    private final OrdersModule module;
    private final Player p;
    private Inventory inv;
    private int page = 0;
    private boolean internalPageSwitch = false;

    public YourOrdersMenu(OrdersModule module, Player p) {
        this.module = module;
        this.p = p;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        // 3 rows = 27 slots
        this.inv = Bukkit.createInventory(this, 27, Utils.formatColors("&8ᴏʀᴅᴇʀѕ -> ʏᴏᴜʀ ᴏʀᴅᴇʀѕ"));

        // Slot 0: New Order
        // &aNew Order
        // &fClick to create new order
        // Slot 0: New Order
        // Orders first, then New Order button
        List<Order> list = this.module.orders().all().stream()
                .filter(o -> o.owner.equals(this.p.getUniqueId()))
                .filter(o -> !o.canceled)
                // .filter(o -> !o.completed) // Show completed orders for collection
                .sorted((o1, o2) -> Long.compare(o2.creationTime, o1.creationTime))
                .collect(Collectors.toList());

        // We have 18 slots (0-17).
        // The list size effectively increases by 1 because of the "New Order" button at
        // the end.
        int totalItems = list.size() + 1;
        int perPage = 18;
        int maxPage = Math.max(0, (totalItems - 1) / perPage);
        if (this.page > maxPage) {
            this.page = maxPage;
        }

        int from = this.page * perPage;
        int to = Math.min(from + perPage, totalItems);

        // Render loop
        int slot = 0;
        for (int i = from; i < to; ++i) {
            if (slot > 17)
                break;

            if (i == list.size()) {
                // This is the "New Order" button position
                this.inv.setItem(slot, makeItem(Material.PAPER, "&aNew Order", List.of("&fClick to create new order")));
            } else {
                // Regular order
                Order o = list.get(i);
                this.inv.setItem(slot, createOrderItem(o));
            }
            slot++;
        }

        // Navigation
        // Slot 18: Back Arrow (if page > 0)
        // Slot 22: Back to Main Menu
        // Slot 26: Next Arrow (if page < max)

        if (this.page > 0) {
            this.inv.setItem(18, makeItem(Material.ARROW, "&5ʙᴀᴄᴋ", List.of("&fClick to go to the previous page")));
        }

        // this.inv.setItem(22, makeItem(Material.BARRIER, "&cBack", List.of("&fReturn
        // to main menu")));

        if (this.page < maxPage) {
            this.inv.setItem(26, makeItem(Material.ARROW, "&5ɴᴇхᴛ", List.of("&fClick to go to the next page")));
        }

        this.p.openInventory(this.inv);
        // this.module.cfg().play(this.p, "sounds.open", "BLOCK_CHEST_OPEN", 0.7f,
        // 1.0f);
        // Removing config dependency, using standard sound or none?
        // Requests usually imply visual changes, I'll keep default sounds for UX if not
        // specified otherwise.
        // Sound removed as per request
        // this.p.playSound(this.p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
    }

    private ItemStack createOrderItem(Order o) {
        // &a%MY_GAMERTAG%'s Order
        // &a%AMOUNT%&f %ITEM_NAME%
        // &a$%AMOUNT%&f each
        // SPACE
        // &6%AMOUNT_DELIVERED%/&a%TOTAL_AMOUNT_DELIVERED%&7 Delivered
        // &6$%AMOUNT_PAID%/&a%TOTAL_AMOUNT_PAID%&7 Paid
        // SPACE
        // &7%EXPIRATION_COUNTDOWN% Untill Order expires

        List<String> lore = new ArrayList<>();
        lore.add(Utils.formatColors("&a" + Utils.abbr(o.requested) + " &f" + o.key.displayName()));
        lore.add(Utils.formatColors("&a$" + Utils.abbr(o.priceEach) + " &feach"));
        lore.add("");
        lore.add(Utils.formatColors("&6" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + " &7Delivered"));
        lore.add(Utils.formatColors("&6$" + Utils.abbr((double) o.delivered * o.priceEach) + "/&a$"
                + Utils.abbr(o.totalPrice()) + " &7Paid"));
        lore.add("");

        long created = o.creationTime;
        if (created == 0)
            created = System.currentTimeMillis(); // Fallback
        long now = System.currentTimeMillis();
        long expiryTime = o.creationTime + (7L * 24 * 60 * 60 * 1000); // 7 days active
        long deletionTime = o.creationTime + (37L * 24 * 60 * 60 * 1000); // 30 days grace

        if (now < expiryTime) {
            long remaining = Math.max(0, expiryTime - now);
            lore.add(Utils.formatColors("&7" + Utils.formatDuration(remaining) + " Untill Order expires"));
        } else {
            long remaining = Math.max(0, deletionTime - now);
            lore.add(Utils.formatColors("&4" + Utils.formatDuration(remaining) + " before it deletes"));
        }

        ItemStack item = new ItemStack(o.key.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String ownerName = Bukkit.getOfflinePlayer(o.owner).getName();
            if (ownerName == null)
                ownerName = "Unknown";

            meta.setDisplayName(Utils.formatColors("&a" + ownerName + "'s Order"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return GuiVariant.merge(item, o.key.buildIcon());
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

        // Slots 0-17: Orders + New Order button
        if (slot >= 0 && slot <= 17) {
            List<Order> list = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(this.p.getUniqueId()))
                    .filter(o -> !o.canceled)
                    // .filter(o -> !o.completed)
                    .sorted((o1, o2) -> Long.compare(o2.creationTime, o1.creationTime))
                    .collect(Collectors.toList());

            int perPage = 18;
            int index = this.page * perPage + slot;

            if (index == list.size()) {
                // New Order Button
                this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new NewOrderMenu(this.module, this.p).open();
                return;
            } else if (index < list.size()) {
                // Existing Order
                this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new EditOrderMenu(this.module, this.p, list.get(index)).open();
                return;
            }
        } else if (slot == 18) {
            // Previous Page
            if (this.page > 0) {
                this.page--;
                this.internalPageSwitch = true;
                this.open();
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        } else if (slot == 26) {
            // Next Page
            List<Order> list = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(this.p.getUniqueId()))
                    .filter(o -> !o.canceled)
                    // .filter(o -> !o.completed)
                    .sorted((o1, o2) -> Long.compare(o2.creationTime, o1.creationTime))
                    .collect(Collectors.toList());
            int totalItems = list.size() + 1;
            int perPage = 18;
            int maxPage = Math.max(0, (totalItems - 1) / perPage);

            if (this.page < maxPage) {
                this.page++;
                this.internalPageSwitch = true;
                this.open();
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
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
        if (this.p.hasMetadata(META_SUPPRESS_CLOSE)) {
            this.p.removeMetadata(META_SUPPRESS_CLOSE, this.module.getPlugin());
            return;
        }
        TaskUtil.runEntityLater(this.module.getPlugin(), this.p,
                () -> new OrdersMainMenu(this.module, this.p).open(),
                1L);
    }
}
