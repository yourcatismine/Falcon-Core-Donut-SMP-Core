package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages PvP safe zones where players cannot engage in PvP
 * Uses database storage for persistence
 */
public class PvPSafeZoneManager {

    private final PrismSurvival plugin;
    private final DatabaseManager databaseManager;
    private final List<PvPSafeZone> safeZones = new ArrayList<>();

    public PvPSafeZoneManager(PrismSurvival plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        loadZones();
    }

    /**
     * Load all safe zones from the database
     */
    public void loadZones() {
        if (!databaseManager.isConnected()) {
            plugin.getLogger().warning("Database not connected, cannot load PvP safe zones.");
            return;
        }
        
        String query = "SELECT * FROM pvp_safe_zones";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            safeZones.clear();
            while (rs.next()) {
                String zoneName = rs.getString("name");
                String world = rs.getString("world");
                double minX = rs.getDouble("min_x");
                double minY = rs.getDouble("min_y");
                double minZ = rs.getDouble("min_z");
                double maxX = rs.getDouble("max_x");
                double maxY = rs.getDouble("max_y");
                double maxZ = rs.getDouble("max_z");
                String createdBy = rs.getString("created_by");
                long createdAt = rs.getLong("created_at");
                
                safeZones.add(new PvPSafeZone(zoneName, world, minX, minY, minZ, maxX, maxY, maxZ, createdBy, createdAt));
            }
            
            plugin.getLogger().info("Loaded " + safeZones.size() + " PvP safe zones from database.");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load PvP safe zones from database", e);
        }
    }

    /**
     * Add a new PvP safe zone
     */
    public boolean addZone(String name, String world, double minX, double minY, double minZ, 
                          double maxX, double maxY, double maxZ, String createdBy) {
        if (!databaseManager.isConnected()) {
            plugin.getLogger().warning("Database not connected, cannot add PvP safe zone.");
            return false;
        }
        
        for (PvPSafeZone zone : safeZones) {
            if (zone.name.equals(name)) {
                return false;
            }
        }
        
        String query = "INSERT INTO pvp_safe_zones (name, world, min_x, min_y, min_z, max_x, max_y, max_z, created_by, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            long createdAt = System.currentTimeMillis();
            ps.setString(1, name);
            ps.setString(2, world);
            ps.setDouble(3, minX);
            ps.setDouble(4, minY);
            ps.setDouble(5, minZ);
            ps.setDouble(6, maxX);
            ps.setDouble(7, maxY);
            ps.setDouble(8, maxZ);
            ps.setString(9, createdBy);
            ps.setLong(10, createdAt);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                safeZones.add(new PvPSafeZone(name, world, minX, minY, minZ, maxX, maxY, maxZ, createdBy, createdAt));
                plugin.getLogger().info("Added PvP safe zone '" + name + "' to database.");
                return true;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add PvP safe zone to database", e);
        }
        
        return false;
    }

    /**
     * Remove a PvP safe zone
     */
    public boolean removeZone(String zoneName) {
        if (!databaseManager.isConnected()) {
            plugin.getLogger().warning("Database not connected, cannot remove PvP safe zone.");
            return false;
        }
        
        String query = "DELETE FROM pvp_safe_zones WHERE name = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, zoneName);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                safeZones.removeIf(zone -> zone.name.equals(zoneName));
                plugin.getLogger().info("Removed PvP safe zone '" + zoneName + "' from database.");
                return true;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove PvP safe zone from database", e);
        }
        
        return false;
    }

    /**
     * Check if a location is within any PvP safe zone
     */
    public boolean isInSafeZone(Location loc) {
        for (PvPSafeZone zone : safeZones) {
            if (zone.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the safe zone at a specific location (if any)
     */
    public PvPSafeZone getSafeZoneAt(Location loc) {
        for (PvPSafeZone zone : safeZones) {
            if (zone.contains(loc)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Get all zone names for tab completion
     */
    public List<String> getAllZoneNames() {
        return safeZones.stream()
                .map(zone -> zone.name)
                .collect(Collectors.toList());
    }

    /**
     * Get all safe zones
     */
    public List<PvPSafeZone> getSafeZones() {
        return new ArrayList<>(safeZones);
    }

    /**
     * Check if a world has any PvP safe zones configured.
     * The Restorer only operates in worlds that return true here.
     */
    public boolean hasZonesInWorld(String worldName) {
        for (PvPSafeZone zone : safeZones) {
            if (zone.worldName.equals(worldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a PvP safe zone
     */
    public static class PvPSafeZone {
        public final String name;
        public final String worldName;
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;
        public final String createdBy;
        public final long createdAt;

        public PvPSafeZone(String name, String worldName, double minX, double minY, double minZ, 
                          double maxX, double maxY, double maxZ, String createdBy, long createdAt) {
            this.name = name;
            this.worldName = worldName;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        /**
         * Check if a location is within this safe zone
         */
        public boolean contains(Location loc) {
            if (!loc.getWorld().getName().equals(worldName)) {
                return false;
            }

            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();

            return x >= minX && x <= maxX &&
                   y >= minY && y <= maxY &&
                   z >= minZ && z <= maxZ;
        }
        
        public String getName() { return name; }
        public String getWorld() { return worldName; }
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }
        public String getCreatedBy() { return createdBy; }
        public long getCreatedAt() { return createdAt; }
    }
}