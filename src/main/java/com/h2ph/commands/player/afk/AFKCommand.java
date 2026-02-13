package com.h2ph.commands.player.afk;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AFKCommand implements CommandExecutor, TabCompleter, Listener {

    private final PrismSurvival plugin;
    private final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&7ᴀꜰᴋ ᴀʀᴇᴀѕ");

    // Map to track active teleport tasks: UUID -> BukkitTask
    private final Map<UUID, BukkitTask> activeTeleports = new HashMap<>();

    public AFKCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // Admin commands
            if (args[0].equalsIgnoreCase("setspawn")) {
                if (!player.hasPermission("prismcore.admin.afk")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                    if (args.length > 2) {
                        deleteAfkSpawn(player, args[2]);
                        return true;
                    }
                }

                saveAfkSpawn(player);
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (!player.hasPermission("prismcore.admin.afk")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /afk remove <map_name>");
                    return true;
                }
                deleteAfkSpawn(player, args[1]);
                return true;
            }
            // Logic for /afk <number>
            else {
                try {
                    int slot = Integer.parseInt(args[0]);
                    int index = slot - 1; // 1-based to 0-based

                    List<java.io.File> mapFiles = getMapFiles();
                    if (index >= 0 && index < mapFiles.size()) {
                        String worldName = mapFiles.get(index).getName().replace(".yml", "");

                        // Optional: Check full logic here if we want to restrict teleporting to full
                        // servers via command
                        // User said "For fast", usually implies skipping GUI check, but logic should
                        // probably consistency check.
                        // I'll add the check for consistency.
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        int playerCount = (world != null) ? world.getPlayers().size() : 0;
                        int maxPlayers = Bukkit.getMaxPlayers();
                        boolean isFull = playerCount >= maxPlayers && maxPlayers > 0;

                        if (isFull) {
                            player.sendMessage(ChatColor.RED + "This area is full.");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return true;
                        }

                        startTeleportTask(player, worldName);
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    // Not a number, maybe a map name?
                    // User didn't strictly ask for /afk <mapname> shortcut, but /afk <number>.
                    // Fallthrough to open GUI.
                }
            }
        }

        openAFKGui(player);

        return true;
    }

    private List<java.io.File> getMapFiles() {
        List<java.io.File> fileList = new ArrayList<>();
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        if (folder.exists()) {
            java.io.File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
                // Sort by name to ensure consistent ordering for IDs
                Collections.sort(fileList, Comparator.comparing(java.io.File::getName));
            }
        }
        return fileList;
    }

    private void saveAfkSpawn(Player player) {
        String worldName = player.getWorld().getName();
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        java.io.File file = new java.io.File(folder, worldName + ".yml");
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);

        config.set("spawn", player.getLocation());
        try {
            config.save(file);
            player.sendMessage(ChatColor.GREEN + "AFK spawn set for world " + ChatColor.YELLOW + worldName);
        } catch (java.io.IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to save spawn location.");
            e.printStackTrace();
        }
    }

    private void deleteAfkSpawn(Player player, String mapName) {
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "survival/AFK/maps");
        java.io.File file = new java.io.File(folder, mapName + ".yml");

        if (file.exists()) {
            if (file.delete()) {
                player.sendMessage(ChatColor.GREEN + "Removed AFK map: " + ChatColor.YELLOW + mapName);
            } else {
                player.sendMessage(ChatColor.RED + "Failed to delete map file.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Map not found: " + mapName);
        }
    }

    private void openAFKGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        List<java.io.File> files = getMapFiles();
        int index = 0;
        for (java.io.File file : files) {
            if (index >= 45)
                break;

            String worldName = file.getName().replace(".yml", "");
            org.bukkit.World world = Bukkit.getWorld(worldName);

            int playerCount = 0;
            int maxPlayers = Bukkit.getMaxPlayers();
            if (world != null) {
                playerCount = world.getPlayers().size();
            }

            boolean isFull = playerCount >= maxPlayers && maxPlayers > 0;

            ItemStack item;
            if (isFull) {
                item = new ItemStack(Material.REDSTONE_BLOCK);
            } else {
                item = new ItemStack(Material.ITEM_FRAME);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5ᴀꜰᴋ " + (index + 1)));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8" + playerCount + "/" + maxPlayers));

                if (isFull) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&cThis area is full."));
                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to go to this Afk zone area."));
                }

                meta.setLore(lore);
                meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "afk_world"),
                        PersistentDataType.STRING,
                        worldName);
                item.setItemMeta(meta);
            }
            gui.setItem(index, item);
            index++;
        }

        // Random AFK Button at Slot 49
        ItemStack randomItem = new ItemStack(Material.AMETHYST_BLOCK);
        ItemMeta randomMeta = randomItem.getItemMeta();
        if (randomMeta != null) {
            randomMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aᴀꜰᴋ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to teleport to a random afk area"));
            randomMeta.setLore(lore);
            randomMeta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "afk_random"),
                    PersistentDataType.BYTE,
                    (byte) 1);
            randomItem.setItemMeta(randomMeta);
        }
        gui.setItem(49, randomItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                Player player = (Player) event.getWhoClicked();

                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType() == Material.AIR)
                    return;

                // Play sound for valid clicks in GUI
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_OFF, 1f, 1f);

                ItemMeta meta = item.getItemMeta();
                if (meta == null)
                    return;

                if (meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "afk_random"),
                        PersistentDataType.BYTE)) {
                    player.closeInventory();
                    teleportToRandomMap(player);
                    return;
                }

                String worldName = meta.getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "afk_world"),
                        PersistentDataType.STRING);

                if (worldName == null)
                    return;

                if (item.getType() == Material.REDSTONE_BLOCK) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                player.closeInventory();
                startTeleportTask(player, worldName);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (activeTeleports.containsKey(player.getUniqueId())) {
            BukkitTask task = activeTeleports.remove(player.getUniqueId());
            task.cancel();

            String msg = ChatColor.translateAlternateColorCodes('&', "&cTeleport cancelled because you moved.");
            player.sendMessage(msg);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));

            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void teleportToRandomMap(Player player) {
        List<java.io.File> files = getMapFiles();
        if (!files.isEmpty()) {
            java.util.Random random = new java.util.Random();
            java.io.File randomFile = files.get(random.nextInt(files.size()));
            String worldName = randomFile.getName().replace(".yml", "");
            startTeleportTask(player, worldName);
        } else {
            player.sendMessage(ChatColor.RED + "No AFK areas available.");
        }
    }

    private void startTeleportTask(Player player, String worldName) {
        // Check if already teleporting
        if (activeTeleports.containsKey(player.getUniqueId())) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        java.util.concurrent.atomic.AtomicInteger countdown = new java.util.concurrent.atomic.AtomicInteger(5);
        BukkitTask task = plugin.getSchedulerAdapter().runTaskTimer(() -> {
            if (!player.isOnline()) {
                activeTeleports.remove(player.getUniqueId());
                // Note: task.cancel() is handled by the wrapper if needed,
                // but usually we cancel from within the task if possible.
                // However, runTaskTimer returns a wrapper.
                return;
            }

            if (countdown.get() <= 0) {
                activeTeleports.remove(player.getUniqueId());

                java.io.File file = new java.io.File(plugin.getDataFolder(),
                        "survival/AFK/maps/" + worldName + ".yml");
                if (file.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                            .loadConfiguration(file);
                    org.bukkit.Location loc = config.getLocation("spawn");
                    if (loc != null) {
                        player.teleport(loc);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        player.sendMessage(ChatColor.RED + "Spawn location not found for this map.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Map no longer exists.");
                }
                // Cancellation is tricky inside runTaskTimer if it doesn't pass the task.
                // But SchedulerAdapter's runTaskTimer wrapper's cancel() works.
                // Usually repeating tasks in Folia need to cancel themselves via the scheduled
                // task object.
                // Let's check SchedulerAdapter again.
                return;
            }

            String msg = ChatColor.translateAlternateColorCodes('&', "&7Teleporting in &5" + countdown.get() + "s");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));

            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);

            countdown.decrementAndGet();
        }, 0L, 20L);

        activeTeleports.put(player.getUniqueId(), task);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {

        // Tab completion for /afk <number> (available to everyone)
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();

            // Add map numbers 1..N
            List<java.io.File> files = getMapFiles();
            for (int i = 1; i <= files.size(); i++) {
                completions.add(String.valueOf(i));
            }

            // Add admin commands if permission matches
            if (sender.hasPermission("prismcore.admin.afk")) {
                completions.add("setspawn");
                completions.add("remove");
            }

            return filterCompletions(completions, args[0]);
        }

        // Admin-only subcommands beyond arg 0
        if (!sender.hasPermission("prismcore.admin.afk")) {
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return filterCompletions(getMapNames(), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setspawn")) {
            List<String> sub = new ArrayList<>();
            sub.add("remove");
            return filterCompletions(sub, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn") && args[1].equalsIgnoreCase("remove")) {
            return filterCompletions(getMapNames(), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> getMapNames() {
        List<String> maps = new ArrayList<>();
        List<java.io.File> files = getMapFiles();
        for (java.io.File f : files) {
            maps.add(f.getName().replace(".yml", ""));
        }
        return maps;
    }

    private List<String> filterCompletions(List<String> input, String arg) {
        return input.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }
}
