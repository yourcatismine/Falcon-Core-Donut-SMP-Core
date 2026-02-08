package com.h2ph.commands.player;

import com.h2ph.managers.TpaRequestManager;
import com.h2ph.utils.SmallCapsUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

import java.util.Collections;
import java.util.List;

public class TpaDenyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        TpaRequestManager.Request request;

        if (args.length > 0) {
            Player targetSender = org.bukkit.Bukkit.getPlayer(args[0]);
            if (targetSender != null) {
                request = TpaRequestManager.getInstance().getRequest(p.getUniqueId(), targetSender.getUniqueId());
            } else {
                org.bukkit.OfflinePlayer offlineSender = org.bukkit.Bukkit.getOfflinePlayer(args[0]);
                String msg;
                if (offlineSender.hasPlayedBefore()) {
                    msg = ChatColor.translateAlternateColorCodes('&', "&cThat player is not online.");
                } else {
                    msg = ChatColor.translateAlternateColorCodes('&', "&cThat player does not exist.");
                }
                p.sendMessage(msg);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        } else {
            request = TpaRequestManager.getInstance().getLastRequest(p.getUniqueId());
        }

        if (request == null) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cThis teleport request does not exist.");
            p.sendMessage(msg);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Deny the request (remove it)
        TpaRequestManager.getInstance().removeRequest(p.getUniqueId(), request.getSender());

        // Get sender name for feedback
        org.bukkit.OfflinePlayer senderPlayer = org.bukkit.Bukkit.getOfflinePlayer(request.getSender());
        String senderName = senderPlayer.getName() != null ? senderPlayer.getName() : "Unknown";
        String smallCapsSender = SmallCapsUtil.toSmallCaps(senderName);

        String feedback;
        if (request.getType() == TpaRequestManager.RequestType.TPA_HERE) {
            feedback = ChatColor.translateAlternateColorCodes('&',
                    "&7You denied &5" + smallCapsSender + "&7 teleport here request.");
        } else {
            feedback = ChatColor.translateAlternateColorCodes('&',
                    "&7You denied &5" + smallCapsSender + "&7 teleport request.");
        }

        p.sendMessage(feedback);
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(feedback));
        // No sound for success as requested

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> playerNames = new java.util.ArrayList<>();
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && ((Player) sender).canSee(player)) {
                    playerNames.add(player.getName());
                }
            }
            return playerNames;
        }
        return Collections.emptyList();
    }
}
