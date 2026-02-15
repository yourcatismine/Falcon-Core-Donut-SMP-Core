package com.prismcore.survival.auction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.prismcore.survival.auction.AuctionController;
import com.prismcore.survival.auction.Transaction;
import com.prismcore.survival.manager.ActivityLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class TransactionManager {
    private final AuctionController controller;
    private final Map<UUID, List<Transaction>> transactions;

    public TransactionManager(AuctionController controller) {
        this.controller = controller;
        this.transactions = new HashMap<UUID, List<Transaction>>();
    }

    public void recordSale(ItemStack item, double price, String seller, String buyer) {
        long now = System.currentTimeMillis();
        Transaction tx = new Transaction(item.clone(), price, buyer, seller, now, true);
        this.addTransaction(seller, tx);
        Transaction txBuyer = new Transaction(item.clone(), price, buyer, seller, now, false);
        this.addTransaction(buyer, txBuyer);

        UUID sellerUuid = controller.getPlugin().getServer().getOfflinePlayer(seller).getUniqueId();
        UUID buyerUuid = controller.getPlugin().getServer().getOfflinePlayer(buyer).getUniqueId();

        String itemName = Utils.prettifyMaterialName(item.getType());
        controller.getPlugin().getActivityLogger().log(sellerUuid, ActivityLogger.LogType.AUCTION,
                "Sold " + itemName + " to " + buyer + " for $" + Utils.formatNumber(price));
        controller.getPlugin().getActivityLogger().log(buyerUuid, ActivityLogger.LogType.AUCTION,
                "Bought " + itemName + " from " + seller + " for $" + price);

        // optimization: remove immediate save
        // this.saveToConfig();
    }

    private void addTransaction(String playerName, Transaction tx) {
        UUID uuid = this.controller.getPlugin().getServer().getOfflinePlayer(playerName).getUniqueId();
        this.transactions.computeIfAbsent(uuid, k -> new ArrayList()).add(tx);
    }

    public List<Transaction> getPlayerTransactions(UUID uuid) {
        List list = this.transactions.getOrDefault(uuid, new ArrayList());
        ArrayList<Transaction> copy = new ArrayList<Transaction>(list);
        copy.sort(Comparator.comparingLong(Transaction::getTimestamp).reversed());
        return copy;
    }

    public double getTotalSpent(UUID uuid) {
        double sum = 0.0;
        for (Transaction tx : this.getPlayerTransactions(uuid)) {
            if (tx.isSale())
                continue;
            sum += tx.getPrice();
        }
        return sum;
    }

    public double getTotalMade(UUID uuid) {
        double sum = 0.0;
        for (Transaction tx : this.getPlayerTransactions(uuid)) {
            if (!tx.isSale())
                continue;
            sum += tx.getPrice();
        }
        return sum;
    }

    public void loadFromConfig() {
        FileConfiguration cfg = this.controller.getStorageConfig();
        this.transactions.clear();
        if (!cfg.isConfigurationSection("transactions")) {
            return;
        }
        ConfigurationSection txSection = cfg.getConfigurationSection("transactions");
        for (String key : txSection.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            ArrayList<Transaction> list = new ArrayList<Transaction>();
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
                list.add(tx);
            }
            list.sort(Comparator.comparingLong(Transaction::getTimestamp));
            this.transactions.put(uuid, list);
        }
    }

    public void saveToConfig() {
        // ... (existing code)
    }

    public void deleteTransaction(Transaction tx) {
        UUID sellerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getSeller()).getUniqueId();
        UUID buyerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getBuyer()).getUniqueId();

        // Remove from seller's list
        List<Transaction> sellerTx = this.transactions.get(sellerUuid);
        if (sellerTx != null) {
            sellerTx.removeIf(t -> t.getTimestamp() == tx.getTimestamp() && t.getPrice() == tx.getPrice()
                    && t.getSeller().equals(tx.getSeller()) && t.getBuyer().equals(tx.getBuyer()));
        }

        // Remove from buyer's list
        List<Transaction> buyerTx = this.transactions.get(buyerUuid);
        if (buyerTx != null) {
            buyerTx.removeIf(t -> t.getTimestamp() == tx.getTimestamp() && t.getPrice() == tx.getPrice()
                    && t.getSeller().equals(tx.getSeller()) && t.getBuyer().equals(tx.getBuyer()));
        }

        this.saveToConfig();
        this.controller.getPlugin().getActivityLogger().log(sellerUuid, ActivityLogger.LogType.AUCTION,
                "[Admin] Deleted transaction for " + Utils.prettifyMaterialName(tx.getItem().getType())
                        + " (Seller side)");
        this.controller.getPlugin().getActivityLogger().log(buyerUuid, ActivityLogger.LogType.AUCTION,
                "[Admin] Deleted transaction for " + Utils.prettifyMaterialName(tx.getItem().getType())
                        + " (Buyer side)");
    }
}
