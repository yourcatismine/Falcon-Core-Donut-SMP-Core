package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class OperatorListener implements Listener {

    private final PrismSurvival plugin;

    public OperatorListener(PrismSurvival plugin) {
        this.plugin = plugin;
        startPeriodicCheck();
    }

    private void startPeriodicCheck() {
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp()) {
                    boolean allowed = plugin.getDatabaseManager().isAllowedOperator(p.getName());
                    if (!allowed) {
                        plugin.getSchedulerAdapter().runTask(() -> {
                            p.setOp(false);
                            p.sendMessage("§cYou are not allowed to be opped on this server. You have been de-opped.");
                            plugin.getLogger().info(
                                    "Auto-deopped " + p.getName() + " (periodic check - not in allowed operators)");
                        });
                    }
                }
            }
        }, 1200L, 1200L);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null)
            return;

        Player player = e.getPlayer();
        String lower = msg.toLowerCase();

        if (player.isOp()) {
            boolean allowed = plugin.getDatabaseManager().isAllowedOperator(player.getName());
            if (!allowed) {
                e.setCancelled(true);
                plugin.getSchedulerAdapter().runTask(() -> player.setOp(false));
                player.sendMessage("§cYou are not allowed to be opped on this server. Command cancelled and de-opped.");
                plugin.getLogger().warning("Blocked command from unauthorized OP: " + player.getName() + " -> " + msg);
                return;
            }
        }

        String cmd = lower.startsWith("/") ? lower.substring(1) : lower;
        String[] parts = cmd.split(" ");
        String baseCmd = parts[0];

        if (baseCmd.equals("op") || baseCmd.equals("minecraft:op") ||
                baseCmd.equals("deop") || baseCmd.equals("minecraft:deop")) {

            if (parts.length >= 2) {
                String target = parts[1];
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(target);
                if (!allowed) {
                    e.setCancelled(true);
                    player.sendMessage("§cThat player is not in the allowed operators list. Operation denied.");

                    Player p = Bukkit.getPlayerExact(target);
                    if (p != null && p.isOp()) {
                        plugin.getSchedulerAdapter().runTask(() -> p.setOp(false));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        String cmd = e.getCommand();
        if (cmd == null)
            return;
        String lower = cmd.toLowerCase();
        String[] parts = lower.split(" ");
        String baseCmd = parts[0];

        if (baseCmd.equals("op") || baseCmd.equals("minecraft:op") ||
                baseCmd.equals("deop") || baseCmd.equals("minecraft:deop")) {

            if (parts.length >= 2) {
                String target = parts[1];
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(target);
                if (!allowed) {
                    plugin.getSchedulerAdapter().runTaskLater(() -> {
                        Player p = Bukkit.getPlayerExact(target);
                        if (p != null && p.isOp()) {
                            p.setOp(false);
                            plugin.getLogger()
                                    .info("Auto-deopped " + target + " (console command - not in allowed operators)");
                        }
                    }, 1L);
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.isOp()) {
            plugin.getSchedulerAdapter().runTaskLaterAsync(() -> {
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(p.getName());
                if (!allowed) {
                    plugin.getSchedulerAdapter().runTask(() -> {
                        p.setOp(false);
                        p.sendMessage("§cYou are not allowed to be opped on this server. You have been de-opped.");
                        plugin.getLogger().info("Auto-deopped " + p.getName() + " on join (not in allowed operators)");
                    });
                }
            }, 10L);
        }
    }
}
