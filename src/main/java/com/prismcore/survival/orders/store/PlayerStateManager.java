/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.prismcore.survival.orders.store;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.data.AlphaSort;
import com.prismcore.survival.orders.data.SortType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class PlayerStateManager {
    private final OrdersModule module;
    private final Map<UUID, View> main = new HashMap<UUID, View>();
    private final Map<UUID, ItemView> selectItem = new HashMap<UUID, ItemView>();

    public PlayerStateManager(OrdersModule module) {
        this.module = module;
    }

    public View main(UUID u) {
        return this.main.computeIfAbsent(u, k -> new View());
    }

    public void resetMain(UUID u) {
        this.main.put(u, new View());
    }

    public ItemView items(UUID u) {
        return this.selectItem.computeIfAbsent(u, k -> new ItemView());
    }

    public void resetItems(UUID u) {
        this.selectItem.put(u, new ItemView());
    }

    public static class View {
        public int page = 0;
        public SortType sort = SortType.MOST_PAID;
        public String filter = "All";
        public String search = null;
    }

    public static class ItemView {
        public int page = 0;
        public AlphaSort alpha = AlphaSort.A_Z;
        public String filter = "All";
        public String search = null;
    }
}
