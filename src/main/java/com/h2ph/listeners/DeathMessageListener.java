package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.DeathMessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathMessageListener implements Listener {

    private final PrismSurvival plugin;
    private final DeathMessageManager deathManager;

    public DeathMessageListener(PrismSurvival plugin) {
        this.plugin = plugin;
        this.deathManager = plugin.getDeathMessageManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathManager.isEnabled()) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        String killerEntity = null;

        EntityDamageEvent.DamageCause cause = EntityDamageEvent.DamageCause.CUSTOM;
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (lastDamage != null) {
            cause = lastDamage.getCause();
            
            if (killer == null && lastDamage instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent entityDamage = 
                    (org.bukkit.event.entity.EntityDamageByEntityEvent) lastDamage;
                org.bukkit.entity.Entity damager = entityDamage.getDamager();
                
                if (damager != null) {
                    killerEntity = getEntityDisplayName(damager);
                }
            }
        }

        event.setDeathMessage(null);

        String customMessage = deathManager.getDeathMessage(victim, cause, killer, killerEntity);
        
        if (customMessage == null || customMessage.isEmpty()) {
            return;
        }

        if (deathManager.isRadiusEnabled()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (deathManager.shouldReceiveMessage(onlinePlayer, victim)) {
                    onlinePlayer.sendMessage(customMessage);
                }
            }
        } else {
            Bukkit.broadcastMessage(customMessage);
        }

        String cleanMessage = customMessage.replaceAll("§[0-9a-fk-or]", "");
        plugin.getLogger().info("Death: " + cleanMessage);
    }

    /**
     * Get a display name for an entity
     */
    private String getEntityDisplayName(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        
        if (entity instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;
            if (living.getCustomName() != null) {
                return living.getCustomName();
            }
        }
        
        if (entity instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) entity;
            if (projectile.getShooter() instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity shooter = (org.bukkit.entity.LivingEntity) projectile.getShooter();
                if (shooter instanceof Player) {
                    return ((Player) shooter).getName();
                } else {
                    return getEntityType(shooter);
                }
            }
        }
        
        return getEntityType(entity);
    }

    /**
     * Get a readable entity type name
     */
    private String getEntityType(org.bukkit.entity.Entity entity) {
        String type = entity.getType().name();
        return java.util.Arrays.stream(type.split("_"))
            .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
            .collect(java.util.stream.Collectors.joining(" "));
    }
}