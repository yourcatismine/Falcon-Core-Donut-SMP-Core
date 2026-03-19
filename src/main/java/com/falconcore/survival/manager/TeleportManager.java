package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final Falcon plugin;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> activeTasks = new HashMap<>();

    public TeleportManager(Falcon plugin) {
        this.plugin = plugin;
    }

    public void startCountdown(Player player, Location target, int seconds, String spawnName) {
        UUID uuid = player.getUniqueId();

        if (activeTasks.containsKey(uuid)) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            return;
        }

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, new Runnable() {
            int timeLeft = seconds;
            final Location startLoc = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    org.bukkit.scheduler.BukkitTask t = activeTasks.get(uuid);
                    if (t != null) {
                        t.cancel();
                        activeTasks.remove(uuid);
                    }
                    return;
                }

                Location current = player.getLocation();
                double dist = Math.pow(current.getX() - startLoc.getX(), 2)
                        + Math.pow(current.getZ() - startLoc.getZ(), 2);
                double distY = Math.abs(current.getY() - startLoc.getY());

                if (dist > 0.1 || distY > 1.5) {
                    String cancelMsg = ChatColor.RED + "Teleport cancelled because you moved.";
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(cancelMsg));
                    player.sendMessage(cancelMsg);

                    try {
                        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    } catch (Throwable ignored) {
                    }

                    org.bukkit.scheduler.BukkitTask t = activeTasks.get(uuid);
                    if (t != null) {
                        t.cancel();
                        activeTasks.remove(uuid);
                    }
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleportAsync(target);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    String msg = ChatColor.translateAlternateColorCodes('&',
                            "&7You teleport to &dѕᴘᴀᴡɴ " + spawnName);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                    player.sendMessage(msg);

                    org.bukkit.scheduler.BukkitTask t = activeTasks.get(uuid);
                    if (t != null) {
                        t.cancel();
                        activeTasks.remove(uuid);
                    }
                    return;
                }

                String msg = ChatColor.translateAlternateColorCodes('&', "&7Teleporting in &d" + timeLeft + "s");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));

                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                } catch (Throwable ignored) {
                }

                timeLeft--;
            }
        }, 1L, 20L);

        activeTasks.put(uuid, task);
    }

    public void teleport(Player player, Location target, int seconds, String countdownTemplate, String successMsg) {
        UUID uuid = player.getUniqueId();

        if (activeTasks.containsKey(uuid)) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            return;
        }

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, new Runnable() {
            int timeLeft = seconds;
            final Location startLoc = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelTask(uuid);
                    return;
                }

                Location current = player.getLocation();
                double dist = Math.pow(current.getX() - startLoc.getX(), 2)
                        + Math.pow(current.getZ() - startLoc.getZ(), 2);
                double distY = Math.abs(current.getY() - startLoc.getY());

                if (dist > 0.1 || distY > 1.5) {
                    String cancelMsg = color("&cTeleport cancelled because you moved.");
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(cancelMsg));
                    player.sendMessage(cancelMsg);

                    try {
                        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    } catch (Throwable ignored) {
                    }

                    cancelTask(uuid);
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleportAsync(target);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                    if (successMsg != null && !successMsg.isEmpty()) {
                        String msg = color(successMsg);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        player.sendMessage(msg);
                    }

                    cancelTask(uuid);
                    return;
                }

                String msg = color(String.format(countdownTemplate, timeLeft + "s"));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));

                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                } catch (Throwable ignored) {
                }

                timeLeft--;
            }
        }, 1L, 20L);

        activeTasks.put(uuid, task);
    }

    private void cancelTask(UUID uuid) {
        org.bukkit.scheduler.BukkitTask t = activeTasks.remove(uuid);
        if (t != null) {
            t.cancel();
        }
    }

    /**
     * Public method to cancel an active teleport task for a player.
     * Use this when a player joins or leaves to ensure a clean state.
     */
    public void cancelActiveTask(UUID uuid) {
        cancelTask(uuid);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
