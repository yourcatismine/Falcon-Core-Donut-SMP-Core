package com.h2ph.placeholders;

import com.h2ph.Falcon;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class RTPPlaceholders extends PlaceholderExpansion {

    private final Falcon plugin;

    public RTPPlaceholders(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "falconsmp";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "h2ph";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.toLowerCase().startsWith("countdown_")) {
            String regionName = params.substring(10);
            return String.valueOf(plugin.getRTPQueueManager().getCountdown(regionName));
        }

        return null;
    }
}
