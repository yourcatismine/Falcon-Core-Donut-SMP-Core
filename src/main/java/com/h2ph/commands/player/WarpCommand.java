package com.h2ph.commands.player;

import com.h2ph.Falcon;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarpCommand implements TabExecutor {

    private final Falcon plugin;

    public WarpCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            return true;
        }

        String name = args[0];
        Location dest = plugin.getWarpManager().getWarp(name);

        if (dest == null) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            return true;
        }

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0f, 1.0f);
        } catch (Throwable ignored) {
        }

        plugin.getTeleportManager().teleport(
                player,
                dest,
                5,
                "&7Teleporting in &b%s",
                "&7You were teleported to warp &b" + name + "&7.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cur = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            try {
                for (String w : plugin.getWarpManager().listWarps()) {
                    if (w.toLowerCase().startsWith(cur)) {
                        result.add(w);
                    }
                }
            } catch (Throwable ignored) {
            }
            return result;
        }
        return Collections.emptyList();
    }
}
