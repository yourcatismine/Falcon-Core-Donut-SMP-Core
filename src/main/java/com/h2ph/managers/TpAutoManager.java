package com.h2ph.managers;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class TpAutoManager {

    public TpAutoManager(PrismSurvival plugin) {
        // Run every 2 seconds (40 ticks) to keep the action bar visible
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                if (data != null && data.isTpAuto()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            ChatColor.translateAlternateColorCodes('&', "&5You have tpauto on.")));
                }
            }
        }, 0L, 40L);
    }
}
