package com.h2ph.commands.admin;

import com.h2ph.Falcon;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpeedCommand implements CommandExecutor, TabCompleter {

    public SpeedCommand(Falcon plugin) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("falcon.speed")) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length < 1) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        try {
            float speed;
            String speedArg = args[0].toLowerCase();

            if (speedArg.equals("normal")) {
                speed = 0.1f;
            } else {
                speed = Float.parseFloat(speedArg) / 10.0f;
            }

            if (speed > 1.0f) {
                speed = 1.0f;
            } else if (speed < 0.0001f) {
                speed = 0.0001f;
            }

            player.setFlySpeed(speed);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("normal", "1", "2", "3", "4", "5", "1.5").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
