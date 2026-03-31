package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandLogListener implements Listener {

    private final Falcon plugin;
    private final Map<UUID, Long> lastLogTime = new HashMap<>();
    private final Map<UUID, Integer> logBurstCount = new HashMap<>();

    public CommandLogListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();

        // IGNORE spammy block manipulation commands
        String lowerCmd = command.toLowerCase();
        if (lowerCmd.startsWith("/fill") || 
            lowerCmd.startsWith("/setblock") || 
            lowerCmd.startsWith("/clone") || 
            lowerCmd.startsWith("//")) { // WorldEdit
            return;
        }

        // RATE LIMIT logging to prevent performance issues
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastLogTime.getOrDefault(uuid, 0L);
        
        if (now - last < 1000) {
            int burst = logBurstCount.getOrDefault(uuid, 0);
            if (burst >= 5) {
                return; // Log at most 5 commands per second
            }
            logBurstCount.put(uuid, burst + 1);
        } else {
            lastLogTime.put(uuid, now);
            logBurstCount.put(uuid, 1);
        }

        String dateTime = LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"));

        String historyMsg = dateTime + " - Executed: " + command;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) {
            data.addHistory(historyMsg);

            // Debounced save is handled in SavePlayerAsync now usually, 
            // but we can also just avoid saving on EVERY command if it's too fast.
            // For now, PlayerDataManager.savePlayerAsync is already async.
            plugin.getPlayerDataManager().savePlayerAsync(player.getUniqueId());
        }
    }
}
