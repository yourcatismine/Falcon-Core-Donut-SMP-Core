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

// Permission required for home slots 3, 4, 5
// prismcore.home.3 / .4 / .5 — slots without permission show a locked RED_BED / RED_DYE

public class HomeGUI {

    // ── How many personal home slots ──
    public static final int HOME_COUNT = 5;

    // Top row: beds start at slot 12
    public static final int BED_START = 12;
    // Bottom row: dyes start at slot 21
    public static final int DYE_START = 21;

    // ─────────────────────────────────────────────
    // Inventory holder (used to identify this GUI)
    // ─────────────────────────────────────────────
    public static class HomeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // Open the GUI for a player
    // ─────────────────────────────────────────────
    public static void open(Player player, PrismSurvival plugin) {
        HomeManager manager = plugin.getHomeManager();

        // Title: &8ʜᴏᴍᴇѕ (size 36 = 4 rows, slots 0-35)
        String title = color("&8\u029c\u1d0f\u1d0d\u1d07\u0455");
        Inventory inv = Bukkit.createInventory(new HomeHolder(), 36, title);

        // ── Team Home Logic ──────────────────────────────────────────────────
        com.h2ph.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean isOwner = data != null && "OWNER".equalsIgnoreCase(data.getTeamRole());

        if (team == null) {
            // Slot 10 : Gray Banner – No Team
            inv.setItem(10, make(Material.GRAY_BANNER,
                    color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                    List.of(color("&cYou are not in a team."))));
        } else if (!team.hasHome()) {
            // Slot 10 : Gray Banner – Team Home (Not set)
            inv.setItem(10, make(Material.GRAY_BANNER,
                    color("&dYour team does not have a home"),
                    List.of(color("&fNo team home"))));

            // Slot 19 : Gray Dye – Team Home (Second row, Not set, Owner only)
            if (isOwner) {
                inv.setItem(19, make(Material.GRAY_DYE,
                        color("&dYour team does not have a home"),
                        List.of(color("&fNo team home"))));
            }
        } else {
            // Slot 10 : Purple Banner – Team Home (Set)
            inv.setItem(10, make(Material.PURPLE_BANNER,
                    color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                    List.of(color("&fCick to teleport to team home"))));

            // Slot 19 : Purple Dye – Delete Team Home (Owner only)
            if (isOwner) {
                inv.setItem(19, make(Material.PURPLE_DYE,
                        color("&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07"),
                        List.of(color("&fClick to delete " + ChatColor.stripColor(team.getName()) + " home"))));
            }
        }

        // ── Slots 12-16 (beds) and 21-25 (dyes)
        // ─────────────────────────────────────────────────────────────────────
        for (int i = 0; i < HOME_COUNT; i++) {
            int homeNumber = i + 1;
            int bedSlot = BED_START + i;
            int dyeSlot = DYE_START + i;

            // Slots 3-5 require permission prismcore.home.N
            if (homeNumber >= 3 && !player.hasPermission("prismcore.home." + homeNumber)) {
                // ── Locked slot ──────────────────────────────────────────────────
                List<String> lockedLore = List.of(
                        color("&fBuy&d \u1d18\u0280\u026a\u0455\u1d0d+&f in /store for more homes"));
                inv.setItem(bedSlot, make(Material.RED_BED,
                        color("&4\u0274\u1d0f \u1d18\u1d07\u0280\u1d0d\u026a\u0455\u0455\u26a4\u1d0f\u0274"),
                        lockedLore));
                inv.setItem(dyeSlot, make(Material.RED_DYE,
                        color("&4\u0274\u1d0f \u1d18\u1d07\u0280\u1d0d\u026a\u0455\u0455\u26a4\u1d0f\u0274"),
                        lockedLore));
                continue;
            }

            if (manager.hasHome(player.getUniqueId(), homeNumber)) {
                // ── Home IS set ──────────────────────────────────────────────────
                String homeName = manager.getHomeName(player.getUniqueId(), homeNumber);
                String baseName = color("&d\u029c\u1d0f\u1d0d\u1d07 " + homeNumber);
                String displayName = (homeName != null && !homeName.isEmpty()) ? baseName + " " + homeName : baseName;

                // Purple bed: click to teleport
                ItemStack bed = make(Material.PURPLE_BED,
                        displayName,
                        List.of(color("&fClick to teleport to your home")));
                inv.setItem(bedSlot, bed);

                // Purple dye: click to delete
                ItemStack dye = make(Material.PURPLE_DYE,
                        displayName,
                        List.of(color("&fClick to delete " + ChatColor.stripColor(displayName))));
                inv.setItem(dyeSlot, dye);
            } else {
                // ── No home set ─────────────────────────────────────────────────
                // Gray bed (top) – decorative only
                ItemStack bed = make(Material.GRAY_BED,
                        color("&7\u0274\u1d0f \u029c\u1d0f\u1d0d\u1d07 \u0455\u1d07\u1d1b"),
                        List.of(color("&f- Click to save your location")));
                inv.setItem(bedSlot, bed);

                // Gray dye (bottom) – click to set
                ItemStack dye = make(Material.GRAY_DYE,
                        color("&7\u0274\u1d0f \u029c\u1d0f\u1d0d\u1d07 \u0455\u1d07\u1d1b"),
                        List.of(color("&f- Click to save your location")));
                inv.setItem(dyeSlot, dye);
            }
        }

        player.openInventory(inv);
    }

    // ── Helpers ──
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
