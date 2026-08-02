package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SpawnManager {

    private final Falcon plugin;
    private Location globalSpawnCache;
    private java.io.File spawnsFile;
    private org.bukkit.configuration.file.FileConfiguration spawnsConfig;

    public SpawnManager(Falcon plugin) {
        this.plugin = plugin;
        java.io.File dataFolder = new java.io.File(plugin.getDataFolder(), "data");
        this.spawnsFile = new java.io.File(dataFolder, "server/spawns.yml");
        reloadConfig();
    }

    /**
     * Legacy method for compatibility - now also reloads the flatfile
     */
    public void reloadConfig() {
        if (!spawnsFile.exists()) {
            try {
                spawnsFile.getParentFile().mkdirs();
                spawnsFile.createNewFile();
            } catch (java.io.IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create spawns.yml", e);
            }
        }
        spawnsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(spawnsFile);
    }

    private void saveSpawnsConfig() {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try {
                spawnsConfig.save(spawnsFile);
            } catch (java.io.IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save spawns.yml", e);
            }
        });
    }

    public boolean saveSpawn(String name, Location loc) {
        String serialized = serializeLocation(loc);
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            spawnsConfig.set("spawns." + name.toLowerCase(), serialized);
            saveSpawnsConfig();
            return true;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
                try (PreparedStatement createTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS named_spawns (name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                    createTable.executeUpdate();
                }
                
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO named_spawns (name, spawn) VALUES (?, ?) ON DUPLICATE KEY UPDATE spawn = ?")) {
                    ps.setString(1, name.toLowerCase());
                    ps.setString(2, serialized);
                    ps.setString(3, serialized);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save named spawn: " + name, e);
            }
        });
        return true;
    }

    public boolean deleteSpawn(String name) {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            spawnsConfig.set("spawns." + name.toLowerCase(), null);
            saveSpawnsConfig();
            return true;
        }

        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM named_spawns WHERE name = ?")) {
            ps.setString(1, name.toLowerCase());
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete named spawn: " + name, e);
            return false;
        }
    }

    public Location getSpawn(String name) {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            String raw = spawnsConfig.getString("spawns." + name.toLowerCase());
            if (raw == null || raw.isEmpty()) return null;
            return deserializeLocation(raw);
        }

        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS named_spawns (name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                createTable.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT spawn FROM named_spawns WHERE name = ?")) {
                ps.setString(1, name.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String raw = rs.getString("spawn");
                        if (raw == null || raw.isEmpty())
                            return null;
                        return deserializeLocation(raw);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get named spawn: " + name, e);
        }
        return null;
    }

    /**
     * Get world-specific spawn location
     * @param worldName The world name
     * @return The spawn location for that world, or null if not set
     */
    public Location getWorldSpawn(String worldName) {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            String raw = spawnsConfig.getString("world_spawns." + worldName);
            if (raw == null || raw.isEmpty()) return null;
            return deserializeLocation(raw);
        }

        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS world_spawns (world_name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                createTable.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT spawn FROM world_spawns WHERE world_name = ?")) {
                ps.setString(1, worldName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String raw = rs.getString("spawn");
                        if (raw == null || raw.isEmpty())
                            return null;
                        return deserializeLocation(raw);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get world spawn for " + worldName, e);
        }
        return null;
    }

    /**
     * Set world-specific spawn location
     * @param worldName The world name
     * @param location The spawn location
     * @return true if saved successfully
     */
    public boolean setWorldSpawn(String worldName, Location location) {
        String serialized = serializeLocation(location);
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            spawnsConfig.set("world_spawns." + worldName, serialized);
            saveSpawnsConfig();
            return true;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
                try (PreparedStatement createTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS world_spawns (world_name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                    createTable.executeUpdate();
                }
                
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO world_spawns (world_name, spawn) VALUES (?, ?) ON DUPLICATE KEY UPDATE spawn = ?")) {
                    ps.setString(1, worldName);
                    ps.setString(2, serialized);
                    ps.setString(3, serialized);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set world spawn for " + worldName, e);
            }
        });
        return true;
    }

    /**
     * Delete world-specific spawn
     * @param worldName The world name
     * @return true if deleted successfully
     */
    public boolean deleteWorldSpawn(String worldName) {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            spawnsConfig.set("world_spawns." + worldName, null);
            saveSpawnsConfig();
            return true;
        }

        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM world_spawns WHERE world_name = ?")) {
            ps.setString(1, worldName);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete world spawn for " + worldName, e);
            return false;
        }
    }

    /**
     * Get the best spawn location for a player
     * Checks in order: world-specific spawn, global spawn, world default spawn
     * @param worldName The world the player is in
     * @return The best available spawn location
     */
    public Location getBestSpawnForWorld(String worldName) {
        Location worldSpawn = getWorldSpawn(worldName);
        if (worldSpawn != null) {
            return worldSpawn;
        }

        Location globalSpawn = getGlobalSpawn();
        if (globalSpawn != null) {
            return globalSpawn;
        }

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world != null) {
            return world.getSpawnLocation();
        }

        return null;
    }

    /**
     * List all world-specific spawns
     * @return List of world names that have custom spawns
     */
    public List<String> listWorldSpawns() {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            org.bukkit.configuration.ConfigurationSection sec = spawnsConfig.getConfigurationSection("world_spawns");
            if (sec == null) return new ArrayList<>();
            return new ArrayList<>(sec.getKeys(false));
        }

        List<String> worldNames = new ArrayList<>();
        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS world_spawns (world_name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                createTable.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT world_name FROM world_spawns")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        worldNames.add(rs.getString("world_name"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list world spawns", e);
        }
        return worldNames;
    }

    public List<String> listSpawns() {
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            org.bukkit.configuration.ConfigurationSection sec = spawnsConfig.getConfigurationSection("spawns");
            if (sec == null) return new ArrayList<>();
            return new ArrayList<>(sec.getKeys(false));
        }

        List<String> spawnNames = new ArrayList<>();
        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS named_spawns (name VARCHAR(255) PRIMARY KEY, spawn TEXT)")) {
                createTable.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM named_spawns")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        spawnNames.add(rs.getString("name"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list named spawns", e);
        }
        return spawnNames;
    }

    public Location getGlobalSpawn() {
        if (globalSpawnCache != null)
            return globalSpawnCache;

        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            String raw = spawnsConfig.getString("global_spawn");
            if (raw == null || raw.isEmpty()) return null;
            globalSpawnCache = deserializeLocation(raw);
            return globalSpawnCache;
        }

        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS server (id INT PRIMARY KEY, spawn TEXT)")) {
                createTable.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT spawn FROM server WHERE id = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String raw = rs.getString("spawn");
                        if (raw == null || raw.isEmpty())
                            return null;
                        globalSpawnCache = deserializeLocation(raw);
                        return globalSpawnCache;
                    }
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
        if (plugin.getFalconSell().getDatabaseManager().isFlatfileMode()) {
            spawnsConfig.set("global_spawn", serialized);
            saveSpawnsConfig();
            return;
        }

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
                try (PreparedStatement createTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS server (id INT PRIMARY KEY, spawn TEXT)")) {
                    createTable.executeUpdate();
                }
                
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO server (id, spawn) VALUES (1, ?) ON DUPLICATE KEY UPDATE spawn = ?")) {
                    ps.setString(1, serialized);
                    ps.setString(2, serialized);
                    ps.executeUpdate();
                }
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
