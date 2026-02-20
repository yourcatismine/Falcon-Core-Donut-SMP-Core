package com.h2ph.managers;

import com.h2ph.PrismSurvival;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores all 54 ender chest slots for a player in a SINGLE row.
 * Schema: enderchest (uuid TEXT PRIMARY KEY, contents TEXT)
 * Contents format: "slot:base64|slot:base64|..." — empty slots are omitted.
 * One row per player — zero UUID duplication.
 */
public class EnderChestManager {

    private final PrismSurvival plugin;

    public EnderChestManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    private Connection getConnection() throws SQLException {
        return plugin.getPrismSell().getDatabaseManager().getConnection();
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    public ItemStack[] loadEnderChest(UUID uuid) {
        ItemStack[] contents = new ItemStack[54];
        String query = "SELECT contents FROM enderchest WHERE uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("contents");
                    deserializeContents(raw, contents);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load ender chest for " + uuid, e);
        }
        return contents;
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    public void saveEnderChest(UUID uuid, ItemStack[] items) {
        String serialized = serializeContents(items);
        // REPLACE INTO = INSERT OR REPLACE — upserts the single row for this UUID
        String query = "REPLACE INTO enderchest (uuid, contents) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serialized);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save ender chest for " + uuid, e);
        }
    }

    // -----------------------------------------------------------------------
    // Serialization helpers  (format: "slot:base64|slot:base64|...")
    // -----------------------------------------------------------------------

    private String serializeContents(ItemStack[] items) {
        StringBuilder sb = new StringBuilder();
        for (int slot = 0; slot < Math.min(items.length, 54); slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            try {
                String encoded = Base64.getEncoder().encodeToString(item.serializeAsBytes());
                if (sb.length() > 0) sb.append('|');
                sb.append(slot).append(':').append(encoded);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to serialize slot " + slot + ": " + e.getMessage());
            }
        }
        return sb.toString();
    }

    private void deserializeContents(String raw, ItemStack[] contents) {
        if (raw == null || raw.isEmpty()) return;
        String[] entries = raw.split("\\|");
        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 1) continue;
            try {
                int slot = Integer.parseInt(entry.substring(0, colon));
                if (slot < 0 || slot >= 54) continue;
                byte[] bytes = Base64.getDecoder().decode(entry.substring(colon + 1));
                contents[slot] = ItemStack.deserializeBytes(bytes);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize ender chest entry: " + e.getMessage());
            }
        }
    }
}
