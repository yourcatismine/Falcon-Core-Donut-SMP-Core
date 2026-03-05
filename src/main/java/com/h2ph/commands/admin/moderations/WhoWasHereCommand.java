package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WhoWasHereCommand implements CommandExecutor {

    private final PrismSurvival plugin;

    public WhoWasHereCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prism.admin.whowashere")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Get the player's current chunk
        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Async Processing
        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            List<ChunkHistoryEntry> history = plugin.getDatabaseManager().getChunkHistory(world, chunkX, chunkZ);

            plugin.getSchedulerAdapter().runTask(() -> {
                if (history.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Chunk History: " + ChatColor.WHITE + "No Activity Found");
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Chunk: " + ChatColor.WHITE + chunkX + ", " + chunkZ);
                    player.sendMessage(ChatColor.RED + " World: " + ChatColor.WHITE + world);
                    player.sendMessage(ChatColor.RED + " Status: " + ChatColor.WHITE + "Unexplored or No Block Activity");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Who Was Here: " + ChatColor.WHITE + "Chunk " + chunkX + ", " + chunkZ);
                    player.sendMessage("");

                    // Display top players who were here (up to 10)
                    int rank = 1;
                    int maxDisplay = Math.min(history.size(), 10);
                    
                    for (int i = 0; i < maxDisplay; i++) {
                        ChunkHistoryEntry entry = history.get(i);
                        String timestamp = sdf.format(new Date(entry.lastActivity));
                        
                        player.sendMessage(ChatColor.RED + " " + rank + ". " + ChatColor.WHITE + entry.playerName + 
                                          ChatColor.GRAY + " (" + entry.actionCount + " actions)");
                        player.sendMessage(ChatColor.GRAY + "    Last seen: " + timestamp);
                        
                        rank++;
                    }

                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Total Players: " + ChatColor.WHITE + history.size());
                    player.sendMessage(ChatColor.RED + " World: " + ChatColor.WHITE + world);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                }
            });
        });

        return true;
    }

    public static class ChunkHistoryEntry {
        public final String playerName;
        public final int actionCount;
        public final long lastActivity;

        public ChunkHistoryEntry(String playerName, int actionCount, long lastActivity) {
            this.playerName = playerName;
            this.actionCount = actionCount;
            this.lastActivity = lastActivity;
        }
    }
}