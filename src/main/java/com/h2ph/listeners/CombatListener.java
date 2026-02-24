package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.EnderPearl;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatListener implements Listener {

    private static CombatListener instance;
    private final PrismSurvival plugin;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> remaining = new ConcurrentHashMap<>();

    private static final int DEFAULT_COMBAT_SECONDS = 20;

    public CombatListener(PrismSurvival plugin) {
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
        if (p.hasPermission("prism.combat.bypass") || p.hasPermission("prism.bypass.combat"))
            return false;
        return remaining.containsKey(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();
        if (!(victim instanceof Player))
            return;

        Player v = (Player) victim;
        Player d = null;

        if (damager instanceof Player) {
            d = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile p = (Projectile) damager;
            // Ignore ender pearl damage (teleportation)
            if (p instanceof EnderPearl)
                return;

            ProjectileSource source = p.getShooter();
            if (source instanceof Player) {
                d = (Player) source;
            }
        }

        // If no player damager found or if damage is self-inflicted, skip
        if (d == null || v.getUniqueId().equals(d.getUniqueId()))
            return;

        // Ignore creative players
        if (v.getGameMode() == GameMode.CREATIVE || d.getGameMode() == GameMode.CREATIVE)
            return;

        // Optional: If AFK region system marks this as a special region, skip
        try {
            if (plugin.getAfkManager() != null) {
                if (plugin.getAfkManager().getRegionAt(v.getLocation()) != null)
                    return;
                if (plugin.getAfkManager().getRegionAt(d.getLocation()) != null)
                    return;
            }
        } catch (Throwable ignored) {
        }

        // Put both players into combat
        startCombatFor(v, DEFAULT_COMBAT_SECONDS);
        startCombatFor(d, DEFAULT_COMBAT_SECONDS);
    }

    private void startCombatFor(Player p, int seconds) {
        UUID uuid = p.getUniqueId();
        remaining.put(uuid, seconds);

        // If a task already exists, just refresh remaining seconds
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

                // Show actionbar countdown
                String msg = ChatColor.translateAlternateColorCodes('&', "&dCombat:&f " + timeLeft + "s");
                try {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                } catch (Throwable ignored) {
                }

                try {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
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

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!remaining.containsKey(uuid))
            return;

        // Bypass permission
        if (p.hasPermission("prism.combat.bypass") || p.hasPermission("prism.bypass.combat"))
            return;

        // Check configured combat-blocked commands
        List<String> blocked = plugin.getSurvivalConfig().getStringList("combat");
        if (blocked == null || blocked.isEmpty())
            return;

        String msg = e.getMessage();
        String root = msg.split(" ")[0].replaceFirst("/", "").toLowerCase();
        // Strip namespace (plugin:command)
        if (root.contains(":")) {
            root = root.substring(root.indexOf(":") + 1);
        }

        final String commandRoot = root;

        if (blocked.stream().anyMatch(c -> c.equalsIgnoreCase(commandRoot))) {
            // Cancel command and show actionbar message
            e.setCancelled(true);
            String bar = ChatColor.translateAlternateColorCodes('&', "&cYou are currently on combat.");
            try {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
            } catch (Throwable ignored) {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!remaining.containsKey(uuid))
            return;

        // Player left while in combat -> kill
        try {
            PlayerData data = plugin.getPlayerDataManager().get(uuid);
            if (data != null) {
                data.setCombatLogged(true);
                // Save immediately to ensure persistence
                plugin.getPlayerDataManager().savePlayer(uuid);
            }
        } catch (Throwable ignored) {
        }

        try {
            p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&cYou Died"),
                    ChatColor.translateAlternateColorCodes('&', "&7You were died from combat logging"), 10, 70, 20);
        } catch (Throwable ignored) {
        }

        try {
            p.setHealth(0.0);
        } catch (Throwable ignored) {
        }

        cancelTask(uuid);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        UUID uuid = p.getUniqueId();

        // If the player dies, ensure combat tag is removed
        if (remaining.containsKey(uuid)) {
            cancelTask(uuid);
        }
    }
}
