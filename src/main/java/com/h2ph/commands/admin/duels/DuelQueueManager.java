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
    private DuelRequestManager requestManager;

    private final Map<UUID, Long> queuedPlayers = new ConcurrentHashMap<>();

    private final Map<UUID, org.bukkit.scheduler.BukkitTask> guiUpdateTasks = new ConcurrentHashMap<>();

    private final Map<UUID, org.bukkit.scheduler.BukkitTask> searchTasks = new ConcurrentHashMap<>();

    public static final String QUEUE_GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8ᴅᴜᴇʟ ǫᴜᴇᴜᴇ & ᴄᴏɴꜰɪʀᴍ");

    public DuelQueueManager(PrismSurvival plugin, DuelStatsManager statsManager, DuelArenaManager arenaManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.arenaManager = arenaManager;

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

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.2f);
            } catch (Exception ignored) {
            }
        }

        int slot = event.getRawSlot();

        if (slot == 10) {
            leaveQueue(player);
            player.closeInventory();
        } else if (slot == 16) {
            if (isInQueue(player.getUniqueId())) {
                leaveQueue(player);
                Inventory topInv = event.getView().getTopInventory();
                if (topInv.getSize() >= 27) {
                    updateQueueGUI(topInv, player);
                }
            } else {
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

        if (queuedPlayers.remove(uuid) != null) {
            cancelSearchTask(uuid);
        }

        cancelGuiUpdates(uuid);
    }

    /**
     * Opens the queue GUI for a player and starts live updates.
     */
    public void openQueueGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, QUEUE_GUI_TITLE);

        updateQueueGUI(gui, player);

        player.openInventory(gui);

        org.bukkit.scheduler.BukkitTask task = plugin.getSchedulerAdapter().runEntityTaskTimer(player, () -> {
            if (player.getOpenInventory().getTitle().equals(QUEUE_GUI_TITLE)) {
                updateQueueGUI(player.getOpenInventory().getTopInventory(), player);
            } else {
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

        ItemStack cancelItem = createItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ",
                "&fClick to cancel");
        gui.setItem(10, cancelItem);

        ItemStack clockItem = createItem(Material.CLOCK, "&aᴡᴀɪᴛ ᴛɪᴍᴇ",
                "&7Estimated Wait: &f" + estimatedWait,
                "&7Currently queued: &f" + queuedCount);
        gui.setItem(12, clockItem);

        ItemStack statsItem = createItem(Material.GRAY_DYE, "&aѕᴛᴀᴛɪѕᴛɪᴄѕ",
                "&7Wins: &f" + wins,
                "&7Losses: &f" + losses,
                "&7Streak: &f" + streak);
        gui.setItem(13, statsItem);

        ItemStack regionItem = createItem(Material.FEATHER, "&aʀᴇɢɪᴏɴ",
                "&7Europe (&5" + ping + "ms&7)");
        gui.setItem(14, regionItem);

        boolean isInQueue = queuedPlayers.containsKey(uuid);
        ItemStack confirmItem;
        if (isInQueue) {
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
            return player.getPing();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Adds a player to the queue.
     */
    public void joinQueue(Player player) {
        if (requestManager != null && requestManager.hasPendingRequest(player)) {
            requestManager.cancelRequest(player);
        }

        long startTime = System.currentTimeMillis();
        queuedPlayers.put(player.getUniqueId(), startTime);
        player.sendMessage(ChatColor.GREEN + "You are now searching for a match...");

        org.bukkit.scheduler.BukkitTask searchTask = plugin.getSchedulerAdapter().runEntityTaskTimer(player, () -> {
            if (!queuedPlayers.containsKey(player.getUniqueId())) {
                cancelSearchTask(player.getUniqueId());
                return;
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            String actionBarMsg;

            if (elapsed >= 30) {
                String failMsg = ChatColor.translateAlternateColorCodes('&', "&cUnable to find players to match");
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(failMsg));

                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Exception ignored) {
                }

                leaveQueue(player);
                return;
            } else {
                String estimatedTime = calculateEstimatedWait(queuedPlayers.size());
                actionBarMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7Searching for a Casual Duel... Estimated Time:&5 " + estimatedTime);
            }

            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(actionBarMsg));
        }, 0L, 40L);

        searchTasks.put(player.getUniqueId(), searchTask);

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
            Iterator<UUID> iterator = queuedPlayers.keySet().iterator();
            UUID player1Uuid = iterator.next();
            UUID player2Uuid = iterator.next();

            Player player1 = Bukkit.getPlayer(player1Uuid);
            Player player2 = Bukkit.getPlayer(player2Uuid);

            if (player1 != null && player2 != null && player1.isOnline() && player2.isOnline()) {
                boolean started = arenaManager.startDuel(player1, player2);

                if (started) {
                    queuedPlayers.remove(player1Uuid);
                    queuedPlayers.remove(player2Uuid);

                    cancelSearchTask(player1Uuid);
                    cancelSearchTask(player2Uuid);

                    cancelGuiUpdates(player1Uuid);
                    cancelGuiUpdates(player2Uuid);

                    player1.closeInventory();
                    player2.closeInventory();
                } else {
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
