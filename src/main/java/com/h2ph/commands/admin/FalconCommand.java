package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.auction.GUIHandler;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import com.prismcore.survival.orders.store.PlayerStateManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cUsage: /falcon <auction|order> [player]");
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

        player.sendMessage("§cUnknown subcommand. Use auction or order.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("auction", "order").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("auction") || args[0].equalsIgnoreCase("order"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
