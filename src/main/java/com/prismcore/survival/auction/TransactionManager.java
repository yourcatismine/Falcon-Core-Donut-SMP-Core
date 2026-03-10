package com.prismcore.survival.auction;

import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

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

    }

    public List<Transaction> getPlayerTransactions(UUID uuid) {
        return new java.util.ArrayList<>();
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
    }

    public void saveToConfig() {
    }

    public void deleteTransaction(Transaction tx) {
        UUID sellerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getSeller()).getUniqueId();
        UUID buyerUuid = this.controller.getPlugin().getServer().getOfflinePlayer(tx.getBuyer()).getUniqueId();

    }

    public void wipeTransactions(UUID uuid) {
    }
}
