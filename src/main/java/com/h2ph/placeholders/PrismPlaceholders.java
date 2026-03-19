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
        return true;
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
    
        if (params.equalsIgnoreCase("shards")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getShards());
        }
    
        if (params.equalsIgnoreCase("shop_spent")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getShopSpent());
        }
    
        if (params.equalsIgnoreCase("balance")) {
            return com.prismcore.survival.utils.NumberUtils.format(data.getMoney());
        }
    
        if (params.equalsIgnoreCase("keyall")) {
            return plugin.getKeyAllManager().getTimeRemainingFormatted();
        }
    
        if (params.toLowerCase().startsWith("keys_")) {
            String keyName = params.substring(5);
            String normalizedKey = plugin.normalizeKeyName(keyName);
            return String.valueOf(data.getKeyCount(normalizedKey));
        }
    
        if (params.toLowerCase().endsWith("_key")) {
            String possibleKey = params.substring(0, params.length() - 4);
            if (plugin.getKeyAllManager().isValidKey(possibleKey)) {
                return String.valueOf(data.getKeyCount(possibleKey));
            }
        }
    
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS));
        }
    
        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.DEATHS));
        }
    
        if (params.equalsIgnoreCase("mobs_killed")) {
            return String.valueOf(player.getStatistic(org.bukkit.Statistic.MOB_KILLS));
        }
    
        if (params.equalsIgnoreCase("playtime")) {
            int ticks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long seconds = ticks / 20L;
            return formatPlaytime(seconds);
        }
    
        if (params.equalsIgnoreCase("blocks_break")) {
            return String.valueOf(com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksBroken(player));
        }
    
        if (params.equalsIgnoreCase("blocks_placed")) {
            return String.valueOf(com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksPlaced(player));
        }
    
        if (params.equalsIgnoreCase("shard_booster")) {
            long remaining = data.getShardBoosterRemainingSeconds();
            if (remaining <= 0) {
                return "0s";
            }
            return formatPlaytime(remaining);
        }
    
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
    
        if (params.toLowerCase().startsWith("balance_number_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return getLeaderboardPlayer("balance", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("shards_number_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return getLeaderboardPlayer("shards", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("kills_number_")) {
            try {
                int position = Integer.parseInt(params.substring(13));
                return getLeaderboardPlayer("kills", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("deaths_number_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return getLeaderboardPlayer("deaths", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("playtime_number_")) {
            try {
                int position = Integer.parseInt(params.substring(16));
                return getLeaderboardPlayer("playtime", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("sell_number_")) {
            try {
                int position = Integer.parseInt(params.substring(12));
                return getLeaderboardPlayer("sell", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("balance_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(18));
                return getLeaderboardValue("balance", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("shards_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(17));
                return getLeaderboardValue("shards", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("kills_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(16));
                return getLeaderboardValue("kills", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("deaths_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(17));
                return getLeaderboardValue("deaths", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("playtime_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(19));
                return getLeaderboardValue("playtime", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        if (params.toLowerCase().startsWith("sell_formatted_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return getLeaderboardValue("sell", position);
            } catch (NumberFormatException e) {
                return "None";
            }
        }
    
        return null;
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
    
    private String getLeaderboardPlayer(String type, int position) {
        if (position <= 0) {
            return "None";
        }
    
        try {
            java.util.List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> entries = null;
    
            switch (type.toLowerCase()) {
                case "balance":
                    entries = plugin.getPlayerDataManager().getTopMoney(position);
                    break;
                case "shards":
                    entries = plugin.getPlayerDataManager().getTopShards(position);
                    break;
                case "kills":
                    entries = plugin.getPlayerDataManager().getTopKills(position);
                    break;
                case "deaths":
                    entries = plugin.getPlayerDataManager().getTopDeaths(position);
                    break;
                case "playtime":
                    entries = plugin.getPlayerDataManager().getTopPlaytime(position);
                    break;
                case "sell":
                    entries = plugin.getPlayerDataManager().getTopSell(position);
                    break;
                default:
                    return "None";
            }
    
            if (entries != null && entries.size() >= position) {
                com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry = entries.get(position - 1);
                return resolveEntryName(entry);
            }
        } catch (Exception e) {
        }
    
        return "None";
    }
    
    private String getLeaderboardValue(String type, int position) {
        if (position <= 0) {
            return "None";
        }
    
        try {
            java.util.List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> entries = null;
    
            switch (type.toLowerCase()) {
                case "balance":
                    entries = plugin.getPlayerDataManager().getTopMoney(position);
                    break;
                case "shards":
                    entries = plugin.getPlayerDataManager().getTopShards(position);
                    break;
                case "kills":
                    entries = plugin.getPlayerDataManager().getTopKills(position);
                    break;
                case "deaths":
                    entries = plugin.getPlayerDataManager().getTopDeaths(position);
                    break;
                case "playtime":
                    entries = plugin.getPlayerDataManager().getTopPlaytime(position);
                    break;
                case "sell":
                    entries = plugin.getPlayerDataManager().getTopSell(position);
                    break;
                default:
                    return "None";
            }
    
            if (entries != null && entries.size() >= position) {
                com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry = entries.get(position - 1);
                return formatLeaderboardValue(type, entry.value);
            }
        } catch (Exception e) {
        }
    
        return "None";
    }
    
    private String formatLeaderboardValue(String type, double value) {
        switch (type.toLowerCase()) {
            case "balance":
                return com.prismcore.survival.utils.NumberUtils.formatMoney(value);
            case "shards":
                return com.prismcore.survival.utils.NumberUtils.format(value);
            case "kills":
                return com.prismcore.survival.utils.NumberUtils.format((int) value);
            case "deaths":
                return com.prismcore.survival.utils.NumberUtils.format((int) value);
            case "playtime":
                return formatPlaytime((long) value);
            case "sell":
                return com.prismcore.survival.utils.NumberUtils.formatMoney(value);
            default:
                return String.valueOf(value);
        }
    }
    
    private String resolveEntryName(com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry) {
        if (entry == null) return "None";
        if (entry.name != null && !entry.name.matches("^[0-9a-fA-F\\-]{36}$")) return entry.name;
        try {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(entry.uuid);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Exception ignored) {
        }
        if (entry.uuid != null) {
            String u = entry.uuid.toString();
            return u.length() > 8 ? u.substring(0, 8) : u;
        }
        return "None";
    }
}