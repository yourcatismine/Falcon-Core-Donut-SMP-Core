package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvisorCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public AdvisorCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("prism.admin.advisor")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            giveWritableBook(player);
            return true;
        }

        openAdvisorBook(player);
        return true;
    }

    private void giveWritableBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dAdvisor Book Editor"));
            List<String> currentPages = plugin.getActiveAdvisorPages();
            if (currentPages != null && !currentPages.isEmpty()) {
                meta.setPages(currentPages);
            }
            book.setItemMeta(meta);
        }

        plugin.markPlayerAsAdvisorWriter(player.getUniqueId());

        int slot = player.getInventory().firstEmpty();
        if (slot != -1) {
            player.getInventory().setItem(slot, book);
            player.sendMessage(ChatColor.GREEN + "You have been given the Advisor Editor book.");
            player.sendMessage(
                    ChatColor.GRAY + "Write your content and sign/save the book to update the /advisor command.");
        } else {
            player.sendMessage(ChatColor.RED + "Your inventory is full! Please clear a slot.");
            plugin.unmarkPlayerAsAdvisorWriter(player.getUniqueId());
        }
    }

    private void openAdvisorBook(Player player) {
        if (!plugin.hasActiveAdvisor()) {
            player.sendMessage(ChatColor.RED + "There is no advisor content available yet.");
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("Server Advisor");
            meta.setAuthor("Server Admin");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dServer Advisor"));
            meta.setPages(plugin.getActiveAdvisorPages());
            book.setItemMeta(meta);
        }

        player.openBook(book);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("prism.admin.advisor")) {
                if ("admin".startsWith(args[0].toLowerCase())) {
                    completions.add("admin");
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
