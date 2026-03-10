/*
 * Decompiled with CFR 0.152.
 */
package com.prismcore.survival.sell.data;

import com.prismcore.survival.sell.category.Category;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final Map<Category, Double> categoryProgress;
    private final Map<Category, Double> categoryMultipliers;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.categoryProgress = new HashMap<Category, Double>();
        this.categoryMultipliers = new HashMap<Category, Double>();
        Category[] categoryArray = Category.values();
        int n = categoryArray.length;
        int n2 = 0;
        while (n2 < n) {
            Category category = categoryArray[n2];
            this.categoryProgress.put(category, 0.0);
            this.categoryMultipliers.put(category, 1.0);
            ++n2;
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public double getProgress(Category category) {
        return this.categoryProgress.getOrDefault((Object) category, 0.0);
    }

    public void setProgress(Category category, double progress) {
        this.categoryProgress.put(category, progress);
        this.markDirty();
    }

    public void addProgress(Category category, double amount) {
        double current = this.getProgress(category);
        this.categoryProgress.put(category, current + amount);
        this.markDirty();
    }

    public double getMultiplier(Category category) {
        return this.categoryMultipliers.getOrDefault((Object) category, 1.0);
    }

    public void setMultiplier(Category category, double multiplier) {
        this.categoryMultipliers.put(category, multiplier);
        this.markDirty();
    }

    public Map<Category, Double> getAllProgress() {
        return new HashMap<Category, Double>(this.categoryProgress);
    }

    public Map<Category, Double> getAllMultipliers() {
        return new HashMap<Category, Double>(this.categoryMultipliers);
    }

    private boolean dirty;

    private long breakBlocks;
    private long placedBlocks;
    private long mobKills;
    private double sellMade;
    private long playtime;
    private long deaths;
    private long kills;
    private long toolExpiry;

    public long getBreakBlocks() {
        return breakBlocks;
    }

    public void setBreakBlocks(long breakBlocks) {
        if (this.breakBlocks != breakBlocks) {
            this.breakBlocks = breakBlocks;
            this.markDirty();
        }
    }

    public long getPlacedBlocks() {
        return placedBlocks;
    }

    public void setPlacedBlocks(long placedBlocks) {
        if (this.placedBlocks != placedBlocks) {
            this.placedBlocks = placedBlocks;
            this.markDirty();
        }
    }

    public long getMobKills() {
        return mobKills;
    }

    public void setMobKills(long mobKills) {
        if (this.mobKills != mobKills) {
            this.mobKills = mobKills;
            this.markDirty();
        }
    }

    public double getSellMade() {
        return sellMade;
    }

    public void setSellMade(double sellMade) {
        if (this.sellMade != sellMade) {
            this.sellMade = sellMade;
            this.markDirty();
        }
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        if (this.playtime != playtime) {
            this.playtime = playtime;
            this.markDirty();
        }
    }

    public long getDeaths() {
        return deaths;
    }

    public void setDeaths(long deaths) {
        if (this.deaths != deaths) {
            this.deaths = deaths;
            this.markDirty();
        }
    }

    public long getKills() {
        return kills;
    }

    public void setKills(long kills) {
        if (this.kills != kills) {
            this.kills = kills;
            this.markDirty();
        }
    }

    public long getToolExpiry() {
        return toolExpiry;
    }

    public void setToolExpiry(long toolExpiry) {
        if (this.toolExpiry != toolExpiry) {
            this.toolExpiry = toolExpiry;
            this.markDirty();
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void resetDirty() {
        this.dirty = false;
    }

    private boolean loadingFailed = false;

    public boolean isLoadingFailed() {
        return loadingFailed;
    }

    public void setLoadingFailed(boolean loadingFailed) {
        this.loadingFailed = loadingFailed;
    }

}
