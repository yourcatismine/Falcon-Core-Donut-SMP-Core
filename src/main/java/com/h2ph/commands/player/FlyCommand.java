package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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

        // Check if allowed in current world
        if (!isFlightAllowed(player)) {
            // Silent failure
            return true;
        }

        boolean flight = !player.getAllowFlight();
        player.setAllowFlight(flight);
        player.setFlying(flight);

        return true;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // If they were flying, but now in a blacklisted world, disable it
        if (player.getAllowFlight() && !isFlightAllowed(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private boolean isFlightAllowed(Player player) {
        // Creative mode always allowed
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        String worldName = player.getWorld().getName();
        List<String> disabledWorlds = plugin.getSurvivalConfig().getStringList("disabled-fly");
        return !disabledWorlds.contains(worldName);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
