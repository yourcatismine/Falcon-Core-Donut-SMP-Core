package com.prismcore.survival.tools;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BucketUseListener implements Listener {

    private final ToolsManager manager;

    public BucketUseListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) {
            return;
        }
        ItemMeta meta = inHand.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                && !meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            return;
        }
        Block clicked = event.getBlockClicked();
        if (clicked == null) {
            event.setCancelled(true);
            return;
        }
        if (clicked.getType() != Material.WATER) {
            event.setCancelled(true);
            return;
        }
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("bucket");
        if (cfg == null) {
            event.setCancelled(true);
            return;
        }
        int rx = cfg.getInt("remove-radius.x");
        int ry = cfg.getInt("remove-radius.y");
        int rz = cfg.getInt("remove-radius.z");
        for (int dx = -rx; dx <= rx; ++dx) {
            for (int dy = -ry; dy <= ry; ++dy) {
                for (int dz = -rz; dz <= rz; ++dz) {
                    Block target = clicked.getRelative(dx, dy, dz);
                    if (target.getType() != Material.WATER)
                        continue;
                    target.setType(Material.AIR);
                }
            }
        }
        event.setCancelled(true);
    }
}
