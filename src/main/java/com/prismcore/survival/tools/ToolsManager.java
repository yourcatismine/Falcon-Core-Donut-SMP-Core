package com.prismcore.survival.tools;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.h2ph.PrismSurvival;

public class ToolsManager {

    private final PrismSurvival plugin;
    private FileConfiguration config;
    private File configFile;

    public static NamespacedKey EXPIRY_KEY;
    public static NamespacedKey REMAINING_KEY;
    public static NamespacedKey MULTI_KEY;
    public static NamespacedKey BOOSTER_KEY;
    public static NamespacedKey AUCTION_PAUSED_KEY;
    public static NamespacedKey LAST_UPDATE_KEY;

    private static ToolsManager instance;
    private ContainerScanner containerScanner;

    public ToolsManager(PrismSurvival plugin) {
        this.plugin = plugin;
        instance = this;
        EXPIRY_KEY = new NamespacedKey(plugin, "tool-expiry");
        REMAINING_KEY = new NamespacedKey(plugin, "tool-remaining");
        MULTI_KEY = new NamespacedKey(plugin, "is-multitool");
        BOOSTER_KEY = new NamespacedKey(plugin, "is-shardbooster");
        AUCTION_PAUSED_KEY = new NamespacedKey(plugin, "auction-paused");
        LAST_UPDATE_KEY = new NamespacedKey(plugin, "last-lore-update");
        loadConfig();
        this.containerScanner = new ContainerScanner(plugin, this);
        registerListeners();
        startUpdateTask();
    }

