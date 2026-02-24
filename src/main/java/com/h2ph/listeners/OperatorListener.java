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
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null)
            return;
        String lower = msg.toLowerCase();
        if (lower.startsWith("/op ") || lower.equals("/op")) {
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                String target = parts[1];
                // If not allowed, cancel and notify
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(target);
                if (!allowed) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage("§cThat player is not in the allowed operators list. Operation denied.");
                    // ensure target is de-opped if they somehow were
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
        if (lower.startsWith("op ") || lower.equals("op")) {
            String[] parts = cmd.split(" ");
            if (parts.length >= 2) {
                String target = parts[1];
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(target);
                if (!allowed) {
                    // For console, we cannot "cancel" server internal op easily, so
                    // schedule to deop immediately after (1 tick delay)
                    plugin.getSchedulerAdapter().runTaskLater(() -> {
                        Player p = Bukkit.getPlayerExact(target);
                        if (p != null && p.isOp()) {
                            p.setOp(false);
                            plugin.getLogger().info("Auto-deopped " + target + " (not in allowed operators)");
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
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                boolean allowed = plugin.getDatabaseManager().isAllowedOperator(p.getName());
                if (!allowed) {
                    plugin.getSchedulerAdapter().runTask(() -> {
                        p.setOp(false);
                        p.sendMessage("§cYou are not allowed to be opped on this server. You have been de-opped.");
                        plugin.getLogger().info("Auto-deopped " + p.getName() + " on join (not in allowed operators)");
                    });
                }
            });
        }
    }
}
