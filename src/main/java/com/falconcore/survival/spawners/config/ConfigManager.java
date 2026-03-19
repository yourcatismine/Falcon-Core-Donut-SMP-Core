package com.falconcore.survival.spawners.config;

import com.h2ph.Falcon;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final Falcon plugin;
    private FileConfiguration config;

    public ConfigManager(Falcon plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getSpawnerConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
