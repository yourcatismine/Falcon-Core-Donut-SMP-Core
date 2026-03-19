
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.List;
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
        this.inv = Bukkit.createInventory(this, 27, Utils.formatColors("&8ᴏʀᴅᴇʀѕ -> ʏᴏᴜʀ ᴏʀᴅᴇʀѕ"));

        List<Order> list = this.module.orders().all().stream()
                .filter(o -> o.owner.equals(this.p.getUniqueId()))
                .filter(o -> (!o.canceled && !o.completed) || !o.storage.isEmpty())
                .sorted((o1, o2) -> Long.compare(o2.creationTime, o1.creationTime))
                .collect(Collectors.toList());

        int totalItems = list.size() + 1;
        int perPage = 18;
        int maxPage = Math.max(0, (totalItems - 1) / perPage);
        if (this.page > maxPage) {
            this.page = maxPage;
        }

        int from = this.page * perPage;
        int to = Math.min(from + perPage, totalItems);

        int slot = 0;
        for (int i = from; i < to; ++i) {
            if (slot > 17)
                break;

            if (i == list.size()) {
                this.inv.setItem(slot, makeItem(Material.PAPER, "&aNew Order", List.of("&fClick to create new order")));
            } else {
                Order o = list.get(i);
                this.inv.setItem(slot, createOrderItem(o));
            }
            slot++;
        }

        if (this.page > 0) {
            this.inv.setItem(18,
                    makeItem(Material.ARROW, "&#A9833Dʙᴀᴄᴋ", List.of("&fClick to go to the previous page")));
        }

        if (this.page < maxPage) {
            this.inv.setItem(26, makeItem(Material.ARROW, "&#A9833Dɴᴇхᴛ", List.of("&fClick to go to the next page")));
        }

        this.p.openInventory(this.inv);
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
        lore.add(Utils.formatColors("&d" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + " &7Delivered"));
        lore.add(Utils.formatColors("&d$" + Utils.abbr((double) o.delivered * o.priceEach) + "/&a$"
                + Utils.abbr(o.totalPrice()) + " &7Paid"));
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

        if (e.getClickedInventory().getHolder() == this) {
            e.setCancelled(true);
        } else {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }

        int slot = e.getSlot();

        if (slot >= 0 && slot <= 17) {
            List<Order> list = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(this.p.getUniqueId()))
                    .filter(o -> (!o.canceled && !o.completed) || !o.storage.isEmpty())
                    .sorted((o1, o2) -> Long.compare(o2.creationTime, o1.creationTime))
                    .collect(Collectors.toList());

            int perPage = 18;
            int index = this.page * perPage + slot;

            if (index == list.size()) {
                this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new NewOrderMenu(this.module, this.p).open();
                return;
            } else if (index < list.size()) {
                Order target = list.get(index);
                this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new EditOrderMenu(this.module, this.p, target).open();
                return;
            }
        } else if (slot == 18) {
            if (this.page > 0) {
                this.page--;
                this.internalPageSwitch = true;
                this.open();
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        } else if (slot == 26) {
            List<Order> list = this.module.orders().all().stream()
                    .filter(o -> o.owner.equals(this.p.getUniqueId()))
                    .filter(o -> (!o.canceled && !o.completed) || !o.storage.isEmpty())
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
