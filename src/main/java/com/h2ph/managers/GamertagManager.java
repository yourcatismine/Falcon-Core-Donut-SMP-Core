package com.h2ph.managers;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class GamertagManager {

    public GamertagManager(Falcon plugin) {
    }

    public String getGamertag(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName();
    }
}
