package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CratesCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public CratesCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("falcon.crates")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
           // player.sendMessage(ChatColor.RED + "Usage: /crate <create|edit|get|delete|effects> ...");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 3) {
              //  player.sendMessage(ChatColor.RED + "Usage: /crate create <name> <key> [type] [container]");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            String type = "NORMAL";
            if (args.length >= 4) {
                type = args[3];
            }
            String container = "CHEST";
            if (args.length >= 5) {
                container = args[4];
            }
            return handleCrateCreate(player, args[1], args[2], type, container);
        } else if (sub.equals("edit")) {
            if (args.length < 2) {
               // player.sendMessage(ChatColor.RED + "Usage: /crate edit <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            return handleCrateEdit(player, args[1]);
        } else if (sub.equals("get")) {
            if (args.length < 2) {
              //  player.sendMessage(ChatColor.RED + "Usage: /crate get <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            return handleCrateGet(player, args[1]);
        } else if (sub.equals("delete")) {
            if (args.length < 2) {
               // player.sendMessage(ChatColor.RED + "Usage: /crate delete <name>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            return handleCrateDelete(player, args[1]);
        } else if (sub.equals("effects")) {
            if (args.length < 4) {
               // player.sendMessage(
                //        ChatColor.RED + "Usage: /crate effects <add|remove|set> <crate> <effect|all>");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            return handleCrateEffects(player, args[1], args[2], args[3]);
        }

        return true;
    }

    private boolean handleCrateCreate(Player player, String crateName, String keyName, String typeStr,
            String containerStr) {
        if (!plugin.getKeyAllManager().isValidKey(keyName)) {
        //    player.sendMessage(ChatColor.RED + "Invalid key: " + keyName);
        //    player.sendMessage(
        //            ChatColor.RED + "Available keys: " + String.join(", ", plugin.getKeyAllManager().getValidKeys()));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String type = typeStr.toUpperCase();
        if (!type.equals("NORMAL") && !type.equals("CAROUSEL")) {
          //  player.sendMessage(ChatColor.RED + "Invalid crate type. Options: NORMAL, CAROUSEL");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        String container = containerStr.toUpperCase();
        Material containerMat = Material.getMaterial(container);
        if (containerMat == null || (!containerMat.equals(Material.CHEST) && !containerMat.equals(Material.ENDER_CHEST)
                && !containerMat.name().endsWith("SHULKER_BOX"))) {
          //  player.sendMessage(
          //          ChatColor.RED + "Invalid container type. Must be CHEST, ENDER_CHEST, or SHULKER_BOX variant.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (crateFile.exists()) {
          //  player.sendMessage(ChatColor.RED + "A crate with that name already exists!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        try {
            crateFile.getParentFile().mkdirs();

            FileConfiguration crateConfig = new YamlConfiguration();
            crateConfig.set("key", keyName);
            crateConfig.set("type", type);
            crateConfig.set("container", container);
            crateConfig.save(crateFile);
          //  player.sendMessage(ChatColor.GREEN + "Crate config created: " + crateFile.getName() + " (" + type + ", "
          //          + container + ")");

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to create crate config file.");
            e.printStackTrace();
            return true;
        }

        giveCrateItem(player, crateName, keyName, container);
        return true;
    }

    private boolean handleCrateGet(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
         ///   player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key", "unknown");
        String container = config.getString("container", "CHEST");

        giveCrateItem(player, crateName, keyName, container);
        return true;
    }

    private boolean handleCrateDelete(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
           // player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
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

            org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "crate_id");
            data.set(key, org.bukkit.persistence.PersistentDataType.STRING, crateName);

            chest.setItemMeta(meta);
        }

        player.getInventory().addItem(chest);
        player.sendMessage(ChatColor.GREEN + "You received a " + crateName + " crate!");
    }

    private boolean handleCrateEdit(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
         //   player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        Inventory inv = org.bukkit.Bukkit.createInventory(player, 27,
                ChatColor.translateAlternateColorCodes('&', "&eEditing: " + crateName));

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String type = config.getString("type", "NORMAL");

        if (type.equalsIgnoreCase("CAROUSEL")) {
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
                }
            }
        }

        player.openInventory(inv);
        return true;
    }

    private boolean handleCrateEffects(Player player, String sub, String crateName, String effectName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists()) {
           // player.sendMessage(ChatColor.RED + "Crate not found: " + crateName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        List<String> effects = config.getStringList("effects");

        if (sub.equalsIgnoreCase("add")) {
            if (effects.contains(effectName)) {
                player.sendMessage(ChatColor.RED + "This effect is already added.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
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
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
                effects.remove(effectName);
                config.set("effects", effects);
                player.sendMessage(ChatColor.GREEN + "Removed effect " + effectName + " from " + crateName);
            }
        } else {
          //  player.sendMessage(ChatColor.RED + "Usage: /crate effects <add|remove|set> <crate> <effect|all>");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        try {
            config.save(crateFile);
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
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("create", "edit", "get", "delete", "effects");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("effects")) {
                List<String> subs = Arrays.asList("add", "remove", "set");
                return subs.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("get")
                    || args[0].equalsIgnoreCase("delete")) {
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
            if (args[0].equalsIgnoreCase("create")) {
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
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("effects")) {
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
            if (args[0].equalsIgnoreCase("create")) {
                return new ArrayList<>(plugin.getKeyAllManager().getValidKeys()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("effects")) {
                List<String> effects = new ArrayList<>();
                if (args[1].equalsIgnoreCase("remove")) {
                    effects.add("all");
                }
                effects.addAll(Arrays.asList("HELIX", "DOUBLE_HELIX", "HALO", "GROUND_RINGS", "VORTEX", "FOUNTAIN",
                        "DISCO", "BEACON", "PULSE", "ORBIT", "ENDER", "TORNADO", "SPHERE", "LAVA_DRIP", "ENCHANT",
                        "FLAME_CROWN"));
                return effects.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("create")) {
                List<String> types = Arrays.asList("NORMAL", "CAROUSEL");
                return types.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("create")) {
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
        }
        return Collections.emptyList();
    }
}
