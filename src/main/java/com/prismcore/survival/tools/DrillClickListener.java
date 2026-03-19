package com.prismcore.survival.tools;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.h2ph.PrismSurvival;

/**
 * Triggers tool lore updates on inventory clicks.
 * 
 * IMPORTANT: The update is delayed by 1 tick because InventoryClickEvent fires
 * BEFORE Bukkit processes the click. If we update immediately,
 * getItemOnCursor()
 * returns the OLD cursor state, bypassing the dupe guard in ContainerScanner.
 * By delaying 1 tick, Bukkit has already moved items, so the cursor check works
 * correctly.
 */
public class DrillClickListener implements Listener {

    private final ToolsManager manager;
    private final PrismSurvival plugin;

    public DrillClickListener(ToolsManager manager, PrismSurvival plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        HumanEntity humanEntity = evt.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;

        plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
            manager.updatePlayerTools(player);
        }, 1L);
    }
}
