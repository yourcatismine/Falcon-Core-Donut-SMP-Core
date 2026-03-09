package com.prismcore.survival.sell.database;

import com.prismcore.survival.sell.PrismSell;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;

public class DatabaseManager {
    private final PrismSell plugin;
    private HikariDataSource dataSource;

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public DatabaseManager(PrismSell plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        FileConfiguration dataConfig = plugin.getPlugin().getConfig();

        String host = dataConfig.getString("database.host", "localhost");
        int port = dataConfig.getInt("database.port", 3306);
        String database = dataConfig.getString("database.database", "falcon_europe");
        String username = dataConfig.getString("database.username", "root");
        String password = dataConfig.getString("database.password", "");

        try {
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
            this.createTables();
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to connect to MySQL database with HikariCP!");
        }
    }

    private void createTables() {
        try (Connection connection = getConnection();
                java.sql.Statement stmt = connection.createStatement()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("CREATE TABLE IF NOT EXISTS player_stats (");
            queryBuilder.append("`uuid` VARCHAR(36) PRIMARY KEY, ");
            queryBuilder.append("`money` DOUBLE DEFAULT 0, ");
            queryBuilder.append("`shards` BIGINT DEFAULT 0, ");
            queryBuilder.append("`break_blocks` BIGINT DEFAULT 0, ");
            queryBuilder.append("`placed_blocks` BIGINT DEFAULT 0, ");
            queryBuilder.append("`mob_kills` BIGINT DEFAULT 0, ");
            queryBuilder.append("`sell_made` DOUBLE DEFAULT 0, ");
            queryBuilder.append("`shop_spent` DOUBLE DEFAULT 0, ");
            queryBuilder.append("`playtime` BIGINT DEFAULT 0, ");
            queryBuilder.append("`deaths` BIGINT DEFAULT 0, ");
            queryBuilder.append("`kills` BIGINT DEFAULT 0, ");
            queryBuilder.append("`keys` BIGINT DEFAULT 0, ");
            queryBuilder.append("`bounty` DOUBLE DEFAULT 0, ");
            queryBuilder.append("`tool_expiry` BIGINT DEFAULT 0, ");
            queryBuilder.append("`team` VARCHAR(36) DEFAULT NULL, ");
            queryBuilder.append("`name_hidden` BOOLEAN DEFAULT FALSE");
            queryBuilder.append(")");

            stmt.execute(queryBuilder.toString());

            String teamsTable = "CREATE TABLE IF NOT EXISTS teams (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(32) NOT NULL, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "created_at BIGINT NOT NULL, " +
                    "pvp_enabled BOOLEAN DEFAULT FALSE, " +
                    "home_world VARCHAR(64), " +
                    "home_x DOUBLE, " +
                    "home_y DOUBLE, " +
                    "home_z DOUBLE, " +
                    "home_yaw FLOAT, " +
                    "home_pitch FLOAT, " +
                    "home_server VARCHAR(32)" +
                    ")";
            stmt.execute(teamsTable);

            String teamMembersTable = "CREATE TABLE IF NOT EXISTS team_members (" +
                    "team_id VARCHAR(36) NOT NULL, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "role VARCHAR(16) NOT NULL, " +
                    "joined_at BIGINT NOT NULL, " +
                    "PRIMARY KEY (team_id, uuid), " +
                    "INDEX (uuid)" +
                    ")";
            stmt.execute(teamMembersTable);

            String enderchestTable = "CREATE TABLE IF NOT EXISTS enderchest (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "contents TEXT" +
                    ")";
            stmt.execute(enderchestTable);

            String sellHistoryTable = "CREATE TABLE IF NOT EXISTS sell_history (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "item VARCHAR(64) NOT NULL, " +
                    "amount BIGINT DEFAULT 0, " +
                    "total DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY (uuid, item), " +
                    "INDEX (uuid)" +
                    ")";
            stmt.execute(sellHistoryTable);

            String playerHomesTable = "CREATE TABLE IF NOT EXISTS player_homes (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "home_index INT NOT NULL, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "home_name VARCHAR(64), " +
                    "PRIMARY KEY (uuid, home_index)" +
                    ")";
            stmt.execute(playerHomesTable);

            StringBuilder categoryDataQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS player_category_data (");
            categoryDataQuery.append("uuid VARCHAR(36) PRIMARY KEY");
            for (com.prismcore.survival.sell.category.Category category : com.prismcore.survival.sell.category.Category
                    .values()) {
                categoryDataQuery.append(", ").append(category.name()).append("_multiplier DOUBLE DEFAULT 1.0");
                categoryDataQuery.append(", ").append(category.name()).append("_progress DOUBLE DEFAULT 0.0");
            }
            categoryDataQuery.append(")");
            stmt.execute(categoryDataQuery.toString());

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
                    "name_hidden BOOLEAN DEFAULT FALSE"
            };

            for (String columnDef : statsColumns) {
                try {
                    stmt.execute("ALTER TABLE player_stats ADD COLUMN " + columnDef);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return this.dataSource.getConnection();
    }

    public void disconnect() {
        if (this.dataSource != null) {
            this.dataSource.close();
            this.plugin.getLogger().info("Database connection closed.");
        }
    }

    public void updateNameHidden(java.util.UUID uuid, boolean hidden) {
        if (!isConnected())
            return;
        String query = "UPDATE player_stats SET name_hidden = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1, hidden);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public boolean isNameHidden(java.util.UUID uuid) {
        if (!isConnected())
            return false;
        String query = "SELECT name_hidden FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("name_hidden");
                }
            }
        } catch (SQLException e) {
        }
        return false;
    }

