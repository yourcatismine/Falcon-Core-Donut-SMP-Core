package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.BountyConfirmGUI;
import com.prismcore.survival.manager.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.text.DecimalFormat;
import java.util.UUID;

public class BountyConfirmGUIListener implements Listener {

    private final PrismSurvival plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public BountyConfirmGUIListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof BountyConfirmGUI.BountyConfirmHolder) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player))
                return;

            Player player = (Player) e.getWhoClicked();
            BountyConfirmGUI.BountyConfirmHolder holder = (BountyConfirmGUI.BountyConfirmHolder) e.getInventory()
                    .getHolder();
            int slot = e.getRawSlot();

            if (slot == 11) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Bounty placement cancelled.");
            } else if (slot == 15) {
                player.closeInventory();
                executeBountyAdd(player, holder.getTargetId(), holder.getTargetName(), holder.getAmount());
            }
        }
    }

    private void executeBountyAdd(Player sender, UUID targetId, String targetName, double amount) {
        if (!com.prismcore.survival.auction.EconomyHandler.chargePlayer(sender, amount, "Bounty on " + targetName)) {
            sender.sendMessage(ChatColor.RED + "You do not have enough money.");
            return;
        }

        plugin.getBountyManager().addBounty(targetId, amount);

        String amountFormatted = formatNumber(amount);
        String senderMsg = color("&7You added &a$" + amountFormatted + " &7bounty to &4" + targetName + "&7.");

        sender.sendMessage(senderMsg);
        sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(senderMsg));

        Player target = Bukkit.getPlayer(targetId);
        if (target != null && target.isOnline()) {
            String targetMsg = color("&d" + sender.getName() + "&7 added &a$" + amountFormatted + " &7to your bounty.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(targetMsg));
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "t");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "b");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "m");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "k");
        } else {
            return DF.format(Math.floor(number * 10) / 10.0);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BountyConfirmGUI.BountyConfirmHolder) {
            e.setCancelled(true);
        }
    }
}
