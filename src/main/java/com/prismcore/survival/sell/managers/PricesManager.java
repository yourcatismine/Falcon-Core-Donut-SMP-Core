/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package com.prismcore.survival.sell.managers;

import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.category.Category;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class PricesManager {
    private final PrismSell plugin;
    private final Map<ItemKey, Double> prices;
    private final Map<ItemKey, Category> itemCategories;

    public PricesManager(PrismSell plugin) {
        this.plugin = plugin;
        this.prices = new HashMap<>();
        this.itemCategories = new HashMap<>();
        this.loadPrices();
    }

    public void loadPrices() {
        this.prices.clear();
        this.itemCategories.clear();
        File categoriesFolder = new File(this.plugin.getDataFolder(), "economy/sell/categories");
        if (!categoriesFolder.exists()) {
            categoriesFolder.mkdirs();
        }
        for (Category category : Category.values()) {
            String fileName = category.getKey().toLowerCase() + ".yml";
            File file = new File(categoriesFolder, fileName);
            if (!file.exists()) {
                try {
                    InputStream in = this.plugin.getResource("economy/sell/categories/" + fileName);
                    if (in != null) {
                        java.nio.file.Files.copy(in, file.toPath());
                    } else {
                        this.plugin.getLogger().warning("Could not find resource: economy/sell/categories/" + fileName);
                        continue;
                    }
                } catch (IOException e) {
                    this.plugin.getLogger().severe("Could not save category file: " + fileName);
                    e.printStackTrace();
                    continue;
                }
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.isConfigurationSection(category.getConfigPath()))
                continue;
            for (String key : config.getConfigurationSection(category.getConfigPath()).getKeys(false)) {
                ItemKey itemKey = parseItemKey(key);
                if (itemKey != null) {
                    double price = config.getDouble(category.getConfigPath() + "." + key);
                    this.prices.put(itemKey, price);
                    this.itemCategories.put(itemKey, category);
                } else {
                    this.plugin.getLogger().warning("Invalid material in " + fileName + ": " + key);
                }
            }
        }
        this.plugin.getLogger().info("Loaded " + this.prices.size() + " item prices from category files.");
    }

    private ItemKey parseItemKey(String key) {
        try {
            Material mat = Material.valueOf(key);
            return ItemKey.of(mat);
        } catch (IllegalArgumentException ignored) {
        }

        String[] parts = key.split(":");
        if (parts.length > 0) {
            Material mat = Material.matchMaterial(parts[0]);
            if (mat == null) {
                return null;
            }

            if (mat == Material.ENCHANTED_BOOK) {
                if (parts.length == 3) {
                    String enchantName = parts[1];
                    int level;
                    try {
                        level = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        return null;
                    }

                    Enchantment enchant = getEnchantment(enchantName);
                    if (enchant != null) {
                        return ItemKey.book(Collections.singletonMap(enchant, level));
                    }
                }
            } else if (ItemKey.isPotionLike(mat)) {
                if (parts.length >= 2) {
                    String typeName = parts[1];
                    boolean strong = parts.length >= 3 && parts[2].equals("2");

                    org.bukkit.potion.PotionType type = getPotionType(typeName, strong);
                    if (type != null) {
                        return ItemKey.potion(mat, type);
                    }
                }
            } else if (parts.length == 1) {
                return ItemKey.of(mat);
            }
        }

        return null;
    }

    private Enchantment getEnchantment(String name) {
        Enchantment ench = Enchantment.getByName(name.toUpperCase());
        if (ench == null) {
            for (Enchantment e : Enchantment.values()) {
                if (e.getKey().getKey().equalsIgnoreCase(name) || e.getName().equalsIgnoreCase(name)) {
                    return e;
                }
            }
        }
        return ench;
    }

    private org.bukkit.potion.PotionType getPotionType(String name, boolean strong) {
        String mappedName = name.toUpperCase();
        switch (mappedName) {
            case "SWIFTNESS":
                mappedName = "SPEED";
                break;
            case "LEAPING":
                mappedName = "JUMP";
                break;
            case "HEALING":
                mappedName = "INSTANT_HEAL";
                break;
            case "HARMING":
                mappedName = "INSTANT_DAMAGE";
                break;
            case "REGENERATION":
                mappedName = "REGEN";
                break;
        }

        String targetName = strong ? "STRONG_" + mappedName : mappedName;
        try {
            return org.bukkit.potion.PotionType.valueOf(targetName);
        } catch (IllegalArgumentException e) {
            try {
                String fallbackName = strong ? "STRONG_" + name.toUpperCase() : name.toUpperCase();
                return org.bukkit.potion.PotionType.valueOf(fallbackName);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public double getPrice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0.0;
        }

        ItemKey key = ItemKey.fromStack(item);
        if (this.prices.containsKey(key)) {
            return this.prices.get(key);
        }

        double maxPrice = 0.0;
        for (Map.Entry<ItemKey, Double> entry : this.prices.entrySet()) {
            if (entry.getKey().matches(item)) {
                if (entry.getValue() > maxPrice) {
                    maxPrice = entry.getValue();
                }
            }
        }

        return maxPrice;
    }

    public Category getCategory(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemKey key = ItemKey.fromStack(item);
        if (this.itemCategories.containsKey(key)) {
            return this.itemCategories.get(key);
        }

        for (Map.Entry<ItemKey, Category> entry : this.itemCategories.entrySet()) {
            if (entry.getKey().matches(item)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<ItemKey, Double> getPrices() {
        return Collections.unmodifiableMap(this.prices);
    }
}
