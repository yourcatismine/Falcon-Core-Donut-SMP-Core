package com.falconcore.survival.auction;

import org.bukkit.inventory.ItemStack;

public class Transaction {
    private final ItemStack item;
    private final double price;
    private final String buyer;
    private final String seller;
    private final long timestamp;
    private final boolean isSale;

    public Transaction(ItemStack item, double price, String buyer, String seller, long timestamp, boolean isSale) {
        this.item = item;
        this.price = price;
        this.buyer = buyer;
        this.seller = seller;
        this.timestamp = timestamp;
        this.isSale = isSale;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public double getPrice() {
        return this.price;
    }

    public String getBuyer() {
        return this.buyer;
    }

    public String getSeller() {
        return this.seller;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public boolean isSale() {
        return this.isSale;
    }
}
