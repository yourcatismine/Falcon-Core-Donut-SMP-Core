package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.RTPDeathManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RTPDeathGUI implements Listener {

    private final PrismSurvival plugin;
    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&8ʀᴛᴘ ᴏɴ ᴅᴇᴀᴛʜ");

    public RTPDeathGUI(PrismSurvival plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        RTPDeathManager manager = plugin.getRTPDeathManager();
        List<ItemStack> items = manager.getItems();

        for (int i = 0; i < Math.min(items.size(), 27); i++) {
            inv.setItem(i, items.get(i));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE))
            return;

        Inventory inv = event.getInventory();
        List<ItemStack> newItems = new ArrayList<>();

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                newItems.add(item);
            }
        }

        plugin.getRTPDeathManager().setItems(newItems);
        event.getPlayer().sendMessage(ChatColor.GREEN + "RTP on Death items updated!");
    }
}
