package com.h2ph.commands.player;

import com.h2ph.Falcon;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class MinigamesCommand implements CommandExecutor {

    private final Falcon plugin;

    public MinigamesCommand(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        String serverName = getMinigameServerName();
        if (serverName == null) {
            player.sendMessage(ChatColor.RED + "Minigame server is not configured!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Transferring you to the minigame server...");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        transferPlayerToServer(player, serverName);

        return true;
    }

    private String getMinigameServerName() {
        try {
            File settingsFile = new File(plugin.getDataFolder(), "servers/minigames/settings.yml");
            if (!settingsFile.exists()) {
                settingsFile.getParentFile().mkdirs();
                plugin.saveResource("servers/minigames/settings.yml", false);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(settingsFile);
            List<String> settings = config.getStringList("settings");
            
            if (settings != null && !settings.isEmpty()) {
                return settings.get(0);
            }
            
            plugin.getLogger().warning("No server names found in minigame settings");
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error reading minigame server settings: " + e.getMessage());
            return null;
        }
    }

    private void transferPlayerToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName);
            out.close();

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending player " + player.getName() + " to server " + serverName + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Failed to transfer to minigame server. Please try again later.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}