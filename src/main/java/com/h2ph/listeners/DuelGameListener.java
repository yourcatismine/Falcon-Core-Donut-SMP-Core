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

        // Check if winner in looting phase died (e.g. killed themselves)
        if (arenaManager.isLooting(victim)) {
            arenaManager.stopLooting(victim);
            return;
        }

        if (arenaManager.isInDuel(victim)) {
            // CRITICAL: Cache spectator location IMMEDIATELY before respawnImmediately
            // fires
            arenaManager.cacheSpectatorLocation(victim);

            // It's a duel death!

            // 1. Handle Drops & Messages
            // User requested winner to loot, so we keep drops enabled (default).
            // Maybe silence death message? User didn't specify, but cleaner without.
            // Let's keep it visible for now or set to null if preferred.
            // Often duel plugins hide it or make it custom. For now, default behavior.

            // 2. Determine Winner
            Player killer = victim.getKiller();
            Player opponent = arenaManager.getOpponent(victim);

            // Fallback if killer is null (e.g. died to environmentally damage)
            Player winner = (killer != null) ? killer : opponent;

            // Ensure the winner is actually the opponent (in case of interference, though
            // likely duel is isolated)
            if (winner == null || !winner.getUniqueId().equals(opponent.getUniqueId())) {
                winner = opponent;
            }

            if (winner != null) {
                // 3. Trigger End Duel
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

        // Check if they need to go to Hub (late respawn)
        if (arenaManager.shouldRespawnAtHub(player)) {
            org.bukkit.Location spawn = arenaManager.getPlugin().getSpawnManager().getSpawn("spawn");
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
            return;
        }

        org.bukkit.Location spectateLoc = arenaManager.getSpectatorLocation(player);
        if (spectateLoc != null) {
            // They are actively spectating a lost duel!
            event.setRespawnLocation(spectateLoc);

            // Set GameMode immediately to ensure they spawn as Spectator
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);

            // Schedule as fallback for server resets
            arenaManager.getPlugin().getSchedulerAdapter().runEntityTaskLater(player, () -> {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player leaver = event.getPlayer();
        if (arenaManager.isInDuel(leaver)) {
            // Combat Logged!
            // Mark as forfeit so the winner sees "Opponent Left"
            arenaManager.markForfeit(leaver);

            // Kill the player so they drop items for the winner
            leaver.setHealth(0);

            // We do NOT manualy call endDuel here anymore, because setHealth(0)
            // will trigger PlayerDeathEvent, which calls endDuel.
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if they joined back inside an arena (e.g. crashed, or quit while
        // spectating/dueling)
        if (arenaManager.isLocationInArena(player.getLocation())) {
            // Delay reset by 1 tick - Folia requires chunk loader to be fully initialized
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
        // Track for both dueling AND looting players
        if (arenaManager.isInDuel(player) || arenaManager.isLooting(player)) {
            String arenaName = arenaManager.getArenaName(player);
            if (arenaName != null && arenaManager.isLocationInArena(event.getBlock().getLocation())) {
                // Record state BEFORE break
                arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
                // Prevent block drops in arena
                event.setDropItems(false);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        // Track for both dueling AND looting players
        if (arenaManager.isInDuel(player) || arenaManager.isLooting(player)) {
            String arenaName = arenaManager.getArenaName(player);
            if (arenaName != null && arenaManager.isLocationInArena(event.getBlock().getLocation())) {
                // Record what was there BEFORE placement (usually Air)
                arenaManager.recordBlockChange(arenaName, event.getBlockReplacedState());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.blockList().isEmpty())
            return;

        // Optimization: Check one block first to find arena? Or check logic?
        // Explosions can cross boundaries, but usually localized.
        // We'll check the source location or the first block.
        String arenaName = arenaManager.getArenaAt(event.getLocation());

        // If source is not in arena (e.g. edge case), check first block?
        if (arenaName == null && !event.blockList().isEmpty()) {
            arenaName = arenaManager.getArenaAt(event.blockList().get(0).getLocation());
        }

        if (arenaName != null) {
            // Prevent block drops from explosion in arena
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
            // Prevent block drops from explosion in arena
            event.setYield(0f);
            for (org.bukkit.block.Block block : event.blockList()) {
                arenaManager.recordBlockChange(arenaName, block.getState());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        // Track fire spread in arenas
        String arenaName = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arenaName != null) {
            // Record the original state before fire spreads to it
            arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        // Track blocks being burned in arenas
        String arenaName = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arenaName != null) {
            // Record the block state before it burns away
            arenaManager.recordBlockChange(arenaName, event.getBlock().getState());
        }
    }

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (arenaManager.isLooting(player)) {
            // Cancel looting/restore text if they leave via TP (Command, Plugin, etc)
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

        // Always allow /duel leave
        if (msg.startsWith("/duel leave")) {
            return;
        }

        // Check banned-commands (blocked during BOTH duel and looting)
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

        // Check ignored-commands (blocked only during active duel, not looting)
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
