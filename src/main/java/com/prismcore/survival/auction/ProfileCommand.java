package com.prismcore.survival.auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ProfileCommand implements CommandExecutor, TabCompleter {
    private final AuctionController controller;

    public ProfileCommand(AuctionController controller) {
        this.controller = controller;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prism.admin.profile")) {
            sender.sendMessage(Utils.formatColors("&#ff4444You do not have permission to use this command."));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.formatColors("&#ff4444Only players can use this command!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.formatColors("&#ff4444Usage: /profile <playername>"));
            return true;
        }

        Player player = (Player) sender;
        String targetName = args[0];
        
        controller.getPlugin().getSchedulerAdapter().runTaskAsync(() -> {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            
            if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
                controller.getPlugin().getSchedulerAdapter().runEntityTask(player, () -> {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Utils.formatColors("&cThat player does not exist.")));
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } catch (Exception e) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1f);
                    }
                });
                return;
            }
            
            controller.getPlugin().getSchedulerAdapter().runEntityTask(player, () -> {
                openProfileGUI(player, targetPlayer);
            });
        });
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("prism.admin.profile")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return controller.getPlugin().getPlayerNameCache().getCompletions(args[0]);
        }

        return Collections.emptyList();
    }

    public static void openProfileGUI(Player viewer, OfflinePlayer targetPlayer) {
        String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";
        String title = Utils.formatColors("&8" + targetName + "'s ᴘʀᴏꜰɪʟᴇ");
        
        Inventory inv = Bukkit.createInventory((InventoryHolder) new ProfileHolder(targetPlayer), 36, title);
        
        ItemStack homesBed = new ItemStack(Material.PURPLE_BED);
        ItemMeta homesMeta = homesBed.getItemMeta();
        if (homesMeta != null) {
            homesMeta.setDisplayName(Utils.formatColors("&dʜᴏᴍᴇѕ"));
            homesMeta.setLore(List.of(Utils.formatColors("&fClick to view homes")));
            homesBed.setItemMeta(homesMeta);
        }
        inv.setItem(11, homesBed);
        
        ItemStack enderchest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta echestMeta = enderchest.getItemMeta();
        if (echestMeta != null) {
            echestMeta.setDisplayName(Utils.formatColors("&dᴇɴᴅᴇʀᴄʜᴇѕᴛ"));
            echestMeta.setLore(List.of(Utils.formatColors("&fClick to view enderchest")));
            enderchest.setItemMeta(echestMeta);
        }
        inv.setItem(12, enderchest);
        
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = chest.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setDisplayName(Utils.formatColors("&dɪɴᴠᴇɴᴛᴏʀʏ"));
            chestMeta.setLore(List.of(Utils.formatColors("&fClick to view inventory")));
            chest.setItemMeta(chestMeta);
        }
        inv.setItem(13, chest);
        
        viewer.openInventory(inv);
    }

    public static class ProfileHolder implements InventoryHolder {
        private final OfflinePlayer targetPlayer;

        public ProfileHolder(OfflinePlayer targetPlayer) {
            this.targetPlayer = targetPlayer;
        }

        public OfflinePlayer getTargetPlayer() {
            return targetPlayer;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
