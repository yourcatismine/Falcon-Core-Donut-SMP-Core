package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.SpawnManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements TabExecutor {

    private final PrismSurvival plugin;

    public SetSpawnCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("prism.admin.setspawn")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /setspawn <name> OR /setspawn delete <name>");
            return true;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        if (spawnManager == null) {
            p.sendMessage(ChatColor.RED + "SpawnManager is not initialized.");
            return true;
        }

        // Check for delete subcommand
        if (args.length >= 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del"))) {
            String name = args[1];
            boolean deleted = spawnManager.deleteSpawn(name);
            if (deleted) {
                p.sendMessage(ChatColor.GREEN + "Deleted spawn '" + name + "'.");
            } else {
                p.sendMessage(ChatColor.RED + "Spawn '" + name + "' not found or could not be deleted.");
            }
            return true;
        }

        String name = args[0];
        boolean ok = spawnManager.saveSpawn(name, p.getLocation());
        if (ok)
            p.sendMessage(ChatColor.GREEN + "Saved spawn '" + name + "'.");
        else
            p.sendMessage(ChatColor.RED + "Failed to save spawn. Check server logs.");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cur = args[0].toLowerCase();
            java.util.List<String> res = new java.util.ArrayList<>();
            if ("delete".startsWith(cur))
                res.add("delete");
            return res;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del"))) {
            String cur = args[1].toLowerCase();
            java.util.List<String> names = new java.util.ArrayList<>();
            try {
                if (plugin.getSpawnManager() != null)
                    names.addAll(plugin.getSpawnManager().listSpawns());
            } catch (Throwable ignored) {
            }
            java.util.List<String> res = new java.util.ArrayList<>();
            for (String s : names) {
                if (s.toLowerCase().startsWith(cur))
                    res.add(s);
            }
            return res;
        }
        return java.util.Collections.emptyList();
    }
}