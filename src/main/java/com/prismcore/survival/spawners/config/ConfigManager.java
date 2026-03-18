package com.prismcore.survival.spawners.config;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final PrismSurvival plugin;
    private FileConfiguration config;

    public ConfigManager(PrismSurvival plugin) {
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
