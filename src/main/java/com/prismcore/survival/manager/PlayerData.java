package com.prismcore.survival.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private double shards;
    private double money;
    private double shopSpent;
    private final Map<String, Integer> keys = new HashMap<>();
    private String name; // Cached name
    private long shardBoosterExpiry; // Timestamp when shard booster expires

    public PlayerData(UUID uuid) {
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

    public void setShards(double shards) {
        this.shards = shards;
    }

    public void addShards(double amount) {
        this.shards += amount;
    }

    public void removeShards(double amount) {
        this.shards -= amount;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    public void removeMoney(double amount) {
        this.money -= amount;
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

    private boolean quickAuctionBuy = false; // Default: OFF

    public boolean isQuickAuctionBuy() {
        return quickAuctionBuy;
    }

    public void setQuickAuctionBuy(boolean quickAuctionBuy) {
        this.quickAuctionBuy = quickAuctionBuy;
    }
}
