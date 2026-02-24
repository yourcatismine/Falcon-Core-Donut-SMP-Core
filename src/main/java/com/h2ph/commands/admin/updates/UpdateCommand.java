package com.h2ph.commands.admin.updates;

import com.h2ph.PrismSurvival;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class UpdateCommand implements CommandExecutor {
    private final PrismSurvival plugin;

    public UpdateCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.isOp() && !p.hasPermission("prism.update")) {
            p.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Load configurable values
        String bookTitle = "ѕᴇʀᴠᴇʀ ᴜᴘᴅᴀᴛᴇ";
        String bookAuthorCfg = "%player%";
        java.util.Map<String, String> msgs = new java.util.HashMap<>();
        msgs.put("opened", "&aWrite your update and sign/save it — it will be queued for the next joiner.");
        msgs.put("given_in_hand",
                "&aA writable book has been placed in your hand. Write and sign it to queue the update.");
        msgs.put("timeout", "&eUpdate timed out — reopen with /update if you still want to send it.");
        msgs.put("no-permission", "&cYou don't have permission to use this command.");
        msgs.put("queued", "&aUpdate queued: it will be shown to the next player who joins.");

        ItemStack writable = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) writable.getItemMeta();
        if (meta != null) {
            meta.setTitle(bookTitle);
            String author = ("%player%".equals(bookAuthorCfg) ? p.getName() : plugin.getName());
            meta.setAuthor(author);
            String itemName = "&aѕᴇʀᴠᴇʀ ᴜᴘᴅᴀᴛᴇ";
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemName));
            meta.setPages(java.util.Collections.singletonList(""));
            writable.setItemMeta(meta);
        }

        // Mark the player as the intended update writer, then open the book GUI without
        // modifying their inventory
        plugin.markPlayerAsUpdateWriter(p.getUniqueId());
        p.sendMessage(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msgs.get("opened")));
        plugin.getLogger().info("Player marked as update writer: " + p.getName());
        // Schedule opening the book on the main thread. Try reflective openBook first;
        // if not available, temporarily give the writable book in hand.
        plugin.getSchedulerAdapter().runAtLocation(p.getLocation(), () -> {
            boolean opened = false;
            try {
                try {
                    java.lang.reflect.Method m = Player.class.getMethod("openBook", ItemStack.class);
                    if (m != null) {
                        m.invoke(p, writable);
                        opened = true;
                    }
                } catch (NoSuchMethodException nsme) {
                    // method not present, will fallback
                } catch (Throwable invokeEx) {
                    // invocation failed, try fallback
                }
                if (!opened) {
                    // fallback: put writable into hand, saving previous item as metadata so we can
                    // restore it
                    int slot = p.getInventory().getHeldItemSlot();
                    ItemStack prev = p.getInventory().getItem(slot);
                    p.setMetadata("update_prev_hand", new org.bukkit.metadata.FixedMetadataValue(plugin, prev));
                    p.setMetadata("update_prev_slot", new org.bukkit.metadata.FixedMetadataValue(plugin, slot));
                    p.setMetadata("update_given_book", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    p.getInventory().setItem(slot, writable);
                    p.updateInventory();
                    try {
                        p.openBook(writable);
                    } catch (Throwable ignored) {
                    }
                    p.sendMessage(
                            net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msgs.get("given_in_hand")));
                }
            } catch (Throwable t) {
                plugin.unmarkPlayerAsUpdateWriter(p.getUniqueId());
                p.sendMessage(ChatColor.RED + "Failed to open book editor: " + t.getMessage());
            }
        });

        // Safety: unmark after 2 minutes if they never sign to avoid accidental queuing
        // later
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            if (plugin.isPlayerMarkedAsUpdateWriter(p.getUniqueId())) {
                plugin.unmarkPlayerAsUpdateWriter(p.getUniqueId());
                plugin.getLogger().info("Auto-unmarked update writer due to timeout: " + p.getName());
                try {
                    p.sendMessage(
                            ChatColor.YELLOW + "Update timed out — reopen with /update if you still want to send it.");
                } catch (Throwable ignored) {
                }
            }
        }, 20L * 120L);

        return true;
    }
}