package com.prismcore.survival.tools;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener for Shard Booster potion consumption.
 * When a player drinks the shard booster, it activates 4x AFK shard production
 * for 24 hours.
 */
public class ShardBoosterListener implements Listener {

    private final PrismSurvival plugin;
    private final ToolsManager manager;

    public ShardBoosterListener(ToolsManager manager, PrismSurvival plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Check if this is a shard booster
        if (!meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE)) {
            return;
        }

        Player player = event.getPlayer();

        // Get booster duration from stored REMAINING_KEY (in seconds)
        long durationSeconds = 86400L; // Default 24 hours
        if (meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)) {
            durationSeconds = meta.getPersistentDataContainer().get(ToolsManager.REMAINING_KEY,
                    PersistentDataType.LONG);
        }

        // Calculate expiry timestamp
        long expiryMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Activate booster for player
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        data.setShardBoosterExpiry(expiryMillis);
        plugin.getPlayerDataManager().savePlayerAsync(player.getUniqueId());

        // Get config for messages and sounds
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("shardbooster");

        // Play activation sound
        String soundName = cfg != null ? cfg.getString("activation-sound", "ENTITY_PLAYER_LEVELUP")
                : "ENTITY_PLAYER_LEVELUP";
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Send activation message
        String message = cfg != null
                ? cfg.getString("activation-message", "&aYou have activated your &5Shard Booster&a for 24h.")
                : "&aYou have activated your &5Shard Booster&a for 24h.";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
