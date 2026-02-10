/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package com.prismcore.survival.orders.cmd;

import java.util.Collections;
import java.util.List;
import com.h2ph.PrismSurvival;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import com.prismcore.survival.orders.store.PlayerStateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class OrdersCommand implements CommandExecutor, TabCompleter {
    private final PrismSurvival plugin;

    public OrdersCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player p = (Player) sender;
        OrdersModule.getInstance().state().resetMain(p.getUniqueId());
        PlayerStateManager.View state = OrdersModule.getInstance().state().main(p.getUniqueId());
        if (args.length > 0) {
            String query = String.join((CharSequence) " ", args).trim();
            state.search = query.isEmpty() ? null : query.toLowerCase();
            state.page = 0;
        }
        new OrdersMainMenu(OrdersModule.getInstance(), p).open();
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
