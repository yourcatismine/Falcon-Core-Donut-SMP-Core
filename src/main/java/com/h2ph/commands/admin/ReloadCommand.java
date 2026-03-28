package com.h2ph.commands.admin;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Falcon plugin;

    public ReloadCommand(Falcon plugin) {
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
        if (!sender.hasPermission("falcon.reload")) {
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("no permission"));
            return true;
        }

        long start = System.currentTimeMillis();

        try {
            plugin.loadSurvivalConfig();

            plugin.loadChatFilterConfig();
            if (plugin.getChatFilter() != null) {
                plugin.getChatFilter().loadConfigAndPatterns();
            }

            if (plugin.getCommandHideListener() != null) {
                plugin.getCommandHideListener().reload();
            }

            plugin.loadUpdateFromConfig();

            plugin.loadRTPConfig();
            plugin.loadGlobalRTPConfig();

            if (plugin.getOffendPlugin() != null) {
                plugin.getOffendPlugin().loadOffendConfig();
            }

            if (plugin.getAfkManager() != null) {
                plugin.getAfkManager().loadConfig();
                plugin.getAfkManager().loadRegions();
            }

            if (plugin.getShardsManager() != null) {
                plugin.getShardsManager().reloadConfig();
            }

            if (plugin.getShopCommand() != null) {
                plugin.getShopCommand().reload();
            }

            if (plugin.getRulesCommand() != null) {
                plugin.getRulesCommand().loadConfig();
            }

            plugin.loadAdvisorFromConfig();

            if (plugin.getSpawnManager() != null) {
                plugin.getSpawnManager().reloadConfig();
            }

            if (plugin.getKeyAllManager() != null) {
                plugin.getKeyAllManager().loadConfig();
            }

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().loadConfig();
            }

            if (plugin.getTabListManager() != null) {
                plugin.getTabListManager().reloadTabList();
            }

            if (plugin.getLimiterConfig() != null) {
                plugin.getLimiterConfig().loadConfig();
            }

            if (plugin.getVoidManager() != null) {
                plugin.getVoidManager().loadRegions();
            }

            if (plugin.getToolsManager() != null) {
                plugin.getToolsManager().reloadConfig();
            }

            if (plugin.getFalconSell() != null) {
                plugin.getFalconSell().reloadConfig();
            }

            if (plugin.getOrdersModule() != null && plugin.getOrdersModule().cfg() != null) {
                plugin.getOrdersModule().cfg().reload();
            }

            if (plugin.getCrateLocationRegistry() != null) {
                plugin.getCrateLocationRegistry().load();
            }

            if (plugin.getBountyManager() != null) {
                plugin.getBountyManager().load();
            }

            if (plugin.getMediaCommand() != null) {
                plugin.getMediaCommand().loadConfig();
            }

            if (plugin.getDeathMessageManager() != null) {
                plugin.getDeathMessageManager().reload();
            }

            if (plugin.getRedstoneManager() != null) {
                plugin.getRedstoneManager().reloadConfig();
            }


            long time = System.currentTimeMillis() - start;

            sender.sendMessage("");
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("falcon") + " " + ChatColor.GREEN
                    + toSmallCaps("reloaded successfully") + ChatColor.GRAY + " (" + time + "ms)");
            sender.sendMessage("");

        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("falcon") + " " + ChatColor.RED
                    + toSmallCaps("reload failed (check console)"));
            e.printStackTrace();
        }

        return true;
    }
}
