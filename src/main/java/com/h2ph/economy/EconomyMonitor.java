package com.h2ph.economy;

import com.h2ph.PrismSurvival;
import org.bukkit.OfflinePlayer;
import java.util.concurrent.atomic.AtomicReference;

public class EconomyMonitor {

    private static EconomyMonitor instance;
    private final PrismSurvival plugin;

    // Total Money Tracking
    private final AtomicReference<Double> totalMoney = new AtomicReference<>(0.0);
    // 24h Volume Tracking
    private final AtomicReference<Double> volume24h = new AtomicReference<>(0.0);
    private boolean initialized = false;

    public EconomyMonitor(PrismSurvival plugin) {
        this.plugin = plugin;
        instance = this;
        startPolling();
        startVolumeDecayTask();
    }

    public static EconomyMonitor getInstance() {
        return instance;
    }

    private void startPolling() {
        // Poll every 10 seconds (200 ticks) for online players only
        plugin.getSchedulerAdapter().runTaskTimerAsync(this::updateOnlineTotalMoney, 200, 200);
    }

    private void updateOnlineTotalMoney() {
        double onlineTotal = 0.0;
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin
                        .getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    net.milkbowl.vault.economy.Economy eco = rsp.getProvider();
                    for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                        if (eco.hasAccount(p)) {
                            onlineTotal += eco.getBalance(p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }

        // we don't overwrite totalMoney completely here, we just use this to keep
        // online balances fresh
        // totalMoney is more of a running total updated by onTransaction
        initialized = true;
    }

    public void onTransaction(double amount) {
        // Update the running total
        totalMoney.updateAndGet(v -> v + amount);
        // Update volume (absolute value of transaction)
        double absAmount = Math.abs(amount);
        volume24h.updateAndGet(v -> v + absAmount);
    }

    public double getTotalMoney() {
        return totalMoney.get();
    }

    public double getVolume24h() {
        return volume24h.get();
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void startVolumeDecayTask() {
        // Simple rolling decay to simulate 24h window
        // Decay by 5% every hour
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            volume24h.updateAndGet(v -> v * 0.95);
        }, 20 * 60 * 60, 20 * 60 * 60);
    }
}
