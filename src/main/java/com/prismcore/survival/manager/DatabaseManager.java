package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final PrismSurvival plugin;
    private final FileConfiguration config;
    private HikariDataSource dataSource;
    private String connectionError = null;

    public DatabaseManager(PrismSurvival plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Load MySQL settings from config
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String database = config.getString("database.database", "falcon");
            String username = config.getString("database.username", "root");
            String password = config.getString("database.password", "");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);

            // HikariCP optimization settings
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            // Connection health/timeout settings
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(300000); // 5 minutes
            hikariConfig.setMaxLifetime(600000); // 10 minutes
            hikariConfig.setKeepaliveTime(300000); // 5 minutes
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setMaximumPoolSize(10);

            this.dataSource = new HikariDataSource(hikariConfig);

            createTables();
        } catch (Exception e) {
            this.connectionError = e.getMessage();
            plugin.getLogger().log(Level.WARNING,
                    "Failed to initialize database with HikariCP. Some features will be unavailable.");
        }
    }

    private void createTables() {
        // Table for Bans
        try (Connection connection = getConnection();
                Statement s = connection.createStatement()) {
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

            String ipLogsTable = "CREATE TABLE IF NOT EXISTS ip_logs (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "ip VARCHAR(45) NOT NULL," +
                    "last_seen BIGINT," +
                    "PRIMARY KEY (uuid, ip)" +
                    ")";
            s.execute(ipLogsTable);

            // Table for Mutes
            String mutesTable = "CREATE TABLE IF NOT EXISTS mutes (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16)," +
                    "mute_id VARCHAR(10)," +
                    "reason TEXT," +
                    "date_muted BIGINT," +
                    "expiry BIGINT," +
                    "muted_by VARCHAR(16)," +
                    "PRIMARY KEY (uuid)" +
                    ")";
            s.execute(mutesTable);

            // Table for Allowed Operators
            String allowedOpsTable = "CREATE TABLE IF NOT EXISTS allowed_operators (" +
                    "player_name VARCHAR(16) NOT NULL," +
                    "PRIMARY KEY (player_name)" +
                    ")";
            s.execute(allowedOpsTable);

            // Table for Auction Pending Payments
            String auctionPendingTable = "CREATE TABLE IF NOT EXISTS auction_pending_payments (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "buyer_name VARCHAR(16) DEFAULT NULL," +
                    "item_name VARCHAR(100) DEFAULT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "PRIMARY KEY (uuid, timestamp)" +
                    ")";
            s.execute(auctionPendingTable);

            // Table for Player Inventories
            String inventoryTable = "CREATE TABLE IF NOT EXISTS player_inventories (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "inventory_data LONGTEXT," +
                    "armor_data LONGTEXT," +
                    "last_updated BIGINT," +
                    "PRIMARY KEY (uuid)" +
                    ")";
            s.execute(inventoryTable);

            // Table for Auction Transactions
            String transactionsTable = "CREATE TABLE IF NOT EXISTS auction_transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "item_data LONGTEXT NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "buyer_name VARCHAR(16) NOT NULL," +
                    "seller_name VARCHAR(16) NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "is_sale TINYINT(1) NOT NULL," +
                    "INDEX (player_uuid)," +
                    "INDEX (timestamp)" +
                    ")";
            s.execute(transactionsTable);

            // Table for Player Stats (Money and Shards)
            String statsTable = "CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "money DOUBLE DEFAULT 0," +
                    "shards BIGINT DEFAULT 0," +
                    "break_blocks BIGINT DEFAULT 0," +
                    "placed_blocks BIGINT DEFAULT 0," +
                    "mob_kills BIGINT DEFAULT 0," +
                    "sell_made DOUBLE DEFAULT 0," +
                    "shop_spent DOUBLE DEFAULT 0," +
                    "playtime BIGINT DEFAULT 0," +
                    "deaths BIGINT DEFAULT 0," +
                    "kills BIGINT DEFAULT 0," +
                    "`keys` BIGINT DEFAULT 0," +
                    "bounty DOUBLE DEFAULT 0," +
                    "tool_expiry BIGINT DEFAULT 0," +
                    "team VARCHAR(36) DEFAULT NULL," +
                    "name_hidden BOOLEAN DEFAULT FALSE," +
                    "status VARCHAR(16) DEFAULT 'Offline'," +
                    "last_world VARCHAR(64) DEFAULT NULL," +
                    "last_x DOUBLE DEFAULT 0," +
                    "last_y DOUBLE DEFAULT 0," +
                    "last_z DOUBLE DEFAULT 0," +
                    "last_yaw FLOAT DEFAULT 0," +
                    "last_pitch FLOAT DEFAULT 0," +
                    "ip VARCHAR(45) DEFAULT NULL," +
                    "last_updated BIGINT" +
                    ")";
            s.execute(statsTable);

            // Migration for Player Stats
            String[] statsColumns = {
                    "money DOUBLE DEFAULT 0",
                    "shards BIGINT DEFAULT 0",
                    "break_blocks BIGINT DEFAULT 0",
                    "placed_blocks BIGINT DEFAULT 0",
                    "mob_kills BIGINT DEFAULT 0",
                    "sell_made DOUBLE DEFAULT 0",
                    "shop_spent DOUBLE DEFAULT 0",
                    "playtime BIGINT DEFAULT 0",
                    "deaths BIGINT DEFAULT 0",
                    "kills BIGINT DEFAULT 0",
                    "`keys` BIGINT DEFAULT 0",
                    "bounty DOUBLE DEFAULT 0",
                    "tool_expiry BIGINT DEFAULT 0",
                    "team VARCHAR(36) DEFAULT NULL",
                    "name_hidden BOOLEAN DEFAULT FALSE",
                    "status VARCHAR(16) DEFAULT 'Offline'",
                    "last_world VARCHAR(64) DEFAULT NULL",
                    "last_x DOUBLE DEFAULT 0",
                    "last_y DOUBLE DEFAULT 0",
                    "last_z DOUBLE DEFAULT 0",
                    "last_yaw FLOAT DEFAULT 0",
                    "last_pitch FLOAT DEFAULT 0",
                    "ip VARCHAR(45) DEFAULT NULL"
            };

            for (String columnDef : statsColumns) {
                try {
                    s.execute("ALTER TABLE player_stats ADD COLUMN " + columnDef);
                } catch (SQLException ignored) {
                }
            }

            // Table for Orders
            String ordersTable = "CREATE TABLE IF NOT EXISTS prism_orders (" +
                    "id VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "owner VARCHAR(36) NOT NULL," +
                    "item_key TEXT NOT NULL," +
                    "requested INT NOT NULL," +
                    "delivered INT NOT NULL," +
                    "price_each DOUBLE NOT NULL," +
                    "paid DOUBLE NOT NULL," +
                    "canceled TINYINT(1) NOT NULL," +
                    "completed TINYINT(1) NOT NULL," +
                    "creation_time BIGINT NOT NULL," +
                    "storage LONGTEXT," + // Base64 serialized List<ItemStack>
                    "INDEX (owner)" +
                    ")";
            s.execute(ordersTable);

            // Table for Bounties
            String bountiesTable = "CREATE TABLE IF NOT EXISTS player_bounties (" +
                    "target_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "amount DOUBLE NOT NULL," +
                    "last_updated BIGINT NOT NULL" +
                    ")";
            s.execute(bountiesTable);

            // Table for Active Auction Items
            String activeAuctionsTable = "CREATE TABLE IF NOT EXISTS active_auction_listings (" +
                    "id VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "seller VARCHAR(16) NOT NULL," +
                    "item_stack LONGTEXT NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "listed_at BIGINT NOT NULL," +
                    "duration INT NOT NULL," +
                    "INDEX (seller)" +
                    ")";
            s.execute(activeAuctionsTable);

            // Table for Player Names (Caching for leaderboards)
            String playerNamesTable = "CREATE TABLE IF NOT EXISTS player_names (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "cached_name VARCHAR(16) NOT NULL" +
                    ")";
            s.execute(playerNamesTable);

            // Migration for Auction Pending Payments
            String[] auctionPendingColumns = {
                    "buyer_name VARCHAR(16) DEFAULT NULL",
                    "item_name VARCHAR(100) DEFAULT NULL"
            };

            for (String columnDef : auctionPendingColumns) {
                try {
                    s.execute("ALTER TABLE auction_pending_payments ADD COLUMN " + columnDef);
                } catch (SQLException ignored) {
                }
            }

            // Table for Server Configuration
            String serverConfigTable = "CREATE TABLE IF NOT EXISTS server_config (" +
                    "config_key VARCHAR(100) NOT NULL PRIMARY KEY," +
                    "config_value VARCHAR(255) NOT NULL," +
                    "last_updated BIGINT" +
                    ")";
            s.execute(serverConfigTable);

            // Table for Block History
            String blockHistoryTable = "CREATE TABLE IF NOT EXISTS block_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "world VARCHAR(64) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "action VARCHAR(20) NOT NULL," +
                    "block_type VARCHAR(50) NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "INDEX location_idx (world, x, y, z)," +
                    "INDEX timestamp_idx (timestamp)" +
                    ")";
            s.execute(blockHistoryTable);

            // Table for Player Chunk Visits (for movement tracking)
            String chunkVisitsTable = "CREATE TABLE IF NOT EXISTS player_chunk_visits (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "world VARCHAR(64) NOT NULL," +
                    "chunk_x INT NOT NULL," +
                    "chunk_z INT NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "visit_count INT DEFAULT 1," +
                    "first_visit BIGINT NOT NULL," +
                    "last_visit BIGINT NOT NULL," +
                    "UNIQUE KEY unique_player_chunk (world, chunk_x, chunk_z, player_uuid)," +
                    "INDEX chunk_idx (world, chunk_x, chunk_z)," +
                    "INDEX player_idx (player_uuid)," +
                    "INDEX timestamp_idx (last_visit)" +
                    ")";
            s.execute(chunkVisitsTable);

        } catch (SQLException e) {
            // Suppress stack trace when database is offline
        }
    }

    // Check connection validity and reconnect if needed
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public String getConnectionError() {
        return connectionError;
    }

    public boolean isRedisEnabled() {
        return false;
    }

    // --- Public API inferred from OffendPlugin ---

    public List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
        // Unique names from active bans
        // A ban is active if expiry == -1 OR expiry > current time
        if (!isConnected())
            return names;
        String query = "SELECT DISTINCT player_name FROM bans WHERE expiry = -1 OR expiry > ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("player_name"));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return names;
    }

    public BanInfo getBanInfo(UUID uuid) {
        if (!isConnected())
            return null;
        // Get the most relevant active ban (e.g. latest or permanent)
        String query = "SELECT * FROM bans WHERE uuid = ? AND (expiry = -1 OR expiry > ?) ORDER BY date_banned DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public BanInfo getBanInfoByName(String name) {
        if (!isConnected())
            return null;
        String query = "SELECT * FROM bans WHERE player_name LIKE ? AND (expiry = -1 OR expiry > ?) ORDER BY date_banned DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public BanInfo getBanInfoById(String banId) {
        if (!isConnected())
            return null;
        String query = "SELECT * FROM bans WHERE ban_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, banId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToBanInfo(rs);
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public void removeBan(String playerName) {
        if (!isConnected())
            return;
        // Unban essentially means removing the active ban record or marking it
        // inactive.
        // OffendPlugin expects 'removeBan'. We'll delete the entry for simplicity as it
        // seems to be "Active Bans" storage.
        String query = "DELETE FROM bans WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void removeBan(UUID uuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM bans WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void removeBanById(String banId) {
        if (!isConnected())
            return;
        String query = "DELETE FROM bans WHERE ban_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, banId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public int getOffenseCount(UUID uuid, String reasonKey) {
        String query = "SELECT count FROM offenses WHERE uuid = ? AND reason_key = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        if (!isConnected())
            return;
        String query = "REPLACE INTO offenses (uuid, reason_key, count) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, reasonKey);
            ps.setInt(3, count);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
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
        if (!isConnected())
            return;
        // Insert into Bans table
        String query = "REPLACE INTO bans (uuid, player_name, ban_id, reason_key, display_reason, offense_count, date_banned, expiry, banned_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
            // Silently fail
        }
    }

    public void logIP(UUID uuid, String ip) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO ip_logs (uuid, ip, last_seen) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public String getLastIP(UUID uuid) {
        if (!isConnected())
            return null;
        String query = "SELECT ip FROM ip_logs WHERE uuid = ? ORDER BY last_seen DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public List<String> getAlts(UUID uuid, String ip) {
        if (!isConnected() || ip == null)
            return new ArrayList<>();
        List<String> alts = new ArrayList<>();
        String query = "SELECT DISTINCT uuid FROM ip_logs WHERE ip = ? AND uuid != ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
            // Silently fail
        }
        return alts;
    }

    public boolean isBanned(UUID uuid) {
        return getBanInfo(uuid) != null;
    }

    // --- Mute Methods ---

    public void addMute(UUID uuid, String playerName, String muteId, String reason, long date, long expiry,
            String mutedBy) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO mutes (uuid, player_name, mute_id, reason, date_muted, expiry, muted_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, muteId);
            ps.setString(4, reason);
            ps.setLong(5, date);
            ps.setLong(6, expiry);
            ps.setString(7, mutedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void removeMute(UUID uuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM mutes WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public MuteInfo getMuteInfo(UUID uuid) {
        if (!isConnected())
            return null;
        String query = "SELECT * FROM mutes WHERE uuid = ? AND (expiry = -1 OR expiry > ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToMuteInfo(rs);
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public MuteInfo getMuteInfoByName(String name) {
        if (!isConnected())
            return null;
        String query = "SELECT * FROM mutes WHERE player_name LIKE ? AND (expiry = -1 OR expiry > ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToMuteInfo(rs);
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public List<String> getMutedPlayerNames() {
        if (!isConnected())
            return new ArrayList<>();
        List<String> names = new ArrayList<>();
        String query = "SELECT DISTINCT player_name FROM mutes WHERE expiry = -1 OR expiry > ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("player_name"));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return names;
    }

    private MuteInfo mapToMuteInfo(ResultSet rs) throws SQLException {
        MuteInfo info = new MuteInfo();
        info.uuid = rs.getString("uuid");
        info.playerName = rs.getString("player_name");
        info.id = rs.getString("mute_id");
        info.reason = rs.getString("reason");
        info.date = rs.getLong("date_muted");
        info.expire = rs.getLong("expiry");
        info.mutedBy = rs.getString("muted_by");
        return info;
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

    public static class MuteInfo {
        public String uuid;
        public String playerName;
        public String id;
        public String reason;
        public long date;
        public long expire;
        public String mutedBy;
    }

    // --- Allowed Operators Methods ---

    public void addAllowedOperator(String playerName) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO allowed_operators (player_name) VALUES (?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void removeAllowedOperator(String playerName) {
        if (!isConnected())
            return;
        String query = "DELETE FROM allowed_operators WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public boolean isAllowedOperator(String playerName) {
        if (!isConnected())
            return false;
        String query = "SELECT player_name FROM allowed_operators WHERE player_name = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return false;
    }

    public java.util.List<String> getAllowedOperators() {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (!isConnected())
            return names;
        String query = "SELECT player_name FROM allowed_operators";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("player_name"));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return names;
    }

    public void addAuctionPendingPayment(UUID uuid, double amount, String buyerName, String itemName) {
        if (!isConnected())
            return;
        String query = "INSERT INTO auction_pending_payments (uuid, amount, buyer_name, item_name, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.setString(3, buyerName);
            ps.setString(4, itemName);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public List<com.prismcore.survival.auction.AuctionManager.OfflineSale> getAndClearDetailedPendingSales(UUID uuid) {
        List<com.prismcore.survival.auction.AuctionManager.OfflineSale> sales = new ArrayList<>();
        if (!isConnected())
            return sales;
        String selectQuery = "SELECT amount, buyer_name, item_name FROM auction_pending_payments WHERE uuid = ?";
        String deleteQuery = "DELETE FROM auction_pending_payments WHERE uuid = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psSelect = conn.prepareStatement(selectQuery)) {
                    psSelect.setString(1, uuid.toString());
                    try (ResultSet rs = psSelect.executeQuery()) {
                        while (rs.next()) {
                            double amount = rs.getDouble("amount");
                            String buyer = rs.getString("buyer_name");
                            String item = rs.getString("item_name");
                            sales.add(new com.prismcore.survival.auction.AuctionManager.OfflineSale(
                                    buyer != null ? buyer : "Unknown",
                                    item != null ? item : "Unknown",
                                    amount));
                        }
                    }
                }
                if (!sales.isEmpty()) {
                    try (PreparedStatement psDelete = conn.prepareStatement(deleteQuery)) {
                        psDelete.setString(1, uuid.toString());
                        psDelete.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                // Silently fail
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return sales;
    }

    // Keep legacy for compatibility during transitions if needed, or remove if
    // fully updated
    public double getAndClearAuctionPendingPayments(UUID uuid) {
        List<com.prismcore.survival.auction.AuctionManager.OfflineSale> sales = getAndClearDetailedPendingSales(uuid);
        return sales.stream().mapToDouble(s -> s.price).sum();
    }

    // --- Inventory Sync Methods ---

    public void saveInventory(UUID uuid, String inventoryBase64, String armorBase64) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO player_inventories (uuid, inventory_data, armor_data, last_updated) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, inventoryBase64);
            ps.setString(3, armorBase64);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public String[] loadInventory(UUID uuid) {
        if (!isConnected())
            return null;
        String query = "SELECT inventory_data, armor_data FROM player_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[] { rs.getString("inventory_data"), rs.getString("armor_data") };
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    // --- Auction Transaction Methods ---

    public void addAuctionTransaction(UUID playerUuid, com.prismcore.survival.auction.Transaction tx) {
        if (!isConnected())
            return;
        String query = "INSERT INTO auction_transactions (player_uuid, item_data, price, buyer_name, seller_name, timestamp, is_sale) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            String itemData = com.prismcore.survival.utils.ItemSerializationManager
                    .itemStackArrayToBase64(new org.bukkit.inventory.ItemStack[] { tx.getItem() });
            ps.setString(2, itemData);
            ps.setDouble(3, tx.getPrice());
            ps.setString(4, tx.getBuyer());
            ps.setString(5, tx.getSeller());
            ps.setLong(6, tx.getTimestamp());
            ps.setBoolean(7, tx.isSale());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public List<com.prismcore.survival.auction.Transaction> getAuctionTransactions(UUID playerUuid) {
        List<com.prismcore.survival.auction.Transaction> list = new ArrayList<>();
        if (!isConnected())
            return list;
        String query = "SELECT * FROM auction_transactions WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT 50";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemData = rs.getString("item_data");
                    org.bukkit.inventory.ItemStack[] items = com.prismcore.survival.utils.ItemSerializationManager
                            .itemStackArrayFromBase64(itemData);
                    if (items.length > 0) {
                        list.add(new com.prismcore.survival.auction.Transaction(
                                items[0],
                                rs.getDouble("price"),
                                rs.getString("buyer_name"),
                                rs.getString("seller_name"),
                                rs.getLong("timestamp"),
                                rs.getBoolean("is_sale")));
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return list;
    }

    public void deleteAuctionTransaction(UUID playerUuid, long timestamp, double price) {
        if (!isConnected())
            return;
        String query = "DELETE FROM auction_transactions WHERE player_uuid = ? AND timestamp = ? AND price = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, timestamp);
            ps.setDouble(3, price);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    // --- Player Stats Methods ---

    public void savePlayerStats(UUID uuid, PlayerData data) {
        if (!isConnected())
            return;
        String query = "UPDATE player_stats SET money = ?, shards = ?, shop_spent = ?, ip = ?, last_updated = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, data.getMoney());
            ps.setDouble(2, data.getShards());
            ps.setDouble(3, data.getShopSpent());
            ps.setString(4, data.getIp());
            ps.setLong(5, System.currentTimeMillis());
            ps.setString(6, uuid.toString());
            int affected = ps.executeUpdate();

            // Fallback to INSERT if update failed (on duplicate key update is also possible
            // but more complex with this schema if not primary key)
            if (affected == 0) {
                String insertQuery = "INSERT INTO player_stats (uuid, money, shards, shop_spent, ip, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ips = conn.prepareStatement(insertQuery)) {
                    ips.setString(1, uuid.toString());
                    ips.setDouble(2, data.getMoney());
                    ips.setDouble(3, data.getShards());
                    ips.setDouble(4, data.getShopSpent());
                    ips.setString(5, data.getIp());
                    ips.setLong(6, System.currentTimeMillis());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public PlayerDataStats loadPlayerStats(UUID uuid) {
        if (!isConnected())
            return null;
        String query = "SELECT money, shards, shop_spent, ip FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerDataStats(rs.getDouble("money"), rs.getDouble("shards"),
                            rs.getDouble("shop_spent"), rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return null;
    }

    public static class PlayerDataStats {
        public double money;
        public double shards;
        public double shopSpent;
        public String ip;

        public PlayerDataStats(double money, double shards, double shopSpent, String ip) {
            this.money = money;
            this.shards = shards;
            this.shopSpent = shopSpent;
            this.ip = ip;
        }
    }

    public void savePlayerName(UUID uuid, String name) {
        if (!isConnected() || name == null)
            return;
        String query = "REPLACE INTO player_names (uuid, cached_name) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public List<PlayerDataManager.LeaderboardEntry> getTopShards(int limit) {
        List<PlayerDataManager.LeaderboardEntry> entries = new ArrayList<>();
        if (!isConnected())
            return entries;
        String query = "SELECT ps.uuid, ps.shards, COALESCE(pn.cached_name, ps.uuid) as name " +
                "FROM player_stats ps " +
                "LEFT JOIN player_names pn ON ps.uuid = pn.uuid " +
                "WHERE ps.shards > 0 ORDER BY ps.shards DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new PlayerDataManager.LeaderboardEntry(
                            rs.getString("name"),
                            UUID.fromString(rs.getString("uuid")),
                            rs.getDouble("shards")));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return entries;
    }

    public List<PlayerDataManager.LeaderboardEntry> getTopMoney(int limit) {
        List<PlayerDataManager.LeaderboardEntry> entries = new ArrayList<>();
        if (!isConnected())
            return entries;
        String query = "SELECT ps.uuid, ps.money, COALESCE(pn.cached_name, ps.uuid) as name " +
                "FROM player_stats ps " +
                "LEFT JOIN player_names pn ON ps.uuid = pn.uuid " +
                "WHERE ps.money > 0 ORDER BY ps.money DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new PlayerDataManager.LeaderboardEntry(
                            rs.getString("name"),
                            UUID.fromString(rs.getString("uuid")),
                            rs.getDouble("money")));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return entries;
    }

    public void updateOfflineBalance(UUID uuid, double balance, boolean isShards) {
        if (!isConnected())
            return;
        String column = isShards ? "shards" : "money";
        String query = "UPDATE player_stats SET " + column + " = ?, last_updated = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, balance);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    // --- Bounty Persistence Methods ---

    public java.util.Map<UUID, Double> loadAllBounties() {
        java.util.Map<UUID, Double> bounties = new java.util.HashMap<>();
        if (!isConnected())
            return bounties;
        String query = "SELECT target_uuid, amount FROM player_bounties";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bounties.put(UUID.fromString(rs.getString("target_uuid")), rs.getDouble("amount"));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return bounties;
    }

    public void saveBounty(UUID target, double amount) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO player_bounties (target_uuid, amount, last_updated) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, target.toString());
            ps.setDouble(2, amount);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void deleteBounty(UUID target) {
        if (!isConnected())
            return;
        String query = "DELETE FROM player_bounties WHERE target_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, target.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    // --- Order Persistence Methods ---

    public java.util.List<com.prismcore.survival.orders.data.Order> loadAllOrders() {
        java.util.List<com.prismcore.survival.orders.data.Order> orders = new java.util.ArrayList<>();
        if (!isConnected())
            return orders;
        String query = "SELECT * FROM prism_orders";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID owner = UUID.fromString(rs.getString("owner"));
                        String itemKey = rs.getString("item_key");
                        int requested = rs.getInt("requested");
                        int delivered = rs.getInt("delivered");
                        double priceEach = rs.getDouble("price_each");
                        double paid = rs.getDouble("paid");
                        boolean canceled = rs.getBoolean("canceled");
                        boolean completed = rs.getBoolean("completed");
                        long creationTime = rs.getLong("creation_time");
                        String storageBase64 = rs.getString("storage");

                        com.prismcore.survival.orders.data.Order order = new com.prismcore.survival.orders.data.Order(
                                id, owner, com.prismcore.survival.orders.data.ItemKey.deserialize(itemKey), requested,
                                delivered, priceEach, paid, canceled, completed,
                                creationTime);

                        if (storageBase64 != null && !storageBase64.isEmpty()) {
                            order.setStorage(com.prismcore.survival.utils.ItemSerializationManager
                                    .itemStackListFromBase64(storageBase64));
                        }
                        orders.add(order);
                    } catch (Exception e) {
                        // Silently fail parse error
                    }
                }
            }
        } catch (SQLException e) {
            // Silently fail loading
        }
        return orders;
    }

    public void saveOrder(com.prismcore.survival.orders.data.Order order) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO prism_orders (id, owner, item_key, requested, delivered, price_each, paid, canceled, completed, creation_time, storage) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, order.getId().toString());
            ps.setString(2, order.getOwner().toString());
            ps.setString(3, order.getItemKey());
            ps.setInt(4, order.getRequested());
            ps.setInt(5, order.getDelivered());
            ps.setDouble(6, order.getPriceEach());
            ps.setDouble(7, order.getPaid());
            ps.setBoolean(8, order.isCanceled());
            ps.setBoolean(9, order.isCompleted());
            ps.setLong(10, order.getCreationTime());

            String storageBase64 = "";
            if (order.getStorage() != null && !order.getStorage().isEmpty()) {
                storageBase64 = com.prismcore.survival.utils.ItemSerializationManager
                        .itemStackListToBase64(order.getStorage());
            }
            ps.setString(11, storageBase64);

            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    /**
     * ANTI-DUPE: Fetch a single order by ID from database for validation
     * @param orderId The UUID of the order to fetch
     * @return Order object or null if not found
     */
    public com.prismcore.survival.orders.data.Order getOrderById(UUID orderId) {
        if (!isConnected())
            return null;
        String query = "SELECT * FROM prism_orders WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, orderId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID owner = UUID.fromString(rs.getString("owner"));
                        String itemKey = rs.getString("item_key");
                        int requested = rs.getInt("requested");
                        int delivered = rs.getInt("delivered");
                        double priceEach = rs.getDouble("price_each");
                        double paid = rs.getDouble("paid");
                        boolean canceled = rs.getBoolean("canceled");
                        boolean completed = rs.getBoolean("completed");
                        long creationTime = rs.getLong("creation_time");
                        String storageBase64 = rs.getString("storage");

                        com.prismcore.survival.orders.data.Order order = new com.prismcore.survival.orders.data.Order(
                                id, owner, com.prismcore.survival.orders.data.ItemKey.deserialize(itemKey), requested,
                                delivered, priceEach, paid, canceled, completed,
                                creationTime);

                        if (storageBase64 != null && !storageBase64.isEmpty()) {
                            order.setStorage(com.prismcore.survival.utils.ItemSerializationManager
                                    .itemStackListFromBase64(storageBase64));
                        }
                        return order;
                    } catch (Exception e) {
                        // Silently fail parse error
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            // Silently fail loading
        }
        return null;
    }

    public void deleteOrder(UUID orderId) {
        if (!isConnected())
            return;
        String query = "DELETE FROM prism_orders WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, orderId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    // --- Auction Items Persistence Methods ---

    public java.util.List<com.prismcore.survival.auction.AuctionItem> loadAllAuctionItems() {
        java.util.List<com.prismcore.survival.auction.AuctionItem> items = new java.util.ArrayList<>();
        if (!isConnected())
            return items;
        String query = "SELECT * FROM active_auction_listings";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID id = UUID.fromString(rs.getString("id"));
                        String seller = rs.getString("seller");
                        String itemBase64 = rs.getString("item_stack");
                        double price = rs.getDouble("price");
                        long listedAt = rs.getLong("listed_at");
                        int duration = rs.getInt("duration");

                        org.bukkit.inventory.ItemStack itemStack = com.prismcore.survival.utils.ItemSerializationManager
                                .itemStackArrayFromBase64(itemBase64)[0];
                        items.add(new com.prismcore.survival.auction.AuctionItem(id, seller, itemStack, price, listedAt,
                                duration));
                    } catch (Exception e) {
                        // Silently fail parse error
                    }
                }
            }
        } catch (SQLException e) {
            // Silently fail loading
        }
        return items;
    }

    public void saveAuctionItem(com.prismcore.survival.auction.AuctionItem item) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO active_auction_listings (id, seller, item_stack, price, listed_at, duration) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, item.getId().toString());
            ps.setString(2, item.getSeller());

            String itemBase64 = com.prismcore.survival.utils.ItemSerializationManager
                    .itemStackArrayToBase64(new org.bukkit.inventory.ItemStack[] { item.getItemStack() });
            ps.setString(3, itemBase64);

            ps.setDouble(4, item.getPrice());
            ps.setLong(5, item.getListedAt());
            ps.setInt(6, item.getDuration());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void saveAuctionItemAsync(com.prismcore.survival.auction.AuctionItem item) {
        CompletableFuture.runAsync(() -> saveAuctionItem(item));
    }

    public void deleteAuctionItem(UUID itemId) {
        if (!isConnected())
            return;
        String query = "DELETE FROM active_auction_listings WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, itemId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void deleteAuctionItemAsync(UUID itemId) {
        CompletableFuture.runAsync(() -> deleteAuctionItem(itemId));
    }

    public void savePlayerNameAsync(UUID uuid, String name) {
        CompletableFuture.runAsync(() -> savePlayerName(uuid, name));
    }

    public void addAuctionTransactionAsync(UUID playerUuid, com.prismcore.survival.auction.Transaction tx) {
        CompletableFuture.runAsync(() -> addAuctionTransaction(playerUuid, tx));
    }

    public void deleteAuctionTransactionAsync(UUID playerUuid, long timestamp, double price) {
        CompletableFuture.runAsync(() -> deleteAuctionTransaction(playerUuid, timestamp, price));
    }

    public void updateStatusAsync(UUID uuid, String status) {
        if (!isConnected())
            return;
        CompletableFuture.runAsync(() -> {
            String query = "UPDATE player_stats SET status = ? WHERE uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, status);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                // Silently fail
            }
        });
    }

    public void saveLastLocationAsync(UUID uuid, org.bukkit.Location loc) {
        if (!isConnected() || loc == null || loc.getWorld() == null)
            return;
        CompletableFuture.runAsync(() -> {
            String query = "UPDATE player_stats SET last_world = ?, last_x = ?, last_y = ?, last_z = ?, last_yaw = ?, last_pitch = ? WHERE uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, loc.getWorld().getName());
                ps.setDouble(2, loc.getX());
                ps.setDouble(3, loc.getY());
                ps.setDouble(4, loc.getZ());
                ps.setFloat(5, loc.getYaw());
                ps.setFloat(6, loc.getPitch());
                ps.setString(7, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                // Silently fail
            }
        });
    }

    public void getOfflinePlayersAsync(java.util.function.Consumer<java.util.List<String>> callback) {
        if (!isConnected()) {
            callback.accept(new ArrayList<>());
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            java.util.List<String> names = new ArrayList<>();
            String query = "SELECT pn.cached_name FROM player_stats ps JOIN player_names pn ON ps.uuid = pn.uuid WHERE ps.status = 'Offline'";
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(query);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("cached_name"));
                }
            } catch (SQLException e) {
                // Silently fail
            }
            return names;
        }).thenAccept(callback);
    }

    public void getLastLocationAsync(UUID uuid, java.util.function.Consumer<org.bukkit.Location> callback) {
        if (!isConnected()) {
            callback.accept(null);
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            String query = "SELECT last_world, last_x, last_y, last_z, last_yaw, last_pitch FROM player_stats WHERE uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String worldName = rs.getString("last_world");
                        if (worldName == null)
                            return null;
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world == null)
                            return null;
                        return new org.bukkit.Location(world, rs.getDouble("last_x"), rs.getDouble("last_y"),
                                rs.getDouble("last_z"), rs.getFloat("last_yaw"), rs.getFloat("last_pitch"));
                    }
                }
            } catch (SQLException e) {
                // Silently fail
            }
            return null;
        }).thenAccept(callback);
    }

    public static class AltInfo {
        public final String name;
        public final String status;

        public AltInfo(String name, String status) {
            this.name = name;
            this.status = status;
        }
    }

    public void getAltsByIpAsync(String ip, java.util.function.Consumer<List<AltInfo>> callback) {
        if (!isConnected() || ip == null) {
            callback.accept(new ArrayList<>());
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            List<AltInfo> alts = new ArrayList<>();
            String query = "SELECT pn.cached_name, ps.status FROM player_stats ps " +
                    "JOIN player_names pn ON ps.uuid = pn.uuid " +
                    "WHERE ps.ip = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, ip);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        alts.add(new AltInfo(rs.getString("cached_name"), rs.getString("status")));
                    }
                }
            } catch (SQLException e) {
                // Silently fail
            }
            return alts;
        }).thenAccept(callback);
    }

    public void wipeAuctionTransactions(UUID playerUuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM auction_transactions WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void wipeOrders(UUID playerUuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM prism_orders WHERE owner = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public void wipeInventory(UUID uuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM player_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to wipe inventory for " + uuid, e);
        }
    }

    public void wipePlayerStats(UUID uuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to wipe player stats for " + uuid, e);
        }
    }

    public void wipeAuctionPendingPayments(UUID uuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM auction_pending_payments WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to wipe auction pending payments for " + uuid, e);
        }
    }

    // --- Server Configuration Methods ---

    public void setServerConfig(String key, String value) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO server_config (config_key, config_value, last_updated) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public String getServerConfig(String key, String defaultValue) {
        if (!isConnected())
            return defaultValue;
        String query = "SELECT config_value FROM server_config WHERE config_key = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return defaultValue;
    }

    public double getServerConfigDouble(String key, double defaultValue) {
        String value = getServerConfig(key, null);
        if (value == null)
            return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // --- Block History Methods ---

    public void recordBlockAction(org.bukkit.Location location, String playerName, UUID playerUuid, String action, String blockType, long timestamp) {
        if (!isConnected()) return;
        
        String query = "INSERT INTO block_history (world, x, y, z, player_uuid, player_name, action, block_type, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            ps.setString(5, playerUuid.toString());
            ps.setString(6, playerName);
            ps.setString(7, action);
            ps.setString(8, blockType);
            ps.setLong(9, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }

    public List<com.h2ph.listeners.HistoryListener.BlockHistoryEntry> getBlockHistory(org.bukkit.Location location) {
        List<com.h2ph.listeners.HistoryListener.BlockHistoryEntry> history = new ArrayList<>();
        if (!isConnected()) return history;
        
        String query = "SELECT player_name, action, block_type, timestamp FROM block_history WHERE world = ? AND x = ? AND y = ? AND z = ? ORDER BY timestamp DESC LIMIT 10";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new com.h2ph.listeners.HistoryListener.BlockHistoryEntry(
                        rs.getString("player_name"),
                        rs.getString("action"),
                        rs.getString("block_type"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        return history;
    }

    public List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> getChunkHistory(String world, int chunkX, int chunkZ) {
        List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> history = new ArrayList<>();
        if (!isConnected()) return history;
        
        // Calculate block coordinates for the chunk boundaries
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;
        
        String query = "SELECT player_name, COUNT(*) as action_count, MAX(timestamp) as last_activity " +
                       "FROM block_history " +
                       "WHERE world = ? AND x >= ? AND x <= ? AND z >= ? AND z <= ? " +
                       "GROUP BY player_name " +
                       "ORDER BY action_count DESC, last_activity DESC";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, world);
            ps.setInt(2, minX);
            ps.setInt(3, maxX);
            ps.setInt(4, minZ);
            ps.setInt(5, maxZ);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int actionCount = rs.getInt("action_count");
                    long lastActivity = rs.getLong("last_activity");
                    
                    history.add(new com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry(
                        playerName, actionCount, lastActivity));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        
        return history;
    }

    /**
     * Get chunk history for a 5x5 area centered on the specified chunk
     * @param world The world name
     * @param centerChunkX The center chunk X coordinate
     * @param centerChunkZ The center chunk Z coordinate
     * @return List of ChunkHistoryEntry objects representing player activity in the area
     */
    public List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> getChunkAreaHistory(String world, int centerChunkX, int centerChunkZ) {
        List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> history = new ArrayList<>();
        if (!isConnected()) return history;
        
        // Calculate 5x5 chunk area (2 chunks in each direction from center)
        int radius = 2;
        int minChunkX = centerChunkX - radius;
        int maxChunkX = centerChunkX + radius;
        int minChunkZ = centerChunkZ - radius;
        int maxChunkZ = centerChunkZ + radius;
        
        // Convert to block coordinates
        int minX = minChunkX * 16;
        int maxX = (maxChunkX * 16) + 15;
        int minZ = minChunkZ * 16;
        int maxZ = (maxChunkZ * 16) + 15;
        
        String query = "SELECT player_name, COUNT(*) as action_count, MAX(timestamp) as last_activity " +
                       "FROM block_history " +
                       "WHERE world = ? AND x >= ? AND x <= ? AND z >= ? AND z <= ? " +
                       "GROUP BY player_name " +
                       "ORDER BY action_count DESC, last_activity DESC";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, world);
            ps.setInt(2, minX);
            ps.setInt(3, maxX);
            ps.setInt(4, minZ);
            ps.setInt(5, maxZ);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int actionCount = rs.getInt("action_count");
                    long lastActivity = rs.getLong("last_activity");
                    
                    history.add(new com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry(
                        playerName, actionCount, lastActivity));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        
        return history;
    }

    /**
     * Ensure the player_chunk_visits table exists (creates if doesn't exist)
     */
    private void ensureChunkVisitsTableExists() {
        if (!isConnected()) return;
        
        String createTable = "CREATE TABLE IF NOT EXISTS player_chunk_visits (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "world VARCHAR(64) NOT NULL," +
                "chunk_x INT NOT NULL," +
                "chunk_z INT NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "visit_count INT DEFAULT 1," +
                "first_visit BIGINT NOT NULL," +
                "last_visit BIGINT NOT NULL," +
                "UNIQUE KEY unique_player_chunk (world, chunk_x, chunk_z, player_uuid)," +
                "INDEX chunk_idx (world, chunk_x, chunk_z)," +
                "INDEX player_idx (player_uuid)," +
                "INDEX timestamp_idx (last_visit)" +
                ")";
        
        try (Connection conn = getConnection(); Statement s = conn.createStatement()) {
            s.execute(createTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create player_chunk_visits table: " + e.getMessage());
        }
    }

    /**
     * Record a player's visit to a chunk (for movement tracking)
     */
    public void recordChunkVisit(String world, int chunkX, int chunkZ, UUID playerUuid, String playerName) {
        if (!isConnected()) return;
        
        // Ensure table exists before attempting to use it
        ensureChunkVisitsTableExists();
        
        String query = "INSERT INTO player_chunk_visits (world, chunk_x, chunk_z, player_uuid, player_name, first_visit, last_visit) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE " +
                       "visit_count = visit_count + 1, " +
                       "last_visit = VALUES(last_visit), " +
                       "player_name = VALUES(player_name)";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            long now = System.currentTimeMillis();
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, playerUuid.toString());
            ps.setString(5, playerName);
            ps.setLong(6, now);
            ps.setLong(7, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail to avoid spamming logs
        }
    }

    /**
     * Get chunk visit history for a single chunk
     */
    public List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> getChunkVisitHistory(String world, int chunkX, int chunkZ) {
        List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> history = new ArrayList<>();
        if (!isConnected()) return history;
        
        // Ensure table exists before attempting to query it
        ensureChunkVisitsTableExists();
        
        String query = "SELECT player_name, visit_count, last_visit " +
                       "FROM player_chunk_visits " +
                       "WHERE world = ? AND chunk_x = ? AND chunk_z = ? " +
                       "ORDER BY visit_count DESC, last_visit DESC";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int visitCount = rs.getInt("visit_count");
                    long lastVisit = rs.getLong("last_visit");
                    
                    history.add(new com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry(
                        playerName, visitCount, lastVisit));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        
        return history;
    }

    /**
     * Get chunk visit history for a 5x5 area centered on the specified chunk
     */
    public List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> getChunkAreaVisitHistory(String world, int centerChunkX, int centerChunkZ) {
        List<com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry> history = new ArrayList<>();
        if (!isConnected()) return history;
        
        // Ensure table exists before attempting to query it
        ensureChunkVisitsTableExists();
        
        // Calculate 5x5 chunk area (2 chunks in each direction from center)
        int radius = 2;
        int minChunkX = centerChunkX - radius;
        int maxChunkX = centerChunkX + radius;
        int minChunkZ = centerChunkZ - radius;
        int maxChunkZ = centerChunkZ + radius;
        
        String query = "SELECT player_name, SUM(visit_count) as total_visits, MAX(last_visit) as last_activity " +
                       "FROM player_chunk_visits " +
                       "WHERE world = ? AND chunk_x >= ? AND chunk_x <= ? AND chunk_z >= ? AND chunk_z <= ? " +
                       "GROUP BY player_name " +
                       "ORDER BY total_visits DESC, last_activity DESC";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, world);
            ps.setInt(2, minChunkX);
            ps.setInt(3, maxChunkX);
            ps.setInt(4, minChunkZ);
            ps.setInt(5, maxChunkZ);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int totalVisits = rs.getInt("total_visits");
                    long lastActivity = rs.getLong("last_activity");
                    
                    history.add(new com.h2ph.commands.admin.moderations.WhoWasHereCommand.ChunkHistoryEntry(
                        playerName, totalVisits, lastActivity));
                }
            }
        } catch (SQLException e) {
            // Silently fail
        }
        
        return history;
    }

    public void cleanupOldBlockHistory(long daysToKeep) {
        if (!isConnected()) return;
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000);
        String query = "DELETE FROM block_history WHERE timestamp < ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, cutoffTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently fail
        }
    }
    public void setServerConfigDouble(String key, double value) {
        setServerConfig(key, String.valueOf(value));
    }
}
