package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import org.bukkit.inventory.ItemStack;

public class AdvisorListener implements Listener {

    private final PrismSurvival plugin;

    public AdvisorListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isPlayerMarkedAsAdvisorWriter(player.getUniqueId())) {
            return;
        }

        try {
            BookMeta newMeta = event.getNewBookMeta();
            if (newMeta != null) {

                List<String> pages = newMeta.getPages();
                if (pages != null && !pages.isEmpty()) {
                    plugin.setActiveAdvisor(pages);
                    player.sendMessage(ChatColor.GREEN + "Advisor content updated successfully!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An error occurred while saving the advisor book.");
            e.printStackTrace();
        }

        plugin.unmarkPlayerAsAdvisorWriter(player.getUniqueId());

        plugin.getSchedulerAdapter().runTaskLater(() -> {
            if (!player.isOnline())
                return;

            ItemStack main = player.getInventory().getItemInMainHand();
            if (main != null && (main.getType() == org.bukkit.Material.WRITABLE_BOOK
                    || main.getType() == org.bukkit.Material.WRITTEN_BOOK)) {
                player.getInventory().setItemInMainHand(null);
            }

            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && (off.getType() == org.bukkit.Material.WRITABLE_BOOK
                    || off.getType() == org.bukkit.Material.WRITTEN_BOOK)) {
                player.getInventory().setItemInOffHand(null);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.isPlayerMarkedAsAdvisorWriter(event.getPlayer().getUniqueId())) {
            plugin.unmarkPlayerAsAdvisorWriter(event.getPlayer().getUniqueId());
        }
    }
}
