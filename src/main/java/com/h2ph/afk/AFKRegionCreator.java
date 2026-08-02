package com.h2ph.afk;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AFKRegionCreator {
    public static void createAFKRegion(Player player, String regionName, AFKManager afkManager) {
        try {
            com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            Region region = session.getSelection(worldEditPlayer.getWorld());

            if (region == null) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            String worldName = player.getWorld().getName();

            Vector minVec = new Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ());
            Vector maxVec = new Vector(max.getBlockX(), max.getBlockY(), max.getBlockZ());

            afkManager.createRegion(regionName, worldName, minVec, maxVec);
            player.sendMessage(ChatColor.GREEN + "Created AFK region " + ChatColor.YELLOW + regionName +
                    ChatColor.GREEN + " in world " + ChatColor.AQUA + worldName);

        } catch (IncompleteRegionException e) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error accessing WorldEdit selection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
