/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 */
package com.prismcore.survival.orders.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.prismcore.survival.orders.data.ItemKey;
import org.bukkit.inventory.ItemStack;

public class Order {
    public UUID id;
    public UUID owner;
    public ItemKey key;
    public int requested;
    public int delivered;
    public double priceEach;
    public double paid;
    public boolean canceled;
    public boolean completed;
    public long creationTime;
    public List<ItemStack> storage = java.util.Collections.synchronizedList(new ArrayList<ItemStack>());

    public Order() {
    }

    public Order(UUID id, UUID owner, ItemKey key, int requested, int delivered, double priceEach, double paid,
            boolean canceled, boolean completed, long creationTime) {
        this.id = id;
        this.owner = owner;
        this.key = key;
        this.requested = requested;
        this.delivered = delivered;
        this.priceEach = priceEach;
        this.paid = paid;
        this.canceled = canceled;
        this.completed = completed;
        this.creationTime = creationTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getItemKey() {
        return key != null ? key.serialize() : "";
    }

    public int getRequested() {
        return requested;
    }

    public int getDelivered() {
        return delivered;
    }

    public double getPriceEach() {
        return priceEach;
    }

    public double getPaid() {
        return paid;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public List<ItemStack> getStorage() {
        return storage;
    }

    public void setStorage(List<ItemStack> storage) {
        this.storage.clear();
        if (storage != null)
            this.storage.addAll(storage);
    }

    public int remainingAmount() {
        return Math.max(0, this.requested - this.delivered);
    }

    public double totalPrice() {
        return (double) this.requested * this.priceEach;
    }
}
