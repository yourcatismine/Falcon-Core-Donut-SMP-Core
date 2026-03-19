package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;


public class HomeGUI {

    public static final int HOME_COUNT = 10;

    public static final int BED_START = 3;
    public static final int DYE_START = 12;

    public static final int BED_START_2 = 21;
    public static final int DYE_START_2 = 30;

    public static class HomeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player player, PrismSurvival plugin) {
        HomeManager manager = plugin.getHomeManager();

        String title = color("&8\u029c\u1d0f\u1d0d\u1d07\u0455");
        Inventory inv = Bukkit.createInventory(new HomeHolder(), 36, title);

        com.h2ph.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean isOwner = data != null && "OWNER".equalsIgnoreCase(data.getTeamRole());

        if (team == null) {
            inv.setItem(10, make(Material.GRAY_BANNER,
                    color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                    List.of(color("&cYou are not in a team."))));
        } else if (!team.hasHome()) {
            inv.setItem(10, make(Material.GRAY_BANNER,
                    color("&dYour team does not have a home"),
                    List.of(color("&fNo team home"))));

            if (isOwner) {
                inv.setItem(19, make(Material.GRAY_DYE,
                        color("&dYour team does not have a home"),
                        List.of(color("&fNo team home"))));
            }
        } else {
            inv.setItem(10, make(Material.ORANGE_BANNER,
                    color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                    List.of(color("&fCick to teleport to team home"))));

            if (isOwner) {
                inv.setItem(19, make(Material.ORANGE_DYE,
                        color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                        List.of(color("&fClick to delete " + ChatColor.stripColor(team.getName()) + " home"))));
            }
        }

        for (int i = 0; i < HOME_COUNT; i++) {
            int homeNumber = i + 1;

            int bedSlot, dyeSlot;
            if (i < 5) {
                bedSlot = BED_START + i;
                dyeSlot = DYE_START + i;
            } else {
                bedSlot = BED_START_2 + (i - 5);
                dyeSlot = DYE_START_2 + (i - 5);
            }

            if (homeNumber >= 3 && !player.hasPermission("falcon.home." + homeNumber) && !player.hasPermission("falcon.home.all")) {
                List<String> lockedLore = List.of(
                        color("&fBuy &dѕᴘʜʏɴх&f in /store for more homes"));
                inv.setItem(bedSlot, make(Material.RED_BED,
                        color("&4\u0274\u1d0f \u1d18\u1d07\u0280\u1d0d\u026a\u0455\u0455\u26a4\u1d0f\u0274"),
                        lockedLore));
                inv.setItem(dyeSlot, make(Material.RED_DYE,
                        color("&4\u0274\u1d0f \u1d18\u1d07\u0280\u1d0d\u026a\u0455\u0455\u26a4\u1d0f\u0274"),
                        lockedLore));
                continue;
            }

            if (manager.hasHome(player.getUniqueId(), homeNumber)) {
                String homeName = manager.getHomeName(player.getUniqueId(), homeNumber);
                String baseName = color("&d\u029c\u1d0f\u1d0d\u1d07 " + homeNumber);
                String displayName = (homeName != null && !homeName.isEmpty()) ? baseName + " " + homeName : baseName;

                ItemStack bed = make(Material.ORANGE_BED,
                        displayName,
                        List.of(color("&fClick to teleport to your home")));
                inv.setItem(bedSlot, bed);

                ItemStack dye = make(Material.ORANGE_DYE,
                        displayName,
                        List.of(color("&fClick to delete " + ChatColor.stripColor(displayName))));
                inv.setItem(dyeSlot, dye);
            } else {
                ItemStack bed = make(Material.GRAY_BED,
                        color("&7\u0274\u1d0f \u029c\u1d0f\u1d0d\u1d07 \u0455\u1d07\u1d1b"),
                        List.of(color("&f- Click to save your location")));
                inv.setItem(bedSlot, bed);

                ItemStack dye = make(Material.GRAY_DYE,
                        color("&7\u0274\u1d0f \u029c\u1d0f\u1d0d\u1d07 \u0455\u1d07\u1d1b"),
                        List.of(color("&f- Click to save your location")));
                inv.setItem(dyeSlot, dye);
            }
        }

        player.openInventory(inv);
    }

    public static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
