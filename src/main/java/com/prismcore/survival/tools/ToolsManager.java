package com.prismcore.survival.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.h2ph.PrismSurvival;

public class ToolsManager {

    private final PrismSurvival plugin;
    private FileConfiguration config;
    private File configFile;

    public static NamespacedKey EXPIRY_KEY;
    public static NamespacedKey REMAINING_KEY;
    public static NamespacedKey MULTI_KEY;
    public static NamespacedKey BOOSTER_KEY;

    private static ToolsManager instance;

    public ToolsManager(PrismSurvival plugin) {
        this.plugin = plugin;
        instance = this;
        EXPIRY_KEY = new NamespacedKey(plugin, "tool-expiry");
        REMAINING_KEY = new NamespacedKey(plugin, "tool-remaining");
        MULTI_KEY = new NamespacedKey(plugin, "is-multitool");
        BOOSTER_KEY = new NamespacedKey(plugin, "is-shardbooster");
        loadConfig();
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
        plugin.getServer().getPluginManager().registerEvents(new DrillClickListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MultitoolBlockBreakListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DrillInventoryListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BucketUseListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ShardBoosterListener(this, plugin), plugin);
    }

    private void startUpdateTask() {
        long intervalSeconds = getConfig().getLong("drill.update-interval", 20L); // Config uses ticks or seconds? "20L"
                                                                                  // default implies ticks usually but
                                                                                  // earlier code treated it as ticks.
        // wait, earlier code: getConfig().getLong("drill.update-interval", 20L) * 20L;
        // -> This implies config is in SECONDS, and we multiply by 20 for ticks.
        // Let's assume config is in Seconds.

        long intervalTicks = intervalSeconds * 20L;
        if (intervalTicks <= 0)
            intervalTicks = 100L;

        // We run the task every interval. The decrement amount is equal to the interval
        // in seconds.
        final long decrementSeconds = intervalSeconds;

        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getSchedulerAdapter().runEntityTask(p, () -> updatePlayerTools(p, decrementSeconds));
            }
        }, intervalTicks, intervalTicks);
    }

    public void updatePlayerTools(Player player) {
        updatePlayerTools(player, 0L);
    }

    public void updatePlayerTools(Player player, long decrementSeconds) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta())
                continue;
            ItemMeta meta = item.getItemMeta();

            // Check for explicit REMAINING_KEY first (New System)
            boolean hasRemaining = meta.getPersistentDataContainer().has(REMAINING_KEY, PersistentDataType.LONG);
            // Check for EXPIRY_KEY (Legacy System)
            boolean hasExpiry = meta.getPersistentDataContainer().has(EXPIRY_KEY, PersistentDataType.LONG);

            if (!hasRemaining && !hasExpiry)
                continue;

            String key = null;
            if (meta.getPersistentDataContainer().has(MULTI_KEY, PersistentDataType.BYTE)) {
                key = "multitool";
            } else {
                String matName = item.getType().name();
                if (matName.endsWith("_PICKAXE")) {
                    key = "drill";
                } else if (matName.endsWith("_AXE")) {
                    key = "axe";
                } else if (matName.endsWith("_SHOVEL")) {
                    key = "shovel";
                } else if (matName.endsWith("_BUCKET") || matName.equals("BUCKET")) {
                    key = "bucket";
                }
            }

            if (key == null || !getConfig().getBoolean(key + ".use-countdown", true))
                continue;

            long remainingSeconds;

            if (hasRemaining) {
                // New System: Just decrement
                long current = meta.getPersistentDataContainer().get(REMAINING_KEY, PersistentDataType.LONG);
                remainingSeconds = current - decrementSeconds;
            } else {
                // Legacy System: Migrate!
                long expiry = meta.getPersistentDataContainer().get(EXPIRY_KEY, PersistentDataType.LONG);
                remainingSeconds = (expiry - System.currentTimeMillis()) / 1000L;

                // Remove old key so we don't migrate again
                meta.getPersistentDataContainer().remove(EXPIRY_KEY);
            }

            if (remainingSeconds <= 0L) {
                player.getInventory().remove(item);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                continue;
            }

            // Update NBT
            meta.getPersistentDataContainer().set(REMAINING_KEY, PersistentDataType.LONG, remainingSeconds);

            // Update Lore
            String countdown = Utils.formatDuration(remainingSeconds);
            List<String> tmpl = getConfig().getStringList(key + ".lore");
            List<String> updated = tmpl.stream()
                    .map(line -> line.replace("%countdown%", countdown))
                    .map(Utils::formatColors)
                    .toList();
            meta.setLore(updated);
            item.setItemMeta(meta);
        }
    }
}
