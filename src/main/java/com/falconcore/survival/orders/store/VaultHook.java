
package com.falconcore.survival.orders.store;

import com.falconcore.survival.orders.OrdersModule;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private final OrdersModule module;
    private Economy econ;

    public VaultHook(OrdersModule module) {
        this.module = module;
        if (module.getPlugin().getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = module.getPlugin().getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp != null) {
            this.econ = rsp.getProvider();
        }
    }

    public boolean hooked() {
        return true;
    }

    public boolean canAfford(OfflinePlayer p, double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        if (amount <= 0.0) {
            return true;
        }
        if (this.econ != null) {
            try {
                return this.econ.has(p, amount);
            } catch (Throwable ignored) {
                return this.bal(p) + 1.0E-9 >= amount;
            }
        }
        com.falconcore.survival.manager.PlayerData pd = module.getPlugin().getPlayerDataManager().get(p.getUniqueId());
        return pd != null && pd.getMoney() >= amount;
    }

    public boolean take(OfflinePlayer p, double amount) {
        return take(p, amount, "Orders");
    }

    public boolean take(OfflinePlayer p, double amount, String source) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        if (amount <= 0.0) {
            return true;
        }
        if (!this.canAfford(p, amount)) {
            return false;
        }
        if (this.econ != null) {
            try {
                com.falconcore.survival.auction.EconomyHandler.setSourceContext(source);
                try {
                    return this.econ.withdrawPlayer(p, amount).transactionSuccess();
                } finally {
                    com.falconcore.survival.auction.EconomyHandler.clearSourceContext();
                }
            } catch (Throwable t) {
                this.module.getPlugin().getLogger().warning("Vault withdraw failed: " + t.getMessage());
                return false;
            }
        }
        com.falconcore.survival.manager.PlayerData pd = module.getPlugin().getPlayerDataManager().get(p.getUniqueId());
        if (pd == null || pd.getMoney() < amount) {
            return false;
        }
        pd.removeMoney(amount, source);
        module.getPlugin().getPlayerDataManager().saveMoneyAsync(p.getUniqueId(), pd);
        return true;
    }

    public boolean give(OfflinePlayer p, double amount) {
        return give(p, amount, "Orders");
    }

    public boolean give(OfflinePlayer p, double amount, String source) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        if (amount <= 0.0) {
            return true;
        }
        if (this.econ != null) {
            try {
                com.falconcore.survival.auction.EconomyHandler.setSourceContext(source);
                try {
                    return this.econ.depositPlayer(p, amount).transactionSuccess();
                } finally {
                    com.falconcore.survival.auction.EconomyHandler.clearSourceContext();
                }
            } catch (Throwable t) {
                this.module.getPlugin().getLogger().warning("Vault deposit failed: " + t.getMessage());
                return false;
            }
        }
        com.falconcore.survival.manager.PlayerData pd = module.getPlugin().getPlayerDataManager().get(p.getUniqueId());
        if (pd != null) {
            pd.addMoney(amount, source);
            module.getPlugin().getPlayerDataManager().saveMoneyAsync(p.getUniqueId(), pd);
            return true;
        }
        return false;
    }

    public double bal(OfflinePlayer p) {
        if (this.econ != null) {
            try {
                return this.econ.getBalance(p);
            } catch (Throwable t) {
                this.module.getPlugin().getLogger().warning("Vault getBalance failed: " + t.getMessage());
            }
        }
        com.falconcore.survival.manager.PlayerData pd = module.getPlugin().getPlayerDataManager().get(p.getUniqueId());
        return pd != null ? pd.getMoney() : 0.0;
    }
}
