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
        // Poll every 5 seconds (100 ticks) to ensure total money is accurate
        // This handles cases where transactions bypass the wrapper (e.g. /eco commands)
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            updateTotalMoney();
        }, 100, 100);
    }

    private void updateTotalMoney() {
        double currentTotal = 0.0;
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin
                        .getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    net.milkbowl.vault.economy.Economy eco = rsp.getProvider();
                    // Some economies might block on getOfflinePlayers, so we must be async (which
                    // we are)
                    for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
                        // Check if account exists to avoid creating files for non-eco players
                        if (eco.hasAccount(p)) {
                            currentTotal += eco.getBalance(p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail or log debug if needed
        }

        // Update the atomic reference
        totalMoney.set(currentTotal);
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
