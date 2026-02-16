package com.h2ph.rtp;

import com.h2ph.PrismSurvival;
import com.h2ph.commands.player.RTPCommand;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class RTPManager {

    private static final Map<UUID, Location> initialLocations = new HashMap<>();
    private static final Map<UUID, org.bukkit.scheduler.BukkitTask> countdownTasks = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>(); // Store expiry time
    private static final Random random = new Random();

    public static boolean isTeleporting(Player player) {
        return countdownTasks.containsKey(player.getUniqueId());
    }

    public static boolean isOnCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            return cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
        }
        return false;
    }

    public static void teleport(Player player, String worldType) {
        teleport(player, "europe", worldType);
    }

    public static void teleport(Player player, String region, String worldType) {
        // Check Cooldown
        if (cooldowns.containsKey(player.getUniqueId())) {
            long expiry = cooldowns.get(player.getUniqueId());
            long remaining = expiry - System.currentTimeMillis();
            if (remaining > 0) {
                long seconds = (remaining / 1000) + 1;
                String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cYou can't rtp for another " + seconds + "s");
                player.sendMessage(msg);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            } else {
                cooldowns.remove(player.getUniqueId());
            }
        }

        // Prevent multiple teleports
        if (countdownTasks.containsKey(player.getUniqueId())) {
            String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cYou are already teleporting!");
            player.sendMessage(msg);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
            return;
        }

        // Close inventory
        player.closeInventory();

        initialLocations.put(player.getUniqueId(), player.getLocation());
        startCountdown(player, region, worldType);
    }

    public static void teleportInstant(Player player, String region, String worldType) {
        // Bypass cooldown and warmup checks for queue system
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                .fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleporting...")));
        calculateLocation(player, region, worldType, (target) -> {
            if (target != null) {
                player.teleportAsync(target);
            }
        });
    }

    private static void startCountdown(Player player, String region, String worldType) {
        PrismSurvival main = JavaPlugin.getPlugin(PrismSurvival.class);

        // Use an array or AtomicInteger to hold mutable count in lambda
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(5);

        // Schedule timer task via adapter
        org.bukkit.scheduler.BukkitTask task = main.getSchedulerAdapter().runTaskTimer(() -> {
            // Strict check: if not in map, stop immediately (avoid spam)
            if (!countdownTasks.containsKey(player.getUniqueId())) {
                return;
            }

            // Initial Move Check
            if (!initialLocations.containsKey(player.getUniqueId()) || hasMoved(player)) {
                cancelTeleport(player, "&cTeleport cancelled because you moved.");
                return;
            }

            if (!player.isOnline()) {
                cancelTeleport(player, null);
                return;
            }

            int currentCount = count.get();
            if (currentCount > 0) {
                // Actionbar: &7Teleporting in &5{COUNT}s
                String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&7Teleporting in &5" + currentCount + "s");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));

                // Sound: BLOCK_NOTE_BLOCK_HAT
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);

                count.decrementAndGet();
            } else {
                // Teleport!
                // Actionbar: Teleporting...
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                        .fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleporting...")));

                // Match legacy behavior (teleport immediately)
                calculateLocation(player, region, worldType, (target) -> {
                    if (target == null) {
                        return;
                    }
                    // Final move check on main thread (actually this callback runs on region thread
                    // which is fine for teleport)
                    if (hasMoved(player)) {
                        cancelTeleport(player, "&cTeleport cancelled because you moved.");
                        return;
                    }

                    player.teleportAsync(target).thenAccept(success -> {
                        if (success) {
                            String successMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&7You teleported to a random location");
                            player.sendMessage(successMsg);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(successMsg));
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                            // Set Cooldown (15 seconds)
                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 15000L);
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&cTeleport failed unexpectly."));
                        }
                        cleanup(player);
                    });
                });
            }
        }, 1L, 20L);

        countdownTasks.put(player.getUniqueId(), task);
    }

    // New public method for QueueTask
    public static void calculateLocation(Player player, String region, String worldType,
            java.util.function.Consumer<Location> callback) {
        findSafeLocation(player, region, worldType, 0, callback);
    }

    private static boolean hasMoved(Player player) {
        Location initial = initialLocations.get(player.getUniqueId());
        Location current = player.getLocation();
        return initial.getWorld() != current.getWorld() ||
                initial.getBlockX() != current.getBlockX() ||
                initial.getBlockZ() != current.getBlockZ() ||
                Math.abs(initial.getBlockY() - current.getBlockY()) > 2;
    }

    private static void cancelTeleport(Player player, String reason) {
        if (reason != null) {
            String coloredReason = org.bukkit.ChatColor.translateAlternateColorCodes('&', reason);
            player.sendMessage(coloredReason);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredReason));
            // Villager No Sound
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        cleanup(player);
    }

    private static void cleanup(Player player) {
        initialLocations.remove(player.getUniqueId());
        org.bukkit.scheduler.BukkitTask task = countdownTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // Changed to void and generally recursive/callback style to handle
    // tick-spreading
    private static void findSafeLocation(Player player, String region, String worldType, int attempts,
            java.util.function.Consumer<Location> callback) {
        PrismSurvival main = JavaPlugin.getPlugin(PrismSurvival.class);
        FileConfiguration rtpConfig = main.getRTPRegionConfig(region);
        FileConfiguration globalConfig = main.getGlobalRTPConfig();

        if (rtpConfig == null || globalConfig == null || attempts >= 10) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&cCould not find a safe location. Please try again."));
            // Only cleanup if we are managing the teleport lifecycle (legacy), but here we
            // might be in queue mode.
            // If calculateLocation is called externally, player might not be in
            // initialLocations/countdownTasks.
            // But cleanup checks map.
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        String worldName = rtpConfig.getString("worlds." + worldType + ".world");
        if (worldName == null) {
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        // Check if player went offline
        if (!player.isOnline()) {
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        // Random coords
        int min = rtpConfig.getInt("worlds." + worldType + ".min", 0);
        int max = rtpConfig.getInt("worlds." + worldType + ".max", 5000);
        int centerX = rtpConfig.getInt("worlds." + worldType + ".center_x", 0);
        int centerZ = rtpConfig.getInt("worlds." + worldType + ".center_z", 0);

        int x = centerX + (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min + 1));
        int z = centerZ + (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min + 1));

        // Use getChunkAtAsync to load chunk properly on Folia
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            // Validate player after async
            if (!player.isOnline()) {
                cleanup(player);
                if (callback != null)
                    callback.accept(null);
                return;
            }

            List<String> blacklist = globalConfig.getStringList("blacklisted-blocks");
            Location target = null;

            // Get Y safely now that chunk is loaded
            int y = 0;
            if (world.getEnvironment() == World.Environment.NETHER) {
                y = 100;
            } else {
                y = world.getHighestBlockYAt(x, z);
            }

            if (world.getEnvironment() == World.Environment.NETHER) {
                for (int scanY = 100; scanY > 30; scanY--) {
                    Location checkLoc = new Location(world, x, scanY, z);
                    if (isSafe(checkLoc, blacklist)) {
                        target = checkLoc.add(0.5, 1, 0.5);
                        break;
                    }
                }
            } else {
                Location checkLoc = new Location(world, x, y, z);
                if (isSafe(checkLoc, blacklist)) {
                    target = checkLoc.add(0.5, 1, 0.5);
                }
            }

            if (target != null) {
                // Success
                if (callback != null)
                    callback.accept(target);
            } else {
                // Retry
                // Need to schedule on global or main scheduler to avoid stack overflow or
                // strict thread constraints?
                // Just calling findSafeLocation recursing via adapter loop is fine.
                main.getSchedulerAdapter().runTaskLater(() -> {
                    findSafeLocation(player, region, worldType, attempts + 1, callback);
                }, 1L);
            }
        }).exceptionally(e -> {
            // If chunk load fails
            main.getSchedulerAdapter().runTaskLater(() -> {
                findSafeLocation(player, region, worldType, attempts + 1, callback);
            }, 1L);
            return null;
        });
    }

    private static boolean isSafe(Location loc, List<String> blacklist) {
        Material type = loc.getBlock().getType();
        // Check blacklist
        if (blacklist.contains(type.name())) {
            return false;
        }
        // Ensure 2 blocks of air above
        if (loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR)
            return false;
        if (loc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR)
            return false;

        // Prevent landing on liquid if not configured? Assumed safe for now or
        // blacklist handled it.
        if (type == Material.LAVA || type == Material.WATER)
            return false;

        return true;
    }
}
