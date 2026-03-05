package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.ActivityLogger;
import com.prismcore.survival.manager.PlayerData;
import com.prismcore.survival.orders.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PrismSurvival plugin;

    public PlayerConnectionListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        // Check if database is connected
        if (!plugin.getDatabaseManager().isConnected()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Utils.formatColors("&cThe database cannot fetch your data."));
            return;
        }

        // Preload player data asynchronously. This puts it into the cache.
        PlayerData data = plugin.getPlayerDataManager().get(event.getUniqueId());

        // Preload team as well to prevent ScoreboardManager from blocking on main
        // thread
        if (data != null && data.getTeamId() != null) {
            plugin.getTeamManager().getTeam(data.getTeamId());
        }

        // Preload enderchest
        plugin.getEnderChestManager().preload(event.getUniqueId(), event.getName());

        // Log IP for alt-account tracking
        if (data != null) {
            data.setIp(event.getAddress().getHostAddress());
        }

        if (plugin.getOffendPlugin() != null && plugin.getOffendPlugin().getDatabaseManager() != null) {
            String ip = event.getAddress().getHostAddress();
            plugin.getOffendPlugin().getDatabaseManager().logIP(event.getUniqueId(), ip);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // This will be instant as data was preloaded in AsyncPlayerPreLoginEvent
        PlayerData data = plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        data.setLastSeenUpdate(System.currentTimeMillis());
        plugin.getDatabaseManager().updateStatusAsync(event.getPlayer().getUniqueId(), "Online");

        plugin.getActivityLogger().log(event.getPlayer().getUniqueId(), ActivityLogger.LogType.GENERAL,
                "Joined the server");

        // Cancel any stale teleport tasks
        plugin.getTeleportManager().cancelActiveTask(event.getPlayer().getUniqueId());

        // Kick notification
        if (data.getPendingKickTeamName() != null) {
            String teamName = data.getPendingKickTeamName();
            data.setPendingKickTeamName(null);

            Player player = event.getPlayer();
            String msg = Utils.formatColors("&7You were kicked from " + teamName + "&7 while you were away.");
            player.sendMessage(msg);
            player.sendActionBar(net.kyori.adventure.text.Component.text(msg));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save and unload data
        plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId())
                .setLastSeenUpdate(System.currentTimeMillis());
        plugin.getActivityLogger().log(event.getPlayer().getUniqueId(), ActivityLogger.LogType.GENERAL,
                "Left the server");
        plugin.getDatabaseManager().updateStatusAsync(event.getPlayer().getUniqueId(), "Offline");
        plugin.getDatabaseManager().saveLastLocationAsync(event.getPlayer().getUniqueId(),
                event.getPlayer().getLocation());

        // Cancel any active teleport tasks before unloading
        plugin.getTeleportManager().cancelActiveTask(event.getPlayer().getUniqueId());

        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());

        // Unload enderchest
        plugin.getEnderChestManager().unload(event.getPlayer().getUniqueId());
    }
}
