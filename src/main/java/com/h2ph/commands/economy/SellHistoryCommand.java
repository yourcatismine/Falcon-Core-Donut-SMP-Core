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
        openSellHistory(p, 1);
        return true;
    }

    private static class AggEntry {
        Material mat;
        long amount;
        double total;
    }

    private void openSellHistory(Player p, int page) {
        UUID uid = p.getUniqueId();

        if (plugin.getPrismSell() != null && plugin.getPrismSell().getDatabaseManager() != null) {
            plugin.getPrismSell().getDatabaseManager().getSellHistoryAsync(uid, (Map<String, double[]> result) -> {
                List<AggEntry> entries = new ArrayList<>();
                for (Map.Entry<String, double[]> entry : result.entrySet()) {
                    try {
                        Material mat = Material.valueOf(entry.getKey());
                        AggEntry ae = new AggEntry();
                        ae.mat = mat;
                        ae.amount = (long) entry.getValue()[0];
                        ae.total = entry.getValue()[1];
                        entries.add(ae);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                boolean byName = sortByName.getOrDefault(uid, false);
                if (byName) {
                    entries.sort(Comparator.comparing(e -> e.mat.name()));
                } else {
                    entries.sort(Comparator.comparingLong((AggEntry e) -> e.amount).reversed());
                }

                int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / PAGE_SIZE));
                int finalPage = Math.max(1, Math.min(page, totalPages));

                String title = ChatColor.translateAlternateColorCodes('&', "&8ѕᴇʟʟ ʜɪѕᴛᴏʀʏ");
                Inventory gui = Bukkit.createInventory(null, 54, title);

                int start = (finalPage - 1) * PAGE_SIZE;
                int end = Math.min(entries.size(), start + PAGE_SIZE);
                int slot = 0;
                for (int i = start; i < end; i++) {
                    AggEntry ae = entries.get(i);
                    ItemStack is = new ItemStack(ae.mat, 1);
                    ItemMeta im = is.getItemMeta();
                    im.setDisplayName(ChatColor.WHITE + capitalize(ae.mat.name()));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                            "&fTotal price: &a$" + formatCurrencyCompact(ae.total)));
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                            "&fTotal amount: " + formatNumberCompact(ae.amount)));
                    im.setLore(lore);
                    is.setItemMeta(im);
                    gui.setItem(slot++, is);
                }

                if (finalPage > 1) {
                    ItemStack prev = new ItemStack(Material.ARROW);
                    ItemMeta pm = prev.getItemMeta();
                    pm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dʙᴀᴄᴋ"));
                    pm.setLore(Collections
                            .singletonList(
                                    ChatColor.translateAlternateColorCodes('&', "&fClick to go to previous page")));
                    prev.setItemMeta(pm);
                    gui.setItem(45, prev);
                }

                ItemStack anvil = new ItemStack(Material.ANVIL);
                ItemMeta am = anvil.getItemMeta();
                am.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dѕᴇʟʟ ʜɪѕᴛᴏʀʏ"));
                String modeText = byName ? "&7(By name)" : "&7(Amount)";
                am.setLore(Arrays.asList(
                        ChatColor.translateAlternateColorCodes('&', "&fClick to sort"),
                        "",
                        ChatColor.translateAlternateColorCodes('&', modeText)));
                anvil.setItemMeta(am);
                gui.setItem(49, anvil);

                if (finalPage < totalPages) {
                    ItemStack next = new ItemStack(Material.ARROW);
                    ItemMeta nm = next.getItemMeta();
                    nm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dɴᴇхᴛ"));
                    nm.setLore(Collections
                            .singletonList(
                                    ChatColor.translateAlternateColorCodes('&', "&fClick to go to the next page")));
                    next.setItemMeta(nm);
                    gui.setItem(53, next);
                }

                plugin.getSchedulerAdapter().runEntityTask(p, () -> p.openInventory(gui));
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null || event.getView().getTitle() == null)
            return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.toLowerCase().startsWith("ѕᴇʟʟ ʜɪѕᴛᴏʀʏ"))
            return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null)
            return;

        if (clickedInv.equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
        } else {
            if (event.isShiftClick()
                    || event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getWhoClicked() instanceof Player p))
            return;

        int slot = event.getRawSlot();
        if (slot < 0)
            return;

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
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            }
            return;
        }

        // prev
        if (slot == 45) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.ARROW) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                openSellHistory(p, Math.max(1, page - 1));
            }
            return;
        }
        // next
        if (slot == 53) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.ARROW) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
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
            return "0";
        String sign = v < 0 ? "-" : "";
        v = Math.abs(v);
        if (v >= 1_000_000_000)
            return sign + formatDecim(v / 1_000_000_000.0) + "B";
        if (v >= 1_000_000)
            return sign + formatDecim(v / 1_000_000.0) + "M";
        if (v >= 1_000)
            return sign + formatDecim(v / 1_000.0) + "K";
        return sign + formatDecim(v);
    }

    private String formatNumberCompact(long n) {
        long v = Math.abs(n);
        String sign = n < 0 ? "-" : "";
        if (v >= 1_000_000_000)
            return sign + formatDecim(v / 1_000_000_000.0) + "B";
        if (v >= 1_000_000)
            return sign + formatDecim(v / 1_000_000.0) + "M";
        if (v >= 1_000)
            return sign + formatDecim(v / 1_000.0) + "K";
        return sign + Long.toString(v);
    }

    private String formatDecim(double val) {
        String num = String.format(java.util.Locale.US, "%.2f", val);
        if (num.contains(".")) {
            num = num.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        return num;
    }

    public static double getTotalSold(UUID uid) {
        PrismSurvival plugin = PrismSurvival.getInstance();
        if (plugin != null && plugin.getPrismSell() != null) {
            return plugin.getPrismSell().getPlayerDataManager().getPlayerData(uid).getSellMade();
        }
        return 0.0;
    }
}