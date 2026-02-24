package com.h2ph.placeholders;

import com.prismcore.survival.manager.PlayerData;
import com.h2ph.PrismSurvival;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PrismPlaceholders extends PlaceholderExpansion {

    private final PrismSurvival plugin;

    public PrismPlaceholders(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "prismcore";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "h2ph";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This expansion should persist through reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) {
            return "0";
        }

        // %prismcore_shards% - Shard count with k/m formatting
        if (params.equalsIgnoreCase("shards")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getShards());
        }

        // %prismcore_shop_spent% - Total money spent in shop
        if (params.equalsIgnoreCase("shop_spent")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getShopSpent());
        }

        // %prismcore_balance% - Player money balance
        if (params.equalsIgnoreCase("balance")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getMoney());
        }

        // %prismcore_keyall% - Countdown timer
        if (params.equalsIgnoreCase("keyall")) {
            return plugin.getKeyAllManager().getTimeRemainingFormatted();
        }

        // %prismcore_keys_<keyname>% - Get specific key count
        if (params.toLowerCase().startsWith("keys_")) {
            String keyName = params.substring(5); // Remove "keys_" prefix
            String normalizedKey = plugin.normalizeKeyName(keyName);
            return String.valueOf(data.getKeyCount(normalizedKey));
        }

        // %prismcore_<keyname>_key% - Get specific key count (Dynamic from config)
        if (params.toLowerCase().endsWith("_key")) {
            String possibleKey = params.substring(0, params.length() - 4); // Remove "_key"
            if (plugin.getKeyAllManager().isValidKey(possibleKey)) {
                return String.valueOf(data.getKeyCount(possibleKey));
            }
        }

        // %prismcore_kills%
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS));
        }

        // %prismcore_deaths%
        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.DEATHS));
        }

        // %prismcore_mobs_killed%
        if (params.equalsIgnoreCase("mobs_killed")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.MOB_KILLS));
        }

        // %prismcore_playtime%
        if (params.equalsIgnoreCase("playtime")) {
            // PLAY_ONE_MINUTE is actually in ticks (legacy naming)
            int ticks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long seconds = ticks / 20L;
            return formatPlaytime(seconds);
        }

        // %prismcore_blocks_break%
        if (params.equalsIgnoreCase("blocks_break")) {
            return String.valueOf(com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksBroken(player));
        }

        // %prismcore_blocks_placed%
        if (params.equalsIgnoreCase("blocks_placed")) {
            return String.valueOf(com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksPlaced(player));
        }

        // %prismcore_shard_booster%
        if (params.equalsIgnoreCase("shard_booster")) {
            long remaining = data.getShardBoosterRemainingSeconds();
            if (remaining <= 0) {
                return "0s";
            }
            return formatPlaytime(remaining);
        }

        // %prismcore_sell_made%
        if (params.equalsIgnoreCase("sell_made")) {
            if (plugin.getPrismSell() != null && plugin.getPrismSell().getPlayerDataManager() != null) {
                com.prismcore.survival.sell.data.PlayerData sellPd = plugin.getPrismSell().getPlayerDataManager()
                        .getPlayerData(player.getUniqueId());
                if (sellPd != null) {
                    return com.prismcore.survival.utils.NumberUtils.format(sellPd.getSellMade());
                }
            }
            return "0";
        }

        return null; // Placeholder is unknown
    }

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long rem = totalSeconds % 86400;
        long hours = rem / 3600;
        rem = rem % 3600;
        long minutes = rem / 60;
        long seconds = rem % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
