package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;

public class DuelArenaManager {

    public enum WinReason {
        NORMAL, FORFEIT
    }

    private final PrismSurvival plugin;
    private final DuelStatsManager statsManager;

    public PrismSurvival getPlugin() {
        return plugin;
    }

    private final java.util.Map<java.util.UUID, java.util.UUID> activeDuels = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, String> playerArenas = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> activeTasks = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Location> spectatingLosers = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> respawnAtHub = new java.util.HashSet<>();

    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> matchTasks = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.List<org.bukkit.block.BlockState>> arenaChanges = new java.util.HashMap<>();

    private java.util.List<String> ignoredCommands = new java.util.ArrayList<>();
    private java.util.List<String> bannedCommands = new java.util.ArrayList<>();
    private final java.util.Set<java.util.UUID> pendingForfeit = new java.util.HashSet<>();

    private final java.util.Map<String, ArenaRegion> arenaMap = new java.util.HashMap<>();

    public DuelArenaManager(PrismSurvival plugin, DuelStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        loadConfig();
    }

    private static class ArenaRegion {
        final String name;
        final String worldName;
        final String spawn1WorldName;
        final String spawn2WorldName;
        final String biome;
        final double minX, minY, minZ;
        final double maxX, maxY, maxZ;
        final Location spawn1;
        final Location spawn2;
        final int lootingMinutes;

        ArenaRegion(String name, YamlConfiguration config) {
            this.name = name;
            this.worldName = config.getString("world");
            this.spawn1WorldName = config.getString("spawn1.world");
            this.spawn2WorldName = config.getString("spawn2.world");
            this.biome = config.getString("biome");
            this.lootingMinutes = config.getInt("looting-minutes", 5);

            this.minX = Math.min(config.getDouble("min.x"), config.getDouble("max.x"));
            this.minY = Math.min(config.getDouble("min.y"), config.getDouble("max.y"));
            this.minZ = Math.min(config.getDouble("min.z"), config.getDouble("max.z"));

            this.maxX = Math.max(config.getDouble("min.x"), config.getDouble("max.x"));
            this.maxY = Math.max(config.getDouble("min.y"), config.getDouble("max.y"));
            this.maxZ = Math.max(config.getDouble("min.z"), config.getDouble("max.z"));

            this.spawn1 = new Location(
                    org.bukkit.Bukkit.getWorld(config.getString("spawn1.world")),
                    config.getInt("spawn1.x") + 0.5,
                    config.getInt("spawn1.y"),
                    config.getInt("spawn1.z") + 0.5,
                    (float) config.getDouble("spawn1.yaw"),
                    (float) config.getDouble("spawn1.pitch"));

            this.spawn2 = new Location(
                    org.bukkit.Bukkit.getWorld(config.getString("spawn2.world")),
                    config.getInt("spawn2.x") + 0.5,
                    config.getInt("spawn2.y"),
                    config.getInt("spawn2.z") + 0.5,
                    (float) config.getDouble("spawn2.yaw"),
                    (float) config.getDouble("spawn2.pitch"));
        }

        boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName))
                return false;
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "survival/duels/config.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("survival/duels/config.yml", false);
            } catch (Exception e) {
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ignoredCommands = config.getStringList("ignored-commands");
        if (ignoredCommands == null) {
            ignoredCommands = new java.util.ArrayList<>();
        }
        bannedCommands = config.getStringList("banned-commands");
        if (bannedCommands == null) {
            bannedCommands = new java.util.ArrayList<>();
        }

        loadArenas();
    }

    private void loadArenas() {
        arenaMap.clear();
        File regionsFolder = new File(plugin.getDataFolder(), "survival/regions/duels");
        if (!regionsFolder.exists())
            return;

        File[] files = regionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                if (cfg.contains("spawn1.world") && cfg.contains("spawn2.world")) {
                    ArenaRegion region = new ArenaRegion(file.getName(), cfg);
                    arenaMap.put(file.getName(), region);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load arena file: " + file.getName());
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("Loaded " + arenaMap.size() + " duel arenas.");
    }

    public void reloadArena(String name) {
        File file = new File(plugin.getDataFolder(), "survival/regions/duels/" + name + ".yml");
        if (!file.exists()) {
            arenaMap.remove(name);
            return;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            if (cfg.contains("spawn1.world") && cfg.contains("spawn2.world")) {
                ArenaRegion region = new ArenaRegion(file.getName(), cfg);
                arenaMap.put(file.getName(), region);
                plugin.getLogger().info("Reloaded arena: " + name);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reload arena: " + name);
            e.printStackTrace();
        }
    }

    public boolean isCommandIgnored(String message) {
        if (message.isEmpty())
            return false;
        String[] parts = message.substring(1).split(" ");
        String cmd = parts[0].toLowerCase();
        return ignoredCommands.contains(cmd);
    }

    public boolean isCommandBanned(String message) {
        if (message.isEmpty())
            return false;
        String[] parts = message.substring(1).split(" ");
        String cmd = parts[0].toLowerCase();
        return bannedCommands.contains(cmd);
    }

    public void markForfeit(Player player) {
        pendingForfeit.add(player.getUniqueId());
    }

    public boolean isForfeit(Player player) {
        return pendingForfeit.contains(player.getUniqueId());
    }

    /**
     * Pre-cache the spectator location before respawn fires (for respawnImmediately
     * support).
     */
    public void cacheSpectatorLocation(Player player) {
        spectatingLosers.put(player.getUniqueId(), player.getLocation());
    }

    public void recordBlockChange(String arenaName, org.bukkit.block.BlockState state) {
        arenaChanges.computeIfAbsent(arenaName, k -> new java.util.ArrayList<>()).add(state);
    }

    public void restoreArena(String arenaName) {
        if (arenaName == null)
            return;
        java.util.List<org.bukkit.block.BlockState> states = arenaChanges.remove(arenaName);
        if (states != null) {
            java.util.Collections.reverse(states);
            for (org.bukkit.block.BlockState state : states) {
                plugin.getSchedulerAdapter().runAtLocation(state.getLocation(), () -> {
                    state.update(true, false);
                });
            }
        }

        ArenaRegion region = arenaMap.get(arenaName);
        if (region == null)
            return;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(region.worldName);
        if (world == null)
            return;

        double minX = region.minX;
        double minY = region.minY;
        double minZ = region.minZ;
        double maxX = region.maxX;
        double maxY = region.maxY;
        double maxZ = region.maxZ;

        Location center = new Location(world, (minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);

        plugin.getSchedulerAdapter().runAtLocation(center, () -> {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof org.bukkit.entity.Player) {
                    continue;
                }

                Location loc = entity.getLocation();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();

                if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                    entity.remove();
                }
            }

            int iMinX = (int) Math.floor(minX);
            int iMinY = (int) Math.floor(minY);
            int iMinZ = (int) Math.floor(minZ);
            int iMaxX = (int) Math.ceil(maxX);
            int iMaxY = (int) Math.ceil(maxY);
            int iMaxZ = (int) Math.ceil(maxZ);

            for (int bx = iMinX; bx <= iMaxX; bx++) {
                for (int by = iMinY; by <= iMaxY; by++) {
                    for (int bz = iMinZ; bz <= iMaxZ; bz++) {
                        org.bukkit.block.Block block = world.getBlockAt(bx, by, bz);
                        org.bukkit.Material type = block.getType();
                        if (type == org.bukkit.Material.FIRE || type == org.bukkit.Material.SOUL_FIRE) {
                            block.setType(org.bukkit.Material.AIR);
                        }
                    }
                }
            }
        });
    }

    public boolean startDuel(Player player1, Player player2) {
        return startDuel(player1, player2, 5, "Random");
    }

    public String getArenaName(Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    public boolean startDuel(Player player1, Player player2, int durationMinutes, String biome) {
        cleanupPendings(player1);
        cleanupPendings(player2);

        ArenaRegion arenaRegion = getAvailableArena(biome);
        if (arenaRegion == null) {
            if (!biome.equalsIgnoreCase("Random")) {
                return false;
            }
            arenaRegion = getAvailableArena("Random");
        }

        if (arenaRegion == null)
            return false;

        if (arenaRegion == null)
            return false;

        Location spawn1 = arenaRegion.spawn1;
        Location spawn2 = arenaRegion.spawn2;

        if (spawn1.getWorld() == null || spawn2.getWorld() == null) {
            return false;
        }

        player1.teleportAsync(spawn1);
        player2.teleportAsync(spawn2);

        activeDuels.put(player1.getUniqueId(), player2.getUniqueId());
        activeDuels.put(player2.getUniqueId(), player1.getUniqueId());
        playerArenas.put(player1.getUniqueId(), arenaRegion.name);
        playerArenas.put(player2.getUniqueId(), arenaRegion.name);

        String titleMain = ChatColor.translateAlternateColorCodes('&',
                "&4" + DuelGUIManager.toSmallCaps("casual duel"));
        String titleSub = ChatColor.translateAlternateColorCodes('&', "&fFight players and steal their loot.");

        player1.sendTitle(titleMain, titleSub, 10, 60, 20);
        player2.sendTitle(titleMain, titleSub, 10, 60, 20);

        String p1WinRate = statsManager.getWinRate(player1.getUniqueId());
        String p2WinRate = statsManager.getWinRate(player2.getUniqueId());

        String p1ActionBar = ChatColor.translateAlternateColorCodes('&',
                "&7Your opponent &a" + player2.getName() + "&7 has a &d" + p2WinRate + "&7 win rate. Good luck.");
        String p2ActionBar = ChatColor.translateAlternateColorCodes('&',
                "&7Your opponent &a" + player1.getName() + "&7 has a &d" + p1WinRate + "&7 win rate. Good luck.");

        player1.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(p1ActionBar));
        player2.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(p2ActionBar));

        try {
            player1.playSound(player1.getLocation(), "ambient.cave", 1f, 1f);
            player2.playSound(player2.getLocation(), "ambient.cave", 1f, 1f);
        } catch (Exception ignored) {
        }

        startMatchTimer(player1, player2, durationMinutes * 60);

        return true;
    }

    private void cleanupPendings(Player p) {
        if (activeTasks.containsKey(p.getUniqueId())) {
            org.bukkit.scheduler.BukkitTask t = activeTasks.remove(p.getUniqueId());
            if (t != null)
                t.cancel();
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

            String arenaName = playerArenas.remove(p.getUniqueId());
            if (arenaName != null) {
                restoreArena(arenaName);
            }
        }
        if (matchTasks.containsKey(p.getUniqueId())) {
            org.bukkit.scheduler.BukkitTask mt = matchTasks.remove(p.getUniqueId());
            if (mt != null)
                mt.cancel();
        }
    }

    private void startMatchTimer(Player p1, Player p2, int seconds) {
        java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> taskRef = new java.util.concurrent.atomic.AtomicReference<>();

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(p1, new Runnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!p1.isOnline() || !p2.isOnline()) {
                    if (!p1.isOnline() && !p2.isOnline()) {
                        org.bukkit.scheduler.BukkitTask t = taskRef.get();
                        if (t != null)
                            t.cancel();
                        matchTasks.remove(p1.getUniqueId());
                        matchTasks.remove(p2.getUniqueId());
                    }
                    return;
                }

                if (remaining <= 0) {
                    endDuelInDraw(p1, p2);
                    org.bukkit.scheduler.BukkitTask t = taskRef.get();
                    if (t != null)
                        t.cancel();
                    return;
                }

                if (remaining % 60 == 0) {
                    int minutes = remaining / 60;
                    String minStr = (minutes == 1) ? "minute" : "minutes";

                    String msg = ChatColor.translateAlternateColorCodes('&',
                            "&7There are &a" + minutes + " " + minStr + "&f left before the match to end");

                    if (p1.isOnline()) {
                        p1.sendMessage(msg);
                    }
                    if (p2.isOnline()) {
                        p2.sendMessage(msg);
                    }
                }

                remaining--;
            }
        }, 20L, 20L);

        taskRef.set(task);

        matchTasks.put(p1.getUniqueId(), task);
        matchTasks.put(p2.getUniqueId(), task);
    }

    public void endDuelInDraw(Player p1, Player p2) {
        String arenaName = playerArenas.get(p1.getUniqueId());

        cleanupDuelData(p1, p2);
        playerArenas.remove(p1.getUniqueId());
        playerArenas.remove(p2.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', "&7&l" + DuelGUIManager.toSmallCaps("draw"));
        String sub = ChatColor.translateAlternateColorCodes('&', "&fNo one lost");

        sendDrawUI(p1, title, sub);
        sendDrawUI(p2, title, sub);

        plugin.getSchedulerAdapter().runEntityTaskLater(p1, () -> {
            if (p1.isOnline())
                teleportToSpawn(p1);
            if (p2.isOnline())
                teleportToSpawn(p2);

            restoreArena(arenaName);
        }, 60L);
    }

    private void sendDrawUI(Player p, String title, String sub) {
        if (p.isOnline()) {
            p.sendTitle(title, sub, 10, 60, 20);
            p.sendMessage(ChatColor.GRAY + "Time limit reached! It's a draw.");
        }
    }

    private void cleanupDuelData(Player p1, Player p2) {
        activeDuels.remove(p1.getUniqueId());
        activeDuels.remove(p2.getUniqueId());

        if (matchTasks.containsKey(p1.getUniqueId())) {
            matchTasks.remove(p1.getUniqueId()).cancel();
        }
        if (matchTasks.containsKey(p2.getUniqueId())) {
            matchTasks.remove(p2.getUniqueId()).cancel();
        }
    }

    public void endDuel(Player winner, Player loser) {
        endDuel(winner, loser, WinReason.NORMAL);
    }

    public void endDuel(Player winner, Player loser, WinReason reason) {
        pendingForfeit.remove(winner.getUniqueId());
        pendingForfeit.remove(loser.getUniqueId());

        if (!spectatingLosers.containsKey(loser.getUniqueId())) {
            spectatingLosers.put(loser.getUniqueId(), loser.getLocation());
        }

        String arenaName = playerArenas.get(winner.getUniqueId());

        cleanupDuelData(winner, loser);
        playerArenas.remove(loser.getUniqueId());

        statsManager.addWin(winner.getUniqueId());
        statsManager.addLoss(loser.getUniqueId());

        int lootingMinutes = 5;
        if (arenaName != null) {
            ArenaRegion region = arenaMap.get(arenaName);
            if (region != null) {
                lootingMinutes = region.lootingMinutes;
            }
        }

        String winTitle;
        if (reason == WinReason.FORFEIT) {
            winTitle = ChatColor.translateAlternateColorCodes('&',
                    "&4&l" + DuelGUIManager.toSmallCaps("opponent left"));
        } else {
            winTitle = ChatColor.translateAlternateColorCodes('&', "&a&l" + DuelGUIManager.toSmallCaps("you won"));
        }
        String winSub = ChatColor.translateAlternateColorCodes('&', "&fGet your loot before the time runs out");
        winner.sendTitle(winTitle, winSub, 10, 60, 20);
        try {
            winner.playSound(winner.getLocation(), "ambient.cave", 1f, 1f);
        } catch (Exception ignored) {
        }

        int totalSeconds = lootingMinutes * 60;
        java.util.concurrent.atomic.AtomicInteger winnerSeconds = new java.util.concurrent.atomic.AtomicInteger(
                totalSeconds);
        java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> winTaskRef = new java.util.concurrent.atomic.AtomicReference<>();

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(winner, () -> {
            if (!winner.isOnline()) {
                org.bukkit.scheduler.BukkitTask t = winTaskRef.get();
                if (t != null)
                    t.cancel();
                activeTasks.remove(winner.getUniqueId());
                playerArenas.remove(winner.getUniqueId());
                restoreArena(arenaName);
                return;
            }

            int remaining = winnerSeconds.get();
            if (remaining > 0) {
                int mins = remaining / 60;
                int secs = remaining % 60;
                String actionMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7You have &d" + mins + "m " + secs + "s&7 to collect the loot");
                winner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));

                try {
                    winner.playSound(winner.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                } catch (NoSuchFieldError | IllegalArgumentException e) {
                }
                winnerSeconds.decrementAndGet();
            } else {
                teleportToSpawn(winner);
                winner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                activeTasks.remove(winner.getUniqueId());
                playerArenas.remove(winner.getUniqueId());
                restoreArena(arenaName);
                org.bukkit.scheduler.BukkitTask t = winTaskRef.get();
                if (t != null)
                    t.cancel();
            }
        }, 0L, 20L);
        winTaskRef.set(task);
        activeTasks.put(winner.getUniqueId(), task);

        String loseTitle = ChatColor.translateAlternateColorCodes('&', "&4&l" + DuelGUIManager.toSmallCaps("you lose"));
        String loseSub = ChatColor.translateAlternateColorCodes('&', "&fBetter luck next time");

        org.bukkit.Location spectatorLoc = spectatingLosers.get(loser.getUniqueId());

        Runnable forceSpectator = () -> {
            if (loser.isOnline() && !loser.isDead()) {
                loser.setGameMode(org.bukkit.GameMode.SPECTATOR);
                if (spectatorLoc != null) {
                    loser.teleportAsync(spectatorLoc);
                }
            }
        };

        if (!loser.isDead()) {
            plugin.getSchedulerAdapter().runEntityTask(loser, forceSpectator);
            plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 1L);
            plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 5L);
            plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 10L);
        } else {
            plugin.getSchedulerAdapter().runEntityTaskLater(loser, () -> {
                if (loser.isOnline() && loser.isDead()) {
                    loser.spigot().respawn();
                }
                plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 1L);
                plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 5L);
                plugin.getSchedulerAdapter().runEntityTaskLater(loser, forceSpectator, 10L);
            }, 1L);
        }

        loser.sendTitle(loseTitle, loseSub, 10, 60, 20);
        try {
            loser.playSound(loser.getLocation(), "ambient.cave", 1f, 1f);
        } catch (Exception ignored) {
        }

        final java.util.concurrent.atomic.AtomicInteger loserSeconds = new java.util.concurrent.atomic.AtomicInteger(5);
        final java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> loseTaskRef = new java.util.concurrent.atomic.AtomicReference<>();

        org.bukkit.scheduler.BukkitTask loseTask = plugin.getSchedulerAdapter().runEntityTaskTimer(loser, () -> {
            if (!loser.isOnline()) {
                spectatingLosers.remove(loser.getUniqueId());
                respawnAtHub.remove(loser.getUniqueId());
                if (loseTaskRef.get() != null)
                    loseTaskRef.get().cancel();
                return;
            }

            int s = loserSeconds.get();
            if (s > 0) {
                String actionMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7Teleporting you back in &d" + s + " seconds");
                loser.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
                try {
                    loser.playSound(loser.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                } catch (NoSuchFieldError | IllegalArgumentException e) {
                    loser.playSound(loser.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
                loserSeconds.decrementAndGet();
            } else {
                spectatingLosers.remove(loser.getUniqueId());
                if (loser.isDead()) {
                    respawnAtHub.add(loser.getUniqueId());
                } else {
                    teleportToSpawn(loser);
                    loser.setGameMode(org.bukkit.GameMode.SURVIVAL);
                }
                loser.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                if (loseTaskRef.get() != null)
                    loseTaskRef.get().cancel();
            }
        }, 0L, 20L);
        loseTaskRef.set(loseTask);

    }

    public Location getSpectatorLocation(Player player) {
        return spectatingLosers.get(player.getUniqueId());
    }

    public boolean shouldRespawnAtHub(Player player) {
        if (respawnAtHub.contains(player.getUniqueId())) {
            respawnAtHub.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    public boolean isLooting(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    public void stopLooting(Player player) {
        java.util.UUID uuid = player.getUniqueId();
        if (activeTasks.containsKey(uuid)) {
            org.bukkit.scheduler.BukkitTask task = activeTasks.remove(uuid);
            if (task != null) {
                task.cancel();
            }

            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(""));

            String arenaName = playerArenas.remove(uuid);
            if (arenaName != null) {
                restoreArena(arenaName);
            }

            teleportToSpawn(player);
        }
    }

    public Player getOpponent(Player player) {
        java.util.UUID oppId = activeDuels.get(player.getUniqueId());
        if (oppId != null) {
            return org.bukkit.Bukkit.getPlayer(oppId);
        }
        return null;
    }

    public boolean isLocationInArena(Location loc) {
        return getArenaAt(loc) != null;
    }

    public String getArenaAt(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;

        for (ArenaRegion region : arenaMap.values()) {
            if (region.contains(loc)) {
                return region.name;
            }
        }
        return null;
    }

    public void resetPlayer(Player player) {
        teleportToSpawn(player);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        if (spectatingLosers.containsKey(player.getUniqueId())) {
            spectatingLosers.remove(player.getUniqueId());
        }
    }

    public void teleportToSpawn(Player player) {
        String worldName = player.getWorld().getName();
        Location spawn = plugin.getSpawnManager().getBestSpawnForWorld(worldName);
        
        if (spawn != null) {
            player.teleportAsync(spawn);
        } else {
            player.teleportAsync(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
            player.sendMessage(ChatColor.RED + "No spawn set for this world, teleported to default world spawn.");
        }
    }

    private ArenaRegion getAvailableArena(String biome) {
        if (arenaMap.isEmpty())
            return null;

        java.util.List<ArenaRegion> regions = new java.util.ArrayList<>(arenaMap.values());
        java.util.Collections.shuffle(regions);

        for (ArenaRegion region : regions) {
            if (playerArenas.containsValue(region.name)) {
                continue;
            }

            if (region.spawn1.getWorld() == null) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(region.spawn1WorldName);
                if (w != null)
                    region.spawn1.setWorld(w);
            }
            if (region.spawn2.getWorld() == null) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(region.spawn2WorldName);
                if (w != null)
                    region.spawn2.setWorld(w);
            }

            if (region.spawn1.getWorld() == null || region.spawn2.getWorld() == null) {
                continue;
            }

            if (arenaChanges.containsKey(region.name)) {
                restoreArena(region.name);
            }

            if (biome != null && !biome.equals("Random")) {
                if (region.biome == null || !region.biome.equalsIgnoreCase(biome)) {
                    continue;
                }
            }

            return region;
        }
        return null;
    }

    private ArenaRegion getAvailableArena() {
        return getAvailableArena("Random");
    }
}
