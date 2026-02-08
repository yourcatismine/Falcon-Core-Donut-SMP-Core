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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.scheduler.BukkitTask;

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
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
                if (offlineTarget.hasPlayedBefore()) {
                    String msg = ChatColor.translateAlternateColorCodes('&', "&cThat player is not online.");
                    p.sendMessage(msg);
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } else {
                    String msg = ChatColor.translateAlternateColorCodes('&', "&cThat player does not exist.");
                    p.sendMessage(msg);
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return true;
            }
        } else {
            // Accept last request
            request = TpaRequestManager.getInstance().getLastRequest(p.getUniqueId());
            if (request != null) {
                targetPlayer = Bukkit.getPlayer(request.getSender());
            } else {
                String msg = ChatColor.translateAlternateColorCodes('&', "&cThis teleport request does not exist.");
                p.sendMessage(msg);
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(msg));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
        }

        if (request == null) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cThis teleport request does not exist.");
            p.sendMessage(msg);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
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
        final AtomicInteger seconds = new AtomicInteger(5);
        final AtomicReference<BukkitTask> task = new AtomicReference<>();
        final org.bukkit.Location startLoc = teleporter.getLocation();

        // Sound on accept: Cow Bell to Requester (Teleporter) and Receiver
        // (Destination)
        // User said "cowbell note sound when a player got accepted".
        // Playing to both to be safe/nice.
        teleporter.playSound(teleporter.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);
        destination.playSound(destination.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!teleporter.isOnline() || !destination.isOnline()) {
                    if (task.get() != null) {
                        task.get().cancel();
                    }
                    return; // Abort if anyone logs off
                }

                // Movement Check
                org.bukkit.Location currentLoc = teleporter.getLocation();
                double dist = Math.pow(currentLoc.getX() - startLoc.getX(), 2)
                        + Math.pow(currentLoc.getZ() - startLoc.getZ(), 2);
                double distY = Math.abs(currentLoc.getY() - startLoc.getY());

                if (dist > 0.1 || distY > 1.5 || !currentLoc.getWorld().equals(startLoc.getWorld())) {
                    String cancelMsg = ChatColor.translateAlternateColorCodes('&',
                            "&cTeleportation cancelled because you moved.");
                    teleporter.sendMessage(cancelMsg);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(cancelMsg));
                    // Play sound at current location to ensure they hear it
                    teleporter.playSound(currentLoc, Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                    if (task.get() != null) {
                        task.get().cancel();
                    }
                    return;
                }

                if (seconds.get() > 0) {
                    // Feedback: &7Teleporting in &5%SECOND%s
                    String msg = ChatColor.translateAlternateColorCodes('&',
                            "&7Teleporting in &5" + seconds.get() + "s");
                    teleporter.sendMessage(msg);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));

                    // Sounds: Tripwire & Enderman teleport sound per count
                    teleporter.playSound(teleporter.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                    teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    seconds.decrementAndGet();
                } else {
                    // Teleport Time!
                    // Teleport Time!

                    // Perform Teleport
                    teleporter.teleportAsync(destination.getLocation()).thenAccept(success -> {
                        if (success) {
                            // Post-Teleport Feedback
                            // &7You were teleported to &5%GAMERTAG% - Actionbar only
                            String successMsg = ChatColor.translateAlternateColorCodes('&',
                                    "&7You were teleported to &5" + destinationName);
                            teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new net.md_5.bungee.api.chat.TextComponent(successMsg));

                            // Enderman Teleport sound
                            teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        }
                    });

                    if (task.get() != null) {
                        task.get().cancel();
                    }
                }
            }
        };

        // Schedule the task using the Folia-safe scheduler adapter on the teleporter
        // entity
        BukkitTask scheduledTask = PrismSurvival.getInstance().getSchedulerAdapter().runEntityTaskTimer(teleporter,
                runnable, 0L, 20L);
        task.set(scheduledTask);

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
