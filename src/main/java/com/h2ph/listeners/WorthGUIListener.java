package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.WorthGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class WorthGUIListener implements Listener {

    private final PrismSurvival plugin;

    public WorthGUIListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthGUI.WorthHolder))
            return;

        // Allow interactions in player inventory, block in GUI
        if (event.getRawSlot() < event.getInventory().getSize()) {
            event.setCancelled(true);
        } else {
            return; // Let player move items in their own inventory
        }

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        WorthGUI.WorthHolder holder = (WorthGUI.WorthHolder) event.getInventory().getHolder();

        // Items logic
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        WorthGUI gui = new WorthGUI(plugin, p);

        if (slot == 45) { // Back
            if (event.getInventory().getItem(45) != null) {
                gui.open(holder.getPage() - 1, holder.getSortType(), holder.getFilterType());
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            }
        } else if (slot == 53) { // Next
            if (event.getInventory().getItem(53) != null) {
                gui.open(holder.getPage() + 1, holder.getSortType(), holder.getFilterType());
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            }
        } else if (slot == 48) { // Sort
            WorthGUI.SortType[] sorts = WorthGUI.SortType.values();
            WorthGUI.SortType nextSort = sorts[(holder.getSortType().ordinal() + 1) % sorts.length];
            gui.open(0, nextSort, holder.getFilterType());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
        } else if (slot == 49) { // Refresh
            gui.open(holder.getPage(), holder.getSortType(), holder.getFilterType());
            p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
        } else if (slot == 50) { // Filter
            WorthGUI.FilterType[] filters = WorthGUI.FilterType.values();
            WorthGUI.FilterType nextFilter = filters[(holder.getFilterType().ordinal() + 1) % filters.length];
            gui.open(0, holder.getSortType(), nextFilter);
            p.playSound(p.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
        } else if (slot < 45) { // Item slots
            p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
        }
    }
}
