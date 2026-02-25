package com.prismcore.survival.auction;

import java.util.List;
import java.util.UUID;
import com.prismcore.survival.manager.ActivityLogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class TransactionManager {
    private final AuctionController controller;

    public TransactionManager(AuctionController controller) {
        this.controller = controller;
    }

    public void recordSale(ItemStack item, double price, String seller, String buyer) {
        long now = System.currentTimeMillis();
        Transaction txSeller = new Transaction(item.clone(), price, buyer, seller, now, true);
        Transaction txBuyer = new Transaction(item.clone(), price, buyer, seller, now, false);

        UUID sellerUuid = controller.getPlugin().getServer().getOfflinePlayer(seller).getUniqueId();
        UUID buyerUuid = controller.getPlugin().getServer().getOfflinePlayer(buyer).getUniqueId();

        // Save to Database
        controller.getPlugin().getDatabaseManager().addAuctionTransaction(sellerUuid, txSeller);
        controller.getPlugin().getDatabaseManager().addAuctionTransaction(buyerUuid, txBuyer);

        String itemName = Utils.prettifyMaterialName(item.getType());
        controller.getPlugin().getActivityLogger().log(sellerUuid, ActivityLogger.LogType.AUCTION,
                "Sold " + itemName + " to " + buyer + " for $" + Utils.formatNumber(price));
        controller.getPlugin().getActivityLogger().log(buyerUuid, ActivityLogger.LogType.AUCTION,
                "Bought " + itemName + " from " + seller + " for $" + price);
    }

    public List<Transaction> getPlayerTransactions(UUID uuid) {
        return controller.getPlugin().getDatabaseManager().getAuctionTransactions(uuid);
    }

    public double getTotalSpent(UUID uuid) {
        return getTotalSpent(this.getPlayerTransactions(uuid));
    }

    public double getTotalSpent(List<Transaction> transactions) {
        double sum = 0.0;
        for (Transaction tx : transactions) {
            if (tx.isSale())
                continue;
            sum += tx.getPrice();
        }
        return sum;
    }

    public double getTotalMade(UUID uuid) {
        return getTotalMade(this.getPlayerTransactions(uuid));
    }

    public double getTotalMade(List<Transaction> transactions) {
        double sum = 0.0;
        for (Transaction tx : transactions) {
            if (!tx.isSale())
                continue;
            sum += tx.getPrice();
        }
        return sum;
    }

    public void loadFromConfig() {
        // Migration logic: Read from YML once and move to Database
        FileConfiguration cfg = this.controller.getStorageConfig();
        if (!cfg.isConfigurationSection("transactions")) {
            return;
        }
        controller.getPlugin().getLogger().info("Migrating auction transactions from YML to Database...");
        ConfigurationSection txSection = cfg.getConfigurationSection("transactions");
        for (String key : txSection.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            ConfigurationSection playerSec = txSection.getConfigurationSection(key);
            for (String idStr : playerSec.getKeys(false)) {
                String path = "transactions." + key + "." + idStr;
                ItemStack item = cfg.getItemStack(path + ".item");
                double price = cfg.getDouble(path + ".price");
                String buyer = cfg.getString(path + ".buyer");
                String seller = cfg.getString(path + ".seller");
                long timestamp = cfg.getLong(path + ".timestamp");
                boolean isSale = cfg.getBoolean(path + ".isSale");
                if (item == null || buyer == null || seller == null)
                    continue;
                Transaction tx = new Transaction(item, price, buyer, seller, timestamp, isSale);
                controller.getPlugin().getDatabaseManager().addAuctionTransaction(uuid, tx);
            }
        }
        cfg.set("transactions", null);
        this.controller.saveStorageFile();
        controller.getPlugin().getLogger().info("Auction transaction migration complete.");
    }

    public void saveToConfig() {
        // No longer needed, as we use Database
    }

    public void deleteTransaction(Transaction tx) {
        UUID sellerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getSeller()).getUniqueId();
        UUID buyerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getBuyer()).getUniqueId();

        // Delete from DB
        controller.getPlugin().getDatabaseManager().deleteAuctionTransaction(sellerUuid, tx.getTimestamp(),
                tx.getPrice());
        controller.getPlugin().getDatabaseManager().deleteAuctionTransaction(buyerUuid, tx.getTimestamp(),
                tx.getPrice());

        this.controller.getPlugin().getActivityLogger().log(sellerUuid, ActivityLogger.LogType.AUCTION,
                "[Admin] Deleted transaction for " + Utils.prettifyMaterialName(tx.getItem().getType())
                        + " (Seller side)");
        this.controller.getPlugin().getActivityLogger().log(buyerUuid, ActivityLogger.LogType.AUCTION,
                "[Admin] Deleted transaction for " + Utils.prettifyMaterialName(tx.getItem().getType())
                        + " (Buyer side)");
    }
}
