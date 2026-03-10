package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;

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

            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(600000);
            hikariConfig.setKeepaliveTime(300000);
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
                    "PRIMARY KEY (uuid, reason_key)" +
                    ")";
            s.execute(bansTable);

            String offensesTable = "CREATE TABLE IF NOT EXISTS offenses (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "reason_key VARCHAR(50) NOT NULL," +
                    "count INT DEFAULT 0," +
                    "PRIMARY KEY (uuid, reason_key)" +
                    ")";
            s.execute(offensesTable);

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

            String allowedOpsTable = "CREATE TABLE IF NOT EXISTS allowed_operators (" +
                    "player_name VARCHAR(16) NOT NULL," +
                    "PRIMARY KEY (player_name)" +
                    ")";
            s.execute(allowedOpsTable);

            String auctionPendingTable = "CREATE TABLE IF NOT EXISTS auction_pending_payments (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "buyer_name VARCHAR(16) DEFAULT NULL," +
                    "item_name VARCHAR(100) DEFAULT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "PRIMARY KEY (uuid, timestamp)" +
                    ")";
            s.execute(auctionPendingTable);

            String inventoryTable = "CREATE TABLE IF NOT EXISTS player_inventories (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "inventory_data LONGTEXT," +
                    "armor_data LONGTEXT," +
                    "last_updated BIGINT," +
                    "PRIMARY KEY (uuid)" +
                    ")";
            s.execute(inventoryTable);

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
                    "disguised BOOLEAN DEFAULT FALSE," +
                    "disguise_name VARCHAR(16) DEFAULT NULL," +
                    "disguise_skin_texture TEXT DEFAULT NULL," +
                    "disguise_skin_signature TEXT DEFAULT NULL," +
                    "last_updated BIGINT" +
                    ")";
            s.execute(statsTable);

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
                    "ip VARCHAR(45) DEFAULT NULL",
                    "disguised BOOLEAN DEFAULT FALSE",
                    "disguise_name VARCHAR(16) DEFAULT NULL",
                    "disguise_skin_texture TEXT DEFAULT NULL",
                    "disguise_skin_signature TEXT DEFAULT NULL"
            };

            for (String columnDef : statsColumns) {
                try {
                    s.execute("ALTER TABLE player_stats ADD COLUMN " + columnDef);
                } catch (SQLException ignored) {
                }
            }

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
                    "storage LONGTEXT," +
                    "INDEX (owner)" +
                    ")";
            s.execute(ordersTable);

            String bountiesTable = "CREATE TABLE IF NOT EXISTS player_bounties (" +
                    "target_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "amount DOUBLE NOT NULL," +
                    "last_updated BIGINT NOT NULL" +
                    ")";
            s.execute(bountiesTable);

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

            String playerNamesTable = "CREATE TABLE IF NOT EXISTS player_names (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "cached_name VARCHAR(16) NOT NULL" +
                    ")";
            s.execute(playerNamesTable);

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

            String serverConfigTable = "CREATE TABLE IF NOT EXISTS server_config (" +
                    "config_key VARCHAR(100) NOT NULL PRIMARY KEY," +
                    "config_value VARCHAR(255) NOT NULL," +
                    "last_updated BIGINT" +
                    ")";
            s.execute(serverConfigTable);

            String pvpSafeZonesTable = "CREATE TABLE IF NOT EXISTS pvp_safe_zones (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(64) NOT NULL UNIQUE," +
                    "world VARCHAR(64) NOT NULL," +
                    "min_x DOUBLE NOT NULL," +
                    "min_y DOUBLE NOT NULL," +
                    "min_z DOUBLE NOT NULL," +
                    "max_x DOUBLE NOT NULL," +
                    "max_y DOUBLE NOT NULL," +
                    "max_z DOUBLE NOT NULL," +
                    "created_by VARCHAR(36) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "INDEX name_idx (name)," +
                    "INDEX world_idx (world)" +
                    ")";
            s.execute(pvpSafeZonesTable);

            String temporaryBlocksTable = "CREATE TABLE IF NOT EXISTS temporary_blocks (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "world VARCHAR(64) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "original_material VARCHAR(50) NOT NULL," +
                    "original_data TEXT," +
                    "placed_time BIGINT NOT NULL," +
                    "INDEX world_idx (world)," +
                    "INDEX time_idx (placed_time)" +
                    ")";
            s.execute(temporaryBlocksTable);

        } catch (SQLException e) {
        }
    }

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

    public List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
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
        }
        return names;
    }

    public BanInfo getBanInfo(UUID uuid) {
        if (!isConnected())
            return null;
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
        }
        return null;
    }

    public void removeBan(String playerName) {
        if (!isConnected())
            return;
        String query = "DELETE FROM bans WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
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
        }
    }

    public void resetOffenseCount(UUID uuid, String reasonKey) {
        setOffenseCount(uuid, reasonKey, 1);

        setOffenseCount(uuid, reasonKey, 0);
    }

    public void resetOffenseCount(String uuidStr, String reasonKey) {
        try {
            resetOffenseCount(UUID.fromString(uuidStr), reasonKey);
        } catch (IllegalArgumentException e) {
        }
    }

    public void addBan(UUID uuid, String playerName, String banId, String reasonKey, String displayReason,
            int offenseCount, long date, long expires, String bannedBy) {
        if (!isConnected())
            return;
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
        }
    }

    public void logIP(UUID uuid, String ip) {
        if (!isConnected())
            return;
        String query = "UPDATE player_stats SET ip = ?, last_updated = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public String getLastIP(UUID uuid) {
        if (!isConnected())
            return null;
        String query = "SELECT ip FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
        }
        return null;
    }

    public List<String> getAlts(UUID uuid, String ip) {
        if (!isConnected() || ip == null)
            return new ArrayList<>();
        List<String> alts = new ArrayList<>();
        String query = "SELECT DISTINCT pn.cached_name FROM player_stats ps JOIN player_names pn ON ps.uuid = pn.uuid WHERE ps.ip = ? AND ps.uuid != ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alts.add(rs.getString("cached_name"));
                }
            }
        } catch (SQLException e) {
        }
        return alts;
    }

    public boolean isBanned(UUID uuid) {
        return getBanInfo(uuid) != null;
    }

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

    public void addAllowedOperator(String playerName) {
        if (!isConnected())
            return;
        String query = "REPLACE INTO allowed_operators (player_name) VALUES (?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
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
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
        }
        return sales;
    }

    public double getAndClearAuctionPendingPayments(UUID uuid) {
        List<com.prismcore.survival.auction.AuctionManager.OfflineSale> sales = getAndClearDetailedPendingSales(uuid);
        return sales.stream().mapToDouble(s -> s.price).sum();
    }

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
        }
        return null;
    }

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
        }
    }

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
        }
    }

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
                    }
                }
            }
        } catch (SQLException e) {
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
        }
    }

    /**
     * ANTI-DUPE: Fetch a single order by ID from database for validation
     * 
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
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
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
        }
    }

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
                    }
                }
            }
        } catch (SQLException e) {
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
        }
    }

    public void saveAuctionItemAsync(com.prismcore.survival.auction.AuctionItem item) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> saveAuctionItem(item));
    }

    public void deleteAuctionItem(UUID itemId) {
        if (!isConnected())
            return;
        String query = "DELETE FROM active_auction_listings WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, itemId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void deleteAuctionItemAsync(UUID itemId) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> deleteAuctionItem(itemId));
    }

    public void savePlayerNameAsync(UUID uuid, String name) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> savePlayerName(uuid, name));
    }

    public void updateStatusAsync(UUID uuid, String status) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String query = "UPDATE player_stats SET status = ? WHERE uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, status);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
            }
        });
    }

    public void saveLastLocationAsync(UUID uuid, org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null)
            return;
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
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
            }
            return alts;
        }).thenAccept(callback);
    }

    public void wipeOrders(UUID playerUuid) {
        if (!isConnected())
            return;
        String query = "DELETE FROM prism_orders WHERE owner = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
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

    public void setServerConfigDouble(String key, double value) {
        setServerConfig(key, String.valueOf(value));
    }

}
