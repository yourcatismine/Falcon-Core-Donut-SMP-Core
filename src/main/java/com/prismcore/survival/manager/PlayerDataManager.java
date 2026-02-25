package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final PrismSurvival plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
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
        PlayerData data = new PlayerData(plugin, uuid);

        // Load Stats from Database (Money and Shards)
        double[] stats = plugin.getDatabaseManager().loadPlayerStats(uuid);
        boolean migratedFromYml = false;

        if (stats != null) {
            data.setMoney(stats[0], "Database Load");
            data.setShards(stats[1], "Database Load");
        }

        // Load Shards Data (Migration/Fallback)
        File shardsFile = new File(dataFolderShards, uuid.toString() + "-shards.db");
        if (shardsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(shardsFile);
            if (stats == null) {
                data.setShards(config.getDouble("shards", 0.0), "YML Fallback");
                migratedFromYml = true;
            }
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
            if (cratesConfig.contains("settings.pay_alerts")) {
                data.setPayAlerts(cratesConfig.getBoolean("settings.pay_alerts"));
            }
            if (cratesConfig.contains("settings.quick_auction_buy")) {
                data.setQuickAuctionBuy(cratesConfig.getBoolean("settings.quick_auction_buy"));
            }
            if (cratesConfig.contains("settings.disable_mob_spawns")) {
                data.setDisableMobSpawns(cratesConfig.getBoolean("settings.disable_mob_spawns"));
            }
            if (cratesConfig.contains("settings.sound_notifications")) {
                data.setSoundNotifications(cratesConfig.getBoolean("settings.sound_notifications"));
            }
            if (cratesConfig.contains("settings.tpa_confirm_menus")) {
                data.setTpaConfirmMenus(cratesConfig.getBoolean("settings.tpa_confirm_menus"));
            }
            if (cratesConfig.contains("settings.duel_requests")) {
                data.setDuelRequests(cratesConfig.getBoolean("settings.duel_requests"));
            }
            if (cratesConfig.contains("settings.tpa_requests")) {
                data.setTpaRequests(cratesConfig.getBoolean("settings.tpa_requests"));
            }
            if (cratesConfig.contains("settings.tpa_here_requests")) {
                data.setTpaHereRequests(cratesConfig.getBoolean("settings.tpa_here_requests"));
            }
            if (cratesConfig.contains("settings.payments")) {
                data.setPayments(cratesConfig.getBoolean("settings.payments"));
            }
            if (cratesConfig.contains("settings.shards_notifier")) {
                data.setShardsNotifier(cratesConfig.getBoolean("settings.shards_notifier"));
            }
            if (cratesConfig.contains("settings.show_scoreboard")) {
                data.setShowScoreboard(cratesConfig.getBoolean("settings.show_scoreboard"));
            }
            if (cratesConfig.contains("settings.tp_auto")) {
                data.setTpAuto(cratesConfig.getBoolean("settings.tp_auto"));
            }
            if (cratesConfig.contains("settings.auction_sort")) {
                data.setAuctionSortOrder(cratesConfig.getString("settings.auction_sort"));
            }
            if (cratesConfig.contains("settings.auction_filter")) {
                data.setAuctionFilter(cratesConfig.getString("settings.auction_filter"));
            }
            if (cratesConfig.contains("settings.auction_category")) {
                data.setAuctionCategory(cratesConfig.getString("settings.auction_category"));
            }
            if (cratesConfig.contains("settings.vanished")) {
                data.setVanished(cratesConfig.getBoolean("settings.vanished"));
            }
            // Load Mute Data
            if (cratesConfig.contains("mute.muted")) {
                data.setMuted(cratesConfig.getBoolean("mute.muted"));
            }
            if (cratesConfig.contains("mute.reason")) {
                data.setMuteReason(cratesConfig.getString("mute.reason"));
            }
            if (cratesConfig.contains("mute.expiry")) {
                data.setMuteExpiry(cratesConfig.getLong("mute.expiry"));
            }
            if (cratesConfig.contains("mute.id")) {
                data.setMuteId(cratesConfig.getString("mute.id"));
            }
            if (cratesConfig.contains("mute.by")) {
                data.setMutedBy(cratesConfig.getString("mute.by"));
            }
            if (cratesConfig.contains("mute.date")) {
                data.setMuteDate(cratesConfig.getLong("mute.date"));
            }
            if (cratesConfig.contains("combat_logged")) {
                data.setCombatLogged(cratesConfig.getBoolean("combat_logged"));
            }
            if (cratesConfig.contains("pending_kick_team")) {
                data.setPendingKickTeamName(cratesConfig.getString("pending_kick_team"));
            }
        }

        // Load Team Data from SQL (Local Database)
        try (Connection conn = plugin.getPrismSell().getDatabaseManager().getConnection()) {
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT ps.team, ps.name_hidden, tm.role FROM player_stats ps " +
                                "LEFT JOIN team_members tm ON ps.uuid = tm.uuid AND ps.team = tm.team_id " +
                                "WHERE ps.uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            data.setTeamId(rs.getString("team"));
                            data.setTeamRole(rs.getString("role"));
                            data.setNameHidden(rs.getBoolean("name_hidden"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load team data from SQL for " + uuid, e);
        }

        // Load Money Data (Migration/Fallback)
        File moneyFile = new File(dataFolderMoney, uuid.toString() + "-money.db");
        if (moneyFile.exists()) {
            FileConfiguration moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
            if (stats == null) {
                data.setMoney(moneyConfig.getDouble("money", 0.0), "YML Fallback");
                migratedFromYml = true;
            }
            // Load cached name if exists
            if (moneyConfig.contains("cached_name")) {
                data.setName(moneyConfig.getString("cached_name"));
            }
        }

        // Complete migration if necessary
        if (migratedFromYml) {
            final PlayerData finalData = data;
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                plugin.getDatabaseManager().savePlayerStats(uuid, finalData.getMoney(), finalData.getShards());
            });
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
        if (data != null) {
            savePlayer(uuid, data);
        }
    }

    public void savePlayer(UUID uuid, PlayerData data) {

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

        // Save Balances to Database
        plugin.getDatabaseManager().savePlayerStats(uuid, data.getMoney(), data.getShards());

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
        cratesConfig.set("settings.pay_alerts", data.isPayAlerts());
        cratesConfig.set("settings.quick_auction_buy", data.isQuickAuctionBuy());
        cratesConfig.set("settings.disable_mob_spawns", data.isDisableMobSpawns());
        cratesConfig.set("settings.sound_notifications", data.isSoundNotifications());
        cratesConfig.set("settings.tpa_confirm_menus", data.isTpaConfirmMenus());
        cratesConfig.set("settings.duel_requests", data.isDuelRequests());
        cratesConfig.set("settings.tpa_requests", data.isTpaRequests());
        cratesConfig.set("settings.tpa_here_requests", data.isTpaHereRequests());
        cratesConfig.set("settings.payments", data.isPayments());
        cratesConfig.set("settings.shards_notifier", data.isShardsNotifier());
        cratesConfig.set("settings.show_scoreboard", data.isShowScoreboard());
        cratesConfig.set("settings.tp_auto", data.isTpAuto());
        cratesConfig.set("settings.auction_sort", data.getAuctionSortOrder());
        cratesConfig.set("settings.auction_filter", data.getAuctionFilter());
        cratesConfig.set("settings.auction_category", data.getAuctionCategory());
        cratesConfig.set("settings.vanished", data.isVanished());

        // Save Mute Data
        cratesConfig.set("mute.muted", data.isMuted());
        cratesConfig.set("mute.reason", data.getMuteReason());
        cratesConfig.set("mute.expiry", data.getMuteExpiry());
        cratesConfig.set("mute.id", data.getMuteId());
        cratesConfig.set("mute.by", data.getMutedBy());
        cratesConfig.set("mute.date", data.getMuteDate());
        cratesConfig.set("combat_logged", data.isCombatLogged());
        cratesConfig.set("pending_kick_team", data.getPendingKickTeamName());

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

        // Save name_hidden to SQL
        plugin.getPrismSell().getDatabaseManager().updateNameHidden(uuid, data.isNameHidden());
    }

    /**
     * Saves only the money file for a player asynchronously.
     * Use this after economy transactions to persist balance immediately
     * without waiting for the full savePlayer() on logout.
     */
    public void saveMoneyAsync(UUID uuid, PlayerData data) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            // Save to Database
            plugin.getDatabaseManager().savePlayerStats(uuid, data.getMoney(), data.getShards());

            File moneyFile = new File(dataFolderMoney, uuid.toString() + "-money.db");
            FileConfiguration moneyConfig = new YamlConfiguration();
            moneyConfig.set("money", data.getMoney());
            if (data.getName() != null) {
                moneyConfig.set("cached_name", data.getName());
            }
            try {
                moneyConfig.save(moneyFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to async-save money for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void unload(UUID uuid) {
        PlayerData data = playerDataMap.remove(uuid);
        if (data != null) {
            plugin.getSchedulerAdapter().runTaskAsync(() -> savePlayer(uuid, data));
        }
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

                            // If missing, we MUST fetch it
                            if (name == null) {
                                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                                name = op.getName();
                                // We don't save back to file here to avoid blocking during a full scan.
                                // Names are naturally cached during player login/quit.
                            }

                            if (name != null) {
                                entries.add(new LeaderboardEntry(org.bukkit.ChatColor.stripColor(name), uuid, amount));
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
                            }

                            if (name != null) {
                                entries.add(new LeaderboardEntry(org.bukkit.ChatColor.stripColor(name), uuid, amount));
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
