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
            if (plugin.getChatFilter() != null) {
                plugin.getChatFilter().loadConfigAndPatterns();
            }

            // Command Whitelist (Hide Listener)
            if (plugin.getCommandHideListener() != null) {
                plugin.getCommandHideListener().reload();
            }

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

            // KeyAll Manager
            if (plugin.getKeyAllManager() != null) {
                plugin.getKeyAllManager().loadConfig();
            }

            // Scoreboard Manager
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().loadConfig();
            }

            // TAB List Manager
            if (plugin.getTabListManager() != null) {
                plugin.getTabListManager().reloadTabList();
            }

            // Limiter Config
            if (plugin.getLimiterConfig() != null) {
                plugin.getLimiterConfig().loadConfig();
            }

            // Void Manager
            if (plugin.getVoidManager() != null) {
                plugin.getVoidManager().loadRegions();
            }

            // Tools Manager
            if (plugin.getToolsManager() != null) {
                plugin.getToolsManager().reloadConfig();
            }

            // Sell Module
            if (plugin.getPrismSell() != null) {
                plugin.getPrismSell().reloadConfig();
            }

            // Orders Module
            if (plugin.getOrdersModule() != null && plugin.getOrdersModule().cfg() != null) {
                plugin.getOrdersModule().cfg().reload();
            }

            // Crate Location Registry
            if (plugin.getCrateLocationRegistry() != null) {
                plugin.getCrateLocationRegistry().load();
            }

            // Bounty Manager
            if (plugin.getBountyManager() != null) {
                plugin.getBountyManager().load();
            }

            // Media Command
            if (plugin.getMediaCommand() != null) {
                plugin.getMediaCommand().loadConfig();
            }

            // Death Message Manager
            if (plugin.getDeathMessageManager() != null) {
                plugin.getDeathMessageManager().reload();
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
