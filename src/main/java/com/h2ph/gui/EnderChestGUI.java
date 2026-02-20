package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnderChestGUI {

    private final PrismSurvival plugin;

    public EnderChestGUI(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ItemStack[] contents = plugin.getEnderChestManager().loadEnderChest(player.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', "&8ᴇɴᴅᴇʀ ᴄʜᴇsᴛ");
        Inventory inv = Bukkit.createInventory(new EnderChestHolder(), 54, title);

        // Fill with loaded contents
        for (int i = 0; i < 54; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i]);
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    public static class EnderChestHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            // Required by interface, not used directly
            return null;
        }
    }
}
