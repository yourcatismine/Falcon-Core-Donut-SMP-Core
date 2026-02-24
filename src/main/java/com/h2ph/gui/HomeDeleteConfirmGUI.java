package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.h2ph.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class HomeDeleteConfirmGUI {

    /**
     * Data bundle to keep track of WHICH home is being confirmed for deletion
     * while the GUI is open.
     */
    public record ConfirmData(int homeIndex, String displayName) {
    }

    public static class HomeDeleteConfirmHolder implements InventoryHolder {
        private final ConfirmData data;

        public HomeDeleteConfirmHolder(ConfirmData data) {
            this.data = data;
        }

        public ConfirmData getData() {
            return data;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player player, PrismSurvival plugin, int homeIndex) {
        HomeManager manager = plugin.getHomeManager();

        // ── Get home name for display ──
        String displayName;
        if (homeIndex == 0) {
            com.h2ph.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            displayName = HomeGUI.color(
                    "&d\u1d1b\u1d07\u1d00\u1d0d \u029c\u1d0f\u1d0d\u1d07" + (team != null ? " " + team.getName() : ""));
        } else {
            String homeName = manager.getHomeName(player.getUniqueId(), homeIndex);
            String baseName = HomeGUI.color("&d\u029c\u1d0f\u1d0d\u1d07 " + homeIndex);
            displayName = (homeName != null && !homeName.isEmpty()) ? baseName + " " + homeName : baseName;
        }

        ConfirmData data = new ConfirmData(homeIndex, displayName);

        // Title: &8ᴄᴏɴꜰɪʀᴍ ᴅᴇʟᴇᴛᴇ (size 27 = 3 rows)
        String title = HomeGUI
                .color("&8\u1d04\u1d0f\u0274\u043c\u026a\u0280\u1d0d \u1d05\u1d07\u1d0c\u1d07\u1d1b\u1d07");
        Inventory inv = Bukkit.createInventory(new HomeDeleteConfirmHolder(data), 27, title);

        // Slot 11: Red Glass (Cancel)
        inv.setItem(11, HomeGUI.make(Material.RED_STAINED_GLASS_PANE,
                HomeGUI.color("&4\u1d04\u1d00\u0274\u1d04\u1d07\u1d0c"),
                List.of(HomeGUI.color("&fClick to cancel"))));

        // Slot 13: Purple Bed (Info) - Purple Banner if Team Home
        inv.setItem(13,
                HomeGUI.make(homeIndex == 0 ? Material.PURPLE_BANNER : Material.PURPLE_BED, displayName, List.of()));

        // Slot 15: Green Glass (Confirm)
        inv.setItem(15, HomeGUI.make(Material.GREEN_STAINED_GLASS_PANE,
                HomeGUI.color("&a\u1d04\u1d0f\u0274\u043c\u026a\u0280\u1d0d"),
                List.of(HomeGUI.color("&fClick to delete"))));

        player.openInventory(inv);
    }
}
