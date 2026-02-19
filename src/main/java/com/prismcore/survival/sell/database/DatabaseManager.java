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
        String createPlayerData = "CREATE TABLE IF NOT EXISTS player_data (uuid TEXT NOT NULL,category TEXT NOT NULL,progress REAL NOT NULL,multiplier REAL NOT NULL,money REAL DEFAULT 0,shards BIGINT DEFAULT 0,break_blocks BIGINT DEFAULT 0,placed_blocks BIGINT DEFAULT 0,mob_kills BIGINT DEFAULT 0,sell_made REAL DEFAULT 0,shop_spent REAL DEFAULT 0,playtime BIGINT DEFAULT 0,deaths BIGINT DEFAULT 0,kills BIGINT DEFAULT 0,keys BIGINT DEFAULT 0,PRIMARY KEY (uuid, category))";
        try (PreparedStatement stmt = this.connection.prepareStatement(createPlayerData)) {
            stmt.executeUpdate();
            this.plugin.getLogger().info("Database tables created successfully!");
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to create database tables!");
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
