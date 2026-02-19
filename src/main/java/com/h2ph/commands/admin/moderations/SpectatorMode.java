package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpectatorMode implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    // Stores the UUIDs of staff currently in Silent Spectator
    private final Set<UUID> spectators = new HashSet<>();
    // Stores the previous GameMode of the player to restore it later
    private final java.util.Map<UUID, GameMode> previousGamemodes = new java.util.HashMap<>();

    public SpectatorMode(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;

        if (!player.hasPermission("prism.admin.spectator")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (spectators.contains(player.getUniqueId())) {
            disableSpectator(player);
        } else {
            enableSpectator(player);
        }
        return true;
    }

    private void enableSpectator(Player player) {
        spectators.add(player.getUniqueId());
        // Save previous gamemode before switching
        previousGamemodes.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);

        // Hide from ALL current players immediately
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("prism.admin.spectator")) {
                online.hidePlayer(plugin, player);
            }
        }

        String message = "&7You set your gamemode to &aSpectator&7 mode.";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message));
    }

    private void disableSpectator(Player player) {
        spectators.remove(player.getUniqueId());

        // Restore previous gamemode if available, otherwise default to Survival
        GameMode previous = previousGamemodes.remove(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Show to ALL players again
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        String message = "&7You set your gamemode to &aSURVIVAL&7 mode.";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message));
    }

    // --- THE FIX: REJOIN HANDLING ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();

        // 1. If the JOINING player is a staff member who was in spectator, ensure they
        // stay hidden
        if (spectators.contains(joinedPlayer.getUniqueId())) {
            // Re-hide them from everyone just in case
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("prism.admin.spectator")) {
                    online.hidePlayer(plugin, joinedPlayer);
                }
            }
            return;
        }

        // 2. THE BUG FIX:
        // We run this task 10 ticks (0.5 seconds) LATER.
        // This ensures the player is fully connected and their Tab List is built
        // BEFORE we try to remove the spectators from it.
        if (plugin instanceof PrismSurvival) {
            ((PrismSurvival) plugin).getSchedulerAdapter().runTaskLater(() -> {
                // Iterate through all active silent spectators
                for (UUID uuid : spectators) {
                    Player hiddenStaff = Bukkit.getPlayer(uuid);
                    // If staff is online and the new player shouldn't see them...
                    if (hiddenStaff != null && !joinedPlayer.hasPermission("prism.admin.spectator")) {
                        joinedPlayer.hidePlayer(plugin, hiddenStaff);
                    }
                }
            }, 10L);
        } else {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    // Iterate through all active silent spectators
                    for (UUID uuid : spectators) {
                        Player hiddenStaff = Bukkit.getPlayer(uuid);

                        // If staff is online and the new player shouldn't see them...
                        if (hiddenStaff != null && !joinedPlayer.hasPermission("prism.admin.spectator")) {
                            joinedPlayer.hidePlayer(plugin, hiddenStaff);
                        }
                    }
                }
            }.runTaskLater(plugin, 10L); // 10 Ticks delay
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Optional: Remove them from the list if they quit, or keep them for when they
        // rejoin.
        // Usually, it's safer to remove them to prevent memory leaks,
        // but if you want them to STAY silent on rejoin, remove this line.
        spectators.remove(event.getPlayer().getUniqueId());
        previousGamemodes.remove(event.getPlayer().getUniqueId());
    }
}
