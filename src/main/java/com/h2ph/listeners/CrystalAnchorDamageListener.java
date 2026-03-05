package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class CrystalAnchorDamageListener implements Listener {

    private final PrismSurvival plugin;

    public CrystalAnchorDamageListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Handle RESPAWN_ANCHOR damage
        if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            double customDamage = plugin.getDamageManager().getAnchorDamage();

            // If damage is 0, play villager no sound (silent)
            if (customDamage == 0) {
                event.setCancelled(true);
                Player player = (Player) event.getEntity();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return;
            }

            // Apply custom damage
            event.setDamage(customDamage);
        }

        // Handle ENTITY_EXPLOSION (from crystals)
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            double customDamage = plugin.getDamageManager().getCrystalDamage();

            // If damage is 0, play villager no sound (silent)
            if (customDamage == 0) {
                event.setCancelled(true);
                Player player = (Player) event.getEntity();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return;
            }

            // Apply custom damage
            event.setDamage(customDamage);
        }
    }
}
