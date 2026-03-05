package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class HistoryListener implements Listener {

    private final PrismSurvival plugin;

    public HistoryListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Record the block action in history
        recordBlockAction(player, block, "Destroy");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Record the block action in history
        recordBlockAction(player, block, "Placed");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        // Check if player has history checking enabled
        if (player.hasPermission("prism.admin.checkhistory")) {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null && data.isCheckHistory()) {
                // Cancel the interaction and show block history
                event.setCancelled(true);
                showBlockHistory(player, block);
                return;
            }
        }
        
        // Normal interaction - record potential replacements
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            Material itemType = event.getItem().getType();
            
            // Check for block replacement actions
            if (itemType == Material.WATER_BUCKET || itemType == Material.LAVA_BUCKET || 
                itemType == Material.POWDER_SNOW_BUCKET || itemType.toString().contains("BUCKET") ||
                itemType.toString().contains("_SEEDS") || itemType == Material.BONE_MEAL ||
                itemType.toString().endsWith("_SPAWN_EGG")) {
                // We'll record this after the event in a delayed task
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    recordBlockAction(player, block, "Replaced");
                }, 1L);
            }
        }
    }

    private void recordBlockAction(Player player, Block block, String action) {
        // Get all block data on main thread before going async to avoid Folia issues
        final Location location = block.getLocation().clone();
        final String blockType = block.getType().toString();
        final String playerName = player.getName();
        final UUID playerUUID = player.getUniqueId();
        final long timestamp = System.currentTimeMillis();
        
        // Store the block action in our history system
        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            plugin.getDatabaseManager().recordBlockAction(
                location, 
                playerName, 
                playerUUID, 
                action, 
                blockType,
                timestamp
            );
        });
    }
    
    private void showBlockHistory(Player player, Block block) {
        // Get block data on main thread before going async  
        final Location location = block.getLocation().clone();
        final String currentBlockType = block.getType().toString();
        
        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            List<BlockHistoryEntry> history = plugin.getDatabaseManager().getBlockHistory(location);
            
            plugin.getSchedulerAdapter().runTask(() -> {
                if (history.isEmpty()) {
                    // No history found - block is vanilla/natural
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String timestamp = sdf.format(new Date());
                    String locationStr = String.format("%d, %d, %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Block History: " + ChatColor.WHITE + "Vanilla");
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Action: " + ChatColor.WHITE + "Natural/Generated");
                    player.sendMessage(ChatColor.RED + " Block: " + ChatColor.WHITE + currentBlockType);
                    player.sendMessage(ChatColor.RED + " Location: " + ChatColor.WHITE + locationStr);
                    player.sendMessage(ChatColor.RED + " World: " + ChatColor.WHITE + location.getWorld().getName());
                    player.sendMessage(ChatColor.RED + " Date: " + ChatColor.WHITE + "Unknown");
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                } else {
                    // Show the most recent history entry
                    BlockHistoryEntry entry = history.get(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String timestamp = sdf.format(new Date(entry.timestamp));
                    String locationStr = String.format("%d, %d, %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Block History: " + ChatColor.WHITE + entry.playerName);
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Action: " + ChatColor.WHITE + entry.action);
                    player.sendMessage(ChatColor.RED + " Block: " + ChatColor.WHITE + entry.blockType);
                    player.sendMessage(ChatColor.RED + " Location: " + ChatColor.WHITE + locationStr);
                    player.sendMessage(ChatColor.RED + " World: " + ChatColor.WHITE + location.getWorld().getName());
                    player.sendMessage(ChatColor.RED + " Date: " + ChatColor.WHITE + timestamp);
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                }
            });
        });
    }
    
    public static class BlockHistoryEntry {
        public final String playerName;
        public final String action;
        public final String blockType;
        public final long timestamp;
        
        public BlockHistoryEntry(String playerName, String action, String blockType, long timestamp) {
            this.playerName = playerName;
            this.action = action;
            this.blockType = blockType;
            this.timestamp = timestamp;
        }
    }
}