package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;

public class FastCrystalListener implements Listener {

    private final PrismSurvival plugin;

    public FastCrystalListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.END_CRYSTAL)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        if (event.getBlockFace() != org.bukkit.block.BlockFace.UP)
            return;

        if (clickedBlock.getType() != Material.OBSIDIAN)
            return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.isFastCrystals())
            return;

        event.setCancelled(true);

        org.bukkit.Location spawnLoc = clickedBlock.getLocation().add(0.5, 1.0, 0.5);
        boolean exists = spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.5, 0.5, 0.5).stream()
                .anyMatch(e -> e.getType() == EntityType.END_CRYSTAL);

        if (exists)
            return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }

        EnderCrystal crystal = (EnderCrystal) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.END_CRYSTAL);
        crystal.setShowingBottom(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
            return;

        Player player = (Player) event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (data == null || !data.isFastCrystals())
            return;

        player.setMaximumNoDamageTicks(0);
        player.setNoDamageTicks(0);

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                player.setMaximumNoDamageTicks(20);
            }
        }, 1L);
    }
}
