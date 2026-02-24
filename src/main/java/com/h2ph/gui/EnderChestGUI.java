package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestGUI {

    private final PrismSurvival plugin;
    // Map to track active enderchest inventories by owner UUID for live-syncing
    private static final Map<UUID, Inventory> activeInventories = new HashMap<>();

    public EnderChestGUI(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public static Map<UUID, Inventory> getActiveInventories() {
        return activeInventories;
    }

    /** Open own echest via /echest command — no block animation. */
    public void open(Player player) {
        open(player, player.getUniqueId(), player.getName(), null);
    }

    /**
     * Open own echest via block interaction — triggers lid open/close animation on
     * the block.
     */
    public void open(Player player, @Nullable Block block) {
        open(player, player.getUniqueId(), player.getName(), block);
    }

    /**
     * Open an enderchest for a specific player (viewer may be different from
     * owner).
     */
    public void open(Player viewer, UUID ownerUUID, String ownerName, @Nullable Block block) {
        Inventory inv = activeInventories.get(ownerUUID);

        if (inv == null) {
            ItemStack[] contents = plugin.getEnderChestManager().loadEnderChest(ownerUUID);
            String title = ChatColor.translateAlternateColorCodes('&', "&8" + ownerName + "'s ᴇɴᴅᴇʀ ᴄʜᴇsᴛ");
            if (viewer.getUniqueId().equals(ownerUUID)) {
                title = ChatColor.translateAlternateColorCodes('&', "&8ᴇɴᴅᴇʀ ᴄʜᴇsᴛ");
            }

            inv = Bukkit.createInventory(new EnderChestHolder(ownerUUID, ownerName, block), 54, title);
            for (int i = 0; i < 54; i++) {
                if (contents[i] != null) {
                    inv.setItem(i, contents[i]);
                }
            }
            activeInventories.put(ownerUUID, inv);
        } else {
            // Update the block reference if we're opening it from a different block
            if (inv.getHolder() instanceof EnderChestHolder) {
                ((EnderChestHolder) inv.getHolder()).setSourceBlock(block);
            }
        }

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);

        // Trigger lid open animation if opened from a block
        if (block != null) {
            plugin.getEnderChestManager().registerViewer(block, viewer);
        }
    }

    public static class EnderChestHolder implements InventoryHolder {
        private final UUID ownerUUID;
        private final String ownerName;
        @Nullable
        private Block sourceBlock;

        public EnderChestHolder(UUID ownerUUID, String ownerName, @Nullable Block sourceBlock) {
            this.ownerUUID = ownerUUID;
            this.ownerName = ownerName;
            this.sourceBlock = sourceBlock;
        }

        public UUID getOwnerUUID() {
            return ownerUUID;
        }

        public String getOwnerName() {
            return ownerName;
        }

        @Nullable
        public Block getSourceBlock() {
            return sourceBlock;
        }

        public void setSourceBlock(@Nullable Block sourceBlock) {
            this.sourceBlock = sourceBlock;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
