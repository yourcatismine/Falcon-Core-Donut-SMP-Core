package com.prismcore.survival.orders.gui;

import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.store.OfflineNotificationManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class OrdersJoinListener implements Listener {

    private final OrdersModule module;

    public OrdersJoinListener(OrdersModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<OfflineNotificationManager.DeliveryRecord> records = module.getOfflineNotifications()
                .getAndClear(player.getUniqueId());

        if (records == null || records.isEmpty())
            return;

        if (records.size() == 1) {
            // Single delivery message
            OfflineNotificationManager.DeliveryRecord record = records.get(0);
            String message = Utils.formatColors("&5" + record.getDelivererName() + "&7 delivered you &a"
                    + Utils.abbr(record.getAmount()) + " &a" + record.getItemName() + "&7 while you were away");

            player.sendMessage(message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        } else {
            // Multiple deliveries message
            double totalMoney = 0;
            for (OfflineNotificationManager.DeliveryRecord record : records) {
                totalMoney += record.getMoney();
            }
            String message = Utils
                    .formatColors("&7You earned &a$" + Utils.abbr(totalMoney) + "&7 from order while you were away");

            player.sendMessage(message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }

        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
}
