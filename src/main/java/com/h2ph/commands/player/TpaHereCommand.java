package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import com.h2ph.utils.SmallCapsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TpaHereCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {
            return false;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        // Cooldown check
        if (com.h2ph.managers.TpaRequestManager.getInstance().isOnCooldown(p.getUniqueId())) {
            String cooldownMsg = ChatColor.translateAlternateColorCodes('&', "&cPlease wait before requesting again.");
            p.sendMessage(cooldownMsg);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(cooldownMsg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (target == null || !p.canSee(target)) {
            String msg;
            if (target != null) {
                // Online but hidden -> "Not online" to the sender
                msg = ChatColor.translateAlternateColorCodes('&', "&cThis user is not online.");
            } else {
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget.hasPlayedBefore()) {
                    msg = ChatColor.translateAlternateColorCodes('&', "&cThis user is not online.");
                } else {
                    msg = ChatColor.translateAlternateColorCodes('&', "&cThat player does not exist.");
                }
            }

            p.sendMessage(msg);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Check if target is in combat
        if (com.h2ph.listeners.CombatListener.getInstance() != null &&
                com.h2ph.listeners.CombatListener.getInstance().isInCombat(target)) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cThis player is currently on combat.");
            p.sendMessage(msg);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Check if target has disabled TPA Here requests
        com.prismcore.survival.manager.PlayerData targetData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(target.getUniqueId());
        if (targetData != null && !targetData.isTpaHereRequests()) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cUser disabled tpahere requests.");
            p.sendMessage(msg);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Check Sender's "TPA Confirm Menus" setting
        com.prismcore.survival.manager.PlayerData senderData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(p.getUniqueId());
        if (senderData != null && !senderData.isTpaConfirmMenus()) {
            // Send immediately
            com.h2ph.managers.TpaRequestManager.getInstance().sendRequest(p, target,
                    com.h2ph.managers.TpaRequestManager.RequestType.TPA_HERE);
            return true;
        }

        openTpaHereGUI(p, target);
        return true;
    }

    private void openTpaHereGUI(Player p, Player target) {
        // Use same title as TPA Command logic as requested
        Inventory gui = Bukkit.createInventory(null, 27, TpaCommand.GUI_TITLE);

        // Slot 10: Cancel
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4ᴄᴀɴᴄᴇʟ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to cancel the teleport"));
            cancelMeta.setLore(lore);
            cancel.setItemMeta(cancelMeta);
        }
        gui.setItem(10, cancel);

        // Slot 12: Location (World)
        Material worldMat = Material.GRASS_BLOCK;
        String locationName = "Overworld";
        switch (target.getWorld().getEnvironment()) {
            case NETHER:
                worldMat = Material.NETHERRACK;
                locationName = "Nether";
                break;
            case THE_END:
                worldMat = Material.END_STONE;
                locationName = "End";
                break;
            default:
                break;
        }
        ItemStack locItem = new ItemStack(worldMat);
        ItemMeta locMeta = locItem.getItemMeta();
        if (locMeta != null) {
            locMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aʟᴏᴄᴀᴛɪᴏɴ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + locationName));
            locMeta.setLore(lore);
            locItem.setItemMeta(locMeta);
        }
        gui.setItem(12, locItem);

        // Slot 13: Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            headMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aᴘʟᴀʏᴇʀ"));
            List<String> lore = new ArrayList<>();
            String smallCapsName = SmallCapsUtil.toSmallCaps(target.getName());
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + smallCapsName));
            headMeta.setLore(lore);
            head.setItemMeta(headMeta);
        }
        gui.setItem(13, head);

        // Slot 14: Region
        ItemStack regionItem = new ItemStack(Material.FEATHER);
        ItemMeta regionMeta = regionItem.getItemMeta();
        if (regionMeta != null) {
            regionMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aʀᴇɢɪᴏɴ"));
            List<String> lore = new ArrayList<>();

            String region = "Unknown";
            try {
                if (PrismSurvival.getInstance().getSurvivalConfig().contains("region")) {
                    List<String> regions = PrismSurvival.getInstance().getSurvivalConfig().getStringList("region");
                    if (!regions.isEmpty()) {
                        region = regions.get(0);
                    }
                }
            } catch (Exception e) {
            }

            lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + region));
            regionMeta.setLore(lore);
            regionItem.setItemMeta(regionMeta);
        }
        gui.setItem(14, regionItem);

        // Slot 16: Confirm request (TPA HERE specific lore)
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aᴄᴏɴꜰɪʀᴍ"));
            List<String> lore = new ArrayList<>();
            String smallCapsName = SmallCapsUtil.toSmallCaps(target.getName());
            // "&fClick to teleport %PLAYER_NAME% to you"
            lore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to teleport " + smallCapsName + " to you"));
            confirmMeta.setLore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        gui.setItem(16, confirm);

        p.openInventory(gui);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && ((Player) sender).canSee(player)) {
                    playerNames.add(player.getName());
                }
            }
            return org.bukkit.util.StringUtil.copyPartialMatches(args[0], playerNames, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
