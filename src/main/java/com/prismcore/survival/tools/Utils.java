package com.prismcore.survival.tools;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;

public final class Utils {
    private static final Pattern HEX_PATTERN_AMP = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_HASH = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final java.text.DecimalFormat NUMBER_FORMAT = new java.text.DecimalFormat("#,##0.00");

    private Utils() {
    }

    public static String formatNumber(double number) {
        return NUMBER_FORMAT.format(number);
    }

    public static String formatColors(String input) {
        if (input == null) {
            return null;
        }

        Matcher m = HEX_PATTERN_AMP.matcher(input);
        StringBuffer buf = new StringBuffer(input.length() + 32);
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                rep.append('\u00a7').append(c);
            }
            m.appendReplacement(buf, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(buf);
        input = buf.toString();

        m = HEX_PATTERN_HASH.matcher(input);
        buf = new StringBuffer(input.length() + 32);
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                rep.append('\u00a7').append(c);
            }
            m.appendReplacement(buf, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(buf);
        input = buf.toString();

        return ChatColor.translateAlternateColorCodes((char) '&', input);
    }

    public static List<String> formatColors(List<String> lines) {
        return lines.stream().map(Utils::formatColors).collect(Collectors.toList());
    }

    public static String formatDuration(long remSec) {
        if (remSec < 0)
            remSec = 0;
        long days = remSec / 86400L;
        long hrs = (remSec % 86400L) / 3600L;
        long mins = (remSec % 3600L) / 60L;
        long secs = remSec % 60L;
        return String.format("%dd %dh %dm %ds", days, hrs, mins, secs);
    }

    public static org.bukkit.block.BlockFace getBlockFace(org.bukkit.entity.Player player) {
        org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(5.0);
        if (result == null || result.getHitBlock() == null)
            return org.bukkit.block.BlockFace.SELF;
        return result.getHitBlockFace();
    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty())
            return -1;

        long totalSeconds = 0;
        Matcher m = Pattern.compile("(\\d+)([wdhmsy])").matcher(input.toLowerCase());

        while (m.find()) {
            int val = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            switch (unit) {
                case "y":
                    totalSeconds += val * 31536000L;
                    break;
                case "w":
                    totalSeconds += val * 604800L;
                    break;
                case "d":
                    totalSeconds += val * 86400L;
                    break;
                case "h":
                    totalSeconds += val * 3600L;
                    break;
                case "m":
                    totalSeconds += val * 60L;
                    break;
                case "s":
                    totalSeconds += val;
                    break;
            }
        }

        return totalSeconds > 0 ? totalSeconds : -1;
    }
}
