package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FlyCommand implements CommandExecutor, TabCompleter, Listener {

    private final PrismSurvival plugin;

    public FlyCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prismsmp.fly")) {
            return true;
        }

        if (!isFlightAllowed(player)) {
            return true;
        }

        boolean flight = !player.getAllowFlight();
        player.setAllowFlight(flight);
        player.setFlying(flight);

        return true;
    }

    // MONITOR priority = runs after ALL other plugins (including Multiverse) have processed the event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // In Folia the player's region context is still transitioning during this event,
        // so immediate setAllowFlight/setFlying calls can be silently ignored.
        // Schedule enforcement on the entity's own thread at 1 and 5 ticks to run
        // after Multiverse has also finished its own next-tick world-apply tasks.
        if (!isFlightAllowed(player)) {
            enforceFlightLater(player, 1L);
            enforceFlightLater(player, 5L);
        }
    }

    // Catches cross-world teleports (e.g. /mvtp, portals) before PlayerChangedWorldEvent
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        String destWorld = to.getWorld().getName();
        List<String> disabledWorlds = plugin.getSurvivalConfig().getStringList("disabled-fly");
        boolean destBlacklisted = disabledWorlds.stream().anyMatch(w -> w.equalsIgnoreCase(destWorld));

        if (destBlacklisted && player.getGameMode() != GameMode.CREATIVE) {
            enforceFlightLater(player, 1L);
            enforceFlightLater(player, 5L);
        }
    }

    // Catches direct flight-toggle attempts (double-jump) in blacklisted worlds
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (event.isFlying() && !isFlightAllowed(player)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
        }
    }

    private void enforceFlightLater(Player player, long delayTicks) {
        try {
            // Folia: must run on the entity's own region thread
            player.getScheduler().runDelayed(plugin, st -> {
                if (player.isOnline() && !isFlightAllowed(player)) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }, null, delayTicks);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            // Paper / Spigot fallback
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !isFlightAllowed(player)) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }, delayTicks);
        }
    }

    private boolean isFlightAllowed(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        String worldName = player.getWorld().getName();
        List<String> disabledWorlds = plugin.getSurvivalConfig().getStringList("disabled-fly");
        // Case-insensitive match to handle Multiverse world name casing differences
        return disabledWorlds.stream().noneMatch(w -> w.equalsIgnoreCase(worldName));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
