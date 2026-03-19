package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.gui.HomeGUI;
import com.h2ph.managers.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class HomeChatListener implements Listener {

    private final Falcon plugin;

    public HomeChatListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        HomeManager manager = plugin.getHomeManager();

        if (!manager.isRenaming(uuid)) {
            return;
        }

        event.setCancelled(true);
        String newName = event.getMessage().trim();
        int homeIndex = manager.getRenamingIndex(uuid);

        manager.stopRenaming(uuid);

        if (newName.isEmpty()) {
            player.sendMessage(HomeGUI.color("&cInvalid name. Renaming cancelled."));
            return;
        }

        manager.renameHome(uuid, homeIndex, newName);

        player.sendMessage(HomeGUI.color("&7Home renamed to: &d" + newName));

        plugin.getSchedulerAdapter().runTask(() -> HomeGUI.open(player, plugin));
    }
}
