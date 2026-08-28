package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import com.falconcore.survival.utils.ItemSerializationManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CombatListener implements Listener, CommandExecutor, TabCompleter {

    private static CombatListener instance;
    private final Falcon plugin;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> remaining = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> explosiveEntityOwners = new ConcurrentHashMap<>();
    private final List<ExplosionTrigger> explosionTriggers = new CopyOnWriteArrayList<>();

    private static final int DEFAULT_COMBAT_SECONDS = 20;
    private static final long EXPLOSION_TRACK_MILLIS = 4_000;
    private static final double EXPLOSION_SEARCH_RADIUS_SQ = 64.0;

    public CombatListener(Falcon plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static CombatListener getInstance() {
        return instance;
    }

    /**
     * Check if a player is currently in combat tags.
     * 
     * @param p The player to check.
     * @return true if the player is in combat and doesn't bypass it.
     */
    public boolean isInCombat(Player p) {
        if (p == null)
            return false;
        if (p.hasPermission("falcon.combat.bypass") || p.hasPermission("falcon.combat.bypass"))
            return false;
        return remaining.containsKey(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        trackExplosiveSource(e);

        Entity victim = e.getEntity();
        if (!(victim instanceof Player))
            return;

        Player v = (Player) victim;
        Player d = resolvePlayerDamager(e.getDamager());
        if (d == null || v.getUniqueId().equals(d.getUniqueId()))
            return;

        if (!canApplyCombat(v, d))
            return;

        startCombatFor(v, DEFAULT_COMBAT_SECONDS);
        startCombatFor(d, DEFAULT_COMBAT_SECONDS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim))
            return;

        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
            return;

        Player attacker = findExplosionAttacker(victim.getLocation());
        if (attacker == null || victim.getUniqueId().equals(attacker.getUniqueId()))
            return;

        if (!canApplyCombat(victim, attacker))
            return;

        startCombatFor(victim, DEFAULT_COMBAT_SECONDS);
        startCombatFor(attacker, DEFAULT_COMBAT_SECONDS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRespawnAnchorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR)
            return;

        registerExplosionTrigger(event.getPlayer(), block.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR)
            return;

        ProjectileSource source = event.getEntity().getShooter();
        if (source instanceof Player player)
            registerExplosionTrigger(player, block.getLocation());
    }

    private void trackExplosiveSource(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderCrystal crystal))
            return;

        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker == null)
            return;

        registerExplosiveEntityOwner(crystal, attacker);
    }

    private void registerExplosiveEntityOwner(EnderCrystal crystal, Player attacker) {
        UUID entityId = crystal.getUniqueId();
        explosiveEntityOwners.put(entityId, attacker.getUniqueId());
        registerExplosionTrigger(attacker, crystal.getLocation());
        plugin.getSchedulerAdapter().runTaskLater(() -> explosiveEntityOwners.remove(entityId), 60L);
    }

    private void registerExplosionTrigger(Player player, Location location) {
        if (player == null || location == null)
            return;

        Location tracked = location.clone();
        tracked.setPitch(0);
        tracked.setYaw(0);
        ExplosionTrigger trigger = new ExplosionTrigger(player.getUniqueId(), tracked,
                System.currentTimeMillis() + EXPLOSION_TRACK_MILLIS);
        explosionTriggers.add(trigger);
        plugin.getSchedulerAdapter().runTaskLater(() -> explosionTriggers.remove(trigger), 100L);
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player)
            return player;

        if (damager instanceof Projectile projectile) {
            if (projectile instanceof EnderPearl)
                return null;

            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player)
                return (Player) source;
        }

        if (damager instanceof EnderCrystal crystal) {
            UUID ownerId = explosiveEntityOwners.get(crystal.getUniqueId());
            if (ownerId != null)
                return Bukkit.getPlayer(ownerId);
        }

        return null;
    }

    private Player findExplosionAttacker(Location location) {
        if (location == null)
            return null;

        long now = System.currentTimeMillis();
        ExplosionTrigger closest = null;
        double bestDistance = Double.MAX_VALUE;

        for (ExplosionTrigger trigger : explosionTriggers) {
            if (trigger.expiresAt < now) {
                explosionTriggers.remove(trigger);
                continue;
            }

            if (!trigger.location.getWorld().equals(location.getWorld()))
                continue;

            double distance = trigger.location.distanceSquared(location);
            if (distance > EXPLOSION_SEARCH_RADIUS_SQ)
                continue;

            if (distance < bestDistance) {
                bestDistance = distance;
                closest = trigger;
            }
        }

        if (closest == null)
            return null;

        explosionTriggers.remove(closest);
        return Bukkit.getPlayer(closest.attacker);
    }

    private boolean canApplyCombat(Player victim, Player attacker) {
        if (victim.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.CREATIVE)
            return false;

        try {
            if (plugin.getAfkManager() != null) {
                if (plugin.getAfkManager().getRegionAt(victim.getLocation()) != null)
                    return false;
                if (plugin.getAfkManager().getRegionAt(attacker.getLocation()) != null)
                    return false;
            }
        } catch (Throwable ignored) {
        }

        return true;
    }

    public void startCombatFor(Player p, int seconds) {
        UUID uuid = p.getUniqueId();
        remaining.put(uuid, seconds);

        if (tasks.containsKey(uuid)) {
            return;
        }

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(p, new Runnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (!p.isOnline() || p.getGameMode() == GameMode.CREATIVE) {
                    cancelTask(uuid);
                    return;
                }

                Integer r = remaining.getOrDefault(uuid, timeLeft);
                timeLeft = r;

                if (timeLeft <= 0) {
                    cancelTask(uuid);
                    return;
                }

                String msg = ChatColor.translateAlternateColorCodes('&', "&dCombat:&f " + timeLeft + "s");
                try {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                } catch (Throwable ignored) {
                }

                remaining.put(uuid, timeLeft - 1);

            }
        }, 1L, 20L);

        tasks.put(uuid, task);
    }

    private void cancelTask(UUID uuid) {
        org.bukkit.scheduler.BukkitTask t = tasks.remove(uuid);
        if (t != null)
            t.cancel();
        remaining.remove(uuid);
    }

    private static final class ExplosionTrigger {
        private final UUID attacker;
        private final Location location;
        private final long expiresAt;

        private ExplosionTrigger(UUID attacker, Location location, long expiresAt) {
            this.attacker = attacker;
            this.location = location;
            this.expiresAt = expiresAt;
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!remaining.containsKey(uuid))
            return;

        if (p.hasPermission("falcon.combat.bypass") || p.hasPermission("falcon.combat.bypass"))
            return;

        List<String> blocked = plugin.getSurvivalConfig().getStringList("combat");
        if (blocked == null || blocked.isEmpty())
            return;

        String msg = e.getMessage();
        String root = msg.split(" ")[0].replaceFirst("/", "").toLowerCase();
        if (root.contains(":")) {
            root = root.substring(root.indexOf(":") + 1);
        }

        final String commandRoot = root;

        if (blocked.stream().anyMatch(c -> c.equalsIgnoreCase(commandRoot))) {
            e.setCancelled(true);
            String bar = ChatColor.translateAlternateColorCodes('&', "&cYou are currently on combat.");
            try {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
            } catch (Throwable ignored) {
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!remaining.containsKey(uuid))
            return;

        try {
            PlayerData data = plugin.getPlayerDataManager().get(uuid);
            if (data != null) {
                data.setCombatLogged(true);
            }
        } catch (Throwable ignored) {
        }

        try {
            p.setHealth(0.0);
        } catch (Throwable ignored) {
        }

        try {
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.getInventory().setItemInOffHand(null);
        } catch (Throwable ignored) {
        }

        try {
            String emptyInv = ItemSerializationManager.itemStackArrayToBase64(new ItemStack[0]);
            String emptyArmor = ItemSerializationManager.itemStackArrayToBase64(new ItemStack[0]);
            plugin.getDatabaseManager().saveInventory(uuid, emptyInv, emptyArmor);
        } catch (Throwable ignored) {
        }

        try {
            plugin.getPlayerDataManager().savePlayerAsync(uuid);
        } catch (Throwable ignored) {
        }

        cancelTask(uuid);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        UUID uuid = p.getUniqueId();

        if (remaining.containsKey(uuid)) {
            cancelTask(uuid);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("testcombat")) {
            return false;
        }

        if (!sender.hasPermission("falcon.testcombat") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player target;
        int seconds = DEFAULT_COMBAT_SECONDS;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /testcombat [seconds] or /testcombat <player> [seconds]");
                return true;
            }
            target = (Player) sender;
        } else if (args.length == 1) {
            try {
                seconds = Integer.parseInt(args[0]);
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Usage: /testcombat <player> [seconds]");
                    return true;
                }
                target = (Player) sender;
            } catch (NumberFormatException e) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                    return true;
                }
            }
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            try {
                seconds = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid seconds: " + args[1]);
                return true;
            }
        }

        if (seconds <= 0) {
            sender.sendMessage(ChatColor.RED + "Seconds must be greater than 0.");
            return true;
        }

        startCombatFor(target, seconds);
        sender.sendMessage(ChatColor.GREEN + "Triggered combat countdown (" + seconds + "s) for " + target.getName() + ".");
        if (target.getGameMode() == GameMode.CREATIVE) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + target.getName() + " is in Creative mode (combat countdown ends immediately in Creative).");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("falcon.testcombat") && !sender.isOp()) {
            return completions;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
            for (String sec : List.of("5", "10", "15", "20", "30", "60")) {
                if (sec.startsWith(input)) {
                    completions.add(sec);
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            for (String sec : List.of("5", "10", "15", "20", "30", "60")) {
                if (sec.startsWith(input)) {
                    completions.add(sec);
                }
            }
        }

        return completions;
    }
}
