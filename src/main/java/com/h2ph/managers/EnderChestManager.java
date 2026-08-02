package com.h2ph.managers;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class EnderChestManager {
    public EnderChestManager(Falcon plugin) {}
    
    public ItemStack[] loadEnderChest(UUID uuid) {
        String base64 = Falcon.getInstance().getDatabaseManager().getYamlStorage().loadEnderChest(uuid);
        if (base64 != null && !base64.isEmpty()) {
            try {
                return com.falconcore.survival.utils.ItemSerializationManager.itemStackArrayFromBase64(base64);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ItemStack[54];
    }
    
    public Inventory getOrCreateInventory(UUID uuid, String name, Object extra, ItemStack[] contents) {
        Inventory inv = Bukkit.createInventory(null, 54, name != null ? name : "EnderChest");
        if (contents != null && contents.length <= 54) {
            inv.setContents(contents);
        } else if (contents != null && contents.length > 54) {
            ItemStack[] resized = new ItemStack[54];
            System.arraycopy(contents, 0, resized, 0, 54);
            inv.setContents(resized);
        }
        return inv;
    }
    
    public Map<UUID, Inventory> getActiveInventories() {
        return new HashMap<>();
    }
    
    public void saveEnderChest(UUID uuid, ItemStack[] contents) {
        try {
            String base64 = com.falconcore.survival.utils.ItemSerializationManager.itemStackArrayToBase64(contents);
            Falcon.getInstance().getDatabaseManager().getYamlStorage().saveEnderChest(uuid, base64);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void wipeEnderChest(UUID uuid) {}
    
    public void registerViewer(Block block, Player player) {}
    
    public void unregisterViewer(Block block, Player player) {}
    
    public void preload(UUID uuid, String name) {}
    
    public void unload(UUID uuid) {}
}
