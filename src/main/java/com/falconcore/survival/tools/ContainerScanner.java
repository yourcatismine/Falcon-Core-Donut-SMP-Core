package com.falconcore.survival.tools;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.h2ph.Falcon;

public class ContainerScanner {

    private final ToolsManager toolsManager;

    public ContainerScanner(Falcon plugin, ToolsManager toolsManager) {
        this.toolsManager = toolsManager;
    }

    /**
     * Scans all loaded containers in all worlds for amethyst tools.
     * This should be called periodically by a scheduled task.
     */
    public void scanAllLoadedContainers() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunkContainers(chunk);
            }
        }
    }

    /**
     * Scans all containers in a specific chunk.
     */
    private void scanChunkContainers(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof org.bukkit.block.Container) {
                org.bukkit.block.Container container = (org.bukkit.block.Container) state;
                scanInventory(container.getInventory(), null, null);
            }
        }
    }

    /**
     * Scans an inventory for amethyst tools and processes expiration.
     * 
     * @param inventory     The inventory to scan
     * @param soundLocation Optional location to play sounds (null for containers)
     * @param player        Optional player reference to check cursor (null for
     *                      containers)
     */
    public void scanInventory(Inventory inventory, org.bukkit.Location soundLocation, org.bukkit.entity.Player player) {
        if (player != null) {
            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR) {
                return;
            }
        }

        ItemStack[] contents = inventory.getContents();
        boolean modified = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            if (!isAmethystTool(meta)) {
                continue;
            }

            if (meta.getPersistentDataContainer().has(ToolsManager.AUCTION_PAUSED_KEY, PersistentDataType.BYTE)) {
                continue;
            }
            
            if (meta.getPersistentDataContainer().has(ToolsManager.ORDERS_PAUSED_KEY, PersistentDataType.LONG) ||
                meta.getPersistentDataContainer().has(ToolsManager.SHOP_PAUSED_KEY, PersistentDataType.LONG) ||
                meta.getPersistentDataContainer().has(ToolsManager.STORAGE_PAUSED_KEY, PersistentDataType.LONG)) {
                continue;
            }

            String configKey = getToolConfigKey(item, meta);
            if (configKey == null) {
                continue;
            }

            if (!toolsManager.getConfig().getBoolean(configKey + ".use-countdown", true)) {
                continue;
            }

            Long expiryTime = getExpirationTimestamp(meta);
            if (expiryTime == null) {
                continue;
            }

            long currentTime = System.currentTimeMillis();

            if (expiryTime <= currentTime) {
                contents[i] = null;
                modified = true;

                if (soundLocation != null) {
                    soundLocation.getWorld().playSound(soundLocation, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 2.0f);
                }
                continue;
            }

            long lastUpdate = meta.getPersistentDataContainer().getOrDefault(ToolsManager.LAST_UPDATE_KEY,
                    PersistentDataType.LONG, 0L);
            long updateIntervalMs = toolsManager.getConfig().getLong(configKey + ".update-interval", 30L) * 1000L;

            if (currentTime - lastUpdate < updateIntervalMs) {
                continue;
            }

            long remainingSeconds = (expiryTime - currentTime) / 1000L;
            String countdown = Utils.formatDuration(remainingSeconds);
            List<String> templateLore = toolsManager.getConfig().getStringList(configKey + ".lore");
            List<String> updatedLore = templateLore.stream()
                    .map(line -> line.replace("%countdown%", countdown))
                    .map(Utils::formatColors)
                    .toList();

            meta.setLore(updatedLore);
            meta.getPersistentDataContainer().set(ToolsManager.LAST_UPDATE_KEY, PersistentDataType.LONG, currentTime);
            item.setItemMeta(meta);
            modified = true;

            if (item.getType().name().contains("SHULKER_BOX") && meta instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) meta;
                if (bsm.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
                    scanInventory(shulker.getInventory(), null, null);
                    bsm.setBlockState(shulker);
                    item.setItemMeta(bsm);
                }
            }
        }

        if (modified) {
            for (int i = 0; i < contents.length; i++) {
                inventory.setItem(i, contents[i]);
            }
        }
    }

    /**
     * Checks if an item is an amethyst tool.
     */
    private boolean isAmethystTool(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)
                || meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                || meta.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)
                || meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE);
    }

    /**
     * Gets the config key for a tool (drill, axe, shovel, multitool, bucket,
     * shardbooster, sellaxe).
     */
    private String getToolConfigKey(ItemStack item, ItemMeta meta) {
        if (meta.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return "multitool";
        }
        if (meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE)) {
            return "shardbooster";
        }
        if (meta.getPersistentDataContainer().has(ToolsManager.SELL_AXE_KEY, PersistentDataType.BYTE)) {
            return "sellaxe";
        }

        String matName = item.getType().name();
        if (matName.endsWith("_PICKAXE")) {
            return "drill";
        } else if (matName.endsWith("_AXE")) {
            return "axe";
        } else if (matName.endsWith("_SHOVEL")) {
            return "shovel";
        } else if (matName.endsWith("_BUCKET") || matName.equals("BUCKET")) {
            return "bucket";
        }

        return null;
    }

    /**
     * Gets the expiration timestamp, migrating from REMAINING_KEY if needed.
     */
    private Long getExpirationTimestamp(ItemMeta meta) {
        if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            return meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG);
        }

        if (meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)) {
            Long remainingSeconds = meta.getPersistentDataContainer().get(ToolsManager.REMAINING_KEY,
                    PersistentDataType.LONG);
            long expiryTimestamp = System.currentTimeMillis() + (remainingSeconds * 1000L);

            meta.getPersistentDataContainer().remove(ToolsManager.REMAINING_KEY);
            meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG, expiryTimestamp);

            return expiryTimestamp;
        }

        return null;
    }
}
