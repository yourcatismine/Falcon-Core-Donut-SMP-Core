
package com.falconcore.survival.sell.data;

import com.falconcore.survival.sell.FalconSell;
import com.falconcore.survival.sell.category.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManagerDB {
    private final FalconSell plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<UUID, PlayerData>();

    public PlayerDataManagerDB(FalconSell plugin) {
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

   
    private PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);

        if (this.plugin.getDatabaseManager().isFlatfileMode()) {
            com.falconcore.survival.manager.DatabaseManager.LoadResult<com.falconcore.survival.manager.DatabaseManager.PlayerDataStats> result =
                    this.plugin.getPlugin().getDatabaseManager().loadPlayerStats(uuid);
            if (result != null && result.getData() != null) {
                com.falconcore.survival.manager.DatabaseManager.PlayerDataStats stats = result.getData();
                data.setBreakBlocks(stats.breakBlocks);
                data.setPlacedBlocks(stats.placedBlocks);
                data.setMobKills(stats.mobKills);
                data.setSellMade(stats.sellMade);
                data.setPlaytime(stats.playtime);
                data.setDeaths(stats.deaths);
                data.setKills(stats.kills);
                data.setToolExpiry(stats.toolExpiry);
            }

            Map<String, double[]> catData = this.plugin.getPlugin().getDatabaseManager().getYamlStorage().loadCategoryData(uuid);
            if (catData != null) {
                for (Category category : Category.values()) {
                    if (catData.containsKey(category.name())) {
                        double[] vals = catData.get(category.name());
                        data.setMultiplier(category, vals[0]);
                        data.setProgress(category, vals[1]);
                    }
                }
            }

            data.resetDirty();
            return data;
        }

        String queryData = "SELECT * FROM player_stats WHERE uuid = ?";
        String queryCategoryData = "SELECT * FROM player_category_data WHERE uuid = ?";

        try (Connection conn = this.plugin.getDatabaseManager().getConnection()) {
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(queryData)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            data.setBreakBlocks(rs.getLong("break_blocks"));
                            data.setPlacedBlocks(rs.getLong("placed_blocks"));
                            data.setMobKills(rs.getLong("mob_kills"));
                            data.setSellMade(rs.getDouble("sell_made"));
                            data.setPlaytime(rs.getLong("playtime"));
                            data.setDeaths(rs.getLong("deaths"));
                            data.setKills(rs.getLong("kills"));
                            data.setToolExpiry(rs.getLong("tool_expiry"));
                        }
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(queryCategoryData)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            for (Category category : Category.values()) {
                                String multiplierCol = category.name() + "_multiplier";
                                try {
                                    double multiplier = rs.getDouble(multiplierCol);
                                    data.setMultiplier(category, multiplier);
                                } catch (SQLException e) {
                                    this.plugin.getLogger()
                                            .warning("Missing column " + multiplierCol + " in player_category_data");
                                }

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
            data.setLoadingFailed(true);
        }
        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = this.cache.get(uuid);
        if (data == null || !data.isDirty() || data.isLoadingFailed()) {
            return;
        }
        this.savePlayerDataSync(uuid, data);
    }

    public void savePlayerDataAsync(UUID uuid) {
        PlayerData data = this.cache.get(uuid);
        if (data != null && data.isDirty() && !data.isLoadingFailed()) {
            CompletableFuture.runAsync(() -> this.savePlayerDataSync(uuid, data));
        }
    }

    private void savePlayerDataSync(UUID uuid, PlayerData data) {
        if (this.plugin.getDatabaseManager().isFlatfileMode()) {
            com.falconcore.survival.storage.YamlFlatfileStorage yaml = this.plugin.getPlugin().getDatabaseManager().getYamlStorage();
            if (yaml != null) {
                yaml.updatePlayerStatsField(uuid, "break_blocks", data.getBreakBlocks());
                yaml.updatePlayerStatsField(uuid, "placed_blocks", data.getPlacedBlocks());
                yaml.updatePlayerStatsField(uuid, "mob_kills", data.getMobKills());
                yaml.updatePlayerStatsField(uuid, "sell_made", data.getSellMade());
                yaml.updatePlayerStatsField(uuid, "playtime", data.getPlaytime());
                yaml.updatePlayerStatsField(uuid, "deaths", data.getDeaths());
                yaml.updatePlayerStatsField(uuid, "kills", data.getKills());
                yaml.updatePlayerStatsField(uuid, "tool_expiry", data.getToolExpiry());

                Map<String, double[]> catMap = new java.util.HashMap<>();
                for (Category cat : Category.values()) {
                    catMap.put(cat.name(), new double[]{data.getMultiplier(cat), data.getProgress(cat)});
                }
                yaml.saveCategoryData(uuid, catMap);
            }
            data.resetDirty();
            return;
        }

        if (!this.plugin.getDatabaseManager().isConnected()) {
            data.resetDirty();
            return;
        }

        String queryData = "INSERT INTO player_stats (uuid, break_blocks, placed_blocks, mob_kills, sell_made, playtime, deaths, kills, tool_expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                +
                " ON DUPLICATE KEY UPDATE break_blocks=VALUES(break_blocks),"
                +
                " placed_blocks=VALUES(placed_blocks), mob_kills=VALUES(mob_kills), sell_made=VALUES(sell_made)," +
                " playtime=VALUES(playtime), deaths=VALUES(deaths), kills=VALUES(kills),"
                +
                " tool_expiry=VALUES(tool_expiry)";

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

            try (PreparedStatement insertStmt = conn.prepareStatement(queryData)) {
                int i = 1;
                insertStmt.setString(i++, uuid.toString());
                insertStmt.setLong(i++, data.getBreakBlocks());
                insertStmt.setLong(i++, data.getPlacedBlocks());
                insertStmt.setLong(i++, data.getMobKills());
                insertStmt.setDouble(i++, data.getSellMade());
                insertStmt.setLong(i++, data.getPlaytime());
                insertStmt.setLong(i++, data.getDeaths());
                insertStmt.setLong(i++, data.getKills());
                insertStmt.setLong(i++, data.getToolExpiry());
                insertStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(queryCategoryData.toString())) {
                int i = 1;
                insertStmt.setString(i++, uuid.toString());
                for (Category category : Category.values()) {
                    insertStmt.setDouble(i++, data.getMultiplier(category));
                    insertStmt.setDouble(i++, data.getProgress(category));
                }
                insertStmt.executeUpdate();
            }

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
        savePlayerDataAsync(uuid);
        this.cache.remove(uuid);
    }
}
