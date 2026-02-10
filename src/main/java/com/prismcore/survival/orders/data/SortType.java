/*
 * Decompiled with CFR 0.152.
 */
package com.prismcore.survival.orders.data;

public enum SortType {
    MOST_PAID,
    MOST_DELIVERED,
    RECENTLY_LISTED,
    MOST_MONEY_PER_ITEM;

    public SortType next() {
        return switch (this.ordinal()) {
            default -> throw new IllegalStateException("Unexpected ordinal: " + this.ordinal());
            case 0 -> MOST_DELIVERED;
            case 1 -> RECENTLY_LISTED;
            case 2 -> MOST_MONEY_PER_ITEM;
            case 3 -> MOST_PAID;
        };
    }
}
