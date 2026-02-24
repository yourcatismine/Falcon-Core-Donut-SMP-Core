package com.h2ph.commands.economy;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;

import com.prismcore.survival.sell.category.Category;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class SellHistoryCommand implements CommandExecutor, Listener {

    private final PrismSurvival plugin;
    private final String GUI_PREFIX = ChatColor.translateAlternateColorCodes('&', "&8ѕᴇʟʟ ʜɪѕᴛᴏʀʏ");
    private final int PAGE_SIZE = 45; // top 5 rows

    // per-player sort mode: true = by name, false = by amount (desc)
    private final Map<UUID, Boolean> sortByName = new HashMap<>();
    // keeps track of which category the player is viewing
    private final Map<UUID, Category> viewingCategory = new HashMap<>();

    public SellHistoryCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        openCategoryMenu(p);
        return true;
    }

    private static class AggEntry {
        Material mat;
        long amount;
        double total;
    }

    private void openCategoryMenu(Player p) {
        viewingCategory.remove(p.getUniqueId());
        String title = ChatColor.translateAlternateColorCodes('&', "&8ѕᴇʟʟ ʜɪѕᴛᴏʀʏ");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        Category[] cats = Category.values();
        for (int i = 0; i < Math.min(cats.length, slots.length); i++) {
            Category cat = cats[i];
            ItemStack is = new ItemStack(cat.getDisplayMaterial());
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(ChatColor.translateAlternateColorCodes('&', cat.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(color(cat.getDescription1()));
            lore.add(color(cat.getDescription2()));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to view history"));
            im.setLore(lore);
            is.setItemMeta(im);
            gui.setItem(slots[i], is);
        }

        // If there are more categories, add them to the second row (not enough slots in
        // 27 maybe?)
        // Let's use 54 for consistency if needed, but 27 is fine for now.
        if (cats.length > slots.length) {
            int[] extraSlots = { 19, 20, 21, 22, 23, 24, 25 };
            for (int i = slots.length; i < Math.min(cats.length, slots.length + extraSlots.length); i++) {
                Category cat = cats[i];
                ItemStack is = new ItemStack(cat.getDisplayMaterial());
                ItemMeta im = is.getItemMeta();
                im.setDisplayName(ChatColor.translateAlternateColorCodes('&', cat.getDisplayName()));
                List<String> lore = new ArrayList<>();
                lore.add(color(cat.getDescription1()));
                lore.add(color(cat.getDescription2()));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to view history"));
                im.setLore(lore);
                is.setItemMeta(im);
                gui.setItem(extraSlots[i - slots.length], is);
            }
        }

        p.openInventory(gui);
    }

    private void openSellHistory(Player p, int page) {
        UUID uid = p.getUniqueId();
        Category cat = viewingCategory.get(uid);
        if (cat == null) {
            openCategoryMenu(p);
            return;
        }

        List<AggEntry> entries = loadAggregatedHistory(uid, cat);
        boolean byName = sortByName.getOrDefault(uid, false);
        if (byName) {
            entries.sort(Comparator.comparing(e -> e.mat.name()));
        } else {
            entries.sort(Comparator.comparingLong((AggEntry e) -> e.amount).reversed());
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / PAGE_SIZE));
        page = Math.max(1, Math.min(page, totalPages));

        String title = ChatColor.translateAlternateColorCodes('&', "&8ѕᴇʟʟ ʜɪѕᴛᴏʀʏ &7(" + cat.getDisplayName() + "&7)");
        if (title.length() > 32)
            title = title.substring(0, 32);
        Inventory gui = Bukkit.createInventory(null, 54, title);

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            AggEntry ae = entries.get(i);
            ItemStack is = new ItemStack(ae.mat, 1);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(ChatColor.WHITE + capitalize(ae.mat.name()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Total price: " + ChatColor.GREEN + formatCurrencyCompact(ae.total));
            lore.add(ChatColor.WHITE + "Total amount: " + ChatColor.WHITE + formatNumberCompact(ae.amount));
            im.setLore(lore);
            is.setItemMeta(im);
            gui.setItem(slot++, is);
        }

        // Bottom controls
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aʙᴀᴄᴋ ᴘᴀɢᴇ"));
            pm.setLore(Collections
                    .singletonList(ChatColor.translateAlternateColorCodes('&', "&fClick to go to the previous page")));
            prev.setItemMeta(pm);
            gui.setItem(45, prev);
        }

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cʙᴀᴄᴋ то ᴄᴀᴛᴇɢᴏʀɪᴇѕ"));
        bm.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', "&fClick to return to menu")));
        back.setItemMeta(bm);
        gui.setItem(48, back);

        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta am = anvil.getItemMeta();
        am.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aѕᴏʀᴛ ʜɪsᴛᴏʀʏ"));
        String modeText = byName ? "Name" : "Amount";
        am.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&fClick to sort by &7" + modeText)));
        anvil.setItemMeta(am);
        gui.setItem(49, anvil);

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aɴᴇхᴛ ᴘᴀɢᴇ"));
            nm.setLore(Collections
                    .singletonList(ChatColor.translateAlternateColorCodes('&', "&fClick to go to the next page")));
            next.setItemMeta(nm);
            gui.setItem(53, next);
        }

        p.openInventory(gui);
    }

    private List<AggEntry> loadAggregatedHistory(UUID uid, Category filter) {
        List<AggEntry> out = new ArrayList<>();
        try {
            File histDir = new File(plugin.getDataFolder(), "economy/sell/history");
            if (!histDir.exists())
                return out;
            File f = new File(histDir, uid.toString() + "-history.db");
            if (!f.exists())
                return out;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            List<?> list = cfg.getMapList("history");
            Map<Material, AggEntry> agg = new HashMap<>();
            for (Object o : list) {
                if (!(o instanceof Map))
                    continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object itemO = m.get("item");
                Object amountO = m.get("amount");
                Object totalO = m.get("total");
                if (itemO == null)
                    continue;
                String name = itemO.toString();
                try {
                    Material mat = Material.valueOf(name);

                    // Filter by category
                    if (filter != null && plugin.getPrismSell() != null) {
                        Category cat = plugin.getPrismSell().getPricesManager().getCategory(new ItemStack(mat));
                        if (cat != filter)
                            continue;
                    }

                    long amount = 0;
                    double total = 0.0;
                    try {
                        amount = Long.parseLong(amountO.toString());
                    } catch (Throwable ignored) {
                    }
                    try {
                        total = Double.parseDouble(totalO.toString());
                    } catch (Throwable ignored) {
                    }
                    AggEntry e = agg.get(mat);
                    if (e == null) {
                        e = new AggEntry();
                        e.mat = mat;
                        e.amount = amount;
                        e.total = total;
                        agg.put(mat, e);
                    } else {
                        e.amount += amount;
                        e.total += total;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            out.addAll(agg.values());
        } catch (Throwable ignored) {
        }
        return out;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null || event.getView().getTitle() == null)
            return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.toLowerCase().startsWith("ѕᴇʟʟ ʜɪѕᴛᴏʀʏ"))
            return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p))
            return;

        int slot = event.getRawSlot();
        if (slot < 0)
            return;

        // Handle Category Menu
        if (title.equalsIgnoreCase("ѕᴇʟʟ ʜɪѕᴛᴏʀʏ")) {
            Category[] cats = Category.values();
            int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };
            for (int i = 0; i < Math.min(cats.length, slots.length); i++) {
                if (slot == slots[i]) {
                    viewingCategory.put(p.getUniqueId(), cats[i]);
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                    openSellHistory(p, 1);
                    return;
                }
            }
            return;
        }

        // Handle History View
        int page = 1;
        try {
            int a = title.indexOf("(page "); // legacy check, but let's just use current title format
            if (a >= 0) {
                String sub = title.substring(a + 6).trim();
                if (sub.endsWith(")"))
                    sub = sub.substring(0, sub.length() - 1);
                page = Integer.parseInt(sub);
            }
        } catch (Throwable ignored) {
        }

        // If player clicked inside item area
        if (slot >= 0 && slot < PAGE_SIZE) {
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            return;
        }

        // back to categories
        if (slot == 48) {
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            openCategoryMenu(p);
            return;
        }

        // prev
        if (slot == 45) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.ARROW) {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                openSellHistory(p, Math.max(1, page - 1));
            }
            return;
        }
        // next
        if (slot == 53) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.ARROW) {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                openSellHistory(p, page + 1);
            }
            return;
        }
        // sort toggle
        if (slot == 49) {
            UUID uid = p.getUniqueId();
            boolean nextSort = !sortByName.getOrDefault(uid, false);
            sortByName.put(uid, nextSort);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            openSellHistory(p, 1);
            return;
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // no-op (keeps GUI behaviour simple)
    }

    private String capitalize(String s) {
        s = s.replace('_', ' ').toLowerCase();
        if (s.length() == 0)
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatCurrencyCompact(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v))
            return "$0";
        String sign = v < 0 ? "-" : "";
        v = Math.abs(v);
        if (v >= 1_000_000_000)
            return sign + String.format("$%.2fB", v / 1_000_000_000.0);
        if (v >= 1_000_000)
            return sign + String.format("$%.2fM", v / 1_000_000.0);
        if (v >= 1_000)
            return sign + String.format("$%.2fK", v / 1_000.0);
        return sign + String.format("$%.2f", v);
    }

    private String formatNumberCompact(long n) {
        long v = Math.abs(n);
        String sign = n < 0 ? "-" : "";
        if (v >= 1_000_000_000)
            return sign + String.format("%.2fB", v / 1_000_000_000.0);
        if (v >= 1_000_000)
            return sign + String.format("%.2fM", v / 1_000_000.0);
        if (v >= 1_000)
            return sign + String.format("%.2fK", v / 1_000.0);
        return sign + Long.toString(v);
    }

    public static double getTotalSold(UUID uid) {
        double total = 0.0;
        try {
            File dataFolder = new File("plugins/PrismSurvival"); // Fallback
            org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("PrismSurvival");
            if (p != null)
                dataFolder = p.getDataFolder();

            File f = new File(dataFolder, "economy/sell/history/" + uid.toString() + "-history.db");
            if (!f.exists())
                return 0.0;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            List<?> list = cfg.getMapList("history");
            if (list == null)
                return 0.0;
            for (Object o : list) {
                if (!(o instanceof Map))
                    continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object totalO = m.get("total");
                if (totalO != null) {
                    try {
                        total += Double.parseDouble(totalO.toString());
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return total;
    }
}