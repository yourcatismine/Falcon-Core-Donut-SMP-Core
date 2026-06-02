package com.falconcore.survival.sell.economy;

import com.falconcore.survival.manager.PlayerData;
import com.falconcore.survival.sell.FalconSell;

import java.util.UUID;

/**
 * Bridges the sell module's economy calls to Falcon's main
 * PlayerDataManager (the YAML-backed one that /bal and all other
 * economy commands read from).
 */
public class FalconDirectEconomyProvider implements SellEconomyProvider {

    private final FalconSell plugin;

    public FalconDirectEconomyProvider(FalconSell plugin) {
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
        return "FalconEconomy";
    }
}
