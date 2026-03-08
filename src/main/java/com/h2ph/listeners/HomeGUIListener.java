package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.HomeGUI;
import com.h2ph.managers.HomeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HomeGUIListener implements Listener {

    private final PrismSurvival plugin;

    public HomeGUIListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Inventory topInv = event.getView().getTopInventory();
        boolean isHomeMain = topInv.getHolder() instanceof HomeGUI.HomeHolder;
        boolean isHomeConfirm = topInv.getHolder() instanceof com.h2ph.gui.HomeDeleteConfirmGUI.HomeDeleteConfirmHolder;

        if (!isHomeMain && !isHomeConfirm)
            return;

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null)
            return;

        if (clickedInv.equals(topInv)) {
            event.setCancelled(true);

            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() != Material.AIR) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            }

            if (isHomeMain) {
                handleHomeMainGrid(event, player);
            } else {
                handleDeleteConfirmation(event, player,
                        (com.h2ph.gui.HomeDeleteConfirmGUI.HomeDeleteConfirmHolder) topInv.getHolder());
            }
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        boolean isHomeMain = topInv.getHolder() instanceof HomeGUI.HomeHolder;
        boolean isHomeConfirm = topInv.getHolder() instanceof com.h2ph.gui.HomeDeleteConfirmGUI.HomeDeleteConfirmHolder;

        if (!isHomeMain && !isHomeConfirm)
            return;

        int topSize = topInv.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleHomeMainGrid(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        HomeManager manager = plugin.getHomeManager();

        if ((slot >= HomeGUI.BED_START && slot < HomeGUI.BED_START + 5) ||
            (slot >= HomeGUI.BED_START_2 && slot < HomeGUI.BED_START_2 + 5)) {

            int homeNumber;
            if (slot >= HomeGUI.BED_START && slot < HomeGUI.BED_START + 5) {
                homeNumber = slot - HomeGUI.BED_START + 1;
            } else {
                homeNumber = slot - HomeGUI.BED_START_2 + 6;
            }

            Material clickedMat = event.getCurrentItem() != null ? event.getCurrentItem().getType() : Material.AIR;

            if (clickedMat == Material.PURPLE_BED) {
                if (event.getClick().isRightClick()) {
                    manager.startRenaming(player.getUniqueId(), homeNumber);
                    player.closeInventory();

                    String msg = HomeGUI.color("&7Please type the new home name on the chat.");
                    player.sendMessage(msg);
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                    return;
                }

                Location dest = manager.getHomeLocation(player.getUniqueId(), homeNumber);
                if (dest != null) {
                    player.closeInventory();
                    String successMsg = "&fYou were teleported to your home.";
                    plugin.getTeleportManager().teleport(player, dest, 5, "&fTeleporting in &6%s", successMsg);
                }
            } else if (clickedMat == Material.RED_BED) {
                String storeMsg = HomeGUI.color("&fBuy&6 \u1d18\u0280\u026a\u0455\u1d0d+&f in /store for more homes");
                player.sendMessage(storeMsg);
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(storeMsg));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
            }
            return;
        }

        if ((slot >= HomeGUI.DYE_START && slot < HomeGUI.DYE_START + 5) ||
            (slot >= HomeGUI.DYE_START_2 && slot < HomeGUI.DYE_START_2 + 5)) {

            int homeNumber;
            if (slot >= HomeGUI.DYE_START && slot < HomeGUI.DYE_START + 5) {
                homeNumber = slot - HomeGUI.DYE_START + 1;
            } else {
                homeNumber = slot - HomeGUI.DYE_START_2 + 6;
            }

            Material clickedMat = event.getCurrentItem() != null ? event.getCurrentItem().getType() : Material.AIR;

            if (clickedMat == Material.PURPLE_DYE) {
                com.h2ph.gui.HomeDeleteConfirmGUI.open(player, plugin, homeNumber);

            } else if (clickedMat == Material.GRAY_DYE) {
                Location loc = player.getLocation().clone();
                manager.setHome(player.getUniqueId(), homeNumber, loc);

                player.sendMessage(HomeGUI.color("&7Home set"));

                player.sendActionBar(
                        LegacyComponentSerializer.legacyAmpersand().deserialize("&7Home set"));

                refreshGUI(player);
            } else if (clickedMat == Material.RED_DYE) {
                String storeMsg = HomeGUI.color("&fBuy&6 \u1d18\u0280\u026a\u0455\u1d0d+&f in /store for more homes");
                player.sendMessage(storeMsg);
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(storeMsg));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
            }
            return;
        }

        if (slot == 10 || slot == 19) {
            com.h2ph.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            boolean isOwner = data != null && "OWNER".equalsIgnoreCase(data.getTeamRole());

            if (team == null) {
                player.sendMessage(HomeGUI.color("&cYou must be in a team to use this."));
                return;
            }

            Material clickedMat = event.getCurrentItem() != null ? event.getCurrentItem().getType() : Material.AIR;

            if (clickedMat == Material.GRAY_BANNER || clickedMat == Material.GRAY_DYE) {
                if (!isOwner) {
                    player.sendMessage(HomeGUI.color("&cYour team does not have a home."));
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&cYour team does not have a home."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                plugin.getTeamManager().setTeamHome(team.getId(), player.getLocation());
                player.sendMessage(HomeGUI.color("&7Team home set"));
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Team home set"));
                refreshGUI(player);

            } else if (clickedMat == Material.PURPLE_BANNER) {
                Location dest = plugin.getTeamManager().getTeamHomeLocation(team.getId());
                if (dest != null) {
                    player.closeInventory();
                    String successMsg = "&fYou were teleported to your team home.";
                    plugin.getTeleportManager().teleport(player, dest, 5, "&fTeleporting in &6%s", successMsg);
                } else {
                    player.sendMessage(HomeGUI.color("&cYour team does not have a home."));
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&cYour team does not have a home."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }

            } else if (clickedMat == Material.PURPLE_DYE) {
                if (!isOwner) {
                    player.sendMessage(HomeGUI.color("&cOnly the team owner can delete the team home."));
                    return;
                }
                com.h2ph.gui.HomeDeleteConfirmGUI.open(player, plugin, 0);
            }
        }
    }

    private void handleDeleteConfirmation(InventoryClickEvent event, Player player,
            com.h2ph.gui.HomeDeleteConfirmGUI.HomeDeleteConfirmHolder holder) {
        int slot = event.getRawSlot();
        HomeManager manager = plugin.getHomeManager();
        int homeIndex = holder.getData().homeIndex();

        if (slot == 11) {
            refreshGUI(player);
        } else if (slot == 15) {
            if (homeIndex == 0) {
                com.h2ph.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team != null) {
                    plugin.getTeamManager().deleteTeamHome(team.getId());
                }
            } else {
                manager.deleteHome(player.getUniqueId(), homeIndex);
            }
            refreshGUI(player);
        }
    }

    private void refreshGUI(Player player) {
        HomeGUI.open(player, plugin);
    }
}
