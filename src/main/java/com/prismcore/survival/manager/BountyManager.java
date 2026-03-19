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
    private final Map<UUID, BountyEntry> activeBounties = new HashMap<>();

    public BountyManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "economy/bounties.yml");
        load();
    }

    public void load() {
        activeBounties.clear();

        Map<UUID, Double> dbBounties = plugin.getDatabaseManager().loadAllBounties();
        for (Map.Entry<UUID, Double> entry : dbBounties.entrySet()) {
            activeBounties.put(entry.getKey(), new BountyEntry(entry.getValue(), System.currentTimeMillis()));
        }

        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.contains("bounties")) {
                plugin.getLogger().info("Migrating bounties from YML to Database...");
                boolean migrated = false;
                for (String key : config.getConfigurationSection("bounties").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        double amount = config.getDouble("bounties." + key + ".amount");

                        double finalAmount = activeBounties.getOrDefault(uuid, new BountyEntry(0.0, 0L)).getAmount()
                                + amount;
                        activeBounties.put(uuid, new BountyEntry(finalAmount, System.currentTimeMillis()));
                        plugin.getDatabaseManager().saveBounty(uuid, finalAmount);
                        migrated = true;
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in bounties.yml: " + key);
                    }
                }
                if (migrated) {
                    plugin.getLogger().info("Bounty migration complete. Deleting legacy bounties.yml.");
                    file.delete();
                }
            } else {
                file.delete();
            }
        }
    }

    public void save() {
    }

    public void addBounty(UUID target, double amount) {
        BountyEntry current = activeBounties.getOrDefault(target, new BountyEntry(0.0, System.currentTimeMillis()));
        double newAmount = current.getAmount() + amount;
        activeBounties.put(target, new BountyEntry(newAmount, System.currentTimeMillis()));

        final double finalAmount = newAmount;
        plugin.getSchedulerAdapter().runTaskAsync(() -> plugin.getDatabaseManager().saveBounty(target, finalAmount));
    }

    public void removeBounty(UUID target) {
        activeBounties.remove(target);

        plugin.getSchedulerAdapter().runTaskAsync(() -> plugin.getDatabaseManager().deleteBounty(target));
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
