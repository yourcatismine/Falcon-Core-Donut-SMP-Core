package com.h2ph.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class LuckPermsUtils {

    /**
     * Gets the primary group of a player.
     * 
     * @param player The player to check.
     * @return The primary group name, or "default" if not found.
     */
    public static String getPrimaryGroup(OfflinePlayer player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return "default";
        }
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                // Try to load user if not online
                user = lp.getUserManager().loadUser(player.getUniqueId()).join();
            }
            if (user != null) {
                return user.getPrimaryGroup();
            }
        } catch (Exception e) {
            // Log if needed, but return default for safety
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
            // Silent error
        }
        return "";
    }
}
