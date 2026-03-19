package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.auction.GUIHandler;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.ChatColor;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.h2ph.afk.AFKManager;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FalconCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public FalconCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (sub.equals("auction")) {
            if (!player.hasPermission("auction.admin")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon auction <player>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            String targetName = args[1];
            player.setMetadata("ah-admin-view", new FixedMetadataValue(plugin, true));
            GUIHandler.openAdminPlayerDetailsGUI(player, targetName, plugin.getAuctionController());
            return true;
        } else if (sub.equals("order")) {
            if (args.length >= 2) {
                if (!player.hasPermission("falcon.orders")) {
                    player.sendMessage("§cYou do not have permission to manage player orders.");
                    return true;
                }
                String targetName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || target.getName() == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                new com.prismcore.survival.orders.gui.AdminOrderDetailsMenu(OrdersModule.getInstance(), player, target)
                        .open();
                return true;
            }

            if (!player.hasPermission("order.use")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            OrdersModule.getInstance().state().resetMain(player.getUniqueId());
            new OrdersMainMenu(OrdersModule.getInstance(), player).open();
            return true;
        } else if (sub.equals("rtpqueue")) {
            if (!player.hasPermission("falcon.rtpqueue")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String rtpAction = args[1].toLowerCase();

            if (rtpAction.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue create <region>");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().createQueue(player, regionName);
                return true;
            } else if (rtpAction.equals("delete")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue delete <region>");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().deleteQueue(player, regionName);
                return true;
            } else {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else if (sub.equals("setafk")) {
            if (!player.hasPermission("falcon.setafk")) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            if (args.length < 2) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String afkAction = args[1].toLowerCase();
            AFKManager afkManager = plugin.getAfkManager();

            if (afkAction.equals("confirm")) {
                if (args.length < 3) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                String regionName = args[2];
                try {
                    com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
                    LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
                    Region region = session.getSelection(worldEditPlayer.getWorld());

                    if (region == null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return true;
                    }

                    BlockVector3 min = region.getMinimumPoint();
                    BlockVector3 max = region.getMaximumPoint();
                    String worldName = player.getWorld().getName();

                    Vector minVec = new Vector(min.x(), min.y(), min.z());
                    Vector maxVec = new Vector(max.x(), max.y(), max.z());

                    afkManager.createRegion(regionName, worldName, minVec, maxVec);
                    player.sendMessage(ChatColor.GREEN + "Created AFK region " + ChatColor.YELLOW + regionName +
                            ChatColor.GREEN + " in world " + ChatColor.AQUA + worldName);

                } catch (IncompleteRegionException e) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception e) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    e.printStackTrace();
                }
                return true;
            } else if (afkAction.equals("delete")) {
                if (args.length < 3) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                String regionName = args[2];
                if (afkManager.deleteRegion(regionName)) {
                    player.sendMessage(ChatColor.GREEN + "Deleted AFK region: " + ChatColor.YELLOW + regionName);
                } else {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return true;
            } else if (afkAction.equals("setspawn")) {
                saveAfkSpawn(player);
                return true;
            } else if (afkAction.equals("remove")) {
                if (args.length < 3) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                deleteAfkSpawn(player, args[2]);
                return true;
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else if (sub.equals("void")) {
            if (!player.hasPermission("falcon.void")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon void create <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String voidAction = args[1].toLowerCase();

            if (voidAction.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon void create <name>");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }

                String name = args[2];
                try {
                    com.sk89q.worldedit.entity.Player worldEditPlayer = com.sk89q.worldedit.bukkit.BukkitAdapter
                            .adapt(player);
                    com.sk89q.worldedit.LocalSession session = com.sk89q.worldedit.WorldEdit.getInstance()
                            .getSessionManager().get(worldEditPlayer);
                    com.sk89q.worldedit.regions.Region worldEditRegion = session
                            .getSelection(worldEditPlayer.getWorld());

                    if (worldEditRegion == null) {
                        player.sendMessage("§cPlease make a selection with WorldEdit first.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return true;
                    }

                    com.sk89q.worldedit.math.BlockVector3 min = worldEditRegion.getMinimumPoint();
                    com.sk89q.worldedit.math.BlockVector3 max = worldEditRegion.getMaximumPoint();
                    String worldName = player.getWorld().getName();

                    plugin.getVoidManager().addRegion(name, worldName, min.x(), min.y(), min.z(), max.x(),
                            max.y(), max.z());
                    player.sendMessage("§aVoid protection region list '" + name + "' has been created!");

                } catch (com.sk89q.worldedit.IncompleteRegionException e) {
                    player.sendMessage("§cPlease make a complete selection (pos1 and pos2) first.");
                } catch (Exception e) {
                    player.sendMessage("§cError accessing WorldEdit selection: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
        } else if (sub.equals("respawngear")) {
            if (!player.hasPermission("falcon.respawngear")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon respawngear <setup|delete>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String respawnAction = args[1].toLowerCase();
            if (respawnAction.equals("setup")) {
                plugin.getRespawnGearGUI().open(player);
                return true;
            } else if (respawnAction.equals("delete")) {
                plugin.getRespawnGearManager().clearItems();
                player.sendMessage("§aRespawn gear items have been deleted.");
                return true;
            } else {
                player.sendMessage("§cUsage: /falcon respawngear <setup|delete>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else if (sub.equals("limiter")) {
            if (!player.hasPermission("falcon.limiter")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon limiter <reload|stats>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String limiterAction = args[1].toLowerCase();

            if (limiterAction.equals("reload")) {
                plugin.getLimiterManager().reload();
                player.sendMessage("§aLimiter configuration and tasks have been reloaded.");
            } else if (limiterAction.equals("stats")) {
                int radius = plugin.getLimiterConfig().getChunkCheckRadius();
                org.bukkit.Location loc = player.getLocation();

                plugin.getSchedulerAdapter().runAtLocation(loc, () -> {
                    org.bukkit.Chunk center = loc.getChunk();
                    org.bukkit.World world = loc.getWorld();

                    int totalEntities = 0;
                    int totalItems = 0;
                    int totalChunks = 0;

                    for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                        for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                            if (world.isChunkLoaded(x, z)) {
                                totalChunks++;
                                for (org.bukkit.entity.Entity e : world.getChunkAt(x, z).getEntities()) {
                                    if (e instanceof org.bukkit.entity.Player)
                                        continue;
                                    if (e instanceof org.bukkit.entity.Item) {
                                        totalItems++;
                                    } else {
                                        totalEntities++;
                                    }
                                }
                            }
                        }
                    }

                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&c&m---------------------------------"));
                    player.sendMessage(org.bukkit.ChatColor.RED + " Limiter Stats: " + org.bukkit.ChatColor.WHITE
                            + "Radius " + radius + " Chunks");
                    player.sendMessage("");
                    player.sendMessage(
                            org.bukkit.ChatColor.RED + " Loaded Chunks: " + org.bukkit.ChatColor.WHITE + totalChunks);
                    player.sendMessage(org.bukkit.ChatColor.RED + " Total Entities: " + org.bukkit.ChatColor.WHITE
                            + totalEntities);
                    player.sendMessage(
                            org.bukkit.ChatColor.RED + " Total Items: " + org.bukkit.ChatColor.WHITE + totalItems);
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&c&m---------------------------------"));
                });
            } else {
                player.sendMessage("§cUsage: /falcon limiter <reload|stats>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return true;
        } else if (sub.equals("crystal")) {
            return handleCrystal(player, args);
        } else if (sub.equals("anchor")) {
            return handleAnchor(player, args);
        } else if (sub.equals("pvpsafe")) {
            return handlePvPSafe(player, args);
        } else if (sub.equals("warps")) {
            return handleWarps(player, args);
        }

        player.sendMessage(
                "§cUnknown subcommand. Use auction, order, rtpqueue, void, setafk, respawngear, limiter, crystal, anchor, pvpsafe, or warps.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        return true;
    }

    private boolean handleWarps(Player player, String[] args) {
        if (!player.hasPermission("falcon.warps")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /falcon warps <set|delete|list> [name]");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("set")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon warps set <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            String name = args[2];
            plugin.getWarpManager().setWarp(name, player.getLocation());
            player.sendMessage("§aWarp §f'" + name + "' §ahas been set at your current location.");
            return true;
        } else if (action.equals("delete") || action.equals("del")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon warps delete <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            String name = args[2];
            boolean deleted = plugin.getWarpManager().deleteWarp(name);
            if (deleted) {
                player.sendMessage("§aWarp §f'" + name + "' §ahas been deleted.");
            } else {
                player.sendMessage("§cWarp '" + name + "' not found.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return true;
        } else if (action.equals("list")) {
            java.util.List<String> warps = plugin.getWarpManager().listWarps();
            if (warps.isEmpty()) {
                player.sendMessage("§7No warps have been set.");
            } else {
                player.sendMessage("§6Warps: §f" + String.join("§7, §f", warps));
            }
            return true;
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays
                    .asList("auction", "order", "rtpqueue", "void", "setafk", "respawngear", "limiter",
                            "crystal", "anchor", "pvpsafe", "warps")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("respawngear")) {
                return Arrays.asList("setup", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("auction") || args[0].equalsIgnoreCase("order")) {
                return plugin.getPlayerNameCache().getCompletions(args[1]);
            } else if (args[0].equalsIgnoreCase("rtpqueue")) {
                return Arrays.asList("create", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("void")) {
                return Arrays.asList("create").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("limiter")) {
                return Arrays.asList("reload", "stats").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("crystal")) {
                return Arrays.asList("normal", "0", "1", "2", "3", "4", "5", "6", "10", "20").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("anchor")) {
                return Arrays.asList("normal", "0", "1", "2", "3", "4", "5", "6", "10", "20").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("pvpsafe")) {
                return Arrays.asList("setup", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("setafk")) {
                return Arrays.asList("confirm", "delete", "setspawn", "remove").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("warps")) {
                return Arrays.asList("set", "delete", "list").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rtpqueue")) {
                if (args[1].equalsIgnoreCase("create")) {
                    return plugin.getRTPQueueManager().getAvailableRegions().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args[1].equalsIgnoreCase("delete")) {
                    return plugin.getRTPQueueManager().getQueueNames().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("pvpsafe") && args[1].equalsIgnoreCase("setup")) {
                return Arrays.asList("[zone_name]");
            } else if (args[0].equalsIgnoreCase("pvpsafe") && args[1].equalsIgnoreCase("delete")) {
                return plugin.getPvPSafeZoneManager().getAllZoneNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("warps") && args[1].equalsIgnoreCase("delete")) {
                return plugin.getWarpManager().listWarps().stream()
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("setafk")) {
                if (args.length == 3) {
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("remove")) {
                        List<String> completions = new ArrayList<>();
                        if (args[1].equalsIgnoreCase("delete")) {
                            completions.addAll(plugin.getAfkManager().getRegionNames());
                        } else {
                            completions.addAll(getMapNames());
                        }
                        return completions.stream()
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private boolean handleCrystal(Player player, String[] args) {
        if (!player.hasPermission("falcon.crystal")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length < 2) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        try {
            String damageArg = args[1].toLowerCase();
            double damage;

            if (damageArg.equals("normal")) {
                damage = 6.0;
            } else {
                damage = Double.parseDouble(damageArg);
            }

            if (damage < 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return true;
            }

            plugin.getDamageManager().setCrystalDamage(damage);
            player.sendMessage("§aCrystal damage set to §f" + damage);
            return true;
        } catch (NumberFormatException ignored) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }
    }

    private boolean handleAnchor(Player player, String[] args) {
        if (!player.hasPermission("falcon.anchor")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length < 2) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        try {
            String damageArg = args[1].toLowerCase();
            double damage;

            if (damageArg.equals("normal")) {
                damage = 6.0;
            } else {
                damage = Double.parseDouble(damageArg);
            }

            if (damage < 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return true;
            }

            plugin.getDamageManager().setAnchorDamage(damage);
            player.sendMessage("§aAnchor damage set to §f" + damage);
            return true;
        } catch (NumberFormatException ignored) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }
    }

    private boolean handlePvPSafe(Player player, String[] args) {
        if (!player.hasPermission("falcon.pvpsafe")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /falcon pvpsafe <setup|delete> [name]");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("setup")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon pvpsafe setup <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String name = args[2];
            try {
                com.sk89q.worldedit.entity.Player worldEditPlayer = com.sk89q.worldedit.bukkit.BukkitAdapter
                        .adapt(player);
                com.sk89q.worldedit.LocalSession session = com.sk89q.worldedit.WorldEdit.getInstance()
                        .getSessionManager().get(worldEditPlayer);
                com.sk89q.worldedit.regions.Region worldEditRegion = session
                        .getSelection(worldEditPlayer.getWorld());

                if (worldEditRegion == null) {
                    player.sendMessage("§cPlease make a selection with WorldEdit first.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }

                com.sk89q.worldedit.math.BlockVector3 min = worldEditRegion.getMinimumPoint();
                com.sk89q.worldedit.math.BlockVector3 max = worldEditRegion.getMaximumPoint();
                String worldName = player.getWorld().getName();

                boolean success = plugin.getPvPSafeZoneManager().addZone(name, worldName, min.x(), min.y(), min.z(),
                        max.x(), max.y(), max.z(), player.getUniqueId().toString());

                if (success) {
                    player.sendMessage("§aPvP safe zone '" + name + "' has been created!");
                    player.sendMessage("§7Players entering this zone will see safe mode messages.");
                } else {
                    player.sendMessage("§cFailed to create PvP safe zone. A zone with that name may already exist.");
                }

            } catch (com.sk89q.worldedit.IncompleteRegionException e) {
                player.sendMessage("§cPlease make a complete selection (pos1 and pos2) first.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            } catch (Exception e) {
                player.sendMessage("§cError accessing WorldEdit selection: " + e.getMessage());
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("delete")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon pvpsafe delete <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            String zoneName = args[2];

            boolean success = plugin.getPvPSafeZoneManager().removeZone(zoneName);

            if (success) {
                player.sendMessage("§aPvP safe zone '" + zoneName + "' has been deleted!");
            } else {
                player.sendMessage("§cFailed to delete PvP safe zone. Zone '" + zoneName + "' may not exist.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }

            return true;
        } else {
            player.sendMessage("§cUsage: /falcon pvpsafe <setup|delete> <name>");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
    }
    private void saveAfkSpawn(Player player) {
        String worldName = player.getWorld().getName();
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        java.io.File file = new java.io.File(folder, worldName + ".yml");
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);

        config.set("spawn", player.getLocation());
        try {
            config.save(file);
            player.sendMessage(ChatColor.GREEN + "AFK spawn set for world " + ChatColor.YELLOW + worldName);
        } catch (java.io.IOException e) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            e.printStackTrace();
        }
    }

    private void deleteAfkSpawn(Player player, String mapName) {
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        java.io.File file = new java.io.File(folder, mapName + ".yml");

        if (file.exists()) {
            if (file.delete()) {
                player.sendMessage(ChatColor.GREEN + "Removed AFK map: " + ChatColor.YELLOW + mapName);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private List<String> getMapNames() {
        List<String> maps = new ArrayList<>();
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        if (folder.exists()) {
            java.io.File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (java.io.File f : files) {
                    maps.add(f.getName().replace(".yml", ""));
                }
                Collections.sort(maps);
            }
        }
        return maps;
    }
}
