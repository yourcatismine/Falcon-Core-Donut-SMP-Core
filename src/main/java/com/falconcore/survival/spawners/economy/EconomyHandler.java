package com.falconcore.survival.spawners.economy;

import com.h2ph.Falcon;
import com.falconcore.survival.sell.FalconSell;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EconomyHandler {
    private final Falcon plugin;

    public EconomyHandler(Falcon plugin) {
        this.plugin = plugin;
    }

    public double getWorth(Material material) {
        FalconSell falconSell = plugin.getFalconSell();
        if (falconSell != null && falconSell.getPricesManager() != null) {
            return falconSell.getPricesManager().getPrice(new ItemStack(material));
        }
        return 0.0;
    }

    public void addMoney(Player player, double amount) {
        FalconSell falconSell = plugin.getFalconSell();
        if (falconSell != null && falconSell.getEconomy() != null) {
            falconSell.getEconomy().deposit(player.getUniqueId(), amount);
        }
    }

    public double sellItems(Player player, Map<Material, Long> items) {
        double total = 0;
        for (Map.Entry<Material, Long> entry : items.entrySet()) {
            double worth = getWorth(entry.getKey()) * entry.getValue();
            total += worth;
        }
        if (total > 0) {
            addMoney(player, total);
        }
        return total;
    }
}
