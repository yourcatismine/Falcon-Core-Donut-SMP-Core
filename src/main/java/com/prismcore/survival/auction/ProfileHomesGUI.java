package com.prismcore.survival.auction;

import com.h2ph.managers.HomeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ProfileHomesGUI {

    public static final int HOME_COUNT = 10;
    public static final int BED_START = 11; // Center position in row 2
    public static final int BED_START_2 = 20; // Second row for homes 6-10

    public static class ProfileHomesHolder implements InventoryHolder {
        private final OfflinePlayer targetPlayer;

        public ProfileHomesHolder(OfflinePlayer targetPlayer) {
            this.targetPlayer = targetPlayer;
        }

        public OfflinePlayer getTargetPlayer() {
            return targetPlayer;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player viewer, OfflinePlayer targetPlayer, HomeManager homeManager) {
        String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";
        String title = Utils.formatColors("&8" + targetName + "'s ʜᴏᴍᴇѕ");
        
        Inventory inv = Bukkit.createInventory(new ProfileHomesHolder(targetPlayer), 36, title);
        
        // Fill with placeholder
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            placeholder.setItemMeta(meta);
        }
        
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, placeholder);
        }
        
        // Slot 1: Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Utils.formatColors("&c« ʙᴀᴄᴋ"));
            backMeta.setLore(List.of(Utils.formatColors("&fClick to go back")));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(1, backButton);
        
        // Load target's homes
        java.util.UUID targetUUID = targetPlayer.getUniqueId();
        java.util.Map<Integer, HomeManager.HomeEntry> homes = homeManager.getHomes(targetUUID);
        
        // Display beds for each home slot
        for (int i = 0; i < HOME_COUNT; i++) {
            int homeNumber = i + 1;
            
            // Calculate slot based on home group
            int bedSlot;
            if (i < 5) {
                // Homes 1-5
                bedSlot = BED_START + i;
            } else {
                // Homes 6-10
                bedSlot = BED_START_2 + (i - 5);
            }
            
            if (homes.containsKey(homeNumber)) {
                // Home is set
                String homeName = homeManager.getHomeName(targetUUID, homeNumber);
                String baseName = Utils.formatColors("&dʜᴏᴍᴇ " + homeNumber);
                String displayName = (homeName != null && !homeName.isEmpty()) ? baseName + " " + homeName : baseName;
                
                // Purple bed: click to teleport
                ItemStack bed = make(Material.PURPLE_BED,
                        displayName,
                        List.of(Utils.formatColors("&fClick to teleport to this home")));
                inv.setItem(bedSlot, bed);
            } else {
                // Home is not set
                ItemStack bed = make(Material.GRAY_BED,
                        Utils.formatColors("&7ɴᴏ ʜᴏᴍᴇ ѕᴇᴛ"),
                        List.of(Utils.formatColors("&f- No home set")));
                inv.setItem(bedSlot, bed);
            }
        }
        
        viewer.openInventory(inv);
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
