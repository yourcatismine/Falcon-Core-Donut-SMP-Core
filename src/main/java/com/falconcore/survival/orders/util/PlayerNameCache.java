package com.falconcore.survival.orders.util;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerNameCache {

    private final Falcon plugin;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public PlayerNameCache(Falcon plugin) {
        this.plugin = plugin;
    }

    public String getName(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            String name = p.getName();
            cache.put(uuid, name);
            return name;
        }

        fetchAsync(uuid);
        return null;
    }

    private void fetchAsync(UUID uuid) {
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
