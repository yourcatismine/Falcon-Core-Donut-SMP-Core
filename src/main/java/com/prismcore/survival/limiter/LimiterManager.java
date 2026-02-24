package com.prismcore.survival.limiter;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LimiterManager {

    private final PrismSurvival plugin;
    private final LimiterConfig config;
    private BukkitTask asyncTimerTask;

    // Track loaded chunks (WorldName -> Set of Chunk Keys)
    private final Map<String, Set<Long>> loadedChunks = new ConcurrentHashMap<>();

    public LimiterManager(PrismSurvival plugin, LimiterConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (asyncTimerTask != null) {
            asyncTimerTask.cancel();
        }

        // Run an async task periodcally to schedule the chunk checks on the region
        // scheduler
        asyncTimerTask = plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            for (World world : Bukkit.getWorlds()) {
                Set<Long> chunks = loadedChunks.get(world.getName());
                if (chunks == null || chunks.isEmpty())
                    continue;

                // Group chunks into 8x8 regions (RegionX = chunkX >> 3, RegionZ = chunkZ >> 3)
                Map<Long, List<Long>> regionToChunks = new HashMap<>();

                for (Long chunkKey : chunks) {
                    int x = (int) (long) chunkKey;
                    int z = (int) ((long) chunkKey >> 4); // this was a bug waiting to happen, chunkKey >> 32 but let's
                                                          // fix the bitshifting throughout
                    int realZ = (int) (chunkKey >> 32);

                    int regionX = x >> 3;
                    int regionZ = realZ >> 3;
                    long regionKey = (long) regionX & 0xffffffffL | ((long) regionZ & 0xffffffffL) << 32;

                    regionToChunks.computeIfAbsent(regionKey, k -> new ArrayList<>()).add(chunkKey);
                }

                // Schedule a task for each region to check its chunks collectively
                for (Map.Entry<Long, List<Long>> entry : regionToChunks.entrySet()) {
                    long rKey = entry.getKey();
                    int rx = (int) (long) rKey;
                    int rz = (int) (rKey >> 32);
                    List<Long> regionChunks = entry.getValue();

                    // The center of the region is roughly (rx * 8 + 4) * 16
                    int blockX = (rx * 8 + 4) * 16;
                    int blockZ = (rz * 8 + 4) * 16;

                    plugin.getSchedulerAdapter().runAtLocation(
                            new org.bukkit.Location(world, blockX, 0, blockZ),
                            () -> processRegionLimiter(world, regionChunks));
                }
            }
        }, config.getCheckIntervalTicks(), config.getCheckIntervalTicks());
    }

    public void stop() {
        if (asyncTimerTask != null) {
            asyncTimerTask.cancel();
            asyncTimerTask = null;
        }
    }

    public void shutdown() {
        stop();
        loadedChunks.clear();
    }

    public void reload() {
        config.loadConfig();
        start();
    }

    public LimiterConfig getConfig() {
        return config;
    }

    public void addChunk(Chunk chunk) {
        addChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public void addChunkCoord(String worldName, int x, int z) {
        loadedChunks.computeIfAbsent(worldName, k -> ConcurrentHashMap.newKeySet())
                .add(getChunkKey(x, z));
    }

    public void removeChunk(Chunk chunk) {
        Set<Long> chunks = loadedChunks.get(chunk.getWorld().getName());
        if (chunks != null) {
            chunks.remove(getChunkKey(chunk.getX(), chunk.getZ()));
        }
    }

    private long getChunkKey(int x, int z) {
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }

    private void processRegionLimiter(World world, List<Long> chunks) {
        Map<EntityType, List<Entity>> entityGroups = new HashMap<>();
        List<Item> items = new ArrayList<>();

        for (Long chunkKey : chunks) {
            int chunkX = (int) (long) chunkKey;
            int chunkZ = (int) (chunkKey >> 32);

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player)
                    continue; // Never limit players

                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    if (!config.getIgnoredItems().contains(item.getItemStack().getType())) {
                        items.add(item);
                    }
                    continue;
                }

                if (config.getIgnoredEntityTypes().contains(entity.getType()))
                    continue;

                if (isProtected(entity)) {
                    if (!config.isCleanProtectedIfOverLimit()) {
                        continue; // Skip completely if we aren't cleaning protected entities
                    }
                }

                entityGroups.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
            }
        }

        // Process Items region-wide
        processItems(items, chunks.size());

        // Process Entities
        for (Map.Entry<EntityType, List<Entity>> entry : entityGroups.entrySet()) {
            EntityType type = entry.getKey();
            List<Entity> typeEntities = entry.getValue();

            int baseLimit = config.getCustomEntityLimits().getOrDefault(type, config.getDefaultEntityLimit());
            int limit = (int) (baseLimit * config.getRegionEntityMultiplier() * chunks.size());

            if (typeEntities.size() > limit) {
                int toRemove = typeEntities.size() - limit;

                // Sort entities so we remove the less important ones first (e.g. non-protected,
                // younger)
                typeEntities.sort((e1, e2) -> {
                    boolean p1 = isProtected(e1);
                    boolean p2 = isProtected(e2);
                    if (p1 && !p2)
                        return 1; // e1 is protected, e2 is not. e1 should be kept (higher index)
                    if (!p1 && p2)
                        return -1; // e2 is protected, keep e2
                    return Integer.compare(e1.getTicksLived(), e2.getTicksLived()); // remove newer entities first
                });

                for (int i = 0; i < toRemove; i++) {
                    Entity e = typeEntities.get(i);
                    // Double check if we should remove protected entities
                    if (isProtected(e) && !config.isCleanProtectedIfOverLimit()) {
                        continue;
                    }

                    if (config.isDebugRemovals()) {
                        broadcastDebug("Removed entity " + type.name() + " at " + e.getLocation().getBlockX() + ", "
                                + e.getLocation().getBlockY() + ", " + e.getLocation().getBlockZ());
                    }

                    e.remove();
                }
            }
        }
    }

    private void processItems(List<Item> items, int numChunks) {
        int currentItemCount = 0;

        // Sort items by age (ticks lived) ascending, so we remove the oldest items
        // first
        items.sort(Comparator.comparingInt(Entity::getTicksLived).reversed()); // reversed so oldest are at the
                                                                               // beginning

        int baseLimit = config.getDefaultItemLimit();
        int limit = (int) (baseLimit * config.getRegionItemMultiplier() * numChunks);

        if (config.isCountItemStackAmount()) {
            // Count total items in stacks
            for (Item item : items) {
                int amount = item.getItemStack().getAmount();
                if (currentItemCount + amount > limit) {
                    // We passed the limit, we need to remove this or reduce its stack
                    int overage = (currentItemCount + amount) - limit;
                    if (overage >= amount) {
                        if (config.isDebugRemovals()) {
                            broadcastDebug("Removed " + amount + "x " + item.getItemStack().getType().name() + " at "
                                    + item.getLocation().getBlockX() + ", " + item.getLocation().getBlockY() + ", "
                                    + item.getLocation().getBlockZ());
                        }
                        item.remove();
                    } else {
                        ItemStack stack = item.getItemStack();
                        stack.setAmount(amount - overage);
                        item.setItemStack(stack);

                        if (config.isDebugRemovals()) {
                            broadcastDebug("Reduced " + overage + "x " + item.getItemStack().getType().name() + " at "
                                    + item.getLocation().getBlockX() + ", " + item.getLocation().getBlockY() + ", "
                                    + item.getLocation().getBlockZ());
                        }

                        currentItemCount = limit;
                    }
                } else {
                    currentItemCount += amount;
                }
            }
        } else {
            // Count item entities
            if (items.size() > limit) {
                int toRemove = items.size() - limit;
                for (int i = 0; i < toRemove; i++) {
                    Item item = items.get(i);
                    if (config.isDebugRemovals()) {
                        broadcastDebug("Removed item entity " + item.getItemStack().getType().name() + " at "
                                + item.getLocation().getBlockX() + ", " + item.getLocation().getBlockY() + ", "
                                + item.getLocation().getBlockZ());
                    }
                    item.remove();
                }
            }
        }
    }

    private boolean isProtected(Entity entity) {
        if (config.isProtectNamedEntities() && entity.getCustomName() != null)
            return true;

        if (entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            if (config.isProtectEquippedEntities() && le.getEquipment() != null) {
                // If they picked up any armor or held item
                if (le.getEquipment().getArmorContents().length > 0) {
                    for (ItemStack item : le.getEquipment().getArmorContents()) {
                        if (item != null && item.getType() != org.bukkit.Material.AIR) {
                            // simplistic check, ideally we check if drop chance is 1.0f (picked up)
                            return true;
                        }
                    }
                }
            }
        }

        if (config.isProtectTamedAnimals() && entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed())
                return true;
        }

        if (config.isProtectLeashedEntities() && entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            if (le.isLeashed())
                return true;
        }

        if (config.isProtectBossEntities()
                && (entity instanceof Boss || entity instanceof EnderDragon || entity instanceof Wither)) {
            return true;
        }

        return false;
    }

    private void broadcastDebug(String msg) {
        plugin.getLogger().info("[Limiter-Debug] " + msg);
        String formatted = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8[&cLimiter-Debug&8] &7" + msg);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("prismcore.limiter.debug")) {
                p.sendMessage(formatted);
            }
        }
    }
}
