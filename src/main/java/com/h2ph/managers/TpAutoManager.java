package com.h2ph.managers;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class TpAutoManager implements Listener {

    public TpAutoManager(Falcon plugin) {
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                if (data != null && data.isTpAuto()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            ChatColor.translateAlternateColorCodes('&', "&dYou have tpauto on.")));
                }
            }
        }, 0L, 40L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        org.bukkit.entity.Player p = event.getEntity();
        PlayerData data = Falcon.getInstance().getPlayerDataManager().get(p.getUniqueId());

        if (data != null && data.isTpAuto()) {
            data.setTpAuto(false);
            p.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', "&cYour tpauto has been disabled because you died."));
        }
    }
}
