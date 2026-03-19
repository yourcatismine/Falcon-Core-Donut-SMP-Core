package com.prismcore.survival.survival;

import com.h2ph.PrismSurvival;
import com.h2ph.utils.LuckPermsUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatFormatter implements Listener {

    private final PrismSurvival plugin;

    public ChatFormatter(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data.isDisguised()) {
            return;
        }
        
        String prefix = getPlayerPrefix(player);

        String format;
        if (prefix == null || prefix.isEmpty()) {
            format = "&7%player_name%:&f %message%";
        } else {
            format = "%prefix%&7%player_name%:&f %message%";
        }

        format = format.replace("%prefix%", prefix)
                .replace("%player_name%", player.getName())
                .replace("%message%", "%2$s");

        event.setFormat(translateColorCodes(format));

        plugin.getDiscordWebhookManager().sendChatMessage(
                player.getName(),
                player.getUniqueId().toString(),
                event.getMessage());
    }

    private String translateColorCodes(String message) {
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern
                .compile("(?:&|)?#([A-Fa-f0-9]{6})|(?:<|\\{)#([A-Fa-f0-9]{6})(?:>|\\})");
        java.util.regex.Matcher matcher = hexPattern.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getPlayerPrefix(Player player) {
        return LuckPermsUtils.getPrefix(player);
    }
}
