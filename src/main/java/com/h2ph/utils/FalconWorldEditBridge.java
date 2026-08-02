package com.h2ph.utils;

import com.h2ph.Falcon;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;

public class FalconWorldEditBridge {
    public static void createVoidRegion(Falcon plugin, Player player, String name) {
        try {
            com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            Region worldEditRegion = session.getSelection(worldEditPlayer.getWorld());

            if (worldEditRegion == null) {
                player.sendMessage("§cPlease make a selection with WorldEdit first.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            BlockVector3 min = worldEditRegion.getMinimumPoint();
            BlockVector3 max = worldEditRegion.getMaximumPoint();
            String worldName = player.getWorld().getName();

            plugin.getVoidManager().addRegion(name, worldName, min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
            player.sendMessage("§aVoid protection region '" + name + "' has been created!");

        } catch (IncompleteRegionException e) {
            player.sendMessage("§cPlease make a complete selection (pos1 and pos2) first.");
        } catch (Exception e) {
            player.sendMessage("§cError accessing WorldEdit selection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void createPvPSafeZone(Falcon plugin, Player player, String name) {
        try {
            com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            Region worldEditRegion = session.getSelection(worldEditPlayer.getWorld());

            if (worldEditRegion == null) {
                player.sendMessage("§cPlease make a selection with WorldEdit first.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            BlockVector3 min = worldEditRegion.getMinimumPoint();
            BlockVector3 max = worldEditRegion.getMaximumPoint();
            String worldName = player.getWorld().getName();

            boolean success = plugin.getPvPSafeZoneManager().addZone(name, worldName, min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ(), player.getUniqueId().toString());

            if (success) {
                player.sendMessage("§aPvP safe zone '" + name + "' has been created!");
                player.sendMessage("§7Players entering this zone will see safe mode messages.");
            } else {
                player.sendMessage("§cFailed to create PvP safe zone. A zone with that name may already exist.");
            }

        } catch (IncompleteRegionException e) {
            player.sendMessage("§cPlease make a complete selection (pos1 and pos2) first.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } catch (Exception e) {
            player.sendMessage("§cError accessing WorldEdit selection: " + e.getMessage());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            e.printStackTrace();
        }
    }
}
