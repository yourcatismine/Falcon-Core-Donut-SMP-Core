package com.prismcore.survival.auction;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class AuctionItem {
    private final UUID id;
    private final String seller;
    private final ItemStack itemStack;
    private double price;
    private final long listedAt;
    private final int duration;

    private final String searchName;
    private final String searchSeller;

    public AuctionItem(UUID id, String seller, ItemStack itemStack, double price, long listedAt, int duration) {
        this.id = id;
        this.seller = seller;
        this.itemStack = itemStack.clone();
        this.price = price;
        this.listedAt = listedAt;
        this.duration = duration;
        this.searchName = Utils.prettifyMaterialName(itemStack.getType()).toLowerCase();
        this.searchSeller = seller.toLowerCase();
    }

    public UUID getId() {
        return this.id;
    }

    public String getSeller() {
        return this.seller;
    }

    public ItemStack getItemStack() {
        return this.itemStack.clone();
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getListedAt() {
        return this.listedAt;
    }

    public int getDuration() {
        return this.duration;
    }

    public String getSearchName() {
        return this.searchName;
    }

    public String getSearchSeller() {
        return this.searchSeller;
    }
}
