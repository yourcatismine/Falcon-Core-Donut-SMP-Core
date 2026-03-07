package com.prismcore.survival.sell.economy;

import java.util.UUID;

public interface SellEconomyProvider {
    boolean withdraw(UUID uuid, double amount);

    boolean deposit(UUID uuid, double amount);

    double getBalance(UUID uuid);

    String getName();
}
