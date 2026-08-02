package com.h2ph.commands.admin.moderations;

import com.h2ph.Falcon;
import com.h2ph.gui.EnderChestGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnderSeeCommand implements CommandExecutor, TabCompleter {

    private final Falcon plugin;

    public EnderSeeCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;
        if (!player.hasPermission("falcon.endersee")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("§cUsage: /endersee <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && target.isOnline()) {
            new EnderChestGUI(plugin).open(player, target.getUniqueId(), target.getName(), null);
        } else {
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
            if (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null) {
                new EnderChestGUI(plugin).open(player, offlineTarget.getUniqueId(), offlineTarget.getName(), null);
            } else {
                player.sendMessage("§cPlayer not found.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return org.bukkit.util.StringUtil.copyPartialMatches(args[0], names, new ArrayList<>());
        }
        return new ArrayList<>();
    }
}
