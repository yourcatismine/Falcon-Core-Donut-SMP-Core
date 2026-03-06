package com.h2ph.managers;

import com.h2ph.PrismSurvival;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RedstoneManager implements Listener {

    private static final int THRESHOLD_MASSIVE = 3000;
    private static final int THRESHOLD_WARNING = 1500;

    private final PrismSurvival plugin;

    private final Map<UUID, Map<Long, AtomicInteger>> chunkRedstoneCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Long, Long>> temporarilyDisabledChunks = new ConcurrentHashMap<>();
    private final List<RedstoneAlert> globalAlertHistory = new CopyOnWriteArrayList<>();
    private long lastBroadcastTime = 0;

    private final Set<UUID> guiViewers = new HashSet<>();
    private final Map<UUID, Map<Long, Long>> lastNotificationTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerNotificationCooldown = new ConcurrentHashMap<>();

    private final Map<String, UUID> blockPlacements = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> chunkPlayers = new ConcurrentHashMap<>();

    private final File dataFile;
    private FileConfiguration dataConfig;

    private static final EnumSet<Material> REDSTONE_COMPONENTS = EnumSet.noneOf(Material.class);

    static {
        for (Material m : Material.values()) {
            String name = m.name();
            if (name.equals("REDSTONE_WIRE") ||
                    name.equals("REPEATER") ||
                    name.equals("COMPARATOR") ||
                    name.equals("OBSERVER") ||
                    name.equals("DAYLIGHT_DETECTOR") ||
                    name.contains("REDSTONE_TORCH") ||
                    name.contains("BUTTON") ||
                    name.equals("TRIPWIRE_HOOK") ||
                    name.equals("TRIPWIRE") ||
                    name.contains("PISTON") ||
                    name.equals("HOPPER") ||
                    name.equals("DROPPER") ||
                    name.equals("DISPENSER") ||
                    name.contains("PRESSURE_PLATE") ||
                    name.contains("DOOR") ||
                    name.contains("TRAPDOOR") ||
                    name.contains("GATE") ||
                    name.equals("LEVER")) {
                REDSTONE_COMPONENTS.add(m);
            }
        }
    }

    public RedstoneManager(JavaPlugin plugin) {
        this.plugin = (PrismSurvival) plugin;
        this.dataFile = new File(plugin.getDataFolder(), "redstone_data.yml");

        loadData();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.plugin.getSchedulerAdapter().runTaskTimer(this::checkThresholds, 20L, 20L);
        this.plugin.getSchedulerAdapter().runTaskTimer(this::updateLiveGUI, 20L, 20L);
        this.plugin.getSchedulerAdapter().runTaskTimer(this::saveData, 6000L, 6000L);
    }

    /**
     * Main Logic: Checks redstone counts every second and applies rules.
     */
    private void checkThresholds() {
        for (Map.Entry<UUID, Map<Long, AtomicInteger>> worldEntry : chunkRedstoneCounts.entrySet()) {
            UUID worldId = worldEntry.getKey();
            org.bukkit.World world = Bukkit.getWorld(worldId);
            if (world == null)
                continue;

            Map<Long, AtomicInteger> worldCounts = worldEntry.getValue();

            for (Map.Entry<Long, AtomicInteger> chunkEntry : worldCounts.entrySet()) {
                long chunkKey = chunkEntry.getKey();
                int count = chunkEntry.getValue().get();

                chunkEntry.getValue().set(0);

                if (count == 0)
                    continue;

                if (isChunkDisabled(worldId, chunkKey))
                    continue;

                if (count >= THRESHOLD_MASSIVE) {
                    if (shouldNotify(worldId, chunkKey, 60000)) {
                        disableChunk(worldId, chunkKey, 60000);
                        recordNotification(worldId, chunkKey);

                        int x = (int) (chunkKey >> 32);
                        int z = (int) chunkKey;

                        runOnChunk(world, x, z, () -> {
                            Chunk chunk = world.getChunkAt(x, z);
                            notifyNearbyPlayers(chunk, 60);

                            int blockX = x * 16 + 8;
                            int blockZ = z * 16 + 8;
                            int blockY = findAverageRedstoneY(world, x, z);

                            Set<UUID> responsiblePlayers = chunkPlayers.getOrDefault(chunkKey, Collections.emptySet());
                            List<String> playerNames = new ArrayList<>();
                            for (UUID uuid : responsiblePlayers) {
                                String name = Bukkit.getOfflinePlayer(uuid).getName();
                                if (name != null) {
                                    playerNames.add(name);
                                }
                            }

                            if (playerNames.isEmpty()) {
                                org.bukkit.Location center = new org.bukkit.Location(world, blockX, blockY, blockZ);
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    if (p.getWorld().equals(world) && p.getLocation().distance(center) <= 48) {
                                        playerNames.add(p.getName());
                                    }
                                }
                            }
                            String nearPlayers = playerNames.isEmpty() ? "Unknown" : String.join(", ", playerNames);

                            RedstoneAlert alert = new RedstoneAlert(
                                    world.getName(), blockX, blockY, blockZ,
                                    count, THRESHOLD_MASSIVE, nearPlayers,
                                    System.currentTimeMillis());

                            globalAlertHistory.add(0, alert);
                            if (globalAlertHistory.size() > 100) {
                                globalAlertHistory.remove(globalAlertHistory.size() - 1);
                            }

                            if (System.currentTimeMillis() - lastBroadcastTime > 20000) {
                                broadcastAlert("Massive high threshold detected", alert, 60);
                                lastBroadcastTime = System.currentTimeMillis();
                            }
                        });
                    }
                } else if (count >= THRESHOLD_WARNING) {
                    if (shouldNotify(worldId, chunkKey, 20000)) {
                        disableChunk(worldId, chunkKey, 20000);
                        recordNotification(worldId, chunkKey);

                        int x = (int) (chunkKey >> 32);
                        int z = (int) chunkKey;

                        runOnChunk(world, x, z, () -> {
                            Chunk chunk = world.getChunkAt(x, z);
                            notifyNearbyPlayers(chunk, 20);

                            int blockX = x * 16 + 8;
                            int blockZ = z * 16 + 8;
                            int blockY = findAverageRedstoneY(world, x, z);

                            Set<UUID> responsiblePlayers = chunkPlayers.getOrDefault(chunkKey, Collections.emptySet());
                            List<String> playerNames = new ArrayList<>();
                            for (UUID uuid : responsiblePlayers) {
                                String name = Bukkit.getOfflinePlayer(uuid).getName();
                                if (name != null) {
                                    playerNames.add(name);
                                }
                            }

                            if (playerNames.isEmpty()) {
                                org.bukkit.Location center = new org.bukkit.Location(world, blockX, blockY, blockZ);
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    if (p.getWorld().equals(world) && p.getLocation().distance(center) <= 48) {
                                        playerNames.add(p.getName());
                                    }
                                }
                            }
                            String nearPlayers = playerNames.isEmpty() ? "Unknown" : String.join(", ", playerNames);

                            RedstoneAlert alert = new RedstoneAlert(
                                    world.getName(), blockX, blockY, blockZ,
                                    count, THRESHOLD_WARNING, nearPlayers,
                                    System.currentTimeMillis());

                            globalAlertHistory.add(0, alert);
                            if (globalAlertHistory.size() > 100) {
                                globalAlertHistory.remove(globalAlertHistory.size() - 1);
                            }

                            if (System.currentTimeMillis() - lastBroadcastTime > 20000) {
                                broadcastAlert("High threshold detected", alert, 20);
                                lastBroadcastTime = System.currentTimeMillis();
                            }
                        });
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        for (Map<Long, Long> map : temporarilyDisabledChunks.values()) {
            map.entrySet().removeIf(entry -> now > entry.getValue());
        }

        globalAlertHistory.removeIf(alert -> now - alert.getTimestamp() > 3600000);
    }

    private void runOnChunk(org.bukkit.World world, int x, int z, Runnable task) {
        try {
            Bukkit.getRegionScheduler().execute(plugin, world, x, z, task);
        } catch (Throwable e) {
            task.run();
        }
    }

    private long getChunkKey(Chunk chunk) {
        return (long) chunk.getX() << 32 | (chunk.getZ() & 0xFFFFFFFFL);
    }

    private boolean isChunkDisabled(UUID worldId, long chunkKey) {
        Map<Long, Long> worldDisabled = temporarilyDisabledChunks.get(worldId);
        if (worldDisabled == null)
            return false;
        Long expiry = worldDisabled.get(chunkKey);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    private void disableChunk(UUID worldId, long chunkKey, long durationMs) {
        temporarilyDisabledChunks.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>())
                .put(chunkKey, System.currentTimeMillis() + durationMs);
    }

    private boolean shouldNotify(UUID worldId, long chunkKey, long cooldownMs) {
        Map<Long, Long> worldNotifs = lastNotificationTime.get(worldId);
        if (worldNotifs == null)
            return true;
        Long last = worldNotifs.get(chunkKey);
        return last == null || System.currentTimeMillis() - last > cooldownMs;
    }

    private void recordNotification(UUID worldId, long chunkKey) {
        lastNotificationTime.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>())
                .put(chunkKey, System.currentTimeMillis());
    }

    private void incrementCount(Chunk chunk) {
        chunkRedstoneCounts
                .computeIfAbsent(chunk.getWorld().getUID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(getChunkKey(chunk), k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * Get location key for block placement tracking
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * Record who placed a redstone block
     */
    private void recordBlockPlacement(Location loc, UUID playerUuid) {
        blockPlacements.put(getLocationKey(loc), playerUuid);

        Chunk chunk = loc.getChunk();
        long key = getChunkKey(chunk);
        chunkPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(playerUuid);
    }

    /**
     * Find average Y-level of redstone components in a chunk
     */
    private int findAverageRedstoneY(World world, int chunkX, int chunkZ) {
        try {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            int totalY = 0;
            int count = 0;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (REDSTONE_COMPONENTS.contains(block.getType())) {
                            totalY += y;
                            count++;
                        }
                    }
                }
            }

            return count > 0 ? totalY / count : 64;
        } catch (Exception e) {
            return 64;
        }
    }

    /**
     * Load persisted data from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No redstone data file found, starting fresh");
            return;
        }

        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);

            if (dataConfig.contains("alerts")) {
                ConfigurationSection alerts = dataConfig.getConfigurationSection("alerts");
                if (alerts != null) {
                    for (String key : alerts.getKeys(false)) {
                        try {
                            String world = alerts.getString(key + ".world");
                            int x = alerts.getInt(key + ".x");
                            int y = alerts.getInt(key + ".y");
                            int z = alerts.getInt(key + ".z");
                            int count = alerts.getInt(key + ".count");
                            int limit = alerts.getInt(key + ".limit");
                            String nearPlayers = alerts.getString(key + ".nearPlayers", "Unknown");
                            long timestamp = alerts.getLong(key + ".timestamp");

                            RedstoneAlert alert = new RedstoneAlert(world, x, y, z, count, limit, nearPlayers,
                                    timestamp);
                            globalAlertHistory.add(alert);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load alert: " + e.getMessage());
                        }
                    }
                }
                plugin.getLogger().info("Loaded " + globalAlertHistory.size() + " redstone alerts");
            }

            if (dataConfig.contains("placements")) {
                ConfigurationSection placements = dataConfig.getConfigurationSection("placements");
                if (placements != null) {
                    for (String key : placements.getKeys(false)) {
                        try {
                            String uuidStr = placements.getString(key);
                            blockPlacements.put(key, UUID.fromString(uuidStr));
                        } catch (Exception e) {
                        }
                    }
                }
                plugin.getLogger().info("Loaded " + blockPlacements.size() + " block placements");
            }

            if (dataConfig.contains("chunk-players")) {
                ConfigurationSection chunks = dataConfig.getConfigurationSection("chunk-players");
                if (chunks != null) {
                    for (String chunkKeyStr : chunks.getKeys(false)) {
                        try {
                            long chunkKey = Long.parseLong(chunkKeyStr);
                            List<String> uuidList = chunks.getStringList(chunkKeyStr);
                            Set<UUID> players = ConcurrentHashMap.newKeySet();
                            for (String uuidStr : uuidList) {
                                players.add(UUID.fromString(uuidStr));
                            }
                            chunkPlayers.put(chunkKey, players);
                        } catch (Exception e) {
                        }
                    }
                }
                plugin.getLogger().info("Loaded " + chunkPlayers.size() + " chunk player mappings");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load redstone data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save data to file
     */
    private void saveData() {
        try {
            dataConfig = new YamlConfiguration();

            ConfigurationSection alerts = dataConfig.createSection("alerts");
            int index = 0;
            for (RedstoneAlert alert : globalAlertHistory) {
                String key = String.valueOf(index++);
                alerts.set(key + ".world", alert.getWorld());
                alerts.set(key + ".x", alert.getX());
                alerts.set(key + ".y", alert.getY());
                alerts.set(key + ".z", alert.getZ());
                alerts.set(key + ".count", alert.getCount());
                alerts.set(key + ".limit", alert.getLimit());
                alerts.set(key + ".nearPlayers", alert.getNearPlayers());
                alerts.set(key + ".timestamp", alert.getTimestamp());
            }

            ConfigurationSection placements = dataConfig.createSection("placements");
            int count = 0;
            for (Map.Entry<String, UUID> entry : blockPlacements.entrySet()) {
                if (count++ > 10000)
                    break;
                placements.set(entry.getKey(), entry.getValue().toString());
            }

            ConfigurationSection chunks = dataConfig.createSection("chunk-players");
            for (Map.Entry<Long, Set<UUID>> entry : chunkPlayers.entrySet()) {
                List<String> uuidList = new ArrayList<>();
                for (UUID uuid : entry.getValue()) {
                    uuidList.add(uuid.toString());
                }
                chunks.set(String.valueOf(entry.getKey()), uuidList);
            }

            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save redstone data: " + e.getMessage());
        }
    }

    /**
     * Notify all nearby players that their area has been disabled
     * Folia-Safe Implementation
     */
    private void notifyNearbyPlayers(Chunk chunk, int seconds) {
        long now = System.currentTimeMillis();
        long cooldown = 30000;
        int maxDist = 48;

        org.bukkit.Location chunkCenter = new org.bukkit.Location(
                chunk.getWorld(),
                chunk.getX() * 16 + 8,
                64,
                chunk.getZ() * 16 + 8);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(chunk.getWorld()))
                continue;

            try {
                if (player.getLocation().distance(chunkCenter) <= maxDist) {
                    Long lastNotified = playerNotificationCooldown.get(player.getUniqueId());
                    if (lastNotified != null && now - lastNotified < cooldown)
                        continue;

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7Your area has been flagged by &fFalcon\n&7A high redstone threshold has been detected your redstones have been disabled for &c"
                                    + seconds + "s."));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    playerNotificationCooldown.put(player.getUniqueId(), now);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Update live GUI countdown timers every second
     */
    private void updateLiveGUI() {
        for (UUID uuid : new HashSet<>(guiViewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                guiViewers.remove(uuid);
                continue;
            }

            String title = player.getOpenInventory().getTitle();
            if (!title.contains("ʀᴇᴅѕᴛᴏɴᴇ ᴍᴀɴᴀɢᴇʀ")) {
                guiViewers.remove(uuid);
                continue;
            }

            org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
            List<RedstoneAlert> recentAlerts = new ArrayList<>(globalAlertHistory);
            recentAlerts.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

            for (int slot = 0; slot < 45 && slot < recentAlerts.size(); slot++) {
                RedstoneAlert alert = recentAlerts.get(slot);
                org.bukkit.inventory.ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() == Material.REDSTONE) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        for (int i = 0; i < lore.size(); i++) {
                            if (lore.get(i).contains("Time Ago:")) {
                                lore.set(i, color("&fTime Ago: " + formatTimeAgo(alert.getTimestamp())));
                                break;
                            }
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    /**
     * Event Handler: Track redstone block placements
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (REDSTONE_COMPONENTS.contains(event.getBlock().getType())) {
            recordBlockPlacement(event.getBlock().getLocation(), event.getPlayer().getUniqueId());
        }
    }

    /**
     * Event Handler: Detects redstone updates.
     */
    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        UUID worldId = chunk.getWorld().getUID();
        long chunkKey = getChunkKey(chunk);

        if (isChunkDisabled(worldId, chunkKey)) {
            event.setNewCurrent(0);
            if (event.getOldCurrent() > 0)
                event.setNewCurrent(0);
            return;
        }

        incrementCount(chunk);
    }

    /**
     * Event Handler: Detects piston extensions.
     */
    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        UUID worldId = chunk.getWorld().getUID();
        long chunkKey = getChunkKey(chunk);

        if (isChunkDisabled(worldId, chunkKey)) {
            event.setCancelled(true);
            return;
        }
        incrementCount(chunk);
    }

    /**
     * Event Handler: Detects piston retractions.
     */
    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        UUID worldId = chunk.getWorld().getUID();
        long chunkKey = getChunkKey(chunk);

        if (isChunkDisabled(worldId, chunkKey)) {
            event.setCancelled(true);
            return;
        }
        incrementCount(chunk);
    }

    /**
     * Event Handler: Detects physics updates.
     */
    @EventHandler
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        if (temporarilyDisabledChunks.isEmpty())
            return;

        if (!REDSTONE_COMPONENTS.contains(event.getBlock().getType())) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        if (isChunkDisabled(chunk.getWorld().getUID(), getChunkKey(chunk))) {
            event.setCancelled(true);
        }
    }

    /**
     * Opens a GUI showing redstone status for the current chunk
     */
    public void openRedstoneGUI(Player player) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, color("&8ʀᴇᴅѕᴛᴏɴᴇ ᴍᴀɴᴀɢᴇʀ"));

        List<RedstoneAlert> recentAlerts = new ArrayList<>(globalAlertHistory);
        recentAlerts.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        int slot = 0;
        for (RedstoneAlert alert : recentAlerts) {
            if (slot >= 45)
                break;

            org.bukkit.inventory.ItemStack alertItem = new org.bukkit.inventory.ItemStack(Material.REDSTONE);
            org.bukkit.inventory.meta.ItemMeta alertMeta = alertItem.getItemMeta();
            if (alertMeta != null) {
                alertMeta.setDisplayName(color("&cRedstone High Usage"));
                List<String> lore = new ArrayList<>();
                lore.add(color("&fNear Players: " + alert.getNearPlayers()));
                lore.add(color("&fThreshold: " + alert.getCount()));
                lore.add(color("&fTime Ago: " + formatTimeAgo(alert.getTimestamp())));
                lore.add(color("&fLocation: &c" + alert.getWorld() + " " + alert.getX() + ", " + alert.getY() + ", "
                        + alert.getZ()));
                lore.add(color("&7(Click to teleport)"));
                alertMeta.setLore(lore);
                alertItem.setItemMeta(alertMeta);
            }
            gui.setItem(slot++, alertItem);
        }

        org.bukkit.inventory.ItemStack refresh = new org.bukkit.inventory.ItemStack(Material.REDSTONE);
        org.bukkit.inventory.meta.ItemMeta refreshMeta = refresh.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName(color("&7ʀᴇꜰʀᴇѕʜ"));
            refreshMeta.setLore(List.of(color("&fClick to refresh")));
            refresh.setItemMeta(refreshMeta);
        }
        gui.setItem(49, refresh);

        guiViewers.add(player.getUniqueId());
        player.openInventory(gui);
    }

    private String formatTimeAgo(long timestamp) {
        long elapsed = System.currentTimeMillis() - timestamp;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        else if (hours > 0)
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        else if (minutes > 0)
            return minutes + "m " + (seconds % 60) + "s";
        else
            return seconds + "s ago";
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Handle clicks in the Redstone Manager GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("ʀᴇᴅѕᴛᴏɴᴇ ᴍᴀɴᴀɢᴇʀ"))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54)
            return;

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            try {
                player.playSound(player.getLocation(), "minecraft:ui.toast.in", 1f, 1f);
            } catch (Exception e) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }
        }

        if (slot == 49) {
            openRedstoneGUI(player);
            return;
        }

        if (slot >= 0 && slot < 45 && slot < globalAlertHistory.size()) {
            RedstoneAlert alert = globalAlertHistory.get(slot);

            org.bukkit.World w = Bukkit.getWorld(alert.getWorld());
            if (w != null) {
                player.teleportAsync(new org.bukkit.Location(w, alert.getX(), alert.getY(), alert.getZ()))
                        .thenAccept(success -> {
                            if (success) {
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f,
                                        1f);
                                player.sendMessage(color("&7You were teleported to &a" + alert.getX() + ", "
                                        + alert.getY() + ", " + alert.getZ()));
                                player.closeInventory();
                            }
                        });
            } else {
                player.sendMessage(ChatColor.RED + "World not found.");
            }
        }
    }

    /**
     * Remove player from GUI viewers when they close the inventory
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("ʀᴇᴅѕᴛᴏɴᴇ ᴍᴀɴᴀɢᴇʀ")) {
            guiViewers.remove(event.getPlayer().getUniqueId());
        }
    }

    private void broadcastAlert(String prefix, RedstoneAlert alert, int seconds) {
        TextComponent message = new TextComponent("");
        message.addExtra(color("&7" + prefix + " area has been detected on "));

        TextComponent location = new TextComponent(
                color("&a[" + alert.getX() + ", " + alert.getY() + ", " + alert.getZ() + "]"));

        location.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/tp " + alert.getX() + " " + alert.getY() + " " + alert.getZ()));
        location.setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to teleport").create()));

        message.addExtra(location);

        message.addExtra(color(" &4" + alert.getCount() + "/" + alert.getLimit()));
        message.addExtra(color("&7 and temporary disabled for &c" + seconds + "s."));

        broadcastAdmin(message);
    }

    private void broadcastAdmin(TextComponent message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("prism.redstone.admin")) {
                p.spigot().sendMessage(message);
            }
        }
    }

    public static class RedstoneAlert {
        private final String world;
        private final int x;
        private final int y;
        private final int z;
        private final int count;
        private final int limit;
        private final String nearPlayers;
        private final long timestamp;

        public RedstoneAlert(String world, int x, int y, int z, int count, int limit, String nearPlayers,
                long timestamp) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.count = count;
            this.limit = limit;
            this.nearPlayers = nearPlayers;
            this.timestamp = timestamp;
        }

        public String getWorld() {
            return world;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getCount() {
            return count;
        }

        public int getLimit() {
            return limit;
        }

        public String getNearPlayers() {
            return nearPlayers;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}