package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class SpawnManager {

    private final PrismSurvival plugin;
    private final File file;
    private FileConfiguration config;
    private Location globalSpawnCache;

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

    public Location getGlobalSpawn() {
        if (globalSpawnCache != null)
            return globalSpawnCache;

        try (Connection conn = plugin.getPrismSell().getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT spawn FROM server WHERE id = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("spawn");
                    if (raw == null || raw.isEmpty())
                        return null;
                    globalSpawnCache = deserializeLocation(raw);
                    return globalSpawnCache;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get global spawn from database", e);
        }
        return null;
    }

    public void setGlobalSpawn(Location loc) {
        globalSpawnCache = loc;
        String serialized = serializeLocation(loc);
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = plugin.getPrismSell().getDatabaseManager().getConnection();
                    PreparedStatement ps = conn.prepareStatement("UPDATE server SET spawn = ? WHERE id = 1")) {
                ps.setString(1, serialized);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set global spawn in database", e);
            }
        });
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private Location deserializeLocation(String raw) {
        String[] parts = raw.split(",");
        if (parts.length < 6)
            return null;
        try {
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null)
                return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }
}
