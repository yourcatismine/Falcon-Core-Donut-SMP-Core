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

    public ChatFormatter(PrismSurvival plugin) {
        // Plugin instance is currently unused but kept for constructor compatibility
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = getPlayerPrefix(player);

        String format;
        if (prefix == null || prefix.isEmpty()) {
            format = "&7%player_name%:&f %message%";
        } else if (getPlayerGroup(player).equalsIgnoreCase("default")) {
            format = "&7%player_name%:&f %message%";
        } else {
            format = "%prefix%&7%player_name%:&f %message%";
        }

        format = format.replace("%prefix%", prefix)
                .replace("%player_name%", player.getName())
                .replace("%message%", "%2$s"); // %2$s is the placeholder for the message in Bukkit's setFormat

        event.setFormat(translateColorCodes(format));
    }

    private String translateColorCodes(String message) {
        // 1. Translate Hex colors (&#RRGGBB, #RRGGBB, <#RRGGBB>, {#RRGGBB})
        // Matches &#RRGGBB, #RRGGBB, <#RRGGBB>, {#RRGGBB}
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

        // 2. Translate legacy colors (&7, &f, etc)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getPlayerGroup(Player player) {
        return LuckPermsUtils.getPrimaryGroup(player);
    }

    private String getPlayerPrefix(Player player) {
        return LuckPermsUtils.getPrefix(player);
    }
}
