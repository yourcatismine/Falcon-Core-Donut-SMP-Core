package com.h2ph.commands.admin.afk;

import com.h2ph.PrismSurvival;
import com.h2ph.afk.AFKManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetAFKCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    private final AFKManager afkManager;

    public SetAFKCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!sender.hasPermission("prismcore.admin.afk")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setafk <confirm|delete> <region_name>");
            return true;
        }

        String action = args[0].toLowerCase();
        String regionName = args[1];

        if (action.equals("delete")) {
            if (afkManager.deleteRegion(regionName)) {
                sender.sendMessage(ChatColor.GREEN + "Deleted AFK region: " + ChatColor.YELLOW + regionName);
            } else {
                sender.sendMessage(ChatColor.RED + "Region not found: " + regionName);
            }
            return true;
        }

        if (action.equals("confirm")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can confirm selections.");
                return true;
            }

            org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) sender;

            try {
                Player worldEditPlayer = BukkitAdapter.adapt(bukkitPlayer);
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
                Region region = session.getSelection(worldEditPlayer.getWorld()); // Allows IncompleteRegionException
                                                                                  // check

                if (region == null) {
                    sender.sendMessage(ChatColor.RED + "Please make a selection with WorldEdit first.");
                    return true;
                }

                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                String worldName = bukkitPlayer.getWorld().getName();

                Vector minVec = new Vector(min.getX(), min.getY(), min.getZ());
                Vector maxVec = new Vector(max.getX(), max.getY(), max.getZ());

                afkManager.createRegion(regionName, worldName, minVec, maxVec);
                sender.sendMessage(ChatColor.GREEN + "Created AFK region " + ChatColor.YELLOW + regionName +
                        ChatColor.GREEN + " in world " + ChatColor.AQUA + worldName);

            } catch (IncompleteRegionException e) {
                sender.sendMessage(ChatColor.RED + "Please make a complete selection (pos1 and pos2) first.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error accessing WorldEdit selection: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /setafk <confirm|delete> <region_name>");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {

        if (!sender.hasPermission("prismcore.admin.afk"))
            return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("confirm", "delete");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return new ArrayList<>(afkManager.getRegionNames());
        }

        return Collections.emptyList();
    }
}
