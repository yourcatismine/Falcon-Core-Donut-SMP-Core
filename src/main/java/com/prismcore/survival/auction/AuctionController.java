package com.prismcore.survival.auction;

import java.io.File;
import java.io.IOException;
import com.h2ph.PrismSurvival;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

public class AuctionController {
    private final PrismSurvival plugin;
    private AuctionManager auctionManager;
    private TransactionManager transactionManager;
    private GUIListener guiListener;
    private File storageFile;
    private FileConfiguration storageConfig;
    private File filterFile;
    private FileConfiguration filterConfig;
    private File configFile;
    private FileConfiguration config;

    public AuctionController(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        this.loadConfig();
        this.setupFilterFile();
        this.auctionManager = new AuctionManager(this);
        this.transactionManager = new TransactionManager(this);
        boolean useVault = this.config.getBoolean("settings.use-vault", true);
        EconomyHandler.setup(plugin, useVault);
        // Logging is handled inside EconomyHandler setup now
        this.setupStorageFile();
        this.auctionManager.loadFromConfig();
        this.transactionManager.loadFromConfig();
        AHCommand ahCmd = new AHCommand(this);
        plugin.getCommand("ah").setExecutor(ahCmd);
        plugin.getCommand("ah").setTabCompleter(ahCmd);
        AdminCommand adminCmd = new AdminCommand(this);
        if (plugin.getCommand("auction") != null) {
            plugin.getCommand("auction").setExecutor((CommandExecutor) adminCmd);
            plugin.getCommand("auction").setTabCompleter((TabCompleter) adminCmd);
        } else {
            plugin.getLogger().severe("Command 'auction' not found in plugin.yml!");
        }
        this.guiListener = new GUIListener(this);
        plugin.getServer().getPluginManager().registerEvents((Listener) this.guiListener, plugin);
        this.startAutoSaveTask();
        plugin.getLogger().info("Auction system enabled.");
    }

    public void disable() {
        this.saveAllData();
        plugin.getLogger().info("Auction system disabled.");
    }

