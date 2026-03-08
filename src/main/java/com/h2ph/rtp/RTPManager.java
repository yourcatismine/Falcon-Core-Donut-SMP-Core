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
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
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

        if (countdownTasks.containsKey(player.getUniqueId())) {
            String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cYou are already teleporting!");
            player.sendMessage(msg);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
            return;
        }

        player.closeInventory();

        initialLocations.put(player.getUniqueId(), player.getLocation());
        startCountdown(player, region, worldType);
    }

    public static void teleportInstant(Player player, String region, String worldType) {
        teleportInstant(player, region, worldType, false);
    }

    public static void teleportInstant(Player player, String region, String worldType, boolean silent) {
        if (!silent) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                    .fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleporting...")));
        }
        calculateLocation(player, region, worldType, (target) -> {
            if (target != null) {
                player.teleportAsync(target);
            }
        });
    }

    private static void startCountdown(Player player, String region, String worldType) {
        PrismSurvival main = JavaPlugin.getPlugin(PrismSurvival.class);

        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(5);

        org.bukkit.scheduler.BukkitTask task = main.getSchedulerAdapter().runTaskTimer(() -> {
            if (!countdownTasks.containsKey(player.getUniqueId())) {
                return;
            }

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
                String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&7Teleporting in &6" + currentCount + "s");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);

                count.decrementAndGet();
            } else if (currentCount == 0) {
                count.set(-1);

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent
                        .fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleporting...")));

                calculateLocation(player, region, worldType, (target) -> {
                    if (target == null) {
                        return;
                    }
                    if (hasMoved(player)) {
                        cancelTeleport(player, "&cTeleport cancelled because you moved.");
                        return;
                    }

                    cleanup(player);
                    player.teleportAsync(target).thenAccept(success -> {
                        if (success) {
                            String successMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&7You teleported to a random location");
                            player.sendMessage(successMsg);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(successMsg));
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 15000L);
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&cTeleport failed unexpectly."));
                        }
                    });
                });
            }
        }, 1L, 20L);

        countdownTasks.put(player.getUniqueId(), task);
    }

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

    private static void findSafeLocation(Player player, String region, String worldType, int attempts,
            java.util.function.Consumer<Location> callback) {
        PrismSurvival main = JavaPlugin.getPlugin(PrismSurvival.class);
        FileConfiguration rtpConfig = main.getRTPRegionConfig(region);
        FileConfiguration globalConfig = main.getGlobalRTPConfig();

        if (rtpConfig == null || globalConfig == null || attempts >= 10) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&cCould not find a safe location. Please try again."));
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
            main.getLogger().warning(
                    "[RTP] Could not find world: " + worldName + ". Please check rtp/" + region + "/config.yml");
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        if (!player.isOnline()) {
            cleanup(player);
            if (callback != null)
                callback.accept(null);
            return;
        }

        int min = rtpConfig.getInt("worlds." + worldType + ".min", 0);
        int max = rtpConfig.getInt("worlds." + worldType + ".max", 5000);
        int centerX = rtpConfig.getInt("worlds." + worldType + ".center_x", 0);
        int centerZ = rtpConfig.getInt("worlds." + worldType + ".center_z", 0);

        int x = centerX + (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min + 1));
        int z = centerZ + (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min + 1));

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            if (!player.isOnline()) {
                cleanup(player);
                if (callback != null)
                    callback.accept(null);
                return;
            }

            List<String> blacklist = globalConfig.getStringList("blacklisted-blocks");
            Location target = null;

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
                if (callback != null)
                    callback.accept(target);
            } else {
                // Need to schedule on global or main scheduler to avoid stack overflow or
                main.getSchedulerAdapter().runTaskLater(() -> {
                    findSafeLocation(player, region, worldType, attempts + 1, callback);
                }, 1L);
            }
        }).exceptionally(e -> {
            main.getSchedulerAdapter().runTaskLater(() -> {
                findSafeLocation(player, region, worldType, attempts + 1, callback);
            }, 1L);
            return null;
        });
    }

    private static boolean isSafe(Location loc, List<String> blacklist) {
        Material type = loc.getBlock().getType();
        if (blacklist.contains(type.name())) {
            return false;
        }
        if (loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR)
            return false;
        if (loc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR)
            return false;

        if (type == Material.LAVA || type == Material.WATER)
            return false;

        return true;
    }
}
