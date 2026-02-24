package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
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

    private final PrismSurvival plugin;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> activeTasks = new HashMap<>();

    public TeleportManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void startCountdown(Player player, Location target, int seconds, String spawnName) {
        UUID uuid = player.getUniqueId();

        // If teleport is already in progress, deny
        if (activeTasks.containsKey(uuid)) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            return;
        }

        // Start countdown task
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

                // Check movement (ignore small Y changes to allow jumping, but prevent flying)
                Location current = player.getLocation();
                double dist = Math.pow(current.getX() - startLoc.getX(), 2)
                        + Math.pow(current.getZ() - startLoc.getZ(), 2);
                double distY = Math.abs(current.getY() - startLoc.getY());

                if (dist > 0.1 || distY > 1.5) {
                    // Send message to Action Bar to avoid chat spam and match countdown style
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
                            "&7You teleport to &5ѕᴘᴀᴡɴ " + spawnName);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                    player.sendMessage(msg);

                    org.bukkit.scheduler.BukkitTask t = activeTasks.get(uuid);
                    if (t != null) {
                        t.cancel();
                        activeTasks.remove(uuid);
                    }
                    return;
                }

                // Action bar countdown
                String msg = ChatColor.translateAlternateColorCodes('&', "&7Teleporting in &5" + timeLeft + "s");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));

                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                } catch (Throwable ignored) {
                }

                timeLeft--;
            }
        }, 1L, 20L); // Using 1L initial delay as per Folia fix

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

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
