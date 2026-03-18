package com.prismcore.survival.spawners.storage;

import com.prismcore.survival.spawners.mob.SpawnerType;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerData {
    private Location location;
    private UUID owner;
    private SpawnerType type;
    private int stackSize;
    private Map<Material, Long> accumulatedDrops;
    private long accumulatedXP;

    public SpawnerData(Location location, UUID owner, SpawnerType type) {
        this(location, owner, type, 1);
    }

    public SpawnerData(Location location, UUID owner, SpawnerType type, int stackSize) {
        this.location = location;
        this.owner = owner;
        this.type = type;
        this.stackSize = stackSize;
        this.accumulatedDrops = new ConcurrentHashMap<>();
        this.accumulatedXP = 0;
    }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public SpawnerType getType() { return type; }
    public void setType(SpawnerType type) { this.type = type; }

    public int getStackSize() { return stackSize; }
    public void setStackSize(int stackSize) { this.stackSize = stackSize; }

    public Map<Material, Long> getAccumulatedDrops() { return accumulatedDrops; }
    public void setAccumulatedDrops(Map<Material, Long> accumulatedDrops) {
        this.accumulatedDrops = new ConcurrentHashMap<>(accumulatedDrops);
    }

    public long getAccumulatedXP() { return accumulatedXP; }
    public void setAccumulatedXP(long accumulatedXP) { this.accumulatedXP = accumulatedXP; }

    public void addDrops(Map<Material, Long> drops) {
        for (Map.Entry<Material, Long> entry : drops.entrySet()) {
            accumulatedDrops.put(entry.getKey(), accumulatedDrops.getOrDefault(entry.getKey(), 0L) + entry.getValue());
        }
    }

    public void addXP(long xp) {
        accumulatedXP += xp;
    }
}
