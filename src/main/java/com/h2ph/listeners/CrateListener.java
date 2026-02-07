package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
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

    private final PrismSurvival plugin;
    private final org.bukkit.NamespacedKey crateKey;

    public CrateListener(PrismSurvival plugin) {
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

            // Transfer to Tile Entity
            if (event.getBlockPlaced().getState() instanceof TileState) {
                TileState state = (TileState) event.getBlockPlaced().getState();
                state.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crateId);
                state.update();

                // Register location
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
                // It's a crate, remove from registry
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

            // Set metadata for the active crate interacting session
            event.getPlayer().setMetadata("prism_active_crate",
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
        String type = config.getString("type", "NORMAL"); // Default to Normal

        if (type.equalsIgnoreCase("CAROUSEL")) {
            plugin.getCarouselManager().openCarouselGUI(player, crateName);
            return;
        }

        Inventory gui = org.bukkit.Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴄʜᴏᴏѕᴇ 1 ɪᴛᴇᴍ"));

        // Populate GUI from config
        if (config.contains("contents")) {
            ConfigurationSection contents = config.getConfigurationSection("contents");
            for (String key : contents.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("contents." + key);
                    gui.setItem(slot, item);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // 1. Crate Selection GUI (Normal)
        if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʜᴏᴏѕᴇ 1 ɪᴛᴇᴍ"))) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            int slot = event.getSlot();
            // Check if slot is within 10-16 (Inclusive) and has an item
            if (slot >= 10 && slot <= 16) {
                // Check for key BEFORE opening confirm GUI
                if (player.hasMetadata("prism_active_crate")) {
                    String crateName = player.getMetadata("prism_active_crate").get(0).asString();
                    if (!playerHasKey(player, crateName)) {
                        // No Key Logic: No Message, Villager No Sound, Do NOT proceed
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                }

                openConfirmGUI(player, clicked);
            }
        }

        // 2. Carousel GUI
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ"))) {
            event.setCancelled(true);

            // Allow clicking start button
            int slot = event.getSlot();
            if (slot == 22) {
                if (player.hasMetadata("prism_active_crate")) {
                    String crateName = player.getMetadata("prism_active_crate").get(0).asString();
                    plugin.getCarouselManager().handleStartClick(player, crateName, event.getInventory());
                }
            }
        }

        // 3. Confirmation GUI
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ"))) {
            event.setCancelled(true);
            int slot = event.getSlot();

            if (slot == 11) { // Cancel
                // Go back to Crate GUI
                if (player.hasMetadata("prism_active_crate")) {
                    String crateName = player.getMetadata("prism_active_crate").get(0).asString();
                    openCrateGUI(player, crateName);
                } else {
                    player.closeInventory();
                }
            } else if (slot == 15) { // Confirm
                if (player.hasMetadata("prism_active_crate")) {
                    String crateName = player.getMetadata("prism_active_crate").get(0).asString();
                    ItemStack reward = event.getInventory().getItem(13); // The item in slot 13
                    if (reward != null) {
                        tryClaimReward(player, crateName, reward);
                    }
                } else {
                    player.closeInventory();
                }
            }
        }

        // 4. Edit GUI Restriction
        else if (title.startsWith(ChatColor.translateAlternateColorCodes('&', "&eEditing: "))) {
            // Only check clicks in the top inventory (The Crate GUI)
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

    // Helper to check key without deducting
    private boolean playerHasKey(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists())
            return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key");

        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        // PlayerData
        return data != null && data.getKeyCount(keyName) > 0;
    }

    private void openConfirmGUI(Player player, ItemStack item) {
        Inventory confirmGui = org.bukkit.Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴄᴏɴꜰɪʀᴍ"));

        // Slot 11: Cancel (Red Glass)
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4ᴄᴀɴᴄᴇʟ"));
        cancel.setItemMeta(cancelMeta);
        confirmGui.setItem(11, cancel);

        // Slot 13: The Item
        confirmGui.setItem(13, item.clone());

        // Slot 15: Confirm (Green Glass)
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE); // LIME is usually better for 'Green' in
                                                                             // GUI
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

        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        // PlayerData
        if (data == null)
            return;

        int keyCount = data.getKeyCount(keyName);
        if (keyCount > 0) {

            // Check Inventory Full
            if (player.getInventory().firstEmpty() == -1) {
                // Inventory Full Logic
                String msg = ChatColor.translateAlternateColorCodes('&', "&cYour inventory is full.");
                player.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            }

            // Success
            data.removeKey(keyName);
            player.getInventory().addItem(reward.clone());
            // No message, No sound
            player.closeInventory();
        } else {
            // Should not happen if pre-checked, but safe fallback
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();

        // Editing Save Logic
        if (title.startsWith(ChatColor.translateAlternateColorCodes('&', "&eEditing: "))) {
            String crateName = ChatColor.stripColor(title).replace("Editing: ", "");
            saveCrateContents(crateName, event.getInventory());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Crate contents saved for " + crateName + "!");
        }
        // Carousel Close Logic
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ"))) {
            plugin.getCarouselManager().handleClose((Player) event.getPlayer());
        }

        // Note: We don't clear metadata here because switching GUIs (Crate -> Confirm)
        // triggers close.
        // The metadata can persist on the player object harmlessly or be overwritten
        // next time.
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        String title = event.getView().getTitle();
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

        config.set("contents", null); // Clear old contents

        String type = config.getString("type", "NORMAL");

        for (int i = 0; i < inv.getSize(); i++) {
            // Skip reserved slots ONLY for CAROUSEL
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
