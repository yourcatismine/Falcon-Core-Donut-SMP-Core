package com.h2ph.commands.admin.crates;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public CrateCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prism.crates.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /crate <create|edit|get> ...");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /crate create <name> <key> [type] [container]");
                return true;
            }
            String type = "NORMAL"; // Default
            if (args.length >= 4) {
                type = args[3];
            }
            String container = "CHEST"; // Default
            if (args.length >= 5) {
                container = args[4];
            }
            return handleCreate(player, args[1], args[2], type, container);
        } else if (args[0].equalsIgnoreCase("edit")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /crate edit <name>");
                return true;
            }
            return handleEdit(player, args[1]);
        } else if (args[0].equalsIgnoreCase("get")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /crate get <name>");
                return true;
            }
            return handleGet(player, args[1]);
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /crate delete <name>");
                return true;
            }
            return handleDelete(player, args[1]);
        } else if (args[0].equalsIgnoreCase("effects")) {
            // /crate effects <add|remove> <crate> <effect>
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /crate effects <add|remove> <crate> <effect>");
                return true;
            }
            return handleEffects(player, args[1], args[2], args[3]);
        }

        return true;
    }

    private boolean handleCreate(Player player, String crateName, String keyName, String typeStr, String containerStr) {
        // Validate Key
        if (!plugin.getKeyAllManager().isValidKey(keyName)) {
            player.sendMessage(ChatColor.RED + "Invalid key: " + keyName);
            player.sendMessage(
                    ChatColor.RED + "Available keys: " + String.join(", ", plugin.getKeyAllManager().getValidKeys()));
            return true;
        }

        String type = typeStr.toUpperCase();
        if (!type.equals("NORMAL") && !type.equals("CAROUSEL")) {
            player.sendMessage(ChatColor.RED + "Invalid crate type. Options: NORMAL, CAROUSEL");
            return true;
        }

        String container = containerStr.toUpperCase();
        Material containerMat = Material.getMaterial(container);
        if (containerMat == null || (!containerMat.equals(Material.CHEST) && !containerMat.equals(Material.ENDER_CHEST)
                && !containerMat.name().endsWith("SHULKER_BOX"))) {
            player.sendMessage(
                    ChatColor.RED + "Invalid container type. Must be CHEST, ENDER_CHEST, or SHULKER_BOX variant.");
            return true;
        }

        // Create Config File
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "A crate with that name already exists!");
            return true;
        }

        try {
            // Ensure directory exists
            crateFile.getParentFile().mkdirs();

            FileConfiguration crateConfig = new YamlConfiguration();
            crateConfig.set("key", keyName);
            crateConfig.set("type", type);
            crateConfig.set("container", container);
            crateConfig.save(crateFile);
            player.sendMessage(ChatColor.GREEN + "Crate config created: " + crateFile.getName() + " (" + type + ", "
                    + container + ")");

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to create crate config file.");
            e.printStackTrace();
            return true;
        }

        giveCrateItem(player, crateName, keyName, container);
        return true;
    }

    private boolean handleGet(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            return true;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key", "unknown");
        String container = config.getString("container", "CHEST");

        giveCrateItem(player, crateName, keyName, container);
        return true;
    }

    private boolean handleDelete(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            return true;
        }

        if (crateFile.delete()) {
            player.sendMessage(ChatColor.GREEN + "Crate " + crateName + " deleted successfully!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete crate file.");
        }
        return true;
    }

    private void giveCrateItem(Player player, String crateName, String keyName, String containerType) {
        Material mat = Material.getMaterial(containerType);
        if (mat == null)
            mat = Material.CHEST;

        ItemStack chest = new ItemStack(mat);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + crateName + " Crate"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Key: " + ChatColor.YELLOW + keyName);
            lore.add(ChatColor.GRAY + "Place this to create the crate.");
            meta.setLore(lore);

            // Add PDC tag
            org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "crate_id");
            data.set(key, org.bukkit.persistence.PersistentDataType.STRING, crateName);

            chest.setItemMeta(meta);
        }

        player.getInventory().addItem(chest);
        player.sendMessage(ChatColor.GREEN + "You received a " + crateName + " crate!");
    }

    private boolean handleEdit(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            return true;
        }

        // Open editing GUI
        Inventory inv = org.bukkit.Bukkit.createInventory(player, 27,
                ChatColor.translateAlternateColorCodes('&', "&eEditing: " + crateName));

        // Load items from config
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String type = config.getString("type", "NORMAL");

        if (type.equalsIgnoreCase("CAROUSEL")) {
            // Pre-fill Reserved Slots
            ItemStack reserved = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = reserved.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Reserved Slot");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Reserved for UI Elements"));
            reserved.setItemMeta(meta);

            inv.setItem(4, reserved);
            inv.setItem(22, reserved);
        }
        if (config.contains("contents")) {
            ConfigurationSection contents = config.getConfigurationSection("contents");
            for (String key : contents.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("contents." + key);
                    inv.setItem(slot, item);
                } catch (NumberFormatException e) {
                    // Ignore invalid keys
                }
            }
        }

        player.openInventory(inv);
        return true;
    }

    private boolean handleEffects(Player player, String sub, String crateName, String effectName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
            player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            return true;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        List<String> effects = config.getStringList("effects");

        if (sub.equalsIgnoreCase("add")) {
            if (effects.contains(effectName)) {
                player.sendMessage(ChatColor.RED + "This effect is already added.");
                return true;
            }
            effects.add(effectName);
            config.set("effects", effects);
            player.sendMessage(ChatColor.GREEN + "Added effect " + effectName + " to " + crateName);
        } else if (sub.equalsIgnoreCase("set")) {
            effects.clear();
            effects.add(effectName);
            config.set("effects", effects);
            player.sendMessage(ChatColor.GREEN + "Set effect to " + effectName + " for " + crateName);
        } else if (sub.equalsIgnoreCase("remove")) {
            if (effectName.equalsIgnoreCase("all")) {
                effects.clear();
                config.set("effects", effects);
                player.sendMessage(ChatColor.GREEN + "Removed ALL effects from " + crateName);
            } else {
                if (!effects.contains(effectName)) {
                    player.sendMessage(ChatColor.RED + "This effect is not present.");
                    return true;
                }
                effects.remove(effectName);
                config.set("effects", effects);
                player.sendMessage(ChatColor.GREEN + "Removed effect " + effectName + " from " + crateName);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /crate effects <add|remove|set> <crate> <effect|all>");
            return true;
        }

        try {
            config.save(crateFile);
            // Refresh cache
            if (plugin.getCrateEffectsManager() != null) {
                plugin.getCrateEffectsManager().clearCache(crateName);
            }
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to save config.");
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prism.crates.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("create");
            subcommands.add("edit");
            subcommands.add("get");
            subcommands.add("delete");
            subcommands.add("effects");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("effects")) {
                List<String> subs = new ArrayList<>();
                subs.add("add");
                subs.add("remove");
                subs.add("set");
                return subs.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("get")
                    || args[0].equalsIgnoreCase("delete")) {
                // Suggest crate names
                File cratesDir = new File(plugin.getDataFolder(), "crates/crate");
                if (cratesDir.exists() && cratesDir.isDirectory()) {
                    List<String> crates = new ArrayList<>();
                    for (File file : cratesDir.listFiles()) {
                        if (file.getName().endsWith("-crate.yml")) {
                            crates.add(file.getName().replace("-crate.yml", ""));
                        }
                    }
                    return crates.stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("effects")) {
            // Suggest Crate Names
            File cratesDir = new File(plugin.getDataFolder(), "crates/crate");
            if (cratesDir.exists() && cratesDir.isDirectory()) {
                List<String> crates = new ArrayList<>();
                for (File file : cratesDir.listFiles()) {
                    if (file.getName().endsWith("-crate.yml")) {
                        crates.add(file.getName().replace("-crate.yml", ""));
                    }
                }
                return crates.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("effects")) {
            List<String> effects = new ArrayList<>();
            if (args[1].equalsIgnoreCase("remove")) {
                effects.add("all");
            }
            effects.add("HELIX");
            effects.add("DOUBLE_HELIX");
            effects.add("HALO");
            effects.add("GROUND_RINGS");
            effects.add("VORTEX");
            effects.add("FOUNTAIN");
            effects.add("DISCO");
            effects.add("BEACON");
            effects.add("PULSE");
            effects.add("ORBIT");
            effects.add("ENDER");
            effects.add("TORNADO");
            effects.add("SPHERE");
            effects.add("LAVA_DRIP");
            effects.add("ENCHANT");
            effects.add("FLAME_CROWN");
            return effects.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return new ArrayList<>(plugin.getKeyAllManager().getValidKeys()).stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            List<String> types = new ArrayList<>();
            types.add("NORMAL");
            types.add("CAROUSEL");
            return types.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            List<String> containers = new ArrayList<>();
            containers.add("CHEST");
            containers.add("ENDER_CHEST");
            for (Material mat : Material.values()) {
                if (mat.name().endsWith("SHULKER_BOX")) {
                    containers.add(mat.name());
                }
            }
            return containers.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
