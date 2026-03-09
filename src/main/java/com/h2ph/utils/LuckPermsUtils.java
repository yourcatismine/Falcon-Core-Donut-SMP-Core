package com.h2ph.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.List;

public class LuckPermsUtils {

    /**
     * Gets the primary group of a player as set in LuckPerms.
     *
     * @param player The player to check.
     * @return The player's primary group, or "default" if not found.
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
                return primary != null ? primary : "default";
            }
        } catch (Exception e) {
        }
        return "default";
    }

    /**
     * Gets all groups a player belongs to.
     *
     * @param player The player to check.
     * @return A list of group names the player is in.
     */
    public static List<String> getGroups(OfflinePlayer player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return java.util.Collections.singletonList("default");
        }
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                user = lp.getUserManager().loadUser(player.getUniqueId()).join();
            }
            if (user != null) {
                List<String> groups = new java.util.ArrayList<>();
                user.getNodes().stream()
                        .filter(node -> node.getKey().startsWith("group."))
                        .forEach(node -> groups.add(node.getKey().substring(6)));
                return groups;
            }
        } catch (Exception e) {
        }
        return java.util.Collections.singletonList("default");
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
