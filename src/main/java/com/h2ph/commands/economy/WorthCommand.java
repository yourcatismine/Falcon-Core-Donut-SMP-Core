package com.h2ph.commands.economy;

import com.h2ph.Falcon;
import com.h2ph.gui.WorthGUI;
import com.falconcore.survival.sell.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WorthCommand implements CommandExecutor {

    private final Falcon plugin;

    public WorthCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() != Material.AIR) {
            double price = plugin.getFalconSell().getPricesManager().getPrice(item);
            if (price > 0) {
                double totalWorth = price * item.getAmount();
                String formatted = MessageUtil.formatMoney(totalWorth);
                String msg = "&7Worth::&a $" + formatted;
                
                MessageUtil.sendMessage(player, msg);
                MessageUtil.sendActionBar(player, msg);
                return true;
            }
        }

        new WorthGUI(plugin, player).open();
        return true;
    }
}
