package com.h2ph.commands.admin.economy;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor, Listener {

    private final PrismSurvival plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> playerSearches = new HashMap<>();

    // Cache leaderboard data to avoid repeated loads
    private List<PlayerDataManager.LeaderboardEntry> cachedEntries = null;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    public BaltopCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        playerSearches.remove(player.getUniqueId());

        // Open loading GUI immediately
        openLoadingGUI(player);

        // Load data async and update GUI
        loadDataAsync(player, 1);
        return true;
    }

    private void openLoadingGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', "&8ᴍᴏѕᴛ ᴍᴏɴᴇʏ (loading...)");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Add loading indicator
        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta meta = loading.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Loading...");
            loading.setItemMeta(meta);
        }
        gui.setItem(22, loading);

        player.openInventory(gui);
    }

    private void loadDataAsync(Player player, int page) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            // Check cache first
            List<PlayerDataManager.LeaderboardEntry> allEntries;
            if (cachedEntries != null && (System.currentTimeMillis() - lastCacheTime < CACHE_DURATION)) {
                allEntries = cachedEntries;
            } else {
                // Load from storage (this is the expensive part)
                allEntries = plugin.getPlayerDataManager().getTopMoney(10000); // Limit to 10000 players
                cachedEntries = allEntries;
                lastCacheTime = System.currentTimeMillis();
            }

            // Apply search filter
            String searchQuery = playerSearches.get(player.getUniqueId());
            List<PlayerDataManager.LeaderboardEntry> displayEntries;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                displayEntries = allEntries.stream()
                        .filter(e -> e.name.toLowerCase().contains(searchQuery.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                displayEntries = allEntries;
            }

            // Build GUI items async (everything except opening inventory)
            int itemsPerPage = 45;
            int totalPlayers = displayEntries.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / itemsPerPage);

            if (totalPages == 0)
                totalPages = 1;
            int finalPage = Math.max(1, Math.min(page, totalPages));

            int startIndex = (finalPage - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalPlayers);

            // Pre-build all items
            List<ItemStack> items = new ArrayList<>();
            for (int i = startIndex; i < endIndex; i++) {
                PlayerDataManager.LeaderboardEntry entry = displayEntries.get(i);
                items.add(createHeadItem(entry, allEntries.indexOf(entry) + 1));
            }

            // Find self entry
            PlayerDataManager.LeaderboardEntry selfEntry = allEntries.stream()
                    .filter(e -> e.uuid.equals(player.getUniqueId()))
                    .findFirst()
                    .orElse(null);
            int selfRank = selfEntry != null ? allEntries.indexOf(selfEntry) + 1 : -1;

            // Switch to main thread to open inventory
            int finalTotalPages = totalPages;
            plugin.getSchedulerAdapter().runTask(() -> {
                if (!player.isOnline())
                    return;

                playerPages.put(player.getUniqueId(), finalPage);

                String title = ChatColor.translateAlternateColorCodes('&', "&8ᴍᴏѕᴛ ᴍᴏɴᴇʏ (page " + finalPage + ")");
                Inventory gui = Bukkit.createInventory(null, 54, title);

                // Add heads
                int slot = 0;
                for (ItemStack item : items) {
                    gui.setItem(slot++, item);
                }

                // Navigation
                if (finalPage > 1) {
                    gui.setItem(45, createKeyItem(Material.ARROW, "&aPrevious Page", "&7Click to switch page"));
                }
                if (finalPage < finalTotalPages) {
                    gui.setItem(53, createKeyItem(Material.ARROW, "&aNext Page", "&7Click to switch page"));
                }

                // Self head
                ItemStack selfHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta selfMeta = (SkullMeta) selfHead.getItemMeta();
                if (selfMeta != null) {
                    selfMeta.setOwningPlayer(player);
                    selfMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a" + player.getName()));
                    List<String> selfLore = new ArrayList<>();

                    double balance;
                    String rankDisplay;

                    if (selfEntry != null) {
                        balance = selfEntry.value;
                        rankDisplay = "&a (#" + selfRank + ")";
                    } else {
                        // Not in top list, fetch directly
                        balance = 0.0;
                        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                            org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin
                                    .getServer()
                                    .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                            if (rsp != null && rsp.getProvider() != null) {
                                balance = rsp.getProvider().getBalance(player);
                            }
                        } else {
                            // Fallback to internal
                            balance = plugin.getPlayerDataManager().get(player.getUniqueId()).getMoney();
                        }
                        rankDisplay = "&7 (Not in top " + allEntries.size() + ")";
                    }

                    selfLore.add(ChatColor.translateAlternateColorCodes('&',
                            "&fMoney:&7 $" + formatNumber(balance) + rankDisplay));

                    selfMeta.setLore(selfLore);
                    selfHead.setItemMeta(selfMeta);
                }
                gui.setItem(48, selfHead);

                // Refresh button
                gui.setItem(49, createKeyItem(Material.EMERALD, "&aᴍᴏѕᴛ ᴍᴏɴᴇʏ", "&fClick to refresh"));

                // Search button
                gui.setItem(50, createKeyItem(Material.OAK_SIGN, "&aѕᴇᴀʀᴄʜ", "&fClick to search for players"));

                player.openInventory(gui);
            });
        });
    }

    private ItemStack createHeadItem(PlayerDataManager.LeaderboardEntry entry, int rank) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                // Use UUID to get the offline player and properly load skin texture
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid);
                meta.setOwningPlayer(offlinePlayer);

                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a" + entry.name));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&fMoney:&7 $" + formatNumber(entry.value) + "&a (#" + rank + ")"));
                meta.setLore(lore);
                head.setItemMeta(meta);
            } catch (Exception e) {
                // Safety catch to prevent one bad head from breaking the entire GUI
                plugin.getLogger().warning(
                        "Failed to create head item for " + entry.name + " (" + entry.uuid + "): " + e.getMessage());
            }
        }

        return head;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (!title.startsWith(ChatColor.translateAlternateColorCodes('&', "&8ᴍᴏѕᴛ ᴍᴏɴᴇʏ"))) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();

        if (clickedInv == null || !clickedInv.equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (event.getSlot() < 45) {
            if (item.getType() == Material.PLAYER_HEAD) {
                playSound(player, Sound.BLOCK_TRIPWIRE_CLICK_ON);
            }
            return;
        }

        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

        if (event.getSlot() == 45 && item.getType() == Material.ARROW) {
            loadDataAsync(player, currentPage - 1);
            playSound(player, Sound.UI_BUTTON_CLICK);
        } else if (event.getSlot() == 53 && item.getType() == Material.ARROW) {
            loadDataAsync(player, currentPage + 1);
            playSound(player, Sound.UI_BUTTON_CLICK);
        } else if (event.getSlot() == 49 && item.getType() == Material.EMERALD) {
            playerSearches.remove(player.getUniqueId());
            cachedEntries = null; // Force refresh
            loadDataAsync(player, 1);
            playSound(player, Sound.UI_BUTTON_CLICK);
        } else if (event.getSlot() == 50 && item.getType() == Material.OAK_SIGN) {
            player.closeInventory();
            plugin.getSignInput().getSearchInput(player, (input) -> {
                String term = input.trim();
                if (!term.isEmpty()) {
                    playerSearches.put(player.getUniqueId(), term);
                }
                loadDataAsync(player, 1);
            });
            playSound(player, Sound.UI_BUTTON_CLICK);
        }
    }

    private ItemStack createKeyItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> l = new ArrayList<>();
            l.add(ChatColor.translateAlternateColorCodes('&', lore));
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void playSound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    private static final java.text.DecimalFormat DF = new java.text.DecimalFormat("#.#");

    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "T");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "B");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "M");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "k");
        } else {
            return DF.format(Math.floor(number * 10) / 10.0);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }
}
