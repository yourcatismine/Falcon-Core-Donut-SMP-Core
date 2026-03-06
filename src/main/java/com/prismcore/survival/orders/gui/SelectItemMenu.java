package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.catalog.ItemCatalog;
import com.prismcore.survival.orders.data.AlphaSort;
import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.orders.store.PlayerStateManager;
import com.prismcore.survival.orders.util.SignInputUtil;
import com.prismcore.survival.orders.util.TaskUtil;
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
import org.bukkit.metadata.FixedMetadataValue;

public class SelectItemMenu implements InventoryHolder, MenuOwner {
    private static final String META_SUPPRESS_CLOSE = "prismorder.suppressClose";
    private final OrdersModule module;
    private final Player p;
    private final Consumer<ItemKey> callback;
    private Inventory inv;
    private final List<ItemCatalog.Entry> pageEntries = new ArrayList<>();
    private int page = 0;

    private int sortIndex = 0;
    private boolean internalPageSwitch = false;

    public SelectItemMenu(OrdersModule module, Player p) {
        this(module, p, null);
    }

    public SelectItemMenu(OrdersModule module, Player p, Consumer<ItemKey> callback) {
        this.module = module;
        this.p = p;
        this.callback = callback;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    private List<ItemCatalog.Entry> computeList() {
        PlayerStateManager.ItemView v = this.module.state().items(this.p.getUniqueId());

        if (v.filter == null || v.filter.isBlank()) {
            v.filter = "All";
        }

        List<ItemCatalog.Entry> all = new ArrayList<>(ItemCatalog.build(this.module));

        all.removeIf(e -> this.module.cfg().isDisabled(e.base));

        if (!"All".equalsIgnoreCase(v.filter)) {
            Set<Material> allow = this.module.filters().resolve(v.filter);
            if (allow != null && !allow.isEmpty()) {
                all.removeIf(e -> !allow.contains(e.base));
            }
        }

        if (v.search != null && !v.search.isBlank()) {
            String s = v.search.toLowerCase(Locale.ENGLISH);
            all.removeIf(e -> !e.search.contains(s));

            all.sort((e1, e2) -> {
                boolean e1Exact = e1.display.equalsIgnoreCase(s);
                boolean e2Exact = e2.display.equalsIgnoreCase(s);
                if (e1Exact && !e2Exact)
                    return -1;
                if (!e1Exact && e2Exact)
                    return 1;

                boolean e1Start = e1.display.toLowerCase().startsWith(s);
                boolean e2Start = e2.display.toLowerCase().startsWith(s);
                if (e1Start && !e2Start)
                    return -1;
                if (!e1Start && e2Start)
                    return 1;

                return e1.display.compareToIgnoreCase(e2.display);
            });
        } else {
            all.sort(Comparator.comparing(e -> e.display));
            if (v.alpha == AlphaSort.Z_A) {
            }
        }
        return all;
    }

    public void open() {
        this.inv = Bukkit.createInventory(this, 54, Utils.formatColors("&8ᴏʀᴅᴇʀѕ -> ѕᴇʟᴇᴄᴛ ɪᴛᴇᴍ"));

        List<ItemCatalog.Entry> list = this.computeList();
        this.pageEntries.clear();

        int perPage = 45;
        int maxPage = Math.max(0, (list.size() - 1) / perPage);

        if (this.page > maxPage)
            this.page = maxPage;
        if (this.page < 0)
            this.page = 0;

        int from = this.page * perPage;
        int to = Math.min(from + perPage, list.size());

        for (int i = from; i < to; ++i) {
            ItemCatalog.Entry e = list.get(i);
            this.pageEntries.add(e);

            ItemStack item = e.stack.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Utils.formatColors("&f" + e.display));
                ItemKey key = ItemKey.fromStack(e.stack);
                if (key != null) {
                    List<String> ench = key.enchantLoreLines("&7");
                    if (!ench.isEmpty()) {
                        List<String> lore = meta.getLore();
                        if (lore == null)
                            lore = new ArrayList<>();
                        lore.add(Utils.formatColors("&7"));
                        lore.addAll(Utils.formatColors(ench));
                        meta.setLore(lore);
                    }
                }
                meta.addItemFlags(ItemFlag.values());
                item.setItemMeta(meta);
            }
            this.inv.setItem(i - from, item);
        }

