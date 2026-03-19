package com.h2ph.gui;

import com.h2ph.Falcon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.UUID;

public class BountyConfirmGUI {

    private final Falcon plugin;

    public BountyConfirmGUI(Falcon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, UUID targetId, String targetName, double amount) {
        String title = color("&8ᴄᴏɴꜰɪʀᴍ ʙᴏᴜɴᴛʏ");
        Inventory inv = Bukkit.createInventory(new BountyConfirmHolder(targetId, targetName, amount), 27, title);

        inv.setItem(11, createItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ", "&fClick to cancel"));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetId));
            meta.setDisplayName(color("&d" + targetName));
            head.setItemMeta(meta);
        }
        inv.setItem(13, head);

        inv.setItem(15, createItem(Material.GREEN_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ", "&fClick to confirm this bounty"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (loreLine != null) {
                meta.setLore(Collections.singletonList(color(loreLine)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static class BountyConfirmHolder implements InventoryHolder {
        private final UUID targetId;
        private final String targetName;
        private final double amount;

        public BountyConfirmHolder(UUID targetId, String targetName, double amount) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.amount = amount;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public String getTargetName() {
            return targetName;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
