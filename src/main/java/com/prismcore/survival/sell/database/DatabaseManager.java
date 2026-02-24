package com.prismcore.survival.sell.database;

import com.prismcore.survival.sell.PrismSell;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;

public class DatabaseManager {
    private final PrismSell plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(PrismSell plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        // Load MySQL settings from data.yml
        File dataFile = new File(plugin.getDataFolder(), "data/data.yml");
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        String host = dataConfig.getString("mysql.host", "localhost");
        int port = dataConfig.getInt("mysql.port", 3306);
        String database = dataConfig.getString("mysql.database", "falcon_europe");
        String username = dataConfig.getString("mysql.username", "root");
        String password = dataConfig.getString("mysql.password", "");

        try {
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
            this.createTables();
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to connect to MySQL database with HikariCP!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        // player_stats table
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

            // Migration: Add columns if they don't exist
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN `team` VARCHAR(36) DEFAULT NULL");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN `name_hidden` BOOLEAN DEFAULT FALSE");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to create table/insert row!");
            e.printStackTrace();
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
        String query = "UPDATE player_stats SET name_hidden = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1, hidden);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isNameHidden(java.util.UUID uuid) {
        String query = "SELECT name_hidden FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("name_hidden");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
