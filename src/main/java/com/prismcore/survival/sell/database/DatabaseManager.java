/*
 * Decompiled with CFR 0.152.
 */
package com.prismcore.survival.sell.database;

import com.prismcore.survival.sell.PrismSell;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private final PrismSell plugin;
    private Connection connection;

    public DatabaseManager(PrismSell plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        File dataFolder = new File(this.plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "playerdata.db");
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            this.plugin.getLogger().info("Connected to SQLite database successfully!");
            this.createTables();
        } catch (ClassNotFoundException e) {
            this.plugin.getLogger().severe("SQLite JDBC driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("CREATE TABLE IF NOT EXISTS player_stats (");
        queryBuilder.append("uuid TEXT PRIMARY KEY, ");
        queryBuilder.append("money REAL DEFAULT 0, ");
        queryBuilder.append("shards BIGINT DEFAULT 0, ");
        queryBuilder.append("break_blocks BIGINT DEFAULT 0, ");
        queryBuilder.append("placed_blocks BIGINT DEFAULT 0, ");
        queryBuilder.append("mob_kills BIGINT DEFAULT 0, ");
        queryBuilder.append("sell_made REAL DEFAULT 0, ");
        queryBuilder.append("shop_spent REAL DEFAULT 0, ");
        queryBuilder.append("playtime BIGINT DEFAULT 0, ");
        queryBuilder.append("deaths BIGINT DEFAULT 0, ");
        queryBuilder.append("kills BIGINT DEFAULT 0, ");
        queryBuilder.append("keys BIGINT DEFAULT 0");
        queryBuilder.append(")");

        try (java.sql.PreparedStatement stmt = this.connection.prepareStatement(queryBuilder.toString())) {
            stmt.executeUpdate();
            // this.plugin.getLogger().info("Table 'player_stats' verified/created.");
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to create table 'player_stats'!");
            e.printStackTrace();
        }

        // Create player_category_data table
        StringBuilder categoryQuery = new StringBuilder();
        categoryQuery.append("CREATE TABLE IF NOT EXISTS player_category_data (");
        categoryQuery.append("uuid TEXT PRIMARY KEY");

        for (com.prismcore.survival.sell.category.Category category : com.prismcore.survival.sell.category.Category
                .values()) {
            categoryQuery.append(", ").append(category.name()).append("_multiplier REAL DEFAULT 1.0");
            categoryQuery.append(", ").append(category.name()).append("_progress REAL DEFAULT 0");
        }

        categoryQuery.append(")");

        try (java.sql.PreparedStatement stmt = this.connection.prepareStatement(categoryQuery.toString())) {
            stmt.executeUpdate();
            // this.plugin.getLogger().info("Table 'player_category_data'
            // verified/created.");
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to create table 'player_category_data'!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connect();
        }
        return this.connection;
    }

    public void disconnect() {
        if (this.connection != null) {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();
                    this.plugin.getLogger().info("Database connection closed.");
                }
            } catch (SQLException e) {
                this.plugin.getLogger().severe("Error closing database connection!");
                e.printStackTrace();
            }
        }
    }
}
