package com.h2ph.managers;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VanishManager {

    private final Falcon plugin;

    public VanishManager(Falcon plugin) {
        this.plugin = plugin;
        startVanishTask();
    }

    /**
     * Toggles vanish state for a player
     */
    public void toggleVanish(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean isVanished = !data.isVanished();
        data.setVanished(isVanished);

        if (isVanished) {
            hidePlayer(player);
        } else {
            showPlayer(player);
        }
    }

    /**
     * Hides a player from all other players
     */
    public void hidePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player))
                continue;

            if (!online.isOnline() || !online.isValid()) {
                continue;
            }

            if (!online.hasPermission("falcon.vanish.see")) {
                try {
                    online.hidePlayer(plugin, player);
                } catch (Exception e) {
                    plugin.getLogger().fine("Failed to hide player " + player.getName() + " from " + online.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Shows a player to all other players
     */
    public void showPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.isOnline() || !online.isValid()) {
                continue;
            }
            
            try {
                online.showPlayer(plugin, player);
            } catch (Exception e) {
                plugin.getLogger().fine("Failed to show player " + player.getName() + " to " + online.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Task to keep vanished players aware of their status
     */
    private void startVanishTask() {
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data.isVanished()) {
                    String message = ChatColor.translateAlternateColorCodes('&', "&fVanish Activated");
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                }
            }
        }, 20L, 20L);
    }

    /**
     * Checks if a player is vanished
     */
    public boolean isVanished(UUID uuid) {
        PlayerData data = plugin.getPlayerDataManager().get(uuid);
        return data != null && data.isVanished();
    }

    /**
     * Checks if a player name belongs to a vanished player
     */
    public boolean isVanished(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return isVanished(player.getUniqueId());
        }
        return false;
    }
}
