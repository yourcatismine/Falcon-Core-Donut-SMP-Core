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
    public final List<ItemStack> storage = java.util.Collections.synchronizedList(new ArrayList<ItemStack>());

    public int remainingAmount() {
        return Math.max(0, this.requested - this.delivered);
    }

    public double totalPrice() {
        return (double) this.requested * this.priceEach;
    }
}
