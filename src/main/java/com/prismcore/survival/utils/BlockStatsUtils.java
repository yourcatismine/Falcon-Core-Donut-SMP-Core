package com.prismcore.survival.utils;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

public class BlockStatsUtils {

    public static int getTotalBlocksBroken(OfflinePlayer player) {
        int total = 0;
        for (Material m : Material.values()) {
            if (m.isBlock()) {
                try {
                    total += player.getStatistic(Statistic.MINE_BLOCK, m);
                } catch (IllegalArgumentException e) {
                    // Statistic not tracked for this material or invalid
                }
            }
        }
        return total;
    }

    public static int getTotalBlocksPlaced(OfflinePlayer player) {
        int total = 0;
        for (Material m : Material.values()) {
            if (m.isBlock()) {
                try {
                    total += player.getStatistic(Statistic.USE_ITEM, m);
                } catch (IllegalArgumentException e) {
                    // Statistic not tracked for this material
                }
            }
        }
        return total;
    }
}
