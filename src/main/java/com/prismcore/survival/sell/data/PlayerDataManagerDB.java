/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.sell.data;

import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.category.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
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
        String queryData = "SELECT * FROM player_stats WHERE uuid = ?";
        String queryCategoryData = "SELECT * FROM player_category_data WHERE uuid = ?";

        try (Connection conn = this.plugin.getDatabaseManager().getConnection()) {
            if (conn != null && !conn.isClosed()) {
                // Load global stats
                try (PreparedStatement stmt = conn.prepareStatement(queryData)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
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
                            data.setBounty(rs.getDouble("bounty"));
                            data.setToolExpiry(rs.getLong("tool_expiry"));
                            data.setTeamId(rs.getString("team"));
                        }
                    }
                }

                // Load category data (multipliers + progress)
                try (PreparedStatement stmt = conn.prepareStatement(queryCategoryData)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            for (Category category : Category.values()) {
                                // Load Multiplier
                                String multiplierCol = category.name() + "_multiplier";
                                try {
                                    double multiplier = rs.getDouble(multiplierCol);
                                    data.setMultiplier(category, multiplier);
                                } catch (SQLException e) {
                                    this.plugin.getLogger()
                                            .warning("Missing column " + multiplierCol + " in player_category_data");
                                }

                                // Load Progress
                                String progressCol = category.name() + "_progress";
                                try {
                                    double progress = rs.getDouble(progressCol);
                                    data.setProgress(category, progress);
                                } catch (SQLException e) {
                                    this.plugin.getLogger()
                                            .warning("Missing column " + progressCol + " in player_category_data");
                                }
                            }
                        } else {
                            this.plugin.getLogger()
                                    .warning("No category data row found for " + uuid + ", using defaults.");
                        }
                    }
                }
            }

            data.resetDirty();
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
        // Query for Global Stats
        String queryData = "INSERT INTO player_stats (uuid, money, shards, break_blocks, placed_blocks, mob_kills, sell_made, shop_spent, playtime, deaths, kills, `keys`, bounty, tool_expiry, team) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                +
                " ON DUPLICATE KEY UPDATE money=VALUES(money), shards=VALUES(shards), break_blocks=VALUES(break_blocks),"
                +
                " placed_blocks=VALUES(placed_blocks), mob_kills=VALUES(mob_kills), sell_made=VALUES(sell_made)," +
                " shop_spent=VALUES(shop_spent), playtime=VALUES(playtime), deaths=VALUES(deaths), kills=VALUES(kills),"
                +
                " `keys`=VALUES(`keys`), bounty=VALUES(bounty), tool_expiry=VALUES(tool_expiry), team=VALUES(team)";

        // Query for Category Data
        StringBuilder queryCategoryData = new StringBuilder();
        queryCategoryData.append("INSERT INTO player_category_data (uuid");
        for (Category category : Category.values()) {
            queryCategoryData.append(", ").append(category.name()).append("_multiplier");
            queryCategoryData.append(", ").append(category.name()).append("_progress");
        }
        queryCategoryData.append(") VALUES (?");
        for (int i = 0; i < Category.values().length * 2; i++) {
            queryCategoryData.append(", ?");
        }
        queryCategoryData.append(") ON DUPLICATE KEY UPDATE ");
        boolean firstCol = true;
        for (Category category : Category.values()) {
            if (!firstCol)
                queryCategoryData.append(", ");
            queryCategoryData.append(category.name()).append("_multiplier=VALUES(").append(category.name())
                    .append("_multiplier)");
            queryCategoryData.append(", ").append(category.name()).append("_progress=VALUES(").append(category.name())
                    .append("_progress)");
            firstCol = false;
        }

        try (Connection conn = this.plugin.getDatabaseManager().getConnection()) {

            // Save Global Stats
            try (PreparedStatement insertStmt = conn.prepareStatement(queryData)) {
                int i = 1;
                insertStmt.setString(i++, uuid.toString());
                insertStmt.setDouble(i++, data.getMoney());
                insertStmt.setLong(i++, data.getShards());
                insertStmt.setLong(i++, data.getBreakBlocks());
                insertStmt.setLong(i++, data.getPlacedBlocks());
                insertStmt.setLong(i++, data.getMobKills());
                insertStmt.setDouble(i++, data.getSellMade());
                insertStmt.setDouble(i++, data.getShopSpent());
                insertStmt.setLong(i++, data.getPlaytime());
                insertStmt.setLong(i++, data.getDeaths());
                insertStmt.setLong(i++, data.getKills());
                insertStmt.setLong(i++, data.getKeys());
                insertStmt.setDouble(i++, data.getBounty());
                insertStmt.setLong(i++, data.getToolExpiry());
                insertStmt.setString(i++, data.getTeamId());
                insertStmt.executeUpdate();
            }

            // Save Category Data
            try (PreparedStatement insertStmt = conn.prepareStatement(queryCategoryData.toString())) {
                int i = 1;
                insertStmt.setString(i++, uuid.toString());
                for (Category category : Category.values()) {
                    insertStmt.setDouble(i++, data.getMultiplier(category));
                    insertStmt.setDouble(i++, data.getProgress(category));
                }
                insertStmt.executeUpdate();
            }

            // Reset dirty flag
            data.resetDirty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        for (UUID uuid : this.cache.keySet()) {
            PlayerData data = this.cache.get(uuid);
            if (data == null)
                continue;
            this.savePlayerDataSync(uuid, data);
        }
    }

    public void unloadPlayer(UUID uuid) {
        this.savePlayerData(uuid);
        this.cache.remove(uuid);
    }
}
