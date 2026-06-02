package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrateLocationRegistry {

    private final Falcon plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<Location, String> crateLocations = new ConcurrentHashMap<>();

    public CrateLocationRegistry(Falcon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates/locations.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        crateLocations.clear();

        if (config.contains("crates")) {
            ConfigurationSection crates = config.getConfigurationSection("crates");
            for (String key : crates.getKeys(false)) {
                String worldName = crates.getString(key + ".world");
                double x = crates.getDouble(key + ".x");
                double y = crates.getDouble(key + ".y");
                double z = crates.getDouble(key + ".z");
                String crateName = crates.getString(key + ".crate");

                if (worldName != null && crateName != null) {
                    org.bukkit.World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        crateLocations.put(loc, crateName);
                    }
                }
            }
        }
    }

    public void save() {
        config = new YamlConfiguration();
        int i = 0;
        for (Map.Entry<Location, String> entry : crateLocations.entrySet()) {
            Location loc = entry.getKey();
            String crateName = entry.getValue();
            String path = "crates." + i;

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".crate", crateName);
            i++;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate locations!");
            e.printStackTrace();
        }
    }

    public void addLocation(String crateName, Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        crateLocations.put(blockLoc, crateName);
        save();
    }

    public void removeLocation(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        crateLocations.remove(blockLoc);
        save();
    }

    public String getCrateName(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return crateLocations.get(blockLoc);
    }

    public Map<Location, String> getAllLocations() {
        return Collections.unmodifiableMap(crateLocations);
    }
}
