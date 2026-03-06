package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InvSeeGUI {

    private final PrismSurvival plugin;

    public InvSeeGUI(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Player target) {
        String title = ChatColor.translateAlternateColorCodes('&', "&8" + target.getName() + "'s ɪɴᴠᴇɴᴛᴏʀʏ");
        Inventory inv = Bukkit.createInventory(new InvSeeHolder(target.getUniqueId(), target.getName()), 54, title);

        updateInventory(inv, target);

        viewer.openInventory(inv);

        viewer.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (!viewer.isOnline() || !viewer.getOpenInventory().getTitle().contains("ɪɴᴠᴇɴᴛᴏʀʏ")) {
                task.cancel();
                return;
            }

            Inventory topInv = viewer.getOpenInventory().getTopInventory();
            if (topInv.getHolder() instanceof InvSeeHolder holder) {
                if (holder.getTargetUUID().equals(target.getUniqueId())) {
                    if (target.isOnline()) {
                        updateInventory(topInv, target);
                    } else {
                        viewer.closeInventory();
                        task.cancel();
                    }
                }
            } else {
                task.cancel();
            }
        }, null, 1, 2);
    }

    public void updateInventory(Inventory inv, Player target) {
        ItemStack[] contents = target.getInventory().getContents();
        ItemStack[] armor = target.getInventory().getArmorContents();
        ItemStack offhand = target.getInventory().getItemInOffHand();

        inv.setItem(0, armor[3]);
        inv.setItem(1, armor[2]);
        inv.setItem(2, armor[1]);
        inv.setItem(3, armor[0]);

        inv.setItem(8, offhand);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, glass);
        }

        for (int i = 0; i < 36; i++) {
            inv.setItem(18 + i, contents[i]);
        }
    }

    public static class InvSeeHolder implements InventoryHolder {
        private final UUID targetUUID;
        private final String targetName;

        public InvSeeHolder(UUID targetUUID, String targetName) {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
        }

        public UUID getTargetUUID() {
            return targetUUID;
        }

        public String getTargetName() {
            return targetName;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
