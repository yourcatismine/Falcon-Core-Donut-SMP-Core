package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.commands.player.TpaCommand;
import com.h2ph.utils.SmallCapsUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TpaGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(TpaCommand.GUI_TITLE)) {
            // Cancel events in top inventory (GUI)
            if (e.getClickedInventory() == null)
                return;

            if (e.getClickedInventory() == e.getView().getTopInventory()) {
                e.setCancelled(true);
            } else {
                // Allow bottom inventory events (dragging items in player inventory)
                // BUT prevent shift-clicking into the GUI
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                }
                return;
            }

            if (!(e.getWhoClicked() instanceof Player))
                return;
            Player p = (Player) e.getWhoClicked();
            ItemStack current = e.getCurrentItem();

            if (current == null || current.getType() == Material.AIR) {
                // No sound for empty slots
                return;
            }

            // Play Tripwire sound on click
            p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);

            int slot = e.getSlot();

            if (slot == 10) { // Cancel
                p.closeInventory();
                p.sendMessage(ChatColor.RED + "Teleport request cancelled.");
            } else if (slot == 16) { // Confirm
                ItemStack head = e.getView().getTopInventory().getItem(13);
                if (head != null && head.hasItemMeta()) {
                    org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                    if (sm != null && sm.getOwningPlayer() != null) {
                        org.bukkit.OfflinePlayer target = sm.getOwningPlayer();
                        if (target.isOnline()) {
                            Player targetPlayer = target.getPlayer();
                            // Check if it is TPA or TPA HERE based on current item lore
                            boolean isTpaHere = false;
                            if (current != null && current.hasItemMeta() && current.getItemMeta().hasLore()) {
                                for (String line : current.getItemMeta().getLore()) {
                                    if (line.contains("to teleport") && line.contains("to you")) {
                                        isTpaHere = true;
                                        break;
                                    }
                                }
                            }

                            p.closeInventory();

                            // Store Request
                            com.h2ph.managers.TpaRequestManager.RequestType type = isTpaHere
                                    ? com.h2ph.managers.TpaRequestManager.RequestType.TPA_HERE
                                    : com.h2ph.managers.TpaRequestManager.RequestType.TPA;

                            com.h2ph.managers.TpaRequestManager.getInstance().sendRequest(p, targetPlayer, type);

                            // TODO: Integrate with TpaManager logic to actually store the request
                        } else {
                            p.closeInventory();
                            p.sendMessage(ChatColor.RED + "That player is no longer online.");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getView().getTitle().equals(TpaCommand.GUI_TITLE)) {
            for (int slot : e.getRawSlots()) {
                if (slot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}