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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HistoryListener implements Listener {

    private final PrismSurvival plugin;
    
    // Batch processing for high-frequency block actions to prevent thread exhaustion
    private final ConcurrentLinkedQueue<BlockActionRecord> pendingActions = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processingBatch = new AtomicBoolean(false);
    private static final int BATCH_SIZE = 50; // Process up to 50 actions at once
    private static final int BATCH_DELAY_TICKS = 5; // Process batches every 5 ticks (0.25s)

    public HistoryListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }
    
    // Record holder for batched block actions
    private static class BlockActionRecord {
        final Location location;
        final String playerName; 
        final UUID playerUUID;
        final String action;
        final String blockType;
        final long timestamp;
        
        BlockActionRecord(Location location, String playerName, UUID playerUUID, String action, String blockType, long timestamp) {
            this.location = location.clone(); // Clone to prevent world unload issues
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.action = action;
            this.blockType = blockType;
            this.timestamp = timestamp;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        recordBlockAction(player, block, "Destroy");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        recordBlockAction(player, block, "Placed");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (player.hasPermission("prism.admin.checkhistory")) {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null && data.isCheckHistory()) {
                event.setCancelled(true);
                showBlockHistory(player, block);
                return;
            }
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            Material itemType = event.getItem().getType();
            
            if (itemType == Material.WATER_BUCKET || itemType == Material.LAVA_BUCKET || 
                itemType == Material.POWDER_SNOW_BUCKET || itemType.toString().contains("BUCKET") ||
                itemType.toString().contains("_SEEDS") || itemType == Material.BONE_MEAL ||
                itemType.toString().endsWith("_SPAWN_EGG")) {
                final Location location = block.getLocation().clone();
                final String blockType = block.getType().toString();
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    recordBlockAction(player, location, blockType, "Replaced");
                }, 1L);
            }
        }
    }

    private void recordBlockAction(Player player, Location location, String blockType, String action) {
        final String playerName = player.getName();
        final UUID playerUUID = player.getUniqueId();
        final long timestamp = System.currentTimeMillis();
        
        // Add to batch queue instead of creating immediate async task
        pendingActions.offer(new BlockActionRecord(location, playerName, playerUUID, action, blockType, timestamp));
        
        // Schedule batch processing if not already running
        if (processingBatch.compareAndSet(false, true)) {
            plugin.getSchedulerAdapter().runTaskLater(this::processBatch, BATCH_DELAY_TICKS);
        }
    }
    
    private void processBatch() {
        if (pendingActions.isEmpty()) {
            processingBatch.set(false);
            return;
        }
        
        // Collect batch of actions (up to BATCH_SIZE)
        List<BlockActionRecord> batch = new ArrayList<>();
        for (int i = 0; i < BATCH_SIZE && !pendingActions.isEmpty(); i++) {
            BlockActionRecord action = pendingActions.poll();
            if (action != null) {
                batch.add(action);
            }
        }
        
        if (!batch.isEmpty()) {
            // Process the batch in a single async task instead of individual tasks
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                for (BlockActionRecord record : batch) {
                    try {
                        plugin.getDatabaseManager().recordBlockAction(
                            record.location,
                            record.playerName,
                            record.playerUUID,
                            record.action,
                            record.blockType,
                            record.timestamp
                        );
                    } catch (Exception e) {
                        // Log error but continue processing other records
                        plugin.getLogger().warning("Failed to record block action: " + e.getMessage());
                    }
                }
            });
        }
        
        // Schedule next batch if more actions are pending
        if (!pendingActions.isEmpty()) {
            plugin.getSchedulerAdapter().runTaskLater(this::processBatch, BATCH_DELAY_TICKS);
        } else {
            processingBatch.set(false);
        }
    }

    private void recordBlockAction(Player player, Block block, String action) {
        final Location location = block.getLocation().clone();
        final String blockType = block.getType().toString();
        
        recordBlockAction(player, location, blockType, action);
    }
    
    private void showBlockHistory(Player player, Block block) {
        final Location location = block.getLocation().clone();
        final String currentBlockType = block.getType().toString();
        
        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            List<BlockHistoryEntry> history = plugin.getDatabaseManager().getBlockHistory(location);
            
            plugin.getSchedulerAdapter().runTask(() -> {
                if (history.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String timestamp = sdf.format(new Date());
                    
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Block History: " + ChatColor.WHITE + "Vanilla");
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Action: " + ChatColor.WHITE + "Natural/Generated");
                    player.sendMessage(ChatColor.RED + " Block: " + ChatColor.WHITE + currentBlockType);
                    player.sendMessage(ChatColor.RED + " World: " + ChatColor.WHITE + location.getWorld().getName());
                    player.sendMessage(ChatColor.RED + " Date: " + ChatColor.WHITE + "Unknown");
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                } else {
                    BlockHistoryEntry entry = history.get(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String timestamp = sdf.format(new Date(entry.timestamp));
                    
                    player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    player.sendMessage(ChatColor.RED + " Block History: " + ChatColor.WHITE + entry.playerName);
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + " Action: " + ChatColor.WHITE + entry.action);
                    player.sendMessage(ChatColor.RED + " Block: " + ChatColor.WHITE + entry.blockType);
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