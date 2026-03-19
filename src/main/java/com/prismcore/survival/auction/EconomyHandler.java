package com.prismcore.survival.auction;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHandler {
    private static Economy vaultEcon;
    private static PrismSurvival plugin;
    private static boolean useVault;
    private static final ThreadLocal<String> sourceContext = new ThreadLocal<>();

    public static void setSourceContext(String source) {
        sourceContext.set(source);
    }

    public static String getSourceContext() {
        return sourceContext.get();
    }

    public static void clearSourceContext() {
        sourceContext.remove();
    }

    public static void setup(PrismSurvival instance, boolean configUseVault) {
        plugin = instance;
        useVault = configUseVault;

        if (useVault) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().warning("Vault not found! Falling back to internal economy.");
                useVault = false;
                return;
            }
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                    .getRegistration(Economy.class);
            if (rsp == null) {
                plugin.getLogger()
                        .warning("Vault found, but no Economy provider registered! Falling back to internal economy.");
                useVault = false;
                return;
            }
            vaultEcon = rsp.getProvider();
            plugin.getLogger().info("EconomyHandler hooked into Vault.");
        } else {
            plugin.getLogger().info("EconomyHandler using internal economy (Configured or Fallback).");
        }
    }

    public static boolean chargePlayer(Player player, double amount) {
        return chargePlayer(player, amount, "Auction");
    }

    public static boolean chargePlayer(Player player, double amount, String source) {
        if (useVault && vaultEcon != null) {
            if (vaultEcon.getBalance((OfflinePlayer) player) < amount) {
                return false;
            }
            setSourceContext(source);
            try {
                EconomyResponse res = vaultEcon.withdrawPlayer((OfflinePlayer) player, amount);
                return res.transactionSuccess();
            } finally {
                clearSourceContext();
            }
        } else {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data == null)
                return false;
            if (data.getMoney() < amount) {
                return false;
            }
            data.removeMoney(amount, source);
            plugin.getPlayerDataManager().saveMoneyAsync(player.getUniqueId(), data);
            return true;
        }
    }

    public static void depositPlayer(Player player, double amount) {
        depositPlayer(player, amount, "General");
    }

    public static void depositPlayer(Player player, double amount, String source) {
        depositOfflinePlayer(player, amount, source);
    }

    public static void depositOfflinePlayer(OfflinePlayer player, double amount) {
        depositOfflinePlayer(player, amount, "General");
    }

    public static void depositOfflinePlayer(OfflinePlayer player, double amount, String source) {
        if (useVault && vaultEcon != null) {
            setSourceContext(source);
            try {
                vaultEcon.depositPlayer(player, amount);
            } finally {
                clearSourceContext();
            }
        } else {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null) {
                data.addMoney(amount, source);
                plugin.getPlayerDataManager().saveMoneyAsync(player.getUniqueId(), data);
            }
        }
    }

    public static boolean depositByName(String playerName, double amount) {
        return depositByName(playerName, amount, "Auction");
    }

    public static boolean depositByName(String playerName, double amount, String source) {
        if (useVault && vaultEcon != null) {
            OfflinePlayer off = plugin.getServer().getOfflinePlayer(playerName);
            setSourceContext(source);
            try {
                EconomyResponse res = vaultEcon.depositPlayer(off, amount);
                return res != null && res.transactionSuccess();
            } finally {
                clearSourceContext();
            }
        } else {
            OfflinePlayer off = plugin.getServer().getOfflinePlayer(playerName);
            if (off.hasPlayedBefore() || off.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().get(off.getUniqueId());
                if (data != null) {
                    data.addMoney(amount, source);
                    plugin.getPlayerDataManager().saveMoneyAsync(off.getUniqueId(), data);
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean usingVault() {
        return useVault && vaultEcon != null;
    }
}
