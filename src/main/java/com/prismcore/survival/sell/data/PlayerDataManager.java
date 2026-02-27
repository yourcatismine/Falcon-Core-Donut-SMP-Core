/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.prismcore.survival.sell.data;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.category.Category;
import com.prismcore.survival.sell.data.PlayerData;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerDataManager {
    private final PrismSell plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataFolder;

    public PlayerDataManager(PrismSell plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<UUID, PlayerData>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (!this.playerDataMap.containsKey(uuid)) {
            PlayerData data = this.loadPlayerData(uuid);
            this.playerDataMap.put(uuid, data);
        }
        return this.playerDataMap.get(uuid);
    }

    private PlayerData loadPlayerData(UUID uuid) {
        ConfigurationSection multiplierSection;
        File file = new File(this.dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerData(uuid);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File) file);
        PlayerData data = new PlayerData(uuid);
        ConfigurationSection progressSection = config.getConfigurationSection("progress");
        if (progressSection != null) {
            for (String key : progressSection.getKeys(false)) {
                try {
                    Category category = Category.valueOf(key);
                    data.setProgress(category, progressSection.getDouble(key));
                } catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
        if ((multiplierSection = config.getConfigurationSection("multipliers")) != null) {
            for (String key : multiplierSection.getKeys(false)) {
                try {
                    Category category = Category.valueOf(key);
                    data.setMultiplier(category, multiplierSection.getDouble(key));
                } catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = this.playerDataMap.get(uuid);
        if (data == null) {
            return;
        }
        File file = new File(this.dataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<Category, Double> entry : data.getAllProgress().entrySet()) {
            config.set("progress." + entry.getKey().name(), (Object) entry.getValue());
        }
        for (Map.Entry<Category, Double> entry : data.getAllMultipliers().entrySet()) {
            config.set("multipliers." + entry.getKey().name(), (Object) entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save player data for " + String.valueOf(uuid));
            e.printStackTrace();
        }
    }

    public void savePlayerDataAsync(UUID uuid) {
        PlayerData data = this.playerDataMap.get(uuid);
        if (data == null) {
            return;
        }
        // Use PrismSurvival's scheduler adapter if available, or just Bukkit async
        PrismSurvival.getInstance().getSchedulerAdapter().runTaskAsync(() -> savePlayerData(uuid));
    }

    public void saveAllData() {
        for (UUID uuid : this.playerDataMap.keySet()) {
            this.savePlayerData(uuid);
        }
    }
}
