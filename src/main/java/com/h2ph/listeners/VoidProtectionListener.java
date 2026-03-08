package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.VoidManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class VoidProtectionListener implements Listener {

    private final PrismSurvival plugin;

    public VoidProtectionListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null)
            return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        VoidManager voidManager = plugin.getVoidManager();

        if (voidManager != null && voidManager.isInVoid(event.getTo())) {
            teleportToSpawn(player);
        }
    }

    private void teleportToSpawn(Player p) {
        Location spawnLoc = null;
        String spawnName = "default";

        String worldName = p.getWorld().getName();
        
        if (plugin.getSpawnManager() != null) {
            spawnLoc = plugin.getSpawnManager().getBestSpawnForWorld(worldName);
            spawnName = "world spawn";
            
            if (spawnLoc == null) {
                spawnLoc = plugin.getSpawnManager().getSpawn("spawn");
                if (spawnLoc == null) {
                    java.util.List<String> spawns = plugin.getSpawnManager().listSpawns();
                    if (!spawns.isEmpty()) {
                        spawnName = spawns.get(0);
                        spawnLoc = plugin.getSpawnManager().getSpawn(spawnName);
                    }
                }
            }
        }

        if (spawnLoc != null) {
            final String finalSpawnName = spawnName;
            final Location finalSpawnLoc = spawnLoc;

            p.teleportAsync(finalSpawnLoc).thenAccept(success -> {
                if (success) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    String msg = ChatColor.translateAlternateColorCodes('&', "&7You fell into the &6Void&7!");
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                }
            });
        }
    }
}
