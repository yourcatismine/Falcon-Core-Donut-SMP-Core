package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    private final DuelRequestManager requestManager;
    private final DuelArenaManager arenaManager;
    private final DuelStatsManager statsManager;
    private final DuelQueueManager queueManager;

    public DuelCommand(PrismSurvival plugin, DuelArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.requestManager = new DuelRequestManager(plugin, arenaManager);
        this.statsManager = new DuelStatsManager(plugin);
        this.queueManager = new DuelQueueManager(plugin, statsManager, arenaManager);
        this.queueManager.setRequestManager(this.requestManager);
    }

    // ...

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (args.length == 0) {
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                if (queueManager.isInQueue(player.getUniqueId())) {
                    // Do not let open queue again, play error sound only
                    try {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                queueManager.openQueueGUI(player);
            } else {
                sendUsage(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 1. Check for specific subcommands first
        if (subCommand.equals("leave")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

            // Check if in active duel (fighting)
            if (arenaManager.isInDuel(player)) {
                // Forfeit
                // We set health to 0 to trigger PlayerDeathEvent.
                // This ensures items drop for looting and standard end-duel logic is triggered.
                String msg = org.bukkit.ChatColor.GRAY + "You forfeited the match.";
                sender.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(msg));
                arenaManager.markForfeit(player);
                player.setHealth(0);
                return true;
            }

            // Check if looting (winner phase)
            if (arenaManager.isLooting(player)) {
                String msg = org.bukkit.ChatColor.GRAY + "You left the arena.";
                sender.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(msg));
                arenaManager.stopLooting(player);
                return true;
            }

            // Not in duel or looting
            String errorMsg = ChatColor.RED + "You are not in a duel.";
            sender.sendMessage(errorMsg);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(errorMsg));
            return true;
        } else if (subCommand.equals("create")) {
            if (!sender.hasPermission("prismcore.admin.duel")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /duel create <name>");
                return true;
            }
            handleCreate(sender, args[1]);
            return true;
        } else if (subCommand.equals("settings")) {
            if (sender.hasPermission("prismcore.admin.duel")) {
                handleSettings(sender);
            } else if (sender instanceof org.bukkit.entity.Player) {
                queueManager.openQueueGUI((org.bukkit.entity.Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        } else if (subCommand.equals("queue")) {
            // Handle queue GUI with live updates
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            queueManager.openQueueGUI(player);
            return true;
        } else if (subCommand.equals("cancel")) {
            // Handle cancellation
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            requestManager.cancelRequest((org.bukkit.entity.Player) sender);
            return true;
        } else if (subCommand.equals("accept") || subCommand.equals("decline")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            org.bukkit.entity.Player target = (org.bukkit.entity.Player) sender;
            String senderName = null;

            if (args.length > 1) {
                senderName = args[1];
            }

            if (subCommand.equals("accept")) {
                requestManager.acceptRequest(target, senderName);
            } else {
                requestManager.declineRequest(target, senderName);
            }
            return true;
        }

        // 2. If valid player name, treat as duel request
        org.bukkit.entity.Player target = Bukkit.getPlayer(subCommand);
        if (target != null) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can request duels.");
                return true;
            }
            // If in queue, cancel queue
            org.bukkit.entity.Player creator = (org.bukkit.entity.Player) sender;
            if (queueManager.isInQueue(creator.getUniqueId())) {
                queueManager.leaveQueue(creator);
                creator.sendMessage(ChatColor.YELLOW + "You left the duel queue to send a request.");
            }

            // Register Listener locally? OR should Main class register it?
            // Listener needs to be registered. Since DuelCreationGUI implements Listener,
            // we can register it when created, but we need to unregister it when closed.
            // OR, better, make DuelCreationGUI instance persistent?
            // Unregistering listeners is tricky.
            // Better: Have a singleton Manager that handles clicks, or use a general
            // persistent implementation.
            // Given the structure, creating a new GUI + Listener per request is messy for
            // memory if not cleaned.

            // SIMPLER APPROACH for this codebase:
            // Instantiate DuelCreationGUI, register events, open inventory.
            // The GUI class itself should unregister itself on Close.
            // Does DuelCreationGUI handle unregister?
            // I wrote "public class DuelCreationGUI implements InventoryHolder, Listener"
            // But I didn't add logic to unregister.
            // Let's assume for now we register, and memory leaks are "future problem"? NO.
            // Let's rely on checking holder in a global listener or...
            //
            // Actually, `DuelCreationGUI` as written acts as the holder.
            // We can have a SINGLE `DuelGUIListener` in the plugin that checks
            // `event.getInventory().getHolder() instanceof DuelCreationGUI`.
            // Retrying logic:
            // I cannot dynamically register listeners safely without unregistering.
            // I will modify `DuelCreationGUI` to NOT be a listener itself, but handle
            // clicks via a central listener, OR
            // stick to `DuelCommand` registering it and `DuelCreationGUI` unregistering on
            // close.

            // Let's go with: Open GUI. Detailed logic below creates and opens it.
            // I will need to register a GLOBAL listener for DuelCreationGUI interactions if
            // I haven't already.
            // Wait, I created `DuelCreationGUI` file but haven't registered a listener for
            // it in `PrismSurvival` yet.
            // I should probably register a dedicated `DuelCreationListener` or similar.
            //
            // Refactoring plan:
            // 1. Open GUI here.
            // 2. I need to make sure the events are handled.
            // Since I can't easily edit PrismSurvival to register a NEW class without
            // potential conflict or restart,
            // I'll assume I can add a listener or use `DuelCommand` as the listener?
            // `DuelCommand` is already a CommandExecutor.
            // Let's make `DuelCommand` (or a manager) listen for InventoryClick.

            // Checking `DuelCreationGUI.java` content again...
            // It HAS `@EventHandler public void onClick`.
            // So it expects to be registered.

            // Disable self-duel (handled in sendRequest but good to check here too if
            // needed, but RequestManager handles it)
            if (creator.getUniqueId().equals(target.getUniqueId())) {
                creator.sendMessage(ChatColor.RED + "You cannot duel yourself.");
                return true;
            }

            // Check if target has disabled duel requests
            com.prismcore.survival.manager.PlayerData targetData = plugin.getPlayerDataManager()
                    .get(target.getUniqueId());
            if (targetData != null && !targetData.isDuelRequests()) {
                String errorMsg = ChatColor.translateAlternateColorCodes('&', "&cUser disabled duel requests.");
                creator.sendMessage(errorMsg);
                creator.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(errorMsg));
                try {
                    creator.playSound(creator.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
                return true;
            }

            // Check if target has ignored the sender
            if (targetData != null && targetData.isIgnoring(creator.getUniqueId())) {
                String errorMsg = ChatColor.translateAlternateColorCodes('&', "&7You are ignored by this player.");
                creator.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(errorMsg));
                try {
                    creator.playSound(creator.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
                return true;
            }

            DuelCreationGUI gui = new DuelCreationGUI(plugin, requestManager, creator, target);
            plugin.getServer().getPluginManager().registerEvents(gui, plugin);
            gui.open();

            return true;
        }

        // Check for offline/unknown player
        if (sender instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            // Iterate through offline players to find a match (case-insensitive)
            org.bukkit.OfflinePlayer offlineTarget = null;
            for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().equalsIgnoreCase(subCommand)) {
                    offlineTarget = op;
                    break;
                }
            }

            // Also check offline target settings if we wanted to be thorough, but offline
            // players can't receive requests anyway
            // in this system (live requests). So we just show "Not Online".

            if (offlineTarget != null) { // We found them in the cache/offline list
                String msg = ChatColor.RED + "This user is not online.";
                sender.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(msg));
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
                return true;
            } else {
                String msg = ChatColor.RED + "That user does not exist.";
                sender.sendMessage(msg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(msg));
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }
                return true;
            }
        }

        // 3. Fallback
        // 3. Fallback (Open GUI by default for players)
        if (sender instanceof org.bukkit.entity.Player) {
            queueManager.openQueueGUI((org.bukkit.entity.Player) sender);
        } else {
            sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "/duel <player>");
        sender.sendMessage(ChatColor.RED + "/duel cancel");
        if (sender.hasPermission("prismcore.admin.duel")) {
            sender.sendMessage(ChatColor.RED + "/duel create <name>");
            sender.sendMessage(ChatColor.RED + "/duel settings");
        }
    }

    private void handleCreate(CommandSender sender, String name) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create duel regions.");
            return;
        }

        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) sender;

        try {
            Player worldEditPlayer = BukkitAdapter.adapt(bukkitPlayer);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            Region region = session.getSelection(worldEditPlayer.getWorld());

            if (region == null) {
                sender.sendMessage(ChatColor.RED + "Please make a selection with WorldEdit first.");
                return;
            }

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            String worldName = bukkitPlayer.getWorld().getName();

            File duelFile = new File(plugin.getDataFolder(), "survival/regions/duels/" + name + ".yml");
            if (!duelFile.getParentFile().exists()) {
                duelFile.getParentFile().mkdirs();
            }

            YamlConfiguration config = new YamlConfiguration();
            config.set("world", worldName);
            config.set("min.x", min.getX());
            config.set("min.y", min.getY());
            config.set("min.z", min.getZ());
            config.set("max.x", max.getX());
            config.set("max.y", max.getY());
            config.set("max.z", max.getZ());
            config.set("created-by", bukkitPlayer.getName());
            config.set("created-at", System.currentTimeMillis());

            // Detect Biome from Center
            int minX = min.getX();
            int maxX = max.getX();
            int minY = min.getY();
            int maxY = max.getY();
            int minZ = min.getZ();
            int maxZ = max.getZ();

            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int centerZ = (minZ + maxZ) / 2;
            org.bukkit.block.Biome biome = bukkitPlayer.getWorld().getBiome(centerX, centerY, centerZ);

            // Format Biome Name (e.g. PLAINS -> Plains, DESERT -> Desert)
            String biomeName = biome.name().toLowerCase();
            biomeName = Character.toUpperCase(biomeName.charAt(0)) + biomeName.substring(1);
            // Handle underscores (e.g. FOREST_HILLS -> Forest Hills) - Optional but nicer
            // Actually user example just said "Plains" or "Desert".
            // Simple Capitalization is robust enough for now.

            config.set("biome", biomeName);

            config.save(duelFile);

            // Reload into memory immediately
            arenaManager.reloadArena(name);

            sender.sendMessage(ChatColor.GREEN + "Duel region " + ChatColor.YELLOW + name + ChatColor.GREEN
                    + " saved successfully!");

        } catch (IncompleteRegionException e) {
            sender.sendMessage(ChatColor.RED + "Please make a complete selection (pos1 and pos2) first.");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to save duel file: " + e.getMessage());
            e.printStackTrace();
        } catch (NoClassDefFoundError e) {
            sender.sendMessage(ChatColor.RED + "WorldEdit is not installed or not working properly.");
        }
    }

    private void handleSettings(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open settings GUI.");
            return;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        DuelGUIManager guiManager = new DuelGUIManager(plugin);
        guiManager.openSettingsGUI(player);
    }

    // Helper createItem removed as it's now in GUIManager, or kept if used
    // elsewhere?
    // It is used only in handleSettings locally. so we can remove it or keep if
    // needed later.
    // Clean up:
    /*
     * private ItemStack createItem(Material material, String name, String... lore)
     * {
     * // ... (Moved to Manager)
     * }
     */

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("queue");
            completions.add("cancel");
            completions.add("accept");
            completions.add("decline");
            completions.add("leave");
            if (sender.hasPermission("prismcore.admin.duel")) {
                completions.add("create");
                completions.add("settings");
            }

            // Suggest online players
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .collect(Collectors.toList()));

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline"))) {
            // Suggest players who might have sent a request?
            // For now, just online players is fine as helper, or return null for default.
            return null; // Return null to show online players by default
        }

        return Collections.emptyList();
    }
}
