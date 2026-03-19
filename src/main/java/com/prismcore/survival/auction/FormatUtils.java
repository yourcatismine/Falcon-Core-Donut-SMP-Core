/*
 * Decompiled with CFR 0.152.
 */
package com.prismcore.survival.auction;

public class FormatUtils {
    public static String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = totalSeconds % 3600 / 60;
        int seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }
}
