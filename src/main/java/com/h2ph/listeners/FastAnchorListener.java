package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import org.bukkit.Sound;

public class FastAnchorListener implements Listener {

    private final PrismSurvival plugin;

    public FastAnchorListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnchorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.GLOWSTONE)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.RESPAWN_ANCHOR)
            return;

        // Get the respawn anchor data
        RespawnAnchor anchor = (RespawnAnchor) clickedBlock.getBlockData();
        
        // Check if anchor is already at max charge (4) - this is when it should explode in the Nether
        if (anchor.getCharges() >= anchor.getMaximumCharges()) {
            // Check if we're in the Nether (where anchors explode)
            if (clickedBlock.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
                // Cancel vanilla event to prevent vanilla delay
                event.setCancelled(true);
                
                // Consume the glowstone manually if not in creative
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                }
                
                // Create immediate explosion at anchor location
                // Use the same explosion power as vanilla respawn anchor (5.0f)
                clickedBlock.getWorld().createExplosion(
                    clickedBlock.getLocation().add(0.5, 0.5, 0.5), // Center of block
                    5.0f, // Explosion power (vanilla anchor power)
                    false, // Don't set fire
                    true // Break blocks
                );
                
                // Remove the anchor block
                clickedBlock.setType(Material.AIR);
                
                // Play the explosion sound
                clickedBlock.getWorld().playSound(
                    clickedBlock.getLocation(),
                    Sound.ENTITY_GENERIC_EXPLODE,
                    1.0f,
                    1.0f
                );
                
                return;
            }
        }
        
        // For non-explosive interactions (charging the anchor), allow vanilla behavior
        // but make it instant by cancelling and manually updating
        if (anchor.getCharges() < anchor.getMaximumCharges()) {
            // Cancel vanilla event to prevent delay
            event.setCancelled(true);
            
            // Consume the glowstone manually if not in creative
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItemInMainHand(item);
            }
            
            // Increase anchor charge instantly
            anchor.setCharges(anchor.getCharges() + 1);
            clickedBlock.setBlockData(anchor);
            
            // Play the charging sound
            clickedBlock.getWorld().playSound(
                clickedBlock.getLocation(),
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                1.0f,
                1.0f
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
            return;

        Player player = (Player) event.getEntity();

        // Fast anchor always enabled for all players - reduce I-frames for anchor explosions
        // This allows multiple anchor explosions to stack damage like fast crystals
        player.setMaximumNoDamageTicks(0);
        player.setNoDamageTicks(0);

        // Schedule restoring their I-frames next tick so normal combat isn't permanently broken
        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                player.setMaximumNoDamageTicks(20);
            }
        }, 1L);
    }
}