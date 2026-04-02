package com.h2ph.gui;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BountyGUI {

    private final Falcon plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public enum SortType {
        AMOUNT("Amount"),
        RECENTLY_SET("Recently Set");

        private final String displayName;

        SortType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public BountyGUI(Falcon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0, SortType.AMOUNT, "");
    }

    public void open(Player player, int page, SortType sortType, String searchQuery) {
        Map<UUID, BountyManager.BountyEntry> bountiesMap = plugin.getBountyManager().getActiveBounties();

        List<Map.Entry<UUID, BountyManager.BountyEntry>> sortedList = new ArrayList<>(bountiesMap.entrySet());

        if (searchQuery != null && !searchQuery.isEmpty()) {
            String finalSearch = searchQuery.toLowerCase();
            sortedList = sortedList.stream()
                    .filter(e -> {
                        String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                        return name != null && name.toLowerCase().contains(finalSearch);
                    })
                    .collect(Collectors.toList());
        }

        if (sortType == SortType.AMOUNT) {
            sortedList.sort((e1, e2) -> Double.compare(e2.getValue().getAmount(), e1.getValue().getAmount()));
        } else {
            sortedList.sort((e1, e2) -> Long.compare(e2.getValue().getTimestamp(), e1.getValue().getTimestamp()));
        }

        int size = 54;
        String title = color("&8ʙᴏᴜɴᴛɪᴇѕ (page " + (page + 1) + ")");
        Inventory inv = Bukkit.createInventory(new BountyHolder(page, sortType, searchQuery), size, title);

        int start = page * 45;
        int end = Math.min(start + 45, sortedList.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            Map.Entry<UUID, BountyManager.BountyEntry> entry = sortedList.get(i);
            UUID targetId = entry.getKey();
            double amount = entry.getValue().getAmount();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                String name = Bukkit.getOfflinePlayer(targetId).getName();
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetId));
                meta.setDisplayName(color("&b" + (name != null ? name : "Unknown")));
                List<String> lore = new ArrayList<>();
                lore.add(color("&fBounty:&7 $" + formatNumber(amount)));
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "&aʙᴀᴄᴋ", "&fClick to go to the previous page"));
        }
        if (end < sortedList.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&aɴᴇхᴛ", "&fClick to go to the next page"));
        }

        List<String> sortLore = new ArrayList<>();
        if (sortType == SortType.AMOUNT) {
            sortLore.add(color("&fClick to sort (Recently Set)"));
        } else {
            sortLore.add(color("&fClick to sort (Amount)"));
        }
        inv.setItem(48, createItem(Material.HOPPER, "&bѕᴏʀᴛ", sortLore));

        List<String> infoLore = new ArrayList<>();
        infoLore.add(color("&fClick to refresh"));
        infoLore.add("");
        infoLore.add(color("&7Set a bounty using this:"));
        infoLore.add(color("&7&o/bounty add (player) (amount)"));
        inv.setItem(49, createItem(Material.SKELETON_SKULL, "&bʙᴏᴜɴᴛɪᴇѕ", infoLore));

        inv.setItem(50, createItem(Material.OAK_SIGN, "&bѕᴇᴀʀᴄʜ", "&fClick to search"));

        player.openInventory(inv);
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
            return DF.format(Math.floor(number * 10) / 10.0);
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

    public static class BountyHolder implements InventoryHolder {
        private final int page;
        private final SortType sortType;
        private final String searchQuery;

        public BountyHolder(int page, SortType sortType, String searchQuery) {
            this.page = page;
            this.sortType = sortType;
            this.searchQuery = searchQuery;
        }

        public int getPage() {
            return page;
        }

        public SortType getSortType() {
            return sortType;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
