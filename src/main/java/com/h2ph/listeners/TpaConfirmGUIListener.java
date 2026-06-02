package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.managers.TpaRequestManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class TpaConfirmGUIListener implements Listener {

    public static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ᴀᴄᴄᴇᴘᴛ ʀᴇǫᴜᴇѕᴛ?");

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(GUI_TITLE)) {
            if (e.getClickedInventory() == null)
                return;

            if (e.getClickedInventory().equals(e.getView().getTopInventory())) {
                e.setCancelled(true);

                if (!(e.getWhoClicked() instanceof Player))
                    return;
                Player p = (Player) e.getWhoClicked();
                ItemStack current = e.getCurrentItem();

                if (current == null || current.getType() == Material.AIR)
                    return;

                int slot = e.getSlot();

                if (slot == 3) {
                    p.closeInventory();
                    p.performCommand("tpaccept");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else if (slot == 5) {
                    p.closeInventory();
                    p.performCommand("tpadeny");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else {
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(GUI_TITLE)) {
            for (int slot : e.getRawSlots()) {
                if (slot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