    public static ToolsManager getInstance() {
        return instance;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "survival/tools/config.yml");
        if (!configFile.exists()) {
            // Ensure the directory exists
            configFile.getParentFile().mkdirs();
            // Try to save default resource if it exists in jar, otherwise create empty or
            // default
            if (plugin.getResource("survival/tools/config.yml") != null) {
                plugin.saveResource("survival/tools/config.yml", false);
            } else {
                try {
                    configFile.createNewFile();
                    // Load empty config to act upon
                    config = YamlConfiguration.loadConfiguration(configFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create config for tools", e);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new DrillBlockBreakListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AxeBlockBreakListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ShovelBlockBreakListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DrillClickListener(this, plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MultitoolBlockBreakListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DrillInventoryListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BucketUseListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ShardBoosterListener(this, plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SellAxeListener(this), plugin);
    }

    private void startUpdateTask() {
        long playerIntervalSeconds = getConfig().getLong("drill.update-interval", 30L);
        long containerIntervalSeconds = getConfig().getLong("container-scan-interval", 60L);

        long playerIntervalTicks = playerIntervalSeconds * 20L;
        if (playerIntervalTicks <= 0)
            playerIntervalTicks = 600L; // 30 seconds default

        long containerIntervalTicks = containerIntervalSeconds * 20L;
        if (containerIntervalTicks <= 0)
            containerIntervalTicks = 1200L; // 60 seconds default

        // Task 1: Update online player inventories (check expiration + update lore)
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getSchedulerAdapter().runEntityTask(p, () -> {
                    containerScanner.scanInventory(p.getInventory(), p.getLocation(), p);
                });
            }
        }, playerIntervalTicks, playerIntervalTicks);

        // Note: Container scanning (chests, etc.) has been removed to avoid Folia
        // threading issues
        // Containers don't need real-time countdown updates - players see updated lore
        // when they open them

        // Task 2: Scan online player ender chests
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getSchedulerAdapter().runEntityTask(p, () -> {
                    containerScanner.scanInventory(p.getEnderChest(), null, p);
                });
            }
        }, playerIntervalTicks, playerIntervalTicks);
    }

    public void updatePlayerTools(Player player) {
        // Legacy method - now uses ContainerScanner
        containerScanner.scanInventory(player.getInventory(), player.getLocation(), player);
    }

    public void giveTool(Player player, String key, long overrideTimer) {
        org.bukkit.Material mat;
        org.bukkit.configuration.ConfigurationSection cfg = getConfig().getConfigurationSection(key);
        if (cfg == null) {
            player.sendMessage(Utils.formatColors("&cError: " + key + " section missing in config.yml"));
            return;
        }
        boolean useCountdown = cfg.getBoolean("use-countdown", true);
        long timerSec = (overrideTimer > 0) ? overrideTimer : cfg.getLong("timer", 0L);
        // NO Expiry calculation here. Just store raw seconds.

        try {
            mat = org.bukkit.Material.valueOf((String) cfg.getString("material", "").toUpperCase());
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Utils.formatColors("&cInvalid material for " + key));
            return;
        }
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Utils.formatColors("&cFailed to create " + key));
            return;
        }
        if (cfg.contains("enchantments")) {
            for (String encKey : cfg.getConfigurationSection("enchantments").getKeys(false)) {
                org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment
                        .getByName((String) encKey.toUpperCase());
                int lvl = cfg.getInt("enchantments." + encKey, 1);
                if (e == null)
                    continue;
                meta.addEnchant(e, lvl, true);
            }
        }
        if (cfg.getBoolean("hide-enchantments", false)) {
            meta.addItemFlags(new org.bukkit.inventory.ItemFlag[] { org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS });
        }
        if (cfg.isString("display-name")) {
            meta.setDisplayName(Utils.formatColors(cfg.getString("display-name")));
        }
        if (cfg.isList("lore")) {
            java.util.List<String> rawLore = cfg.getStringList("lore");
            java.util.List<String> finalLore = rawLore.stream().map(line -> {
                if (useCountdown) {
                    String initCountdown = Utils.formatDuration(timerSec);
                    return Utils.formatColors(line.replace("%countdown%", initCountdown));
                }
                return Utils.formatColors(line);
            }).toList();
            meta.setLore(finalLore);
        }
        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            ((org.bukkit.inventory.meta.Damageable) meta).setDamage(0);
        }
        // Set Expiration Timestamp (timestamp-based system)
        long expiryTimestamp = System.currentTimeMillis() + (timerSec * 1000L);
        meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG,
                expiryTimestamp);
        item.setItemMeta(meta);
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack[] { item });
    }

    public void giveMultiTool(Player player, long overrideTimer) {
        org.bukkit.Material baseMat;
        org.bukkit.configuration.ConfigurationSection cfg = getConfig().getConfigurationSection("multitool");
        if (cfg == null) {
            player.sendMessage(Utils.formatColors("&cError: multitool section missing in config.yml"));
            return;
        }
        boolean useCountdown = cfg.getBoolean("use-countdown", true);
        long timerSec = (overrideTimer > 0) ? overrideTimer : cfg.getLong("timer", 0L);

        try {
            baseMat = org.bukkit.Material.valueOf((String) cfg.getString("material", "").toUpperCase());
        } catch (IllegalArgumentException e) {
            baseMat = org.bukkit.Material.DIAMOND_HOE;
        }
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(baseMat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Utils.formatColors("&cFailed to create multitool"));
            return;
        }
        if (cfg.contains("enchantments")) {
            for (String encKey : cfg.getConfigurationSection("enchantments").getKeys(false)) {
                org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment
                        .getByName((String) encKey.toUpperCase());
                int lvl = cfg.getInt("enchantments." + encKey, 1);
                if (e == null)
                    continue;
                meta.addEnchant(e, lvl, true);
            }
        }
        if (cfg.getBoolean("hide-enchantments", false)) {
            meta.addItemFlags(new org.bukkit.inventory.ItemFlag[] { org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS });
        }
        if (cfg.isString("display-name")) {
            meta.setDisplayName(Utils.formatColors(cfg.getString("display-name")));
        }
        if (cfg.isList("lore")) {
            java.util.List<String> rawLore = cfg.getStringList("lore");
            java.util.List<String> finalLore = rawLore.stream().map(line -> {
                if (useCountdown) {
                    String initCountdown = Utils.formatDuration(timerSec);
                    return Utils.formatColors(line.replace("%countdown%", initCountdown));
                }
                return Utils.formatColors(line);
            }).toList();
            meta.setLore(finalLore);
        }
        meta.getPersistentDataContainer().set(ToolsManager.MULTI_KEY, org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);
        // Set Expiration Timestamp (timestamp-based system)
        long expiryTimestamp = System.currentTimeMillis() + (timerSec * 1000L);
        meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG,
                expiryTimestamp);
        item.setItemMeta(meta);
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack[] { item });
    }

    public void giveBucket(Player player, long overrideTimer) {
        org.bukkit.Material baseMat;
        org.bukkit.configuration.ConfigurationSection cfg = getConfig().getConfigurationSection("bucket");
        if (cfg == null) {
            player.sendMessage(Utils.formatColors("&cError: bucket section missing in config.yml"));
            return;
        }
        boolean useCountdown = cfg.getBoolean("use-countdown", true);
        long timerSec = (overrideTimer > 0) ? overrideTimer : cfg.getLong("timer", 0L);

        try {
            baseMat = org.bukkit.Material.valueOf((String) cfg.getString("material", "BUCKET").toUpperCase());
        } catch (IllegalArgumentException e) {
            baseMat = org.bukkit.Material.BUCKET;
        }
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(baseMat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Utils.formatColors("&cFailed to create bucket"));
            return;
        }
        if (cfg.contains("enchantments")) {
            for (String encKey : cfg.getConfigurationSection("enchantments").getKeys(false)) {
                org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment
                        .getByName((String) encKey.toUpperCase());
                int lvl = cfg.getInt("enchantments." + encKey, 1);
                if (e == null)
                    continue;
                meta.addEnchant(e, lvl, true);
            }
        }
        if (cfg.getBoolean("hide-enchantments", false)) {
            meta.addItemFlags(new org.bukkit.inventory.ItemFlag[] { org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS });
        }
        if (cfg.isString("display-name")) {
            meta.setDisplayName(Utils.formatColors(cfg.getString("display-name")));
        }
        if (cfg.isList("lore")) {
            java.util.List<String> rawLore = cfg.getStringList("lore");
            java.util.List<String> finalLore = rawLore.stream().map(line -> {
                if (useCountdown) {
                    String initCountdown = Utils.formatDuration(timerSec);
                    return Utils.formatColors(line.replace("%countdown%", initCountdown));
                }
                return Utils.formatColors(line);
            }).toList();
            meta.setLore(finalLore);
        }
        // Set Expiration Timestamp (timestamp-based system)
        long expiryTimestamp = System.currentTimeMillis() + (timerSec * 1000L);
        meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG,
                expiryTimestamp);
        item.setItemMeta(meta);
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack[] { item });
    }

    public void giveShardBooster(Player player, long overrideTimer) {
        org.bukkit.configuration.ConfigurationSection cfg = getConfig().getConfigurationSection("shardbooster");
        if (cfg == null) {
            player.sendMessage(Utils.formatColors("&cError: shardbooster section missing in config.yml"));
            return;
        }
        long timerSec = (overrideTimer > 0) ? overrideTimer : cfg.getLong("timer", 86400L);

        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Utils.formatColors("&cFailed to create shard booster"));
            return;
        }

        // Set potion to water bottle base
        meta.setBasePotionType(org.bukkit.potion.PotionType.WATER);

        // Set custom color
        String colorHex = cfg.getString("potion-color", "8B5CF6");
        try {
            int rgb = Integer.parseInt(colorHex, 16);
            meta.setColor(org.bukkit.Color.fromRGB(rgb));
        } catch (NumberFormatException e) {
            meta.setColor(org.bukkit.Color.PURPLE);
        }

        if (cfg.isString("display-name")) {
            meta.setDisplayName(Utils.formatColors(cfg.getString("display-name")));
        }
        if (cfg.isList("lore")) {
            java.util.List<String> rawLore = cfg.getStringList("lore");
            String initCountdown = Utils.formatDuration(timerSec);
            java.util.List<String> finalLore = rawLore.stream()
                    .map(line -> Utils.formatColors(line.replace("%countdown%", initCountdown)))
                    .toList();
            meta.setLore(finalLore);
        }

        // Mark as shard booster and store expiration timestamp
        meta.getPersistentDataContainer().set(ToolsManager.BOOSTER_KEY, org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);
        long expiryTimestamp = System.currentTimeMillis() + (timerSec * 1000L);
        meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG,
                expiryTimestamp);

        item.setItemMeta(meta);
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack[] { item });
    }

    public static final NamespacedKey SELL_AXE_KEY = new NamespacedKey(PrismSurvival.getInstance(), "is-sellaxe");

    public void giveSellAxe(Player player, long overrideTimer) {
        org.bukkit.configuration.ConfigurationSection cfg = getConfig().getConfigurationSection("sellaxe");
        if (cfg == null) {
            player.sendMessage(Utils.formatColors("&cError: sellaxe section missing in config.yml"));
            return;
        }
        boolean useCountdown = cfg.getBoolean("use-countdown", true);
        long timerSec = (overrideTimer > 0) ? overrideTimer : cfg.getLong("timer", 259200L);

        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf(cfg.getString("material", "NETHERITE_AXE").toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = org.bukkit.Material.NETHERITE_AXE;
        }

        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Utils.formatColors("&cFailed to create sell axe"));
            return;
        }

        if (cfg.contains("enchantments")) {
            for (String encKey : cfg.getConfigurationSection("enchantments").getKeys(false)) {
                org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment
                        .getByName(encKey.toUpperCase());
                int lvl = cfg.getInt("enchantments." + encKey, 1);
                if (e != null) {
                    meta.addEnchant(e, lvl, true);
                }
            }
        }
        if (cfg.getBoolean("hide-enchantments", false)) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        if (cfg.isString("display-name")) {
            meta.setDisplayName(Utils.formatColors(cfg.getString("display-name")));
        }

        if (cfg.isList("lore")) {
            java.util.List<String> rawLore = cfg.getStringList("lore");
            String initCountdown = Utils.formatDuration(timerSec);
            java.util.List<String> finalLore = rawLore.stream()
                    .map(line -> Utils.formatColors(line.replace("%countdown%", initCountdown)))
                    .toList();
            meta.setLore(finalLore);
        }

        // Mark as sell axe
        meta.getPersistentDataContainer().set(SELL_AXE_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Expiry
        long expiryTimestamp = System.currentTimeMillis() + (timerSec * 1000L);
        meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG,
                expiryTimestamp);

        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        player.sendMessage(Utils.formatColors("&aGiven Sell Axe to &f" + player.getName()));
    }
}
