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

        // Fast Crystals can only be placed on the top face of obsidian
        if (event.getBlockFace() != org.bukkit.block.BlockFace.UP)
            return;

        if (clickedBlock.getType() != Material.OBSIDIAN)
            return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.isFastCrystals())
            return;

        // Cancel vanilla placement event to bypass vanilla delays
        event.setCancelled(true);

        // Check if a crystal already exists at this location (No Stacking)
        org.bukkit.Location spawnLoc = clickedBlock.getLocation().add(0.5, 1.0, 0.5);
        boolean exists = spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.5, 0.5, 0.5).stream()
                .anyMatch(e -> e.getType() == EntityType.END_CRYSTAL);

        if (exists)
            return;

        // Consume the crystal manually if not in creative
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item); // Update hand visually
        }

        // Spawn perfectly centered crystal without the vanilla bedrock pedestal
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

        // The core of the "reduced I-Frames" mechanic for stackable crystals.
        // We set the player's max no-damage ticks to 0 temporarily.
        // This causes the CURRENT damage event to apply, and when vanilla tries to set
        // noDamageTicks=20, it tops out at 0.
        // This allows compounding/simultaneous explosions to ALL deal damage in the
        // same tick!
        player.setMaximumNoDamageTicks(0);
        player.setNoDamageTicks(0);

        // Schedule restoring their I-frames next tick so normal combat isn't
        // permanently broken
        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            if (player.isOnline()) {
                player.setMaximumNoDamageTicks(20);
            }
        }, 1L);
    }
}
