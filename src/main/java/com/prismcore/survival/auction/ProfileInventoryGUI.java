package com.prismcore.survival.auction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ProfileInventoryGUI {

    public static class ProfileInventoryHolder implements InventoryHolder {
        private final UUID targetPlayerUUID;
        private final String targetPlayerName;

        public ProfileInventoryHolder(UUID targetPlayerUUID, String targetPlayerName) {
            this.targetPlayerUUID = targetPlayerUUID;
            this.targetPlayerName = targetPlayerName;
        }

        public UUID getTargetPlayerUUID() {
            return targetPlayerUUID;
        }

        public String getTargetPlayerName() {
            return targetPlayerName;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player viewer, Player targetPlayer) {
        String targetName = targetPlayer.getName();
        String title = Utils.formatColors("&8" + targetName + "'s ɪɴᴠᴇɴᴛᴏʀʏ");
        
        // Create a custom inventory with proper title
        Inventory customInv = Bukkit.createInventory(
                new ProfileInventoryHolder(targetPlayer.getUniqueId(), targetName),
                36,
                title);
        
        // Copy all items from target player's inventory
        // Slots 0-26: Main inventory storage
        ItemStack[] storageContents = targetPlayer.getInventory().getStorageContents();
        for (int i = 0; i < Math.min(27, storageContents.length); i++) {
            customInv.setItem(i, storageContents[i] != null ? storageContents[i].clone() : null);
        }
        
        // Slots 27-30: Armor (boots, leggings, chestplate, helmet)
        ItemStack[] armor = targetPlayer.getInventory().getArmorContents();
        for (int i = 0; i < Math.min(4, armor.length); i++) {
            customInv.setItem(27 + i, armor[i] != null ? armor[i].clone() : null);
        }
        
        // Slot 31: Offhand
        ItemStack offhand = targetPlayer.getInventory().getItemInOffHand();
        customInv.setItem(31, offhand != null ? offhand.clone() : null);
        
        viewer.openInventory(customInv);
    }
}
