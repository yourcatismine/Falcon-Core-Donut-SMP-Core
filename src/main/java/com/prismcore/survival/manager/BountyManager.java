package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private final PrismSurvival plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, BountyEntry> activeBounties = new HashMap<>();

    public BountyManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "economy/bounties.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create bounties.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        activeBounties.clear();
        if (config.contains("bounties")) {
            for (String key : config.getConfigurationSection("bounties").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double amount = config.getDouble("bounties." + key + ".amount");
                    long timestamp = config.getLong("bounties." + key + ".timestamp");
                    activeBounties.put(uuid, new BountyEntry(amount, timestamp));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in bounties.yml: " + key);
                }
            }
        }
    }

    public void save() {
        config.set("bounties", null); // Clear existing
        for (Map.Entry<UUID, BountyEntry> entry : activeBounties.entrySet()) {
            config.set("bounties." + entry.getKey().toString() + ".amount", entry.getValue().getAmount());
            config.set("bounties." + entry.getKey().toString() + ".timestamp", entry.getValue().getTimestamp());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save bounties.yml: " + e.getMessage());
        }
    }

    public void addBounty(UUID target, double amount) {
        BountyEntry current = activeBounties.getOrDefault(target, new BountyEntry(0.0, System.currentTimeMillis()));
        activeBounties.put(target, new BountyEntry(current.getAmount() + amount, System.currentTimeMillis()));
        save();
    }

    public void removeBounty(UUID target) {
        activeBounties.remove(target);
        save();
    }

    public double getBounty(UUID target) {
        BountyEntry entry = activeBounties.get(target);
        return entry != null ? entry.getAmount() : 0.0;
    }

    public Map<UUID, BountyEntry> getActiveBounties() {
        return new HashMap<>(activeBounties);
    }

    public boolean hasBounty(UUID target) {
        return activeBounties.containsKey(target);
    }

    public static class BountyEntry {
        private final double amount;
        private final long timestamp;

        public BountyEntry(double amount, long timestamp) {
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public double getAmount() {
            return amount;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
