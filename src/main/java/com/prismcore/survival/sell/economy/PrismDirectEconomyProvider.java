package com.prismcore.survival.sell.economy;

import com.prismcore.survival.sell.PrismSell;
import com.prismcore.survival.sell.data.PlayerData;
import java.util.UUID;

public class PrismDirectEconomyProvider implements SellEconomyProvider {

    private final PrismSell plugin;

    public PrismDirectEconomyProvider(PrismSell plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data == null)
            return false;
        double current = data.getMoney();
        if (current < amount)
            return false;
        data.setMoney(current - amount);
        return true;
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data == null)
            return false;
        data.setMoney(data.getMoney() + amount);
        return true;
    }

    @Override
    public double getBalance(UUID uuid) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        return (data != null) ? data.getMoney() : 0.0;
    }

    @Override
    public String getName() {
        return "PrismEconomy";
    }
}
