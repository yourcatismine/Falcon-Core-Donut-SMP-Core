package com.h2ph.listeners;

import com.h2ph.Falcon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.entity.Entity;
import java.util.stream.Collectors;
import java.util.List;

public class LiveSignListener implements Listener {

    private final Falcon plugin;

    public LiveSignListener(Falcon plugin) {
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

        List<String> nearby = player.getNearbyEntities(10, 10, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> ((Player) e).getName())
                .collect(Collectors.toList());

        plugin.getApiServer().broadcastSignUsage(
                player,
                text,
                nearby);
    }
}
