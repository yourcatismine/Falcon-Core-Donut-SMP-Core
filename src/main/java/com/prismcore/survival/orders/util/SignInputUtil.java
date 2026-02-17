/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Server
 *  org.bukkit.block.Block
 *  org.bukkit.block.Sign
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.SignChangeEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.prismcore.survival.orders.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SignInputUtil {
    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, JavaPlugin> PLUGINS = new ConcurrentHashMap<UUID, JavaPlugin>();
    private static final Map<UUID, Location> SIGN_LOC = new ConcurrentHashMap<UUID, Location>();
    private static final Map<UUID, BlockData> OLD_DATA = new ConcurrentHashMap<UUID, BlockData>();
    private static final Map<UUID, Integer> INPUT_LINE = new ConcurrentHashMap<UUID, Integer>();
    private static final Map<UUID, Consumer<String>> CALLBACK = new ConcurrentHashMap<UUID, Consumer<String>>();
    private static final Map<UUID, Object> HIDE_TASK = new ConcurrentHashMap<UUID, Object>();
    public static final String META_SIGN_INPUT = "prismorder-sign-input";

    private SignInputUtil() {
    }

    public static void openFromConfig(JavaPlugin plugin, Player player, ConfigurationSection section,
            Consumer<String> callback) {
        int inputLine;
        if (plugin == null || player == null) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        SignInputUtil.ensureRegistered(plugin);
        List<String> lines = section != null ? section.getStringList("lines") : Collections.emptyList();
        int n = inputLine = section != null ? section.getInt("input-line", 1) : 1;
        if (lines == null || lines.isEmpty()) {
            lines = Arrays.asList("", "^^^^^^^^^^", "Input", "");
        }
        SignInputUtil.open(plugin, player, lines, inputLine, callback);
    }

    public static void open(JavaPlugin plugin, Player player, List<String> rawLines, int inputLineOneBased,
            Consumer<String> callback) {
        SignInputUtil.cancel(player);
        if (!player.isOnline()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        UUID uuid = player.getUniqueId();
        int lineIndex = Math.max(0, Math.min(3, inputLineOneBased - 1));
        List lines = new ArrayList<String>();
        if (rawLines != null) {
            lines.addAll(rawLines);
        }
        while (lines.size() < 4) {
            lines.add("");
        }
        if (lines.size() > 4) {
            lines = lines.subList(0, 4);
        }
        String[] plain = new String[4];
        for (int i = 0; i < 4; ++i) {
            String s = (String) lines.get(i);
            if (s == null) {
                s = "";
            }
            s = ChatColor.translateAlternateColorCodes((char) '&', (String) s);
            plain[i] = (s = ChatColor.stripColor((String) s)) == null ? "" : s;
        }
        Placement placement = SignInputUtil.findGroundPlacement(player);
        if (placement == null) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        PLUGINS.put(uuid, plugin);
        SIGN_LOC.put(uuid, placement.loc);
        OLD_DATA.put(uuid, placement.oldData);
        INPUT_LINE.put(uuid, lineIndex);
        if (callback != null) {
            CALLBACK.put(uuid, callback);
        }
        player.setMetadata(META_SIGN_INPUT, (MetadataValue) new FixedMetadataValue((Plugin) plugin, (Object) true));
        Location signLoc = placement.loc.clone();
        BlockData old = placement.oldData;
        Scheduler.runEntity((Plugin) plugin, player, () -> ((Player) player).closeInventory());
        Scheduler.runRegion((Plugin) plugin, signLoc, () -> {
            Block block = signLoc.getBlock();
            block.setType(Material.OAK_SIGN, false);
            if (!(block.getState() instanceof Sign)) {
                SignInputUtil.finish(player, null);
                return;
            }
            Sign sign = (Sign) block.getState();
            boolean usedSide = false;
            try {
                Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
                Method getSide = sign.getClass().getMethod("getSide", sideClass);
                Object front = sideClass.getEnumConstants()[0];
                Object signSide = getSide.invoke((Object) sign, front);
                Method setLine = signSide.getClass().getMethod("setLine", Integer.TYPE, String.class);
                for (int i = 0; i < 4; ++i) {
                    setLine.invoke(signSide, i, plain[i]);
                }
                usedSide = true;
            } catch (Throwable sideClass) {
                // empty catch block
            }
            if (!usedSide) {
                for (int i = 0; i < 4; ++i) {
                    try {
                        sign.setLine(i, plain[i]);
                        continue;
                    } catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
            try {
                Method editable = sign.getClass().getMethod("setEditable", Boolean.TYPE);
                editable.invoke((Object) sign, true);
            } catch (Throwable editable) {
                // empty catch block
            }
            try {
                Method allowed = sign.getClass().getMethod("setAllowedEditorUniqueId", UUID.class);
                allowed.invoke((Object) sign, uuid);
            } catch (Throwable throwable) {
                // empty catch block
            }
            sign.update(true, false);
            Scheduler.runEntity((Plugin) plugin, player, () -> {
                try {
                    player.sendBlockChange(signLoc, sign.getBlockData());
                } catch (Throwable throwable) {
                    // empty catch block
                }
            });
            SignInputUtil.startHideFromOthers(plugin, player, signLoc, old);
            Scheduler.runRegionLater((Plugin) plugin, signLoc, 2L, () -> {
                Block b2 = signLoc.getBlock();
                if (!(b2.getState() instanceof Sign)) {
                    SignInputUtil.finish(player, null);
                    return;
                }
                Sign signState = (Sign) b2.getState();
                Scheduler.runEntity((Plugin) plugin, player, () -> {
                    try {
                        player.openSign(signState);
                    } catch (Throwable t) {
                        SignInputUtil.finish(player, null);
                    }
                });
            });
        });
    }

    public static void cancel(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        JavaPlugin plugin = PLUGINS.remove(uuid);
        Object task = HIDE_TASK.remove(uuid);
        Scheduler.cancelAny(task);
        Location loc = SIGN_LOC.remove(uuid);
        BlockData old = OLD_DATA.remove(uuid);
        INPUT_LINE.remove(uuid);
        Consumer<String> cb = CALLBACK.remove(uuid);
        if (plugin != null) {
            player.removeMetadata(META_SIGN_INPUT, (Plugin) plugin);
        }
        if (plugin != null && loc != null && old != null) {
            Location restore = loc.clone();
            Scheduler.runRegion((Plugin) plugin, restore, () -> {
                Block b = restore.getBlock();
                b.setBlockData(old, false);
                Scheduler.runEntity((Plugin) plugin, player, () -> {
                    try {
                        player.sendBlockChange(restore, old);
                    } catch (Throwable throwable) {
                        // empty catch block
                    }
                });
                SignInputUtil.sendOriginalToOthers(plugin, player, restore, old);
            });
        }
    }

    private static void finish(Player player, String value) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        JavaPlugin plugin = PLUGINS.remove(uuid);
        Object task = HIDE_TASK.remove(uuid);
        Scheduler.cancelAny(task);
        Location loc = SIGN_LOC.remove(uuid);
        BlockData old = OLD_DATA.remove(uuid);
        INPUT_LINE.remove(uuid);
        Consumer<String> cb = CALLBACK.remove(uuid);
        if (plugin != null) {
            player.removeMetadata(META_SIGN_INPUT, (Plugin) plugin);
        }
        if (plugin != null && loc != null && old != null) {
            Location restore = loc.clone();
            Scheduler.runRegion((Plugin) plugin, restore, () -> {
                Block b = restore.getBlock();
                b.setBlockData(old, false);
                Scheduler.runEntity((Plugin) plugin, player, () -> {
                    try {
                        player.sendBlockChange(restore, old);
                    } catch (Throwable throwable) {
                        // empty catch block
                    }
                });
                SignInputUtil.sendOriginalToOthers(plugin, player, restore, old);
                if (cb != null) {
                    Scheduler.runEntity((Plugin) plugin, player, () -> cb.accept(value));
                }
            });
            return;
        }
        if (cb != null) {
            cb.accept(value);
        }
    }

    private static Placement findGroundPlacement(Player p) {
        Location base = p.getLocation();
        int y = base.getBlockY();
        List<int[]> offsets = Arrays.asList(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 2, 0 },
                new int[] { -1, 0 }, new int[] { -2, 0 }, new int[] { 0, 1 }, new int[] { 0, 2 }, new int[] { 0, -1 },
                new int[] { 0, -2 }, new int[] { 1, 1 }, new int[] { 1, -1 }, new int[] { -1, 1 }, new int[] { -1, -1 },
                new int[] { 2, 1 }, new int[] { 2, -1 }, new int[] { -2, 1 }, new int[] { -2, -1 });
        int bx = base.getBlockX();
        int bz = base.getBlockZ();
        for (int[] off : offsets) {
            Block below;
            int x = bx + off[0];
            int z = bz + off[1];
            Location cand = new Location(base.getWorld(), (double) x, (double) y, (double) z);
            Block b = cand.getBlock();
            // Relaxed check: Allow placement in air/water, don't require solid ground
            // below.
            // This allows flying players and swimming players to use the sign.
            // We interact with the specific block via packets mostly anyway.
            if (!SignInputUtil.isReplaceable(b.getType()) || cand.distanceSquared(base) > 9.0)
                continue;
            return new Placement(b.getLocation(), b.getBlockData());
        }
        return null;
    }

    private static boolean isReplaceable(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR || m == Material.WATER
                || m == Material.LAVA;
    }

    private static void startHideFromOthers(JavaPlugin plugin, Player owner, Location loc, BlockData originalData) {
        UUID uuid = owner.getUniqueId();
        SignInputUtil.sendOriginalToOthers(plugin, owner, loc, originalData);
        Object task = Scheduler.runGlobalAtFixedRate((Plugin) plugin, () -> {
            if (!owner.isOnline()) {
                return;
            }
            SignInputUtil.sendOriginalToOthers(plugin, owner, loc, originalData);
        }, 0L, 1L);
        if (task != null) {
            HIDE_TASK.put(uuid, task);
        }
        Scheduler.runGlobalLater((Plugin) plugin, () -> {
            Object t = HIDE_TASK.remove(uuid);
            Scheduler.cancelAny(t);
        }, 10L);
    }

    private static void sendOriginalToOthers(JavaPlugin plugin, Player owner, Location loc, BlockData originalData) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == null || !other.isOnline() || other.equals((Object) owner) || other.getWorld() == null
                    || loc.getWorld() == null || !other.getWorld().equals((Object) loc.getWorld()))
                continue;
            Scheduler.runEntity((Plugin) plugin, other, () -> {
                try {
                    other.sendBlockChange(loc, originalData);
                } catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }
    }

    private static void ensureRegistered(JavaPlugin plugin) {
        String key = plugin.getName();
        if (REGISTERED.add(key)) {
            Bukkit.getPluginManager().registerEvents((Listener) new InternalListener(), (Plugin) plugin);
        }
    }

    private static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        if (!a.getWorld().equals((Object) b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private static final class Placement {
        final Location loc;
        final BlockData oldData;

        Placement(Location loc, BlockData oldData) {
            this.loc = loc;
            this.oldData = oldData;
        }
    }

    private static final class Scheduler {
        private Scheduler() {
        }

        private static boolean isFolia() {
            try {
                Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler", new Class[0]);
                Bukkit.getServer().getClass().getMethod("getRegionScheduler", new Class[0]);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static void runEntity(Plugin plugin, Player player, Runnable run) {
            if (plugin == null || player == null || run == null) {
                return;
            }
            if (Scheduler.isFolia()) {
                try {
                    Object sched = player.getClass().getMethod("getScheduler", new Class[0]).invoke((Object) player,
                            new Object[0]);
                    Method runMethod = sched.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                    Consumer<Object> c = ignored -> run.run();
                    runMethod.invoke(sched, plugin, c, null);
                    return;
                } catch (Throwable throwable) {
                    // empty catch block
                }
            }
            Bukkit.getScheduler().runTask(plugin, run);
        }

        static void runRegion(Plugin plugin, Location loc, Runnable run) {
            if (plugin == null || loc == null || run == null) {
                return;
            }
            if (Scheduler.isFolia()) {
                try {
                    Server server = Bukkit.getServer();
                    Object region = server.getClass().getMethod("getRegionScheduler", new Class[0])
                            .invoke((Object) server, new Object[0]);
                    Method mRun = region.getClass().getMethod("run", Plugin.class, Location.class, Consumer.class);
                    Consumer<Object> c = ignored -> run.run();
                    mRun.invoke(region, plugin, loc, c);
                    return;
                } catch (Throwable throwable) {
                    // empty catch block
                }
            }
            Bukkit.getScheduler().runTask(plugin, run);
        }

        static void runRegionLater(Plugin plugin, Location loc, long delayTicks, Runnable run) {
            if (plugin == null || loc == null || run == null) {
                return;
            }
            if (Scheduler.isFolia()) {
                try {
                    Server server = Bukkit.getServer();
                    Object region = server.getClass().getMethod("getRegionScheduler", new Class[0])
                            .invoke((Object) server, new Object[0]);
                    Method mRunDelayed = region.getClass().getMethod("runDelayed", Plugin.class, Location.class,
                            Consumer.class, Long.TYPE);
                    Consumer<Object> c = ignored -> run.run();
                    mRunDelayed.invoke(region, plugin, loc, c, delayTicks);
                    return;
                } catch (Throwable throwable) {
                    // empty catch block
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, run, delayTicks);
        }

        static void runGlobalLater(Plugin plugin, Runnable run, long delayTicks) {
            if (plugin == null || run == null) {
                return;
            }
            if (Scheduler.isFolia()) {
                try {
                    Server server = Bukkit.getServer();
                    Object global = server.getClass().getMethod("getGlobalRegionScheduler", new Class[0])
                            .invoke((Object) server, new Object[0]);
                    Method mRunDelayed = global.getClass().getMethod("runDelayed", Plugin.class, Consumer.class,
                            Long.TYPE);
                    Consumer<Object> c = ignored -> run.run();
                    mRunDelayed.invoke(global, plugin, c, delayTicks);
                } catch (Throwable throwable) {
                    // empty catch block
                }
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, run, delayTicks);
        }

        static Object runGlobalAtFixedRate(Plugin plugin, Runnable run, long delayTicks, long periodTicks) {
            if (plugin == null || run == null) {
                return null;
            }
            if (Scheduler.isFolia()) {
                try {
                    Server server = Bukkit.getServer();
                    Object global = server.getClass().getMethod("getGlobalRegionScheduler", new Class[0])
                            .invoke((Object) server, new Object[0]);
                    Method mFixed = global.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class,
                            Long.TYPE, Long.TYPE);
                    Consumer<Object> c = ignored -> run.run();
                    return mFixed.invoke(global, plugin, c, delayTicks, periodTicks);
                } catch (Throwable ignored2) {
                    return null;
                }
            }
            try {
                return Bukkit.getScheduler().runTaskTimer(plugin, run, delayTicks, periodTicks);
            } catch (Throwable t) {
                return null;
            }
        }

        static void cancelAny(Object task) {
            if (task == null) {
                return;
            }
            if (task instanceof BukkitTask) {
                BukkitTask bt = (BukkitTask) task;
                try {
                    bt.cancel();
                } catch (Throwable throwable) {
                    // empty catch block
                }
                return;
            }
            try {
                Method cancel = task.getClass().getMethod("cancel", new Class[0]);
                cancel.invoke(task, new Object[0]);
            } catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private static final class InternalListener
            implements Listener {
        private InternalListener() {
        }

        @EventHandler
        public void onSignChange(SignChangeEvent event) {
            String raw;
            Player p = event.getPlayer();
            UUID uuid = p.getUniqueId();
            Location expected = SIGN_LOC.get(uuid);
            if (expected == null) {
                return;
            }
            if (!SignInputUtil.sameBlock(event.getBlock().getLocation(), expected)) {
                return;
            }
            int idx = INPUT_LINE.getOrDefault(uuid, 0);
            try {
                raw = event.getLine(idx);
            } catch (Throwable t) {
                raw = "";
            }
            if (raw == null) {
                raw = "";
            }
            if ((raw = ChatColor.stripColor((String) raw)) == null) {
                raw = "";
            }
            raw = raw.trim();
            SignInputUtil.finish(p, raw);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            SignInputUtil.cancel(event.getPlayer());
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (SIGN_LOC.containsValue(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onBlockExplode(BlockExplodeEvent event) {
            event.blockList().removeIf(block -> SIGN_LOC.containsValue(block.getLocation()));
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            event.blockList().removeIf(block -> SIGN_LOC.containsValue(block.getLocation()));
        }

        @EventHandler
        public void onPistonExtend(BlockPistonExtendEvent event) {
            for (Block block : event.getBlocks()) {
                if (SIGN_LOC.containsValue(block.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        @EventHandler
        public void onPistonRetract(BlockPistonRetractEvent event) {
            for (Block block : event.getBlocks()) {
                if (SIGN_LOC.containsValue(block.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        @EventHandler
        public void onBlockBurn(BlockBurnEvent event) {
            if (SIGN_LOC.containsValue(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onBlockFromTo(BlockFromToEvent event) {
            if (SIGN_LOC.containsValue(event.getToBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}
