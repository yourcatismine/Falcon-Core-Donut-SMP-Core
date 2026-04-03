package com.falconcore.survival.spawners.gui;

import com.h2ph.Falcon;
import com.falconcore.survival.spawners.holder.SpawnerGUIHolder;
import com.falconcore.survival.spawners.storage.SpawnerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FilterGUI {
    private final Falcon plugin;
    private final SpawnerData data;
    private final Inventory inventory;

    public FilterGUI(Falcon plugin, SpawnerData data) {
        this.plugin = plugin;
        this.data = data;
        String title = ChatColor.translateAlternateColorCodes('&', "&8ꜰɪʟᴛᴇʀ");
        this.inventory = Bukkit.createInventory(new SpawnerGUIHolder(data, false, 888), 27, title);
        setupGUI();
    }

    private void setupGUI() {
        // Slots 4, 13, 22: Black stained glass pane (&8ᴄʟɪᴄᴋ ᴛᴏ ꜰɪʟᴛᴇʀ)
        ItemStack filterInfo = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta filterMeta = filterInfo.getItemMeta();
        filterMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bᴄʟɪᴄᴋ ᴛᴏ ꜰɪʟᴛᴇʀ"));
        List<String> filterLore = new ArrayList<>();
       // filterLore.add(ChatColor.translateAlternateColorCodes('&', ""));
        filterMeta.setLore(filterLore);
        filterInfo.setItemMeta(filterMeta);

        inventory.setItem(4, filterInfo);
        inventory.setItem(13, filterInfo);
        inventory.setItem(22, filterInfo);

        // Slot 18: Red stained glass pane
        ItemStack backItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4ʙᴀᴄᴋ"));
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to return to the spawner"));
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(18, backItem);

        // Populate Loot and Blacklisted slots
        List<Material> possibleDrops = data.getType().getPossibleDrops();
        Set<Material> blacklist = data.getBlacklistedLoot();

        int[] lootSlots = {0, 1, 2, 3, 9, 10, 11, 12, 19, 20, 21};
        int[] blacklistedSlots = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26};

        int lootIdx = 0;
        int blacklistIdx = 0;

        for (Material mat : possibleDrops) {
            if (blacklist.contains(mat)) {
                if (blacklistIdx < blacklistedSlots.length) {
                    inventory.setItem(blacklistedSlots[blacklistIdx++], createLootItem(mat, true));
                }
            } else {
                if (lootIdx < lootSlots.length) {
                    inventory.setItem(lootSlots[lootIdx++], createLootItem(mat, false));
                }
            }
        }
    }

    private ItemStack createLootItem(Material mat, boolean blacklisted) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String name = mat.name().replace("_", " ").toLowerCase();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (blacklisted ? "&f" : "&f") + name));
        List<String> lore = new ArrayList<>();
      //  lore.add(ChatColor.translateAlternateColorCodes('&', blacklisted ? "&7Status: &cBlacklisted" : "&7Status: &aActive"));
      //  lore.add("");
      //  lore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to " + (blacklisted ? "allow" : "filter")));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
