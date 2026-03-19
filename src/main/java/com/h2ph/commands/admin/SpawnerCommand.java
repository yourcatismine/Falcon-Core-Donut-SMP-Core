package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public SpawnerCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("falcon.spawners")) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String targetName = args[1];
        String typeStr = args[2];
        int amountArg = 1;
        if (args.length >= 4) {
            try {
                amountArg = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }
        final int amount = amountArg;

        com.prismcore.survival.spawners.mob.SpawnerType type = com.prismcore.survival.spawners.mob.SpawnerType.fromString(typeStr);
        if (type == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            boolean hasPlayed = offlineTarget.hasPlayedBefore();
            boolean isOnline = offlineTarget.isOnline();
            
            plugin.getSchedulerAdapter().runAtLocation(player.getLocation(), () -> {
                if (!hasPlayed && !isOnline) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                
                if (!isOnline) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                Player onlineTarget = offlineTarget.getPlayer();
                if (onlineTarget != null) {
                    ItemStack item = com.prismcore.survival.spawners.util.SpawnerItemUtil.createSpawnerItem(type, amount);
                    onlineTarget.getInventory().addItem(item);

                    String msg = com.prismcore.survival.tools.Utils.formatColors("&7Given&a " + onlineTarget.getName() + "&7 spawner&a " + type.name() + "&7 " + amount);
                    player.sendMessage(msg);
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(com.prismcore.survival.spawners.mob.SpawnerType.values())
                    .map(type -> type.name().toLowerCase())
                    .filter(name -> name.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
