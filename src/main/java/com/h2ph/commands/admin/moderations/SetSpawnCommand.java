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
        if (!p.hasPermission("falcon.setspawn")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(ChatColor.YELLOW + "Usage:");
            p.sendMessage(ChatColor.YELLOW + "  /setspawn <name> - Set a named spawn");
            p.sendMessage(ChatColor.YELLOW + "  /setspawn world [worldname] - Set world-specific spawn");
            p.sendMessage(ChatColor.YELLOW + "  /setspawn delete <name> - Delete a named spawn");
            p.sendMessage(ChatColor.YELLOW + "  /setspawn deleteworld <worldname> - Delete world spawn");
            return true;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        if (spawnManager == null) {
            p.sendMessage(ChatColor.RED + "SpawnManager is not initialized.");
            return true;
        }

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

        if (args.length >= 2 && args[0].equalsIgnoreCase("deleteworld")) {
            String worldName = args[1];
            boolean deleted = spawnManager.deleteWorldSpawn(worldName);
            if (deleted) {
                p.sendMessage(ChatColor.GREEN + "Deleted world spawn for '" + worldName + "'.");
            } else {
                p.sendMessage(ChatColor.RED + "World spawn for '" + worldName + "' not found or could not be deleted.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("world")) {
            String worldName;
            if (args.length >= 2) {
                worldName = args[1];
            } else {
                worldName = p.getWorld().getName();
            }
            
            boolean ok = spawnManager.setWorldSpawn(worldName, p.getLocation());
            if (ok) {
                p.sendMessage(ChatColor.GREEN + "Set world spawn for '" + worldName + "' at your current location.");
            } else {
                p.sendMessage(ChatColor.RED + "Failed to set world spawn. Check server logs.");
            }
            return true;
        }

        String name = args[0];
        boolean ok = spawnManager.saveSpawn(name, p.getLocation());
        if (ok) {
            p.sendMessage(ChatColor.GREEN + "Saved spawn '" + name + "'.");
            if (name.equalsIgnoreCase("spawn") || name.equalsIgnoreCase("default")) {
                spawnManager.setGlobalSpawn(p.getLocation());
            }
        } else
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
            if ("deleteworld".startsWith(cur))
                res.add("deleteworld");
            if ("world".startsWith(cur))
                res.add("world");
            return res;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del")) {
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
            } else if (args[0].equalsIgnoreCase("deleteworld")) {
                String cur = args[1].toLowerCase();
                java.util.List<String> worldNames = new java.util.ArrayList<>();
                try {
                    if (plugin.getSpawnManager() != null)
                        worldNames.addAll(plugin.getSpawnManager().listWorldSpawns());
                } catch (Throwable ignored) {
                }
                java.util.List<String> res = new java.util.ArrayList<>();
                for (String s : worldNames) {
                    if (s.toLowerCase().startsWith(cur))
                        res.add(s);
                }
                return res;
            } else if (args[0].equalsIgnoreCase("world")) {
                String cur = args[1].toLowerCase();
                java.util.List<String> worldNames = new java.util.ArrayList<>();
                for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                    worldNames.add(world.getName());
                }
                java.util.List<String> res = new java.util.ArrayList<>();
                for (String s : worldNames) {
                    if (s.toLowerCase().startsWith(cur))
                        res.add(s);
                }
                return res;
            }
        }
        return java.util.Collections.emptyList();
    }
}