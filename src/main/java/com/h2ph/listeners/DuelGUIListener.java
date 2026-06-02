package com.h2ph.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DuelGUIListener implements Listener {

    private final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ᴅᴜᴇʟ ѕᴇᴛᴛɪɴɢѕ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ");

    private final java.util.Map<java.util.UUID, String> setupSessions = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> isRedirecting = new java.util.HashSet<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (event.getRawSlot() == 11) {
                if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f,
                                    1.2f);
                        } catch (Exception ignored) {
                        }
                    }
                    com.h2ph.commands.admin.duels.DuelGUIManager guiManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                            com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));
                    guiManager.openRegionsGUI(player);
                }
            }
        } else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ʀᴇɢɪᴏɴѕ"))) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (event.getCurrentItem() != null && event.getWhoClicked() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();

                if (event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                    try {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.2f);
                    } catch (Exception ignored) {
                    }
                }

                com.h2ph.commands.admin.duels.DuelGUIManager guiManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));

                if (event.getCurrentItem().hasItemMeta()) {
                    org.bukkit.persistence.PersistentDataContainer pdc = event.getCurrentItem().getItemMeta()
                            .getPersistentDataContainer();
                    if (pdc.has(guiManager.getRegionKey(), org.bukkit.persistence.PersistentDataType.STRING)) {
                        String regionName = pdc.get(guiManager.getRegionKey(),
                                org.bukkit.persistence.PersistentDataType.STRING);
                        guiManager.openRegionSettingsGUI(player, regionName);
                    }
                }
            }
        } else if (title.contains(ChatColor.translateAlternateColorCodes('&', "ѕᴇᴛᴛɪɴɢѕ"))
                && !title.equals(GUI_TITLE)
                && !title.equals(com.h2ph.commands.player.SettingsCommand.GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (event.getCurrentItem() == null || !(event.getWhoClicked() instanceof org.bukkit.entity.Player))
                return;
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();

            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.2f);
            } catch (Exception ignored) {
            }

            com.h2ph.commands.admin.duels.DuelGUIManager guiManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                    com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));

            if (!event.getCurrentItem().hasItemMeta())
                return;
            org.bukkit.persistence.PersistentDataContainer pdc = event.getCurrentItem().getItemMeta()
                    .getPersistentDataContainer();
            if (!pdc.has(guiManager.getRegionKey(), org.bukkit.persistence.PersistentDataType.STRING))
                return;

            String regionName = pdc.get(guiManager.getRegionKey(), org.bukkit.persistence.PersistentDataType.STRING);
            java.io.File file = new java.io.File(
                    com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getDataFolder(),
                    "survival/regions/duels/" + regionName + ".yml");
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);

            int slot = event.getRawSlot();
            boolean save = false;
            boolean refresh = false;

            boolean isUnsetClick = false;
            if (event.getCurrentItem().getItemMeta().hasLore()) {
                for (String line : event.getCurrentItem().getItemMeta().getLore()) {
                    if (ChatColor.stripColor(line).contains("Click to unset")) {
                        isUnsetClick = true;
                        break;
                    }
                }
            }

            if (slot == 11) {
                if (isUnsetClick) {
                    config.set("spawn1", null);
                    player.sendMessage(ChatColor.GRAY + "Position 1 unset.");
                    save = true;
                    refresh = true;
                } else {
                    setupSessions.put(player.getUniqueId(), regionName + ":spawn1");
                    isRedirecting.add(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(
                            ChatColor.GRAY + "Go to the location for position 1 to set it and type " + ChatColor.GREEN
                                    + "confirm" + ChatColor.GRAY + " in chat.");
                }
            } else if (slot == 15) {
                if (isUnsetClick) {
                    config.set("spawn2", null);
                    player.sendMessage(ChatColor.GRAY + "Position 2 unset.");
                    save = true;
                    refresh = true;
                } else {
                    setupSessions.put(player.getUniqueId(), regionName + ":spawn2");
                    isRedirecting.add(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(
                            ChatColor.GRAY + "Go to the location for position 2 to set it and type " + ChatColor.GREEN
                                    + "confirm" + ChatColor.GRAY + " in chat.");
                }
            } else if (slot == 13) {
                int minutes = config.getInt("looting-minutes", 5);
                if (event.isLeftClick()) {
                    if (minutes < 10)
                        minutes++;
                } else if (event.isRightClick()) {
                    if (minutes > 3)
                        minutes--;
                }
                config.set("looting-minutes", minutes);
                save = true;
                refresh = true;
            } else if (slot == 18) {
                isRedirecting.add(player.getUniqueId());
                guiManager.openRegionsGUI(player);
                return;
            } else if (slot == 26) {
                isRedirecting.add(player.getUniqueId());
                guiManager.openDeleteConfirmGUI(player, regionName);
                return;
            }

            if (save) {
                try {
                    config.save(file);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Failed to save settings: " + e.getMessage());
                }
            }

            if (refresh) {
                isRedirecting.add(player.getUniqueId());
                guiManager.openRegionSettingsGUI(player, regionName);
            }
        } else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&4ᴄᴏɴꜰɪʀᴍ ᴅᴇʟᴇᴛɪᴏɴ?"))) {
            event.setCancelled(true);
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize())
                return;

            if (event.getCurrentItem() == null || !(event.getWhoClicked() instanceof org.bukkit.entity.Player))
                return;
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();

            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.2f);
            } catch (Exception ignored) {
            }

            com.h2ph.commands.admin.duels.DuelGUIManager guiManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                    com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));

            if (!event.getCurrentItem().hasItemMeta())
                return;
            org.bukkit.persistence.PersistentDataContainer pdc = event.getCurrentItem().getItemMeta()
                    .getPersistentDataContainer();
            if (!pdc.has(guiManager.getRegionKey(), org.bukkit.persistence.PersistentDataType.STRING))
                return;

            String regionName = pdc.get(guiManager.getRegionKey(), org.bukkit.persistence.PersistentDataType.STRING);
            int slot = event.getRawSlot();

            if (slot == 11) {
                isRedirecting.add(player.getUniqueId());
                guiManager.openRegionSettingsGUI(player, regionName);
            } else if (slot == 15) {
                java.io.File file = new java.io.File(
                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getDataFolder(),
                        "survival/regions/duels/" + regionName + ".yml");

                isRedirecting.add(player.getUniqueId());
                player.closeInventory();

                if (file.exists()) {
                    if (file.delete()) {
                        player.sendMessage(ChatColor.RED + "Region " + regionName + " deleted forever.");
                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getLogger().info(
                                "Player " + player.getName() + " deleted duel region: " + regionName);
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                        } catch (Exception ignored) {
                        }

                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getDuelArenaManager()
                                .reloadArena(regionName);
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to delete region file.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Region file not found (already deleted?).");
                }

                com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getSchedulerAdapter()
                        .runEntityTaskLater(player, () -> guiManager.openRegionsGUI(player), 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains(ChatColor.translateAlternateColorCodes('&', "ѕᴇᴛᴛɪɴɢѕ"))
                && !title.equals(GUI_TITLE)
                && !title.equals(com.h2ph.commands.player.SettingsCommand.GUI_TITLE)) {
            if (event.getPlayer() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getPlayer();
                if (!isRedirecting.contains(player.getUniqueId())) {
                    com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getSchedulerAdapter()
                            .runEntityTaskLater(player, () -> {
                                com.h2ph.commands.admin.duels.DuelGUIManager guiManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));
                                guiManager.openRegionsGUI(player);
                            }, 1L);
                }
                isRedirecting.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        if (setupSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            org.bukkit.entity.Player player = event.getPlayer();
            String msg = event.getMessage();

            if (msg.equalsIgnoreCase("confirm")) {
                String sessionData = setupSessions.remove(player.getUniqueId());
                String[] parts = sessionData.split(":");
                String regionName = parts[0];
                String path = parts[1];

                com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getSchedulerAdapter()
                        .runEntityTask(player, () -> {
                            try {
                                java.io.File file = new java.io.File(
                                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getDataFolder(),
                                        "survival/regions/duels/" + regionName + ".yml");
                                org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                                        .loadConfiguration(file);

                                org.bukkit.Location loc = player.getLocation();
                                config.set(path + ".world", loc.getWorld().getName());
                                config.set(path + ".x", loc.getBlockX());
                                config.set(path + ".y", loc.getBlockY());
                                config.set(path + ".z", loc.getBlockZ());
                                config.set(path + ".yaw", loc.getYaw());
                                config.set(path + ".pitch", loc.getPitch());

                                config.save(file);
                                player.sendMessage(ChatColor.GREEN + "Position set successfully.");
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                                com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class).getDuelArenaManager()
                                        .reloadArena(regionName);

                                com.h2ph.commands.admin.duels.DuelGUIManager reopenManager = new com.h2ph.commands.admin.duels.DuelGUIManager(
                                        com.h2ph.Falcon.getPlugin(com.h2ph.Falcon.class));
                                reopenManager.openRegionSettingsGUI(player, regionName);

                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "Error saving position.");
                                e.printStackTrace();
                            }
                        });

            } else if (msg.equalsIgnoreCase("cancel")) {
                setupSessions.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Setup cancelled.");
            } else {
                player.sendMessage(ChatColor.RED + "Please type 'confirm' to set the location, or 'cancel' to abort.");
            }
        }
    }
}
