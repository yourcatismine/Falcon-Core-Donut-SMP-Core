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

        RespawnAnchor anchor = (RespawnAnchor) clickedBlock.getBlockData();
        
        if (anchor.getCharges() >= anchor.getMaximumCharges()) {
            if (clickedBlock.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
                event.setCancelled(true);
                
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                }
                
                clickedBlock.getWorld().createExplosion(
                    clickedBlock.getLocation().add(0.5, 0.5, 0.5),
                    5.0f,
                    false,
                    true
                );
                
                clickedBlock.setType(Material.AIR);
                
                clickedBlock.getWorld().playSound(
                    clickedBlock.getLocation(),
                    Sound.ENTITY_GENERIC_EXPLODE,
                    1.0f,
                    1.0f
                );
                
                return;
            }
        }
        
        if (anchor.getCharges() < anchor.getMaximumCharges()) {
            event.setCancelled(true);
            
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItemInMainHand(item);
            }
            
            anchor.setCharges(anchor.getCharges() + 1);
            clickedBlock.setBlockData(anchor);
            
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

        player.setMaximumNoDamageTicks(0);
        player.setNoDamageTicks(0);

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                player.setMaximumNoDamageTicks(20);
            }
        }, 1L);
    }
}