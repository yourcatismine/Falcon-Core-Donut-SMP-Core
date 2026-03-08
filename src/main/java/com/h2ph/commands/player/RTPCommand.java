package com.h2ph.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class RTPCommand implements org.bukkit.command.TabExecutor {

    public static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            com.h2ph.PrismSurvival main = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.h2ph.PrismSurvival.class);
            java.io.File regionFile = new java.io.File(main.getDataFolder(), "rtp/" + args[0]);
            java.io.File configFile = new java.io.File(regionFile, "config.yml");

            if (regionFile.exists() && regionFile.isDirectory() && configFile.exists()) {
                com.h2ph.rtp.RTPManager.teleport(player, args[0], "overworld");
            } else {
                openRTPGUI(player);
            }
        } else {
            openRTPGUI(player);
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            java.util.List<String> regions = new java.util.ArrayList<>();
            com.h2ph.PrismSurvival main = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.h2ph.PrismSurvival.class);
            java.io.File rtpFolder = new java.io.File(main.getDataFolder(), "rtp");

            if (rtpFolder.exists() && rtpFolder.isDirectory()) {
                java.io.File[] files = rtpFolder.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isDirectory()) {
                            java.io.File configFile = new java.io.File(file, "config.yml");
                            if (configFile.exists()) {
                                regions.add(file.getName());
                            }
                        }
                    }
                }
            }
            String input = args[0].toLowerCase();
            java.util.List<String> filtered = new java.util.ArrayList<>();
            for (String region : regions) {
                if (region.toLowerCase().startsWith(input)) {
                    filtered.add(region);
                }
            }
            return filtered;
        }
        return java.util.Collections.emptyList();
    }

    public static void openRTPGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        updateItems(gui, player);

        player.openInventory(gui);
    }

    public static void openOverworldGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        updateOverworldItems(gui, player);

        player.openInventory(gui);
    }

    public static void updateOverworldItems(Inventory gui, Player player) {
        String count = formatCount(getPlayerCount("overworld"));
        String ping = getPing(player);

        gui.setItem(10, createItem(org.bukkit.Material.GRASS_BLOCK, "&aᴏᴠᴇʀᴡᴏʀʟᴅ",
                "&fClick to randomly teleport",
                "",
                "&7Players (&6" + count + "&7)",
                "&7Europe (&6" + ping + "ms&7)"));
    }

    public static void updateItems(Inventory gui, Player player) {
        String overworldCount = formatCount(getPlayerCount("overworld"));
        String netherCount = formatCount(getPlayerCount("nether"));
        String endCount = formatCount(getPlayerCount("end"));
        String ping = getPing(player);

        gui.setItem(11, createItem(org.bukkit.Material.GRASS_BLOCK, "&aᴏᴠᴇʀᴡᴏʀʟᴅ",
                "&fClick to select a region",
                "",
                "&7Players (&6" + overworldCount + "&7)"));

        gui.setItem(13, createItem(org.bukkit.Material.NETHERRACK, "&aɴᴇᴛʜᴇʀ",
                "&fClick to randomly teleport",
                "",
                "&7Players (&6" + netherCount + "&7)",
                "&7Europe (&6" + ping + "ms&7)"));

        gui.setItem(15, createItem(org.bukkit.Material.END_STONE, "&aᴇɴᴅ",
                "&fClick to randomly teleport",
                "",
                "&7Players (&6" + endCount + "&7)",
                "&7Europe (&#A9833D" + ping + "ms&7)"));
    }

    private static int getPlayerCount(String type) {
        com.h2ph.PrismSurvival main = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.h2ph.PrismSurvival.class);
        if (main.getRTPConfig() == null) {
            return 0;
        }

        String worldName = main.getRTPConfig().getString("worlds." + type + ".world");
        if (worldName == null) {
            return 0;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return 0;
        }

        return world.getPlayers().size();
    }

    private static String getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return String.valueOf(entityPlayer.getClass().getField("ping").getInt(entityPlayer));
        } catch (Exception e) {
            try {
                return String.valueOf(player.getPing());
            } catch (NoSuchMethodError ex) {
                return "0";
            }
        }
    }

    private static String formatCount(int count) {
        if (count < 1000)
            return String.valueOf(count);
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.2f%c", count / Math.pow(1000, exp), "kMGTPE".charAt(exp - 1));
    }

    private static org.bukkit.inventory.ItemStack createItem(org.bukkit.Material material, String name,
            String... lore) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            java.util.List<String> loreList = new java.util.ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
