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
            return; // Death messages disabled, use vanilla behavior
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        String killerEntity = null;

        // Get the damage cause and killer entity info
        EntityDamageEvent.DamageCause cause = EntityDamageEvent.DamageCause.CUSTOM;
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (lastDamage != null) {
            cause = lastDamage.getCause();
            
            // If no player killer but there's an entity attacker, get the entity name
            if (killer == null && lastDamage instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent entityDamage = 
                    (org.bukkit.event.entity.EntityDamageByEntityEvent) lastDamage;
                org.bukkit.entity.Entity damager = entityDamage.getDamager();
                
                if (damager != null) {
                    // Get a more readable name for the entity
                    killerEntity = getEntityDisplayName(damager);
                }
            }
        }

        // Hide vanilla death message
        event.setDeathMessage(null);

        // Get custom death message
        String customMessage = deathManager.getDeathMessage(victim, cause, killer, killerEntity);
        
        if (customMessage == null || customMessage.isEmpty()) {
            return; // No custom message found, hide death message completely
        }

        // Send death message to appropriate players based on radius settings
        if (deathManager.isRadiusEnabled()) {
            // Send to players within chunk radius
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (deathManager.shouldReceiveMessage(onlinePlayer, victim)) {
                    onlinePlayer.sendMessage(customMessage);
                }
            }
        } else {
            // Send to all online players
            Bukkit.broadcastMessage(customMessage);
        }

        // Log death message to console (remove color codes for clean console output)
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
        
        // Handle named entities
        if (entity instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;
            if (living.getCustomName() != null) {
                return living.getCustomName();
            }
        }
        
        // Handle projectiles shot by entities
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
        // Convert IRON_GOLEM to "Iron Golem"
        return java.util.Arrays.stream(type.split("_"))
            .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
            .collect(java.util.stream.Collectors.joining(" "));
    }
}