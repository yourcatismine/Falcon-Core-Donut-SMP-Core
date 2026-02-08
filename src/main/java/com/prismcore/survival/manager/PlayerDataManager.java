package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final PrismSurvival plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final File dataFolderShards;
    private final File dataFolderMoney;
    private final File dataFolderCrates;

    public PlayerDataManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.dataFolderShards = new File(plugin.getDataFolder(), "economy/shards/players");
        this.dataFolderMoney = new File(plugin.getDataFolder(), "economy/money/players");
        this.dataFolderCrates = new File(plugin.getDataFolder(), "crates/data");

        if (!dataFolderShards.exists()) {
            dataFolderShards.mkdirs();
        }
        if (!dataFolderMoney.exists()) {
            dataFolderMoney.mkdirs();
        }
        if (!dataFolderCrates.exists()) {
            dataFolderCrates.mkdirs();
        }
    }

    public PlayerData get(UUID uuid) {
        if (playerDataMap.containsKey(uuid)) {
            return playerDataMap.get(uuid);
        }

        // Load from file
        PlayerData data = loadPlayer(uuid);
        playerDataMap.put(uuid, data);
        return data;
    }

    public PlayerData loadPlayer(UUID uuid) {
        PlayerData data = new PlayerData(uuid);

        // Load Shards Data
        File shardsFile = new File(dataFolderShards, uuid.toString() + "-shards.db");
        if (shardsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(shardsFile);
            data.setShards(config.getDouble("shards", 0.0));
            data.setShopSpent(config.getDouble("shop_spent", 0.0));

            // Legacy key loading (migration support, optional)
            // If keys exist here but NOT in new file, we could load them.
            // But let's prioritize the new file, and if new file doesn't exist, check old.
            // Or just load old, then load new (new overwrites).
            if (config.contains("keys")) {
                for (String key : config.getConfigurationSection("keys").getKeys(false)) {
                    int count = config.getInt("keys." + key, 0);
                    data.setKeyCount(key, count);
                }
            }
        }

        // Load Keys Data (New Location)
        File cratesFile = new File(dataFolderCrates, uuid.toString() + ".db");
        if (cratesFile.exists()) {
            FileConfiguration cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);
            if (cratesConfig.contains("keys")) {
                // Clear legacy keys if we found new data? No, let's just overwrite/merge.
                // Usually better to start fresh from new source if it exists.
                // But simplified: Just load on top.
                for (String key : cratesConfig.getConfigurationSection("keys").getKeys(false)) {
                    int count = cratesConfig.getInt("keys." + key, 0);
                    data.setKeyCount(key, count);
                }
            }
            // Load tracking
            if (cratesConfig.contains("last_seen_update")) {
                data.setLastSeenUpdate(cratesConfig.getLong("last_seen_update"));
            }
            // Load shard booster expiry
            if (cratesConfig.contains("shard_booster_expiry")) {
                data.setShardBoosterExpiry(cratesConfig.getLong("shard_booster_expiry"));
            }
            // Load Settings
            if (cratesConfig.contains("settings.hide_chat")) {
                data.setHideChat(cratesConfig.getBoolean("settings.hide_chat"));
            }
            if (cratesConfig.contains("settings.private_messages")) {
                data.setPrivateMessages(cratesConfig.getBoolean("settings.private_messages"));
            }
        }

        // Load Money Data
        File moneyFile = new File(dataFolderMoney, uuid.toString() + "-money.db");
        if (moneyFile.exists()) {
            FileConfiguration moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
            data.setMoney(moneyConfig.getDouble("money", 0.0));
            // Load cached name if exists
            if (moneyConfig.contains("cached_name")) {
                data.setName(moneyConfig.getString("cached_name"));
            }
        }

        // If name is still null (not in money file), try shards file or fetch and cache
        if (data.getName() == null && shardsFile.exists()) {
            FileConfiguration shardsConfig = YamlConfiguration.loadConfiguration(shardsFile);
            if (shardsConfig.contains("cached_name")) {
                data.setName(shardsConfig.getString("cached_name"));
            }
        }

        // If still null, fetch it safely (async usually) or just leave it null until
        // save?
        // Ideally we fetch it now if we can, but if this is called async it's fine.
        if (data.getName() == null) {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null) {
                data.setName(op.getName());
            }
        }

        return data;
    }

    public void savePlayer(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) {
            return;
        }

        // Save Shards Data
        File shardsFile = new File(dataFolderShards, uuid.toString() + "-shards.db");
        FileConfiguration shardsConfig = new YamlConfiguration();

        shardsConfig.set("shards", data.getShards());
        shardsConfig.set("shop_spent", data.getShopSpent());

        // We do NOT save keys here anymore to migrate fully away over time,
        // OR we keep saving them for backup?
        // User wants SPECIFIC file. Let's strictly save to new file.
        // To clean up old file, we stop writing keys to it.

        try {
            shardsConfig.save(shardsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shards data for " + uuid + ": " + e.getMessage());
        }

        // Save Keys Data (New Location)
        File cratesFile = new File(dataFolderCrates, uuid.toString() + ".db");
        FileConfiguration cratesConfig = new YamlConfiguration();

        Map<String, Integer> keys = data.getKeys();
        for (Map.Entry<String, Integer> entry : keys.entrySet()) {
            cratesConfig.set("keys." + entry.getKey(), entry.getValue());
        }

        cratesConfig.set("last_seen_update", data.getLastSeenUpdate());
        cratesConfig.set("last_seen_update", data.getLastSeenUpdate());
        cratesConfig.set("shard_booster_expiry", data.getShardBoosterExpiry());

        // Save Settings
        cratesConfig.set("settings.hide_chat", data.isHideChat());
        cratesConfig.set("settings.private_messages", data.isPrivateMessages());

        try {
            cratesConfig.save(cratesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crates data for " + uuid + ": " + e.getMessage());
        }

        // Save Money Data
        File moneyFile = new File(dataFolderMoney, uuid.toString() + "-money.db");
        FileConfiguration moneyConfig = new YamlConfiguration();

        moneyConfig.set("money", data.getMoney());
        // Save cached name
        if (data.getName() != null) {
            moneyConfig.set("cached_name", data.getName());
        } else {
            // Try to fetch if missing
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null) {
                moneyConfig.set("cached_name", op.getName());
                data.setName(op.getName());
            }
        }

        try {
            moneyConfig.save(moneyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save money data for " + uuid + ": " + e.getMessage());
        }
    }

    public void unload(UUID uuid) {
        savePlayer(uuid);
        playerDataMap.remove(uuid);
    }

    // Leaderboard Support

    private long lastShardsUpdate = 0;
    private List<LeaderboardEntry> cachedShardsTop = null;
    private long lastMoneyUpdate = 0;
    private List<LeaderboardEntry> cachedMoneyTop = null;
    private static final long CACHE_DURATION = 60 * 1000; // 1 minute cache

    public static class LeaderboardEntry {
        public String name;
        public UUID uuid;
        public double value;

        public LeaderboardEntry(String name, UUID uuid, double value) {
            this.name = name;
            this.uuid = uuid;
            this.value = value;
        }
    }

    public List<LeaderboardEntry> getTopShards(int limit) {
        if (cachedShardsTop != null && (System.currentTimeMillis() - lastShardsUpdate < CACHE_DURATION)) {
            return cachedShardsTop.size() > limit ? cachedShardsTop.subList(0, limit) : cachedShardsTop;
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        if (dataFolderShards.exists()) {
            File[] files = dataFolderShards.listFiles((dir, name) -> name.endsWith("-shards.db"));
            if (files != null) {
                for (File file : files) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    double amount = config.getDouble("shards", 0.0);
                    if (amount > 0) {
                        String uuidStr = file.getName().replace("-shards.db", "");
                        try {
                            UUID uuid = UUID.fromString(uuidStr);

                            // Try to get name from file first
                            String name = config.getString("cached_name");

                            // If missing, we MUST fetch it (costly, but one time cost per player)
                            if (name == null) {
                                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                                name = op.getName();

                                // Save it back to file so next time it's fast!
                                if (name != null) {
                                    config.set("cached_name", name);
                                    try {
                                        config.save(file);
                                    } catch (IOException ignored) {
                                    }
                                }
                            }

                            if (name != null) {
                                entries.add(new LeaderboardEntry(name, uuid, amount));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }

        entries.sort((a, b) -> Double.compare(b.value, a.value));
        cachedShardsTop = entries;
        lastShardsUpdate = System.currentTimeMillis();

        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    private void fetchVaultTopMoney() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            return;
        }

        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return;
        }

        net.milkbowl.vault.economy.Economy eco = rsp.getProvider();
        if (eco == null) {
            return;
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        // Iterate all offline players - can be heavy, but necessary for external eco
        // without direct DB access
        for (org.bukkit.OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
            if (p.getName() == null)
                continue;
            try {
                // Essentials and others usually handle this call efficiently enough or caching
                // is needed
                if (eco.hasAccount(p)) {
                    double bal = eco.getBalance(p);
                    if (bal > 0) {
                        entries.add(new LeaderboardEntry(p.getName(), p.getUniqueId(), bal));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        entries.sort((a, b) -> Double.compare(b.value, a.value));
        cachedMoneyTop = entries;
        lastMoneyUpdate = System.currentTimeMillis();
    }

    private boolean isUpdatingMoney = false;

    public List<LeaderboardEntry> getTopMoney(int limit) {
        // Return memory cache immediately if valid
        if (cachedMoneyTop != null && !cachedMoneyTop.isEmpty()) {
            // If cache is getting old, trigger update in background, but still return
            // current cache
            if (System.currentTimeMillis() - lastMoneyUpdate > CACHE_DURATION) {
                triggerVaultUpdateAsync();
            }
            return cachedMoneyTop.size() > limit ? cachedMoneyTop.subList(0, limit) : cachedMoneyTop;
        }

        // Check if we should use Vault
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            triggerVaultUpdateAsync();
            // Fallthrough to look at internal storage as temporary placeholder
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        if (dataFolderMoney.exists()) {
            File[] files = dataFolderMoney.listFiles((dir, name) -> name.endsWith("-money.db"));
            if (files != null) {
                for (File file : files) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    double amount = config.getDouble("money", 0.0);
                    if (amount > 0) {
                        String uuidStr = file.getName().replace("-money.db", "");
                        try {
                            UUID uuid = UUID.fromString(uuidStr);

                            // Try to get name from file first
                            String name = config.getString("cached_name");

                            // If missing, we MUST fetch it
                            if (name == null) {
                                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                                name = op.getName();

                                // Save it back to file
                                if (name != null) {
                                    config.set("cached_name", name);
                                    try {
                                        config.save(file);
                                    } catch (IOException ignored) {
                                    }
                                }
                            }

                            if (name != null) {
                                entries.add(new LeaderboardEntry(name, uuid, amount));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }

        entries.sort((a, b) -> Double.compare(b.value, a.value));

        // If we have nothing from Vault yet, we don't overwrite cachedMoneyTop heavily
        // unless we have to.
        // Actually, let's allow internal storage to be the "initial" cache.
        if (cachedMoneyTop == null || cachedMoneyTop.isEmpty()) {
            // Only set valid cache if we found entries, otherwise keep it null so Vault
            // update tries again?
            // Or set it to empty so we don't query disk every time?
            // Let's safe-guard:
            if (!entries.isEmpty()) {
                cachedMoneyTop = entries;
                lastMoneyUpdate = System.currentTimeMillis();
            }
        }

        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    private void triggerVaultUpdateAsync() {
        if (isUpdatingMoney)
            return;
        isUpdatingMoney = true;

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try {
                fetchVaultTopMoney();
            } finally {
                isUpdatingMoney = false;
            }
        });
    }
}
