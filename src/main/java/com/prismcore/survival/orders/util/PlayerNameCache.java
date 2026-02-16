package com.prismcore.survival.orders.util;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerNameCache {

    private final PrismSurvival plugin;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public PlayerNameCache(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public String getName(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        // Check if online first (fast)
        org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            String name = p.getName();
            cache.put(uuid, name);
            return name;
        }

        // If not cached and not online, return null and trigger async fetch
        // Caller should handle null (e.g. show "Loading...")
        fetchAsync(uuid);
        return null;
    }

    private void fetchAsync(UUID uuid) {
        // Prevent duplicate fetches if already fetching?
        // For simplicity, just run it. ConcurrentHashMap handles the put safety.
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            if (name != null) {
                cache.put(uuid, name);
            }
        });
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}
