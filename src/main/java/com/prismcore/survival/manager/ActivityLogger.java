package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ActivityLogger {

    private final PrismSurvival plugin;
    private Connection connection;

    public ActivityLogger(PrismSurvival plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            File dbDir = new File(plugin.getDataFolder(), "survival/logs");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            File dbFile = new File(dbDir, "activity.db");

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement s = connection.createStatement()) {
                String logsTable = "CREATE TABLE IF NOT EXISTS activity_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "type VARCHAR(20) NOT NULL," + // GENERAL, MESSAGE, ORDER, AUCTION, ITEM
                        "content TEXT NOT NULL," +
                        "timestamp BIGINT NOT NULL" +
                        ")";
                s.execute(logsTable);

                // Index for performance
                s.execute("CREATE INDEX IF NOT EXISTS idx_uuid_type ON activity_logs(uuid, type)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON activity_logs(timestamp)");

                String hazardsTable = "CREATE TABLE IF NOT EXISTS hazards (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "type VARCHAR(20) NOT NULL," + // SPEED, REPETITION
                        "details TEXT NOT NULL," +
                        "timestamp BIGINT NOT NULL" +
                        ")";
                s.execute(hazardsTable);
                s.execute("CREATE INDEX IF NOT EXISTS idx_hazard_uuid ON hazards(uuid)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize activity log database", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            initializeDatabase();
        }
        return connection;
    }

    public void log(UUID uuid, LogType type, String content) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            // Broadcast live
            if (plugin.getApiServer() != null) {
                plugin.getApiServer().broadcastActivityLog(uuid, type, content);
            }

            String query = "INSERT INTO activity_logs (uuid, type, content, timestamp) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                ps.setString(3, content);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public List<Map<String, Object>> getLogs(UUID uuid, LogType type, int limit, int offset) {
        List<Map<String, Object>> logs = new ArrayList<>();
        String query = "SELECT content, timestamp FROM activity_logs WHERE uuid = ? AND type = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("content", rs.getString("content"));
                    log.put("timestamp", rs.getLong("timestamp"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public enum LogType {
        GENERAL, MESSAGE, ORDER, AUCTION, ITEM, INVENTORY, HAZARD, MONEY, SHARDS
    }
}
