package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RulesCommand implements CommandExecutor, Listener {

    private final PrismSurvival plugin;
    private FileConfiguration config;
    private File configFile;

    public RulesCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "survival/rules/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("survival/rules/config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        openRulesGUI(player);
        return true;
    }

    private void openRulesGUI(Player player) {
        if (config == null) {
            loadConfig();
        }

        String title = ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "&8ʀᴜʟᴇѕ"));
        int size = config.getInt("gui.size", 27);

        Inventory gui = Bukkit.createInventory(null, size, title);

        if (config.contains("gui.items")) {
            for (String key : config.getConfigurationSection("gui.items").getKeys(false)) {
                String path = "gui.items." + key;
                int slot = config.getInt(path + ".slot");
                String materialName = config.getString(path + ".material", "BOOK");
                String name = config.getString(path + ".name", "&5Server Rules");
                List<String> lore = config.getStringList(path + ".lore");

                Material material = Material.matchMaterial(materialName);
                if (material == null)
                    material = Material.BOOK;

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(coloredLore);
                    item.setItemMeta(meta);
                }

                if (slot >= 0 && slot < size) {
                    gui.setItem(slot, item);
                }
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle()
                .equals(ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "&8ʀᴜʟᴇѕ")))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTitle()
                .equals(ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "&8ʀᴜʟᴇѕ")))) {
            event.setCancelled(true);
        }
    }
}
