package com.h2ph.listeners;

import com.h2ph.commands.admin.duels.DuelArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DuelGameListener implements Listener {

    private final DuelArenaManager arenaManager;

    public DuelGameListener(DuelArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        if (arenaManager.isLooting(victim)) {
            arenaManager.stopLooting(victim);
            return;
        }

        if (arenaManager.isInDuel(victim)) {
            arenaManager.cacheSpectatorLocation(victim);



            Player killer = victim.getKiller();
            Player opponent = arenaManager.getOpponent(victim);

            Player winner = (killer != null) ? killer : opponent;

            if (winner == null || !winner.getUniqueId().equals(opponent.getUniqueId())) {
                winner = opponent;
            }

            if (winner != null) {
                DuelArenaManager.WinReason reason = arenaManager.isForfeit(victim)
                        ? DuelArenaManager.WinReason.FORFEIT
                        : DuelArenaManager.WinReason.NORMAL;

                arenaManager.endDuel(winner, victim, reason);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (arenaManager.shouldRespawnAtHub(player)) {
            org.bukkit.Location spawn = arenaManager.getPlugin().getSpawnManager().getSpawn("spawn");
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            return;
        }

        org.bukkit.Location spectateLoc = arenaManager.getSpectatorLocation(player);
        if (spectateLoc != null) {
            event.setRespawnLocation(spectateLoc);

            player.setGameMode(org.bukkit.GameMode.SPECTATOR);

            arenaManager.getPlugin().getSchedulerAdapter().runEntityTaskLater(player, () -> {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player leaver = event.getPlayer();
        if (arenaManager.isInDuel(leaver)) {
            arenaManager.markForfeit(leaver);

            leaver.setHealth(0);

        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (arenaManager.isLocationInArena(player.getLocation())) {
            arenaManager.getPlugin().getSchedulerAdapter().runEntityTaskLater(player, () -> {
                if (player.isOnline()) {
                    arenaManager.resetPlayer(player);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (arenaManager.isInDuel(player) || arenaManager.isLooting(player)) {
            String arenaName = arenaManager.getArenaName(player);
            if (arenaName != null && arenaManager.isLocationInArena(event.getBlock().getLocation())) {
                arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
                event.setDropItems(false);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (arenaManager.isInDuel(player) || arenaManager.isLooting(player)) {
            String arenaName = arenaManager.getArenaName(player);
            if (arenaName != null && arenaManager.isLocationInArena(event.getBlock().getLocation())) {
                arenaManager.recordBlockChange(arenaName, event.getBlockReplacedState());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        String arenaName = arenaManager.getArenaAt(event.getLocation());

        if (arenaName == null && !event.blockList().isEmpty()) {
            arenaName = arenaManager.getArenaAt(event.blockList().get(0).getLocation());
        }

        if (arenaName != null) {
            event.setYield(0f);
            for (org.bukkit.block.Block block : event.blockList()) {
                arenaManager.recordBlockChange(arenaName, block.getState());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;
        String arenaName = arenaManager.getArenaAt(event.getBlock().getLocation());

        if (arenaName != null) {
            event.setYield(0f);
            for (org.bukkit.block.Block block : event.blockList()) {
                arenaManager.recordBlockChange(arenaName, block.getState());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        String arenaName = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arenaName != null) {
            arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        String arenaName = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arenaName != null) {
            arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
        }
    }

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (arenaManager.isLooting(player)) {
            if (event.getCause() != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
                    event.getCause() != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
                arenaManager.stopLooting(player);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/duel leave")) {
            return;
        }

        if (arenaManager.isInDuel(player) || arenaManager.isLooting(player)) {
            if (arenaManager.isCommandBanned(event.getMessage())) {
                event.setCancelled(true);
                String warning = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cYou cannot able to use this command on a duels.");
                player.sendMessage(warning);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(warning));
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
                return;
            }
        }

        if (arenaManager.isInDuel(player)) {
            if (arenaManager.isCommandIgnored(event.getMessage())) {
                event.setCancelled(true);
                String warning = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cYou cannot use this command while on duels! &7Type &a/duel leave&7 to left the match.");
                player.sendMessage(warning);
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
