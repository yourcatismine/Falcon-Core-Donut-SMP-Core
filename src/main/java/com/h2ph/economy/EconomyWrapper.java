package com.h2ph.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class EconomyWrapper implements Economy {

    private final Economy wrapped;
    private final EconomyMonitor monitor;

    public EconomyWrapper(Economy wrapped, EconomyMonitor monitor) {
        this.wrapped = wrapped;
        this.monitor = monitor;
    }

    @Override
    public boolean isEnabled() {
        return wrapped.isEnabled();
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public boolean hasBankSupport() {
        return wrapped.hasBankSupport();
    }

    @Override
    public int fractionalDigits() {
        return wrapped.fractionalDigits();
    }

    @Override
    public String format(double amount) {
        return wrapped.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return wrapped.currencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return wrapped.currencyNameSingular();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return wrapped.hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return wrapped.hasAccount(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return wrapped.hasAccount(playerName, worldName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return wrapped.hasAccount(player, worldName);
    }

    @Override
    public double getBalance(String playerName) {
        return wrapped.getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return wrapped.getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return wrapped.getBalance(playerName, world);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return wrapped.getBalance(player, world);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return wrapped.has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return wrapped.has(player, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return wrapped.has(playerName, worldName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return wrapped.has(player, worldName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        EconomyResponse response = wrapped.withdrawPlayer(playerName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(-amount);
        }
        return response;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        EconomyResponse response = wrapped.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(-amount);
        }
        return response;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        EconomyResponse response = wrapped.withdrawPlayer(playerName, worldName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(-amount);
        }
        return response;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        EconomyResponse response = wrapped.withdrawPlayer(player, worldName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(-amount);
        }
        return response;
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        EconomyResponse response = wrapped.depositPlayer(playerName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(amount);
        }
        return response;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        EconomyResponse response = wrapped.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(amount);
        }
        return response;
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        EconomyResponse response = wrapped.depositPlayer(playerName, worldName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(amount);
        }
        return response;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        EconomyResponse response = wrapped.depositPlayer(player, worldName, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(amount);
        }
        return response;
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return wrapped.createBank(name, player);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return wrapped.createBank(name, player);
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return wrapped.deleteBank(name);
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return wrapped.bankBalance(name);
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return wrapped.bankHas(name, amount);
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        EconomyResponse response = wrapped.bankWithdraw(name, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(-amount);
        }
        return response;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        EconomyResponse response = wrapped.bankDeposit(name, amount);
        if (response.transactionSuccess()) {
            monitor.onTransaction(amount);
        }
        return response;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return wrapped.isBankOwner(name, playerName);
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return wrapped.isBankOwner(name, player);
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return wrapped.isBankMember(name, playerName);
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return wrapped.isBankMember(name, player);
    }

    @Override
    public List<String> getBanks() {
        return wrapped.getBanks();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return wrapped.createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return wrapped.createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return wrapped.createPlayerAccount(playerName, worldName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return wrapped.createPlayerAccount(player, worldName);
    }
}
