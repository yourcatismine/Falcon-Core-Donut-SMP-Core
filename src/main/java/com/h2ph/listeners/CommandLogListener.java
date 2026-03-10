package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CommandLogListener implements Listener {

    private final PrismSurvival plugin;

    public CommandLogListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();

        // 1. Get current UTC time
        String dateTime = LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"));

        // 2. Format history message
        String historyMsg = dateTime + " - Executed: " + command;

        // 3. Add to player history
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) {
            data.addHistory(historyMsg);

            // 4. Save asynchronously
            plugin.getPlayerDataManager().savePlayerAsync(player.getUniqueId());
        }
    }
}