    private void loadConfig() {
        this.configFile = new File(plugin.getDataFolder(), "economy/auction/config.yml");
        if (!this.configFile.exists()) {
            this.configFile.getParentFile().mkdirs();
            plugin.saveResource("economy/auction/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            loadConfig();
        }
        return this.config;
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void setupStorageFile() {
        this.storageFile = new File(plugin.getDataFolder(), "economy/auction/storage.yml");
        if (!this.storageFile.exists()) {
            this.storageFile.getParentFile().mkdirs();
            plugin.saveResource("economy/auction/storage.yml", false);
        }
        this.storageConfig = YamlConfiguration.loadConfiguration(this.storageFile);
    }

    private void setupFilterFile() {
        this.filterFile = new File(plugin.getDataFolder(), "economy/auction/filter.yml");
        if (!this.filterFile.exists()) {
            this.filterFile.getParentFile().mkdirs();
            plugin.saveResource("economy/auction/filter.yml", false);
        }
        this.filterConfig = YamlConfiguration.loadConfiguration(this.filterFile);
    }

    public void reloadAllConfigs() {
        this.reloadConfig();
        this.setupFilterFile();
    }

    public FileConfiguration getFilterConfig() {
        return this.filterConfig;
    }

    public AuctionManager getAuctionManager() {
        return this.auctionManager;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public FileConfiguration getStorageConfig() {
        return this.storageConfig;
    }

    public void saveStorageFile() {
        if (this.storageConfig == null || this.storageFile == null) {
            return;
        }
        try {
            this.storageConfig.save(this.storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save storage.yml: " + e.getMessage());
        }
    }

    public void startAutoSaveTask() {
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            this.saveAllData();
        }, 12000L, 12000L); // Default 10 minutes (12000 ticks)
    }

    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> activeTasks = new java.util.concurrent.ConcurrentHashMap<>();

    public void startUpdateTask(org.bukkit.entity.Player player) {
        if (activeTasks.containsKey(player.getUniqueId()))
            return;

        org.bukkit.NamespacedKey auctionKey = new org.bukkit.NamespacedKey(plugin, "auction-expire");
        org.bukkit.NamespacedKey txKey = new org.bukkit.NamespacedKey(plugin, "transaction-timestamp");

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, () -> {
            if (!player.isOnline()) {
                stopUpdateTask(player);
                return;
            }
            org.bukkit.inventory.InventoryView view = player.getOpenInventory();
            if (view == null)
                return;
            org.bukkit.inventory.Inventory top = view.getTopInventory();
            org.bukkit.inventory.InventoryHolder holder = top.getHolder();

            // Should match holders where we expect live updates
            if (!(holder instanceof GUIHandler.MainHolder
                    || holder instanceof GUIHandler.YourItemsHolder
                    || holder instanceof GUIHandler.TransactionsHolder
                    || holder instanceof GUIHandler.AdminPlayerDetailsHolder
                    || holder instanceof GUIHandler.AdminTransactionsHolder)) {
                return;
            }

            for (int i = 0; i < top.getSize(); i++) {
                org.bukkit.inventory.ItemStack item = top.getItem(i);
                if (item == null || !item.hasItemMeta())
                    continue;
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

                if (pdc.has(auctionKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    long expireTime = pdc.get(auctionKey, org.bukkit.persistence.PersistentDataType.LONG);
                    long remaining = expireTime - System.currentTimeMillis();

                    if (remaining <= 0) {
                        // If it's the main GUI or Admin Details, hide the item when it expires
                        if (holder instanceof GUIHandler.MainHolder
                                || holder instanceof GUIHandler.AdminPlayerDetailsHolder) {
                            top.setItem(i, null);
                            continue;
                        }
                    }

                    String timeStr = FormatUtils.formatTime((int) (remaining / 1000));
                    if (meta.hasLore()) {
                        java.util.List<String> lore = meta.getLore();
                        boolean changed = false;
                        for (int j = 0; j < lore.size(); ++j) {
                            if (lore.get(j).contains("Time left:")) {
                                String newLine = Utils.formatColors("&fTime left: &#34ee80" + timeStr);
                                if (!lore.get(j).equals(newLine)) {
                                    lore.set(j, newLine);
                                    changed = true;
                                }
                            }
                        }
                        if (changed) {
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                    }
                } else if (pdc.has(txKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    long soldTime = pdc.get(txKey, org.bukkit.persistence.PersistentDataType.LONG);
                    long elapsedSeconds = (System.currentTimeMillis() - soldTime) / 1000L;
                    String timeAgo = FormatUtils.formatTime((int) elapsedSeconds);
                    if (meta.hasLore()) {
                        java.util.List<String> lore = meta.getLore();
                        boolean changed = false;
                        for (int j = 0; j < lore.size(); ++j) {
                            String check = lore.get(j);
                            // Check for standard "Sold:" logic
                            if (check.contains("Sold:")) {
                                String newLine = Utils.formatColors("&fSold: &7" + timeAgo + " ago");
                                if (!check.equals(newLine)) {
                                    lore.set(j, newLine);
                                    changed = true;
                                }
                            }
                            // Check for Admin "ago" logic (line ends with " ago")
                            else if (check.endsWith(" ago")) {
                                String newLine = Utils.formatColors("&a" + timeAgo + " ago");
                                if (!check.equals(newLine)) {
                                    lore.set(j, newLine);
                                    changed = true;
                                }
                            }
                        }
                        if (changed) {
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        }, 20L, 20L);

        activeTasks.put(player.getUniqueId(), task);
    }

    public void stopUpdateTask(org.bukkit.entity.Player player) {
        org.bukkit.scheduler.BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void saveAllData() {
        if (this.auctionManager != null) {
            this.auctionManager.saveToConfig();
        }
        if (this.transactionManager != null) {
            this.transactionManager.saveToConfig();
        }
        this.saveStorageFile();
    }

    public PrismSurvival getPlugin() {
        return plugin;
    }
}
