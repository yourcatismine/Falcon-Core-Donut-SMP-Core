package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.gui.WorthGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class WorthGUIListener implements Listener {

    private final Falcon plugin;

    public WorthGUIListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthGUI.WorthHolder))
            return;

        if (event.getRawSlot() < event.getInventory().getSize()) {
            event.setCancelled(true);
        } else {
            return;
        }

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        WorthGUI.WorthHolder holder = (WorthGUI.WorthHolder) event.getInventory().getHolder();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        WorthGUI gui = new WorthGUI(plugin, p);

        if (slot == 45) {
            if (event.getInventory().getItem(45) != null) {
                gui.open(holder.getPage() - 1, holder.getSortType(), holder.getFilterType());
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            }
        } else if (slot == 53) {
            if (event.getInventory().getItem(53) != null) {
                gui.open(holder.getPage() + 1, holder.getSortType(), holder.getFilterType());
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            }
        } else if (slot == 48) {
            WorthGUI.SortType[] sorts = WorthGUI.SortType.values();
            WorthGUI.SortType nextSort = sorts[(holder.getSortType().ordinal() + 1) % sorts.length];
            gui.open(0, nextSort, holder.getFilterType());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
        } else if (slot == 49) {
            gui.open(holder.getPage(), holder.getSortType(), holder.getFilterType());
            p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
        } else if (slot == 50) {
            WorthGUI.FilterType[] filters = WorthGUI.FilterType.values();
            WorthGUI.FilterType nextFilter = filters[(holder.getFilterType().ordinal() + 1) % filters.length];
            gui.open(0, holder.getSortType(), nextFilter);
            p.playSound(p.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
        } else if (slot < 45) {
            p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
        }
    }
}
