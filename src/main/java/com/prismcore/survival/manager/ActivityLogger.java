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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ActivityLogger {

    private final PrismSurvival plugin;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Connection connection;
    private volatile boolean shuttingDown = false;
    private final ConcurrentLinkedQueue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService worker;

    public ActivityLogger(PrismSurvival plugin) {
        this.plugin = plugin;
        initializeDatabase();
        this.worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PrismActivityLogger-Worker");
            t.setDaemon(true);
            return t;
        });
        this.worker.scheduleAtFixedRate(this::processQueue, 1, 1, TimeUnit.SECONDS);
    }

    private void initializeDatabase() {
        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

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
                        "type VARCHAR(20) NOT NULL," +
                        "content TEXT NOT NULL," +
                        "timestamp BIGINT NOT NULL" +
                        ")";
                s.execute(logsTable);

                s.execute("CREATE INDEX IF NOT EXISTS idx_uuid_type ON activity_logs(uuid, type)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON activity_logs(timestamp)");

                String hazardsTable = "CREATE TABLE IF NOT EXISTS hazards (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "type VARCHAR(20) NOT NULL," +
                        "details TEXT NOT NULL," +
                        "timestamp BIGINT NOT NULL" +
                        ")";
                s.execute(hazardsTable);
                s.execute("CREATE INDEX IF NOT EXISTS idx_hazard_uuid ON hazards(uuid)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize activity log database", e);
        } finally {
            lock.unlock();
        }
    }

    public Connection getConnection() {
        if (shuttingDown)
            return null;
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            initializeDatabase();
        }
        return connection;
    }

    public void shutdown() {
        shuttingDown = true;
        worker.shutdown();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
            processQueue();
        } catch (InterruptedException e) {
            worker.shutdownNow();
        }

        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing activity log database", e);
        } finally {
            lock.unlock();
        }
    }

    private void processQueue() {
        if (logQueue.isEmpty())
            return;

        Connection conn = getConnection();
        if (conn == null)
            return;

        String query = "INSERT INTO activity_logs (uuid, type, content, timestamp) VALUES (?, ?, ?, ?)";
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                LogEntry entry;
                int count = 0;
                while ((entry = logQueue.poll()) != null) {
                    ps.setString(1, entry.uuid.toString());
                    ps.setString(2, entry.type.name());
                    ps.setString(3, entry.content);
                    ps.setLong(4, entry.timestamp);
                    ps.addBatch();
                    count++;
                    if (count >= 500) {
                        ps.executeBatch();
                        count = 0;
                    }
                }
                if (count > 0) {
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (!shuttingDown) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to process activity log batch", e);
                }
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            if (!shuttingDown) {
                plugin.getLogger().log(Level.SEVERE, "Database error during activity log processing", e);
            }
        }
    }

    public void log(UUID uuid, LogType type, String content) {
        if (shuttingDown)
            return;

        LogEntry entry = new LogEntry(uuid, type, content, System.currentTimeMillis());
        logQueue.add(entry);

        if (plugin.getApiServer() != null) {
            plugin.getSchedulerAdapter().runTaskAsync(() -> {
                if (!shuttingDown) {
                    plugin.getApiServer().broadcastActivityLog(uuid, type, content);
                }
            });
        }
    }

    private static class LogEntry {
        final UUID uuid;
        final LogType type;
        final String content;
        final long timestamp;

        LogEntry(UUID uuid, LogType type, String content, long timestamp) {
            this.uuid = uuid;
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
        }
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

    public int getOffsetAtTimestamp(UUID uuid, LogType type, long timestamp) {
        String query = "SELECT COUNT(*) FROM activity_logs WHERE uuid = ? AND type = ? AND timestamp > ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.setLong(3, timestamp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public enum LogType {
        GENERAL, MESSAGE, ORDER, AUCTION, ITEM, INVENTORY, HAZARD, MONEY, SHARDS
    }
}
