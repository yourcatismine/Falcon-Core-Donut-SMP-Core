package com.h2ph.commands.player;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class TpAutoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        PlayerData data = Falcon.getInstance().getPlayerDataManager().get(p.getUniqueId());

        if (data == null) {
            p.sendMessage(ChatColor.RED + "Data not found.");
            return true;
        }

        boolean newState = !data.isTpAuto();
        data.setTpAuto(newState);

        if (newState) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&7You turned on tpauto.");
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else {
            String msg = ChatColor.translateAlternateColorCodes('&', "&7You turned off tpauto.");
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        return true;
    }
}
