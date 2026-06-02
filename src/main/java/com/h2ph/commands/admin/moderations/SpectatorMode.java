package com.h2ph.commands.admin.moderations;

import com.h2ph.Falcon;
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
    private final Set<UUID> spectators = new HashSet<>();
    private final java.util.Map<UUID, GameMode> previousGamemodes = new java.util.HashMap<>();

    public SpectatorMode(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;

        if (!player.hasPermission("falcon.spectator")) {
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
        previousGamemodes.put(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("falcon.spectator")) {
                online.hidePlayer(plugin, player);
            }
        }

        String message = "&7You set your gamemode to &aSpectator&7 mode.";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message));
    }

    private void disableSpectator(Player player) {
        spectators.remove(player.getUniqueId());

        GameMode previous = previousGamemodes.remove(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        String message = "&7You set your gamemode to &aSURVIVAL&7 mode.";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message));
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();

        if (spectators.contains(joinedPlayer.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("falcon.spectator")) {
                    online.hidePlayer(plugin, joinedPlayer);
                }
            }
            return;
        }

        if (plugin instanceof Falcon) {
            ((Falcon) plugin).getSchedulerAdapter().runTaskLater(() -> {
                for (UUID uuid : spectators) {
                    Player hiddenStaff = Bukkit.getPlayer(uuid);
                    if (hiddenStaff != null && !joinedPlayer.hasPermission("falcon.spectator")) {
                        joinedPlayer.hidePlayer(plugin, hiddenStaff);
                    }
                }
            }, 10L);
        } else {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    for (UUID uuid : spectators) {
                        Player hiddenStaff = Bukkit.getPlayer(uuid);

                        if (hiddenStaff != null && !joinedPlayer.hasPermission("falcon.spectator")) {
                            joinedPlayer.hidePlayer(plugin, hiddenStaff);
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spectators.remove(event.getPlayer().getUniqueId());
        previousGamemodes.remove(event.getPlayer().getUniqueId());
    }
}
