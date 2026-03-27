package com.h2ph.utils.internal;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

public class LicenseVerification {

    public static boolean verify(Falcon plugin) {
        String licenseKey = plugin.getSurvivalConfig().getString("license-key");
        
        if (licenseKey == null || licenseKey.isEmpty() || licenseKey.equals("YOUR_KEY_HERE")) {
            ConsoleCommandSender console = plugin.getServer().getConsoleSender();
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', " &c&l[Falcon] &fLicense key missing in config.yml!"));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', " &fPlugin is now &c&lSHUTTING DOWN&f."));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        if (!com.lukittu.api.LukittuAPI.verify(licenseKey)) {
            ConsoleCommandSender console = plugin.getServer().getConsoleSender();
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', " &c&l[Falcon] &fInvalid license key!"));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', " &fPlugin is now &c&lSHUTTING DOWN&f."));
            console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        com.lukittu.api.LukittuAPI.setupHeartbeat(plugin, licenseKey);
        return true;
    }
}
