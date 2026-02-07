package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.TpaRequestManager;
import com.h2ph.utils.SmallCapsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        TpaRequestManager.Request request = null;
        Player targetPlayer = null;

        if (args.length > 0) {
            // Accept specific player
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                request = TpaRequestManager.getInstance().getRequest(p.getUniqueId(), target.getUniqueId());
                targetPlayer = target;
            } else {
                p.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&cThat player is not online or does not exist."));
                return true;
            }
        } else {
            // Accept last request
            request = TpaRequestManager.getInstance().getLastRequest(p.getUniqueId());
            if (request != null) {
                targetPlayer = Bukkit.getPlayer(request.getSender());
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou have no pending teleport requests."));
                return true;
            }
        }

        if (request == null) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cYou have no pending teleport requests from that player."));
            return true;
        }

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThat player is no longer online."));
            TpaRequestManager.getInstance().removeRequest(p.getUniqueId(), request.getSender()); // Cleanup
            return true;
        }

        String smallCapsTarget = SmallCapsUtil.toSmallCaps(targetPlayer.getName());
        // Correct message structure as requested:
        // &7You accepted &5%PLAYER_NAME%&7 teleport request.
        // &7You accepted &5%PLAYER_NAME%&7 teleport here request.

        // Determine who teleports where
        final Player teleporter;
        final Player destination;
        final String destinationName; // For final message

        if (request.getType() == TpaRequestManager.RequestType.TPA_HERE) {
            teleporter = p; // Acceptor teleports to Sender
            destination = targetPlayer;
            destinationName = SmallCapsUtil.toSmallCaps(destination.getName());
        } else {
            teleporter = targetPlayer; // Sender teleports to Acceptor
            destination = p;
            destinationName = SmallCapsUtil.toSmallCaps(destination.getName()); // Wait, message says %GAMERTAG%.
            // "You were teleported to %GAMERTAG%" -> For TPA, Sender sees Acceptor's name.
        }

        // Start Teleport Countdown Task
        new org.bukkit.scheduler.BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (!teleporter.isOnline() || !destination.isOnline()) {
                    this.cancel();
                    return; // Abort if anyone logs off
                }

                if (seconds > 0) {
                    // Feedback: &7Teleporting in &5%SECOND%s
                    String msg = ChatColor.translateAlternateColorCodes('&', "&7Teleporting in &5" + seconds + "s");
                    teleporter.sendMessage(msg);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));

                    // Sounds: Tripwire & Enderman teleport sound per count
                    teleporter.playSound(teleporter.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                    teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    seconds--;
                } else {
                    // Teleport Time!
                    // Feedback: &7Teleporting...
                    String teleportingMsg = ChatColor.translateAlternateColorCodes('&', "&7Teleporting...");
                    teleporter.sendMessage(teleportingMsg); // "Both chat and actionbar" implied from context or
                                                            // explicit request?
                    // User Request: "&7Teleporting..." (didn't specify location, assuming
                    // Chat/Actionbar consistency)
                    // Let's do both to be safe/consistent.
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(teleportingMsg));

                    // Perform Teleport
                    teleporter.teleport(destination.getLocation());

                    // Post-Teleport Feedback
                    // &7You were teleported to &5%GAMERTAG% - Actionbar only
                    String successMsg = ChatColor.translateAlternateColorCodes('&',
                            "&7You were teleported to &5" + destinationName);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(successMsg));

                    // Enderman Teleport sound
                    teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    this.cancel();
                }
            }
        }.runTaskTimer(PrismSurvival.getInstance(), 0L, 20L); // Run immediately (0 delay), every 20 ticks (1 second)

        // Remove request after accept
        TpaRequestManager.getInstance().removeRequest(p.getUniqueId(), request.getSender());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && ((Player) sender).canSee(player)) {
                    playerNames.add(player.getName());
                }
            }
            return playerNames;
        }
        return Collections.emptyList();
    }
}
