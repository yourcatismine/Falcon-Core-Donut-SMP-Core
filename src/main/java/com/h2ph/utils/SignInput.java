package com.h2ph.utils;

import com.h2ph.PrismSurvival; // Assuming PrismSurvival is the main class package
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SignInput implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Location> pendingSignLocations = new ConcurrentHashMap<>();
    private final Map<UUID, BlockData> pendingSignOriginalBlockData = new ConcurrentHashMap<>();
    private final Map<UUID, BlockData> pendingSupportOriginalBlockData = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pendingSupportLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<String>> pendingCallbacks = new ConcurrentHashMap<>();

    public SignInput(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a sign input for the player.
     * 
     * @param player  The player to get input from.
     * @param onInput The callback to run when input is received.
     */
    public void getSearchInput(Player player, Consumer<String> onInput) {
        Location loc = player.getLocation().getBlock().getLocation();

        // Avoid overwriting existing TileEntities (Chests, etc.)
        if (loc.getBlock().getState() instanceof org.bukkit.block.TileState) {
            loc.add(0, 1, 0);
        }

        // Store original block data for the sign location
        BlockData original = loc.getBlock().getBlockData();
        pendingSignLocations.put(player.getUniqueId(), loc);
        pendingSignOriginalBlockData.put(player.getUniqueId(), original);
        pendingCallbacks.put(player.getUniqueId(), onInput);

        // No longer placing temporary support block (Bedrock) to avoid terrain
        // modification
        /*
         * Location below = loc.clone().add(0, -1, 0);
         * if (!below.getBlock().getType().isSolid()) {
         * // Store original support block
         * BlockData originalSupport = below.getBlock().getBlockData();
         * pendingSupportOriginalBlockData.put(player.getUniqueId(), originalSupport);
         * pendingSupportLocations.put(player.getUniqueId(), below);
         * 
         * // Place temporary solid block (Bedrock is safest to avoid breaking)
         * below.getBlock().setType(Material.BEDROCK, false);
         * }
         */

        // Delay slightly to ensure client sync and avoid immediate physics updates
        // Use SchedulerAdapter via the plugin instance
        ((PrismSurvival) plugin).getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (!player.isOnline()) {
                restoreBlocks(player.getUniqueId());
                return;
            }

            // Place the sign
            loc.getBlock().setType(Material.OAK_SIGN, false);

            if (loc.getBlock().getState() instanceof Sign) {
                Sign sign = (Sign) loc.getBlock().getState();
                // Optionally set some text like "^ ^ ^" or "Search:"
                // but we leave it blank for clean input
                player.openSign(sign);
            }
        }, 2L);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();

        if (pendingSignLocations.containsKey(uuid)) {
            Location loc = pendingSignLocations.get(uuid);

            // Verify it's the correct sign
            if (event.getBlock().getLocation().equals(loc)) {
                event.setCancelled(true); // Don't actually change the text on the world

                // Get input
                StringBuilder mb = new StringBuilder();
                for (String line : event.getLines()) {
                    mb.append(line);
                }
                String input = mb.toString().trim();

                // Callback
                Consumer<String> callback = pendingCallbacks.remove(uuid);
                if (callback != null) {
                    // Run callback on next tick or immediately?
                    // Use sync task to be safe with Bukkit API calls in callback
                    // Use Entity Task for Folia compatibility (GUI operations usually require
                    // entity context)
                    ((PrismSurvival) plugin).getSchedulerAdapter().runEntityTask(p, () -> callback.accept(input));
                }

                // Restore blocks
                restoreBlocks(uuid);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (pendingSignLocations.containsValue(event.getBlock().getLocation()) ||
                pendingSupportLocations.containsValue(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> pendingSignLocations.containsValue(block.getLocation()) ||
                pendingSupportLocations.containsValue(block.getLocation()));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> pendingSignLocations.containsValue(block.getLocation()) ||
                pendingSupportLocations.containsValue(block.getLocation()));
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (org.bukkit.block.Block block : event.getBlocks()) {
            if (pendingSignLocations.containsValue(block.getLocation()) ||
                    pendingSupportLocations.containsValue(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (org.bukkit.block.Block block : event.getBlocks()) {
            if (pendingSignLocations.containsValue(block.getLocation()) ||
                    pendingSupportLocations.containsValue(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (pendingSignLocations.containsValue(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (pendingSignLocations.containsValue(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        restoreBlocks(event.getPlayer().getUniqueId());
    }

    private void restoreBlocks(UUID uuid) {
        // Restore Sign Block
        if (pendingSignLocations.containsKey(uuid)) {
            Location loc = pendingSignLocations.remove(uuid);
            BlockData original = pendingSignOriginalBlockData.remove(uuid);
            if (loc != null && original != null) {
                loc.getBlock().setBlockData(original);
            }
        }

        // Restore Support Block
        /*
         * if (pendingSupportLocations.containsKey(uuid)) {
         * Location loc = pendingSupportLocations.remove(uuid);
         * BlockData original = pendingSupportOriginalBlockData.remove(uuid);
         * if (loc != null && original != null) {
         * loc.getBlock().setBlockData(original);
         * }
         * }
         */

        // Clean up callback if present (e.g. on quit)
        pendingCallbacks.remove(uuid);
    }

    public void cleanup() {
        for (UUID uuid : new java.util.HashSet<>(pendingSignLocations.keySet())) {
            restoreBlocks(uuid);
        }
    }
}
