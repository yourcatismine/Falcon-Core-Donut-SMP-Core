/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.sell.data;

import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.category.Category;
import com.prismcore.survival.sell.data.PlayerData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManagerDB {
    private final PrismSell plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<UUID, PlayerData>();

    public PlayerDataManagerDB(PrismSell plugin) {
        this.plugin = plugin;
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (this.cache.containsKey(uuid)) {
            return this.cache.get(uuid);
        }
        PlayerData data = this.loadPlayerData(uuid);
        this.cache.put(uuid, data);
        return data;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        String query = "SELECT category, progress, multiplier, money, shards, break_blocks, placed_blocks, mob_kills, sell_made, shop_spent, playtime, deaths, kills, keys FROM player_data WHERE uuid = ?";
        try {
            Connection conn = this.plugin.getDatabaseManager().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String categoryName = rs.getString("category");
                        if (categoryName.equals("BALANCE")) {
                            data.setMoney(rs.getDouble("money"));
                            data.setShards(rs.getLong("shards"));
                            data.setBreakBlocks(rs.getLong("break_blocks"));
                            data.setPlacedBlocks(rs.getLong("placed_blocks"));
                            data.setMobKills(rs.getLong("mob_kills"));
                            data.setSellMade(rs.getDouble("sell_made"));
                            data.setShopSpent(rs.getDouble("shop_spent"));
                            data.setPlaytime(rs.getLong("playtime"));
                            data.setDeaths(rs.getLong("deaths"));
                            data.setKills(rs.getLong("kills"));
                            data.setKeys(rs.getLong("keys"));
                            // Reset dirty flag after loading
                            data.resetDirty();
                            continue;
                        }

                        double progress = rs.getDouble("progress");
                        double multiplier = rs.getDouble("multiplier");
                        try {
                            Category category = Category.valueOf(categoryName);
                            data.setProgress(category, progress);
                            data.setMultiplier(category, multiplier);
                        } catch (IllegalArgumentException e) {
                            this.plugin.getLogger().warning("Invalid category in database for player "
                                    + uuid + ": " + categoryName);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to load player data for " + uuid);
            e.printStackTrace();
        }
        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = this.cache.get(uuid);
        if (data == null) {
            return;
        }
        if (!data.isDirty()) {
            return;
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> this.savePlayerDataSync(uuid, data));
    }

    private void savePlayerDataSync(UUID uuid, PlayerData data) {
        String insertQuery = "INSERT OR REPLACE INTO player_data (uuid, category, progress, multiplier, money, shards, break_blocks, placed_blocks, mob_kills, sell_made, shop_spent, playtime, deaths, kills, keys) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = this.plugin.getDatabaseManager().getConnection();
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (Category category : Category.values()) {
                    double progress = data.getProgress(category);
                    double multiplier = data.getMultiplier(category);
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setString(2, category.name());
                    insertStmt.setDouble(3, progress);
                    insertStmt.setDouble(4, multiplier);
                    insertStmt.setDouble(5, 0.0);
                    insertStmt.setLong(6, 0);
                    insertStmt.setLong(7, 0);
                    insertStmt.setLong(8, 0);
                    insertStmt.setLong(9, 0);
                    insertStmt.setDouble(10, 0.0);
                    insertStmt.setDouble(11, 0.0);
                    insertStmt.setLong(12, 0);
                    insertStmt.setLong(13, 0);
                    insertStmt.setLong(14, 0);
                    insertStmt.setLong(15, 0);
                    insertStmt.addBatch();
                }

                // Save Balance & Stats
                insertStmt.setString(1, uuid.toString());
                insertStmt.setString(2, "BALANCE");
                insertStmt.setDouble(3, 0.0);
                insertStmt.setDouble(4, 0.0);
                insertStmt.setDouble(5, data.getMoney());
                insertStmt.setLong(6, data.getShards());
                insertStmt.setLong(7, data.getBreakBlocks());
                insertStmt.setLong(8, data.getPlacedBlocks());
                insertStmt.setLong(9, data.getMobKills());
                insertStmt.setDouble(10, data.getSellMade());
                insertStmt.setDouble(11, data.getShopSpent());
                insertStmt.setLong(12, data.getPlaytime());
                insertStmt.setLong(13, data.getDeaths());
                insertStmt.setLong(14, data.getKills());
                insertStmt.setLong(15, data.getKeys());
                insertStmt.addBatch();

                insertStmt.executeBatch();

                // Reset dirty flag
                data.resetDirty();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to save player data for " + uuid);
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        this.plugin.getLogger().info("Saving all player data to database...");
        for (UUID uuid : this.cache.keySet()) {
            PlayerData data = this.cache.get(uuid);
            if (data == null)
                continue;
            this.savePlayerDataSync(uuid, data);
        }
        this.plugin.getLogger().info("Saved data for " + this.cache.size() + " players.");
    }

    public void unloadPlayer(UUID uuid) {
        this.savePlayerData(uuid);
        this.cache.remove(uuid);
    }
}
