package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandHideListener implements Listener {

    private final PrismSurvival plugin;
    private final Set<String> allowedCommands;
    private boolean whitelistEnabled;

    public CommandHideListener(PrismSurvival plugin) {
        this.plugin = plugin;
        this.allowedCommands = new HashSet<>();
        loadConfig();
    }

    /**
     * Load command whitelist from config
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getSurvivalConfig();
        whitelistEnabled = config.getBoolean("command-whitelist.enabled", true);

        allowedCommands.clear();
        List<String> commands = config.getStringList("command-whitelist.allowed-commands");
        for (String cmd : commands) {
            // Store in lowercase for case-insensitive matching
            allowedCommands.add(cmd.toLowerCase());
        }

        plugin.getLogger().info("Command whitelist loaded: " + allowedCommands.size() + " commands allowed");
    }

    /**
     * Step 1: Hide from the Tab-Complete list (Visual)
     */
    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        // OPs should still see everything for debugging
        if (event.getPlayer().isOp())
            return;

        // If whitelist is disabled, don't filter
        if (!whitelistEnabled)
            return;

        Collection<String> commands = event.getCommands();
        org.bukkit.entity.Player player = event.getPlayer();

        // Remove commands that are NOT in the whitelist or start with emporiumsurvival:
        // Note: In this event, commands do NOT start with "/"
        commands.removeIf(command -> {
            String lowerCmd = command.toLowerCase();

            // Explicitly hide emporiumsurvival and minecraft prefixed commands
            if (lowerCmd.startsWith("prismcore:") || lowerCmd.startsWith("minecraft:")) {
                return true;
            }

            // Extract base command (before ":")
            String baseCommand = lowerCmd;
            if (lowerCmd.contains(":")) {
                baseCommand = lowerCmd.substring(lowerCmd.indexOf(":") + 1);
            }

            // Check if this command is in the whitelist AND player has permission
            return !isAllowed(player, baseCommand);
        });
    }

    /**
     * Step 2: Block execution if they try to type it manually
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Allow OPs to bypass
        if (event.getPlayer().isOp())
            return;

        // If whitelist is disabled, don't block
        if (!whitelistEnabled)
            return;

        String message = event.getMessage().toLowerCase();

        // Extract command name (remove leading / and arguments)
        String commandName = message.substring(1); // Remove /
        if (commandName.contains(" ")) {
            commandName = commandName.substring(0, commandName.indexOf(" "));
        }

        // Remove plugin prefix if present (e.g. "emporiumsurvival:help" -> "help")
        if (commandName.contains(":")) {
            commandName = commandName.substring(commandName.indexOf(":") + 1);
        }

        if (!isAllowed(event.getPlayer(), commandName)) {
            event.setCancelled(true);

            // If the command has an explicitly empty permission message, fail silently
            org.bukkit.command.PluginCommand pluginCommand = org.bukkit.Bukkit.getPluginCommand(commandName);
            if (pluginCommand != null) {
                String permMsg = pluginCommand.getPermissionMessage();
                if (permMsg != null && permMsg.isEmpty()) {
                    return;
                }
            }

            // Chat message
            event.getPlayer()
                    .sendMessage(
                            org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cThis command does not exist."));

            // Action bar
            String actionBarMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&cThis command does not exist.");
            net.md_5.bungee.api.ChatMessageType actionBarType = net.md_5.bungee.api.ChatMessageType.ACTION_BAR;
            event.getPlayer().spigot().sendMessage(actionBarType,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionBarMsg));

            // Sound: Villager No
            event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    /**
     * Check if a command is in the whitelist AND if the player has permission
     */
    private boolean isAllowed(org.bukkit.entity.Player player, String command) {
        // 1. Must be in whitelist
        if (!allowedCommands.contains(command.toLowerCase())) {
            return false;
        }

        // 2. If it is in whitelist, check if the command has a specific permission
        // associated with it
        org.bukkit.command.PluginCommand pluginCommand = org.bukkit.Bukkit.getPluginCommand(command);
        if (pluginCommand != null) {
            String perm = pluginCommand.getPermission();
            if (perm != null && !perm.isEmpty()) {
                // If player lacks the permission, treat as NOT allowed (hidden)
                if (!player.hasPermission(perm)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Reload the whitelist (can be called from plugin reload)
     */
    public void reload() {
        loadConfig();
    }
}