package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TabCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public TabCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prism.admin.tab")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getTabListManager().reloadTabList();
                sender.sendMessage(ChatColor.GREEN + "TAB list configuration reloaded!");
                break;

            case "refresh":
                plugin.getTabListManager().refreshTabListSorting();
                sender.sendMessage(ChatColor.GREEN + "TAB list sorting refreshed for all players!");
                break;

            case "rankings":
                showRankings(sender);
                break;

            case "setranking":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /tab setranking <group> <ranking>");
                    return true;
                }
                setRanking(sender, args[1], args[2]);
                break;

            case "toggle":
                toggleGroupSorting(sender);
                break;

            case "info":
                showInfo(sender);
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== TAB List Management ===");
        sender.sendMessage(ChatColor.YELLOW + "/tab reload" + ChatColor.WHITE + " - Reload TAB configuration");
        sender.sendMessage(ChatColor.YELLOW + "/tab refresh" + ChatColor.WHITE + " - Refresh TAB sorting for all players");
        sender.sendMessage(ChatColor.YELLOW + "/tab rankings" + ChatColor.WHITE + " - Show current group rankings");
        sender.sendMessage(ChatColor.YELLOW + "/tab setranking <group> <ranking>" + ChatColor.WHITE + " - Set group ranking (higher = first)");
        sender.sendMessage(ChatColor.YELLOW + "/tab toggle" + ChatColor.WHITE + " - Toggle group sorting on/off");
        sender.sendMessage(ChatColor.YELLOW + "/tab info" + ChatColor.WHITE + " - Show TAB list information");
    }

    private void showRankings(CommandSender sender) {
        Map<String, Integer> rankings = plugin.getTabListManager().getGroupRankings();
        
        if (rankings.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No group rankings configured!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Group Rankings ===");
        rankings.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.WHITE + ": " + 
                    ChatColor.GREEN + entry.getValue());
            });
    }

    private void setRanking(CommandSender sender, String group, String rankingStr) {
        try {
            int ranking = Integer.parseInt(rankingStr);
            plugin.getTabListManager().setGroupRanking(group, ranking);
            sender.sendMessage(ChatColor.GREEN + "Set ranking for group '" + group + "' to " + ranking);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid ranking number: " + rankingStr);
        }
    }

    private void toggleGroupSorting(CommandSender sender) {
        boolean enabled = plugin.getTabListManager().isGroupSortingEnabled();
        plugin.getTabListManager().setGroupSortingEnabled(!enabled);
        
        String status = enabled ? ChatColor.RED + "disabled" : ChatColor.GREEN + "enabled";
        sender.sendMessage(ChatColor.YELLOW + "Group sorting " + status + ChatColor.YELLOW + "!");
    }

    private void showInfo(CommandSender sender) {
        boolean enabled = plugin.getTabListManager().isGroupSortingEnabled();
        Map<String, Integer> rankings = plugin.getTabListManager().getGroupRankings();
        
        sender.sendMessage(ChatColor.GOLD + "=== TAB List Information ===");
        sender.sendMessage(ChatColor.YELLOW + "Group Sorting: " + 
            (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Configured Groups: " + ChatColor.WHITE + rankings.size());
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sender.sendMessage(ChatColor.YELLOW + "Online Players: " + ChatColor.WHITE + 
                plugin.getServer().getOnlinePlayers().size());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("prism.admin.tab")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "refresh", "rankings", "setranking", "toggle", "info");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setranking")) {
            return Arrays.asList("owner", "dev", "manager", "vip", "default");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setranking")) {
            return Arrays.asList("100", "90", "80", "70", "60", "50", "40", "30", "20", "10");
        }

        return Collections.emptyList();
    }
}