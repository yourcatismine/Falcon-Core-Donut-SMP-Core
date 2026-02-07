package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the duel queue system with live GUI updates.
 */
public class DuelQueueManager implements Listener {

    private final PrismSurvival plugin;
    private final DuelStatsManager statsManager;
    private final DuelArenaManager arenaManager;
    private DuelRequestManager requestManager; // dependencies

    // Players currently in queue (UUID -> queue start time)
    private final Map<UUID, Long> queuedPlayers = new ConcurrentHashMap<>();

    // Players with open queue GUI (UUID -> task for updates)
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> guiUpdateTasks = new ConcurrentHashMap<>();

    // Players searching (UUID -> action bar task)
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> searchTasks = new ConcurrentHashMap<>();

    // GUI title for queue
    public static final String QUEUE_GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ᴅᴜᴇʟ ǫᴜᴇᴜᴇ & ᴄᴏɴꜰɪʀᴍ");

    public DuelQueueManager(PrismSurvival plugin, DuelStatsManager statsManager, DuelArenaManager arenaManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.arenaManager = arenaManager;

        // Register as listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setRequestManager(DuelRequestManager requestManager) {
        this.requestManager = requestManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(QUEUE_GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        // Only process clicks in the GUI (not player inventory)
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // Play click sound for non-empty slot
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.2f);
            } catch (Exception ignored) {
            }
        }

        int slot = event.getRawSlot();

        if (slot == 10) {
            // Cancel - Leave queue if in it, close GUI
            leaveQueue(player);
            player.closeInventory();
        } else if (slot == 16) {
            // Toggle queue status
            if (isInQueue(player.getUniqueId())) {
                // Leave queue
                leaveQueue(player);
                // Update GUI immediately - use the inventory from the event view
                Inventory topInv = event.getView().getTopInventory();
                if (topInv.getSize() >= 27) {
                    updateQueueGUI(topInv, player);
                }
            } else {
                // Join queue and close GUI
                joinQueue(player);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(QUEUE_GUI_TITLE)) {
            return;
        }

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            cancelGuiUpdates(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Remove from queue if present
        if (queuedPlayers.remove(uuid) != null) {
            cancelSearchTask(uuid);
        }

        // Cancel any GUI update tasks
        cancelGuiUpdates(uuid);
    }

    /**
     * Opens the queue GUI for a player and starts live updates.
     */
    public void openQueueGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, QUEUE_GUI_TITLE);

        // Initial population
        updateQueueGUI(gui, player);

        player.openInventory(gui);

        // Start live update task (every 20 ticks = 1 second)
        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, () -> {
            // Check if player still has the inventory open
            if (player.getOpenInventory().getTitle().equals(QUEUE_GUI_TITLE)) {
                updateQueueGUI(player.getOpenInventory().getTopInventory(), player);
            } else {
                // Player closed inventory, cancel task
                cancelGuiUpdates(player.getUniqueId());
            }
        }, 20L, 20L);

        guiUpdateTasks.put(player.getUniqueId(), task);
    }

    /**
     * Updates the queue GUI with live data.
     */
    private void updateQueueGUI(Inventory gui, Player player) {
        UUID uuid = player.getUniqueId();
        int wins = statsManager.getWins(uuid);
        int losses = statsManager.getLosses(uuid);
        int streak = statsManager.getStreak(uuid);

        int queuedCount = queuedPlayers.size();
        String estimatedWait = calculateEstimatedWait(queuedCount);
        int ping = getPlayerPing(player);

        // Slot 10 - Red Glass - Cancel
        ItemStack cancelItem = createItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ",
                "&fClick to cancel");
        gui.setItem(10, cancelItem);

        // Slot 12 - Clock - Await Time (LIVE)
        ItemStack clockItem = createItem(Material.CLOCK, "&aᴡᴀɪᴛ ᴛɪᴍᴇ",
                "&7Estimated Wait: &f" + estimatedWait,
                "&7Currently queued: &f" + queuedCount);
        gui.setItem(12, clockItem);

        // Slot 13 - Gray Dye - Statistics
        ItemStack statsItem = createItem(Material.GRAY_DYE, "&aѕᴛᴀᴛɪѕᴛɪᴄѕ",
                "&7Wins: &f" + wins,
                "&7Losses: &f" + losses,
                "&7Streak: &f" + streak);
        gui.setItem(13, statsItem);

        // Slot 14 - Feather - Region (LIVE ping)
        ItemStack regionItem = createItem(Material.FEATHER, "&aʀᴇɢɪᴏɴ",
                "&7Europe (&5" + ping + "ms&7)");
        gui.setItem(14, regionItem);

        // Slot 16 - Green Glass - Confirm
        boolean isInQueue = queuedPlayers.containsKey(uuid);
        ItemStack confirmItem;
        if (isInQueue) {
            // Already in queue - show searching status
            long waitTime = (System.currentTimeMillis() - queuedPlayers.get(uuid)) / 1000;
            confirmItem = createItem(Material.LIME_STAINED_GLASS_PANE, "&aѕᴇᴀʀᴄʜɪɴɢ...",
                    "&7Searching for &f" + waitTime + "s",
                    "&cClick to leave queue");
        } else {
            confirmItem = createItem(Material.GREEN_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ",
                    "&fClick to start searching for match");
        }
        gui.setItem(16, confirmItem);
    }

    /**
     * Calculates estimated wait time based on queue size.
     */
    private String calculateEstimatedWait(int queuedCount) {
        if (queuedCount == 0) {
            return "Instant";
        } else if (queuedCount == 1) {
            return "~30s";
        } else if (queuedCount <= 3) {
            return "~1min";
        } else if (queuedCount <= 5) {
            return "~2min";
        } else {
            return "~" + (queuedCount / 2) + "min";
        }
    }

    /**
     * Gets the player's ping in milliseconds.
     */
    private int getPlayerPing(Player player) {
        try {
            // Use Spigot API for ping
            return player.getPing();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Adds a player to the queue.
     */
    public void joinQueue(Player player) {
        // Cancel any pending requests first
        if (requestManager != null && requestManager.hasPendingRequest(player)) {
            requestManager.cancelRequest(player);
        }

        long startTime = System.currentTimeMillis();
        queuedPlayers.put(player.getUniqueId(), startTime);
        player.sendMessage(ChatColor.GREEN + "You are now searching for a match...");

        // Start repeating action bar task
        org.bukkit.scheduler.BukkitTask searchTask = plugin.getSchedulerAdapter().runEntityTaskTimer(player, () -> {
            if (!queuedPlayers.containsKey(player.getUniqueId())) {
                // Player left queue, cancel task
                cancelSearchTask(player.getUniqueId());
                return;
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            String actionBarMsg;

            if (elapsed >= 30) {
                // After 30 seconds, show unable to find message then remove from queue
                String failMsg = ChatColor.translateAlternateColorCodes('&', "&cUnable to find players to match");
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(failMsg));

                // Play villager no sound
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }

                // Remove from queue and stop task
                leaveQueue(player);
                return;
            } else {
                // Show searching message with estimated time
                String estimatedTime = calculateEstimatedWait(queuedPlayers.size());
                actionBarMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7Searching for a Casual Duel... Estimated Time:&5 " + estimatedTime);
            }

            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(actionBarMsg));
        }, 0L, 40L); // Every 2 seconds (40 ticks)

        searchTasks.put(player.getUniqueId(), searchTask);

        // Check for match
        tryMatchPlayers();
    }

    /**
     * Removes a player from the queue.
     */
    public void leaveQueue(Player player) {
        if (queuedPlayers.remove(player.getUniqueId()) != null) {
            cancelSearchTask(player.getUniqueId());
        }
    }

    /**
     * Cancels the search action bar task for a player.
     */
    private void cancelSearchTask(UUID uuid) {
        org.bukkit.scheduler.BukkitTask task = searchTasks.remove(uuid);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Cancels GUI update task for a player.
     */
    public void cancelGuiUpdates(UUID uuid) {
        org.bukkit.scheduler.BukkitTask task = guiUpdateTasks.remove(uuid);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Checks if a player is in queue.
     */
    public boolean isInQueue(UUID uuid) {
        return queuedPlayers.containsKey(uuid);
    }

    /**
     * Gets the current queue count.
     */
    public int getQueueCount() {
        return queuedPlayers.size();
    }

    /**
     * Attempts to match two players from the queue.
     */
    private void tryMatchPlayers() {
        if (queuedPlayers.size() >= 2) {
            // Get two players from queue
            Iterator<UUID> iterator = queuedPlayers.keySet().iterator();
            UUID player1Uuid = iterator.next();
            UUID player2Uuid = iterator.next();

            Player player1 = Bukkit.getPlayer(player1Uuid);
            Player player2 = Bukkit.getPlayer(player2Uuid);

            if (player1 != null && player2 != null && player1.isOnline() && player2.isOnline()) {
                // Try to start the duel with arena manager
                boolean started = arenaManager.startDuel(player1, player2);

                if (started) {
                    // Successfully started, remove from queue
                    queuedPlayers.remove(player1Uuid);
                    queuedPlayers.remove(player2Uuid);

                    // Stop search tasks immediately and clear action bar
                    cancelSearchTask(player1Uuid);
                    cancelSearchTask(player2Uuid);

                    // Close their GUIs if open
                    cancelGuiUpdates(player1Uuid);
                    cancelGuiUpdates(player2Uuid);

                    // Close inventories
                    player1.closeInventory();
                    player2.closeInventory();
                } else {
                    // No available arena, keep them in queue
                    plugin.getLogger().info(
                            "No available arena for queue match: " + player1.getName() + " vs " + player2.getName());
                }
            }
        }
    }

    /**
     * Called when player closes the queue GUI.
     */
    public void onGuiClose(Player player) {
        cancelGuiUpdates(player.getUniqueId());
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

}
