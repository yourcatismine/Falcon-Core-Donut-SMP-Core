/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 */
package com.prismcore.survival.sell.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        message = MessageUtil.translateHexColors(message);
        return ChatColor.translateAlternateColorCodes((char) '&', (String) message);
    }

    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of((String) ("#" + group)).toString());
        }
        return matcher.appendTail(buffer).toString();
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtil.colorize(message));
    }

    public static void sendActionBar(Player player, String message) {
        String colorized = MessageUtil.colorize(message);
        TextComponent component = new TextComponent(TextComponent.fromLegacyText((String) colorized));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent) component);
    }

    public static List<String> colorizeList(List<String> list) {
        ArrayList<String> colorized = new ArrayList<String>();
        for (String line : list) {
            colorized.add(MessageUtil.colorize(line));
        }
        return colorized;
    }

    public static String formatMoney(double amount) {
        if (amount >= 1.0E9) {
            return String.format("%.0fB", amount / 1.0E9);
        }
        if (amount >= 1000000.0) {
            return String.format("%.0fM", amount / 1000000.0);
        }
        if (amount >= 1000.0) {
            return String.format("%.0fK", amount / 1000.0);
        }
        if (amount < 1.0) {
            return String.format("%.2f", amount);
        }
        return String.format("%.0f", amount);
    }
}
