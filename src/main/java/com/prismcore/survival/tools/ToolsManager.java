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
}
