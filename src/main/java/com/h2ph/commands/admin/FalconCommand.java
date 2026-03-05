package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.auction.GUIHandler;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

public class FalconCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public FalconCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(
                    "§cUsage: /falcon <auction|order|rtpqueue|speed|tools|void|respawngear|limiter|crate|crystal|anchor> [args]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (sub.equals("auction")) {
            if (!player.hasPermission("auction.admin")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon auction <player>");
                return true;
            }
            String targetName = args[1];
            player.setMetadata("ah-admin-view", new FixedMetadataValue(plugin, true));
            GUIHandler.openAdminPlayerDetailsGUI(player, targetName, plugin.getAuctionController());
            return true;
        } else if (sub.equals("order")) {
            if (args.length >= 2) {
                if (!player.hasPermission("prism.admin.orders")) {
                    player.sendMessage("§cYou do not have permission to manage player orders.");
                    return true;
                }
                String targetName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || target.getName() == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                new com.prismcore.survival.orders.gui.AdminOrderDetailsMenu(OrdersModule.getInstance(), player, target)
                        .open();
                return true;
            }

            if (!player.hasPermission("order.use")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            OrdersModule.getInstance().state().resetMain(player.getUniqueId());
            new OrdersMainMenu(OrdersModule.getInstance(), player).open();
            return true;
        }

        if (sub.equals("rtpqueue")) {
            if (!player.hasPermission("falcon.rtpqueue")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                return true;
            }

            String action = args[1].toLowerCase();

            if (action.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue create <region>");
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().createQueue(player, regionName);
                return true;
            } else if (action.equals("delete")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon rtpqueue delete <region>");
                    return true;
                }
                String regionName = args[2];
                plugin.getRTPQueueManager().deleteQueue(player, regionName);
                return true;
            } else {
                player.sendMessage("§cUsage: /falcon rtpqueue <create|delete> <region>");
                return true;
            }
        }

        if (sub.equals("speed")) {
            if (args.length < 2) {
                return true;
            }

            try {
                float speed;
                String speedArg = args[1].toLowerCase();

                if (speedArg.equals("normal")) {
                    speed = 0.1f;
                } else {
                    speed = Float.parseFloat(speedArg) / 10.0f;
                }

                if (speed > 1.0f) {
                    speed = 1.0f;
                } else if (speed < 0.0001f) {
                    speed = 0.0001f;
                }

                player.setFlySpeed(speed);
            } catch (Exception ignored) {
                // No error messages as requested
            }
            return true;
        }

        if (sub.equals("tools")) {
            if (!player.hasPermission("prism.admin.tools")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§cUsage: /falcon tools <tool> <player> [duration]");
                return true;
            }

            String toolType = args[1].toLowerCase();
            String playerName = args[2];
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                player.sendMessage(com.prismcore.survival.tools.Utils.formatColors("&cThat player is not online."));
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                com.prismcore.survival.tools.Utils.formatColors("&cThat player is not online.")));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            if (target.getInventory().firstEmpty() == -1) {
                player.sendMessage(com.prismcore.survival.tools.Utils.formatColors("&cThis player inventory is full!"));
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                com.prismcore.survival.tools.Utils.formatColors("&cThis player inventory is full!")));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }

            long overrideTimer = 0;
            if (args.length >= 4) {
                overrideTimer = com.prismcore.survival.tools.Utils.parseDuration(args[3]);
                if (overrideTimer <= 0) {
                    player.sendMessage("§cInvalid duration format. Use: 1d, 12h, 30m");
                    return true;
                }
            }

            com.prismcore.survival.tools.ToolsManager manager = com.prismcore.survival.tools.ToolsManager.getInstance();
            if (manager == null) {
                player.sendMessage("§cToolsManager is not initialized.");
                return true;
            }

            if (Arrays.asList("drill", "axe", "shovel").contains(toolType)) {
                manager.giveTool(target, toolType, overrideTimer);
                player.sendMessage("§aGiven " + toolType + " to §f" + target.getName());
                return true;
            } else if (toolType.equals("multitool")) {
                manager.giveMultiTool(target, overrideTimer);
                player.sendMessage("§aGiven multitool to §f" + target.getName());
                return true;
            } else if (toolType.equals("bucket")) {
                manager.giveBucket(target, overrideTimer);
                player.sendMessage("§aGiven countdown bucket to §f" + target.getName());
                return true;
            } else if (toolType.equals("shardbooster")) {
                manager.giveShardBooster(target, overrideTimer);
                player.sendMessage("§aGiven shard booster to §f" + target.getName());
                return true;
            } else if (toolType.equals("sellaxe")) {
                manager.giveSellAxe(target, overrideTimer);
                // Message handled in manager
                return true;
            } else {
                player.sendMessage(
                        "§cInvalid tool type. Valid: drill, axe, shovel, multitool, bucket, shardbooster, sellaxe");
                return true;
            }
        }

        if (sub.equals("void")) {
            if (!player.hasPermission("falcon.void")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon void create <name>");
                return true;
            }

            String action = args[1].toLowerCase();

            if (action.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /falcon void create <name>");
                    return true;
                }

                String name = args[2];
                try {
                    com.sk89q.worldedit.entity.Player worldEditPlayer = com.sk89q.worldedit.bukkit.BukkitAdapter
                            .adapt(player);
                    com.sk89q.worldedit.LocalSession session = com.sk89q.worldedit.WorldEdit.getInstance()
                            .getSessionManager().get(worldEditPlayer);
                    com.sk89q.worldedit.regions.Region worldEditRegion = session
                            .getSelection(worldEditPlayer.getWorld());

                    if (worldEditRegion == null) {
                        player.sendMessage("§cPlease make a selection with WorldEdit first.");
                        return true;
                    }

                    com.sk89q.worldedit.math.BlockVector3 min = worldEditRegion.getMinimumPoint();
                    com.sk89q.worldedit.math.BlockVector3 max = worldEditRegion.getMaximumPoint();
                    String worldName = player.getWorld().getName();

                    plugin.getVoidManager().addRegion(name, worldName, min.x(), min.y(), min.z(), max.x(),
                            max.y(), max.z());
                    player.sendMessage("§aVoid protection region list '" + name + "' has been created!");

                } catch (com.sk89q.worldedit.IncompleteRegionException e) {
                    player.sendMessage("§cPlease make a complete selection (pos1 and pos2) first.");
                } catch (Exception e) {
                    player.sendMessage("§cError accessing WorldEdit selection: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
        }
        if (sub.equals("respawngear")) {
            if (!player.hasPermission("falcon.respawngear")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon respawngear <setup|delete>");
                return true;
            }

            String action = args[1].toLowerCase();
            if (action.equals("setup")) {
                plugin.getRespawnGearGUI().open(player);
                return true;
            } else if (action.equals("delete")) {
                plugin.getRespawnGearManager().clearItems();
                player.sendMessage("§aRespawn gear items have been deleted.");
                return true;
            } else {
                player.sendMessage("§cUsage: /falcon respawngear <setup|delete>");
                return true;
            }
        }

        if (sub.equals("limiter")) {
            if (!player.hasPermission("falcon.limiter")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /falcon limiter <reload|stats>");
                return true;
            }

            String action = args[1].toLowerCase();

            if (action.equals("reload")) {
                plugin.getLimiterManager().reload();
                player.sendMessage("§aLimiter configuration and tasks have been reloaded.");
            } else if (action.equals("stats")) {
                int radius = plugin.getLimiterConfig().getChunkCheckRadius();
                org.bukkit.Location loc = player.getLocation();

                // Folia requires chunk operations to safely happen on the region thread, NOT
                // async.
                plugin.getSchedulerAdapter().runAtLocation(loc, () -> {
                    org.bukkit.Chunk center = loc.getChunk();
                    org.bukkit.World world = loc.getWorld();

                    int totalEntities = 0;
                    int totalItems = 0;
                    int totalChunks = 0;

                    for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                        for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                            if (world.isChunkLoaded(x, z)) {
                                totalChunks++;
                                for (org.bukkit.entity.Entity e : world.getChunkAt(x, z).getEntities()) {
                                    if (e instanceof org.bukkit.entity.Player)
                                        continue;
                                    if (e instanceof org.bukkit.entity.Item) {
                                        totalItems++;
                                    } else {
                                        totalEntities++;
                                    }
                                }
                            }
                        }
                    }

                    // On Folia, we're now on the region thread, which is perfectly safe to send
                    // messages from.
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&c&m---------------------------------"));
                    player.sendMessage(org.bukkit.ChatColor.RED + " Limiter Stats: " + org.bukkit.ChatColor.WHITE
                            + "Radius " + radius + " Chunks");
                    player.sendMessage("");
                    player.sendMessage(
                            org.bukkit.ChatColor.RED + " Loaded Chunks: " + org.bukkit.ChatColor.WHITE + totalChunks);
                    player.sendMessage(org.bukkit.ChatColor.RED + " Total Entities: " + org.bukkit.ChatColor.WHITE
                            + totalEntities);
                    player.sendMessage(
                            org.bukkit.ChatColor.RED + " Total Items: " + org.bukkit.ChatColor.WHITE + totalItems);
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&c&m---------------------------------"));
                });
            } else {
                player.sendMessage("§cUsage: /falcon limiter <reload|stats>");
            }
            return true;
        }

        if (sub.equals("crate")) {
            return handleCrate(player, args);
        }

        if (sub.equals("crystal")) {
            return handleCrystal(player, args);
        }

        if (sub.equals("anchor")) {
            return handleAnchor(player, args);
        }

        player.sendMessage(
                "§cUnknown subcommand. Use auction, order, rtpqueue, speed, tools, void, respawngear, limiter, crate, crystal, or anchor.");
        return true;
    }

    private boolean handleCrate(Player player, String[] args) {
        if (!player.hasPermission("prism.crates.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /falcon crate <create|edit|get|delete|effects> ...");
            return true;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /falcon crate create <name> <key> [type] [container]");
                return true;
            }
            String type = "NORMAL"; // Default
            if (args.length >= 5) {
                type = args[4];
            }
            String container = "CHEST"; // Default
            if (args.length >= 6) {
                container = args[5];
            }
            return handleCrateCreate(player, args[2], args[3], type, container);
        } else if (sub.equals("edit")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /falcon crate edit <name>");
                return true;
            }
            return handleCrateEdit(player, args[2]);
        } else if (sub.equals("get")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /falcon crate get <name>");
                return true;
            }
            return handleCrateGet(player, args[2]);
        } else if (sub.equals("delete")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /falcon crate delete <name>");
                return true;
            }
            return handleCrateDelete(player, args[2]);
        } else if (sub.equals("effects")) {
            // /falcon crate effects <add|remove|set> <crate> <effect|all>
            if (args.length < 5) {
                player.sendMessage(
                        ChatColor.RED + "Usage: /falcon crate effects <add|remove|set> <crate> <effect|all>");
                return true;
            }
            return handleCrateEffects(player, args[2], args[3], args[4]);
        }

        return true;
    }

    private boolean handleCrateCreate(Player player, String crateName, String keyName, String typeStr,
            String containerStr) {
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

    private boolean handleCrateGet(Player player, String crateName) {
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

    private boolean handleCrateDelete(Player player, String crateName) {
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

    private boolean handleCrateEdit(Player player, String crateName) {
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

    private boolean handleCrateEffects(Player player, String sub, String crateName, String effectName) {
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
            player.sendMessage(ChatColor.RED + "Usage: /falcon crate effects <add|remove|set> <crate> <effect|all>");
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
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays
                    .asList("auction", "order", "rtpqueue", "speed", "tools", "void", "respawngear", "limiter",
                            "crate", "crystal", "anchor")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("respawngear")) {
                return Arrays.asList("setup", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("auction") || args[0].equalsIgnoreCase("order")) {
                // Use async player name cache to prevent TPS drops
                return plugin.getPlayerNameCache().getCompletions(args[1]);
            } else if (args[0].equalsIgnoreCase("rtpqueue")) {
                return Arrays.asList("create", "delete").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("speed")) {
                return Arrays.asList("normal", "1", "2", "3", "4", "5", "1.5").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("tools")) {
                return Arrays.asList("drill", "axe", "shovel", "multitool", "bucket", "shardbooster", "sellaxe")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("void")) {
                return Arrays.asList("create").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("limiter")) {
                return Arrays.asList("reload", "stats").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("crate")) {
                List<String> subcommands = Arrays.asList("create", "edit", "get", "delete", "effects");
                return subcommands.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("crystal")) {
                return Arrays.asList("normal", "0", "1", "2", "3", "4", "5", "6", "10", "20").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("anchor")) {
                return Arrays.asList("normal", "0", "1", "2", "3", "4", "5", "6", "10", "20").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rtpqueue")) {
                if (args[1].equalsIgnoreCase("create")) {
                    return plugin.getRTPQueueManager().getAvailableRegions().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args[1].equalsIgnoreCase("delete")) {
                    return plugin.getRTPQueueManager().getQueueNames().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("tools")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("crate")) {
                if (args[1].equalsIgnoreCase("effects")) {
                    List<String> subs = Arrays.asList("add", "remove", "set");
                    return subs.stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("get")
                        || args[1].equalsIgnoreCase("delete")) {
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
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
                if (args[1].equalsIgnoreCase("create")) {
                    // Suggest crate names for the name slot, though usually it's unique
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
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("tools")) {
            return Arrays.asList("1d", "12h", "30m", "1w");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("crate")) {
            if (args[1].equalsIgnoreCase("effects")) {
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
                            .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (args[1].equalsIgnoreCase("create")) {
                // Suggest keys at args[3]
                return new ArrayList<>(plugin.getKeyAllManager().getValidKeys()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("crate")) {
            if (args[1].equalsIgnoreCase("effects")) {
                List<String> effects = new ArrayList<>();
                if (args[2].equalsIgnoreCase("remove")) {
                    effects.add("all");
                }
                effects.addAll(Arrays.asList("HELIX", "DOUBLE_HELIX", "HALO", "GROUND_RINGS", "VORTEX", "FOUNTAIN",
                        "DISCO", "BEACON", "PULSE", "ORBIT", "ENDER", "TORNADO", "SPHERE", "LAVA_DRIP", "ENCHANT",
                        "FLAME_CROWN"));
                return effects.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[1].equalsIgnoreCase("create")) {
                List<String> types = Arrays.asList("NORMAL", "CAROUSEL");
                return types.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("crate")) {
            if (args[1].equalsIgnoreCase("create")) {
                List<String> containers = new ArrayList<>();
                containers.add("CHEST");
                containers.add("ENDER_CHEST");
                for (Material mat : Material.values()) {
                    if (mat.name().endsWith("SHULKER_BOX")) {
                        containers.add(mat.name());
                    }
                }
                return containers.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[5].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private boolean handleCrystal(Player player, String[] args) {
        if (!player.hasPermission("falcon.crystal")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        if (args.length < 2) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        try {
            String damageArg = args[1].toLowerCase();
            double damage;

            if (damageArg.equals("normal")) {
                damage = 6.0; // Vanilla crystal damage
            } else {
                damage = Double.parseDouble(damageArg);
            }

            if (damage < 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return true;
            }

            plugin.getDamageManager().setCrystalDamage(damage);
            player.sendMessage("§aCrystal damage set to §f" + damage);
            return true;
        } catch (NumberFormatException ignored) {
            // No error messages as requested, just villager no sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }
    }

    private boolean handleAnchor(Player player, String[] args) {
        if (!player.hasPermission("falcon.anchor")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        if (args.length < 2) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }

        try {
            String damageArg = args[1].toLowerCase();
            double damage;

            if (damageArg.equals("normal")) {
                damage = 6.0; // Vanilla anchor damage
            } else {
                damage = Double.parseDouble(damageArg);
            }

            if (damage < 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
                return true;
            }

            plugin.getDamageManager().setAnchorDamage(damage);
            player.sendMessage("§aAnchor damage set to §f" + damage);
            return true;
        } catch (NumberFormatException ignored) {
            // No error messages as requested, just villager no sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0f, 0f);
            return true;
        }
    }
}

