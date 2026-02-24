package com.h2ph.managers;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RTPDeathManager {

    private final PrismSurvival plugin;
    private final File file;
    private FileConfiguration config;
    private List<ItemStack> items = new ArrayList<>();

    public RTPDeathManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rtp/death_items.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
        items = (List<ItemStack>) config.get("items", new ArrayList<>());
    }

    public void save() {
        config.set("items", items);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save death_items.yml");
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
        save();
    }

    public void clearItems() {
        this.items.clear();
        save();
    }
}
