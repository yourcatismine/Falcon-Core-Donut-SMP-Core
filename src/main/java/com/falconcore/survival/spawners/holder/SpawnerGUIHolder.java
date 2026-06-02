package com.falconcore.survival.spawners.holder;

import com.falconcore.survival.spawners.storage.SpawnerData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SpawnerGUIHolder implements InventoryHolder {
    private final SpawnerData data;
    private final boolean isStorage;
    private int page;

    public SpawnerGUIHolder(SpawnerData data, boolean isStorage) {
        this(data, isStorage, 1);
    }

    public SpawnerGUIHolder(SpawnerData data, boolean isStorage, int page) {
        this.data = data;
        this.isStorage = isStorage;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public SpawnerData getData() {
        return data;
    }

    public boolean isStorage() {
        return isStorage;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
