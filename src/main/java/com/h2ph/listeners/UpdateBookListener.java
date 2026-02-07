package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class UpdateBookListener implements Listener {
    private final PrismSurvival plugin;

    public UpdateBookListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent e) {
        Player p = e.getPlayer();
        // Only process the edit if this player was marked as the update writer via
        // /update
        if (!plugin.isPlayerMarkedAsUpdateWriter(p.getUniqueId()))
            return;
        try {
            BookMeta newMeta = e.getNewBookMeta();
            if (newMeta != null) {
                List<String> pages = newMeta.getPages();
                if (pages != null && !pages.isEmpty()) {
                    plugin.setActiveUpdate(pages);
                    String queuedMsg = "&aUpdate queued: it will be shown to the next player who joins.";
                    p.sendMessage(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', queuedMsg));
                    // Play saved sound to the player who saved the update
                    try {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // clear the mark regardless so accidental subsequent edits are ignored
        try {
            plugin.unmarkPlayerAsUpdateWriter(p.getUniqueId());
        } catch (Throwable ignored) {
        }

        // Schedule restoration/clearing one tick later to allow the server to update
        // inventories
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            try {
                boolean hadGivenBook = p.hasMetadata("update_given_book");

                if (p.hasMetadata("update_prev_slot")) {
                    try {
                        java.util.List<MetadataValue> slotVals = p.getMetadata("update_prev_slot");
                        if (slotVals != null && !slotVals.isEmpty()) {
                            Object slotVal = slotVals.get(0).value();
                            int slot = (slotVal instanceof Number) ? ((Number) slotVal).intValue()
                                    : p.getInventory().getHeldItemSlot();
                            java.util.List<MetadataValue> vals = p.getMetadata("update_prev_hand");
                            Object val = (vals != null && !vals.isEmpty()) ? vals.get(0).value() : null;
                            try {
                                if (val instanceof ItemStack) {
                                    p.getInventory().setItem(slot, (ItemStack) val);
                                } else {
                                    p.getInventory().setItem(slot, null);
                                }
                                p.updateInventory();
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        p.removeMetadata("update_prev_slot", plugin);
                    } catch (Throwable ignored) {
                    }
                    try {
                        p.removeMetadata("update_prev_hand", plugin);
                    } catch (Throwable ignored) {
                    }
                } else if (hadGivenBook) {
                    // No previous slot stored, but we flagged that we gave a book: clear the held
                    // slot
                    try {
                        p.getInventory().setItem(p.getInventory().getHeldItemSlot(), null);
                        p.updateInventory();
                    } catch (Throwable ignored) {
                    }
                }

                if (p.hasMetadata("update_given_book"))
                    try {
                        p.removeMetadata("update_given_book", plugin);
                    } catch (Throwable ignored) {
                    }
            } catch (Throwable ignored) {
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.hasActiveUpdate())
            return;

        long currentVersion = plugin.getActiveUpdateVersion();
        long playerSeen = plugin.getPlayerDataManager().get(p.getUniqueId()).getLastSeenUpdate();

        if (playerSeen >= currentVersion) {
            return; // Already seen
        }

        // Mark as seen immediately so they don't get spam if they rejoin,
        // regardless of whether they actually successfully opened the book (imperfect
        // but safe).
        plugin.getPlayerDataManager().get(p.getUniqueId()).setLastSeenUpdate(currentVersion);

        java.util.List<String> finalPages = plugin.getActiveUpdatePages();
        if (finalPages == null || finalPages.isEmpty())
            return;

        // Attempt to open the book, but some auth plugins (eg. NLogin) prevent
        // immediate UI actions.
        // Retry a few times (once per second) and fall back to placing the book in
        // inventory.
        // Recursive task for retries
        new Runnable() {
            private int tries = 0;
            private final int maxTries = 15; // ~15 seconds

            @Override
            public void run() {
                try {
                    if (!p.isOnline()) {
                        return;
                    }

                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();
                    if (meta != null) {
                        String bookTitle = "ѕᴇʀᴠᴇʀ ᴜᴘᴅᴀᴛᴇ";
                        String bookAuthorCfg = "%player%";
                        meta.setTitle(bookTitle);
                        String author = ("%player%".equals(bookAuthorCfg) ? plugin.getName() : bookAuthorCfg);
                        meta.setAuthor(author);
                        String itemName = "&aѕᴇʀᴠᴇʀ ᴜᴘᴅᴀᴛᴇ";
                        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemName));
                        meta.setPages(finalPages);
                        book.setItemMeta(meta);
                    }

                    try {
                        p.openBook(book);
                        // opened successfully; play join sound and cancel retries
                        try {
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_SNARE, 1.0f, 1.0f);
                        } catch (Throwable t) {
                        }
                        return;
                    } catch (Throwable openEx) {
                        // not opened yet (likely blocked by auth); retry
                    }

                    tries++;
                    if (tries >= maxTries) {
                        // give fallback inventory book
                        int free = p.getInventory().firstEmpty();
                        if (free >= 0)
                            p.getInventory().setItem(free, book);
                        String joinerMsg = "&6A server update has been placed in your inventory.";
                        p.sendMessage(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', joinerMsg));
                        try {
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_SNARE, 1.0f, 1.0f);
                        } catch (Throwable t) {
                        }
                        return;
                    }

                    // Re-schedule
                    plugin.getSchedulerAdapter().runTaskLater(this, 20L);

                } catch (Throwable ignored) {
                }
            }
        }.run();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // If player quits and still has stored previous hand metadata, clear it
        Player p = e.getPlayer();
        try {
            // ensure marking cleared
            plugin.unmarkPlayerAsUpdateWriter(p.getUniqueId());
        } catch (Throwable ignored) {
        }
    }
}