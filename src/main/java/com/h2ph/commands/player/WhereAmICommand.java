package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhereAmICommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public WhereAmICommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length >= 1) {
            String targetName = args[0];
            Player onlineTarget = Bukkit.getPlayer(targetName);

            if (onlineTarget != null && player.canSee(onlineTarget)) {
                sendLocationActionBar(player, onlineTarget);
                return true;
            }

            // Check offline / hidden asynchronously to prevent TPS drops
            final boolean isOnlineButHidden = (onlineTarget != null);
            plugin.getSchedulerAdapter().runTaskAsync(() -> {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                final boolean hasPlayed = offlineTarget.hasPlayedBefore() || offlineTarget.isOnline();

                plugin.getSchedulerAdapter().runEntityTask(player, () -> {
                    if (!player.isOnline()) return;
                    String msg;
                    if (!hasPlayed && !isOnlineButHidden) {
                        msg = ChatColor.translateAlternateColorCodes('&', "&cThat player does not exist.");
                    } else {
                        // Player exists but is offline, or is online yet hidden from sender
                        msg = ChatColor.translateAlternateColorCodes('&', "&cPlayer is not online.");
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(msg));
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } catch (Exception e) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1f);
                    }
                });
            });
            return true;
        }

        // No args: show own location
        sendLocationActionBar(player, player);
        return true;
    }

    private void sendLocationActionBar(Player viewer, Player target) {
        Location loc = target.getLocation();
        String world = getWorldName(target);
        String msg = ChatColor.translateAlternateColorCodes('&',
                "&7" + target.getName() + " &8— &a" + world
                        + " &7(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private String getWorldName(Player player) {
        switch (player.getWorld().getEnvironment()) {
            case NETHER:
                return "Nether";
            case THE_END:
                return "End";
            default:
                return "Overworld";
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && ((Player) sender).canSee(p)) {
                    names.add(p.getName());
                }
            }
            return org.bukkit.util.StringUtil.copyPartialMatches(args[0], names, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
