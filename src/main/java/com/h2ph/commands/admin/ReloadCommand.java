package com.h2ph.commands.admin;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final PrismSurvival plugin;

    public ReloadCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    private String toSmallCaps(String input) {
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ";
        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = normal.indexOf(c);
            builder.append(index != -1 ? small.charAt(index) : c);
        }
        return builder.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prism.admin.reload")) {
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("no permission"));
            return true;
        }

        long start = System.currentTimeMillis();

        // Reload Plugin Configs
        try {
            // Main Config
            plugin.loadSurvivalConfig();

            // Chat Filter
            plugin.loadChatFilterConfig();

            // Update Data (Optional)
            plugin.loadUpdateFromConfig();

            // RTP Configs
            plugin.loadRTPConfig();
            plugin.loadGlobalRTPConfig();

            // Offend Module
            if (plugin.getOffendPlugin() != null) {
                plugin.getOffendPlugin().loadOffendConfig();
            }

            // AFK Manager
            if (plugin.getAfkManager() != null) {
                plugin.getAfkManager().loadConfig();
                plugin.getAfkManager().loadRegions();
            }

            // Shards Manager
            if (plugin.getShardsManager() != null) {
                plugin.getShardsManager().reloadConfig();
            }

            // Shop
            if (plugin.getShopCommand() != null) {
                plugin.getShopCommand().reload();
            }

            // Rules
            if (plugin.getRulesCommand() != null) {
                plugin.getRulesCommand().loadConfig();
            }

            // Advisor
            plugin.loadAdvisorFromConfig();

            // Spawn Manager
            if (plugin.getSpawnManager() != null) {
                plugin.getSpawnManager().reloadConfig();
            }

            // AntiXray - Removed
            // com.prismcore.antixray.PrismCoreAntiXPlugin antiXray =
            // com.prismcore.antixray.PrismCoreAntiXPlugin
            // .getInstance();
            // if (antiXray != null) {
            // antiXray.reloadConfig();
            // antiXray.loadConfigValues();
            // }

            long time = System.currentTimeMillis() - start;

            sender.sendMessage("");
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("prism") + " " + ChatColor.GREEN
                    + toSmallCaps("reloaded successfully") + ChatColor.GRAY + " (" + time + "ms)");
            sender.sendMessage("");

        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("prism") + " " + ChatColor.RED
                    + toSmallCaps("reload failed (check console)"));
            e.printStackTrace();
        }

        return true;
    }
}
