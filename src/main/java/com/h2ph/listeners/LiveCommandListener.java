package com.h2ph.listeners;

import com.h2ph.Falcon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class LiveCommandListener implements Listener {

    private final Falcon plugin;

    public LiveCommandListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled())
            return;

        plugin.getApiServer().broadcastCommandUsage(
                event.getPlayer(),
                event.getMessage());

    }
}
