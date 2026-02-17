package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Periodically logs player money and shard balances to the ActivityLogger.
 * Snapshots are taken every 10 minutes.
 */
public class BalanceLogger {

    private final PrismSurvival plugin;

    public BalanceLogger(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Run every 10 minutes (12000 ticks)
        // Initial delay: 1 minute (1200 ticks) to let players load
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                logBalance(player);
            }
        }, 1200L, 12000L);
    }

    private void logBalance(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().get(uuid);
        if (data == null)
            return;

        // Use Vault balance if available, fallback to internal
        net.milkbowl.vault.economy.Economy eco = getEconomy();
        double money = (eco != null) ? eco.getBalance(player) : data.getMoney();
        double shards = data.getShards();

        plugin.getActivityLogger().log(uuid, ActivityLogger.LogType.MONEY, String.valueOf(money));
        plugin.getActivityLogger().log(uuid, ActivityLogger.LogType.SHARDS, String.valueOf(shards));
    }

    private net.milkbowl.vault.economy.Economy getEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }
}
