package com.h2ph.managers;

import com.h2ph.PrismSurvival;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DeathMessageManager {

    private final PrismSurvival plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private boolean radiusEnabled;
    private int chunkRadius;

    private final Map<EntityDamageEvent.DamageCause, String> causeMapping = new HashMap<>();

    public DeathMessageManager(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfigurations();
        setupCauseMappings();
    }

    /**
     * Load death configuration files
     */
    private void loadConfigurations() {
        File configFile = new File(plugin.getDataFolder(), "survival/death/config.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("Death config.yml not found!");
            return;
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File messagesFile = new File(plugin.getDataFolder(), "survival/death/messages.yml");
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Death messages.yml not found!");
            return;
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        this.radiusEnabled = config.getBoolean("SETTINGS.RADIUS", true);
        this.chunkRadius = config.getInt("SETTINGS.CHUNKS", 5);
    }

    /**
     * Setup mappings between damage causes and message keys
     */
    private void setupCauseMappings() {
        causeMapping.put(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, "BLOCK-EXPLOSION");
        causeMapping.put(EntityDamageEvent.DamageCause.CONTACT, "CONTACT");
        causeMapping.put(EntityDamageEvent.DamageCause.DROWNING, "DROWNING");
        causeMapping.put(EntityDamageEvent.DamageCause.ENTITY_ATTACK, "ENTITY-ATTACK");
        causeMapping.put(EntityDamageEvent.DamageCause.FALL, "FALL");
        causeMapping.put(EntityDamageEvent.DamageCause.FALLING_BLOCK, "FALLING-BLOCK");
        causeMapping.put(EntityDamageEvent.DamageCause.FIRE, "FIRE");
        causeMapping.put(EntityDamageEvent.DamageCause.FIRE_TICK, "FIRE-TICK");
        causeMapping.put(EntityDamageEvent.DamageCause.LAVA, "LAVA");
        causeMapping.put(EntityDamageEvent.DamageCause.LIGHTNING, "LIGHTNING");
        causeMapping.put(EntityDamageEvent.DamageCause.POISON, "POISON");
        causeMapping.put(EntityDamageEvent.DamageCause.PROJECTILE, "PROJECTILE");
        causeMapping.put(EntityDamageEvent.DamageCause.STARVATION, "STARVATION");
        causeMapping.put(EntityDamageEvent.DamageCause.SUFFOCATION, "SUFFOCATION");
        causeMapping.put(EntityDamageEvent.DamageCause.SUICIDE, "SUICIDE");
        causeMapping.put(EntityDamageEvent.DamageCause.THORNS, "THORNS");
        causeMapping.put(EntityDamageEvent.DamageCause.VOID, "VOID");
        causeMapping.put(EntityDamageEvent.DamageCause.WITHER, "WITHER");
        causeMapping.put(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, "ENTITY-EXPLOSION");
    }

    /**
     * Get custom death message for a player based on death cause and settings
     */
    public String getDeathMessage(Player victim, EntityDamageEvent.DamageCause cause, Player killer, String killerEntity) {
        if (!messages.getBoolean("MESSAGES.ENABLED", true)) {
            return null;
        }

        String messageKey = causeMapping.getOrDefault(cause, "DEFAULT");
        String messagePath = "MESSAGES." + messageKey;
        
        String message;
        String selectedPath = "";
        
        if (killer != null && messages.contains(messagePath + ".PVP")) {
            message = messages.getString(messagePath + ".PVP");
            selectedPath = messagePath + ".PVP";
        } else if (killerEntity != null && messages.contains(messagePath + ".NORMAL")) {
            message = messages.getString(messagePath + ".NORMAL");
            selectedPath = messagePath + ".NORMAL";
        } else if (messages.contains(messagePath + ".NORMAL")) {
            message = messages.getString(messagePath + ".NORMAL");
            selectedPath = messagePath + ".NORMAL";
        } else if (messages.contains(messagePath)) {
            message = messages.getString(messagePath);
            selectedPath = messagePath;
        } else {
            message = messages.getString("MESSAGES.DEFAULT", "{player} died");
            selectedPath = "MESSAGES.DEFAULT";
        }

        if (message == null) {
            message = messages.getString("MESSAGES.DEFAULT", "{player} died");
            selectedPath = "MESSAGES.DEFAULT (fallback)";
        }

        message = message.replace("{player}", victim.getName());
        if (killer != null) {
            message = message.replace("{killer}", killer.getName());
        } else if (killerEntity != null) {
            message = message.replace("{killer}", killerEntity);
        } else {
            message = message.replace("{killer}", "Unknown");
        }

        String prefix = messages.getString("MESSAGES.PREFIX", "");
        if (!prefix.isEmpty()) {
            message = prefix + message;
        }

        message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    /**
     * Backward compatibility method for 3-parameter call
     */
    public String getDeathMessage(Player victim, EntityDamageEvent.DamageCause cause, Player killer) {
        return getDeathMessage(victim, cause, killer, null);
    }

    /**
     * Check if a player should receive the death message based on radius settings
     */
    public boolean shouldReceiveMessage(Player receiver, Player victim) {
        if (!radiusEnabled) {
            return true;
        }

        Location victimLoc = victim.getLocation();
        Location receiverLoc = receiver.getLocation();

        if (!victimLoc.getWorld().equals(receiverLoc.getWorld())) {
            return false;
        }

        int victimChunkX = victimLoc.getBlockX() >> 4;
        int victimChunkZ = victimLoc.getBlockZ() >> 4;
        int receiverChunkX = receiverLoc.getBlockX() >> 4;
        int receiverChunkZ = receiverLoc.getBlockZ() >> 4;

        int chunkDistance = Math.max(
            Math.abs(victimChunkX - receiverChunkX),
            Math.abs(victimChunkZ - receiverChunkZ)
        );

        return chunkDistance <= chunkRadius;
    }

    /**
     * Reload configurations
     */
    public void reload() {
        loadConfigurations();
        setupCauseMappings();
    }

    /**
     * Check if death messages are enabled
     */
    public boolean isEnabled() {
        return messages != null && messages.getBoolean("MESSAGES.ENABLED", true);
    }

    public boolean isRadiusEnabled() {
        return radiusEnabled;
    }

    public int getChunkRadius() {
        return chunkRadius;
    }
}