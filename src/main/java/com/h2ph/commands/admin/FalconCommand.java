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
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cUsage: /falcon <auction|order|rtpqueue|speed|tools> [args]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("auction")) {
            if (!player.hasPermission("auction.admin")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon auction <player>");
                return true;
            }
            String targetName = args[1];
            player.setMetadata("ah-admin-view", new FixedMetadataValue(plugin, true));
            GUIHandler.openAdminPlayerDetailsGUI(player, targetName, plugin.getAuctionController());
            return true;
        } else if (sub.equals("order")) {
            if (args.length >= 2) {
                if (!player.hasPermission("prism.admin.orders")) {
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
        }

        if (sub.equals("rtpqueue")) {
            if (!player.hasPermission("falcon.rtpqueue")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                return true;
            }

            String action = args[1].toLowerCase();

            if (action.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue create <region>");
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().createQueue(player, regionName);
                return true;
            } else if (action.equals("delete")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue delete <region>");
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().deleteQueue(player, regionName);
                return true;
            } else {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                return true;
            }
        }

        if (sub.equals("speed")) {
            if (args.length < 2) {
                return true;
            }

            try {
                float speed;
                String speedArg = args[1].toLowerCase();

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
                // No error messages as requested
            }
            return true;
        }

        if (sub.equals("tools")) {
            if (!player.hasPermission("prism.admin.tools")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon tools <tool> <player> [duration]");
                return true;
            }

            String toolType = args[1].toLowerCase();
            String playerName = args[2];
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                player.sendMessage(com.prismcore.survival.tools.Utils.formatColors("&cThat player is not online."));
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                com.prismcore.survival.tools.Utils.formatColors("&cThat player is not online.")));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            if (target.getInventory().firstEmpty() == -1) {
                player.sendMessage(com.prismcore.survival.tools.Utils.formatColors("&cThis player inventory is full!"));
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                com.prismcore.survival.tools.Utils.formatColors("&cThis player inventory is full!")));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            long overrideTimer = 0;
            if (args.length >= 4) {
                overrideTimer = com.prismcore.survival.tools.Utils.parseDuration(args[3]);
                if (overrideTimer <= 0) {
                    player.sendMessage("§cInvalid duration format. Use: 1d, 12h, 30m");
                    return true;
                }
            }

            com.prismcore.survival.tools.ToolsManager manager = com.prismcore.survival.tools.ToolsManager.getInstance();
            if (manager == null) {
                player.sendMessage("§cToolsManager is not initialized.");
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
                // Message handled in manager
                return true;
            } else {
                player.sendMessage(
                        "§cInvalid tool type. Valid: drill, axe, shovel, multitool, bucket, shardbooster, sellaxe");
                return true;
            }
        }

        player.sendMessage("§cUnknown subcommand. Use auction, order, rtpqueue, speed, or tools.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("auction", "order", "rtpqueue", "speed", "tools").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("auction") || args[0].equalsIgnoreCase("order")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("rtpqueue")) {
                return Arrays.asList("create", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("speed")) {
                return Arrays.asList("normal", "1", "2", "3", "4", "5", "1.5").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("tools")) {
                return Arrays.asList("drill", "axe", "shovel", "multitool", "bucket", "shardbooster", "sellaxe")
                        .stream()
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
            } else if (args[0].equalsIgnoreCase("tools")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("tools")) {
            return Arrays.asList("1d", "12h", "30m", "1w");
        }
        return Collections.emptyList();
    }
}
