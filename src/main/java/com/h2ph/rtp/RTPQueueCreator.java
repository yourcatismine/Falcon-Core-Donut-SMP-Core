package com.h2ph.rtp;

import com.h2ph.Falcon;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class RTPQueueCreator {

    public static void createQueue(Falcon plugin, Player player, String regionName, RTPQueueManager manager) {
        File rtpFolder = new File(plugin.getDataFolder(), "rtp");
        File regionFolder = new File(rtpFolder, regionName);

        if (!regionFolder.exists() || !regionFolder.isDirectory()) {
            player.sendMessage(ChatColor.RED + "RTP Region '" + regionName + "' does not exist.");
            return;
        }

        try {
            com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            Region region = session.getSelection(worldEditPlayer.getWorld());

            if (region == null) {
                player.sendMessage(ChatColor.RED + "Please make a selection with WorldEdit first.");
                return;
            }

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            String worldName = player.getWorld().getName();

            File queueFolder = new File(rtpFolder, "queue");
            if (!queueFolder.exists()) {
                queueFolder.mkdirs();
            }

            File queueFile = new File(queueFolder, regionName + ".yml");

            if (!queueFile.exists()) {
                queueFile.createNewFile();
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(queueFile);

            config.set("world", worldName);
            config.set("pos1.x", min.getX());
            config.set("pos1.y", min.getY());
            config.set("pos1.z", min.getZ());
            config.set("pos2.x", max.getX());
            config.set("pos2.y", max.getY());
            config.set("pos2.z", max.getZ());

            config.save(queueFile);
            manager.loadQueues();

            player.sendMessage(ChatColor.GREEN + "RTP Queue for region " + ChatColor.YELLOW + regionName +
                    ChatColor.GREEN + " created successfully!");

        } catch (IncompleteRegionException e) {
            player.sendMessage(ChatColor.RED + "Please make a complete selection (pos1 and pos2) first.");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to save queue configuration: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error accessing WorldEdit selection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
