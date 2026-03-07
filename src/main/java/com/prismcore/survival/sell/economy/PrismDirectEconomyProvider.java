package com.prismcore.survival.sell.economy;

import com.prismcore.survival.manager.PlayerData;
import com.prismcore.survival.sell.PrismSell;

import java.util.UUID;

/**
 * Bridges the sell module's economy calls to PrismSurvival's main
 * PlayerDataManager (the YAML-backed one that /bal and all other
 * economy commands read from).
 */
public class PrismDirectEconomyProvider implements SellEconomyProvider {

    private final PrismSell plugin;

    public PrismDirectEconomyProvider(PrismSell plugin) {
        this.plugin = plugin;
    }

    private PlayerData getData(UUID uuid) {
        return plugin.getPlugin().getPlayerDataManager().get(uuid);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        PlayerData data = getData(uuid);
        if (data == null)
            return false;
        if (data.getMoney() < amount)
            return false;
        data.removeMoney(amount, "Sell-Withdraw");
        plugin.getPlugin().getPlayerDataManager().saveMoneyAsync(uuid, data);
        return true;
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        PlayerData data = getData(uuid);
        if (data == null)
            return false;
        data.addMoney(amount, "Sell-Deposit");
        plugin.getPlugin().getPlayerDataManager().saveMoneyAsync(uuid, data);
        return true;
    }

    @Override
    public double getBalance(UUID uuid) {
        PlayerData data = getData(uuid);
        return (data != null) ? data.getMoney() : 0.0;
    }

    @Override
    public String getName() {
        return "PrismEconomy";
    }
}
