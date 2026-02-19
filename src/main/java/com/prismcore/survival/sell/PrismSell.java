package com.prismcore.survival.sell;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.sell.commands.PrismSellCommand;
import com.prismcore.survival.sell.commands.SellCommand;
import com.prismcore.survival.sell.data.PlayerDataManagerDB;
import com.prismcore.survival.sell.database.DatabaseManager;
import com.prismcore.survival.sell.economy.PrismDirectEconomyProvider;
import com.prismcore.survival.sell.economy.SellEconomyProvider;
import com.prismcore.survival.sell.economy.VaultEconomyProvider;
import com.prismcore.survival.sell.gui.ProgressGUI;
import com.prismcore.survival.sell.gui.SellGUI;
import com.prismcore.survival.sell.managers.GUIManager;
import com.prismcore.survival.sell.managers.PricesManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;

public class PrismSell {

    private final PrismSurvival plugin;
    private SellEconomyProvider economy;
    private DatabaseManager databaseManager;
    private PlayerDataManagerDB playerDataManager;
    private PricesManager pricesManager;
    private GUIManager guiManager;
    private SellGUI sellGUI;
    private ProgressGUI progressGUI;

    private File configFile;
    private FileConfiguration config;

    public PrismSell(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }

    public org.bukkit.Server getServer() {
        return plugin.getServer();
    }

    public java.io.InputStream getResource(String filename) {
        return plugin.getResource(filename);
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "economy/sell/config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        // We assume resources are in "economy/sell/" folder in jar
        java.io.InputStream defConfigStream = plugin.getResource("economy/sell/config.yml");
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defConfigStream, java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "economy/sell/config.yml");
        }
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try (java.io.InputStream in = plugin.getResource("economy/sell/config.yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, configFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    plugin.getLogger().warning("Could not find economy/sell/config.yml resource!");
                }
            } catch (java.io.IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + configFile, e);
            }
        }
    }

    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (java.io.IOException ex) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void onEnable() {
        this.saveDefaultConfig();
        if (!setupEconomy()) {
            plugin.getLogger().warning("No economy found for PrismSell!");
        }

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.pricesManager = new PricesManager(this);
        this.guiManager = new GUIManager(this);
        this.playerDataManager = new PlayerDataManagerDB(this);
        this.sellGUI = new SellGUI(this);
        this.progressGUI = new ProgressGUI(this);

        plugin.getCommand("sell").setExecutor(new SellCommand(this));

        // Register prismsell command (renamed from donutsell)
        PrismSellCommand adminCmd = new PrismSellCommand(this);
        plugin.getCommand("prismsell").setExecutor(adminCmd);

        plugin.getServer().getPluginManager().registerEvents(this.sellGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(this.progressGUI, plugin);

        plugin.getLogger().info("PrismSell has been enabled!");
    }

    public void onDisable() {
        if (this.playerDataManager != null) {
            this.playerDataManager.saveAllData();
        }
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
        plugin.getLogger().info("PrismSell has been disabled!");
    }

    private boolean setupEconomy() {
        // Load economy config to check if Vault (PrismEconomy) is enabled
        File ecoConfig = new File(plugin.getDataFolder(), "economy/config.yml");
        boolean prismEcoEnabled = true;

        if (ecoConfig.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(ecoConfig);
            prismEcoEnabled = config.getBoolean("vault-enabled", true);
        }

        if (prismEcoEnabled) {
            this.economy = new PrismDirectEconomyProvider(this);
            plugin.getLogger().info("PrismSell using Prism Direct Economy.");
            return true;
        } else {
            // Check for Vault
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                    .getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            this.economy = new VaultEconomyProvider(rsp.getProvider());
            plugin.getLogger().info("PrismSell using Vault Economy (" + this.economy.getName() + ").");
            return true;
        }
    }

    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    public PrismSurvival getPlugin() {
        return plugin;
    }

    public SellEconomyProvider getEconomy() {
        return this.economy;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public PlayerDataManagerDB getPlayerDataManager() {
        return this.playerDataManager;
    }

    public PricesManager getPricesManager() {
        return this.pricesManager;
    }

    public GUIManager getGUIManager() {
        return this.guiManager;
    }

    public SellGUI getSellGUI() {
        return this.sellGUI;
    }

    public ProgressGUI getProgressGUI() {
        return this.progressGUI;
    }
}
