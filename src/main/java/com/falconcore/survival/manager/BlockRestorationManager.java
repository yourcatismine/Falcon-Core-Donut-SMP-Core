package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BlockRestorationManager implements Listener {

    private final Falcon plugin;
    private final DatabaseManager databaseManager;
    private final PvPSafeZoneManager pvpSafeZoneManager;
    
    private static final long RESTORATION_TIME = 30 * 1000L;

    public BlockRestorationManager(Falcon plugin, DatabaseManager databaseManager, PvPSafeZoneManager pvpSafeZoneManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.pvpSafeZoneManager = pvpSafeZoneManager;
        
        startRestorationScheduler();
        
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            restoreExpiredBlocks();
            plugin.getLogger().info("Restored any blocks that expired during server downtime");
        }, 100L);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        Block replacedBlock = event.getBlockReplacedState().getBlock();
        Material originalMaterial = event.getBlockReplacedState().getType();
        BlockData originalBlockData = event.getBlockReplacedState().getBlockData();

        if (!pvpSafeZoneManager.hasZonesInWorld(location.getWorld().getName())) {
            return;
        }

        boolean inSafeZone = pvpSafeZoneManager.isInSafeZone(location);
        
        if (!inSafeZone) {
            trackBlockForRestoration(location, originalMaterial, originalBlockData);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        Material brokenMaterial = block.getType();
        BlockData brokenBlockData = block.getBlockData();
        
        if (brokenMaterial == Material.AIR) {
            return;
        }

        if (!pvpSafeZoneManager.hasZonesInWorld(location.getWorld().getName())) {
            return;
        }

        boolean inSafeZone = pvpSafeZoneManager.isInSafeZone(location);
        
        if (!inSafeZone) {
            trackBlockForRestoration(location, brokenMaterial, brokenBlockData);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location location = block.getLocation();
            Material blockMaterial = block.getType();
            BlockData blockData = block.getBlockData();
            
            if (blockMaterial == Material.AIR) {
                continue;
            }

            if (!pvpSafeZoneManager.hasZonesInWorld(location.getWorld().getName())) {
                continue;
            }
            
            boolean inSafeZone = pvpSafeZoneManager.isInSafeZone(location);
            
            if (!inSafeZone) {
                trackBlockForRestoration(location, blockMaterial, blockData);
            }
        }
    }

    private void trackBlockForRestoration(Location location, Material originalMaterial, BlockData originalBlockData) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String checkSQL = "SELECT id FROM temporary_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                    checkStmt.setString(1, location.getWorld().getName());
                    checkStmt.setInt(2, location.getBlockX());
                    checkStmt.setInt(3, location.getBlockY());
                    checkStmt.setInt(4, location.getBlockZ());
                    
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        plugin.getLogger().fine("Location already tracked for restoration, keeping original state: " + location.toString());
                        return;
                    }
                }
                
                String originalDataString = originalBlockData.getAsString();
                
                String insertSQL = "INSERT INTO temporary_blocks (world, x, y, z, original_material, original_data, placed_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                    stmt.setString(1, location.getWorld().getName());
                    stmt.setInt(2, location.getBlockX());
                    stmt.setInt(3, location.getBlockY());
                    stmt.setInt(4, location.getBlockZ());
                    stmt.setString(5, originalMaterial.toString());
                    stmt.setString(6, originalDataString);
                    stmt.setLong(7, System.currentTimeMillis());
                    
                    stmt.executeUpdate();
                    plugin.getLogger().fine("Started tracking location for restoration: " + location.toString() + " -> " + originalMaterial.toString());
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error tracking block for restoration: " + e.getMessage());
            }
        });
    }

    private void startRestorationScheduler() {
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            restoreExpiredBlocks();
        }, 600L, 600L);
    }

    private void restoreExpiredBlocks() {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            long currentTime = System.currentTimeMillis();
            long expirationTime = currentTime - RESTORATION_TIME;

            try (Connection conn = databaseManager.getConnection()) {
                String selectSQL = "SELECT id, world, x, y, z, original_material, original_data, placed_time FROM temporary_blocks WHERE placed_time <= ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                    selectStmt.setLong(1, expirationTime);
                    
                    ResultSet rs = selectStmt.executeQuery();
                    
                    Map<String, List<BlockRestoration>> blocksByWorld = new HashMap<>();
                    List<Integer> idsToDelete = new ArrayList<>();
                    
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String worldName = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        String originalMaterial = rs.getString("original_material");
                        String originalData = rs.getString("original_data");
                        long placedTime = rs.getLong("placed_time");
                        
                        blocksByWorld.computeIfAbsent(worldName, k -> new ArrayList<>()).add(
                            new BlockRestoration(worldName, x, y, z, originalMaterial, originalData, placedTime)
                        );
                        
                        idsToDelete.add(id);
                    }
                    
                    if (!blocksByWorld.isEmpty()) {
                        plugin.getSchedulerAdapter().runTask(() -> {
                            for (Map.Entry<String, List<BlockRestoration>> entry : blocksByWorld.entrySet()) {
                                String worldName = entry.getKey();
                                List<BlockRestoration> blocks = entry.getValue();
                                if (!pvpSafeZoneManager.hasZonesInWorld(worldName)) {
                                    plugin.getLogger().fine("Skipping restoration for world '" + worldName + "' — no PvP safe zones configured.");
                                    continue;
                                }
                                restoreBlocksBatch(worldName, blocks);
                            }
                        });
                    }
                    
                    if (!idsToDelete.isEmpty()) {
                        String deleteSQL = "DELETE FROM temporary_blocks WHERE id IN (" + 
                            String.join(",", Collections.nCopies(idsToDelete.size(), "?")) + ")";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {
                            for (int i = 0; i < idsToDelete.size(); i++) {
                                deleteStmt.setInt(i + 1, idsToDelete.get(i));
                            }
                            int deleted = deleteStmt.executeUpdate();
                            if (deleted > 0) {
                                plugin.getLogger().info("Restored " + deleted + " blocks to their original state");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error restoring expired blocks: " + e.getMessage());
            }
        });
    }

    private void restoreBlock(String worldName, int x, int y, int z, String materialName, String blockDataString) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found for block restoration");
            return;
        }

        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();
        
        try {
            Material material = Material.valueOf(materialName);
            
            if (material == Material.AIR) {
                block.setType(Material.AIR);
                return;
            }
            
            block.setType(material);
            
            if (blockDataString != null && !blockDataString.isEmpty() 
                && !blockDataString.equals("minecraft:air")) {
                try {
                    BlockData blockData = Bukkit.createBlockData(blockDataString);
                    if (blockData.getMaterial() == material) {
                        block.setBlockData(blockData);
                    } else {
                        plugin.getLogger().fine("Block data material mismatch for " + materialName + ", using material only");
                    }
                } catch (IllegalArgumentException e) {
                    try {
                        BlockData defaultData = Bukkit.createBlockData(material);
                        block.setBlockData(defaultData);
                    } catch (IllegalArgumentException e2) {
                        plugin.getLogger().fine("Could not create block data for: " + materialName);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for block restoration: " + materialName);
            block.setType(Material.AIR);
        }
    }

    /**
     * Clean up all temporary blocks for a specific world
     */
    public void cleanupWorld(String worldName) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String deleteSQL = "DELETE FROM temporary_blocks WHERE world = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
                    stmt.setString(1, worldName);
                    int deleted = stmt.executeUpdate();
                    if (deleted > 0) {
                        plugin.getLogger().info("Cleaned up " + deleted + " temporary blocks for world: " + worldName);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error cleaning up temporary blocks for world " + worldName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Restore multiple blocks at once for better performance (like /fill command)
     */
    private void restoreBlocksBatch(String worldName, List<BlockRestoration> blocks) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found for batch block restoration");
            return;
        }

        for (BlockRestoration blockData : blocks) {
            Location location = new Location(world, blockData.x, blockData.y, blockData.z);
            
            plugin.getSchedulerAdapter().runAtLocation(location, () -> {
                Block block = location.getBlock();
                
                try {
                    Material material = Material.valueOf(blockData.originalMaterial);
                    
                    if (material == Material.AIR) {
                        block.setType(Material.AIR);
                        return;
                    }
                    
                    block.setType(material);
                    
                    if (blockData.originalData != null && !blockData.originalData.isEmpty() 
                        && !blockData.originalData.equals("minecraft:air")) {
                        try {
                            BlockData blockDataObj = Bukkit.createBlockData(blockData.originalData);
                            if (blockDataObj.getMaterial() == material) {
                                block.setBlockData(blockDataObj);
                            }
                        } catch (IllegalArgumentException e) {
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for restoration: " + blockData.originalMaterial);
                    block.setType(Material.AIR);
                }
            });
        }
    }

    /**
     * Helper class to store block restoration data
     */
    private static class BlockRestoration {
        final String worldName;
        final int x, y, z;
        final String originalMaterial;
        final String originalData;
        final long placedTime;

        BlockRestoration(String worldName, int x, int y, int z, String originalMaterial, String originalData, long placedTime) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.originalMaterial = originalMaterial;
            this.originalData = originalData;
            this.placedTime = placedTime;
        }
    }
}