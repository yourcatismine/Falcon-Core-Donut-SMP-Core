package com.prismcore.survival.spawners.gui;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.spawners.holder.SpawnerGUIHolder;
import com.prismcore.survival.spawners.storage.SpawnerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpawnerGUI {
    private final PrismSurvival plugin;
    private final SpawnerData data;
    private final Inventory inventory;
    private final boolean isStorage;
    private final int page;
    private final int totalPages;

    public SpawnerGUI(PrismSurvival plugin, SpawnerData data, boolean isStorage) {
        this(plugin, data, isStorage, 1);
    }

    public SpawnerGUI(PrismSurvival plugin, SpawnerData data, boolean isStorage, int page) {
        this.plugin = plugin;
        this.data = data;
        this.isStorage = isStorage;
        this.page = page;
        int totalStacks = 0;
        if (isStorage) {
            for (Map.Entry<Material, Long> entry : data.getAccumulatedDrops().entrySet()) {
                totalStacks += (int) Math.ceil(entry.getValue() / 64.0);
            }
        }
        int calcPages = isStorage ? (int) Math.ceil(totalStacks / 45.0) : 1;
        this.totalPages = calcPages == 0 ? 1 : calcPages;

        String rawType = data.getType().getDisplayName();
        String stylizedType = stylizeSmallCaps(rawType);
        String title;
        if (isStorage) {
            title = "&8" + data.getStackSize() + " " + stylizedType + " &8ѕᴘᴀᴡɴᴇʀ" + " (&8" + page + "/" + this.totalPages + "&8)";
        } else {
            title = "&8" + data.getStackSize() + " " + stylizedType + " &8ѕᴘᴀᴡɴᴇʀ";
        }
        String titleColored = ChatColor.translateAlternateColorCodes('&', title);

        this.inventory = Bukkit.createInventory(new SpawnerGUIHolder(data, isStorage), isStorage ? 54 : 27, titleColored);
        setupGUI();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void setupGUI() {
        if (isStorage) {
            setupStorage();
        } else {
            setupMain();
        }
    }

    private void setupMain() {
        ItemStack skull = new ItemStack(data.getType().getHeadMaterial());
        ItemMeta skullMeta = skull.getItemMeta();
    
        String typeAndPlural = data.getType().getDisplayName() + " spawners";
        String stylizedFull = stylizeSmallCaps(typeAndPlural);
        String skullName = "&d" + data.getStackSize() + " " + stylizedFull;
        skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', skullName));
    
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&d\u25CF&f Click to sell items and collect xp"));
    
        long storedItems = data.getAccumulatedDrops().values().stream().mapToLong(Long::longValue).sum();
        long storageCapacity = Math.max(1L, plugin.getSpawnerConfig().getLong("settings.storage_capacity", 1_000_000L));
        double fillPercent = Math.min(100.0D, (storedItems * 100.0D) / storageCapacity);
        String percentStr = String.format(Locale.ENGLISH, "%.1f", fillPercent);
    
        String storageLine = "&dStorage:&f " + percentStr + "%&d Filled";
        lore.add(ChatColor.translateAlternateColorCodes('&', storageLine));
    
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        inventory.setItem(13, skull);
    
        ItemStack storageItem = new ItemStack(Material.CHEST);
        ItemMeta storageMeta = storageItem.getItemMeta();
        String storageTitle = "&d" + stylizeSmallCaps("Spawner Storage");
        storageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', storageTitle));
        List<String> storageLore = new ArrayList<>();
        data.getAccumulatedDrops().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.<Material, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(2)
                .forEach(entry -> {
                    String amount = formatCompactAmountWithDecimal(entry.getValue());
                    String itemName = capitalizeWords(entry.getKey().name());
                    storageLore.add(ChatColor.translateAlternateColorCodes('&', "&d" + amount + "&f " + itemName));
                });
    
        if (storageLore.isEmpty()) {
            storageLore.add(ChatColor.translateAlternateColorCodes('&', "&7Empty"));
        }
        storageMeta.setLore(storageLore);
        storageItem.setItemMeta(storageMeta);
        inventory.setItem(11, storageItem);
    
        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBottle.getItemMeta();
        String xpTitle = "&a" + stylizeSmallCaps("Collect XP");
        xpMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', xpTitle));
        List<String> xpLore = new ArrayList<>();
        String xpVal = formatCompactAmountWithDecimal(data.getAccumulatedXP());
        xpLore.add(ChatColor.translateAlternateColorCodes('&', "&a" + xpVal + "&f XP Points"));
        xpMeta.setLore(xpLore);
        xpBottle.setItemMeta(xpMeta);
        inventory.setItem(15, xpBottle);
    }


    private void setupStorage() {
        NamespacedKey pageKey = new NamespacedKey(plugin, "gui_page");
        NamespacedKey targetPageKey = new NamespacedKey(plugin, "gui_target_page");

        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        List<String> backLore = new ArrayList<>();
        backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cʙᴀᴄᴋ"));
        backLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to return to the spawner"));
        backMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);

        if (page > 1) {
            ItemStack previousArrow = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previousArrow.getItemMeta();
            previousMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dᴘʀᴇᴠɪᴏᴜs"));
            List<String> previousLore = new ArrayList<>();
            previousLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to go back a page"));
            previousMeta.setLore(previousLore);
            previousMeta.getPersistentDataContainer().set(targetPageKey, PersistentDataType.INTEGER, page - 1);
            previousArrow.setItemMeta(previousMeta);
            inventory.setItem(48, previousArrow);
        }

        ItemStack spawnerCollectItem = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta spawnerCollectMeta = spawnerCollectItem.getItemMeta();
        spawnerCollectMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dѕᴘᴀᴡɴᴇʀ"));
        List<String> spawnerCollectLore = new ArrayList<>();
        spawnerCollectLore.add(ChatColor.translateAlternateColorCodes('&', "&d● &fCollect your loot from the storage"));
        spawnerCollectMeta.setLore(spawnerCollectLore);
        spawnerCollectItem.setItemMeta(spawnerCollectMeta);
        inventory.setItem(49, spawnerCollectItem);

        if (page < totalPages) {
            ItemStack nextArrow = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextArrow.getItemMeta();
            nextMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dɴᴇxᴛ"));
            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to go to next page"));
            nextMeta.setLore(nextLore);
            nextMeta.getPersistentDataContainer().set(targetPageKey, PersistentDataType.INTEGER, page + 1);
            nextArrow.setItemMeta(nextMeta);
            inventory.setItem(50, nextArrow);
        }

        ItemStack dropper = new ItemStack(Material.DROPPER);
        ItemMeta dropperMeta = dropper.getItemMeta();
        dropperMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dᴅʀᴏᴘ ʟᴏᴏᴛ"));
        List<String> dropLore = new ArrayList<>();
        dropLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to drop all loot on the page"));
        dropperMeta.setLore(dropLore);
        dropper.setItemMeta(dropperMeta);
        inventory.setItem(52, dropper);

        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = goldIngot.getItemMeta();
        goldMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dѕᴇʟʟ ᴀʟʟ"));
        List<String> sellAllLore = new ArrayList<>();
        sellAllLore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to sell all mob drops!"));
        goldMeta.setLore(sellAllLore);
        goldIngot.setItemMeta(goldMeta);
        inventory.setItem(53, goldIngot);

        int slot = 0;
        int startIndex = (page - 1) * 45;
        int endIndex = startIndex + 45;
        int index = 0;
        for (Map.Entry<Material, Long> entry : data.getAccumulatedDrops().entrySet()) {
            Material mat = entry.getKey();
            long totalAmount = entry.getValue();
            long remaining = totalAmount;
            while (remaining > 0 && index < endIndex) {
                if (index >= startIndex) {
                    if (slot >= 45) break;
                    int stackSize = (int) Math.min(remaining, 64);
                    ItemStack item = new ItemStack(mat, stackSize);
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + capitalize(mat.name()) + " x" + stackSize));
                    item.setItemMeta(itemMeta);
                    inventory.setItem(slot, item);
                    slot++;
                }
                remaining -= 64;
                index++;
            }
            if (slot >= 45) break;
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    private String formatCompactAmount(long amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.ENGLISH, "%.1fM", amount / 1_000_000.0);
        }
        if (amount >= 1_000) {
            return String.format(Locale.ENGLISH, "%.0fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }

    private String formatCompactAmountWithDecimal(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format(Locale.ENGLISH, "%.1fB", amount / 1_000_000_000.0);
        }
        if (amount >= 1_000_000) {
            return String.format(Locale.ENGLISH, "%.1fM", amount / 1_000_000.0);
        }
        if (amount >= 1_000) {
            return String.format(Locale.ENGLISH, "%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }

    private String capitalizeWords(String input) {
        String[] parts = input.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i > 0) result.append(" ");
            result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return result.toString();
    }

    private static final Map<Character, String> SMALL_CAPS_MAP = new HashMap<>();
    static {
        SMALL_CAPS_MAP.put('a', "ᴀ");
        SMALL_CAPS_MAP.put('b', "ʙ");
        SMALL_CAPS_MAP.put('c', "ᴄ");
        SMALL_CAPS_MAP.put('d', "ᴅ");
        SMALL_CAPS_MAP.put('e', "ᴇ");
        SMALL_CAPS_MAP.put('f', "ꜰ");
        SMALL_CAPS_MAP.put('g', "ɢ");
        SMALL_CAPS_MAP.put('h', "ʜ");
        SMALL_CAPS_MAP.put('i', "ɪ");
        SMALL_CAPS_MAP.put('j', "ᴊ");
        SMALL_CAPS_MAP.put('k', "ᴋ");
        SMALL_CAPS_MAP.put('l', "ʟ");
        SMALL_CAPS_MAP.put('m', "ᴍ");
        SMALL_CAPS_MAP.put('n', "ɴ");
        SMALL_CAPS_MAP.put('o', "ᴏ");
        SMALL_CAPS_MAP.put('p', "ᴘ");
        SMALL_CAPS_MAP.put('q', "ǫ");
        SMALL_CAPS_MAP.put('r', "ʀ");
        SMALL_CAPS_MAP.put('s', "ѕ");
        SMALL_CAPS_MAP.put('t', "ᴛ");
        SMALL_CAPS_MAP.put('u', "ᴜ");
        SMALL_CAPS_MAP.put('v', "ᴠ");
        SMALL_CAPS_MAP.put('w', "ᴡ");
        SMALL_CAPS_MAP.put('x', "х");
        SMALL_CAPS_MAP.put('y', "ʏ");
        SMALL_CAPS_MAP.put('z', "ᴢ");

        SMALL_CAPS_MAP.put('A', "ᴀ");
        SMALL_CAPS_MAP.put('B', "ʙ");
        SMALL_CAPS_MAP.put('C', "ᴄ");
        SMALL_CAPS_MAP.put('D', "ᴅ");
        SMALL_CAPS_MAP.put('E', "ᴇ");
        SMALL_CAPS_MAP.put('F', "ꜰ");
        SMALL_CAPS_MAP.put('G', "ɢ");
        SMALL_CAPS_MAP.put('H', "ʜ");
        SMALL_CAPS_MAP.put('I', "ɪ");
        SMALL_CAPS_MAP.put('J', "ᴊ");
        SMALL_CAPS_MAP.put('K', "ᴋ");
        SMALL_CAPS_MAP.put('L', "ʟ");
        SMALL_CAPS_MAP.put('M', "ᴍ");
        SMALL_CAPS_MAP.put('N', "ɴ");
        SMALL_CAPS_MAP.put('O', "ᴏ");
        SMALL_CAPS_MAP.put('P', "ᴘ");
        SMALL_CAPS_MAP.put('Q', "ǫ");
        SMALL_CAPS_MAP.put('R', "ʀ");
        SMALL_CAPS_MAP.put('S', "ѕ");
        SMALL_CAPS_MAP.put('T', "ᴛ");
        SMALL_CAPS_MAP.put('U', "ᴜ");
        SMALL_CAPS_MAP.put('V', "ᴠ");
        SMALL_CAPS_MAP.put('W', "ᴡ");
        SMALL_CAPS_MAP.put('X', "х");
        SMALL_CAPS_MAP.put('Y', "ʏ");
        SMALL_CAPS_MAP.put('Z', "ᴢ");
        SMALL_CAPS_MAP.put(' ', " ");
        SMALL_CAPS_MAP.put('_', " ");
        SMALL_CAPS_MAP.put('-', " ");
    }

    private String stylizeSmallCaps(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        String trimmed = input.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            String mapped = SMALL_CAPS_MAP.get(c);
            if (mapped != null) {
                sb.append(mapped);
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }
}