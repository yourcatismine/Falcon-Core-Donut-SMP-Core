package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final PrismSurvival plugin;
    private final UUID uuid;
    private double shards;
    private double money;
    private double shopSpent;
    private boolean muted;
    private String muteReason;
    private long muteExpiry;
    private String muteId;
    private String mutedBy;
    private long muteDate;
    private final Map<String, Integer> keys = new HashMap<>();
    private String name; // Cached name
    private long shardBoosterExpiry; // Timestamp when shard booster expires
    private boolean vanished = false;
    private boolean combatLogged = false;
    private boolean teamChat = false;
    private String pendingKickTeamName = null;
    private boolean nameHidden = false;

    public PlayerData(PrismSurvival plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.shards = 0.0;
        this.money = 0.0;
        this.shopSpent = 0.0;
        this.shardBoosterExpiry = 0;
        this.name = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getShards() {
        return shards;
    }

    public synchronized void setShards(double shards) {
        this.setShards(shards, "Manual Set");
    }

    public synchronized void setShards(double shards, String source) {
        double old = this.shards;
        this.shards = shards;
        logEconomyChange(shards - old, "SHARDS", source);
    }

    public synchronized void addShards(double amount) {
        this.addShards(amount, "Unknown");
    }

    public synchronized void addShards(double amount, String source) {
        this.shards += amount;
        logEconomyChange(amount, "SHARDS", source);
    }

    public synchronized void removeShards(double amount) {
        this.removeShards(amount, "Unknown");
    }

    public synchronized void removeShards(double amount, String source) {
        this.shards -= amount;
        logEconomyChange(-amount, "SHARDS", source);
    }

    public synchronized double getMoney() {
        return money;
    }

    public synchronized void setMoney(double money) {
        this.setMoney(money, "Manual Set");
    }

    public synchronized void setMoney(double money, String source) {
        if (!Double.isFinite(money))
            return;
        double old = this.money;
        this.money = money;
        logEconomyChange(money - old, "MONEY", source);
    }

    public synchronized void addMoney(double amount) {
        this.addMoney(amount, "Unknown");
    }

    public synchronized void addMoney(double amount, String source) {
        if (!Double.isFinite(amount) || amount < 0)
            return;
        this.money += amount;
        logEconomyChange(amount, "MONEY", source);
    }

    public synchronized void removeMoney(double amount) {
        this.removeMoney(amount, "Unknown");
    }

    public synchronized void removeMoney(double amount, String source) {
        if (!Double.isFinite(amount) || amount < 0)
            return;
        this.money -= amount;
        logEconomyChange(-amount, "MONEY", source);
    }

    private void logEconomyChange(double change, String type, String source) {
        if (Math.abs(change) < 0.001)
            return;

        String prefix = change >= 0 ? "+" : "";
        String symbol = type.equals("MONEY") ? "$" : "";
        String suffix = type.equals("SHARDS") ? "x Shards" : "";

        String formattedChange = prefix + symbol + String.format("%,.2f", change) + suffix;
        double currentBalance = type.equals("MONEY") ? this.money : this.shards;
        String formattedBalance = symbol + String.format("%,.2f", currentBalance) + suffix;

        String content = formattedChange + " (" + source + ") | Bal: " + formattedBalance;

        ActivityLogger.LogType logType = type.equals("MONEY") ? ActivityLogger.LogType.MONEY
                : ActivityLogger.LogType.SHARDS;

        if (plugin.getActivityLogger() != null) {
            plugin.getActivityLogger().log(uuid, logType, content);
        }
    }

    public double getShopSpent() {
        return shopSpent;
    }

    public void setShopSpent(double shopSpent) {
        this.shopSpent = shopSpent;
    }

    public void addShopSpent(double amount) {
        this.shopSpent += amount;
    }

    // Key management
    public void addKey(String keyName) {
        int current = keys.getOrDefault(keyName, 0);
        keys.put(keyName, current + 1);
    }

    public void removeKey(String keyName) {
        int current = keys.getOrDefault(keyName, 0);
        if (current > 0) {
            keys.put(keyName, current - 1);
        }
    }

    public int getKeyCount(String keyName) {
        return keys.getOrDefault(keyName, 0);
    }

    public void setKeyCount(String keyName, int count) {
        keys.put(keyName, count);
    }

    public Map<String, Integer> getKeys() {
        return new HashMap<>(keys);
    }

    public Map<String, Integer> getAllKeys() {
        return keys;
    }

    // Update tracking
    private long lastSeenUpdate = 0;

    public long getLastSeenUpdate() {
        return lastSeenUpdate;
    }

    public void setLastSeenUpdate(long lastSeenUpdate) {
        this.lastSeenUpdate = lastSeenUpdate;
    }

    // Shard Booster management
    public boolean hasActiveShardBooster() {
        return shardBoosterExpiry > System.currentTimeMillis();
    }

    public long getShardBoosterExpiry() {
        return shardBoosterExpiry;
    }

    public void setShardBoosterExpiry(long expiryMillis) {
        this.shardBoosterExpiry = expiryMillis;
    }

    public long getShardBoosterRemainingSeconds() {
        if (!hasActiveShardBooster())
            return 0;
        return (shardBoosterExpiry - System.currentTimeMillis()) / 1000L;
    }

    // Settings
    private boolean hideChat = false; // Default: Chat Visible (OFF from "Hide Chat" perspective)

    public boolean isHideChat() {
        return hideChat;
    }

    public void setHideChat(boolean hideChat) {
        this.hideChat = hideChat;
    }

    private boolean privateMessages = true; // Default: ON (Allowed)

    public boolean isPrivateMessages() {
        return privateMessages;
    }

    public void setPrivateMessages(boolean privateMessages) {
        this.privateMessages = privateMessages;
    }

    private boolean payAlerts = true; // Default: ON (Receive Alerts)

    public boolean isPayAlerts() {
        return payAlerts;
    }

    public void setPayAlerts(boolean payAlerts) {
        this.payAlerts = payAlerts;
    }

    private boolean quickAuctionBuy = false;
    private boolean disableMobSpawns = false;

    public boolean isDisableMobSpawns() {
        return disableMobSpawns;
    }

    public void setDisableMobSpawns(boolean disableMobSpawns) {
        this.disableMobSpawns = disableMobSpawns;
    } // Default: OFF

    public boolean isQuickAuctionBuy() {
        return quickAuctionBuy;
    }

    public void setQuickAuctionBuy(boolean quickAuctionBuy) {
        this.quickAuctionBuy = quickAuctionBuy;
    }

    private boolean soundNotifications = true; // Default: ON

    public boolean isSoundNotifications() {
        return soundNotifications;
    }

    public void setSoundNotifications(boolean soundNotifications) {
        this.soundNotifications = soundNotifications;
    }

    private boolean tpaConfirmMenus = true; // Default: ON

    public boolean isTpaConfirmMenus() {
        return tpaConfirmMenus;
    }

    public void setTpaConfirmMenus(boolean tpaConfirmMenus) {
        this.tpaConfirmMenus = tpaConfirmMenus;
    }

    private boolean duelRequests = true; // Default: ON

    public boolean isDuelRequests() {
        return duelRequests;
    }

    public void setDuelRequests(boolean duelRequests) {
        this.duelRequests = duelRequests;
    }

    private boolean tpaRequests = true; // Default: ON

    public boolean isTpaRequests() {
        return tpaRequests;
    }

    public void setTpaRequests(boolean tpaRequests) {
        this.tpaRequests = tpaRequests;
    }

    private boolean tpaHereRequests = true; // Default: ON

    public boolean isTpaHereRequests() {
        return tpaHereRequests;
    }

    public void setTpaHereRequests(boolean tpaHereRequests) {
        this.tpaHereRequests = tpaHereRequests;
    }

    private boolean showScoreboard = true; // Default: ON

    public boolean isShowScoreboard() {
        return showScoreboard;
    }

    public void setShowScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
    }

    private boolean payments = true; // Default: ON

    public boolean isPayments() {
        return payments;
    }

    public void setPayments(boolean payments) {
        this.payments = payments;
    }

    private boolean shardsNotifier = true; // Default: ON

    public boolean isShardsNotifier() {
        return shardsNotifier;
    }

    public void setShardsNotifier(boolean shardsNotifier) {
        this.shardsNotifier = shardsNotifier;
    }

    private boolean tpAuto = false; // Default: OFF

    public boolean isTpAuto() {
        return tpAuto;
    }

    public void setTpAuto(boolean tpAuto) {
        this.tpAuto = tpAuto;
    }

    private String auctionSortOrder = "Highest Price"; // Default
    private String auctionFilter = "";
    private String auctionCategory = "All";

    public String getAuctionSortOrder() {
        return auctionSortOrder;
    }

    public void setAuctionSortOrder(String auctionSortOrder) {
        this.auctionSortOrder = auctionSortOrder;
    }

    public String getAuctionFilter() {
        return auctionFilter;
    }

    public void setAuctionFilter(String auctionFilter) {
        this.auctionFilter = auctionFilter;
    }

    public String getAuctionCategory() {
        return auctionCategory;
    }

    public void setAuctionCategory(String auctionCategory) {
        this.auctionCategory = auctionCategory;
    }

    public boolean isMuted() {
        if (muted && muteExpiry > 0 && muteExpiry < System.currentTimeMillis()) {
            muted = false;
        }
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(String muteReason) {
        this.muteReason = muteReason;
    }

    public long getMuteExpiry() {
        return muteExpiry;
    }

    public void setMuteExpiry(long muteExpiry) {
        this.muteExpiry = muteExpiry;
    }

    public String getMuteId() {
        return muteId;
    }

    public void setMuteId(String muteId) {
        this.muteId = muteId;
    }

    public String getMutedBy() {
        return mutedBy;
    }

    public void setMutedBy(String mutedBy) {
        this.mutedBy = mutedBy;
    }

    public long getMuteDate() {
        return muteDate;
    }

    public void setMuteDate(long muteDate) {
        this.muteDate = muteDate;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    private String teamId = null;
    private String teamRole = null;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public boolean isTeamChat() {
        return teamChat;
    }

    public void setTeamChat(boolean teamChat) {
        this.teamChat = teamChat;
    }

    public boolean isCombatLogged() {
        return combatLogged;
    }

    public void setCombatLogged(boolean combatLogged) {
        this.combatLogged = combatLogged;
    }

    public String getPendingKickTeamName() {
        return pendingKickTeamName;
    }

    public void setPendingKickTeamName(String pendingKickTeamName) {
        this.pendingKickTeamName = pendingKickTeamName;
    }

    private final Map<String, TeamInvite> teamInvites = new HashMap<>();
    private String lastInviter = null;

    public void addTeamInvite(String teamId, String inviterName, long expiry) {
        teamInvites.put(inviterName.toLowerCase(), new TeamInvite(teamId, inviterName, expiry));
        this.lastInviter = inviterName;
    }

    public TeamInvite getTeamInvite(String inviterName) {
        if (inviterName == null)
            return null;
        TeamInvite invite = teamInvites.get(inviterName.toLowerCase());
        if (invite != null && invite.isExpired()) {
            teamInvites.remove(inviterName.toLowerCase());
            if (inviterName.equalsIgnoreCase(lastInviter))
                lastInviter = null;
            return null;
        }
        return invite;
    }

    public String getLastInviter() {
        if (lastInviter != null) {
            TeamInvite invite = getTeamInvite(lastInviter);
            if (invite == null)
                lastInviter = null;
        }
        return lastInviter;
    }

    public void removeTeamInvite(String inviterName) {
        teamInvites.remove(inviterName.toLowerCase());
        if (inviterName.equalsIgnoreCase(lastInviter))
            lastInviter = null;
    }

    public boolean isNameHidden() {
        return nameHidden;
    }

    public void setNameHidden(boolean nameHidden) {
        this.nameHidden = nameHidden;
    }

    public static class TeamInvite {
        private final String teamId;
        private final String inviterName;
        private final long expiry;

        public TeamInvite(String teamId, String inviterName, long expiry) {
            this.teamId = teamId;
            this.inviterName = inviterName;
            this.expiry = expiry;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getInviterName() {
            return inviterName;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }
}
