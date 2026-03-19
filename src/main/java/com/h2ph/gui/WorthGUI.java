package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.orders.data.ItemKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class WorthGUI {

    private final PrismSurvival plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public enum SortType {
        HIGHEST_PRICE("Highest Price"),
        LOWEST_PRICE("Lowest Price"),
        BY_NAME("By name");

        private final String displayName;

        SortType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum FilterType {
        ALL("All"),
        BLOCKS("Blocks"),
        TOOLS("Tools"),
        FOOD("Food"),
        COMBAT("Combat"),
        POTIONS("Potions"),
        BOOKS("Books"),
        INGREDIENTS("Ingredients"),
        UTILITIES("Utilities");

        private final String displayName;

        FilterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Player player;

    public WorthGUI(PrismSurvival plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        open(0, SortType.HIGHEST_PRICE, FilterType.ALL);
    }

    public void open(int page, SortType sortType, FilterType filterType) {
        Map<ItemKey, Double> pricesMap = plugin.getPrismSell().getPricesManager().getPrices();
        List<Map.Entry<ItemKey, Double>> items = new ArrayList<>(pricesMap.entrySet());

        items = items.stream().filter(entry -> matchesFilter(entry.getKey(), filterType)).collect(Collectors.toList());

        switch (sortType) {
            case HIGHEST_PRICE:
                items.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
                break;
            case LOWEST_PRICE:
                items.sort((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()));
                break;
            case BY_NAME:
                items.sort((e1, e2) -> e1.getKey().displayName().compareToIgnoreCase(e2.getKey().displayName()));
                break;
        }

        int pageSize = 45;
        int maxPage = (int) Math.max(0, Math.ceil(items.size() / (double) pageSize) - 1);
        if (page > maxPage)
            page = maxPage;
        if (page < 0)
            page = 0;

        String title = color("&8ɪᴛᴇᴍ ᴘʀɪᴄᴇѕ (Page " + (page + 1) + ")");
        Inventory inv = Bukkit.createInventory(new WorthHolder(page, sortType, filterType), 54, title);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, items.size());

        for (int i = start; i < end; i++) {
            Map.Entry<ItemKey, Double> entry = items.get(i);
            ItemStack item = entry.getKey().buildIcon();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&d" + entry.getKey().displayName()));
                List<String> lore = new ArrayList<>();
                lore.add(color("&fPrice: &a$" + formatNumber(entry.getValue())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i - start, item);
        }

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "&aʙᴀᴄᴋ", "&fClick to go to the previous page"));
        }
        if (end < items.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&aɴᴇхᴛ", "&fClick to go to the next page"));
        }

        List<String> sortLore = new ArrayList<>();
        for (SortType st : SortType.values()) {
            if (st == sortType) {
                sortLore.add(color("&a• " + st.getDisplayName()));
            } else {
                sortLore.add(color("&f• " + st.getDisplayName()));
            }
        }
        inv.setItem(48, createItem(Material.CAULDRON, "&dѕᴏʀᴛ", sortLore));

        inv.setItem(49, createItem(Material.ANVIL, "&dɪᴛᴇᴍ ᴘʀɪᴄᴇѕ", "&fClick to refresh"));

        List<String> filterLore = new ArrayList<>();
        for (FilterType ft : FilterType.values()) {
            if (ft == filterType) {
                filterLore.add(color("&a• " + ft.getDisplayName()));
            } else {
                filterLore.add(color("&f• " + ft.getDisplayName()));
            }
        }
        inv.setItem(50, createItem(Material.HOPPER, "&dꜰɪʟᴛᴇʀ", filterLore));

        player.openInventory(inv);
    }

    private boolean matchesFilter(ItemKey key, FilterType filter) {
        if (filter == FilterType.ALL)
            return true;
        Material mat = key.material;
        String name = mat.name();

        switch (filter) {
            case BLOCKS:
                return mat.isBlock();
            case TOOLS:
                return name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                        || name.endsWith("_HOE") || mat == Material.SHEARS || mat == Material.FLINT_AND_STEEL
                        || mat == Material.FISHING_ROD || mat == Material.BRUSH;
            case FOOD:
                return mat.isEdible();
            case COMBAT:
                return name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || mat == Material.BOW
                        || mat == Material.CROSSBOW || mat == Material.TRIDENT || mat == Material.SHIELD
                        || mat == Material.MACE;
            case POTIONS:
                return name.contains("POTION") || mat == Material.TIPPED_ARROW;
            case BOOKS:
                return mat == Material.ENCHANTED_BOOK || mat == Material.BOOK || mat == Material.WRITTEN_BOOK
                        || mat == Material.WRITABLE_BOOK;
            case INGREDIENTS:
                return !mat.isBlock() && !mat.isEdible() && !name.endsWith("_SWORD") && !name.endsWith("_AXE")
                        && !name.endsWith("_PICKAXE");
            case UTILITIES:
                return mat == Material.CHEST || mat == Material.ENDER_CHEST || mat == Material.HOPPER
                        || mat == Material.DISPENSER || mat == Material.DROPPER || mat == Material.CRAFTING_TABLE
                        || mat == Material.FURNACE || mat == Material.BLAST_FURNACE || mat == Material.SMOKER
                        || mat == Material.ANVIL;
            default:
                return true;
        }
    }

    private ItemStack createItem(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (loreLine != null) {
                meta.setLore(Collections.singletonList(color(loreLine)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "T");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "B");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "M");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "K");
        } else {
            return DF.format(Math.floor(number * 100) / 100.0);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        if (scaled == (long) scaled) {
            return String.valueOf((long) scaled) + suffix;
        }
        return DF.format(scaled) + suffix;
    }

    public static class WorthHolder implements InventoryHolder {
        private final int page;
        private final SortType sortType;
        private final FilterType filterType;

        public WorthHolder(int page, SortType sortType, FilterType filterType) {
            this.page = page;
            this.sortType = sortType;
            this.filterType = filterType;
        }

        public int getPage() {
            return page;
        }

        public SortType getSortType() {
            return sortType;
        }

        public FilterType getFilterType() {
            return filterType;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
