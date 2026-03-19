package com.h2ph.managers;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.DatabaseManager;

public class DamageManager {

    private final Falcon plugin;
    private final DatabaseManager databaseManager;

    private double crystalDamage = 6.0;
    private double anchorDamage = 6.0;

    public DamageManager(Falcon plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        loadConfig();
    }

    private void loadConfig() {
        crystalDamage = databaseManager.getServerConfigDouble("damage.crystal", 6.0);
        anchorDamage = databaseManager.getServerConfigDouble("damage.anchor", 6.0);
    }

    public void setCrystalDamage(double damage) {
        this.crystalDamage = damage;
        databaseManager.setServerConfigDouble("damage.crystal", damage);
    }

    public void setAnchorDamage(double damage) {
        this.anchorDamage = damage;
        databaseManager.setServerConfigDouble("damage.anchor", damage);
    }

    public double getCrystalDamage() {
        return crystalDamage;
    }

    public double getAnchorDamage() {
        return anchorDamage;
    }

    public void reload() {
        loadConfig();
    }
}

