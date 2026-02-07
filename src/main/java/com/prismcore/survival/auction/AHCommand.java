package com.prismcore.survival.auction;

import java.util.List;
import com.prismcore.survival.auction.AuctionItem;
import com.prismcore.survival.auction.AuctionController;
import com.prismcore.survival.auction.GUIHandler;
import com.prismcore.survival.auction.Utils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AHCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final AuctionController controller;

    public AHCommand(AuctionController controller) {
        this.controller = controller;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.formatColors("&#ff4444Only players can use this command!"));
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration cfg = this.controller.getConfig();
        Sound villagerNo = Sound.valueOf((String) cfg.getString("sounds.villager-no"));
        if (args.length == 0) {
            player.removeMetadata("ah-filter", (Plugin) this.controller.getPlugin());
            player.removeMetadata("ah-admin-view", (Plugin) this.controller.getPlugin());
            GUIHandler.openMainGUI(player, 1, this.controller);
            return true;
        }
        if (args.length >= 1 && args[0].trim().equalsIgnoreCase("admin")) {
            if (!player.hasPermission("auction.admin")) {
                player.removeMetadata("ah-filter", (Plugin) this.controller.getPlugin());
                player.removeMetadata("ah-admin-view", (Plugin) this.controller.getPlugin());
                GUIHandler.openMainGUI(player, 1, this.controller);
                return true;
            }
            player.setMetadata("ah-admin-view",
                    (MetadataValue) new FixedMetadataValue((Plugin) this.controller.getPlugin(), (Object) true));
            player.removeMetadata("ah-admin-player-filter", (Plugin) this.controller.getPlugin());
            player.removeMetadata("ah-admin-target", (Plugin) this.controller.getPlugin());
            GUIHandler.openAdminPlayerListGUI(player, 1, this.controller);
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
            int max = cfg.getInt("settings.max-auction-listed");
            int currentCount = 0;
            for (AuctionItem ai : this.controller.getAuctionManager().getActiveItems()) {
                if (!ai.getSeller().equals(player.getName()))
                    continue;
                ++currentCount;
            }
            if (currentCount >= max) {
                String msg = cfg.getString("messages.max-listings").replace("{count}", String.valueOf(currentCount))
                        .replace("{max}", String.valueOf(max));
                player.sendMessage(Utils.formatColors(msg));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            String priceArg = args[1];
            double rawPrice = Utils.parsePrice(priceArg);
            if (rawPrice < 0.0) {
                player.sendMessage(Utils.formatColors("&#ff4444Invalid price format. Use numbers or K/M/B/T suffix."));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            double maxPrice = cfg.getDouble("settings.max-auction-price", 1000000000000.0);
            if (rawPrice > maxPrice) {
                player.sendMessage(Utils.formatColors(cfg.getString("messages.max-price-exceeded",
                        "&#ff4444Price exceeds the maximum allowed limit!")));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage(Utils.formatColors("&#ff4444You must hold an item to sell!"));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            String matName = held.getType().name();
            List disabled = cfg.getStringList("settings.disabled-items");
            for (Object dObj : disabled) {
                String d = (String) dObj;
                if (!d.trim().equalsIgnoreCase(matName))
                    continue;
                player.sendMessage(Utils.formatColors(cfg.getString("messages.disabled-item")));
                player.playSound(player.getLocation(), villagerNo, 1.0f, 1.0f);
                return true;
            }
            GUIHandler.openSellConfirm(player, rawPrice, this.controller);
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s).append(" ");
        }
        String searchTerm = sb.toString().trim().toLowerCase();
        player.setMetadata("ah-filter",
                (MetadataValue) new FixedMetadataValue((Plugin) this.controller.getPlugin(), (Object) searchTerm));
        player.removeMetadata("ah-admin-view", (Plugin) this.controller.getPlugin());
        GUIHandler.openMainGUI(player, 1, this.controller);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            if (sender.hasPermission("auction.admin")) {
                completions.add("admin");
            }
            completions.add("<search>");
            return org.bukkit.util.StringUtil.copyPartialMatches(args[0], completions, new java.util.ArrayList<>());
        }
        return java.util.Collections.emptyList();
    }
}
