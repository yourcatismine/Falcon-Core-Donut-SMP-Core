package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final PrismSurvival plugin;
    private final FileConfiguration config;
    private HikariDataSource dataSource;

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
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setMaximumPoolSize(10);

            this.dataSource = new HikariDataSource(hikariConfig);

            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize ban database with HikariCP", e);
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
                    "shards DOUBLE DEFAULT 0," +
                    "last_updated BIGINT" +
                    ")";
            s.execute(statsTable);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check connection validity and reconnect if needed
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public boolean isRedisEnabled() {
        return false;
    }

    // --- Public API inferred from OffendPlugin ---

    public List<String> getBannedPlayerNames() {
        List<String> names = new ArrayList<>();
        // Unique names from active bans
        // A ban is active if expiry == -1 OR expiry > current time
        String query = "SELECT DISTINCT player_name FROM bans WHERE expiry = -1 OR expiry > ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBan(UUID uuid) {
        String query = "DELETE FROM bans WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBanById(String banId) {
        String query = "DELETE FROM bans WHERE ban_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, banId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
        String query = "REPLACE INTO offenses (uuid, reason_key, count) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
            e.printStackTrace();
        }
    }

    public void logIP(UUID uuid, String ip) {
        String query = "REPLACE INTO ip_logs (uuid, ip, last_seen) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
            e.printStackTrace();
        }
        return alts;
    }

    public boolean isBanned(UUID uuid) {
        return getBanInfo(uuid) != null;
    }

    // --- Mute Methods ---

    public void addMute(UUID uuid, String playerName, String muteId, String reason, long date, long expiry,
            String mutedBy) {
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
            e.printStackTrace();
        }
    }

    public void removeMute(UUID uuid) {
        String query = "DELETE FROM mutes WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MuteInfo getMuteInfo(UUID uuid) {
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
            e.printStackTrace();
        }
        return null;
    }

    public MuteInfo getMuteInfoByName(String name) {
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
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getMutedPlayerNames() {
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
            e.printStackTrace();
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
        String query = "REPLACE INTO allowed_operators (player_name) VALUES (?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeAllowedOperator(String playerName) {
        String query = "DELETE FROM allowed_operators WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isAllowedOperator(String playerName) {
        String query = "SELECT player_name FROM allowed_operators WHERE player_name = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public java.util.List<String> getAllowedOperators() {
        java.util.List<String> names = new java.util.ArrayList<>();
        String query = "SELECT player_name FROM allowed_operators";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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

    public void addAuctionPendingPayment(UUID uuid, double amount) {
        String query = "INSERT INTO auction_pending_payments (uuid, amount, timestamp) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getAndClearAuctionPendingPayments(UUID uuid) {
        double total = 0;
        String selectQuery = "SELECT amount FROM auction_pending_payments WHERE uuid = ?";
        String deleteQuery = "DELETE FROM auction_pending_payments WHERE uuid = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psSelect = conn.prepareStatement(selectQuery)) {
                    psSelect.setString(1, uuid.toString());
                    try (ResultSet rs = psSelect.executeQuery()) {
                        while (rs.next()) {
                            total += rs.getDouble("amount");
                        }
                    }
                }
                if (total > 0) {
                    try (PreparedStatement psDelete = conn.prepareStatement(deleteQuery)) {
                        psDelete.setString(1, uuid.toString());
                        psDelete.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    // --- Inventory Sync Methods ---

    public void saveInventory(UUID uuid, String inventoryBase64, String armorBase64) {
        String query = "REPLACE INTO player_inventories (uuid, inventory_data, armor_data, last_updated) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, inventoryBase64);
            ps.setString(3, armorBase64);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String[] loadInventory(UUID uuid) {
        String query = "SELECT inventory_data, armor_data FROM player_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[] { rs.getString("inventory_data"), rs.getString("armor_data") };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Auction Transaction Methods ---

    public void addAuctionTransaction(UUID playerUuid, com.prismcore.survival.auction.Transaction tx) {
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
            e.printStackTrace();
        }
    }

    public List<com.prismcore.survival.auction.Transaction> getAuctionTransactions(UUID playerUuid) {
        List<com.prismcore.survival.auction.Transaction> list = new ArrayList<>();
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
            e.printStackTrace();
        }
        return list;
    }

    public void deleteAuctionTransaction(UUID playerUuid, long timestamp, double price) {
        String query = "DELETE FROM auction_transactions WHERE player_uuid = ? AND timestamp = ? AND price = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, timestamp);
            ps.setDouble(3, price);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Player Stats Methods ---

    public void savePlayerStats(UUID uuid, double money, double shards) {
        String query = "REPLACE INTO player_stats (uuid, money, shards, last_updated) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, money);
            ps.setDouble(3, shards);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player stats for " + uuid, e);
        }
    }

    public double[] loadPlayerStats(UUID uuid) {
        String query = "SELECT money, shards FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[] { rs.getDouble("money"), rs.getDouble("shards") };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player stats for " + uuid, e);
        }
        return null;
    }

    public void updateOfflineBalance(UUID uuid, double balance, boolean isShards) {
        String column = isShards ? "shards" : "money";
        String query = "UPDATE player_stats SET " + column + " = ?, last_updated = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, balance);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update offline balance for " + uuid, e);
        }
    }
}
