package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.gui.BountyGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class BountyGUIListener implements Listener {

    private final Falcon plugin;

    public BountyGUIListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof BountyGUI.BountyHolder) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player))
                return;

            Player player = (Player) e.getWhoClicked();
            BountyGUI.BountyHolder holder = (BountyGUI.BountyHolder) e.getInventory().getHolder();
            int slot = e.getRawSlot();

            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR)
                return;

            BountyGUI gui = new BountyGUI(plugin);

            if (slot == 45) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                gui.open(player, holder.getPage() - 1, holder.getSortType(), holder.getSearchQuery());
            }
            else if (slot == 53) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                gui.open(player, holder.getPage() + 1, holder.getSortType(), holder.getSearchQuery());
            }
            else if (slot == 49) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
                gui.open(player, 0, holder.getSortType(), holder.getSearchQuery());
            }
            else if (slot == 48) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                BountyGUI.SortType nextSort = holder.getSortType() == BountyGUI.SortType.AMOUNT
                        ? BountyGUI.SortType.RECENTLY_SET
                        : BountyGUI.SortType.AMOUNT;
                gui.open(player, 0, nextSort, holder.getSearchQuery());
            }
            else if (slot == 50) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                player.closeInventory();
                plugin.getSignInput().getSearchInput(player, input -> {
                    gui.open(player, 0, holder.getSortType(), input);
                });
            }
            else {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BountyGUI.BountyHolder) {
            e.setCancelled(true);
        }
    }
}
