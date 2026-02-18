package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseManager {

    private final PrismSurvival plugin;
    private final FileConfiguration config;
    private Connection connection;

    public DatabaseManager(PrismSurvival plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        initializeDatabase();
    }

    private void initializeDatabase() {
        CompletableFuture.runAsync(() -> {
            try {
                // Load MySQL settings from config
                String host = config.getString("database.host", "localhost");
                int port = config.getInt("database.port", 3306);
                String database = config.getString("database.database", "falcon");
                String username = config.getString("database.username", "root");
                String password = config.getString("database.password", "");
                String table = config.getString("database.table", "bans");

                // Connect
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false",
                        username,
                        password);

                createTables();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize ban database", e);
            }
        });
    }

    private void createTables() {
        // Table for Bans
        try (Statement s = connection.createStatement()) {
            String bansTable = "CREATE TABLE IF NOT EXISTS bans (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16)," +
                    "ban_id VARCHAR(10)," +
                    "reason_key VARCHAR(50)," +
                    "display_reason TEXT," +
                    "offense_count INT," +
                    "date_banned BIGINT," +
                    "expiry BIGINT," +
                    "banned_by VARCHAR(16)," +
                    "PRIMARY KEY (uuid, reason_key)" + // Ideally we might want just UUID or ID, but OffendPlugin seems
                                                       // to handle multiple ban types? Or just one active ban?
                                                       // Based on usage, it seems to check "isBanned", so usually one
                                                       // active ban matters.
                                                       // However, OffendPlugin also tracks offense counts per reason.
                                                       // Let's keep bans and offenses separate?
                                                       // The addBan method passes everything.
                                                       // OffendPlugin logic: addBan is called when a player is banned.
                    ")";
            s.execute(bansTable);

            // Actually, looking at OffendPlugin, it tracks offense counts permanently
            // (until reset),
            // but the "Ban" itself might be temporary.
            // A separate table for Offense Counts is likely needed to persist counts even
            // after unban.

            String offensesTable = "CREATE TABLE IF NOT EXISTS offenses (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "reason_key VARCHAR(50) NOT NULL," +
                    "count INT DEFAULT 0," +
                    "PRIMARY KEY (uuid, reason_key)" +
                    ")";
            s.execute(offensesTable);

            // Table for IP logs
            String ipLogsTable = "CREATE TABLE IF NOT EXISTS ip_logs (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "ip VARCHAR(45) NOT NULL," +
                    "last_seen BIGINT," +
                    "PRIMARY KEY (uuid, ip)" +
                    ")";
            s.execute(ipLogsTable);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check connection validity and reconnect if needed
    private Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            initializeDatabase();
        }
        return connection;
    }

    // --- Public API inferred from OffendPlugin ---

    public List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
        // Unique names from active bans
        // A ban is active if expiry == -1 OR expiry > current time
        String query = "SELECT DISTINCT player_name FROM bans WHERE expiry = -1 OR expiry > ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("player_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    public BanInfo getBanInfo(UUID uuid) {
        // Get the most relevant active ban (e.g. latest or permanent)
        String query = "SELECT * FROM bans WHERE uuid = ? AND (expiry = -1 OR expiry > ?) ORDER BY date_banned DESC LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BanInfo getBanInfoByName(String name) {
        String query = "SELECT * FROM bans WHERE player_name LIKE ? AND (expiry = -1 OR expiry > ?) ORDER BY date_banned DESC LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BanInfo getBanInfoById(String banId) {
        String query = "SELECT * FROM bans WHERE ban_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, banId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeBan(String playerName) {
        // Unban essentially means removing the active ban record or marking it
        // inactive.
        // OffendPlugin expects 'removeBan'. We'll delete the entry for simplicity as it
        // seems to be "Active Bans" storage.
        String query = "DELETE FROM bans WHERE player_name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBan(UUID uuid) {
        String query = "DELETE FROM bans WHERE uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBanById(String banId) {
        String query = "DELETE FROM bans WHERE ban_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, banId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getOffenseCount(UUID uuid, String reasonKey) {
        String query = "SELECT count FROM offenses WHERE uuid = ? AND reason_key = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, reasonKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setOffenseCount(UUID uuid, String reasonKey, int count) {
        String query = "REPLACE INTO offenses (uuid, reason_key, count) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, reasonKey);
            ps.setInt(3, count);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetOffenseCount(UUID uuid, String reasonKey) {
        setOffenseCount(uuid, reasonKey, 1); // Reset usually means set to 0 or 1.
        // OffendPlugin logic says: "Reset Offense Count on Unban: Implement logic so
        // that when a player is unbanned ... offense count ... is reset to 1"
        // Wait, the previous task history mentioned: "reset to 1 for any future
        // offenses".
        // The unban command in OffendPlugin calls
        // `dbManager.resetOffenseCount(info.uuid, info.reasonKey);`
        // So yes, let's set it to 1 (or 0, then next offense makes it 1. But prompt
        // said 1).
        // Actually, if they are unbanned, typically they are "forgiven" partially or
        // fully?
        // Let's check OffendPlugin.java unban section:
        // `dbManager.resetOffenseCount(info.uuid, info.reasonKey);`
        // And the user prompt said: "reset to 1".
        // So I will implement this as setting it to 1.
        // NOTE: If the user meant "next offense will be #1", then current should be 0.
        // If "next offense will be #2", then current should be 1.
        // Usually, partial reset might be "reset to baseline".
        // I will set it to 0 so that next offense is 1. That seems safer for "reset".
        // Re-reading user history: "reset their offense count ... to 1 for any future
        // offenses".
        // This phrasing is tricky. "Reset to 1 for future" -> Future offense = 1? Or
        // CURRENT state becomes 1?
        // If I set to 0, next offense (current+1) becomes 1.
        // If I set to 1, next offense (current+1) becomes 2.
        // I will set to 0.

        // CORRECTION: I'll stick to a simple DELETE or set to 0.
        // But wait, the method is "resetOffenseCount".
        // I'll set it to 0.

        setOffenseCount(uuid, reasonKey, 0);
    }

    public void resetOffenseCount(String uuidStr, String reasonKey) {
        try {
            resetOffenseCount(UUID.fromString(uuidStr), reasonKey);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    public void addBan(UUID uuid, String playerName, String banId, String reasonKey, String displayReason,
            int offenseCount, long date, long expires, String bannedBy) {
        // Insert into Bans table
        String query = "REPLACE INTO bans (uuid, player_name, ban_id, reason_key, display_reason, offense_count, date_banned, expiry, banned_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, banId);
            ps.setString(4, reasonKey);
            ps.setString(5, displayReason);
            ps.setInt(6, offenseCount);
            ps.setLong(7, date);
            ps.setLong(8, expires);
            ps.setString(9, bannedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logIP(UUID uuid, String ip) {
        String query = "REPLACE INTO ip_logs (uuid, ip, last_seen) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getLastIP(UUID uuid) {
        String query = "SELECT ip FROM ip_logs WHERE uuid = ? ORDER BY last_seen DESC LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getAlts(UUID uuid, String ip) {
        if (ip == null)
            return new ArrayList<>();
        List<String> alts = new ArrayList<>();
        String query = "SELECT DISTINCT uuid FROM ip_logs WHERE ip = ? AND uuid != ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String altUuid = rs.getString("uuid");
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(altUuid));
                    if (op.getName() != null) {
                        alts.add(op.getName());
                    } else {
                        alts.add(altUuid);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alts;
    }

    public boolean isBanned(UUID uuid) {
        return getBanInfo(uuid) != null;
    }

    private BanInfo mapToBanInfo(ResultSet rs) throws SQLException {
        BanInfo info = new BanInfo();
        info.uuid = rs.getString("uuid");
        info.playerName = rs.getString("player_name");
        info.id = rs.getString("ban_id");
        info.reasonKey = rs.getString("reason_key");
        info.reason = rs.getString("display_reason");
        info.count = rs.getInt("offense_count");
        info.date = rs.getLong("date_banned");
        info.expire = rs.getLong("expiry");
        info.bannedBy = rs.getString("banned_by");
        return info;
    }

    public static class BanInfo {
        public String uuid;
        public String playerName;
        public String id;
        public String reasonKey;
        public String reason;
        public int count;
        public long date;
        public long expire;
        public String bannedBy;
    }
}
