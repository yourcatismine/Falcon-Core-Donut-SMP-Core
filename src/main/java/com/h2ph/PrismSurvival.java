package com.h2ph;

import com.prismcore.survival.manager.PlayerDataManager;
import com.prismcore.survival.manager.DatabaseManager;
import com.prismcore.survival.scheduler.SchedulerAdapter;
import com.h2ph.commands.economy.ShopCommand;
import com.h2ph.commands.player.QuickGameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PrismSurvival extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private SchedulerAdapter schedulerAdapter;
    private ShopCommand shopCommand;
    private com.h2ph.commands.player.RulesCommand rulesCommand;
    private com.h2ph.commands.player.MediaCommand mediaCommand;

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

    private com.h2ph.rtp.RTPQueueManager rtpQueueManager;

    private com.h2ph.commands.admin.duels.DuelStatsManager duelStatsManager;
    private com.h2ph.commands.admin.duels.DuelArenaManager duelArenaManager;

    private com.h2ph.maintenance.MaintenanceManager maintenanceManager;
    private com.prismcore.survival.orders.OrdersModule ordersModule;
    private com.prismcore.survival.sell.PrismSell prismSell;
    private com.prismcore.survival.manager.ActivityLogger activityLogger;
    private com.prismcore.survival.manager.InventoryLogManager inventoryLogManager;
    private com.h2ph.managers.RedstoneManager redstoneManager;
    private com.prismcore.survival.manager.HazardManager hazardManager;
    private com.prismcore.survival.manager.BalanceLogger balanceLogger;
    private com.h2ph.managers.PrivateMessageManager privateMessageManager;
    private com.prismcore.survival.manager.BountyManager bountyManager;
    private com.h2ph.utils.SignInput signInput;
    private com.h2ph.managers.EnderChestManager enderChestManager;
    private com.h2ph.managers.HomeManager homeManager;
    private com.h2ph.managers.ScoreboardManager scoreboardManager;
    private com.prismcore.survival.manager.VoidManager voidManager;
    private com.h2ph.managers.VanishManager vanishManager;
    private com.h2ph.managers.RTPDeathManager rtpDeathManager;
    private com.h2ph.gui.RTPDeathGUI rtpDeathGUI;
    private com.h2ph.teams.TeamManager teamManager;
    private com.h2ph.teams.TeamInviteManager teamInviteManager;
    private com.h2ph.managers.GamertagManager gamertagManager;

    private com.prismcore.survival.limiter.LimiterConfig limiterConfig;
    private com.prismcore.survival.limiter.LimiterManager limiterManager;
    private com.prismcore.survival.survival.ChatFilter chatFilter;
    private com.h2ph.listeners.CommandHideListener commandHideListener;

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
        this.commandHideListener = new com.h2ph.listeners.CommandHideListener(this);
        getServer().getPluginManager().registerEvents(commandHideListener, this);

        // Register MessageHider
        new com.prismcore.survival.survival.MessageHider(this);

        // Register ItemMerger (Aggressive Stacking)
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.survival.ItemMerger(this), this);

        // Register ChatFilter
        this.chatFilter = new com.prismcore.survival.survival.ChatFilter(this);
        getServer().getPluginManager().registerEvents(chatFilter, this);

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

        // Register Combat Listener (handles combat tagging, actionbar countdowns and
        // combat-logout)
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.AutoRTPListener(this), this);

        // Register Live Sign Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        // Initialize managers
        this.playerDataManager = new PlayerDataManager(this);
        this.databaseManager = new DatabaseManager(this, getSurvivalConfig());
        this.schedulerAdapter = new SchedulerAdapter(this);
        this.keyAllManager = new com.prismcore.survival.manager.KeyAllManager(this);
        this.carouselManager = new com.prismcore.survival.manager.CarouselManager(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateEffectsManager = new com.prismcore.survival.manager.CrateEffectsManager(this);
        this.spawnManager = new com.prismcore.survival.manager.SpawnManager(this);
        this.teleportManager = new com.prismcore.survival.manager.TeleportManager(this);
        this.privateMessageManager = new com.h2ph.managers.PrivateMessageManager();
        this.bountyManager = new com.prismcore.survival.manager.BountyManager(this);

        // Register Live Sign Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        // Register Crate Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CrateListener(this), this);

        // Register Player Connection Listener (For saving data)
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.PlayerConnectionListener(this), this);

        // Register Inventory Sync Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.InventorySyncListener(this), this);

        // Setup Player Inventory Auto-Save Task (Every 10 minutes = 12000 ticks)
        getSchedulerAdapter().runTaskTimer(() -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                // Capture inventory contents on main thread (clone array for safety)
                final org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents().clone();
                final org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents().clone();
                final java.util.UUID uuid = player.getUniqueId();
                final String name = player.getName();

                // Serialize and save asynchronously to avoid blocking the main thread
                getSchedulerAdapter().runTaskAsynchronously(() -> {
                    try {
                        String invBase64 = com.prismcore.survival.utils.ItemSerializationManager
                                .itemStackArrayToBase64(contents);
                        String armorBase64 = com.prismcore.survival.utils.ItemSerializationManager
                                .itemStackArrayToBase64(armor);
                        getDatabaseManager().saveInventory(uuid, invBase64, armorBase64);
                    } catch (Exception e) {
                        getLogger().log(java.util.logging.Level.SEVERE,
                                "Failed to serialize inventory for auto-save of " + name, e);
                    }
                });
            }
        }, 12000L, 12000L);

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
        // Register SignInput Utility
        this.signInput = new com.h2ph.utils.SignInput(this);
        getServer().getPluginManager().registerEvents(this.signInput, this);

        // Register shards admin command
        com.h2ph.commands.admin.economy.ShardsCommand shardsCommand = new com.h2ph.commands.admin.economy.ShardsCommand(
                this);
        getCommand("shards").setExecutor(shardsCommand);
        getCommand("shards").setTabCompleter(shardsCommand);

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

        // Register Sell History Command
        com.h2ph.commands.economy.SellHistoryCommand sellHistoryCommand = new com.h2ph.commands.economy.SellHistoryCommand(
                this);
        getCommand("sellhistory").setExecutor(sellHistoryCommand);

        // Register Worth Command
        getCommand("worth").setExecutor(new com.h2ph.commands.economy.WorthCommand(this));

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

        // Register Mute Command
        getCommand("mute").setExecutor(new com.h2ph.commands.admin.moderations.MuteCommand(this));
        getCommand("mute").setTabCompleter(new com.h2ph.commands.admin.moderations.MuteCommand(this));

        getCommand("unmute").setExecutor(new com.h2ph.commands.admin.moderations.UnmuteCommand(this));
        getCommand("unmute").setTabCompleter(new com.h2ph.commands.admin.moderations.UnmuteCommand(this));
        getCommand("checkmute").setExecutor(new com.h2ph.commands.admin.moderations.CheckMuteCommand(this));
        getCommand("checkmute").setTabCompleter(new com.h2ph.commands.admin.moderations.CheckMuteCommand(this));

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

        // Initialize RTP Queue Manager
        this.rtpQueueManager = new com.h2ph.rtp.RTPQueueManager(this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.RTPQueueListener(this), this);

        // Initialize RTP Death Manager
        this.rtpDeathManager = new com.h2ph.managers.RTPDeathManager(this);
        this.rtpDeathGUI = new com.h2ph.gui.RTPDeathGUI(this);

        // Register Rules Command
        this.rulesCommand = new com.h2ph.commands.player.RulesCommand(this);
        getCommand("rules").setExecutor(rulesCommand);
        getServer().getPluginManager().registerEvents(rulesCommand, this);

        // Register Media Command
        this.mediaCommand = new com.h2ph.commands.player.MediaCommand(this);
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

        // Register Reply Command
        com.h2ph.commands.player.ReplyCommand replyCmd = new com.h2ph.commands.player.ReplyCommand();
        getCommand("reply").setExecutor(replyCmd);
        getCommand("reply").setTabCompleter(replyCmd);

        // Register Settings Command
        com.h2ph.commands.player.SettingsCommand settingsCmd = new com.h2ph.commands.player.SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCmd);

        // Register Bounty Command
        com.h2ph.commands.player.BountyCommand bountyCommand = new com.h2ph.commands.player.BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);
        getCommand("bounties").setExecutor(bountyCommand);
        getCommand("bounties").setTabCompleter(bountyCommand);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyConfirmGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.WorthGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.SettingsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TpaConfirmGUIListener(), this);

        // Register Quick Gamemode Commands
        QuickGameMode quickGameMode = new QuickGameMode();
        getCommand("gmc").setExecutor(quickGameMode);
        getCommand("gms").setExecutor(quickGameMode);
        getCommand("gma").setExecutor(quickGameMode);

        // Register Fly Command
        com.h2ph.commands.player.FlyCommand flyCommand = new com.h2ph.commands.player.FlyCommand(this);
        getCommand("fly").setExecutor(flyCommand);
        getServer().getPluginManager().registerEvents(flyCommand, this);

        // Register NightVision Command
        getCommand("nv").setExecutor(new com.h2ph.commands.player.NightVisionCommand(this));

        // Register Discord Command
        getCommand("discord").setExecutor(new com.h2ph.commands.player.DiscordCommand(this));

        // Register Store Command
        getCommand("store").setExecutor(new com.h2ph.commands.player.StoreCommand(this));

        // Initialize Vanish Manager
        this.vanishManager = new com.h2ph.managers.VanishManager(this);

        // Register Vanish Command
        com.h2ph.commands.admin.moderations.VanishCommand vanishCmd = new com.h2ph.commands.admin.moderations.VanishCommand(
                this);
        getCommand("vanish").setExecutor(vanishCmd);
        getCommand("vanish").setTabCompleter(vanishCmd);

        // Register Vanish Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.VanishListener(this), this);

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.h2ph.placeholders.PrismPlaceholders(this).register();
            new com.h2ph.placeholders.RTPPlaceholders(this).register();
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

        // Initialize Sell Module
        this.prismSell = new com.prismcore.survival.sell.PrismSell(this);
        this.prismSell.onEnable();

        // Initialize Ender Chest Manager (requires PrismSell DB)
        this.enderChestManager = new com.h2ph.managers.EnderChestManager(this);

        // Register Ender Chest Command
        getCommand("echest").setExecutor(new com.h2ph.commands.player.EnderChestCommand(this));

        // Register Ender Chest GUI Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.EnderChestGUIListener(this), this);

        // Initialize Home Manager
        this.homeManager = new com.h2ph.managers.HomeManager(this);

        // Register Home Command
        com.h2ph.commands.player.HomeCommand homeCmd = new com.h2ph.commands.player.HomeCommand(this);
        getCommand("home").setExecutor(homeCmd);
        getCommand("home").setTabCompleter(homeCmd);

        // Register Home GUI Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.HomeGUIListener(this), this);

        // Register Home Chat Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.HomeChatListener(this), this);

        // Initialize Scoreboard Manager
        this.scoreboardManager = new com.h2ph.managers.ScoreboardManager(this);
        this.scoreboardManager.setup();

        // Initialize Gamertag Manager
        this.gamertagManager = new com.h2ph.managers.GamertagManager(this);

        // Initialize Team Managers
        this.teamManager = new com.h2ph.teams.TeamManager(this);
        this.teamInviteManager = new com.h2ph.teams.TeamInviteManager(this);

        // Register Team Listeners
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TeamPvPListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TeamChatListener(this), this);

        // Register Team Command
        com.h2ph.commands.player.TeamCommand teamCommand = new com.h2ph.commands.player.TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

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

        // Start Team Chat Actionbar Reminder
        startTeamChatTask();

        // Register Reload Command
        getCommand("prismreload").setExecutor(new com.h2ph.commands.admin.ReloadCommand(this));

        // Initialize Void Manager
        this.voidManager = new com.prismcore.survival.manager.VoidManager(this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.VoidProtectionListener(this), this);

        // Initialize Limiter Manager
        this.limiterConfig = new com.prismcore.survival.limiter.LimiterConfig(this);
        this.limiterManager = new com.prismcore.survival.limiter.LimiterManager(this, this.limiterConfig);
        this.limiterManager.start();
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.limiter.LimiterListener(this), this);

        // Register Falcon Command
        com.h2ph.commands.admin.FalconCommand falconCommand = new com.h2ph.commands.admin.FalconCommand(this);
        getCommand("falcon").setExecutor(falconCommand);
        getCommand("falcon").setTabCompleter(falconCommand);

        // Register Stats Command
        getCommand("stats").setExecutor(new com.h2ph.commands.player.StatsCommand(this));

        // Register WhereAmI Command
        com.h2ph.commands.player.WhereAmICommand whereAmICommand = new com.h2ph.commands.player.WhereAmICommand(this);
        getCommand("whereami").setExecutor(whereAmICommand);
        getCommand("whereami").setTabCompleter(whereAmICommand);

        // Register HideName Command
        getCommand("hidename").setExecutor(new com.h2ph.commands.player.HideNameCommand(this));
        // Register InvSee and EnderSee admin commands
        com.h2ph.commands.admin.moderations.InvSeeCommand invSeeCmd = new com.h2ph.commands.admin.moderations.InvSeeCommand(
                this);
        getCommand("invsee").setExecutor(invSeeCmd);
        getCommand("invsee").setTabCompleter(invSeeCmd);

        com.h2ph.commands.admin.moderations.EnderSeeCommand enderSeeCmd = new com.h2ph.commands.admin.moderations.EnderSeeCommand(
                this);
        getCommand("endersee").setExecutor(enderSeeCmd);
        getCommand("endersee").setTabCompleter(enderSeeCmd);

        // Register InvSee Listener
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.InvSeeListener(this), this);

        // Register Operator listener to enforce allowed operators list
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.OperatorListener(this), this);

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
        // Save all online players first while databases are still open
        if (this.playerDataManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                this.playerDataManager.savePlayer(player.getUniqueId());
            }
        }

        // Save all online ender chests
        if (this.enderChestManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                // Close the inventory first if it's an ender chest GUI to ensure save
                if (player.getOpenInventory().getTopInventory()
                        .getHolder() instanceof com.h2ph.gui.EnderChestGUI.EnderChestHolder) {
                    org.bukkit.inventory.ItemStack[] contents = player.getOpenInventory().getTopInventory()
                            .getContents().clone();
                    this.enderChestManager.saveEnderChest(player.getUniqueId(), contents);
                }
            }
        }

        if (this.prismSell != null) {
            this.prismSell.onDisable();
        }

        if (this.apiServer != null) {
            this.apiServer.stop();
        }

        if (this.ordersModule != null) {
            this.ordersModule.disable();
        }

        if (this.rtpQueueManager != null) {
            this.rtpQueueManager.disable();
        }

        if (this.auctionController != null) {
            this.auctionController.disable();
        }

        if (this.limiterManager != null) {
            this.limiterManager.shutdown();
        }

        if (this.signInput != null) {
            this.signInput.cleanup();
        }

        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }

        if (this.activityLogger != null) {
            this.activityLogger.shutdown();
        }

        if (this.bountyManager != null) {
            this.bountyManager.save();
        }

        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }

        getLogger().info("PrismCore has been disabled!");
        instance = null;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    public com.h2ph.rtp.RTPQueueManager getRTPQueueManager() {
        return rtpQueueManager;
    }

    public com.h2ph.maintenance.MaintenanceManager getMaintenanceManager() {
        return maintenanceManager;
    }

    public com.prismcore.survival.sell.PrismSell getPrismSell() {
        return prismSell;
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

    public com.h2ph.managers.PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public com.prismcore.survival.manager.BountyManager getBountyManager() {
        return bountyManager;
    }

    public com.h2ph.utils.SignInput getSignInput() {
        return signInput;
    }

    public com.h2ph.managers.EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }

    public com.h2ph.managers.HomeManager getHomeManager() {
        return homeManager;
    }

    public com.h2ph.managers.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public com.prismcore.survival.manager.VoidManager getVoidManager() {
        return voidManager;
    }

    public com.h2ph.managers.VanishManager getVanishManager() {
        return vanishManager;
    }

    public com.h2ph.managers.RTPDeathManager getRTPDeathManager() {
        return rtpDeathManager;
    }

    public com.h2ph.gui.RTPDeathGUI getRTPDeathGUI() {
        return rtpDeathGUI;
    }

    public com.h2ph.teams.TeamManager getTeamManager() {
        return teamManager;
    }

    public com.h2ph.teams.TeamInviteManager getTeamInviteManager() {
        return teamInviteManager;
    }

    public com.h2ph.managers.GamertagManager getGamertagManager() {
        return gamertagManager;
    }

    public com.prismcore.survival.limiter.LimiterConfig getLimiterConfig() {
        return limiterConfig;
    }

    public com.prismcore.survival.limiter.LimiterManager getLimiterManager() {
        return limiterManager;
    }

    public com.prismcore.survival.manager.KeyAllManager getKeyAllManager() {
        return keyAllManager;
    }

    public com.prismcore.survival.tools.ToolsManager getToolsManager() {
        return com.prismcore.survival.tools.ToolsManager.getInstance();
    }

    public com.h2ph.commands.player.MediaCommand getMediaCommand() {
        return mediaCommand;
    }

    public com.prismcore.survival.survival.ChatFilter getChatFilter() {
        return chatFilter;
    }

    public com.h2ph.listeners.CommandHideListener getCommandHideListener() {
        return commandHideListener;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        return (rsp == null) ? null : rsp.getProvider();
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
        saveResourceSafely("scoreboard/config.yml");
    }

    private void startTeamChatTask() {
        getSchedulerAdapter().runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                com.prismcore.survival.manager.PlayerData data = getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.isTeamChat() && data.getTeamId() != null) {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(
                                    com.prismcore.survival.orders.Utils.formatColors("&dYou have team chat on.")));
                }
            }
        }, 40L, 40L); // Every 2 seconds
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
