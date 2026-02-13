package com.h2ph;

import com.prismcore.survival.manager.PlayerDataManager;
import com.prismcore.survival.scheduler.SchedulerAdapter;
import com.h2ph.commands.economy.ShopCommand;
import com.h2ph.commands.player.QuickGameMode;
import org.bukkit.plugin.java.JavaPlugin;

public class PrismSurvival extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private SchedulerAdapter schedulerAdapter;
    private ShopCommand shopCommand;
    private com.h2ph.commands.player.RulesCommand rulesCommand;

    private static PrismSurvival instance;

    public static PrismSurvival getInstance() {
        return instance;
    }

    private com.h2ph.afk.AFKManager afkManager;
    private org.bukkit.configuration.file.FileConfiguration survivalConfig;
    private com.h2ph.commands.admin.moderations.OffendPlugin offendPlugin;
    private com.h2ph.api.ApiServer apiServer;
    private com.prismcore.survival.manager.KeyAllManager keyAllManager;
    private com.prismcore.survival.manager.CarouselManager carouselManager;
    private com.prismcore.survival.manager.CrateLocationRegistry crateLocationRegistry;
    private com.prismcore.survival.manager.CrateEffectsManager crateEffectsManager;
    private com.prismcore.survival.manager.SpawnManager spawnManager;
    private com.prismcore.survival.manager.TeleportManager teleportManager;
    private com.prismcore.survival.shards.ShardsManager shardsManager;
    private com.prismcore.survival.auction.AuctionController auctionController;

    private com.h2ph.commands.admin.duels.DuelStatsManager duelStatsManager;
    private com.h2ph.commands.admin.duels.DuelArenaManager duelArenaManager;

    private com.h2ph.maintenance.MaintenanceManager maintenanceManager;
    private com.prismcore.survival.orders.OrdersModule ordersModule;
    private com.prismcore.survival.manager.ActivityLogger activityLogger;
    private com.prismcore.survival.manager.InventoryLogManager inventoryLogManager;
    private com.h2ph.managers.RedstoneManager redstoneManager;
    private com.prismcore.survival.manager.HazardManager hazardManager;
    private com.prismcore.survival.manager.BalanceLogger balanceLogger;

    @Override
    public void onLoad() {
        // Initialize AntiXray and PacketEvents

    }

    @Override
    public void onEnable() {
        instance = this;
        // Save all default resources
        saveAllResources();

        // Load Survival Config
        loadSurvivalConfig();

        // Register Command Hide Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CommandHideListener(this), this);

        // Register MessageHider
        new com.prismcore.survival.survival.MessageHider(this);

        // Register ItemMerger (Aggressive Stacking)
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.survival.ItemMerger(this), this);

        // Register ChatFilter
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.survival.ChatFilter(this), this);

        // Register ChatFormatter
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.survival.ChatFormatter(this), this);

        // Load persistent update
        loadUpdateFromConfig();

        // Register Update Command and Listener
        getCommand("update").setExecutor(new com.h2ph.commands.admin.updates.UpdateCommand(this));
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.UpdateBookListener(this), this);

        // Load advisor content
        loadAdvisorFromConfig();

        // Register RTP Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.RTPListener(), this);

        // Register Live Command Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveCommandListener(this), this);

        // Register Live Sign Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        // Initialize managers
        this.playerDataManager = new PlayerDataManager(this);
        this.schedulerAdapter = new SchedulerAdapter(this);
        this.keyAllManager = new com.prismcore.survival.manager.KeyAllManager(this);
        this.carouselManager = new com.prismcore.survival.manager.CarouselManager(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateEffectsManager = new com.prismcore.survival.manager.CrateEffectsManager(this);
        this.spawnManager = new com.prismcore.survival.manager.SpawnManager(this);
        this.teleportManager = new com.prismcore.survival.manager.TeleportManager(this);

        // Register Live Sign Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        // Register Crate Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CrateListener(this), this);

        // Register Player Connection Listener (For saving data)
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.PlayerConnectionListener(this), this);

        this.activityLogger = new com.prismcore.survival.manager.ActivityLogger(this);
        this.inventoryLogManager = new com.prismcore.survival.manager.InventoryLogManager(this);

        // Register Inventory Log Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.InventoryLogListener(this), this);

        // Initialize managers
        this.redstoneManager = new com.h2ph.managers.RedstoneManager(this);
        this.hazardManager = new com.prismcore.survival.manager.HazardManager(this);
        this.balanceLogger = new com.prismcore.survival.manager.BalanceLogger(this);
        this.balanceLogger.start();

        // Register shop command
        this.shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getServer().getPluginManager().registerEvents(shopCommand, this);

        // Register Redstone Command
        getCommand("redstone").setExecutor(new com.h2ph.commands.admin.RedstoneCommand(this));

        // Register OffendPlugin (Moderation)
        this.offendPlugin = new com.h2ph.commands.admin.moderations.OffendPlugin(this);

        // Register Tools Manager
        new com.prismcore.survival.tools.ToolsManager(this);

        // Register SignInput Utility
        this.signInput = new com.h2ph.utils.SignInput(this);
        getServer().getPluginManager().registerEvents(this.signInput, this);
        getCommand("tools").setExecutor(
                new com.prismcore.survival.tools.ToolCommand(com.prismcore.survival.tools.ToolsManager.getInstance()));
        getCommand("tools").setTabCompleter(
                new com.prismcore.survival.tools.ToolCommand(com.prismcore.survival.tools.ToolsManager.getInstance()));

        // Register shards admin command
        com.h2ph.commands.admin.economy.ShardsCommand shardsCommand = new com.h2ph.commands.admin.economy.ShardsCommand(
                this);
        getCommand("shards").setExecutor(shardsCommand);
        getCommand("shards").setTabCompleter(shardsCommand);

        // Register Crate Command
        com.h2ph.commands.admin.crates.CrateCommand crateCommand = new com.h2ph.commands.admin.crates.CrateCommand(
                this);
        getCommand("crate").setExecutor(crateCommand);
        getCommand("crate").setTabCompleter(crateCommand);

        // Register Key Command
        com.h2ph.commands.admin.crates.KeyCommand keyCommand = new com.h2ph.commands.admin.crates.KeyCommand(this);
        getCommand("key").setExecutor(keyCommand);
        getCommand("key").setTabCompleter(keyCommand);

        // Register Billford Command
        com.h2ph.commands.admin.economy.BillfordCommand billfordCommand = new com.h2ph.commands.admin.economy.BillfordCommand(
                this);
        getCommand("billford").setExecutor(billfordCommand);
        getCommand("billford").setTabCompleter(billfordCommand);
        getServer().getPluginManager().registerEvents(billfordCommand, this);

        // Register Baltop Command
        com.h2ph.commands.admin.economy.BaltopCommand baltopCommand = new com.h2ph.commands.admin.economy.BaltopCommand(
                this);
        getCommand("baltop").setExecutor(baltopCommand);
        getServer().getPluginManager().registerEvents(baltopCommand, this);

        // Register Balance Command
        com.h2ph.commands.economy.BalanceCommand balanceCommand = new com.h2ph.commands.economy.BalanceCommand(this);
        getCommand("balance").setExecutor(balanceCommand);
        getCommand("balance").setTabCompleter(balanceCommand);

        // Register Pay Command
        com.h2ph.commands.economy.PayCommand payCommand = new com.h2ph.commands.economy.PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        // Load economy config to check if enabled
        java.io.File ecoConfig = new java.io.File(getDataFolder(), "economy/config.yml");
        org.bukkit.configuration.file.FileConfiguration ecoConfigYaml = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(ecoConfig);
        boolean vaultEnabled = ecoConfigYaml.getBoolean("vault-enabled", true);

        // Register economy command only if enabled
        if (vaultEnabled) {
            com.h2ph.commands.admin.economy.EconomyCommand economyCommand = new com.h2ph.commands.admin.economy.EconomyCommand(
                    this);
            getCommand("economy").setExecutor(economyCommand);
            getCommand("economy").setTabCompleter(economyCommand);
        } else {
            getLogger().info("Economy Command disabled in config (vault-enabled: false).");
            getCommand("economy").setExecutor((sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "PrismEconomy is currently disabled.");
                return true;
            });
            getCommand("economy").setTabCompleter((sender, command, label, args) -> java.util.Collections.emptyList());
        }

        // Initialize AFK Manager
        this.afkManager = new com.h2ph.afk.AFKManager(this);
        this.afkManager.startTask();

        // Initialize Shards Manager (Passive)
        this.shardsManager = new com.prismcore.survival.shards.ShardsManager(this);

        // Register AFK Command (Requires WorldEdit)
        if (getServer().getPluginManager().getPlugin("WorldEdit") != null ||
                getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            getCommand("setafk").setExecutor(new com.h2ph.commands.admin.afk.SetAFKCommand(this));

            // Register Player AFK Command
            com.h2ph.commands.player.afk.AFKCommand afkCommand = new com.h2ph.commands.player.afk.AFKCommand(this);
            getCommand("afk").setExecutor(afkCommand);
            getCommand("afk").setTabCompleter(afkCommand);
            getServer().getPluginManager().registerEvents(afkCommand, this);

            getLogger().info("WorldEdit found! AFK region features enabled.");
        } else {
            getLogger().warning("WorldEdit/FAWE not found! /setafk and /afk commands disabled.");
        }

        // Initialize and Enable Auction
        this.auctionController = new com.prismcore.survival.auction.AuctionController(this);
        this.auctionController.enable();

        // Register Sus Command
        com.h2ph.commands.admin.moderations.SusCommand susCommand = new com.h2ph.commands.admin.moderations.SusCommand(
                this);
        getCommand("sus").setExecutor(susCommand);
        getServer().getPluginManager().registerEvents(susCommand, this);

        // Register Spectator Command
        com.h2ph.commands.admin.moderations.SpectatorMode spectatorMode = new com.h2ph.commands.admin.moderations.SpectatorMode(
                this);
        getCommand("gmsp").setExecutor(spectatorMode);
        getServer().getPluginManager().registerEvents(spectatorMode, this);

        // Initialize Duel Managers
        this.duelStatsManager = new com.h2ph.commands.admin.duels.DuelStatsManager(this);
        this.duelArenaManager = new com.h2ph.commands.admin.duels.DuelArenaManager(this, duelStatsManager);

        // Register Duel Command
        // Register Duel Command (Requires WorldEdit)
        if (getServer().getPluginManager().getPlugin("WorldEdit") != null ||
                getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            com.h2ph.commands.admin.duels.DuelCommand duelCommand = new com.h2ph.commands.admin.duels.DuelCommand(this,
                    duelArenaManager);
            getCommand("duel").setExecutor(duelCommand);
            getCommand("duel").setTabCompleter(duelCommand);
            getServer().getPluginManager().registerEvents(new com.h2ph.listeners.DuelGUIListener(), this);
            getServer().getPluginManager().registerEvents(new com.h2ph.listeners.DuelGameListener(duelArenaManager),
                    this);
        } else {
            getLogger().warning("WorldEdit/FAWE not found! /duel command disabled.");
        }

        // Register RTP Command
        com.h2ph.commands.player.RTPCommand rtpCmd = new com.h2ph.commands.player.RTPCommand();
        getCommand("rtp").setExecutor(rtpCmd);
        getCommand("rtp").setTabCompleter(rtpCmd);

        // Register Rules Command
        this.rulesCommand = new com.h2ph.commands.player.RulesCommand(this);
        getCommand("rules").setExecutor(rulesCommand);
        getServer().getPluginManager().registerEvents(rulesCommand, this);

        // Register Media Command
        com.h2ph.commands.player.MediaCommand mediaCommand = new com.h2ph.commands.player.MediaCommand(this);
        getCommand("media").setExecutor(mediaCommand);
        getServer().getPluginManager().registerEvents(mediaCommand, this);

        // Register Advisor Command
        com.h2ph.commands.player.AdvisorCommand advisorCmd = new com.h2ph.commands.player.AdvisorCommand(this);
        getCommand("advisor").setExecutor(advisorCmd);
        getCommand("advisor").setTabCompleter(advisorCmd);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.AdvisorListener(this), this);

        // Register Spawn System Commands
        com.h2ph.commands.admin.moderations.SetSpawnCommand setSpawnCmd = new com.h2ph.commands.admin.moderations.SetSpawnCommand(
                this);
        getCommand("setspawn").setExecutor(setSpawnCmd);
        getCommand("setspawn").setTabCompleter(setSpawnCmd);

        com.h2ph.commands.player.SpawnCommand spawnCmd = new com.h2ph.commands.player.SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCmd);
        getCommand("spawn").setTabCompleter(spawnCmd);
        getServer().getPluginManager().registerEvents(spawnCmd, this);

        // Register TPA GUI Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TpaGUIListener(), this);

        // Register TpAuto
        getCommand("tpauto").setExecutor(new com.h2ph.commands.player.TpAutoCommand());
        new com.h2ph.managers.TpAutoManager(this);

        // Register TPA Commands
        com.h2ph.commands.player.TpaCommand tpaCmd = new com.h2ph.commands.player.TpaCommand();
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpa").setTabCompleter(tpaCmd);

        com.h2ph.commands.player.TpaHereCommand tpaHereCmd = new com.h2ph.commands.player.TpaHereCommand();
        getCommand("tpahere").setExecutor(tpaHereCmd);
        getCommand("tpahere").setTabCompleter(tpaHereCmd);

        com.h2ph.commands.player.TpAcceptCommand tpAcceptCmd = new com.h2ph.commands.player.TpAcceptCommand();
        getCommand("tpaccept").setExecutor(tpAcceptCmd);
        getCommand("tpaccept").setTabCompleter(tpAcceptCmd);

        com.h2ph.commands.player.TpaCancelCommand tpaCancelCmd = new com.h2ph.commands.player.TpaCancelCommand();
        getCommand("tpacancel").setExecutor(tpaCancelCmd);
        getCommand("tpacancel").setTabCompleter(tpaCancelCmd);

        com.h2ph.commands.player.TpaDenyCommand tpaDenyCmd = new com.h2ph.commands.player.TpaDenyCommand();
        getCommand("tpadeny").setExecutor(tpaDenyCmd);
        getCommand("tpadeny").setTabCompleter(tpaDenyCmd);

        // Register Msg Command
        com.h2ph.commands.player.MsgCommand msgCmd = new com.h2ph.commands.player.MsgCommand();
        getCommand("msg").setExecutor(msgCmd);
        getCommand("msg").setTabCompleter(msgCmd);

        // Register Settings Command
        com.h2ph.commands.player.SettingsCommand settingsCmd = new com.h2ph.commands.player.SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCmd);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.SettingsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TpaConfirmGUIListener(), this);

        // Register Quick Gamemode Commands
        QuickGameMode quickGameMode = new QuickGameMode();
        getCommand("gmc").setExecutor(quickGameMode);
        getCommand("gms").setExecutor(quickGameMode);
        getCommand("gma").setExecutor(quickGameMode);

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.h2ph.placeholders.PrismPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        // Register Vault Economy
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            if (vaultEnabled) {
                com.h2ph.economy.PrismEconomy economy = new com.h2ph.economy.PrismEconomy(this);
                getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, economy, this,
                        org.bukkit.plugin.ServicePriority.Highest);
                getLogger().info("Registered PrismEconomy as Vault provider!");
            } else {
                getLogger().info("PrismEconomy Vault hook disabled in config.");
            }
        } else {
            getLogger().warning("Vault not found! Economy features will be disabled.");
        }

        // Initialize Maintenance Manager
        this.maintenanceManager = new com.h2ph.maintenance.MaintenanceManager(this);

        // Register Maintenance Command
        getCommand("maintenance").setExecutor(new com.h2ph.maintenance.MaintenanceCommand(this.maintenanceManager));

        // Register Maintenance Listener
        getServer().getPluginManager()
                .registerEvents(new com.h2ph.maintenance.MaintenanceListener(this.maintenanceManager), this);

        // Initialize Orders Module
        this.ordersModule = new com.prismcore.survival.orders.OrdersModule(this);
        this.ordersModule.enable();

        // Initialize and Start API Server
        this.apiServer = new com.h2ph.api.ApiServer(this);
        this.apiServer.start();

        // Register Reload Command
        getCommand("prismreload").setExecutor(new com.h2ph.commands.admin.ReloadCommand(this));

        // Initialize Economy Monitor
        new com.h2ph.economy.EconomyMonitor(this);

        // Wrap Vault Economy if enabled
        if (vaultEnabled && getServer().getPluginManager().getPlugin("Vault") != null) {
            getSchedulerAdapter().runTaskLater(() -> {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                        .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    net.milkbowl.vault.economy.Economy existing = rsp.getProvider();
                    if (!(existing instanceof com.h2ph.economy.EconomyWrapper)) {
                        com.h2ph.economy.EconomyWrapper wrapper = new com.h2ph.economy.EconomyWrapper(existing,
                                com.h2ph.economy.EconomyMonitor.getInstance());

                        // Register new wrapper with Highest priority to shadow others
                        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, wrapper,
                                this,
                                org.bukkit.plugin.ServicePriority.Highest);
                    }
                }
            }, 40); // 2 second delay to ensure all plugins registered their economy
        }

        // Print Startup Banner
        printStartupBanner(vaultEnabled);

        // Register Log4j Filter (Hide 429 Errors)
        try {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                    .getRootLogger();
            rootLogger.addFilter(new com.h2ph.logger.Log4jFilter());
        } catch (Exception e) {
            getLogger().warning("Failed to register Log4j Filter: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {

        if (this.apiServer != null) {
            this.apiServer.stop();
        }

        if (this.ordersModule != null) {
            this.ordersModule.disable();
        }

        if (this.auctionController != null) {
            this.auctionController.disable();
        }

        if (this.signInput != null) {
            this.signInput.cleanup();
        }

        // Save all online players
        if (this.playerDataManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                this.playerDataManager.savePlayer(player.getUniqueId());
            }
        }

        getLogger().info("PrismCore has been disabled!");
        instance = null;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public com.h2ph.afk.AFKManager getAfkManager() {
        return afkManager;
    }

    public com.prismcore.survival.shards.ShardsManager getShardsManager() {
        return shardsManager;
    }

    public com.prismcore.survival.auction.AuctionController getAuctionController() {
        return auctionController;
    }

    public com.h2ph.maintenance.MaintenanceManager getMaintenanceManager() {
        return maintenanceManager;
    }

    public com.h2ph.commands.admin.duels.DuelArenaManager getDuelArenaManager() {
        return duelArenaManager;
    }

    public com.prismcore.survival.orders.OrdersModule getOrdersModule() {
        return ordersModule;
    }

    public com.h2ph.managers.RedstoneManager getRedstoneManager() {
        return redstoneManager;
    }

    public com.prismcore.survival.manager.HazardManager getHazardManager() {
        return hazardManager;
    }

    public com.prismcore.survival.manager.ActivityLogger getActivityLogger() {
        return activityLogger;
    }

    public com.prismcore.survival.manager.InventoryLogManager getInventoryLogManager() {
        return inventoryLogManager;
    }

    public String normalizeKeyName(String keyName) {
        // Normalize key names (e.g., "Common Key" -> "common_key")
        return keyName.toLowerCase().replace(" ", "_");
    }

    private void saveAllResources() {
        // Save Anti-Xray config

        // Save Shop config
        saveResourceSafely("economy/shop/config.yml");

        // Save Shop Categories
        saveResourceSafely("economy/shop/categories/end.yml");
        saveResourceSafely("economy/shop/categories/food.yml");
        saveResourceSafely("economy/shop/categories/gear.yml");
        saveResourceSafely("economy/shop/categories/nether.yml");
        saveResourceSafely("economy/shop/categories/redstone.yml");
        saveResourceSafely("economy/shop/categories/redstone.yml");
        saveResourceSafely("economy/shop/categories/shard.yml");
        saveResourceSafely("survival/AFK/config.yml");
        saveResourceSafely("economy/config.yml");
        saveResourceSafely("rtp/europe/config.yml");
        saveResourceSafely("rtp/europe/config.yml");
        saveResourceSafely("rtp/config.yml");
        saveResourceSafely("crates/keys/config.yml");
    }

    private void saveResourceSafely(String path) {
        if (!new java.io.File(getDataFolder(), path).exists()) {
            try {
                saveResource(path, false);
            } catch (Exception e) {
                getLogger().warning("Failed to save resource: " + path);
            }
        }
    }

    public void loadSurvivalConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/config.yml");
        if (!file.exists()) {
            saveResource("survival/config.yml", false);
        }
        survivalConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getSurvivalConfig() {
        if (survivalConfig == null) {
            loadSurvivalConfig();
        }
        return survivalConfig;
    }

    // --- Update Command Logic ---

    private final java.util.Set<java.util.UUID> updateWriters = new java.util.HashSet<>();
    private java.util.List<String> activeUpdatePages = new java.util.ArrayList<>();
    private long activeUpdateVersion = 0;

    public void markPlayerAsUpdateWriter(java.util.UUID uuid) {
        updateWriters.add(uuid);
    }

    public void unmarkPlayerAsUpdateWriter(java.util.UUID uuid) {
        updateWriters.remove(uuid);
    }

    public boolean isPlayerMarkedAsUpdateWriter(java.util.UUID uuid) {
        return updateWriters.contains(uuid);
    }

    public void setActiveUpdate(java.util.List<String> pages) {
        this.activeUpdatePages = new java.util.ArrayList<>(pages);
        this.activeUpdateVersion = System.currentTimeMillis();
        saveUpdateToConfig();
    }

    public java.util.List<String> getActiveUpdatePages() {
        return activeUpdatePages;
    }

    public long getActiveUpdateVersion() {
        return activeUpdateVersion;
    }

    public boolean hasActiveUpdate() {
        return activeUpdatePages != null && !activeUpdatePages.isEmpty();
    }

    private void saveUpdateToConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/update.yml");
        org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);
        eConfig.set("pages", activeUpdatePages);
        eConfig.set("version", activeUpdateVersion);
        try {
            eConfig.save(file);
        } catch (Exception e) {
            getLogger().warning("Failed to save update.yml");
        }
    }

    public void loadUpdateFromConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/update.yml");
        if (file.exists()) {
            org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);
            if (eConfig.contains("pages")) {
                this.activeUpdatePages = eConfig.getStringList("pages");
            }
            this.activeUpdateVersion = eConfig.getLong("version", 0);
        }
    }

    // --- Advisor Command Logic ---

    private final java.util.Set<java.util.UUID> advisorWriters = new java.util.HashSet<>();
    private java.util.List<String> activeAdvisorPages = new java.util.ArrayList<>();

    public void markPlayerAsAdvisorWriter(java.util.UUID uuid) {
        advisorWriters.add(uuid);
    }

    public void unmarkPlayerAsAdvisorWriter(java.util.UUID uuid) {
        advisorWriters.remove(uuid);
    }

    public boolean isPlayerMarkedAsAdvisorWriter(java.util.UUID uuid) {
        return advisorWriters.contains(uuid);
    }

    public void setActiveAdvisor(java.util.List<String> pages) {
        this.activeAdvisorPages = new java.util.ArrayList<>(pages);
        saveAdvisorToConfig();
    }

    public java.util.List<String> getActiveAdvisorPages() {
        return activeAdvisorPages;
    }

    public boolean hasActiveAdvisor() {
        return activeAdvisorPages != null && !activeAdvisorPages.isEmpty();
    }

    private void saveAdvisorToConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/advisor.yml");
        org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);
        eConfig.set("pages", activeAdvisorPages);
        try {
            eConfig.save(file);
        } catch (Exception e) {
            getLogger().warning("Failed to save advisor.yml");
        }
    }

    public void loadAdvisorFromConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/advisor.yml");
        if (file.exists()) {
            org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);
            if (eConfig.contains("pages")) {
                this.activeAdvisorPages = eConfig.getStringList("pages");
            }
        }
    }

    public com.h2ph.commands.admin.moderations.OffendPlugin getOffendPlugin() {
        return offendPlugin;
    }

    public com.h2ph.api.ApiServer getApiServer() {
        return apiServer;
    }

    public com.prismcore.survival.manager.KeyAllManager getKeyAllManager() {
        return keyAllManager;
    }

    public com.prismcore.survival.manager.CarouselManager getCarouselManager() {
        return carouselManager;
    }

    public com.prismcore.survival.manager.CrateLocationRegistry getCrateLocationRegistry() {
        return crateLocationRegistry;
    }

    public com.prismcore.survival.manager.CrateEffectsManager getCrateEffectsManager() {
        return crateEffectsManager;
    }

    public com.prismcore.survival.manager.SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public com.prismcore.survival.manager.TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public ShopCommand getShopCommand() {
        return shopCommand;
    }

    public com.h2ph.commands.player.RulesCommand getRulesCommand() {
        return rulesCommand;
    }

    // --- Chat Filter Config ---
    private org.bukkit.configuration.file.FileConfiguration chatFilterConfig;

    public void loadChatFilterConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/chatfilter/config.yml");
        if (!file.exists()) {
            saveResourceSafely("survival/chatfilter/config.yml");
        }
        chatFilterConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getChatFilterConfig() {
        if (chatFilterConfig == null) {
            loadChatFilterConfig();
        }
        return chatFilterConfig;
    }

    // --- RTP Config ---
    private org.bukkit.configuration.file.FileConfiguration rtpConfig;

    public void loadRTPConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/europe/config.yml");
        if (!file.exists()) {
            saveResourceSafely("rtp/europe/config.yml");
        }
        rtpConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getRTPConfig() {
        if (rtpConfig == null) {
            loadRTPConfig();
        }
        return rtpConfig;
    }

    public org.bukkit.configuration.file.FileConfiguration getRTPRegionConfig(String regionName) {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/" + regionName + "/config.yml");
        if (file.exists()) {
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        }
        return null;
    }

    // --- Global RTP Config ---
    private org.bukkit.configuration.file.FileConfiguration rtpGlobalConfig;

    public void loadGlobalRTPConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/config.yml");
        if (!file.exists()) {
            saveResourceSafely("rtp/config.yml");
        }
        rtpGlobalConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getGlobalRTPConfig() {
        if (rtpGlobalConfig == null) {
            loadGlobalRTPConfig();
        }
        return rtpGlobalConfig;
    }

    // --- SignInput Utility ---
    private com.h2ph.utils.SignInput signInput;

    public com.h2ph.utils.SignInput getSignInput() {
        return signInput;
    }

    private void printStartupBanner(boolean vaultEnabled) {
        org.bukkit.command.ConsoleCommandSender console = getServer().getConsoleSender();
        String version = getDescription().getVersion();

        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l   ___       _                &3&l  ___                "));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l  / _ \\ _ __(_)___ _ __ ___   &3&l / __\\___  _ __ ___  "));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l / /_)/ '__| / __| '_ ` _ \\  &3&l/ /  / _ \\| '__/ _ \\ "));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l/ ___/| |  | \\__ \\ | | | | | &3&l/ /__| (_) | | |  __/ "));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l\\/    |_|  |_|___/_| |_| |_| &3&l\\____/\\___/|_|  \\___| "));
        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7        Running &bPrismCore &7v" + version + " by &bh2ph"));
        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  &lMODULE STATUS:"));

        // Economy Status
        if (vaultEnabled && getServer().getPluginManager().getPlugin("Vault") != null) {
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [+] &fEconomy System: &a&lONLINE &7(Vault Hooked)"));
        } else {
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [-] &fEconomy System: &c&lOFFLINE &7(Vault Missing/Disabled)"));
        }

        // Api Status
        if (apiServer != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fWeb API Server: &a&lONLINE"));
        } else {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [-] &fWeb API Server: &c&lOFFLINE"));
        }

        // Moderation Status
        if (offendPlugin != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fModeration Core: &a&lONLINE"));
        }

        // WorldEdit Hook
        if (getServer().getPluginManager().getPlugin("WorldEdit") != null
                || getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fWorldEdit Hook: &a&lCONNECTED"));
        } else {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [-] &fWorldEdit Hook: &c&lNOT FOUND"));
        }

        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
        console.sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a&l  PRISM CORE SUCCESSFULLY INITALIZED"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
    }
}
