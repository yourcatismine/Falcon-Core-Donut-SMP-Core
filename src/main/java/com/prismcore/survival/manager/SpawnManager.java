package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpawnManager {

    private final PrismSurvival plugin;
    private final File file;
    private FileConfiguration config;

    public SpawnManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "survival/regions/spawn/locations.yml");
        reloadConfig();
    }

    public void reloadConfig() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locations.yml!");
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean saveSpawn(String name, Location loc) {
        if (config == null)
            reloadConfig();
        config.set("spawns." + name.toLowerCase(), loc);
        return save();
    }

    public boolean deleteSpawn(String name) {
        if (config == null)
            reloadConfig();
        if (config.contains("spawns." + name.toLowerCase())) {
            config.set("spawns." + name.toLowerCase(), null);
            return save();
        }
        return false;
    }

    public Location getSpawn(String name) {
        if (config == null)
            reloadConfig();
        return config.getLocation("spawns." + name.toLowerCase());
    }

    public List<String> listSpawns() {
        if (config == null)
            reloadConfig();
        if (config.isConfigurationSection("spawns")) {
            Set<String> keys = config.getConfigurationSection("spawns").getKeys(false);
            return new ArrayList<>(keys);
        }
        return new ArrayList<>();
    }

    private boolean save() {
        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locations.yml!");
            e.printStackTrace();
            return false;
        }
    }
}
