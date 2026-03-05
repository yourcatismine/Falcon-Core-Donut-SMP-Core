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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final PrismSurvival plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> savingFutures = new ConcurrentHashMap<>();
    private final File dataFolderCrates;

    public PlayerDataManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.dataFolderCrates = new File(plugin.getDataFolder(), "crates/data");

        if (!dataFolderCrates.exists()) {
            dataFolderCrates.mkdirs();
        }
    }

    public PlayerData get(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            // If it's in the cache, it might be marked for unloading.
            // Reset the flag since the player is now active again.
            data.setUnloading(false);
            return data;
        }

        // Check if there's an active save operation for this player.
        // If so, we MUST wait for it to finish before loading to avoid stale data.
        CompletableFuture<Void> saveFuture = savingFutures.get(uuid);
        if (saveFuture != null && !saveFuture.isDone()) {
            try {
                // Wait for the save to complete (with a reasonable timeout if possible, but
                // join() is safer here)
                saveFuture.join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error waiting for save future for " + uuid, e);
            }
        }

        // Use computeIfAbsent to ensure thread-safety and prevent duplicate loads
        return playerDataMap.computeIfAbsent(uuid, k -> {
            // Load from file/database
            return loadPlayer(k);
        });
    }

    public PlayerData loadPlayer(UUID uuid) {
        PlayerData data = new PlayerData(plugin, uuid);

        // Load Stats from Database (Money, Shards, ShopSpent)
        DatabaseManager.PlayerDataStats stats = null;
        if (plugin.getDatabaseManager().isConnected()) {
            stats = plugin.getDatabaseManager().loadPlayerStats(uuid);
        }
        boolean migratedFromYml = false;

        if (stats != null) {
            data.setMoney(stats.money, "Database Load");
            data.setShards(stats.shards, "Database Load");
            data.setShopSpent(stats.shopSpent);
            data.setIp(stats.ip);
        }

        // Load Shards Data (Migration/Fallback)
        File legacyShardsFolder = new File(plugin.getDataFolder(), "economy/shards/players");
        File shardsFile = new File(legacyShardsFolder, uuid.toString() + "-shards.db");
        if (shardsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(shardsFile);
            if (stats == null) {
                data.setShards(config.getDouble("shards", 0.0), "YML Fallback");
                data.setShopSpent(config.getDouble("shop_spent", 0.0));
                migratedFromYml = true;
            }

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
            if (cratesConfig.contains("settings.check_history")) {
                data.setCheckHistory(cratesConfig.getBoolean("settings.check_history"));
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
            if (cratesConfig.contains("settings.fast_crystals")) {
                data.setFastCrystals(cratesConfig.getBoolean("settings.fast_crystals"));
            }
            if (cratesConfig.contains("settings.respawn_rtp")) {
                data.setRespawnRTP(cratesConfig.getBoolean("settings.respawn_rtp"));
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

            // Load ignored players list
            if (cratesConfig.contains("ignored_players")) {
                java.util.List<String> ignoredUuids = cratesConfig.getStringList("ignored_players");
                java.util.Set<java.util.UUID> ignoredSet = new java.util.HashSet<>();
                for (String uuidString : ignoredUuids) {
                    try {
                        ignoredSet.add(java.util.UUID.fromString(uuidString));
                    } catch (IllegalArgumentException e) {
                        // Invalid UUID, skip
                    }
                }
                data.setIgnoredPlayers(ignoredSet);
            }
        }

        // Load Team Data from SQL (Local Database)
        if (plugin.getPrismSell().getDatabaseManager().isConnected()) {
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
                // Silently fail
            }
        }

        // Load Money Data (Migration/Fallback)
        File legacyMoneyFolder = new File(plugin.getDataFolder(), "economy/money/players");
        File moneyFile = new File(legacyMoneyFolder, uuid.toString() + "-money.db");
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
                plugin.getDatabaseManager().savePlayerStats(uuid, finalData);
                plugin.getDatabaseManager().savePlayerName(uuid, finalData.getName());
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

        // Cache/Update name in DB for leaderboards
        if (data.getName() != null) {
            plugin.getDatabaseManager().savePlayerNameAsync(uuid, data.getName());
        }

        return data;
    }

    public void savePlayer(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            savePlayer(uuid, data);
        }
    }

    public void savePlayerAsync(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            plugin.getSchedulerAdapter().runTaskAsync(() -> savePlayer(uuid, data));
        }
    }

    public void savePlayer(UUID uuid, PlayerData data) {

        // Legacy YML saving removed - Database is source of truth
        plugin.getDatabaseManager().savePlayerStats(uuid, data);

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
        cratesConfig.set("settings.check_history", data.isCheckHistory());
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
        cratesConfig.set("settings.fast_crystals", data.isFastCrystals());
        cratesConfig.set("settings.respawn_rtp", data.isRespawnRTP());

        // Save Mute Data
        cratesConfig.set("mute.muted", data.isMuted());
        cratesConfig.set("mute.reason", data.getMuteReason());
        cratesConfig.set("mute.expiry", data.getMuteExpiry());
        cratesConfig.set("mute.id", data.getMuteId());
        cratesConfig.set("mute.by", data.getMutedBy());
        cratesConfig.set("mute.date", data.getMuteDate());
        cratesConfig.set("combat_logged", data.isCombatLogged());
        cratesConfig.set("pending_kick_team", data.getPendingKickTeamName());

        // Save ignored players list
        java.util.List<String> ignoredUuids = new java.util.ArrayList<>();
        for (UUID ignoredUuid : data.getIgnoredPlayers()) {
            ignoredUuids.add(ignoredUuid.toString());
        }
        cratesConfig.set("ignored_players", ignoredUuids);

        try {
            cratesConfig.save(cratesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crates data for " + uuid + ": " + e.getMessage());
        }

        // Save name to SQL for leaderboards
        if (data.getName() != null) {
            plugin.getDatabaseManager().savePlayerNameAsync(uuid, data.getName());
        }

        // Save name_hidden to SQL
        if (plugin.getPrismSell().getDatabaseManager().isConnected()) {
            plugin.getPrismSell().getDatabaseManager().updateNameHidden(uuid, data.isNameHidden());
        }
    }

    /**
     * Saves only the money file for a player asynchronously.
     * Use this after economy transactions to persist balance immediately
     * without waiting for the full savePlayer() on logout.
     */
    public void saveMoneyAsync(UUID uuid, PlayerData data) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            // Save to Database
            plugin.getDatabaseManager().savePlayerStats(uuid, data);
            plugin.getDatabaseManager().savePlayerName(uuid, data.getName());
        });
    }

    public void unload(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            // Mark as unloading so that final cleanup knows it's okay to remove
            data.setUnloading(true);

            // Save first, then remove from map ONLY after save is complete
            // This prevents race conditions where a fast rejoin loads stale data
            CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(() -> {
                savePlayer(uuid, data);
            }, runnable -> plugin.getSchedulerAdapter().runTaskAsync(runnable));

            // Track this save future
            savingFutures.put(uuid, saveFuture);

            saveFuture.whenComplete((v, throwable) -> {
                try {
                    // Remove the future once done
                    savingFutures.remove(uuid, saveFuture);

                    // Remove from map AFTER save is finished, but ONLY if still marked as
                    // unloading.
                    // If the player rejoined, get() would have set unloading to false.
                    if (data.isUnloading()) {
                        playerDataMap.remove(uuid, data);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in unload cleanup for " + uuid, e);
                }
            });
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
        return plugin.getDatabaseManager().getTopShards(limit);
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
            // Fallthrough to look at internal database as temporary placeholder
        }

        List<LeaderboardEntry> entries = plugin.getDatabaseManager().getTopMoney(limit);

        // If we have nothing from Vault yet, we don't overwrite cachedMoneyTop heavily
        // unless we have to.
        // Actually, let's allow internal storage to be the "initial" cache.
        if (cachedMoneyTop == null || cachedMoneyTop.isEmpty()) {
            if (!entries.isEmpty()) {
                cachedMoneyTop = entries;
                lastMoneyUpdate = System.currentTimeMillis();
            }
        }

        return entries;
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
