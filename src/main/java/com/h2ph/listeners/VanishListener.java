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
                if (!online.hasPermission("prism.admin.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
        }

        // 2. Hide already vanished players from the JOINING player
        // Delay slightly to ensure tab list is initialized
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player))
                    continue;
                if (vanishManager.isVanished(online.getUniqueId())) {
                    if (!player.hasPermission("prism.admin.vanish.see")) {
                        player.hidePlayer(plugin, online);
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
        }
    }
}
