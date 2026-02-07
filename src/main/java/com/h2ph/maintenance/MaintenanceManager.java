package com.h2ph.maintenance;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MaintenanceManager {

    private final PrismSurvival plugin;
    private boolean maintenanceEnabled;
    private File dataFile;
    private FileConfiguration dataConfig;

    public MaintenanceManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "maintenance/data.yml");

        // Ensure config.yml is saved on startup
        saveDefaultConfig();

        loadState();
    }

    private void saveDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "maintenance/config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("maintenance/config.yml", false);
        }
    }

    private void loadState() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create maintenance data file!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.maintenanceEnabled = dataConfig.getBoolean("enabled", false);
    }

    public void setMaintenance(boolean enabled) {
        this.maintenanceEnabled = enabled;
        dataConfig.set("enabled", enabled);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save maintenance state!");
            e.printStackTrace();
        }

        if (enabled) {
            kickPlayers();
        }
    }

    public boolean isMaintenanceEnabled() {
        return maintenanceEnabled;
    }

    public void kickPlayers() {
        FileConfiguration config = getMaintenanceConfig();
        List<String> messageLines = config.getStringList("disconnect-message");
        String kickMessage = ChatColor.translateAlternateColorCodes('&', String.join("\n", messageLines));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!canBypass(player)) {
                player.kickPlayer(kickMessage);
            }
        }
    }

    public boolean canBypass(Player player) {
        return player.isOp() || player.hasPermission("prismcore.maintenance.bypass");
    }

    public FileConfiguration getMaintenanceConfig() {
        File file = new File(plugin.getDataFolder(), "maintenance/config.yml");
        if (!file.exists()) {
            plugin.saveResource("maintenance/config.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