        if (this.page > 0) {
            this.inv.setItem(45, makeItem(Material.ARROW, "&5ʙᴀᴄᴋ", List.of("&fClick to go to the previous page")));
        }

        List<String> sortOptions = List.of("Most Paid", "Most Delivered", "Recently Listed", "Most Money Per Item");
        List<String> sortLore = new ArrayList<>();
        for (int i = 0; i < sortOptions.size(); i++) {
            String prefix = (i == this.sortIndex) ? "&a• " : "&f• ";
            sortLore.add(prefix + sortOptions.get(i));
        }
        this.inv.setItem(48, makeItem(Material.CAULDRON, "&5ѕᴏʀᴛ", sortLore));

        PlayerStateManager.ItemView v = this.module.state().items(this.p.getUniqueId());
        List<String> categories = List.of("All", "Blocks", "Tools", "Food", "Combat", "Potions", "Books", "Ingredients",
                "Utilities");
        List<String> filterLore = new ArrayList<>();
        for (String cat : categories) {
            String prefix = cat.equalsIgnoreCase(v.filter) ? "&a• " : "&f• ";
            filterLore.add(prefix + cat);
        }
        this.inv.setItem(49, makeItem(Material.HOPPER, "&5ꜰɪʟᴛᴇʀ", filterLore));

        this.inv.setItem(50, makeItem(Material.OAK_SIGN, "&5ѕᴇᴀʀᴄʜ", List.of("&fClick to search")));

        if (this.page < maxPage) {
            this.inv.setItem(53, makeItem(Material.ARROW, "&5ɴᴇхᴛ", List.of("&fClick to go to the next page")));
        }

        this.p.openInventory(this.inv);
        this.p.playSound(this.p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            if (lore != null)
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

        if (slot == 45) {
            if (this.page > 0) {
                int oldPage = this.page + 1;
                this.page--;
                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Navigated: Select Item (Page " + oldPage + " -> " + (this.page + 1) + ")");
                this.p.playSound(this.p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                this.internalPageSwitch = true;
                open();
            }
            return;
        }

        if (slot == 53) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.ARROW) {
                int oldPage = this.page + 1;
                this.page++;
                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Navigated: Select Item (Page " + oldPage + " -> " + (this.page + 1) + ")");
                this.p.playSound(this.p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                this.internalPageSwitch = true;
                open();
            }
            return;
        }

        if (slot == 48) {
            this.sortIndex = (this.sortIndex + 1) % 4;
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.internalPageSwitch = true;
            open();
            return;
        }

        if (slot == 49) {
            PlayerStateManager.ItemView v = this.module.state().items(this.p.getUniqueId());
            List<String> categories = List.of("All", "Blocks", "Tools", "Food", "Combat", "Potions", "Books",
                    "Ingredients", "Utilities");
            int idx = -1;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).equalsIgnoreCase(v.filter)) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1)
                idx = 0;

            int nextIdx = (idx + 1) % categories.size();
            v.filter = categories.get(nextIdx);

            this.page = 0;
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.internalPageSwitch = true;
            open();
            return;
        }

        if (slot == 50) {
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
            this.p.closeInventory();

            ConfigurationSection sec = this.module.cfg().cfg().getConfigurationSection("search-sign");
            SignInputUtil.openFromConfig(this.module.getPlugin(), this.p, sec, (lines) -> {
                String input = lines == null ? "" : lines.trim();
                if (input.equals("-")) {
                    input = "";
                }
                PlayerStateManager.ItemView v = this.module.state().items(this.p.getUniqueId());
                v.search = input.isEmpty() ? null : input;
                this.page = 0;

                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Searched Item: '" + (v.search == null ? "None" : v.search) + "'");

                TaskUtil.runEntity(this.module.getPlugin(), this.p, this::open);
            });
            return;
        }

        if (slot >= 0 && slot <= 44) {
            if (slot < this.pageEntries.size()) {
                ItemCatalog.Entry e2 = this.pageEntries.get(slot);
                com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                        com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                        "Selected Item for Order: " + e2.display);
                this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                if (this.callback != null) {
                    ItemKey key = null;
                    if (e2.stack != null) {
                        key = ItemKey.fromStack(e2.stack);
                    }
                    if (key == null) {
                        key = ItemKey.of(e2.base);
                    }

                    this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
                    this.callback.accept(key);
                }
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
                () -> new NewOrderMenu(this.module, this.p).open(),
                1L);
    }
}
