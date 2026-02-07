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

public class TpaCommand implements CommandExecutor, TabCompleter {

    public static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ ʀᴇǫᴜᴇѕᴛ");

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
            String msg = ChatColor.translateAlternateColorCodes('&',
                    "&cThat player does not exist.\n&cThis user is not online.");
            p.sendMessage(msg);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg.replace("\n", " "))); // Actionbar
                                                                                                           // single
                                                                                                           // line? Or
                                                                                                           // just first
                                                                                                           // line?
            // "Both chat and actionbar" implies showing the error on both.
            // Let's show "That player does not exist" on action bar maybe? Or "This user is
            // not online".
            // The request says: "&cThat player does not exist. &cThis user is not online. -
            // Both chat and actionbar"
            // I'll send both lines to chat, and maybe the second or combined to actionbar.
            // Actually Actionbar usually supports one line. I'll join them.
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        openTpaGUI(p, target);
        return true;
    }

    private void openTpaGUI(Player p, Player target) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

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
            // Apply small caps to player name
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
            // Check config
            // Assuming PrismSurvival is main class and static instance available or passed
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

        // Slot 16: Confirm request
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aᴄᴏɴꜰɪʀᴍ"));
            List<String> lore = new ArrayList<>();
            // Small caps for player name in lore logic if needed, user said "%PLAYER_NAME%
            // font must be - ᴀʙᴄᴅᴇ" generally
            String smallCapsName = SmallCapsUtil.toSmallCaps(target.getName());
            lore.add(
                    ChatColor.translateAlternateColorCodes('&', "&fClick to send " + smallCapsName + " a tpa request"));
            confirmMeta.setLore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        gui.setItem(16, confirm);

        // Play open sound? "Make sure there has a tripwire sounds on the GUI" -> likely
        // on click.
        // But usually opening has a sound too. I'll stick to clicks as requested.

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
            return playerNames;
        }
        return Collections.emptyList();
    }
}
