package com.prismcore.survival.tools;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
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

    public ShardBoosterListener(PrismSurvival plugin) {
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

        // Get booster duration (priority: REMAINING_KEY > EXPIRY_KEY > default 24h)
        long durationSeconds = 86400L;
        if (meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)) {
            durationSeconds = meta.getPersistentDataContainer().get(ToolsManager.REMAINING_KEY,
                    PersistentDataType.LONG);
        } else if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            long expiryTime = meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG);
            durationSeconds = Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000L);
        }

        if (durationSeconds <= 0) {
            player.sendMessage(ChatColor.RED + "This booster has already expired!");
            return;
        }

        // Calculate expiry timestamp
        long expiryMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Activate booster for player
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        data.setShardBoosterExpiry(expiryMillis);

        // Award 8 shards if the player has the passive permission or is in an AFK
        // region
        boolean inAfkRegion = plugin.getAfkManager().getRegionAt(player.getLocation()) != null;
        if (player.hasPermission("prism.shards.passive") || inAfkRegion) {
            data.addShards(8, "Shard Booster Reward");
        }

        plugin.getPlayerDataManager().savePlayerAsync(player.getUniqueId());

        // Play activation sound
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        // Send activation message
        String msg = ChatColor.translateAlternateColorCodes('&', "&dShard booster activated.");
        player.sendMessage(msg);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
    }
}
