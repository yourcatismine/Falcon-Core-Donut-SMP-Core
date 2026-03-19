/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 */
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.data.SortType;
import com.prismcore.survival.orders.store.PlayerStateManager;
import com.prismcore.survival.orders.util.SignInputUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OrdersMainMenu implements InventoryHolder, MenuOwner {
    private final OrdersModule module;
    private final Player p;
    private Inventory inv;

    public OrdersMainMenu(OrdersModule module, Player p) {
        this.module = module;
        this.p = p;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        PlayerStateManager.View st = this.module.state().main(this.p.getUniqueId());

        if (st.sort == null)
            st.sort = SortType.MOST_PAID;
        if (st.filter == null || st.filter.isBlank())
            st.filter = "All";
        if (st.page < 0)
            st.page = 0;

        String title = Utils.formatColors("&8ᴏʀᴅᴇʀѕ (Page " + (st.page + 1) + ")");
        this.inv = Bukkit.createInventory(this, 54, title);

        List<Order> list = getFilteredOrders(st);

        int perPage = 45;
        int maxPage = Math.max(0, (list.size() - 1) / perPage);
        if (st.page > maxPage) {
            st.page = maxPage;
        }

        if (st.page > 0) {
            this.inv.setItem(45,
                    makeItem(Material.ARROW, "&dʙᴀᴄᴋ", List.of("&fClick to go to the previous page")));
        }

        if (st.page < maxPage) {
            this.inv.setItem(53, makeItem(Material.ARROW, "&dɴᴇхᴛ", List.of("&fClick to go to the next page")));
        }

        List<String> sortLore = new ArrayList<>();
        sortLore.add(isSort(st.sort, SortType.MOST_PAID) + "Most Paid");
        sortLore.add(isSort(st.sort, SortType.MOST_DELIVERED) + "Most Delivered");
        sortLore.add(isSort(st.sort, SortType.RECENTLY_LISTED) + "Recently Listed");
        sortLore.add(isSort(st.sort, SortType.MOST_MONEY_PER_ITEM) + "Most Money Per Item");
        this.inv.setItem(47, makeItem(Material.CAULDRON, "&dѕᴏʀᴛ", sortLore));

        List<String> filterLore = new ArrayList<>();
        List<String> cats = new ArrayList<>();

        cats.addAll(this.module.filters().categoryNames());
        for (String c : cats) {
            filterLore.add(isFilter(st.filter, c) + c);
        }
        this.inv.setItem(48, makeItem(Material.HOPPER, "&dꜰɪʟᴛᴇʀ", filterLore));

        this.inv.setItem(49, makeItem(Material.MAP, "&dᴏʀᴅᴇʀꜱ", List.of("&fClick to refresh")));

        this.inv.setItem(50, makeItem(Material.OAK_SIGN, "&dѕᴇᴀʀᴄʜ", List.of("&fClick to search")));

        this.inv.setItem(51, makeItem(Material.CHEST, "&dʏᴏᴜʀ ᴏʀᴅᴇʀꜱ", List.of("&fClick to view your orders")));

        int from = st.page * perPage;
        int to = Math.min(from + perPage, list.size());

        int slot = 0;
        for (int i = from; i < to; ++i) {
            Order o = list.get(i);
            this.inv.setItem(slot++, createOrderDisplayItem(o));
        }

        this.p.openInventory(this.inv);
    }

    private List<Order> getFilteredOrders(PlayerStateManager.View st) {
        List<Order> list = this.module.orders().all().stream()
                .filter(o -> !o.canceled)
                .filter(o -> !o.completed)
                .filter(o -> System.currentTimeMillis() < o.creationTime + (7L * 24 * 60 * 60 * 1000))
                .collect(Collectors.toList());

        if (st.search != null && !st.search.isBlank()) {
            String s = st.search.toLowerCase(Locale.ENGLISH);
            list.removeIf(o -> {
                String disp = o.key.displayName().toLowerCase(Locale.ENGLISH);
                String mat = o.key.material.name().toLowerCase(Locale.ENGLISH);
                return !disp.contains(s) && !mat.contains(s);
            });
        }

        Set<Material> allow;
        if (!"All".equalsIgnoreCase(st.filter) && (allow = this.module.filters().resolve(st.filter)) != null
                && !allow.isEmpty()) {
            list.removeIf(o -> !allow.contains(o.key.material));
        }

        switch (st.sort) {
            case MOST_PAID -> list.sort(Comparator.comparingDouble(o -> -o.totalPrice()));
            case MOST_DELIVERED -> list.sort(Comparator.comparingInt(o -> -o.delivered));
            case RECENTLY_LISTED -> list.sort(Comparator.comparingLong(o -> -o.creationTime));
            case MOST_MONEY_PER_ITEM -> list.sort(Comparator.comparingDouble(o -> -o.priceEach));
        }
        return list;
    }

    private String isSort(SortType current, SortType check) {
        return current == check ? "&a• " : "&f• ";
    }

    private String isFilter(String current, String check) {
        return current.equalsIgnoreCase(check) ? "&a• " : "&f• ";
    }

    private ItemStack createOrderDisplayItem(Order o) {
        String ownerName = this.module.getNameCache().getName((UUID) o.owner);
        if (ownerName == null)
            ownerName = "Loading...";

        long expiryTime = o.creationTime + (7L * 24 * 60 * 60 * 1000);
        long remaining = Math.max(0, expiryTime - System.currentTimeMillis());
        String countdown = Utils.formatDuration(remaining);

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
        lore.add(Utils.formatColors("&d" + Utils.abbr(o.delivered) + "/&a" + Utils.abbr(o.requested) + "&7 Delivered"));
        lore.add(Utils.formatColors("&d$" + Utils.abbr((double) o.delivered * o.priceEach) + "/&a$"
                + Utils.abbr(o.totalPrice()) + "&7 Paid"));
        lore.add("");
        lore.add(Utils.formatColors("&fClick to deliver &a" + ownerName + "&f " + o.key.displayName()));
        lore.add(Utils.formatColors("&7" + countdown + " Untill Order expires"));

        ItemStack item = new ItemStack(o.key.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors("&a" + ownerName));
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
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
                return;
            }
        } else {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }

        int slot = e.getSlot();
        PlayerStateManager.View st = this.module.state().main(this.p.getUniqueId());

        if (slot == 45) {
            if (st.page > 0) {
                st.page--;
                playSound(Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                open();
            }
            return;
        }

        if (slot == 53)

        {
            st.page++;
            playSound(Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            open();
            return;
        }

        if (slot == 47) {
            st.sort = nextSort(st.sort);

            playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            st.page = 0;
            open();
            return;
        }

        if (slot == 48) {
            ArrayList<String> cats = new ArrayList<>();

            cats.addAll(this.module.filters().categoryNames());
            int i = Math.max(0, cats.indexOf(st.filter));
            st.filter = cats.get((i + 1) % cats.size());

            playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            st.page = 0;
            open();
            return;
        }

        if (slot == 49) {
            st.search = null;
            st.page = 0;

            playSound(Sound.UI_TOAST_IN, 1.0f, 1.0f);
            open();
            return;
        }

        if (slot == 50) {
            playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.p.closeInventory();
            ConfigurationSection sec = this.module.cfg().cfg().getConfigurationSection("search-sign");
            SignInputUtil.openFromConfig(this.module.getPlugin(), this.p, sec, input -> {
                String trimmed = input == null ? "" : input.trim();
                PlayerStateManager.View st2 = this.module.state().main(this.p.getUniqueId());
                if (trimmed.equals("-"))
                    trimmed = "";
                st2.search = trimmed.isEmpty() ? null : trimmed;
                st2.page = 0;

                new OrdersMainMenu(this.module, this.p).open();
            });
            return;
        }

        if (slot == 51) {
            playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new YourOrdersMenu(this.module, this.p).open();
            return;
        }

        if (slot >= 0 && slot <= 44) {
            List<Order> list = getFilteredOrders(st);

            int index = st.page * 45 + slot;
            if (index >= 0 && index < list.size()) {
                Order target = list.get(index);
                if (target.owner.equals(this.p.getUniqueId())) {
                    playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    return;
                }
                String ownerName = this.module.getNameCache().getName((UUID) target.owner);
                if (ownerName == null)
                    ownerName = "Loading...";

                playSound(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new DeliverItemsMenu(this.module, this.p, target).open();
            }
        }
    }

    private void playSound(Sound sound, float vol, float pitch) {
        this.p.playSound(this.p.getLocation(), sound, vol, pitch);
    }

    private SortType nextSort(SortType cur) {
        return switch (cur) {
            case MOST_PAID -> SortType.MOST_DELIVERED;
            case MOST_DELIVERED -> SortType.RECENTLY_LISTED;
            case RECENTLY_LISTED -> SortType.MOST_MONEY_PER_ITEM;
            case MOST_MONEY_PER_ITEM -> SortType.MOST_PAID;
            default -> throw new IllegalStateException("Unexpected value: " + cur);
        };
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
    }
}
