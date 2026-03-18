package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuelGUIManager {

    private final PrismSurvival plugin;
    private static final Map<Character, Character> SMALL_CAPS_MAP = new HashMap<>();

    static {
        char[] normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        char[] small = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ".toCharArray();
        for (int i = 0; i < normal.length && i < small.length; i++) {
            SMALL_CAPS_MAP.put(normal[i], small[i]);
        }
    }

    private final org.bukkit.NamespacedKey regionKey;

    public DuelGUIManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.regionKey = new org.bukkit.NamespacedKey(plugin, "duel_region_name");
    }

    public static String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(SMALL_CAPS_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    public void openSettingsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', "&8ᴅᴜᴇʟ ѕᴇᴛᴛɪɴɢѕ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ"));

        ItemStack regionsItem = createItem(Material.GRASS_BLOCK, "&aʀᴇɢɪᴏɴѕ", null, "&fClick to view all regions");
        gui.setItem(11, regionsItem);

        ItemStack settingsItem = createItem(Material.WRITABLE_BOOK, "&aѕᴇᴛᴛɪɴɢѕ", null, "&fClick to open settings");
        gui.setItem(13, settingsItem);

        ItemStack playersItem = createItem(Material.NAME_TAG, "&aᴍᴀɴᴀɢᴇ ᴘʟᴀʏᴇʀѕ", null, "&fClick to manage players");
        gui.setItem(15, playersItem);

        player.openInventory(gui);
    }

    public void openRegionsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', "&8ʀᴇɢɪᴏɴѕ"));

        File regionsFolder = new File(plugin.getDataFolder(), "survival/regions/duels");
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
        }

        File[] files = regionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            int slot = 0;
            for (File file : files) {
                if (slot > 44)
                    break;

                String nameRaw = file.getName().replace(".yml", "");
                String nameSmallCaps = toSmallCaps(nameRaw);

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String creator = config.getString("created-by", "Unknown");

                ItemStack item = createItem(Material.GRASS_BLOCK,
                        "&a" + nameSmallCaps,
                        nameRaw,
                        "&fCreated by &f" + creator,
                        "&cClick to view settings");

                gui.setItem(slot, item);
                slot++;
            }
        }

        player.openInventory(gui);
    }

    public void openRegionSettingsGUI(Player player, String regionName) {
        String title = ChatColor.translateAlternateColorCodes('&', "&8" + toSmallCaps(regionName) + " ѕᴇᴛᴛɪɴɢѕ");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        File file = new File(plugin.getDataFolder(), "survival/regions/duels/" + regionName + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        int minutes = config.getInt("looting-minutes", 5);

        List<String> pos1Lore = new ArrayList<>();
        if (config.contains("spawn1.world")) {
            int x = config.getInt("spawn1.x");
            int y = config.getInt("spawn1.y");
            int z = config.getInt("spawn1.z");
            pos1Lore.add("&7" + x + " " + y + " " + z);
            pos1Lore.add("&cClick to unset");
        } else {
            pos1Lore.add("&fClick to set pos 1");
        }
        ItemStack pos1 = createItem(Material.ARMOR_STAND, "&aᴘʟᴀʏᴇʀ 1", regionName, pos1Lore.toArray(new String[0]));
        gui.setItem(11, pos1);

        ItemStack clock = createItem(Material.CLOCK, "&aᴍɪɴᴜᴛᴇѕ ꜰᴏʀ ʟᴏᴏᴛɪɴɢ", regionName,
                "&7" + minutes + " minutes",
                "&fLeft click to extend / Right click to deduct");
        gui.setItem(13, clock);

        List<String> pos2Lore = new ArrayList<>();
        if (config.contains("spawn2.world")) {
            int x = config.getInt("spawn2.x");
            int y = config.getInt("spawn2.y");
            int z = config.getInt("spawn2.z");
            pos2Lore.add("&7" + x + " " + y + " " + z);
            pos2Lore.add("&cClick to unset");
        } else {
            pos2Lore.add("&fClick to set pos 2");
        }
        ItemStack pos2 = createItem(Material.ARMOR_STAND, "&aᴘʟᴀʏᴇʀ 2", regionName, pos2Lore.toArray(new String[0]));
        gui.setItem(15, pos2);

        ItemStack backBtn = createItem(Material.ARROW, "&eʙᴀᴄᴋ", null, "&fReturn to regions");
        gui.setItem(18, backBtn);

        ItemStack deleteBtn = createItem(Material.RED_STAINED_GLASS_PANE, "&4ð¦¸¿ð¦»¿ð¦¿ð¦´ð¦Æð¦´ ð¦¦ð¦´ð¦¸ð¦ªð¦¾ð¦½",
                regionName,
                "&fClick to delete this region");
        gui.setItem(26, deleteBtn);

        player.openInventory(gui);
    }

    public void openDeleteConfirmGUI(Player player, String regionName) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&4ᴄᴏɴꜰɪʀᴍ ᴅᴇʟᴇᴛɪᴏɴ?"));

        ItemStack cancelBtn = createItem(Material.GREEN_STAINED_GLASS_PANE, "&aᴄᴀɴᴄᴇʟ", regionName,
                "&fKeep " + regionName);
        gui.setItem(11, cancelBtn);

        ItemStack infoBtn = createItem(Material.PAPER, "&e" + toSmallCaps(regionName), null,
                "&7Are you sure you want to delete this?",
                "&cThis action cannot be undone.");
        gui.setItem(13, infoBtn);

        ItemStack confirmBtn = createItem(Material.RED_CONCRETE, "&4ᴄᴏɴꜰɪʀᴍ ᴅᴇʟᴇᴛᴇ", regionName,
                "&fDelete " + regionName + " forever");
        gui.setItem(15, confirmBtn);

        player.openInventory(gui);
    }

    public org.bukkit.NamespacedKey getRegionKey() {
        return regionKey;
    }

    /**
     * Opens the duel queue GUI for a player.
     * Small chest (27 slots) with queue/confirm options.
     * 
     * @param player            The player to show the GUI to
     * @param statsManager      The stats manager to retrieve player stats
     * @param queuedPlayerCount Current number of players in queue
     */
    public void openQueueGUI(Player player, com.h2ph.commands.admin.duels.DuelStatsManager statsManager,
            int queuedPlayerCount) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴅᴜᴇʟ ǫᴜᴇᴜᴇ & ᴄᴏɴꜰɪʀᴍ"));

        java.util.UUID uuid = player.getUniqueId();
        int wins = statsManager.getWins(uuid);
        int losses = statsManager.getLosses(uuid);
        int streak = statsManager.getStreak(uuid);

        ItemStack cancelItem = createItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ", null, "&fClick to cancel");
        gui.setItem(10, cancelItem);

        ItemStack clockItem = createItem(Material.CLOCK, "&aᴡᴀɪᴛ ᴛɪᴍᴇ", null,
                "&7Estimated Wait: Calculating...",
                "&7Currently queued: " + queuedPlayerCount);
        gui.setItem(12, clockItem);

        ItemStack statsItem = createItem(Material.GRAY_DYE, "&aѕᴛᴀᴛɪѕᴛɪᴄѕ", null,
                "&7Wins: " + wins,
                "&7Losses: " + losses,
                "&7Streak: " + streak);
        gui.setItem(13, statsItem);

        ItemStack regionItem = createItem(Material.FEATHER, "&aʀᴇɢɪᴏɴ", null,
                "&7Europe (&d--&7)");
        gui.setItem(14, regionItem);

        ItemStack confirmItem = createItem(Material.GREEN_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ", null,
                "&fClick to start searching for match");
        gui.setItem(16, confirmItem);

        player.openInventory(gui);
    }

    private ItemStack createItem(Material material, String name, String storedRegionName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);

            if (storedRegionName != null) {
                meta.getPersistentDataContainer().set(regionKey, org.bukkit.persistence.PersistentDataType.STRING,
                        storedRegionName);
            }

            item.setItemMeta(meta);
        }
        return item;
    }
}
