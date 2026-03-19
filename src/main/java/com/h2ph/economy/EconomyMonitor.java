package com.h2ph.economy;

import com.h2ph.Falcon;
import org.bukkit.OfflinePlayer;
import java.util.concurrent.atomic.AtomicReference;

public class EconomyMonitor {

    private static EconomyMonitor instance;
    private final Falcon plugin;

    private final AtomicReference<Double> totalMoney = new AtomicReference<>(0.0);
    private final AtomicReference<Double> volume24h = new AtomicReference<>(0.0);
    private boolean initialized = false;

    public EconomyMonitor(Falcon plugin) {
        this.plugin = plugin;
        instance = this;
        startPolling();
        startVolumeDecayTask();
    }

    public static EconomyMonitor getInstance() {
        return instance;
    }

    private void startPolling() {
        plugin.getSchedulerAdapter().runTaskTimerAsync(this::updateOnlineTotalMoney, 200, 200);
    }

    private void updateOnlineTotalMoney() {
        double onlineTotal = 0.0;
        try {
            if (plugin != null && plugin.isEnabled() && plugin.getServer() != null && 
                plugin.getServer().getPluginManager() != null && 
                plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin
                        .getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    net.milkbowl.vault.economy.Economy eco = rsp.getProvider();
                    if (eco != null) {
                        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                            if (p != null && eco.hasAccount(p)) {
                                onlineTotal += eco.getBalance(p);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        initialized = true;
    }

    public void onTransaction(double amount) {
        totalMoney.updateAndGet(v -> v + amount);
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
        plugin.getSchedulerAdapter().runTaskTimerAsync(() -> {
            volume24h.updateAndGet(v -> v * 0.95);
        }, 20 * 60 * 60, 20 * 60 * 60);
    }
}
