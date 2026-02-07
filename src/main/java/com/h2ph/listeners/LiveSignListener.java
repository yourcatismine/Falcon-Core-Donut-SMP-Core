package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.entity.Entity;
import java.util.stream.Collectors;
import java.util.List;

public class LiveSignListener implements Listener {

    private final PrismSurvival plugin;

    public LiveSignListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        String[] lines = event.getLines();
        String text = String.join(" ", lines).trim();

        if (text.isEmpty())
            return;

        // Get nearby players within 10 block radius
        List<String> nearby = player.getNearbyEntities(10, 10, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> ((Player) e).getName())
                .collect(Collectors.toList());

        // Broadcast to API Server
        plugin.getApiServer().broadcastSignUsage(
                player,
                text,
                nearby);
    }
}
