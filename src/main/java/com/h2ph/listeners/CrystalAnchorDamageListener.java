package com.h2ph.listeners;

import com.h2ph.Falcon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class CrystalAnchorDamageListener implements Listener {

    private final Falcon plugin;

    public CrystalAnchorDamageListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            double customDamage = plugin.getDamageManager().getAnchorDamage();

            if (customDamage == 0) {
                event.setCancelled(true);
                Player player = (Player) event.getEntity();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return;
            }

            event.setDamage(customDamage);
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            double customDamage = plugin.getDamageManager().getCrystalDamage();

            if (customDamage == 0) {
                event.setCancelled(true);
                Player player = (Player) event.getEntity();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return;
            }

            event.setDamage(customDamage);
        }
    }
}
