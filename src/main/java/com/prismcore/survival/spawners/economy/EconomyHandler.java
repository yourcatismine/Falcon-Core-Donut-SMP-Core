package com.prismcore.survival.spawners.economy;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.sell.PrismSell;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EconomyHandler {
    private final PrismSurvival plugin;

    public EconomyHandler(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public double getWorth(Material material) {
        PrismSell prismSell = plugin.getPrismSell();
        if (prismSell != null && prismSell.getPricesManager() != null) {
            return prismSell.getPricesManager().getPrice(new ItemStack(material));
        }
        return 0.0;
    }

    public void addMoney(Player player, double amount) {
        PrismSell prismSell = plugin.getPrismSell();
        if (prismSell != null && prismSell.getEconomy() != null) {
            prismSell.getEconomy().deposit(player.getUniqueId(), amount);
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
