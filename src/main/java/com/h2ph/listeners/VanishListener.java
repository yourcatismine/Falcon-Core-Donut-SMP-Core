package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.VanishManager;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class VanishListener implements Listener {

    private final PrismSurvival plugin;

    public VanishListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        VanishManager vanishManager = plugin.getVanishManager();

        // 1. If the JOINING player is vanished, hide them from everyone who shouldn't
        // see them
        if (vanishManager.isVanished(player.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player))
                    continue;
                    
                // Validate observer is still online and valid
                if (!online.isOnline() || !online.isValid()) {
                    continue;
                }
                    
                if (!online.hasPermission("prism.admin.vanish.see")) {
                    try {
                        online.hidePlayer(plugin, player);
                    } catch (Exception e) {
                        // Silently handle packet encoding errors to prevent server crash
                        plugin.getLogger().fine("Failed to hide joining player " + player.getName() + " from " + online.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // 2. Hide already vanished players from the JOINING player
        // Delay slightly to ensure tab list is initialized
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            // Validate joining player is still online
            if (!player.isOnline() || !player.isValid()) {
                return;
            }
            
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player))
                    continue;
                    
                // Validate target player is still online and valid
                if (!online.isOnline() || !online.isValid()) {
                    continue;
                }
                    
                if (vanishManager.isVanished(online.getUniqueId())) {
                    if (!player.hasPermission("prism.admin.vanish.see")) {
                        try {
                            player.hidePlayer(plugin, online);
                        } catch (Exception e) {
                            // Silently handle packet encoding errors to prevent server crash
                            plugin.getLogger().fine("Failed to hide vanished player " + online.getName() + " from joining player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        VanishManager vanishManager = plugin.getVanishManager();

        // Simple check for @mentions
        if (message.contains("@")) {
            for (Player vanishedPlayer : Bukkit.getOnlinePlayers()) {
                if (vanishManager.isVanished(vanishedPlayer.getUniqueId())) {
                    String mention = "@" + vanishedPlayer.getName();
                    if (message.toLowerCase().contains(mention.toLowerCase())) {
                        // Suppress the mention if the sender isn't staff
                        if (!event.getPlayer().hasPermission("prism.admin.vanish.see")) {
                            // Replace @VanishedName with just the name or something else to "un-mention"
                            // For simplicity, let's just strip the @ if they type it
                            String newMessage = message.replaceAll("(?i)@" + vanishedPlayer.getName(),
                                    vanishedPlayer.getName());
                            event.setMessage(newMessage);
                        }
                    }
                }
            }

            // Check for ignored player mentions
            com.prismcore.survival.manager.PlayerData senderData = plugin.getPlayerDataManager()
                    .get(event.getPlayer().getUniqueId());
            if (senderData != null) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    UUID mentionedPlayerUuid = onlinePlayer.getUniqueId();
                    String mention = "@" + onlinePlayer.getName();
                    
                    if (message.toLowerCase().contains(mention.toLowerCase())) {
                        // Check if the mentioned player has ignored the sender
                        com.prismcore.survival.manager.PlayerData mentionedPlayerData = plugin.getPlayerDataManager()
                                .get(mentionedPlayerUuid);
                        if (mentionedPlayerData != null && mentionedPlayerData.isIgnoring(event.getPlayer().getUniqueId())) {
                            // Suppress the mention by removing the @
                            String newMessage = message.replaceAll("(?i)@" + onlinePlayer.getName(),
                                    onlinePlayer.getName());
                            event.setMessage(newMessage);
                        }
                    }
                }
            }
        }
    }
}
