package com.prismcore.survival.sell.listeners;

import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsListener implements Listener {

    private final PrismSell plugin;
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public PlayerStatsListener(PrismSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPlayerDataManager() != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                data.setBreakBlocks(data.getBreakBlocks() + 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPlayerDataManager() != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                data.setPlacedBlocks(data.getPlacedBlocks() + 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            if (plugin.getPlayerDataManager() != null) {
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
                if (data != null) {
                    data.setMobKills(data.getMobKills() + 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (plugin.getPlayerDataManager() != null) {
            PlayerData victimData = plugin.getPlayerDataManager().getPlayerData(victim.getUniqueId());
            if (victimData != null) {
                victimData.setDeaths(victimData.getDeaths() + 1);
            }

            if (victim.getKiller() != null) {
                Player killer = victim.getKiller();
                PlayerData killerData = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
                if (killerData != null) {
                    killerData.setKills(killerData.getKills() + 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        joinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (joinTimes.containsKey(uuid)) {
            long joinTime = joinTimes.remove(uuid);
            long sessionTime = System.currentTimeMillis() - joinTime;
            long sessionSeconds = sessionTime / 1000;

            if (plugin.getPlayerDataManager() != null) {
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
                if (data != null) {
                    data.setPlaytime(data.getPlaytime() + sessionSeconds);
                    // Save and unload data
                    plugin.getPlayerDataManager().unloadPlayer(uuid);
                }
            }
        }
    }
}
