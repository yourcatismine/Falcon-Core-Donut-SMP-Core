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
package com.falconcore.survival.sell.utils;

import java.text.DecimalFormat;
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
    private static final DecimalFormat DF = new DecimalFormat("#.##");

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

    public static String formatMoney(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "T");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "B");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "M");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "K");
        } else {
            return DF.format(Math.floor(number * 100) / 100.0);
        }
    }

    private static String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        if (scaled == (long) scaled) {
            return String.valueOf((long) scaled) + suffix;
        }
        return DF.format(scaled) + suffix;
    }
}
