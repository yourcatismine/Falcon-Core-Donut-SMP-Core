package com.prismcore.survival.tools;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import com.prismcore.survival.tools.Utils;
import com.h2ph.PrismSurvival;

import java.util.List;

/**
 * Utility class for managing amethyst tool timers during storage/retrieval operations.
 * Provides pause/resume functionality similar to the auction system.
 */
public class AmethystTimerManager {
    
    private final ToolsManager toolsManager;
    
    public AmethystTimerManager(ToolsManager toolsManager) {
        this.toolsManager = toolsManager;
    }
    
    /**
     * Recursively pauses amethyst tool timers in an item (including inside shulker boxes).
     * Uses a custom pause key to track storage timestamp.
     * 
     * @param item The item to pause timers for
     * @param storageTimestamp The timestamp when item was stored
     * @param pauseKey The namespace key to use for marking pause state
     */
    public void pauseAmethystTimers(ItemStack item, long storageTimestamp, org.bukkit.NamespacedKey pauseKey) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        if (isAmethystTool(meta)) {
            meta.getPersistentDataContainer().set(pauseKey, PersistentDataType.LONG, storageTimestamp);
            item.setItemMeta(meta);
        }
        
        if (item.getType().name().contains("SHULKER_BOX") && meta instanceof BlockStateMeta) {
            BlockStateMeta bsm = (BlockStateMeta) meta;
            if (bsm.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
                ItemStack[] contents = shulker.getInventory().getContents();
                
                for (ItemStack content : contents) {
                    if (content != null) {
                        pauseAmethystTimers(content, storageTimestamp, pauseKey);
                    }
                }
                
                bsm.setBlockState(shulker);
                item.setItemMeta(bsm);
            }
        }
    }
    
    /**
     * Recursively resumes amethyst tool timers in an item (including inside shulker boxes).
     * 
     * @param item The item to resume timers for
     * @param pauseKey The namespace key used for marking pause state
     */
    public void resumeAmethystTimers(ItemStack item, org.bukkit.NamespacedKey pauseKey) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        if (isAmethystTool(meta) && meta.getPersistentDataContainer().has(pauseKey, PersistentDataType.LONG)) {
            long storageTimestamp = meta.getPersistentDataContainer().get(pauseKey, PersistentDataType.LONG);
            
            meta.getPersistentDataContainer().remove(pauseKey);
            
            if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
                long currentExpiry = meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY,
                        PersistentDataType.LONG);
                long timeInStorage = System.currentTimeMillis() - storageTimestamp;
                
                if (timeInStorage > 0) {
                    long newExpiry = currentExpiry + timeInStorage;
                    meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG,
                            newExpiry);
                    
                    updateToolLore(item, meta, newExpiry);
                }
            }
            
            item.setItemMeta(meta);
        }
        
        if (item.getType().name().contains("SHULKER_BOX") && meta instanceof BlockStateMeta) {
            BlockStateMeta bsm = (BlockStateMeta) meta;
            if (bsm.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
                ItemStack[] contents = shulker.getInventory().getContents();
                
                for (ItemStack content : contents) {
                    if (content != null) {
                        resumeAmethystTimers(content, pauseKey);
                    }
                }
                
                bsm.setBlockState(shulker);
                item.setItemMeta(bsm);
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
                || meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE)
                || meta.getPersistentDataContainer().has(ToolsManager.SELL_AXE_KEY, PersistentDataType.BYTE);
    }
    
    /**
     * Updates an amethyst tool's lore to display the correct countdown after resume.
     */
    private void updateToolLore(ItemStack item, ItemMeta meta, long expiryTime) {
        String configKey = getToolConfigKey(item);
        if (configKey == null) {
            return;
        }
        
        ConfigurationSection cfg = toolsManager.getConfig().getConfigurationSection(configKey);
        if (cfg == null || !cfg.getBoolean("use-countdown", true)) {
            return;
        }
        
        long remainingTime = Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000);
        String countdown = Utils.formatDuration(remainingTime);
        
        List<String> templateLore = cfg.getStringList("lore");
        List<String> updatedLore = templateLore.stream()
                .map(line -> line.replace("%countdown%", countdown))
                .map(Utils::formatColors)
                .toList();
        
        meta.setLore(updatedLore);
        meta.getPersistentDataContainer().set(ToolsManager.LAST_UPDATE_KEY, PersistentDataType.LONG, 
                System.currentTimeMillis());
    }
    
    /**
     * Gets the config key for a tool (drill, axe, shovel, multitool, bucket, shardbooster, sellaxe).
     */
    private String getToolConfigKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        if (meta.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return "multitool";
        } else if (meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE)) {
            return "shardbooster";
        } else if (meta.getPersistentDataContainer().has(ToolsManager.SELL_AXE_KEY, PersistentDataType.BYTE)) {
            return "sellaxe";
        } else {
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
        }
        
        return null;
    }
}