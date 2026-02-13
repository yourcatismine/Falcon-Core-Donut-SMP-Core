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
        if (useVault && vaultEcon != null) {
            if (vaultEcon.getBalance((OfflinePlayer) player) < amount) {
                return false;
            }
            EconomyResponse res = vaultEcon.withdrawPlayer((OfflinePlayer) player, amount);
            return res.transactionSuccess();
        } else {
            // Internal Economy
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data == null)
                return false;
            if (data.getMoney() < amount) {
                return false;
            }
            data.removeMoney(amount, "Auction");
            // Internal economy usually auto-saves or logic is handled in manager,
            // but we might want to trigger a save or update if necessary.
            // PlayerDataManager saves on quit/interval.
            return true;
        }
    }

    public static void depositPlayer(Player player, double amount) {
        if (useVault && vaultEcon != null) {
            vaultEcon.depositPlayer((OfflinePlayer) player, amount);
        } else {
            // Internal Economy
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null) {
                data.addMoney(amount, "Auction");
            }
        }
    }

    public static boolean depositByName(String playerName, double amount) {
        if (useVault && vaultEcon != null) {
            OfflinePlayer off = plugin.getServer().getOfflinePlayer(playerName);
            EconomyResponse res = vaultEcon.depositPlayer(off, amount);
            return res != null && res.transactionSuccess();
        } else {
            // Internal Economy
            // Need UUID for PlayerDataManager.
            // Try to resolve UUID from name via Bukkit (might be offline).
            // This is tricky if offline.
            OfflinePlayer off = plugin.getServer().getOfflinePlayer(playerName);
            if (off.hasPlayedBefore() || off.isOnline()) {
                PlayerData data = plugin.getPlayerDataManager().get(off.getUniqueId());
                // getPlayerData might load from DB if not cached
                if (data != null) {
                    data.addMoney(amount, "Auction");
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
