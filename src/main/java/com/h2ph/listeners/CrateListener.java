package com.h2ph.listeners;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;

public class CrateListener implements Listener {

    private final Falcon plugin;
    private final org.bukkit.NamespacedKey crateKey;

    public CrateListener(Falcon plugin) {
        this.plugin = plugin;
        this.crateKey = new org.bukkit.NamespacedKey(plugin, "crate_id");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Material mat = item.getType();
        if (mat != Material.CHEST && mat != Material.ENDER_CHEST && !mat.name().endsWith("SHULKER_BOX"))
            return;

        if (!item.hasItemMeta())
            return;

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (data.has(crateKey, PersistentDataType.STRING)) {
            String crateId = data.get(crateKey, PersistentDataType.STRING);

            if (event.getBlockPlaced().getState() instanceof TileState) {
                TileState state = (TileState) event.getBlockPlaced().getState();
                state.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crateId);
                state.update();

                plugin.getCrateLocationRegistry().addLocation(crateId, event.getBlockPlaced().getLocation());

                event.getPlayer().sendMessage(ChatColor.GREEN + "Crate placed successfully!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof TileState) {
            TileState state = (TileState) event.getBlock().getState();
            if (state.getPersistentDataContainer().has(crateKey, PersistentDataType.STRING)) {
                plugin.getCrateLocationRegistry().removeLocation(event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (event.getClickedBlock() == null)
            return;

        Material mat = event.getClickedBlock().getType();
        if (mat != Material.CHEST && mat != Material.ENDER_CHEST && !mat.name().endsWith("SHULKER_BOX"))
            return;

        if (!(event.getClickedBlock().getState() instanceof TileState))
            return;

        TileState state = (TileState) event.getClickedBlock().getState();
        PersistentDataContainer data = state.getPersistentDataContainer();

        if (data.has(crateKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String crateName = data.get(crateKey, PersistentDataType.STRING);

            event.getPlayer().setMetadata("falcon_active_crate",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, crateName));

            openCrateGUI(event.getPlayer(), crateName);
        }
    }

    private void openCrateGUI(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: Crate configuration not found.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String type = config.getString("type", "NORMAL");

        if (type.equalsIgnoreCase("CAROUSEL")) {
            plugin.getCarouselManager().openCarouselGUI(player, crateName);
            return;
        }

        Inventory gui = org.bukkit.Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴄʜᴏᴏѕᴇ 1 ɪᴛᴇᴍ"));

        if (config.contains("contents")) {
            ConfigurationSection contents = config.getConfigurationSection("contents");
            for (String key : contents.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("contents." + key);
                    gui.setItem(slot, item);
                } catch (NumberFormatException e) {
                }
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʜᴏᴏѕᴇ 1 ɪᴛᴇᴍ"))) {
            if (event.getClickedInventory() == null)
                return;

            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            int slot = event.getSlot();
            if (slot >= 10 && slot <= 16) {
                if (player.hasMetadata("falcon_active_crate")) {
                    String crateName = player.getMetadata("falcon_active_crate").get(0).asString();
                    if (!playerHasKey(player, crateName)) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                }

                openConfirmGUI(player, clicked);
            }
        }

        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ"))) {
            if (event.getClickedInventory() == null)
                return;

            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }
            event.setCancelled(true);

            int slot = event.getSlot();
            if (slot == 22) {
                if (player.hasMetadata("falcon_active_crate")) {
                    String crateName = player.getMetadata("falcon_active_crate").get(0).asString();
                    plugin.getCarouselManager().handleStartClick(player, crateName, event.getInventory());
                }
            }
        }

        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ"))) {
            if (event.getClickedInventory() == null)
                return;

            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }
            event.setCancelled(true);

            int slot = event.getSlot();

            if (slot == 11) {
                if (player.hasMetadata("falcon_active_crate")) {
                    String crateName = player.getMetadata("falcon_active_crate").get(0).asString();
                    openCrateGUI(player, crateName);
                } else {
                    player.closeInventory();
                }
            } else if (slot == 15) {
                if (player.hasMetadata("falcon_active_crate")) {
                    String crateName = player.getMetadata("falcon_active_crate").get(0).asString();
                    ItemStack reward = event.getInventory().getItem(13);
                    if (reward != null) {
                        tryClaimReward(player, crateName, reward);
                    }
                } else {
                    player.closeInventory();
                }
            }
        }

        else if (title.startsWith(ChatColor.translateAlternateColorCodes('&', "&eEditing: "))) {
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                int slot = event.getSlot();

                String crateName = ChatColor.stripColor(title).replace("Editing: ", "");
                File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
                if (crateFile.exists()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
                    if (config.getString("type", "NORMAL").equalsIgnoreCase("CAROUSEL")) {
                        if (slot == 4 || slot == 22) {
                            event.setCancelled(true);
                            player.sendMessage(
                                    ChatColor.RED
                                            + "You cannot modify this slot as it is reserved for crate UI elements.");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        }
                    }
                }
            }
        }
    }

    private boolean playerHasKey(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists())
            return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key");

        com.falconcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data != null && data.getKeyCount(keyName) > 0;
    }

    private void openConfirmGUI(Player player, ItemStack item) {
        Inventory confirmGui = org.bukkit.Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ"));

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4ᴄᴀɴᴄᴇʟ"));
        cancel.setItemMeta(cancelMeta);
        confirmGui.setItem(11, cancel);

        confirmGui.setItem(13, item.clone());

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aᴄᴏɴꜰɪʀᴍ"));
        confirm.setItemMeta(confirmMeta);
        confirmGui.setItem(15, confirm);

        player.openInventory(confirmGui);
    }

    private void tryClaimReward(Player player, String crateName, ItemStack reward) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: Crate config missing.");
            player.closeInventory();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key");

        com.falconcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null)
            return;

        int keyCount = data.getKeyCount(keyName);
        if (keyCount > 0) {

            if (player.getInventory().firstEmpty() == -1) {
                String msg = ChatColor.translateAlternateColorCodes('&', "&cYour inventory is full.");
                player.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            }

            data.removeKey(keyName);
            ItemStack toGive = reward.clone();
            com.falconcore.survival.tools.ToolsManager toolsManager = com.falconcore.survival.tools.ToolsManager
                    .getInstance();
            if (toolsManager != null) {
                toolsManager.refreshExpiryForReward(toGive);
            }
            player.getInventory().addItem(toGive);

            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.translateAlternateColorCodes('&', "&eEditing: "))) {
            String crateName = ChatColor.stripColor(title).replace("Editing: ", "");
            saveCrateContents(crateName, event.getInventory());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Crate contents saved for " + crateName + "!");
        } else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ"))) {
            plugin.getCarouselManager().handleClose((Player) event.getPlayer());
        }

    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʜᴏᴏѕᴇ 1 ɪᴛᴇᴍ")) ||
                title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ")) ||
                title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ"))) {

            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (title.startsWith(ChatColor.translateAlternateColorCodes('&', "&eEditing: "))) {
            String crateName = ChatColor.stripColor(title).replace("Editing: ", "");
            File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
            if (crateFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
                if (config.getString("type", "NORMAL").equalsIgnoreCase("CAROUSEL")) {
                    for (int slot : event.getRawSlots()) {
                        if (slot == 4 || slot == 22) {
                            event.setCancelled(true);
                            ((Player) event.getWhoClicked())
                                    .sendMessage(ChatColor.RED + "You cannot modify this slot.");
                            return;
                        }
                    }
                }
            }
        }
    }

    private void saveCrateContents(String crateName, Inventory inv) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);

        config.set("contents", null);

        String type = config.getString("type", "NORMAL");

        for (int i = 0; i < inv.getSize(); i++) {
            if (type.equalsIgnoreCase("CAROUSEL") && (i == 4 || i == 22))
                continue;

            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                config.set("contents." + i, item);
            }
        }

        try {
            config.save(crateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crate config for " + crateName);
            e.printStackTrace();
        }
    }
}
