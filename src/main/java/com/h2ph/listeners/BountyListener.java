package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.text.DecimalFormat;
import java.util.UUID;

public class BountyListener implements Listener {

    private final PrismSurvival plugin;
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public BountyListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        if (killer == null)
            return;
        if (killer.equals(victim))
            return;

        UUID victimId = victim.getUniqueId();
        if (plugin.getBountyManager().hasBounty(victimId)) {
            com.h2ph.teams.Team vTeam = plugin.getTeamManager().getPlayerTeam(victimId);
            com.h2ph.teams.Team kTeam = plugin.getTeamManager().getPlayerTeam(killer.getUniqueId());

            if (vTeam != null && kTeam != null && vTeam.getId().equals(kTeam.getId())) {
                killer.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&cYou cannot claim a bounty on your teammate!"));
                return;
            }

            double amount = plugin.getBountyManager().getBounty(victimId);
            plugin.getBountyManager().removeBounty(victimId);

            net.milkbowl.vault.economy.Economy econ = plugin.getEconomy();
            if (econ != null) {
                econ.depositPlayer(killer, amount);
            } else {
                PlayerData killerData = plugin.getPlayerDataManager().get(killer.getUniqueId());
                if (killerData == null)
                    killerData = plugin.getPlayerDataManager().loadPlayer(killer.getUniqueId());

                killerData.addMoney(amount, "Bounty claim on " + victim.getName());
                plugin.getPlayerDataManager().savePlayerAsync(killer.getUniqueId());
            }

            String amountFormatted = formatNumber(amount);

            killer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You claimed a bounty of &a$"
                    + amountFormatted + " &7for killing &#A9833D" + victim.getName() + "&7!"));
        }
    }

    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "t");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "b");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "m");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "k");
        } else {
            return DF.format(Math.floor(number * 10) / 10.0);
        }
    }

    private String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }
}
