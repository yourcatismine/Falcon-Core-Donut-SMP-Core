package com.prismcore.survival.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PvPSafeZoneManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player movement tracking for PvP safe zones
 */
public class PvPSafeZoneListener implements Listener {

    private final PrismSurvival plugin;
    private final PvPSafeZoneManager safeZoneManager;
    
    // Track which players are currently in safe zones
    private final Map<UUID, Boolean> playersInSafeZone = new HashMap<>();

    public PvPSafeZoneListener(PrismSurvival plugin) {
        this.plugin = plugin;
        this.safeZoneManager = plugin.getPvPSafeZoneManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only check if player moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        boolean wasInSafeZone = playersInSafeZone.getOrDefault(playerId, false);
        boolean isNowInSafeZone = safeZoneManager.isInSafeZone(event.getTo());

        // Player entered a safe zone
        if (!wasInSafeZone && isNowInSafeZone) {
            playersInSafeZone.put(playerId, true);
            showSafeZoneEntry(player);
        }
        // Player left a safe zone
        else if (wasInSafeZone && !isNowInSafeZone) {
            playersInSafeZone.put(playerId, false);
            showSafeZoneExit(player);
        }
    }

    /**
     * Display safe zone entry messages
     */
    private void showSafeZoneEntry(Player player) {
        // Send title
        String title = ChatColor.translateAlternateColorCodes('&', "&6ѕᴀꜰᴇ ᴍᴏᴅᴇ");
        String subtitle = ChatColor.translateAlternateColorCodes('&', "&fYou are safe to this area");
        player.sendTitle(title, subtitle, 10, 40, 10);

        // Send actionbar
        String actionBar = ChatColor.translateAlternateColorCodes('&', "&fYou are safe to this area");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));

        // Play sound
        try {
            player.playSound(player.getLocation(), "minecraft:ambient.cave", 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback to different sound if ambient.cave doesn't exist
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            } catch (Exception ex) {
                // If both sounds fail, use a basic sound
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Display safe zone exit messages  
     */
    private void showSafeZoneExit(Player player) {
        // Send title
        String title = ChatColor.translateAlternateColorCodes('&', "&4ᴘᴠᴘ ᴍᴏᴅᴇ");
        String subtitle = ChatColor.translateAlternateColorCodes('&', "&fGo and PvP with someone!");
        player.sendTitle(title, subtitle, 10, 40, 10);

        // Send actionbar
        String actionBar = ChatColor.translateAlternateColorCodes('&', "&fGo and PvP with someone!");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));

        // Play sound
        try {
            player.playSound(player.getLocation(), "minecraft:ambient.cave", 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback to different sound if ambient.cave doesn't exist
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            } catch (Exception ex) {
                // If both sounds fail, use a basic sound
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Clean up tracking when player leaves
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersInSafeZone.remove(event.getPlayer().getUniqueId());
    }
}