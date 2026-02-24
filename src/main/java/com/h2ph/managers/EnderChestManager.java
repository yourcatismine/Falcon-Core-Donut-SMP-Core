package com.h2ph.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import com.h2ph.PrismSurvival;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores all 54 ender chest slots for a player in a SINGLE row.
 * Schema: enderchest (uuid TEXT PRIMARY KEY, contents TEXT)
 * Contents format: "slot:base64|slot:base64|..." — empty slots are omitted.
 * One row per player — zero UUID duplication.
 *
 * Also tracks per-block viewer counts and sends Block Action packets
 * via PacketEvents to animate the ender chest lid (open/close).
 */
public class EnderChestManager {

    private final PrismSurvival plugin;

    // Tracks which players currently have a specific block's GUI open
    // Key: block location, Value: set of player UUIDs viewing it
    private final Map<Location, Set<UUID>> blockViewers = Collections.synchronizedMap(new HashMap<>());

    public EnderChestManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    private Connection getConnection() throws SQLException {
        return plugin.getPrismSell().getDatabaseManager().getConnection();
    }

    // -----------------------------------------------------------------------
    // Block Animation (PacketEvents Block Action packets)
    // -----------------------------------------------------------------------

    /**
     * Called when a player opens the GUI from a specific block.
     * Sends the lid-open animation to nearby players if this is the first viewer.
     */
    public void registerViewer(Block block, Player player) {
        Location loc = block.getLocation();
        Set<UUID> viewers = blockViewers.computeIfAbsent(loc, k -> new HashSet<>());
        boolean wasEmpty = viewers.isEmpty();
        viewers.add(player.getUniqueId());

        if (wasEmpty) {
            // First viewer — send open animation (viewer count = 1)
            sendBlockAction(block, 1);
        }
    }

    /**
     * Called when a player closes the GUI that was opened from a specific block.
     * Sends the lid-close animation to nearby players if this was the last viewer.
     */
    public void unregisterViewer(Block block, Player player) {
        Location loc = block.getLocation();
        Set<UUID> viewers = blockViewers.get(loc);
        if (viewers == null)
            return;

        viewers.remove(player.getUniqueId());
        if (viewers.isEmpty()) {
            blockViewers.remove(loc);
            // Last viewer left — send close animation (viewer count = 0)
            sendBlockAction(block, 0);
        }
    }

    /**
     * Sends a Block Action packet (0x1E) to all players within 64 blocks of the
     * chest.
     * This is the same packet vanilla uses to animate chest lids.
     *
     * @param block       the ender chest block
     * @param viewerCount 1 = open lid, 0 = close lid
     */
    private void sendBlockAction(Block block, int viewerCount) {
        Vector3i pos = new Vector3i(block.getX(), block.getY(), block.getZ());

        // Get the numeric global block type ID that the packet requires
        WrappedBlockState wrappedState = SpigotConversionUtil.fromBukkitBlockData(block.getBlockData());
        int blockTypeId = wrappedState.getGlobalId();

        WrapperPlayServerBlockAction packet = new WrapperPlayServerBlockAction(
                pos,
                1, // action ID 1 = animate chest lid (same as vanilla)
                viewerCount, // 0 = close, ≥1 = open
                blockTypeId);

        // Send to all players within 64 blocks so everyone sees the animation
        for (Player nearby : block.getWorld().getNearbyPlayers(block.getLocation(), 64)) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(nearby, packet);
        }
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    public ItemStack[] loadEnderChest(UUID uuid) {
        ItemStack[] contents = new ItemStack[54];
        String query = "SELECT contents FROM enderchest WHERE uuid = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serialized);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save ender chest for " + uuid, e);
        }
    }

    // -----------------------------------------------------------------------
    // Serialization helpers (format: "slot:base64|slot:base64|...")
    // -----------------------------------------------------------------------

    private String serializeContents(ItemStack[] items) {
        StringBuilder sb = new StringBuilder();
        for (int slot = 0; slot < Math.min(items.length, 54); slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == org.bukkit.Material.AIR)
                continue;
            try {
                String encoded = Base64.getEncoder().encodeToString(item.serializeAsBytes());
                if (sb.length() > 0)
                    sb.append('|');
                sb.append(slot).append(':').append(encoded);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to serialize slot " + slot + ": " + e.getMessage());
            }
        }
        return sb.toString();
    }

    private void deserializeContents(String raw, ItemStack[] contents) {
        if (raw == null || raw.isEmpty())
            return;
        String[] entries = raw.split("\\|");
        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 1)
                continue;
            try {
                int slot = Integer.parseInt(entry.substring(0, colon));
                if (slot < 0 || slot >= 54)
                    continue;
                byte[] bytes = Base64.getDecoder().decode(entry.substring(colon + 1));
                contents[slot] = ItemStack.deserializeBytes(bytes);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize ender chest entry: " + e.getMessage());
            }
        }
    }
}
