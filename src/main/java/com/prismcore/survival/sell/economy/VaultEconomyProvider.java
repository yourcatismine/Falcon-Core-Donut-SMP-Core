package com.prismcore.survival.sell.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import java.util.UUID;

public class VaultEconomyProvider implements SellEconomyProvider {

    private final Economy economy;

    public VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        return economy.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount).transactionSuccess();
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        return economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount).transactionSuccess();
    }

    @Override
    public double getBalance(UUID uuid) {
        return economy.getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    @Override
    public String getName() {
        return "Vault";
    }
}
