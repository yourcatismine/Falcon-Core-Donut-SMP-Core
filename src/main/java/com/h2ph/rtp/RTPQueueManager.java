package com.h2ph.rtp;

import com.h2ph.PrismSurvival;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RTPQueueManager {

    private final PrismSurvival plugin;

    private final List<RTPQueueRegion> cachedQueues = new ArrayList<>();
    private final java.util.Map<String, Integer> globalCountdowns = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, java.util.Set<java.util.UUID>> playersInQueue = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, org.bukkit.Location> preCalculatedLocations = new java.util.concurrent.ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask globalTimer;

    public RTPQueueManager(PrismSurvival plugin) {
        this.plugin = plugin;
        loadQueues();
        startGlobalTimer();
    }

    public void loadQueues() {
        // Cleanup
        disable();

        cachedQueues.clear();
        globalCountdowns.clear();
        playersInQueue.clear();
        preCalculatedLocations.clear();

        File rtpFolder = new File(plugin.getDataFolder(), "rtp");
        File queueFolder = new File(rtpFolder, "queue");

        if (!queueFolder.exists() || !queueFolder.isDirectory()) {
            return;
        }

        File[] files = queueFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String worldName = config.getString("world");
            if (worldName == null)
                continue;

            double x1 = config.getDouble("pos1.x");
            double y1 = config.getDouble("pos1.y");
            double z1 = config.getDouble("pos1.z");
            double x2 = config.getDouble("pos2.x");
            double y2 = config.getDouble("pos2.y");
            double z2 = config.getDouble("pos2.z");
            String regionName = file.getName().replace(".yml", "").toLowerCase();

            RTPQueueRegion region = new RTPQueueRegion(regionName, worldName,
                    Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
            cachedQueues.add(region);

            // Initialize global countdown
            globalCountdowns.put(regionName, 20);
            playersInQueue.put(regionName, java.util.concurrent.ConcurrentHashMap.newKeySet());
        }

        // Ensure timer is running
        startGlobalTimer();
    }

    private void startGlobalTimer() {
        if (globalTimer != null)
            globalTimer.cancel();

        globalTimer = plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (RTPQueueRegion region : cachedQueues) {
                String name = region.name;
                int current = globalCountdowns.getOrDefault(name, 20);
                int nextVal;

                if (current <= 1) {
                    // Teleport everyone in queue
                    teleportQueue(region);
                    nextVal = 20;
                } else {
                    nextVal = current - 1;
                }
                globalCountdowns.put(name, nextVal);

                // Trigger pre-calculation at 5 seconds remaining
                if (nextVal == 5) {
                    java.util.Set<java.util.UUID> players = playersInQueue.get(name);
                    if (players != null && !players.isEmpty()) {
                        // Pick a leader to determine the group location
                        java.util.UUID leaderId = players.iterator().next();
                        Player leader = plugin.getServer().getPlayer(leaderId);

                        if (leader != null && leader.isOnline()) {
                            String worldType = "overworld";
                            if (leader.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER)
                                worldType = "nether";
                            if (leader.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END)
                                worldType = "end";

                            final String type = worldType;
                            RTPManager.calculateLocation(leader, name, worldType, (centerLoc) -> {
                                if (centerLoc != null) {
                                    // Distribute this location to all players with slight scatter
                                    for (java.util.UUID uuid : players) {
                                        // Skip if already has a location? No, overwrite for group sync

                                        org.bukkit.Location target = centerLoc.clone();
                                        // Scatter +/- 3 blocks
                                        double dx = (Math.random() * 6) - 3;
                                        double dz = (Math.random() * 6) - 3;
                                        target.add(dx, 0, dz);

                                        // Re-adjust Y for overworld to ensure safety
                                        if (type.equals("overworld")) {
                                            int highestY = target.getWorld().getHighestBlockYAt(target);
                                            target.setY(highestY + 1);
                                        }
                                        // For nether/end, we keep the center Y (safe-ish assumption for small scatter)

                                        preCalculatedLocations.put(uuid, target);
                                    }
                                }
                            });
                        }
                    }
                }

                // Send synchronized titles to all players in this queue
                // Delay by 10 ticks (0.5s) to match slow hologram/placeholder updates
                final int displayVal = nextVal;
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    java.util.Set<java.util.UUID> players = playersInQueue.get(name);
                    if (players != null && !players.isEmpty()) {
                        String titleText = ChatColor.translateAlternateColorCodes('&', "&dʀᴛᴘ ᴢᴏɴᴇ");
                        String subtitleText;
                        boolean playTickSound = false;

                        if (displayVal <= 5) {
                            subtitleText = ChatColor.translateAlternateColorCodes('&',
                                    "&cTeleporting in " + displayVal + "s");
                            playTickSound = true;
                        } else {
                            subtitleText = ChatColor.translateAlternateColorCodes('&',
                                    "&fTeleporting in " + displayVal);
                        }

                        for (java.util.UUID uuid : players) {
                            Player player = plugin.getServer().getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                // Updates: 0 fade-in/out for crisp synchronization
                                player.sendTitle(titleText, subtitleText, 0, 30, 0);
                                if (playTickSound) {
                                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f,
                                            2f);
                                }
                            }
                        }
                    }
                }, 10L);
            }
        }, 20L, 20L);
    }

    private void teleportQueue(RTPQueueRegion region) {
        java.util.Set<java.util.UUID> players = playersInQueue.get(region.name);
        if (players == null || players.isEmpty())
            return;

        for (java.util.UUID uuid : players) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Check if we have a pre-calculated location
                org.bukkit.Location target = preCalculatedLocations.remove(uuid);

                if (target != null) {
                    teleportPlayer(player, target);
                } else {
                    String worldType = "overworld";
                    if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER)
                        worldType = "nether";
                    if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END)
                        worldType = "end";

                    // Fallback: calculate now if missed pre-calc
                    RTPManager.calculateLocation(player, region.name, worldType, (loc) -> {
                        if (loc != null) {
                            teleportPlayer(player, loc);
                        }
                    });
                }
            } else {
                // Offline clean up
                preCalculatedLocations.remove(uuid);
            }
        }
        players.clear();
    }

    private void teleportPlayer(Player player, org.bukkit.Location target) {
        player.teleportAsync(target).thenAccept(success -> {
            if (success) {
                String successMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7You teleported to a random location");
                player.sendMessage(successMsg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(successMsg));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
        });
    }

    public int getCountdown(String regionName) {
        return globalCountdowns.getOrDefault(regionName.toLowerCase(), 20);
    }

    public void joinQueue(String regionName, java.util.UUID uuid) {
        String regionKey = regionName.toLowerCase();
        java.util.Set<java.util.UUID> players = playersInQueue.get(regionKey);
        if (players != null) {
            players.add(uuid);

            // Send immediate title
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                int current = globalCountdowns.getOrDefault(regionKey, 20);
                String titleText = ChatColor.translateAlternateColorCodes('&', "&dʀᴛᴘ ᴢᴏɴᴇ");
                String subtitleText = ChatColor.translateAlternateColorCodes('&', "&fTeleporting in " + current);
                // Initial send: Fade in
                player.sendTitle(titleText, subtitleText, 10, 40, 10);
            }
        }
    }

    public void leaveQueue(String regionName, java.util.UUID uuid) {
        String name = regionName.toLowerCase();
        java.util.Set<java.util.UUID> players = playersInQueue.get(name);
        if (players != null) {
            players.remove(uuid);
        }
        preCalculatedLocations.remove(uuid);

        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            int current = globalCountdowns.getOrDefault(name, 20);

            String titleText = ChatColor.translateAlternateColorCodes('&', "&dʀᴛᴘ ᴢᴏɴᴇ");
            String subtitleText;

            if (current <= 5) {
                subtitleText = ChatColor.translateAlternateColorCodes('&', "&cTeleporting in " + current + "s");
            } else {
                subtitleText = ChatColor.translateAlternateColorCodes('&', "&fTeleporting in " + current);
            }

            // Send with 0 stay and 10 fade-out to trigger smooth exit
            player.sendTitle(titleText, subtitleText, 0, 0, 10);
        }
    }

    public void disable() {
        if (globalTimer != null) {
            globalTimer.cancel();
            globalTimer = null;
        }
    }

    public RTPQueueRegion getQueueAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;
        String worldName = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        for (RTPQueueRegion region : cachedQueues) {
            if (region.worldName.equals(worldName) &&
                    x >= region.minX && x <= region.maxX &&
                    y >= region.minY && y <= region.maxY &&
                    z >= region.minZ && z <= region.maxZ) {
                return region;
            }
        }
        return null;
    }

    public static class RTPQueueRegion {
        public final String name;
        public final String worldName;
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;

        public RTPQueueRegion(String name, String worldName, double minX, double minY, double minZ, double maxX,
                double maxY, double maxZ) {
            this.name = name;
            this.worldName = worldName;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public org.bukkit.Location getCenter() {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null)
                return null;
            return new org.bukkit.Location(world, (minX + maxX) / 2.0, maxY, (minZ + maxZ) / 2.0);
        }
    }

    public void createQueue(Player player, String regionName) {
        // Check if the region exists in rtp folder
        File rtpFolder = new File(plugin.getDataFolder(), "rtp");
        File regionFolder = new File(rtpFolder, regionName);

        if (!regionFolder.exists() || !regionFolder.isDirectory()) {
            player.sendMessage(ChatColor.RED + "RTP Region '" + regionName + "' does not exist.");
            return;
        }

        // Get WorldEdit selection
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

            // Save to rtp/queue/<region>.yml
            File queueFolder = new File(rtpFolder, "queue");
            if (!queueFolder.exists()) {
                queueFolder.mkdirs();
            }

            File queueFile = new File(queueFolder, regionName + ".yml");

            // Create file if not exists
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
            loadQueues(); // Reload cache

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

    public List<String> getAvailableRegions() {
        File rtpFolder = new File(plugin.getDataFolder(), "rtp");
        if (!rtpFolder.exists() || !rtpFolder.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = rtpFolder.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(name -> !name.equalsIgnoreCase("queue")) // Exclude "queue" folder
                .collect(Collectors.toList());
    }

    public List<String> getQueueNames() {
        return cachedQueues.stream().map(r -> r.name).collect(Collectors.toList());
    }

    public void deleteQueue(Player player, String regionName) {
        File rtpFolder = new File(plugin.getDataFolder(), "rtp");
        File queueFolder = new File(rtpFolder, "queue");
        File queueFile = new File(queueFolder, regionName + ".yml");

        if (!queueFile.exists()) {
            player.sendMessage(ChatColor.RED + "RTP Queue '" + regionName + "' does not exist.");
            return;
        }

        if (queueFile.delete()) {
            player.sendMessage(ChatColor.GREEN + "RTP Queue '" + regionName + "' deleted successfully.");
            loadQueues(); // Reload cache
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete RTP Queue '" + regionName + "'.");
        }
    }
}
