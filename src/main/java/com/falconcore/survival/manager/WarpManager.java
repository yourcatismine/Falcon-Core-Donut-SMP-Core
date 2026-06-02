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

public class WarpManager {

    private final Falcon plugin;

    public WarpManager(Falcon plugin) {
        this.plugin = plugin;
    }

    public void setWarp(String name, Location loc) {
        String serialized = serializeLocation(loc);
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
                try (PreparedStatement create = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS warps (name VARCHAR(255) PRIMARY KEY, location TEXT NOT NULL)")) {
                    create.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO warps (name, location) VALUES (?, ?) ON DUPLICATE KEY UPDATE location = ?")) {
                    ps.setString(1, name.toLowerCase());
                    ps.setString(2, serialized);
                    ps.setString(3, serialized);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save warp: " + name, e);
            }
        });
    }

    public boolean deleteWarp(String name) {
        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS warps (name VARCHAR(255) PRIMARY KEY, location TEXT NOT NULL)")) {
                create.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM warps WHERE name = ?")) {
                ps.setString(1, name.toLowerCase());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete warp: " + name, e);
            return false;
        }
    }

    public Location getWarp(String name) {
        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS warps (name VARCHAR(255) PRIMARY KEY, location TEXT NOT NULL)")) {
                create.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT location FROM warps WHERE name = ?")) {
                ps.setString(1, name.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return deserializeLocation(rs.getString("location"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get warp: " + name, e);
        }
        return null;
    }

    public List<String> listWarps() {
        List<String> names = new ArrayList<>();
        try (Connection conn = plugin.getFalconSell().getDatabaseManager().getConnection()) {
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS warps (name VARCHAR(255) PRIMARY KEY, location TEXT NOT NULL)")) {
                create.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM warps ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list warps", e);
        }
        return names;
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLocation(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split(",");
        if (parts.length < 6) return null;
        try {
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize warp location: " + raw, e);
            return null;
        }
    }
}
