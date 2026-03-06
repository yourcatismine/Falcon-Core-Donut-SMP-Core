package com.h2ph.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class LuckPermsUtils {

    private static final String[] GROUP_HIERARCHY = {
            "owner", "co-owner", "manager", "dev", "sradmin", "admin",
            "srmod", "mod", "srhelper", "helper", "media", "donut+", "booster", "default"
    };

    /**
     * Gets the most significant group of a player based on a predefined hierarchy.
     * 
     * @param player The player to check.
     * @return The highest group name found, or "default" if not found.
     */
    public static String getPrimaryGroup(OfflinePlayer player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return "default";
        }
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                user = lp.getUserManager().loadUser(player.getUniqueId()).join();
            }
            if (user != null) {
                String primary = user.getPrimaryGroup();

                for (String group : GROUP_HIERARCHY) {
                    if (primary.equalsIgnoreCase(group)) {
                        return group;
                    }
                }

                for (String group : GROUP_HIERARCHY) {
                    if (user.getNodes().stream().anyMatch(node -> {
                        String key = node.getKey().toLowerCase();
                        return key.equals("group." + group.toLowerCase()) || key.equals(group.toLowerCase());
                    })) {
                        return group;
                    }
                }

                return primary;
            }
        } catch (Exception e) {
        }
        return "default";
    }

    /**
     * Gets the prefix of a player.
     * 
     * @param player The player to check.
     * @return The player's prefix, or an empty string if not found.
     */
    public static String getPrefix(OfflinePlayer player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return "";
        }
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                user = lp.getUserManager().loadUser(player.getUniqueId()).join();
            }
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : "";
            }
        } catch (Exception e) {
        }
        return "";
    }
}
