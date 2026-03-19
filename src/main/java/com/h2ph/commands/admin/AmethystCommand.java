package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
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

public class AmethystCommand implements CommandExecutor, TabCompleter {

    public AmethystCommand(PrismSurvival plugin) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("falcon.amethysts")) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length < 2) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String toolType = args[0].toLowerCase();
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (target.getInventory().firstEmpty() == -1) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        long overrideTimer = 0;
        if (args.length >= 3) {
            overrideTimer = com.prismcore.survival.tools.Utils.parseDuration(args[2]);
            if (overrideTimer <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        }

        com.prismcore.survival.tools.ToolsManager manager = com.prismcore.survival.tools.ToolsManager.getInstance();
        if (manager == null) {
            return true;
        }

        if (Arrays.asList("drill", "axe", "shovel").contains(toolType)) {
            manager.giveTool(target, toolType, overrideTimer);
            player.sendMessage("§aGiven " + toolType + " to §f" + target.getName());
            return true;
        } else if (toolType.equals("multitool")) {
            manager.giveMultiTool(target, overrideTimer);
            player.sendMessage("§aGiven multitool to §f" + target.getName());
            return true;
        } else if (toolType.equals("bucket")) {
            manager.giveBucket(target, overrideTimer);
            player.sendMessage("§aGiven countdown bucket to §f" + target.getName());
            return true;
        } else if (toolType.equals("shardbooster")) {
            manager.giveShardBooster(target, overrideTimer);
            player.sendMessage("§aGiven shard booster to §f" + target.getName());
            return true;
        } else if (toolType.equals("sellaxe")) {
            manager.giveSellAxe(target, overrideTimer);
            player.sendMessage("§aGiven sell axe to §f" + target.getName());
            return true;
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("drill", "axe", "shovel", "multitool", "bucket", "shardbooster", "sellaxe")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            return Arrays.asList("1d", "12h", "30m", "1w");
        }
        return Collections.emptyList();
    }
}