    public void updateDisguiseStatus(java.util.UUID uuid, boolean disguised) {
        if (!isConnected())
            return;
        String query = "UPDATE player_stats SET disguised = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1, disguised);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void updateDisguiseInfo(java.util.UUID uuid, String disguiseName, String skinTexture, String skinSignature) {
        if (!isConnected())
            return;
        String query = "UPDATE player_stats SET disguise_name = ?, disguise_skin_texture = ?, disguise_skin_signature = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, disguiseName);
            ps.setString(2, skinTexture);
            ps.setString(3, skinSignature);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public boolean isDisguised(java.util.UUID uuid) {
        if (!isConnected())
            return false;
        String query = "SELECT disguised FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("disguised");
                }
            }
        } catch (SQLException e) {
        }
        return false;
    }

    public String[] getDisguiseInfo(java.util.UUID uuid) {
        if (!isConnected())
            return new String[]{null, null, null};
        String query = "SELECT disguise_name, disguise_skin_texture, disguise_skin_signature FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        rs.getString("disguise_name"),
                        rs.getString("disguise_skin_texture"), 
                        rs.getString("disguise_skin_signature")
                    };
                }
            }
        } catch (SQLException e) {
        }
        return new String[]{null, null, null};
    }

    public void saveSellHistoryAsync(java.util.UUID uuid, java.util.Map<String, double[]> history) {
        if (!isConnected() || history == null || history.isEmpty())
            return;
        this.plugin.getPlugin().getSchedulerAdapter().runTaskAsync(() -> {
            String query = "INSERT INTO sell_history (uuid, item, amount, total) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount), total = total + VALUES(total)";
            try (Connection conn = getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
                for (java.util.Map.Entry<String, double[]> entry : history.entrySet()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, entry.getKey());
                    ps.setLong(3, (long) entry.getValue()[0]);
                    ps.setDouble(4, entry.getValue()[1]);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                this.plugin.getLogger().warning("Failed to save sell history for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void getSellHistoryAsync(java.util.UUID uuid,
            java.util.function.Consumer<java.util.Map<String, double[]>> callback) {
        if (!isConnected()) {
            this.plugin.getPlugin().getSchedulerAdapter().runTask(() -> callback.accept(new java.util.HashMap<>()));
            return;
        }
        this.plugin.getPlugin().getSchedulerAdapter().runTaskAsync(() -> {
            java.util.Map<String, double[]> history = new java.util.HashMap<>();
            String query = "SELECT item, amount, total FROM sell_history WHERE uuid = ?";
            try (Connection conn = getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String item = rs.getString("item");
                        long amount = rs.getLong("amount");
                        double total = rs.getDouble("total");
                        history.put(item, new double[] { (double) amount, total });
                    }
                }
            } catch (SQLException e) {
                this.plugin.getLogger().warning("Failed to load sell history for " + uuid + ": " + e.getMessage());
            }
            this.plugin.getPlugin().getSchedulerAdapter().runTask(() -> callback.accept(history));
        });
    }

    public void wipeAllPlayerData(java.util.UUID uuid) {
        if (!isConnected())
            return;
        String uuidStr = uuid.toString();
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_stats WHERE uuid = ?")) {
                ps.setString(1, uuidStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sell_history WHERE uuid = ?")) {
                ps.setString(1, uuidStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_homes WHERE uuid = ?")) {
                ps.setString(1, uuidStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_category_data WHERE uuid = ?")) {
                ps.setString(1, uuidStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM enderchest WHERE uuid = ?")) {
                ps.setString(1, uuidStr);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().warning("Failed to perform deep wipe for " + uuid + ": " + e.getMessage());
        }
    }
}
